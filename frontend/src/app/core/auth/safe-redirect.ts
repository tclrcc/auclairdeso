/** A path inside this site: never another domain ("//evil.example", "https://..."). */
export function isSafeRedirect(value: string | null | undefined): value is string {
  return !!value && value.startsWith('/') && !value.startsWith('//') && !value.startsWith('/\\');
}
