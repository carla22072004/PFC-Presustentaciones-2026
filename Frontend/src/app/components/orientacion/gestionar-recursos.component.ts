import { Component, OnInit, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrientacionService, RecursoTitulacion, GuardarRecursoRequest } from '../../services/orientacion.service';
import { CatalogoService, Carrera } from '../../services/catalogo.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-gestionar-recursos',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './gestionar-recursos.component.html',
  styleUrls: ['./gestionar-recursos.component.css']
})
export class GestionarRecursosComponent implements OnInit {

  recursos: RecursoTitulacion[] = [];
  carreras: Carrera[] = [];
  categoriasSugeridas: string[] = [];

  filtroCarreraId?: number;
  cargando = true;
  error = '';

  modalAbierto = false;
  modo: 'crear' | 'editar' = 'crear';
  editandoId: number | null = null;
  guardando = false;
  form: GuardarRecursoRequest = this.formVacio();

  constructor(
    private orientacion: OrientacionService,
    private catalogo: CatalogoService,
    private noti: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.catalogo.listarCarreras().subscribe({ next: c => { this.carreras = c || []; this.cdr.markForCheck(); }, error: () => {} });
    this.cargar();
  }

  private formVacio(): GuardarRecursoRequest {
    return { titulo: '', categoria: '', urlArchivo: '', carreraId: null };
  }

  cargar(): void {
    this.cargando = true;
    this.error = '';
    this.orientacion.recursos(this.filtroCarreraId).subscribe({
      next: data => {
        this.recursos = data || [];
        this.categoriasSugeridas = [...new Set(this.recursos.map(r => r.categoria).filter(Boolean))].sort();
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: () => { this.cargando = false; this.error = 'No se pudieron cargar los recursos.'; this.cdr.markForCheck(); }
    });
  }

  abrirNuevo(): void {
    this.modo = 'crear';
    this.editandoId = null;
    this.form = this.formVacio();
    this.modalAbierto = true;
  }

  abrirEditar(r: RecursoTitulacion): void {
    this.modo = 'editar';
    this.editandoId = r.id;
    this.form = {
      titulo: r.titulo, categoria: r.categoria, urlArchivo: r.urlArchivo,
      carreraId: r.carreraId ?? null
    };
    this.modalAbierto = true;
  }

  cerrarModal(): void { this.modalAbierto = false; this.editandoId = null; }

  get formValido(): boolean {
    const t = (this.form.titulo || '').trim();
    const c = (this.form.categoria || '').trim();
    const u = (this.form.urlArchivo || '').trim();
    return t.length > 0 && t.length <= 255
      && c.length > 0 && c.length <= 100
      && u.length > 0 && u.length <= 500;
  }

  guardar(): void {
    if (!this.formValido || this.guardando) { return; }
    this.guardando = true;
    const payload: GuardarRecursoRequest = {
      ...this.form,
      titulo: this.form.titulo.trim(),
      categoria: this.form.categoria.trim(),
      urlArchivo: this.form.urlArchivo.trim()
    };
    const req$ = this.modo === 'crear'
      ? this.orientacion.crearRecurso(payload)
      : this.orientacion.actualizarRecurso(this.editandoId!, payload);

    req$.subscribe({
      next: () => {
        this.guardando = false;
        this.modalAbierto = false;
        this.noti.success(this.modo === 'crear' ? 'Recurso creado.' : 'Recurso actualizado.', 'Listo');
        this.cargar();
      },
      error: (e) => {
        this.guardando = false;
        const msg = e?.error?.message || e?.error?.error || 'No se pudo guardar el recurso. Revisa los datos.';
        this.noti.error(msg, 'Error');
        this.cdr.markForCheck();
      }
    });
  }

  async eliminar(r: RecursoTitulacion): Promise<void> {
    const Swal = (await import('sweetalert2')).default;
    const res = await Swal.fire({
      title: '¿Eliminar este recurso?',
      text: `"${r.titulo}" dejará de estar disponible para los estudiantes.`,
      icon: 'warning', showCancelButton: true, confirmButtonColor: '#d33',
      confirmButtonText: 'Eliminar', cancelButtonText: 'Cancelar'
    });
    if (!res.isConfirmed) { return; }
    this.orientacion.eliminarRecurso(r.id).subscribe({
      next: () => { this.noti.success('Recurso eliminado.', 'Listo'); this.cargar(); },
      error: () => this.noti.error('No se pudo eliminar el recurso.', 'Error')
    });
  }

  trackById(_: number, r: RecursoTitulacion): number { return r.id; }
}
