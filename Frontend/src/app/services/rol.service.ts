import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface Rol {
    id?: number;
    codigo: string;
    nombre: string;
    usuariosAsignados?: number;
    permisos?: string[];
}

export interface Permiso {
    id: number;
    codigo: string;
    nombre: string;
    categoria: string;
    descripcion: string;
}

@Injectable({ providedIn: 'root' })
export class RolService {
    private rolesUrl = 'http://127.0.0.1:8080/api/roles';
    private permisosUrl = 'http://127.0.0.1:8080/api/permisos';

    constructor(private http: HttpClient) {}

    listarRoles(): Observable<Rol[]> {
        return this.http.get<Rol[]>(this.rolesUrl);
    }

    crearRol(rol: { codigo: string; nombre: string }): Observable<Rol> {
        return this.http.post<Rol>(this.rolesUrl, rol);
    }

    renombrarRol(id: number, nombre: string): Observable<Rol> {
        return this.http.put<Rol>(`${this.rolesUrl}/${id}`, { nombre });
    }

    eliminarRol(id: number): Observable<void> {
        return this.http.delete<void>(`${this.rolesUrl}/${id}`);
    }

    listarPermisos(): Observable<Permiso[]> {
        return this.http.get<Permiso[]>(this.permisosUrl);
    }

    actualizarPermisosDeRol(rolId: number, codigosPermisos: string[]): Observable<string[]> {
        return this.http.put<string[]>(`${this.permisosUrl}/rol/${rolId}`, codigosPermisos);
    }
}
