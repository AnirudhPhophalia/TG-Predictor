'use client';

import { useSession, signOut } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { ArrowLeft, LogOut, Mail, User, BookOpen } from 'lucide-react';
import Link from 'next/link';
import { motion } from 'framer-motion';

export default function ProfilePage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  if (status === 'loading') {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-[#6b4423] border-t-[#a0704d] rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-[#6b5448]">Loading profile...</p>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    router.push('/login');
    return null;
  }

  const handleLogout = async () => {
    await signOut({ redirect: true, callbackUrl: '/login' });
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#f5ede0] via-[#faf6f1] to-[#f5ede0] burnt-texture">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <Link
            href="/student"
            className="flex items-center gap-2 text-[#6b4423] hover:text-[#8b5a3c] transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            <span>Back to Dashboard</span>
          </Link>
        </div>

        {/* Profile Card */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="vintage-card parchment-edge lantern-glow"
        >
          <div className="flex flex-col md:flex-row gap-8">
            {/* Profile Section */}
            <div className="flex-1">
              <h1 className="text-3xl font-bold text-[#6b4423] mb-6" style={{ fontFamily: 'var(--font-playfair)' }}>
                My Profile
              </h1>

              <div className="space-y-6">
                {/* Name */}
                <div className="flex items-center gap-4 p-4 bg-gradient-to-r from-[#f0e6d8] to-transparent rounded-lg">
                  <User className="w-6 h-6 text-[#a0704d]" />
                  <div>
                    <p className="text-sm text-[#8b6f47]">Full Name</p>
                    <p className="text-lg font-semibold text-[#2a1810]">{session?.user?.name || 'Student'}</p>
                  </div>
                </div>

                {/* Email */}
                <div className="flex items-center gap-4 p-4 bg-gradient-to-r from-[#f0e6d8] to-transparent rounded-lg">
                  <Mail className="w-6 h-6 text-[#a0704d]" />
                  <div>
                    <p className="text-sm text-[#8b6f47]">Email Address</p>
                    <p className="text-lg font-semibold text-[#2a1810]">{session?.user?.email}</p>
                  </div>
                </div>

                {/* Additional Info */}
                <div className="grid grid-cols-2 gap-4">
                  <div className="p-4 bg-gradient-to-r from-[#f0e6d8] to-transparent rounded-lg">
                    <p className="text-sm text-[#8b6f47]">Account Status</p>
                    <p className="text-lg font-semibold text-[#27ae60] flex items-center gap-2 mt-2">
                      <span className="w-2 h-2 bg-[#27ae60] rounded-full"></span>
                      Verified
                    </p>
                  </div>
                  <div className="p-4 bg-gradient-to-r from-[#f0e6d8] to-transparent rounded-lg">
                    <p className="text-sm text-[#8b6f47]">Member Since</p>
                    <p className="text-lg font-semibold text-[#2a1810] mt-2">
                      {new Date().toLocaleDateString()}
                    </p>
                  </div>
                </div>
              </div>
            </div>

            {/* Quick Actions */}
            <div className="md:w-64">
              <h2 className="text-xl font-semibold text-[#6b4423] mb-4" style={{ fontFamily: 'var(--font-playfair)' }}>
                Quick Actions
              </h2>

              <div className="space-y-3">
                <Link
                  href="/student"
                  className="flex items-center gap-3 p-4 bg-gradient-to-r from-[#a0704d] to-[#8b5a3c] text-[#f5ede0] rounded-lg hover:shadow-lg transition-shadow"
                >
                  <BookOpen className="w-5 h-5" />
                  <span>Dashboard</span>
                </Link>

                <button
                  onClick={handleLogout}
                  className="w-full flex items-center gap-3 p-4 bg-gradient-to-r from-[#dc2626] to-[#b91c1c] text-white rounded-lg hover:shadow-lg transition-shadow"
                >
                  <LogOut className="w-5 h-5" />
                  <span>Logout</span>
                </button>
              </div>
            </div>
          </div>
        </motion.div>

        {/* Help Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5, delay: 0.2 }}
          className="mt-8 vintage-card parchment-edge"
        >
          <h2 className="text-xl font-semibold text-[#6b4423] mb-4" style={{ fontFamily: 'var(--font-playfair)' }}>
            Need Help?
          </h2>
          <p className="text-[#6b5448] mb-4">
            If you encounter any issues or need to modify your marks, please contact your course administrator or visit the help section.
          </p>
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div className="p-4 border border-[#d9cdc0] rounded-lg hover:bg-[#f0e6d8] transition-colors">
              <h3 className="font-semibold text-[#6b4423] mb-2">Request Mark Change</h3>
              <p className="text-sm text-[#8b6f47]">
                Submit a request to modify your submitted marks with proper justification.
              </p>
            </div>
            <div className="p-4 border border-[#d9cdc0] rounded-lg hover:bg-[#f0e6d8] transition-colors">
              <h3 className="font-semibold text-[#6b4423] mb-2">View Analytics</h3>
              <p className="text-sm text-[#8b6f47]">
                Check your percentile ranking and predicted grade compared to other students.
              </p>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
