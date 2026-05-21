export default function LoadingButton({
	loading,
	children,
	className = "btn btn-primary btn-block",
	...props
}) {
	return (
		<button disabled={loading} className={className} {...props}>
			<span className="btn-label" style={{ opacity: loading ? 0.6 : 1 }}>
				{children}
			</span>
			{loading && <span className="spinner" />}
		</button>
	);
}
