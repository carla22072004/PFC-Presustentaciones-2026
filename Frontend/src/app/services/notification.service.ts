import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  constructor() { }

  // Cuadro para éxito (como el registro de tesis)
  success(message: string, title: string = '¡Operación Exitosa!') {
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
  error(message: string, title: string = 'Oops...') {
    Swal.fire({
      title: title,
      text: message,
      icon: 'error',
      confirmButtonColor: '#d33'
    });
  }
}
