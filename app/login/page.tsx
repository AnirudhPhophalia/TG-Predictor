'use client';

import { signIn } from 'next-auth/react';
import { useRouter, useSearchParams } from 'next/navigation';
import { useState, useEffect } from 'react';
import { ChevronRight, AlertCircle, BookOpen } from 'lucide-react';

import { Suspense } from 'react';

function LoginContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const errorParam = searchParams.get('error');

  useEffect(() => {
    if (errorParam === 'BatchRestriction') {
      setError('Currently, only Batch 2028 users can access this platform. It will be available for other batches soon.');
    } else if (errorParam === 'AccessDenied') {
      setError('Access denied. Please use a valid Thapar Institute email.');
    }
  }, [errorParam]);

  const callbackUrl = searchParams.get('callbackUrl') || '/student';

  const handleGoogleSignIn = async () => {
    setIsLoading(true);
    setError(null);

    try {
      const result = await signIn('google', {
        callbackUrl,
        redirect: true,
      });

      if (result?.error && !errorParam) {
        setError(
          'Authentication failed. Please sign in with your @thapar.edu email.'
        );
      }
    } catch (err) {
      setError('An error occurred during sign-in. Please try again.');
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-[#f5ede0] via-[#faf6f1] to-[#f5ede0] burnt-texture parchment-edge">
      <div className="absolute top-0 left-0 w-full h-full overflow-hidden pointer-events-none">
        <div className="absolute top-10 left-10 w-32 h-32 bg-[#a0704d] opacity-[0.03] rounded-full blur-3xl"></div>
        <div className="absolute bottom-20 right-20 w-40 h-40 bg-[#6b4423] opacity-[0.03] rounded-full blur-3xl"></div>
        <div className="absolute top-1/2 right-10 w-48 h-48 bg-[#8b5a3c] opacity-[0.02] rounded-full blur-3xl"></div>
      </div>

      <div className="relative z-10 w-full max-w-md px-6">
        <div className="vintage-card fade-in-edges space-y-8">
          {/* Header */}
          <div className="text-center space-y-3">
            <div className="flex justify-center mb-4">
              <div className="w-16 h-16 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full flex items-center justify-center shadow-lg">
                <BookOpen className="w-8 h-8 text-[#f5ede0]" />
              </div>
            </div>
            <h1 className="text-4xl font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Thapar Grade
            </h1>
            <p className="text-5xl font-bold text-[#6b4423]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Predictor
            </p>
            <p className="text-sm text-[#6b5448] mt-4">
              Predict your expected grades based on comparative performance
            </p>
          </div>

          {/* Error Message */}
          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg flex items-start gap-3">
              <AlertCircle className="w-5 h-5 text-red-600 flex-shrink-0 mt-0.5" />
              <p className="text-sm text-red-700">{error}</p>
            </div>
          )}

          {/* Sign In Button */}
          <div className="space-y-4">
            <button
              onClick={handleGoogleSignIn}
              disabled={isLoading}
              className="w-full wax-seal text-[#f5ede0] flex items-center justify-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {isLoading ? (
                <>
                  <div className="w-4 h-4 border-2 border-[#f5ede0] border-t-transparent rounded-full animate-spin"></div>
                  Signing in...
                </>
              ) : (
                <>
                  <svg className="w-5 h-5" viewBox="0 0 24 24" fill="currentColor">
                    <path d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" fill="#f5ede0"/>
                    <path d="M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" fill="#f5ede0"/>
                    <path d="M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" fill="#f5ede0"/>
                    <path d="M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" fill="#f5ede0"/>
                  </svg>
                  Continue with Google
                </>
              )}
            </button>

            <p className="text-xs text-center text-[#6b5448]">
              Only Batch 2028 Thapar Institute email accounts (@thapar.edu) are allowed currently.
            </p>
          </div>

          {/* Features */}
          <div className="space-y-3 pt-4 border-t border-[#d9cdc0]">
            <p className="text-xs font-semibold text-[#6b4423] uppercase tracking-wide">Features</p>
            <ul className="space-y-2 text-sm text-[#3d2817]">
              <li className="flex items-center gap-2">
                <ChevronRight className="w-4 h-4 text-[#a0704d]" />
                <span>Grade prediction based on percentile ranking</span>
              </li>
              <li className="flex items-center gap-2">
                <ChevronRight className="w-4 h-4 text-[#a0704d]" />
                <span>Advanced statistical analysis</span>
              </li>
              <li className="flex items-center gap-2">
                <ChevronRight className="w-4 h-4 text-[#a0704d]" />
                <span>Historical performance tracking</span>
              </li>
            </ul>
          </div>
        </div>

        {/* Footer */}
        <p className="text-center text-xs text-[#6b5448] mt-6">
          Secure authentication using Google Sign-In
        </p>
      </div>
    </div>
  );
}

export default function LoginPage() {
  return (
    <Suspense fallback={<div className="min-h-screen flex items-center justify-center bg-[#f5ede0] burnt-texture"><div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full animate-pulse"></div></div>}>
      <LoginContent />
    </Suspense>
  );
}
