'use client';

import { useState, useEffect } from 'react';
import { useRouter, useParams } from 'next/navigation';
import { ArrowLeft, Lock, Unlock, ShieldCheck } from 'lucide-react';
import Link from 'next/link';

export default function MarkDetailsPage() {
  const params = useParams();
  const router = useRouter();
  const [mark, setMark] = useState<any>(null);
  const [statistics, setStatistics] = useState<any>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [reason, setReason] = useState('');
  const [requestStatus, setRequestStatus] = useState<'idle' | 'submitting' | 'success' | 'error'>('idle');
  const [requestMessage, setRequestMessage] = useState('');

  useEffect(() => {
    const fetchData = async () => {
      try {
        if (!params?.markId) return;
        setIsLoading(true);
        
        // Fetch mark details
        const marksRes = await fetch('/api/student/marks');
        if (!marksRes.ok) throw new Error('Failed to fetch marks');
        const marksData = await marksRes.json();
        
        const currentMark = marksData.marks.find((m: any) => m.id === params.markId);
        if (!currentMark) throw new Error('Mark not found');
        
        setMark(currentMark);

        // Fetch statistics
        const statsRes = await fetch(`/api/student/statistics/${currentMark.subjectId}`);
        if (!statsRes.ok) throw new Error('Failed to fetch statistics');
        const statsData = await statsRes.json();
        
        setStatistics(statsData);
      } catch (err) {
        setError(err instanceof Error ? err.message : 'An error occurred');
      } finally {
        setIsLoading(false);
      }
    };

    fetchData();
  }, [params?.markId]);

  const handleRequestUnlock = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!reason.trim()) return;
    
    setRequestStatus('submitting');
    try {
      const res = await fetch('/api/student/edit-requests', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          markId: mark.id,
          reason,
        }),
      });
      
      const data = await res.json();
      
      if (!res.ok) {
        throw new Error(data.error || 'Failed to submit unlock request');
      }
      
      setRequestStatus('success');
      setRequestMessage('Unlock request submitted successfully. You will be notified when an admin reviews it.');
      setTimeout(() => setIsModalOpen(false), 3000);
    } catch (err) {
      setRequestStatus('error');
      setRequestMessage(err instanceof Error ? err.message : 'An error occurred');
    }
  };

  if (isLoading) {
    return (
      <div className="min-h-screen bg-[#f5ede0] burnt-texture flex items-center justify-center">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading details...</p>
        </div>
      </div>
    );
  }

  if (error || !mark || !statistics) {
    return (
      <div className="min-h-screen bg-[#f5ede0] burnt-texture">
        <div className="max-w-6xl mx-auto px-6 py-12">
          <Link
            href="/student"
            className="inline-flex items-center gap-2 text-[#6b4423] hover:text-[#3d2817] mb-8 font-medium"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <div className="vintage-card text-center py-12">
            <p className="text-red-600">{error || 'Mark not found'}</p>
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      {/* Header */}
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge mb-8">
        <div className="max-w-6xl mx-auto px-6 py-8">
          <Link
            href="/student"
            className="inline-flex items-center gap-2 text-[#d4c4b0] hover:text-[#f5ede0] mb-4 font-medium text-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                {mark.subject.code}
              </h1>
              <p className="text-[#d4c4b0] mt-1">{mark.subject.name}</p>
            </div>
            {mark.isLocked ? (
              <button
                onClick={() => setIsModalOpen(true)}
                className="px-4 py-2 bg-[#d4c4b0] text-[#2a1810] rounded-lg hover:bg-[#c9a876] transition-colors text-sm font-medium shadow-md flex items-center gap-2"
              >
                <Lock className="w-4 h-4" />
                Request Unlock to Edit
              </button>
            ) : (
              <Link
                href={`/student/submit/${mark.subjectId}`}
                className="px-4 py-2 bg-green-600 text-white rounded-lg hover:bg-green-700 transition-colors text-sm font-medium shadow-md flex items-center gap-2"
              >
                <Unlock className="w-4 h-4" />
                Edit Unlocked Marks
              </Link>
            )}
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 pb-12">
        {/* Privacy Shield Banner */}
        <div className="mb-8 p-4 rounded-lg bg-green-50 border border-green-200 flex items-center gap-3">
          <ShieldCheck className="w-5 h-5 text-green-700 flex-shrink-0" />
          <p className="text-sm text-green-800">
            <strong>Strict Confidentiality:</strong> Your marks are private to you. Administrators and peers cannot view your scores. Marks are used solely by the automated prediction algorithm to determine relative percentiles.
          </p>
        </div>

        {/* Grade Overview */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-12">
          <div className="vintage-card">
            <p className="text-[#6b5448] text-sm font-medium mb-2">Your Marks</p>
            <p className="text-4xl font-bold text-[#2a1810]">{mark.totalMarks}</p>
            <p className="text-xs text-[#6b5448] mt-2">/100 Total</p>
          </div>

          <div className="vintage-card">
            <p className="text-[#6b5448] text-sm font-medium mb-2">Predicted Grade</p>
            <p className="text-4xl font-bold text-[#a0704d]">{mark.predictedGrade || 'N/A'}</p>
            <p className="text-xs text-[#6b5448] mt-2">Relative Grading</p>
          </div>

          <div className="vintage-card">
            <p className="text-[#6b5448] text-sm font-medium mb-2">Percentile Rank</p>
            <p className="text-4xl font-bold text-[#6b4423]">
              {mark.percentile !== null && mark.percentile !== undefined ? `${mark.percentile}%` : 'Historical'}
            </p>
            <p className="text-xs text-[#6b5448] mt-2">Among classmates</p>
          </div>

          <div className="vintage-card">
            <p className="text-[#6b5448] text-sm font-medium mb-2">Subject Average</p>
            <p className="text-4xl font-bold text-[#3d2817]">
              {statistics?.averageMarks !== undefined ? statistics.averageMarks : '-'}
            </p>
            <p className="text-xs text-[#6b5448] mt-2">Class Benchmark</p>
          </div>
        </div>

        {/* Breakdown Card */}
        <div className="vintage-card">
          <h3 className="text-lg font-bold text-[#2a1810] mb-4 font-playfair">Marks Breakdown</h3>
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-4">
            <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0] text-center">
              <span className="text-xs text-[#6b5448]">EST (30)</span>
              <p className="text-xl font-bold text-[#2a1810] mt-1">{mark.estMarks ?? '-'}</p>
            </div>
            <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0] text-center">
              <span className="text-xs text-[#6b5448]">MST (40)</span>
              <p className="text-xl font-bold text-[#2a1810] mt-1">{mark.mstMarks ?? '-'}</p>
            </div>
            <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0] text-center">
              <span className="text-xs text-[#6b5448]">Quiz (10)</span>
              <p className="text-xl font-bold text-[#2a1810] mt-1">{mark.quizMarks ?? '-'}</p>
            </div>
            <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0] text-center">
              <span className="text-xs text-[#6b5448]">Assignment (20)</span>
              <p className="text-xl font-bold text-[#2a1810] mt-1">{mark.assignmentMarks ?? '-'}</p>
            </div>
            <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0] text-center">
              <span className="text-xs text-[#6b5448]">Lab (20)</span>
              <p className="text-xl font-bold text-[#2a1810] mt-1">{mark.labMarks ?? '-'}</p>
            </div>
            <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0] text-center">
              <span className="text-xs text-[#6b5448]">Practical (20)</span>
              <p className="text-xl font-bold text-[#2a1810] mt-1">{mark.practicalMarks ?? '-'}</p>
            </div>
          </div>
        </div>
      </main>

      {/* Unlock Request Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-[#f5ede0] burnt-texture rounded-lg shadow-xl p-6 max-w-md w-full border border-[#d9cdc0]">
            <h2 className="text-xl font-bold text-[#2a1810] mb-2" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Request Submission Unlock
            </h2>
            <p className="text-xs text-[#6b5448] mb-4">
              Your submission is locked to preserve academic integrity. Submitting a request allows an admin to unlock your window so you can update marks privately.
            </p>
            
            {requestStatus === 'success' ? (
              <div className="p-4 bg-green-100 text-green-800 rounded-lg border border-green-200">
                {requestMessage}
              </div>
            ) : (
              <form onSubmit={handleRequestUnlock} className="space-y-4">
                {requestStatus === 'error' && (
                  <div className="p-3 bg-red-100 text-red-800 rounded-lg text-sm">
                    {requestMessage}
                  </div>
                )}

                <div>
                  <label htmlFor="reason" className="block text-sm font-medium text-[#3d2817] mb-1">
                    Reason for Request *
                  </label>
                  <textarea
                    id="reason"
                    required
                    rows={4}
                    value={reason}
                    onChange={(e) => setReason(e.target.value)}
                    placeholder="e.g., Entered MST marks incorrectly, got re-evaluation marks..."
                    className="w-full px-4 py-2 rounded-lg border border-[#d9cdc0] bg-white focus:outline-none focus:ring-2 focus:ring-[#a0704d] resize-none"
                  ></textarea>
                </div>

                <div className="flex gap-3 mt-6">
                  <button
                    type="button"
                    onClick={() => {
                      setIsModalOpen(false);
                      setRequestStatus('idle');
                      setRequestMessage('');
                    }}
                    className="flex-1 px-4 py-2 rounded-lg border border-[#d9cdc0] text-[#6b4423] hover:bg-[#eae0d5] transition-colors"
                  >
                    Cancel
                  </button>
                  <button
                    type="submit"
                    disabled={requestStatus === 'submitting' || !reason.trim()}
                    className="flex-1 px-4 py-2 rounded-lg bg-gradient-to-r from-[#a0704d] to-[#8b5a3c] text-[#f5ede0] font-medium hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all"
                  >
                    {requestStatus === 'submitting' ? 'Submitting...' : 'Submit Request'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
