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
  String _estatusFilter = 'TODOS';
  List<TransaccionHistorialDto>? _items;
  bool _loading = false;
  Object? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_load);
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

  Color _statusColor(String estatus) {
    switch (estatus) {
      case 'EXITOSA':
        return AppColors.accent;
      case 'RECHAZADA':
        return AppColors.danger;
      default:
        return AppColors.warning;
    }
  }

  String _formatFecha(String? isoOrRaw) {
    if (isoOrRaw == null || isoOrRaw.trim().isEmpty) return 'Sin fecha';
    final parsed = DateTime.tryParse(isoOrRaw.trim());
    if (parsed == null) return isoOrRaw.trim();
    final d = parsed.day.toString().padLeft(2, '0');
    final m = parsed.month.toString().padLeft(2, '0');
    final y = parsed.year.toString();
    final hh = parsed.hour.toString().padLeft(2, '0');
    final mm = parsed.minute.toString().padLeft(2, '0');
    return '$d/$m/$y $hh:$mm';
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
    final filtros = [
      'TODOS',
      'EXITOSA',
      'PENDIENTE_CONCILIACION',
      'RECHAZADA',
      'REVISION_MANUAL',
    ];

    final filtered = (_items ?? const <TransaccionHistorialDto>[])
        .where(
          (tx) =>
              _estatusFilter == 'TODOS' || tx.estatus == _estatusFilter,
        )
        .toList();

    return Column(
      children: [
        Padding(
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 6),
          child: Row(
            children: [
              const Expanded(
                child: Text(
                  'Historial de Pagos',
                  style: TextStyle(fontSize: 17, fontWeight: FontWeight.w800),
                ),
              ),
              IconButton.filledTonal(
                onPressed: _loading ? null : _load,
                icon: const Icon(Icons.refresh),
              ),
            ],
          ),
        ),
        SizedBox(
          height: 40,
          child: ListView.separated(
            padding: const EdgeInsets.symmetric(horizontal: 16),
            scrollDirection: Axis.horizontal,
            itemCount: filtros.length,
            separatorBuilder: (_, _) => const SizedBox(width: 8),
            itemBuilder: (_, i) {
              final f = filtros[i];
              return ChoiceChip(
                label: Text(f, style: const TextStyle(fontSize: 11.5)),
                selected: _estatusFilter == f,
                onSelected: (_) => setState(() => _estatusFilter = f),
              );
            },
          ),
        ),
        const SizedBox(height: 8),
        Expanded(
          child: _loading && _items == null
              ? const Center(child: CircularProgressIndicator())
              : _error != null
                  ? Padding(
                      padding: const EdgeInsets.all(16),
                      child: ErrorBanner(error: _error!, onRetry: _load),
                    )
                  : filtered.isEmpty
                      ? const Center(
                          child: Text('No hay transacciones registradas.'),
                        )
                      : RefreshIndicator(
                          onRefresh: _load,
                          child: ListView.separated(
                            padding: const EdgeInsets.all(16),
                            itemCount: filtered.length,
                            separatorBuilder: (_, _) =>
                                const SizedBox(height: 10),
                            itemBuilder: (_, i) {
                              final tx = filtered[i];
                              final color = _statusColor(tx.estatus);
                              final logo = tx.urlLogo?.trim();
                              return InkWell(
                                borderRadius: BorderRadius.circular(16),
                                onTap: () => _abrirDetalle(tx),
                                child: Card(
                                  child: Padding(
                                    padding: const EdgeInsets.all(14),
                                    child: Column(
                                      crossAxisAlignment:
                                          CrossAxisAlignment.start,
                                      children: [
                                        Row(
                                          children: [
                                            Container(
                                              width: 42,
                                              height: 42,
                                              padding: const EdgeInsets.all(5),
                                              decoration: BoxDecoration(
                                                color: AppColors.background,
                                                borderRadius:
                                                    BorderRadius.circular(10),
                                                border: Border.all(
                                                  color: AppColors.border,
                                                ),
                                              ),
                                              child: (logo != null &&
                                                      logo.startsWith('http'))
                                                  ? Image.network(
                                                      logo,
                                                      fit: BoxFit.contain,
                                                      errorBuilder: (_, _, _) =>
                                                          const Icon(
                                                        Icons.payments_outlined,
                                                        size: 20,
                                                        color:
                                                            AppColors.secondary,
                                                      ),
                                                    )
                                                  : const Icon(
                                                      Icons.payments_outlined,
                                                      size: 20,
                                                      color:
                                                          AppColors.secondary,
                                                    ),
                                            ),
                                            const SizedBox(width: 10),
                                            Expanded(
                                              child: Column(
                                                crossAxisAlignment:
                                                    CrossAxisAlignment.start,
                                                children: [
                                                  Text(
                                                    tx.servicio ??
                                                        'Servicio #${tx.idServicio}',
                                                    style: const TextStyle(
                                                      fontWeight:
                                                          FontWeight.w700,
                                                      fontSize: 14.5,
                                                    ),
                                                  ),
                                                  if (tx.producto != null &&
                                                      tx.producto!.isNotEmpty)
                                                    Text(
                                                      tx.producto!,
                                                      style: const TextStyle(
                                                        fontSize: 12,
                                                        color:
                                                            AppColors.mutedText,
                                                      ),
                                                    ),
                                                ],
                                              ),
                                            ),
                                            Container(
                                              padding:
                                                  const EdgeInsets.symmetric(
                                                horizontal: 8,
                                                vertical: 3,
                                              ),
                                              decoration: BoxDecoration(
                                                color: color.withValues(
                                                  alpha: 0.12,
                                                ),
                                                borderRadius:
                                                    BorderRadius.circular(10),
                                              ),
                                              child: Text(
                                                tx.estatus,
                                                style: TextStyle(
                                                  color: color,
                                                  fontSize: 11,
                                                  fontWeight: FontWeight.w700,
                                                ),
                                              ),
                                            ),
                                          ],
                                        ),
                                        const SizedBox(height: 10),
                                        Row(
                                          mainAxisAlignment:
                                              MainAxisAlignment.spaceBetween,
                                          children: [
                                            Text(
                                              formatMxn(tx.monto),
                                              style: const TextStyle(
                                                fontSize: 18,
                                                fontWeight: FontWeight.w800,
                                              ),
                                            ),
                                            Text(
                                              _formatFecha(
                                                tx.fechaCreacion ?? tx.horaLocal,
                                              ),
                                              style: const TextStyle(
                                                fontSize: 12,
                                                color: AppColors.mutedText,
                                              ),
                                            ),
                                          ],
                                        ),
                                        const SizedBox(height: 6),
                                        Text(
                                          'Ref: ${tx.referencia} • Folio UPC: ${tx.upc}',
                                          style: const TextStyle(
                                            fontSize: 12.5,
                                            fontFamily: 'monospace',
                                            color: AppColors.mutedText,
                                          ),
                                        ),
                                        if (tx.numAutorizacion != null)
                                          Text(
                                            'Autorización: ${tx.numAutorizacion}',
                                            style: const TextStyle(
                                              fontSize: 12.5,
                                              fontWeight: FontWeight.w600,
                                            ),
                                          ),
                                        const SizedBox(height: 6),
                                        Row(
                                          mainAxisAlignment:
                                              MainAxisAlignment.end,
                                          children: [
                                            TextButton.icon(
                                              onPressed: () =>
                                                  _abrirDetalle(tx),
                                              icon: const Icon(
                                                Icons.receipt_long_outlined,
                                                size: 16,
                                              ),
                                              label: const Text('Ver detalle'),
                                            ),
                                            TextButton.icon(
                                              onPressed: () =>
                                                  Navigator.of(context).push(
                                                MaterialPageRoute<void>(
                                                  builder: (_) =>
                                                      PagoConciliacionScreen(
                                                    upc: tx.upc,
                                                    mensajeInicial:
                                                        tx.mensajeRespuesta ??
                                                            'Consultando estatus...',
                                                  ),
                                                ),
                                              ),
                                              icon: const Icon(
                                                Icons.sync,
                                                size: 16,
                                              ),
                                              label: const Text('Estatus'),
                                            ),
                                          ],
                                        ),
                                      ],
                                    ),
                                  ),
                                ),
                              );
                            },
                          ),
                        ),
        ),
      ],
    );
  }
}
