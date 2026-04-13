import type { UseGame } from '../hooks/useGame';
import ActionList from './ActionList';
import EventCard from './EventCard';
import MapColumn from './MapColumn';
import StatsPanel from './StatsPanel';
import TurnRecapCard from './TurnRecapCard';
import WeatherBadge from './WeatherBadge';

export default function GameScreen({ game }: { game: UseGame }) {
  const s = game.state!;
  return (
    <div className="h-screen flex flex-col p-4 gap-3">
      {/* Top bar */}
      <div className="flex items-center justify-between px-4 py-3 bg-white/[0.06] rounded-lg text-sm">
        <span className="font-display italic text-2xl leading-none">Silicon Valley Trail</span>
        <div className="flex gap-4 items-center text-white/50 tabular-nums">
          <span>Turn <span className="text-accent-teal font-semibold">{s.currentTurn}</span></span>
          <span>{s.currentGameDate}</span>
          <WeatherBadge weather={s.weather} />
        </div>
      </div>

      {/* Main: left panel + map column */}
      <div className="flex gap-3 flex-1 min-h-0">
        {/* Left panel */}
        <div className="flex-[1.3] flex flex-col gap-3 min-h-0">
          <StatsPanel stats={s.stats} />

          {/* Event/action region — bounded height keeps the event card compact */}
          <section className="h-[44%] min-h-0 flex flex-col">
            {game.phase === 'EVENT' ? (
              <div key="event-phase" className="flex-1 min-h-0 flex flex-col animate-fade-in">
                <EventCard
                  events={s.pendingEvents}
                  pendingChoices={game.pendingChoices}
                  acknowledgedEventIds={game.acknowledgedEventIds}
                  onPickChoice={game.pickChoice}
                  onAcknowledge={game.acknowledgeEvent}
                />
              </div>
            ) : (
              <div key="action-phase" className="flex-1 min-h-0 flex flex-col animate-fade-in">
                <ActionList
                  actions={s.availableActions}
                  nextLocations={s.availableNextLocations}
                  onSubmit={game.submitAction}
                />
              </div>
            )}
          </section>

          {/* Last-turn recap — fills the lower panel when a prior resolution exists */}
          {game.phase === 'EVENT' && s.lastResolution ? (
            <TurnRecapCard summary={s.lastResolution} />
          ) : (
            <div className="flex-1" />
          )}
        </div>

        {/* Right column (map) */}
        <MapColumn locations={game.locations} currentLocationId={s.currentLocation.id} />
      </div>
    </div>
  );
}
