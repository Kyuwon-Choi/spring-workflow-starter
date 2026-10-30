import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useAuth } from '../auth/Auth';

type Member = { login: string; tenant: string; department: string; roles: string; projects: string; active: boolean; version: number };
type Rule = { key: string; effectiveAt: string; financeAbove: number | null };
type Delegation = {
  id: number;
  grantor: string;
  delegate: string;
  project: string;
  validFrom: string;
  validUntil: string;
  revokedAt: string | null;
};
type Overview = {
  member: Member;
  members: Member[];
  projects: { code: string; department: string; budgetLimit?: number; spent?: number }[];
  rules: Rule[];
  delegations: Delegation[];
  audit: { id: number; actor: string; action: string; occurredAt: string }[];
};
const BASE = '/api/fde-enterprise';
const roles = ['STAFF', 'MANAGER', 'FINANCE', 'ORG_ADMIN'];
const message = (error: unknown) =>
  axios.isAxiosError(error) ? String(error.response?.data?.detail || '현재 권한 또는 입력을 확인하세요.') : '처리 중 오류가 발생했습니다.';

export default function Organization() {
  const systemAdmin = useAuth().authorities.includes('ROLE_ADMIN');
  const [data, setData] = useState<Overview | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);
  const [company, setCompany] = useState('');
  const [department, setDepartment] = useState('');
  const [project, setProject] = useState('');
  const [budget, setBudget] = useState('');
  const [key, setKey] = useState('');
  const [threshold, setThreshold] = useState('300000');
  const [effective, setEffective] = useState('');
  const [login, setLogin] = useState('');
  const [memberDepartment, setMemberDepartment] = useState('');
  const [memberRoles, setMemberRoles] = useState<string[]>(['STAFF']);
  const [memberProjects, setMemberProjects] = useState<string[]>([]);
  const [active, setActive] = useState(true);
  const [delegate, setDelegate] = useState('');
  const [delegateProject, setDelegateProject] = useState('');
  const [minutes, setMinutes] = useState('60');
  const refresh = async () => setData((await axios.get<Overview>(BASE)).data);
  useEffect(() => {
    void refresh().catch(e => setError(message(e)));
  }, []);
  const run = async (work: () => Promise<unknown>) => {
    setBusy(true);
    setError('');
    try {
      await work();
      await refresh();
    } catch (e) {
      setError(message(e));
    } finally {
      setBusy(false);
    }
  };
  const submit = (event: React.FormEvent, work: () => Promise<unknown>) => {
    event.preventDefault();
    void run(work);
  };
  const field = (label: string, value: string, update: (s: string) => void, type = 'text', required = true) => (
    <label className="form-label d-block">
      {label}
      <input className="form-control" type={type} value={value} required={required} maxLength={64} onChange={e => update(e.target.value)} />
    </label>
  );
  const isAdmin = data?.member.roles.split(',').includes('ORG_ADMIN');
  const isManager = data?.member.roles.split(',').includes('MANAGER');
  const chooseMember = (name: string) => {
    setLogin(name);
    const member = data?.members.find(m => m.login === name);
    setMemberDepartment(member?.department || '');
    setMemberRoles(member?.roles.split(',') || ['STAFF']);
    setMemberProjects(member?.projects.split(',').filter(Boolean) || []);
    setActive(member?.active ?? true);
  };
  return (
    <section className="py-3" aria-label="조직 권한과 승인 설정">
      <h1>조직 권한과 승인 설정</h1>
      <p>
        <a href="/fde">업무 요청 처리로 이동</a>
      </p>
      {error && (
        <div role="alert" className="alert alert-danger">
          {error}
        </div>
      )}
      {!data && systemAdmin && (
        <form
          className="card card-body"
          onSubmit={e => submit(e, () => axios.post(`${BASE}/organization`, { tenant: company, department }))}
        >
          <h2 className="h5">최초 조직 구성</h2>
          {field('회사 코드', company, setCompany)}
          {field('관리 부서', department, setDepartment)}
          <button disabled={busy}>조직 만들기</button>
        </form>
      )}
      {data && (
        <>
          <p>
            {data.member.login} · 회사 {data.member.tenant} · 부서 {data.member.department} · 역할 {data.member.roles}
          </p>
          <h2 className="h5">참여 프로젝트</h2>
          <table className="table">
            <thead>
              <tr>
                <th>프로젝트</th>
                <th>부서</th>
                <th>예산</th>
                <th>사용액</th>
              </tr>
            </thead>
            <tbody>
              {data.projects.map(p => (
                <tr key={p.code}>
                  <td>{p.code}</td>
                  <td>{p.department}</td>
                  <td>{p.budgetLimit ?? '조회 제한'}</td>
                  <td>{p.spent ?? '조회 제한'}</td>
                </tr>
              ))}
            </tbody>
          </table>
          <h2 className="h5">승인 규칙 이력</h2>
          <p>제출 시점의 규칙을 유지합니다. 반려 후 재제출하면 현재 규칙을 적용합니다.</p>
          <ul>
            {data.rules.map(r => (
              <li key={r.key}>
                {r.key} · {new Date(r.effectiveAt).toLocaleString()}부터 ·{' '}
                {r.financeAbove === null ? '부서 승인으로 완료' : `${r.financeAbove.toLocaleString()}원 초과 시 재무 추가 승인`}
              </li>
            ))}
          </ul>
          <div className="row g-3">
            {isAdmin && (
              <>
                <form
                  className="col-md-6"
                  onSubmit={e =>
                    submit(e, () =>
                      axios.post(`${BASE}/rules`, {
                        key,
                        financeAbove: threshold === '' ? null : Number(threshold),
                        effectiveAt: effective ? new Date(effective).toISOString() : null,
                      }),
                    )
                  }
                >
                  <h2 className="h5">새 승인 규칙 게시</h2>
                  {field('규칙 이름', key, setKey)}
                  {field('재무 추가 승인 기준 금액 (초과)', threshold, setThreshold, 'number', false)}
                  {field('시행 시각 (비우면 즉시)', effective, setEffective, 'datetime-local', false)}
                  <button className="btn btn-primary" disabled={busy}>
                    새 규칙 게시
                  </button>
                </form>
                <form
                  className="col-md-6"
                  onSubmit={e =>
                    submit(e, () => axios.post(`${BASE}/projects`, { code: project, department, budgetLimit: Number(budget) }))
                  }
                >
                  <h2 className="h5">프로젝트 추가</h2>
                  {field('새 프로젝트 코드', project, setProject)}
                  {field('프로젝트 부서', department, setDepartment)}
                  {field('프로젝트 예산', budget, setBudget, 'number')}
                  <button className="btn btn-primary" disabled={busy}>
                    프로젝트 추가
                  </button>
                </form>
                <form
                  className="col-md-6"
                  onSubmit={e =>
                    submit(e, () =>
                      axios.post(`${BASE}/members`, {
                        login,
                        department: memberDepartment,
                        roles: memberRoles,
                        projects: memberProjects,
                        active,
                        expectedVersion: data.members.find(m => m.login === login)?.version ?? null,
                      }),
                    )
                  }
                >
                  <h2 className="h5">계정 소속과 권한</h2>
                  <p>기존 로그인 계정에 회사 권한을 부여합니다.</p>
                  <label className="form-label">
                    구성원 선택
                    <select
                      className="form-select"
                      value={data.members.some(m => m.login === login) ? login : ''}
                      onChange={e => chooseMember(e.target.value)}
                    >
                      <option value="">새 구성원 또는 직접 입력</option>
                      {data.members.map(m => (
                        <option key={m.login}>{m.login}</option>
                      ))}
                    </select>
                  </label>
                  {field('계정 로그인', login, chooseMember)}
                  {field('계정 부서', memberDepartment, setMemberDepartment)}
                  <fieldset>
                    <legend className="h6">업무 역할</legend>
                    {roles.map(role => (
                      <label className="me-3" key={role}>
                        <input
                          type="checkbox"
                          checked={memberRoles.includes(role)}
                          onChange={e => setMemberRoles(old => (e.target.checked ? [...old, role] : old.filter(r => r !== role)))}
                        />{' '}
                        {role}
                      </label>
                    ))}
                  </fieldset>
                  <fieldset>
                    <legend className="h6">참여 프로젝트</legend>
                    {data.projects.map(p => (
                      <label className="me-3" key={p.code}>
                        <input
                          type="checkbox"
                          checked={memberProjects.includes(p.code)}
                          onChange={e =>
                            setMemberProjects(old => (e.target.checked ? [...old, p.code] : old.filter(code => code !== p.code)))
                          }
                        />{' '}
                        {p.code}
                      </label>
                    ))}
                  </fieldset>
                  <label className="d-block my-2">
                    <input type="checkbox" checked={active} onChange={e => setActive(e.target.checked)} /> 업무 접근 활성
                  </label>
                  <button className="btn btn-primary" disabled={busy}>
                    소속과 권한 저장
                  </button>
                </form>
              </>
            )}
            {isManager && (
              <form
                className="col-md-6"
                onSubmit={e =>
                  submit(e, () => {
                    const now = new Date();
                    return axios.post(`${BASE}/delegations`, {
                      to: delegate,
                      project: delegateProject,
                      from: now.toISOString(),
                      until: new Date(now.getTime() + Number(minutes) * 60000).toISOString(),
                    });
                  })
                }
              >
                <h2 className="h5">기간 한정 승인 위임</h2>
                {field('대리 승인 계정', delegate, setDelegate)}
                {field('위임 프로젝트 코드', delegateProject, setDelegateProject)}
                {field('위임 기간 (분, 최대 1440)', minutes, setMinutes, 'number')}
                <button className="btn btn-primary" disabled={busy}>
                  승인 권한 위임
                </button>
              </form>
            )}
          </div>
          <h2 className="h5 mt-4">위임 이력</h2>
          <ul>
            {data.delegations.map(d => (
              <li key={d.id}>
                {d.grantor} → {d.delegate} · {d.project} · {new Date(d.validUntil).toLocaleString()}까지 ·{' '}
                {d.revokedAt ? '취소됨' : '기간에 따라 유효'}{' '}
                {!d.revokedAt && (isAdmin || d.grantor === data.member.login) && (
                  <button
                    className="btn btn-sm btn-outline-danger"
                    disabled={busy}
                    onClick={() => void run(() => axios.post(`${BASE}/delegations/${d.id}/revoke`))}
                  >
                    위임 취소
                  </button>
                )}
              </li>
            ))}
          </ul>
          {isAdmin && (
            <>
              <h2 className="h5">최근 설정 변경 이력</h2>
              <ul>
                {data.audit.map(a => (
                  <li key={a.id}>
                    {a.actor} · {a.action} · {new Date(a.occurredAt).toLocaleString()}
                  </li>
                ))}
              </ul>
            </>
          )}
          <button className="btn btn-outline-secondary" disabled={busy} onClick={() => void run(async () => {})}>
            새로고침
          </button>
        </>
      )}
    </section>
  );
}
