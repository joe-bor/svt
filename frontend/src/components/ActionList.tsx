import { useState } from 'react';
import {
  ArrowLeft,
  Briefcase,
  Footprints,
  Megaphone,
  Moon,
  Settings,
  ShoppingCart,
  SkipForward,
  TrendingUp,
} from 'lucide-react';
import type { AvailableActionDto, AvailableNextLocationDto, ActionType } from '../api/types';

interface Props {
  actions: AvailableActionDto[];
  nextLocations: AvailableNextLocationDto[];
  onSubmit: (action: { type: ActionType; destinationLocationId?: number; amount?: number }) => void;
}

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

// Format "-$300, -3 coffee, -1 coffee (weather)" from the numeric cost fields.
// SKIP has no costs; INVEST_CRYPTO's principal is min/max, not a fixed cost.
function formatCost(a: AvailableActionDto): string {
  if (a.type === 'SKIP') return 'forced';
  if (a.type === 'INVEST_CRYPTO') return `$${a.minAmount ?? 500}+ principal`;

  const parts: string[] = [];
  const totalCash = a.cashCost + a.weatherSurcharge.cashAdded;
  const totalCoffee = a.coffeeCost + a.weatherSurcharge.coffeeAdded;
  const totalMorale = a.moraleCost + a.weatherSurcharge.moraleAdded;
  if (totalCash > 0) parts.push(`-$${totalCash}`);
  if (totalCoffee > 0) parts.push(`-${totalCoffee} coffee`);
  if (totalMorale > 0) parts.push(`-${totalMorale} morale`);
  return parts.length > 0 ? parts.join(', ') : 'free';
}

export default function ActionList({ actions, nextLocations, onSubmit }: Props) {
  const [expanded, setExpanded] = useState<ActionType | null>(null);

  // SKIP-only UI: when a LOSE_ACTION event (Burnout Wave) is pending, the
  // server filters availableActions down to just SKIP. Show a label so the
  // player understands why they have no real choice.
  const onlySkip = actions.length === 1 && actions[0].type === 'SKIP';

  if (onlySkip) {
    return (
      <div className="flex-1 min-h-0 bg-white/[0.04] border border-white/10 rounded-lg p-3 flex flex-col items-center justify-center gap-3 animate-fade-in">
        <div className="text-xs text-accent-red text-center">
          Forced rest — Burnout Wave
        </div>
        <button
          onClick={() => onSubmit({ type: 'SKIP' })}
          className="px-5 py-2 rounded-md text-sm bg-white/[0.08] border border-white/15 hover:bg-white/[0.14] transition duration-200 ease-out-quart active:scale-[0.97]"
        >
          Skip turn
        </button>
      </div>
    );
  }

  // INVEST_CRYPTO amount state — initialized to the server's minAmount (500).
  const investMin = actions.find((a) => a.type === 'INVEST_CRYPTO')?.minAmount ?? 500;
  const investMax = actions.find((a) => a.type === 'INVEST_CRYPTO')?.maxAmount ?? 500;

  if (expanded === 'TRAVEL') {
    return (
      <div key="travel-view" className="flex-1 min-h-0 bg-white/[0.04] border border-white/10 rounded-lg p-3 flex flex-col gap-2 animate-fade-in">
        <button
          onClick={() => setExpanded(null)}
          className="self-start inline-flex items-center gap-1 text-[10px] uppercase tracking-widest text-white/50 hover:text-white/80 transition-colors duration-150"
        >
          <ArrowLeft size={11} /> Back
        </button>
        <div className="text-[10px] uppercase tracking-[0.22em] text-white/40 font-medium">Pick destination</div>
        <div className="flex flex-col gap-1.5 min-h-0 overflow-auto">
          {nextLocations.map((n, i) => (
            <button
              key={n.locationId}
              onClick={() => onSubmit({ type: 'TRAVEL', destinationLocationId: n.locationId })}
              className="text-left text-xs px-2 py-1.5 rounded bg-accent-teal/15 border border-accent-teal/30 text-accent-teal hover:bg-accent-teal/25 transition duration-200 ease-out-quart active:scale-[0.98] animate-fade-in-up"
              style={{ animationDelay: `${80 + i * 60}ms` }}
            >
              {n.name}
              {n.detour && <span className="text-white/40"> · detour</span>}
              {!n.detour && <span className="text-white/40"> · {n.eta} hops to SF</span>}
              {n.detourBonusStat && (
                <span className="text-accent-pink"> · {n.detourBonusValue! > 0 ? '+' : ''}{n.detourBonusValue} {n.detourBonusStat.toLowerCase()}</span>
              )}
            </button>
          ))}
        </div>
      </div>
    );
  }

  if (expanded === 'INVEST_CRYPTO') {
    return (
      <div key="invest-view" className="flex-1 min-h-0 bg-white/[0.04] border border-white/10 rounded-lg p-3 flex flex-col gap-2 animate-fade-in">
        <button
          onClick={() => setExpanded(null)}
          className="self-start inline-flex items-center gap-1 text-[10px] uppercase tracking-widest text-white/50 hover:text-white/80 transition-colors duration-150"
        >
          <ArrowLeft size={11} /> Back
        </button>
        <InvestInput min={investMin} max={investMax} onSubmit={(amount) => onSubmit({ type: 'INVEST_CRYPTO', amount })} />
      </div>
    );
  }

  return (
    <div key="actions-view" className="flex-1 min-h-0 bg-white/[0.04] border border-white/10 rounded-lg p-3 flex flex-col gap-2 animate-fade-in">
      <div className="text-[10px] uppercase tracking-[0.22em] text-white/40 font-medium">Choose Action</div>
      <div className="grid grid-cols-2 gap-1.5">
        {actions.map((a) => {
          const enabled = a.disabledReason === null;
          const meta = ACTION_META[a.type];
          const Icon = meta?.Icon;
          return (
            <button
              key={a.type}
              disabled={!enabled}
              onClick={() => {
                if (!enabled) return;
                if (a.type === 'TRAVEL' || a.type === 'INVEST_CRYPTO') {
                  setExpanded(a.type);
                } else {
                  onSubmit({ type: a.type });
                }
              }}
              className={`text-left px-2 py-1.5 rounded-md text-xs border transition duration-200 ease-out-quart
                ${enabled
                  ? 'bg-white/[0.08] border-white/15 hover:bg-white/[0.14] active:scale-[0.97]'
                  : 'bg-white/[0.03] border-white/5 text-white/25 cursor-not-allowed'}`}
              title={a.disabledReason ?? undefined}
            >
              <div className="flex items-center gap-1.5 font-medium">
                {Icon && <Icon size={13} />}
                <span>{meta?.label ?? a.type}</span>
              </div>
              <div className="text-[10px] text-white/40 ml-[22px] tabular-nums">{formatCost(a)}</div>
            </button>
          );
        })}
      </div>
    </div>
  );
}

function InvestInput({ min, max, onSubmit }: { min: number; max: number; onSubmit: (amount: number) => void }) {
  const [amount, setAmount] = useState(min);
  const valid = Number.isInteger(amount) && amount >= min && amount <= max;
  return (
    <div className="bg-white/[0.06] border border-white/10 rounded-md p-2 flex flex-col gap-1">
      <div className="text-[10px] uppercase tracking-[0.22em] text-white/40 font-medium tabular-nums">Invest amount (${min} – ${max})</div>
      <input
        type="number"
        min={min}
        max={max}
        step={1}
        value={amount}
        onChange={(e) => setAmount(Number(e.target.value))}
        className="px-2 py-1 rounded bg-white/[0.08] border border-white/15 text-xs"
      />
      <button
        disabled={!valid}
        onClick={() => onSubmit(amount)}
        className="text-xs px-2 py-1 rounded bg-accent-teal/15 border border-accent-teal/30 text-accent-teal hover:bg-accent-teal/25 transition duration-200 ease-out-quart active:scale-[0.97] disabled:opacity-40 disabled:cursor-not-allowed disabled:active:scale-100"
      >
        Invest ${amount}
      </button>
    </div>
  );
}
