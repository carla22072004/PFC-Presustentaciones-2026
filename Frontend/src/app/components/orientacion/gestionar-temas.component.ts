import { Component, OnInit, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrientacionService, TemaPropuesto, GuardarTemaRequest } from '../../services/orientacion.service';
import { CatalogoService, Carrera, LineaInvestigacion, AreaTematica } from '../../services/catalogo.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-gestionar-temas',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestionar-temas.component.html',
  styleUrls: ['./gestionar-temas.component.css']
})
export class GestionarTemasComponent implements OnInit {

  temas: TemaPropuesto[] = [];
  carreras: Carrera[] = [];
  lineas: LineaInvestigacion[] = [];
  areas: AreaTematica[] = [];
  niveles = ['BASICO', 'INTERMEDIO', 'AVANZADO'];

  filtroCarreraId?: number;
  cargando = true;
  error = '';

  modalAbierto = false;
  modo: 'crear' | 'editar' = 'crear';
  editandoId: number | null = null;
  guardando = false;
  form: GuardarTemaRequest = this.formVacio();

  constructor(
    private orientacion: OrientacionService,
    private catalogo: CatalogoService,
    private noti: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.catalogo.listarCarreras().subscribe({ next: c => { this.carreras = c || []; this.cdr.markForCheck(); }, error: () => {} });
    this.catalogo.listarLineasInvestigacion().subscribe({ next: l => { this.lineas = l || []; this.cdr.markForCheck(); }, error: () => {} });
    this.cargar();
  }

  private formVacio(): GuardarTemaRequest {
    return {
      titulo: '', problema: '', objetivoGeneral: '', objetivosEspecificos: '',
      justificacion: '', beneficiarios: '', nivelDificultad: '',
      carreraId: null, lineaInvestigacionId: null, areaId: null
    };
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.orientacion.explorar({ carreraId: this.filtroCarreraId }).subscribe({
      next: data => { this.temas = data || []; this.cargando = false; this.cdr.markForCheck(); },
      error: () => { this.cargando = false; this.error = 'No se pudieron cargar los temas.'; this.cdr.markForCheck(); }
    });
  }

  private cargarAreas(lineaId?: number | null): void {
    this.areas = [];
    if (lineaId) {
      this.catalogo.listarAreasTematicas(lineaId).subscribe({
        next: a => { this.areas = a || []; this.cdr.markForCheck(); }, error: () => {}
      });
    }
  }

  onLineaFormChange(): void {
    this.form.areaId = null;
    this.cargarAreas(this.form.lineaInvestigacionId);
  }

  abrirNuevo(): void {
    this.modo = 'crear';
    this.editandoId = null;
    this.form = this.formVacio();
    this.areas = [];
    this.modalAbierto = true;
  }

  abrirEditar(t: TemaPropuesto): void {
    this.modo = 'editar';
    this.editandoId = t.id;
    this.form = {
      titulo: t.titulo, problema: t.problema || '', objetivoGeneral: t.objetivoGeneral || '',
      objetivosEspecificos: t.objetivosEspecificos || '', justificacion: t.justificacion || '',
      beneficiarios: t.beneficiarios || '', nivelDificultad: t.nivelDificultad || '',
      carreraId: t.carreraId ?? null, lineaInvestigacionId: t.lineaInvestigacionId ?? null, areaId: t.areaId ?? null
    };
    this.cargarAreas(this.form.lineaInvestigacionId);
    this.modalAbierto = true;
  }

  cerrarModal(): void { this.modalAbierto = false; this.editandoId = null; }

  get formValido(): boolean {
    const t = (this.form.titulo || '').trim();
    return t.length > 0 && t.length <= 500;
  }

  guardar(): void {
    if (!this.formValido || this.guardando) { return; }
    this.guardando = true;
    const payload: GuardarTemaRequest = { ...this.form, titulo: this.form.titulo.trim() };
    const req$ = this.modo === 'crear'
      ? this.orientacion.crearTema(payload)
      : this.orientacion.actualizarTema(this.editandoId!, payload);

    req$.subscribe({
      next: () => {
        this.guardando = false;
        this.modalAbierto = false;
        this.noti.success(this.modo === 'crear' ? 'Tema creado.' : 'Tema actualizado.', 'Listo');
        this.cargar();
      },
      error: (e) => {
        this.guardando = false;
        const msg = e?.error?.message || e?.error?.error || 'No se pudo guardar el tema. Revisa los datos.';
        this.noti.error(msg, 'Error');
        this.cdr.markForCheck();
      }
    });
  }

  async eliminar(t: TemaPropuesto): Promise<void> {
    const Swal = (await import('sweetalert2')).default;
    const r = await Swal.fire({
      title: '¿Eliminar este tema?',
      text: `"${t.titulo}" se quitará del catálogo y de la lista de los estudiantes que lo guardaron.`,
      icon: 'warning', showCancelButton: true, confirmButtonColor: '#d33',
      confirmButtonText: 'Eliminar', cancelButtonText: 'Cancelar'
    });
    if (!r.isConfirmed) { return; }
    this.orientacion.eliminarTema(t.id).subscribe({
      next: () => { this.noti.success('Tema eliminado.', 'Listo'); this.cargar(); },
      error: () => this.noti.error('No se pudo eliminar el tema.', 'Error')
    });
  }

  nivelClase(nivel?: string): string {
    switch ((nivel || '').toUpperCase()) {
      case 'BASICO': return 'nivel-basico';
      case 'INTERMEDIO': return 'nivel-intermedio';
      case 'AVANZADO': return 'nivel-avanzado';
      default: return 'nivel-generico';
    }
  }

  trackById(_: number, t: TemaPropuesto): number { return t.id; }
}
