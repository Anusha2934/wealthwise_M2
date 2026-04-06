import { useState } from 'react';
import { uploadCas } from '../api';

export default function Upload({ userId }) {
  const [file, setFile]       = useState(null);
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [result, setResult]   = useState(null);
  const [error, setError]     = useState('');

  function onFile(f) {
    if (f && f.type === 'application/pdf') {
      setFile(f); setError(''); setResult(null);
    } else {
      setError('Please select a PDF file.');
    }
  }

  async function submit() {
    if (!file) return setError('Please choose a CAS PDF first.');
    setUploading(true); setError(''); setResult(null);
    try {
      const data = await uploadCas(file, userId);
      if (data.status === 'SUCCESS') setResult(data);
      else setError(data.message ?? 'Upload failed. Check backend logs.');
    } catch (e) {
      setError('Network error — is the backend running on port 8080?');
    }
    setUploading(false);
  }

  return (
    <div>
      <p className="page-title">Upload CAS PDF</p>
      <p className="page-sub">Import your CAMS / KFintech Consolidated Account Statement</p>

      <div style={{ maxWidth: 560 }}>
        <div
          className={`upload-zone${dragging ? ' dragging' : ''}`}
          onDragOver={e => { e.preventDefault(); setDragging(true); }}
          onDragLeave={() => setDragging(false)}
          onDrop={e => { e.preventDefault(); setDragging(false); onFile(e.dataTransfer.files[0]); }}
        >
          <input type="file" accept=".pdf" onChange={e => onFile(e.target.files[0])} />
          <div className="upload-icon">📄</div>
          {file ? (
            <>
              <div className="upload-title" style={{ color: 'var(--green)' }}>{file.name}</div>
              <div className="upload-sub">{(file.size / 1024).toFixed(1)} KB — click or drag to replace</div>
            </>
          ) : (
            <>
              <div className="upload-title">Drop your CAS PDF here</div>
              <div className="upload-sub">or click to browse · max 50 MB</div>
            </>
          )}
        </div>

        <div style={{ marginTop: 14, display: 'flex', gap: 10, alignItems: 'center' }}>
          <label style={{ fontSize: 13, color: 'var(--muted)' }}>User ID</label>
          <span style={{
            padding: '8px 14px',
            background: 'var(--surface2)',
            border: '1px solid var(--border)',
            borderRadius: 8,
            fontSize: 13,
            fontWeight: 600,
            color: 'var(--accent)',
          }}>{userId}</span>
          <button className="btn btn-primary" style={{ marginLeft: 'auto' }}
            onClick={submit} disabled={uploading || !file}>
            {uploading ? 'Uploading…' : '🚀 Parse & Import'}
          </button>
        </div>

        {error && (
          <div className="alert alert-error" style={{ marginTop: 16 }}>❌ {error}</div>
        )}

        {result && (
          <div style={{ marginTop: 20 }}>
            <div className="alert alert-success">✅ Import successful!</div>
            <div className="card">
              <div className="stat-grid" style={{ gridTemplateColumns: '1fr 1fr 1fr' }}>
                <div className="stat-card">
                  <div className="stat-label">Folios</div>
                  <div className="stat-value blue">{result.totalFolios}</div>
                </div>
                <div className="stat-card">
                  <div className="stat-label">Transactions</div>
                  <div className="stat-value blue">{result.totalTransactions}</div>
                </div>
                <div className="stat-card">
                  <div className="stat-label">Upload ID</div>
                  <div className="stat-value" style={{ fontSize: 18 }}>#{result.uploadId}</div>
                </div>
              </div>
              <p style={{ marginTop: 12, fontSize: 13, color: 'var(--muted)' }}>
                Go to the Portfolio tab to see your holdings and tax summary.
              </p>
            </div>
          </div>
        )}

        <div className="card" style={{ marginTop: 20 }}>
          <div className="section-title" style={{ marginBottom: 10 }}>📋 How to get your CAS PDF</div>
          <ol style={{ paddingLeft: 18, fontSize: 13, color: 'var(--muted)', lineHeight: 2 }}>
            <li>Go to <strong style={{ color: 'var(--text)' }}>mycams.com</strong> → Get CAS</li>
            <li>Or <strong style={{ color: 'var(--text)' }}>kfintech.com</strong> → Account Statement</li>
            <li>Choose "Detailed" statement with transactions</li>
            <li>Download the PDF and upload it here</li>
          </ol>
        </div>
      </div>
    </div>
  );
}
