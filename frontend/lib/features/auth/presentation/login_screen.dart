import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/biometrics/face_embedding_provider.dart';
import 'package:gestopago_app/core/config/app_config.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
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
  final _embeddingCtrl = TextEditingController(
    text: '[0.12, -0.45, 0.89, -0.05, 0.67]',
  );
  bool _obscurePassword = true;
  bool _useBiometric = false;
  Object? _error;

  @override
  void dispose() {
    _emailCtrl.dispose();
    _passwordCtrl.dispose();
    _embeddingCtrl.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() => _error = null);
    try {
      if (_useBiometric) {
        await ref.read(sessionControllerProvider.notifier).loginWithBiometrics(
              username: _emailCtrl.text,
              biometricoFacialEmbedding: _embeddingCtrl.text,
            );
      } else {
        await ref.read(sessionControllerProvider.notifier).loginWithPassword(
              username: _emailCtrl.text,
              password: _passwordCtrl.text,
            );
      }
      if (!mounted) return;
      context.go('/cliente');
    } catch (e) {
      if (!mounted) return;
      setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionControllerProvider);
    final baseUrl = ref.watch(apiBaseUrlProvider);
    final biometrics = ref.watch(faceEmbeddingProvider);

    return Scaffold(
      body: SafeArea(
        child: Center(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(20),
            child: ConstrainedBox(
              constraints: const BoxConstraints(maxWidth: 440),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(
                          horizontal: 12,
                          vertical: 6,
                        ),
                        decoration: BoxDecoration(
                          color: AppColors.primary,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: const Text(
                          'GestoPagos Móvil',
                          style: TextStyle(
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                            fontSize: 12,
                          ),
                        ),
                      ),
                      IconButton(
                        tooltip: 'Configurar servidor ($baseUrl)',
                        onPressed: () => showServerConfigDialog(context, ref),
                        icon: const Icon(Icons.dns_outlined),
                      ),
                    ],
                  ),
                  const SizedBox(height: 16),
                  const Text(
                    'Bienvenido de nuevo',
                    style: TextStyle(
                      fontSize: 28,
                      fontWeight: FontWeight.w800,
                      color: AppColors.primary,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'Inicia sesión para consultar tu cuenta, pagar servicios o hacer recargas. Servidor: $baseUrl',
                    style: const TextStyle(
                      color: AppColors.mutedText,
                      fontSize: 13,
                    ),
                  ),
                  const SizedBox(height: 20),
                  if (session.expiredMessage != null) ...[
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppColors.warning.withValues(alpha: 0.14),
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: AppColors.warning.withValues(alpha: 0.4),
                        ),
                      ),
                      child: Row(
                        children: [
                          const Icon(
                            Icons.timer_off_outlined,
                            color: AppColors.warning,
                          ),
                          const SizedBox(width: 10),
                          Expanded(
                            child: Text(
                              session.expiredMessage!,
                              style: const TextStyle(fontSize: 13),
                            ),
                          ),
                        ],
                      ),
                    ),
                    const SizedBox(height: 16),
                  ],
                  if (_error != null) ...[
                    ErrorBanner(error: _error!),
                    const SizedBox(height: 16),
                  ],
                  Card(
                    child: Padding(
                      padding: const EdgeInsets.all(20),
                      child: Form(
                        key: _formKey,
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.stretch,
                          children: [
                            TextFormField(
                              controller: _emailCtrl,
                              keyboardType: TextInputType.emailAddress,
                              textInputAction: TextInputAction.next,
                              decoration: const InputDecoration(
                                labelText: 'Correo electrónico',
                                prefixIcon: Icon(Icons.email_outlined),
                                hintText: 'usuario@dominio.com',
                              ),
                              validator: (v) => BackendValidators.email(v),
                            ),
                            const SizedBox(height: 14),
                            SegmentedButton<bool>(
                              segments: const [
                                ButtonSegment(
                                  value: false,
                                  label: Text('Contraseña'),
                                  icon: Icon(Icons.key_outlined),
                                ),
                                ButtonSegment(
                                  value: true,
                                  label: Text('Biometría Facial'),
                                  icon: Icon(Icons.face_retouching_natural),
                                ),
                              ],
                              selected: {_useBiometric},
                              onSelectionChanged: (val) {
                                setState(() => _useBiometric = val.first);
                              },
                            ),
                            const SizedBox(height: 14),
                            if (!_useBiometric) ...[
                              TextFormField(
                                controller: _passwordCtrl,
                                obscureText: _obscurePassword,
                                textInputAction: TextInputAction.done,
                                onFieldSubmitted: (_) => _submit(),
                                decoration: InputDecoration(
                                  labelText: 'Contraseña',
                                  prefixIcon: const Icon(Icons.lock_outline),
                                  suffixIcon: IconButton(
                                    onPressed: () => setState(
                                      () =>
                                          _obscurePassword = !_obscurePassword,
                                    ),
                                    icon: Icon(
                                      _obscurePassword
                                          ? Icons.visibility_outlined
                                          : Icons.visibility_off_outlined,
                                    ),
                                  ),
                                ),
                                validator: (v) => (v == null || v.isEmpty)
                                    ? 'Ingresa tu contraseña'
                                    : null,
                              ),
                            ] else ...[
                              TextFormField(
                                controller: _embeddingCtrl,
                                maxLines: 2,
                                textInputAction: TextInputAction.done,
                                onFieldSubmitted: (_) => _submit(),
                                decoration: const InputDecoration(
                                  labelText:
                                      'Vector biométrico facial (MediaPipe)',
                                  prefixIcon: Icon(Icons.face),
                                  hintText: '[0.12, -0.45, 0.89, -0.05, 0.67]',
                                ),
                                validator: (v) =>
                                    (v == null || v.trim().isEmpty)
                                        ? 'Ingresa el vector biométrico'
                                        : null,
                              ),
                            ],
                            const SizedBox(height: 18),
                            FilledButton(
                              onPressed: session.isBusy ? null : _submit,
                              child: session.isBusy
                                  ? const SizedBox(
                                      height: 22,
                                      width: 22,
                                      child: CircularProgressIndicator(
                                        strokeWidth: 2.2,
                                        color: Colors.white,
                                      ),
                                    )
                                  : const Text('Iniciar sesión'),
                            ),
                            const SizedBox(height: 12),
                            Container(
                              padding: const EdgeInsets.all(10),
                              decoration: BoxDecoration(
                                color: AppColors.background,
                                borderRadius: BorderRadius.circular(10),
                              ),
                              child: Row(
                                children: [
                                  const Icon(
                                    Icons.face_retouching_natural,
                                    color: AppColors.secondary,
                                    size: 20,
                                  ),
                                  const SizedBox(width: 8),
                                  Expanded(
                                    child: Text(
                                      biometrics.statusMessage,
                                      style: const TextStyle(
                                        fontSize: 11.5,
                                        color: AppColors.mutedText,
                                      ),
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ),
                  const SizedBox(height: 16),
                  OutlinedButton.icon(
                    onPressed: () => context.push('/onboarding'),
                    icon: const Icon(Icons.person_add_alt_1_outlined),
                    label: const Text('Registrar nuevo cliente (Onboarding)'),
                  ),
                  const SizedBox(height: 10),
                  TextButton.icon(
                    onPressed: () => context.push('/backoffice'),
                    icon: const Icon(Icons.admin_panel_settings_outlined),
                    label: const Text('Abrir Portal BackOffice / Admin'),
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
