# --- Etapa 1: build de la app React/Vite ---
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# Vite "hornea" estas variables dentro del bundle en tiempo de BUILD,
# por eso van como build-args (no sirve pasarlas solo en runtime).
ARG VITE_API_URL
ARG VITE_AZURE_CLIENT_ID
ARG VITE_AZURE_TENANT_ID
ARG VITE_AZURE_REDIRECT_URI
ENV VITE_API_URL=${VITE_API_URL} \
    VITE_AZURE_CLIENT_ID=${VITE_AZURE_CLIENT_ID} \
    VITE_AZURE_TENANT_ID=${VITE_AZURE_TENANT_ID} \
    VITE_AZURE_REDIRECT_URI=${VITE_AZURE_REDIRECT_URI}

RUN npm run build

# --- Etapa 2: servir el build estático con nginx ---
FROM nginx:1.27-alpine
RUN apk add --no-cache openssl && \
    mkdir -p /etc/nginx/certs && \
    openssl req -x509 -nodes -days 365 -newkey rsa:2048 \
      -keyout /etc/nginx/certs/selfsigned.key \
      -out /etc/nginx/certs/selfsigned.crt \
      -subj "/CN=smartlogix"
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80 443
