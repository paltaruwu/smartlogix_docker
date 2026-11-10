# Despliegue en la EC2 con Kafka y RabbitMQ

Pasos para levantar en la EC2 (Amazon Linux 2023, ~/smartlogix_docker) el proyecto completo:
tienda + microservicios + clúster RabbitMQ + clúster Kafka + Kafka-UI.

## Qué se levanta

| Grupo | Contenedores |
|---|---|
| Base y mensajería RabbitMQ | mysql, rabbitmq1, rabbitmq2 |
| Infraestructura | eureka, apigateway |
| Servicios con **Kafka** | order, inventory, shipment, kafkaadmin |
| Servicios con **RabbitMQ** | technician, payment, notification, rabbitadmin |
| Tienda | bff, frontend |
| Clúster **Kafka** | zookeeper1-3, kafka1-3, kafka-ui (+ kafka-init, que crea los tópicos y termina solo) |

Son 22 contenedores en marcha. Con todo arriba usan unos 5 GB de RAM.

## 1. Antes de empezar (disco y memoria)

La EC2 tiene 20 GB de disco. Construir las imágenes llena el caché de Docker, así que limpia antes:

```bash
df -h /
sudo docker builder prune -af
sudo docker image prune -f
free -h
```

Si `df -h /` muestra menos de ~12 GB libres, agranda el volumen EBS (en la consola de AWS: EC2 > Volumes > Modify volume, 40 GB)
y luego en la EC2:

```bash
sudo growpart /dev/nvme0n1 1
sudo xfs_growfs -d /
```

## 2. Traer el código

```bash
cd ~/smartlogix_docker
git pull
```

El `.env` con `PUBLIC_URL` sigue ahí (no se sube a git). Si cambió el túnel de Cloudflare, actualízalo (ver el paso 5).

## 3. Levantar todo

Construir de a pocos servicios a la vez evita quedarse sin memoria compilando Maven:

```bash
export COMPOSE_PARALLEL_LIMIT=2
sudo -E docker-compose \
  -f docker-compose.cloud.yml \
  -f docker-compose.kafka.yml \
  -f docker-compose.kafka-ui.yml \
  up -d --build
```

Tarda varios minutos la primera vez (compila 11 servicios y baja las imágenes de Kafka, ~2 GB).

## 4. Comprobar que todo está bien

```bash
sudo docker ps --format "table {{.Names}}\t{{.Status}}"
sudo docker logs sl-kafka-init | tail -15
```

- `sl-kafka1`, `sl-kafka2` y `sl-kafka3` deben decir `(healthy)`.
- `kafka-init` debe terminar describiendo los tópicos `order-events`, `shipment-events` e `inventory-events` (con sus `.DLT`). Si ya existían, no imprime «Created topic»: es normal.
- Eureka (puerto 8761) debe listar los servicios; los de Java tardan 1 a 2 minutos en registrarse.

Tópicos y grupos desde la EC2:

```bash
sudo docker exec sl-kafka1 kafka-topics --bootstrap-server kafka1:29092 --list
sudo docker exec sl-kafka1 kafka-consumer-groups --bootstrap-server kafka1:29092 --list
```

Deben verse `order-group`, `inventory-group` y `shipment-group`.

## 5. Túneles de Cloudflare (como siempre)

| Qué | Túnel hacia |
|---|---|
| La tienda | `http://localhost:80` |
| Panel de RabbitMQ | `http://localhost:15672` |
| **Kafka-UI (nuevo)** | `http://localhost:8085` |

```bash
cd ~/smartlogix_docker/frontend
nohup ./cloudflared tunnel --url http://localhost:8085 > /tmp/tunnel-kafkaui.log 2>&1 &
```

La URL del túnel de la tienda va en `.env` como `PUBLIC_URL` y se registra en Azure como Redirect URI (sin `/` ni ruta al final).
Después: `sudo docker-compose -f docker-compose.cloud.yml up -d --build frontend`.

## 6. Colas viejas de RabbitMQ (solo si ya tenías datos de antes)

Si la EC2 ya corrió una versión anterior, su RabbitMQ conserva las colas de order, shipment e inventory, que ya no se usan.
Las 3 colas vigentes son `payment.request`, `technician.payment.result` y `notification`. Para quitar las viejas
(esto borra los mensajes que tengan dentro):

```bash
for q in order.notification.queue order.notification.dlq shipment.dispatch.queue shipment.dispatch.dlq inventory.restock.queue inventory.restock.dlq; do
  curl -s -u admin:admin123 -X DELETE "http://localhost:15672/api/queues/%2F/$q"
done
```

## 7. Si algo falla

- **Un contenedor se reinicia o desaparece:** probablemente falta memoria. Revisa `free -h` y `sudo docker stats --no-stream`.
- **Kafka no queda `healthy`:** `sudo docker logs sl-kafka1 | tail -30`. Los Zookeeper deben estar arriba primero.
- **Los pedidos aprobados dan 503:** Kafka no está disponible. Es intencional: el pedido vuelve a `CREADO` en lugar de quedar aprobado sin envío.
- **Después de reiniciar el lab de AWS:** los contenedores vuelven solos; rehaz los túneles y el `PUBLIC_URL` (paso 5).

## Demostración (qué mostrar)

1. **Kafka-UI** (túnel 8085): Topics (`order-events`, `shipment-events` e `inventory-events` con 3 particiones y 3 réplicas), Brokers (3), Consumers (4 grupos, con lag y offsets: `order-group`, `inventory-group`, `shipment-group` e `inventory-stock-group`).
2. **RabbitMQ** (túnel 15672): Queues (3 colas con sus DLQ), Exchanges (`smartlogix.direct` y `smartlogix.topic`), Admin (2 nodos).
3. **Flujo:** crear un pedido en la tienda, aprobarlo desde Admin y ver el evento en Kafka-UI; contratar un técnico y ver pasar los mensajes por RabbitMQ.
