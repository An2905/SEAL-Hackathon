import { Fragment, useState } from 'react'

export const LIST_VISIBLE_LIMIT = 5

export function useCollapsibleList(items = [], limit = LIST_VISIBLE_LIMIT) {
  const [expanded, setExpanded] = useState(false)
  const total = items.length
  const hasMore = total > limit
  const visibleItems = expanded || !hasMore ? items : items.slice(0, limit)
  const hiddenCount = Math.max(0, total - limit)

  return {
    visibleItems,
    expanded,
    hasMore,
    hiddenCount,
    total,
    toggle: () => setExpanded((v) => !v),
    setExpanded
  }
}

export function CollapsibleListToggle({ hasMore, expanded, hiddenCount, onToggle, className = '' }) {
  if (!hasMore) return null

  return (
    <div className={`collapsible-list-toggle${className ? ` ${className}` : ''}`}>
      <button type='button' className='btn btn-outline collapsible-list-toggle-btn' onClick={onToggle}>
        {expanded ? 'Thu gọn' : `Xem thêm (${hiddenCount})`}
      </button>
    </div>
  )
}

export default function CollapsibleKvList({
  items = [],
  limit = LIST_VISIBLE_LIMIT,
  className = 'kv-list',
  style,
  renderItem,
  getItemKey,
  toggleClassName = ''
}) {
  const { visibleItems, expanded, hasMore, hiddenCount, toggle } = useCollapsibleList(items, limit)

  if (!items.length) return null

  return (
    <>
      <div className={className} style={style}>
        {visibleItems.map((item, index) => {
          const key = getItemKey ? getItemKey(item, index) : index
          return <Fragment key={key}>{renderItem(item, index)}</Fragment>
        })}
      </div>
      <CollapsibleListToggle
        hasMore={hasMore}
        expanded={expanded}
        hiddenCount={hiddenCount}
        onToggle={toggle}
        className={toggleClassName}
      />
    </>
  )
}

export function CollapsibleSimpleList({
  items = [],
  limit = LIST_VISIBLE_LIMIT,
  className = 'simple-list',
  renderItem,
  getItemKey,
  toggleClassName = ''
}) {
  const { visibleItems, expanded, hasMore, hiddenCount, toggle } = useCollapsibleList(items, limit)

  if (!items.length) return null

  return (
    <>
      <ul className={className}>
        {visibleItems.map((item, index) => {
          const key = getItemKey ? getItemKey(item, index) : index
          return <Fragment key={key}>{renderItem(item, index)}</Fragment>
        })}
      </ul>
      <CollapsibleListToggle
        hasMore={hasMore}
        expanded={expanded}
        hiddenCount={hiddenCount}
        onToggle={toggle}
        className={toggleClassName}
      />
    </>
  )
}
