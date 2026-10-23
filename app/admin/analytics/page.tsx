'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import useSWR from 'swr';
import { ArrowLeft, BookOpen, Users, FileText, Activity } from 'lucide-react';
import Link from 'next/link';
import { BarChart, Bar, PieChart as RechartsChart, Pie, Cell, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from 'recharts';

export default function AdminAnalyticsPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  const { data, isLoading, error } = useSWR(
    '/api/admin/analytics',
    (url) => fetch(url).then((r) => r.json())
  );

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/login');
    }
  }, [status, router]);

  if (status === 'loading' || isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f5ede0] burnt-texture">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading analytics...</p>
        </div>
      </div>
    );
  }

  if (error || !data || data.error) {
    return (
      <div className="min-h-screen bg-[#f5ede0] burnt-texture flex items-center justify-center">
        <div className="text-center text-red-600">Failed to load analytics.</div>
      </div>
    );
  }

  const { dashboard, gradeDistribution, topSubjects } = data;
  const COLORS = ['#a0704d', '#8b5a3c', '#6b4423', '#5a3f2b', '#c9a876', '#d4c4b0', '#b89968'];
  
  const gradeData = Object.entries(gradeDistribution).map(([name, value]) => ({ name, value }));

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge mb-8">
        <div className="max-w-7xl mx-auto px-6 py-8">
          <Link
            href="/admin"
            className="inline-flex items-center gap-2 text-[#d4c4b0] hover:text-[#f5ede0] mb-4 font-medium text-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <div>
            <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              System Analytics
            </h1>
            <p className="text-[#d4c4b0] mt-1">Platform overview and student performance insights.</p>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 pb-12">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6 mb-12">
          <div className="vintage-card flex items-center gap-4">
            <div className="p-3 bg-[#a0704d]/20 rounded-lg text-[#a0704d]">
              <Users className="w-8 h-8" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#6b5448]">Total Students</p>
              <p className="text-3xl font-bold text-[#2a1810]">{dashboard.totalStudents}</p>
            </div>
          </div>
          
          <div className="vintage-card flex items-center gap-4">
            <div className="p-3 bg-[#a0704d]/20 rounded-lg text-[#a0704d]">
              <BookOpen className="w-8 h-8" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#6b5448]">Active Subjects</p>
              <p className="text-3xl font-bold text-[#2a1810]">{dashboard.activeSubjects}</p>
            </div>
          </div>

          <div className="vintage-card flex items-center gap-4">
            <div className="p-3 bg-[#a0704d]/20 rounded-lg text-[#a0704d]">
              <FileText className="w-8 h-8" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#6b5448]">Total Submissions</p>
              <p className="text-3xl font-bold text-[#2a1810]">{dashboard.totalSubmissions}</p>
            </div>
          </div>

          <div className="vintage-card flex items-center gap-4">
            <div className="p-3 bg-[#a0704d]/20 rounded-lg text-[#a0704d]">
              <Activity className="w-8 h-8" />
            </div>
            <div>
              <p className="text-sm font-medium text-[#6b5448]">Pending Edits</p>
              <p className="text-3xl font-bold text-[#2a1810]">{dashboard.pendingEditRequests}</p>
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-2 gap-8 mb-12">
          <div className="vintage-card">
            <h3 className="text-lg font-bold text-[#2a1810] mb-6" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Overall Grade Distribution
            </h3>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <RechartsChart>
                  <Pie
                    data={gradeData}
                    cx="50%"
                    cy="50%"
                    outerRadius={100}
                    dataKey="value"
                    label={({ name, percent }) => `${name} ${(percent * 100).toFixed(0)}%`}
                  >
                    {gradeData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={COLORS[index % COLORS.length]} />
                    ))}
                  </Pie>
                  <Tooltip contentStyle={{ backgroundColor: '#faf6f1', border: '1px solid #d9cdc0' }} />
                  <Legend />
                </RechartsChart>
              </ResponsiveContainer>
            </div>
          </div>

          <div className="vintage-card">
            <h3 className="text-lg font-bold text-[#2a1810] mb-6" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Top Performing Subjects
            </h3>
            <div className="h-[300px]">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={topSubjects} layout="vertical" margin={{ left: 50 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#d9cdc0" horizontal={false} />
                  <XAxis type="number" stroke="#6b5448" domain={[0, 100]} />
                  <YAxis dataKey="subject.code" type="category" stroke="#6b5448" width={100} />
                  <Tooltip 
                    contentStyle={{ backgroundColor: '#faf6f1', border: '1px solid #d9cdc0' }}
                    formatter={(value) => [`${value}/100`, 'Average Marks']}
                  />
                  <Bar dataKey="averageMarks" fill="#a0704d" radius={[0, 4, 4, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
