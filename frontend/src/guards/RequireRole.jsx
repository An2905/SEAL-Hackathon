import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export default function RequireRole({ role, children }) {
	const { auth, isLoggedIn, pathForRole } = useAuth();

	if (!isLoggedIn) return <Navigate to="/" replace />;
	if (auth.role !== role)
		return <Navigate to={pathForRole(auth.role)} replace />;

	return children;
}
