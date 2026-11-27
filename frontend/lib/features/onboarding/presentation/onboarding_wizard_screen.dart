import 'dart:async';

import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/network/api_error.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/core/widgets/modern_selectors.dart';
import 'package:gestopago_app/core/widgets/otp_code_input.dart';
import 'package:gestopago_app/features/auth/presentation/session_controller.dart';
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
  bool _sendingOtp = false;
  bool _submitting = false;
  bool _autoLoggingIn = false;
  Object? _error;
  String? _validationSummary;
  String? _otpFieldError;

  // Paso 0: Identidad
  final _nombreCtrl = TextEditingController();
  final _segundoNombreCtrl = TextEditingController();
  final _paternoCtrl = TextEditingController();
  final _maternoCtrl = TextEditingController();
  String _fechaNacimiento = '1995-05-20';
  final _curpCtrl = TextEditingController();
  final _rfcCtrl = TextEditingController();
  String _sexo = 'Masculino';
  String _estadoCivil = 'Soltero';
  NacionalidadDto? _nacionalidadSeleccionada;

  // Paso 1: Contacto y Domicilio
  final _correoCtrl = TextEditingController();
  final _telMovilCtrl = TextEditingController();
  final _telAltCtrl = TextEditingController();
  final _calleCtrl = TextEditingController();
  final _numExtCtrl = TextEditingController();
  final _numIntCtrl = TextEditingController();
  final _coloniaCtrl = TextEditingController();
  final _municipioCtrl = TextEditingController();
  final _estadoCtrl = TextEditingController();
  final _cpCtrl = TextEditingController();
  final _paisCtrl = TextEditingController(text: 'Mexico');

  // Paso 2: Empleo y Saldo Inicial
  final _ocupacionCtrl = TextEditingController();
  final _empresaCtrl = TextEditingController();
  final _ingresoCtrl = TextEditingController(text: '25000.00');
  final _saldoInicialCtrl = TextEditingController(text: '1000.00');

  // Paso 3: Seguridad (Contraseña)
  final _passwordCtrl = TextEditingController();
  final _confirmPasswordCtrl = TextEditingController();
  bool _obscurePass = true;
  bool _obscureConfirm = true;

  // Paso 4: Verificación OTP por correo
  String _codigoOtp = '';
  String? _otpInfoMsg;
  int _otpSecondsLeft = 0;
  Timer? _otpTimer;
  ClienteDto? _creado;

  static const _stepTitles = [
    'Identidad',
    'Domicilio',
    'Apertura',
    'Contraseña',
    'Verificación',
  ];

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

  @override
  void dispose() {
    _otpTimer?.cancel();
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
      _passwordCtrl,
      _confirmPasswordCtrl,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  void _startOtpTimer(int seconds) {
    _otpTimer?.cancel();
    setState(() => _otpSecondsLeft = seconds > 0 ? seconds : 300);
    _otpTimer = Timer.periodic(const Duration(seconds: 1), (_) {
      if (!mounted) return;
      if (_otpSecondsLeft <= 1) {
        _otpTimer?.cancel();
        setState(() => _otpSecondsLeft = 0);
      } else {
        setState(() => _otpSecondsLeft--);
      }
    });
  }

  Future<void> _enviarCodigoOtp({bool esReenvio = false}) async {
    setState(() {
      _sendingOtp = true;
      _error = null;
      _validationSummary = null;
      _otpFieldError = null;
    });

    try {
      final res = await ref.read(clienteRepositoryProvider).solicitarOtpOnboarding(
            correoElectronico: _correoCtrl.text,
            nombre: _nombreCtrl.text,
            curp: _curpCtrl.text,
            rfc: _rfcCtrl.text,
          );
      if (!mounted) return;
      setState(() {
        _codigoOtp = '';
        _otpInfoMsg = res.mensaje;
        _currentStep = 4;
      });
      _startOtpTimer(res.expiraEnSegundos);
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          behavior: SnackBarBehavior.floating,
          content: Text(
            esReenvio
                ? 'Enviamos un nuevo código a ${_correoCtrl.text.trim().toLowerCase()}.'
                : 'Código de verificación enviado a tu correo.',
          ),
        ),
      );
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _sendingOtp = false);
    }
  }

  void _handleNext() {
    setState(() {
      _error = null;
      _validationSummary = null;
    });

    if (_currentStep < 4) {
      final valid = _stepKeys[_currentStep].currentState?.validate() ?? false;
      if (!valid) {
        setState(() {
          _validationSummary =
              'Revisa los campos marcados en rojo antes de continuar.';
        });
        return;
      }
      if (_currentStep == 0 && _nacionalidadSeleccionada == null) {
        setState(() {
          _validationSummary = 'Selecciona tu nacionalidad para continuar.';
        });
        return;
      }
      if (_currentStep == 3) {
        _enviarCodigoOtp();
      } else {
        setState(() => _currentStep++);
      }
    } else {
      _submitVerified();
    }
  }

  Future<void> _submitVerified() async {
    final otpErr = BackendValidators.codigoOtp(_codigoOtp);
    if (otpErr != null) {
      setState(() {
        _otpFieldError = otpErr;
        _validationSummary = otpErr;
      });
      return;
    }

    setState(() {
      _submitting = true;
      _error = null;
      _validationSummary = null;
      _otpFieldError = null;
    });

    try {
      final req = ClienteRegistroRequestDto(
        nombre: _nombreCtrl.text,
        segundoNombre: _segundoNombreCtrl.text,
        apellidoPaterno: _paternoCtrl.text,
        apellidoMaterno: _maternoCtrl.text,
        fechaNacimiento: _fechaNacimiento,
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

      final creado = await ref.read(clienteRepositoryProvider).registrarConOtp(
            codigoOtp: _codigoOtp,
            registro: req,
          );
      _otpTimer?.cancel();
      if (!mounted) return;
      setState(() => _creado = creado);
    } on ApiException catch (e) {
      if (mounted) setState(() => _error = e);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _submitting = false);
    }
  }

  Future<void> _autoLogin() async {
    setState(() {
      _autoLoggingIn = true;
      _error = null;
    });
    try {
      await ref.read(sessionControllerProvider.notifier).loginWithPassword(
            username: _correoCtrl.text,
            password: _passwordCtrl.text,
          );
      if (!mounted) return;
      context.go('/cliente');
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _autoLoggingIn = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final nacionalidadesAsync = ref.watch(nacionalidadesCatalogProvider);
    final busy = _sendingOtp || _submitting || _autoLoggingIn;

    if (_creado != null) {
      return _buildSuccessView(_creado!);
    }

    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        title: const Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              'Abrir Cuenta Digital',
              style: TextStyle(fontSize: 16.5, fontWeight: FontWeight.w800),
            ),
            Text(
              'Proceso guiado con verificación de correo',
              style: TextStyle(fontSize: 11.5, color: Color(0xFF94A3B8)),
            ),
          ],
        ),
      ),
      body: Column(
        children: [
          _StepHeader(
            titles: _stepTitles,
            currentStep: _currentStep,
            busy: busy,
            onStepTap: (idx) {
              if (idx < _currentStep && !busy) {
                setState(() {
                  _currentStep = idx;
                  _error = null;
                  _validationSummary = null;
                });
              }
            },
          ),
          Expanded(
            child: SingleChildScrollView(
              padding: const EdgeInsets.all(16),
              child: ConstrainedBox(
                constraints: const BoxConstraints(maxWidth: 640),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
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
                        onDismiss: () =>
                            setState(() => _validationSummary = null),
                      ),
                      const SizedBox(height: 12),
                    ],
                    if (_currentStep == 0)
                      _buildStep0Identidad(nacionalidadesAsync),
                    if (_currentStep == 1) _buildStep1Domicilio(),
                    if (_currentStep == 2) _buildStep2Apertura(),
                    if (_currentStep == 3) _buildStep3Password(),
                    if (_currentStep == 4) _buildStep4Otp(),
                    const SizedBox(height: 24),
                  ],
                ),
              ),
            ),
          ),
        ],
      ),
      bottomNavigationBar: _buildBottomBar(busy),
    );
  }

  Widget _buildBottomBar(bool busy) {
    final String primaryLabel;
    final IconData primaryIcon;
    if (_currentStep < 3) {
      primaryLabel = 'Siguiente';
      primaryIcon = Icons.arrow_forward_rounded;
    } else if (_currentStep == 3) {
      primaryLabel = _sendingOtp ? 'Enviando...' : 'Enviar código al correo';
      primaryIcon = Icons.mark_email_read_outlined;
    } else {
      primaryLabel = _submitting ? 'Verificando...' : 'Confirmar y abrir cuenta';
      primaryIcon = Icons.verified_user_outlined;
    }

    return Container(
      padding: EdgeInsets.fromLTRB(
        16,
        12,
        16,
        12 + MediaQuery.of(context).padding.bottom,
      ),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(top: BorderSide(color: AppColors.border)),
      ),
      child: Row(
        children: [
          if (_currentStep > 0) ...[
            Expanded(
              child: OutlinedButton.icon(
                onPressed: busy
                    ? null
                    : () => setState(() {
                          _currentStep--;
                          _error = null;
                          _validationSummary = null;
                        }),
                icon: const Icon(Icons.arrow_back_rounded, size: 18),
                label: const Text('Anterior'),
              ),
            ),
            const SizedBox(width: 12),
          ],
          Expanded(
            flex: 2,
            child: FilledButton(
              style: FilledButton.styleFrom(
                backgroundColor: _currentStep == 4
                    ? AppColors.accent
                    : AppColors.primary,
              ),
              onPressed: busy ? null : _handleNext,
              child: (_sendingOtp || _submitting)
                  ? const SizedBox(
                      width: 20,
                      height: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Row(
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        Text(primaryLabel),
                        const SizedBox(width: 8),
                        Icon(primaryIcon, size: 18),
                      ],
                    ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStep0Identidad(
    AsyncValue<List<NacionalidadDto>> nacionalidadesAsync,
  ) {
    return Form(
      key: _stepKeys[0],
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Datos de Identidad',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _nombreCtrl,
                textCapitalization: TextCapitalization.words,
                decoration: const InputDecoration(labelText: 'Primer nombre *'),
                validator: (v) =>
                    BackendValidators.soloLetrasRequerido(v, 'El nombre'),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _segundoNombreCtrl,
                textCapitalization: TextCapitalization.words,
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
                textCapitalization: TextCapitalization.words,
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
                textCapitalization: TextCapitalization.words,
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
                label: 'Fecha de nacimiento (mayor de 18 años) *',
                value: _fechaNacimiento,
                onChanged: (next) => setState(() => _fechaNacimiento = next),
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _curpCtrl,
                maxLength: 18,
                textCapitalization: TextCapitalization.characters,
                decoration: const InputDecoration(
                  labelText: 'CURP (18 caracteres) *',
                  counterText: '',
                  hintText: 'PELJ950520HDFRRN09',
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
                  hintText: 'PELJ9505201A0',
                ),
                validator: BackendValidators.rfc,
              ),
              const SizedBox(height: 12),
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
                    final mex = items.where(
                      (n) => n.claveIso.toUpperCase() == 'MEX',
                    );
                    _nacionalidadSeleccionada =
                        mex.isNotEmpty ? mex.first : items.first;
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
                    label: 'Nacionalidad *',
                    sheetTitle: 'Selecciona tu nacionalidad',
                    leadingIcon: Icons.public_outlined,
                    searchable: true,
                    searchHint: 'Buscar país, nacionalidad o código...',
                    value: _nacionalidadSeleccionada,
                    options: options,
                    onChanged: (v) =>
                        setState(() => _nacionalidadSeleccionada = v),
                    validator: (v) =>
                        v == null ? 'Selecciona tu nacionalidad.' : null,
                  );
                },
                loading: () => const LinearProgressIndicator(),
                error: (e, _) => ErrorBanner(
                  error: e,
                  onRetry: () => ref.invalidate(nacionalidadesCatalogProvider),
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStep1Domicilio() {
    return Form(
      key: _stepKeys[1],
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Contacto y Domicilio',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _correoCtrl,
                keyboardType: TextInputType.emailAddress,
                decoration: const InputDecoration(
                  labelText: 'Correo electrónico (recibirás código OTP) *',
                  prefixIcon: Icon(Icons.email_outlined),
                  hintText: 'nombre@correo.com',
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
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                      decoration: const InputDecoration(
                        labelText: 'Teléfono móvil *',
                        counterText: '',
                        hintText: '5512345678',
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
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
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
              const Divider(height: 28),
              TextFormField(
                controller: _calleCtrl,
                decoration: const InputDecoration(
                  labelText: 'Calle (sin carácter "#") *',
                  hintText: 'Av. Insurgentes Sur',
                ),
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
                decoration: const InputDecoration(
                  labelText: 'Colonia (sin "#") *',
                ),
                validator: (v) =>
                    BackendValidators.calleOColonia(v, 'La colonia', max: 100),
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
                      inputFormatters: [FilteringTextInputFormatter.digitsOnly],
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
        ),
      ),
    );
  }

  Widget _buildStep2Apertura() {
    return Form(
      key: _stepKeys[2],
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Perfil Laboral y Apertura',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _ocupacionCtrl,
                decoration: const InputDecoration(
                  labelText: 'Ocupación / Profesión *',
                ),
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
                decoration: const InputDecoration(
                  labelText: 'Empresa o centro de trabajo (sin "#") *',
                ),
                validator: BackendValidators.empresa,
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _ingresoCtrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Ingreso mensual estimado *',
                  prefixText: r'$ ',
                  suffixText: 'MXN',
                ),
                validator: (v) => BackendValidators.montoDecimal(
                  v,
                  'El ingreso mensual',
                  min: 0.01,
                ),
              ),
              const SizedBox(height: 12),
              TextFormField(
                controller: _saldoInicialCtrl,
                keyboardType: const TextInputType.numberWithOptions(decimal: true),
                decoration: const InputDecoration(
                  labelText: 'Saldo inicial de apertura *',
                  prefixText: r'$ ',
                  suffixText: 'MXN',
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
    );
  }

  Widget _buildStep3Password() {
    final pass = _passwordCtrl.text;
    final rules = [
      ('Entre 8 y 64 caracteres', pass.length >= 8 && pass.length <= 64),
      ('Al menos una mayúscula (A-Z)', RegExp(r'[A-Z]').hasMatch(pass)),
      ('Al menos una minúscula (a-z)', RegExp(r'[a-z]').hasMatch(pass)),
      ('Al menos un número (0-9)', RegExp(r'\d').hasMatch(pass)),
      ('Un carácter especial (!@#\$%*)', RegExp(r'[^a-zA-Z0-9\s]').hasMatch(pass)),
    ];
    final passedCount = rules.where((r) => r.$2).length;

    return Form(
      key: _stepKeys[3],
      child: Card(
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(
                'Seguridad de tu Cuenta',
                style: TextStyle(
                  fontSize: 16,
                  fontWeight: FontWeight.w800,
                  color: AppColors.primary,
                ),
              ),
              const SizedBox(height: 14),
              TextFormField(
                controller: _passwordCtrl,
                obscureText: _obscurePass,
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(
                  labelText: 'Contraseña segura *',
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
              const SizedBox(height: 12),
              TextFormField(
                controller: _confirmPasswordCtrl,
                obscureText: _obscureConfirm,
                onChanged: (_) => setState(() {}),
                decoration: InputDecoration(
                  labelText: 'Confirmar contraseña *',
                  suffixIcon: IconButton(
                    onPressed: () =>
                        setState(() => _obscureConfirm = !_obscureConfirm),
                    icon: Icon(
                      _obscureConfirm
                          ? Icons.visibility_outlined
                          : Icons.visibility_off_outlined,
                    ),
                  ),
                ),
                validator: (v) => BackendValidators.confirmacionPassword(
                  _passwordCtrl.text,
                  v,
                ),
              ),
              const SizedBox(height: 16),
              Container(
                padding: const EdgeInsets.all(14),
                decoration: BoxDecoration(
                  color: AppColors.background,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(color: AppColors.border),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Text(
                          'FORTALEZA DE CONTRASEÑA',
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.w700,
                            color: AppColors.mutedText,
                          ),
                        ),
                        Text(
                          '$passedCount/${rules.length}',
                          style: const TextStyle(
                            fontFamily: 'monospace',
                            fontWeight: FontWeight.w700,
                            fontSize: 12,
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 8),
                    ClipRRect(
                      borderRadius: BorderRadius.circular(4),
                      child: LinearProgressIndicator(
                        value: passedCount / rules.length,
                        minHeight: 6,
                        backgroundColor: AppColors.border,
                        color: passedCount == rules.length
                            ? AppColors.accent
                            : passedCount >= 3
                                ? AppColors.secondary
                                : AppColors.warning,
                      ),
                    ),
                    const SizedBox(height: 10),
                    ...rules.map(
                      (r) => Padding(
                        padding: const EdgeInsets.only(bottom: 4),
                        child: Row(
                          children: [
                            Icon(
                              r.$2
                                  ? Icons.check_circle
                                  : Icons.radio_button_unchecked,
                              size: 15,
                              color: r.$2
                                  ? AppColors.accent
                                  : const Color(0xFFCBD5E1),
                            ),
                            const SizedBox(width: 8),
                            Text(
                              r.$1,
                              style: TextStyle(
                                fontSize: 12,
                                fontWeight: FontWeight.w500,
                                color: r.$2
                                    ? const Color(0xFF047857)
                                    : AppColors.mutedText,
                              ),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 14),
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: AppColors.skySoft,
                  borderRadius: BorderRadius.circular(14),
                  border: Border.all(
                    color: AppColors.secondary.withValues(alpha: 0.25),
                  ),
                ),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Icon(
                      Icons.mark_email_read_outlined,
                      size: 18,
                      color: AppColors.secondary,
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Text(
                        'En el siguiente paso enviaremos un código de seguridad de 6 dígitos a ${_correoCtrl.text.trim().isNotEmpty ? _correoCtrl.text.trim().toLowerCase() : 'tu correo'} para confirmar tu identidad antes de abrir la cuenta.',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.sky950,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStep4Otp() {
    final correoNorm = _correoCtrl.text.trim().toLowerCase();
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppColors.skySoft,
                borderRadius: BorderRadius.circular(14),
                border: Border.all(
                  color: AppColors.secondary.withValues(alpha: 0.25),
                ),
              ),
              child: Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    width: 36,
                    height: 36,
                    decoration: BoxDecoration(
                      color: Colors.white,
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(
                      Icons.mark_email_read_outlined,
                      color: AppColors.secondary,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Verifica tu correo electrónico',
                          style: TextStyle(
                            fontWeight: FontWeight.w800,
                            fontSize: 13.5,
                            color: AppColors.primary,
                          ),
                        ),
                        const SizedBox(height: 3),
                        Text(
                          '${_otpInfoMsg ?? 'Enviamos un código de 6 dígitos a:'}\n$correoNorm',
                          style: const TextStyle(
                            fontSize: 12,
                            color: AppColors.sky950,
                          ),
                        ),
                        const SizedBox(height: 4),
                        InkWell(
                          onTap: _submitting
                              ? null
                              : () => setState(() {
                                    _currentStep = 1;
                                    _error = null;
                                    _validationSummary = null;
                                  }),
                          child: const Text(
                            '¿Correo incorrecto? Editar dirección',
                            style: TextStyle(
                              fontSize: 11.5,
                              fontWeight: FontWeight.w700,
                              color: AppColors.secondary,
                              decoration: TextDecoration.underline,
                            ),
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 18),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                const Text(
                  'Código de verificación (6 dígitos) *',
                  style: TextStyle(
                    fontSize: 12.5,
                    fontWeight: FontWeight.w700,
                    color: AppColors.primary,
                  ),
                ),
                if (_otpSecondsLeft > 0)
                  Row(
                    children: [
                      const Icon(
                        Icons.schedule,
                        size: 14,
                        color: AppColors.secondary,
                      ),
                      const SizedBox(width: 4),
                      Text(
                        formatCountdown(_otpSecondsLeft),
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontWeight: FontWeight.w700,
                          fontSize: 12.5,
                        ),
                      ),
                    ],
                  )
                else
                  const Text(
                    'Código expirado',
                    style: TextStyle(
                      fontSize: 12,
                      fontWeight: FontWeight.w700,
                      color: Color(0xFFD97706),
                    ),
                  ),
              ],
            ),
            const SizedBox(height: 10),
            OtpCodeInput(
              value: _codigoOtp,
              autoFocus: true,
              enabled: !_submitting,
              hasError: _otpFieldError != null,
              onChange: (val) {
                setState(() {
                  _codigoOtp = val;
                  if (_otpFieldError != null) _otpFieldError = null;
                });
              },
            ),
            if (_otpFieldError != null) ...[
              const SizedBox(height: 6),
              Text(
                _otpFieldError!,
                style: const TextStyle(
                  fontSize: 12,
                  color: AppColors.danger,
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
            const SizedBox(height: 16),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppColors.background,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppColors.border),
              ),
              child: Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  const Text(
                    '¿No recibiste el correo?',
                    style: TextStyle(fontSize: 12, color: AppColors.mutedText),
                  ),
                  TextButton.icon(
                    onPressed:
                        (_sendingOtp || _submitting)
                            ? null
                            : () => _enviarCodigoOtp(esReenvio: true),
                    icon: _sendingOtp
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.refresh, size: 16),
                    label: Text(_sendingOtp ? 'Enviando...' : 'Reenviar código'),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSuccessView(ClienteDto creado) {
    final cuenta = creado.cuentaPrincipal;
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        automaticallyImplyLeading: false,
        title: const Text('Apertura de Cuenta Exitosa'),
      ),
      body: Center(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(20),
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 460),
            child: Card(
              child: Padding(
                padding: const EdgeInsets.all(24),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    Center(
                      child: Container(
                        width: 64,
                        height: 64,
                        decoration: BoxDecoration(
                          color: AppColors.emeraldSoft,
                          borderRadius: BorderRadius.circular(20),
                        ),
                        child: const Icon(
                          Icons.check_circle_outline,
                          color: AppColors.accent,
                          size: 36,
                        ),
                      ),
                    ),
                    const SizedBox(height: 16),
                    Text(
                      '¡Bienvenido, ${creado.nombreCompleto.isNotEmpty ? creado.nombreCompleto : creado.nombre}!',
                      textAlign: TextAlign.center,
                      style: const TextStyle(
                        fontSize: 20,
                        fontWeight: FontWeight.w800,
                        color: AppColors.primary,
                      ),
                    ),
                    const SizedBox(height: 6),
                    const Text(
                      'Tu correo electrónico fue verificado y tu cuenta digital a 16 dígitos ya está activa.',
                      textAlign: TextAlign.center,
                      style: TextStyle(fontSize: 13, color: AppColors.mutedText),
                    ),
                    const SizedBox(height: 18),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppColors.background,
                        borderRadius: BorderRadius.circular(14),
                        border: Border.all(color: AppColors.border),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.stretch,
                        children: [
                          Row(
                            children: [
                              const Expanded(
                                flex: 2,
                                child: Text(
                                  'Correo verificado',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: AppColors.mutedText,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Expanded(
                                flex: 3,
                                child: Text(
                                  creado.correoElectronico,
                                  textAlign: TextAlign.right,
                                  style: const TextStyle(
                                    fontWeight: FontWeight.w700,
                                    fontSize: 12.5,
                                  ),
                                ),
                              ),
                            ],
                          ),
                          if (cuenta != null) ...[
                            const Divider(height: 18),
                            Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'Cuenta 16 dígitos',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: AppColors.mutedText,
                                  ),
                                ),
                                const SizedBox(height: 6),
                                CopyChip(
                                  value: cuenta.numeroCuenta,
                                  displayLabel:
                                      formatCuentaGrupos(cuenta.numeroCuenta),
                                  copyLabel: 'Número de cuenta',
                                ),
                              ],
                            ),
                            if (cuenta.saldo != null) ...[
                              const Divider(height: 18),
                              Row(
                                children: [
                                  const Expanded(
                                    child: Text(
                                      'Saldo disponible inicial',
                                      style: TextStyle(
                                        fontSize: 12,
                                        color: AppColors.mutedText,
                                      ),
                                    ),
                                  ),
                                  const SizedBox(width: 8),
                                  Text(
                                    formatMxn(cuenta.saldo!.saldoDisponible),
                                    style: const TextStyle(
                                      fontWeight: FontWeight.w800,
                                      color: Color(0xFF047857),
                                    ),
                                  ),
                                ],
                              ),
                            ],
                          ],
                        ],
                      ),
                    ),
                    if (_error != null) ...[
                      const SizedBox(height: 14),
                      ErrorBanner(error: _error!),
                    ],
                    const SizedBox(height: 20),
                    if (widget.fromBackoffice)
                      FilledButton(
                        onPressed: () => Navigator.of(context).pop(true),
                        child: const Text('Volver a Consola'),
                      )
                    else ...[
                      FilledButton.icon(
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.accent,
                        ),
                        onPressed: _autoLoggingIn ? null : _autoLogin,
                        icon: _autoLoggingIn
                            ? const SizedBox(
                                width: 18,
                                height: 18,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2,
                                  color: Colors.white,
                                ),
                              )
                            : const Icon(Icons.login_rounded),
                        label: const Text('Entrar a mi cuenta ahora'),
                      ),
                      const SizedBox(height: 10),
                      OutlinedButton(
                        onPressed:
                            _autoLoggingIn ? null : () => context.go('/login'),
                        child: const Text('Ir a Iniciar Sesión'),
                      ),
                    ],
                  ],
                ),
              ),
            ),
          ),
        ),
      ),
    );
  }
}

class _StepHeader extends StatelessWidget {
  const _StepHeader({
    required this.titles,
    required this.currentStep,
    required this.busy,
    required this.onStepTap,
  });

  final List<String> titles;
  final int currentStep;
  final bool busy;
  final ValueChanged<int> onStepTap;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: const BoxDecoration(
        color: Colors.white,
        border: Border(bottom: BorderSide(color: AppColors.border)),
      ),
      child: Row(
        children: List.generate(titles.length, (idx) {
          final done = idx < currentStep;
          final active = idx == currentStep;
          return Expanded(
            child: InkWell(
              onTap: (done && !busy) ? () => onStepTap(idx) : null,
              borderRadius: BorderRadius.circular(10),
              child: Container(
                margin: EdgeInsets.only(right: idx < titles.length - 1 ? 6 : 0),
                padding: const EdgeInsets.symmetric(horizontal: 4, vertical: 6),
                decoration: BoxDecoration(
                  color: active
                      ? AppColors.skySoft
                      : done
                          ? AppColors.emeraldSoft
                          : AppColors.background,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(
                    color: active
                        ? AppColors.secondary
                        : done
                            ? AppColors.accent.withValues(alpha: 0.4)
                            : AppColors.border,
                  ),
                ),
                child: Column(
                  children: [
                    CircleAvatar(
                      radius: 10,
                      backgroundColor: active
                          ? AppColors.secondary
                          : done
                              ? AppColors.accent
                              : const Color(0xFFCBD5E1),
                      child: done
                          ? const Icon(Icons.check, size: 12, color: Colors.white)
                          : Text(
                              '${idx + 1}',
                              style: const TextStyle(
                                fontSize: 10.5,
                                fontWeight: FontWeight.w800,
                                color: Colors.white,
                              ),
                            ),
                    ),
                    const SizedBox(height: 3),
                    Text(
                      titles[idx],
                      maxLines: 1,
                      overflow: TextOverflow.ellipsis,
                      style: TextStyle(
                        fontSize: 10,
                        fontWeight: FontWeight.w700,
                        color: active
                            ? AppColors.secondary
                            : done
                                ? const Color(0xFF047857)
                                : AppColors.mutedText,
                      ),
                    ),
                  ],
                ),
              ),
            ),
          );
        }),
      ),
    );
  }
}
