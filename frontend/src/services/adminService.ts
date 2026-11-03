import { API_URL as BASE_URL } from "../config/apiConfig";

const API_URL = `${BASE_URL}/api`;

// Todas las funciones reciben el access token de Entra ID (obtenido en el
// componente vía useAuth().getAccessToken()) y lo mandan como Bearer,
// ya que /admin ahora requiere sesión iniciada.
const authHeaders = (token: string) => ({ Authorization: `Bearer ${token}` });

export const getOrders = async (token: string) => {
    const res = await fetch(`${API_URL}/order/v1`, { headers: authHeaders(token) });
    if (!res.ok) throw new Error("Error cargando órdenes");
    return res.json();
};

export const approveOrder = async (id: number, token: string) => {
    const res = await fetch(`${API_URL}/order/v1/${id}/status?status=APROBADO`, {
        method: "PUT",
        headers: authHeaders(token),
    });
    if (!res.ok) throw new Error("Error aprobando orden");
    return res.json();
};

export const addStock = async (id: number, quantity: number, token: string) => {
    const reason = encodeURIComponent("Ingreso desde Panel Admin");
    const res = await fetch(`${API_URL}/inventory/v1/${id}/stock/replenish?quantity=${quantity}&reason=${reason}`, {
        method: "POST",
        headers: authHeaders(token),
    });
    if (!res.ok) throw new Error("Error reponiendo stock");
};

export const getProductMovements = async (id: number, token: string) => {
    const res = await fetch(`${API_URL}/inventory/v1/${id}/movements`, { headers: authHeaders(token) });
    if (!res.ok) throw new Error("Error cargando el historial");
    return res.json();
};
