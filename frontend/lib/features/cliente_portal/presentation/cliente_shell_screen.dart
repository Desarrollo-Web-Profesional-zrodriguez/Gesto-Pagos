import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/catalogo_productos/presentation/catalogo_productos_tab.dart';
import 'package:gestopago_app/features/clientes/presentation/perfil_tab.dart';
import 'package:gestopago_app/features/cuentas/presentation/cuenta_home_tab.dart';
import 'package:gestopago_app/features/pagos/presentation/historial_tab.dart';
import 'package:go_router/go_router.dart';

class ClienteShellScreen extends ConsumerStatefulWidget {
  const ClienteShellScreen({super.key});

  @override
  ConsumerState<ClienteShellScreen> createState() => _ClienteShellScreenState();
}

class _ClienteShellScreenState extends ConsumerState<ClienteShellScreen> {
  int _index = 0;

  String _formatCountdown(int totalSeconds) {
    final mins = (totalSeconds ~/ 60).toString().padLeft(2, '0');
    final secs = (totalSeconds % 60).toString().padLeft(2, '0');
    return '$mins:$secs';
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionControllerProvider);
    final lowTime = session.secondsRemaining > 0 && session.secondsRemaining <= 60;

    final tabs = [
      CuentaHomeTab(
        onGoToCatalogo: () => setState(() => _index = 1),
        onGoToHistorial: () => setState(() => _index = 2),
      ),
      const CatalogoProductosTab(),
      const HistorialTab(),
      const PerfilTab(),
    ];

    return Scaffold(
      appBar: AppBar(
        title: const Text('GestoPagos'),
        actions: [
          TextButton.icon(
            onPressed: () => ref
                .read(sessionControllerProvider.notifier)
                .renovarSesionRemota(),
            icon: Icon(
              Icons.timer_outlined,
              size: 18,
              color: lowTime ? const Color(0xFFFCD34D) : Colors.white70,
            ),
            label: Text(
              _formatCountdown(session.secondsRemaining),
              style: TextStyle(
                color: lowTime ? const Color(0xFFFCD34D) : Colors.white,
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
          IconButton(
            tooltip: 'Cerrar sesión',
            onPressed: () async {
              await ref.read(sessionControllerProvider.notifier).logout();
              if (context.mounted) context.go('/login');
            },
            icon: const Icon(Icons.logout),
          ),
        ],
      ),
      body: Column(
        children: [
          if (lowTime)
            MaterialBanner(
              backgroundColor: AppColors.warning.withValues(alpha: 0.16),
              content: Text(
                'Tu sesión expira en ${_formatCountdown(session.secondsRemaining)} por inactividad.',
                style: const TextStyle(fontSize: 13),
              ),
              actions: [
                TextButton(
                  onPressed: () => ref
                      .read(sessionControllerProvider.notifier)
                      .renovarSesionRemota(),
                  child: const Text('Renovar 5 min'),
                ),
              ],
            ),
          Expanded(child: tabs[_index]),
        ],
      ),
      bottomNavigationBar: NavigationBar(
        selectedIndex: _index,
        onDestinationSelected: (v) => setState(() => _index = v),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.account_balance_wallet_outlined),
            selectedIcon: Icon(Icons.account_balance_wallet),
            label: 'Cuenta',
          ),
          NavigationDestination(
            icon: Icon(Icons.grid_view_outlined),
            selectedIcon: Icon(Icons.grid_view),
            label: 'Servicios',
          ),
          NavigationDestination(
            icon: Icon(Icons.receipt_long_outlined),
            selectedIcon: Icon(Icons.receipt_long),
            label: 'Historial',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(Icons.person),
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
