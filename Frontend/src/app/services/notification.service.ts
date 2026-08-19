import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor() { }

  // sweetalert2 se carga en un chunk aparte (no en el bundle inicial) porque solo se
  // necesita cuando realmente se muestra una notificación.
  private async swal() {
    return (await import('sweetalert2')).default;
  }

  // Cuadro para éxito (como el registro de tesis)
  async success(message: string, title: string = '¡Operación Exitosa!') {
    const Swal = await this.swal();
    Swal.fire({
      title: title,
      text: message,
      icon: 'success',
      confirmButtonColor: '#28a745', // Un verde académico
      timer: 2500,
      showClass: {
        popup: 'animate__animated animate__fadeInDown'
      }
    });
  }

  // Cuadro para errores
  async error(message: string, title: string = 'Oops...') {
    const Swal = await this.swal();
    Swal.fire({
      title: title,
      text: message,
      icon: 'error',
      confirmButtonColor: '#d33'
    });
  }
}
