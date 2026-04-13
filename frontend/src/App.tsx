import { useGame } from './hooks/useGame';
import HomeScreen from './components/HomeScreen';
import GameScreen from './components/GameScreen';
import GameOverScreen from './components/GameOverScreen';
import Toast from './components/Toast';

export default function App() {
  const game = useGame();

  const screen = (() => {
    if (game.phase === 'IDLE') return <HomeScreen onStart={game.startGame} />;
    if (game.phase === 'LOADING')
      return <div className="h-screen flex items-center justify-center text-white/60">Loading...</div>;
    if (game.phase === 'GAME_OVER' && game.state)
      return <GameOverScreen state={game.state} onPlayAgain={game.playAgain} />;
    if (game.state) return <GameScreen game={game} />;
    return null;
  })();

  return (
    <>
      {screen}
      <Toast message={game.toastMessage} onDismiss={game.dismissToast} />
    </>
  );
}
