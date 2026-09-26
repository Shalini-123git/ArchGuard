import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { afterEach, describe, expect, it, vi } from 'vitest';
import ScanQueueDashboard from './ScanQueueDashboard.jsx';

afterEach(() => vi.restoreAllMocks());

describe('scan queue dashboard', () => {
  it('shows live queue state, flags old active scans, and opens completed results', async () => {
    const queued = {
      id: 'queued-123456789', repositoryUrl: 'local:C:\\workspace\\sample', status: 'RUNNING',
      createdAt: new Date(Date.now() - 180_000).toISOString(),
      startedAt: new Date(Date.now() - 150_000).toISOString()
    };
    const completed = {
      id: 'completed-123456', repositoryUrl: 'https://github.com/acme/sample.git', status: 'COMPLETED',
      createdAt: new Date().toISOString(), completedAt: new Date().toISOString()
    };
    vi.stubGlobal('fetch', vi.fn()
      .mockResolvedValueOnce(response([queued, completed]))
      .mockResolvedValueOnce(response({ runningScans: 1, queuedScans: 2, maxConcurrentScans: 4 })));
    const onOpenResults = vi.fn();

    render(<ScanQueueDashboard onOpenResults={onOpenResults} pollIntervalMs={60_000} stuckThresholdMs={120_000} />);

    expect(await screen.findByText('Long-running')).toBeTruthy();
    expect(screen.getByText('1 running')).toBeTruthy();
    expect(screen.getByText('2 queued')).toBeTruthy();
    expect(screen.getByText('4 max concurrent')).toBeTruthy();
    expect(screen.getByText('C:\\workspace\\sample')).toBeTruthy();
    fireEvent.click(screen.getByRole('link', { name: 'View results' }));
    await waitFor(() => expect(onOpenResults).toHaveBeenCalledWith(completed));
  });
});

function response(body) {
  return { ok: true, status: 200, json: () => Promise.resolve(body) };
}