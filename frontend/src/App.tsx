import { BrowserRouter, Routes, Route } from "react-router-dom";
import { Home } from "./pages/Home";
import { Products } from "./pages/Products";
import { Cart } from "./pages/Cart";
import { ProcederAlPago } from "./pages/ProcederAlPago";
import { Navbar } from "./components/Navbar";
import { Admin } from "./pages/Admin";
import { Technicians } from "./pages/Technicians";
import { AdminServicios } from "./pages/AdminServicios";
import { Login } from "./pages/login";
import { Register } from "./pages/register";
import { ProtectedRoute } from "./components/ProtectedRoute";

export default function App() {
    return (
        <BrowserRouter>
            <Navbar />

            <Routes>
                <Route path="/" element={<Home />} />
                <Route path="/products" element={<Products />} />
                <Route path="/technicians" element={<Technicians />} />
                <Route
                    path="/admin/servicios"
                    element={
                        <ProtectedRoute>
                            <AdminServicios />
                        </ProtectedRoute>
                    }
                />
                <Route path="/cart" element={<Cart />} />
                <Route path="/checkout" element={<ProcederAlPago />} />
                <Route path="/login" element={<Login />} />
                <Route path="/register" element={<Register />} />
                <Route
                    path="/admin"
                    element={
                        <ProtectedRoute>
                            <Admin />
                        </ProtectedRoute>
                    }
                />
            </Routes>
        </BrowserRouter>
    );
}