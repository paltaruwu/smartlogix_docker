import { API_URL as BASE_URL } from "../config/apiConfig";

const API_URL = `${BASE_URL}/api`;

export interface Technician {
    id: number;
    name: string;
    specialty: string;
    description: string;
    price: number;
    available: boolean;
}

export interface ServiceRequest {
    id: number;
    technicianId: number;
    technicianName: string;
    specialty: string;
    client: string;
    email: string;
    address: string;
    description: string;
    amount: number;
    status: "SOLICITADO" | "PAGADO" | "EN_CURSO" | "FINALIZADO" | "CANCELADO";
    paymentStatus: "PENDIENTE" | "APROBADO" | "RECHAZADO";
    paymentReason: string | null;
    createdAt: string;
}

export interface HirePayload {
    technicianId: number;
    client: string;
    email: string;
    address: string;
    description: string;
    cardNumber: string;
}

export interface PaymentRecord {
    id: number;
    referenceType: string;
    referenceId: number;
    client: string;
    amount: number;
    cardLast4: string;
    status: "APROBADO" | "RECHAZADO";
    reason: string;
    createdAt: string;
}

export interface AppNotification {
    id: number;
    eventType: string;
    recipient: string;
    title: string;
    message: string;
    channel: string;
    createdAt: string;
}

const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

// Los microservicios responden errores de negocio como { detail: "..." }
const readError = async (res: Response, fallback: string): Promise<string> => {
    try {
        const body = await res.json();
        return body.detail || fallback;
    } catch {
        return fallback;
    }
};

// ---------- Público (sin login) ----------

export const getTechnicians = async (): Promise<Technician[]> => {
    const res = await fetch(`${API_URL}/technician/v1`);
    if (!res.ok) throw new Error("Error cargando técnicos");
    return res.json();
};

export const hireTechnician = async (payload: HirePayload): Promise<ServiceRequest> => {
    const res = await fetch(`${API_URL}/technician/v1/hire`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(payload),
    });
    if (!res.ok) throw new Error(await readError(res, "No se pudo contratar al técnico"));
    return res.json();
};

export const trackRequest = async (id: number): Promise<ServiceRequest> => {
    const res = await fetch(`${API_URL}/technician/v1/track/${id}`);
    if (!res.ok) throw new Error("Error consultando la solicitud");
    return res.json();
};

// ---------- Administración (requiere token de Entra ID) ----------

export const getServiceRequests = async (token: string): Promise<ServiceRequest[]> => {
    const res = await fetch(`${API_URL}/technician/v1/requests`, { headers: authHeaders(token) });
    if (!res.ok) throw new Error("Error cargando solicitudes");
    return res.json();
};

export const changeRequestStatus = async (id: number, status: string, token: string): Promise<ServiceRequest> => {
    const res = await fetch(`${API_URL}/technician/v1/requests/${id}/status?status=${status}`, {
        method: "PUT",
        headers: authHeaders(token),
    });
    if (!res.ok) throw new Error(await readError(res, "No se pudo cambiar el estado"));
    return res.json();
};

export const getPayments = async (token: string): Promise<PaymentRecord[]> => {
    const res = await fetch(`${API_URL}/payment/v1`, { headers: authHeaders(token) });
    if (!res.ok) throw new Error("Error cargando pagos");
    return res.json();
};

export const getNotifications = async (token: string): Promise<AppNotification[]> => {
    const res = await fetch(`${API_URL}/notification/v1`, { headers: authHeaders(token) });
    if (!res.ok) throw new Error("Error cargando notificaciones");
    return res.json();
};
