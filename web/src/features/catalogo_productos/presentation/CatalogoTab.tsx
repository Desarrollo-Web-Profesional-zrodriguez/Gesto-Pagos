import React, { useEffect, useMemo, useState } from 'react';
import {
  ArrowRight,
  Loader2,
  RefreshCw,
  Search,
  Sparkles,
  Tag,
} from 'lucide-react';
import { ErrorBanner, ServiceLogo } from '../../../core/components/UiPrimitives';
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
  if (cat === '1') return 'Recargas y Pines (1)';
  if (cat === '2') return 'Pago de Servicios (2)';
  return cat;
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
    return Object.entries(catalogo.categorias).sort(([a], [b]) => a.localeCompare(b));
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

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-center justify-between gap-4">
        <div>
          <h2 className="text-2xl font-bold text-gp-primary">Catálogo de Servicios y Recargas</h2>
          <p className="text-sm text-gp-muted">
            Productos dinámicos de GestoPago obtenidos en tiempo real desde la API REST
          </p>
        </div>

        <div className="flex items-center gap-2">
          {catalogo?.origen && (
            <span className="inline-flex items-center gap-1.5 rounded-xl border border-gp-border bg-white px-3 py-1.5 text-xs font-semibold text-gp-muted">
              <Sparkles className="h-3.5 w-3.5 text-gp-secondary" />
              {catalogo.origen} · {catalogo.totalProductos} productos
            </span>
          )}
          <button
            type="button"
            onClick={cargarProductos}
            disabled={loading}
            className="btn-secondary"
          >
            <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
            Recargar
          </button>
        </div>
      </div>

      <ErrorBanner error={error} onDismiss={() => setError(null)} />

      {/* Search and Category Chips */}
      <div className="card-surface space-y-4 p-4">
        <div className="relative">
          <Search className="pointer-events-none absolute left-3.5 top-1/2 h-4 w-4 -translate-y-1/2 text-gp-muted" />
          <input
            type="search"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Buscar por servicio, producto, categoría, descripción o ID..."
            className="input-field pl-10"
          />
        </div>

        <div className="flex flex-wrap gap-2">
          <button
            type="button"
            onClick={() => setSelectedCategory('TODAS')}
            className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
              selectedCategory === 'TODAS'
                ? 'bg-gp-primary text-white'
                : 'bg-slate-100 text-gp-muted hover:bg-slate-200 hover:text-gp-primary'
            }`}
          >
            Todas ({catalogo?.totalProductos ?? 0})
          </button>
          {categoriasEntries.map(([cat, items]) => (
            <button
              key={cat}
              type="button"
              onClick={() => setSelectedCategory(cat)}
              className={`rounded-full px-3.5 py-1.5 text-xs font-semibold transition ${
                selectedCategory === cat
                  ? 'bg-gp-secondary text-white'
                  : 'bg-slate-100 text-gp-muted hover:bg-slate-200 hover:text-gp-primary'
              }`}
            >
              {nombreCategoria(cat)} ({items.length})
            </button>
          ))}
        </div>
      </div>

      {/* Product Grid */}
      {loading ? (
        <div className="card-surface flex flex-col items-center justify-center gap-3 py-16 text-gp-muted">
          <Loader2 className="h-8 w-8 animate-spin text-gp-secondary" />
          <p className="text-sm font-medium">Cargando catálogo de GestoPago...</p>
        </div>
      ) : filteredProducts.length === 0 ? (
        <div className="card-surface py-16 text-center text-sm text-gp-muted">
          No se encontraron productos que coincidan con el filtro aplicado.
        </div>
      ) : (
        <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
          {filteredProducts.map(({ categoria, item }) => {
            const fijo = esMontoFijo(item);
            const precio = item.precio ?? 0;
            return (
              <div
                key={`${item.idServicio}-${item.idProducto}`}
                className="card-surface flex flex-col justify-between p-5 transition hover:border-sky-300 hover:shadow-md"
              >
                <div>
                  <div className="flex items-start gap-3.5">
                    <ServiceLogo
                      urlLogo={item.urlLogo}
                      alt={item.servicio}
                      tipoFront={item.tipoFront}
                      size="lg"
                    />
                    <div className="min-w-0 flex-1">
                      <div className="flex flex-wrap items-center gap-1.5">
                        <span className="inline-flex items-center gap-1 rounded-md bg-sky-50 px-2 py-0.5 text-[11px] font-semibold text-gp-secondary">
                          <Tag className="h-3 w-3" />
                          {nombreCategoria(categoria)}
                        </span>
                        <span className="rounded-md bg-slate-100 px-2 py-0.5 text-[11px] font-medium text-gp-muted">
                          {etiquetaFlujo(item)}
                        </span>
                      </div>
                      <h3 className="mt-1.5 truncate text-base font-bold text-gp-primary">
                        {item.producto}
                      </h3>
                      <p className="text-xs font-medium text-gp-muted">{item.servicio}</p>
                    </div>
                  </div>

                  {item.legend && item.legend.trim().length > 0 && (
                    <p className="mt-3 line-clamp-2 rounded-xl bg-slate-50 px-3 py-2 text-xs text-slate-600">
                      {item.legend}
                    </p>
                  )}

                  <div className="mt-3 flex flex-wrap gap-2 text-[11px] font-mono text-gp-muted">
                    <span className="rounded-md border border-gp-border px-2 py-0.5">
                      ID Serv: {item.idServicio}
                    </span>
                    <span className="rounded-md border border-gp-border px-2 py-0.5">
                      ID Prod: {item.idProducto}
                    </span>
                    {item.tipoReferencia && (
                      <span className="rounded-md border border-gp-border px-2 py-0.5">
                        Ref: {item.tipoReferencia}
                      </span>
                    )}
                  </div>
                </div>

                <div className="mt-4 flex items-center justify-between border-t border-gp-border pt-3.5">
                  <div>
                    <p className="text-[11px] uppercase tracking-wide text-gp-muted">
                      {fijo ? 'Precio fijo' : 'Importe'}
                    </p>
                    <p className="text-sm font-bold text-gp-primary">
                      {fijo
                        ? formatMxn(precio)
                        : precio > 0
                        ? `Variable (+${formatMxn(precio)} com.)`
                        : 'Monto abierto'}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={() => onSelectProducto(item)}
                    className="btn-primary px-3.5 py-2 text-xs"
                  >
                    Validar y pagar
                    <ArrowRight className="h-3.5 w-3.5" />
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}
    </div>
  );
};
