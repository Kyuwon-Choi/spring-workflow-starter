import React, { useState } from 'react';
import axios from 'axios';
import type { WorkflowToolsProps } from '../workflow/Workbench';

type Preview = { valid: boolean; rows: Record<string, unknown>[]; errors: { row: number; detail: string }[] };

export default function CsvTools({ base, fields, canCreate, q, stateFilter, refresh }: WorkflowToolsProps) {
  const [csv, setCsv] = useState('');
  const [preview, setPreview] = useState<Preview | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState('');
  const run = async (work: () => Promise<void>) => {
    setBusy(true); setError('');
    try { await work(); } catch (failure) {
      setError(axios.isAxiosError(failure) ? String(failure.response?.data?.detail || 'CSV 처리에 실패했습니다.') : 'CSV 처리에 실패했습니다.');
    } finally { setBusy(false); }
  };
  return <section className="card card-body" aria-label="CSV 입출력">
    <h2 className="h5">CSV 입출력</h2>
    {error && <p role="alert" className="text-danger">{error}</p>}
    <button disabled={busy} className="btn btn-outline-secondary mb-2" onClick={() => void run(async () => {
      const response = await axios.get<string>(`${base}/export`, { params: { q, state: stateFilter }, responseType: 'text' });
      const url = URL.createObjectURL(new Blob([response.data], { type: 'text/csv;charset=utf-8' }));
      const link = document.createElement('a'); link.href = url; link.download = 'objects.csv'; link.click();
      URL.revokeObjectURL(url);
    })}>현재 조회 결과 내보내기</button>
    {canCreate && <>
      <p className="small text-muted">열 이름: {Object.entries(fields).filter(([, field]) => !field.readOnly).map(([name]) => name).join(', ')}. 최대 100행.</p>
      <label htmlFor="csv-input">CSV 내용</label>
      <textarea id="csv-input" className="form-control mb-2" rows={5} maxLength={100000} value={csv}
        onChange={event => { setCsv(event.target.value); setPreview(null); }}/>
      <button disabled={busy || !csv.trim()} className="btn btn-outline-primary mb-2"
        onClick={() => void run(async () => setPreview((await axios.post<Preview>(`${base}/import/preview`, { csv })).data))}>입력 미리보기</button>
      {preview && <div role="status">
        <p>{preview.valid ? '검증 통과' : '오류 수정 필요'} · 유효 {preview.rows.length}행</p>
        <ul>{preview.errors.map((row, index) => <li key={index}>{row.row}행: {row.detail}</li>)}</ul>
        <button disabled={busy || !preview.valid} className="btn btn-primary" onClick={() => void run(async () => {
          await axios.post(`${base}/import`, { rows: preview.rows }); setPreview(null); setCsv(''); await refresh();
        })}>검증된 행 저장</button>
      </div>}
    </>}
  </section>;
}
