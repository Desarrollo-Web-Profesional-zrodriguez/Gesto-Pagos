import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/catalogo_productos/data/producto_repository.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_screen.dart';

final catalogoProductosProvider =
    FutureProvider<ProductCategorizedResponseDto>(
  (ref) => ref.watch(productoRepositoryProvider).obtenerProductos(),
);

class CatalogoProductosTab extends ConsumerStatefulWidget {
  const CatalogoProductosTab({super.key});

  @override
  ConsumerState<CatalogoProductosTab> createState() =>
      _CatalogoProductosTabState();
}

class _CatalogoProductosTabState extends ConsumerState<CatalogoProductosTab> {
  final _searchCtrl = TextEditingController();
  int? _selectedCategory; // null = Todas las categorías

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  String _labelCategoria(int tipoFront) {
    switch (tipoFront) {
      case 1:
        return 'Recargas y Pines';
      case 2:
        return 'Pago de Servicios';
      case 4:
        return 'Consulta de Referencia';
      case 5:
        return 'Pagos por Referencia';
      case 30:
        return 'Depósitos';
      case 31:
        return 'Retiros';
      case 40:
        return 'Transferencias';
      case 0:
        return 'Otros';
      default:
        return 'Servicios';
    }
  }

  @override
  Widget build(BuildContext context) {
    final catalogoAsync = ref.watch(catalogoProductosProvider);

    return catalogoAsync.when(
      loading: () => const Center(child: CircularProgressIndicator()),
      error: (err, _) => Padding(
        padding: const EdgeInsets.all(16),
        child: ErrorBanner(
          error: err,
          onRetry: () => ref.invalidate(catalogoProductosProvider),
        ),
      ),
      data: (catalogo) {
        final keys = catalogo.categorias.keys.toList()..sort();
        if (keys.isEmpty) {
          return Center(
            child: Padding(
              padding: const EdgeInsets.all(24),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  const Icon(
                    Icons.inventory_2_outlined,
                    size: 48,
                    color: AppColors.mutedText,
                  ),
                  const SizedBox(height: 12),
                  const Text(
                    'No hay servicios disponibles por el momento.',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontWeight: FontWeight.w600),
                  ),
                  const SizedBox(height: 14),
                  FilledButton(
                    onPressed: () => ref.invalidate(catalogoProductosProvider),
                    child: const Text('Recargar catálogo'),
                  ),
                ],
              ),
            ),
          );
        }

        final query = _searchCtrl.text.trim().toLowerCase();
        final List<({int categoria, ProductItemDto item})> products = [];
        for (final k in keys) {
          if (_selectedCategory != null && _selectedCategory != k) continue;
          final catName = _labelCategoria(k).toLowerCase();
          for (final p in catalogo.categorias[k] ?? const <ProductItemDto>[]) {
            if (query.isNotEmpty) {
              final match = p.servicio.toLowerCase().contains(query) ||
                  p.producto.toLowerCase().contains(query) ||
                  (p.legend ?? '').toLowerCase().contains(query) ||
                  catName.contains(query);
              if (!match) continue;
            }
            products.add((categoria: k, item: p));
          }
        }

        final hayFiltros = query.isNotEmpty || _selectedCategory != null;

        return Column(
          children: [
            Container(
              color: Colors.white,
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 12),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            const Text(
                              'Catálogo de Servicios',
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: AppColors.primary,
                              ),
                            ),
                            Text(
                              '${catalogo.totalProductos} servicios y recargas disponibles',
                              style: const TextStyle(
                                fontSize: 12,
                                color: AppColors.mutedText,
                              ),
                            ),
                          ],
                        ),
                      ),
                      IconButton.filledTonal(
                        tooltip: 'Actualizar catálogo',
                        onPressed: () =>
                            ref.invalidate(catalogoProductosProvider),
                        icon: const Icon(Icons.refresh),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  TextField(
                    controller: _searchCtrl,
                    onChanged: (_) => setState(() {}),
                    decoration: InputDecoration(
                      hintText: 'Buscar CFE, Telcel, Netflix, agua o internet...',
                      prefixIcon: const Icon(Icons.search),
                      suffixIcon: _searchCtrl.text.isNotEmpty
                          ? IconButton(
                              tooltip: 'Limpiar búsqueda',
                              onPressed: () {
                                _searchCtrl.clear();
                                setState(() {});
                              },
                              icon: const Icon(Icons.clear),
                            )
                          : null,
                    ),
                  ),
                  const SizedBox(height: 10),
                  SizedBox(
                    height: 38,
                    child: ListView(
                      scrollDirection: Axis.horizontal,
                      children: [
                        ChoiceChip(
                          label: Text('Todas (${catalogo.totalProductos})'),
                          selected: _selectedCategory == null,
                          onSelected: (_) =>
                              setState(() => _selectedCategory = null),
                        ),
                        ...keys.map((k) {
                          final count = catalogo.categorias[k]?.length ?? 0;
                          return Padding(
                            padding: const EdgeInsets.only(left: 8),
                            child: ChoiceChip(
                              label: Text('${_labelCategoria(k)} ($count)'),
                              selected: _selectedCategory == k,
                              onSelected: (_) =>
                                  setState(() => _selectedCategory = k),
                            ),
                          );
                        }),
                      ],
                    ),
                  ),
                  if (hayFiltros) ...[
                    const SizedBox(height: 8),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          '${products.length} resultado${products.length == 1 ? '' : 's'}',
                          style: const TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w600,
                            color: AppColors.mutedText,
                          ),
                        ),
                        TextButton.icon(
                          style: TextButton.styleFrom(
                            visualDensity: VisualDensity.compact,
                          ),
                          onPressed: () {
                            _searchCtrl.clear();
                            setState(() => _selectedCategory = null);
                          },
                          icon: const Icon(Icons.close, size: 15),
                          label: const Text('Restablecer filtros'),
                        ),
                      ],
                    ),
                  ],
                ],
              ),
            ),
            const Divider(height: 1, color: AppColors.border),
            Expanded(
              child: products.isEmpty
                  ? Center(
                      child: Padding(
                        padding: const EdgeInsets.all(24),
                        child: Column(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(
                              Icons.search_off_outlined,
                              size: 46,
                              color: AppColors.mutedText,
                            ),
                            const SizedBox(height: 10),
                            const Text(
                              'No encontramos servicios con ese criterio',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontWeight: FontWeight.w700,
                                color: AppColors.primary,
                              ),
                            ),
                            const SizedBox(height: 4),
                            const Text(
                              'Prueba buscando por nombre comercial o selecciona otra categoría.',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 12.5,
                                color: AppColors.mutedText,
                              ),
                            ),
                            if (hayFiltros) ...[
                              const SizedBox(height: 14),
                              OutlinedButton(
                                onPressed: () {
                                  _searchCtrl.clear();
                                  setState(() => _selectedCategory = null);
                                },
                                child: const Text('Mostrar catálogo completo'),
                              ),
                            ],
                          ],
                        ),
                      ),
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 12, 16, 24),
                      itemCount: products.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final entry = products[index];
                        return _ProductoCard(
                          producto: entry.item,
                          categoriaNombre: _labelCategoria(entry.categoria),
                          onTap: () => Navigator.of(context).push(
                            MaterialPageRoute<void>(
                              builder: (_) => PagoScreen(producto: entry.item),
                            ),
                          ),
                        );
                      },
                    ),
            ),
          ],
        );
      },
    );
  }
}

class _ProductoCard extends StatelessWidget {
  const _ProductoCard({
    required this.producto,
    required this.categoriaNombre,
    required this.onTap,
  });

  final ProductItemDto producto;
  final String categoriaNombre;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final badgeColor = producto.esPinOEntretenimiento
        ? AppColors.violet
        : producto.esPagoServicio
            ? const Color(0xFF047857)
            : AppColors.secondary;

    final precioLabel = producto.esPrecioFijo
        ? 'Importe fijo'
        : (producto.precio != null && producto.precio! > 0)
            ? 'Monto libre + comisión'
            : 'Importe';

    final precioValue = producto.esPagoServicio
        ? (producto.precio != null && producto.precio! > 0
            ? 'Comisión ${formatMxn(producto.precio)}'
            : 'Monto abierto')
        : (producto.precio != null && producto.precio! > 0
            ? formatMxn(producto.precio)
            : 'Monto variable');

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  ServiceLogo(
                    urlLogo: producto.urlLogo,
                    tipoFront: producto.tipoFront,
                    size: 52,
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 8,
                            vertical: 3,
                          ),
                          decoration: BoxDecoration(
                            color: badgeColor.withValues(alpha: 0.1),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            producto.etiquetaFlujo,
                            style: TextStyle(
                              fontSize: 10.5,
                              fontWeight: FontWeight.w700,
                              color: badgeColor,
                            ),
                          ),
                        ),
                        const SizedBox(height: 5),
                        Text(
                          producto.producto,
                          style: const TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 14.5,
                            color: AppColors.primary,
                          ),
                        ),
                        Text(
                          '${producto.servicio} · $categoriaNombre',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.mutedText,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              if (producto.legend != null &&
                  producto.legend!.trim().isNotEmpty) ...[
                const SizedBox(height: 10),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: const Color(0xFFF8FAFC),
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: const Color(0xFFF1F5F9)),
                  ),
                  child: Text(
                    producto.legend!.trim(),
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: const TextStyle(
                      fontSize: 11.5,
                      color: Color(0xFF475569),
                    ),
                  ),
                ),
              ],
              if (producto.tipoReferencia != null &&
                  producto.tipoReferencia!.trim().isNotEmpty) ...[
                const SizedBox(height: 8),
                Text(
                  'Referencia requerida: ${producto.tipoReferencia!.trim()}',
                  style: const TextStyle(
                    fontSize: 11.5,
                    color: AppColors.mutedText,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
              const Divider(height: 20, color: Color(0xFFF1F5F9)),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        precioLabel.toUpperCase(),
                        style: const TextStyle(
                          fontSize: 10,
                          fontWeight: FontWeight.w700,
                          color: AppColors.mutedText,
                        ),
                      ),
                      const SizedBox(height: 2),
                      Text(
                        precioValue,
                        style: const TextStyle(
                          fontSize: 14.5,
                          fontWeight: FontWeight.w800,
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                  ),
                  FilledButton(
                    style: FilledButton.styleFrom(
                      minimumSize: const Size(100, 38),
                      padding: const EdgeInsets.symmetric(horizontal: 16),
                    ),
                    onPressed: onTap,
                    child: const Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Text('Pagar'),
                        SizedBox(width: 5),
                        Icon(Icons.arrow_forward_rounded, size: 15),
                      ],
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
      ),
    );
  }
}
