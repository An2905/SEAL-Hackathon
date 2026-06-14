import { resolveAssetUrl } from '../../api/client'

export default function Avatar({ name, avatarUrl, size = 28, fontSize, style }) {
  const initial = (name?.[0] || 'U').toUpperCase()
  const base = {
    width: size,
    height: size,
    borderRadius: '50%',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: 0,
    overflow: 'hidden',
    background: 'var(--accent-soft,#dbeafe)',
    color: 'var(--accent,#2563eb)',
    fontWeight: 700,
    fontSize: fontSize ?? Math.round(size * 0.4),
    ...style
  }

  if (avatarUrl) {
    return <img src={resolveAssetUrl(avatarUrl)} alt={name || 'Avatar'} style={{ ...base, objectFit: 'cover' }} />
  }

  return <span style={base}>{initial}</span>
}
