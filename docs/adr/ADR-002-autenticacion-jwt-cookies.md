# ADR-002: Esquema de Autenticación JWT con Cookies de Seguridad HTTP-Only

**Estado:** Aceptado  
**Fecha:** 2026-07-18  
**Decisores:** Equipo de Seguridad y Backend UTEQ  

## Contexto
Es necesario asegurar la autenticación de usuarios sin mantener estado de sesión en el servidor (Stateless), previniendo ataques de tipo Cross-Site Scripting (XSS) y Cross-Site Request Forgery (CSRF).

## Decisión
Implementar autenticación basada en JSON Web Tokens (JJWT 0.12.5) combinando el encabezado estándar `Authorization: Bearer <token>` con cookies de seguridad `HTTP-Only`, `SameSite=Lax` y marcas de expiración deterministas (24 horas).

## Consecuencias
- **Positivas:** Protección nativa contra el robo de tokens desde JavaScript (XSS); soporte nativo para consumidores SPA Angular, móviles y Postman; sesiones totalmente Stateless.
- **Negativas:** Obliga a gestionar la invalidación de tokens expirados en cliente o lista negra en Redis si se requiere cierre forzado instantáneo.
