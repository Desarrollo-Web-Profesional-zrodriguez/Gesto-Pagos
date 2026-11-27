import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:intl/intl.dart';

String formatMxn(num? amount) {
  final formatter = NumberFormat.currency(
    locale: 'es_MX',
    symbol: r'$',
    decimalDigits: 2,
  );
  return '${formatter.format(amount ?? 0)} MXN';
}

String maskCuenta(String? numCuenta) {
  if (numCuenta == null || numCuenta.trim().length < 4) return '****';
  final clean = numCuenta.trim();
  return '**** **** **** ${clean.substring(clean.length - 4)}';
}

String formatCuentaGrupos(String? numCuenta) {
  if (numCuenta == null || numCuenta.trim().isEmpty) return 'No disponible';
  final clean = numCuenta.trim();
  return clean.replaceAllMapped(RegExp(r'(.{4})'), (m) => '${m.group(1)} ').trim();
}

String formatFecha(String? raw) {
  if (raw == null || raw.trim().isEmpty) return 'Sin fecha';
  final parsed = DateTime.tryParse(raw.trim());
  if (parsed == null) return raw.trim();
  final d = parsed.day.toString().padLeft(2, '0');
  final m = parsed.month.toString().padLeft(2, '0');
  final y = parsed.year.toString();
  final hh = parsed.hour.toString().padLeft(2, '0');
  final mm = parsed.minute.toString().padLeft(2, '0');
  return '$d/$m/$y $hh:$mm';
}

String formatCountdown(int totalSeconds) {
  final safe = totalSeconds < 0 ? 0 : totalSeconds;
  final mins = (safe ~/ 60).toString().padLeft(2, '0');
  final secs = (safe % 60).toString().padLeft(2, '0');
  return '$mins:$secs';
}

String getInitials(String? name) {
  if (name == null || name.trim().isEmpty) return 'GP';
  final parts = name.trim().split(RegExp(r'\s+')).where((e) => e.isNotEmpty).toList();
  if (parts.isEmpty) return 'GP';
  if (parts.length == 1) {
    final p = parts.first;
    return (p.length >= 2 ? p.substring(0, 2) : p).toUpperCase();
  }
  return '${parts[0][0]}${parts[1][0]}'.toUpperCase();
}

String friendlyStatusLabel(String? estatus) {
  final norm = (estatus ?? 'DESCONOCIDO').trim().toUpperCase();
  const labels = <String, String>{
    'EXITOSA': 'Exitosa',
    'ACTIVA': 'Activa',
    'VALIDA': 'Válida',
    'PENDIENTE_CONCILIACION': 'En conciliación',
    'PROCESANDO': 'Procesando',
    'PENDIENTE': 'Pendiente',
    'REVISION_MANUAL': 'Revisión manual',
    'INACTIVO': 'Inactivo',
    'INACTIVA': 'Inactiva',
    'RECHAZADA': 'Rechazada',
    'FALLIDA': 'Fallida',
  };
  return labels[norm] ?? norm.replaceAll('_', ' ');
}

Future<void> copyToClipboard(
  BuildContext context,
  String value, {
  String label = 'Dato',
}) async {
  await Clipboard.setData(ClipboardData(text: value));
  if (!context.mounted) return;
  ScaffoldMessenger.of(context)
    ..hideCurrentSnackBar()
    ..showSnackBar(
      SnackBar(
        content: Text('$label copiado al portapapeles'),
        behavior: SnackBarBehavior.floating,
      ),
    );
}

class ErrorBanner extends StatelessWidget {
  const ErrorBanner({
    super.key,
    required this.error,
    this.onRetry,
    this.onDismiss,
  });

  final Object error;
  final VoidCallback? onRetry;
  final VoidCallback? onDismiss;

  @override
  Widget build(BuildContext context) {
    final apiError = error is ApiException ? error as ApiException : null;
    final message = apiError?.mensaje ?? error.toString();
    final isValidationStatus = apiError != null &&
        (apiError.statusCode == 400 || apiError.statusCode == 422);

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.roseSoft,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.danger.withValues(alpha: 0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Container(
                padding: const EdgeInsets.all(6),
                decoration: BoxDecoration(
                  color: AppColors.danger.withValues(alpha: 0.14),
                  borderRadius: BorderRadius.circular(10),
                ),
                child: const Icon(
                  Icons.error_outline,
                  color: AppColors.danger,
                  size: 18,
                ),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      isValidationStatus
                          ? 'Revisa la información ingresada'
                          : 'No se pudo completar la operación',
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 13,
                        color: Color(0xFF881337),
                      ),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      message,
                      style: const TextStyle(
                        fontSize: 12.5,
                        color: Color(0xFF9F1239),
                        height: 1.35,
                      ),
                    ),
                  ],
                ),
              ),
              if (onDismiss != null)
                IconButton(
                  visualDensity: VisualDensity.compact,
                  padding: EdgeInsets.zero,
                  constraints: const BoxConstraints(),
                  onPressed: onDismiss,
                  icon: const Icon(
                    Icons.close,
                    size: 18,
                    color: AppColors.danger,
                  ),
                ),
            ],
          ),
          if (apiError != null && apiError.detallesValidacion.isNotEmpty) ...[
            const SizedBox(height: 8),
            ...apiError.detallesValidacion.entries.map(
              (entry) => Padding(
                padding: const EdgeInsets.only(left: 38, bottom: 2),
                child: Text(
                  '• ${BackendValidators.friendlyFieldName(entry.key)}: ${entry.value}',
                  style: const TextStyle(
                    fontSize: 12,
                    color: Color(0xFF881337),
                  ),
                ),
              ),
            ),
          ],
          if (onRetry != null) ...[
            const SizedBox(height: 8),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh, size: 17),
                label: const Text('Reintentar'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

class ValidationSummary extends StatelessWidget {
  const ValidationSummary({
    super.key,
    required this.message,
    this.errorCount = 1,
    this.onDismiss,
  });

  final String? message;
  final int errorCount;
  final VoidCallback? onDismiss;

  @override
  Widget build(BuildContext context) {
    if (message == null || message!.trim().isEmpty) {
      return const SizedBox.shrink();
    }
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(12),
      decoration: BoxDecoration(
        color: AppColors.amberSoft,
        borderRadius: BorderRadius.circular(14),
        border: Border.all(color: AppColors.warning.withValues(alpha: 0.45)),
      ),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Icon(
            Icons.warning_amber_rounded,
            color: Color(0xFFD97706),
            size: 20,
          ),
          const SizedBox(width: 10),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  errorCount > 1
                      ? 'Por favor revisa $errorCount campos marcados'
                      : 'Verifica el siguiente dato para continuar',
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    fontSize: 12.5,
                    color: Color(0xFF78350F),
                  ),
                ),
                const SizedBox(height: 2),
                Text(
                  message!,
                  style: const TextStyle(
                    fontSize: 12,
                    color: Color(0xFF92400E),
                  ),
                ),
              ],
            ),
          ),
          if (onDismiss != null)
            IconButton(
              visualDensity: VisualDensity.compact,
              padding: EdgeInsets.zero,
              constraints: const BoxConstraints(),
              onPressed: onDismiss,
              icon: const Icon(
                Icons.close,
                size: 17,
                color: Color(0xFFD97706),
              ),
            ),
        ],
      ),
    );
  }
}

class StatusBadge extends StatelessWidget {
  const StatusBadge({
    super.key,
    required this.estatus,
    this.inverse = false,
  });

  final String? estatus;
  final bool inverse;

  @override
  Widget build(BuildContext context) {
    final norm = (estatus ?? 'DESCONOCIDO').trim().toUpperCase();
    final label = friendlyStatusLabel(norm);

    late final Color bg;
    late final Color fg;
    late final Color borderColor;
    late final IconData icon;

    if (norm == 'EXITOSA' || norm == 'ACTIVA' || norm == 'VALIDA') {
      bg = inverse
          ? AppColors.accent.withValues(alpha: 0.22)
          : AppColors.emeraldSoft;
      fg = inverse ? const Color(0xFFA7F3D0) : const Color(0xFF047857);
      borderColor = inverse
          ? AppColors.accent.withValues(alpha: 0.45)
          : AppColors.accent.withValues(alpha: 0.25);
      icon = Icons.check_circle_outline;
    } else if (norm == 'PENDIENTE_CONCILIACION' ||
        norm == 'PROCESANDO' ||
        norm == 'PENDIENTE') {
      bg = inverse
          ? AppColors.warning.withValues(alpha: 0.22)
          : AppColors.amberSoft;
      fg = inverse ? const Color(0xFFFDE68A) : const Color(0xFFB45309);
      borderColor = inverse
          ? AppColors.warning.withValues(alpha: 0.45)
          : AppColors.warning.withValues(alpha: 0.25);
      icon = Icons.schedule;
    } else if (norm == 'REVISION_MANUAL') {
      bg = AppColors.violetSoft;
      fg = AppColors.violet;
      borderColor = AppColors.violet.withValues(alpha: 0.25);
      icon = Icons.warning_amber_outlined;
    } else if (norm == 'INACTIVO' || norm == 'INACTIVA') {
      bg = const Color(0xFFF1F5F9);
      fg = AppColors.mutedText;
      borderColor = AppColors.border;
      icon = Icons.remove_circle_outline;
    } else {
      bg = inverse
          ? AppColors.danger.withValues(alpha: 0.22)
          : AppColors.roseSoft;
      fg = inverse ? const Color(0xFFFECDD3) : const Color(0xFFBE123C);
      borderColor = inverse
          ? AppColors.danger.withValues(alpha: 0.45)
          : AppColors.danger.withValues(alpha: 0.25);
      icon = Icons.cancel_outlined;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: bg,
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: borderColor),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: fg),
          const SizedBox(width: 5),
          Text(
            label,
            style: TextStyle(
              fontSize: 11.5,
              fontWeight: FontWeight.w700,
              color: fg,
            ),
          ),
        ],
      ),
    );
  }
}

class CopyChip extends StatefulWidget {
  const CopyChip({
    super.key,
    required this.value,
    this.displayLabel,
    this.copyLabel = 'Dato',
  });

  final String? value;
  final String? displayLabel;
  final String copyLabel;

  @override
  State<CopyChip> createState() => _CopyChipState();
}

class _CopyChipState extends State<CopyChip> {
  bool _copied = false;

  Future<void> _onTap() async {
    final val = widget.value?.trim() ?? '';
    if (val.isEmpty) return;
    await copyToClipboard(context, val, label: widget.copyLabel);
    if (!mounted) return;
    setState(() => _copied = true);
    Future.delayed(const Duration(milliseconds: 1600), () {
      if (mounted) setState(() => _copied = false);
    });
  }

  @override
  Widget build(BuildContext context) {
    final val = widget.value?.trim() ?? '';
    if (val.isEmpty) {
      return const Text('-', style: TextStyle(color: AppColors.mutedText));
    }

    return InkWell(
      onTap: _onTap,
      borderRadius: BorderRadius.circular(8),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
        decoration: BoxDecoration(
          color: _copied ? AppColors.emeraldSoft : const Color(0xFFF8FAFC),
          borderRadius: BorderRadius.circular(8),
          border: Border.all(
            color: _copied
                ? AppColors.accent.withValues(alpha: 0.4)
                : AppColors.border,
          ),
        ),
        child: Row(
          mainAxisSize: MainAxisSize.min,
          children: [
            Flexible(
              child: Text(
                widget.displayLabel ?? val,
                overflow: TextOverflow.ellipsis,
                style: TextStyle(
                  fontFamily: 'monospace',
                  fontSize: 12,
                  fontWeight: FontWeight.w700,
                  color: _copied ? const Color(0xFF047857) : AppColors.primary,
                ),
              ),
            ),
            const SizedBox(width: 5),
            Icon(
              _copied ? Icons.check : Icons.copy,
              size: 13,
              color: _copied ? AppColors.accent : AppColors.mutedText,
            ),
          ],
        ),
      ),
    );
  }
}

class ServiceLogo extends StatelessWidget {
  const ServiceLogo({
    super.key,
    this.urlLogo,
    this.tipoFront = 2,
    this.size = 46,
  });

  final String? urlLogo;
  final int tipoFront;
  final double size;

  @override
  Widget build(BuildContext context) {
    final clean = urlLogo?.trim();
    final fallbackIcon = tipoFront == 1
        ? Icons.smartphone_outlined
        : Icons.receipt_long_outlined;

    return Container(
      width: size,
      height: size,
      padding: const EdgeInsets.all(6),
      decoration: BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.border),
      ),
      child: (clean != null && clean.startsWith('http'))
          ? Image.network(
              clean,
              fit: BoxFit.contain,
              errorBuilder: (_, _, _) => Icon(
                fallbackIcon,
                size: size * 0.48,
                color: AppColors.secondary,
              ),
            )
          : Icon(
              fallbackIcon,
              size: size * 0.48,
              color: AppColors.secondary,
            ),
    );
  }
}
