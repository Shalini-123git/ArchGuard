const API_ROOT = '/api';

async function request(path, options) {
  const response = await fetch(`${API_ROOT}${path}`, options);
  const body = await response.json().catch(() => null);
  if (!response.ok) throw new Error(body?.message || `Request failed (${response.status})`);
  return body;
}

export function createScan({ repoUrl, rulesYaml }) {
  return request('/scans', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ repoUrl, rulesYaml: rulesYaml || null })
  });
}

export function getScan(scanId) { return request(`/scans/${scanId}`); }
export function getScans({ status, page = 0, size = 50 } = {}) {
  const query = new URLSearchParams({ page: String(page), size: String(size) });
  if (status) query.set('status', status);
  return request(`/scans?${query}`);
}
export function getQueueStatus() { return request('/scans/queue-status'); }
export function getGraph(scanId) { return request(`/scans/${scanId}/graph`); }
export function getViolations(scanId) { return request(`/scans/${scanId}/violations`); }
export function getRepositoryHistory(repositoryId) { return request(`/repos/${repositoryId}/history`); }
