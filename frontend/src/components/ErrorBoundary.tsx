import { Component, type ReactNode } from "react";

interface Props {
    children: ReactNode;
}

interface State {
    hasError: boolean;
    error: Error | null;
}

export class ErrorBoundary extends Component<Props, State> {
    constructor(props: Props) {
        super(props);
        this.state = { hasError: false, error: null };
    }

    static getDerivedStateFromError(error: Error): State {
        return { hasError: true, error };
    }

    componentDidCatch(error: Error, errorInfo: unknown) {
        console.error("React Error Boundary caught an error:", error, errorInfo);
    }

    render() {
        if (this.state.hasError) {
            return (
                <div className="container py-5 text-center">
                    <div className="glass-container text-dark mx-auto" style={{ maxWidth: "600px" }}>
                        <h2 className="text-danger fw-bold mb-3">⚠️ Error de Renderizado en la App</h2>
                        <p className="text-secondary">{this.state.error?.message || "Ocurrió un error inesperado."}</p>
                        <button
                            className="btn btn-bubble mt-3"
                            onClick={() => {
                                this.setState({ hasError: false, error: null });
                                window.location.reload();
                            }}
                        >
                            🔄 Reintentar
                        </button>
                    </div>
                </div>
            );
        }

        return this.props.children;
    }
}
