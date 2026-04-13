import { useEffect, useRef, useState } from 'react';
import { Coffee, DollarSign, Heart, Users } from 'lucide-react';
import type { StatsDto } from '../api/types';

interface Props { stats: StatsDto; }

type Tile = {
  key: keyof StatsDto;
  label: string;
  color: string;
  Icon: React.ComponentType<{ size?: number; className?: string }>;
  format: (v: number) => string;
};

const tiles: Tile[] = [
  { key: 'cash',      label: 'Cash',      color: 'text-accent-teal',  Icon: DollarSign, format: (v) => `$${v.toLocaleString()}` },
  { key: 'morale',    label: 'Morale',    color: 'text-accent-pink',  Icon: Heart,      format: (v) => String(v) },
  { key: 'customers', label: 'Customers', color: 'text-accent-blue',  Icon: Users,      format: (v) => String(v) },
  { key: 'coffee',    label: 'Coffee',    color: 'text-accent-amber', Icon: Coffee,     format: (v) => String(v) },
];

export default function StatsPanel({ stats }: Props) {
  return (
    <div className="grid grid-cols-2 gap-3">
      {tiles.map((t) => (
        <StatTile key={t.key} tile={t} value={stats[t.key]} />
      ))}
    </div>
  );
}

// Per-tile component tracks its own previous value so a stat change triggers
// a one-shot pulse + digit flash without coupling the parent to animation state.
function StatTile({ tile, value }: { tile: Tile; value: number }) {
  const { label, color, Icon, format } = tile;
  const prev = useRef(value);
  const [pulseKey, setPulseKey] = useState(0);

  useEffect(() => {
    if (prev.current !== value) {
      prev.current = value;
      setPulseKey((k) => k + 1);
    }
  }, [value]);

  return (
    <div
      key={`tile-${pulseKey}`}
      className="bg-white/[0.07] rounded-lg px-4 py-3 border border-white/10 animate-pulse-flash"
    >
      <div className="text-[10px] uppercase tracking-[0.18em] text-white/40 font-medium">{label}</div>
      <div
        key={`val-${pulseKey}`}
        className={`text-2xl font-bold tabular-nums ${color} inline-flex items-center gap-1.5 animate-fade-in`}
      >
        <Icon size={20} />
        {format(value)}
      </div>
    </div>
  );
}
