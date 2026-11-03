param([switch]$NoPause)
# Pruebas CP-RMQ-001, 004, 005 y 006 contra el microservicio rabbitadmin (puerto 8090)
# y el panel de RabbitMQ Management (http://localhost:15672, admin / admin123).
# En cada PAUSA abre el panel, toma el pantallazo y presiona Enter.

$ErrorActionPreference = "Stop"
$admin = "http://localhost:8090/rabbit-admin/v1"
$mgmt  = "http://localhost:15672/api"
$cred  = [Convert]::ToBase64String([Text.Encoding]::ASCII.GetBytes("admin:admin123"))
$h     = @{ Authorization = "Basic $cred" }

function Api($method, $url, $body) {
    $p = @{ Method = $method; Uri = $url; ContentType = "application/json" }
    if ($body) { $p.Body = ($body | ConvertTo-Json) }
    Invoke-RestMethod @p
}
function Mgmt($path) { Invoke-RestMethod -Uri "$mgmt/$path" -Headers $h }
function Pausa($msg) { Write-Host "`n>>> PAUSA - $msg" -ForegroundColor Yellow; if (-not $NoPause) { Read-Host "Enter para continuar" | Out-Null } }

# ---------- CP-RMQ-001: Features D / DLX / DLK ----------
Write-Host "`n=== CP-RMQ-001: cola con DLX y DLK ===" -ForegroundColor Cyan
$q = Mgmt "queues/%2F/payment.request.queue"
Write-Host "durable: $($q.durable)"
Write-Host "argumentos:"; $q.arguments | Format-List
Pausa "Panel > Queues > fila payment.request.queue: columna Features (D, DLX, DLK). Pantallazo."

# ---------- Preparacion de asistencia.queue + asistencia.queue.dlq ----------
Write-Host "`n=== Preparando asistencia.queue (con DLQ) ===" -ForegroundColor Cyan
Api POST "$admin/queues" @{ name = "asistencia.queue"; durable = $true; withDlq = $true } | Out-Null
# El endpoint /publish exige un exchange no vacio, asi que el mensaje previo se publica via API de management
$msg = @{ properties = @{}; routing_key = "asistencia.queue.dlq"; payload = '{"evento":"MENSAJE_PREVIO"}'; payload_encoding = "string" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri "$mgmt/exchanges/%2F/amq.default/publish" -Headers $h -ContentType "application/json" -Body $msg | Out-Null
Start-Sleep 2
Pausa "Panel > Queues: se ven asistencia.queue y asistencia.queue.dlq (Ready 1 en la dlq). Pantallazo."

# ---------- CP-RMQ-005: exchange topic + binding ----------
Write-Host "`n=== CP-RMQ-005: exchange topic y binding ===" -ForegroundColor Cyan
Api POST "$admin/exchanges" @{ name = "asistencia.exchange"; type = "topic"; durable = $true } | Out-Null
Api POST "$admin/bindings" @{ queueName = "asistencia.queue"; exchangeName = "asistencia.exchange"; routingKey = "asistencia.evento.*" } | Out-Null
$ex = Mgmt "exchanges/%2F/asistencia.exchange"
Write-Host "exchange: type=$($ex.type) durable=$($ex.durable)"
foreach ($x in @(Mgmt "exchanges/%2F/asistencia.exchange/bindings/source")) { Write-Host "binding: $($x.source) -> $($x.destination)  key=$($x.routing_key)" }
Pausa "Panel > Exchanges > asistencia.exchange > Bindings (topic, D, asistencia.evento.*). Pantallazo."

# ---------- CP-RMQ-006: eliminar binding ----------
Write-Host "`n=== CP-RMQ-006: eliminar binding ===" -ForegroundColor Cyan
Api DELETE "$admin/bindings" @{ queueName = "asistencia.queue"; exchangeName = "asistencia.exchange"; routingKey = "asistencia.evento.*" } | Out-Null
$b = Mgmt "exchanges/%2F/asistencia.exchange/bindings/source"
Write-Host "bindings restantes: $(@($b).Count)"
Api POST "$admin/publish" @{ exchange = "asistencia.exchange"; routingKey = "asistencia.evento.test"; payload = @{ evento = "TEST" } } | Out-Null
Start-Sleep 6
$qa = Mgmt "queues/%2F/asistencia.queue"
Write-Host "mensajes en asistencia.queue tras publicar sin binding: $($qa.messages)  (esperado 0)"
Pausa "Panel > Exchanges > asistencia.exchange: sin bindings; Queues: asistencia.queue Ready 0. Pantallazo."

# ---------- CP-RMQ-004: eliminar cola sin afectar su DLQ ----------
Write-Host "`n=== CP-RMQ-004: eliminar asistencia.queue ===" -ForegroundColor Cyan
Api DELETE "$admin/queues/asistencia.queue" | Out-Null
Start-Sleep 1
Start-Sleep 5
$d = Mgmt "queues/%2F/asistencia.queue.dlq"; Write-Host "cola: $($d.name)  mensajes=$($d.messages)"
try { Mgmt "queues/%2F/asistencia.queue" | Out-Null; Write-Host "asistencia.queue AUN EXISTE" } catch { Write-Host "asistencia.queue eliminada (404)" }
Pausa "Panel > Queues: solo queda asistencia.queue.dlq con su mensaje. Pantallazo."

# Limpieza
Api DELETE "$admin/exchanges/asistencia.exchange" | Out-Null
Api DELETE "$admin/queues/asistencia.queue.dlq" | Out-Null
Write-Host "`nLimpieza hecha. Fin." -ForegroundColor Green
