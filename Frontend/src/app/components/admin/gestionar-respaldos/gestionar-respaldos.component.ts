import { Component, ViewEncapsulation, OnInit, OnDestroy, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { forkJoin } from 'rxjs';
import {
    BackupService, BackupInfo, EstadoRespaldos, RespaldoConfig, PruebaRestauracion,
    EstadoWal, BaseFisica
} from '../../../services/backup.service';
import { NotificationService } from '../../../services/notification.service';

interface PresetCron { label: string; cron: string; }

/**
 * Apartado de administrador para la gestión de respaldos de la base de datos
 * (ver docs/basedatos/PLAN-RESPALDOS-RECUPERACION.md):
 *   - Panel de estado (última copia, próxima programada, espacio, RPO).
 *   - Cronograma: FULL + diferencial automáticos (cron editable), retención GFS y de WAL.
 *   - Copias: generar FULL / diferencial / descargar / restaurar / eliminar.
 *   - Fase 2: panel de archivado de WAL (PITR) + respaldos físicos base.
 *   - Bitácora de pruebas de restauración.
 */
@Component({
    encapsulation: ViewEncapsulation.None,
    selector: 'app-gestionar-respaldos',
    standalone: true,
    imports: [CommonModule, FormsModule],
    templateUrl: './gestionar-respaldos.component.html',
    styleUrls: ['./gestionar-respaldos.component.css']
})
export class GestionarRespaldosComponent implements OnInit, OnDestroy {
    respaldos: BackupInfo[] = [];
    estado: EstadoRespaldos | null = null;
    pruebas: PruebaRestauracion[] = [];
    wal: EstadoWal | null = null;

    cargando = true;
    generando = false;
    generandoDif = false;
    aplicandoRetencion = false;
    walOcupado: '' | 'switch' | 'limpiar' | 'base' = '';
    ocupado: string | null = null;

    // ── Cronograma ──────────────────────────────────────────────────────────
    cfg: RespaldoConfig = {
        activo: true, cron: '0 0 23 * * SUN',
        retenerDiarios: 7, retenerSemanales: 5, retenerMensuales: 12,
        retenerDiasWal: 14, diferencialActivo: false, cronDiferencial: '0 30 2 * * WED,FRI'
    };
    guardandoCfg = false;
    readonly presets: PresetCron[] = [
        { label: 'Cada domingo 23:00 (recomendado)', cron: '0 0 23 * * SUN' },
        { label: 'Todos los días 02:00',             cron: '0 0 2 * * *' },
        { label: 'Lunes a viernes 01:00',            cron: '0 0 1 * * MON-FRI' },
        { label: 'Día 1 de cada mes 00:00',          cron: '0 0 0 1 * *' },
        { label: 'Personalizado…',                   cron: 'CUSTOM' },
    ];
    presetSel = '0 0 23 * * SUN';
    readonly presetsDif: PresetCron[] = [
        { label: 'Miércoles y viernes 02:30 (recomendado)', cron: '0 30 2 * * WED,FRI' },
        { label: 'Todos los días 02:30',                    cron: '0 30 2 * * *' },
        { label: 'Lunes a viernes 02:30',                   cron: '0 30 2 * * MON-FRI' },
        { label: 'Personalizado…',                          cron: 'CUSTOM' },
    ];
    presetDifSel = '0 30 2 * * WED,FRI';

    // ── Modal: registrar prueba de restauración ─────────────────────────────
    modalPrueba = false;
    pruebaForm = { respaldoNombre: '', resultado: 'EXITOSA' as 'EXITOSA' | 'FALLIDA', responsable: '', notas: '' };
    guardandoPrueba = false;

    private refrescoTimer: any = null;

    constructor(
        private backupService: BackupService,
        private notification: NotificationService,
        private cdr: ChangeDetectorRef
    ) {}

    ngOnInit(): void {
        this.cargarTodo();
        // refresca el panel cada 60 s para ver caer los respaldos automáticos
        this.refrescoTimer = setInterval(() => this.cargarEstado(), 60_000);
    }

    ngOnDestroy(): void {
        if (this.refrescoTimer) clearInterval(this.refrescoTimer);
    }

    private async swal() {
        return (await import('sweetalert2')).default;
    }

    // ── Carga ───────────────────────────────────────────────────────────────
    cargarTodo(): void {
        this.cargando = true;
        forkJoin({
            lista: this.backupService.listar(),
            estado: this.backupService.estado(),
            config: this.backupService.getConfig(),
            pruebas: this.backupService.listarPruebas(),
            wal: this.backupService.estadoWal(),
        }).subscribe({
            next: (r) => {
                this.respaldos = r.lista || [];
                this.estado = r.estado;
                this.aplicarConfig(r.config);
                this.pruebas = r.pruebas || [];
                this.wal = r.wal;
                this.cargando = false;
                this.cdr.markForCheck();
            },
            error: () => {
                this.notification.error('No se pudo cargar la gestión de respaldos.', 'Error');
                this.cargando = false;
                this.cdr.markForCheck();
            }
        });
    }

    cargarEstado(): void {
        this.backupService.estado().subscribe({
            next: (e) => { this.estado = e; this.cdr.markForCheck(); },
            error: () => {}
        });
        this.backupService.estadoWal().subscribe({
            next: (w) => { this.wal = w; this.cdr.markForCheck(); },
            error: () => {}
        });
    }

    private recargarLista(): void {
        this.backupService.listar().subscribe({ next: (l) => { this.respaldos = l || []; this.cargarEstado(); this.cdr.markForCheck(); } });
    }

    private aplicarConfig(c: RespaldoConfig): void {
        this.cfg = { ...c };
        this.presetSel = this.presets.find(p => p.cron === c.cron)?.cron ?? 'CUSTOM';
        this.presetDifSel = this.presetsDif.find(p => p.cron === c.cronDiferencial)?.cron ?? 'CUSTOM';
    }

    // ── Cronograma ──────────────────────────────────────────────────────────
    onPresetChange(): void {
        if (this.presetSel !== 'CUSTOM') this.cfg.cron = this.presetSel;
    }
    onPresetDifChange(): void {
        if (this.presetDifSel !== 'CUSTOM') this.cfg.cronDiferencial = this.presetDifSel;
    }

    get esCronPersonalizado(): boolean { return this.presetSel === 'CUSTOM'; }
    get esCronDifPersonalizado(): boolean { return this.presetDifSel === 'CUSTOM'; }

    guardarCfg(): void {
        if (!this.cfg.cron || !this.cfg.cron.trim()) {
            this.notification.error('La expresión cron no puede estar vacía.', 'Cronograma');
            return;
        }
        this.guardandoCfg = true;
        this.backupService.guardarConfig(this.cfg).subscribe({
            next: (c) => {
                this.guardandoCfg = false;
                this.aplicarConfig(c);
                this.notification.success('Cronograma actualizado.', '✓ Guardado');
                this.cargarEstado();
            },
            error: (err) => {
                this.guardandoCfg = false;
                this.notification.error(err?.error?.message || 'No se pudo guardar el cronograma.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async aplicarRetencionAhora(): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Aplicar retención ahora?',
            html: `Se eliminarán las copias <b>automáticas</b> que sobren según la política `
                + `(${this.cfg.retenerDiarios} diarias · ${this.cfg.retenerSemanales} semanales · ${this.cfg.retenerMensuales} mensuales).<br>`
                + `Las copias manuales y por evento no se tocan.`,
            icon: 'question', showCancelButton: true,
            confirmButtonText: 'Sí, aplicar', cancelButtonText: 'Cancelar'
        });
        if (!res.isConfirmed) return;
        this.aplicandoRetencion = true;
        this.backupService.aplicarRetencion().subscribe({
            next: (eliminados) => {
                this.aplicandoRetencion = false;
                this.notification.success(
                    eliminados.length ? `${eliminados.length} copia(s) eliminada(s).` : 'No había copias que eliminar.',
                    'Retención aplicada');
                this.recargarLista();
            },
            error: (err) => {
                this.aplicandoRetencion = false;
                this.notification.error(err?.error?.message || 'No se pudo aplicar la retención.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    // ── Copias ──────────────────────────────────────────────────────────────
    generar(): void {
        if (this.generando) return;
        this.generando = true;
        this.cdr.markForCheck();
        this.backupService.generar('MANUAL').subscribe({
            next: (info) => {
                this.generando = false;
                this.notification.success(`Respaldo "${info.nombre}" generado (${info.tamanoLegible}).`, 'Respaldo creado');
                this.recargarLista();
            },
            error: (err) => {
                this.generando = false;
                this.notification.error(err?.error?.message || 'No se pudo generar el respaldo.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    generarDiferencial(): void {
        if (this.generandoDif) return;
        this.generandoDif = true;
        this.cdr.markForCheck();
        this.backupService.generarDiferencial('MANUAL').subscribe({
            next: (info) => {
                this.generandoDif = false;
                this.notification.success(`Diferencial "${info.nombre}" generado (${info.tamanoLegible}).`, 'Diferencial creado');
                this.recargarLista();
            },
            error: (err) => {
                this.generandoDif = false;
                this.notification.error(err?.error?.message || 'No se pudo generar el diferencial.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    // ── Fase 2: WAL / PITR ─────────────────────────────────────────────────
    switchWal(): void {
        this.walOcupado = 'switch';
        this.backupService.switchWal().subscribe({
            next: (wal) => {
                this.walOcupado = '';
                this.notification.success(`Segmento ${wal} cerrado. Se archivará en segundos.`, 'WAL');
                setTimeout(() => this.cargarEstado(), 3000);
            },
            error: (err) => {
                this.walOcupado = '';
                this.notification.error(err?.error?.message || 'No se pudo forzar el cierre del segmento.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async limpiarWal(): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Limpiar WAL archivado?',
            html: `Se borrarán los segmentos de WAL más antiguos que <b>${this.cfg.retenerDiasWal} días</b>, `
                + `salvo los que necesite la base física más antigua.`,
            icon: 'question', showCancelButton: true,
            confirmButtonText: 'Sí, limpiar', cancelButtonText: 'Cancelar'
        });
        if (!res.isConfirmed) return;
        this.walOcupado = 'limpiar';
        this.backupService.limpiarWal().subscribe({
            next: (n) => {
                this.walOcupado = '';
                this.notification.success(n ? `${n} segmento(s) eliminados.` : 'No había WAL para limpiar.', 'Limpieza de WAL');
                this.cargarEstado();
            },
            error: (err) => {
                this.walOcupado = '';
                this.notification.error(err?.error?.message || 'No se pudo limpiar el WAL.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async crearBaseFisica(): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Crear base física?',
            html: `Se ejecutará <code>pg_basebackup</code> (copia binaria del clúster). Es la base a partir `
                + `de la cual se puede recuperar hacia adelante con el WAL archivado (PITR).`,
            icon: 'question', showCancelButton: true,
            confirmButtonText: 'Sí, crear', cancelButtonText: 'Cancelar'
        });
        if (!res.isConfirmed) return;
        this.walOcupado = 'base';
        this.backupService.generarBaseFisica().subscribe({
            next: (b) => {
                this.walOcupado = '';
                this.notification.success(`Base física "${b.nombre}" creada (${b.tamanoLegible}).`, '✓ Base creada');
                this.cargarEstado();
            },
            error: (err) => {
                this.walOcupado = '';
                this.notification.error(err?.error?.message || 'No se pudo crear la base física.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async eliminarBase(b: BaseFisica): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Eliminar esta base física?',
            text: `Se eliminará "${b.nombre}". Si es la más antigua, el WAL anterior deja de servir para PITR.`,
            icon: 'warning', showCancelButton: true,
            confirmButtonText: 'Sí, eliminar', cancelButtonText: 'Cancelar', confirmButtonColor: '#dc2626'
        });
        if (!res.isConfirmed) return;
        this.backupService.eliminarBase(b.nombre).subscribe({
            next: () => { this.notification.success('Base física eliminada.', 'Listo'); this.cargarEstado(); },
            error: (err) => this.notification.error(err?.error?.message || 'No se pudo eliminar la base física.', 'Error')
        });
    }

    descargar(r: BackupInfo): void {
        this.backupService.descargar(r.nombre).subscribe({
            next: (blob) => {
                const url = URL.createObjectURL(blob);
                const a = document.createElement('a');
                a.href = url; a.download = r.nombre; a.click();
                URL.revokeObjectURL(url);
            },
            error: () => this.notification.error('No se pudo descargar el respaldo.', 'Error')
        });
    }

    async restaurar(r: BackupInfo): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Restaurar la base de datos?',
            html: `Se reemplazará <b>todo</b> el contenido actual de la base por el del respaldo `
                + `<b>${r.nombre}</b> (${this.fecha(r.fechaCreacion)}).<br><br>`
                + `Esta acción no se puede deshacer. Después conviene reiniciar el backend.`,
            icon: 'warning', showCancelButton: true,
            confirmButtonText: 'Sí, restaurar', cancelButtonText: 'Cancelar', confirmButtonColor: '#dc2626'
        });
        if (!res.isConfirmed) return;
        this.ocupado = r.nombre;
        this.cdr.markForCheck();
        this.backupService.restaurar(r.nombre).subscribe({
            next: (resp: any) => {
                this.ocupado = null;
                this.notification.success(resp?.message || 'Base de datos restaurada desde el respaldo.', 'Restauración completa');
                this.cdr.markForCheck();
            },
            error: (err) => {
                this.ocupado = null;
                this.notification.error(err?.error?.message || 'No se pudo restaurar la base de datos.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    async eliminar(r: BackupInfo): Promise<void> {
        const Swal = await this.swal();
        const res = await Swal.fire({
            title: '¿Eliminar este respaldo?',
            text: `Se eliminará permanentemente "${r.nombre}". Esta acción no se puede deshacer.`,
            icon: 'warning', showCancelButton: true,
            confirmButtonText: 'Sí, eliminar', cancelButtonText: 'Cancelar', confirmButtonColor: '#dc2626'
        });
        if (!res.isConfirmed) return;
        this.ocupado = r.nombre;
        this.cdr.markForCheck();
        this.backupService.eliminar(r.nombre).subscribe({
            next: () => { this.ocupado = null; this.notification.success('Respaldo eliminado.', 'Listo'); this.recargarLista(); },
            error: (err) => {
                this.ocupado = null;
                this.notification.error(err?.error?.message || 'No se pudo eliminar el respaldo.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    // ── Pruebas de restauración ─────────────────────────────────────────────
    abrirModalPrueba(): void {
        this.pruebaForm = {
            respaldoNombre: this.respaldos[0]?.nombre || '',
            resultado: 'EXITOSA', responsable: '', notas: ''
        };
        this.modalPrueba = true;
    }
    cerrarModalPrueba(): void { this.modalPrueba = false; }

    guardarPrueba(): void {
        if (!this.pruebaForm.respaldoNombre) {
            this.notification.error('Selecciona el respaldo probado.', 'Falta el respaldo');
            return;
        }
        this.guardandoPrueba = true;
        this.backupService.registrarPrueba({
            respaldoNombre: this.pruebaForm.respaldoNombre,
            resultado: this.pruebaForm.resultado,
            responsable: this.pruebaForm.responsable || undefined,
            notas: this.pruebaForm.notas || undefined,
        }).subscribe({
            next: () => {
                this.guardandoPrueba = false;
                this.modalPrueba = false;
                this.notification.success('Prueba registrada en la bitácora.', '✓ Registrada');
                this.backupService.listarPruebas().subscribe(p => { this.pruebas = p || []; this.cargarEstado(); this.cdr.markForCheck(); });
            },
            error: (err) => {
                this.guardandoPrueba = false;
                this.notification.error(err?.error?.message || 'No se pudo registrar la prueba.', 'Error');
                this.cdr.markForCheck();
            }
        });
    }

    // ── Utilidades de plantilla ────────────────────────────────────────────
    fecha(iso: string | null): string {
        if (!iso) return '—';
        const d = new Date(iso);
        return isNaN(d.getTime()) ? iso : d.toLocaleString('es-EC');
    }

    tipoTexto(t: string): string {
        return t === 'DIFERENCIAL' ? 'Diferencial' : 'Full';
    }
    tipoClase(t: string): string {
        return t === 'DIFERENCIAL' ? 'bkp-b-dif' : 'bkp-b-tipo';
    }
    origenTexto(o: string): string {
        return o === 'AUTOMATICO' ? 'Automático' : o === 'EVENTO' ? 'Evento' : 'Manual';
    }
    origenClase(o: string): string {
        return o === 'AUTOMATICO' ? 'bkp-b-auto' : o === 'EVENTO' ? 'bkp-b-evento' : 'bkp-b-manual';
    }
}
