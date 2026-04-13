import type {
  ActionRequest,
  ApiErrorResponse,
  GameStateDto,
  LocationDto,
} from './types';

const BASE = import.meta.env.VITE_API_BASE_URL ?? 'http://localhost:8080';

class ApiError extends Error {
  readonly body: ApiErrorResponse;
  constructor(body: ApiErrorResponse) {
    super(body.message);
    this.name = 'ApiError';
    this.body = body;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const res = await fetch(`${BASE}${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
  });
  if (!res.ok) {
    const body = (await res.json().catch(() => null)) as ApiErrorResponse | null;
    throw new ApiError(body ?? {
      timestamp: new Date().toISOString(),
      status: res.status,
      error: res.statusText,
      message: `Request failed: ${res.status}`,
      path,
      validationErrors: [],
    });
  }
  return res.json() as Promise<T>;
}

export const gameApi = {
  getLocations: (): Promise<LocationDto[]> =>
    request<LocationDto[]>('/api/catalog/locations'),

  createGame: (): Promise<GameStateDto> =>
    request<GameStateDto>('/api/games', { method: 'POST' }),

  getGame: (id: string): Promise<GameStateDto> =>
    request<GameStateDto>(`/api/games/${id}`),

  rollNextTurn: (id: string): Promise<GameStateDto> =>
    request<GameStateDto>(`/api/games/${id}/turns/next`, { method: 'POST' }),

  submitAction: (id: string, body: ActionRequest): Promise<GameStateDto> =>
    request<GameStateDto>(`/api/games/${id}/actions`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
};

export { ApiError };
