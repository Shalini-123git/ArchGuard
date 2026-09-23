export default function ScanStatus({ scan, loadingResults, onStartOver }) {
  const active = ['QUEUED', 'RUNNING'].includes(scan.status);
  return <section className="panel status-card"><h2>{active ? 'Scanning repository' : `Scan ${scan.status.toLowerCase()}`}</h2>
    <p className={`status ${scan.status.toLowerCase()}`}>{scan.status}</p>
    <p>{active ? 'The dashboard checks for results every second.' : loadingResults ? 'Loading the graph and violations.' : scan.errorMessage || 'The scan did not produce results.'}</p>
    {!active && !loadingResults && <button onClick={onStartOver}>Start another scan</button>}
  </section>;
}
