export function formatMxn(amount?: number | null): string {
  const value = typeof amount === 'number' && Number.isFinite(amount) ? amount : 0;
  const formatted = new Intl.NumberFormat('es-MX', {
    style: 'currency',
    currency: 'MXN',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
  return `${formatted} MXN`;
}

export function maskCuenta(numCuenta?: string | null): string {
  if (!numCuenta || numCuenta.trim().length < 4) return '****';
  const clean = numCuenta.trim();
  return `**** **** **** ${clean.slice(-4)}`;
}

export function formatFecha(raw?: string | null): string {
  if (!raw || !raw.trim()) return 'Sin fecha';
  const date = new Date(raw.trim());
  if (Number.isNaN(date.getTime())) return raw.trim();
  return new Intl.DateTimeFormat('es-MX', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(date);
}

export function formatCountdown(totalSeconds: number): string {
  const safe = Math.max(0, totalSeconds);
  const mins = String(Math.floor(safe / 60)).padStart(2, '0');
  const secs = String(safe % 60).padStart(2, '0');
  return `${mins}:${secs}`;
}
