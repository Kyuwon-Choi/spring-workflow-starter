import React, { useEffect, useState } from 'react';
import axios from 'axios';
import { useAuth } from '../auth/Auth';

export type Field = {
  label: string;
  type: string;
  required: boolean;
  maxLength?: number;
  min?: number;
  max?: number;
  options?: string[];
  reference?: string;
  readOnly?: boolean;
};
type Action = { label: string; inputs: Record<string, Field> };
type ObjectType = {
  label: string;
  labelField: string;
  fields: Record<string, Field>;
  states: string[];
  createRoles: string[];
  actions: Record<string, Action>;
};
export type Domain = { id: string; label: string; types: Record<string, ObjectType> };
type Item = {
  id: number;
  data: Record<string, string | number | boolean>;
  state: string;
  owner: string;
  version: number;
  availableActions: string[];
};
type Result = { items: Item[]; total: number; page: number; size: number };
type Audit = {
  id: number;
  actor: string;
  action: string;
  beforeData: unknown;
  afterData: unknown;
  beforeState: string;
  afterState: string;
  occurredAt: string;
};
export type WorkflowToolsProps = { base: string; fields: Record<string, Field>; canCreate: boolean; q: string; stateFilter: string; refresh: () => Promise<void> };

export const typedValues = (fields: Record<string, Field>, values: Record<string, string>): Record<string, unknown> =>
  Object.fromEntries(
    Object.entries(fields)
      .filter(([key, field]) => !field.readOnly && values[key] !== undefined && values[key] !== '')
      .map(([key, field]) => {
        const raw = values[key];
        return [
          key,
          ['integer', 'decimal', 'reference'].includes(field.type) ? Number(raw) : field.type === 'boolean' ? raw === 'true' : raw,
        ];
      }),
  );

const Inputs = ({
  fields,
  values,
  change,
  references,
  prefix,
}: {
  fields: Record<string, Field>;
  values: Record<string, string>;
  change: (name: string, value: string) => void;
  references: Record<string, Item[]>;
  prefix: string;
}) => (
  <>
    {Object.entries(fields)
      .filter(([, field]) => !field.readOnly)
      .map(([name, field]) => {
        const id = `${prefix}-${name}`;
        return (
          <div className="mb-2" key={name}>
            <label htmlFor={id} className="form-label">
              {field.label}
              {field.required ? ' *' : ''}
            </label>
            {['enum', 'boolean', 'reference'].includes(field.type) ? (
              <select
                id={id}
                className="form-select"
                required={field.required}
                value={values[name] || ''}
                onChange={event => change(name, event.target.value)}
              >
                <option value="">선택</option>
                {field.type === 'enum' && field.options?.map(option => <option key={option}>{option}</option>)}
                {field.type === 'boolean' && (
                  <>
                    <option value="true">예</option>
                    <option value="false">아니오</option>
                  </>
                )}
                {field.type === 'reference' &&
                  (references[field.reference || ''] || []).map(item => (
                    <option key={item.id} value={item.id}>
                      #{item.id} · {String(Object.values(item.data).slice(0, 2).join(' / '))}
                    </option>
                  ))}
              </select>
            ) : (
              <input
                id={id}
                className="form-control"
                required={field.required}
                maxLength={field.maxLength || 500}
                min={field.min}
                max={field.max}
                step={field.type === 'decimal' ? 'any' : 1}
                type={field.type === 'date' ? 'date' : ['integer', 'decimal'].includes(field.type) ? 'number' : 'text'}
                value={values[name] || ''}
                onChange={event => change(name, event.target.value)}
              />
            )}
            {field.type === 'reference' && (
              <small className="text-muted">연결 대상에서 먼저 항목을 등록하세요. 최근 100건을 선택할 수 있습니다.</small>
            )}
          </div>
        );
      })}
  </>
);

const Workbench = ({ tools: Tools }: { tools?: React.ComponentType<WorkflowToolsProps> }) => {
  const { authorities } = useAuth();
  const [catalog, setCatalog] = useState<Domain[]>([]);
  const [domainId, setDomainId] = useState('');
  const [typeId, setTypeId] = useState('');
  const [result, setResult] = useState<Result>({ items: [], total: 0, page: 0, size: 25 });
  const [counts, setCounts] = useState<Record<string, number>>({});
  const [references, setReferences] = useState<Record<string, Item[]>>({});
  const [q, setQ] = useState('');
  const [stateFilter, setStateFilter] = useState('');
  const [page, setPage] = useState(0);
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [selected, setSelected] = useState<Item | null>(null);
  const [history, setHistory] = useState<Audit[]>([]);
  const [actionId, setActionId] = useState('');
  const [parameters, setParameters] = useState<Record<string, string>>({});
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const domain = catalog.find(d => d.id === domainId);
  const type = domain?.types[typeId];
  const base = `/api/fde/${domainId}/${typeId}`;
  const canCreate = type?.createRoles.some(role => authorities.includes(role));
  const failureMessage = (failure: unknown) =>
    axios.isAxiosError(failure)
      ? String(failure.response?.data?.detail || '작업에 실패했습니다. 권한·입력·연결을 확인하세요.')
      : '작업에 실패했습니다.';
  const run = async (work: () => Promise<void>) => {
    setBusy(true);
    setError('');
    try {
      await work();
    } catch (failure) {
      setError(failureMessage(failure));
    } finally {
      setBusy(false);
    }
  };
  useEffect(() => {
    let active = true;
    axios
      .get<Domain[]>('/api/fde/catalog')
      .then(response => {
        if (!active) return;
        setCatalog(response.data);
        const first = response.data[0];
        if (first) {
          setDomainId(first.id);
          setTypeId(Object.keys(first.types)[0]);
        }
      })
      .catch(failure => {
        if (active) setError(failureMessage(failure));
      });
    return () => {
      active = false;
    };
  }, []);
  const refresh = async () => {
    const [objects, summary] = await Promise.all([
      axios.get<Result>(`${base}/objects`, { params: { page, size: 25, q, state: stateFilter } }),
      axios.get<Record<string, number>>(`${base}/summary`),
    ]);
    setResult(objects.data);
    setCounts(summary.data);
  };
  useEffect(() => {
    if (!type) return;
    let active = true;
    Promise.all([
      axios.get<Result>(`${base}/objects`, { params: { page, size: 25, q, state: stateFilter } }),
      axios.get<Record<string, number>>(`${base}/summary`),
    ])
      .then(([objects, summary]) => {
        if (active) {
          setResult(objects.data);
          setCounts(summary.data);
        }
      })
      .catch(failure => {
        if (active) setError(failureMessage(failure));
      });
    return () => {
      active = false;
    };
  }, [domainId, typeId, page, q, stateFilter]);
  useEffect(() => {
    if (!type) return;
    let active = true;
    setReferences({});
    const referenceTypes = [
      ...new Set(
        [...Object.values(canCreate ? type.fields : {}), ...Object.values(type.actions[actionId]?.inputs || {})]
          .filter(field => !field.readOnly && field.type === 'reference')
          .map(field => field.reference)
          .filter((value): value is string => Boolean(value)),
      ),
    ];
    Promise.all(
      referenceTypes.map(async reference => {
        const response = await axios.get<Result>(`/api/fde/${domainId}/${reference}/objects`, { params: { size: 100 } });
        return [reference, response.data.items] as const;
      }),
    )
      .then(entries => {
        if (active) setReferences(Object.fromEntries(entries));
      })
      .catch(failure => {
        if (active) setError(failureMessage(failure));
      });
    return () => {
      active = false;
    };
  }, [domainId, typeId, actionId, canCreate]);
  const resetSelection = () => {
    setSelected(null);
    setHistory([]);
    setActionId('');
    setParameters({});
    setDraft({});
    setPage(0);
    setQ('');
    setStateFilter('');
    setError('');
  };
  const select = async (item: Item) => {
    const [current, audit] = await Promise.all([
      axios.get<Item>(`${base}/objects/${item.id}`),
      axios.get<Audit[]>(`${base}/objects/${item.id}/history`),
    ]);
    setSelected(current.data);
    setHistory(audit.data);
    setActionId('');
    setParameters({});
  };
  const create = (event: React.FormEvent) => {
    event.preventDefault();
    if (!type) return;
    void run(async () => {
      await axios.post(`${base}/objects`, { data: typedValues(type.fields, draft) });
      setDraft({});
      await refresh();
    });
  };
  const execute = (event: React.FormEvent) => {
    event.preventDefault();
    if (!selected || !type || !actionId) return;
    void run(async () => {
      const response = await axios.post<Item>(`${base}/objects/${selected.id}/actions/${actionId}`, {
        expectedVersion: selected.version,
        data: typedValues(type.actions[actionId].inputs, parameters),
      });
      await refresh();
      await select(response.data);
    });
  };
  return (
    <section className="py-3" aria-label="업무 처리">
      <h1>업무 처리</h1>
      {error && (
        <div role="alert" className="alert alert-danger">
          {error}
        </div>
      )}
      <div className="row mb-3">
        <div className="col-md-6">
          <label htmlFor="fde-domain" className="form-label">
            업무
          </label>
          <select
            disabled={busy}
            id="fde-domain"
            className="form-select"
            value={domainId}
            onChange={event => {
              const next = catalog.find(d => d.id === event.target.value);
              resetSelection();
              setDomainId(event.target.value);
              setTypeId(next ? Object.keys(next.types)[0] : '');
            }}
          >
            {catalog.map(d => (
              <option value={d.id} key={d.id}>
                {d.label}
              </option>
            ))}
          </select>
        </div>
        <div className="col-md-6">
          <label htmlFor="fde-type" className="form-label">
            객체
          </label>
          <select
            disabled={busy}
            id="fde-type"
            className="form-select"
            value={typeId}
            onChange={event => {
              resetSelection();
              setTypeId(event.target.value);
            }}
          >
            {domain &&
              Object.entries(domain.types).map(([id, definition]) => (
                <option value={id} key={id}>
                  {definition.label}
                </option>
              ))}
          </select>
        </div>
      </div>
      {type && (
        <>
          <div className="d-flex flex-wrap gap-2 mb-3" aria-label="상태별 집계">
            {Object.entries(counts).map(([state, count]) => (
              <span className="badge text-bg-secondary fs-6" key={state}>
                {state}: {count}
              </span>
            ))}
          </div>
          <div className="row g-3">
            <div className="col-lg-4">
              {canCreate && (
                <form className="card card-body mb-3" onSubmit={create}>
                  <h2 className="h5">{type.label} 등록</h2>
                  <Inputs
                    fields={type.fields}
                    values={draft}
                    change={(name, value) => setDraft(old => ({ ...old, [name]: value }))}
                    references={references}
                    prefix="create"
                  />
                  <button className="btn btn-primary" type="submit" disabled={busy}>
                    등록
                  </button>
                </form>
              )}
              {Tools && <Tools key={base} base={base} fields={type.fields} canCreate={Boolean(canCreate)} q={q} stateFilter={stateFilter} refresh={refresh}/>}
            </div>
            <div className="col-lg-8">
              <div className="card card-body mb-3">
                <h2 className="h5">
                  {type.label} 목록 · {result.total}건
                </h2>
                <div className="d-flex gap-2 mb-3">
                  <label className="visually-hidden" htmlFor="fde-search">
                    검색
                  </label>
                  <input
                    id="fde-search"
                    placeholder="검색"
                    className="form-control"
                    maxLength={120}
                    value={q}
                    onChange={event => {
                      setPage(0);
                      setQ(event.target.value);
                    }}
                  />
                  <label className="visually-hidden" htmlFor="fde-state">
                    상태 필터
                  </label>
                  <select
                    id="fde-state"
                    className="form-select"
                    value={stateFilter}
                    onChange={event => {
                      setPage(0);
                      setStateFilter(event.target.value);
                    }}
                  >
                    <option value="">전체 상태</option>
                    {type.states.map(s => (
                      <option key={s}>{s}</option>
                    ))}
                  </select>
                </div>
                <div className="table-responsive">
                  <table className="table">
                    <thead>
                      <tr>
                        <th>ID</th>
                        <th>{type.fields[type.labelField]?.label}</th>
                        <th>상태</th>
                        <th>생성자</th>
                        <th>동작</th>
                      </tr>
                    </thead>
                    <tbody>
                      {result.items.map(item => (
                        <tr key={item.id}>
                          <td>{item.id}</td>
                          <td>{String(item.data[type.labelField] ?? '')}</td>
                          <td>{item.state}</td>
                          <td>{item.owner}</td>
                          <td>
                            <button className="btn btn-sm btn-outline-primary" disabled={busy} onClick={() => void run(() => select(item))}>
                              상세·동작
                            </button>
                          </td>
                        </tr>
                      ))}
                    </tbody>
                  </table>
                </div>
                {!result.items.length && <p>표시할 항목이 없습니다.</p>}
                <div className="d-flex gap-2 align-items-center">
                  <button className="btn btn-sm btn-outline-secondary" disabled={busy || page === 0} onClick={() => setPage(p => p - 1)}>
                    이전
                  </button>
                  <span>{page + 1}페이지</span>
                  <button
                    className="btn btn-sm btn-outline-secondary"
                    disabled={busy || (page + 1) * 25 >= result.total}
                    onClick={() => setPage(p => p + 1)}
                  >
                    다음
                  </button>
                  <button className="btn btn-sm btn-outline-secondary" disabled={busy} onClick={() => void run(refresh)}>
                    새로고침
                  </button>
                </div>
              </div>
              {selected && (
                <section className="card card-body" aria-label="객체 상세">
                  <h2 className="h5">
                    #{selected.id} 상세 · {selected.state} · v{selected.version}
                  </h2>
                  <dl>
                    {Object.entries(selected.data).map(([name, value]) => (
                      <React.Fragment key={name}>
                        <dt>{type.fields[name]?.label || name}</dt>
                        <dd>{String(value)}</dd>
                      </React.Fragment>
                    ))}
                  </dl>
                  {selected.availableActions.length > 0 && (
                    <form onSubmit={execute}>
                      <label htmlFor="fde-action" className="form-label">
                        업무 동작
                      </label>
                      <select
                        id="fde-action"
                        className="form-select mb-2"
                        value={actionId}
                        onChange={event => {
                          setActionId(event.target.value);
                          setParameters({});
                        }}
                      >
                        <option value="">동작 선택</option>
                        {selected.availableActions.map(id => (
                          <option key={id} value={id}>
                            {type.actions[id].label}
                          </option>
                        ))}
                      </select>
                      {actionId && (
                        <Inputs
                          fields={type.actions[actionId].inputs}
                          values={parameters}
                          change={(name, value) => setParameters(old => ({ ...old, [name]: value }))}
                          references={references}
                          prefix="action"
                        />
                      )}
                      <button type="submit" className="btn btn-primary mb-3" disabled={busy || !actionId}>
                        동작 실행
                      </button>
                    </form>
                  )}
                  <h3 className="h6">변경 이력 · 최근 200건</h3>
                  <ol>
                    {history.map(event => (
                      <li key={event.id}>
                        {event.actor} · {event.action} · {event.beforeState || '생성'} → {event.afterState} ·{' '}
                        {new Date(event.occurredAt).toLocaleString()}
                        <details>
                          <summary>변경 전후</summary>
                          <pre className="text-wrap">{JSON.stringify({ before: event.beforeData, after: event.afterData }, null, 2)}</pre>
                        </details>
                      </li>
                    ))}
                  </ol>
                </section>
              )}
            </div>
          </div>
        </>
      )}
    </section>
  );
};
export default Workbench;
