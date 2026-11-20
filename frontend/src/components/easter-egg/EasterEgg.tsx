import { useEffect, useRef } from "react";
import { Button } from "@/components/ui/button.tsx";
import { popDialog } from "@/store/dialogStore.tsx";

export function EasterEgg() {
    const canvasRef = useRef<HTMLCanvasElement | null>(null);
    const rafRef = useRef<number | null>(null);

    useEffect(() => {
        const canvas = canvasRef.current!;
        const ctx = canvas.getContext("2d")!;
        let width = (canvas.width = canvas.clientWidth);
        let height = (canvas.height = canvas.clientHeight);

        const onResize = () => {
            width = canvas.width = canvas.clientWidth;
            height = canvas.height = canvas.clientHeight;
        };
        const handleResize = () => requestAnimationFrame(onResize);
        window.addEventListener("resize", handleResize);

        type Star = { x: number; y: number; z: number; pz: number };
        const starCount = Math.min(400, Math.floor((width * height) / 2500));
        const stars: Star[] = Array.from({ length: starCount }, () => ({
            x: (Math.random() - 0.5) * width * 2,
            y: (Math.random() - 0.5) * height * 2,
            z: Math.random() * width,
            pz: 0,
        }));

        const draw = () => {
            ctx.fillStyle = "#05060a";
            ctx.fillRect(0, 0, width, height);

            ctx.translate(width / 2, height / 2);
            for (const s of stars) {
                s.z -= 4; // speed
                if (s.z < 1) {
                    s.x = (Math.random() - 0.5) * width * 2;
                    s.y = (Math.random() - 0.5) * height * 2;
                    s.z = width;
                    s.pz = s.z;
                }
                const sx = (s.x / s.z) * width;
                const sy = (s.y / s.z) * height;
                const r = Math.max(0.5, 2 - s.z / (width / 2));
                ctx.fillStyle = "#a6e3ff";
                ctx.beginPath();
                ctx.arc(sx, sy, r, 0, Math.PI * 2);
                ctx.fill();

                // streak
                const px = (s.x / s.pz) * width;
                const py = (s.y / s.pz) * height;
                s.pz = s.z;
                ctx.strokeStyle = "rgba(166,227,255,0.35)";
                ctx.lineWidth = 1;
                ctx.beginPath();
                ctx.moveTo(px, py);
                ctx.lineTo(sx, sy);
                ctx.stroke();
            }
            ctx.setTransform(1, 0, 0, 1, 0, 0);
            rafRef.current = requestAnimationFrame(draw);
        };
        rafRef.current = requestAnimationFrame(draw);

        return () => {
            if (rafRef.current) cancelAnimationFrame(rafRef.current);
            window.removeEventListener("resize", handleResize);
        };
    }, []);

    return (
        <div className="relative h-full w-full rounded-md overflow-hidden">
            <canvas
                ref={canvasRef}
                className="absolute inset-0 w-full h-full"
            />
            <div className="relative z-10 h-full w-full flex flex-col items-center justify-center gap-4 text-center px-6">
                <h2 className="text-2xl md:text-3xl font-bold tracking-wide text-white drop-shadow">
                    Clarke's Third Law
                </h2>
                <p className="text-sm md:text-base text-slate-200 max-w-[60ch]">
                    Any sufficiently advanced technology is indistinguishable
                    from magic.
                </p>
                <div className="opacity-90 text-xs md:text-sm text-slate-300">
                    Arthur C. Clarke - 1968
                </div>
                <Button variant="secondary" onClick={() => popDialog()}>
                    Go Back
                </Button>
            </div>
            <div className="pointer-events-none absolute inset-0 bg-[radial-gradient(circle_at_50%_20%,rgba(255,255,255,0.08),transparent_40%),radial-gradient(circle_at_20%_80%,rgba(166,227,255,0.06),transparent_35%),radial-gradient(circle_at_80%_70%,rgba(255,170,220,0.05),transparent_35%)]" />
        </div>
    );
}
