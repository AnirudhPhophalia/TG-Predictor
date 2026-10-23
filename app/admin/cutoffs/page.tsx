'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import useSWR from 'swr';
import { ArrowLeft, Edit2, Save, X } from 'lucide-react';
import Link from 'next/link';

interface HistoricalCutoff {
  id: string;
  subjectId: string;
  cutoffA: number;
  cutoffAMinus: number;
  cutoffB: number;
  cutoffBMinus: number;
  cutoffC: number;
  cutoffD: number;
}

interface Subject {
  id: string;
  code: string;
  name: string;
  semester: number;
  historicalCutoffs?: HistoricalCutoff;
}

export default function AdminCutoffsPage() {
  const { data: session, status } = useSession();
  const router = useRouter();

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editForm, setEditForm] = useState<Partial<HistoricalCutoff>>({});
  const [isSaving, setIsSaving] = useState(false);

  const { data, isLoading, mutate } = useSWR<{ subjects: Subject[] }>(
    '/api/admin/subjects',
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
          <p className="text-[#6b5448]">Loading subjects & cutoffs...</p>
        </div>
      </div>
    );
  }

  const handleEditClick = (subject: Subject) => {
    setEditingId(subject.id);
    if (subject.historicalCutoffs) {
      setEditForm(subject.historicalCutoffs);
    } else {
      // Default fallbacks
      setEditForm({
        cutoffA: 85,
        cutoffAMinus: 75,
        cutoffB: 65,
        cutoffBMinus: 55,
        cutoffC: 45,
        cutoffD: 35,
      });
    }
  };

  const handleSave = async (subjectId: string) => {
    setIsSaving(true);
    try {
      const res = await fetch('/api/admin/cutoffs', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subjectId,
          ...editForm,
        }),
      });
      
      const resData = await res.json();
      if (!res.ok) throw new Error(resData.error || 'Failed to save cutoffs');
      
      mutate();
      setEditingId(null);
    } catch (err: any) {
      alert(err.message || 'An error occurred while saving.');
    } finally {
      setIsSaving(false);
    }
  };

  const handleChange = (field: keyof HistoricalCutoff, value: string) => {
    setEditForm(prev => ({
      ...prev,
      [field]: Number(value)
    }));
  };

  const subjects = data?.subjects || [];

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
              Historical Grade Cutoffs
            </h1>
            <p className="text-[#d4c4b0] mt-1">Manage boundaries for predictions before 100+ submissions.</p>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 pb-12">
        <div className="vintage-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead className="border-b-2 border-[#d9cdc0]">
                <tr>
                  <th className="px-4 py-4 font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>Subject</th>
                  <th className="px-4 py-4 font-bold text-[#2a1810] text-center" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>A</th>
                  <th className="px-4 py-4 font-bold text-[#2a1810] text-center" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>A-</th>
                  <th className="px-4 py-4 font-bold text-[#2a1810] text-center" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>B</th>
                  <th className="px-4 py-4 font-bold text-[#2a1810] text-center" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>B-</th>
                  <th className="px-4 py-4 font-bold text-[#2a1810] text-center" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>C</th>
                  <th className="px-4 py-4 font-bold text-[#2a1810] text-center" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>D</th>
                  <th className="px-4 py-4 font-bold text-[#2a1810] text-right" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#d9cdc0]">
                {subjects.map((subject) => {
                  const isEditing = editingId === subject.id;
                  const cutoffs = subject.historicalCutoffs || {
                    cutoffA: 85, cutoffAMinus: 75, cutoffB: 65, cutoffBMinus: 55, cutoffC: 45, cutoffD: 35
                  };
                  
                  return (
                    <tr key={subject.id} className="hover:bg-[#faf6f1] transition-colors">
                      <td className="px-4 py-4">
                        <p className="font-bold text-[#3d2817]">{subject.code}</p>
                        <p className="text-xs text-[#6b5448]">{subject.name}</p>
                      </td>
                      
                      {isEditing ? (
                        <>
                          <td className="px-2 py-4 text-center">
                            <input type="number" value={editForm.cutoffA || ''} onChange={(e) => handleChange('cutoffA', e.target.value)} className="w-14 px-2 py-1 text-center border rounded bg-white" />
                          </td>
                          <td className="px-2 py-4 text-center">
                            <input type="number" value={editForm.cutoffAMinus || ''} onChange={(e) => handleChange('cutoffAMinus', e.target.value)} className="w-14 px-2 py-1 text-center border rounded bg-white" />
                          </td>
                          <td className="px-2 py-4 text-center">
                            <input type="number" value={editForm.cutoffB || ''} onChange={(e) => handleChange('cutoffB', e.target.value)} className="w-14 px-2 py-1 text-center border rounded bg-white" />
                          </td>
                          <td className="px-2 py-4 text-center">
                            <input type="number" value={editForm.cutoffBMinus || ''} onChange={(e) => handleChange('cutoffBMinus', e.target.value)} className="w-14 px-2 py-1 text-center border rounded bg-white" />
                          </td>
                          <td className="px-2 py-4 text-center">
                            <input type="number" value={editForm.cutoffC || ''} onChange={(e) => handleChange('cutoffC', e.target.value)} className="w-14 px-2 py-1 text-center border rounded bg-white" />
                          </td>
                          <td className="px-2 py-4 text-center">
                            <input type="number" value={editForm.cutoffD || ''} onChange={(e) => handleChange('cutoffD', e.target.value)} className="w-14 px-2 py-1 text-center border rounded bg-white" />
                          </td>
                          <td className="px-4 py-4 text-right flex justify-end gap-2">
                            <button onClick={() => setEditingId(null)} className="p-2 text-[#6b5448] hover:bg-[#eae0d5] rounded" title="Cancel">
                              <X className="w-4 h-4" />
                            </button>
                            <button onClick={() => handleSave(subject.id)} disabled={isSaving} className="p-2 text-green-600 hover:bg-green-50 rounded" title="Save">
                              <Save className="w-4 h-4" />
                            </button>
                          </td>
                        </>
                      ) : (
                        <>
                          <td className="px-4 py-4 text-center font-medium text-[#2a1810]">≥ {cutoffs.cutoffA}</td>
                          <td className="px-4 py-4 text-center font-medium text-[#2a1810]">≥ {cutoffs.cutoffAMinus}</td>
                          <td className="px-4 py-4 text-center font-medium text-[#2a1810]">≥ {cutoffs.cutoffB}</td>
                          <td className="px-4 py-4 text-center font-medium text-[#2a1810]">≥ {cutoffs.cutoffBMinus}</td>
                          <td className="px-4 py-4 text-center font-medium text-[#2a1810]">≥ {cutoffs.cutoffC}</td>
                          <td className="px-4 py-4 text-center font-medium text-[#2a1810]">≥ {cutoffs.cutoffD}</td>
                          <td className="px-4 py-4 text-right">
                            <button onClick={() => handleEditClick(subject)} className="inline-flex items-center gap-1 px-3 py-1.5 text-sm text-[#a0704d] hover:bg-[#eae0d5] rounded-lg transition-colors">
                              <Edit2 className="w-4 h-4" /> Edit
                            </button>
                          </td>
                        </>
                      )}
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
}
