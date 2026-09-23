import { formatPrice } from '../../api/client'

/**
 * Horizontal bar chart: revenue per category.
 *
 * <p>Form chosen before colour. One measure across a handful of named categories
 * is a magnitude comparison, so bars are right — horizontal because the category
 * names are words, not dates, and horizontal labels never need rotating.
 *
 * <p><b>One colour, not a palette.</b> This is a single series: bar *length*
 * already carries the magnitude, so colouring each bar differently would encode
 * nothing and imply a distinction that does not exist. A categorical palette here
 * would be decoration pretending to be information. The one colour used
 * (moss-600, #266e49) measures 6.16:1 against the white card, comfortably clear.
 *
 * <p>Every bar is directly labelled, so no legend is needed and no value depends
 * on reading a colour.
 */
export default function RevenueByCategoryChart({
  data,
}: {
  data: { category: string; revenue: number }[]
}) {
  const rows = data.filter((row) => row.revenue > 0).sort((a, b) => b.revenue - a.revenue)

  if (rows.length === 0) {
    return (
      <p className="py-8 text-center text-sm text-ink-400">
        No sales yet — place an order to see revenue here.
      </p>
    )
  }

  const max = Math.max(...rows.map((row) => row.revenue))

  return (
    <div className="flex flex-col gap-3">
      {rows.map((row) => (
        <div key={row.category} className="group grid grid-cols-[88px_1fr_auto] items-center gap-3">
          <span className="truncate text-sm text-ink-600" title={row.category}>
            {row.category}
          </span>

          {/* The track is a recessive rail; the bar is the only saturated mark.
              Rounded data-end, square against the baseline. */}
          <div className="h-5 rounded-sm bg-ink-100">
            <div
              className="h-5 rounded-r-sm bg-[#266e49] transition-[width] duration-500"
              style={{ width: `${Math.max((row.revenue / max) * 100, 2)}%` }}
              role="img"
              aria-label={`${row.category}: ${formatPrice(row.revenue)}`}
            />
          </div>

          {/* Direct label — the value never depends on reading the axis, and the
              text wears an ink token rather than the series colour. */}
          <span className="text-sm font-medium tabular-nums text-ink-800">
            {formatPrice(row.revenue)}
          </span>
        </div>
      ))}
    </div>
  )
}
