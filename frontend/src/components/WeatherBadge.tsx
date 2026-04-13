import type { WeatherDto } from '../api/types';

interface Props { weather: WeatherDto; }

const icon: Record<string, string> = {
  CLEAR: '☀️', FOGGY: '🌫', RAINY: '🌧', STORMY: '⛈',
};

export default function WeatherBadge({ weather }: Props) {
  const temp = Math.round(weather.apparentTemperatureMaxF);
  return (
    <span className="text-[10px] bg-amber-400/15 text-accent-amber px-2 py-0.5 rounded-full">
      {icon[weather.bucket] ?? '🌡'} {weather.bucket} · {temp}°F{weather.fallback ? ' *' : ''}
    </span>
  );
}
