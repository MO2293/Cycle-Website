interface Props {
  page: number
  totalPages: number
  onChange: (page: number) => void
}

export default function Pagination({ page, totalPages, onChange }: Props) {
  if (totalPages <= 1) {
    return null
  }

  return (
    <nav className="flex items-center justify-center gap-2 pt-8" aria-label="Pagination">
      <button
        type="button"
        onClick={() => onChange(page - 1)}
        disabled={page === 0}
        className="rounded-lg border border-ink-200 bg-white px-3 py-2 text-sm text-ink-800 disabled:opacity-40"
      >
        Previous
      </button>

      {Array.from({ length: totalPages }, (_, index) => (
        <button
          key={index}
          type="button"
          onClick={() => onChange(index)}
          aria-current={index === page ? 'page' : undefined}
          className={
            index === page
              ? 'rounded-lg bg-moss-600 px-3.5 py-2 text-sm font-medium text-white'
              : 'rounded-lg border border-ink-200 bg-white px-3.5 py-2 text-sm text-ink-800 hover:bg-ink-50'
          }
        >
          {index + 1}
        </button>
      ))}

      <button
        type="button"
        onClick={() => onChange(page + 1)}
        disabled={page >= totalPages - 1}
        className="rounded-lg border border-ink-200 bg-white px-3 py-2 text-sm text-ink-800 disabled:opacity-40"
      >
        Next
      </button>
    </nav>
  )
}
