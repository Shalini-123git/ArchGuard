export default function ViolationList({ violations, selectedId, onSelect }) {
  if (!violations.length) return <section className="panel"><h3>Violations</h3><p>No architecture violations were found.</p></section>;
  return <section className="panel violation-list"><h3>Violations <span>{violations.length}</span></h3>{violations.map((violation) => <button className={selectedId === violation.id ? 'selected' : ''} key={violation.id} onClick={() => onSelect(violation)}>
    <strong>{violation.ruleId}</strong><small>{violation.fromModule || 'Cycle'} → {violation.toModule || 'members'} · {violation.blastRadiusCount} affected</small></button>)}</section>;
}
