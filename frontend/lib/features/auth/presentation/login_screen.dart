import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/biometrics/face_embedding_provider.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/backoffice/presentation/backoffice_login_dialog.dart';
import 'package:go_router/go_router.dart';

class LoginScreen extends ConsumerStatefulWidget {
  const LoginScreen({super.key});

  @override
  ConsumerState<LoginScreen> createState() => _LoginScreenState();
}

class _LoginScreenState extends ConsumerState<LoginScreen> {
  final _formKey = GlobalKey<FormState>();
  final _emailCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  bool _obscurePassword = true;
  bool _submitting = false;
  Object? _error;

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _error = null;
      _submitting = true;
    });

    try {
      await ref.read(sessionControllerProvider.notifier).loginWithPassword(
            username: _emailCtrl.text,
            password: _passwordCtrl.text,
          );
      if (!mounted) return;
      context.go('/cliente');
    } catch (e) {
      // Si las credenciales no corresponden a un cliente, verificar acceso administrativo 2FA.
      try {
        final dio = ref.read(dioProvider);
        final resp = await dio.post<Map<String, dynamic>>(
          '/auth/backoffice/login',
          data: {
            'email': _emailCtrl.text.trim(),
            'password': _passwordCtrl.text,
            'plataforma': kIsWeb ? 'WEB' : 'MOBILE',
          },
        );
        final data = resp.data ?? {};
        final challengeId = (data['challengeId'] ?? '').toString();
        if (data['requiere2fa'] == true && challengeId.isNotEmpty && mounted) {
          _passwordCtrl.clear();
          setState(() => _submitting = false);
          final ok = await showBackoffice2faDialog(
            context,
            ref,
            challengeId: challengeId,
            email: _emailCtrl.text.trim(),
            emailEnmascarado: (data['emailEnmascarado'] ?? '').toString(),
            infoMessage:
                (data['mensaje'] ?? 'Código de verificación enviado al correo.')
                    .toString(),
            expiraEnSegundos:
                (data['expiraEnSegundos'] as num?)?.toInt() ?? 300,
          );
          if (ok && mounted) {
            context.go('/backoffice');
          }
          return;
        }
      } on DioException {
        // No es acceso administrativo: mostrar error de cliente
      } catch (_) {
        // Ignorar y mostrar error original
      }
      if (!mounted) return;
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _submitWithFace() async {
    final emailErr = BackendValidators.email(_emailCtrl.text);
    if (emailErr != null) {
      setState(() {
        _error =
            'Ingresa primero tu correo electrónico registrado para iniciar sesión con reconocimiento facial.';
      });
      return;
    }

    setState(() => _error = null);
    final embedding =
        await ref.read(faceEmbeddingProvider).captureFacialEmbedding(
              context,
              title: 'Acceso con Reconocimiento Facial',
              subtitle:
                  'Verifica tu identidad frente a la cámara para entrar a tu cuenta',
            );
    if (embedding == null || embedding.isEmpty || !mounted) return;

    setState(() => _submitting = true);
    try {
      await ref.read(sessionControllerProvider.notifier).loginWithBiometrics(
            username: _emailCtrl.text,
            biometricoFacialEmbedding: embedding,
          );
      if (!mounted) return;
      context.go('/cliente');
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionControllerProvider);
    final busy = session.isBusy || _submitting;

    return Scaffold(
      backgroundColor: AppColors.slate950,
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Column(
                    children: [
                      Container(
                        width: 56,
                        height: 56,
                        decoration: BoxDecoration(
                          borderRadius: BorderRadius.circular(18),
                          gradient: LinearGradient(
                            begin: Alignment.topLeft,
                            end: Alignment.bottomRight,
                            colors: [
                              AppColors.secondary.withValues(alpha: 0.35),
                              AppColors.accent.withValues(alpha: 0.2),
                            ],
                          ),
                          border: Border.all(
                            color: AppColors.secondary.withValues(alpha: 0.45),
                          ),
                        ),
                        child: const Icon(
                          Icons.shield_outlined,
                          color: Color(0xFF7DD3FC),
                          size: 28,
                        ),
                      ),
                      const SizedBox(height: 14),
                      const Text.rich(
                        TextSpan(
                          text: 'Gesto',
                          style: TextStyle(
                            fontSize: 28,
                            fontWeight: FontWeight.w800,
                            color: Colors.white,
                            letterSpacing: -0.5,
                          ),
                          children: [
                            TextSpan(
                              text: 'Pagos',
                              style: TextStyle(color: Color(0xFF38BDF8)),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Acceso protegido a tu cuenta digital y portal de pagos',
                        textAlign: TextAlign.center,
                        style: TextStyle(
                          color: Color(0xFF94A3B8),
                          fontSize: 12.5,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 24),
                  Container(
                    padding: const EdgeInsets.all(22),
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(24),
                      boxShadow: [
                        BoxShadow(
                          color: Colors.black.withValues(alpha: 0.25),
                          blurRadius: 24,
                          offset: const Offset(0, 12),
                        ),
                      ],
                    ),
                    child: Form(
                      key: _formKey,
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              const Expanded(
                                child: Column(
                                  crossAxisAlignment: CrossAxisAlignment.start,
                                  children: [
                                    Text(
                                      'Iniciar Sesión',
                                      style: TextStyle(
                                        fontSize: 21,
                                        fontWeight: FontWeight.w800,
                                        color: AppColors.primary,
                                      ),
                                    ),
                                    SizedBox(height: 2),
                                    Text(
                                      'Sesión protegida con cierre automático por inactividad',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: AppColors.mutedText,
                                      ),
                                    ),
                                  ],
                                ),
                              ),
                              Container(
                                width: 42,
                                height: 42,
                                decoration: BoxDecoration(
                                  color: AppColors.skySoft,
                                  borderRadius: BorderRadius.circular(14),
                                ),
                                child: const Icon(
                                  Icons.lock_outline,
                                  color: AppColors.secondary,
                                  size: 20,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 16),
                          if (session.expiredMessage != null) ...[
                            Container(
                              padding: const EdgeInsets.all(12),
                              decoration: BoxDecoration(
                                color: AppColors.amberSoft,
                                borderRadius: BorderRadius.circular(12),
                                border: Border.all(
                                  color: AppColors.warning.withValues(alpha: 0.45),
                                ),
                              ),
                              child: Row(
                                children: [
                                  const Icon(
                                    Icons.timer_off_outlined,
                                    color: Color(0xFFD97706),
                                    size: 20,
                                  ),
                                  const SizedBox(width: 10),
                                  Expanded(
                                    child: Text(
                                      session.expiredMessage!,
                                      style: const TextStyle(
                                        fontSize: 12.5,
                                        color: Color(0xFF78350F),
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                            const SizedBox(height: 14),
                          ],
                          if (_error != null) ...[
                            ErrorBanner(
                              error: _error!,
                              onDismiss: () => setState(() => _error = null),
                            ),
                            const SizedBox(height: 14),
                          ],
                          TextFormField(
                            controller: _emailCtrl,
                            keyboardType: TextInputType.emailAddress,
                            textInputAction: TextInputAction.next,
                            decoration: const InputDecoration(
                              labelText: 'Correo electrónico',
                              prefixIcon: Icon(Icons.email_outlined),
                              hintText: 'nombre@correo.com',
                            ),
                            validator: (v) => BackendValidators.email(v),
                          ),
                          const SizedBox(height: 14),
                          TextFormField(
                            controller: _passwordCtrl,
                            obscureText: _obscurePassword,
                            textInputAction: TextInputAction.done,
                            onFieldSubmitted: (_) => _submit(),
                            decoration: InputDecoration(
                              labelText: 'Contraseña',
                              prefixIcon: const Icon(Icons.lock_outline),
                              hintText: 'Ingresa tu contraseña',
                              suffixIcon: IconButton(
                                onPressed: () => setState(
                                  () => _obscurePassword = !_obscurePassword,
                                ),
                                icon: Icon(
                                  _obscurePassword
                                      ? Icons.visibility_outlined
                                      : Icons.visibility_off_outlined,
                                ),
                              ),
                            ),
                            validator: BackendValidators.passwordLogin,
                          ),
                          const SizedBox(height: 20),
                          FilledButton(
                            onPressed: busy ? null : _submit,
                            child: busy
                                ? const SizedBox(
                                    height: 22,
                                    width: 22,
                                    child: CircularProgressIndicator(
                                      strokeWidth: 2.2,
                                      color: Colors.white,
                                    ),
                                  )
                                : const Row(
                                    mainAxisAlignment: MainAxisAlignment.center,
                                    children: [
                                      Text('Iniciar sesión'),
                                      SizedBox(width: 8),
                                      Icon(Icons.arrow_forward_rounded, size: 18),
                                    ],
                                  ),
                          ),
                          if (ref
                              .watch(faceEmbeddingProvider)
                              .isCameraSupported) ...[
                            const SizedBox(height: 10),
                            OutlinedButton.icon(
                              onPressed: busy ? null : _submitWithFace,
                              icon: const Icon(
                                Icons.face_retouching_natural,
                                size: 19,
                              ),
                              label: const Text(
                                'Iniciar sesión con reconocimiento facial',
                              ),
                            ),
                          ],
                          const SizedBox(height: 18),
                          const Divider(height: 1, color: AppColors.border),
                          const SizedBox(height: 16),
                          const Text(
                            '¿Aún no tienes cuenta?',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontWeight: FontWeight.w700,
                              fontSize: 13,
                              color: AppColors.primary,
                            ),
                          ),
                          const SizedBox(height: 2),
                          const Text(
                            'Obtén tu cuenta digital de 16 dígitos al instante.',
                            textAlign: TextAlign.center,
                            style: TextStyle(
                              fontSize: 11.5,
                              color: AppColors.mutedText,
                            ),
                          ),
                          const SizedBox(height: 12),
                          OutlinedButton.icon(
                            onPressed: busy
                                ? null
                                : () => context.push('/onboarding'),
                            icon: const Icon(
                              Icons.person_add_alt_1_outlined,
                              color: AppColors.secondary,
                            ),
                            label: const Text('Crear nueva cuenta'),
                          ),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(height: 18),
                  const Text(
                    'GestoPagos · Banca digital y pagos de servicios protegidos',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      color: Color(0xFF64748B),
                      fontSize: 11.5,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
