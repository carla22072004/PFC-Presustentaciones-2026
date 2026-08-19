# Stage 1: build de produccion del SPA Angular
FROM node:20-alpine AS build
WORKDIR /app
COPY package.json package-lock.json ./
RUN npm ci
COPY . .
RUN npx ng build --configuration production

# Stage 2: servir el build con nginx, proxy inverso a /api/v1 y /actuator
FROM nginx:alpine
COPY --from=build /app/dist/presustentaciones-frontend/browser /usr/share/nginx/html
COPY nginx.railway.conf.template /etc/nginx/templates/default.conf.template

# Railway inyecta $PORT dinamicamente; BACKEND_INTERNAL_URL se configura como variable
# de entorno del servicio en el dashboard de Railway (ver docs/despliegue/DEPLOYMENT.md).
ENV PORT=8080
EXPOSE 8080
