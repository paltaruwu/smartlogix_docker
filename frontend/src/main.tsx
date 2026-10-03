import "bootstrap/dist/css/bootstrap.min.css";
import "bootstrap-icons/font/bootstrap-icons.css";
import { StrictMode } from "react";
import { createRoot } from "react-dom/client";
import "./index.css";
import App from "./App";
import { CartProvider } from "./contexts/CartContext";
import { PublicClientApplication, EventType } from "@azure/msal-browser";
import { MsalProvider } from "@azure/msal-react";
import { msalConfig } from "./config/authConfig";
import { AuthProvider } from "./contexts/AuthContext";
import { ErrorBoundary } from "./components/ErrorBoundary";

const msalInstance = new PublicClientApplication(msalConfig);

let isRendered = false;

const renderApp = () => {
    if (isRendered) return;
    isRendered = true;

    try {
        if (!msalInstance.getActiveAccount() && msalInstance.getAllAccounts().length > 0) {
            msalInstance.setActiveAccount(msalInstance.getAllAccounts()[0]);
        }

        msalInstance.addEventCallback((event) => {
            if (event.eventType === EventType.LOGIN_SUCCESS && event.payload) {
                const account = (event.payload as { account?: import("@azure/msal-browser").AccountInfo }).account;
                if (account) msalInstance.setActiveAccount(account);
            }
        });
    } catch (e) {
        console.warn("MSAL setup event warning:", e);
    }

    const container = document.getElementById("root")!;
    createRoot(container).render(
        <StrictMode>
            <ErrorBoundary>
                <MsalProvider instance={msalInstance}>
                    <AuthProvider>
                        <CartProvider>
                            <App />
                        </CartProvider>
                    </AuthProvider>
                </MsalProvider>
            </ErrorBoundary>
        </StrictMode>
    );
};

// Initialize MSAL and handle redirect response before rendering
msalInstance.initialize()
    .then(() => msalInstance.handleRedirectPromise())
    .then(renderApp)
    .catch((error) => {
        console.error("MSAL Initialization Error:", error);
        renderApp();
    });