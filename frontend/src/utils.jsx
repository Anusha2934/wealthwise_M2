export function fmt(n) {
  if (n == null) return '—';
  const num = parseFloat(n);
  if (Math.abs(num) >= 1e7) return '₹' + (num / 1e7).toFixed(2) + ' Cr';
  if (Math.abs(num) >= 1e5) return '₹' + (num / 1e5).toFixed(2) + ' L';
  if (Math.abs(num) >= 1e3) return '₹' + (num / 1e3).toFixed(1) + ' K';
  return '₹' + num.toFixed(2);
}

export function pct(n) {
  if (n == null) return '—';
  const num = parseFloat(n);
  return (num >= 0 ? '+' : '') + num.toFixed(2) + '%';
}

export function gainColor(n) {
  if (n == null) return '';
  return parseFloat(n) >= 0 ? 'green' : 'red';
}

export function RiskPips({ level }) {
  return (
    <div className="risk-bar">
      {[1,2,3,4,5,6].map(i => (
        <div key={i} className={`risk-pip${i <= level ? ` filled-${level}` : ''}`} />
      ))}
    </div>
  );
}

export function Badge({ text, type = 'gray' }) {
  return <span className={`badge badge-${type}`}>{text}</span>;
}

export function categoryBadgeType(cat) {
  const m = { EQUITY:'green', DEBT:'blue', HYBRID:'yellow', SOLUTION:'yellow', OTHER:'gray' };
  return m[cat] || 'gray';
}

export const PIE_COLORS = ['#3b82f6','#10b981','#f59e0b','#8b5cf6','#ef4444','#06b6d4','#f97316'];

export function PieChart({ data }) {
  /* data: [{label, value}] */
  const total = data.reduce((s, d) => s + d.value, 0);
  if (total === 0) return <div className="empty">No allocation data</div>;

  let cumAngle = -Math.PI / 2;
  const slices = data.map((d, i) => {
    const frac  = d.value / total;
    const angle = frac * 2 * Math.PI;
    const start = cumAngle;
    cumAngle += angle;
    return { ...d, frac, start, end: cumAngle, color: PIE_COLORS[i % PIE_COLORS.length] };
  });

  const arc = (s) => {
    const r = 70, cx = 90, cy = 90;
    const x1 = cx + r * Math.cos(s.start), y1 = cy + r * Math.sin(s.start);
    const x2 = cx + r * Math.cos(s.end),   y2 = cy + r * Math.sin(s.end);
    const large = s.frac > 0.5 ? 1 : 0;
    return `M${cx},${cy} L${x1},${y1} A${r},${r} 0 ${large},1 ${x2},${y2} Z`;
  };

  return (
    <div className="pie-wrap">
      <svg viewBox="0 0 180 180" style={{ width: 160, height: 160 }}>
        {slices.map((s, i) => (
          <path key={i} d={arc(s)} fill={s.color} stroke="#0a0d14" strokeWidth="2">
            <title>{s.label}: {(s.frac*100).toFixed(1)}%</title>
          </path>
        ))}
        <circle cx="90" cy="90" r="36" fill="#111827" />
      </svg>
      <div className="pie-legend" style={{ maxWidth: 220 }}>
        {slices.map((s, i) => (
          <div key={i} className="legend-item">
            <span className="legend-left">
              <span className="legend-dot" style={{ background: s.color }} />
              {s.label}
            </span>
            <span style={{ fontWeight: 600 }}>{(s.frac*100).toFixed(1)}%</span>
          </div>
        ))}
      </div>
    </div>
  );
}
