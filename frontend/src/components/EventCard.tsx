import type { PendingEventDto } from '../api/types';

interface Props {
  events: PendingEventDto[];
  stepIndex: number;
  onPickChoice: (eventId: number, choiceId: number) => void;
  onAdvanceNoChoice: () => void;
}

export default function EventCard({ events, stepIndex, onPickChoice, onAdvanceNoChoice }: Props) {
  const pending = events[stepIndex];
  if (!pending) return null;

  // PendingEventDto wraps EventDto — all display fields live on `pending.event`.
  const ev = pending.event;

  const typeLabel =
    ev.eventType === 'RANDOM' ? 'Random' :
    ev.eventType === 'CONDITIONAL' ? 'Conditional' :
    'Location';

  return (
    <div className="flex-1 min-h-0 flex flex-col gap-2">
      {/* Progress dots */}
      <div className="flex items-center gap-2">
        <span className="text-[9px] uppercase tracking-widest text-white/40">Events</span>
        <div className="flex gap-1">
          {events.map((_, i) => (
            <div
              key={i}
              className={`w-2 h-2 rounded-full ${i === stepIndex ? 'bg-accent-teal' : i < stepIndex ? 'bg-accent-teal/40' : 'bg-white/20'}`}
            />
          ))}
        </div>
        <span className="text-[9px] text-white/30">{stepIndex + 1} of {events.length}</span>
      </div>

      {/* Event card */}
      <div className="flex-1 bg-red-500/10 border border-red-400/30 rounded-lg p-3 overflow-auto min-h-0">
        <div className="text-[9px] uppercase tracking-widest text-red-300/70 mb-1">
          ⚡ {typeLabel} Event
        </div>
        <div className="text-sm font-bold mb-1">{ev.name}</div>
        <div className="text-xs text-white/55 mb-3">{ev.description}</div>

        {pending.requiresChoice ? (
          <div className="flex flex-col gap-1.5">
            {ev.choices.map((c, i) => (
              <button
                key={c.id}
                onClick={() => onPickChoice(ev.id, c.id)}
                className={`text-left px-3 py-1.5 rounded-md text-xs border transition
                  ${i === 0
                    ? 'bg-accent-teal/15 border-accent-teal/30 text-accent-teal hover:bg-accent-teal/25'
                    : 'bg-white/[0.06] border-white/10 text-white/70 hover:bg-white/[0.10]'}`}
              >
                {c.label}
              </button>
            ))}
          </div>
        ) : (
          <button
            onClick={onAdvanceNoChoice}
            className="px-3 py-1.5 rounded-md text-xs bg-white/[0.08] border border-white/15 hover:bg-white/[0.12]"
          >
            Continue →
          </button>
        )}
      </div>
    </div>
  );
}
