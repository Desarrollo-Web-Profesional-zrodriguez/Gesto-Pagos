import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';

/// Widget de 6 casillas individuales para ingresar códigos OTP de verificación.
/// Soporta avance automático, pegado completo de 6 dígitos y retroceso entre casillas.
class OtpCodeInput extends StatefulWidget {
  const OtpCodeInput({
    super.key,
    required this.value,
    required this.onChange,
    this.hasError = false,
    this.enabled = true,
    this.autoFocus = false,
    this.onCompleted,
  });

  final String value;
  final ValueChanged<String> onChange;
  final bool hasError;
  final bool enabled;
  final bool autoFocus;
  final ValueChanged<String>? onCompleted;

  @override
  State<OtpCodeInput> createState() => _OtpCodeInputState();
}

class _OtpCodeInputState extends State<OtpCodeInput> {
  static const int _length = 6;
  late final List<TextEditingController> _controllers;
  late final List<FocusNode> _focusNodes;
  bool _syncing = false;

  @override
  void initState() {
    super.initState();
    _controllers = List.generate(_length, (_) => TextEditingController());
    _focusNodes = List.generate(_length, (_) => FocusNode());
    _syncFromValue(widget.value);
    if (widget.autoFocus) {
      WidgetsBinding.instance.addPostFrameCallback((_) {
        if (mounted && widget.enabled) {
          _focusNodes[0].requestFocus();
        }
      });
    }
  }

  @override
  void didUpdateWidget(covariant OtpCodeInput oldWidget) {
    super.didUpdateWidget(oldWidget);
    if (oldWidget.value != widget.value) {
      _syncFromValue(widget.value);
    }
  }

  void _syncFromValue(String raw) {
    final clean = raw.replaceAll(RegExp(r'\D'), '');
    _syncing = true;
    for (var i = 0; i < _length; i++) {
      final char = i < clean.length ? clean[i] : '';
      if (_controllers[i].text != char) {
        _controllers[i].text = char;
        _controllers[i].selection = TextSelection.collapsed(offset: char.length);
      }
    }
    _syncing = false;
  }

  @override
  void dispose() {
    for (final c in _controllers) {
      c.dispose();
    }
    for (final f in _focusNodes) {
      f.dispose();
    }
    super.dispose();
  }

  String _currentDigits() =>
      _controllers.map((c) => c.text.replaceAll(RegExp(r'\D'), '')).join();

  void _handleBoxChange(int index, String raw) {
    if (_syncing) return;
    final clean = raw.replaceAll(RegExp(r'\D'), '');

    if (clean.length > 1) {
      // Pegado o autocompletado de múltiples dígitos
      final digits = clean.length >= _length
          ? clean.substring(0, _length)
          : clean;
      _syncing = true;
      if (clean.length >= _length) {
        for (var i = 0; i < _length; i++) {
          _controllers[i].text = digits[i];
          _controllers[i].selection = const TextSelection.collapsed(offset: 1);
        }
        _focusNodes[_length - 1].requestFocus();
      } else {
        var cursor = index;
        for (var j = 0; j < digits.length && cursor < _length; j++, cursor++) {
          _controllers[cursor].text = digits[j];
          _controllers[cursor].selection =
              const TextSelection.collapsed(offset: 1);
        }
        final nextFocus = cursor < _length ? cursor : _length - 1;
        _focusNodes[nextFocus].requestFocus();
      }
      _syncing = false;
      final full = _currentDigits();
      widget.onChange(full);
      if (full.length == _length) {
        widget.onCompleted?.call(full);
      }
      setState(() {});
      return;
    }

    _syncing = true;
    _controllers[index].text = clean;
    _controllers[index].selection =
        TextSelection.collapsed(offset: clean.length);
    _syncing = false;

    if (clean.isNotEmpty && index < _length - 1) {
      _focusNodes[index + 1].requestFocus();
    } else if (clean.isEmpty && index > 0) {
      _focusNodes[index - 1].requestFocus();
    }

    final full = _currentDigits();
    widget.onChange(full);
    if (full.length == _length) {
      widget.onCompleted?.call(full);
    }
    setState(() {});
  }

  KeyEventResult _handleKeyEvent(int index, KeyEvent event) {
    if (event is KeyDownEvent &&
        event.logicalKey == LogicalKeyboardKey.backspace &&
        _controllers[index].text.isEmpty &&
        index > 0) {
      _syncing = true;
      _controllers[index - 1].clear();
      _syncing = false;
      _focusNodes[index - 1].requestFocus();
      widget.onChange(_currentDigits());
      setState(() {});
      return KeyEventResult.handled;
    }
    return KeyEventResult.ignored;
  }

  @override
  Widget build(BuildContext context) {
    return Semantics(
      label: 'Código de verificación de 6 dígitos',
      child: Row(
        children: List.generate(_length, (idx) {
          final filled = _controllers[idx].text.isNotEmpty;
          final borderColor = widget.hasError
              ? AppColors.danger
              : filled
                  ? AppColors.secondary.withValues(alpha: 0.75)
                  : AppColors.border;
          final fillColor = widget.hasError
              ? AppColors.roseSoft.withValues(alpha: 0.5)
              : filled
                  ? AppColors.skySoft
                  : Colors.white;

          return Expanded(
            child: Padding(
              padding: EdgeInsets.only(right: idx < _length - 1 ? 8 : 0),
              child: Focus(
                onKeyEvent: (_, event) => _handleKeyEvent(idx, event),
                child: TextField(
                  key: ValueKey('otp_box_$idx'),
                  controller: _controllers[idx],
                  focusNode: _focusNodes[idx],
                  enabled: widget.enabled,
                  keyboardType: TextInputType.number,
                  textInputAction: idx == _length - 1
                      ? TextInputAction.done
                      : TextInputAction.next,
                  textAlign: TextAlign.center,
                  autofillHints: idx == 0 ? const [AutofillHints.oneTimeCode] : null,
                  inputFormatters: [
                    FilteringTextInputFormatter.digitsOnly,
                    LengthLimitingTextInputFormatter(_length),
                  ],
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    fontSize: 20,
                    fontWeight: FontWeight.w800,
                    color: AppColors.primary,
                  ),
                  decoration: InputDecoration(
                    filled: true,
                    fillColor: fillColor,
                    counterText: '',
                    contentPadding: const EdgeInsets.symmetric(vertical: 14),
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(color: borderColor, width: 1.3),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: BorderSide(
                        color: widget.hasError
                            ? AppColors.danger
                            : AppColors.secondary,
                        width: 2,
                      ),
                    ),
                    disabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(12),
                      borderSide: const BorderSide(color: AppColors.border),
                    ),
                  ),
                  onChanged: (val) => _handleBoxChange(idx, val),
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}
