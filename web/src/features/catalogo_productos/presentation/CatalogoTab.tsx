import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  CreditCard,
  RefreshCw,
  Search,
  Smartphone,
  Sparkles,
  Store,
  Ticket,
  X,
} from 'lucide-react';
import {
  EmptyState,
  ErrorBanner,
  ServiceLogo,
  SkeletonBlock,
} from '../../../core/components/UiPrimitives';
import { formatMxn } from '../../../core/utils/formatters';
import {
  esMontoFijo,
  etiquetaFlujo,
  ProductCategorizedResponseDto,
  ProductItemDto,
  productoRepository,
} from '../data/productoRepository';

interface CatalogoTabProps {
  onSelectProducto: (producto: ProductItemDto) => void;
}

function nombreCategoria(cat: string): string {
  switch (cat) {
    case '1':
      return 'Recargas y Pines';
    case '2':
      return 'Pago de Servicios';
    case '4':
      return 'Consulta de Referencia';
    case '5':
      return 'Pagos por Referencia';
    case '30':
      return 'Depósitos';
    case '31':
      return 'Retiros';
    case '40':
      return 'Transferencias';
    default:
      return `Categoría ${cat}`;
  }
}

function estiloFlujo(item: ProductItemDto): {
  badge: string;
  Icon: React.FC<{ className?: string }>;
} {
  const esPin = item.idCatTipoServicio === 10 || item.idCatTipoServicio === 11;
  if (item.tipoFront === 1 && esPin) {
    return {
      badge: 'bg-violet-50 text-violet-700 ring-1 ring-inset ring-violet-200/80',
      Icon: Ticket,
    };
  }
  if (item.tipoFront === 1) {
    return {
      badge: 'bg-sky-50 text-sky-700 ring-1 ring-inset ring-sky-200/80',
      Icon: Smartphone,
    };
  }
  return {
    badge: 'bg-emerald-50 text-emerald-700 ring-1 ring-inset ring-emerald-200/80',
    Icon: CreditCard,
  };
}

export const CatalogoTab: React.FC<CatalogoTabProps> = ({ onSelectProducto }) => {
  const [catalogo, setCatalogo] = useState<ProductCategorizedResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [selectedCategory, setSelectedCategory] = useState<string>('TODAS');
  const [search, setSearch] = useState('');

  const cargarProductos = async () => {
    setLoading(true);
    setError(null);
    try {
      const data = await productoRepository.obtenerProductos();
      setCatalogo(data);
    } catch (err) {
      setError(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    void cargarProductos();
  }, []);

  const categoriasEntries = useMemo(() => {
    if (!catalogo?.categorias) return [];
    return Object.entries(catalogo.categorias).sort(([a], [b]) => Number(a) - Number(b));
  }, [catalogo]);

  const filteredProducts = useMemo(() => {
    const query = search.trim().toLowerCase();
    const list: Array<{ categoria: string; item: ProductItemDto }> = [];

    for (const [cat, items] of categoriasEntries) {
      if (selectedCategory !== 'TODAS' && cat !== selectedCategory) {
        continue;
      }
      for (const item of items) {
        if (query) {
          const matches =
            item.servicio.toLowerCase().includes(query) ||
            item.producto.toLowerCase().includes(query) ||
            (item.legend ?? '').toLowerCase().includes(query) ||
            cat.toLowerCase().includes(query) ||
            nombreCategoria(cat).toLowerCase().includes(query) ||
            String(item.idServicio).includes(query) ||
            String(item.idProducto).includes(query);
          if (!matches) continue;
        }
        list.push({ categoria: cat, item });
      }
    }
    return list;
  }, [categoriasEntries, search, selectedCategory]);

  const hayFiltros = search.trim().length > 0 || selectedCategory !== 'TODAS';

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-extrabold tracking-tight text-gp-primary">
            Catálogo de Servicios y Recargas
          </h2>
          <p className="mt-0.5 text-sm text-gp-muted">
            Selecciona un proveedor para validar tu referencia o número celular antes del cargo
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          {catalogo && (
            <span className="inline-flex items-center gap-1.5 rounded-xl border border-gp-border bg-white px-3 py-2 text-xs font-semibold text-gp-muted shadow-sm">
              <Sparkles className="h-3.5 w-3.5 text-gp-secondary" />
              <span className="font-mono text-gp-primary tabular-nums">
                {catalogo.totalProductos}
              </span>
              <span>servicios disponibles</span>
            </span>
          )}
          <button
            type="button"
            onClick={cargarProductos}
            disabled={loading}
            className="btn-secondary"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            <span>Actualizar</span>
          </button>
        </div>
      </div>

      <ErrorBanner error={error} onDismiss={() => setError(null)} />

      {/* Search + Category Filter Bar */}
      <div className="card-surface space-y-4 p-4 sm:p-5">
        <div className="flex flex-col gap-3 sm:flex-row sm:items-center">
          <div className="relative flex-1">
            <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
            <input
              type="text"
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              placeholder="Buscar CFE, Telcel, Netflix, agua, internet o número de servicio..."
              className="input-field pl-10 pr-9"
            />
            {search && (
              <button
                type="button"
                onClick={() => setSearch('')}
                aria-label="Limpiar texto de búsqueda"
                className="absolute right-2.5 top-1/2 -translate-y-1/2 rounded-lg p-1 text-gp-muted hover:bg-slate-100 hover:text-gp-primary"
              >
                <X className="h-4 w-4" />
              </button>
            )}
          </div>
          {hayFiltros && (
            <div className="flex items-center justify-between gap-2.5 sm:justify-end">
              <span className="font-mono text-xs font-semibold text-gp-muted tabular-nums">
                {filteredProducts.length} resultado{filteredProducts.length === 1 ? '' : 's'}
              </span>
              <button
                type="button"
                onClick={() => {
                  setSearch('');
                  setSelectedCategory('TODAS');
                }}
                className="inline-flex items-center gap-1 rounded-xl border border-slate-200 bg-slate-50 px-3 py-2 text-xs font-semibold text-gp-primary transition hover:bg-slate-100"
              >
                <X className="h-3.5 w-3.5" />
                Restablecer filtros
              </button>
            </div>
          )}
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setSelectedCategory('TODAS')}
            className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
              selectedCategory === 'TODAS'
                ? 'bg-slate-950 text-white shadow-sm'
                : 'bg-slate-100 text-gp-muted hover:bg-slate-200/80 hover:text-gp-primary'
            }`}
          >
            <span>Todas las categorías</span>
            <span
              className={`rounded-full px-1.5 py-0.5 font-mono text-[10px] tabular-nums ${
                selectedCategory === 'TODAS' ? 'bg-white/20 text-white' : 'bg-white text-gp-muted'
              }`}
            >
              {catalogo?.totalProductos ?? 0}
            </span>
          </button>
          {categoriasEntries.map(([cat, items]) => {
            const active = selectedCategory === cat;
            return (
              <button
                key={cat}
                type="button"
                onClick={() => setSelectedCategory(cat)}
                className={`inline-flex items-center gap-1.5 rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
                  active
                    ? 'bg-gp-secondary text-white shadow-sm'
                    : 'bg-slate-100 text-gp-muted hover:bg-slate-200/80 hover:text-gp-primary'
                }`}
              >
                <span>{nombreCategoria(cat)}</span>
                <span
                  className={`rounded-full px-1.5 py-0.5 font-mono text-[10px] tabular-nums ${
                    active ? 'bg-white/20 text-white' : 'bg-white text-gp-muted'
                  }`}
                >
                  {items.length}
                </span>
              </button>
            );
          })}
        </div>
      </div>

      {/* Product Grid */}
      {loading ? (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {Array.from({ length: 6 }).map((_, idx) => (
            <SkeletonBlock key={idx} className="h-56" />
          ))}
        </div>
      ) : filteredProducts.length === 0 ? (
        <EmptyState
          icon={Store}
          title="No encontramos servicios con ese criterio"
          description="Prueba buscando por nombre comercial del proveedor o selecciona otra categoría."
          action={
            hayFiltros ? (
              <button
                type="button"
                onClick={() => {
                  setSearch('');
                  setSelectedCategory('TODAS');
                }}
                className="btn-secondary text-xs"
              >
                Mostrar catálogo completo
              </button>
            ) : undefined
          }
        />
      ) : (
        <div className="grid gap-4 sm:grid-cols-2 xl:grid-cols-3">
          {filteredProducts.map(({ categoria, item }) => {
            const fijo = esMontoFijo(item);
            const precio = item.precio ?? 0;
            const { badge, Icon: FlowIcon } = estiloFlujo(item);

            return (
              <article
                key={`${item.idServicio}-${item.idProducto}`}
                className="group card-interactive flex flex-col justify-between p-5"
              >
                <div>
                  {/* Card Top: Logo + Flow Pill + Title */}
                  <div className="flex items-start gap-3.5">
                    <div className="shrink-0 transition-transform duration-200 group-hover:scale-105">
                      <ServiceLogo
                        urlLogo={item.urlLogo}
                        alt={item.servicio}
                        tipoFront={item.tipoFront}
                        size="lg"
                      />
                    </div>
                    <div className="min-w-0 flex-1">
                      <span
                        className={`inline-flex items-center gap-1 rounded-full px-2.5 py-0.5 text-[11px] font-semibold ${badge}`}
                      >
                        <FlowIcon className="h-3 w-3" />
                        {etiquetaFlujo(item)}
                      </span>
                      <h3
                        className="mt-1.5 truncate text-[15px] font-bold leading-snug text-gp-primary transition-colors group-hover:text-gp-secondary"
                        title={item.producto}
                      >
                        {item.producto}
                      </h3>
                      <p className="truncate text-xs font-medium text-gp-muted" title={item.servicio}>
                        {item.servicio} · {nombreCategoria(categoria)}
                      </p>
                    </div>
                  </div>

                  {/* Description / Legend */}
                  {item.legend && item.legend.trim().length > 0 && (
                    <div
                      className="mt-3.5 rounded-xl border border-slate-100 bg-slate-50/80 px-3 py-2.5"
                      title={item.legend}
                    >
                      <p
                        className="overflow-hidden text-xs leading-relaxed text-slate-600"
                        style={{
                          display: '-webkit-box',
                          WebkitLineClamp: 2,
                          WebkitBoxOrient: 'vertical',
                        }}
                      >
                        {item.legend}
                      </p>
                    </div>
                  )}

                  {/* Reference info */}
                  {item.tipoReferencia && (
                    <div className="mt-3 flex items-center gap-1.5 text-[11px] font-medium text-slate-400">
                      <span>Referencia requerida:</span>
                      <span className="font-semibold text-slate-600">{item.tipoReferencia}</span>
                    </div>
                  )}
                </div>

                {/* Price + CTA */}
                <div className="mt-4 flex items-end justify-between gap-3 border-t border-slate-100 pt-3.5">
                  <div className="min-w-0">
                    <p className="text-[10px] font-semibold uppercase tracking-wider text-gp-muted">
                      {fijo ? 'Importe fijo' : precio > 0 ? 'Monto libre + comisión' : 'Importe'}
                    </p>
                    <p className="mt-0.5 truncate font-display text-base font-extrabold tracking-tight text-gp-primary tabular-nums">
                      {fijo
                        ? formatMxn(precio)
                        : precio > 0
                        ? `Comisión ${formatMxn(precio)}`
                        : 'Monto abierto'}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => onSelectProducto(item)}
                    className="btn-primary shrink-0 px-3.5 py-2 text-xs group-hover:bg-gp-secondary"
                  >
                    <span>Pagar</span>
                    <ArrowRight className="h-3.5 w-3.5 transition-transform duration-200 group-hover:translate-x-0.5" />
                  </button>
                </div>
              </article>
            );
          })}
        </div>
      )}
    </div>
  );
};
