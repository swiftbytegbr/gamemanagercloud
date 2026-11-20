export function uniqueStrings(values: string[]) {
    const seen = new Set<string>();
    const unique: string[] = [];
    values.forEach((value) => {
        if (seen.has(value)) return;
        seen.add(value);
        unique.push(value);
    });
    return unique;
}
