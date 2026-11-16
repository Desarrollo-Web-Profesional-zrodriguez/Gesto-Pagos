import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';

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
        title: const Text('Reenviar comprobante por correo'),
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
          content: Text(
            res.enviado
                ? res.mensaje
                : '${res.mensaje} ${res.errorDetalle ?? ''}',
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error al reenviar: $e')),
      );
    } finally {
      if (mounted) setState(() => _resending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final c = widget.comprobante;
    return Scaffold(
      appBar: AppBar(title: const Text('Comprobante Digital')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: AppColors.primary,
              borderRadius: BorderRadius.circular(18),
            ),
            child: Column(
              children: [
                const CircleAvatar(
                  radius: 26,
                  backgroundColor: AppColors.accent,
                  child: Icon(Icons.check, color: Colors.white, size: 30),
                ),
                const SizedBox(height: 10),
                Text(
                  c.mensaje,
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  '${c.servicio} • ${c.producto}',
                  textAlign: TextAlign.center,
                  style: const TextStyle(color: Colors.white70, fontSize: 13),
                ),
                const SizedBox(height: 14),
                Text(
                  formatMxn(c.montoPagado),
                  style: const TextStyle(
                    color: Colors.white,
                    fontSize: 30,
                    fontWeight: FontWeight.w800,
                  ),
                ),
              ],
            ),
          ),
          if (c.pin != null && c.pin!.isNotEmpty) ...[
            const SizedBox(height: 14),
            Card(
              color: const Color(0xFFECFDF5),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'PIN / Folio Canjeable (guárdalo ahora)',
                      style: TextStyle(
                        fontWeight: FontWeight.w800,
                        color: Color(0xFF065F46),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Row(
                      children: [
                        Expanded(
                          child: SelectableText(
                            c.pin!,
                            style: const TextStyle(
                              fontFamily: 'monospace',
                              fontSize: 20,
                              fontWeight: FontWeight.w800,
                            ),
                          ),
                        ),
                        IconButton(
                          onPressed: () =>
                              copyToClipboard(context, c.pin!, label: 'PIN'),
                          icon: const Icon(Icons.copy),
                        ),
                      ],
                    ),
                    const Text(
                      'Por seguridad, este PIN no vuelve a mostrarse en el historial.',
                      style: TextStyle(fontSize: 11.5, color: Color(0xFF047857)),
                    ),
                  ],
                ),
              ),
            ),
          ],
          const SizedBox(height: 14),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                children: [
                  _ReceiptRow('Autorización', c.numAutorizacion ?? 'N/A'),
                  _ReceiptRow('Folio UPC', c.upc),
                  _ReceiptRow('ID Tx Proveedor', c.idTx ?? 'N/A'),
                  _ReceiptRow('Cuenta', maskCuenta(c.numCuenta)),
                  if (c.telefono != null && c.telefono!.isNotEmpty)
                    _ReceiptRow('Teléfono', c.telefono!),
                  if (c.referencia != null && c.referencia!.isNotEmpty)
                    _ReceiptRow('Referencia', c.referencia!),
                  if (c.comision != null)
                    _ReceiptRow('Comisión reportada', formatMxn(c.comision)),
                  if (c.saldoAnterior != null)
                    _ReceiptRow('Saldo anterior', formatMxn(c.saldoAnterior)),
                  if (c.saldoDisponibleActual != null)
                    _ReceiptRow(
                      'Saldo disponible actual',
                      formatMxn(c.saldoDisponibleActual),
                    ),
                  if (c.fechaProveedor != null)
                    _ReceiptRow('Fecha proveedor', c.fechaProveedor!),
                  _ReceiptRow(
                    'Correo Brevo',
                    c.emailEnviado == true ? 'Enviado' : 'No enviado / pendiente',
                  ),
                ],
              ),
            ),
          ),
          if (c.legend != null && c.legend!.isNotEmpty) ...[
            const SizedBox(height: 12),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Text(
                  c.legend!,
                  style: const TextStyle(fontSize: 12, color: AppColors.mutedText),
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
            onPressed: () => Navigator.of(context).popUntil((r) => r.isFirst),
            child: const Text('Volver al inicio'),
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
        await ref
            .read(sessionControllerProvider.notifier)
            .refrescarSaldoActual();
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
    final estadoLabel = est?.estatus ?? 'PENDIENTE_CONCILIACION';
    final isSuccess = estadoLabel == 'EXITOSA';
    final isRejected = estadoLabel == 'RECHAZADA';

    final color = isSuccess
        ? AppColors.accent
        : isRejected
            ? AppColors.danger
            : AppColors.warning;

    return Scaffold(
      appBar: AppBar(title: const Text('Seguimiento de Pago (UPC)')),
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
                        ? Icons.check_circle
                        : isRejected
                            ? Icons.cancel
                            : Icons.hourglass_top_rounded,
                    color: color,
                    size: 54,
                  ),
                  const SizedBox(height: 12),
                  Text(
                    estadoLabel,
                    style: TextStyle(
                      fontSize: 20,
                      fontWeight: FontWeight.w800,
                      color: color,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    est?.mensaje ?? widget.mensajeInicial,
                    textAlign: TextAlign.center,
                    style: const TextStyle(fontSize: 13.5),
                  ),
                  const SizedBox(height: 16),
                  Container(
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(
                          'UPC: ${widget.upc}',
                          style: const TextStyle(
                            fontFamily: 'monospace',
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        IconButton(
                          visualDensity: VisualDensity.compact,
                          onPressed: () => copyToClipboard(
                            context,
                            widget.upc,
                            label: 'UPC',
                          ),
                          icon: const Icon(Icons.copy, size: 18),
                        ),
                      ],
                    ),
                  ),
                  if (est?.numAutorizacion != null) ...[
                    const SizedBox(height: 8),
                    Text(
                      'Autorización: ${est!.numAutorizacion}',
                      style: const TextStyle(fontWeight: FontWeight.w700),
                    ),
                  ],
                  if (est?.monto != null) ...[
                    const SizedBox(height: 4),
                    Text('Monto: ${formatMxn(est!.monto)}'),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.warning.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(12),
            ),
            child: const Text(
              'Importante: No reintentes el pago mientras el estatus no sea definitivo para evitar cargos duplicados.',
              style: TextStyle(fontSize: 12.5),
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
            label: const Text('Consultar estatus ahora'),
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
              numAutorizacion: _estatusVivo?.numAutorizacion ?? tx.numAutorizacion,
              idTx: tx.idTxGestoPago,
              upc: tx.upc,
              numCuenta: tx.numeroCuenta,
              idServicio: tx.idServicio,
              servicio: tx.servicio ?? 'Servicio #${tx.idServicio}',
              idProducto: tx.idProducto,
              producto: tx.producto ?? 'Producto #${tx.idProducto}',
              telefono: tx.telefono,
              referencia: tx.referencia,
              montoPagado: tx.monto,
              comision: tx.comision,
              legend: tx.legend,
              horaLocal: tx.horaLocal,
              logoUrl: tx.urlLogo,
            ),
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            res.enviado
                ? res.mensaje
                : '${res.mensaje} ${res.errorDetalle ?? ''}',
          ),
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Error al enviar comprobante: $e')),
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
    final isRejected = estatusActual == 'RECHAZADA';
    final statusColor = isSuccess
        ? AppColors.accent
        : isRejected
            ? AppColors.danger
            : AppColors.warning;

    final logoClean = tx.urlLogo?.trim();

    return Scaffold(
      appBar: AppBar(title: const Text('Detalle de Transacción')),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(18),
              child: Column(
                children: [
                  Container(
                    width: 60,
                    height: 60,
                    padding: const EdgeInsets.all(6),
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      borderRadius: BorderRadius.circular(14),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: (logoClean != null && logoClean.startsWith('http'))
                        ? Image.network(
                            logoClean,
                            fit: BoxFit.contain,
                            errorBuilder: (_, _, _) => const Icon(
                              Icons.payments_outlined,
                              color: AppColors.secondary,
                              size: 30,
                            ),
                          )
                        : const Icon(
                            Icons.payments_outlined,
                            color: AppColors.secondary,
                            size: 30,
                          ),
                  ),
                  const SizedBox(height: 10),
                  Text(
                    tx.servicio ?? 'Servicio #${tx.idServicio}',
                    textAlign: TextAlign.center,
                    style: const TextStyle(
                      fontSize: 18,
                      fontWeight: FontWeight.w800,
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
                  Container(
                    padding: const EdgeInsets.symmetric(
                      horizontal: 12,
                      vertical: 5,
                    ),
                    decoration: BoxDecoration(
                      color: statusColor.withValues(alpha: 0.14),
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      estatusActual,
                      style: TextStyle(
                        color: statusColor,
                        fontWeight: FontWeight.w800,
                        fontSize: 12.5,
                      ),
                    ),
                  ),
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
                  _ReceiptRow('Folio UPC', tx.upc),
                  if (numAutActual != null && numAutActual.isNotEmpty)
                    _ReceiptRow('Autorización', numAutActual),
                  if (tx.idTxGestoPago != null && tx.idTxGestoPago!.isNotEmpty)
                    _ReceiptRow('ID Tx GestoPago', tx.idTxGestoPago!),
                  _ReceiptRow('Cuenta', maskCuenta(tx.numeroCuenta)),
                  _ReceiptRow('Referencia', tx.referencia),
                  if (tx.telefono.isNotEmpty)
                    _ReceiptRow('Teléfono', tx.telefono),
                  if (tx.comision != null)
                    _ReceiptRow('Comisión', formatMxn(tx.comision)),
                  _ReceiptRow(
                    'Fecha de operación',
                    tx.fechaCreacion ?? tx.horaLocal ?? 'N/A',
                  ),
                  if (tx.fechaActualizacion != null)
                    _ReceiptRow('Última actualización', tx.fechaActualizacion!),
                  if (tx.codigoRespuesta != null)
                    _ReceiptRow('Código proveedor', tx.codigoRespuesta!),
                  _ReceiptRow(
                    'Mensaje',
                    _estatusVivo?.mensaje ?? tx.mensajeRespuesta ?? 'Sin detalle',
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
            label: const Text('Actualizar estatus en línea (GET /pagos/estatus)'),
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
              style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
            ),
          ),
        ],
      ),
    );
  }
}
