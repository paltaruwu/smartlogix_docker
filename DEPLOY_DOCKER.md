# Despliegue con Docker en 2 EC2 (reemplazo de XAMPP)

Se agregaron:
- `backend/Dockerfile` + `backend/docker-compose.yml` + `backend/mysql-init/init.sql`
- `frontend/Dockerfile` + `frontend/nginx.conf` + `frontend/docker-compose.yml`

La idea: el EC2 de **backend** levanta MySQL (reemplaza el MySQL de XAMPP) + Eureka + API Gateway + los 3 microservicios + BFF, todo con `docker compose`. El EC2 de **frontend** levanta un solo contenedor nginx sirviendo el build de Vite.

---

## 0. En ambos EC2: instalar Docker

Amazon Linux 2023:
```bash
sudo dnf update -y
sudo dnf install -y docker
sudo systemctl enable --now docker
sudo usermod -aG docker ec2-user
# cierra sesión SSH y vuelve a entrar para que el grupo tome efecto

# plugin docker compose
sudo mkdir -p /usr/local/lib/docker/cli-plugins
sudo curl -SL https://github.com/docker/compose/releases/latest/download/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
sudo chmod +x /usr/local/lib/docker/cli-plugins/docker-compose
docker compose version
```

Ubuntu:
```bash
sudo apt update && sudo apt install -y docker.io docker-compose-plugin
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
```

## 1. Security Groups

- **EC2 backend**: abrir `8089` (API Gateway) y `8761` (Eureka, opcional solo para verlo). El resto de puertos internos (mysql, microservicios) NO hace falta exponerlos a internet.
- **EC2 frontend**: abrir `80`.

## 2. EC2 de BACKEND

```bash
git clone <URL_DE_TU_REPO> smartlogix
cd smartlogix/backend
cp .env.example .env
nano .env   # pon una password real de MySQL
```

Antes de construir, edita el CORS del Gateway con la IP/dominio público real del EC2 del frontend:

`backend/infraestructure/apiGateway/src/main/resources/application.yml`
```yaml
allowedOrigins:
  - "http://IP_PUBLICA_EC2_FRONTEND"
```

Levantar todo:
```bash
docker compose up -d --build
docker compose ps
docker compose logs -f apigateway
```

Verifica que todos los servicios se registraron en Eureka:
`http://IP_PUBLICA_EC2_BACKEND:8761`

## 3. EC2 de FRONTEND

```bash
git clone <URL_DE_TU_REPO> smartlogix
cd smartlogix/frontend
```

Crea un `.env` (para el build, no para runtime, Vite las hornea en el bundle):
```bash
cat > .env << 'EOF'
VITE_API_URL=http://IP_PUBLICA_EC2_BACKEND:8089
VITE_AZURE_CLIENT_ID=<client id del App Registration en Entra ID>
VITE_AZURE_TENANT_ID=<tenant id>
VITE_AZURE_REDIRECT_URI=http://IP_PUBLICA_EC2_FRONTEND
EOF
```

`VITE_AZURE_REDIRECT_URI` debe estar registrado EXACTO como Redirect URI tipo **SPA** en Azure Entra ID.

Levantar:
```bash
docker compose up -d --build
docker compose ps
```

Abre `http://IP_PUBLICA_EC2_FRONTEND` en el navegador.

## 4. Comandos útiles

```bash
docker compose logs -f <servicio>      # ver logs
docker compose restart <servicio>      # reiniciar uno
docker compose down                    # apagar todo (no borra datos de mysql, el volumen persiste)
docker compose down -v                 # apagar y borrar también la data de mysql
docker compose up -d --build           # reconstruir tras un cambio de código
```

## 5. Notas

- Ya no necesitas XAMPP ni MySQL local: el contenedor `mysql` del compose crea automáticamente `inventory_db`, `order_db` y `shipment_db` la primera vez (via `mysql-init/init.sql`), y Hibernate crea las tablas (`ddl-auto=update/create`).
- `keyCloakAdapter` y `springBootAdmin1` no se incluyeron en el compose porque el README indica que el login ya usa Azure Entra ID/MSAL, no Keycloak. Si los necesitas, agrégalos al `docker-compose.yml` del backend siguiendo el mismo patrón que los demás servicios (mismo `Dockerfile`, cambiando `MODULE`).
- Si prefieres un dominio + HTTPS en vez de IP pública en puerto 80/8089, lo siguiente sería poner un nginx/Caddy o un ALB delante con certificado, pero eso ya es un paso aparte del dockerizado.
