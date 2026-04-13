import { useState } from 'react';
import type { AvailableActionDto, AvailableNextLocationDto, ActionType } from '../api/types';

interface Props {
  actions: AvailableActionDto[];
  nextLocations: AvailableNextLocationDto[];
  onSubmit: (action: { type: ActionType; destinationLocationId?: number; amount?: number }) => void;
}

const label: Record<ActionType, string> = {
  TRAVEL: '🚶 Travel',
  REST: '💤 Rest',
  WORK_ON_PRODUCT: '⚙️ Work on Product',
  MARKETING: '📣 Marketing',
  PITCH_VCS: '💼 Pitch VCs',
  BUY_SUPPLIES: '🛒 Buy Supplies',
  INVEST_CRYPTO: '📈 Invest Crypto',
  SKIP: '⏭ Skip',
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
      <div className="flex-1 min-h-0 bg-white/[0.04] border border-white/10 rounded-lg p-3 flex flex-col items-center justify-center gap-3">
        <div className="text-xs text-accent-red text-center">
          Forced rest — Burnout Wave
        </div>
        <button
          onClick={() => onSubmit({ type: 'SKIP' })}
          className="px-5 py-2 rounded-md text-sm bg-white/[0.08] border border-white/15 hover:bg-white/[0.14]"
        >
          Skip turn
        </button>
      </div>
    );
  }

  // INVEST_CRYPTO amount state — initialized to the server's minAmount (500).
  const investMin = actions.find((a) => a.type === 'INVEST_CRYPTO')?.minAmount ?? 500;
  const investMax = actions.find((a) => a.type === 'INVEST_CRYPTO')?.maxAmount ?? 500;

  return (
    <div className="flex-1 min-h-0 bg-white/[0.04] border border-white/10 rounded-lg p-3 flex flex-col gap-2 overflow-auto">
      <div className="text-[9px] uppercase tracking-widest text-white/40">Choose Action</div>
      <div className="grid grid-cols-2 gap-1.5">
        {actions.map((a) => {
          const enabled = a.disabledReason === null;
          return (
            <button
              key={a.type}
              disabled={!enabled}
              onClick={() => {
                if (!enabled) return;
                if (a.type === 'TRAVEL' || a.type === 'INVEST_CRYPTO') {
                  setExpanded(expanded === a.type ? null : a.type);
                } else {
                  onSubmit({ type: a.type });
                }
              }}
              className={`text-left px-2 py-1.5 rounded-md text-xs border transition
                ${enabled
                  ? 'bg-white/[0.08] border-white/15 hover:bg-white/[0.14]'
                  : 'bg-white/[0.03] border-white/5 text-white/25 cursor-not-allowed'}`}
              title={a.disabledReason ?? undefined}
            >
              <div className="font-medium">{label[a.type] ?? a.type}</div>
              <div className="text-[10px] text-white/40">{formatCost(a)}</div>
            </button>
          );
        })}
      </div>

      {expanded === 'TRAVEL' && (
        <div className="bg-white/[0.06] border border-white/10 rounded-md p-2 flex flex-col gap-1">
          <div className="text-[9px] uppercase text-white/40">Pick destination</div>
          {nextLocations.map((n) => (
            <button
              key={n.locationId}
              onClick={() => onSubmit({ type: 'TRAVEL', destinationLocationId: n.locationId })}
              className="text-left text-xs px-2 py-1 rounded bg-accent-teal/15 border border-accent-teal/30 text-accent-teal hover:bg-accent-teal/25"
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
      )}

      {expanded === 'INVEST_CRYPTO' && (
        <InvestInput min={investMin} max={investMax} onSubmit={(amount) => onSubmit({ type: 'INVEST_CRYPTO', amount })} />
      )}
    </div>
  );
}

function InvestInput({ min, max, onSubmit }: { min: number; max: number; onSubmit: (amount: number) => void }) {
  const [amount, setAmount] = useState(min);
  const valid = amount >= min && amount <= max;
  return (
    <div className="bg-white/[0.06] border border-white/10 rounded-md p-2 flex flex-col gap-1">
      <div className="text-[9px] uppercase text-white/40">Invest amount (${min} – ${max})</div>
      <input
        type="number"
        min={min}
        max={max}
        value={amount}
        onChange={(e) => setAmount(Number(e.target.value))}
        className="px-2 py-1 rounded bg-white/[0.08] border border-white/15 text-xs"
      />
      <button
        disabled={!valid}
        onClick={() => onSubmit(amount)}
        className="text-xs px-2 py-1 rounded bg-accent-teal/15 border border-accent-teal/30 text-accent-teal hover:bg-accent-teal/25 disabled:opacity-40 disabled:cursor-not-allowed"
      >
        Invest ${amount}
      </button>
    </div>
  );
}
