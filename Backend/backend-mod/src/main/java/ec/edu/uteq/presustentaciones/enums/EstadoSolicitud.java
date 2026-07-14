package ec.edu.uteq.presustentaciones.enums;

public enum EstadoSolicitud {
    CREADA,
    ENVIADA,
    APROBADA,
    RECHAZADA,
    SUSPENDIDA,
    TUTORIA,
    EVALUACION,
    CALIFICADA,
    COMPLETADA;

    public static boolean esSuspendible(EstadoSolicitud estado) {
        return estado != null 
            && estado != COMPLETADA 
            && estado != SUSPENDIDA 
            && estado != RECHAZADA;
    }

    public boolean esSuspendible() {
        return esSuspendible(this);
    }
}
