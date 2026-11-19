import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/features/catalogos_sistema/data/nacionalidad_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/data/cliente_repository.dart';
import 'package:gestopago_app/features/clientes/presentation/cliente_edit_screen.dart';
import 'package:gestopago_app/features/cuentas/data/cuenta_repository.dart';
import 'package:gestopago_app/features/onboarding/presentation/onboarding_wizard_screen.dart';
import 'package:gestopago_app/features/pagos/presentation/historial_tab.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_resultado_views.dart';

class BackofficeScreen extends ConsumerStatefulWidget {
  const BackofficeScreen({super.key});

  @override
  ConsumerState<BackofficeScreen> createState() => _BackofficeScreenState();
}

class _BackofficeScreenState extends ConsumerState<BackofficeScreen>
    with SingleTickerProviderStateMixin {
  late final TabController _tabController = TabController(
    length: 3,
    vsync: this,
  );

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('BackOffice / Operador'),
        actions: [
          IconButton(
            tooltip: 'Configurar servidor',
            onPressed: () => showServerConfigDialog(context, ref),
            icon: const Icon(Icons.dns_outlined),
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white60,
          indicatorColor: AppColors.accent,
          tabs: const [
            Tab(icon: Icon(Icons.people_outline), text: 'Clientes'),
            Tab(icon: Icon(Icons.credit_card_outlined), text: 'Cuentas'),
            Tab(icon: Icon(Icons.public_outlined), text: 'Catálogos / UPC'),
          ],
        ),
      ),
      body: TabBarView(
        controller: _tabController,
        children: const [
          _ClientesAdminTab(),
          _CuentasAdminTab(),
          _CatalogosYUpcAdminTab(),
        ],
      ),
    );
  }
}

class _ClientesAdminTab extends ConsumerStatefulWidget {
  const _ClientesAdminTab();

  @override
  ConsumerState<_ClientesAdminTab> createState() => _ClientesAdminTabState();
}

class _ClientesAdminTabState extends ConsumerState<_ClientesAdminTab> {
  final _identificadorCtrl = TextEditingController();
  final _curpCtrl = TextEditingController();
  final _rfcCtrl = TextEditingController();
  final _correoCtrl = TextEditingController();
  final _cuentaCtrl = TextEditingController();
  bool? _activoFilter;

  List<ClienteDto> _clientes = const [];
  bool _loading = false;
  Object? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_buscar);
  }

  @override
  void dispose() {
    _identificadorCtrl.dispose();
    _curpCtrl.dispose();
    _rfcCtrl.dispose();
    _correoCtrl.dispose();
    _cuentaCtrl.dispose();
    super.dispose();
  }

  Future<void> _buscar() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list = await ref.read(clienteRepositoryProvider).buscarClientes(
            ClienteBusquedaAvanzadaDto(
              identificador: _identificadorCtrl.text,
              curp: _curpCtrl.text,
              rfc: _rfcCtrl.text,
              correoElectronico: _correoCtrl.text,
              numeroCuenta: _cuentaCtrl.text,
              activo: _activoFilter,
            ),
          );
      if (mounted) setState(() => _clientes = list);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _toggleActivo(ClienteDto cliente) async {
    final nuevoEstado = !cliente.activo;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: Text(nuevoEstado ? 'Reactivar cliente' : 'Desactivar cliente'),
        content: Text(
          nuevoEstado
              ? 'Se reactivará a ${cliente.nombreCompleto} y sus cuentas asociadas.'
              : 'Se aplicará baja lógica a ${cliente.nombreCompleto} y sus cuentas pasarán a INACTIVA.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(minimumSize: const Size(110, 42)),
            onPressed: () => Navigator.of(ctx).pop(true),
            child: const Text('Confirmar'),
          ),
        ],
      ),
    );
    if (confirmed != true) return;

    try {
      await ref.read(clienteRepositoryProvider).actualizarParcial(
            ClientePatchRequestDto(
              identificador: cliente.curp,
              activo: nuevoEstado,
            ),
          );
      await _buscar();
    } catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Row(
          children: [
            Expanded(
              child: FilledButton.icon(
                onPressed: () async {
                  final created = await Navigator.of(context).push<bool>(
                    MaterialPageRoute<bool>(
                      builder: (_) =>
                          const OnboardingWizardScreen(fromBackoffice: true),
                    ),
                  );
                  if (created == true) await _buscar();
                },
                icon: const Icon(Icons.person_add_outlined),
                label: const Text('Nuevo cliente'),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Card(
          child: ExpansionTile(
            title: const Text(
              'Búsqueda avanzada (POST /clientes/buscar)',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
            ),
            initiallyExpanded: true,
            childrenPadding: const EdgeInsets.fromLTRB(14, 0, 14, 14),
            children: [
              TextField(
                controller: _identificadorCtrl,
                decoration: const InputDecoration(
                  labelText: 'Identificador general (nombre, CURP, RFC, correo)',
                ),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _curpCtrl,
                      decoration: const InputDecoration(labelText: 'CURP'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: _rfcCtrl,
                      decoration: const InputDecoration(labelText: 'RFC'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _correoCtrl,
                      decoration: const InputDecoration(labelText: 'Correo'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: _cuentaCtrl,
                      decoration: const InputDecoration(labelText: 'Cuenta'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              DropdownButtonFormField<bool?>(
                initialValue: _activoFilter,
                decoration: const InputDecoration(labelText: 'Estatus'),
                items: const [
                  DropdownMenuItem(value: null, child: Text('Todos')),
                  DropdownMenuItem(value: true, child: Text('Solo activos')),
                  DropdownMenuItem(value: false, child: Text('Solo inactivos')),
                ],
                onChanged: (v) => setState(() => _activoFilter = v),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: OutlinedButton(
                      onPressed: () {
                        _identificadorCtrl.clear();
                        _curpCtrl.clear();
                        _rfcCtrl.clear();
                        _correoCtrl.clear();
                        _cuentaCtrl.clear();
                        setState(() => _activoFilter = null);
                        _buscar();
                      },
                      child: const Text('Limpiar'),
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: FilledButton.icon(
                      onPressed: _loading ? null : _buscar,
                      icon: const Icon(Icons.search),
                      label: const Text('Buscar'),
                    ),
                  ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        if (_error != null) ...[
          ErrorBanner(error: _error!, onRetry: _buscar),
          const SizedBox(height: 12),
        ],
        if (_loading)
          const Padding(
            padding: EdgeInsets.all(24),
            child: Center(child: CircularProgressIndicator()),
          )
        else if (_clientes.isEmpty)
          const Padding(
            padding: EdgeInsets.all(24),
            child: Center(child: Text('Sin resultados.')),
          )
        else
          ..._clientes.map((c) {
            final cuenta = c.cuentaPrincipal;
            return Padding(
              padding: const EdgeInsets.only(bottom: 10),
              child: Card(
                child: Padding(
                  padding: const EdgeInsets.all(14),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(
                        children: [
                          Expanded(
                            child: Text(
                              c.nombreCompleto,
                              style: const TextStyle(
                                fontWeight: FontWeight.w800,
                                fontSize: 15,
                              ),
                            ),
                          ),
                          Chip(
                            label: Text(
                              c.activo ? 'ACTIVO' : 'INACTIVO',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.w700,
                                color: c.activo
                                    ? AppColors.accent
                                    : AppColors.danger,
                              ),
                            ),
                            visualDensity: VisualDensity.compact,
                          ),
                        ],
                      ),
                      Text(
                        '${c.correoElectronico} • CURP: ${c.curp}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.mutedText,
                        ),
                      ),
                      if (cuenta != null)
                        Padding(
                          padding: const EdgeInsets.only(top: 4),
                          child: Text(
                            'Cuenta ${cuenta.numeroCuenta} (${cuenta.estatus}) • Saldo: ${formatMxn(cuenta.saldo?.saldoDisponible)}',
                            style: const TextStyle(
                              fontSize: 12.5,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 8,
                        children: [
                          TextButton.icon(
                            onPressed: () async {
                              final updated =
                                  await Navigator.of(context).push<ClienteDto>(
                                MaterialPageRoute<ClienteDto>(
                                  builder: (_) =>
                                      ClienteEditScreen(cliente: c),
                                ),
                              );
                              if (updated != null) await _buscar();
                            },
                            icon: const Icon(Icons.edit_outlined, size: 16),
                            label: const Text('Editar'),
                          ),
                          TextButton.icon(
                            onPressed: () => _toggleActivo(c),
                            icon: Icon(
                              c.activo
                                  ? Icons.block_outlined
                                  : Icons.check_circle_outline,
                              size: 16,
                              color: c.activo
                                  ? AppColors.danger
                                  : AppColors.accent,
                            ),
                            label: Text(
                              c.activo ? 'Desactivar' : 'Reactivar',
                              style: TextStyle(
                                color: c.activo
                                    ? AppColors.danger
                                    : AppColors.accent,
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
          }),
      ],
    );
  }
}

class _CuentasAdminTab extends ConsumerStatefulWidget {
  const _CuentasAdminTab();

  @override
  ConsumerState<_CuentasAdminTab> createState() => _CuentasAdminTabState();
}

class _CuentasAdminTabState extends ConsumerState<_CuentasAdminTab> {
  final _cuentaSearchCtrl = TextEditingController();
  List<CuentaDto> _activas = const [];
  CuentaDto? _consultaIndividual;
  bool _loading = false;
  Object? _error;

  @override
  void initState() {
    super.initState();
    Future.microtask(_cargarActivas);
  }

  @override
  void dispose() {
    _cuentaSearchCtrl.dispose();
    super.dispose();
  }

  Future<void> _cargarActivas() async {
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final list =
          await ref.read(cuentaRepositoryProvider).listarCuentasActivas();
      if (mounted) setState(() => _activas = list);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  Future<void> _consultarPuntual() async {
    final err = BackendValidators.numeroCuenta(_cuentaSearchCtrl.text);
    if (err != null) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(err)));
      return;
    }
    setState(() {
      _loading = true;
      _error = null;
    });
    try {
      final cta = await ref
          .read(cuentaRepositoryProvider)
          .obtenerPorNumeroCuenta(_cuentaSearchCtrl.text);
      if (mounted) setState(() => _consultaIndividual = cta);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _loading = false);
    }
  }

  void _verHistorialCuenta(String numCuenta) {
    Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => Scaffold(
          appBar: AppBar(title: Text('Historial $numCuenta')),
          body: HistorialTab(numeroCuentaOverride: numCuenta),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Consulta directa (GET /cuentas/{numeroCuenta})',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _cuentaSearchCtrl,
                        keyboardType: TextInputType.number,
                        maxLength: 16,
                        decoration: const InputDecoration(
                          labelText: 'Número de cuenta (16 dígitos)',
                          counterText: '',
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    FilledButton(
                      style: FilledButton.styleFrom(
                        minimumSize: const Size(92, 48),
                      ),
                      onPressed: _loading ? null : _consultarPuntual,
                      child: const Text('Buscar'),
                    ),
                  ],
                ),
                if (_consultaIndividual != null) ...[
                  const Divider(height: 20),
                  Text(
                    'Titular: ${_consultaIndividual!.nombreTitular ?? 'N/A'}',
                    style: const TextStyle(fontWeight: FontWeight.w700),
                  ),
                  Text(
                    'Disponible: ${formatMxn(_consultaIndividual!.saldo?.saldoDisponible)} • Contable: ${formatMxn(_consultaIndividual!.saldo?.saldoContable)}',
                  ),
                  Align(
                    alignment: Alignment.centerRight,
                    child: TextButton(
                      onPressed: () =>
                          _verHistorialCuenta(_consultaIndividual!.numeroCuenta),
                      child: const Text('Ver historial de pagos'),
                    ),
                  ),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 14),
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            Text(
              'Cuentas activas (${_activas.length})',
              style: const TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
            ),
            IconButton(
              onPressed: _loading ? null : _cargarActivas,
              icon: const Icon(Icons.refresh),
            ),
          ],
        ),
        if (_error != null) ErrorBanner(error: _error!, onRetry: _cargarActivas),
        ..._activas.map(
          (cta) => Padding(
            padding: const EdgeInsets.only(bottom: 10),
            child: Card(
              child: ListTile(
                title: Text(
                  cta.numeroCuenta,
                  style: const TextStyle(
                    fontFamily: 'monospace',
                    fontWeight: FontWeight.w700,
                  ),
                ),
                subtitle: Text(
                  '${cta.nombreTitular ?? ''}\nDisponible: ${formatMxn(cta.saldo?.saldoDisponible)}',
                ),
                trailing: IconButton(
                  tooltip: 'Ver historial',
                  onPressed: () => _verHistorialCuenta(cta.numeroCuenta),
                  icon: const Icon(Icons.receipt_long_outlined),
                ),
              ),
            ),
          ),
        ),
      ],
    );
  }
}

class _CatalogosYUpcAdminTab extends ConsumerStatefulWidget {
  const _CatalogosYUpcAdminTab();

  @override
  ConsumerState<_CatalogosYUpcAdminTab> createState() =>
      _CatalogosYUpcAdminTabState();
}

class _CatalogosYUpcAdminTabState
    extends ConsumerState<_CatalogosYUpcAdminTab> {
  final _upcCtrl = TextEditingController();
  final _nacCtrl = TextEditingController();
  NacionalidadDto? _nacBuscada;
  Object? _error;

  @override
  void dispose() {
    _upcCtrl.dispose();
    _nacCtrl.dispose();
    super.dispose();
  }

  Future<void> _buscarNacionalidad() async {
    final criterio = _nacCtrl.text.trim();
    if (criterio.isEmpty) return;
    setState(() => _error = null);
    try {
      final res = await ref
          .read(nacionalidadRepositoryProvider)
          .buscarPorCriterio(criterio);
      if (mounted) setState(() => _nacBuscada = res);
    } catch (e) {
      if (mounted) setState(() => _error = e);
    }
  }

  @override
  Widget build(BuildContext context) {
    final nacionalidades = ref.watch(nacionalidadesCatalogProvider);

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Consultar Estatus de Pago por UPC (GET /pagos/estatus/{upc})',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _upcCtrl,
                        maxLength: 10,
                        decoration: const InputDecoration(
                          labelText: 'UPC (10 caracteres alfanuméricos)',
                          counterText: '',
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    FilledButton(
                      style: FilledButton.styleFrom(
                        minimumSize: const Size(100, 48),
                      ),
                      onPressed: () {
                        final err = BackendValidators.upc(_upcCtrl.text);
                        if (err != null) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text(err)),
                          );
                          return;
                        }
                        Navigator.of(context).push(
                          MaterialPageRoute<void>(
                            builder: (_) => PagoConciliacionScreen(
                              upc: _upcCtrl.text.trim(),
                              mensajeInicial:
                                  'Consulta manual desde BackOffice',
                            ),
                          ),
                        );
                      },
                      child: const Text('Consultar'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
        const SizedBox(height: 14),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Buscar Nacionalidad (POST /catalogos/nacionalidades/buscar)',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _nacCtrl,
                        decoration: const InputDecoration(
                          labelText: 'ISO, país o gentilicio (ej. MEX)',
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    FilledButton(
                      style: FilledButton.styleFrom(
                        minimumSize: const Size(92, 48),
                      ),
                      onPressed: _buscarNacionalidad,
                      child: const Text('Buscar'),
                    ),
                  ],
                ),
                if (_nacBuscada != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    'Resultado: ID ${_nacBuscada!.idNacionalidad} • ${_nacBuscada!.claveIso} • ${_nacBuscada!.pais} (${_nacBuscada!.gentilicio})',
                    style: const TextStyle(fontWeight: FontWeight.w600),
                  ),
                ],
                if (_error != null) ...[
                  const SizedBox(height: 8),
                  ErrorBanner(error: _error!),
                ],
              ],
            ),
          ),
        ),
        const SizedBox(height: 14),
        const Text(
          'Catálogo completo de Nacionalidades',
          style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
        ),
        const SizedBox(height: 8),
        nacionalidades.when(
          loading: () => const Center(child: CircularProgressIndicator()),
          error: (e, _) => ErrorBanner(error: e),
          data: (list) => Column(
            children: list
                .map(
                  (n) => Padding(
                    padding: const EdgeInsets.only(bottom: 6),
                    child: Card(
                      child: ListTile(
                        dense: true,
                        leading: CircleAvatar(
                          child: Text(
                            n.claveIso,
                            style: const TextStyle(fontSize: 11),
                          ),
                        ),
                        title: Text(n.pais),
                        subtitle: Text('Gentilicio: ${n.gentilicio}'),
                        trailing: Text('ID ${n.idNacionalidad}'),
                      ),
                    ),
                  ),
                )
                .toList(),
          ),
        ),
      ],
    );
  }
}
