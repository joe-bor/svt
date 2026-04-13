import {
  Briefcase,
  Coffee,
  DollarSign,
  Footprints,
  Heart,
  Megaphone,
  Moon,
  Settings,
  ShoppingCart,
  SkipForward,
  Sparkles,
  Thermometer,
  TrendingUp,
  Users,
  Zap,
} from 'lucide-react';
import type { ActionType, StatDeltas, TurnResolutionSummaryDto } from '../api/types';

type StatKey = 'cash' | 'customers' | 'morale' | 'coffee';

const STAT_META: Record<StatKey, { color: string; Icon: React.ComponentType<{ size?: number; className?: string }> }> = {
  cash:      { color: 'text-accent-teal',  Icon: DollarSign },
  customers: { color: 'text-accent-blue',  Icon: Users },
  morale:    { color: 'text-accent-pink',  Icon: Heart },
  coffee:    { color: 'text-accent-amber', Icon: Coffee },
};

const ACTION_META: Record<ActionType, { label: string; Icon: React.ComponentType<{ size?: number; className?: string }> }> = {
  TRAVEL:          { label: 'Travel',          Icon: Footprints },
  REST:            { label: 'Rest',            Icon: Moon },
  WORK_ON_PRODUCT: { label: 'Work on Product', Icon: Settings },
  MARKETING:       { label: 'Marketing',       Icon: Megaphone },
  PITCH_VCS:       { label: 'Pitch VCs',       Icon: Briefcase },
  BUY_SUPPLIES:    { label: 'Buy Supplies',    Icon: ShoppingCart },
  INVEST_CRYPTO:   { label: 'Invest Crypto',   Icon: TrendingUp },
  SKIP:            { label: 'Skip',            Icon: SkipForward },
};

interface Chip {
  kind: StatKey;
  value: number;
}

interface Row {
  label: string;
  Icon?: React.ComponentType<{ size?: number; className?: string }>;
  iconColor?: string;
  chips: Chip[];
  note?: string;
}

function chipsFromDeltas(d: StatDeltas): Chip[] {
  const out: Chip[] = [];
  if (d.cash !== 0)      out.push({ kind: 'cash',      value: d.cash });
  if (d.customers !== 0) out.push({ kind: 'customers', value: d.customers });
  if (d.morale !== 0)    out.push({ kind: 'morale',    value: d.morale });
  if (d.coffee !== 0)    out.push({ kind: 'coffee',    value: d.coffee });
  return out;
}

function buildRows(summary: TurnResolutionSummaryDto): Row[] {
  const rows: Row[] = [];

  summary.eventResolutions.forEach((e, i) => {
    rows.push({
      label: e.dynamicNote?.trim() || `Event #${i + 1}`,
      Icon: Zap,
      iconColor: 'text-accent-red/70',
      chips: chipsFromDeltas(e.statDeltas),
    });
  });

  const p = summary.passiveDeltas;
  if (p.cashFromEconomy !== 0) {
    rows.push({
      label: 'Economy',
      Icon: TrendingUp,
      iconColor: 'text-white/40',
      chips: [{ kind: 'cash', value: p.cashFromEconomy }],
    });
  }
  if (p.coffeeDecay !== 0) {
    rows.push({
      label: 'Coffee decay',
      Icon: Coffee,
      iconColor: 'text-white/40',
      chips: [{ kind: 'coffee', value: p.coffeeDecay }],
    });
  }
  const tm = p.temperatureModifier;
  if (tm.morale !== 0 || tm.coffee !== 0) {
    const chips: Chip[] = [];
    if (tm.morale !== 0) chips.push({ kind: 'morale', value: tm.morale });
    if (tm.coffee !== 0) chips.push({ kind: 'coffee', value: tm.coffee });
    rows.push({
      label: 'Temperature',
      Icon: Thermometer,
      iconColor: 'text-white/40',
      chips,
    });
  }
  if (p.cryptoSettlementCredited != null && p.cryptoSettlementCredited !== 0) {
    rows.push({
      label: 'Crypto settled',
      Icon: Sparkles,
      iconColor: 'text-accent-teal/60',
      chips: [{ kind: 'cash', value: p.cryptoSettlementCredited }],
    });
  }

  const ar = summary.actionResolution;
  const meta = ACTION_META[ar.actionType];
  const actionChips = chipsFromDeltas({
    cash: ar.cashDelta,
    customers: ar.customersDelta,
    morale: ar.moraleDelta,
    coffee: ar.coffeeDelta,
  });
  const actionNoteParts: string[] = [];
  if (ar.weatherSurcharges.coffee > 0) actionNoteParts.push(`weather −${ar.weatherSurcharges.coffee} coffee`);
  if (ar.detourBonusApplied) actionNoteParts.push(`detour bonus`);
  rows.push({
    label: meta?.label ?? ar.actionType,
    Icon: meta?.Icon,
    iconColor: 'text-white/50',
    chips: actionChips,
    note: actionNoteParts.length > 0 ? actionNoteParts.join(' · ') : undefined,
  });

  return rows;
}

function formatChip(c: Chip): string {
  const sign = c.value > 0 ? '+' : '−';
  const abs = Math.abs(c.value);
  return c.kind === 'cash' ? `${sign}$${abs}` : `${sign}${abs}`;
}

export default function TurnRecapCard({ summary }: { summary: TurnResolutionSummaryDto }) {
  const rows = buildRows(summary);
  if (rows.length === 0) return null;

  return (
    <div className="flex-1 min-h-0 bg-white/[0.04] border border-white/10 rounded-lg p-3 flex flex-col gap-1.5 animate-fade-in">
      <div className="text-[10px] uppercase tracking-[0.22em] text-white/40 font-medium">
        Last turn
      </div>
      <ul className="flex flex-col min-h-0">
        {rows.map((r, i) => {
          const RowIcon = r.Icon;
          return (
            <li
              key={i}
              className="flex items-center justify-between gap-3 py-1 border-b border-white/[0.04] last:border-b-0 animate-fade-in-up"
              style={{ animationDelay: `${i * 45}ms` }}
            >
              <span className="flex items-center gap-1.5 min-w-0">
                {RowIcon && <RowIcon size={11} className={`${r.iconColor ?? 'text-white/40'} shrink-0`} />}
                <span className="text-[11px] text-white/70 truncate">{r.label}</span>
                {r.note && (
                  <span className="text-[10px] text-white/35 italic truncate">· {r.note}</span>
                )}
              </span>
              <span className="flex items-center gap-2 shrink-0 tabular-nums">
                {r.chips.length === 0 ? (
                  <span className="text-[10px] text-white/30">—</span>
                ) : (
                  r.chips.map((c, j) => {
                    const { color, Icon } = STAT_META[c.kind];
                    return (
                      <span key={j} className={`inline-flex items-center gap-0.5 text-[11px] font-semibold ${color}`}>
                        <Icon size={10} />
                        {formatChip(c)}
                      </span>
                    );
                  })
                )}
              </span>
            </li>
          );
        })}
      </ul>
    </div>
  );
}
