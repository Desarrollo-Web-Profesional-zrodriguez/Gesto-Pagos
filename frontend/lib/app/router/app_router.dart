import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/features/auth/presentation/login_screen.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/backoffice/presentation/backoffice_screen.dart';
import 'package:gestopago_app/features/cliente_portal/presentation/cliente_shell_screen.dart';
import 'package:gestopago_app/features/onboarding/presentation/onboarding_wizard_screen.dart';
import 'package:go_router/go_router.dart';

class _RouterRefreshNotifier extends ChangeNotifier {
  _RouterRefreshNotifier(Ref ref) {
    ref.listen<SessionState>(
      sessionControllerProvider,
      (prev, next) {
        if (prev?.isAuthenticated != next.isAuthenticated) {
          notifyListeners();
        }
      },
    );
  }
}

final appRouterProvider = Provider<GoRouter>((ref) {
  final refreshNotifier = _RouterRefreshNotifier(ref);
  ref.onDispose(refreshNotifier.dispose);

  return GoRouter(
    initialLocation: '/login',
    refreshListenable: refreshNotifier,
    redirect: (context, state) {
      final isAuth = ref.read(sessionControllerProvider).isAuthenticated;
      final loc = state.matchedLocation;

      if (loc.startsWith('/cliente') && !isAuth) {
        return '/login';
      }
      if (loc == '/login' && isAuth) {
        return '/cliente';
      }
      return null;
    },
    routes: [
      GoRoute(
        path: '/login',
        builder: (_, _) => const LoginScreen(),
      ),
      GoRoute(
        path: '/onboarding',
        builder: (_, _) => const OnboardingWizardScreen(),
      ),
      GoRoute(
        path: '/cliente',
        builder: (_, _) => const ClienteShellScreen(),
      ),
      GoRoute(
        path: '/backoffice',
        builder: (_, _) => const BackofficeScreen(),
      ),
    ],
  );
});
