import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/config/app_config.dart';
import 'package:gestopago_app/core/network/api_error.dart';
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
  if (numCuenta == null || numCuenta.length < 4) return '****';
  final clean = numCuenta.trim();
  return '**** **** **** ${clean.substring(clean.length - 4)}';
}

Future<void> copyToClipboard(
  BuildContext context,
  String value, {
  String label = 'Dato',
}) async {
  await Clipboard.setData(ClipboardData(text: value));
  if (!context.mounted) return;
  ScaffoldMessenger.of(context).showSnackBar(
    SnackBar(content: Text('$label copiado al portapapeles')),
  );
}

class ErrorBanner extends StatelessWidget {
  const ErrorBanner({
    super.key,
    required this.error,
    this.onRetry,
  });

  final Object error;
  final VoidCallback? onRetry;

  @override
  Widget build(BuildContext context) {
    final apiError = error is ApiException ? error as ApiException : null;
    final message = apiError?.mensaje ?? error.toString();

    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppColors.danger.withValues(alpha: 0.08),
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppColors.danger.withValues(alpha: 0.35)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.error_outline, color: AppColors.danger, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  apiError?.codigoCatalogo ?? 'Error en la operación',
                  style: const TextStyle(
                    fontWeight: FontWeight.w700,
                    color: AppColors.danger,
                  ),
                ),
              ),
              if (apiError?.codigo != null)
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                  decoration: BoxDecoration(
                    color: AppColors.danger.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(10),
                  ),
                  child: Text(
                    'Cód. ${apiError!.codigo}',
                    style: const TextStyle(
                      fontSize: 11,
                      fontWeight: FontWeight.w700,
                      color: AppColors.danger,
                    ),
                  ),
                ),
            ],
          ),
          const SizedBox(height: 6),
          Text(message, style: const TextStyle(fontSize: 13)),
          if (apiError != null && apiError.detallesValidacion.isNotEmpty) ...[
            const SizedBox(height: 8),
            ...apiError.detallesValidacion.entries.map(
              (entry) => Padding(
                padding: const EdgeInsets.only(bottom: 2),
                child: Text(
                  '• ${entry.key}: ${entry.value}',
                  style: const TextStyle(fontSize: 12, color: AppColors.primary),
                ),
              ),
            ),
          ],
          if (onRetry != null) ...[
            const SizedBox(height: 10),
            Align(
              alignment: Alignment.centerRight,
              child: TextButton.icon(
                onPressed: onRetry,
                icon: const Icon(Icons.refresh, size: 18),
                label: const Text('Reintentar'),
              ),
            ),
          ],
        ],
      ),
    );
  }
}

Future<void> showServerConfigDialog(BuildContext context, WidgetRef ref) async {
  final current = ref.read(apiBaseUrlProvider);
  final controller = TextEditingController(text: current);

  await showDialog<void>(
    context: context,
    builder: (ctx) => AlertDialog(
      title: const Text('Servidor Backend'),
      content: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          const Text(
            'Configura la URL base de la API Spring Boot:',
            style: TextStyle(fontSize: 13, color: AppColors.mutedText),
          ),
          const SizedBox(height: 12),
          TextField(
            controller: controller,
            decoration: const InputDecoration(
              labelText: 'API Base URL',
              hintText: 'http://10.0.2.2:8080',
            ),
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: AppEnvironment.presets
                .map(
                  (preset) => ActionChip(
                    label: Text(preset.label, style: const TextStyle(fontSize: 11)),
                    onPressed: () => controller.text = preset.url,
                  ),
                )
                .toList(),
          ),
        ],
      ),
      actions: [
        TextButton(
          onPressed: () => Navigator.of(ctx).pop(),
          child: const Text('Cancelar'),
        ),
        FilledButton(
          style: FilledButton.styleFrom(minimumSize: const Size(110, 42)),
          onPressed: () {
            ref.read(apiBaseUrlProvider.notifier).setBaseUrl(controller.text);
            Navigator.of(ctx).pop();
            ScaffoldMessenger.of(context).showSnackBar(
              SnackBar(
                content: Text(
                  'Servidor actualizado: ${ref.read(apiBaseUrlProvider)}',
                ),
              ),
            );
          },
          child: const Text('Guardar'),
        ),
      ],
    ),
  );
}
