import { Skull, Trophy } from 'lucide-react';
import type { GameStateDto } from '../api/types';

const messages: Record<string, string> = {
  REACHED_SF:        'You made it to San Francisco!',
  CASH_BANKRUPT:     'You ran out of money.',
  CUSTOMERS_ZERO:    'Your users abandoned you.',
  MORALE_ZERO:       'You burned out.',
  ACQUIRED:          'You got acquired — but is that winning?',
  TOOK_LINKEDIN_JOB: 'You took the LinkedIn job. Safe choice.',
};

export default function GameOverScreen({ state, onPlayAgain }: { state: GameStateDto; onPlayAgain: () => void }) {
  const won = state.status === 'WON';
  const reason = state.gameEndReason ?? 'UNKNOWN';
  return (
    <div className="h-screen flex flex-col items-center justify-center gap-3 p-8">
      <div className="animate-scale-in" style={{ animationDelay: '40ms' }}>
        {won
          ? <Trophy size={80} className="text-accent-teal" />
          : <Skull size={80} className="text-accent-red" />}
      </div>
      <div
        className={`font-display italic text-4xl leading-tight text-center max-w-xl animate-fade-in-up ${won ? 'text-accent-teal' : 'text-accent-red'}`}
        style={{ animationDelay: '220ms' }}
      >
        {messages[reason] ?? reason}
      </div>
      <div
        className="text-white/50 text-xs tabular-nums tracking-wide animate-fade-in"
        style={{ animationDelay: '420ms' }}
      >
        {reason} · {state.currentTurn} turns · ${state.stats.cash.toLocaleString()} remaining
      </div>
      <button
        onClick={onPlayAgain}
        className="mt-2 px-5 py-2 rounded-lg text-sm font-medium bg-white/[0.10] border border-white/20 hover:bg-white/[0.15] transition duration-200 ease-out-quart active:scale-[0.97] animate-fade-in-up"
        style={{ animationDelay: '600ms' }}
      >
        Play Again
      </button>
    </div>
  );
}
