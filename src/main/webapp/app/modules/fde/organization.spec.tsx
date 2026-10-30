import React from 'react';
import { beforeEach, afterEach, describe, it, expect, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import { MemoryRouter } from 'react-router';
import axios from 'axios';
import Organization from './organization';

vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn(), isAxiosError: vi.fn(() => false) } }));
vi.mock('app/config/store', () => ({
  useAppSelector: selector => selector({ authentication: { account: { authorities: ['ROLE_USER'] } } }),
}));
const base = {
  member: { login: 'employee', tenant: 'T1', department: 'D1', roles: 'STAFF', projects: 'P1', active: true, version: 0 },
  members: [],
  projects: [{ code: 'P1', department: 'D1' }],
  rules: [{ key: 'V1', effectiveAt: '2026-10-28T00:00:00Z', financeAbove: null }],
  delegations: [],
  audit: [],
};
describe('Organization policy controls', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(axios.get).mockResolvedValue({ data: structuredClone(base) });
    vi.mocked(axios.post).mockResolvedValue({ data: {} });
  });
  afterEach(cleanup);
  it('shows staff their scope but hides administrative policy and delegation controls', async () => {
    render(
      <MemoryRouter>
        <Organization />
      </MemoryRouter>,
    );
    await screen.findByText(/employee · 회사 T1/);
    expect(screen.queryByRole('button', { name: '새 규칙 게시' })).toBeNull();
    expect(screen.queryByRole('button', { name: '승인 권한 위임' })).toBeNull();
    expect(screen.getAllByText('조회 제한')).toHaveLength(2);
  });
  it('publishes a new numeric threshold without changing an existing rule', async () => {
    const data = structuredClone(base);
    data.member.roles = 'ORG_ADMIN';
    vi.mocked(axios.get).mockResolvedValue({ data });
    render(
      <MemoryRouter>
        <Organization />
      </MemoryRouter>,
    );
    fireEvent.change(await screen.findByLabelText('규칙 이름'), { target: { value: 'V2' } });
    fireEvent.change(screen.getByLabelText('재무 추가 승인 기준 금액 (초과)'), { target: { value: '300000' } });
    fireEvent.click(screen.getByRole('button', { name: '새 규칙 게시' }));
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith('/api/fde-enterprise/rules', { key: 'V2', financeAbove: 300000, effectiveAt: null }),
    );
    expect(data.rules[0].financeAbove).toBeNull();
  });
});
