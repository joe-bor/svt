import type { LocationDto } from '../api/types';

interface Props {
  locations: LocationDto[];
  currentLocationId: number;
}

export default function MapColumn({ locations, currentLocationId }: Props) {
  // Main route only — detours have null `routeOrder`. If the player is on a
  // detour, the highlight lifts off the list (acceptable: detours are brief
  // side-trips and the top label still communicates location).
  const mainRoute = locations
    .filter((l) => l.routeOrder !== null)
    .sort((a, b) => (a.routeOrder ?? 0) - (b.routeOrder ?? 0));

  const current = locations.find((l) => l.id === currentLocationId);
  const onDetour = current?.detour === true;

  return (
    <div className="w-56 bg-white/[0.04] rounded-lg p-4 flex flex-col gap-1.5 text-sm overflow-hidden">
      <div className="text-[10px] uppercase tracking-[0.22em] text-white/40 mb-2 font-medium">Route</div>
      {onDetour && (
        <div className="text-xs bg-accent-teal/15 border border-accent-teal/40 text-accent-teal rounded px-2.5 py-1 mb-1">
          ▶ {current!.name} (detour)
        </div>
      )}
      {mainRoute.map((loc) => {
        const isCurrent = !onDetour && loc.id === currentLocationId;
        const cls = isCurrent
          ? 'bg-accent-teal/15 border border-accent-teal/40 text-accent-teal font-semibold rounded px-2.5 py-1'
          : 'text-white/40 pl-2.5 py-0.5';
        return (
          <div key={loc.id} className={`truncate ${cls}`}>
            {isCurrent ? '▶' : '·'} {loc.name}
          </div>
        );
      })}
    </div>
  );
}
