# SmartLogix - E-Commerce Cloud Native Platform

Plataforma distribuida de comercio electrónico y gestión logística desarrollada con arquitectura de **Microservicios (Java 17 / Spring Boot)**, frontend modular en **React con TypeScript y Vite**, mensajería asíncrona tolerante a fallos mediante **RabbitMQ Cluster**, y seguridad perimetral basada en **OAuth 2.0 / JWT con Microsoft Entra ID** integrada en un **API Gateway**.

---

## 📋 Descripción del Sistema

**SmartLogix** gestiona el ciclo de vida completo de compras, inventario y despachos de una tienda de productos comerciales. La solución desacopla sus dominios de negocio para garantizar escalabilidad horizontal, consistencia eventual y tolerancia a caídas:

* **Gestión de Catálogo e Inventario:** Consulta de stock, control de existencias y reabastecimiento en almacén.
* **Procesamiento de Órdenes:** Recepción de solicitudes de compra, verificación y checkout integrado.
* **Logística y Despacho:** Asignación de envíos y seguimiento de pedidos procesados.
* **Panel de Administración (Back-office):** Supervisión y mantenimiento del inventario por usuarios administradores autorizados.

---

## 🏛️ Arquitectura de la Solución

```
                     ┌──────────────────────────────────────────────────┐
                     │          Microsoft Entra ID (Azure IDaaS)        │
                     └────────────────────────┬─────────────────────────┘
                                              │ Emite JWT (Bearer)
                                              ▼
┌──────────────────┐               ┌─────────────────────────────────────┐
│  React Frontend  │──────────────>│         Spring Cloud Gateway        │
│ (Catálogo/Admin) │ HTTPS / Axios │ (Filtros JWT, CORS, Enrutamiento)   │
└──────────────────┘               └──────────────────┬──────────────────┘
                                                      │
         ┌───────────────────┬────────────────────────┼────────────────────────┐
         │                   │                        │                        │
         ▼                   ▼                        ▼                        ▼
 ┌───────────────┐   ┌───────────────┐        ┌───────────────┐        ┌───────────────┐
 │   BFF Store   │   │   Inventory   │        │     Order     │        │   Shipment    │
 │ (Agregador)   │   │ (Stock & Mov) │        │ (Checkout/Ord)│        │  (Despachos)  │
 └───────┬───────┘   └───────┬───────┘        └───────┬───────┘        └───────┬───────┘
         │                   │                        │                        │
         │                   └──────────┐      ┌──────┴───────┐      ┌─────────┘
         │                              ▼      ▼              ▼      ▼
         │                      ┌──────────────────────────────────────────────┐
         │                      │           RabbitMQ Cluster (AMQP)            │
         │                      │    orders.exchange / inventory.exchange      │
         └─────────────────────>│       DLQ / Consumer Acknowledgements        │
                                └──────────────────────┬───────────────────────┘
                                                       ▲
                                                       │
                                            ┌──────────┴──────────┐
                                            │     RabbitAdmin     │
                                            │ (Gestión REST AMQP) │
                                            └─────────────────────┘
```

---

## 📦 Componentes del Sistema

### 1. Frontend Web (`/frontend`)
* Desarrollado en **React 18 + Vite + TypeScript**.
* Vistas de cliente para exploración de catálogo, carrito de compras y pasarela de checkout.
* Vista protegida **`/admin`** para auditoría y modificación del inventario.
* Integración con la librería `@azure/msal-browser` para autenticación con cuentas institucionales / corporativas.

### 2. Microservicios de Negocio (`/backend/microservices`)
* **`bff` (Backend For Frontend):** Agregador y orquestador liviano que centraliza las peticiones de la tienda hacia los servicios internos.
* **`inventory`:** Administra existencias de productos (`Product`), movimientos de bodega (`InventoryMovement`) y sincronización de stock post-compra.
* **`order`:** Controla la creación de órdenes (`Order`, `OrderDetail`), validando montos y estados de transacción.
* **`shipment`:** Gestiona el despacho físico de pedidos (`Shipment`, `ShipmentOrder`), asignando guías de transporte una vez confirmada la orden.
* **`rabbitadmin`:** Servicio utilitario que expone endpoints REST administrativos para la creación, consulta, purga y eliminación dinámica de recursos AMQP (colas, exchanges, bindings) con validación mediante DTOs.

### 3. Infraestructura y Soporte (`/backend/infraestructure`)
* **`apiGateway`:** Punto de entrada perimetral con `GlobalPreFiltering`, `GlobalPostFiltering` y `AuthenticationFilterinig` para autorización de tokens.
* **`eurekaServer`:** Servidor de registro y descubrimiento dinámico de microservicios.
* **`springBootAdmin1`:** Dashboard de monitoreo en tiempo real del estado de salud (Spring Boot Actuator) de cada servicio.

---

## 🐇 Arquitectura de Mensajería con RabbitMQ

La integración de RabbitMQ desacopla la comunicación entre los tres microservicios principales mediante eventos de dominio asíncronos:

```
  [Order Service]
         │
         │ (Publica OrderCreatedEvent)
         ▼
[orders.exchange] (Direct / Topic)
   ├─── Routing Key: "order.created" ───> [inventory.queue] ──> InventoryService (Descuenta Stock)
   └─── Routing Key: "order.created" ───> [shipment.queue]  ──> ShipmentService  (Genera Despacho)
```

### Características Técnicas Implementadas:
1. **Clúster de Alta Disponibilidad:** 
   * Configuración de 2 nodos (`rabbitmq-1` y `rabbitmq-2`) orquestados con Docker Compose y Erlang Cookie unificado (`secret_cookie_value`).
   * Autodescubrimiento clásico vía `rabbitmq.conf` y precarga automatizada de usuarios, colas y permisos mediante `definitions.json`.
2. **Clases `@Configuration` Dedicadas:**
   * Separación estricta entre la lógica de negocio y la infraestructura de colas. La declaración de beans `Queue`, `TopicExchange`/`DirectExchange` y `Binding` reside exclusivamente en paquetes de configuración (`RabbitMQConfig.java`).
3. **Consumidores con `@RabbitListener` y Concurrencia:**
   * Listeners modulares organizados por dominio funcional (`InventoryRestockListener`, `OrderNotificationListener`, `ShipmentDispatchListener`).
   * Pools de consumidores concurrentes configurados mediante `SimpleRabbitListenerContainerFactory`.
4. **Resiliencia, ACKs y Dead Letter Queues (DLQ):**
   * Confirmación explícita/gestionada de mensajes (ACK / NACK).
   * Redirección automática de mensajes con errores no recuperables hacia colas de descarte (`orders.dlq`, `inventory.dlq`) para prevenir pérdida de datos y evitar bloqueos en el hilo principal de procesamiento.

---

## 🔐 Seguridad y Autenticación con Microsoft Entra ID

El sistema utiliza **Microsoft Entra ID (Azure Active Directory)** como proveedor de identidad centralizado (IDaaS) para proteger las operaciones críticas del negocio:

* **Flujo OAuth 2.0 / OpenID Connect (PKCE):** 
  * Los administradores inician sesión desde el frontend React contra el tenant corporativo de Microsoft.
  * Una vez autenticado, el cliente almacena el token de acceso JWT de forma segura en `sessionStorage`.
* **Protección Perimetral en API Gateway:**
  * Toda solicitud dirigida a recursos protegidos (ej. `/api/inventory/v1/**` o `/api/rabbit-admin/**`) debe incluir la cabecera `Authorization: Bearer <JWT>`.
  * Los filtros de seguridad del Gateway (`AuthenticationFilterinig`) validan la firma criptográfica, la expiración, el emisor (`Issuer URL`) y la audiencia (`Audience / Client ID`) del token contra Entra ID.
  * Solicitudes anónimas o con tokens inválidos son rechazadas en el perímetro con respuestas HTTP `401 Unauthorized`.
* **Rutas Públicas vs. Rutas Protegidas:**
  * Acceso público permitido únicamente para la consulta visual de productos y checkout regular por clientes.
  * Acceso restringido exclusivamente a usuarios con rol o asignación de administrador para la gestión de catálogo y administración de colas.

---

## 🚀 Despliegue con Docker Compose

El repositorio provee archivos de composición según el entorno requerido:

### 1. Despliegue Local (`docker-compose.local.yml`)
Levanta la base de datos MySQL, el clúster RabbitMQ y los microservicios:

```bash
# Iniciar servicios base y clúster RabbitMQ
docker compose -f docker-compose.local.yml up -d
```

### 2. Despliegue en Cloud / AWS EC2 (`docker-compose.cloud.yml`)
Optimizado para despliegue productivo en instancias EC2:

```bash
docker compose -f docker-compose.cloud.yml up -d --build
```

### 3. Puertos de Acceso

| Servicio | Puerto Host | Descripción |
|---|---|---|
| **Frontend Web** | `80` / `5173` | Interfaz de Usuario y Panel Admin |
| **API Gateway** | `8080` / `8089` | Puerta de enlace perimetral segura |
| **RabbitMQ Nodo 1 (AMQP)** | `5672` | Conexión de microservicios |
| **RabbitMQ Nodo 1 (UI)** | `15672` | Management Dashboard |
| **RabbitMQ Nodo 2 (AMQP)** | `5673` | Réplica AMQP |
| **RabbitMQ Nodo 2 (UI)** | `15673` | Management Dashboard Réplica |
| **Eureka Server** | `8761` | Servidor de Registro y Descubrimiento |
| **Spring Boot Admin** | `9090` | Monitor de métricas y Actuator |

---

## 🧪 Verificación de Endpoints y Clúster

### Comprobar Clúster RabbitMQ:
```bash
docker exec rabbitmq-1 rabbitmqctl cluster_status
```
