import { useCallback, useEffect, useState } from "react";
import { useAuth } from "../contexts/AuthContext";
import { useCar } from "../contexts/CartContext";
import {
    changeRequestStatus,
    getNotifications,
    getPayments,
    getServiceRequests,
    type AppNotification,
    type PaymentRecord,
    type ServiceRequest,
} from "../services/technicianService";

type Tab = "solicitudes" | "pagos" | "notificaciones";

const statusColor: Record<string, string> = {
    SOLICITADO: "bg-warning text-dark",
    PAGADO: "bg-info text-dark",
    EN_CURSO: "bg-primary",
    FINALIZADO: "bg-success",
    CANCELADO: "bg-danger",
    APROBADO: "bg-success",
    RECHAZADO: "bg-danger",
};

const formatDate = (iso: string) => new Date(iso).toLocaleString("es-CL");

export const AdminServicios = () => {
    const { getAccessToken } = useAuth();
    const { formatCLP } = useCar();

    const [tab, setTab] = useState<Tab>("solicitudes");
    const [requests, setRequests] = useState<ServiceRequest[]>([]);
    const [payments, setPayments] = useState<PaymentRecord[]>([]);
    const [notifications, setNotifications] = useState<AppNotification[]>([]);
    const [error, setError] = useState<string | null>(null);

    const load = useCallback(async () => {
        try {
            const token = await getAccessToken();
            if (!token) return;
            const [r, p, n] = await Promise.all([
                getServiceRequests(token),
                getPayments(token),
                getNotifications(token),
            ]);
            setRequests([...r].reverse());
            setPayments([...p].reverse());
            setNotifications(n);
            setError(null);
        } catch {
            setError("No se pudieron cargar los datos. Revisa que los microservicios estén arriba.");
        }
    }, [getAccessToken]);

    useEffect(() => {
        load();
        const timer = setInterval(load, 5000);
        return () => clearInterval(timer);
    }, [load]);

    const handleStatus = async (id: number, status: string) => {
        try {
            const token = await getAccessToken();
            if (!token) return;
            await changeRequestStatus(id, status, token);
            load();
        } catch (e) {
            alert(e instanceof Error ? e.message : "No se pudo cambiar el estado.");
        }
    };

    return (
        <div className="container mt-4 mb-5">
            <h2 className="fw-bold text-center mb-4" style={{ color: "#0077b6" }}>🔧 Servicios Técnicos</h2>

            <ul className="nav nav-pills justify-content-center gap-2 mb-4">
                {([
                    ["solicitudes", `Solicitudes (${requests.length})`],
                    ["pagos", `Pagos (${payments.length})`],
                    ["notificaciones", `Notificaciones (${notifications.length})`],
                ] as [Tab, string][]).map(([key, label]) => (
                    <li className="nav-item" key={key}>
                        <button className={`nav-link fw-semibold ${tab === key ? "active" : ""}`} onClick={() => setTab(key)}>
                            {label}
                        </button>
                    </li>
                ))}
            </ul>

            {error && <div className="alert alert-danger">{error}</div>}

            <div className="glass-container">
                {tab === "solicitudes" && (
                    <>
                        {requests.length === 0 && <p className="mb-0">No hay solicitudes todavía.</p>}
                        {requests.map((r) => (
                            <div key={r.id} className="glossy-card p-3 mb-3 border-0">
                                <div className="d-flex justify-content-between align-items-center mb-2">
                                    <strong style={{ color: "#0077b6" }}>#{r.id} · {r.technicianName} ({r.specialty})</strong>
                                    <span className={`badge ${statusColor[r.status]}`}>{r.status}</span>
                                </div>
                                <p className="mb-1 small text-muted"><strong>Cliente:</strong> {r.client} · {r.address}</p>
                                <p className="mb-1 small text-muted"><strong>Problema:</strong> {r.description}</p>
                                <p className="mb-2 small text-muted">
                                    <strong>Monto:</strong> {formatCLP(r.amount)} · <strong>Pago:</strong> {r.paymentStatus}
                                    {r.paymentReason ? ` (${r.paymentReason})` : ""} · {formatDate(r.createdAt)}
                                </p>
                                <div className="d-flex gap-2">
                                    {r.status === "PAGADO" && (
                                        <button className="btn btn-sm btn-bubble" onClick={() => handleStatus(r.id, "EN_CURSO")}>
                                            🚗 Técnico en camino
                                        </button>
                                    )}
                                    {(r.status === "PAGADO" || r.status === "EN_CURSO") && (
                                        <button className="btn btn-sm btn-success" onClick={() => handleStatus(r.id, "FINALIZADO")}>
                                            ✅ Finalizar
                                        </button>
                                    )}
                                    {(r.status === "SOLICITADO" || r.status === "PAGADO") && (
                                        <button className="btn btn-sm btn-outline-danger" onClick={() => handleStatus(r.id, "CANCELADO")}>
                                            Cancelar
                                        </button>
                                    )}
                                </div>
                            </div>
                        ))}
                    </>
                )}

                {tab === "pagos" && (
                    <>
                        {payments.length === 0 && <p className="mb-0">No hay pagos todavía.</p>}
                        {payments.length > 0 && (
                            <div className="table-responsive">
                                <table className="table table-sm align-middle mb-0">
                                    <thead>
                                        <tr><th>ID</th><th>Referencia</th><th>Cliente</th><th>Tarjeta</th><th>Monto</th><th>Estado</th><th>Detalle</th></tr>
                                    </thead>
                                    <tbody>
                                        {payments.map((p) => (
                                            <tr key={p.id}>
                                                <td>{p.id}</td>
                                                <td>{p.referenceType} #{p.referenceId}</td>
                                                <td>{p.client}</td>
                                                <td>•••• {p.cardLast4}</td>
                                                <td>{formatCLP(p.amount)}</td>
                                                <td><span className={`badge ${statusColor[p.status]}`}>{p.status}</span></td>
                                                <td className="small text-muted">{p.reason}</td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </>
                )}

                {tab === "notificaciones" && (
                    <>
                        {notifications.length === 0 && <p className="mb-0">No hay notificaciones todavía.</p>}
                        {notifications.map((n) => (
                            <div key={n.id} className="glossy-card p-3 mb-2 border-0">
                                <div className="d-flex justify-content-between">
                                    <strong style={{ color: "#0077b6" }}>{n.title}</strong>
                                    <span className="small text-muted">{formatDate(n.createdAt)}</span>
                                </div>
                                <p className="mb-1 small">{n.message}</p>
                                <span className="small text-muted">Para: {n.recipient} · {n.channel} · {n.eventType}</span>
                            </div>
                        ))}
                    </>
                )}
            </div>
        </div>
    );
};
