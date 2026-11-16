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
  int? _selectedCategory;

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  String _labelCategoria(int tipoFront) {
    switch (tipoFront) {
      case 1:
        return 'Recargas y Pines (1)';
      case 2:
        return 'Pago de Servicios (2)';
      case 0:
        return 'Otros (0)';
      default:
        return 'Categoría $tipoFront';
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
        final keys = catalogo.categorias.keys.toList();
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
                  Text(catalogo.message, textAlign: TextAlign.center),
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

        final activeKey = (_selectedCategory != null &&
                catalogo.categorias.containsKey(_selectedCategory))
            ? _selectedCategory!
            : keys.first;

        final query = _searchCtrl.text.trim().toLowerCase();
        final products = (catalogo.categorias[activeKey] ?? const [])
            .where((p) {
              if (query.isEmpty) return true;
              return p.servicio.toLowerCase().contains(query) ||
                  p.producto.toLowerCase().contains(query) ||
                  p.idServicio.toString().contains(query) ||
                  p.idProducto.toString().contains(query);
            })
            .toList();

        return Column(
          children: [
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
              child: Column(
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: _searchCtrl,
                          onChanged: (_) => setState(() {}),
                          decoration: InputDecoration(
                            hintText: 'Buscar servicio o producto...',
                            prefixIcon: const Icon(Icons.search),
                            suffixIcon: _searchCtrl.text.isNotEmpty
                                ? IconButton(
                                    onPressed: () {
                                      _searchCtrl.clear();
                                      setState(() {});
                                    },
                                    icon: const Icon(Icons.clear),
                                  )
                                : null,
                          ),
                        ),
                      ),
                      const SizedBox(width: 8),
                      IconButton.filledTonal(
                        tooltip: 'Actualizar catálogo',
                        onPressed: () =>
                            ref.invalidate(catalogoProductosProvider),
                        icon: const Icon(Icons.refresh),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  Row(
                    children: [
                      Chip(
                        avatar: const Icon(Icons.storage, size: 16),
                        label: Text(
                          '${catalogo.origen} • ${catalogo.totalProductos} productos',
                          style: const TextStyle(fontSize: 11.5),
                        ),
                        visualDensity: VisualDensity.compact,
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  SizedBox(
                    height: 38,
                    child: ListView.separated(
                      scrollDirection: Axis.horizontal,
                      itemCount: keys.length,
                      separatorBuilder: (_, _) => const SizedBox(width: 8),
                      itemBuilder: (context, index) {
                        final k = keys[index];
                        final count = catalogo.categorias[k]?.length ?? 0;
                        return ChoiceChip(
                          label: Text('${_labelCategoria(k)} ($count)'),
                          selected: activeKey == k,
                          onSelected: (_) =>
                              setState(() => _selectedCategory = k),
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
            Expanded(
              child: products.isEmpty
                  ? const Center(
                      child: Text(
                        'No hay productos que coincidan con tu búsqueda.',
                      ),
                    )
                  : ListView.separated(
                      padding: const EdgeInsets.fromLTRB(16, 8, 16, 24),
                      itemCount: products.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 10),
                      itemBuilder: (context, index) {
                        final p = products[index];
                        return _ProductoCard(
                          producto: p,
                          onTap: () => Navigator.of(context).push(
                            MaterialPageRoute<void>(
                              builder: (_) => PagoScreen(producto: p),
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
    required this.onTap,
  });

  final ProductItemDto producto;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final badgeText = producto.esPinOEntretenimiento
        ? 'PIN / Entretenimiento'
        : producto.esPagoServicio
            ? 'Servicio (monto abierto)'
            : 'Recarga fija';

    final precioText = producto.esPagoServicio
        ? (producto.precio != null && producto.precio! > 0
            ? 'Comisión: ${formatMxn(producto.precio)}'
            : 'Monto abierto')
        : (producto.precio != null && producto.precio! > 0
            ? formatMxn(producto.precio)
            : 'Monto variable');

    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(14),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Row(
            children: [
              _LogoThumbnail(url: producto.urlLogo),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      producto.servicio,
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 14,
                      ),
                    ),
                    Text(
                      producto.producto,
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.mutedText,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      'ID Servicio: ${producto.idServicio} • ID Producto: ${producto.idProducto}',
                      style: const TextStyle(
                        fontSize: 11,
                        fontFamily: 'monospace',
                        color: AppColors.mutedText,
                      ),
                    ),
                    if (producto.legend != null &&
                        producto.legend!.trim().isNotEmpty) ...[
                      const SizedBox(height: 4),
                      Text(
                        producto.legend!.trim(),
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 11.5,
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 6,
                      runSpacing: 4,
                      children: [
                        _MiniTag(badgeText, AppColors.secondary),
                        _MiniTag(precioText, AppColors.accent),
                      ],
                    ),
                  ],
                ),
              ),
              const Icon(Icons.chevron_right, color: AppColors.mutedText),
            ],
          ),
        ),
      ),
    );
  }
}

class _LogoThumbnail extends StatelessWidget {
  const _LogoThumbnail({this.url});
  final String? url;

  @override
  Widget build(BuildContext context) {
    final clean = url?.trim();
    return Container(
      width: 48,
      height: 48,
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: AppColors.background,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.border),
      ),
      child: (clean != null && clean.startsWith('http'))
          ? Image.network(
              clean,
              fit: BoxFit.contain,
              errorBuilder: (_, _, _) => const Icon(
                Icons.payments_outlined,
                color: AppColors.secondary,
              ),
            )
          : const Icon(Icons.payments_outlined, color: AppColors.secondary),
    );
  }
}

class _MiniTag extends StatelessWidget {
  const _MiniTag(this.text, this.color);
  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        text,
        style: TextStyle(
          fontSize: 11,
          fontWeight: FontWeight.w700,
          color: color,
        ),
      ),
    );
  }
}
