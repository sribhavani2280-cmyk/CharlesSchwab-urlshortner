import { useCallback, useEffect, useRef, useState } from "react";

/** Polls an async function on an interval and exposes data, error, and a manual refresh. */
export function usePolling<T>(
  fn: () => Promise<T>,
  intervalMs: number,
  enabled: boolean,
): { data: T | null; error: string | null; refresh: () => Promise<void> } {
  const [data, setData] = useState<T | null>(null);
  const [error, setError] = useState<string | null>(null);
  const fnRef = useRef(fn);
  fnRef.current = fn;

  const refresh = useCallback(async () => {
    try {
      const next = await fnRef.current();
      setData(next);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Request failed");
    }
  }, []);

  useEffect(() => {
    if (!enabled) return;
    refresh();
    const id = setInterval(refresh, intervalMs);
    return () => clearInterval(id);
  }, [enabled, intervalMs, refresh]);

  return { data, error, refresh };
}
