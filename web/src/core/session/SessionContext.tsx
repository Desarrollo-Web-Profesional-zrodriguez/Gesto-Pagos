import React, { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react';
import { apiClient } from '../network/apiClient';
import { ApiException } from '../network/apiError';
import { authRepository, LoginResponseDto } from '../../features/auth/data/authRepository';
import {
  ClienteDto,
  CuentaDto,
  SaldoDto,
} from '../../features/clientes/data/clienteModels';
import { clienteRepository } from '../../features/clientes/data/clienteRepository';
import { cuentaRepository } from '../../features/cuentas/data/cuentaRepository';

const SESSION_STORAGE_KEY = 'gestopago_web_session_v1';

interface SessionContextValue {
  sesion: LoginResponseDto | null;
  cliente: ClienteDto | null;
  cuentaActiva: CuentaDto | null;
  saldoActual: SaldoDto | null;
  remainingSeconds: number;
  loadingSession: boolean;
  expiredMessage: string | null;
  clearExpiredMessage: () => void;
  loginWithPassword: (username: string, password: string) => Promise<void>;
  loginWithBiometrics: (username: string, embedding: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshPerfilYSaldo: () => Promise<void>;
  seleccionarCuenta: (cuenta: CuentaDto) => Promise<void>;
  renovarSesionRemota: () => Promise<void>;
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

function readStoredSession(): LoginResponseDto | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = window.sessionStorage.getItem(SESSION_STORAGE_KEY);
    if (!raw) return null;
    const parsed = JSON.parse(raw) as LoginResponseDto;
    return parsed?.tokenSesion ? parsed : null;
  } catch {
    return null;
  }
}

function writeStoredSession(sesion: LoginResponseDto | null): void {
  if (typeof window === 'undefined') return;
  try {
    if (sesion) {
      window.sessionStorage.setItem(SESSION_STORAGE_KEY, JSON.stringify(sesion));
    } else {
      window.sessionStorage.removeItem(SESSION_STORAGE_KEY);
    }
  } catch {
    // Ignore storage quota errors
  }
}

export const SessionProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [sesion, setSesion] = useState<LoginResponseDto | null>(null);
  const [cliente, setCliente] = useState<ClienteDto | null>(null);
  const [cuentaActiva, setCuentaActiva] = useState<CuentaDto | null>(null);
  const [saldoActual, setSaldoActual] = useState<SaldoDto | null>(null);
  const [remainingSeconds, setRemainingSeconds] = useState<number>(300);
  const [loadingSession, setLoadingSession] = useState<boolean>(true);
  const [expiredMessage, setExpiredMessage] = useState<string | null>(null);

  const sesionRef = useRef<LoginResponseDto | null>(null);
  const cuentaRef = useRef<CuentaDto | null>(null);
  const lastRemoteRenewRef = useRef<number>(0);

  sesionRef.current = sesion;
  cuentaRef.current = cuentaActiva;

  const clearState = useCallback((reason?: string) => {
    writeStoredSession(null);
    setSesion(null);
    setCliente(null);
    setCuentaActiva(null);
    setSaldoActual(null);
    setRemainingSeconds(0);
    if (reason) {
      setExpiredMessage(reason);
    }
  }, []);

  const cargarPerfilYSaldo = useCallback(
    async (username: string, preferredNumCuenta?: string) => {
      const cli = await clienteRepository.consultarPorIdentificador(username);
      setCliente(cli);

      const cuentas = cli.cuentas ?? [];
      if (cuentas.length === 0) {
        setCuentaActiva(null);
        setSaldoActual(null);
        return;
      }

      const targetNum = preferredNumCuenta ?? cuentaRef.current?.numeroCuenta;
      const activa =
        (targetNum ? cuentas.find((c) => c.numeroCuenta === targetNum) : undefined) ??
        cuentas.find((c) => (c.estatus ?? '').toUpperCase() === 'ACTIVA') ??
        cuentas[0];

      setCuentaActiva(activa);
      if (activa?.numeroCuenta) {
        try {
          const saldo = await cuentaRepository.consultarSaldo(activa.numeroCuenta);
          setSaldoActual(saldo);
        } catch {
          setSaldoActual(activa.saldo ?? null);
        }
      }
    },
    []
  );

  const resetLocalCountdown = useCallback((loginRes?: LoginResponseDto | null) => {
    const mins = loginRes?.minutosInactividadMaximos ?? sesionRef.current?.minutosInactividadMaximos ?? 5;
    setRemainingSeconds(Math.max(1, mins) * 60);
  }, []);

  const renovarSesionRemota = useCallback(async () => {
    const active = sesionRef.current;
    if (!active?.tokenSesion) return;
    try {
      const check = await authRepository.validarSesion(active.tokenSesion);
      if (check.valida) {
        lastRemoteRenewRef.current = Date.now();
        resetLocalCountdown(active);
      } else {
        clearState(check.mensaje || 'Tu sesión expiró por inactividad.');
      }
    } catch (err) {
      if (err instanceof ApiException && (err.esSesionExpirada || err.statusCode === 401)) {
        clearState(err.message);
      }
    }
  }, [clearState, resetLocalCountdown]);

  useEffect(() => {
    apiClient.configure({
      tokenProvider: () => sesionRef.current?.tokenSesion ?? null,
      onUnauthorizedExpired: () => {
        if (sesionRef.current) {
          clearState('Tu sesión ha expirado (límite de 5 minutos de inactividad). Inicia sesión nuevamente.');
        }
      },
      onUserNetworkActivity: () => {
        if (sesionRef.current) {
          resetLocalCountdown(sesionRef.current);
        }
      },
    });
  }, [clearState, resetLocalCountdown]);

  // Restore session on page reload
  useEffect(() => {
    let mounted = true;
    (async () => {
      const stored = readStoredSession();
      if (!stored) {
        if (mounted) setLoadingSession(false);
        return;
      }
      try {
        const res = await authRepository.validarSesion(stored.tokenSesion);
        if (!mounted) return;
        if (!res.valida) {
          clearState();
          setLoadingSession(false);
          return;
        }
        setSesion(stored);
        sesionRef.current = stored;
        resetLocalCountdown(stored);
        lastRemoteRenewRef.current = Date.now();
        await cargarPerfilYSaldo(stored.username);
      } catch {
        if (mounted) {
          clearState();
        }
      } finally {
        if (mounted) setLoadingSession(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [cargarPerfilYSaldo, clearState, resetLocalCountdown]);

  // 1-second countdown + local inactivity detection
  useEffect(() => {
    if (!sesion) return;
    const interval = window.setInterval(() => {
      setRemainingSeconds((prev) => {
        if (prev <= 1) {
          clearState(
            'Tu sesión se cerró automáticamente tras 5 minutos de inactividad para proteger tu cuenta.'
          );
          return 0;
        }
        return prev - 1;
      });
    }, 1000);
    return () => window.clearInterval(interval);
  }, [sesion, clearState]);

  // Throttled UI interaction heartbeat -> keeps backend 5-min sliding window aligned when user is active
  useEffect(() => {
    if (!sesion) return;
    const handleActivity = () => {
      resetLocalCountdown(sesionRef.current);
      const now = Date.now();
      if (now - lastRemoteRenewRef.current > 60000) {
        lastRemoteRenewRef.current = now;
        void renovarSesionRemota();
      }
    };
    window.addEventListener('click', handleActivity);
    window.addEventListener('keydown', handleActivity);
    return () => {
      window.removeEventListener('click', handleActivity);
      window.removeEventListener('keydown', handleActivity);
    };
  }, [sesion, renovarSesionRemota, resetLocalCountdown]);

  const establishSession = useCallback(
    async (loginRes: LoginResponseDto) => {
      setExpiredMessage(null);
      writeStoredSession(loginRes);
      setSesion(loginRes);
      sesionRef.current = loginRes;
      lastRemoteRenewRef.current = Date.now();
      resetLocalCountdown(loginRes);
      try {
        await cargarPerfilYSaldo(loginRes.username);
      } catch (err) {
        clearState();
        throw err;
      }
    },
    [cargarPerfilYSaldo, clearState, resetLocalCountdown]
  );

  const loginWithPassword = useCallback(
    async (username: string, password: string) => {
      const res = await authRepository.login({ username, password });
      await establishSession(res);
    },
    [establishSession]
  );

  const loginWithBiometrics = useCallback(
    async (username: string, biometricoFacialEmbedding: string) => {
      const res = await authRepository.login({ username, biometricoFacialEmbedding });
      await establishSession(res);
    },
    [establishSession]
  );

  const logout = useCallback(async () => {
    const token = sesionRef.current?.tokenSesion;
    try {
      if (token) {
        await authRepository.logout(token);
      }
    } catch {
      // Clear local session regardless of server logout errors
    } finally {
      clearState();
    }
  }, [clearState]);

  const refreshPerfilYSaldo = useCallback(async () => {
    const current = sesionRef.current;
    if (!current) return;
    await cargarPerfilYSaldo(current.username, cuentaRef.current?.numeroCuenta);
  }, [cargarPerfilYSaldo]);

  const seleccionarCuenta = useCallback(async (cuenta: CuentaDto) => {
    setCuentaActiva(cuenta);
    try {
      const saldo = await cuentaRepository.consultarSaldo(cuenta.numeroCuenta);
      setSaldoActual(saldo);
    } catch {
      setSaldoActual(cuenta.saldo ?? null);
    }
  }, []);

  const clearExpiredMessage = useCallback(() => setExpiredMessage(null), []);

  return (
    <SessionContext.Provider
      value={{
        sesion,
        cliente,
        cuentaActiva,
        saldoActual,
        remainingSeconds,
        loadingSession,
        expiredMessage,
        clearExpiredMessage,
        loginWithPassword,
        loginWithBiometrics,
        logout,
        refreshPerfilYSaldo,
        seleccionarCuenta,
        renovarSesionRemota,
      }}
    >
      {children}
    </SessionContext.Provider>
  );
};

export function useSession(): SessionContextValue {
  const ctx = useContext(SessionContext);
  if (!ctx) {
    throw new Error('useSession debe utilizarse dentro de un SessionProvider');
  }
  return ctx;
}
