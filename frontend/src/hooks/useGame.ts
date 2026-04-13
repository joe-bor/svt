import { useCallback, useEffect, useState } from 'react';
import { ApiError, gameApi } from '../api/gameApi';
import type {
  ActionRequest,
  ActionRequestAction,
  GameStateDto,
  LocationDto,
} from '../api/types';

export type Phase = 'IDLE' | 'LOADING' | 'EVENT' | 'ACTION' | 'GAME_OVER';

export interface UseGame {
  phase: Phase;
  state: GameStateDto | null;
  stepIndex: number;
  pendingChoices: Record<number, number>;
  locations: LocationDto[];
  toastMessage: string | null;
  startGame: () => void;
  pickChoice: (eventId: number, choiceId: number) => void;
  advanceNoChoiceEvent: () => void;
  submitAction: (action: ActionRequestAction) => void;
  playAgain: () => void;
  dismissToast: () => void;
}

export function useGame(): UseGame {
  const [phase, setPhase] = useState<Phase>('IDLE');
  const [state, setState] = useState<GameStateDto | null>(null);
  const [stepIndex, setStepIndex] = useState(0);
  const [pendingChoices, setPendingChoices] = useState<Record<number, number>>({});
  const [locations, setLocations] = useState<LocationDto[]>([]);
  const [toastMessage, setToastMessage] = useState<string | null>(null);

  useEffect(() => {
    gameApi.getLocations().then(setLocations).catch((e: ApiError) => setToastMessage(e.message));
  }, []);

  const toast = (e: unknown) => {
    const msg = e instanceof ApiError ? e.message : 'Unexpected error';
    setToastMessage(msg);
  };

  const routeAfterResponse = useCallback(async (next: GameStateDto) => {
    setState(next);
    if (next.status !== 'IN_PROGRESS') {
      setPhase('GAME_OVER');
      setStepIndex(0);
      setPendingChoices({});
      return;
    }
    if (next.pendingEvents.length > 0) {
      setPhase('EVENT');
      setStepIndex(0);
      setPendingChoices({});
      return;
    }
    // IN_PROGRESS and no pending events — roll the next turn
    setPhase('LOADING');
    try {
      const rolled = await gameApi.rollNextTurn(next.id);
      setState(rolled);
      if (rolled.status !== 'IN_PROGRESS') {
        setPhase('GAME_OVER');
      } else if (rolled.pendingEvents.length > 0) {
        setPhase('EVENT');
        setStepIndex(0);
        setPendingChoices({});
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
      setStepIndex(0);
      setPendingChoices({});
      // Per contract §5.8, turn 1 always rolls events, so pendingEvents is non-empty.
      setPhase(created.pendingEvents.length > 0 ? 'EVENT' : 'ACTION');
    } catch (e) {
      toast(e);
      setPhase('IDLE');
    }
  }, []);

  // `eventId` here is the inner EventDto's id (i.e. `pendingEvent.event.id`),
  // which is what the API expects in ActionRequest.eventChoices[].eventId.
  const pickChoice = useCallback((eventId: number, choiceId: number) => {
    setPendingChoices((p) => ({ ...p, [eventId]: choiceId }));
    setStepIndex((i) => {
      const total = state?.pendingEvents.length ?? 0;
      const nextIdx = i + 1;
      if (nextIdx >= total) setPhase('ACTION');
      return nextIdx;
    });
  }, [state]);

  const advanceNoChoiceEvent = useCallback(() => {
    setStepIndex((i) => {
      const total = state?.pendingEvents.length ?? 0;
      const nextIdx = i + 1;
      if (nextIdx >= total) setPhase('ACTION');
      return nextIdx;
    });
  }, [state]);

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
    setPhase('IDLE');
    setState(null);
    setStepIndex(0);
    setPendingChoices({});
  }, []);

  const dismissToast = useCallback(() => setToastMessage(null), []);

  return {
    phase, state, stepIndex, pendingChoices, locations, toastMessage,
    startGame, pickChoice, advanceNoChoiceEvent, submitAction, playAgain, dismissToast,
  };
}
