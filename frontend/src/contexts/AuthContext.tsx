import { createContext, useContext, type ReactNode } from "react";
import { useMsal, useIsAuthenticated } from "@azure/msal-react";
import { InteractionRequiredAuthError, type AccountInfo } from "@azure/msal-browser";
import { loginRequest } from "../config/authConfig";

type AuthContextType = {
    isAuthenticated: boolean;
    user: AccountInfo | null;
    login: () => Promise<void>;
    logout: () => void;
    getAccessToken: () => Promise<string | null>;
};

const AuthContext = createContext<AuthContextType | null>(null);

export function AuthProvider({ children }: { children: ReactNode }) {
    let instance: ReturnType<typeof useMsal>["instance"] | null = null;
    let accounts: AccountInfo[] = [];
    let isAuthenticated = false;

    try {
        const msal = useMsal();
        instance = msal.instance;
        accounts = msal.accounts;
    } catch (e) {
        console.warn("Msal context fallback active:", e);
    }

    try {
        const isAuth = useIsAuthenticated();
        isAuthenticated = isAuth;
    } catch {
        isAuthenticated = accounts.length > 0;
    }

    const user = accounts[0] ?? null;

    const login = async () => {
        if (!instance) return;
        try {
            await instance.loginRedirect(loginRequest);
        } catch (err) {
            console.error("Error en loginRedirect:", err);
        }
    };

    const logout = () => {
        if (!instance) return;
        try {
            instance.logoutRedirect({
                postLogoutRedirectUri: "/",
            });
        } catch (err) {
            console.error("Error en logoutRedirect:", err);
        }
    };

    const getAccessToken = async (): Promise<string | null> => {
        if (!instance || accounts.length === 0) return null;

        try {
            const response = await instance.acquireTokenSilent({
                ...loginRequest,
                account: accounts[0],
            });
            return response.idToken;
        } catch (error) {
            if (error instanceof InteractionRequiredAuthError) {
                const response = await instance.acquireTokenPopup(loginRequest);
                return response.idToken;
            }
            console.error("Error obteniendo el access token de Entra ID", error);
            return null;
        }
    };

    return (
        <AuthContext.Provider value={{ isAuthenticated, user, login, logout, getAccessToken }}>
            {children}
        </AuthContext.Provider>
    );
}

export const useAuth = () => {
    const ctx = useContext(AuthContext);
    if (!ctx) {
        return {
            isAuthenticated: false,
            user: null,
            login: async () => {},
            logout: () => {},
            getAccessToken: async () => null,
        };
    }
    return ctx;
};
