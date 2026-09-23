import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, formatPrice, mediaUrl } from '../api/client'
import type { Category, Item, PageResponse } from '../api/types'
import { CATEGORY_LABELS } from '../api/types'

/**
 * The storefront's front door.
 *
 * <p>Separate from the catalogue at /shop. The original had one page doing both
 * jobs — a product grid with a banner bolted on top — which meant it neither
 * introduced the shop nor let you browse it well.
 */

const CATEGORY_BLURBS: Record<Category, string> = {
  ROAD: 'Light frames and quick handling for distance and pace.',
  MOUNTAIN: 'Suspension and grip for trails that fight back.',
  ELECTRIC: 'Assistance when you want it, a real bike when you don’t.',
  BMX: 'Chromoly, sealed bearings, built to be thrown around.',
  TANDEM: 'Two riders, one drivetrain, a surprising amount of speed.',
  KIDS: 'Properly scaled, not just shrunk — including the brake levers.',
}

const CATEGORY_ORDER: Category[] = ['ROAD', 'MOUNTAIN', 'ELECTRIC', 'BMX', 'KIDS', 'TANDEM']

/** Carried over from the original storefront's testimonial slider. */
const TESTIMONIALS = [
  { quote: 'Great service and amazing bikes.', name: 'Eva Gagne' },
  { quote: 'I love my new bike, highly recommend.', name: 'Frank Hayes' },
  { quote: 'Excellent customer support and fast delivery.', name: 'Rita Kwan' },
  { quote: 'Best bike shop, and the packaging was faultless.', name: 'Cecile Lavallee' },
]

function BikeIcon() {
  return (
    <svg viewBox="0 0 48 28" className="h-6 w-10" aria-hidden="true">
      <circle cx="10" cy="19" r="7" fill="none" stroke="currentColor" strokeWidth="2" />
      <circle cx="38" cy="19" r="7" fill="none" stroke="currentColor" strokeWidth="2" />
      <path
        d="M10 19 L20 19 L27 8 L34 19 M20 19 L27 8"
        fill="none"
        stroke="currentColor"
        strokeWidth="2"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  )
}

export default function LandingPage() {
  const [featured, setFeatured] = useState<Item[]>([])
  const [hero, setHero] = useState<Item | null>(null)

  useEffect(() => {
    // Featured products come from the live catalogue rather than being
    // hardcoded, so the landing page reflects real stock and never advertises
    // something that has been deleted.
    api
      .get<PageResponse<Item>>('/api/items?size=4&sort=price_desc&inStockOnly=true', false)
      .then((page) => {
        setHero(page.content[0] ?? null)
        setFeatured(page.content.slice(0, 3))
      })
      .catch(() => {
        setFeatured([])
        setHero(null)
      })
  }, [])

  return (
    <>
      {/* ---------- Hero ---------- */}
      <section className="border-b border-ink-100 bg-white">
        <div className="mx-auto grid max-w-6xl items-center gap-10 px-4 py-16 md:grid-cols-2 md:py-24">
          <div>
            <span className="inline-flex items-center gap-2 rounded-full bg-moss-50 px-3 py-1 text-sm font-medium text-moss-700">
              <BikeIcon />
              Toronto’s bike shop since 2024
            </span>

            <h1 className="mt-5 text-4xl font-semibold leading-tight tracking-tight text-ink-900 md:text-5xl">
              The right bike,
              <br />
              properly set up.
            </h1>

            <p className="mt-4 max-w-md text-lg leading-relaxed text-ink-600">
              Road, mountain, electric, BMX, tandem and kids’ bikes — chosen by people who ride
              them, and sized to fit you rather than the box they came in.
            </p>

            <div className="mt-8 flex flex-wrap gap-3">
              <Link
                to="/shop"
                className="rounded-lg bg-moss-600 px-6 py-3 font-medium text-white transition-colors hover:bg-moss-700"
              >
                Shop all bikes
              </Link>
              <Link
                to="/shop?category=ELECTRIC"
                className="rounded-lg border border-ink-200 bg-white px-6 py-3 font-medium text-ink-800 transition-colors hover:bg-ink-50"
              >
                Browse e-bikes
              </Link>
            </div>
          </div>

          <div className="relative">
            {hero ? (
              <Link to={`/items/${hero.id}`} className="group block">
                <div className="overflow-hidden rounded-2xl bg-ink-50 p-6">
                  <img
                    src={mediaUrl(hero.imageUrl)}
                    alt={hero.name}
                    className="h-full w-full object-contain transition-transform duration-500 group-hover:scale-105"
                  />
                </div>
                <div className="mt-3 flex items-baseline justify-between">
                  <span className="text-sm font-medium text-ink-800">{hero.name}</span>
                  <span className="text-sm text-ink-400">from {formatPrice(hero.price)}</span>
                </div>
              </Link>
            ) : (
              <div className="aspect-4/3 rounded-2xl bg-ink-100" />
            )}
          </div>
        </div>
      </section>

      {/* ---------- Categories ---------- */}
      <section className="mx-auto max-w-6xl px-4 py-16">
        <h2 className="text-2xl font-semibold tracking-tight text-ink-900">Find your category</h2>
        <p className="mt-1 text-ink-600">Six kinds of riding. Start wherever you are.</p>

        <div className="mt-8 grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
          {CATEGORY_ORDER.map((category) => (
            <Link
              key={category}
              to={`/shop?category=${category}`}
              className="group rounded-xl border border-ink-100 bg-white p-6 transition-all hover:border-moss-500/40 hover:shadow-md"
            >
              <span className="text-moss-600">
                <BikeIcon />
              </span>
              <h3 className="mt-3 font-semibold text-ink-900">{CATEGORY_LABELS[category]}</h3>
              <p className="mt-1 text-sm leading-relaxed text-ink-600">
                {CATEGORY_BLURBS[category]}
              </p>
              <span className="mt-3 inline-block text-sm font-medium text-moss-600 group-hover:text-moss-700">
                Browse {CATEGORY_LABELS[category].toLowerCase()} →
              </span>
            </Link>
          ))}
        </div>
      </section>

      {/* ---------- Featured ---------- */}
      {featured.length > 0 && (
        <section className="border-y border-ink-100 bg-white">
          <div className="mx-auto max-w-6xl px-4 py-16">
            <div className="flex flex-wrap items-end justify-between gap-3">
              <div>
                <h2 className="text-2xl font-semibold tracking-tight text-ink-900">
                  Featured this month
                </h2>
                <p className="mt-1 text-ink-600">The bikes we’d pick for ourselves.</p>
              </div>
              <Link to="/shop" className="font-medium text-moss-600 hover:text-moss-700">
                See everything →
              </Link>
            </div>

            <div className="mt-8 grid gap-6 sm:grid-cols-2 lg:grid-cols-3">
              {featured.map((item) => (
                <Link
                  key={item.id}
                  to={`/items/${item.id}`}
                  className="group flex flex-col overflow-hidden rounded-xl border border-ink-100 transition-shadow hover:shadow-lg"
                >
                  <div className="aspect-4/3 overflow-hidden bg-ink-50">
                    <img
                      src={mediaUrl(item.imageUrl)}
                      alt={item.name}
                      loading="lazy"
                      className="h-full w-full object-contain transition-transform duration-300 group-hover:scale-105"
                    />
                  </div>
                  <div className="p-4">
                    <h3 className="font-medium text-ink-900">{item.name}</h3>
                    <p className="text-sm text-ink-400">
                      {CATEGORY_LABELS[item.category]} · {item.colour}
                    </p>
                    <p className="mt-2 font-semibold text-ink-900">{formatPrice(item.price)}</p>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        </section>
      )}

      {/* ---------- Why us ---------- */}
      <section className="mx-auto max-w-6xl px-4 py-16">
        <div className="grid gap-8 sm:grid-cols-3">
          {[
            {
              title: 'Fitted before it ships',
              body: 'Every bike is assembled, indexed and torque-checked — not flat-packed with an Allen key and good luck.',
            },
            {
              title: 'Free first service',
              body: 'Cables stretch and spokes settle in the first month. Bring it back and we’ll sort it.',
            },
            {
              title: 'Thirty-day returns',
              body: 'Ride it properly. If the fit is wrong, send it back and we’ll find the one that isn’t.',
            },
          ].map((feature) => (
            <div key={feature.title}>
              <h3 className="font-semibold text-ink-900">{feature.title}</h3>
              <p className="mt-2 text-sm leading-relaxed text-ink-600">{feature.body}</p>
            </div>
          ))}
        </div>
      </section>

      {/* ---------- Testimonials ---------- */}
      <section className="border-t border-ink-100 bg-white">
        <div className="mx-auto max-w-6xl px-4 py-16">
          <h2 className="text-2xl font-semibold tracking-tight text-ink-900">
            What our customers say
          </h2>

          <div className="mt-8 grid gap-5 sm:grid-cols-2 lg:grid-cols-4">
            {TESTIMONIALS.map((testimonial) => (
              <figure
                key={testimonial.name}
                className="flex h-full flex-col rounded-xl border border-ink-100 bg-ink-50 p-5"
              >
                <blockquote className="flex-1 leading-relaxed text-ink-800">
                  “{testimonial.quote}”
                </blockquote>
                <figcaption className="mt-4 text-sm font-medium text-ink-400">
                  — {testimonial.name}
                </figcaption>
              </figure>
            ))}
          </div>
        </div>
      </section>

      {/* ---------- Closing CTA ---------- */}
      <section className="mx-auto max-w-6xl px-4 py-16">
        <div className="rounded-2xl bg-ink-900 px-8 py-14 text-center">
          <h2 className="text-3xl font-semibold tracking-tight text-white">
            Ready to find yours?
          </h2>
          <p className="mx-auto mt-3 max-w-md text-ink-200">
            Seventeen bikes across six categories, with filters that actually narrow things
            down.
          </p>
          <Link
            to="/shop"
            className="mt-7 inline-block rounded-lg bg-moss-500 px-7 py-3 font-medium text-white transition-colors hover:bg-moss-600"
          >
            Shop all bikes
          </Link>
        </div>
      </section>
    </>
  )
}
