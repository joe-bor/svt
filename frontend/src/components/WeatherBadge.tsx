import {
  CloudFog,
  CloudLightning,
  CloudRain,
  Sun,
  Thermometer,
} from 'lucide-react';
import type { WeatherDto } from '../api/types';

interface Props { weather: WeatherDto; }

const ICONS: Record<string, React.ComponentType<{ size?: number; className?: string }>> = {
  CLEAR: Sun,
  FOGGY: CloudFog,
  RAINY: CloudRain,
  STORMY: CloudLightning,
};

export default function WeatherBadge({ weather }: Props) {
  const temp = Math.round(weather.apparentTemperatureMaxF);
  const Icon = ICONS[weather.bucket] ?? Thermometer;
  return (
    <span className="inline-flex items-center gap-1.5 text-xs bg-amber-400/15 text-accent-amber px-2.5 py-1 rounded-full tabular-nums font-medium">
      <Icon size={13} />
      <span>{weather.bucket} · {temp}°F{weather.fallback ? ' *' : ''}</span>
    </span>
  );
}
