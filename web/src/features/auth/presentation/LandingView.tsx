import React, { useEffect, useState } from 'react';
import {
  ArrowRight,
  CheckCircle2,
  ChevronLeft,
  ChevronRight,
  Clock,
  CreditCard,
  Lock,
  MailCheck,
  ShieldCheck,
  Smartphone,
  Sparkles,
  Store,
  UserPlus,
  Zap,
} from 'lucide-react';

interface LandingViewProps {
  onGoLogin: () => void;
  onOpenOnboarding: () => void;
}

interface FeatureSlide {
  badge: string;
  title: string;
  description: string;
  highlight: string;
  highlightLabel: string;
  bullets: string[];
  accentFrom: string;
  accentTo: string;
  Icon: React.FC<{ className?: string }>;
}

const SLIDES: FeatureSlide[] = [
  {
    badge: 'Cuenta Digital 16 Dígitos',
    title: 'Tu saldo disponible y contable bajo control absoluto',
    description:
      'Consulta en tiempo real tu saldo disponible, contable y movimientos en proceso desde tu computadora o teléfono.',
    highlight: '24/7',
    highlightLabel: 'Disponibilidad continua',
    bullets: [
      'Apertura digital en minutos con confirmación por correo',
      'Desglose transparente de saldo libre y en verificación',
    ],
    accentFrom: 'from-sky-500/30',
    accentTo: 'to-indigo-500/20',
    Icon: CreditCard,
  },
  {
    badge: 'Pagos y Recargas',
    title: 'Pagos de servicios, recargas móviles y pines digitales',
    description:
      'Paga electricidad, agua, internet, televisión, recargas telefónicas y tarjetas de entretenimiento desde un solo lugar.',
    highlight: '+900',
    highlightLabel: 'Servicios disponibles',
    bullets: [
      'Búsqueda instantánea por empresa o categoría',
      'Importes fijos o abiertos con comisión visible antes de pagar',
    ],
    accentFrom: 'from-emerald-500/30',
    accentTo: 'to-teal-500/20',
    Icon: Store,
  },
  {
    badge: 'Certeza en cada pago',
    title: 'Validación previa y protección contra cargos duplicados',
    description:
      'Verificamos cada referencia o número celular antes de autorizar el cargo y enviamos tu comprobante digital al instante.',
    highlight: '100%',
    highlightLabel: 'Operaciones respaldadas',
    bullets: [
      'Resumen previo y confirmación explícita antes del cargo',
      'Comprobante con folio único enviado directo a tu correo',
    ],
    accentFrom: 'from-violet-500/30',
    accentTo: 'to-sky-500/20',
    Icon: MailCheck,
  },
  {
    badge: 'Seguridad Bancaria',
    title: 'Protección activa en cada acceso y operación',
    description:
      'Sesiones protegidas con cierre automático tras 5 minutos de inactividad, autorización en dos pasos y conexión cifrada.',
    highlight: '2FA',
    highlightLabel: 'Autorización reforzada',
    bullets: [
      'Cierre preventivo automático ante inactividad',
      'Confirmación de seguridad en cada operación de pago',
    ],
    accentFrom: 'from-amber-500/30',
    accentTo: 'to-sky-500/20',
    Icon: ShieldCheck,
  },
];

export const LandingView: React.FC<LandingViewProps> = ({ onGoLogin, onOpenOnboarding }) => {
  const [activeSlide, setActiveSlide] = useState(0);
  const [paused, setPaused] = useState(false);

  useEffect(() => {
    if (paused) return;
    const timer = window.setInterval(() => {
      setActiveSlide((prev) => (prev + 1) % SLIDES.length);
    }, 5500);
    return () => window.clearInterval(timer);
  }, [paused]);

  const slide = SLIDES[activeSlide];
  const SlideIcon = slide.Icon;

  const goPrev = () => setActiveSlide((prev) => (prev - 1 + SLIDES.length) % SLIDES.length);
  const goNext = () => setActiveSlide((prev) => (prev + 1) % SLIDES.length);

  return (
    <div className="relative flex min-h-screen flex-col overflow-hidden bg-slate-950 text-white">
      <div className="pointer-events-none absolute inset-0 bg-grid-dark opacity-50" />
      <div className="pointer-events-none absolute -left-32 -top-32 h-[30rem] w-[30rem] rounded-full bg-sky-500/15 blur-3xl" />
      <div className="pointer-events-none absolute -bottom-40 -right-32 h-[32rem] w-[32rem] rounded-full bg-emerald-500/10 blur-3xl" />

      {/* Top Header */}
      <header className="sticky top-0 z-20 border-b border-white/10 bg-slate-950/70 backdrop-blur-xl">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-3 px-4 py-3.5 sm:px-6 lg:px-8">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-gradient-to-br from-sky-400/25 to-emerald-400/15 text-sky-300 ring-1 ring-inset ring-sky-400/30">
              <ShieldCheck className="h-5 w-5" />
            </div>
            <div>
              <span className="font-display text-lg font-extrabold tracking-tight text-white">
                Gesto<span className="text-sky-400">Pagos</span>
              </span>
              <span className="ml-2 hidden rounded-full border border-white/10 bg-white/5 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wider text-slate-300 sm:inline-block">
                Banca Digital
              </span>
            </div>
          </div>

          <div className="flex items-center gap-2 sm:gap-2.5">
            <button
              type="button"
              onClick={onGoLogin}
              className="rounded-xl px-3.5 py-2 text-xs font-semibold text-slate-200 transition hover:bg-white/10 hover:text-white sm:px-4 sm:text-sm"
            >
              Iniciar sesión
            </button>
            <button
              type="button"
              onClick={onOpenOnboarding}
              className="inline-flex items-center gap-1.5 rounded-xl bg-sky-500 px-3.5 py-2 text-xs font-bold text-slate-950 shadow-glow transition hover:bg-sky-400 sm:px-4 sm:text-sm"
            >
              <UserPlus className="h-4 w-4" />
              <span>Crear cuenta</span>
            </button>
          </div>
        </div>
      </header>

      {/* Main Hero */}
      <main className="relative z-10 mx-auto flex w-full max-w-6xl flex-1 flex-col justify-center px-4 py-12 sm:px-6 lg:px-8 lg:py-16">
        <div className="grid items-center gap-12 lg:grid-cols-12">
          <div className="lg:col-span-6">
            <div className="inline-flex items-center gap-2 rounded-full border border-sky-400/30 bg-sky-500/10 px-3.5 py-1.5 text-xs font-semibold text-sky-300">
              <Sparkles className="h-3.5 w-3.5" />
              Cuenta Digital y Pago de Servicios
            </div>

            <h1 className="mt-5 font-display text-3xl font-extrabold leading-[1.12] tracking-tight sm:text-5xl">
              Paga tus servicios y administra tu dinero{' '}
              <span className="bg-gradient-to-r from-sky-400 via-cyan-300 to-emerald-300 bg-clip-text text-transparent">
                con total confianza
              </span>
            </h1>

            <p className="mt-5 max-w-xl text-base leading-relaxed text-slate-300/95">
              Realiza pagos de recibos, recargas telefónicas y pines electrónicos con validación
              previa de referencia, confirmación de seguridad y comprobantes instantáneos en tu
              correo.
            </p>

            <div className="mt-8 flex flex-wrap items-center gap-3.5">
              <button
                type="button"
                onClick={onGoLogin}
                className="inline-flex items-center gap-2 rounded-2xl bg-white px-6 py-3.5 text-sm font-bold text-slate-950 shadow-xl shadow-slate-950/40 transition duration-200 hover:-translate-y-0.5 hover:bg-sky-50"
              >
                Acceder a mi cuenta
                <ArrowRight className="h-4 w-4" />
              </button>

              <button
                type="button"
                onClick={onOpenOnboarding}
                className="inline-flex items-center gap-2 rounded-2xl border border-white/20 bg-white/[0.06] px-6 py-3.5 text-sm font-semibold text-white backdrop-blur-sm transition duration-200 hover:border-white/35 hover:bg-white/15"
              >
                Abrir cuenta digital
              </button>
            </div>

            <div className="mt-8 flex flex-wrap items-center gap-x-5 gap-y-2 text-xs text-slate-400">
              <span className="inline-flex items-center gap-1.5">
                <Lock className="h-3.5 w-3.5 text-emerald-400" />
                Conexión bancaria cifrada
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Zap className="h-3.5 w-3.5 text-sky-400" />
                Protección contra cargos duplicados
              </span>
              <span className="inline-flex items-center gap-1.5">
                <Smartphone className="h-3.5 w-3.5 text-indigo-400" />
                Disponible en Web y Móvil
              </span>
            </div>
          </div>

          <div
            className="lg:col-span-6"
            onMouseEnter={() => setPaused(true)}
            onMouseLeave={() => setPaused(false)}
          >
            <div className="relative overflow-hidden rounded-3xl border border-white/15 bg-gradient-to-b from-white/[0.09] to-white/[0.04] p-6 shadow-2xl backdrop-blur-xl sm:p-8">
              <div
                className={`pointer-events-none absolute -right-12 -top-12 h-56 w-56 rounded-full bg-gradient-to-br ${slide.accentFrom} ${slide.accentTo} blur-3xl transition-all duration-500`}
              />

              <div className="relative">
                <div className="flex items-center justify-between gap-4">
                  <span className="inline-flex items-center gap-1.5 rounded-full border border-white/15 bg-white/10 px-3 py-1 text-xs font-semibold text-sky-200">
                    <SlideIcon className="h-3.5 w-3.5" />
                    {slide.badge}
                  </span>
                  <span className="font-mono text-xs font-medium text-slate-400">
                    0{activeSlide + 1} / 0{SLIDES.length}
                  </span>
                </div>

                <div className="mt-6 min-h-[220px]">
                  <div className="mb-4 flex items-baseline gap-3">
                    <span className="font-display text-4xl font-extrabold tracking-tight text-white">
                      {slide.highlight}
                    </span>
                    <span className="text-xs font-semibold uppercase tracking-wider text-sky-300">
                      {slide.highlightLabel}
                    </span>
                  </div>

                  <h2 className="font-display text-xl font-bold leading-snug text-white sm:text-2xl">
                    {slide.title}
                  </h2>
                  <p className="mt-2.5 text-sm leading-relaxed text-slate-300">
                    {slide.description}
                  </p>

                  <ul className="mt-4 space-y-2 border-t border-white/10 pt-4">
                    {slide.bullets.map((b) => (
                      <li key={b} className="flex items-center gap-2 text-xs text-slate-200">
                        <CheckCircle2 className="h-4 w-4 shrink-0 text-emerald-400" />
                        <span>{b}</span>
                      </li>
                    ))}
                  </ul>
                </div>

                <div className="mt-6 flex items-center justify-between border-t border-white/10 pt-5">
                  <div className="flex items-center gap-2">
                    {SLIDES.map((s, idx) => (
                      <button
                        key={s.badge}
                        type="button"
                        onClick={() => setActiveSlide(idx)}
                        aria-label={`Ver beneficio ${idx + 1}: ${s.badge}`}
                        className={`h-2 rounded-full transition-all duration-300 ${
                          idx === activeSlide
                            ? 'w-8 bg-sky-400'
                            : 'w-2 bg-white/25 hover:bg-white/50'
                        }`}
                      />
                    ))}
                  </div>

                  <div className="flex items-center gap-2">
                    <button
                      type="button"
                      onClick={goPrev}
                      aria-label="Beneficio anterior"
                      className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/15 bg-white/5 text-white transition hover:bg-white/15"
                    >
                      <ChevronLeft className="h-4 w-4" />
                    </button>
                    <button
                      type="button"
                      onClick={goNext}
                      aria-label="Siguiente beneficio"
                      className="flex h-9 w-9 items-center justify-center rounded-xl border border-white/15 bg-white/5 text-white transition hover:bg-white/15"
                    >
                      <ChevronRight className="h-4 w-4" />
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div className="mt-14 grid gap-4 border-t border-white/10 pt-8 sm:grid-cols-3">
          {[
            {
              Icon: Clock,
              title: 'Operación inmediata 24/7',
              desc: 'Paga servicios y recargas sin horarios bancarios ni filas.',
            },
            {
              Icon: MailCheck,
              title: 'Comprobante al instante',
              desc: 'Recibe número de autorización y folio de seguimiento en tu correo.',
            },
            {
              Icon: ShieldCheck,
              title: 'Respaldo de tu saldo',
              desc: 'Tu dinero está protegido en todo momento con verificación automática.',
            },
          ].map(({ Icon, title, desc }) => (
            <div
              key={title}
              className="flex items-start gap-3.5 rounded-2xl border border-white/[0.07] bg-white/[0.03] p-4 transition hover:border-white/15 hover:bg-white/[0.05]"
            >
              <div className="flex h-10 w-10 shrink-0 items-center justify-center rounded-xl bg-sky-500/15 text-sky-300 ring-1 ring-inset ring-sky-400/20">
                <Icon className="h-4 w-4" />
              </div>
              <div>
                <p className="text-sm font-bold text-white">{title}</p>
                <p className="mt-1 text-xs leading-relaxed text-slate-400">{desc}</p>
              </div>
            </div>
          ))}
        </div>
      </main>
    </div>
  );
};
