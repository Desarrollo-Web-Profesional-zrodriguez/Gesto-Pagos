# Etapa 1: Compilación de producción con Node 20 LTS
FROM node:20-alpine AS builder
WORKDIR /app

COPY package.json package-lock.json ./
RUN npm ci

COPY . .
ARG VITE_API_BASE_URL=https://gesto-pagos-api.onrender.com
ENV VITE_API_BASE_URL=${VITE_API_BASE_URL}

RUN npm run build

# Etapa 2: Servidor estático Nginx optimizado para Render (soporta PORT dinámico)
FROM nginx:1.27-alpine
ENV PORT=80
ENV NGINX_ENVSUBST_OUTPUT_DIR=/etc/nginx/conf.d

RUN rm -f /etc/nginx/conf.d/default.conf
COPY nginx.conf.template /etc/nginx/templates/default.conf.template
COPY --from=builder /app/dist /usr/share/nginx/html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]
