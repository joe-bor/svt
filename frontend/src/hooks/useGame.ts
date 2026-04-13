import { useCallback, useEffect, useState } from 'react';
import { ApiError, gameApi } from '../api/gameApi';
import type {
  ActionRequest,
  ActionRequestAction,
  GameStateDto,
  LocationDto,
} from '../api/types';

export type Phase = 'IDLE' | 'LOADING' | 'EVENT' | 'ACTION' | 'GAME_OVER';

// URL sync — the `?game=<uuid>` param is the source of truth for which session
// to display, so a refresh after poking the database shows the up-to-date state.
const GAME_URL_PARAM = 'game';
const PENDING_TURN_ADVANCE_KEY = 'svt:pending-turn-advance';

function readGameIdFromUrl(): string | null {
  if (typeof window === 'undefined') return null;
  return new URLSearchParams(window.location.search).get(GAME_URL_PARAM);
}

function writeGameIdToUrl(id: string | null) {
  if (typeof window === 'undefined') return;
  const url = new URL(window.location.href);
  if (id) url.searchParams.set(GAME_URL_PARAM, id);
  else url.searchParams.delete(GAME_URL_PARAM);
  window.history.replaceState(null, '', url.toString());
}

function readPendingTurnAdvanceId(): string | null {
  if (typeof window === 'undefined') return null;
  return window.localStorage.getItem(PENDING_TURN_ADVANCE_KEY);
}

function writePendingTurnAdvanceId(id: string | null) {
  if (typeof window === 'undefined') return;
  if (id) window.localStorage.setItem(PENDING_TURN_ADVANCE_KEY, id);
  else window.localStorage.removeItem(PENDING_TURN_ADVANCE_KEY);
}

function hasPendingTransition(next: GameStateDto): boolean {
  return next.status === 'IN_PROGRESS' && next.pendingEvents.length === 0;
}

function areAllEventsResolved(
  state: GameStateDto,
  pendingChoices: Record<number, number>,
  acknowledgedEventIds: Set<number>,
): boolean {
  return state.pendingEvents.every((p) =>
    p.requiresChoice
      ? pendingChoices[p.event.id] !== undefined
      : acknowledgedEventIds.has(p.event.id),
  );
}

export interface UseGame {
  phase: Phase;
  state: GameStateDto | null;
  pendingChoices: Record<number, number>;
  acknowledgedEventIds: Set<number>;
  locations: LocationDto[];
  toastMessage: string | null;
  startGame: () => void;
  pickChoice: (eventId: number, choiceId: number) => void;
  acknowledgeEvent: (eventId: number) => void;
  submitAction: (action: ActionRequestAction) => void;
  playAgain: () => void;
  dismissToast: () => void;
}

export function useGame(): UseGame {
  // Seed phase from the URL so a refresh with `?game=<id>` paints the loading
  // screen instead of flashing HomeScreen before the fetch starts.
  const [phase, setPhase] = useState<Phase>(() => (readGameIdFromUrl() ? 'LOADING' : 'IDLE'));
  const [state, setState] = useState<GameStateDto | null>(null);
  const [pendingChoices, setPendingChoices] = useState<Record<number, number>>({});
  const [acknowledgedEventIds, setAcknowledgedEventIds] = useState<Set<number>>(new Set());
  const [locations, setLocations] = useState<LocationDto[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    gameApi.getLocations().then(setLocations).catch((e: ApiError) => setToastMessage(e.message));
  }, []);

  const toast = (e: unknown) => {
    const msg = e instanceof ApiError ? e.message : 'Unexpected error';
    setToastMessage(msg);
  };

  const resetEventState = () => {
    setPendingChoices({});
    setAcknowledgedEventIds(new Set());
  };

  const syncPhaseFromState = useCallback((next: GameStateDto) => {
    setState(next);
    if (next.status !== 'IN_PROGRESS') {
      writePendingTurnAdvanceId(null);
      setPhase('GAME_OVER');
      resetEventState();
      return;
    }
    if (next.pendingEvents.length > 0) {
      writePendingTurnAdvanceId(null);
      setPhase('EVENT');
      resetEventState();
      return;
    }
    setPhase('ACTION');
    resetEventState();
  }, []);

  const rollIntoNextTurn = useCallback(async (gameId: string, carriedResolution: GameStateDto['lastResolution']) => {
    writePendingTurnAdvanceId(gameId);
    setPhase('LOADING');
    try {
      const rolled = await gameApi.rollNextTurn(gameId);
      const merged = { ...rolled, lastResolution: rolled.lastResolution ?? carriedResolution };
      writePendingTurnAdvanceId(null);
      syncPhaseFromState(merged);
    } catch {
      setToastMessage('Action saved, but loading the next turn failed. Refresh to continue.');
      setPhase('LOADING');
    }
  }, [syncPhaseFromState]);

  const startGame = useCallback(async () => {
    setPhase('LOADING');
    try {
      const created = await gameApi.createGame();
      writePendingTurnAdvanceId(null);
      writeGameIdToUrl(created.id);
      syncPhaseFromState(created);
    } catch (e) {
      toast(e);
      setPhase('IDLE');
    }
  }, [syncPhaseFromState]);

  // On mount, if the URL carries a game id, hydrate from the server so a refresh
  // after editing the database reflects the new state.
  useEffect(() => {
    const id = readGameIdFromUrl();
    if (!id) return;
    let cancelled = false;
    (async () => {
      try {
        const loaded = await gameApi.getGame(id);
        if (cancelled) return;
        if (readPendingTurnAdvanceId() === loaded.id && hasPendingTransition(loaded)) {
          await rollIntoNextTurn(loaded.id, loaded.lastResolution);
          return;
        }
        syncPhaseFromState(loaded);
      } catch (e) {
        if (cancelled) return;
        toast(e);
        writePendingTurnAdvanceId(null);
        writeGameIdToUrl(null);
        setPhase('IDLE');
      }
    })();
    return () => { cancelled = true; };
  }, [rollIntoNextTurn, syncPhaseFromState]);

  // `eventId` here is the inner EventDto's id (i.e. `pendingEvent.event.id`),
  // which is what the API expects in ActionRequest.eventChoices[].eventId.
  const pickChoice = useCallback((eventId: number, choiceId: number) => {
    setPendingChoices((p) => {
      const next = { ...p, [eventId]: choiceId };
      if (state && areAllEventsResolved(state, next, acknowledgedEventIds)) {
        setPhase('ACTION');
      }
      return next;
    });
  }, [acknowledgedEventIds, state]);

  const acknowledgeEvent = useCallback((eventId: number) => {
    setAcknowledgedEventIds((s) => {
      const next = new Set(s);
      next.add(eventId);
      if (state && areAllEventsResolved(state, pendingChoices, next)) {
        setPhase('ACTION');
      }
      return next;
    });
  }, [pendingChoices, state]);

  const submitAction = useCallback(async (action: ActionRequestAction) => {
    if (!state) return;
    const eventChoices = Object.entries(pendingChoices).map(
      ([eventId, choiceId]) => ({ eventId: Number(eventId), choiceId }),
    );
    const body: ActionRequest = { eventChoices, action };
    setPhase('LOADING');
    try {
      const next = await gameApi.submitAction(state.id, body);
      setState(next);
      if (next.status !== 'IN_PROGRESS') {
        writePendingTurnAdvanceId(null);
        setPhase('GAME_OVER');
        resetEventState();
        return;
      }
      if (next.pendingEvents.length > 0) {
        writePendingTurnAdvanceId(null);
        setPhase('EVENT');
        resetEventState();
        return;
      }
      await rollIntoNextTurn(next.id, next.lastResolution);
    } catch (e) {
      toast(e);
      setPhase('ACTION'); // let the player pick again
    }
  }, [state, pendingChoices, rollIntoNextTurn]);

  const playAgain = useCallback(() => {
    writePendingTurnAdvanceId(null);
    writeGameIdToUrl(null);
    setPhase('IDLE');
    setState(null);
    resetEventState();
  }, []);

  const dismissToast = useCallback(() => setToastMessage(null), []);

  return {
    phase, state, pendingChoices, acknowledgedEventIds, locations, toastMessage,
    startGame, pickChoice, acknowledgeEvent, submitAction, playAgain, dismissToast,
  };
}
