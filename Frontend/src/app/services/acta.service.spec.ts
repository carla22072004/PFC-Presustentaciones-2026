import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActaService } from './acta.service';

/**
 * El authInterceptor real reescribe /api/ -> /api/v1/; en estos tests el interceptor no
 * está registrado, así que se verifican las URLs relativas /api/actas/... que produce el
 * servicio antes del interceptor.
 */
describe('ActaService', () => {
  let service: ActaService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ActaService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ActaService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('misActas pide la página del docente con page/size', () => {
    service.misActas(2, 5).subscribe();
    const req = http.expectOne(r => r.url === '/api/actas/mis-actas');
    expect(req.request.params.get('page')).toBe('2');
    expect(req.request.params.get('size')).toBe('5');
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 5 });
  });

  it('buscar omite los filtros vacíos y manda solo los presentes', () => {
    service.buscar({ estado: 'REVISADA', q: '', page: 0, size: 10 }).subscribe();
    const req = http.expectOne(r => r.url === '/api/actas/buscar');
    expect(req.request.params.get('estado')).toBe('REVISADA');
    expect(req.request.params.has('q')).toBe(false);
    expect(req.request.params.has('carrera')).toBe(false);
    req.flush({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 10 });
  });

  it('cambiarEstado hace PATCH al endpoint del acta con nuevoEstado y motivo', () => {
    service.cambiarEstado(7, 'ANULADA', 'motivo x').subscribe();
    const req = http.expectOne('/api/actas/7/estado');
    expect(req.request.method).toBe('PATCH');
    expect(req.request.body).toEqual({ nuevoEstado: 'ANULADA', motivo: 'motivo x' });
    req.flush({});
  });

  it('historial pide GET /api/actas/{id}/historial', () => {
    service.historial(3).subscribe();
    http.expectOne({ method: 'GET', url: '/api/actas/3/historial' }).flush([]);
  });
});
