import { useCallback, useEffect, useState } from "react";
import { useCar } from "../contexts/CartContext";
import {
    getTechnicians,
    hireTechnician,
    trackRequest,
    type ServiceRequest,
    type Technician,
} from "../services/technicianService";

const emptyForm = { client: "", email: "", address: "", description: "", cardNumber: "" };

export const Technicians = () => {
    const { formatCLP } = useCar();

    const [technicians, setTechnicians] = useState<Technician[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);

    const [selected, setSelected] = useState<Technician | null>(null);
    const [form, setForm] = useState(emptyForm);
    const [submitting, setSubmitting] = useState(false);
    const [formError, setFormError] = useState<string | null>(null);

    // Solicitud en seguimiento: se consulta hasta que Payment responde por RabbitMQ
    const [request, setRequest] = useState<ServiceRequest | null>(null);

    const loadTechnicians = useCallback(async () => {
        try {
            setTechnicians(await getTechnicians());
            setLoadError(null);
        } catch {
            setLoadError("No se pudo cargar el catálogo de técnicos.");
        } finally {
            setLoading(false);
        }
    }, []);

    useEffect(() => {
        loadTechnicians();
    }, [loadTechnicians]);

    useEffect(() => {
        if (!request || request.status !== "SOLICITADO") return;

        let attempts = 0;
        const timer = setInterval(async () => {
            attempts += 1;
            try {
                const updated = await trackRequest(request.id);
                if (updated.status !== "SOLICITADO") {
                    setRequest(updated);
                    loadTechnicians();
                }
            } catch {
                // se reintenta en el siguiente ciclo
            }
            if (attempts >= 20) clearInterval(timer);
        }, 1000);

        return () => clearInterval(timer);
    }, [request, loadTechnicians]);

    const openForm = (tech: Technician) => {
        setSelected(tech);
        setForm(emptyForm);
        setFormError(null);
        setRequest(null);
    };

    const closeAll = () => {
        setSelected(null);
        setRequest(null);
        setFormError(null);
    };

    const handleSubmit = async () => {
        if (!selected) return;
        if (!form.client.trim() || !form.address.trim() || !form.description.trim()) {
            setFormError("Completa tu nombre, dirección y el problema a resolver.");
            return;
        }
        if (form.cardNumber.replace(/\D/g, "").length < 12) {
            setFormError("Ingresa un número de tarjeta válido (mínimo 12 dígitos).");
            return;
        }

        setSubmitting(true);
        setFormError(null);
        try {
            const created = await hireTechnician({ technicianId: selected.id, ...form });
            setRequest(created);
            loadTechnicians();
        } catch (e) {
            setFormError(e instanceof Error ? e.message : "No se pudo contratar al técnico.");
            loadTechnicians();
        } finally {
            setSubmitting(false);
        }
    };

    if (loading) {
        return (
            <div className="container text-center mt-5">
                <div className="spinner-border text-primary" role="status"></div>
                <p className="mt-3 text-primary fw-bold">Cargando técnicos...</p>
            </div>
        );
    }

    return (
        <div className="container mb-5">
            <div className="glass-container mb-4 text-center">
                <h2 className="fw-bold" style={{ color: "#0077b6" }}>🔧 Contrata un Técnico</h2>
                <p className="text-muted mb-0">Elige un especialista, describe tu problema y paga en línea.</p>
            </div>

            {loadError && <div className="alert alert-danger">{loadError}</div>}

            <div className="row row-cols-1 row-cols-md-2 row-cols-lg-4 g-4">
                {technicians.map((t) => (
                    <div className="col" key={t.id}>
                        <div className="card glossy-card p-3 h-100 border-0">
                            <div className="d-flex justify-content-between align-items-start mb-2">
                                <div
                                    className="rounded-circle d-flex align-items-center justify-content-center fw-bold text-white shadow-sm"
                                    style={{ width: 52, height: 52, background: "linear-gradient(135deg,#0077b6,#00b4d8)", fontSize: "1.2rem" }}
                                >
                                    {t.name.split(" ").map((p) => p[0]).slice(0, 2).join("")}
                                </div>
                                <span className={`badge p-2 ${t.available ? "bg-success" : "bg-secondary"}`}>
                                    {t.available ? "Disponible" : "Ocupado"}
                                </span>
                            </div>
                            <h5 className="fw-bold text-dark mb-0">{t.name}</h5>
                            <p className="fw-semibold small mb-2" style={{ color: "#0077b6" }}>{t.specialty}</p>
                            <p className="text-muted small flex-grow-1">{t.description}</p>
                            <strong className="fs-5 mb-3" style={{ color: "#0077b6" }}>{formatCLP(t.price)}</strong>
                            <button
                                className={`btn ${t.available ? "btn-bubble" : "btn-secondary"} w-100 rounded-pill fw-bold py-2`}
                                disabled={!t.available}
                                onClick={() => openForm(t)}
                            >
                                {t.available ? "Contratar" : "No disponible"}
                            </button>
                        </div>
                    </div>
                ))}
            </div>

            {selected && (
                <div
                    className="position-fixed top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center p-3"
                    style={{ background: "rgba(13,59,102,0.45)", zIndex: 2000, overflowY: "auto" }}
                    onClick={(e) => { if (e.target === e.currentTarget && !submitting) closeAll(); }}
                >
                    <div className="glass-container w-100" style={{ maxWidth: 520, background: "rgba(255,255,255,0.92)" }}>
                        {!request && (
                            <>
                                <h4 className="fw-bold mb-1" style={{ color: "#0077b6" }}>Contratar a {selected.name}</h4>
                                <p className="text-muted small mb-3">
                                    {selected.specialty} · <strong>{formatCLP(selected.price)}</strong>
                                </p>

                                <input className="form-control mb-2" placeholder="Tu nombre completo"
                                    value={form.client} onChange={(e) => setForm({ ...form, client: e.target.value })} />
                                <input className="form-control mb-2" type="email" placeholder="Correo (para notificaciones)"
                                    value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} />
                                <input className="form-control mb-2" placeholder="Dirección donde se realizará el servicio"
                                    value={form.address} onChange={(e) => setForm({ ...form, address: e.target.value })} />
                                <textarea className="form-control mb-3" rows={3} placeholder="Describe el problema"
                                    value={form.description} onChange={(e) => setForm({ ...form, description: e.target.value })} />

                                <h6 className="fw-bold text-muted">Pago con tarjeta</h6>
                                <input className="form-control mb-1" inputMode="numeric" placeholder="Número de tarjeta"
                                    value={form.cardNumber} onChange={(e) => setForm({ ...form, cardNumber: e.target.value })} />
                                <p className="small text-muted mb-3">
                                    Ingresa el número de tu tarjeta de crédito o débito.
                                </p>

                                {formError && <div className="alert alert-danger py-2 small">{formError}</div>}

                                <div className="d-flex gap-2">
                                    <button className="btn btn-outline-secondary rounded-pill flex-fill" onClick={closeAll} disabled={submitting}>
                                        Cancelar
                                    </button>
                                    <button className="btn btn-bubble rounded-pill fw-bold flex-fill" onClick={handleSubmit} disabled={submitting}>
                                        {submitting ? "Enviando..." : `Pagar ${formatCLP(selected.price)}`}
                                    </button>
                                </div>
                            </>
                        )}

                        {request && (
                            <div className="text-center">
                                {request.status === "SOLICITADO" && (
                                    <>
                                        <div className="spinner-border text-primary mb-3" role="status"></div>
                                        <h4 className="fw-bold" style={{ color: "#0077b6" }}>Procesando tu pago...</h4>
                                        <p className="text-muted">Solicitud #{request.id} enviada. Esperando respuesta del servicio de pagos.</p>
                                    </>
                                )}
                                {request.status !== "SOLICITADO" && request.status !== "CANCELADO" && (
                                    <>
                                        <div style={{ fontSize: "3rem" }}>✅</div>
                                        <h4 className="fw-bold text-success">¡Pago aprobado!</h4>
                                        <p className="mb-1"><strong>{request.technicianName}</strong> fue asignado a tu servicio.</p>
                                        <p className="text-muted small">Solicitud #{request.id} · {formatCLP(request.amount)} · Estado: {request.status}</p>
                                    </>
                                )}
                                {request.status === "CANCELADO" && (
                                    <>
                                        <div style={{ fontSize: "3rem" }}>❌</div>
                                        <h4 className="fw-bold text-danger">Pago rechazado</h4>
                                        <p className="mb-1">{request.paymentReason ?? "Tu pago no pudo ser procesado."}</p>
                                        <p className="text-muted small">La solicitud #{request.id} fue cancelada y el técnico quedó libre.</p>
                                    </>
                                )}
                                {request.status !== "SOLICITADO" && (
                                    <button className="btn btn-bubble rounded-pill px-4 mt-2" onClick={closeAll}>Cerrar</button>
                                )}
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
};
