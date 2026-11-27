import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/core/widgets/modern_selectors.dart';
import 'package:gestopago_app/features/catalogos_sistema/data/nacionalidad_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/data/cliente_repository.dart';

class ClienteEditScreen extends ConsumerStatefulWidget {
  const ClienteEditScreen({
    super.key,
    required this.cliente,
    this.fullUpdateMode = false,
    this.allowFullReplace = false,
  });

  final ClienteDto cliente;
  final bool fullUpdateMode;
  final bool allowFullReplace;

  @override
  ConsumerState<ClienteEditScreen> createState() => _ClienteEditScreenState();
}

class _ClienteEditScreenState extends ConsumerState<ClienteEditScreen> {
  final _formKey = GlobalKey<FormState>();
  late bool _fullReplaceMode = widget.allowFullReplace && widget.fullUpdateMode;
  bool _saving = false;
  bool _obscurePassword = true;
  Object? _error;
  String? _validationSummary;

  late final TextEditingController _nombreCtrl;
  late final TextEditingController _segundoNombreCtrl;
  late final TextEditingController _paternoCtrl;
  late final TextEditingController _maternoCtrl;
  late String _fechaNacimiento;
  late String _sexo;
  late String _estadoCivil;
  NacionalidadDto? _nacionalidadSeleccionada;

  late final TextEditingController _curpCtrl;
  late final TextEditingController _rfcCtrl;
  late final TextEditingController _correoCtrl;
  late final TextEditingController _telMovilCtrl;
  late final TextEditingController _telAltCtrl;
  late final TextEditingController _ocupacionCtrl;
  late final TextEditingController _empresaCtrl;
  late final TextEditingController _ingresoCtrl;
  late final TextEditingController _saldoRefCtrl;
  late final TextEditingController _calleCtrl;
  late final TextEditingController _numExtCtrl;
  late final TextEditingController _numIntCtrl;
  late final TextEditingController _coloniaCtrl;
  late final TextEditingController _municipioCtrl;
  late final TextEditingController _estadoCtrl;
  late final TextEditingController _cpCtrl;
  late final TextEditingController _paisCtrl;
  final _passwordCtrl = TextEditingController();

  static const _sexoOptions = <SelectOption<String>>[
    SelectOption(value: 'Masculino', label: 'Masculino'),
    SelectOption(value: 'Femenino', label: 'Femenino'),
  ];

  static const _estadoCivilOptions = <SelectOption<String>>[
    SelectOption(value: 'Soltero', label: 'Soltero(a)'),
    SelectOption(value: 'Casado', label: 'Casado(a)'),
    SelectOption(value: 'Unión libre', label: 'Unión libre'),
    SelectOption(value: 'Viudo', label: 'Viudo(a)'),
  ];

  static String _normalizeSexo(String raw) {
    final norm = raw.trim().toLowerCase();
    if (norm == 'femenino') return 'Femenino';
    return 'Masculino';
  }

  static String _normalizeEstadoCivil(String raw) {
    final norm = raw.trim().toLowerCase();
    if (norm == 'casado') return 'Casado';
    if (norm == 'union libre' || norm == 'unión libre') return 'Unión libre';
    if (norm == 'viudo') return 'Viudo';
    return 'Soltero';
  }

  @override
  void initState() {
    super.initState();
    final c = widget.cliente;
    final dom = c.domicilios.isNotEmpty ? c.domicilios.first : null;
    _nombreCtrl = TextEditingController(text: c.nombre);
    _segundoNombreCtrl = TextEditingController(text: c.segundoNombre ?? '');
    _paternoCtrl = TextEditingController(text: c.apellidoPaterno);
    _maternoCtrl = TextEditingController(text: c.apellidoMaterno);
    _fechaNacimiento =
        c.fechaNacimiento.isNotEmpty ? c.fechaNacimiento : '1995-05-20';
    _sexo = _normalizeSexo(c.sexo);
    _estadoCivil = _normalizeEstadoCivil(c.estadoCivil);

    _curpCtrl = TextEditingController(text: c.curp);
    _rfcCtrl = TextEditingController(text: c.rfc);
    _correoCtrl = TextEditingController(text: c.correoElectronico);
    _telMovilCtrl = TextEditingController(text: c.telefonoMovil);
    _telAltCtrl = TextEditingController(text: c.telefonoAlternativo ?? '');
    _ocupacionCtrl = TextEditingController(text: c.ocupacion);
    _empresaCtrl = TextEditingController(text: c.empresa);
    _ingresoCtrl = TextEditingController(
      text: StrictDecimal.fromDouble(c.ingresoMensual).toStrictLiteral(),
    );
    final saldoActual = c.cuentaPrincipal?.saldo?.saldoDisponible ?? 0.0;
    _saldoRefCtrl = TextEditingController(
      text: StrictDecimal.fromDouble(saldoActual).toStrictLiteral(),
    );
    _calleCtrl = TextEditingController(text: dom?.calle ?? '');
    _numExtCtrl = TextEditingController(text: dom?.numeroExterior ?? '');
    _numIntCtrl = TextEditingController(text: dom?.numeroInterior ?? '');
    _coloniaCtrl = TextEditingController(text: dom?.colonia ?? '');
    _municipioCtrl = TextEditingController(text: dom?.municipio ?? '');
    _estadoCtrl = TextEditingController(text: dom?.estado ?? '');
    _cpCtrl = TextEditingController(text: dom?.codigoPostal ?? '');
    _paisCtrl = TextEditingController(text: dom?.pais ?? 'Mexico');
  }

  @override
  void dispose() {
    for (final c in [
      _nombreCtrl,
      _segundoNombreCtrl,
      _paternoCtrl,
      _maternoCtrl,
      _curpCtrl,
      _rfcCtrl,
      _correoCtrl,
      _telMovilCtrl,
      _telAltCtrl,
      _ocupacionCtrl,
      _empresaCtrl,
      _ingresoCtrl,
      _saldoRefCtrl,
      _calleCtrl,
      _numExtCtrl,
      _numIntCtrl,
      _coloniaCtrl,
      _municipioCtrl,
      _estadoCtrl,
      _cpCtrl,
      _paisCtrl,
      _passwordCtrl,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _save() async {
    setState(() {
      _error = null;
      _validationSummary = null;
    });

    if (!(_formKey.currentState?.validate() ?? false)) {
      setState(() {
        _validationSummary =
            'Revisa los campos marcados en rojo antes de guardar los cambios.';
      });
      return;
    }

    setState(() => _saving = true);

    try {
      final dom = DomicilioDto(
        calle: _calleCtrl.text,
        numeroExterior: _numExtCtrl.text,
        numeroInterior: _numIntCtrl.text,
        colonia: _coloniaCtrl.text,
        municipio: _municipioCtrl.text,
        estado: _estadoCtrl.text,
        codigoPostal: _cpCtrl.text,
        pais: _paisCtrl.text,
      );

      final idNac =
          _nacionalidadSeleccionada?.idNacionalidad ?? widget.cliente.idNacionalidad;
      final nacNombre =
          _nacionalidadSeleccionada?.gentilicio ?? widget.cliente.nacionalidad;

      final repo = ref.read(clienteRepositoryProvider);
      final ClienteDto updated;
      if (_fullReplaceMode) {
        updated = await repo.actualizarCompleto(
          ClienteRegistroRequestDto(
            identificador: widget.cliente.curp,
            nombre: _nombreCtrl.text,
            segundoNombre: _segundoNombreCtrl.text,
            apellidoPaterno: _paternoCtrl.text,
            apellidoMaterno: _maternoCtrl.text,
            fechaNacimiento: _fechaNacimiento,
            curp: _curpCtrl.text,
            rfc: _rfcCtrl.text,
            sexo: _sexo,
            idNacionalidad: idNac,
            nacionalidad: nacNombre,
            estadoCivil: _estadoCivil,
            correoElectronico: _correoCtrl.text,
            telefonoMovil: _telMovilCtrl.text,
            telefonoAlternativo: _telAltCtrl.text,
            domicilio: dom,
            ocupacion: _ocupacionCtrl.text,
            empresa: _empresaCtrl.text,
            ingresoMensual: StrictDecimal.parse(_ingresoCtrl.text),
            saldoInicial: StrictDecimal.parse(_saldoRefCtrl.text),
            password: _passwordCtrl.text,
          ),
        );
      } else {
        updated = await repo.actualizarParcial(
          ClientePatchRequestDto(
            identificador: widget.cliente.curp,
            nombre: _nombreCtrl.text,
            segundoNombre: _segundoNombreCtrl.text,
            apellidoPaterno: _paternoCtrl.text,
            apellidoMaterno: _maternoCtrl.text,
            fechaNacimiento: _fechaNacimiento,
            sexo: _sexo,
            idNacionalidad: idNac,
            nacionalidad: nacNombre,
            estadoCivil: _estadoCivil,
            correoElectronico: _correoCtrl.text,
            telefonoMovil: _telMovilCtrl.text,
            telefonoAlternativo: _telAltCtrl.text,
            domicilio: dom,
            ocupacion: _ocupacionCtrl.text,
            empresa: _empresaCtrl.text,
            ingresoMensual: StrictDecimal.parse(_ingresoCtrl.text),
          ),
        );
      }
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text('Expediente actualizado correctamente.'),
        ),
      );
      Navigator.of(context).pop(updated);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final nacionalidadesAsync = ref.watch(nacionalidadesCatalogProvider);

    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        title: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Editar Expediente',
              style: TextStyle(fontSize: 16.5, fontWeight: FontWeight.w800),
            ),
            Text(
              widget.cliente.nombreCompleto,
              overflow: TextOverflow.ellipsis,
              style: const TextStyle(fontSize: 11.5, color: Color(0xFF94A3B8)),
            ),
          ],
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            if (widget.allowFullReplace) ...[
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(12),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      SegmentedButton<bool>(
                        segments: const [
                          ButtonSegment(
                            value: false,
                            label: Text('Actualización de Datos'),
                            icon: Icon(Icons.edit_note_outlined, size: 18),
                          ),
                          ButtonSegment(
                            value: true,
                            label: Text('Reemplazo Integral'),
                            icon: Icon(Icons.sync_alt, size: 18),
                          ),
                        ],
                        selected: {_fullReplaceMode},
                        onSelectionChanged: (val) {
                          setState(() {
                            _fullReplaceMode = val.first;
                            _validationSummary = null;
                          });
                        },
                      ),
                      const SizedBox(height: 10),
                      Container(
                        padding: const EdgeInsets.all(10),
                        decoration: BoxDecoration(
                          color: _fullReplaceMode
                              ? AppColors.amberSoft
                              : AppColors.skySoft,
                          borderRadius: BorderRadius.circular(10),
                        ),
                        child: Text(
                          _fullReplaceMode
                              ? 'Actualiza el expediente completo incluyendo verificación fiscal, saldo de referencia y nueva contraseña.'
                              : 'Modifica datos personales, contacto, actividad laboral y domicilio conservando CURP, RFC y contraseña actuales.',
                          style: TextStyle(
                            fontSize: 12,
                            color: _fullReplaceMode
                                ? const Color(0xFF78350F)
                                : AppColors.sky950,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 12),
            ],
            if (_error != null) ...[
              ErrorBanner(
                error: _error!,
                onDismiss: () => setState(() => _error = null),
              ),
              const SizedBox(height: 12),
            ],
            if (_validationSummary != null) ...[
              ValidationSummary(
                message: _validationSummary,
                onDismiss: () => setState(() => _validationSummary = null),
              ),
              const SizedBox(height: 12),
            ],

            // Sección 1: Información Personal
            _EditSectionCard(
              icon: Icons.person_outline,
              title: 'Información Personal',
              children: [
                if (!_fullReplaceMode) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppColors.background,
                      borderRadius: BorderRadius.circular(10),
                      border: Border.all(color: AppColors.border),
                    ),
                    child: Text(
                      'CURP: ${widget.cliente.curp} • RFC: ${widget.cliente.rfc} (protegidos contra edición)',
                      style: const TextStyle(
                        fontSize: 12,
                        color: AppColors.mutedText,
                        fontFamily: 'monospace',
                      ),
                    ),
                  ),
                  const SizedBox(height: 12),
                ],
                TextFormField(
                  controller: _nombreCtrl,
                  decoration: const InputDecoration(labelText: 'Primer nombre *'),
                  validator: (v) =>
                      BackendValidators.soloLetrasRequerido(v, 'El nombre'),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _segundoNombreCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Segundo nombre (opcional)',
                  ),
                  validator: (v) => BackendValidators.soloLetrasOpcional(
                    v,
                    'El segundo nombre',
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _paternoCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Apellido paterno *',
                  ),
                  validator: (v) => BackendValidators.soloLetrasRequerido(
                    v,
                    'El apellido paterno',
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _maternoCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Apellido materno *',
                  ),
                  validator: (v) => BackendValidators.soloLetrasRequerido(
                    v,
                    'El apellido materno',
                  ),
                ),
                const SizedBox(height: 14),
                BirthDateSelector(
                  value: _fechaNacimiento,
                  onChanged: (next) => setState(() => _fechaNacimiento = next),
                ),
                const SizedBox(height: 14),
                Row(
                  children: [
                    Expanded(
                      child: ModernSelectField<String>(
                        label: 'Sexo *',
                        value: _sexo,
                        options: _sexoOptions,
                        onChanged: (v) => setState(() => _sexo = v),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: ModernSelectField<String>(
                        label: 'Estado civil *',
                        value: _estadoCivil,
                        options: _estadoCivilOptions,
                        onChanged: (v) => setState(() => _estadoCivil = v),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                nacionalidadesAsync.when(
                  data: (items) {
                    if (_nacionalidadSeleccionada == null && items.isNotEmpty) {
                      final byId = items.where(
                        (n) => n.idNacionalidad == widget.cliente.idNacionalidad,
                      );
                      if (byId.isNotEmpty) {
                        _nacionalidadSeleccionada = byId.first;
                      } else if (widget.cliente.nacionalidad != null) {
                        final target =
                            widget.cliente.nacionalidad!.trim().toLowerCase();
                        final byName = items.where(
                          (n) =>
                              n.gentilicio.toLowerCase() == target ||
                              n.pais.toLowerCase() == target,
                        );
                        if (byName.isNotEmpty) {
                          _nacionalidadSeleccionada = byName.first;
                        }
                      }
                    }
                    final options = items
                        .map(
                          (n) => SelectOption<NacionalidadDto>(
                            value: n,
                            label: n.gentilicio,
                            badge: '${n.pais} · ${n.claveIso}',
                            searchTerms: '${n.pais} ${n.claveIso}',
                          ),
                        )
                        .toList();
                    return ModernSelectField<NacionalidadDto>(
                      label: 'Nacionalidad',
                      sheetTitle: 'Selecciona una nacionalidad',
                      placeholder: widget.cliente.nacionalidad ??
                          'Seleccionar nacionalidad',
                      leadingIcon: Icons.public_outlined,
                      searchable: true,
                      searchHint: 'Buscar país, nacionalidad o código...',
                      value: _nacionalidadSeleccionada,
                      options: options,
                      onChanged: (v) =>
                          setState(() => _nacionalidadSeleccionada = v),
                    );
                  },
                  loading: () => const LinearProgressIndicator(),
                  error: (_, _) => const SizedBox.shrink(),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Sección 2: Datos de Contacto
            _EditSectionCard(
              icon: Icons.phone_outlined,
              title: 'Datos de Contacto',
              children: [
                TextFormField(
                  controller: _correoCtrl,
                  keyboardType: TextInputType.emailAddress,
                  decoration: const InputDecoration(
                    labelText: 'Correo electrónico *',
                    prefixIcon: Icon(Icons.email_outlined),
                  ),
                  validator: (v) => BackendValidators.email(v),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _telMovilCtrl,
                        keyboardType: TextInputType.phone,
                        maxLength: 10,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: const InputDecoration(
                          labelText: 'Teléfono móvil *',
                          counterText: '',
                        ),
                        validator: (v) => BackendValidators.telefono(v),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextFormField(
                        controller: _telAltCtrl,
                        keyboardType: TextInputType.phone,
                        maxLength: 10,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: const InputDecoration(
                          labelText: 'Tel. alternativo',
                          counterText: '',
                        ),
                        validator: (v) => BackendValidators.telefono(
                          v,
                          requiredField: false,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Sección 3: Perfil Laboral e Ingresos
            _EditSectionCard(
              icon: Icons.work_outline,
              title: 'Perfil Laboral e Ingresos',
              children: [
                TextFormField(
                  controller: _ocupacionCtrl,
                  decoration: const InputDecoration(labelText: 'Ocupación *'),
                  validator: (v) => BackendValidators.soloLetrasRequerido(
                    v,
                    'La ocupación',
                    min: 3,
                    max: 100,
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _empresaCtrl,
                  decoration: const InputDecoration(labelText: 'Empresa *'),
                  validator: BackendValidators.empresa,
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _ingresoCtrl,
                  keyboardType: const TextInputType.numberWithOptions(
                    decimal: true,
                  ),
                  decoration: const InputDecoration(
                    labelText: 'Ingreso mensual *',
                    prefixText: r'$ ',
                    suffixText: 'MXN',
                  ),
                  validator: (v) => BackendValidators.montoDecimal(
                    v,
                    'El ingreso mensual',
                    min: 0.01,
                  ),
                ),
              ],
            ),
            const SizedBox(height: 12),

            // Sección 4: Domicilio Residencial
            _EditSectionCard(
              icon: Icons.location_on_outlined,
              title: 'Domicilio Residencial',
              children: [
                TextFormField(
                  controller: _calleCtrl,
                  decoration: const InputDecoration(labelText: 'Calle *'),
                  validator: (v) =>
                      BackendValidators.calleOColonia(v, 'La calle', max: 150),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _numExtCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Núm. Exterior *',
                        ),
                        validator: (v) => BackendValidators.numeroDomicilio(
                          v,
                          'El número exterior',
                          requiredField: true,
                        ),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextFormField(
                        controller: _numIntCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Núm. Interior',
                        ),
                        validator: (v) => BackendValidators.numeroDomicilio(
                          v,
                          'El número interior',
                          requiredField: false,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _coloniaCtrl,
                  decoration: const InputDecoration(labelText: 'Colonia *'),
                  validator: (v) => BackendValidators.calleOColonia(
                    v,
                    'La colonia',
                    max: 100,
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _municipioCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Municipio / Alcaldía *',
                  ),
                  validator: (v) => BackendValidators.soloLetrasRequerido(
                    v,
                    'El municipio',
                    min: 2,
                    max: 100,
                  ),
                ),
                const SizedBox(height: 12),
                TextFormField(
                  controller: _estadoCtrl,
                  decoration: const InputDecoration(labelText: 'Estado *'),
                  validator: (v) => BackendValidators.soloLetrasRequerido(
                    v,
                    'El estado',
                    min: 2,
                    max: 100,
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextFormField(
                        controller: _cpCtrl,
                        keyboardType: TextInputType.number,
                        maxLength: 5,
                        inputFormatters: [
                          FilteringTextInputFormatter.digitsOnly,
                        ],
                        decoration: const InputDecoration(
                          labelText: 'Código Postal *',
                          counterText: '',
                        ),
                        validator: BackendValidators.codigoPostal,
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: TextFormField(
                        controller: _paisCtrl,
                        decoration: const InputDecoration(labelText: 'País *'),
                        validator: (v) => BackendValidators.soloLetrasRequerido(
                          v,
                          'El país',
                          min: 2,
                          max: 50,
                        ),
                      ),
                    ),
                  ],
                ),
              ],
            ),

            // Sección 5: Identidad Fiscal y Seguridad (cuando está en modo reemplazo completo)
            if (_fullReplaceMode) ...[
              const SizedBox(height: 12),
              _EditSectionCard(
                icon: Icons.security_outlined,
                title: 'Identidad Fiscal y Seguridad',
                borderColor: AppColors.warning.withValues(alpha: 0.45),
                children: [
                  TextFormField(
                    controller: _curpCtrl,
                    maxLength: 18,
                    textCapitalization: TextCapitalization.characters,
                    decoration: const InputDecoration(
                      labelText: 'CURP (18 caracteres) *',
                      counterText: '',
                    ),
                    validator: BackendValidators.curp,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _rfcCtrl,
                    maxLength: 13,
                    textCapitalization: TextCapitalization.characters,
                    decoration: const InputDecoration(
                      labelText: 'RFC con homoclave *',
                      counterText: '',
                    ),
                    validator: BackendValidators.rfc,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _saldoRefCtrl,
                    keyboardType: const TextInputType.numberWithOptions(
                      decimal: true,
                    ),
                    decoration: const InputDecoration(
                      labelText: 'Saldo de referencia *',
                      prefixText: r'$ ',
                      suffixText: 'MXN',
                    ),
                    validator: (v) => BackendValidators.montoDecimal(
                      v,
                      'El saldo de referencia',
                      min: 0.0,
                    ),
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _passwordCtrl,
                    obscureText: _obscurePassword,
                    decoration: InputDecoration(
                      labelText: 'Nueva contraseña segura *',
                      helperText:
                          'Mín. 8 caracteres: mayúscula, minúscula, número y símbolo',
                      suffixIcon: IconButton(
                        onPressed: () => setState(
                          () => _obscurePassword = !_obscurePassword,
                        ),
                        icon: Icon(
                          _obscurePassword
                              ? Icons.visibility_outlined
                              : Icons.visibility_off_outlined,
                        ),
                      ),
                    ),
                    validator: BackendValidators.passwordSegura,
                  ),
                ],
              ),
            ],
            const SizedBox(height: 20),
            FilledButton.icon(
              onPressed: _saving ? null : _save,
              icon: _saving
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.save_outlined),
              label: Text(_saving ? 'Guardando...' : 'Guardar cambios'),
            ),
            const SizedBox(height: 24),
          ],
        ),
      ),
    );
  }
}

class _EditSectionCard extends StatelessWidget {
  const _EditSectionCard({
    required this.icon,
    required this.title,
    required this.children,
    this.borderColor,
  });

  final IconData icon;
  final String title;
  final List<Widget> children;
  final Color? borderColor;

  @override
  Widget build(BuildContext context) {
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(18),
        side: BorderSide(color: borderColor ?? AppColors.border),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Row(
              children: [
                Container(
                  width: 32,
                  height: 32,
                  decoration: BoxDecoration(
                    color: AppColors.skySoft,
                    borderRadius: BorderRadius.circular(9),
                  ),
                  child: Icon(icon, size: 17, color: AppColors.secondary),
                ),
                const SizedBox(width: 10),
                Text(
                  title,
                  style: const TextStyle(
                    fontWeight: FontWeight.w800,
                    fontSize: 14,
                    color: AppColors.primary,
                  ),
                ),
              ],
            ),
            const Divider(height: 22, color: Color(0xFFF1F5F9)),
            ...children,
          ],
        ),
      ),
    );
  }
}
