import { useEffect } from 'react';

interface Props {
  message: string | null;
  onDismiss: () => void;
}

export default function Toast({ message, onDismiss }: Props) {
  useEffect(() => {
    if (!message) return;
    const id = setTimeout(onDismiss, 4000);
    return () => clearTimeout(id);
  }, [message, onDismiss]);

  if (!message) return null;
  return (
    <div className="fixed top-4 left-0 right-0 z-50 flex justify-center pointer-events-none">
      <div
        className="pointer-events-auto bg-red-500/20 border border-red-400/50 text-red-100
                   px-4 py-2 rounded-lg text-sm shadow-lg cursor-pointer max-w-md
                   animate-fade-in-down"
        onClick={onDismiss}
        role="alert"
      >
        {message}
      </div>
    </div>
  );
}
