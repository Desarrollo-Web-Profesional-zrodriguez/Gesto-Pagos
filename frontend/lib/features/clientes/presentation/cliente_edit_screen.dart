import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/network/strict_decimal_json.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/data/cliente_repository.dart';

class ClienteEditScreen extends ConsumerStatefulWidget {
  const ClienteEditScreen({
    super.key,
    required this.cliente,
    this.fullUpdateMode = false,
  });

  final ClienteDto cliente;
  final bool fullUpdateMode;

  @override
  ConsumerState<ClienteEditScreen> createState() => _ClienteEditScreenState();
}

class _ClienteEditScreenState extends ConsumerState<ClienteEditScreen> {
  final _formKey = GlobalKey<FormState>();
  late bool _fullPutMode = widget.fullUpdateMode;
  bool _saving = false;
  Object? _error;

  late final TextEditingController _nombreCtrl;
  late final TextEditingController _segundoNombreCtrl;
  late final TextEditingController _paternoCtrl;
  late final TextEditingController _maternoCtrl;
  late final TextEditingController _correoCtrl;
  late final TextEditingController _telMovilCtrl;
  late final TextEditingController _telAltCtrl;
  late final TextEditingController _ocupacionCtrl;
  late final TextEditingController _empresaCtrl;
  late final TextEditingController _ingresoCtrl;
  late final TextEditingController _calleCtrl;
  late final TextEditingController _numExtCtrl;
  late final TextEditingController _numIntCtrl;
  late final TextEditingController _coloniaCtrl;
  late final TextEditingController _municipioCtrl;
  late final TextEditingController _estadoCtrl;
  late final TextEditingController _cpCtrl;
  late final TextEditingController _paisCtrl;
  final _passwordPutCtrl = TextEditingController();

  @override
  void initState() {
    super.initState();
    final c = widget.cliente;
    final dom = c.domicilios.isNotEmpty ? c.domicilios.first : null;
    _nombreCtrl = TextEditingController(text: c.nombre);
    _segundoNombreCtrl = TextEditingController(text: c.segundoNombre ?? '');
    _paternoCtrl = TextEditingController(text: c.apellidoPaterno);
    _maternoCtrl = TextEditingController(text: c.apellidoMaterno);
    _correoCtrl = TextEditingController(text: c.correoElectronico);
    _telMovilCtrl = TextEditingController(text: c.telefonoMovil);
    _telAltCtrl = TextEditingController(text: c.telefonoAlternativo ?? '');
    _ocupacionCtrl = TextEditingController(text: c.ocupacion);
    _empresaCtrl = TextEditingController(text: c.empresa);
    _ingresoCtrl = TextEditingController(
      text: StrictDecimal.fromDouble(c.ingresoMensual).toStrictLiteral(),
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
      _correoCtrl,
      _telMovilCtrl,
      _telAltCtrl,
      _ocupacionCtrl,
      _empresaCtrl,
      _ingresoCtrl,
      _calleCtrl,
      _numExtCtrl,
      _numIntCtrl,
      _coloniaCtrl,
      _municipioCtrl,
      _estadoCtrl,
      _cpCtrl,
      _paisCtrl,
      _passwordPutCtrl,
    ]) {
      c.dispose();
    }
    super.dispose();
  }

  Future<void> _save() async {
    if (!(_formKey.currentState?.validate() ?? false)) return;
    setState(() {
      _saving = true;
      _error = null;
    });

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

      final repo = ref.read(clienteRepositoryProvider);
      final ClienteDto updated;
      if (_fullPutMode) {
        final saldoActual =
            widget.cliente.cuentaPrincipal?.saldo?.saldoDisponible ?? 0.0;
        updated = await repo.actualizarCompleto(
          ClienteRegistroRequestDto(
            identificador: widget.cliente.curp,
            nombre: _nombreCtrl.text,
            segundoNombre: _segundoNombreCtrl.text,
            apellidoPaterno: _paternoCtrl.text,
            apellidoMaterno: _maternoCtrl.text,
            fechaNacimiento: widget.cliente.fechaNacimiento,
            curp: widget.cliente.curp,
            rfc: widget.cliente.rfc,
            sexo: widget.cliente.sexo,
            idNacionalidad: widget.cliente.idNacionalidad,
            nacionalidad: widget.cliente.nacionalidad,
            estadoCivil: widget.cliente.estadoCivil,
            correoElectronico: _correoCtrl.text,
            telefonoMovil: _telMovilCtrl.text,
            telefonoAlternativo: _telAltCtrl.text,
            domicilio: dom,
            ocupacion: _ocupacionCtrl.text,
            empresa: _empresaCtrl.text,
            ingresoMensual: StrictDecimal.parse(_ingresoCtrl.text),
            saldoInicial: StrictDecimal.fromDouble(saldoActual),
            password: _passwordPutCtrl.text,
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
      Navigator.of(context).pop(updated);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _saving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Actualizar Datos del Cliente')),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            SwitchListTile.adaptive(
              value: _fullPutMode,
              onChanged: (v) => setState(() => _fullPutMode = v),
              title: const Text('Modo actualización completa (PUT /clientes)'),
              subtitle: Text(
                _fullPutMode
                    ? 'Envía el contrato completo de ClienteRegistroRequest'
                    : 'Usa actualización parcial (PATCH /clientes)',
                style: const TextStyle(fontSize: 12),
              ),
            ),
            const SizedBox(height: 8),
            Card(
              child: Padding(
                padding: const EdgeInsets.all(12),
                child: Text(
                  'CURP: ${widget.cliente.curp} • RFC: ${widget.cliente.rfc} (protegidos contra edición)',
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppColors.mutedText,
                  ),
                ),
              ),
            ),
            const SizedBox(height: 12),
            if (_error != null) ...[
              ErrorBanner(error: _error!),
              const SizedBox(height: 12),
            ],
            TextFormField(
              controller: _nombreCtrl,
              decoration: const InputDecoration(labelText: 'Primer nombre *'),
              validator: (v) =>
                  BackendValidators.soloLetrasRequerido(v, 'nombre'),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _segundoNombreCtrl,
              decoration: const InputDecoration(labelText: 'Segundo nombre'),
              validator: (v) =>
                  BackendValidators.soloLetrasOpcional(v, 'segundo nombre'),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _paternoCtrl,
              decoration: const InputDecoration(labelText: 'Apellido paterno *'),
              validator: (v) =>
                  BackendValidators.soloLetrasRequerido(v, 'apellido paterno'),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _maternoCtrl,
              decoration: const InputDecoration(labelText: 'Apellido materno *'),
              validator: (v) =>
                  BackendValidators.soloLetrasRequerido(v, 'apellido materno'),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _correoCtrl,
              decoration: const InputDecoration(labelText: 'Correo electrónico *'),
              validator: (v) => BackendValidators.email(v),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _telMovilCtrl,
              maxLength: 10,
              decoration: const InputDecoration(
                labelText: 'Teléfono móvil *',
                counterText: '',
              ),
              validator: (v) => BackendValidators.telefono(v),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _telAltCtrl,
              maxLength: 10,
              decoration: const InputDecoration(
                labelText: 'Teléfono alternativo',
                counterText: '',
              ),
              validator: (v) =>
                  BackendValidators.telefono(v, requiredField: false),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _ocupacionCtrl,
              decoration: const InputDecoration(labelText: 'Ocupación *'),
              validator: (v) => BackendValidators.soloLetrasRequerido(
                v,
                'ocupación',
                min: 3,
                max: 100,
              ),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _empresaCtrl,
              decoration: const InputDecoration(labelText: 'Empresa *'),
              validator: BackendValidators.empresa,
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _ingresoCtrl,
              keyboardType: const TextInputType.numberWithOptions(decimal: true),
              decoration: const InputDecoration(labelText: 'Ingreso mensual *'),
              validator: (v) => BackendValidators.montoDecimal(
                v,
                'El ingreso mensual',
                min: 0.01,
              ),
            ),
            const SizedBox(height: 14),
            const Text(
              'Domicilio',
              style: TextStyle(fontWeight: FontWeight.w700),
            ),
            const SizedBox(height: 8),
            TextFormField(
              controller: _calleCtrl,
              decoration: const InputDecoration(labelText: 'Calle *'),
              validator: (v) =>
                  BackendValidators.calleOColonia(v, 'La calle', max: 150),
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                Expanded(
                  child: TextFormField(
                    controller: _numExtCtrl,
                    decoration: const InputDecoration(labelText: 'Exterior *'),
                    validator: (v) => BackendValidators.numeroDomicilio(
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
                    decoration: const InputDecoration(labelText: 'Interior'),
                    validator: (v) => BackendValidators.numeroDomicilio(
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
              decoration: const InputDecoration(labelText: 'Colonia *'),
              validator: (v) =>
                  BackendValidators.calleOColonia(v, 'La colonia', max: 100),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _municipioCtrl,
              decoration: const InputDecoration(labelText: 'Municipio *'),
              validator: (v) => BackendValidators.soloLetrasRequerido(
                v,
                'municipio',
                min: 2,
                max: 100,
              ),
            ),
            const SizedBox(height: 10),
            TextFormField(
              controller: _estadoCtrl,
              decoration: const InputDecoration(labelText: 'Estado *'),
              validator: (v) => BackendValidators.soloLetrasRequerido(
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
                    maxLength: 5,
                    decoration: const InputDecoration(
                      labelText: 'CP *',
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
                      'país',
                      min: 2,
                      max: 50,
                    ),
                  ),
                ),
              ],
            ),
            if (_fullPutMode) ...[
              const SizedBox(height: 12),
              TextFormField(
                controller: _passwordPutCtrl,
                obscureText: true,
                decoration: const InputDecoration(
                  labelText: 'Contraseña requerida por validación PUT *',
                ),
                validator: BackendValidators.passwordSegura,
              ),
            ],
            const SizedBox(height: 20),
            FilledButton(
              onPressed: _saving ? null : _save,
              child: _saving
                  ? const SizedBox(
                      height: 20,
                      width: 20,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : Text(_fullPutMode ? 'Guardar (PUT)' : 'Guardar (PATCH)'),
            ),
          ],
        ),
      ),
    );
  }
}
