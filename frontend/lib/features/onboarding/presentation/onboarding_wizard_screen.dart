import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/catalogos_sistema/data/nacionalidad_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/data/cliente_repository.dart';
import 'package:go_router/go_router.dart';

class OnboardingWizardScreen extends ConsumerStatefulWidget {
  const OnboardingWizardScreen({
    super.key,
    this.fromBackoffice = false,
  });

  final bool fromBackoffice;

  @override
  ConsumerState<OnboardingWizardScreen> createState() =>
      _OnboardingWizardScreenState();
}

class _OnboardingWizardScreenState
    extends ConsumerState<OnboardingWizardScreen> {
  final _stepKeys = List.generate(4, (_) => GlobalKey<FormState>());
  int _currentStep = 0;
  bool _submitting = false;
  Object? _error;

  // Paso 1: Datos personales
  final _nombreCtrl = TextEditingController();
  final _segundoNombreCtrl = TextEditingController();
  final _paternoCtrl = TextEditingController();
  final _maternoCtrl = TextEditingController();
  final _fechaNacCtrl = TextEditingController();
  final _curpCtrl = TextEditingController();
  final _rfcCtrl = TextEditingController();
  String _sexo = 'Masculino';
  String _estadoCivil = 'Soltero';
  NacionalidadDto? _nacionalidadSeleccionada;

  // Paso 2: Domicilio
  final _calleCtrl = TextEditingController();
  final _numExtCtrl = TextEditingController();
  final _numIntCtrl = TextEditingController();
  final _coloniaCtrl = TextEditingController();
  final _municipioCtrl = TextEditingController();
  final _estadoCtrl = TextEditingController();
  final _cpCtrl = TextEditingController();
  final _paisCtrl = TextEditingController(text: 'Mexico');

  // Paso 3: Laboral y Saldo Inicial
  final _ocupacionCtrl = TextEditingController();
  final _empresaCtrl = TextEditingController();
  final _ingresoCtrl = TextEditingController();
  final _saldoInicialCtrl = TextEditingController(text: '1000.00');

  // Paso 4: Contacto y Credenciales
  final _correoCtrl = TextEditingController();
  final _telMovilCtrl = TextEditingController();
  final _telAltCtrl = TextEditingController();
  final _passwordCtrl = TextEditingController();
  final _confirmPasswordCtrl = TextEditingController();
  bool _obscurePass = true;

  @override
  void dispose() {
    for (final c in [
      _nombreCtrl,
      _segundoNombreCtrl,
      _paternoCtrl,
      _maternoCtrl,
      _fechaNacCtrl,
      _curpCtrl,
      _rfcCtrl,
      _calleCtrl,
      _numExtCtrl,
      _numIntCtrl,
      _coloniaCtrl,
      _municipioCtrl,
      _estadoCtrl,
      _cpCtrl,
      _paisCtrl,
      _ocupacionCtrl,
      _empresaCtrl,
      _ingresoCtrl,
      _saldoInicialCtrl,
      _correoCtrl,
      _telMovilCtrl,
      _telAltCtrl,
      _passwordCtrl,
      _confirmPasswordCtrl,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _pickBirthDate() async {
    final now = DateTime.now();
    final initial = DateTime(now.year - 25, now.month, now.day);
    final picked = await showDatePicker(
      context: context,
      initialDate: initial,
      firstDate: DateTime(1920),
      lastDate: now,
    );
    if (picked != null) {
      final y = picked.year.toString().padLeft(4, '0');
      final m = picked.month.toString().padLeft(2, '0');
      final d = picked.day.toString().padLeft(2, '0');
      _fechaNacCtrl.text = '$y-$m-$d';
    }
  }

  void _nextStep() {
    if (!(_stepKeys[_currentStep].currentState?.validate() ?? false)) return;
    if (_currentStep == 0 && _nacionalidadSeleccionada == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Selecciona una nacionalidad')),
      );
      return;
    }
    if (_currentStep < 3) {
      setState(() => _currentStep++);
    } else {
      _submit();
    }
  }

  Future<void> _submit() async {
    setState(() {
      _submitting = true;
      _error = null;
    });
    try {
      final req = ClienteRegistroRequestDto(
        nombre: _nombreCtrl.text,
        segundoNombre: _segundoNombreCtrl.text,
        apellidoPaterno: _paternoCtrl.text,
        apellidoMaterno: _maternoCtrl.text,
        fechaNacimiento: _fechaNacCtrl.text,
        curp: _curpCtrl.text,
        rfc: _rfcCtrl.text,
        sexo: _sexo,
        idNacionalidad: _nacionalidadSeleccionada?.idNacionalidad,
        nacionalidad: _nacionalidadSeleccionada?.gentilicio,
        estadoCivil: _estadoCivil,
        correoElectronico: _correoCtrl.text,
        telefonoMovil: _telMovilCtrl.text,
        telefonoAlternativo: _telAltCtrl.text,
        domicilio: DomicilioDto(
          calle: _calleCtrl.text,
          numeroExterior: _numExtCtrl.text,
          numeroInterior: _numIntCtrl.text,
          colonia: _coloniaCtrl.text,
          municipio: _municipioCtrl.text,
          estado: _estadoCtrl.text,
          codigoPostal: _cpCtrl.text,
          pais: _paisCtrl.text,
        ),
        ocupacion: _ocupacionCtrl.text,
        empresa: _empresaCtrl.text,
        ingresoMensual: StrictDecimal.parse(_ingresoCtrl.text),
        saldoInicial: StrictDecimal.parse(_saldoInicialCtrl.text),
        password: _passwordCtrl.text,
      );

      final creado =
          await ref.read(clienteRepositoryProvider).registrarCliente(req);
      if (!mounted) return;
      await _showSuccessDialog(creado);
    } on ApiException catch (e) {
      setState(() => _error = e);
    } catch (e) {
      setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _showSuccessDialog(ClienteDto creado) async {
    final cuenta = creado.cuentaPrincipal;
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => AlertDialog(
        title: const Row(
          children: [
            Icon(Icons.check_circle, color: AppColors.accent),
            SizedBox(width: 8),
            Expanded(child: Text('Cliente Registrado')),
          ],
        ),
        content: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('Titular: ${creado.nombreCompleto}'),
            const SizedBox(height: 6),
            Text('Correo: ${creado.correoElectronico}'),
            if (cuenta != null) ...[
              const SizedBox(height: 12),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.background,
                  borderRadius: BorderRadius.circular(10),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Cuenta bancaria generada (16 dígitos):',
                      style: TextStyle(fontSize: 12, color: AppColors.mutedText),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      children: [
                        Expanded(
                          child: Text(
                            cuenta.numeroCuenta,
                            style: const TextStyle(
                              fontWeight: FontWeight.w800,
                              fontSize: 16,
                            ),
                          ),
                        ),
                        IconButton(
                          onPressed: () => copyToClipboard(
                            context,
                            cuenta.numeroCuenta,
                            label: 'Número de cuenta',
                          ),
                          icon: const Icon(Icons.copy, size: 18),
                        ),
                      ],
                    ),
                    if (cuenta.saldo != null)
                      Text(
                        'Saldo inicial: ${formatMxn(cuenta.saldo!.saldoDisponible)}',
                        style: const TextStyle(
                          color: AppColors.accent,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                  ],
                ),
              ),
            ],
          ],
        ),
        actions: [
          FilledButton(
            onPressed: () {
              Navigator.of(ctx).pop();
              if (widget.fromBackoffice) {
                context.pop(true);
              } else {
                context.go('/login');
              }
            },
            child: Text(
              widget.fromBackoffice
                  ? 'Volver a BackOffice'
                  : 'Ir a Iniciar Sesión',
            ),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final nacionalidadesAsync = ref.watch(nacionalidadesCatalogProvider);

    return Scaffold(
      appBar: AppBar(title: const Text('Registro de Cliente (Onboarding)')),
      body: Column(
        children: [
          if (_error != null)
            Padding(
              padding: const EdgeInsets.fromLTRB(16, 12, 16, 0),
              child: ErrorBanner(error: _error!),
            ),
          Expanded(
            child: Stepper(
              currentStep: _currentStep,
              onStepTapped: (step) {
                if (step < _currentStep) setState(() => _currentStep = step);
              },
              controlsBuilder: (context, details) => Padding(
                padding: const EdgeInsets.only(top: 16),
                child: Row(
                  children: [
                    Expanded(
                      child: FilledButton(
                        onPressed: _submitting ? null : _nextStep,
                        child: _submitting
                            ? const SizedBox(
                                height: 20,
                                width: 20,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : Text(
                                _currentStep == 3
                                    ? 'Finalizar Registro'
                                    : 'Continuar',
                              ),
                      ),
                    ),
                    if (_currentStep > 0) ...[
                      const SizedBox(width: 12),
                      Expanded(
                        child: OutlinedButton(
                          onPressed: _submitting
                              ? null
                              : () => setState(() => _currentStep--),
                          child: const Text('Atrás'),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              steps: [
                Step(
                  title: const Text('Datos Personales'),
                  isActive: _currentStep >= 0,
                  content: Form(
                    key: _stepKeys[0],
                    child: Column(
                      children: [
                        TextFormField(
                          controller: _nombreCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Primer nombre *',
                          ),
                          validator: (v) =>
                              BackendValidators.soloLetrasRequerido(v, 'nombre'),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _segundoNombreCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Segundo nombre (opcional)',
                          ),
                          validator: (v) =>
                              BackendValidators.soloLetrasOpcional(
                            v,
                            'segundo nombre',
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _paternoCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Apellido paterno *',
                          ),
                          validator: (v) =>
                              BackendValidators.soloLetrasRequerido(
                            v,
                            'apellido paterno',
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _maternoCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Apellido materno *',
                          ),
                          validator: (v) =>
                              BackendValidators.soloLetrasRequerido(
                            v,
                            'apellido materno',
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _fechaNacCtrl,
                          decoration: InputDecoration(
                            labelText: 'Fecha nacimiento (AAAA-MM-DD) *',
                            hintText: '1992-05-20',
                            suffixIcon: IconButton(
                              onPressed: _pickBirthDate,
                              icon: const Icon(Icons.calendar_today_outlined),
                            ),
                          ),
                          validator: BackendValidators.fechaNacimientoMayorEdad,
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _curpCtrl,
                          textCapitalization: TextCapitalization.characters,
                          decoration: const InputDecoration(
                            labelText: 'CURP (18 caracteres) *',
                          ),
                          validator: BackendValidators.curp,
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _rfcCtrl,
                          textCapitalization: TextCapitalization.characters,
                          decoration: const InputDecoration(
                            labelText: 'RFC con homoclave *',
                          ),
                          validator: BackendValidators.rfc,
                        ),
                        const SizedBox(height: 10),
                        DropdownButtonFormField<String>(
                          initialValue: _sexo,
                          decoration: const InputDecoration(labelText: 'Sexo *'),
                          items: const [
                            DropdownMenuItem(
                              value: 'Masculino',
                              child: Text('Masculino'),
                            ),
                            DropdownMenuItem(
                              value: 'Femenino',
                              child: Text('Femenino'),
                            ),
                          ],
                          onChanged: (v) =>
                              setState(() => _sexo = v ?? 'Masculino'),
                        ),
                        const SizedBox(height: 10),
                        DropdownButtonFormField<String>(
                          initialValue: _estadoCivil,
                          decoration: const InputDecoration(
                            labelText: 'Estado civil *',
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: 'Soltero',
                              child: Text('Soltero'),
                            ),
                            DropdownMenuItem(
                              value: 'Casado',
                              child: Text('Casado'),
                            ),
                            DropdownMenuItem(
                              value: 'Union libre',
                              child: Text('Unión libre'),
                            ),
                            DropdownMenuItem(
                              value: 'Viudo',
                              child: Text('Viudo'),
                            ),
                          ],
                          onChanged: (v) =>
                              setState(() => _estadoCivil = v ?? 'Soltero'),
                        ),
                        const SizedBox(height: 10),
                        nacionalidadesAsync.when(
                          data: (items) {
                            _nacionalidadSeleccionada ??=
                                items.isNotEmpty ? items.first : null;
                            return DropdownButtonFormField<NacionalidadDto>(
                              initialValue: _nacionalidadSeleccionada,
                              decoration: const InputDecoration(
                                labelText: 'Nacionalidad (Catálogo BD) *',
                              ),
                              items: items
                                  .map(
                                    (n) => DropdownMenuItem(
                                      value: n,
                                      child: Text(
                                        '${n.gentilicio} (${n.claveIso})',
                                      ),
                                    ),
                                  )
                                  .toList(),
                              onChanged: (v) => setState(
                                () => _nacionalidadSeleccionada = v,
                              ),
                            );
                          },
                          loading: () => const LinearProgressIndicator(),
                          error: (e, _) => ErrorBanner(
                            error: e,
                            onRetry: () =>
                                ref.invalidate(nacionalidadesCatalogProvider),
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                Step(
                  title: const Text('Domicilio'),
                  isActive: _currentStep >= 1,
                  content: Form(
                    key: _stepKeys[1],
                    child: Column(
                      children: [
                        TextFormField(
                          controller: _calleCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Calle (sin "#") *',
                          ),
                          validator: (v) => BackendValidators.calleOColonia(
                            v,
                            'La calle',
                            max: 150,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: _numExtCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Núm. Exterior *',
                                ),
                                validator: (v) =>
                                    BackendValidators.numeroDomicilio(
                                  v,
                                  'Número exterior',
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
                                validator: (v) =>
                                    BackendValidators.numeroDomicilio(
                                  v,
                                  'Número interior',
                                  requiredField: false,
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _coloniaCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Colonia (sin "#") *',
                          ),
                          validator: (v) => BackendValidators.calleOColonia(
                            v,
                            'La colonia',
                            max: 100,
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _municipioCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Municipio / Alcaldía *',
                          ),
                          validator: (v) =>
                              BackendValidators.soloLetrasRequerido(
                            v,
                            'municipio',
                            min: 2,
                            max: 100,
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _estadoCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Estado *',
                          ),
                          validator: (v) =>
                              BackendValidators.soloLetrasRequerido(
                            v,
                            'estado',
                            min: 2,
                            max: 100,
                          ),
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: _cpCtrl,
                                keyboardType: TextInputType.number,
                                maxLength: 5,
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
                                decoration: const InputDecoration(
                                  labelText: 'País *',
                                ),
                                validator: (v) =>
                                    BackendValidators.soloLetrasRequerido(
                                  v,
                                  'país',
                                  min: 2,
                                  max: 50,
                                ),
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ),
                ),
                Step(
                  title: const Text('Empleo y Saldo Inicial'),
                  isActive: _currentStep >= 2,
                  content: Form(
                    key: _stepKeys[2],
                    child: Column(
                      children: [
                        TextFormField(
                          controller: _ocupacionCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Ocupación *',
                          ),
                          validator: (v) =>
                              BackendValidators.soloLetrasRequerido(
                            v,
                            'ocupación',
                            min: 3,
                            max: 100,
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _empresaCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Empresa (sin "#") *',
                          ),
                          validator: BackendValidators.empresa,
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _ingresoCtrl,
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          decoration: const InputDecoration(
                            labelText: 'Ingreso mensual (MXN) *',
                            hintText: '45000.00',
                          ),
                          validator: (v) => BackendValidators.montoDecimal(
                            v,
                            'El ingreso mensual',
                            min: 0.01,
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _saldoInicialCtrl,
                          keyboardType: const TextInputType.numberWithOptions(
                            decimal: true,
                          ),
                          decoration: const InputDecoration(
                            labelText: 'Saldo inicial de apertura (MXN) *',
                            hintText: '2500.00',
                          ),
                          validator: (v) => BackendValidators.montoDecimal(
                            v,
                            'El saldo inicial',
                            min: 0.00,
                          ),
                        ),
                      ],
                    ),
                  ),
                ),
                Step(
                  title: const Text('Contacto y Seguridad'),
                  isActive: _currentStep >= 3,
                  content: Form(
                    key: _stepKeys[3],
                    child: Column(
                      children: [
                        TextFormField(
                          controller: _correoCtrl,
                          keyboardType: TextInputType.emailAddress,
                          decoration: const InputDecoration(
                            labelText: 'Correo electrónico *',
                          ),
                          validator: (v) => BackendValidators.email(v),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _telMovilCtrl,
                          keyboardType: TextInputType.phone,
                          maxLength: 10,
                          decoration: const InputDecoration(
                            labelText: 'Teléfono móvil (10 dígitos) *',
                            counterText: '',
                          ),
                          validator: (v) => BackendValidators.telefono(v),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _telAltCtrl,
                          keyboardType: TextInputType.phone,
                          maxLength: 10,
                          decoration: const InputDecoration(
                            labelText: 'Teléfono alternativo (opcional)',
                            counterText: '',
                          ),
                          validator: (v) => BackendValidators.telefono(
                            v,
                            requiredField: false,
                          ),
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _passwordCtrl,
                          obscureText: _obscurePass,
                          decoration: InputDecoration(
                            labelText: 'Contraseña segura *',
                            helperText:
                                'Mín. 8 chars: mayúscula, minúscula, número y símbolo',
                            suffixIcon: IconButton(
                              onPressed: () =>
                                  setState(() => _obscurePass = !_obscurePass),
                              icon: Icon(
                                _obscurePass
                                    ? Icons.visibility_outlined
                                    : Icons.visibility_off_outlined,
                              ),
                            ),
                          ),
                          validator: BackendValidators.passwordSegura,
                        ),
                        const SizedBox(height: 10),
                        TextFormField(
                          controller: _confirmPasswordCtrl,
                          obscureText: _obscurePass,
                          decoration: const InputDecoration(
                            labelText: 'Confirmar contraseña *',
                          ),
                          validator: (v) => v != _passwordCtrl.text
                              ? 'Las contraseñas no coinciden'
                              : null,
                        ),
                      ],
                    ),
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
