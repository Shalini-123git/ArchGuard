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

  it('only shows package suggestions when they are provided and explains empty guided states', () => {
    const { rerender } = render(<ScanForm onSubmit={vi.fn()} submitting={false} />);
    expect(screen.queryByText('Packages in this project')).toBeNull();
    expect(screen.getByText('No layers yet. Pick a preset above or click Add layer.')).toBeTruthy();
    expect(screen.getByText('Add at least two layers first.')).toBeTruthy();
    rerender(<ScanForm onSubmit={vi.fn()} submitting={false} packageModules={[{ id: 'com.example.web.Main' }]} />);
    expect(screen.getByText('Packages in this project')).toBeTruthy();
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

  it('adds a package suggestion to the focused layer', () => {
    render(<ScanForm onSubmit={vi.fn()} submitting={false} packageModules={[{ id: 'com.termux.shared.util' }]} initialModel={{ layers: [{ name: 'web', packagePatterns: [] }], forbidden: [], noCycles: true }} />);
    fireEvent.focus(screen.getByLabelText('Add package prefix for web'));
    fireEvent.click(screen.getByRole('button', { name: /com\.termux\.shared \(1\)/i }));
    expect(screen.getByText('com.termux.shared')).toBeTruthy();
  });

  it('asks for a layer when a package suggestion is clicked before focusing a layer', () => {
    render(<ScanForm onSubmit={vi.fn()} submitting={false} packageModules={[{ id: 'com.termux.shared.util' }]} initialModel={{ layers: [{ name: 'web', packagePatterns: [] }], forbidden: [], noCycles: true }} />);
    fireEvent.click(screen.getByRole('button', { name: /com\.termux\.shared \(1\)/i }));
    fireEvent.click(screen.getByRole('button', { name: 'web' }));
    expect(screen.getByText('com.termux.shared')).toBeTruthy();
  });
});
