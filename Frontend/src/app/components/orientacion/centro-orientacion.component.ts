import { Component, OnInit, ChangeDetectorRef, ViewEncapsulation } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { OrientacionService, TemaPropuesto, FiltroTemas, RecursoTitulacion, ProgresoTitulacion, PasoProgreso } from '../../services/orientacion.service';
import { CatalogoService, Carrera, LineaInvestigacion, AreaTematica } from '../../services/catalogo.service';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

type Tab = 'explorar' | 'guardados' | 'recursos' | 'progreso';

@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-centro-orientacion',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './centro-orientacion.component.html',
  styleUrls: ['./centro-orientacion.component.css']
})
export class CentroOrientacionComponent implements OnInit {

  tab: Tab = 'explorar';
  esEstudiante = false;

  // Catálogos para los filtros
  carreras: Carrera[] = [];
  lineas: LineaInvestigacion[] = [];
  areas: AreaTematica[] = [];
  niveles = ['BASICO', 'INTERMEDIO', 'AVANZADO'];

  filtro: FiltroTemas = {};

  temas: TemaPropuesto[] = [];
  guardados: TemaPropuesto[] = [];
  seleccionado: TemaPropuesto | null = null;

  cargando = false;
  cargandoGuardados = false;
  error = '';
  accionEnCurso: number | null = null;

  // Recursos
  recursos: RecursoTitulacion[] = [];
  recursosPorCategoria: { categoria: string; items: RecursoTitulacion[] }[] = [];
  cargandoRecursos = false;
  errorRecursos = '';

  // Progreso
  progreso: ProgresoTitulacion | null = null;
  cargandoProgreso = false;
  errorProgreso = '';
  pasoEnCurso: string | null = null;

  constructor(
    private orientacion: OrientacionService,
    private catalogo: CatalogoService,
    private auth: AuthService,
    private noti: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.esEstudiante = this.auth.getRole() === 'ESTUDIANTE';

    this.catalogo.listarCarreras().subscribe({
      next: c => { this.carreras = c || []; this.cdr.markForCheck(); },
      error: () => {}
    });
    this.catalogo.listarLineasInvestigacion().subscribe({
      next: l => { this.lineas = l || []; this.cdr.markForCheck(); },
      error: () => {}
    });

    this.explorar();
    this.cargarRecursos();
    if (this.esEstudiante) {
      this.cargarGuardados();
    }
  }

  // ── Navegación de pestañas ────────────────────────────────────────────
  irA(tab: Tab): void {
    this.tab = tab;
    this.seleccionado = null;
    if (tab === 'guardados' && !this.guardados.length) {
      this.cargarGuardados();
    }
    if (tab === 'recursos' && !this.recursos.length) {
      this.cargarRecursos();
    }
    if (tab === 'progreso' && !this.progreso) {
      this.cargarProgreso();
    }
  }

  // ── Recursos ─────────────────────────────────────────────────────────
  cargarRecursos(): void {
    this.cargandoRecursos = true;
    this.errorRecursos = '';
    this.orientacion.recursos().subscribe({
      next: data => {
        this.recursos = data || [];
        this.recursosPorCategoria = this.agruparPorCategoria(this.recursos);
        this.cargandoRecursos = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.cargandoRecursos = false;
        this.errorRecursos = 'No se pudieron cargar los recursos de titulación.';
        this.cdr.markForCheck();
      }
    });
  }

  private agruparPorCategoria(items: RecursoTitulacion[]) {
    const mapa = new Map<string, RecursoTitulacion[]>();
    for (const r of items) {
      const k = r.categoria || 'Otros';
      if (!mapa.has(k)) { mapa.set(k, []); }
      mapa.get(k)!.push(r);
    }
    return [...mapa.entries()].map(([categoria, its]) => ({ categoria, items: its }));
  }

  // ── Progreso ─────────────────────────────────────────────────────────
  cargarProgreso(): void {
    if (!this.esEstudiante) { return; }
    this.cargandoProgreso = true;
    this.errorProgreso = '';
    this.orientacion.miProgreso().subscribe({
      next: p => { this.progreso = p; this.cargandoProgreso = false; this.cdr.markForCheck(); },
      error: () => {
        this.cargandoProgreso = false;
        this.errorProgreso = 'No se pudo cargar tu ruta de titulación.';
        this.cdr.markForCheck();
      }
    });
  }

  togglePaso(paso: PasoProgreso): void {
    if (!this.esEstudiante || this.pasoEnCurso) { return; }
    this.pasoEnCurso = paso.clave;
    const nuevoValor = !paso.completado;
    this.orientacion.actualizarProgreso({ [paso.clave]: nuevoValor }).subscribe({
      next: p => { this.progreso = p; this.pasoEnCurso = null; this.cdr.markForCheck(); },
      error: () => {
        this.pasoEnCurso = null;
        this.noti.error('No se pudo actualizar el paso. Intenta nuevamente.', 'Error');
        this.cdr.markForCheck();
      }
    });
  }

  // ── Filtros ───────────────────────────────────────────────────────────
  onLineaChange(): void {
    this.filtro.areaId = undefined;
    this.areas = [];
    if (this.filtro.lineaInvestigacionId != null) {
      this.catalogo.listarAreasTematicas(this.filtro.lineaInvestigacionId).subscribe({
        next: a => { this.areas = a || []; this.cdr.markForCheck(); },
        error: () => {}
      });
    }
  }

  limpiarFiltros(): void {
    this.filtro = {};
    this.areas = [];
    this.explorar();
  }

  // ── Carga de datos ────────────────────────────────────────────────────
  explorar(): void {
    this.cargando = true;
    this.error = '';
    this.orientacion.explorar(this.filtro).subscribe({
      next: data => {
        this.temas = data || [];
        this.cargando = false;
        this.cdr.markForCheck();
      },
      error: () => {
        this.cargando = false;
        this.error = 'No se pudieron cargar los temas propuestos. Intenta nuevamente.';
        this.cdr.markForCheck();
      }
    });
  }

  cargarGuardados(): void {
    if (!this.esEstudiante) { return; }
    this.cargandoGuardados = true;
    this.orientacion.misGuardados().subscribe({
      next: data => {
        this.guardados = data || [];
        this.cargandoGuardados = false;
        this.sincronizarFlagsGuardado();
        this.cdr.markForCheck();
      },
      error: () => {
        this.cargandoGuardados = false;
        this.noti.error('No se pudieron cargar tus temas guardados.', 'Error');
        this.cdr.markForCheck();
      }
    });
  }

  // ── Acciones del estudiante ───────────────────────────────────────────
  guardar(tema: TemaPropuesto, ev?: Event): void {
    ev?.stopPropagation();
    if (!this.esEstudiante || this.accionEnCurso === tema.id) { return; }
    this.accionEnCurso = tema.id;
    this.orientacion.guardar(tema.id).subscribe({
      next: () => {
        tema.guardado = true;
        this.accionEnCurso = null;
        this.cargarGuardados();
        this.noti.success('Tema guardado en tu lista.', 'Guardado');
        this.cdr.markForCheck();
      },
      error: (e) => {
        this.accionEnCurso = null;
        const msg = e?.status === 409
          ? 'Este tema ya estaba en tu lista.'
          : 'No se pudo guardar el tema. Intenta nuevamente.';
        this.noti.error(msg, 'Error');
        this.cdr.markForCheck();
      }
    });
  }

  quitar(tema: TemaPropuesto, ev?: Event): void {
    ev?.stopPropagation();
    if (!this.esEstudiante || this.accionEnCurso === tema.id) { return; }
    this.accionEnCurso = tema.id;
    this.orientacion.quitarGuardado(tema.id).subscribe({
      next: () => {
        tema.guardado = false;
        this.guardados = this.guardados.filter(t => t.id !== tema.id);
        this.accionEnCurso = null;
        this.noti.success('Tema quitado de tu lista.', 'Listo');
        this.cdr.markForCheck();
      },
      error: () => {
        this.accionEnCurso = null;
        this.noti.error('No se pudo quitar el tema. Intenta nuevamente.', 'Error');
        this.cdr.markForCheck();
      }
    });
  }

  // ── Detalle ───────────────────────────────────────────────────────────
  verDetalle(tema: TemaPropuesto): void {
    this.seleccionado = tema;
    this.orientacion.detalle(tema.id).subscribe({
      next: d => { this.seleccionado = { ...d, guardado: tema.guardado }; this.cdr.markForCheck(); },
      error: () => {}
    });
  }

  cerrarDetalle(): void { this.seleccionado = null; }

  // ── Helpers ───────────────────────────────────────────────────────────
  private sincronizarFlagsGuardado(): void {
    const ids = new Set(this.guardados.map(g => g.id));
    this.temas.forEach(t => t.guardado = ids.has(t.id));
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
