import { Component, ViewEncapsulation, OnInit, ViewChild, ElementRef, ChangeDetectorRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterModule } from '@angular/router';
import { forkJoin } from 'rxjs';
import Swal from 'sweetalert2';
import { TutoriaService } from '../../../services/tutoria.service';
import { SolicitudService } from '../../../services/solicitud.service';
import { AuthService } from '../../../services/auth.service';
import { NotificationService } from '../../../services/notification.service';
import { TutoriaFase, TutoriaMensaje, TutoriaResumen } from '../../../models/tutoria.model';

@Component({
  encapsulation: ViewEncapsulation.None,
  selector: 'app-detalle-tutoria',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './detalle-tutoria.component.html',
  styleUrls: ['./detalle-tutoria.component.css']
})
export class DetalleTutoriaComponent implements OnInit {
  @ViewChild('chatContainer') chatContainer!: ElementRef;

  tutorId!: number;
  resumen!: TutoriaResumen;
  fases: TutoriaFase[] = [];
  faseSeleccionada: TutoriaFase | null = null;
  rol = '';
  userId = 0;

  cargando = true;
  observacionNuevaFase = '';
  nuevoMensaje = '';
  archivoSeleccionado: File | null = null;
  isDragOver = false;
  subiendoPdf = false;
  creandoFase = false;
  aprobando = false;
  enviandoMensaje = false;

  // Suspension de la solicitud
  solicitudSuspendida = false;
  solicitudMotivoSuspension = '';

  readonly SLOTS = [1, 2, 3];

  constructor(
    private route: ActivatedRoute,
    private tutoriaService: TutoriaService,
    private solicitudService: SolicitudService,
    private authService: AuthService,
    private notificationService: NotificationService,
    private cdr: ChangeDetectorRef
  ) {}

  ngOnInit(): void {
    this.rol = this.authService.getRole();
    this.userId = this.authService.getUserId();
    this.tutorId = Number(this.route.snapshot.paramMap.get('id'));
    this.cargarDatos(true);
  }

  // ── Carga / Recarga ───────────────────────────────────────────────────────

  private cargarDatos(seleccionarUltima = false): void {
    this.cargando = true;
    forkJoin({
      resumen: this.tutoriaService.obtenerResumen(this.tutorId, this.userId),
      fases: this.tutoriaService.obtenerFases(this.tutorId)
    }).subscribe({
      next: ({ resumen, fases }) => {
        this.resumen = resumen;
        this.fases = fases;
        this.cargando = false;

        if (resumen.solicitudId) {
          this.solicitudService.obtenerPorId(resumen.solicitudId).subscribe({
            next: (solicitud) => {
              this.solicitudSuspendida = solicitud.estado === 'SUSPENDIDA';
              this.solicitudMotivoSuspension = solicitud.motivoSuspension || '';
              this.cdr.markForCheck();
            },
            error: () => {
              this.solicitudSuspendida = false;
              this.cdr.markForCheck();
            }
          });
        }

        if (fases.length) {
          if (seleccionarUltima) {
            const ultima = fases.reduce((p, c) => c.numeroFase > p.numeroFase ? c : p);
            this.seleccionarFase(ultima);
          } else if (this.faseSeleccionada) {
            const actualizada = fases.find(f => f.id === this.faseSeleccionada!.id);
            if (actualizada) this.seleccionarFase(actualizada);
          }
        }
        this.cdr.markForCheck();
      },
      error: () => {
        this.notificationService.error('No se pudo cargar la tutoría.', 'Error');
        this.cargando = false;
        this.cdr.markForCheck();
      }
    });
  }

  // ── Selección de fase ─────────────────────────────────────────────────────

  seleccionarFase(fase: TutoriaFase): void {
    this.faseSeleccionada = fase;
    this.tutoriaService.marcarMensajesLeidos(fase.id, this.userId).subscribe();
    setTimeout(() => this.scrollToBottom(), 80);
  }

  private scrollToBottom(): void {
    if (this.chatContainer?.nativeElement) {
      this.chatContainer.nativeElement.scrollTop = this.chatContainer.nativeElement.scrollHeight;
    }
  }

  // ── Helpers de plantilla ──────────────────────────────────────────────────

  getFasePorNumero(n: number): TutoriaFase | undefined {
    return this.fases.find(f => f.numeroFase === n);
  }

  get ultimaObservacion(): TutoriaMensaje | null {
    if (!this.faseSeleccionada) return null;
    const obs = this.faseSeleccionada.mensajes.filter(m => m.tipo === 'OBSERVACION');
    return obs.length ? obs[obs.length - 1] : null;
  }

  get tipoMensajeEnviar(): 'OBSERVACION' | 'RESPUESTA' {
    return this.rol === 'DOCENTE' ? 'OBSERVACION' : 'RESPUESTA';
  }

  /** Calculado dinámicamente del array fases[], más preciso que resumen.fasesAprobadas */
  get fasesAprobadas(): number {
    return this.fases.filter(f => f.estado === 'APROBADA').length;
  }

  get tutoriaCompleta(): boolean {
    return this.resumen?.estadoTutoria === 'COMPLETADA' || this.fasesAprobadas === 3;
  }

  esMensajePropio(m: TutoriaMensaje): boolean {
    return m.remitenteId === this.userId;
  }

  formatDate(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return `${String(d.getDate()).padStart(2, '0')}/${String(d.getMonth() + 1).padStart(2, '0')}/${d.getFullYear()}`;
  }

  formatDateTime(iso: string): string {
    if (!iso) return '';
    const d = new Date(iso);
    return `${this.formatDate(iso)} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`;
  }

  formatBytes(bytes: number): string {
    if (!bytes) return '';
    return bytes < 1024 * 1024
      ? `${(bytes / 1024).toFixed(1)} KB`
      : `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  }

  // ── Archivo / Drag & Drop ─────────────────────────────────────────────────

  onFileSelected(event: any): void {
    const file: File = event?.target?.files?.[0] ?? event;
    if (!file) return;
    if (file.type !== 'application/pdf') {
      this.notificationService.error('Solo se permiten archivos en formato PDF.', 'Formato inválido');
      return;
    }
    if (file.size > 10 * 1024 * 1024) {
      this.notificationService.error('El archivo supera los 10 MB permitidos.', 'Archivo muy grande');
      return;
    }
    this.archivoSeleccionado = file;
  }

  quitarArchivo(): void { this.archivoSeleccionado = null; }

  onDragOver(event: DragEvent): void { event.preventDefault(); this.isDragOver = true; }
  onDragLeave(): void { this.isDragOver = false; }
  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.isDragOver = false;
    const file = event.dataTransfer?.files[0];
    if (file) this.onFileSelected(file);
  }

  // ── Acciones ──────────────────────────────────────────────────────────────

  crearFase(): void {
    if (!this.observacionNuevaFase.trim()) {
      this.notificationService.error('Debes escribir una observación para iniciar la fase.', 'Campo requerido');
      return;
    }
    this.creandoFase = true;
    this.tutoriaService.crearFaseConObservacion(this.tutorId, this.userId, this.observacionNuevaFase).subscribe({
      next: () => {
        this.observacionNuevaFase = '';
        this.creandoFase = false;
        this.notificationService.success('Fase creada correctamente.', '¡Fase iniciada!');
        this.cargarDatos(true);
      },
      error: (err) => {
        this.creandoFase = false;
        this.notificationService.error(err?.error?.error || 'Ocurrió un error', 'Error');
      }
    });
  }

  subirPdf(): void {
    if (!this.archivoSeleccionado || !this.faseSeleccionada) return;
    this.subiendoPdf = true;
    const faseId = this.faseSeleccionada.id;
    this.tutoriaService.subirPdfCorregido(faseId, this.archivoSeleccionado, this.userId).subscribe({
      next: () => {
        this.archivoSeleccionado = null;
        this.subiendoPdf = false;
        this.notificationService.success('Corrección enviada al tutor.', '¡Enviado!');
        // Recarga silenciosa: sin spinner, igual que confirmarAprobar
        forkJoin({
          resumen: this.tutoriaService.obtenerResumen(this.tutorId, this.userId),
          fases:   this.tutoriaService.obtenerFases(this.tutorId)
        }).subscribe({
          next: ({ resumen, fases }) => {
            this.resumen = resumen;
            this.fases   = fases;
            const actualizada = fases.find(f => f.id === faseId) ?? null;
            if (actualizada) this.seleccionarFase(actualizada);
          }
        });
      },
      error: (err) => {
        this.subiendoPdf = false;
        this.notificationService.error(err?.error?.error || 'Ocurrió un error', 'Error');
      }
    });
  }

  async confirmarAprobar(): Promise<void> {
    if (!this.faseSeleccionada) return;
    const result = await Swal.fire({
      title: `¿Aprobar Fase ${this.faseSeleccionada.numeroFase}?`,
      text: 'Esta acción no se puede deshacer.',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#22c55e',
      cancelButtonColor: '#6b7280',
      confirmButtonText: 'Sí, aprobar',
      cancelButtonText: 'Cancelar'
    });
    if (!result.isConfirmed) return;

    this.aprobando = true;
    const faseId = this.faseSeleccionada.id;
    this.tutoriaService.aprobarFase(faseId, this.userId, this.nuevoMensaje).subscribe({
      next: () => {
        this.aprobando = false;
        this.nuevoMensaje = '';
        forkJoin({
          resumen: this.tutoriaService.obtenerResumen(this.tutorId, this.userId),
          fases: this.tutoriaService.obtenerFases(this.tutorId)
        }).subscribe({
          next: ({ resumen, fases }) => {
            this.resumen = resumen;
            this.fases = fases;
            const actualizada = fases.find(f => f.id === faseId) ?? null;
            if (actualizada) this.seleccionarFase(actualizada);

            if (resumen.estadoTutoria === 'COMPLETADA') {
              Swal.fire({
                title: '¡Tutoría Completada! 🎉',
                text: 'Se han aprobado las 3 fases de tutoría. El estudiante puede continuar con el proceso de sustentación.',
                icon: 'success',
                confirmButtonColor: '#22c55e',
                confirmButtonText: '¡Excelente!'
              });
            } else {
              this.notificationService.success(`Fase ${this.faseSeleccionada?.numeroFase} aprobada correctamente.`, '¡Aprobada!');
            }
          }
        });
      },
      error: (err) => {
        this.aprobando = false;
        this.notificationService.error(err?.error?.error || 'Ocurrió un error', 'Error');
      }
    });
  }

    enviarMensaje(): void {
    if (this.solicitudSuspendida) {
      this.notificationService.error(
        'Tu solicitud ha sido suspendida. No puedes enviar mensajes hasta que se levante la suspensión.',
        'Solicitud suspendida'
      );
      return;
    }
    if (!this.nuevoMensaje.trim() || !this.faseSeleccionada) return;
    this.enviandoMensaje = true;
    this.tutoriaService.enviarMensaje(
      this.faseSeleccionada.id,
      this.userId,
      this.nuevoMensaje,
      this.tipoMensajeEnviar
    ).subscribe({
      next: (mensaje) => {
        this.faseSeleccionada!.mensajes.push(mensaje);
        this.nuevoMensaje = '';
        this.enviandoMensaje = false;
        this.cdr.markForCheck();
        setTimeout(() => this.scrollToBottom(), 50);
      },
      error: (err) => {
        this.enviandoMensaje = false;
        this.notificationService.error(err?.error?.error || 'Ocurrió un error', 'Error');
      }
    });
  }

  verPdf(faseId: number): void {
    this.tutoriaService.verPdfFase(faseId).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        window.open(url, '_blank');
      },
      error: () => this.notificationService.error('No se pudo cargar el PDF.', 'Error')
    });
  }
}
