import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_resultado_views.dart';

class HistorialTab extends ConsumerStatefulWidget {
  const HistorialTab({super.key, this.numeroCuentaOverride});

  final String? numeroCuentaOverride;

  @override
  ConsumerState<HistorialTab> createState() => _HistorialTabState();
}

class _HistorialTabState extends ConsumerState<HistorialTab> {
  final _searchCtrl = TextEditingController();
  String _estatusFilter = 'TODOS';
  List<TransaccionHistorialDto>? _items;
  bool _loading = false;
  Object? _error;

  static const _filtros = <({String id, String label})>[
    (id: 'TODOS', label: 'Todas'),
    (id: 'EXITOSA', label: 'Exitosas'),
    (id: 'PENDIENTE', label: 'En conciliación'),
    (id: 'RECHAZADA', label: 'Rechazadas / Revisión'),
  ];

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
  }

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  Future<void> _load() async {
    final cuenta = widget.numeroCuentaOverride ??
        ref.read(sessionControllerProvider).numeroCuentaPrincipal;
    if (cuenta == null || cuenta.isEmpty) return;

    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list =
          await ref.read(pagoRepositoryProvider).consultarHistorial(cuenta);
      if (mounted) setState(() => _items = list);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _abrirDetalle(TransaccionHistorialDto tx) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => TransaccionDetalleScreen(transaccion: tx),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    ref.listen<int>(historyRefreshSignalProvider, (_, _) {
      _load();
    });

    final allItems = _items ?? const <TransaccionHistorialDto>[];
    var exitosas = 0;
    var pendientes = 0;
    var rechazadas = 0;
    var volumenExitoso = 0.0;

    for (final tx in allItems) {
      final est = tx.estatus.toUpperCase();
      if (est == 'EXITOSA') {
        exitosas++;
        volumenExitoso += tx.monto;
      } else if (est == 'PENDIENTE_CONCILIACION' || est == 'PROCESANDO') {
        pendientes++;
      } else {
        rechazadas++;
      }
    }

    final query = _searchCtrl.text.trim().toLowerCase();
    final filtered = allItems.where((tx) {
      final est = tx.estatus.toUpperCase();
      if (_estatusFilter == 'EXITOSA' && est != 'EXITOSA') return false;
      if (_estatusFilter == 'PENDIENTE' &&
          est != 'PENDIENTE_CONCILIACION' &&
          est != 'PROCESANDO') {
        return false;
      }
      if (_estatusFilter == 'RECHAZADA' &&
          (est == 'EXITOSA' ||
              est == 'PENDIENTE_CONCILIACION' ||
              est == 'PROCESANDO')) {
        return false;
      }
      if (query.isEmpty) return true;
      return (tx.servicio ?? '').toLowerCase().contains(query) ||
          (tx.producto ?? '').toLowerCase().contains(query) ||
          tx.referencia.toLowerCase().contains(query) ||
          tx.telefono.toLowerCase().contains(query) ||
          tx.upc.toLowerCase().contains(query) ||
          (tx.numAutorizacion ?? '').toLowerCase().contains(query);
    }).toList();

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
                  const Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Historial de Operaciones',
                          style: TextStyle(
                            fontSize: 18,
                            fontWeight: FontWeight.w800,
                            color: AppColors.primary,
                          ),
                        ),
                        Text(
                          'Comprobantes y seguimiento por folio UPC',
                          style: TextStyle(
                            fontSize: 12,
                            color: AppColors.mutedText,
                          ),
                        ),
                      ],
                    ),
                  ),
                  IconButton.filledTonal(
                    tooltip: 'Actualizar historial',
                    onPressed: _loading ? null : _load,
                    icon: const Icon(Icons.refresh),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              TextField(
                controller: _searchCtrl,
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(
                  hintText:
                      'Buscar por servicio, referencia, teléfono o UPC...',
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
                child: ListView.separated(
                  scrollDirection: Axis.horizontal,
                  itemCount: _filtros.length,
                  separatorBuilder: (_, _) => const SizedBox(width: 8),
                  itemBuilder: (_, i) {
                    final f = _filtros[i];
                    final count = switch (f.id) {
                      'EXITOSA' => exitosas,
                      'PENDIENTE' => pendientes,
                      'RECHAZADA' => rechazadas,
                      _ => allItems.length,
                    };
                    return ChoiceChip(
                      label: Text('${f.label} ($count)'),
                      selected: _estatusFilter == f.id,
                      onSelected: (_) => setState(() => _estatusFilter = f.id),
                    );
                  },
                ),
              ),
            ],
          ),
        ),
        const Divider(height: 1, color: AppColors.border),
        Expanded(
          child: _loading && _items == null
              ? const Center(child: CircularProgressIndicator())
              : _error != null
                  ? Padding(
                      padding: const EdgeInsets.all(16),
                      child: ErrorBanner(error: _error!, onRetry: _load),
                    )
                  : RefreshIndicator(
                      onRefresh: _load,
                      child: ListView(
                        padding: const EdgeInsets.all(16),
                        children: [
                          if (allItems.isNotEmpty) ...[
                            Row(
                              children: [
                                Expanded(
                                  child: _KpiMiniCard(
                                    label: 'Pagado exitoso',
                                    value: formatMxn(volumenExitoso),
                                    color: const Color(0xFF047857),
                                  ),
                                ),
                                const SizedBox(width: 8),
                                Expanded(
                                  child: _KpiMiniCard(
                                    label: 'Operaciones',
                                    value:
                                        '${allItems.length} ($exitosas OK)',
                                    color: AppColors.primary,
                                  ),
                                ),
                              ],
                            ),
                            const SizedBox(height: 12),
                          ],
                          if (filtered.isEmpty)
                            Padding(
                              padding: const EdgeInsets.symmetric(vertical: 40),
                              child: Column(
                                children: [
                                  const Icon(
                                    Icons.receipt_long_outlined,
                                    size: 46,
                                    color: AppColors.mutedText,
                                  ),
                                  const SizedBox(height: 10),
                                  const Text(
                                    'No hay transacciones para mostrar',
                                    style: TextStyle(
                                      fontWeight: FontWeight.w700,
                                      color: AppColors.primary,
                                    ),
                                  ),
                                  if (query.isNotEmpty ||
                                      _estatusFilter != 'TODOS') ...[
                                    const SizedBox(height: 10),
                                    OutlinedButton(
                                      onPressed: () {
                                        _searchCtrl.clear();
                                        setState(
                                          () => _estatusFilter = 'TODOS',
                                        );
                                      },
                                      child: const Text('Limpiar filtros'),
                                    ),
                                  ],
                                ],
                              ),
                            )
                          else
                            ...filtered.map(
                              (tx) => Padding(
                                padding: const EdgeInsets.only(bottom: 10),
                                child: _HistorialCard(
                                  tx: tx,
                                  onTap: () => _abrirDetalle(tx),
                                  onCheckStatus: () =>
                                      Navigator.of(context).push(
                                    MaterialPageRoute<void>(
                                      builder: (_) => PagoConciliacionScreen(
                                        upc: tx.upc,
                                        mensajeInicial: tx.mensajeRespuesta ??
                                            'Consultando estatus...',
                                      ),
                                    ),
                                  ),
                                ),
                              ),
                            ),
                        ],
                      ),
                    ),
        ),
      ],
    );
  }
}

class _KpiMiniCard extends StatelessWidget {
  const _KpiMiniCard({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(fontSize: 11, color: AppColors.mutedText),
            ),
            const SizedBox(height: 3),
            Text(
              value,
              overflow: TextOverflow.ellipsis,
              style: TextStyle(
                fontFamily: 'monospace',
                fontSize: 13.5,
                fontWeight: FontWeight.w800,
                color: color,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _HistorialCard extends StatelessWidget {
  const _HistorialCard({
    required this.tx,
    required this.onTap,
    required this.onCheckStatus,
  });

  final TransaccionHistorialDto tx;
  final VoidCallback onTap;
  final VoidCallback onCheckStatus;

  @override
  Widget build(BuildContext context) {
    final title = tx.producto ?? tx.servicio ?? 'Pago de servicio';
    final refOrTel = tx.referencia.isNotEmpty
        ? tx.referencia
        : (tx.telefono.isNotEmpty ? tx.telefono : 'Sin referencia');

    return InkWell(
      borderRadius: BorderRadius.circular(18),
      onTap: onTap,
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
                    urlLogo: tx.urlLogo,
                    tipoFront: tx.referencia.isNotEmpty ? 2 : 1,
                    size: 44,
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          title,
                          style: const TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 14.5,
                            color: AppColors.primary,
                          ),
                        ),
                        if (tx.servicio != null &&
                            tx.producto != null &&
                            tx.servicio != tx.producto)
                          Text(
                            tx.servicio!,
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppColors.mutedText,
                            ),
                          ),
                      ],
                    ),
                  ),
                  const SizedBox(width: 6),
                  StatusBadge(estatus: tx.estatus),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    formatMxn(tx.monto),
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: AppColors.primary,
                    ),
                  ),
                  Text(
                    formatFecha(tx.fechaCreacion ?? tx.horaLocal),
                    style: const TextStyle(
                      fontSize: 12,
                      color: AppColors.mutedText,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                crossAxisAlignment: WrapCrossAlignment.center,
                children: [
                  Text(
                    'Ref: $refOrTel',
                    style: const TextStyle(
                      fontSize: 12,
                      fontFamily: 'monospace',
                      color: AppColors.mutedText,
                    ),
                  ),
                  CopyChip(
                    value: tx.upc,
                    displayLabel: 'UPC: ${tx.upc}',
                    copyLabel: 'Folio UPC',
                  ),
                  if (tx.numAutorizacion != null &&
                      tx.numAutorizacion!.isNotEmpty)
                    Text(
                      'Aut: ${tx.numAutorizacion}',
                      style: const TextStyle(
                        fontSize: 12,
                        fontFamily: 'monospace',
                        fontWeight: FontWeight.w700,
                        color: AppColors.secondary,
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 6),
              Row(
                mainAxisAlignment: MainAxisAlignment.end,
                children: [
                  TextButton.icon(
                    onPressed: onTap,
                    icon: const Icon(Icons.receipt_long_outlined, size: 16),
                    label: const Text('Comprobante'),
                  ),
                  TextButton.icon(
                    onPressed: onCheckStatus,
                    icon: const Icon(Icons.sync, size: 16),
                    label: const Text('Estatus'),
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
