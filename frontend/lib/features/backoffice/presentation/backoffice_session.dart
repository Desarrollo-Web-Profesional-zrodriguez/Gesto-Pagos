import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';

class BackofficeSessionState {
  const BackofficeSessionState({
    this.tokenBackoffice,
    this.email,
  });

  final String? tokenBackoffice;
  final String? email;

  bool get isAuthenticated =>
      tokenBackoffice != null && tokenBackoffice!.trim().isNotEmpty;
}

class BackofficeSessionNotifier extends Notifier<BackofficeSessionState> {
  @override
  BackofficeSessionState build() => const BackofficeSessionState();

  void establecerSesion({
    required String tokenBackoffice,
    String? email,
  }) {
    state = BackofficeSessionState(
      tokenBackoffice: tokenBackoffice.trim(),
      email: email?.trim(),
    );
  }

  Future<void> logout() async {
    final token = state.tokenBackoffice;
    state = const BackofficeSessionState();
    if (token != null && token.isNotEmpty) {
      try {
        await ref.read(apiClientProvider).post(
          '/auth/backoffice/logout',
          body: <String, dynamic>{},
          headers: {'Authorization': 'Bearer $token'},
        );
      } catch (_) {
        // El cierre local siempre procede aunque falle la red.
      }
    }
  }
}

final backofficeSessionProvider =
    NotifierProvider<BackofficeSessionNotifier, BackofficeSessionState>(
  BackofficeSessionNotifier.new,
);
