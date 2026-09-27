function positiveInteger(value, fallback) {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : fallback;
}

export const SCAN_QUEUE_POLL_INTERVAL_MS = positiveInteger(import.meta.env.VITE_SCAN_QUEUE_POLL_INTERVAL_MS, 5_000);
export const SCAN_STUCK_THRESHOLD_MS = positiveInteger(import.meta.env.VITE_SCAN_STUCK_THRESHOLD_MS, 120_000);