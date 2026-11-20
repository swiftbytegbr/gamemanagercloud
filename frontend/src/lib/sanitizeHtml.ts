import DOMPurify from "dompurify";

/**
 * Sanitize HTML content to prevent XSS when using `dangerouslySetInnerHTML`.
 */
export function sanitizeHtml(dirtyHtml: string): string {
    return DOMPurify.sanitize(dirtyHtml);
}
