import { Analytics } from '@vercel/analytics/next'
import type { Metadata, Viewport } from 'next'
import { Playfair_Display, Crimson_Text } from 'next/font/google'
import './globals.css'
import { Providers } from './providers'

const playfairDisplay = Playfair_Display({ 
  variable: '--font-playfair', 
  subsets: ['latin'],
  weight: ['400', '600', '700'],
})

const crimsonText = Crimson_Text({ 
  variable: '--font-crimson', 
  subsets: ['latin'],
  weight: ['400', '600'],
})

export const metadata: Metadata = {
  title: 'Thapar Grade Predictor',
  description: 'Advanced grade prediction system for Thapar Institute students. Predict your expected grades based on comparative performance analysis.',
  openGraph: {
    title: 'Thapar Grade Predictor',
    description: 'Advanced grade prediction system for Thapar Institute students',
    type: 'website',
  },
}

export const viewport: Viewport = {
  colorScheme: 'light',
  themeColor: '#6b4423',
  width: 'device-width',
  initialScale: 1,
  maximumScale: 1,
}

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode
}>) {
  return (
    <html 
      lang="en" 
      className={`${playfairDisplay.variable} ${crimsonText.variable} bg-background`}
      style={{
        '--font-playfair': playfairDisplay.style.fontFamily,
        '--font-crimson': crimsonText.style.fontFamily,
      } as React.CSSProperties}
    >
      <head>
        <style>{`
          :root {
            --font-playfair: ${playfairDisplay.style.fontFamily};
            --font-crimson: ${crimsonText.style.fontFamily};
          }
        `}</style>
      </head>
      <body className="font-sans antialiased burnt-texture">
        <Providers>
          <div className="min-h-screen flex flex-col">
            {children}
          </div>
        </Providers>
        {process.env.NODE_ENV === 'production' && <Analytics />}
      </body>
    </html>
  )
}
