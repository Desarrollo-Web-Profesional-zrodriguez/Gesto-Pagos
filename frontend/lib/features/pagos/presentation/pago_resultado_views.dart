import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_screen.dart';

class PagoComprobanteScreen extends ConsumerStatefulWidget {
  const PagoComprobanteScreen({
    super.key,
    required this.comprobante,
    this.tipoFront,
    this.destinatarioDefault,
    this.nombreDefault,
  });

  final PagoServicioResponseDto comprobante;
  final int? tipoFront;
  final String? destinatarioDefault;
  final String? nombreDefault;

  @override
  ConsumerState<PagoComprobanteScreen> createState() =>
      _PagoComprobanteScreenState();
}

class _PagoComprobanteScreenState
    extends ConsumerState<PagoComprobanteScreen> {
  bool _resending = false;

  Future<void> _reenviarComprobante() async {
    final emailCtrl = TextEditingController(
      text: widget.destinatarioDefault ?? '',
    );
    final formKey = GlobalKey<FormState>();
    final confirmed = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text('Enviar comprobante por correo'),
        content: Form(
          key: formKey,
          child: TextFormField(
            controller: emailCtrl,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(
              labelText: 'Correo destino',
              hintText: 'cliente@dominio.com',
            ),
            validator: (v) => BackendValidators.email(v),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(minimumSize: const Size(100, 42)),
            onPressed: () {
              if (formKey.currentState?.validate() ?? false) {
                Navigator.of(ctx).pop(emailCtrl.text.trim());
              }
            },
            child: const Text('Enviar'),
          ),
        ],
      ),
    );
    emailCtrl.dispose();
    if (confirmed == null || !mounted) return;

    setState(() => _resending = true);
    try {
      final c = widget.comprobante;
      final res = await ref.read(pagoRepositoryProvider).enviarComprobante(
            ComprobanteEmailDto(
              destinatarioEmail: confirmed,
              destinatarioNombre: widget.nombreDefault,
              numAutorizacion: c.numAutorizacion,
              idTx: c.idTx,
              upc: c.upc,
              numCuenta: c.numCuenta,
              idServicio: c.idServicio,
              servicio: c.servicio,
              idProducto: c.idProducto,
              producto: c.producto,
              telefono: c.telefono,
              referencia: c.referencia,
              montoPagado: c.montoPagado,
              comision: c.comision,
              pin: c.pin,
              legend: c.legend,
              fechaProveedor: c.fechaProveedor,
              horaLocal: c.horaLocal,
              logoUrl: c.urlLogo,
              tipoFront: widget.tipoFront,
            ),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text(
            res.enviado
                ? res.mensaje
                : '${res.mensaje} ${res.errorDetalle ?? ''}'.trim(),
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text('No fue posible enviar el comprobante: $e'),
        ),
      );
    } finally {
      if (mounted) setState(() => _resending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = widget.comprobante;
    final refOTel = (c.referencia != null && c.referencia!.isNotEmpty)
        ? c.referencia!
        : (c.telefono != null && c.telefono!.isNotEmpty ? c.telefono! : '-');

    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        title: const Text('Comprobante Digital'),
      ),
      body: Column(
        children: [
          const PagoPhaseProgressBar(activeIndex: 2),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                // Comprobante estilo ticket
                Container(
                  decoration: BoxDecoration(
                    color: Colors.white,
                    borderRadius: BorderRadius.circular(22),
                    border: Border.all(
                      color: AppColors.accent.withValues(alpha: 0.4),
                    ),
                    boxShadow: [
                      BoxShadow(
                        color: AppColors.primary.withValues(alpha: 0.06),
                        blurRadius: 16,
                        offset: const Offset(0, 6),
                      ),
                    ],
                  ),
                  child: Column(
                    children: [
                      Container(
                        width: double.infinity,
                        padding: const EdgeInsets.all(20),
                        decoration: const BoxDecoration(
                          color: AppColors.emeraldSoft,
                          borderRadius: BorderRadius.vertical(
                            top: Radius.circular(21),
                          ),
                        ),
                        child: Column(
                          children: [
                            const CircleAvatar(
                              radius: 26,
                              backgroundColor: AppColors.accent,
                              child: Icon(
                                Icons.check,
                                color: Colors.white,
                                size: 30,
                              ),
                            ),
                            const SizedBox(height: 10),
                            const Text(
                              '¡Pago Aplicado Exitosamente!',
                              textAlign: TextAlign.center,
                              style: TextStyle(
                                fontSize: 18,
                                fontWeight: FontWeight.w800,
                                color: Color(0xFF065F46),
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              c.mensaje.isNotEmpty
                                  ? c.mensaje
                                  : '${c.servicio} · ${c.producto}',
                              textAlign: TextAlign.center,
                              style: const TextStyle(
                                color: Color(0xFF047857),
                                fontSize: 12.5,
                              ),
                            ),
                            const SizedBox(height: 12),
                            Text(
                              formatMxn(c.montoPagado),
                              style: const TextStyle(
                                color: AppColors.primary,
                                fontSize: 30,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const _DashedTicketDivider(),
                      Padding(
                        padding: const EdgeInsets.all(18),
                        child: Column(
                          children: [
                            _TicketCopyRow(
                              label: 'Folio UPC',
                              value: c.upc,
                              copyLabel: 'Folio UPC',
                            ),
                            _TicketCopyRow(
                              label: 'Autorización',
                              value: c.numAutorizacion,
                              copyLabel: 'Número de autorización',
                            ),
                            if (c.idTx != null && c.idTx!.isNotEmpty)
                              _ReceiptRow('ID de operación', c.idTx!),
                            _ReceiptRow(
                              'Servicio',
                              '${c.servicio} · ${c.producto}',
                            ),
                            _ReceiptRow('Cuenta de cargo', maskCuenta(c.numCuenta)),
                            _ReceiptRow('Referencia / Teléfono', refOTel),
                            if (c.comision != null && c.comision! > 0)
                              _ReceiptRow('Comisión', formatMxn(c.comision)),
                            if (c.saldoDisponibleActual != null)
                              _ReceiptRow(
                                'Nuevo saldo disponible',
                                formatMxn(c.saldoDisponibleActual),
                              ),
                            if (c.fechaProveedor != null)
                              _ReceiptRow('Fecha de aplicación', c.fechaProveedor!),
                            _ReceiptRow(
                              'Comprobante por correo',
                              c.emailEnviado == true
                                  ? 'Enviado correctamente'
                                  : 'Disponible para reenvío',
                            ),
                          ],
                        ),
                      ),
                    ],
                  ),
                ),
                if (c.pin != null && c.pin!.isNotEmpty) ...[
                  const SizedBox(height: 14),
                  Container(
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppColors.amberSoft,
                      borderRadius: BorderRadius.circular(18),
                      border: Border.all(
                        color: AppColors.warning.withValues(alpha: 0.45),
                      ),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            const Text(
                              'PIN Digital',
                              style: TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 14,
                                color: Color(0xFF78350F),
                              ),
                            ),
                            CopyChip(
                              value: c.pin!,
                              copyLabel: 'PIN Digital',
                            ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        const Text(
                          'Guarda este PIN ahora. Por seguridad, no vuelve a mostrarse en el historial.',
                          style: TextStyle(
                            fontSize: 11.5,
                            color: Color(0xFF92400E),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
                if (c.legend != null && c.legend!.isNotEmpty) ...[
                  const SizedBox(height: 12),
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(14),
                      child: Text(
                        c.legend!,
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.mutedText,
                        ),
                      ),
                    ),
                  ),
                ],
                const SizedBox(height: 16),
                OutlinedButton.icon(
                  onPressed: _resending ? null : _reenviarComprobante,
                  icon: _resending
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.email_outlined),
                  label: const Text('Enviar o reenviar comprobante por correo'),
                ),
                const SizedBox(height: 10),
                FilledButton(
                  onPressed: () =>
                      Navigator.of(context).popUntil((r) => r.isFirst),
                  child: const Text('Finalizar y volver al inicio'),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DashedTicketDivider extends StatelessWidget {
  const _DashedTicketDivider();

  @override
  Widget build(BuildContext context) {
    return LayoutBuilder(
      builder: (_, constraints) {
        final count = (constraints.maxWidth / 10).floor().clamp(10, 60);
        return Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: List.generate(
            count,
            (_) => Container(
              width: 5,
              height: 1.5,
              color: AppColors.accent.withValues(alpha: 0.35),
            ),
          ),
        );
      },
    );
  }
}

class _TicketCopyRow extends StatelessWidget {
  const _TicketCopyRow({
    required this.label,
    required this.value,
    required this.copyLabel,
  });

  final String label;
  final String? value;
  final String copyLabel;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: const TextStyle(fontSize: 13, color: AppColors.mutedText),
            ),
          ),
          Expanded(
            flex: 3,
            child: Align(
              alignment: Alignment.centerRight,
              child: (value != null && value!.trim().isNotEmpty)
                  ? CopyChip(value: value, copyLabel: copyLabel)
                  : const Text(
                      'No disponible',
                      style: TextStyle(fontSize: 13, color: AppColors.mutedText),
                    ),
            ),
          ),
        ],
      ),
    );
  }
}

class PagoConciliacionScreen extends ConsumerStatefulWidget {
  const PagoConciliacionScreen({
    super.key,
    required this.upc,
    required this.mensajeInicial,
  });

  final String upc;
  final String mensajeInicial;

  @override
  ConsumerState<PagoConciliacionScreen> createState() =>
      _PagoConciliacionScreenState();
}

class _PagoConciliacionScreenState
    extends ConsumerState<PagoConciliacionScreen> {
  Timer? _pollingTimer;
  PagoEstatusDto? _estatus;
  bool _loading = false;
  Object? _error;

  @override
  void initState() {
    super.initState();
    _consultar();
    _pollingTimer = Timer.periodic(
      const Duration(seconds: 8),
      (_) => _consultar(),
    );
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }

  Future<void> _consultar() async {
    if (_loading) return;
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final res = await ref
          .read(pagoRepositoryProvider)
          .consultarEstatus(widget.upc);
      if (!mounted) return;
      setState(() => _estatus = res);
      if (res.definitivo || res.estatus == 'REVISION_MANUAL') {
        _pollingTimer?.cancel();
        try {
          await ref
              .read(sessionControllerProvider.notifier)
              .refrescarSaldoActual();
        } catch (_) {}
        ref.read(historyRefreshSignalProvider.notifier).bump();
      }
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final est = _estatus;
    final estadoRaw = est?.estatus ?? 'PENDIENTE_CONCILIACION';
    final isSuccess = estadoRaw == 'EXITOSA';
    final isRejected = estadoRaw == 'RECHAZADA';

    final color = isSuccess
        ? AppColors.accent
        : isRejected
            ? AppColors.danger
            : AppColors.warning;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        title: const Text('Seguimiento de Operación'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                children: [
                  Icon(
                    isSuccess
                        ? Icons.check_circle_outline
                        : isRejected
                            ? Icons.cancel_outlined
                            : Icons.hourglass_top_rounded,
                    color: color,
                    size: 54,
                  ),
                  const SizedBox(height: 12),
                  StatusBadge(estatus: estadoRaw),
                  const SizedBox(height: 10),
                  Text(
                    est?.mensaje ?? widget.mensajeInicial,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 13.5),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 14,
                      vertical: 10,
                    ),
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Folio UPC',
                          style: TextStyle(
                            fontSize: 12.5,
                            color: AppColors.mutedText,
                          ),
                        ),
                        CopyChip(value: widget.upc, copyLabel: 'Folio UPC'),
                      ],
                    ),
                  ),
                  if (est?.numAutorizacion != null) ...[
                    const SizedBox(height: 10),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'Autorización',
                          style: TextStyle(
                            fontSize: 12.5,
                            color: AppColors.mutedText,
                          ),
                        ),
                        CopyChip(
                          value: est!.numAutorizacion,
                          copyLabel: 'Autorización',
                        ),
                      ],
                    ),
                  ],
                  if (est?.monto != null) ...[
                    const SizedBox(height: 8),
                    Text(
                      'Importe: ${formatMxn(est!.monto)}',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.amberSoft,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(
                color: AppColors.warning.withValues(alpha: 0.4),
              ),
            ),
            child: const Text(
              'Importante: No reintentes el pago mientras el estatus no sea definitivo para evitar cargos duplicados.',
              style: TextStyle(fontSize: 12.5, color: Color(0xFF78350F)),
            ),
          ),
          if (_error != null) ...[
            const SizedBox(height: 12),
            ErrorBanner(error: _error!, onRetry: _consultar),
          ],
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: _loading ? null : _consultar,
            icon: _loading
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.refresh),
            label: const Text('Consultar estatus en vivo'),
          ),
          const SizedBox(height: 10),
          FilledButton(
            onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
            child: const Text('Volver al inicio'),
          ),
        ],
      ),
    );
  }
}

class TransaccionDetalleScreen extends ConsumerStatefulWidget {
  const TransaccionDetalleScreen({
    super.key,
    required this.transaccion,
  });

  final TransaccionHistorialDto transaccion;

  @override
  ConsumerState<TransaccionDetalleScreen> createState() =>
      _TransaccionDetalleScreenState();
}

class _TransaccionDetalleScreenState
    extends ConsumerState<TransaccionDetalleScreen> {
  PagoEstatusDto? _estatusVivo;
  bool _checking = false;
  bool _resending = false;
  Object? _error;

  Future<void> _refrescarEstatus() async {
    setState(() {
      _checking = true;
      _error = null;
    });
    try {
      final res = await ref
          .read(pagoRepositoryProvider)
          .consultarEstatus(widget.transaccion.upc);
      if (mounted) setState(() => _estatusVivo = res);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _checking = false);
    }
  }

  Future<void> _reenviarComprobante() async {
    final session = ref.read(sessionControllerProvider);
    final emailDefault =
        session.cliente?.correoElectronico ?? session.loginResponse?.username ?? '';
    final emailCtrl = TextEditingController(text: emailDefault);
    final formKey = GlobalKey<FormState>();

    final confirmed = await showDialog<String>(
      context: context,
      builder: (ctx) => AlertDialog(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: const Text('Enviar comprobante por correo'),
        content: Form(
          key: formKey,
          child: TextFormField(
            controller: emailCtrl,
            keyboardType: TextInputType.emailAddress,
            decoration: const InputDecoration(
              labelText: 'Correo destino',
              hintText: 'cliente@dominio.com',
            ),
            validator: (v) => BackendValidators.email(v),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(minimumSize: const Size(100, 42)),
            onPressed: () {
              if (formKey.currentState?.validate() ?? false) {
                Navigator.of(ctx).pop(emailCtrl.text.trim());
              }
            },
            child: const Text('Enviar'),
          ),
        ],
      ),
    );
    emailCtrl.dispose();
    if (confirmed == null || !mounted) return;

    setState(() => _resending = true);
    try {
      final tx = widget.transaccion;
      final res = await ref.read(pagoRepositoryProvider).enviarComprobante(
            ComprobanteEmailDto(
              destinatarioEmail: confirmed,
              destinatarioNombre: session.loginResponse?.nombreCliente,
              numAutorizacion:
                  _estatusVivo?.numAutorizacion ?? tx.numAutorizacion,
              idTx: tx.idTxGestoPago,
              upc: tx.upc,
              numCuenta: tx.numeroCuenta,
              idServicio: tx.idServicio,
              servicio: tx.servicio ?? 'Pago de servicio',
              idProducto: tx.idProducto,
              producto: tx.producto ?? 'Operación digital',
              telefono: tx.telefono,
              referencia: tx.referencia,
              montoPagado: tx.monto,
              comision: tx.comision,
              legend: tx.legend,
              horaLocal: tx.horaLocal,
              logoUrl: tx.urlLogo,
              tipoFront: tx.referencia.isNotEmpty ? 2 : 1,
            ),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text(
            res.enviado
                ? res.mensaje
                : '${res.mensaje} ${res.errorDetalle ?? ''}'.trim(),
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text('No fue posible enviar el comprobante: $e'),
        ),
      );
    } finally {
      if (mounted) setState(() => _resending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final tx = widget.transaccion;
    final estatusActual = _estatusVivo?.estatus ?? tx.estatus;
    final numAutActual = _estatusVivo?.numAutorizacion ?? tx.numAutorizacion;
    final isSuccess = estatusActual == 'EXITOSA';

    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        title: const Text('Detalle de Transacción'),
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  ServiceLogo(
                    urlLogo: tx.urlLogo,
                    tipoFront: tx.referencia.isNotEmpty ? 2 : 1,
                    size: 58,
                  ),
                  const SizedBox(height: 10),
                  Text(
                    tx.servicio ?? 'Pago de servicio',
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
                      color: AppColors.primary,
                    ),
                  ),
                  if (tx.producto != null && tx.producto!.isNotEmpty)
                    Text(
                      tx.producto!,
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 13,
                        color: AppColors.mutedText,
                      ),
                    ),
                  const SizedBox(height: 12),
                  Text(
                    formatMxn(tx.monto),
                    style: const TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.w800,
                      color: AppColors.primary,
                    ),
                  ),
                  const SizedBox(height: 8),
                  StatusBadge(estatus: estatusActual),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          if (_error != null) ...[
            ErrorBanner(error: _error!, onRetry: _refrescarEstatus),
            const SizedBox(height: 12),
          ],
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  _TicketCopyRow(
                    label: 'Folio UPC',
                    value: tx.upc,
                    copyLabel: 'Folio UPC',
                  ),
                  _TicketCopyRow(
                    label: 'Autorización',
                    value: numAutActual,
                    copyLabel: 'Autorización',
                  ),
                  if (tx.idTxGestoPago != null && tx.idTxGestoPago!.isNotEmpty)
                    _ReceiptRow('ID de operación', tx.idTxGestoPago!),
                  _ReceiptRow('Cuenta', maskCuenta(tx.numeroCuenta)),
                  _TicketCopyRow(
                    label: 'Referencia / Teléfono',
                    value: tx.referencia.isNotEmpty ? tx.referencia : tx.telefono,
                    copyLabel: 'Referencia',
                  ),
                  if (tx.comision != null)
                    _ReceiptRow('Comisión', formatMxn(tx.comision)),
                  _ReceiptRow(
                    'Fecha de operación',
                    formatFecha(tx.fechaCreacion ?? tx.horaLocal),
                  ),
                  if (tx.fechaActualizacion != null)
                    _ReceiptRow(
                      'Última actualización',
                      formatFecha(tx.fechaActualizacion),
                    ),
                  _ReceiptRow(
                    'Detalle',
                    _estatusVivo?.mensaje ?? tx.mensajeRespuesta ?? 'Operación registrada',
                  ),
                ],
              ),
            ),
          ),
          if (tx.legend != null && tx.legend!.trim().isNotEmpty) ...[
            const SizedBox(height: 12),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Text(
                  tx.legend!,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.mutedText,
                  ),
                ),
              ),
            ),
          ],
          const SizedBox(height: 16),
          OutlinedButton.icon(
            onPressed: _checking ? null : _refrescarEstatus,
            icon: _checking
                ? const SizedBox(
                    width: 18,
                    height: 18,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  )
                : const Icon(Icons.sync),
            label: const Text('Verificar estatus actual en vivo'),
          ),
          if (isSuccess) ...[
            const SizedBox(height: 10),
            FilledButton.icon(
              onPressed: _resending ? null : _reenviarComprobante,
              icon: _resending
                  ? const SizedBox(
                      width: 18,
                      height: 18,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.email_outlined),
              label: const Text('Enviar comprobante por correo'),
            ),
          ],
        ],
      ),
    );
  }
}

class _ReceiptRow extends StatelessWidget {
  const _ReceiptRow(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: const TextStyle(fontSize: 13, color: AppColors.mutedText),
            ),
          ),
          Expanded(
            flex: 3,
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: const TextStyle(
                fontSize: 13,
                fontWeight: FontWeight.w600,
                color: AppColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
