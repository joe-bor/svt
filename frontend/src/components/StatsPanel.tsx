import type { StatsDto } from '../api/types';

interface Props { stats: StatsDto; }

const tiles = [
  { key: 'cash',      label: 'Cash',      color: 'text-accent-teal',  format: (v: number) => `$${v.toLocaleString()}` },
  { key: 'morale',    label: 'Morale',    color: 'text-accent-pink',  format: (v: number) => String(v) },
  { key: 'customers', label: 'Customers', color: 'text-accent-blue',  format: (v: number) => String(v) },
  { key: 'coffee',    label: 'Coffee',    color: 'text-accent-amber', format: (v: number) => `☕ ${v}` },
] as const;

export default function StatsPanel({ stats }: Props) {
  return (
    <div className="grid grid-cols-2 gap-2">
      {tiles.map((t) => (
        <div key={t.key} className="bg-white/[0.07] rounded-lg px-3 py-2 border border-white/10">
          <div className="text-[9px] uppercase tracking-widest text-white/40">{t.label}</div>
          <div className={`text-xl font-extrabold ${t.color}`}>{t.format(stats[t.key])}</div>
        </div>
      ))}
    </div>
  );
}
