import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ReporteService } from './reporte.service';

describe('ReporteService', () => {
  let service: ReporteService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [ReporteService, provideHttpClient(), provideHttpClientTesting()]
    });
    service = TestBed.inject(ReporteService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('resumen pasa los filtros de fecha/carrera como query params', () => {
    service.resumen({ desde: '2025-01-01', hasta: '2025-12-31', carrera: 'Software' }).subscribe();
    const req = http.expectOne(r => r.url === '/api/reportes/resumen');
    expect(req.request.params.get('desde')).toBe('2025-01-01');
    expect(req.request.params.get('hasta')).toBe('2025-12-31');
    expect(req.request.params.get('carrera')).toBe('Software');
    req.flush({});
  });

  it('actividadDocente es un GET sin parámetros', () => {
    service.actividadDocente().subscribe();
    const req = http.expectOne('/api/reportes/actividad-docente');
    expect(req.request.method).toBe('GET');
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });

  it('solicitudesPorEstado sin filtros no añade query params', () => {
    service.solicitudesPorEstado().subscribe();
    const req = http.expectOne(r => r.url === '/api/reportes/solicitudes-por-estado');
    expect(req.request.params.keys().length).toBe(0);
    req.flush([]);
  });
});
