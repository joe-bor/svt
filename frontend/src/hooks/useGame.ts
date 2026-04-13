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

  const routeAfterResponse = useCallback(async (next: GameStateDto) => {
    setState(next);
    if (next.status !== 'IN_PROGRESS') {
      setPhase('GAME_OVER');
      resetEventState();
      return;
    }
    if (next.pendingEvents.length > 0) {
      setPhase('EVENT');
      resetEventState();
      return;
    }
    // IN_PROGRESS and no pending events — roll the next turn.
    // rollNextTurn's response always has lastResolution=null (backend only sets it
    // on the action submission response), so we carry the resolution forward here
    // so the recap card is visible once the next turn's events arrive.
    const carriedResolution = next.lastResolution;
    setPhase('LOADING');
    try {
      const rolled = await gameApi.rollNextTurn(next.id);
      const merged = { ...rolled, lastResolution: rolled.lastResolution ?? carriedResolution };
      setState(merged);
      if (merged.status !== 'IN_PROGRESS') {
        setPhase('GAME_OVER');
      } else if (merged.pendingEvents.length > 0) {
        setPhase('EVENT');
        resetEventState();
      } else {
        // Extremely rare — no events rolled. Allow action anyway.
        setPhase('ACTION');
      }
    } catch (e) {
      toast(e);
      setPhase('ACTION'); // fall back so UI is not stuck on LOADING
    }
  }, []);

  const startGame = useCallback(async () => {
    setPhase('LOADING');
    try {
      const created = await gameApi.createGame();
      setState(created);
      resetEventState();
      writeGameIdToUrl(created.id);
      // Per contract §5.8, turn 1 always rolls events, so pendingEvents is non-empty.
      setPhase(created.pendingEvents.length > 0 ? 'EVENT' : 'ACTION');
    } catch (e) {
      toast(e);
      setPhase('IDLE');
    }
  }, []);

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
        resetEventState();
        await routeAfterResponse(loaded);
      } catch (e) {
        if (cancelled) return;
        toast(e);
        writeGameIdToUrl(null);
        setPhase('IDLE');
      }
    })();
    return () => { cancelled = true; };
  }, [routeAfterResponse]);

  // `eventId` here is the inner EventDto's id (i.e. `pendingEvent.event.id`),
  // which is what the API expects in ActionRequest.eventChoices[].eventId.
  const pickChoice = useCallback((eventId: number, choiceId: number) => {
    setPendingChoices((p) => ({ ...p, [eventId]: choiceId }));
  }, []);

  const acknowledgeEvent = useCallback((eventId: number) => {
    setAcknowledgedEventIds((s) => {
      const next = new Set(s);
      next.add(eventId);
      return next;
    });
  }, []);

  // When every pending event is resolved, advance to ACTION.
  useEffect(() => {
    if (phase !== 'EVENT' || !state) return;
    const allResolved = state.pendingEvents.every((p) =>
      p.requiresChoice
        ? pendingChoices[p.event.id] !== undefined
        : acknowledgedEventIds.has(p.event.id),
    );
    if (allResolved) setPhase('ACTION');
  }, [phase, state, pendingChoices, acknowledgedEventIds]);

  const submitAction = useCallback(async (action: ActionRequestAction) => {
    if (!state) return;
    const eventChoices = Object.entries(pendingChoices).map(
      ([eventId, choiceId]) => ({ eventId: Number(eventId), choiceId }),
    );
    const body: ActionRequest = { eventChoices, action };
    setPhase('LOADING');
    try {
      const next = await gameApi.submitAction(state.id, body);
      await routeAfterResponse(next);
    } catch (e) {
      toast(e);
      setPhase('ACTION'); // let the player pick again
    }
  }, [state, pendingChoices, routeAfterResponse]);

  const playAgain = useCallback(() => {
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
