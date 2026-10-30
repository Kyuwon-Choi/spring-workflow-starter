import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import axios from 'axios';
import KitBoard from './kit-board';

vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn(), isAxiosError: vi.fn(() => false) } }));

describe('KitBoard user actions', () => {
  const item = { id: 1, assetId: 2, assetCode: 'EQ-02', title: 'Inspect packaging', ownerLogin: 'user', state: 'OPEN', version: 0 };
  beforeEach(() => {
    vi.clearAllMocks();
    vi.mocked(axios.get).mockImplementation(url =>
      Promise.resolve({ data: String(url).endsWith('/assets') ? [{ id: 2, code: 'EQ-02', name: 'Packaging' }] : [item] }),
    );
    vi.mocked(axios.post).mockResolvedValue({ data: item });
  });
  afterEach(cleanup);

  it('submits the chosen asset and title', async () => {
    render(<KitBoard />);
    await screen.findByText('EQ-02 · Packaging');
    fireEvent.change(screen.getByLabelText('장비'), { target: { value: '2' } });
    fireEvent.change(screen.getByLabelText('요청 내용'), { target: { value: 'New inspection' } });
    fireEvent.click(screen.getByRole('button', { name: '요청 등록' }));
    await waitFor(() => expect(axios.post).toHaveBeenCalledWith('/api/kit/requests', { assetId: 2, title: 'New inspection' }));
  });

  it('advances using the rendered version and opens history', async () => {
    render(<KitBoard />);
    fireEvent.click(await screen.findByRole('button', { name: '작업 시작' }));
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith('/api/kit/requests/1/actions', { target: 'IN_PROGRESS', expectedVersion: 0 }),
    );
    await waitFor(() => expect(screen.getByRole('button', { name: '이력 보기' }).hasAttribute('disabled')).toBe(false));
    fireEvent.click(screen.getByRole('button', { name: '이력 보기' }));
    await screen.findByRole('heading', { name: '요청 #1 변경 이력' });
    expect(axios.get).toHaveBeenCalledWith('/api/kit/requests/1/history');
  });
});
