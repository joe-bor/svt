// Enums (SCREAMING_SNAKE_CASE on the wire per contract §3.5)
export type GameStatus = 'IN_PROGRESS' | 'WON' | 'LOST';
export type GameEndReason =
  | 'REACHED_SF'
  | 'CASH_BANKRUPT'
  | 'CUSTOMERS_ZERO'
  | 'MORALE_ZERO'
  | 'ACQUIRED'
  | 'TOOK_LINKEDIN_JOB';
export type EventType = 'RANDOM' | 'CONDITIONAL' | 'LOCATION';
export type SpecialEffectType = 'GAME_OVER' | 'RANDOM_5050' | 'LINKEDIN_BONUS' | 'LOSE_ACTION';
export type DetourBonusStat = 'CASH' | 'CUSTOMERS' | 'MORALE' | 'COFFEE' | 'BUGS';
export type ActionType =
  | 'TRAVEL' | 'REST' | 'WORK_ON_PRODUCT' | 'MARKETING'
  | 'PITCH_VCS' | 'BUY_SUPPLIES' | 'INVEST_CRYPTO' | 'SKIP';
export type WeatherBucket = 'CLEAR' | 'FOGGY' | 'RAINY' | 'STORMY';
export type TemperatureBracket = 'COLD' | 'NORMAL' | 'HOT';

// Shared DTOs — verified against `LocationDto.java`, `EventDto.java`,
// `EventChoiceDto.java` and contract §5.3–§5.9.

export interface LocationDto {
  id: number;
  name: string;
  description: string;
  routeOrder: number | null;         // null for detour rows
  detour: boolean;
  branchesFromId: number | null;     // parent main-route id for detours, null otherwise
  latitude: number;
  longitude: number;
  detourBonusStat: DetourBonusStat | null;
  detourBonusValue: number | null;
}

export interface StatsDto {
  cash: number;
  customers: number;
  morale: number;
  coffee: number;
}

export interface WeatherDto {
  weatherCode: number;
  bucket: WeatherBucket;
  apparentTemperatureMaxF: number;
  temperatureBracket: TemperatureBracket;
  fallback: boolean;
}

// EventChoiceDto — mirrors the Java record verbatim.
// specialEffect is the enum NAME as a string (e.g. "GAME_OVER") or null.
export interface EventChoiceDto {
  id: number;
  label: string;
  cashEffect: number;
  customersEffect: number;
  moraleEffect: number;
  coffeeEffect: number;
  bugsEffect: number;
  specialEffect: SpecialEffectType | null;
}

// EventDto — mirrors the Java record verbatim.
// `eventType` is a string (not `type`); `locationId` is nullable for non-LOCATION events.
export interface EventDto {
  id: number;
  name: string;
  description: string;
  eventType: EventType;
  locationId: number | null;
  hasChoice: boolean;
  autoCashEffect: number;
  autoCustomersEffect: number;
  autoMoraleEffect: number;
  autoCoffeeEffect: number;
  autoBugsEffect: number;
  specialEffect: SpecialEffectType | null;
  choices: EventChoiceDto[];
}

// PendingEventDto wraps EventDto (contract §5.5).
export interface PendingEventDto {
  rollOrder: number;
  event: EventDto;
  requiresChoice: boolean;
}

export interface WeatherSurcharge {
  coffeeAdded: number;
  cashAdded: number;
  moraleAdded: number;
}

// AvailableActionDto (contract §5.6). Cost fields are numeric — the client
// formats them. There is NO `costSummary` string and NO `enabled` boolean on
// the wire. "Enabled" == `disabledReason === null` (for MVP the server
// filters the list, so `disabledReason` is always null).
export interface AvailableActionDto {
  type: ActionType;
  cashCost: number;
  coffeeCost: number;
  moraleCost: number;
  weatherSurcharge: WeatherSurcharge;
  requiresDestination: boolean;
  requiresAmount: boolean;
  minAmount: number | null;
  maxAmount: number | null;
  disabledReason: string | null;
}

// AvailableNextLocationDto (contract §5.7). `travelCost` is NOT here —
// travel cost lives on the TRAVEL AvailableActionDto.
export interface AvailableNextLocationDto {
  locationId: number;
  name: string;
  detour: boolean;
  eta: number;
  detourBonusStat: DetourBonusStat | null;
  detourBonusValue: number | null;
}

// TurnResolutionSummaryDto — mirrors the Java record per contract §5.9.
export interface StatDeltas {
  cash: number;
  customers: number;
  morale: number;
  coffee: number;
}

export interface EventResolutionDetail {
  eventId: number;
  choiceId: number | null;
  statDeltas: StatDeltas;
  triggeredGameOver: boolean;
  dynamicNote: string | null;
}

export interface TemperatureModifier {
  morale: number;
  coffee: number;
}

export interface PassiveDeltas {
  cashFromEconomy: number;
  coffeeDecay: number;
  temperatureModifier: TemperatureModifier;
  cryptoSettlementCredited: number | null;
}

export interface WeatherSurcharges {
  coffee: number;
}

export interface ActionResolutionDetail {
  actionType: ActionType;
  cashDelta: number;
  coffeeDelta: number;
  moraleDelta: number;
  customersDelta: number;
  destinationLocationId: number | null;
  weatherSurcharges: WeatherSurcharges;
  detourBonusApplied: string | null;
  notes: string[];
}

export interface WinLossResult {
  ended: boolean;
  reason: GameEndReason | null;
}

export interface TurnResolutionSummaryDto {
  eventResolutions: EventResolutionDetail[];
  passiveDeltas: PassiveDeltas;
  actionResolution: ActionResolutionDetail;
  winLoss: WinLossResult;
}

export interface GameStateDto {
  id: string;
  status: GameStatus;
  gameEndReason: GameEndReason | null;
  gameStartDate: string;     // YYYY-MM-DD
  currentGameDate: string;   // YYYY-MM-DD
  currentTurn: number;
  currentLocation: LocationDto;
  stats: StatsDto;
  pendingCryptoSettlement: number | null;
  linkedinBonusActive: boolean;
  weather: WeatherDto;
  pendingEvents: PendingEventDto[];
  availableActions: AvailableActionDto[];
  availableNextLocations: AvailableNextLocationDto[];
  lastResolution: TurnResolutionSummaryDto | null;
}

// Request bodies
export interface EventChoiceSelection {
  eventId: number;
  choiceId: number;
}

export interface ActionRequestAction {
  type: ActionType;
  destinationLocationId?: number;
  amount?: number;
}

export interface ActionRequest {
  eventChoices: EventChoiceSelection[];
  action: ActionRequestAction;
}

// Error body
export interface ApiErrorResponse {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  validationErrors: { field: string; message: string }[];
}
