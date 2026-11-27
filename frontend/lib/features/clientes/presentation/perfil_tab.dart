import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
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
        child: FilledButton(
          onPressed: () => ref
              .read(sessionControllerProvider.notifier)
              .refrescarPerfilYSaldo(),
          child: const Text('Cargar mi perfil'),
        ),
      );
    }

    final dom = cliente.domicilios.isNotEmpty ? cliente.domicilios.first : null;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(16),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  cliente.nombreCompleto,
                  style: const TextStyle(
                    fontSize: 18,
                    fontWeight: FontWeight.w800,
                  ),
                ),
                const SizedBox(height: 4),
                Text(
                  cliente.correoElectronico,
                  style: const TextStyle(color: AppColors.mutedText),
                ),
                const Divider(height: 22),
                _Row('CURP', cliente.curp),
                _Row('RFC', cliente.rfc),
                _Row('Nacimiento', cliente.fechaNacimiento),
                _Row('Nacionalidad', cliente.nacionalidad ?? 'N/A'),
                _Row('Estado civil', cliente.estadoCivil),
                _Row('Teléfono móvil', cliente.telefonoMovil),
                _Row('Ocupación', cliente.ocupacion),
                _Row('Empresa', cliente.empresa),
                _Row('Ingreso mensual', formatMxn(cliente.ingresoMensual)),
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
                  const Text(
                    'Domicilio registrado',
                    style: TextStyle(fontWeight: FontWeight.w700),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    '${dom.calle} ${dom.numeroExterior} ${dom.numeroInterior ?? ''}\n'
                    '${dom.colonia}, ${dom.municipio}, ${dom.estado}\n'
                    'CP ${dom.codigoPostal}, ${dom.pais}',
                    style: const TextStyle(fontSize: 13),
                  ),
                ],
              ),
            ),
          ),
        ],
        const SizedBox(height: 14),
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
          label: const Text('Editar mis datos (PATCH / PUT)'),
        ),
        const SizedBox(height: 10),
        OutlinedButton.icon(
          onPressed: () => context.push('/backoffice'),
          icon: const Icon(Icons.admin_panel_settings_outlined),
          label: const Text('Abrir módulo BackOffice / Admin'),
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

class _Row extends StatelessWidget {
  const _Row(this.label, this.value);
  final String label;
  final String value;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 4),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(fontSize: 13, color: AppColors.mutedText),
          ),
          Flexible(
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
