interface Props {
  onStart: () => void;
}

export default function HomeScreen({ onStart }: Props) {
  return (
    <div className="h-screen flex flex-col items-center justify-center gap-5">
      <div
        className="text-[10px] tracking-[0.3em] uppercase text-white/40 animate-fade-in"
        style={{ animationDelay: '60ms' }}
      >
        LinkedIn Reach · Take-Home
      </div>
      <h1
        className="font-display italic text-[clamp(3.5rem,8vw,6rem)] leading-[0.95] tracking-tight text-center animate-fade-in-up"
        style={{ animationDelay: '180ms' }}
      >
        Silicon Valley Trail
      </h1>
      <p
        className="font-display italic text-white/55 text-xl leading-snug animate-fade-in-up"
        style={{ animationDelay: '380ms' }}
      >
        Can your startup make it to San Francisco?
      </p>
      <button
        onClick={onStart}
        className="mt-3 px-7 py-2.5 rounded-lg font-semibold text-sm tracking-wide
                   bg-gradient-to-br from-accent-teal to-accent-pink
                   text-[#0f0c29] hover:brightness-110 transition duration-200 ease-out-quart active:scale-[0.97] animate-fade-in-up"
        style={{ animationDelay: '560ms' }}
      >
        Start Journey →
      </button>
    </div>
  );
}
