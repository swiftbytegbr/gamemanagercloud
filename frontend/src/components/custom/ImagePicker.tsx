import {
    type ChangeEvent,
    forwardRef,
    useEffect,
    useImperativeHandle,
    useRef,
    useState,
} from "react";
import { Button } from "@/components/ui/button.tsx";

export type ProfileImagePickerHandle = {
    exportCroppedFile: () => Promise<File | null>;
    hasImage: () => boolean;
};

type ProfileImagePickerProps = {
    initialImageUrl?: string;
    outputSize?: number;
    accept?: string;
    onReadyChange?: (ready: boolean) => void;
};

export const ImagePicker = forwardRef<
    ProfileImagePickerHandle,
    ProfileImagePickerProps
>(function ProfileImagePicker(
    { initialImageUrl, outputSize = 256, accept = "image/*", onReadyChange },
    ref,
) {
    const [fileUrl, setFileUrl] = useState<string | null>(null);
    const [zoom, setZoom] = useState(1);
    const [imgEl, setImgEl] = useState<HTMLImageElement | null>(null);
    const inputRef = useRef<HTMLInputElement | null>(null);

    useEffect(() => {
        if (!initialImageUrl) return;
        setFileUrl(initialImageUrl);
    }, [initialImageUrl]);

    useEffect(() => {
        return () => {
            if (fileUrl && !initialImageUrl) URL.revokeObjectURL(fileUrl);
        };
    }, [fileUrl, initialImageUrl]);

    const handleFileSelect = (e: ChangeEvent<HTMLInputElement>) => {
        const f = e.target.files?.[0];
        if (!f) return;
        if (fileUrl && fileUrl.startsWith("blob:"))
            URL.revokeObjectURL(fileUrl);
        const url = URL.createObjectURL(f);
        setFileUrl(url);
        setZoom(1);
    };

    useEffect(() => {
        if (!fileUrl) {
            setImgEl(null);
            return;
        }
        const img = new Image();
        img.src = fileUrl;
        img.onload = () => setImgEl(img);
        img.onerror = () => setImgEl(null);
    }, [fileUrl]);

    useEffect(() => {
        onReadyChange?.(!!imgEl);
    }, [imgEl, onReadyChange]);

    const exportCroppedFile = async (): Promise<File | null> => {
        if (!imgEl) return null;
        const canvas = document.createElement("canvas");
        const size = outputSize;
        canvas.width = size;
        canvas.height = size;
        const ctx = canvas.getContext("2d");
        if (!ctx) return null;

        const { naturalWidth: iw, naturalHeight: ih } = imgEl;

        const baseScale = Math.max(size / iw, size / ih);
        const scale = baseScale * zoom;
        const drawW = iw * scale;
        const drawH = ih * scale;
        const dx = (size - drawW) / 2;
        const dy = (size - drawH) / 2;
        ctx.clearRect(0, 0, size, size);
        ctx.imageSmoothingEnabled = true;
        ctx.imageSmoothingQuality = "high";
        ctx.drawImage(imgEl, dx, dy, drawW, drawH);

        const blob: Blob | null = await new Promise((resolve) =>
            canvas.toBlob((b) => resolve(b), "image/png", 0.9),
        );
        if (!blob) return null;

        const filename = "profile.png";

        return new File([blob], filename, { type: blob.type });
    };

    useImperativeHandle(ref, () => ({
        exportCroppedFile,
        hasImage: () => !!imgEl,
    }));

    return (
        <div className="flex flex-col gap-4">
            <div className="flex justify-center items-center gap-3">
                <input
                    ref={inputRef}
                    type="file"
                    accept={accept}
                    className="hidden"
                    onChange={handleFileSelect}
                />
                <Button
                    type="button"
                    variant="input"
                    onClick={() => inputRef.current?.click()}>
                    Select image
                </Button>
                <span className="text-xs text-muted-foreground">
                    JPG, PNG, WEBP • square crop
                </span>
            </div>

            <div className="flex flex-col items-center gap-3">
                <div
                    className="relative rounded-md border bg-background overflow-hidden"
                    style={{ width: 256, height: 256 }}>
                    {fileUrl ? (
                        <>
                            <img
                                src={fileUrl}
                                alt="Selected"
                                className="absolute inset-0 w-full h-full object-cover select-none"
                                style={{
                                    transform: `scale(${zoom})`,
                                    transformOrigin: "center",
                                }}
                                draggable={false}
                            />
                            <div
                                aria-hidden
                                role="presentation"
                                className="pointer-events-none absolute inset-0"
                                style={{
                                    background:
                                        "radial-gradient(circle at center, rgba(0,0,0,0) 70%, rgba(0,0,0,0.55) 71%)",
                                }}
                            />
                        </>
                    ) : (
                        <div className="w-full h-full grid place-items-center text-muted-foreground text-sm">
                            No image selected
                        </div>
                    )}
                </div>
                <input
                    type="range"
                    min={1}
                    max={3}
                    step={0.01}
                    value={zoom}
                    onChange={(e) => setZoom(Number(e.target.value))}
                    className="w-48"
                    aria-label="Zoom"
                />
            </div>
        </div>
    );
});

export default ImagePicker;
