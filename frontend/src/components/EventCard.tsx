import { Check, Zap } from 'lucide-react';
import type { PendingEventDto } from '../api/types';

interface Props {
  events: PendingEventDto[];
  pendingChoices: Record<number, number>;
  acknowledgedEventIds: Set<number>;
  onPickChoice: (eventId: number, choiceId: number) => void;
  onAcknowledge: (eventId: number) => void;
}

export default function EventCard({
  events,
  pendingChoices,
  acknowledgedEventIds,
  onPickChoice,
  onAcknowledge,
}: Props) {
  if (events.length === 0) return null;

  const colsClass =
    events.length === 1 ? 'grid-cols-1' :
    events.length === 2 ? 'grid-cols-2' :
    'grid-cols-3';

  return (
    <div className={`grid gap-2 h-full ${colsClass}`}>
      {events.map((pending, i) => (
        <EventTile
          key={pending.event.id}
          pending={pending}
          chosenChoiceId={pendingChoices[pending.event.id]}
          acknowledged={acknowledgedEventIds.has(pending.event.id)}
          onPickChoice={onPickChoice}
          onAcknowledge={onAcknowledge}
          index={i}
        />
      ))}
    </div>
  );
}

interface TileProps {
  pending: PendingEventDto;
  chosenChoiceId: number | undefined;
  acknowledged: boolean;
  onPickChoice: (eventId: number, choiceId: number) => void;
  onAcknowledge: (eventId: number) => void;
  index: number;
}

function EventTile({ pending, chosenChoiceId, acknowledged, onPickChoice, onAcknowledge, index }: TileProps) {
  const ev = pending.event;

  const typeLabel =
    ev.eventType === 'RANDOM' ? 'Random' :
    ev.eventType === 'CONDITIONAL' ? 'Conditional' :
    'Location';

  const resolved = pending.requiresChoice ? chosenChoiceId !== undefined : acknowledged;

  return (
    <div
      className={`relative flex flex-col bg-red-500/10 border rounded-lg p-3 overflow-auto min-h-0 animate-fade-in-up transition-[opacity,border-color] duration-300 ease-out-quart
        ${resolved ? 'opacity-50 border-red-400/15' : 'border-red-400/30'}`}
      style={{ animationDelay: `${index * 80}ms` }}
    >
      {resolved && (
        <div className="absolute top-2 right-2 flex items-center justify-center w-5 h-5 rounded-full bg-accent-teal/25 border border-accent-teal/50 animate-check-in">
          <Check size={12} className="text-accent-teal" />
        </div>
      )}

      <div className="flex items-center gap-1 text-[10px] uppercase tracking-[0.22em] text-red-300/70 mb-1.5 font-medium">
        <Zap size={11} />
        <span>{typeLabel} Event</span>
      </div>
      <div className="font-display italic text-lg leading-tight mb-1">{ev.name}</div>
      <div className="text-[13px] leading-snug text-white/60 mb-3">{ev.description}</div>

      {pending.requiresChoice ? (
        <div className="flex flex-col gap-1.5">
          {ev.choices.map((c, i) => {
            const isChosen = chosenChoiceId === c.id;
            const disabled = resolved && !isChosen;
            return (
              <button
                key={c.id}
                onClick={() => !resolved && onPickChoice(ev.id, c.id)}
                disabled={disabled}
                className={`text-left px-3 py-1.5 rounded-md text-xs border transition duration-200 ease-out-quart active:scale-[0.97]
                  ${isChosen
                    ? 'bg-accent-teal/25 border-accent-teal/60 text-accent-teal'
                    : i === 0
                      ? 'bg-accent-teal/15 border-accent-teal/30 text-accent-teal hover:bg-accent-teal/25'
                      : 'bg-white/[0.06] border-white/10 text-white/70 hover:bg-white/[0.10]'}
                  ${disabled ? 'cursor-not-allowed active:scale-100' : ''}`}
              >
                {c.label}
              </button>
            );
          })}
        </div>
      ) : (
        <button
          onClick={() => !resolved && onAcknowledge(ev.id)}
          disabled={resolved}
          className="px-3 py-1.5 rounded-md text-xs bg-white/[0.08] border border-white/15 hover:bg-white/[0.12] transition duration-200 ease-out-quart active:scale-[0.97] disabled:cursor-not-allowed disabled:active:scale-100"
        >
          Continue
        </button>
      )}
    </div>
  );
}
