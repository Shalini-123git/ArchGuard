import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import ScanForm from './ScanForm.jsx';

describe('ScanForm', () => {
  it('disables submit when the guided model is invalid', () => {
    render(<ScanForm onSubmit={vi.fn()} submitting={false} />);
    fireEvent.change(screen.getByLabelText('Repository URL'), { target: { value: 'https://github.com/acme/app.git' } });
    fireEvent.click(screen.getByRole('button', { name: 'Add layer' }));
    expect(screen.getByRole('button', { name: 'Start scan' }).disabled).toBe(true);
  });

  it('submits raw YAML from the advanced tab', () => {
    const onSubmit = vi.fn();
    render(<ScanForm onSubmit={onSubmit} submitting={false} />);
    fireEvent.change(screen.getByLabelText('Repository URL'), { target: { value: 'https://github.com/acme/app.git' } });
    fireEvent.click(screen.getByRole('tab', { name: 'YAML (advanced)' }));
    fireEvent.change(screen.getByLabelText(/Architecture rules/), { target: { value: 'noCycles: true' } });
    fireEvent.click(screen.getByRole('button', { name: 'Start scan' }));
    expect(onSubmit).toHaveBeenCalledWith({ repoUrl: 'https://github.com/acme/app.git', rulesYaml: 'noCycles: true' });
  });
});
