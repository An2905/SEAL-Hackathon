export default function FormMessage({ message, type = "error" }) {
	if (!message) return null;
	return <div className={`form-message ${type}`}>{message}</div>;
}
