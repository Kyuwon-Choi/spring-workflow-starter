import React from 'react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { cleanup, fireEvent, render, screen, waitFor } from '@testing-library/react';
import axios from 'axios';
import Workbench, { type Domain, typedValues } from './workbench';

vi.mock('axios', () => ({ default: { get: vi.fn(), post: vi.fn(), isAxiosError: vi.fn(() => false) } }));
vi.mock('app/config/store', () => ({
  useAppSelector: selector => selector({ authentication: { account: { authorities: ['ROLE_USER'] } } }),
}));
const domains: Domain[] = [
  {
    id: 'alpha',
    label: 'Alpha domain',
    types: {
      record: {
        label: 'Record',
        labelField: 'name',
        createRoles: ['ROLE_USER'],
        states: ['NEW', 'DONE'],
        fields: {
          name: { label: 'Display name', type: 'string', required: true },
          count: { label: 'Count', type: 'integer', required: true },
        },
        actions: { finish: { label: 'Finish', inputs: { note: { label: 'Completion note', type: 'string', required: true } } } },
      },
    },
  },
  {
    id: 'beta',
    label: 'Beta domain',
    types: {
      product: {
        label: 'Product',
        labelField: 'sku',
        createRoles: ['ROLE_USER'],
        states: ['AVAILABLE'],
        fields: { sku: { label: 'SKU', type: 'string', required: true } },
        actions: {},
      },
    },
  },
];
const object = { id: 12, data: { name: 'Existing', count: 3 }, state: 'NEW', owner: 'user', version: 7, availableActions: ['finish'] };
let activeDomains: Domain[];
describe('Metadata-driven FDE workbench', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    activeDomains = structuredClone(domains);
    vi.mocked(axios.get).mockImplementation(url => {
      let data: unknown;
      if (String(url).endsWith('/catalog')) data = activeDomains;
      else if (String(url).endsWith('/summary')) data = { NEW: 1 };
      else if (String(url).endsWith('/history')) data = [];
      else if (String(url).endsWith('/objects/12')) data = object;
      else
        data = {
          items: String(url).includes('/alpha/') ? [object] : [],
          total: String(url).includes('/alpha/') ? 1 : 0,
          page: 0,
          size: 25,
        };
      return Promise.resolve({ data });
    });
    vi.mocked(axios.post).mockResolvedValue({ data: object });
  });
  afterEach(cleanup);
  it('omits server-managed fields from creation and forged typed form values', async () => {
    activeDomains[0].types.record.fields.grant = { label: 'Server grant', type: 'integer', required: false, readOnly: true };
    render(<Workbench />);
    await screen.findByLabelText('Display name *');
    expect(screen.queryByLabelText('Server grant')).toBeNull();
    expect(typedValues(activeDomains[0].types.record.fields, { name: 'Safe', count: '1', grant: '999' })).toEqual({
      name: 'Safe',
      count: 1,
    });
    expect(screen.getByText(/열 이름:/).textContent).not.toContain('grant');
  });
  it('renders arbitrary schema and posts typed input to the chosen domain', async () => {
    render(<Workbench />);
    fireEvent.change(await screen.findByLabelText('Display name *'), { target: { value: 'New record' } });
    fireEvent.change(screen.getByLabelText('Count *'), { target: { value: '4' } });
    fireEvent.click(screen.getByRole('button', { name: '등록', exact: true }));
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith('/api/fde/alpha/record/objects', { data: { name: 'New record', count: 4 } }),
    );
    await waitFor(() => expect(screen.getByLabelText('업무').hasAttribute('disabled')).toBe(false));
    fireEvent.change(screen.getByLabelText('업무'), { target: { value: 'beta' } });
    await screen.findByLabelText('SKU *');
    expect(screen.queryByLabelText('Display name *')).toBeNull();
  });
  it('executes an action with the displayed version and configured parameters', async () => {
    render(<Workbench />);
    fireEvent.click(await screen.findByRole('button', { name: '상세·동작' }));
    fireEvent.change(await screen.findByLabelText('업무 동작'), { target: { value: 'finish' } });
    fireEvent.change(screen.getByLabelText('Completion note *'), { target: { value: 'Checked' } });
    fireEvent.click(screen.getByRole('button', { name: '동작 실행' }));
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith('/api/fde/alpha/record/objects/12/actions/finish', {
        expectedVersion: 7,
        data: { note: 'Checked' },
      }),
    );
  });
  it('loads action reference choices by target type, even when the parameter has a different name', async () => {
    activeDomains[0].types.record.actions.finish.inputs.related = {
      label: 'Related record',
      type: 'reference',
      required: true,
      reference: 'record',
    };
    render(<Workbench />);
    fireEvent.click(await screen.findByRole('button', { name: '상세·동작' }));
    fireEvent.change(await screen.findByLabelText('업무 동작'), { target: { value: 'finish' } });
    await waitFor(() => expect(screen.getByLabelText('Related record *').querySelector('option[value="12"]')).not.toBeNull());
    fireEvent.change(screen.getByLabelText('Related record *'), { target: { value: '12' } });
    fireEvent.change(screen.getByLabelText('Completion note *'), { target: { value: 'Linked' } });
    fireEvent.click(screen.getByRole('button', { name: '동작 실행' }));
    await waitFor(() =>
      expect(axios.post).toHaveBeenCalledWith('/api/fde/alpha/record/objects/12/actions/finish', {
        expectedVersion: 7,
        data: { note: 'Linked', related: 12 },
      }),
    );
  });
  it('does not permit saving a CSV preview containing errors', async () => {
    vi.mocked(axios.post).mockResolvedValue({ data: { valid: false, rows: [], errors: [{ row: 2, detail: 'Invalid number' }] } });
    render(<Workbench />);
    fireEvent.change(await screen.findByLabelText('CSV 내용'), { target: { value: 'name,count\nA,bad' } });
    fireEvent.click(screen.getByRole('button', { name: '입력 미리보기' }));
    await screen.findByText('2행: Invalid number');
    expect(screen.getByRole('button', { name: '검증된 행 저장' }).hasAttribute('disabled')).toBe(true);
    fireEvent.change(screen.getByLabelText('CSV 내용'), { target: { value: 'name,count\nA,3' } });
    expect(screen.queryByText('2행: Invalid number')).toBeNull();
  });
  it('keeps booleans and numeric references typed and excludes unrelated form values', () => {
    expect(
      typedValues(
        {
          enabled: { label: 'Enabled', type: 'boolean', required: true },
          relation: { label: 'Related', type: 'reference', required: true },
        },
        { enabled: 'false', relation: '42', owner: 'admin' },
      ),
    ).toEqual({ enabled: false, relation: 42 });
  });
});
