import { useState } from 'react';

export default function ScanForm({ onSubmit, submitting }) {
  const [repoUrl, setRepoUrl] = useState('');
  const [rulesYaml, setRulesYaml] = useState('');
  function submit(event) { event.preventDefault(); onSubmit({ repoUrl, rulesYaml }); }
  return <section className="panel scan-form"><h2>New scan</h2><p>Submit a public HTTPS Git repository. Rules are optional YAML.</p>
    <form onSubmit={submit}><label>Repository URL<input type="url" value={repoUrl} onChange={(event) => setRepoUrl(event.target.value)} placeholder="https://github.com/org/project.git" required /></label>
      <label>Architecture rules <span>(optional)</span><textarea value={rulesYaml} onChange={(event) => setRulesYaml(event.target.value)} placeholder={'layers:\n  - name: web\n    packagePatterns: [com.example.web]'} rows="9" /></label>
      <button disabled={submitting}>{submitting ? 'Queueing scan…' : 'Start scan'}</button></form>
  </section>;
}
