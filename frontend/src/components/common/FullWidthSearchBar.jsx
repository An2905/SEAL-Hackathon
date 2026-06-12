export default function FullWidthSearchBar({
  value,
  onChange,
  onSearch,
  placeholder = 'Tìm kiếm…',
  disabled = false,
  buttonLabel = 'Tìm kiếm',
  className = ''
}) {
  const handleSubmit = (e) => {
    e.preventDefault()
    onSearch?.(value)
  }

  return (
    <form className={`fullwidth-search-bar${className ? ` ${className}` : ''}`} onSubmit={handleSubmit}>
      <input
        type='search'
        className='fullwidth-search-bar-input'
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        disabled={disabled}
        aria-label={placeholder}
      />
      <button type='submit' className='btn btn-primary fullwidth-search-bar-btn' disabled={disabled}>
        {buttonLabel}
      </button>
    </form>
  )
}
