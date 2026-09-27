export default function ViolationDetails({ violation }) {
  if (!violation) return <section className="panel details"><h3>Details</h3><p>Select a violation to focus its dependency and explanation.</p></section>;
  return <section className="panel details"><h3>{violation.ruleId}</h3><p><b>From:</b> {violation.fromModule || 'Cycle member'}<br /><b>To:</b> {violation.toModule || 'Cycle member'}</p><p className="explanation">{violation.explanation || 'No explanation was generated.'}</p><small>{violation.explanationFallback ? 'Deterministic fallback explanation' : 'Groq explanation'}</small></section>;
}
