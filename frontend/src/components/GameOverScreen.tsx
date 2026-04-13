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
      <div className="text-5xl">{won ? '🏆' : '💀'}</div>
      <div className={`text-2xl font-extrabold ${won ? 'text-accent-teal' : 'text-accent-red'}`}>
        {messages[reason] ?? reason}
      </div>
      <div className="text-white/50 text-xs">
        {reason} · {state.currentTurn} turns · ${state.stats.cash.toLocaleString()} remaining
      </div>
      <button
        onClick={onPlayAgain}
        className="mt-2 px-5 py-2 rounded-lg text-sm bg-white/[0.10] border border-white/20 hover:bg-white/[0.15]"
      >
        Play Again
      </button>
    </div>
  );
}
