import { useState } from 'react';

export default function ScanStatus({ scan, loadingResults, onStartOver, onCancel }) {
  const active = ['QUEUED', 'RUNNING'].includes(scan.status);
  const [confirming, setConfirming] = useState(false);
  async function cancel() {
    if (!confirming) { setConfirming(true); return; }
    await onCancel();
  }
  return <section className="panel status-card"><h2>{active ? 'Scanning repository' : `Scan ${scan.status.toLowerCase()}`}</h2>
    <p className={`status ${scan.status.toLowerCase()}`}>{scan.status}</p>
    <p>{active ? 'The dashboard checks for results every second.' : loadingResults ? 'Loading the graph and violations.' : scan.errorMessage || 'The scan did not produce results.'}</p>
    {active && <button className="secondary" onClick={cancel}>{confirming ? 'Cancel this scan? Partial results are discarded.' : 'Cancel'}</button>}
    {!active && !loadingResults && <button onClick={onStartOver}>Start another scan</button>}
  </section>;
}
