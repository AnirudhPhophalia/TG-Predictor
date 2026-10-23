'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { Settings, Users, BookOpen, BarChart3, FileText, ShieldCheck } from 'lucide-react';
import Link from 'next/link';

export default function AdminDashboard() {
  const { data: session, status } = useSession();
  const router = useRouter();

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/login');
    }
  }, [status, router]);

  if (status === 'loading') {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading admin dashboard...</p>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return null;
  }

  const adminMenuItems = [
    {
      title: 'Subjects',
      description: 'Manage subjects and academic details',
      icon: BookOpen,
      href: '/admin/subjects',
      color: 'from-[#a0704d] to-[#8b5a3c]',
    },
    {
      title: 'Grade Cutoffs',
      description: 'Configure historical grade boundaries',
      icon: BarChart3,
      href: '/admin/cutoffs',
      color: 'from-[#8b5a3c] to-[#6b4423]',
    },
    {
      title: 'Users',
      description: 'Manage student accounts and roles',
      icon: Users,
      href: '/admin/users',
      color: 'from-[#c9a876] to-[#a0704d]',
    },
    {
      title: 'Unlock Requests',
      description: 'Review student requests to unlock marks submissions',
      icon: FileText,
      href: '/admin/edit-requests',
      color: 'from-[#a0704d] to-[#8b5a3c]',
    },
    {
      title: 'Polls',
      description: 'Create and manage subject polls',
      icon: FileText,
      href: '/admin/polls',
      color: 'from-[#c9a876] to-[#a0704d]',
    },
    {
      title: 'Analytics',
      description: 'View system statistics and trends',
      icon: BarChart3,
      href: '/admin/analytics',
      color: 'from-[#6b4423] to-[#5a3f2b]',
    },
    {
      title: 'Settings',
      description: 'System configuration and preferences',
      icon: Settings,
      href: '/admin/settings',
      color: 'from-[#8b5a3c] to-[#6b4423]',
    },
  ];

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      {/* Header */}
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge">
        <div className="max-w-7xl mx-auto px-6 py-8">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                Admin Dashboard
              </h1>
              <p className="text-[#d4c4b0] text-sm mt-1">System management and privacy-preserving academic configuration</p>
            </div>
            <Link
              href="/student"
              className="px-4 py-2 rounded-lg bg-[#a0704d] text-[#f5ede0] hover:bg-[#8b5a3c] transition-colors text-sm font-medium"
            >
              Back to Student View
            </Link>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-12">
        {/* Privacy Banner */}
        <div className="mb-8 p-4 rounded-lg bg-green-50 border border-green-200 flex items-center gap-3">
          <ShieldCheck className="w-6 h-6 text-green-700 flex-shrink-0" />
          <p className="text-sm text-green-800">
            <strong>Student Data Privacy Guarantee:</strong> Individual student marks are strictly private. The platform architecture guarantees that only the uploading student and internal prediction calculators can access marks. Administrators cannot view or export individual student marks.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {adminMenuItems.map((item) => {
            const Icon = item.icon;
            return (
              <Link
                key={item.href}
                href={item.href}
                className="vintage-card hover:shadow-lg transition-all group overflow-hidden"
              >
                <div className={`h-12 w-12 rounded-lg bg-gradient-to-br ${item.color} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                  <Icon className="w-6 h-6 text-[#f5ede0]" />
                </div>
                <h3 className="text-lg font-bold text-[#2a1810] mb-1">{item.title}</h3>
                <p className="text-sm text-[#6b5448]">{item.description}</p>
              </Link>
            );
          })}
        </div>

        {/* Info Box */}
        <div className="mt-12 vintage-card">
          <h3 className="text-lg font-bold text-[#2a1810] mb-4" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
            System Information & Reports
          </h3>
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-6">
            <div>
              <p className="text-sm font-medium text-[#6b5448] mb-1">Database Status</p>
              <p className="text-2xl font-bold text-[#2a1810]">Connected</p>
            </div>
            <div>
              <p className="text-sm font-medium text-[#6b5448] mb-1">Export Subject Analytics</p>
              <div className="flex gap-2 mt-2">
                <button
                  onClick={async () => {
                    const res = await fetch('/api/admin/export', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ format: 'csv' }) });
                    if (!res.ok) return alert('Export failed');
                    const blob = await res.blob();
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a'); a.href = url; a.download = 'subject_statistics.csv'; a.click();
                  }}
                  className="px-3 py-1 bg-[#a0704d] text-white rounded text-xs hover:bg-[#8b5a3c]"
                >
                  CSV
                </button>
                <button
                  onClick={async () => {
                    const res = await fetch('/api/admin/export', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ format: 'excel' }) });
                    if (!res.ok) return alert('Export failed');
                    const blob = await res.blob();
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a'); a.href = url; a.download = 'subject_statistics.xlsx'; a.click();
                  }}
                  className="px-3 py-1 bg-green-600 text-white rounded text-xs hover:bg-green-700"
                >
                  Excel
                </button>
                <button
                  onClick={async () => {
                    const res = await fetch('/api/admin/export', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ format: 'pdf' }) });
                    if (!res.ok) return alert('Export failed');
                    const blob = await res.blob();
                    const url = window.URL.createObjectURL(blob);
                    const a = document.createElement('a'); a.href = url; a.download = 'subject_statistics.pdf'; a.click();
                  }}
                  className="px-3 py-1 bg-red-600 text-white rounded text-xs hover:bg-red-700"
                >
                  PDF
                </button>
              </div>
            </div>
            <div>
              <p className="text-sm font-medium text-[#6b5448] mb-1">Confidentiality Standard</p>
              <p className="text-sm text-[#2a1810]">Exports contain subject aggregate stats only. Student identity is decoupled.</p>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
