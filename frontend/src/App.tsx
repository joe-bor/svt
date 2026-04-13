import { useGame } from './hooks/useGame';
import HomeScreen from './components/HomeScreen';
import GameScreen from './components/GameScreen';

export default function App() {
  const game = useGame();

  if (game.phase === 'IDLE') {
    return <HomeScreen onStart={game.startGame} />;
  }
  if (game.phase === 'LOADING' && !game.state) {
    return (
      <div className="h-screen flex items-center justify-center text-white/60">
        Loading...
      </div>
    );
  }
  if (game.phase === 'GAME_OVER') {
    return (
      <div className="h-screen flex items-center justify-center text-white/80">
        Game over — screen coming soon
      </div>
    );
  }
  if (game.phase === 'EVENT' || game.phase === 'ACTION') {
    return <GameScreen game={game} />;
  }
  return (
    <div className="h-screen flex items-center justify-center text-white/80">
      Game running — turn {game.state?.currentTurn}, phase {game.phase}
    </div>
  );
}
