import 'package:flutter/material.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';

class SelectOption<T> {
  const SelectOption({
    required this.value,
    required this.label,
    this.badge,
    this.dotColor,
    this.searchTerms = '',
  });

  final T value;
  final String label;
  final String? badge;
  final Color? dotColor;
  final String searchTerms;
}

/// Selector moderno con modal bottom sheet estilizado y buscador opcional.
class ModernSelectField<T> extends FormField<T> {
  ModernSelectField({
    super.key,
    required this.label,
    required T? value,
    required this.options,
    required this.onChanged,
    this.sheetTitle,
    this.placeholder = 'Seleccionar...',
    this.searchable = false,
    this.searchHint = 'Buscar...',
    this.leadingIcon,
    super.enabled = true,
    this.mono = false,
    super.validator,
    super.autovalidateMode,
  }) : super(
          initialValue: value,
          builder: (state) {
            return _ModernSelectFieldBody<T>(
              label: label,
              value: value,
              options: options,
              sheetTitle: sheetTitle ?? label,
              placeholder: placeholder,
              searchable: searchable,
              searchHint: searchHint,
              leadingIcon: leadingIcon,
              enabled: state.widget.enabled,
              mono: mono,
              errorText: state.errorText,
              onSelected: (selected) {
                state.didChange(selected);
                onChanged(selected);
              },
            );
          },
        );

  final String label;
  final List<SelectOption<T>> options;
  final ValueChanged<T> onChanged;
  final String? sheetTitle;
  final String placeholder;
  final bool searchable;
  final String searchHint;
  final IconData? leadingIcon;
  final bool mono;
}

class _ModernSelectFieldBody<T> extends StatelessWidget {
  const _ModernSelectFieldBody({
    required this.label,
    required this.value,
    required this.options,
    required this.sheetTitle,
    required this.placeholder,
    required this.searchable,
    required this.searchHint,
    required this.leadingIcon,
    required this.enabled,
    required this.mono,
    required this.errorText,
    required this.onSelected,
  });

  final String label;
  final T? value;
  final List<SelectOption<T>> options;
  final String sheetTitle;
  final String placeholder;
  final bool searchable;
  final String searchHint;
  final IconData? leadingIcon;
  final bool enabled;
  final bool mono;
  final String? errorText;
  final ValueChanged<T> onSelected;

  SelectOption<T>? get _selectedOption {
    for (final opt in options) {
      if (opt.value == value) return opt;
    }
    return null;
  }

  Future<void> _openSheet(BuildContext context) async {
    if (!enabled) return;
    final picked = await showModalBottomSheet<T>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => _SelectBottomSheet<T>(
        title: sheetTitle,
        options: options,
        currentValue: value,
        searchable: searchable || options.length > 12,
        searchHint: searchHint,
        mono: mono,
      ),
    );
    if (picked != null) {
      onSelected(picked);
    }
  }

  @override
  Widget build(BuildContext context) {
    final selected = _selectedOption;
    final hasError = errorText != null && errorText!.isNotEmpty;

    return InkWell(
      onTap: enabled ? () => _openSheet(context) : null,
      borderRadius: BorderRadius.circular(12),
      child: InputDecorator(
        isEmpty: selected == null,
        decoration: InputDecoration(
          labelText: label,
          errorText: errorText,
          enabled: enabled,
          prefixIcon: leadingIcon != null
              ? Icon(leadingIcon, color: AppColors.secondary, size: 20)
              : null,
          suffixIcon: Icon(
            Icons.keyboard_arrow_down_rounded,
            color: hasError ? AppColors.danger : AppColors.mutedText,
          ),
        ),
        child: selected == null
            ? Text(
                placeholder,
                style: const TextStyle(
                  color: Color(0xFF94A3B8),
                  fontSize: 14,
                ),
              )
            : Row(
                children: [
                  if (selected.dotColor != null) ...[
                    Container(
                      width: 8,
                      height: 8,
                      decoration: BoxDecoration(
                        color: selected.dotColor,
                        shape: BoxShape.circle,
                      ),
                    ),
                    const SizedBox(width: 8),
                  ],
                  Expanded(
                    child: Text(
                      selected.label,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontFamily: mono ? 'monospace' : null,
                        fontSize: 14,
                        fontWeight: FontWeight.w600,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                  if (selected.badge != null && selected.badge!.isNotEmpty) ...[
                    const SizedBox(width: 6),
                    Container(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 8,
                        vertical: 2,
                      ),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(6),
                      ),
                      child: Text(
                        selected.badge!,
                        style: const TextStyle(
                          fontSize: 11,
                          fontWeight: FontWeight.w600,
                          color: AppColors.mutedText,
                        ),
                      ),
                    ),
                  ],
                ],
              ),
      ),
    );
  }
}

class _SelectBottomSheet<T> extends StatefulWidget {
  const _SelectBottomSheet({
    required this.title,
    required this.options,
    required this.currentValue,
    required this.searchable,
    required this.searchHint,
    required this.mono,
  });

  final String title;
  final List<SelectOption<T>> options;
  final T? currentValue;
  final bool searchable;
  final String searchHint;
  final bool mono;

  @override
  State<_SelectBottomSheet<T>> createState() => _SelectBottomSheetState<T>();
}

class _SelectBottomSheetState<T> extends State<_SelectBottomSheet<T>> {
  final _searchCtrl = TextEditingController();

  @override
  void dispose() {
    _searchCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final query = _searchCtrl.text.trim().toLowerCase();
    final filtered = query.isEmpty
        ? widget.options
        : widget.options.where((opt) {
            return opt.label.toLowerCase().contains(query) ||
                (opt.badge ?? '').toLowerCase().contains(query) ||
                opt.searchTerms.toLowerCase().contains(query);
          }).toList();

    final maxHeight = MediaQuery.of(context).size.height * 0.72;

    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.of(context).viewInsets.bottom),
      child: ConstrainedBox(
        constraints: BoxConstraints(maxHeight: maxHeight),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const SizedBox(height: 10),
            Container(
              width: 40,
              height: 4,
              decoration: BoxDecoration(
                color: AppColors.border,
                borderRadius: BorderRadius.circular(4),
              ),
            ),
            Padding(
              padding: const EdgeInsets.fromLTRB(20, 14, 12, 10),
              child: Row(
                children: [
                  Expanded(
                    child: Text(
                      widget.title,
                      style: const TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close, color: AppColors.mutedText),
                  ),
                ],
              ),
            ),
            if (widget.searchable)
              Padding(
                padding: const EdgeInsets.fromLTRB(16, 0, 16, 12),
                child: TextField(
                  controller: _searchCtrl,
                  autofocus: true,
                  onChanged: (_) => setState(() {}),
                  decoration: InputDecoration(
                    hintText: widget.searchHint,
                    prefixIcon: const Icon(Icons.search, size: 20),
                    suffixIcon: _searchCtrl.text.isNotEmpty
                        ? IconButton(
                            onPressed: () {
                              _searchCtrl.clear();
                              setState(() {});
                            },
                            icon: const Icon(Icons.clear, size: 18),
                          )
                        : null,
                  ),
                ),
              ),
            const Divider(height: 1, color: AppColors.border),
            Flexible(
              child: filtered.isEmpty
                  ? Padding(
                      padding: const EdgeInsets.symmetric(
                        horizontal: 20,
                        vertical: 32,
                      ),
                      child: Text(
                        'Sin resultados para "${_searchCtrl.text.trim()}"',
                        style: const TextStyle(color: AppColors.mutedText),
                      ),
                    )
                  : ListView.separated(
                      shrinkWrap: true,
                      padding: const EdgeInsets.fromLTRB(12, 8, 12, 20),
                      itemCount: filtered.length,
                      separatorBuilder: (_, _) => const SizedBox(height: 4),
                      itemBuilder: (ctx, idx) {
                        final opt = filtered[idx];
                        final selected = opt.value == widget.currentValue;
                        return ListTile(
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(12),
                          ),
                          tileColor: selected ? AppColors.skySoft : null,
                          onTap: () => Navigator.of(ctx).pop(opt.value),
                          leading: opt.dotColor != null
                              ? Container(
                                  width: 10,
                                  height: 10,
                                  decoration: BoxDecoration(
                                    color: opt.dotColor,
                                    shape: BoxShape.circle,
                                  ),
                                )
                              : null,
                          title: Text(
                            opt.label,
                            style: TextStyle(
                              fontFamily: widget.mono ? 'monospace' : null,
                              fontWeight:
                                  selected ? FontWeight.w700 : FontWeight.w500,
                              color: selected
                                  ? AppColors.secondary
                                  : AppColors.primary,
                              fontSize: 14,
                            ),
                          ),
                          subtitle: opt.badge != null && opt.badge!.isNotEmpty
                              ? Text(
                                  opt.badge!,
                                  style: const TextStyle(
                                    fontSize: 12,
                                    color: AppColors.mutedText,
                                  ),
                                )
                              : null,
                          trailing: selected
                              ? const Icon(
                                  Icons.check_rounded,
                                  color: AppColors.secondary,
                                )
                              : null,
                        );
                      },
                    ),
            ),
          ],
        ),
      ),
    );
  }
}

/// Selector moderno de fecha de nacimiento (Día / Mes / Año con buscador + calendario).
class BirthDateSelector extends FormField<String> {
  BirthDateSelector({
    super.key,
    required String value,
    required ValueChanged<String> onChanged,
    String label = 'Fecha de nacimiento *',
    super.validator = BackendValidators.fechaNacimientoMayorEdad,
  }) : super(
          initialValue: value,
          builder: (state) {
            return _BirthDateSelectorBody(
              label: label,
              value: state.value ?? value,
              errorText: state.errorText,
              onChanged: (next) {
                state.didChange(next);
                onChanged(next);
              },
            );
          },
        );
}

class _BirthDateSelectorBody extends StatelessWidget {
  const _BirthDateSelectorBody({
    required this.label,
    required this.value,
    required this.errorText,
    required this.onChanged,
  });

  final String label;
  final String value;
  final String? errorText;
  final ValueChanged<String> onChanged;

  static const _meses = <SelectOption<String>>[
    SelectOption(value: '01', label: 'Enero'),
    SelectOption(value: '02', label: 'Febrero'),
    SelectOption(value: '03', label: 'Marzo'),
    SelectOption(value: '04', label: 'Abril'),
    SelectOption(value: '05', label: 'Mayo'),
    SelectOption(value: '06', label: 'Junio'),
    SelectOption(value: '07', label: 'Julio'),
    SelectOption(value: '08', label: 'Agosto'),
    SelectOption(value: '09', label: 'Septiembre'),
    SelectOption(value: '10', label: 'Octubre'),
    SelectOption(value: '11', label: 'Noviembre'),
    SelectOption(value: '12', label: 'Diciembre'),
  ];

  int _daysInMonth(int year, int month) => DateTime(year, month + 1, 0).day;

  String _monthShortLabel(String mm) {
    final m = int.tryParse(mm) ?? 1;
    if (m < 1 || m > 12) return mm;
    return _meses[m - 1].label;
  }

  Future<void> _pickPart<T>(
    BuildContext context, {
    required String title,
    required List<SelectOption<T>> options,
    required T current,
    required bool searchable,
    required ValueChanged<T> onPicked,
  }) async {
    final res = await showModalBottomSheet<T>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.white,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (_) => _SelectBottomSheet<T>(
        title: title,
        options: options,
        currentValue: current,
        searchable: searchable,
        searchHint: 'Buscar...',
        mono: true,
      ),
    );
    if (res != null) onPicked(res);
  }

  Future<void> _openCalendar(BuildContext context, String y, String m, String d) async {
    final now = DateTime.now();
    final parsed = DateTime.tryParse('$y-$m-$d') ?? DateTime(now.year - 25, 5, 20);
    final picked = await showDatePicker(
      context: context,
      initialDate: parsed.isBefore(now) ? parsed : DateTime(now.year - 25, 5, 20),
      firstDate: DateTime(1920),
      lastDate: now,
    );
    if (picked != null) {
      final yy = picked.year.toString().padLeft(4, '0');
      final mm = picked.month.toString().padLeft(2, '0');
      final dd = picked.day.toString().padLeft(2, '0');
      onChanged('$yy-$mm-$dd');
    }
  }

  @override
  Widget build(BuildContext context) {
    final parts = value.split('-');
    final yStr = parts.length == 3 ? parts[0] : '1995';
    final mStr = parts.length == 3 ? parts[1] : '05';
    final dStr = parts.length == 3 ? parts[2] : '20';

    final yearNum = int.tryParse(yStr) ?? 1995;
    final monthNum = int.tryParse(mStr) ?? 5;
    final maxDays = _daysInMonth(yearNum, monthNum);

    final nowYear = DateTime.now().year;
    final yearOptions = List.generate(83, (i) {
      final yr = (nowYear - 18 - i).toString();
      return SelectOption<String>(value: yr, label: yr);
    });
    final dayOptions = List.generate(maxDays, (i) {
      final dd = (i + 1).toString().padLeft(2, '0');
      return SelectOption<String>(value: dd, label: dd);
    });

    void update(String newY, String newM, String newD) {
      final limit = _daysInMonth(int.tryParse(newY) ?? 1995, int.tryParse(newM) ?? 1);
      final clampedDay =
          (int.tryParse(newD) ?? 1).clamp(1, limit).toString().padLeft(2, '0');
      onChanged('$newY-$newM-$clampedDay');
    }

    final hasError = errorText != null && errorText!.isNotEmpty;

    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Row(
          children: [
            Expanded(
              child: Row(
                children: [
                  const Icon(
                    Icons.cake_outlined,
                    size: 16,
                    color: AppColors.secondary,
                  ),
                  const SizedBox(width: 6),
                  Expanded(
                    child: Text(
                      label,
                      style: const TextStyle(
                        fontSize: 12.5,
                        fontWeight: FontWeight.w700,
                        color: AppColors.primary,
                      ),
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(width: 6),
            TextButton.icon(
              style: TextButton.styleFrom(
                visualDensity: VisualDensity.compact,
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
              ),
              onPressed: () => _openCalendar(context, yStr, mStr, dStr),
              icon: const Icon(Icons.calendar_month_outlined, size: 15),
              label: const Text('Calendario', style: TextStyle(fontSize: 12)),
            ),
          ],
        ),
        const SizedBox(height: 6),
        Row(
          children: [
            Expanded(
              flex: 2,
              child: _DateSegmentButton(
                caption: 'Día',
                valueLabel: dStr,
                hasError: hasError,
                mono: true,
                onTap: () => _pickPart<String>(
                  context,
                  title: 'Día de nacimiento',
                  options: dayOptions,
                  current: dStr,
                  searchable: false,
                  onPicked: (val) => update(yStr, mStr, val),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              flex: 3,
              child: _DateSegmentButton(
                caption: 'Mes',
                valueLabel: _monthShortLabel(mStr),
                hasError: hasError,
                mono: false,
                onTap: () => _pickPart<String>(
                  context,
                  title: 'Mes de nacimiento',
                  options: _meses,
                  current: mStr,
                  searchable: false,
                  onPicked: (val) => update(yStr, val, dStr),
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              flex: 2,
              child: _DateSegmentButton(
                caption: 'Año',
                valueLabel: yStr,
                hasError: hasError,
                mono: true,
                onTap: () => _pickPart<String>(
                  context,
                  title: 'Año de nacimiento',
                  options: yearOptions,
                  current: yStr,
                  searchable: true,
                  onPicked: (val) => update(val, mStr, dStr),
                ),
              ),
            ),
          ],
        ),
        if (hasError) ...[
          const SizedBox(height: 6),
          Padding(
            padding: const EdgeInsets.only(left: 12),
            child: Text(
              errorText!,
              style: const TextStyle(color: AppColors.danger, fontSize: 12),
            ),
          ),
        ],
      ],
    );
  }
}

class _DateSegmentButton extends StatelessWidget {
  const _DateSegmentButton({
    required this.caption,
    required this.valueLabel,
    required this.hasError,
    required this.mono,
    required this.onTap,
  });

  final String caption;
  final String valueLabel;
  final bool hasError;
  final bool mono;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      onTap: onTap,
      borderRadius: BorderRadius.circular(12),
      child: Container(
        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 10),
        decoration: BoxDecoration(
          color: Colors.white,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(
            color: hasError ? AppColors.danger : AppColors.border,
          ),
        ),
        child: Row(
          children: [
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    caption,
                    style: const TextStyle(
                      fontSize: 10.5,
                      color: AppColors.mutedText,
                    ),
                  ),
                  const SizedBox(height: 2),
                  Text(
                    valueLabel,
                    overflow: TextOverflow.ellipsis,
                    style: TextStyle(
                      fontFamily: mono ? 'monospace' : null,
                      fontSize: 13.5,
                      fontWeight: FontWeight.w700,
                      color: AppColors.primary,
                    ),
                  ),
                ],
              ),
            ),
            const Icon(
              Icons.keyboard_arrow_down_rounded,
              size: 18,
              color: AppColors.mutedText,
            ),
          ],
        ),
      ),
    );
  }
}
