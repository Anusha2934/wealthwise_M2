const BASE = '/api/v1';

export async function fetchSchemeSummary() {
  const r = await fetch(`${BASE}/schemes/summary`);
  return r.json();
}

export async function fetchSchemes(page = 0, size = 15, amc = '') {
  const q = amc ? `&amc=${encodeURIComponent(amc)}` : '';
  const r = await fetch(`${BASE}/schemes?page=${page}&size=${size}${q}`);
  return r.json();
}

export async function fetchSchemeCategory(amfiCode) {
  const r = await fetch(`${BASE}/schemes/${amfiCode}/category`);
  if (!r.ok) return null;
  return r.json();
}

export async function fetchPortfolioReport(userId) {
  const r = await fetch(`${BASE}/portfolio/${userId}/report`);
  return r.json();
}

export async function fetchTaxSummary(userId) {
  const r = await fetch(`${BASE}/portfolio/${userId}/tax-summary`);
  return r.json();
}

export async function uploadCas(file, userId) {
  const form = new FormData();
  form.append('file', file);
  form.append('userId', userId);
  const r = await fetch(`${BASE}/portfolio/upload-cas`, { method: 'POST', body: form });
  return r.json();
}

export async function classifyAll() {
  const r = await fetch(`${BASE}/schemes/classify-all`, { method: 'POST' });
  return r.text();
}
