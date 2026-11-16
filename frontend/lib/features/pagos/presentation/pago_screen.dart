import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/catalogo_productos/data/producto_repository.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_resultado_views.dart';
import 'package:uuid/uuid.dart';

class PagoScreen extends ConsumerStatefulWidget {
  const PagoScreen({
    super.key,
    required this.producto,
  });

  final ProductItemDto producto;

  @override
  ConsumerState<PagoScreen> createState() => _PagoScreenState();
}

class _PagoScreenState extends ConsumerState<PagoScreen> {
  final _formKey = GlobalKey<FormState>();
  final _montoCtrl = TextEditingController();
  final _telefonoCtrl = TextEditingController();
  final _referenciaCtrl = TextEditingController();
  final _correoCtrl = TextEditingController();

  String _idempotencyKey = _generateKey();
  bool _validating = false;
  bool _submitting = false;
  bool _confirmadoExplicitamente = false;
  ValidarReferenciaResponseDto? _validacionResult;
  Object? _error;

  static String _generateKey() =>
      'gp-${const Uuid().v4().replaceAll('-', '').substring(0, 20)}';

  @override
  void initState() {
    super.initState();
    final p = widget.producto;
    if (p.esPrecioFijo && p.precio != null) {
      _montoCtrl.text = StrictDecimal.fromDouble(p.precio!).toStrictLiteral();
    }
    final session = ref.read(sessionControllerProvider);
    _correoCtrl.text =
        session.cliente?.correoElectronico ?? session.loginResponse?.username ?? '';
  }

  @override
  void dispose() {
    _montoCtrl.dispose();
    _telefonoCtrl.dispose();
    _referenciaCtrl.dispose();
    _correoCtrl.dispose();
    super.dispose();
  }

  void _onParamChanged() {
    setState(() {
      _idempotencyKey = _generateKey();
      _validacionResult = null;
      _confirmadoExplicitamente = false;
      _error = null;
    });
  }

  Future<void> _validarReferencia() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    final numCuenta =
        ref.read(sessionControllerProvider).numeroCuentaPrincipal;
    if (numCuenta == null || numCuenta.isEmpty) {
      setState(() {
        _error = 'No se encontró una cuenta de 16 dígitos asociada a tu perfil.';
      });
      return;
    }

    setState(() {
      _validating = true;
      _error = null;
      _validacionResult = null;
      _confirmadoExplicitamente = false;
    });

    try {
      final p = widget.producto;
      final montoParsed = StrictDecimal.tryParse(_montoCtrl.text);
      final req = ValidarReferenciaRequestDto(
        numCuenta: numCuenta,
        idServicio: p.idServicio,
        idProducto: p.idProducto,
        referencia: p.esPagoServicio ? _referenciaCtrl.text : null,
        telefono: p.esRecargaTelefonica
            ? _telefonoCtrl.text
            : (_telefonoCtrl.text.trim().isNotEmpty
                ? _telefonoCtrl.text
                : null),
        monto: montoParsed,
      );

      final res =
          await ref.read(pagoRepositoryProvider).validarReferencia(req);
      if (!mounted) return;
      if (!res.valida) {
        setState(() {
          _error = res.mensaje.isNotEmpty
              ? res.mensaje
              : 'La referencia no pudo validarse con el servicio.';
        });
        return;
      }
      setState(() => _validacionResult = res);
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _validating = false);
    }
  }

  Future<String?> _ask2FA() async {
    final passCtrl = TextEditingController();
    final bioCtrl = TextEditingController(
      text: '[0.12, -0.45, 0.89, -0.05, 0.67]',
    );
    String selectedMode = 'token';
    final token = ref.read(sessionControllerProvider).token;

    final result = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setModalState) => Padding(
          padding: EdgeInsets.fromLTRB(
            20,
            20,
            20,
            20 + MediaQuery.of(ctx).viewInsets.bottom,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Confirmación de Seguridad (2FA)',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.w800),
              ),
              const SizedBox(height: 6),
              const Text(
                'El endpoint POST /pagos requiere validar tu identidad con sesión activa, contraseña o biometría facial.',
                style: TextStyle(fontSize: 12.5, color: AppColors.mutedText),
              ),
              const SizedBox(height: 14),
              SegmentedButton<String>(
                segments: const [
                  ButtonSegment(
                    value: 'token',
                    label: Text('Sesión'),
                    icon: Icon(Icons.verified_user_outlined),
                  ),
                  ButtonSegment(
                    value: 'password',
                    label: Text('Clave'),
                    icon: Icon(Icons.lock_outline),
                  ),
                  ButtonSegment(
                    value: 'biometric',
                    label: Text('Biometría'),
                    icon: Icon(Icons.face_retouching_natural),
                  ),
                ],
                selected: {selectedMode},
                onSelectionChanged: (val) {
                  setModalState(() => selectedMode = val.first);
                },
              ),
              const SizedBox(height: 12),
              if (selectedMode == 'token') ...[
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppColors.primary.withValues(alpha: 0.08),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: const Text(
                    'Se utilizará tu token de sesión activa vigente como factor 2FA seguro.',
                    style: TextStyle(fontSize: 12.5, color: AppColors.primary),
                  ),
                ),
              ] else if (selectedMode == 'password') ...[
                TextField(
                  controller: passCtrl,
                  obscureText: true,
                  decoration: const InputDecoration(
                    labelText: 'Contraseña de tu cuenta',
                    prefixIcon: Icon(Icons.lock_outline),
                  ),
                ),
              ] else ...[
                TextField(
                  controller: bioCtrl,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: 'Vector biométrico facial (MediaPipe)',
                    prefixIcon: Icon(Icons.face),
                    hintText: '[0.12, -0.45, 0.89, -0.05, 0.67]',
                  ),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Vector de flotantes JSON registrado con MediaPipe Face Landmarker.',
                  style: TextStyle(fontSize: 11, color: AppColors.mutedText),
                ),
              ],
              const SizedBox(height: 16),
              FilledButton(
                onPressed: () {
                  if (selectedMode == 'token') {
                    if (token != null && token.isNotEmpty) {
                      Navigator.of(ctx).pop(token);
                    }
                    return;
                  }
                  if (selectedMode == 'password') {
                    if (passCtrl.text.trim().isNotEmpty) {
                      Navigator.of(ctx).pop(passCtrl.text.trim());
                    }
                    return;
                  }
                  if (selectedMode == 'biometric') {
                    if (bioCtrl.text.trim().isNotEmpty) {
                      Navigator.of(ctx).pop(bioCtrl.text.trim());
                    }
                    return;
                  }
                },
                child: const Text('Autorizar y pagar'),
              ),
            ],
          ),
        ),
      ),
    );
    passCtrl.dispose();
    bioCtrl.dispose();
    return result;
  }

  Future<void> _submit() async {
    if (_validacionResult == null || !_validacionResult!.valida) {
      setState(() {
        _error =
            'Debes verificar y validar la referencia antes de confirmar el pago.';
      });
      return;
    }
    if (!_confirmadoExplicitamente) {
      setState(() {
        _error =
            'Marca la confirmación explícita del resumen de operación para continuar.';
      });
      return;
    }
    if (!(_formKey.currentState?.validate() ?? false)) return;

    final numCuenta =
        ref.read(sessionControllerProvider).numeroCuentaPrincipal;
    if (numCuenta == null || numCuenta.isEmpty) {
      setState(() {
        _error = 'No se encontró una cuenta de 16 dígitos asociada a tu perfil.';
      });
      return;
    }

    final factor2fa = await _ask2FA();
    if (factor2fa == null || !mounted) return;

    setState(() {
      _submitting = true;
      _error = null;
    });

    try {
      final p = widget.producto;
      final req = PagoServicioRequestDto(
        numCuenta: numCuenta,
        idServicio: p.idServicio,
        idProducto: p.idProducto,
        monto: StrictDecimal.parse(_montoCtrl.text),
        telefono: p.esRecargaTelefonica
            ? _telefonoCtrl.text
            : (_telefonoCtrl.text.trim().isNotEmpty
                ? _telefonoCtrl.text
                : null),
        referencia: p.esPagoServicio ? _referenciaCtrl.text : null,
        codigo2fa: factor2fa,
        correoElectronico: _correoCtrl.text,
      );

      final result = await ref.read(pagoRepositoryProvider).realizarPago(
            req,
            idempotencyKey: _idempotencyKey,
          );

      await ref.read(sessionControllerProvider.notifier).refrescarSaldoActual();
      if (!mounted) return;

      switch (result) {
        case PagoExitosoResult(:final comprobante):
          await Navigator.of(context).pushReplacement(
            MaterialPageRoute<void>(
              builder: (_) => PagoComprobanteScreen(
                comprobante: comprobante,
                tipoFront: p.tipoFront,
                destinatarioDefault: _correoCtrl.text.trim(),
                nombreDefault:
                    ref.read(sessionControllerProvider).loginResponse?.nombreCliente,
              ),
            ),
          );
        case PagoPendienteConciliacionResult(:final upc, :final mensaje):
          if (upc != null) {
            await Navigator.of(context).pushReplacement(
              MaterialPageRoute<void>(
                builder: (_) => PagoConciliacionScreen(
                  upc: upc,
                  mensajeInicial: mensaje,
                ),
              ),
            );
          } else {
            setState(() => _error = mensaje);
          }
      }
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.producto;
    final cuenta = ref.watch(sessionControllerProvider).cuentaPrincipal;
    final val = _validacionResult;

    return Scaffold(
      appBar: AppBar(title: Text(p.servicio)),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      _ServiceLogo(url: p.urlLogo),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              p.producto,
                              style: const TextStyle(
                                fontSize: 17,
                                fontWeight: FontWeight.w800,
                              ),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'idServicio: ${p.idServicio} • idProducto: ${p.idProducto} • tipoFront: ${p.tipoFront}',
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
                  if (p.esPagoServicio && p.precio != null && p.precio! > 0) ...[
                    const SizedBox(height: 10),
                    Text(
                      'Comisión estimada de catálogo: ${formatMxn(p.precio)}',
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        color: AppColors.secondary,
                      ),
                    ),
                  ],
                  if (p.legend != null && p.legend!.trim().isNotEmpty) ...[
                    const SizedBox(height: 8),
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Text(
                        p.legend!,
                        style: const TextStyle(fontSize: 12),
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          if (cuenta != null)
            Card(
              child: ListTile(
                leading: const Icon(Icons.account_balance_wallet_outlined),
                title: Text('Cargo a ${maskCuenta(cuenta.numeroCuenta)}'),
                subtitle: Text(
                  'Disponible: ${formatMxn(cuenta.saldo?.saldoDisponible)}',
                ),
              ),
            ),
          const SizedBox(height: 12),
          if (_error != null) ...[
            ErrorBanner(error: _error!),
            const SizedBox(height: 12),
          ],
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Form(
                key: _formKey,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    const Text(
                      'Paso 1: Datos y Validación de Referencia',
                      style: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 10),
                    if (p.esPinOEntretenimiento)
                      Container(
                        margin: const EdgeInsets.only(bottom: 12),
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.secondary.withValues(alpha: 0.08),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Text(
                          'Producto PIN / Entretenimiento: no requiere referencia ni teléfono; el PIN se entregará en el comprobante inmediato.',
                          style: TextStyle(fontSize: 12.5),
                        ),
                      ),
                    if (p.esRecargaTelefonica) ...[
                      TextFormField(
                        controller: _telefonoCtrl,
                        keyboardType: TextInputType.phone,
                        maxLength: 10,
                        onChanged: (_) => _onParamChanged(),
                        decoration: const InputDecoration(
                          labelText: 'Teléfono a recargar (10 dígitos) *',
                          counterText: '',
                        ),
                        validator: (v) => BackendValidators.telefono(v),
                      ),
                      const SizedBox(height: 12),
                    ],
                    if (p.esPagoServicio) ...[
                      TextFormField(
                        controller: _referenciaCtrl,
                        onChanged: (_) => _onParamChanged(),
                        decoration: InputDecoration(
                          labelText: p.tipoReferencia != null &&
                                  p.tipoReferencia!.trim().isNotEmpty
                              ? 'Referencia del servicio (${p.tipoReferencia}) *'
                              : 'Referencia del servicio (alfanumérica) *',
                        ),
                        validator: (v) => BackendValidators.referenciaPago(
                          v,
                          requiredField: true,
                        ),
                      ),
                      const SizedBox(height: 12),
                    ],
                    TextFormField(
                      controller: _montoCtrl,
                      readOnly: p.esPrecioFijo,
                      keyboardType: const TextInputType.numberWithOptions(
                        decimal: true,
                      ),
                      onChanged: (_) => _onParamChanged(),
                      decoration: InputDecoration(
                        labelText: p.esPrecioFijo
                            ? 'Monto fijo del producto (MXN)'
                            : 'Monto a pagar (MXN, 1.00 - 10,000.00) *',
                        helperText: p.esPrecioFijo
                            ? 'El catálogo define precio fijo para este producto'
                            : 'Se enviará estrictamente con 2 decimales (ej. 150.00)',
                      ),
                      validator: (v) => p.esPrecioFijo
                          ? BackendValidators.montoDecimal(v, 'El monto')
                          : BackendValidators.montoDecimal(
                              v,
                              'El monto',
                              min: 1.00,
                              max: 10000.00,
                            ),
                    ),
                    if (p.esPagoServicio) ...[
                      const SizedBox(height: 12),
                      TextFormField(
                        controller: _telefonoCtrl,
                        keyboardType: TextInputType.phone,
                        maxLength: 10,
                        onChanged: (_) => _onParamChanged(),
                        decoration: const InputDecoration(
                          labelText: 'Teléfono asociado (opcional)',
                          counterText: '',
                        ),
                        validator: (v) => BackendValidators.telefono(
                          v,
                          requiredField: false,
                        ),
                      ),
                    ],
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _correoCtrl,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(
                        labelText: 'Correo para comprobante (opcional)',
                      ),
                      validator: (v) => BackendValidators.email(
                        v,
                        requiredField: false,
                      ),
                    ),
                    const SizedBox(height: 14),
                    OutlinedButton.icon(
                      onPressed:
                          (_validating || _submitting) ? null : _validarReferencia,
                      icon: _validating
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            )
                          : const Icon(Icons.fact_check_outlined),
                      label: Text(
                        _validating
                            ? 'Verificando con el servicio...'
                            : '1. Verificar referencia y validar servicio',
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ),
          if (val != null && val.valida) ...[
            const SizedBox(height: 14),
            Card(
              color: const Color(0xFFF0FDF4),
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(16),
                side: BorderSide(
                  color: AppColors.accent.withValues(alpha: 0.45),
                ),
              ),
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Row(
                      children: [
                        const Icon(
                          Icons.verified,
                          color: AppColors.accent,
                        ),
                        const SizedBox(width: 8),
                        const Expanded(
                          child: Text(
                            'Paso 2: Resumen de la Operación (Referencia Verificada)',
                            style: TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 15,
                              color: Color(0xFF065F46),
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      val.mensaje,
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: Color(0xFF047857),
                      ),
                    ),
                    const Divider(height: 20),
                    _SummaryRow('Servicio', val.servicio),
                    _SummaryRow('Producto', val.producto),
                    if (val.referenciaNormalizada != null)
                      _SummaryRow('Referencia verificada', val.referenciaNormalizada!),
                    if (val.telefonoNormalizado != null && p.esRecargaTelefonica)
                      _SummaryRow('Teléfono verificado', val.telefonoNormalizado!),
                    if (val.numCuentaEnmascarada != null)
                      _SummaryRow('Cuenta cargo', val.numCuentaEnmascarada!),
                    if (val.saldoDisponibleActual != null)
                      _SummaryRow(
                        'Saldo disponible',
                        formatMxn(val.saldoDisponibleActual),
                      ),
                    if (val.comisionEstimada != null && val.comisionEstimada! > 0)
                      _SummaryRow(
                        'Comisión catálogo',
                        formatMxn(val.comisionEstimada),
                      ),
                    if (val.montoValidado != null)
                      _SummaryRow(
                        'Importe a pagar',
                        formatMxn(val.montoValidado),
                        bold: true,
                      ),
                    if (val.saldoSuficiente == false) ...[
                      const SizedBox(height: 8),
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: AppColors.danger.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: const Text(
                          'Advertencia: El saldo disponible es menor al importe a pagar.',
                          style: TextStyle(
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            color: AppColors.danger,
                          ),
                        ),
                      ),
                    ],
                    const SizedBox(height: 8),
                    CheckboxListTile(
                      contentPadding: EdgeInsets.zero,
                      controlAffinity: ListTileControlAffinity.leading,
                      value: _confirmadoExplicitamente,
                      onChanged: _submitting
                          ? null
                          : (v) => setState(
                                () => _confirmadoExplicitamente = v ?? false,
                              ),
                      title: const Text(
                        'Confirmo que los datos del servicio, referencia e importe son correctos.',
                        style: TextStyle(
                          fontSize: 12.5,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Idempotency-Key: $_idempotencyKey',
                      style: const TextStyle(
                        fontSize: 11,
                        fontFamily: 'monospace',
                        color: AppColors.mutedText,
                      ),
                    ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: (_submitting || !_confirmadoExplicitamente)
                          ? null
                          : _submit,
                      icon: _submitting
                          ? const SizedBox(
                              width: 18,
                              height: 18,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.verified_user_outlined),
                      label: Text(
                        _submitting
                            ? 'Procesando con GestoPago...'
                            : '2. Confirmar y pagar servicio',
                      ),
                    ),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class _ServiceLogo extends StatelessWidget {
  const _ServiceLogo({this.url});
  final String? url;

  @override
  Widget build(BuildContext context) {
    final clean = url?.trim();
    return Container(
      width: 52,
      height: 52,
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

class _SummaryRow extends StatelessWidget {
  const _SummaryRow(this.label, this.value, {this.bold = false});
  final String label;
  final String value;
  final bool bold;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 3),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: const TextStyle(fontSize: 12.5, color: AppColors.mutedText),
            ),
          ),
          Expanded(
            flex: 3,
            child: Text(
              value,
              textAlign: TextAlign.right,
              style: TextStyle(
                fontSize: bold ? 15 : 13,
                fontWeight: bold ? FontWeight.w800 : FontWeight.w600,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
