'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { ArrowLeft, Save, Settings } from 'lucide-react';
import Link from 'next/link';

export default function AdminSettingsPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  
  const [isSaving, setIsSaving] = useState(false);
  const [settings, setSettings] = useState({
    minSubmissionsForPercentile: 100,
    allowStudentEdits: true,
    maintenanceMode: false,
    systemEmail: 'admin@thapar.edu',
  });

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/login');
    }
  }, [status, router]);

  if (status === 'loading') {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f5ede0] burnt-texture">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading settings...</p>
        </div>
      </div>
    );
  }

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSaving(true);
    // In a real application, this would call an API route to save settings in DB/Redis
    // For now, we simulate the save as there is no Settings model in Prisma
    setTimeout(() => {
      setIsSaving(false);
      alert('Settings saved successfully!');
    }, 800);
  };

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge mb-8">
        <div className="max-w-4xl mx-auto px-6 py-8">
          <Link
            href="/admin"
            className="inline-flex items-center gap-2 text-[#d4c4b0] hover:text-[#f5ede0] mb-4 font-medium text-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <div className="flex items-center gap-3">
            <Settings className="w-8 h-8 text-[#d4c4b0]" />
            <div>
              <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                System Settings
              </h1>
              <p className="text-[#d4c4b0] mt-1">Configure global platform behavior</p>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-4xl mx-auto px-6 pb-12">
        <div className="vintage-card">
          <form onSubmit={handleSave} className="space-y-8">
            
            {/* Prediction Algorithm Settings */}
            <div>
              <h3 className="text-lg font-bold text-[#2a1810] mb-4 border-b border-[#d9cdc0] pb-2" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                Prediction Algorithm
              </h3>
              <div className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-[#3d2817] mb-1">
                    Minimum Submissions for Percentile Grading
                  </label>
                  <p className="text-xs text-[#6b5448] mb-2">
                    Number of submissions required before the system switches from historical cutoffs to relative percentile grading.
                  </p>
                  <input
                    type="number"
                    value={settings.minSubmissionsForPercentile}
                    onChange={(e) => setSettings({...settings, minSubmissionsForPercentile: Number(e.target.value)})}
                    className="w-full max-w-xs px-4 py-2 rounded-lg border border-[#d9cdc0] bg-white focus:outline-none focus:ring-2 focus:ring-[#a0704d]"
                  />
                </div>
              </div>
            </div>

            {/* General Settings */}
            <div>
              <h3 className="text-lg font-bold text-[#2a1810] mb-4 border-b border-[#d9cdc0] pb-2" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                General Configuration
              </h3>
              <div className="space-y-4">
                <div className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    id="allowStudentEdits"
                    checked={settings.allowStudentEdits}
                    onChange={(e) => setSettings({...settings, allowStudentEdits: e.target.checked})}
                    className="w-5 h-5 rounded border-[#d9cdc0] text-[#a0704d] focus:ring-[#a0704d]"
                  />
                  <div>
                    <label htmlFor="allowStudentEdits" className="text-sm font-medium text-[#3d2817]">
                      Allow Student Edit Requests
                    </label>
                    <p className="text-xs text-[#6b5448]">If disabled, students cannot submit change requests for locked marks.</p>
                  </div>
                </div>

                <div className="flex items-center gap-3">
                  <input
                    type="checkbox"
                    id="maintenanceMode"
                    checked={settings.maintenanceMode}
                    onChange={(e) => setSettings({...settings, maintenanceMode: e.target.checked})}
                    className="w-5 h-5 rounded border-[#d9cdc0] text-[#a0704d] focus:ring-[#a0704d]"
                  />
                  <div>
                    <label htmlFor="maintenanceMode" className="text-sm font-medium text-[#3d2817]">
                      Maintenance Mode
                    </label>
                    <p className="text-xs text-[#6b5448]">Display a maintenance message and restrict non-admin access.</p>
                  </div>
                </div>

                <div className="mt-4">
                  <label className="block text-sm font-medium text-[#3d2817] mb-1">
                    System Notification Email
                  </label>
                  <input
                    type="email"
                    value={settings.systemEmail}
                    onChange={(e) => setSettings({...settings, systemEmail: e.target.value})}
                    className="w-full max-w-md px-4 py-2 rounded-lg border border-[#d9cdc0] bg-white focus:outline-none focus:ring-2 focus:ring-[#a0704d]"
                  />
                </div>
              </div>
            </div>

            <div className="flex justify-end pt-4">
              <button
                type="submit"
                disabled={isSaving}
                className="inline-flex items-center gap-2 px-6 py-2 rounded-lg bg-gradient-to-r from-[#a0704d] to-[#8b5a3c] text-[#f5ede0] font-medium hover:shadow-lg disabled:opacity-50 transition-all"
              >
                <Save className="w-4 h-4" />
                {isSaving ? 'Saving...' : 'Save Settings'}
              </button>
            </div>
          </form>
        </div>
      </main>
    </div>
  );
}
