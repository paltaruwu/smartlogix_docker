#!/bin/bash
# Crea los tópicos de SmartLogix en el clúster Kafka (3 particiones, 3 réplicas).
# Es idempotente: --if-not-exists evita errores si ya existen.
# También se puede ejecutar a mano:  docker exec sl-kafka1 bash /create-topics.sh

set -e
BOOTSTRAP="${BOOTSTRAP:-kafka1:29092,kafka2:29092,kafka3:29092}"

create() {
  local topic="$1"; shift
  kafka-topics --bootstrap-server "$BOOTSTRAP" --create --if-not-exists \
    --topic "$topic" --partitions 3 --replication-factor 3 "$@"
}

# Eventos del ciclo de vida del pedido (stream de eventos): se conservan 7 días
create order-events \
  --config cleanup.policy=delete \
  --config retention.ms=604800000 \
  --config min.insync.replicas=2

# Estado de envíos: tópico compactado, solo importa el último estado de cada pedido (clave = orderId)
create shipment-events \
  --config cleanup.policy=compact \
  --config min.cleanable.dirty.ratio=0.1 \
  --config segment.ms=3600000 \
  --config min.insync.replicas=2

# Cambios de stock (reposiciones desde el panel Admin): se conservan 7 días
create inventory-events \
  --config cleanup.policy=delete \
  --config retention.ms=604800000 \
  --config min.insync.replicas=2

# Dead Letter Topics: mensajes que fallaron tras los reintentos, se conservan 14 días para revisión
create order-events.DLT \
  --config cleanup.policy=delete \
  --config retention.ms=1209600000
create shipment-events.DLT \
  --config cleanup.policy=delete \
  --config retention.ms=1209600000
create inventory-events.DLT \
  --config cleanup.policy=delete \
  --config retention.ms=1209600000

echo "Tópicos creados:"
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic order-events
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic shipment-events
kafka-topics --bootstrap-server "$BOOTSTRAP" --describe --topic inventory-events
