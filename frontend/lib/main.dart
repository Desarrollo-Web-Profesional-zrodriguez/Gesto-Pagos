import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/router/app_router.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';

void main() {
  runApp(const ProviderScope(child: GestoPagosApp()));
}

class GestoPagosApp extends ConsumerWidget {
  const GestoPagosApp({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final router = ref.watch(appRouterProvider);
    return MaterialApp.router(
      title: 'GestoPagos',
      debugShowCheckedModeBanner: false,
      theme: AppTheme.lightTheme,
      routerConfig: router,
    );
  }
}
