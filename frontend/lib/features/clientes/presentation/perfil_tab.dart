import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/biometrics/face_embedding_provider.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/presentation/cliente_edit_screen.dart';
import 'package:go_router/go_router.dart';

class PerfilTab extends ConsumerWidget {
  const PerfilTab({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final session = ref.watch(sessionControllerProvider);
    final cliente = session.cliente;

    if (cliente == null) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: FilledButton(
            onPressed: () => ref
                .read(sessionControllerProvider.notifier)
                .refrescarPerfilYSaldo(),
            child: const Text('Cargar mi perfil'),
          ),
        ),
      );
    }

    final dom = cliente.domicilios.isNotEmpty ? cliente.domicilios.first : null;
    final fullName = cliente.nombreCompleto.isNotEmpty
        ? cliente.nombreCompleto
        : [
            cliente.nombre,
            if (cliente.segundoNombre != null &&
                cliente.segundoNombre!.isNotEmpty)
              cliente.segundoNombre!,
            cliente.apellidoPaterno,
            cliente.apellidoMaterno,
          ].join(' ');

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        // Banner ejecutivo del perfil
        Container(
          decoration: BoxDecoration(
            color: Colors.white,
            borderRadius: BorderRadius.circular(20),
            border: Border.all(color: AppColors.border),
          ),
          clipBehavior: Clip.antiAlias,
          child: Column(
            children: [
              Container(
                padding: const EdgeInsets.all(18),
                decoration: const BoxDecoration(
                  gradient: LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      AppColors.slate950,
                      AppColors.primary,
                      AppColors.sky950,
                    ],
                  ),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 52,
                      height: 52,
                      decoration: BoxDecoration(
                        color: AppColors.secondary.withValues(alpha: 0.25),
                        borderRadius: BorderRadius.circular(16),
                        border: Border.all(
                          color: AppColors.secondary.withValues(alpha: 0.45),
                        ),
                      ),
                      alignment: Alignment.center,
                      child: Text(
                        getInitials(fullName),
                        style: const TextStyle(
                          color: Color(0xFF7DD3FC),
                          fontWeight: FontWeight.w800,
                          fontSize: 18,
                        ),
                      ),
                    ),
                    const SizedBox(width: 14),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Wrap(
                            spacing: 8,
                            runSpacing: 4,
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: [
                              Text(
                                fullName,
                                style: const TextStyle(
                                  color: Colors.white,
                                  fontSize: 16.5,
                                  fontWeight: FontWeight.w800,
                                ),
                              ),
                              StatusBadge(
                                estatus: cliente.activo ? 'ACTIVA' : 'INACTIVO',
                                inverse: true,
                              ),
                            ],
                          ),
                          const SizedBox(height: 4),
                          Text(
                            cliente.correoElectronico,
                            style: const TextStyle(
                              color: Color(0xFFCBD5E1),
                              fontSize: 12.5,
                            ),
                          ),
                          Text(
                            cliente.telefonoMovil,
                            style: const TextStyle(
                              color: Color(0xFF94A3B8),
                              fontSize: 12,
                              fontFamily: 'monospace',
                            ),
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              Container(
                width: double.infinity,
                padding:
                    const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
                color: const Color(0xFFF8FAFC),
                child: Wrap(
                  spacing: 12,
                  runSpacing: 8,
                  crossAxisAlignment: WrapCrossAlignment.center,
                  children: [
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Text(
                          'CURP: ',
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w700,
                            color: AppColors.mutedText,
                          ),
                        ),
                        CopyChip(value: cliente.curp, copyLabel: 'CURP'),
                      ],
                    ),
                    Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        const Text(
                          'RFC: ',
                          style: TextStyle(
                            fontSize: 11.5,
                            fontWeight: FontWeight.w700,
                            color: AppColors.mutedText,
                          ),
                        ),
                        CopyChip(value: cliente.rfc, copyLabel: 'RFC'),
                      ],
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        _FacialSecurityCard(biometriaActiva: session.tieneBiometriaFacial),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _SectionHeader(
                  icon: Icons.person_outline,
                  title: 'Identidad y Contacto',
                ),
                const Divider(height: 22, color: Color(0xFFF1F5F9)),
                _Row(
                  'Nombre(s)',
                  [
                    cliente.nombre,
                    if (cliente.segundoNombre != null &&
                        cliente.segundoNombre!.isNotEmpty)
                      cliente.segundoNombre!,
                  ].join(' '),
                ),
                _Row(
                  'Apellidos',
                  '${cliente.apellidoPaterno} ${cliente.apellidoMaterno}',
                ),
                _Row(
                  'Fecha de nacimiento',
                  cliente.edad != null
                      ? '${cliente.fechaNacimiento} (${cliente.edad} años)'
                      : cliente.fechaNacimiento,
                ),
                _Row('Sexo', cliente.sexo),
                _Row('Estado civil', cliente.estadoCivil),
                _Row('Nacionalidad', cliente.nacionalidad ?? '-'),
                _Row('Teléfono móvil', cliente.telefonoMovil),
                _Row(
                  'Teléfono alternativo',
                  (cliente.telefonoAlternativo != null &&
                          cliente.telefonoAlternativo!.isNotEmpty)
                      ? cliente.telefonoAlternativo!
                      : 'No registrado',
                ),
                _Row('Correo electrónico', cliente.correoElectronico),
              ],
            ),
          ),
        ),
        if (dom != null) ...[
          const SizedBox(height: 12),
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const _SectionHeader(
                    icon: Icons.home_outlined,
                    title: 'Domicilio Registrado',
                  ),
                  const Divider(height: 22, color: Color(0xFFF1F5F9)),
                  Text(
                    '${dom.calle} ${dom.numeroExterior}'
                    '${(dom.numeroInterior != null && dom.numeroInterior!.isNotEmpty) ? ', Int. ${dom.numeroInterior}' : ''}',
                    style: const TextStyle(
                      fontSize: 13.5,
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary,
                    ),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${dom.colonia}, ${dom.municipio}, ${dom.estado}\nCP ${dom.codigoPostal} · ${dom.pais}',
                    style: const TextStyle(
                      fontSize: 12.5,
                      color: AppColors.mutedText,
                    ),
                  ),
                ],
              ),
            ),
          ),
        ],
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const _SectionHeader(
                  icon: Icons.work_outline,
                  title: 'Actividad Económica',
                ),
                const Divider(height: 22, color: Color(0xFFF1F5F9)),
                _Row('Ocupación', cliente.ocupacion),
                _Row('Empresa', cliente.empresa),
                _Row('Ingreso mensual', formatMxn(cliente.ingresoMensual)),
              ],
            ),
          ),
        ),
        const SizedBox(height: 16),
        FilledButton.icon(
          onPressed: () async {
            final updated = await Navigator.of(context).push<ClienteDto>(
              MaterialPageRoute<ClienteDto>(
                builder: (_) => ClienteEditScreen(cliente: cliente),
              ),
            );
            if (updated != null) {
              ref
                  .read(sessionControllerProvider.notifier)
                  .actualizarClienteLocal(updated);
            }
          },
          icon: const Icon(Icons.edit_outlined),
          label: const Text('Editar mis datos'),
        ),
        const SizedBox(height: 10),
        OutlinedButton.icon(
          style: OutlinedButton.styleFrom(
            foregroundColor: AppColors.danger,
            side: const BorderSide(color: AppColors.danger),
          ),
          onPressed: () async {
            await ref.read(sessionControllerProvider.notifier).logout();
            if (context.mounted) context.go('/login');
          },
          icon: const Icon(Icons.logout),
          label: const Text('Cerrar sesión'),
        ),
      ],
    );
  }
}

class _FacialSecurityCard extends ConsumerStatefulWidget {
  const _FacialSecurityCard({required this.biometriaActiva});

  final bool biometriaActiva;

  @override
  ConsumerState<_FacialSecurityCard> createState() =>
      _FacialSecurityCardState();
}

class _FacialSecurityCardState extends ConsumerState<_FacialSecurityCard> {
  bool _saving = false;
  Object? _error;

  Future<String?> _promptPassword({
    required String title,
    required String subtitle,
  }) {
    return showDialog<String>(
      context: context,
      builder: (_) => _PasswordConfirmDialog(
        title: title,
        subtitle: subtitle,
      ),
    );
  }

  Future<void> _registrarOActualizar() async {
    setState(() => _error = null);
    final password = await _promptPassword(
      title: widget.biometriaActiva
          ? 'Actualizar Reconocimiento Facial'
          : 'Activar Reconocimiento Facial',
      subtitle:
          'Por tu seguridad, confirma tu contraseña actual antes de abrir el escáner facial.',
    );
    if (password == null || !mounted) return;

    final embedding =
        await ref.read(faceEmbeddingProvider).captureFacialEmbedding(
              context,
              title: widget.biometriaActiva
                  ? 'Actualizar tu Rostro'
                  : 'Registrar tu Rostro',
              subtitle:
                  'Podrás usarlo para iniciar sesión y autorizar pagos de servicios',
            );
    if (embedding == null || embedding.isEmpty || !mounted) return;

    setState(() => _saving = true);
    try {
      final res = await ref
          .read(sessionControllerProvider.notifier)
          .configurarBiometriaFacial(
            passwordActual: password,
            embedding: embedding,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text(res.mensaje),
        ),
      );
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  Future<void> _desactivar() async {
    setState(() => _error = null);
    final password = await _promptPassword(
      title: 'Desactivar Reconocimiento Facial',
      subtitle:
          'Confirma tu contraseña actual para eliminar el registro facial de tu cuenta.',
    );
    if (password == null || !mounted) return;

    setState(() => _saving = true);
    try {
      final res = await ref
          .read(sessionControllerProvider.notifier)
          .configurarBiometriaFacial(
            passwordActual: password,
            eliminar: true,
          );
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text(res.mensaje),
        ),
      );
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final cameraSupported = ref.watch(faceEmbeddingProvider).isCameraSupported;

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: widget.biometriaActiva
                        ? AppColors.emeraldSoft
                        : AppColors.skySoft,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: Icon(
                    Icons.face_retouching_natural,
                    color: widget.biometriaActiva
                        ? AppColors.accent
                        : AppColors.secondary,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 10),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Reconocimiento Facial',
                        style: TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.w800,
                          color: AppColors.primary,
                        ),
                      ),
                      Text(
                        'Inicio de sesión rápido y autorización de pagos',
                        style: TextStyle(
                          fontSize: 11.5,
                          color: AppColors.mutedText,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                  decoration: BoxDecoration(
                    color: widget.biometriaActiva
                        ? AppColors.emeraldSoft
                        : AppColors.background,
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: widget.biometriaActiva
                          ? AppColors.accent.withValues(alpha: 0.4)
                          : AppColors.border,
                    ),
                  ),
                  child: Text(
                    widget.biometriaActiva ? 'Activo' : 'Sin activar',
                    style: TextStyle(
                      fontSize: 11.5,
                      fontWeight: FontWeight.w700,
                      color: widget.biometriaActiva
                          ? const Color(0xFF047857)
                          : AppColors.mutedText,
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            Text(
              widget.biometriaActiva
                  ? 'Tu rostro está vinculado a tu perfil y protege todas tus cuentas para entrar a la app y autorizar pagos de servicios.'
                  : 'Registra tu rostro con la cámara frontal para entrar sin escribir contraseña y autorizar tus pagos en segundos.',
              style: const TextStyle(
                fontSize: 12.5,
                color: AppColors.mutedText,
              ),
            ),
            if (_error != null) ...[
              const SizedBox(height: 12),
              ErrorBanner(
                error: _error!,
                onDismiss: () => setState(() => _error = null),
              ),
            ],
            if (cameraSupported) ...[
              const SizedBox(height: 14),
              if (!widget.biometriaActiva)
                FilledButton.icon(
                  style: FilledButton.styleFrom(
                    backgroundColor: AppColors.secondary,
                  ),
                  onPressed: _saving ? null : _registrarOActualizar,
                  icon: _saving
                      ? const SizedBox(
                          width: 18,
                          height: 18,
                          child: CircularProgressIndicator(
                            strokeWidth: 2,
                            color: Colors.white,
                          ),
                        )
                      : const Icon(Icons.camera_front_outlined, size: 19),
                  label: Text(
                    _saving
                        ? 'Guardando configuración...'
                        : 'Activar reconocimiento facial',
                  ),
                )
              else
                Row(
                  children: [
                    Expanded(
                      child: OutlinedButton.icon(
                        onPressed: _saving ? null : _registrarOActualizar,
                        icon: const Icon(Icons.refresh, size: 18),
                        label: const Text('Actualizar rostro'),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: OutlinedButton.icon(
                        style: OutlinedButton.styleFrom(
                          foregroundColor: AppColors.danger,
                          side: const BorderSide(color: AppColors.danger),
                        ),
                        onPressed: _saving ? null : _desactivar,
                        icon: const Icon(Icons.person_off_outlined, size: 18),
                        label: const Text('Desactivar'),
                      ),
                    ),
                  ],
                ),
            ],
          ],
        ),
      ),
    );
  }
}

class _PasswordConfirmDialog extends StatefulWidget {
  const _PasswordConfirmDialog({
    required this.title,
    required this.subtitle,
  });

  final String title;
  final String subtitle;

  @override
  State<_PasswordConfirmDialog> createState() => _PasswordConfirmDialogState();
}

class _PasswordConfirmDialogState extends State<_PasswordConfirmDialog> {
  final _formKey = GlobalKey<FormState>();
  final _passCtrl = TextEditingController();
  bool _obscure = true;

  @override
  void dispose() {
    _passCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return AlertDialog(
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(22)),
      title: Text(
        widget.title,
        style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w800),
      ),
      content: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                widget.subtitle,
                style: const TextStyle(
                  fontSize: 12.5,
                  color: AppColors.mutedText,
                ),
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _passCtrl,
                obscureText: _obscure,
                decoration: InputDecoration(
                  labelText: 'Contraseña actual',
                  prefixIcon: const Icon(Icons.lock_outline),
                  suffixIcon: IconButton(
                    onPressed: () => setState(() => _obscure = !_obscure),
                    icon: Icon(
                      _obscure
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                    ),
                  ),
                ),
                validator: BackendValidators.passwordLogin,
              ),
            ],
          ),
        ),
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(context).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          style: FilledButton.styleFrom(minimumSize: const Size(110, 42)),
          onPressed: () {
            if (_formKey.currentState?.validate() ?? false) {
              Navigator.of(context).pop(_passCtrl.text);
            }
          },
          child: const Text('Continuar'),
        ),
      ],
    );
  }
}

class _SectionHeader extends StatelessWidget {
  const _SectionHeader({required this.icon, required this.title});
  final IconData icon;
  final String title;

  @override
  Widget build(BuildContext context) {
    return Row(
      children: [
        Container(
          width: 34,
          height: 34,
          decoration: BoxDecoration(
            color: AppColors.skySoft,
            borderRadius: BorderRadius.circular(10),
          ),
          child: Icon(icon, size: 18, color: AppColors.secondary),
        ),
        const SizedBox(width: 10),
        Text(
          title,
          style: const TextStyle(
            fontSize: 15,
            fontWeight: FontWeight.w800,
            color: AppColors.primary,
          ),
        ),
      ],
    );
  }
}

class _Row extends StatelessWidget {
  const _Row(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 5),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Expanded(
            flex: 2,
            child: Text(
              label,
              style: const TextStyle(
                fontSize: 12.5,
                color: AppColors.mutedText,
              ),
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
