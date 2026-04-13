import type { UseGame } from '../hooks/useGame';
import StatsPanel from './StatsPanel';
import WeatherBadge from './WeatherBadge';

export default function GameScreen({ game }: { game: UseGame }) {
  const s = game.state!;
  return (
    <div className="h-screen flex flex-col p-3 gap-2">
      {/* Top bar */}
      <div className="flex items-center justify-between px-3 py-1.5 bg-white/[0.06] rounded-lg text-xs">
        <span className="font-bold tracking-widest">SILICON VALLEY TRAIL</span>
        <div className="flex gap-3 items-center text-white/40">
          <span>Turn <span className="text-accent-teal font-bold">{s.currentTurn}</span></span>
          <span>{s.currentGameDate}</span>
          <WeatherBadge weather={s.weather} />
        </div>
      </div>

      {/* Main: left panel + map column */}
      <div className="flex gap-2 flex-1 min-h-0">
        {/* Left panel */}
        <div className="flex-[1.4] flex flex-col gap-2 min-h-0">
          <StatsPanel stats={s.stats} />

          <div className="flex-1 bg-white/[0.03] border border-dashed border-white/10 rounded-lg p-3 flex items-center justify-center text-white/40 text-sm min-h-0">
            {game.phase === 'EVENT' ? 'Events panel coming soon' : 'Action list coming soon'}
          </div>
        </div>

        {/* Right column (map placeholder) */}
        <div className="w-40 bg-white/[0.04] rounded-lg p-3 text-white/40 text-xs">
          Map column coming soon
        </div>
      </div>
    </div>
  );
}
