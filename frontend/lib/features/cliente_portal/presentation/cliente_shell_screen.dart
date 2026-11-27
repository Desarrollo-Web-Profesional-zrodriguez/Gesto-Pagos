import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
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

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionControllerProvider);
    final lowTime =
        session.secondsRemaining > 0 && session.secondsRemaining <= 60;
    final cuentaMask = maskCuenta(session.numeroCuentaPrincipal);

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
        backgroundColor: AppColors.slate950,
        title: Row(
          children: [
            Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                color: AppColors.secondary.withValues(alpha: 0.2),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(
                  color: AppColors.secondary.withValues(alpha: 0.4),
                ),
              ),
              child: const Icon(
                Icons.shield_outlined,
                color: Color(0xFF7DD3FC),
                size: 18,
              ),
            ),
            const SizedBox(width: 10),
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'GestoPagos',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
                  ),
                  if (session.numeroCuentaPrincipal != null)
                    Text(
                      'Cuenta $cuentaMask',
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        fontSize: 11,
                        color: Color(0xFF94A3B8),
                        fontFamily: 'monospace',
                      ),
                    ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          Tooltip(
            message: 'Toca para renovar tu sesión activa 5 minutos',
            child: InkWell(
              onTap: () async {
                await ref
                    .read(sessionControllerProvider.notifier)
                    .renovarSesionRemota();
                if (!context.mounted) return;
                ScaffoldMessenger.of(context)
                  ..hideCurrentSnackBar()
                  ..showSnackBar(
                    const SnackBar(
                      behavior: SnackBarBehavior.floating,
                      content: Text(
                        'Sesión renovada. Temporizador restablecido a 5 minutos.',
                      ),
                    ),
                  );
              },
              borderRadius: BorderRadius.circular(20),
              child: Container(
                padding:
                    const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: lowTime
                      ? AppColors.warning.withValues(alpha: 0.22)
                      : Colors.white.withValues(alpha: 0.1),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(
                    color: lowTime
                        ? const Color(0xFFFCD34D)
                        : Colors.white.withValues(alpha: 0.16),
                  ),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    Icon(
                      Icons.schedule,
                      size: 15,
                      color: lowTime
                          ? const Color(0xFFFCD34D)
                          : const Color(0xFF7DD3FC),
                    ),
                    const SizedBox(width: 5),
                    Text(
                      formatCountdown(session.secondsRemaining),
                      style: TextStyle(
                        fontFamily: 'monospace',
                        color:
                            lowTime ? const Color(0xFFFCD34D) : Colors.white,
                        fontWeight: FontWeight.w800,
                        fontSize: 12.5,
                      ),
                    ),
                  ],
                ),
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
              backgroundColor: AppColors.amberSoft,
              content: Text(
                'Tu sesión expira en ${formatCountdown(session.secondsRemaining)} por inactividad.',
                style: const TextStyle(
                  fontSize: 13,
                  color: Color(0xFF78350F),
                ),
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
        backgroundColor: Colors.white,
        indicatorColor: AppColors.skySoft,
        selectedIndex: _index,
        onDestinationSelected: (v) => setState(() => _index = v),
        destinations: const [
          NavigationDestination(
            icon: Icon(Icons.account_balance_wallet_outlined),
            selectedIcon: Icon(
              Icons.account_balance_wallet,
              color: AppColors.secondary,
            ),
            label: 'Cuenta',
          ),
          NavigationDestination(
            icon: Icon(Icons.storefront_outlined),
            selectedIcon: Icon(
              Icons.storefront,
              color: AppColors.secondary,
            ),
            label: 'Servicios',
          ),
          NavigationDestination(
            icon: Icon(Icons.receipt_long_outlined),
            selectedIcon: Icon(
              Icons.receipt_long,
              color: AppColors.secondary,
            ),
            label: 'Historial',
          ),
          NavigationDestination(
            icon: Icon(Icons.person_outline),
            selectedIcon: Icon(
              Icons.person,
              color: AppColors.secondary,
            ),
            label: 'Perfil',
          ),
        ],
      ),
    );
  }
}
