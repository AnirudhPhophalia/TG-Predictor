import { withAuth } from 'next-auth/middleware';
import { NextResponse } from 'next/server';
import type { NextRequest } from 'next/server';
import type { JWT } from 'next-auth/jwt';

const publicRoutes = ['/login', '/auth', '/auth/error'];
const adminRoutes = ['/admin'];
const studentRoutes = ['/student'];

// Basic In-Memory Rate Limiting (Per-Edge Isolate)
const rateLimitMap = new Map<string, { count: number; startTime: number }>();
const MAX_REQUESTS = 100; // max requests per minute
const WINDOW_MS = 60 * 1000; // 1 minute

// Basic cleanup every minute to prevent memory leak
setInterval(() => {
  const now = Date.now();
  for (const [ip, data] of rateLimitMap.entries()) {
    if (now - data.startTime > WINDOW_MS) {
      rateLimitMap.delete(ip);
    }
  }
}, WINDOW_MS);

export const middleware = withAuth(
  function middleware(req: NextRequest & { nextauth: { token: JWT | null } }) {
    const pathname = req.nextUrl.pathname;
    const token = req.nextauth.token;

    // --- Rate Limiting Logic ---
    const ip = req.ip || req.headers.get('x-forwarded-for') || 'unknown';
    if (ip !== 'unknown') {
      const now = Date.now();
      const clientData = rateLimitMap.get(ip);
      
      if (clientData) {
        if (now - clientData.startTime > WINDOW_MS) {
          rateLimitMap.set(ip, { count: 1, startTime: now });
        } else {
          clientData.count++;
          if (clientData.count > MAX_REQUESTS) {
            return new NextResponse('Too Many Requests. Please slow down.', { status: 429 });
          }
        }
      } else {
        rateLimitMap.set(ip, { count: 1, startTime: now });
      }
    }
    // ---------------------------

    // Allow public routes
    if (publicRoutes.some((route) => pathname.startsWith(route))) {
      return NextResponse.next();
    }

    // Protect routes
    if (!token) {
      return NextResponse.redirect(new URL('/login', req.url));
    }

    // Admin route protection
    if (adminRoutes.some((route) => pathname.startsWith(route))) {
      // Check if user is admin (would need to fetch from DB in real scenario)
      // For now, we'll let the API handle role checking
      return NextResponse.next();
    }

    // Student route protection
    if (studentRoutes.some((route) => pathname.startsWith(route))) {
      return NextResponse.next();
    }

    return NextResponse.next();
  },
  {
    callbacks: {
      authorized: ({ token }) => {
        return !!token;
      },
    },
  }
);

export const config = {
  matcher: [
    '/student/:path*',
    '/admin/:path*',
    '/api/student/:path*',
    '/api/admin/:path*',
  ],
};
