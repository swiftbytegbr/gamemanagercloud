import {
    type CSSProperties,
    useCallback,
    useEffect,
    useMemo,
    useRef,
    useState,
} from "react";
import { Snowflake as SnowflakeIcon } from "lucide-react";

const SNOWFLAKE_ANIMATION_NAME = "snowflake-fall";
const SNOWFLAKE_SWAY_ANIMATION_NAME = "snowflake-sway";

type FallingSnowflakeProps = {
    onClick: () => void;
    onFinish: () => void;
};

function FallingSnowflake({ onClick, onFinish }: FallingSnowflakeProps) {
    const [isVisible, setIsVisible] = useState(true);
    const finishCalledRef = useRef(false);

    const startX = useMemo(() => Math.random() * 100, []);
    const fallDurationSeconds = useMemo(() => 8 + Math.random() * 3, []);
    const swayDurationSeconds = useMemo(() => 2.5 + Math.random(), []);
    const swayDistance = useMemo(
        () => (20 + Math.random() * 30) * (Math.random() > 0.5 ? 1 : -1),
        [],
    );
    const flakeSize = useMemo(() => 28 + Math.random() * 10, []);

    useEffect(() => {
        const timeout = window.setTimeout(
            () => setIsVisible(false),
            fallDurationSeconds * 1000 + 500,
        );
        return () => window.clearTimeout(timeout);
    }, [fallDurationSeconds]);

    useEffect(() => {
        if (!isVisible && !finishCalledRef.current) {
            finishCalledRef.current = true;
            onFinish();
        }
    }, [isVisible, onFinish]);

    if (!isVisible) return null;

    return (
        <div
            className="fixed z-[9999] select-none"
            style={{
                left: `${startX}%`,
                top: 0,
                animation: `${SNOWFLAKE_ANIMATION_NAME} ${fallDurationSeconds}s linear forwards`,
                cursor: "pointer",
            }}
            onClick={onClick}
            role="button"
            aria-label="Snowflake">
            <div
                className="text-primary drop-shadow-lg"
                style={
                    {
                        animation: `${SNOWFLAKE_SWAY_ANIMATION_NAME} ${swayDurationSeconds}s ease-in-out infinite alternate`,
                        "--sway-distance": `${swayDistance}px`,
                    } as CSSProperties
                }>
                <SnowflakeIcon size={flakeSize} strokeWidth={1.25} />
            </div>
        </div>
    );
}

export function Snowflake() {
    const [flakes, setFlakes] = useState<number[]>([]);
    const nextIdRef = useRef(0);

    const spawnFlakes = useCallback((count = 1) => {
        setFlakes((prev) => {
            const additions = Array.from(
                { length: count },
                () => nextIdRef.current++,
            );
            return [...prev, ...additions];
        });
    }, []);

    const removeFlake = useCallback((id: number) => {
        setFlakes((prev) => prev.filter((flakeId) => flakeId !== id));
    }, []);

    useEffect(() => {
        spawnFlakes(1);
    }, [spawnFlakes]);

    const randomSpawnCount = () => 2 + Math.floor(Math.random() * 3); // 2-4 new flakes

    return (
        <>
            {flakes.map((id) => (
                <FallingSnowflake
                    key={id}
                    onClick={() => spawnFlakes(randomSpawnCount())}
                    onFinish={() => removeFlake(id)}
                />
            ))}
        </>
    );
}
