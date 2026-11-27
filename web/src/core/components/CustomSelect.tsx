import React, { useEffect, useLayoutEffect, useMemo, useRef, useState } from 'react';
import ReactDOM from 'react-dom';
import { Check, ChevronDown, Search, X } from 'lucide-react';

export interface SelectOption {
  value: string;
  label: string;
  badge?: string;
  dotClass?: string;
}

interface CustomSelectProps {
  value: string;
  onChange: (newValue: string) => void;
  options: SelectOption[];
  placeholder?: string;
  searchable?: boolean;
  searchPlaceholder?: string;
  disabled?: boolean;
  leadingIcon?: React.ReactNode;
  ariaLabel?: string;
  mono?: boolean;
  hasError?: boolean;
  className?: string;
}

interface MenuCoords {
  top?: number;
  bottom?: number;
  left: number;
  width: number;
  maxHeight: number;
}

export const CustomSelect: React.FC<CustomSelectProps> = ({
  value,
  onChange,
  options,
  placeholder = 'Seleccionar...',
  searchable = false,
  searchPlaceholder = 'Buscar opción...',
  disabled = false,
  leadingIcon,
  ariaLabel,
  mono = false,
  hasError = false,
  className = '',
}) => {
  const [open, setOpen] = useState(false);
  const [search, setSearch] = useState('');
  const [activeIndex, setActiveIndex] = useState<number>(-1);
  const [coords, setCoords] = useState<MenuCoords | null>(null);

  const triggerRef = useRef<HTMLButtonElement | null>(null);
  const menuRef = useRef<HTMLDivElement | null>(null);
  const searchInputRef = useRef<HTMLInputElement | null>(null);

  const showSearch = searchable ?? options.length > 15;

  const selectedOption = useMemo(
    () => options.find((opt) => opt.value === value) ?? null,
    [options, value]
  );

  const filteredOptions = useMemo(() => {
    const q = search.trim().toLowerCase();
    if (!q) return options;
    return options.filter(
      (opt) =>
        opt.label.toLowerCase().includes(q) ||
        (opt.badge ?? '').toLowerCase().includes(q) ||
        opt.value.toLowerCase().includes(q)
    );
  }, [options, search]);

  const updatePosition = () => {
    const el = triggerRef.current;
    if (!el) return;
    const rect = el.getBoundingClientRect();
    const viewportH = window.innerHeight;
    const spaceBelow = viewportH - rect.bottom - 12;
    const spaceAbove = rect.top - 12;
    const openUpward = spaceBelow < 230 && spaceAbove > spaceBelow;

    if (openUpward) {
      setCoords({
        bottom: viewportH - rect.top + 6,
        left: rect.left,
        width: Math.max(rect.width, 160),
        maxHeight: Math.min(280, Math.max(140, spaceAbove - 12)),
      });
    } else {
      setCoords({
        top: rect.bottom + 6,
        left: rect.left,
        width: Math.max(rect.width, 160),
        maxHeight: Math.min(280, Math.max(140, spaceBelow - 12)),
      });
    }
  };

  useLayoutEffect(() => {
    if (!open) return;
    updatePosition();
  }, [open, search]);

  useEffect(() => {
    if (!open) return;

    const currentIdx = filteredOptions.findIndex((o) => o.value === value);
    setActiveIndex(currentIdx >= 0 ? currentIdx : filteredOptions.length > 0 ? 0 : -1);

    if (showSearch) {
      window.requestAnimationFrame(() => searchInputRef.current?.focus());
    }

    const handleOutside = (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        triggerRef.current &&
        !triggerRef.current.contains(target) &&
        menuRef.current &&
        !menuRef.current.contains(target)
      ) {
        setOpen(false);
      }
    };

    const handleScrollOrResize = (e: Event) => {
      if (menuRef.current && e.target instanceof Node && menuRef.current.contains(e.target)) {
        return;
      }
      updatePosition();
    };

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        e.stopPropagation();
        setOpen(false);
        triggerRef.current?.focus();
      } else if (e.key === 'ArrowDown') {
        e.preventDefault();
        setActiveIndex((prev) =>
          filteredOptions.length === 0 ? -1 : (prev + 1) % filteredOptions.length
        );
      } else if (e.key === 'ArrowUp') {
        e.preventDefault();
        setActiveIndex((prev) =>
          filteredOptions.length === 0
            ? -1
            : (prev - 1 + filteredOptions.length) % filteredOptions.length
        );
      } else if (e.key === 'Enter' && activeIndex >= 0 && filteredOptions[activeIndex]) {
        e.preventDefault();
        onChange(filteredOptions[activeIndex].value);
        setOpen(false);
        triggerRef.current?.focus();
      }
    };

    document.addEventListener('mousedown', handleOutside);
    window.addEventListener('scroll', handleScrollOrResize, true);
    window.addEventListener('resize', handleScrollOrResize);
    document.addEventListener('keydown', handleKeyDown, true);

    return () => {
      document.removeEventListener('mousedown', handleOutside);
      window.removeEventListener('scroll', handleScrollOrResize, true);
      window.removeEventListener('resize', handleScrollOrResize);
      document.removeEventListener('keydown', handleKeyDown, true);
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [open, filteredOptions.length]);

  const toggleOpen = () => {
    if (disabled) return;
    setSearch('');
    setOpen((prev) => !prev);
  };

  return (
    <>
      <button
        ref={triggerRef}
        type="button"
        disabled={disabled}
        aria-label={ariaLabel}
        aria-haspopup="listbox"
        aria-expanded={open}
        onClick={toggleOpen}
        className={`input-field flex items-center justify-between gap-2 text-left ${
          hasError ? 'input-field-error' : ''
        } ${open ? 'border-gp-secondary ring-4 ring-gp-secondary/20' : ''} ${
          mono ? 'font-mono' : ''
        } ${className}`}
      >
        <span className="flex min-w-0 items-center gap-2">
          {leadingIcon && <span className="shrink-0 text-gp-secondary">{leadingIcon}</span>}
          {selectedOption?.dotClass && (
            <span className={`h-2 w-2 shrink-0 rounded-full ${selectedOption.dotClass}`} />
          )}
          {selectedOption ? (
            <span className="flex min-w-0 items-center gap-2 truncate">
              <span className="truncate font-medium text-gp-primary">{selectedOption.label}</span>
              {selectedOption.badge && (
                <span className="shrink-0 rounded-md bg-slate-100 px-1.5 py-0.5 text-[11px] font-medium text-gp-muted">
                  {selectedOption.badge}
                </span>
              )}
            </span>
          ) : (
            <span className="truncate text-slate-400">{placeholder}</span>
          )}
        </span>

        <ChevronDown
          className={`h-4 w-4 shrink-0 text-gp-muted transition-transform duration-200 ${
            open ? 'rotate-180 text-gp-secondary' : ''
          }`}
        />
      </button>

      {open &&
        coords &&
        typeof document !== 'undefined' &&
        ReactDOM.createPortal(
          <div
            ref={menuRef}
            role="listbox"
            style={{
              position: 'fixed',
              top: coords.top,
              bottom: coords.bottom,
              left: coords.left,
              width: coords.width,
            }}
            className="z-[90] overflow-hidden rounded-2xl border border-slate-200/95 bg-white shadow-2xl ring-1 ring-slate-950/5 animate-fade-in"
          >
            {showSearch && (
              <div className="border-b border-slate-100 bg-slate-50/80 p-2">
                <div className="relative">
                  <Search className="pointer-events-none absolute left-3 top-1/2 h-3.5 w-3.5 -translate-y-1/2 text-gp-muted" />
                  <input
                    ref={searchInputRef}
                    type="text"
                    value={search}
                    onChange={(e) => setSearch(e.target.value)}
                    placeholder={searchPlaceholder}
                    className="w-full rounded-xl border border-gp-border bg-white py-1.5 pl-8 pr-7 text-xs text-gp-primary placeholder-slate-400 focus:border-gp-secondary focus:outline-none focus:ring-2 focus:ring-gp-secondary/20"
                  />
                  {search && (
                    <button
                      type="button"
                      onClick={() => setSearch('')}
                      aria-label="Limpiar filtro"
                      className="absolute right-2 top-1/2 -translate-y-1/2 rounded-md p-0.5 text-slate-400 hover:bg-slate-100 hover:text-gp-primary"
                    >
                      <X className="h-3 w-3" />
                    </button>
                  )}
                </div>
              </div>
            )}

            <div
              style={{ maxHeight: showSearch ? coords.maxHeight - 48 : coords.maxHeight }}
              className="overflow-y-auto p-1.5"
            >
              {filteredOptions.length === 0 ? (
                <p className="px-3 py-4 text-center text-xs text-gp-muted">
                  Sin resultados para &ldquo;{search}&rdquo;
                </p>
              ) : (
                filteredOptions.map((opt, idx) => {
                  const selected = opt.value === value;
                  const highlighted = idx === activeIndex;
                  return (
                    <button
                      key={opt.value || '__empty__'}
                      type="button"
                      role="option"
                      aria-selected={selected}
                      onMouseEnter={() => setActiveIndex(idx)}
                      onClick={() => {
                        onChange(opt.value);
                        setOpen(false);
                        triggerRef.current?.focus();
                      }}
                      className={`flex w-full items-center justify-between gap-2 rounded-xl px-3 py-2 text-left text-xs transition ${
                        selected
                          ? 'bg-sky-50 font-semibold text-gp-secondary'
                          : highlighted
                          ? 'bg-slate-100/90 text-gp-primary'
                          : 'text-gp-primary hover:bg-slate-50'
                      } ${mono ? 'font-mono' : ''}`}
                    >
                      <span className="flex min-w-0 items-center gap-2">
                        {opt.dotClass && (
                          <span className={`h-2 w-2 shrink-0 rounded-full ${opt.dotClass}`} />
                        )}
                        <span className="truncate">{opt.label}</span>
                        {opt.badge && (
                          <span className="shrink-0 rounded-md bg-slate-100 px-1.5 py-0.5 text-[11px] font-normal text-gp-muted">
                            {opt.badge}
                          </span>
                        )}
                      </span>
                      {selected && <Check className="h-3.5 w-3.5 shrink-0 text-gp-secondary" />}
                    </button>
                  );
                })
              )}
            </div>
          </div>,
          document.body
        )}
    </>
  );
};
