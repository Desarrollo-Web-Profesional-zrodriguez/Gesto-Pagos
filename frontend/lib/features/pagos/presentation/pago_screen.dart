import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/biometrics/face_embedding_provider.dart';
import 'package:gestopago_app/core/biometrics/fingerprint_auth_service.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/catalogo_productos/data/producto_repository.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_resultado_views.dart';
import 'package:uuid/uuid.dart';

enum _PagoPhase { captura, confirmacion, rechazado }

enum _TwoFaMode { token, password, rostro, huella }

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
  static const List<String> _quickAmounts = [
    '50.00',
    '100.00',
    '200.00',
    '500.00',
  ];

  final _formKey = GlobalKey<FormState>();
  final _montoCtrl = TextEditingController();
  final _telefonoCtrl = TextEditingController();
  final _referenciaCtrl = TextEditingController();
  final _correoCtrl = TextEditingController();
  final _twoFaPassCtrl = TextEditingController();

  String _idempotencyKey = _generateKey();
  _PagoPhase _phase = _PagoPhase.captura;
  _TwoFaMode _twoFaMode = _TwoFaMode.token;
  bool _obscureTwoFaPass = true;
  bool _huellaDisponible = false;
  bool _validating = false;
  bool _submitting = false;
  bool _confirmadoExplicitamente = false;
  ValidarReferenciaResponseDto? _validacionResult;
  Object? _error;
  String? _validationSummary;

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
    _correoCtrl.text = session.cliente?.correoElectronico ??
        session.loginResponse?.username ??
        '';
    _checkFingerprint();
  }

  Future<void> _checkFingerprint() async {
    if (kIsWeb) return;
    final ok = await ref
        .read(fingerprintAuthServiceProvider)
        .estaDisponibleEnTelefono();
    if (mounted) {
      setState(() => _huellaDisponible = ok);
    }
  }

  @override
  void dispose() {
    _montoCtrl.dispose();
    _telefonoCtrl.dispose();
    _referenciaCtrl.dispose();
    _correoCtrl.dispose();
    _twoFaPassCtrl.dispose();
    super.dispose();
  }

  void _onParamChanged() {
    if (_validacionResult != null || _error != null || _validationSummary != null) {
      setState(() {
        _idempotencyKey = _generateKey();
        _validacionResult = null;
        _confirmadoExplicitamente = false;
        _error = null;
        _validationSummary = null;
      });
    }
  }

  Future<void> _validarReferencia() async {
    setState(() {
      _error = null;
      _validationSummary = null;
    });

    if (!(_formKey.currentState?.validate() ?? false)) {
      setState(() {
        _validationSummary =
            'Revisa los campos marcados antes de validar el servicio.';
      });
      return;
    }

    final numCuenta =
        ref.read(sessionControllerProvider).numeroCuentaPrincipal;
    if (numCuenta == null || numCuenta.isEmpty) {
      setState(() {
        _error =
            'No se encontró una cuenta de 16 dígitos asociada a tu perfil.';
      });
      return;
    }

    setState(() {
      _validating = true;
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
      setState(() {
        _validacionResult = res;
        _idempotencyKey = _generateKey();
        _confirmadoExplicitamente = false;
        _phase = _PagoPhase.confirmacion;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _validating = false);
    }
  }

  Future<String?> _resolve2FaSecret() async {
    final token = ref.read(sessionControllerProvider).token;
    switch (_twoFaMode) {
      case _TwoFaMode.token:
        if (token == null || token.isEmpty) {
          setState(() {
            _validationSummary =
                'Tu sesión no está disponible. Inicia sesión nuevamente.';
          });
          return null;
        }
        return token;
      case _TwoFaMode.password:
        final pass = _twoFaPassCtrl.text.trim();
        if (pass.isEmpty) {
          setState(() {
            _validationSummary =
                'Ingresa tu contraseña de cuenta para autorizar el pago.';
          });
          return null;
        }
        return pass;
      case _TwoFaMode.rostro:
        final embedding =
            await ref.read(faceEmbeddingProvider).captureFacialEmbedding(
                  context,
                  title: 'Autorizar Pago con Rostro',
                  subtitle:
                      'Mira de frente a la cámara para autorizar este cargo',
                );
        if (embedding == null || embedding.isEmpty) {
          if (mounted) {
            setState(() {
              _validationSummary =
                  'Completa el escaneo facial o elige otro método para autorizar el pago.';
            });
          }
          return null;
        }
        return embedding;
      case _TwoFaMode.huella:
        final authRes = await ref
            .read(fingerprintAuthServiceProvider)
            .autenticarConHuella(
              motivo: 'Confirma tu identidad para autorizar el pago',
            );
        if (!authRes.autenticado) {
          if (mounted) {
            setState(() {
              _error = authRes.mensaje ?? 'Verificación biométrica cancelada.';
            });
          }
          return null;
        }
        if (token == null || token.isEmpty) {
          if (mounted) {
            setState(() {
              _error = 'Tu sesión expiró. Inicia sesión nuevamente.';
            });
          }
          return null;
        }
        return token;
    }
  }

  Future<void> _submit() async {
    final val = _validacionResult;
    if (val == null || !val.valida) {
      setState(() {
        _phase = _PagoPhase.captura;
        _error = 'Debes verificar el servicio antes de confirmar el pago.';
      });
      return;
    }
    if (val.saldoSuficiente == false) {
      setState(() {
        _validationSummary =
            'Tu saldo disponible es insuficiente para cubrir esta operación.';
      });
      return;
    }
    if (_correoCtrl.text.trim().isNotEmpty) {
      final emailErr = BackendValidators.email(
        _correoCtrl.text,
        requiredField: false,
      );
      if (emailErr != null) {
        setState(() => _validationSummary = emailErr);
        return;
      }
    }
    if (!_confirmadoExplicitamente) {
      setState(() {
        _validationSummary =
            'Marca la casilla de confirmación explícita para autorizar el cargo.';
      });
      return;
    }

    final numCuenta =
        ref.read(sessionControllerProvider).numeroCuentaPrincipal;
    if (numCuenta == null || numCuenta.isEmpty) {
      setState(() {
        _error =
            'No se encontró una cuenta de 16 dígitos asociada a tu perfil.';
      });
      return;
    }

    final factor2fa = await _resolve2FaSecret();
    if (factor2fa == null || !mounted) return;

    setState(() {
      _submitting = true;
      _error = null;
      _validationSummary = null;
    });

    try {
      final p = widget.producto;
      final montoToSend = StrictDecimal.tryParse(_montoCtrl.text) ??
          StrictDecimal.fromDouble(val.montoValidado ?? p.precio ?? 0);

      final req = PagoServicioRequestDto(
        numCuenta: numCuenta,
        idServicio: p.idServicio,
        idProducto: p.idProducto,
        monto: montoToSend,
        telefono: p.esRecargaTelefonica
            ? (val.telefonoNormalizado ?? _telefonoCtrl.text)
            : (_telefonoCtrl.text.trim().isNotEmpty
                ? _telefonoCtrl.text
                : null),
        referencia: p.esPagoServicio
            ? (val.referenciaNormalizada ?? _referenciaCtrl.text)
            : null,
        codigo2fa: factor2fa,
        correoElectronico: _correoCtrl.text.trim().isNotEmpty
            ? _correoCtrl.text.trim()
            : null,
      );

      final result = await ref.read(pagoRepositoryProvider).realizarPago(
            req,
            idempotencyKey: _idempotencyKey,
          );

      try {
        await ref
            .read(sessionControllerProvider.notifier)
            .refrescarSaldoActual();
      } catch (_) {}
      ref.read(historyRefreshSignalProvider.notifier).bump();

      if (!mounted) return;

      switch (result) {
        case PagoExitosoResult(:final comprobante):
          await Navigator.of(context).pushReplacement(
            MaterialPageRoute<void>(
              builder: (_) => PagoComprobanteScreen(
                comprobante: comprobante,
                tipoFront: p.tipoFront,
                destinatarioDefault: _correoCtrl.text.trim(),
                nombreDefault: ref
                    .read(sessionControllerProvider)
                    .loginResponse
                    ?.nombreCliente,
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
            setState(() {
              _error = mensaje;
              _phase = _PagoPhase.rechazado;
            });
          }
      }
    } catch (e) {
      try {
        await ref
            .read(sessionControllerProvider.notifier)
            .refrescarSaldoActual();
      } catch (_) {}
      if (!mounted) return;
      setState(() {
        _error = e;
        _phase = _PagoPhase.rechazado;
      });
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final p = widget.producto;
    final cuenta = ref.watch(sessionControllerProvider).cuentaPrincipal;
    final stepIndex = _phase == _PagoPhase.captura ? 0 : 1;

    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              p.producto,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
            ),
            Text(
              '${p.servicio} · ${p.etiquetaFlujo}',
              style: const TextStyle(fontSize: 11.5, color: Color(0xFF94A3B8)),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          PagoPhaseProgressBar(activeIndex: stepIndex),
          Expanded(
            child: ListView(
              padding: const EdgeInsets.all(16),
              children: [
                if (_error != null && _phase != _PagoPhase.rechazado) ...[
                  ErrorBanner(
                    error: _error!,
                    onDismiss: () => setState(() => _error = null),
                  ),
                  const SizedBox(height: 12),
                ],
                if (_validationSummary != null) ...[
                  ValidationSummary(
                    message: _validationSummary,
                    onDismiss: () => setState(() => _validationSummary = null),
                  ),
                  const SizedBox(height: 12),
                ],
                if (_phase == _PagoPhase.captura)
                  _buildPhase1Captura(p, cuenta?.numeroCuenta, cuenta?.saldo?.saldoDisponible)
                else if (_phase == _PagoPhase.confirmacion)
                  _buildPhase2Confirmacion(p, _validacionResult!)
                else
                  _buildPhase3CRechazado(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildPhase1Captura(
    ProductItemDto p,
    String? numCuenta,
    double? saldoDisponible,
  ) {
    final ayudaUrl = p.urlReferenciaAyuda?.trim();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    ServiceLogo(
                      urlLogo: p.urlLogo,
                      tipoFront: p.tipoFront,
                      size: 48,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            p.producto,
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 15,
                              color: AppColors.primary,
                            ),
                          ),
                          Text(
                            p.servicio,
                            style: const TextStyle(
                              fontSize: 12.5,
                              color: AppColors.mutedText,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
                const Divider(height: 20, color: Color(0xFFF1F5F9)),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Text(
                      'Disponible: ${formatMxn(saldoDisponible)}',
                      style: const TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFF047857),
                      ),
                    ),
                    Text(
                      'Cuenta: ${maskCuenta(numCuenta)}',
                      style: const TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 12,
                        color: AppColors.mutedText,
                      ),
                    ),
                  ],
                ),
                if (p.legend != null && p.legend!.trim().isNotEmpty) ...[
                  const SizedBox(height: 10),
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.skySoft,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Icon(
                          Icons.info_outline,
                          size: 16,
                          color: AppColors.secondary,
                        ),
                        const SizedBox(width: 8),
                        Expanded(
                          child: Text(
                            p.legend!.trim(),
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppColors.sky950,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    '1. Captura de Datos y Validación',
                    style: TextStyle(
                      fontSize: 15.5,
                      fontWeight: FontWeight.w800,
                      color: AppColors.primary,
                    ),
                  ),
                  const SizedBox(height: 12),
                  if (p.esPinOEntretenimiento) ...[
                    Container(
                      margin: const EdgeInsets.only(bottom: 12),
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.violetSoft,
                        borderRadius: BorderRadius.circular(12),
                      ),
                      child: const Text(
                        'Producto digital / entretenimiento: no requiere número de referencia; recibirás tu PIN digital en el comprobante.',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppColors.violet,
                          fontWeight: FontWeight.w600,
                        ),
                      ),
                    ),
                    TextFormField(
                      controller: _telefonoCtrl,
                      keyboardType: TextInputType.phone,
                      maxLength: 10,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      onChanged: (_) => _onParamChanged(),
                      decoration: const InputDecoration(
                        labelText: 'Teléfono asociado (opcional, 10 dígitos)',
                        counterText: '',
                        hintText: '5512345678',
                      ),
                      validator: (v) => BackendValidators.telefono(
                        v,
                        requiredField: false,
                      ),
                    ),
                    const SizedBox(height: 12),
                  ],
                  if (p.esRecargaTelefonica) ...[
                    TextFormField(
                      controller: _telefonoCtrl,
                      keyboardType: TextInputType.phone,
                      maxLength: 10,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      onChanged: (_) => _onParamChanged(),
                      decoration: const InputDecoration(
                        labelText: 'Número telefónico celular (10 dígitos) *',
                        prefixIcon: Icon(Icons.smartphone_outlined),
                        counterText: '',
                        hintText: '5512345678',
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
                            ? 'Referencia (${p.tipoReferencia}) *'
                            : 'Referencia o línea de captura *',
                        prefixIcon: const Icon(Icons.receipt_outlined),
                        hintText: 'Ej. 25720381',
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
                          ? 'Importe fijo del producto *'
                          : 'Importe a pagar (1.00 - 10,000.00) *',
                      prefixText: r'$ ',
                      suffixText: 'MXN',
                      helperText: p.esPrecioFijo
                          ? 'Este producto tiene precio fijo definido'
                          : 'Ingresa un importe con hasta 2 decimales',
                    ),
                    validator: (v) => p.esPrecioFijo
                        ? BackendValidators.montoDecimal(v, 'El importe')
                        : BackendValidators.montoDecimal(
                            v,
                            'El importe',
                            min: 1.00,
                            max: 10000.00,
                          ),
                  ),
                  if (!p.esPrecioFijo) ...[
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 8,
                      runSpacing: 6,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        const Text(
                          'Importes rápidos:',
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w600,
                            color: AppColors.mutedText,
                          ),
                        ),
                        ..._quickAmounts.map((amt) {
                          final selected = _montoCtrl.text.trim() == amt;
                          return ChoiceChip(
                            label: Text(
                              '\$$amt',
                              style: const TextStyle(
                                fontFamily: 'monospace',
                                fontSize: 12,
                                fontWeight: FontWeight.w700,
                              ),
                            ),
                            selected: selected,
                            onSelected: (_) {
                              setState(() {
                                _montoCtrl.text = amt;
                                _validationSummary = null;
                                _error = null;
                              });
                            },
                          );
                        }),
                      ],
                    ),
                  ],
                  if (p.esPagoServicio) ...[
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: _telefonoCtrl,
                      keyboardType: TextInputType.phone,
                      maxLength: 10,
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
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
                    if (p.precio != null && p.precio! > 0) ...[
                      const SizedBox(height: 8),
                      Text(
                        'Comisión estimada para este servicio: ${formatMxn(p.precio)}',
                        style: const TextStyle(
                          fontSize: 12,
                          fontWeight: FontWeight.w600,
                          color: AppColors.secondary,
                        ),
                      ),
                    ],
                  ],
                  if (ayudaUrl != null && ayudaUrl.startsWith('http')) ...[
                    const SizedBox(height: 14),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(color: AppColors.border),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Guía visual para localizar tu referencia:',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: FontWeight.w700,
                              color: AppColors.mutedText,
                            ),
                          ),
                          const SizedBox(height: 8),
                          ClipRRect(
                            borderRadius: BorderRadius.circular(8),
                            child: Image.network(
                              ayudaUrl,
                              height: 140,
                              fit: BoxFit.contain,
                              errorBuilder: (_, _, _) => const SizedBox.shrink(),
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                  const SizedBox(height: 18),
                  FilledButton(
                    onPressed: _validating ? null : _validarReferencia,
                    child: _validating
                        ? const SizedBox(
                            width: 20,
                            height: 20,
                            child: CircularProgressIndicator(
                              strokeWidth: 2,
                              color: Colors.white,
                            ),
                          )
                        : const Row(
                            mainAxisAlignment: MainAxisAlignment.center,
                            children: [
                              Text('Validar y continuar'),
                              SizedBox(width: 8),
                              Icon(Icons.arrow_forward_rounded, size: 18),
                            ],
                          ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPhase2Confirmacion(
    ProductItemDto p,
    ValidarReferenciaResponseDto val,
  ) {
    final ayudaUrl = val.urlReferenciaAyuda?.trim() ?? p.urlReferenciaAyuda?.trim();
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: AppColors.emeraldSoft,
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: AppColors.accent.withValues(alpha: 0.4)),
          ),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Icon(Icons.verified, color: AppColors.accent),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      '2. Referencia Verificada',
                      style: TextStyle(
                        fontWeight: FontWeight.w800,
                        fontSize: 14.5,
                        color: Color(0xFF065F46),
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      val.mensaje,
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: Color(0xFF047857),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'RESUMEN PREVIO AL CARGO',
                  style: TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.9,
                    color: AppColors.mutedText,
                  ),
                ),
                const SizedBox(height: 10),
                _SummaryRow('Servicio', val.servicio),
                _SummaryRow('Producto', val.producto),
                if (val.referenciaNormalizada != null && p.esPagoServicio)
                  _SummaryRow('Referencia validada', val.referenciaNormalizada!),
                if (val.telefonoNormalizado != null && p.esRecargaTelefonica)
                  _SummaryRow('Teléfono validado', val.telefonoNormalizado!),
                if (val.numCuentaEnmascarada != null)
                  _SummaryRow('Cuenta de cargo', val.numCuentaEnmascarada!),
                if (val.montoValidado != null)
                  _SummaryRow(
                    'Importe del servicio',
                    formatMxn(val.montoValidado),
                    bold: true,
                  ),
                if (val.comisionEstimada != null && val.comisionEstimada! > 0)
                  _SummaryRow(
                    'Comisión estimada',
                    formatMxn(val.comisionEstimada),
                  ),
                const Divider(height: 18, color: Color(0xFFF1F5F9)),
                if (val.saldoDisponibleActual != null)
                  _SummaryRow(
                    'Saldo disponible actual',
                    formatMxn(val.saldoDisponibleActual),
                    bold: true,
                    valueColor: val.saldoSuficiente == false
                        ? AppColors.danger
                        : const Color(0xFF047857),
                  ),
              ],
            ),
          ),
        ),
        if (val.saldoSuficiente == false) ...[
          const SizedBox(height: 10),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppColors.roseSoft,
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppColors.danger.withValues(alpha: 0.35)),
            ),
            child: const Text(
              'Tu saldo disponible no es suficiente para cubrir esta operación.',
              style: TextStyle(
                fontSize: 12.5,
                fontWeight: FontWeight.w700,
                color: Color(0xFF9F1239),
              ),
            ),
          ),
        ],
        if (ayudaUrl != null && ayudaUrl.startsWith('http')) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(12),
              child: Image.network(
                ayudaUrl,
                height: 130,
                fit: BoxFit.contain,
                errorBuilder: (_, _, _) => const SizedBox.shrink(),
              ),
            ),
          ),
        ],
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                TextFormField(
                  controller: _correoCtrl,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    labelText: 'Correo para comprobante digital (opcional)',
                    prefixIcon: Icon(Icons.email_outlined),
                    hintText: 'cliente@dominio.com',
                  ),
                ),
                const SizedBox(height: 16),
                const Row(
                  children: [
                    Icon(
                      Icons.verified_user_outlined,
                      size: 16,
                      color: AppColors.secondary,
                    ),
                    SizedBox(width: 6),
                    Text(
                      'CONFIRMACIÓN DE SEGURIDAD (2FA)',
                      style: TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w800,
                        letterSpacing: 0.7,
                        color: AppColors.primary,
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 10),
                SegmentedButton<_TwoFaMode>(
                  segments: [
                    const ButtonSegment(
                      value: _TwoFaMode.token,
                      label: Text('Sesión'),
                      icon: Icon(Icons.verified_user_outlined, size: 17),
                    ),
                    const ButtonSegment(
                      value: _TwoFaMode.password,
                      label: Text('Contraseña'),
                      icon: Icon(Icons.lock_outline, size: 17),
                    ),
                    if (ref
                            .watch(sessionControllerProvider)
                            .tieneBiometriaFacial &&
                        ref.watch(faceEmbeddingProvider).isCameraSupported)
                      const ButtonSegment(
                        value: _TwoFaMode.rostro,
                        label: Text('Rostro'),
                        icon: Icon(Icons.face_retouching_natural, size: 17),
                      ),
                    if (_huellaDisponible)
                      const ButtonSegment(
                        value: _TwoFaMode.huella,
                        label: Text('Huella'),
                        icon: Icon(Icons.fingerprint, size: 17),
                      ),
                  ],
                  selected: {_twoFaMode},
                  onSelectionChanged: (val) {
                    setState(() {
                      _twoFaMode = val.first;
                      _twoFaPassCtrl.clear();
                      _validationSummary = null;
                    });
                  },
                ),
                const SizedBox(height: 10),
                if (_twoFaMode == _TwoFaMode.token)
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.skySoft,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Text(
                      'Se autorizará la operación mediante tu sesión segura activa.',
                      style: TextStyle(fontSize: 12, color: AppColors.sky950),
                    ),
                  )
                else if (_twoFaMode == _TwoFaMode.password)
                  TextField(
                    controller: _twoFaPassCtrl,
                    obscureText: _obscureTwoFaPass,
                    decoration: InputDecoration(
                      labelText: 'Contraseña de tu cuenta',
                      prefixIcon: const Icon(Icons.lock_outline),
                      suffixIcon: IconButton(
                        onPressed: () => setState(
                          () => _obscureTwoFaPass = !_obscureTwoFaPass,
                        ),
                        icon: Icon(
                          _obscureTwoFaPass
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                      ),
                    ),
                  )
                else if (_twoFaMode == _TwoFaMode.rostro)
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.emeraldSoft,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Text(
                      'Al confirmar el pago se abrirá la cámara frontal para verificar tu rostro registrado.',
                      style: TextStyle(
                        fontSize: 12,
                        color: Color(0xFF065F46),
                      ),
                    ),
                  )
                else
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.skySoft,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Text(
                      'Al confirmar el pago se solicitará la huella digital registrada en este dispositivo.',
                      style: TextStyle(fontSize: 12, color: AppColors.sky950),
                    ),
                  ),
                const SizedBox(height: 12),
                Material(
                  color: _confirmadoExplicitamente
                      ? AppColors.emeraldSoft
                      : AppColors.background,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(12),
                    side: BorderSide(
                      color: _confirmadoExplicitamente
                          ? AppColors.accent.withValues(alpha: 0.45)
                          : AppColors.border,
                    ),
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: CheckboxListTile(
                    contentPadding:
                        const EdgeInsets.symmetric(horizontal: 10, vertical: 2),
                    controlAffinity: ListTileControlAffinity.leading,
                    value: _confirmadoExplicitamente,
                    onChanged: _submitting
                        ? null
                        : (v) => setState(() {
                              _confirmadoExplicitamente = v ?? false;
                              _validationSummary = null;
                            }),
                    title: const Text(
                      'Confirmo que verifiqué el proveedor, referencia e importe de esta operación y autorizo el cargo definitivo a mi cuenta.',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w600,
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: _submitting
                            ? null
                            : () => setState(() {
                                  _phase = _PagoPhase.captura;
                                  _validationSummary = null;
                                  _error = null;
                                }),
                        icon: const Icon(Icons.arrow_back_rounded, size: 17),
                        label: const Text('Modificar'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      flex: 2,
                      child: FilledButton.icon(
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.accent,
                        ),
                        onPressed:
                            (_submitting || val.saldoSuficiente == false)
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
                            : const Icon(Icons.check_circle_outline, size: 18),
                        label: Text(
                          _submitting
                              ? 'Procesando pago...'
                              : 'Confirmar y pagar',
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildPhase3CRechazado() {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Container(
          padding: const EdgeInsets.all(20),
          decoration: BoxDecoration(
            color: AppColors.roseSoft,
            borderRadius: BorderRadius.circular(18),
            border: Border.all(color: AppColors.danger.withValues(alpha: 0.35)),
          ),
          child: Column(
            children: [
              const Icon(Icons.cancel_outlined, color: AppColors.danger, size: 48),
              const SizedBox(height: 10),
              const Text(
                'Operación Rechazada',
                style: TextStyle(
                  fontSize: 18,
                  fontWeight: FontWeight.w800,
                  color: Color(0xFF881337),
                ),
              ),
              const SizedBox(height: 6),
              Text(
                _error?.toString() ?? 'No fue posible procesar el pago.',
                textAlign: TextAlign.center,
                style: const TextStyle(fontSize: 13, color: Color(0xFF9F1239)),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        Container(
          padding: const EdgeInsets.all(14),
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(14),
            border: Border.all(color: AppColors.border),
          ),
          child: const Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Icon(
                Icons.info_outline,
                color: Color(0xFFD97706),
                size: 19,
              ),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  'La retención preventiva en tu cuenta fue liberada inmediatamente; no se aplicó ningún cargo a tu saldo disponible.',
                  style: TextStyle(fontSize: 12.5, color: AppColors.primary),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: () {
            setState(() {
              _idempotencyKey = _generateKey();
              _validacionResult = null;
              _confirmadoExplicitamente = false;
              _error = null;
              _phase = _PagoPhase.captura;
            });
          },
          icon: const Icon(Icons.refresh),
          label: const Text('Revisar datos e intentar de nuevo'),
        ),
        const SizedBox(height: 10),
        OutlinedButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cerrar'),
        ),
      ],
    );
  }
}

class PagoPhaseProgressBar extends StatelessWidget {
  const PagoPhaseProgressBar({super.key, required this.activeIndex});

  final int activeIndex;

  static const _labels = [
    '1. Validación',
    '2. Autorización',
    '3. Comprobante',
  ];

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(bottom: BorderSide(color: AppColors.border)),
      ),
      child: Row(
        children: List.generate(_labels.length, (idx) {
          final done = idx < activeIndex;
          final active = idx == activeIndex;
          return Expanded(
            child: Container(
              margin: EdgeInsets.only(right: idx < _labels.length - 1 ? 8 : 0),
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 7),
              decoration: BoxDecoration(
                color: active
                    ? AppColors.skySoft
                    : done
                        ? AppColors.emeraldSoft
                        : AppColors.background,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(
                  color: active
                      ? AppColors.secondary
                      : done
                          ? AppColors.accent.withValues(alpha: 0.4)
                          : AppColors.border,
                ),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.center,
                children: [
                  if (done) ...[
                    const Icon(
                      Icons.check_circle,
                      size: 13,
                      color: AppColors.accent,
                    ),
                    const SizedBox(width: 4),
                  ],
                  Flexible(
                    child: Text(
                      _labels[idx],
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 11.5,
                        fontWeight: FontWeight.w700,
                        color: active
                            ? AppColors.secondary
                            : done
                                ? const Color(0xFF047857)
                                : AppColors.mutedText,
                      ),
                    ),
                  ),
                ],
              ),
            ),
          );
        }),
      ),
    );
  }
}

class _SummaryRow extends StatelessWidget {
  const _SummaryRow(
    this.label,
    this.value, {
    this.bold = false,
    this.valueColor,
  });

  final String label;
  final String value;
  final bool bold;
  final Color? valueColor;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
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
                fontSize: bold ? 14.5 : 13,
                fontWeight: bold ? FontWeight.w800 : FontWeight.w600,
                color: valueColor ?? AppColors.primary,
              ),
            ),
          ),
        ],
      ),
    );
  }
}
