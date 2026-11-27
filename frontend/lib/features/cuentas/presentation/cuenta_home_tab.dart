import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/pagos/data/pago_repository.dart';

class CuentaHomeTab extends ConsumerStatefulWidget {
  const CuentaHomeTab({
    super.key,
    required this.onGoToCatalogo,
    required this.onGoToHistorial,
  });

  final VoidCallback onGoToCatalogo;
  final VoidCallback onGoToHistorial;

  @override
  ConsumerState<CuentaHomeTab> createState() => _CuentaHomeTabState();
}

class _CuentaHomeTabState extends ConsumerState<CuentaHomeTab> {
  bool _showFullCuenta = false;
  bool _hideBalance = false;
  bool _copiedCard = false;
  bool _refreshing = false;
  Object? _error;

  List<TransaccionHistorialDto> _recentTx = const [];
  bool _loadingRecent = false;
  String? _loadedCuenta;

  @override
  void initState() {
    super.initState();
    Future.microtask(_loadRecentActivity);
  }

  Future<void> _loadRecentActivity({bool force = false}) async {
    final numCuenta =
        ref.read(sessionControllerProvider).numeroCuentaPrincipal;
    if (numCuenta == null || numCuenta.isEmpty) {
      if (mounted) {
        setState(() {
          _recentTx = const [];
          _loadedCuenta = null;
        });
      }
      return;
    }
    if (!force && _loadedCuenta == numCuenta && _recentTx.isNotEmpty) return;

    setState(() => _loadingRecent = true);
    try {
      final list =
          await ref.read(pagoRepositoryProvider).consultarHistorial(numCuenta);
      if (mounted) {
        setState(() {
          _recentTx = list.take(4).toList();
          _loadedCuenta = numCuenta;
        });
      }
    } catch (_) {
      if (mounted) {
        setState(() {
          _recentTx = const [];
          _loadedCuenta = numCuenta;
        });
      }
    } finally {
      if (mounted) setState(() => _loadingRecent = false);
    }
  }

  Future<void> _refresh() async {
    setState(() {
      _refreshing = true;
      _error = null;
    });
    try {
      await ref
          .read(sessionControllerProvider.notifier)
          .refrescarPerfilYSaldo();
      await _loadRecentActivity(force: true);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _refreshing = false);
    }
  }

  Future<void> _copyCardCuenta(String numeroCuenta) async {
    await copyToClipboard(
      context,
      numeroCuenta,
      label: 'Número de cuenta',
    );
    if (!mounted) return;
    setState(() => _copiedCard = true);
    Future.delayed(const Duration(milliseconds: 1600), () {
      if (mounted) setState(() => _copiedCard = false);
    });
  }

  @override
  Widget build(BuildContext context) {
    final session = ref.watch(sessionControllerProvider);
    ref.listen<int>(historyRefreshSignalProvider, (_, _) {
      _loadRecentActivity(force: true);
    });

    final cliente = session.cliente;
    final CuentaDto? cuenta = session.cuentaPrincipal;
    final SaldoDto? saldo = cuenta?.saldo;
    final cuentas = cliente?.cuentas ?? const [];

    if (cuenta != null &&
        _loadedCuenta != cuenta.numeroCuenta &&
        !_loadingRecent) {
      Future.microtask(() => _loadRecentActivity(force: true));
    }

    final disponible = saldo?.saldoDisponible ?? 0.0;
    final contable = saldo?.saldoContable ?? 0.0;
    final retenido = saldo?.montoRetenido ?? 0.0;
    final pctLibre = contable > 0
        ? ((disponible / contable) * 100).round().clamp(0, 100)
        : 100;

    return RefreshIndicator(
      onRefresh: _refresh,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          Row(
            children: [
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Hola, ${session.loginResponse?.nombreCliente ?? cliente?.nombreCompleto ?? 'Cliente'}',
                      style: const TextStyle(
                        fontSize: 21,
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 2),
                    const Text(
                      'Disponibilidad inmediata y actividad reciente',
                      style: TextStyle(
                        fontSize: 12.5,
                        color: AppColors.mutedText,
                      ),
                    ),
                  ],
                ),
              ),
              IconButton.filledTonal(
                tooltip: _hideBalance ? 'Mostrar saldos' : 'Modo discreto',
                onPressed: () => setState(() => _hideBalance = !_hideBalance),
                icon: Icon(
                  _hideBalance
                      ? Icons.visibility_outlined
                      : Icons.visibility_off_outlined,
                ),
              ),
              const SizedBox(width: 6),
              IconButton.filledTonal(
                tooltip: 'Actualizar saldo',
                onPressed: _refreshing ? null : _refresh,
                icon: _refreshing
                    ? const SizedBox(
                        width: 18,
                        height: 18,
                        child: CircularProgressIndicator(strokeWidth: 2),
                      )
                    : const Icon(Icons.refresh),
              ),
            ],
          ),
          const SizedBox(height: 14),
          if (_error != null) ...[
            ErrorBanner(error: _error!, onRetry: _refresh),
            const SizedBox(height: 14),
          ],
          if (cuentas.length > 1) ...[
            Card(
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'TUS CUENTAS DIGITALES (${cuentas.length})',
                      style: const TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 0.9,
                        color: AppColors.mutedText,
                      ),
                    ),
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 8,
                      runSpacing: 8,
                      children: cuentas.map((c) {
                        final isSelected =
                            c.numeroCuenta == cuenta?.numeroCuenta;
                        final balLabel = _hideBalance
                            ? '••••'
                            : formatMxn(c.saldo?.saldoDisponible ?? 0);
                        return ChoiceChip(
                          label: Text(
                            '${maskCuenta(c.numeroCuenta)} · ${c.tipoCuenta.isEmpty ? 'DÉBITO' : c.tipoCuenta} · $balLabel',
                            style: TextStyle(
                              fontSize: 12,
                              fontWeight: isSelected
                                  ? FontWeight.w700
                                  : FontWeight.w500,
                            ),
                          ),
                          selected: isSelected,
                          onSelected: (selected) {
                            if (selected) {
                              ref
                                  .read(sessionControllerProvider.notifier)
                                  .seleccionarCuenta(c);
                            }
                          },
                        );
                      }).toList(),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 14),
          ],
          if (cuenta == null)
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  children: [
                    const Text(
                      'No se pudo cargar el detalle de tu cuenta bancaria.',
                    ),
                    const SizedBox(height: 12),
                    FilledButton(
                      onPressed: _refresh,
                      child: const Text('Cargar datos de cuenta'),
                    ),
                  ],
                ),
              ),
            )
          else ...[
            _DigitalEmvCard(
              cuenta: cuenta,
              titular:
                  cliente?.nombreCompleto ?? cuenta.nombreTitular ?? 'Titular',
              disponible: disponible,
              retenido: retenido,
              hideBalance: _hideBalance,
              showFullCuenta: _showFullCuenta,
              copied: _copiedCard,
              onToggleReveal: () =>
                  setState(() => _showFullCuenta = !_showFullCuenta),
              onCopy: () => _copyCardCuenta(cuenta.numeroCuenta),
            ),
            const SizedBox(height: 14),
            _AccountingBreakdownCard(
              disponible: disponible,
              contable: contable,
              retenido: retenido,
              pctLibre: pctLibre,
              hideBalance: _hideBalance,
              ultimaActualizacion: saldo?.fechaUltimaActualizacion,
            ),
          ],
          const SizedBox(height: 16),
          const Text(
            'Acciones rápidas',
            style: TextStyle(
              fontSize: 15.5,
              fontWeight: FontWeight.w800,
              color: AppColors.primary,
            ),
          ),
          const SizedBox(height: 10),
          Row(
            children: [
              Expanded(
                child: _QuickActionPrimaryCard(
                  icon: Icons.storefront_outlined,
                  title: 'Pagar Servicio',
                  subtitle: 'Recargas, luz, agua y pines',
                  onTap: widget.onGoToCatalogo,
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: _QuickActionSecondaryCard(
                  icon: Icons.receipt_long_outlined,
                  title: 'Ver Historial',
                  subtitle: 'Comprobantes y estatus UPC',
                  onTap: widget.onGoToHistorial,
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),
          _RecentActivityCard(
            loading: _loadingRecent,
            items: _recentTx,
            hideBalance: _hideBalance,
            onGoCatalog: widget.onGoToCatalogo,
            onGoHistory: widget.onGoToHistorial,
          ),
          const SizedBox(height: 14),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: Colors.white,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: AppColors.border),
            ),
            child: Row(
              children: [
                Container(
                  width: 38,
                  height: 38,
                  decoration: BoxDecoration(
                    color: AppColors.emeraldSoft,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.verified_user_outlined,
                    color: Color(0xFF047857),
                    size: 20,
                  ),
                ),
                const SizedBox(width: 12),
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Protección Bancaria en Cada Operación',
                        style: TextStyle(
                          fontWeight: FontWeight.w700,
                          fontSize: 13,
                          color: AppColors.primary,
                        ),
                      ),
                      SizedBox(height: 2),
                      Text(
                        'Validación previa de referencias y respaldo automático de tu saldo disponible.',
                        style: TextStyle(
                          fontSize: 11.5,
                          color: AppColors.mutedText,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _DigitalEmvCard extends StatelessWidget {
  const _DigitalEmvCard({
    required this.cuenta,
    required this.titular,
    required this.disponible,
    required this.retenido,
    required this.hideBalance,
    required this.showFullCuenta,
    required this.copied,
    required this.onToggleReveal,
    required this.onCopy,
  });

  final CuentaDto cuenta;
  final String titular;
  final double disponible;
  final double retenido;
  final bool hideBalance;
  final bool showFullCuenta;
  final bool copied;
  final VoidCallback onToggleReveal;
  final VoidCallback onCopy;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        borderRadius: BorderRadius.circular(24),
        gradient: const LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [AppColors.slate950, AppColors.primary, AppColors.sky950],
        ),
        boxShadow: [
          BoxShadow(
            color: AppColors.primary.withValues(alpha: 0.28),
            blurRadius: 20,
            offset: const Offset(0, 10),
          ),
        ],
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              // Chip EMV metálico
              Container(
                width: 44,
                height: 32,
                padding: const EdgeInsets.all(4),
                decoration: BoxDecoration(
                  borderRadius: BorderRadius.circular(8),
                  gradient: const LinearGradient(
                    begin: Alignment.topLeft,
                    end: Alignment.bottomRight,
                    colors: [
                      Color(0xFFFDE68A),
                      Color(0xFFD97706),
                      Color(0xFFFCD34D),
                    ],
                  ),
                  border: Border.all(
                    color: const Color(0xFFFEF3C7).withValues(alpha: 0.6),
                  ),
                ),
                child: Column(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Container(
                          width: 14,
                          height: 9,
                          decoration: BoxDecoration(
                            border: Border.all(
                              color: const Color(0xFF78350F)
                                  .withValues(alpha: 0.45),
                              width: 0.8,
                            ),
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                        Container(
                          width: 14,
                          height: 9,
                          decoration: BoxDecoration(
                            border: Border.all(
                              color: const Color(0xFF78350F)
                                  .withValues(alpha: 0.45),
                              width: 0.8,
                            ),
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ],
                    ),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Container(
                          width: 14,
                          height: 9,
                          decoration: BoxDecoration(
                            border: Border.all(
                              color: const Color(0xFF78350F)
                                  .withValues(alpha: 0.45),
                              width: 0.8,
                            ),
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                        Container(
                          width: 14,
                          height: 9,
                          decoration: BoxDecoration(
                            border: Border.all(
                              color: const Color(0xFF78350F)
                                  .withValues(alpha: 0.45),
                              width: 0.8,
                            ),
                            borderRadius: BorderRadius.circular(2),
                          ),
                        ),
                      ],
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'GESTOPAGOS · ${cuenta.tipoCuenta.isEmpty ? 'DÉBITO DIGITAL' : cuenta.tipoCuenta}',
                      style: const TextStyle(
                        color: Color(0xFF7DD3FC),
                        fontSize: 10.5,
                        fontWeight: FontWeight.w700,
                        letterSpacing: 1.0,
                      ),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      titular,
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: const TextStyle(
                        color: Colors.white,
                        fontSize: 15,
                        fontWeight: FontWeight.w700,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 8),
              const Icon(
                Icons.wifi,
                color: Color(0xFF7DD3FC),
                size: 18,
              ),
              const SizedBox(width: 8),
              StatusBadge(estatus: cuenta.estatus, inverse: true),
            ],
          ),
          const SizedBox(height: 22),
          const Text(
            'Saldo disponible para pagos',
            style: TextStyle(color: Color(0xFFCBD5E1), fontSize: 12),
          ),
          const SizedBox(height: 4),
          Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 10,
            runSpacing: 6,
            children: [
              Text(
                hideBalance ? '••••••••' : formatMxn(disponible),
                style: const TextStyle(
                  color: Colors.white,
                  fontSize: 29,
                  fontWeight: FontWeight.w800,
                  letterSpacing: -0.5,
                ),
              ),
              if (!hideBalance && retenido > 0.001)
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 10, vertical: 3),
                  decoration: BoxDecoration(
                    color: AppColors.warning.withValues(alpha: 0.22),
                    borderRadius: BorderRadius.circular(20),
                    border: Border.all(
                      color: AppColors.warning.withValues(alpha: 0.45),
                    ),
                  ),
                  child: Text(
                    '${formatMxn(retenido)} en conciliación',
                    style: const TextStyle(
                      color: Color(0xFFFDE68A),
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 20),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: Colors.white.withValues(alpha: 0.08),
              borderRadius: BorderRadius.circular(14),
              border: Border.all(color: Colors.white.withValues(alpha: 0.14)),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'CUENTA DIGITAL (16 DÍGITOS)',
                  style: TextStyle(
                    color: Color(0xFF94A3B8),
                    fontSize: 10,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 0.9,
                  ),
                ),
                const SizedBox(height: 6),
                Text(
                  showFullCuenta
                      ? formatCuentaGrupos(cuenta.numeroCuenta)
                      : maskCuenta(cuenta.numeroCuenta),
                  style: const TextStyle(
                    color: Colors.white,
                    fontFamily: 'monospace',
                    fontSize: 16,
                    fontWeight: FontWeight.w700,
                    letterSpacing: 1.2,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    _CardPillButton(
                      icon: showFullCuenta
                          ? Icons.visibility_off_outlined
                          : Icons.visibility_outlined,
                      label: showFullCuenta ? 'Ocultar' : 'Ver 16 dígitos',
                      onTap: onToggleReveal,
                    ),
                    const SizedBox(width: 8),
                    _CardPillButton(
                      icon: copied ? Icons.check : Icons.copy,
                      label: copied ? 'Copiado' : 'Copiar cuenta',
                      accent: copied,
                      onTap: onCopy,
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }
}

class _CardPillButton extends StatelessWidget {
  const _CardPillButton({
    required this.icon,
    required this.label,
    required this.onTap,
    this.accent = false,
  });

  final IconData icon;
  final String label;
  final VoidCallback onTap;
  final bool accent;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(10),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
        decoration: BoxDecoration(
          color: accent
              ? AppColors.accent.withValues(alpha: 0.25)
              : Colors.white.withValues(alpha: 0.12),
          borderRadius: BorderRadius.circular(10),
          border: Border.all(
            color: accent
                ? AppColors.accent.withValues(alpha: 0.5)
                : Colors.white.withValues(alpha: 0.18),
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(
              icon,
              size: 14,
              color: accent ? const Color(0xFF6EE7B7) : Colors.white,
            ),
            const SizedBox(width: 5),
            Text(
              label,
              style: TextStyle(
                fontSize: 11.5,
                fontWeight: FontWeight.w700,
                color: accent ? const Color(0xFFA7F3D0) : Colors.white,
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _AccountingBreakdownCard extends StatelessWidget {
  const _AccountingBreakdownCard({
    required this.disponible,
    required this.contable,
    required this.retenido,
    required this.pctLibre,
    required this.hideBalance,
    this.ultimaActualizacion,
  });

  final double disponible;
  final double contable;
  final double retenido;
  final int pctLibre;
  final bool hideBalance;
  final String? ultimaActualizacion;

  @override
  Widget build(BuildContext context) {
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
                    color: AppColors.skySoft,
                    borderRadius: BorderRadius.circular(12),
                  ),
                  child: const Icon(
                    Icons.account_balance_wallet_outlined,
                    color: AppColors.secondary,
                    size: 20,
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Desglose contable',
                        style: TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 14,
                          color: AppColors.primary,
                        ),
                      ),
                      Text(
                        ultimaActualizacion != null
                            ? 'Actualizado: ${formatFecha(ultimaActualizacion)}'
                            : 'Sincronizado en tiempo real',
                        style: const TextStyle(
                          fontSize: 11.5,
                          color: AppColors.mutedText,
                        ),
                      ),
                    ],
                  ),
                ),
                Container(
                  padding:
                      const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                  decoration: BoxDecoration(
                    color: AppColors.emeraldSoft,
                    borderRadius: BorderRadius.circular(8),
                  ),
                  child: Text(
                    '$pctLibre% libre',
                    style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 11,
                      fontWeight: FontWeight.w800,
                      color: Color(0xFF047857),
                    ),
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),
            ClipRRect(
              borderRadius: BorderRadius.circular(6),
              child: LinearProgressIndicator(
                value: pctLibre / 100.0,
                minHeight: 7,
                backgroundColor: AppColors.background,
                color: AppColors.accent,
              ),
            ),
            const SizedBox(height: 12),
            _BreakdownRow(
              label: 'Disponible inmediato',
              value: hideBalance ? '••••••' : formatMxn(disponible),
              valueColor: const Color(0xFF047857),
            ),
            const SizedBox(height: 6),
            _BreakdownRow(
              label: 'Saldo contable total',
              value: hideBalance ? '••••••' : formatMxn(contable),
              valueColor: AppColors.primary,
            ),
            if (retenido > 0.001) ...[
              const SizedBox(height: 6),
              _BreakdownRow(
                label: 'Retenido en conciliación',
                value: hideBalance ? '••••••' : formatMxn(retenido),
                valueColor: const Color(0xFFB45309),
                highlightBg: AppColors.amberSoft,
              ),
            ],
          ],
        ),
      ),
    );
  }
}

class _BreakdownRow extends StatelessWidget {
  const _BreakdownRow({
    required this.label,
    required this.value,
    required this.valueColor,
    this.highlightBg,
  });

  final String label;
  final String value;
  final Color valueColor;
  final Color? highlightBg;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: highlightBg ?? const Color(0xFFF8FAFC),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        mainAxisAlignment: MainAxisAlignment.spaceBetween,
        children: [
          Text(
            label,
            style: const TextStyle(
              fontSize: 12.5,
              fontWeight: FontWeight.w600,
              color: AppColors.mutedText,
            ),
          ),
          Text(
            value,
            style: TextStyle(
              fontFamily: 'monospace',
              fontSize: 13,
              fontWeight: FontWeight.w800,
              color: valueColor,
            ),
          ),
        ],
      ),
    );
  }
}

class _QuickActionPrimaryCard extends StatelessWidget {
  const _QuickActionPrimaryCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: AppColors.slate950,
          borderRadius: BorderRadius.circular(18),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                CircleAvatar(
                  radius: 18,
                  backgroundColor: Colors.white.withValues(alpha: 0.12),
                  child: Icon(icon, color: const Color(0xFF7DD3FC), size: 19),
                ),
                const Icon(
                  Icons.north_east_rounded,
                  color: Color(0xFF94A3B8),
                  size: 16,
                ),
              ],
            ),
            const SizedBox(height: 14),
            Text(
              title,
              style: const TextStyle(
                fontWeight: FontWeight.w800,
                fontSize: 14,
                color: Colors.white,
              ),
            ),
            const SizedBox(height: 2),
            Text(
              subtitle,
              style: const TextStyle(
                fontSize: 11,
                color: Color(0xFFCBD5E1),
              ),
            ),
          ],
        ),
      ),
    );
  }
}

class _QuickActionSecondaryCard extends StatelessWidget {
  const _QuickActionSecondaryCard({
    required this.icon,
    required this.title,
    required this.subtitle,
    required this.onTap,
  });

  final IconData icon;
  final String title;
  final String subtitle;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(18),
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(14),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  CircleAvatar(
                    radius: 18,
                    backgroundColor: AppColors.skySoft,
                    child: Icon(icon, color: AppColors.secondary, size: 19),
                  ),
                  const Icon(
                    Icons.north_east_rounded,
                    color: AppColors.mutedText,
                    size: 16,
                  ),
                ],
              ),
              const SizedBox(height: 14),
              Text(
                title,
                style: const TextStyle(
                  fontWeight: FontWeight.w800,
                  fontSize: 14,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                subtitle,
                style: const TextStyle(
                  fontSize: 11,
                  color: AppColors.mutedText,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

class _RecentActivityCard extends StatelessWidget {
  const _RecentActivityCard({
    required this.loading,
    required this.items,
    required this.hideBalance,
    required this.onGoCatalog,
    required this.onGoHistory,
  });

  final bool loading;
  final List<TransaccionHistorialDto> items;
  final bool hideBalance;
  final VoidCallback onGoCatalog;
  final VoidCallback onGoHistory;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 14, 10, 10),
            child: Row(
              children: [
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Actividad Reciente',
                        style: TextStyle(
                          fontWeight: FontWeight.w800,
                          fontSize: 15,
                          color: AppColors.primary,
                        ),
                      ),
                      Text(
                        'Últimas operaciones en tu cuenta activa',
                        style: TextStyle(
                          fontSize: 11.5,
                          color: AppColors.mutedText,
                        ),
                      ),
                    ],
                  ),
                ),
                TextButton.icon(
                  onPressed: onGoHistory,
                  icon: const Text('Ver todo'),
                  label: const Icon(Icons.arrow_forward_rounded, size: 16),
                ),
              ],
            ),
          ),
          const Divider(height: 1, color: AppColors.border),
          if (loading)
            const Padding(
              padding: EdgeInsets.all(20),
              child: Center(child: CircularProgressIndicator()),
            )
          else if (items.isEmpty)
            Padding(
              padding: const EdgeInsets.all(16),
              child: Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: const Color(0xFFF8FAFC),
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  children: [
                    const Text(
                      'Aún no tienes movimientos registrados',
                      style: TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 13.5,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Explora el catálogo GestoPago para realizar tu primera recarga o pago de servicio.',
                      textAlign: TextAlign.center,
                      style: TextStyle(
                        fontSize: 12,
                        color: AppColors.mutedText,
                      ),
                    ),
                    const SizedBox(height: 12),
                    FilledButton.icon(
                      onPressed: onGoCatalog,
                      icon: const Icon(Icons.storefront_outlined, size: 18),
                      label: const Text('Ir al catálogo'),
                    ),
                  ],
                ),
              ),
            )
          else
            ListView.separated(
              shrinkWrap: true,
              physics: const NeverScrollableScrollPhysics(),
              padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 8),
              itemCount: items.length,
              separatorBuilder: (_, _) =>
                  const Divider(height: 1, color: Color(0xFFF1F5F9)),
              itemBuilder: (_, idx) {
                final tx = items[idx];
                final title = tx.producto ?? tx.servicio ?? 'Pago de servicio';
                final refOrTel = tx.referencia.isNotEmpty
                    ? tx.referencia
                    : (tx.telefono.isNotEmpty ? tx.telefono : 'Sin referencia');
                return Padding(
                  padding: const EdgeInsets.symmetric(vertical: 10),
                  child: Row(
                    children: [
                      ServiceLogo(
                        urlLogo: tx.urlLogo,
                        tipoFront: tx.referencia.isNotEmpty ? 2 : 1,
                        size: 40,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Wrap(
                              spacing: 6,
                              runSpacing: 4,
                              crossAxisAlignment: WrapCrossAlignment.center,
                              children: [
                                Text(
                                  title,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 13.5,
                                    color: AppColors.primary,
                                  ),
                                ),
                                StatusBadge(estatus: tx.estatus),
                              ],
                            ),
                            const SizedBox(height: 2),
                            Text(
                              'Ref: $refOrTel · ${formatFecha(tx.fechaCreacion ?? tx.horaLocal)}',
                              overflow: TextOverflow.ellipsis,
                              style: const TextStyle(
                                fontSize: 11.5,
                                color: AppColors.mutedText,
                              ),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      Text(
                        hideBalance ? '••••••' : '-${formatMxn(tx.monto)}',
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontWeight: FontWeight.w800,
                          fontSize: 13,
                          color: AppColors.primary,
                        ),
                      ),
                    ],
                  ),
                );
              },
            ),
        ],
      ),
    );
  }
}
