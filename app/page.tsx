'use client';

import { useEffect } from 'react';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { BookOpen, Loader } from 'lucide-react';

export default function Page() {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/login');
    } else if (status === 'authenticated' && session?.user) {
      if (session.user.role === 'admin') {
        router.push('/admin');
      } else {
        router.push('/student');
      }
    }
  }, [status, session, router]);

  return (
    <main className="flex min-h-screen items-center justify-center bg-gradient-to-br from-[#f5ede0] via-[#faf6f1] to-[#f5ede0] burnt-texture">
      <div className="flex flex-col items-center gap-4 text-center">
        <div className="w-16 h-16 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full flex items-center justify-center shadow-lg animate-pulse">
          <BookOpen className="w-8 h-8 text-[#f5ede0]" />
        </div>
        <div className="flex items-center gap-2">
          <Loader className="w-5 h-5 text-[#6b4423] animate-spin" />
          <p className="text-sm font-medium text-[#6b5448]">Loading Thapar Grade Predictor...</p>
        </div>
      </div>
    </main>
  );
}
