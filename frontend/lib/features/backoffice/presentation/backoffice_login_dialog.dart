import 'dart:async';

import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/biometrics/fingerprint_auth_service.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/core/widgets/otp_code_input.dart';
import 'package:gestopago_app/features/backoffice/presentation/backoffice_session.dart';

/// Diálogo de verificación en dos pasos para Consola Operativa (código OTP de 6 dígitos o huella digital en móvil).
Future<bool> showBackoffice2faDialog(
  BuildContext context,
  WidgetRef ref, {
  required String challengeId,
  required String infoMessage,
  String email = '',
  String emailEnmascarado = '',
  int expiraEnSegundos = 300,
}) async {
  final result = await showDialog<bool>(
    context: context,
    barrierDismissible: false,
    builder: (_) => _BackofficeLoginDialog(
      initialChallengeId: challengeId,
      initialInfoMessage: infoMessage,
      email: email,
      emailEnmascarado: emailEnmascarado,
      expiraEnSegundos: expiraEnSegundos,
    ),
  );
  return result ?? false;
}

class _BackofficeLoginDialog extends ConsumerStatefulWidget {
  const _BackofficeLoginDialog({
    required this.initialChallengeId,
    required this.initialInfoMessage,
    required this.email,
    required this.emailEnmascarado,
    required this.expiraEnSegundos,
  });

  final String initialChallengeId;
  final String initialInfoMessage;
  final String email;
  final String emailEnmascarado;
  final int expiraEnSegundos;

  @override
  ConsumerState<_BackofficeLoginDialog> createState() =>
      _BackofficeLoginDialogState();
}

class _BackofficeLoginDialogState extends ConsumerState<_BackofficeLoginDialog> {
  String _otpCode = '';
  bool _loading = false;
  bool _resending = false;
  bool _huellaDisponible = false;
  int _secondsLeft = 300;
  Timer? _timer;

  late final String _challengeId;
  late String _infoMessage;
  Object? _error;
  String? _otpFieldError;

  @override
  void initState() {
    super.initState();
    _challengeId = widget.initialChallengeId;
    _infoMessage = widget.initialInfoMessage;
    _secondsLeft = widget.expiraEnSegundos > 0 ? widget.expiraEnSegundos : 300;
    _startCountdown();
    _checkFingerprint();
  }

  void _startCountdown() {
    _timer?.cancel();
    _timer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      if (_secondsLeft <= 1) {
        _timer?.cancel();
        setState(() => _secondsLeft = 0);
      } else {
        setState(() => _secondsLeft--);
      }
    });
  }

  Future<void> _checkFingerprint() async {
    final ok = await ref
        .read(fingerprintAuthServiceProvider)
        .estaDisponibleEnTelefono();
    if (mounted) {
      setState(() => _huellaDisponible = ok);
    }
  }

  @override
  void dispose() {
    _timer?.cancel();
    super.dispose();
  }

  String get _plataforma => kIsWeb ? 'WEB' : 'MOBILE';

  Future<void> _verifyWithOtp() async {
    final validation = BackendValidators.codigoOtp(_otpCode);
    if (validation != null) {
      setState(() => _otpFieldError = validation);
      return;
    }

    setState(() {
      _loading = true;
      _error = null;
      _otpFieldError = null;
    });

    try {
      final dio = ref.read(dioProvider);
      final resp = await dio.post<Map<String, dynamic>>(
        '/auth/backoffice/verificar-2fa',
        data: {
          'challengeId': _challengeId,
          'metodo': 'EMAIL_OTP',
          'codigo': _otpCode.replaceAll(RegExp(r'\D'), ''),
          'plataforma': _plataforma,
        },
      );
      final data = resp.data ?? {};
      final token = (data['tokenBackoffice'] ?? '').toString();
      if (data['autenticado'] == true && mounted) {
        ref.read(backofficeSessionProvider.notifier).establecerSesion(
              tokenBackoffice: token.isNotEmpty ? token : _challengeId,
              email: (data['email'] ?? widget.email).toString(),
            );
        Navigator.of(context).pop(true);
      }
    } on DioException catch (e) {
      if (!mounted) return;
      setState(() => _error = ApiException.fromDio(e));
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _verifyWithFingerprint() async {
    setState(() {
      _loading = true;
      _error = null;
      _otpFieldError = null;
    });

    try {
      final authResult = await ref
          .read(fingerprintAuthServiceProvider)
          .autenticarConHuella(
            motivo: 'Confirma tu huella digital para entrar a la Consola Operativa',
          );

      if (!authResult.autenticado) {
        if (!mounted) return;
        setState(() => _error = authResult.mensaje ?? 'Huella no verificada.');
        return;
      }

      final dio = ref.read(dioProvider);
      final resp = await dio.post<Map<String, dynamic>>(
        '/auth/backoffice/verificar-2fa',
        data: {
          'challengeId': _challengeId,
          'metodo': 'HUELLA',
          'biometriaVerificada': true,
          'plataforma': 'MOBILE',
        },
      );
      final data = resp.data ?? {};
      final token = (data['tokenBackoffice'] ?? '').toString();
      if (data['autenticado'] == true && mounted) {
        ref.read(backofficeSessionProvider.notifier).establecerSesion(
              tokenBackoffice: token.isNotEmpty ? token : _challengeId,
              email: (data['email'] ?? widget.email).toString(),
            );
        Navigator.of(context).pop(true);
      }
    } on DioException catch (e) {
      if (!mounted) return;
      setState(() => _error = ApiException.fromDio(e));
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _resendOtp() async {
    if (_challengeId.isEmpty) return;
    setState(() {
      _resending = true;
      _error = null;
      _otpFieldError = null;
    });
    try {
      final dio = ref.read(dioProvider);
      final resp = await dio.post<Map<String, dynamic>>(
        '/auth/backoffice/reenviar-otp',
        data: {'challengeId': _challengeId},
      );
      if (!mounted) return;
      final data = resp.data ?? {};
      final newTtl = (data['expiraEnSegundos'] as num?)?.toInt() ?? 300;
      setState(() {
        _otpCode = '';
        _secondsLeft = newTtl;
        _infoMessage =
            (data['mensaje'] ?? 'Nuevo código enviado a tu correo.').toString();
      });
      _startCountdown();
    } on DioException catch (e) {
      if (!mounted) return;
      setState(() => _error = ApiException.fromDio(e));
    } finally {
      if (mounted) setState(() => _resending = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final maskedTarget = widget.emailEnmascarado.isNotEmpty
        ? widget.emailEnmascarado
        : widget.email;

    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(24)),
      titlePadding: const EdgeInsets.fromLTRB(20, 20, 20, 8),
      contentPadding: const EdgeInsets.fromLTRB(20, 8, 20, 12),
      title: Row(
        children: [
          Container(
            width: 42,
            height: 42,
            decoration: BoxDecoration(
              color: AppColors.amberSoft,
              borderRadius: BorderRadius.circular(14),
            ),
            child: const Icon(
              Icons.admin_panel_settings_outlined,
              color: Color(0xFFB45309),
            ),
          ),
          const SizedBox(width: 12),
          const Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Verificación en Dos Pasos',
                  style: TextStyle(
                    fontSize: 16.5,
                    fontWeight: FontWeight.w800,
                    color: AppColors.primary,
                  ),
                ),
                Text(
                  'Acceso Administrativo · Consola Operativa',
                  style: TextStyle(
                    fontSize: 12,
                    fontWeight: FontWeight.w500,
                    color: AppColors.mutedText,
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
      content: SingleChildScrollView(
        child: SizedBox(
          width: 380,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              if (_error != null) ...[
                ErrorBanner(
                  error: _error!,
                  onDismiss: () => setState(() => _error = null),
                ),
                const SizedBox(height: 12),
              ],
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.skySoft,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: AppColors.secondary.withValues(alpha: 0.25),
                  ),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(
                      Icons.mark_email_read_outlined,
                      size: 18,
                      color: AppColors.secondary,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Text(
                            'Código enviado por correo electrónico',
                            style: TextStyle(
                              fontWeight: FontWeight.w700,
                              fontSize: 12.5,
                              color: AppColors.primary,
                            ),
                          ),
                          const SizedBox(height: 2),
                          Text(
                            _infoMessage.isNotEmpty
                                ? _infoMessage
                                : 'Ingresa el código de 6 dígitos enviado a $maskedTarget.',
                            style: const TextStyle(
                              fontSize: 12,
                              color: AppColors.sky950,
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              if (!kIsWeb && _huellaDisponible) ...[
                FilledButton.icon(
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.secondary,
                  ),
                  onPressed: _loading ? null : _verifyWithFingerprint,
                  icon: const Icon(Icons.fingerprint),
                  label: const Text('Verificar con huella digital'),
                ),
                const Padding(
                  padding: EdgeInsets.symmetric(vertical: 12),
                  child: Row(
                    children: [
                      Expanded(child: Divider()),
                      Padding(
                        padding: EdgeInsets.symmetric(horizontal: 10),
                        child: Text(
                          'o ingresa el código del correo',
                          style: TextStyle(
                            fontSize: 11.5,
                            color: AppColors.mutedText,
                          ),
                        ),
                      ),
                      Expanded(child: Divider()),
                    ],
                  ),
                ),
              ],
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    'Código de seguridad (6 dígitos)',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary,
                    ),
                  ),
                  if (_secondsLeft > 0)
                    Row(
                      children: [
                        const Icon(
                          Icons.schedule,
                          size: 13,
                          color: AppColors.secondary,
                        ),
                        const SizedBox(width: 4),
                        Text(
                          formatCountdown(_secondsLeft),
                          style: const TextStyle(
                            fontFamily: 'monospace',
                            fontSize: 12,
                            fontWeight: FontWeight.w700,
                            color: AppColors.primary,
                          ),
                        ),
                      ],
                    )
                  else
                    const Text(
                      'Expirado',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.w700,
                        color: Color(0xFFD97706),
                      ),
                    ),
                ],
              ),
              const SizedBox(height: 10),
              OtpCodeInput(
                value: _otpCode,
                autoFocus: kIsWeb || !_huellaDisponible,
                enabled: !_loading,
                hasError: _otpFieldError != null,
                onChange: (val) {
                  setState(() {
                    _otpCode = val;
                    if (_otpFieldError != null) _otpFieldError = null;
                  });
                },
              ),
              if (_otpFieldError != null) ...[
                const SizedBox(height: 6),
                Text(
                  _otpFieldError!,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.danger,
                    fontWeight: FontWeight.w600,
                  ),
                ),
              ],
              const SizedBox(height: 8),
              Align(
                alignment: Alignment.centerRight,
                child: TextButton.icon(
                  onPressed: (_resending || _loading) ? null : _resendOtp,
                  icon: _resending
                      ? const SizedBox(
                          width: 14,
                          height: 14,
                          child: CircularProgressIndicator(strokeWidth: 2),
                        )
                      : const Icon(Icons.refresh, size: 16),
                  label: Text(_resending ? 'Reenviando...' : 'Reenviar código'),
                ),
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: _loading ? null : () => Navigator.of(context).pop(false),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          style: FilledButton.styleFrom(minimumSize: const Size(140, 44)),
          onPressed: _loading ? null : _verifyWithOtp,
          child: _loading
              ? const SizedBox(
                  height: 18,
                  width: 18,
                  child: CircularProgressIndicator(
                    strokeWidth: 2,
                    color: Colors.white,
                  ),
                )
              : const Text('Verificar e ingresar'),
        ),
      ],
    );
  }
}
