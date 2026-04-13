interface Props {
  onStart: () => void;
}

export default function HomeScreen({ onStart }: Props) {
  return (
    <div className="h-screen flex flex-col items-center justify-center gap-4">
      <div className="text-xs tracking-widest uppercase text-white/40">
        LinkedIn Reach · Take-Home
      </div>
      <h1 className="text-4xl font-extrabold tracking-tight">
        Silicon Valley Trail
      </h1>
      <p className="text-white/50 text-sm">
        Can your startup make it to San Francisco?
      </p>
      <button
        onClick={onStart}
        className="mt-2 px-7 py-2.5 rounded-lg font-bold text-sm tracking-wide
                   bg-gradient-to-br from-accent-teal to-accent-pink
                   text-[#0f0c29] hover:brightness-110 transition"
      >
        Start Journey →
      </button>
    </div>
  );
}
