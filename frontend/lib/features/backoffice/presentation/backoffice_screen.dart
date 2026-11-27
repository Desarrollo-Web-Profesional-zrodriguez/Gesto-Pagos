import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:gestopago_app/app/theme/app_theme.dart';
import 'package:gestopago_app/core/validation/backend_validators.dart';
import 'package:gestopago_app/core/widgets/app_feedback.dart';
import 'package:gestopago_app/core/widgets/modern_selectors.dart';
import 'package:gestopago_app/features/backoffice/presentation/backoffice_session.dart';
import 'package:gestopago_app/features/catalogos_sistema/data/nacionalidad_repository.dart';
import 'package:gestopago_app/features/clientes/data/cliente_models.dart';
import 'package:gestopago_app/features/clientes/data/cliente_repository.dart';
import 'package:gestopago_app/features/clientes/presentation/cliente_edit_screen.dart';
import 'package:gestopago_app/features/cuentas/data/cuenta_repository.dart';
import 'package:gestopago_app/features/onboarding/presentation/onboarding_wizard_screen.dart';
import 'package:gestopago_app/features/pagos/presentation/historial_tab.dart';
import 'package:gestopago_app/features/pagos/presentation/pago_resultado_views.dart';
import 'package:go_router/go_router.dart';

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

  Future<void> _cerrarConsola() async {
    await ref.read(backofficeSessionProvider.notifier).logout();
    if (mounted) context.go('/login');
  }

  @override
  void dispose() {
    _tabController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        backgroundColor: AppColors.slate950,
        title: Row(
          children: [
            Container(
              width: 34,
              height: 34,
              decoration: BoxDecoration(
                color: AppColors.warning.withValues(alpha: 0.16),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(
                  color: AppColors.warning.withValues(alpha: 0.35),
                ),
              ),
              child: const Icon(
                Icons.business_outlined,
                color: Color(0xFFFCD34D),
                size: 18,
              ),
            ),
            const SizedBox(width: 10),
            const Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Consola Operativa',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.w800),
                  ),
                  Text(
                    'Administración y supervisión',
                    style: TextStyle(fontSize: 11, color: Color(0xFF94A3B8)),
                  ),
                ],
              ),
            ),
          ],
        ),
        actions: [
          TextButton.icon(
            onPressed: _cerrarConsola,
            icon: const Icon(
              Icons.logout,
              size: 18,
              color: Color(0xFFFCA5A5),
            ),
            label: const Text(
              'Salir',
              style: TextStyle(
                color: Color(0xFFFECDD3),
                fontWeight: FontWeight.w700,
              ),
            ),
          ),
        ],
        bottom: TabBar(
          controller: _tabController,
          labelColor: Colors.white,
          unselectedLabelColor: Colors.white60,
          indicatorColor: AppColors.secondary,
          tabs: const [
            Tab(icon: Icon(Icons.people_outline), text: 'Clientes'),
            Tab(icon: Icon(Icons.credit_card_outlined), text: 'Cuentas'),
            Tab(icon: Icon(Icons.public_outlined), text: 'UPC y Catálogos'),
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
  String _activoFilter = 'todos';

  List<ClienteDto> _clientes = const [];
  bool _loading = false;
  Object? _error;

  static const _estatusOptions = <SelectOption<String>>[
    SelectOption(
      value: 'todos',
      label: 'Todos los estados',
      dotColor: Color(0xFF94A3B8),
    ),
    SelectOption(
      value: 'true',
      label: 'Solo activos',
      dotColor: AppColors.accent,
    ),
    SelectOption(
      value: 'false',
      label: 'Solo inactivos',
      dotColor: AppColors.danger,
    ),
  ];

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
      final activoBool =
          _activoFilter == 'todos' ? null : _activoFilter == 'true';
      final list = await ref.read(clienteRepositoryProvider).buscarClientes(
            ClienteBusquedaAvanzadaDto(
              identificador: _identificadorCtrl.text,
              curp: _curpCtrl.text,
              rfc: _rfcCtrl.text,
              correoElectronico: _correoCtrl.text,
              numeroCuenta: _cuentaCtrl.text,
              activo: activoBool,
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
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
        title: Text(nuevoEstado ? 'Reactivar cliente' : 'Desactivar cliente'),
        content: Text(
          nuevoEstado
              ? 'Se reactivará el expediente de ${cliente.nombreCompleto} y sus cuentas asociadas.'
              : 'Se desactivará a ${cliente.nombreCompleto} y sus cuentas pasarán a estado inactivo.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          FilledButton(
            style: FilledButton.styleFrom(
              minimumSize: const Size(110, 42),
              backgroundColor:
                  nuevoEstado ? AppColors.accent : AppColors.danger,
            ),
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
    final activos = _clientes.where((c) => c.activo).length;
    final inactivos = _clientes.length - activos;

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Row(
          children: [
            Expanded(
              child: _AdminStatTile(
                label: 'Listados',
                value: '${_clientes.length}',
                color: AppColors.primary,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _AdminStatTile(
                label: 'Activos',
                value: '$activos',
                color: const Color(0xFF047857),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: _AdminStatTile(
                label: 'Inactivos',
                value: '$inactivos',
                color: AppColors.mutedText,
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        FilledButton.icon(
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
          label: const Text('Alta de nuevo cliente'),
        ),
        const SizedBox(height: 12),
        Card(
          child: ExpansionTile(
            title: const Text(
              'Búsqueda avanzada de clientes',
              style: TextStyle(fontSize: 14, fontWeight: FontWeight.w700),
            ),
            initiallyExpanded: true,
            childrenPadding: const EdgeInsets.fromLTRB(14, 0, 14, 14),
            children: [
              TextField(
                controller: _identificadorCtrl,
                decoration: const InputDecoration(
                  labelText: 'Identificador (nombre, CURP, RFC o correo)',
                  prefixIcon: Icon(Icons.search),
                ),
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _curpCtrl,
                      textCapitalization: TextCapitalization.characters,
                      decoration: const InputDecoration(labelText: 'CURP'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: _rfcCtrl,
                      textCapitalization: TextCapitalization.characters,
                      decoration: const InputDecoration(labelText: 'RFC'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _correoCtrl,
                      keyboardType: TextInputType.emailAddress,
                      decoration: const InputDecoration(labelText: 'Correo'),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: TextField(
                      controller: _cuentaCtrl,
                      keyboardType: TextInputType.number,
                      decoration: const InputDecoration(labelText: 'Cuenta'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 10),
              ModernSelectField<String>(
                label: 'Estado de expediente',
                value: _activoFilter,
                options: _estatusOptions,
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
                        setState(() => _activoFilter = 'todos');
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
            child: Center(
              child: Text(
                'No se encontraron expedientes con esos criterios.',
                style: TextStyle(color: AppColors.mutedText),
              ),
            ),
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
                                color: AppColors.primary,
                              ),
                            ),
                          ),
                          StatusBadge(
                            estatus: c.activo ? 'ACTIVA' : 'INACTIVO',
                          ),
                        ],
                      ),
                      const SizedBox(height: 4),
                      Text(
                        '${c.correoElectronico} • Tel: ${c.telefonoMovil}',
                        style: const TextStyle(
                          fontSize: 12,
                          color: AppColors.mutedText,
                        ),
                      ),
                      const SizedBox(height: 8),
                      Wrap(
                        spacing: 6,
                        runSpacing: 6,
                        children: [
                          CopyChip(
                            value: c.curp,
                            displayLabel: 'CURP: ${c.curp}',
                            copyLabel: 'CURP',
                          ),
                          CopyChip(
                            value: c.rfc,
                            displayLabel: 'RFC: ${c.rfc}',
                            copyLabel: 'RFC',
                          ),
                        ],
                      ),
                      if (cuenta != null) ...[
                        const SizedBox(height: 8),
                        Container(
                          padding: const EdgeInsets.symmetric(
                            horizontal: 10,
                            vertical: 6,
                          ),
                          decoration: BoxDecoration(
                            color: AppColors.emeraldSoft,
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Text(
                            '${maskCuenta(cuenta.numeroCuenta)} · ${formatMxn(cuenta.saldo?.saldoDisponible)}',
                            style: const TextStyle(
                              fontFamily: 'monospace',
                              fontSize: 12,
                              fontWeight: FontWeight.w700,
                              color: Color(0xFF047857),
                            ),
                          ),
                        ),
                      ],
                      const SizedBox(height: 8),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton.icon(
                            onPressed: () async {
                              final updated =
                                  await Navigator.of(context).push<ClienteDto>(
                                MaterialPageRoute<ClienteDto>(
                                  builder: (_) => ClienteEditScreen(
                                    cliente: c,
                                    allowFullReplace: true,
                                  ),
                                ),
                              );
                              if (updated != null) await _buscar();
                            },
                            icon: const Icon(Icons.edit_outlined, size: 16),
                            label: const Text('Editar'),
                          ),
                          const SizedBox(width: 4),
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

class _AdminStatTile extends StatelessWidget {
  const _AdminStatTile({
    required this.label,
    required this.value,
    required this.color,
  });

  final String label;
  final String value;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              label,
              style: const TextStyle(fontSize: 11, color: AppColors.mutedText),
            ),
            const SizedBox(height: 4),
            Text(
              value,
              style: TextStyle(
                fontSize: 20,
                fontWeight: FontWeight.w800,
                color: color,
              ),
            ),
          ],
        ),
      ),
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
          appBar: AppBar(title: Text('Historial · ${maskCuenta(numCuenta)}')),
          body: HistorialTab(numeroCuentaOverride: numCuenta),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final liquidez = _activas.fold<double>(
      0.0,
      (acc, c) => acc + (c.saldo?.saldoDisponible ?? 0.0),
    );

    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Row(
          children: [
            Expanded(
              child: _AdminStatTile(
                label: 'Cuentas activas',
                value: '${_activas.length}',
                color: AppColors.primary,
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              flex: 2,
              child: _AdminStatTile(
                label: 'Liquidez disponible',
                value: formatMxn(liquidez),
                color: const Color(0xFF047857),
              ),
            ),
          ],
        ),
        const SizedBox(height: 12),
        Card(
          child: Padding(
            padding: const EdgeInsets.all(14),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Consulta directa por número de cuenta',
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
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          'Titular: ${_consultaIndividual!.nombreTitular ?? 'No disponible'}',
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                      ),
                      StatusBadge(estatus: _consultaIndividual!.estatus),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Disponible: ${formatMxn(_consultaIndividual!.saldo?.saldoDisponible)} • Contable: ${formatMxn(_consultaIndividual!.saldo?.saldoContable)}',
                    style: const TextStyle(fontSize: 12.5),
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
              child: Padding(
                padding: const EdgeInsets.all(14),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        CopyChip(
                          value: cta.numeroCuenta,
                          copyLabel: 'Número de cuenta',
                        ),
                        StatusBadge(estatus: cta.estatus),
                      ],
                    ),
                    const SizedBox(height: 8),
                    Text(
                      '${cta.nombreTitular ?? 'Titular'} · ${cta.tipoCuenta}',
                      style: const TextStyle(
                        fontWeight: FontWeight.w700,
                        fontSize: 13.5,
                      ),
                    ),
                    const SizedBox(height: 4),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Text(
                          'Disponible: ${formatMxn(cta.saldo?.saldoDisponible)}',
                          style: const TextStyle(
                            fontWeight: FontWeight.w700,
                            color: Color(0xFF047857),
                          ),
                        ),
                        TextButton.icon(
                          onPressed: () => _verHistorialCuenta(cta.numeroCuenta),
                          icon: const Icon(Icons.receipt_long_outlined, size: 16),
                          label: const Text('Historial'),
                        ),
                      ],
                    ),
                  ],
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
                  'Consulta de pago por folio UPC',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _upcCtrl,
                        maxLength: 10,
                        textCapitalization: TextCapitalization.characters,
                        decoration: const InputDecoration(
                          labelText: 'Folio UPC (10 caracteres)',
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
                                  'Consulta de seguimiento administrativo',
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
                  'Buscar nacionalidad',
                  style: TextStyle(fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: _nacCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Código ISO, país o gentilicio (ej. MEX)',
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
                    'Resultado: ${_nacBuscada!.claveIso} • ${_nacBuscada!.pais} (${_nacBuscada!.gentilicio})',
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
          'Catálogo de Nacionalidades',
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
                          backgroundColor: AppColors.skySoft,
                          child: Text(
                            n.claveIso,
                            style: const TextStyle(
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                              color: AppColors.secondary,
                            ),
                          ),
                        ),
                        title: Text(
                          n.pais,
                          style: const TextStyle(fontWeight: FontWeight.w700),
                        ),
                        subtitle: Text('Gentilicio: ${n.gentilicio}'),
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
