import 'dart:async';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/core/network/dio_client.dart';
import 'package:gestopago_app/core/session/session_storage.dart';
import 'package:gestopago_app/features/auth/data/auth_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/data/cliente_repository.dart';
import 'package:gestopago_app/features/cuentas/data/cuenta_repository.dart';

class SessionState {
  const SessionState({
    this.loginResponse,
    this.cliente,
    this.cuentaActiva,
    this.secondsRemaining = 0,
    this.isBusy = false,
    this.expiredMessage,
  });

  final LoginResponseDto? loginResponse;
  final ClienteDto? cliente;
  final CuentaDto? cuentaActiva;
  final int secondsRemaining;
  final bool isBusy;
  final String? expiredMessage;

  bool get isAuthenticated =>
      loginResponse != null &&
      loginResponse!.tokenSesion.isNotEmpty &&
      secondsRemaining > 0;

  String? get token => loginResponse?.tokenSesion;
  CuentaDto? get cuentaPrincipal => cuentaActiva ?? cliente?.cuentaPrincipal;
  String? get numeroCuentaPrincipal => cuentaPrincipal?.numeroCuenta;

  SessionState copyWith({
    LoginResponseDto? loginResponse,
    bool clearLogin = false,
    ClienteDto? cliente,
    bool clearCliente = false,
    CuentaDto? cuentaActiva,
    bool clearCuentaActiva = false,
    int? secondsRemaining,
    bool? isBusy,
    String? expiredMessage,
    bool clearExpiredMessage = false,
  }) =>
      SessionState(
        loginResponse:
            clearLogin ? null : (loginResponse ?? this.loginResponse),
        cliente: clearCliente ? null : (cliente ?? this.cliente),
        cuentaActiva:
            clearCuentaActiva ? null : (cuentaActiva ?? this.cuentaActiva),
        secondsRemaining: secondsRemaining ?? this.secondsRemaining,
        isBusy: isBusy ?? this.isBusy,
        expiredMessage: clearExpiredMessage
            ? null
            : (expiredMessage ?? this.expiredMessage),
      );
}

class SessionController extends Notifier<SessionState> {
  Timer? _countdownTimer;
  DateTime? _lastRemoteRenewal;
  bool _isDisposed = false;
  bool _isRestoring = false;

  @override
  SessionState build() {
    _isDisposed = false;
    final hooks = ref.watch(sessionHooksProvider);
    hooks.readToken = () => state.token;
    hooks.onExpired = _handleRemoteExpired;
    hooks.onActivity = _handleNetworkActivity;

    ref.onDispose(() {
      _isDisposed = true;
      _countdownTimer?.cancel();
      _countdownTimer = null;
    });

    Future.microtask(restaurarSesion);

    return const SessionState();
  }

  Future<bool> restaurarSesion() async {
    if (state.isAuthenticated) return true;
    if (_isDisposed) return false;
    if (_isRestoring) {
      while (_isRestoring) {
        await Future.delayed(const Duration(milliseconds: 10));
        if (_isDisposed) return false;
      }
      return state.isAuthenticated;
    }

    final storage = ref.read(tokenStorageProvider);
    _isRestoring = true;
    try {
      final token = await storage.readToken();
      if (token == null || token.isEmpty || _isDisposed) return false;

      state = state.copyWith(isBusy: true, clearExpiredMessage: true);

      final authRepo = ref.read(authRepositoryProvider);
      final isValid = await authRepo.validarSesion(token);
      if (_isDisposed) return false;
      if (!isValid) {
        await storage.clearToken();
        if (!_isDisposed) {
          state = state.copyWith(isBusy: false);
        }
        return false;
      }

      final username = await storage.readUsername();
      ClienteDto? cliente;
      if (username != null && username.isNotEmpty) {
        try {
          final clienteRepo = ref.read(clienteRepositoryProvider);
          cliente = await clienteRepo.consultarPorIdentificador(username);
        } catch (_) {
          // Si la carga de perfil falla en red, la sesión sigue válida
        }
      }

      if (_isDisposed) return false;

      final loginResp = LoginResponseDto(
        tokenSesion: token,
        activa: true,
        tipoToken: 'Bearer',
        minutosInactividadMaximos: 5,
        username: username ?? cliente?.correoElectronico ?? '',
        nombreCliente: cliente?.nombreCompleto ?? username ?? 'Cliente',
      );

      final ttlSeconds = loginResp.minutosInactividadMaximos * 60;
      _lastRemoteRenewal = DateTime.now();
      state = SessionState(
        loginResponse: loginResp,
        cliente: cliente,
        cuentaActiva: cliente?.cuentaPrincipal,
        secondsRemaining: ttlSeconds,
        isBusy: false,
      );
      _startTimer();
      return true;
    } catch (_) {
      await storage.clearToken();
      if (!_isDisposed) {
        state = state.copyWith(isBusy: false);
      }
      return false;
    } finally {
      _isRestoring = false;
    }
  }

  Future<void> loginWithPassword({
    required String username,
    required String password,
  }) async {
    state = state.copyWith(isBusy: true, clearExpiredMessage: true);
    try {
      final authRepo = ref.read(authRepositoryProvider);
      final clienteRepo = ref.read(clienteRepositoryProvider);
      final storage = ref.read(tokenStorageProvider);

      final loginResp = await authRepo.login(
        username: username,
        password: password,
      );
      await storage.saveToken(loginResp.tokenSesion, loginResp.username);

      ClienteDto? cliente;
      try {
        cliente = await clienteRepo.consultarPorIdentificador(
          loginResp.username,
        );
      } catch (_) {
        // Si falla la hidratación inicial de perfil, el usuario puede recargar en Home.
      }

      final ttlSeconds = loginResp.minutosInactividadMaximos * 60;
      _lastRemoteRenewal = DateTime.now();
      state = SessionState(
        loginResponse: loginResp,
        cliente: cliente,
        cuentaActiva: cliente?.cuentaPrincipal,
        secondsRemaining: ttlSeconds,
        isBusy: false,
      );
      _startTimer();
    } catch (_) {
      state = state.copyWith(isBusy: false);
      rethrow;
    }
  }

  Future<void> loginWithBiometrics({
    required String username,
    required String biometricoFacialEmbedding,
  }) async {
    state = state.copyWith(isBusy: true, clearExpiredMessage: true);
    try {
      final authRepo = ref.read(authRepositoryProvider);
      final clienteRepo = ref.read(clienteRepositoryProvider);
      final storage = ref.read(tokenStorageProvider);

      final loginResp = await authRepo.login(
        username: username,
        biometricoFacialEmbedding: biometricoFacialEmbedding,
      );
      await storage.saveToken(loginResp.tokenSesion, loginResp.username);

      ClienteDto? cliente;
      try {
        cliente = await clienteRepo.consultarPorIdentificador(
          loginResp.username,
        );
      } catch (_) {
        // Si falla la hidratación inicial de perfil, el usuario puede recargar en Home.
      }

      final ttlSeconds = loginResp.minutosInactividadMaximos * 60;
      _lastRemoteRenewal = DateTime.now();
      state = SessionState(
        loginResponse: loginResp,
        cliente: cliente,
        cuentaActiva: cliente?.cuentaPrincipal,
        secondsRemaining: ttlSeconds,
        isBusy: false,
      );
      _startTimer();
    } catch (_) {
      state = state.copyWith(isBusy: false);
      rethrow;
    }
  }

  Future<void> renovarSesionRemota() async {
    final token = state.token;
    if (token == null || token.isEmpty) return;

    try {
      final ok = await ref.read(authRepositoryProvider).validarSesion(token);
      if (ok) {
        final ttl =
            (state.loginResponse?.minutosInactividadMaximos ?? 5) * 60;
        _lastRemoteRenewal = DateTime.now();
        state = state.copyWith(secondsRemaining: ttl);
      }
    } catch (_) {
      _handleRemoteExpired();
    }
  }

  Future<void> refrescarPerfilYSaldo() async {
    final username = state.loginResponse?.username ?? state.cliente?.correoElectronico;
    if (username == null || username.isEmpty) return;
    final cliente = await ref
        .read(clienteRepositoryProvider)
        .consultarPorIdentificador(username);
    CuentaDto? updatedCuentaActiva;
    if (state.cuentaActiva != null) {
      try {
        updatedCuentaActiva = cliente.cuentas.firstWhere(
          (c) => c.numeroCuenta == state.cuentaActiva!.numeroCuenta,
        );
      } catch (_) {
        updatedCuentaActiva = cliente.cuentaPrincipal;
      }
    }
    state = state.copyWith(
      cliente: cliente,
      cuentaActiva: updatedCuentaActiva ?? cliente.cuentaPrincipal,
    );
  }

  Future<void> seleccionarCuenta(CuentaDto cuenta) async {
    state = state.copyWith(cuentaActiva: cuenta);
    try {
      final saldo = await ref
          .read(cuentaRepositoryProvider)
          .consultarSaldoPost(cuenta.numeroCuenta);
      final updatedCuenta = CuentaDto(
        idCuenta: cuenta.idCuenta,
        nombreTitular: cuenta.nombreTitular,
        numeroCuenta: cuenta.numeroCuenta,
        tipoCuenta: cuenta.tipoCuenta,
        estatus: cuenta.estatus,
        fechaApertura: cuenta.fechaApertura,
        saldo: saldo,
      );
      state = state.copyWith(cuentaActiva: updatedCuenta);
    } catch (_) {
      // Si la consulta remota falla, se conserva la cuenta seleccionada.
    }
  }

  Future<void> refrescarSaldoActual() async {
    final numCuenta = state.numeroCuentaPrincipal;
    final currentCliente = state.cliente;
    if (numCuenta == null || currentCliente == null) return;

    final saldo = await ref.read(cuentaRepositoryProvider).consultarSaldoPost(
          numCuenta,
        );
    final updatedCuentas = currentCliente.cuentas.map((c) {
      if (c.numeroCuenta == numCuenta) {
        return CuentaDto(
          idCuenta: c.idCuenta,
          nombreTitular: c.nombreTitular,
          numeroCuenta: c.numeroCuenta,
          tipoCuenta: c.tipoCuenta,
          estatus: c.estatus,
          fechaApertura: c.fechaApertura,
          saldo: saldo,
        );
      }
      return c;
    }).toList();

    CuentaDto? updatedCuentaActiva = state.cuentaActiva;
    if (updatedCuentaActiva != null && updatedCuentaActiva.numeroCuenta == numCuenta) {
      updatedCuentaActiva = CuentaDto(
        idCuenta: updatedCuentaActiva.idCuenta,
        nombreTitular: updatedCuentaActiva.nombreTitular,
        numeroCuenta: updatedCuentaActiva.numeroCuenta,
        tipoCuenta: updatedCuentaActiva.tipoCuenta,
        estatus: updatedCuentaActiva.estatus,
        fechaApertura: updatedCuentaActiva.fechaApertura,
        saldo: saldo,
      );
    }

    state = state.copyWith(
      cuentaActiva: updatedCuentaActiva,
      cliente: ClienteDto(
        nombre: currentCliente.nombre,
        segundoNombre: currentCliente.segundoNombre,
        apellidoPaterno: currentCliente.apellidoPaterno,
        apellidoMaterno: currentCliente.apellidoMaterno,
        nombreCompleto: currentCliente.nombreCompleto,
        fechaNacimiento: currentCliente.fechaNacimiento,
        edad: currentCliente.edad,
        curp: currentCliente.curp,
        rfc: currentCliente.rfc,
        sexo: currentCliente.sexo,
        idNacionalidad: currentCliente.idNacionalidad,
        nacionalidad: currentCliente.nacionalidad,
        estadoCivil: currentCliente.estadoCivil,
        correoElectronico: currentCliente.correoElectronico,
        telefonoMovil: currentCliente.telefonoMovil,
        telefonoAlternativo: currentCliente.telefonoAlternativo,
        ocupacion: currentCliente.ocupacion,
        empresa: currentCliente.empresa,
        ingresoMensual: currentCliente.ingresoMensual,
        activo: currentCliente.activo,
        fechaCreacion: currentCliente.fechaCreacion,
        fechaModificacion: currentCliente.fechaModificacion,
        domicilios: currentCliente.domicilios,
        cuentas: updatedCuentas,
      ),
    );
  }

  void actualizarClienteLocal(ClienteDto actualizado) {
    state = state.copyWith(cliente: actualizado);
  }

  Future<void> logout({String? reason}) async {
    final token = state.token;
    _countdownTimer?.cancel();
    _countdownTimer = null;

    if (token != null && token.isNotEmpty) {
      try {
        await ref.read(authRepositoryProvider).logout(token);
      } catch (_) {
        // El cierre local siempre procede aunque la red falle.
      }
    }
    await ref.read(tokenStorageProvider).clearToken();
    state = SessionState(
      secondsRemaining: 0,
      expiredMessage: reason,
    );
  }

  void _startTimer() {
    _countdownTimer?.cancel();
    _countdownTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      final remaining = state.secondsRemaining - 1;
      if (remaining <= 0) {
        _countdownTimer?.cancel();
        _countdownTimer = null;
        unawaited(
          logout(
            reason:
                'Tu sesión se cerró automáticamente tras 5 minutos de inactividad.',
          ),
        );
      } else {
        state = state.copyWith(secondsRemaining: remaining);
      }
    });
  }

  void _handleRemoteExpired() {
    if (!state.isAuthenticated) return;
    _countdownTimer?.cancel();
    _countdownTimer = null;
    unawaited(
      ref.read(tokenStorageProvider).clearToken().then((_) {
        state = const SessionState(
          expiredMessage:
              'La sesión expiró en el servidor. Inicia sesión nuevamente.',
        );
      }),
    );
  }

  void _handleNetworkActivity() {
    if (!state.isAuthenticated) return;
    final now = DateTime.now();
    // Si ha pasado más de 45s desde la última renovación con /auth/validar-sesion,
    // extendemos silenciosamente mientras haya actividad.
    if (_lastRemoteRenewal == null ||
        now.difference(_lastRemoteRenewal!).inSeconds >= 45) {
      _lastRemoteRenewal = now;
      final ttl =
          (state.loginResponse?.minutosInactividadMaximos ?? 5) * 60;
      state = state.copyWith(secondsRemaining: ttl);
      unawaited(renovarSesionRemota());
    }
  }
}

final sessionControllerProvider =
    NotifierProvider<SessionController, SessionState>(SessionController.new);
