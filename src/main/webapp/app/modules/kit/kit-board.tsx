import React, { useEffect, useState } from 'react';
import axios from 'axios';

type Asset = { id: number; code: string; name: string };
type WorkRequest = {
  id: number;
  assetId: number;
  assetCode: string;
  title: string;
  ownerLogin: string;
  state: 'OPEN' | 'IN_PROGRESS' | 'DONE';
  version: number;
};
type Audit = { id: number; actor: string; action: string; fromState: string | null; toState: string; occurredAt: string };

const KitBoard = () => {
  const [assets, setAssets] = useState<Asset[]>([]);
  const [requests, setRequests] = useState<WorkRequest[]>([]);
  const [title, setTitle] = useState('');
  const [assetId, setAssetId] = useState('');
  const [history, setHistory] = useState<Audit[]>([]);
  const [selected, setSelected] = useState<number | null>(null);
  const [error, setError] = useState('');
  const [busy, setBusy] = useState(false);

  const message = (failure: unknown) =>
    axios.isAxiosError(failure)
      ? String(failure.response?.data?.detail || '요청이 실패했습니다. 로그인 상태와 입력을 확인하세요.')
      : '요청이 실패했습니다.';

  const refresh = async () => {
    const [assetResponse, requestResponse] = await Promise.all([
      axios.get<Asset[]>('/api/kit/assets'),
      axios.get<WorkRequest[]>('/api/kit/requests?page=0&size=50'),
    ]);
    setAssets(assetResponse.data);
    setRequests(requestResponse.data);
  };

  useEffect(() => {
    let live = true;
    refresh().catch(failure => {
      if (live) setError(message(failure));
    });
    return () => {
      live = false;
    };
  }, []);

  const run = async (work: () => Promise<void>) => {
    setBusy(true);
    setError('');
    try {
      await work();
    } catch (failure) {
      setError(message(failure));
    } finally {
      setBusy(false);
    }
  };

  const create = (event: React.FormEvent) => {
    event.preventDefault();
    void run(async () => {
      await axios.post('/api/kit/requests', { assetId: Number(assetId), title });
      setTitle('');
      await refresh();
    });
  };

  const advance = (request: WorkRequest) =>
    run(async () => {
      await axios.post(`/api/kit/requests/${request.id}/actions`, {
        target: request.state === 'OPEN' ? 'IN_PROGRESS' : 'DONE',
        expectedVersion: request.version,
      });
      await refresh();
      if (selected === request.id) setHistory((await axios.get<Audit[]>(`/api/kit/requests/${request.id}/history`)).data);
    });

  const showHistory = (id: number) =>
    run(async () => {
      setSelected(id);
      setHistory((await axios.get<Audit[]>(`/api/kit/requests/${id}/history`)).data);
    });

  return (
    <section aria-labelledby="kit-title" className="py-3">
      <h1 id="kit-title">작업 요청 보드</h1>
      <p>장비에 작업을 등록하고 진행 상황을 확인하세요. 담당자는 본인 요청을, 관리자는 전체 요청을 확인할 수 있습니다.</p>
      {error && (
        <div role="alert" className="alert alert-danger">
          {error}
        </div>
      )}
      <form onSubmit={create} className="card card-body mb-4">
        <h2 className="h5">새 작업 요청</h2>
        <label htmlFor="kit-asset">장비</label>
        <select id="kit-asset" required className="form-select mb-3" value={assetId} onChange={event => setAssetId(event.target.value)}>
          <option value="">장비 선택</option>
          {assets.map(asset => (
            <option key={asset.id} value={asset.id}>
              {asset.code} · {asset.name}
            </option>
          ))}
        </select>
        <label htmlFor="kit-title-input">요청 내용</label>
        <input
          id="kit-title-input"
          required
          maxLength={160}
          className="form-control mb-3"
          value={title}
          onChange={event => setTitle(event.target.value)}
        />
        <button type="submit" className="btn btn-primary align-self-start" disabled={busy || !assetId || !title.trim()}>
          요청 등록
        </button>
      </form>
      <div className="d-flex align-items-center justify-content-between mb-2">
        <h2 className="h4">작업 목록</h2>
        <button className="btn btn-outline-secondary" disabled={busy} onClick={() => void run(refresh)}>
          새로고침
        </button>
      </div>
      <p className="text-muted">최근 50건을 표시합니다.</p>
      {!requests.length && <p>등록된 요청이 없습니다.</p>}
      <div className="table-responsive">
        <table className="table">
          <thead>
            <tr>
              <th>장비</th>
              <th>요청</th>
              <th>담당자</th>
              <th>상태</th>
              <th>동작</th>
            </tr>
          </thead>
          <tbody>
            {requests.map(request => (
              <tr key={request.id}>
                <td>{request.assetCode}</td>
                <td>{request.title}</td>
                <td>{request.ownerLogin}</td>
                <td>{request.state}</td>
                <td>
                  {request.state !== 'DONE' && (
                    <button disabled={busy} className="btn btn-sm btn-primary me-2" onClick={() => void advance(request)}>
                      {request.state === 'OPEN' ? '작업 시작' : '작업 완료'}
                    </button>
                  )}
                  <button disabled={busy} className="btn btn-sm btn-outline-secondary" onClick={() => void showHistory(request.id)}>
                    이력 보기
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {selected !== null && (
        <section aria-label="변경 이력">
          <h2 className="h4">요청 #{selected} 변경 이력</h2>
          <ol>
            {history.map(event => (
              <li key={event.id}>
                {event.actor} · {event.fromState || '생성'} → {event.toState} · {new Date(event.occurredAt).toLocaleString()}
              </li>
            ))}
          </ol>
        </section>
      )}
    </section>
  );
};

export default KitBoard;
