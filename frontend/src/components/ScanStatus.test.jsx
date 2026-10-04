import { fireEvent, render, screen, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ScanStatus from './ScanStatus.jsx';

describe('ScanStatus cancellation', () => {
  it('confirms and cancels an active scan, then renders cancelled state', async () => {
    const onCancel = vi.fn().mockImplementation(async () => {});
    const { rerender } = render(<ScanStatus scan={{ id: 'scan-1', status: 'RUNNING' }} loadingResults={false} onStartOver={vi.fn()} onCancel={onCancel} />);
    fireEvent.click(screen.getByRole('button', { name: 'Cancel' }));
    expect(screen.getByText('Cancel this scan? Partial results are discarded.')).toBeTruthy();
    fireEvent.click(screen.getByRole('button', { name: 'Cancel this scan? Partial results are discarded.' }));
    await waitFor(() => expect(onCancel).toHaveBeenCalled());
    rerender(<ScanStatus scan={{ id: 'scan-1', status: 'CANCELLED', errorMessage: 'Cancelled by user' }} loadingResults={false} onStartOver={vi.fn()} onCancel={onCancel} />);
    expect(screen.getByText('CANCELLED')).toBeTruthy();
    expect(screen.queryByRole('button', { name: 'Cancel' })).toBeNull();
  });
});
