'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { Plus, Users, Clock, BarChart3, Settings, ChevronDown, ChevronUp, X, ArrowLeft, ShieldCheck } from 'lucide-react';
import Link from 'next/link';

export default function AdminPollsPage() {
  const [subjectId, setSubjectId] = useState('');
  const [question, setQuestion] = useState('How much marks did you get?');
  const [durationMins, setDurationMins] = useState(60);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Expanded Poll ID for viewing aggregate response stats
  const [expandedPoll, setExpandedPoll] = useState<string | null>(null);
  const [pollSummaries, setPollSummaries] = useState<Record<string, any>>({});
  const [loadingSummary, setLoadingSummary] = useState<string | null>(null);

  // Manual cutoffs state
  const [editingCutoffsPoll, setEditingCutoffsPoll] = useState<any>(null);
  const [cutoffsForm, setCutoffsForm] = useState({
    cutoffA: '',
    cutoffAMinus: '',
    cutoffB: '',
    cutoffBMinus: '',
    cutoffC: '',
    cutoffD: '',
  });
  const [isSavingCutoffs, setIsSavingCutoffs] = useState(false);

  const { data: subjectsData } = useSWR('/api/admin/subjects', (url) => fetch(url).then(r => r.json()));
  const { data: pollsData, mutate: mutatePolls } = useSWR('/api/admin/polls', (url) => fetch(url).then(r => r.json()));

  const handleCreatePoll = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      const res = await fetch('/api/admin/polls', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subjectId,
          question,
          durationMins,
        }),
      });

      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to create poll');

      setQuestion('How much marks did you get?');
      setDurationMins(60);
      setSubjectId('');
      mutatePolls();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setIsLoading(false);
    }
  };

  const handleToggleResponses = async (pollId: string) => {
    if (expandedPoll === pollId) {
      setExpandedPoll(null);
      return;
    }
    setExpandedPoll(pollId);
    
    // Fetch if not already fetched
    if (!pollSummaries[pollId]) {
      setLoadingSummary(pollId);
      try {
        const res = await fetch(`/api/admin/polls/${pollId}/responses`);
        const data = await res.json();
        if (res.ok) {
          setPollSummaries(prev => ({ ...prev, [pollId]: data.summary }));
        }
      } catch (err) {
        console.error('Failed to load poll summary');
      } finally {
        setLoadingSummary(null);
      }
    }
  };

  const handleOpenCutoffs = (poll: any) => {
    setEditingCutoffsPoll(poll);
    setCutoffsForm({
      cutoffA: poll.cutoffA?.toString() || '',
      cutoffAMinus: poll.cutoffAMinus?.toString() || '',
      cutoffB: poll.cutoffB?.toString() || '',
      cutoffBMinus: poll.cutoffBMinus?.toString() || '',
      cutoffC: poll.cutoffC?.toString() || '',
      cutoffD: poll.cutoffD?.toString() || '',
    });
  };

  const handleSaveCutoffs = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsSavingCutoffs(true);
    try {
      const payload = {
        cutoffA: cutoffsForm.cutoffA ? parseFloat(cutoffsForm.cutoffA) : null,
        cutoffAMinus: cutoffsForm.cutoffAMinus ? parseFloat(cutoffsForm.cutoffAMinus) : null,
        cutoffB: cutoffsForm.cutoffB ? parseFloat(cutoffsForm.cutoffB) : null,
        cutoffBMinus: cutoffsForm.cutoffBMinus ? parseFloat(cutoffsForm.cutoffBMinus) : null,
        cutoffC: cutoffsForm.cutoffC ? parseFloat(cutoffsForm.cutoffC) : null,
        cutoffD: cutoffsForm.cutoffD ? parseFloat(cutoffsForm.cutoffD) : null,
      };
      
      const res = await fetch(`/api/admin/polls/${editingCutoffsPoll.id}/cutoffs`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(payload)
      });
      if (!res.ok) throw new Error('Failed to save cutoffs');
      
      setEditingCutoffsPoll(null);
      mutatePolls();
    } catch (err: any) {
      alert(err.message);
    } finally {
      setIsSavingCutoffs(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge">
        <div className="max-w-6xl mx-auto px-6 py-6">
          <Link href="/admin" className="inline-flex items-center gap-2 text-[#d4c4b0] hover:text-[#f5ede0] mb-4 text-sm font-medium">
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <h1 className="text-3xl font-bold font-playfair">Manage Polls</h1>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8 grid grid-cols-1 md:grid-cols-3 gap-8">
        <div className="md:col-span-1">
          <div className="vintage-card">
            <h2 className="text-xl font-bold text-[#2a1810] mb-6 font-playfair">Create New Poll</h2>
            {error && <div className="p-3 bg-red-50 text-red-700 text-sm rounded-lg mb-4">{error}</div>}
            
            <form onSubmit={handleCreatePoll} className="space-y-4">
              <div>
                <label className="block text-sm font-semibold text-[#3d2817] mb-1">Subject</label>
                <select
                  required
                  value={subjectId}
                  onChange={e => setSubjectId(e.target.value)}
                  className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                >
                  <option value="">Select Subject</option>
                  {subjectsData?.subjects?.map((s: any) => (
                    <option key={s.id} value={s.id}>{s.code} - {s.name}</option>
                  ))}
                </select>
              </div>

              <div>
                <label className="block text-sm font-semibold text-[#3d2817] mb-1">Question</label>
                <input
                  required
                  type="text"
                  value={question}
                  onChange={e => setQuestion(e.target.value)}
                  className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                />
              </div>

              <div>
                <label className="block text-sm font-semibold text-[#3d2817] mb-1">Duration (Minutes)</label>
                <input
                  required
                  type="number"
                  min="1"
                  value={durationMins}
                  onChange={e => setDurationMins(parseInt(e.target.value))}
                  className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                />
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full wax-seal text-[#f5ede0] flex items-center justify-center gap-2 mt-4"
              >
                {isLoading ? 'Creating...' : <><Plus className="w-4 h-4"/> Create Poll</>}
              </button>
            </form>
          </div>
        </div>

        <div className="md:col-span-2">
          <div className="space-y-4">
            <h2 className="text-xl font-bold text-[#2a1810] font-playfair">Active & Past Polls</h2>
            
            {pollsData?.polls?.length === 0 ? (
              <p className="text-[#6b5448]">No polls created yet.</p>
            ) : (
              pollsData?.polls?.map((poll: any) => {
                const isEnded = new Date() > new Date(poll.endTime);
                const summary = pollSummaries[poll.id];

                return (
                  <div key={poll.id} className={`vintage-card ${isEnded ? 'opacity-90' : ''}`}>
                    <div className="flex justify-between items-start mb-2">
                      <h3 className="font-bold text-[#3d2817] text-lg">{poll.subject.code} - {poll.subject.name}</h3>
                      <span className={`text-xs px-2 py-1 rounded-full font-medium ${isEnded ? 'bg-red-100 text-red-800' : 'bg-green-100 text-green-800'}`}>
                        {isEnded ? 'Ended' : 'Active'}
                      </span>
                    </div>
                    <p className="text-[#6b5448] text-sm mb-4">{poll.question}</p>
                    <div className="flex items-center gap-6 text-sm text-[#8b5a3c] mt-2 flex-wrap">
                      <div className="flex items-center gap-1">
                        <Users className="w-4 h-4" />
                        {poll._count.responses} Responses
                      </div>
                      <div className="flex items-center gap-1">
                        <Clock className="w-4 h-4" />
                        Ends: {new Date(poll.endTime).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                      <div className="flex items-center gap-1">
                        <BarChart3 className="w-4 h-4" />
                        Results: {new Date(new Date(poll.endTime).getTime() + 10 * 60000).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </div>
                    </div>

                    <div className="flex items-center gap-4 mt-4">
                      <button 
                        onClick={() => handleToggleResponses(poll.id)}
                        className="flex items-center gap-2 text-sm font-medium text-[#6b4423] hover:text-[#a0704d] transition-colors"
                      >
                        {expandedPoll === poll.id ? (
                          <><ChevronUp className="w-4 h-4" /> Hide Statistics</>
                        ) : (
                          <><ChevronDown className="w-4 h-4" /> View Statistics</>
                        )}
                      </button>
                      
                      <button 
                        onClick={() => handleOpenCutoffs(poll)}
                        className="flex items-center gap-2 text-sm font-medium text-[#6b4423] hover:text-[#a0704d] transition-colors"
                      >
                        <Settings className="w-4 h-4" /> Set Cutoffs
                      </button>
                    </div>

                    {expandedPoll === poll.id && (
                      <div className="mt-4 pt-4 border-t border-[#d9cdc0]">
                        {loadingSummary === poll.id ? (
                          <div className="text-center py-4 text-[#6b5448]">Loading statistics...</div>
                        ) : summary ? (
                          <div className="space-y-4">
                            <div className="flex items-center gap-2 text-xs text-[#27ae60] bg-green-50 p-2.5 rounded border border-green-200">
                              <ShieldCheck className="w-4 h-4 flex-shrink-0" />
                              <span><strong>Privacy Protected:</strong> Individual student scores are private. Only aggregate statistics are shown.</span>
                            </div>

                            <div className="grid grid-cols-2 sm:grid-cols-4 gap-3 text-center">
                              <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0]">
                                <p className="text-xs text-[#6b5448]">Total</p>
                                <p className="text-lg font-bold text-[#2a1810]">{summary.totalResponses}</p>
                              </div>
                              <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0]">
                                <p className="text-xs text-[#6b5448]">Average</p>
                                <p className="text-lg font-bold text-[#6b4423]">{summary.averageMarks} / 100</p>
                              </div>
                              <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0]">
                                <p className="text-xs text-[#6b5448]">Highest</p>
                                <p className="text-lg font-bold text-[#27ae60]">{summary.highestMarks}</p>
                              </div>
                              <div className="p-3 bg-[#faf6f1] rounded border border-[#d9cdc0]">
                                <p className="text-xs text-[#6b5448]">Lowest</p>
                                <p className="text-lg font-bold text-[#e74c3c]">{summary.lowestMarks}</p>
                              </div>
                            </div>

                            <div>
                              <h4 className="text-xs font-semibold text-[#3d2817] mb-2 uppercase tracking-wider">Score Distribution</h4>
                              <div className="grid grid-cols-5 gap-2 text-center text-xs">
                                {Object.entries(summary.distribution || {}).map(([range, count]: [string, any]) => (
                                  <div key={range} className="p-2 bg-[#eae0d5] rounded">
                                    <span className="block text-[#6b5448]">{range}</span>
                                    <span className="font-bold text-[#2a1810]">{count} students</span>
                                  </div>
                                ))}
                              </div>
                            </div>
                          </div>
                        ) : (
                          <div className="text-center py-4 text-[#6b5448]">No responses yet.</div>
                        )}
                      </div>
                    )}
                  </div>
                );
              })
            )}
          </div>
        </div>
      </main>

      {/* Set Cutoffs Modal */}
      {editingCutoffsPoll && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center p-4 z-50">
          <div className="bg-[#f5ede0] burnt-texture rounded-lg shadow-xl max-w-md w-full border border-[#d9cdc0]">
            <div className="flex justify-between items-center p-6 border-b border-[#d9cdc0]">
              <h2 className="text-xl font-bold text-[#2a1810] font-playfair">Manual Cutoffs</h2>
              <button 
                onClick={() => setEditingCutoffsPoll(null)}
                className="text-[#6b5448] hover:text-[#2a1810]"
              >
                <X className="w-6 h-6" />
              </button>
            </div>
            
            <form onSubmit={handleSaveCutoffs} className="p-6 space-y-4">
              <p className="text-sm text-[#6b5448] mb-4">
                Leave blank to use default dynamic percentile calculations. Setting these values will override percentiles for this poll.
              </p>
              
              <div className="grid grid-cols-2 gap-4">
                {['A', 'AMinus', 'B', 'BMinus', 'C', 'D'].map((grade) => (
                  <div key={grade}>
                    <label className="block text-sm font-semibold text-[#3d2817] mb-1">
                      Grade {grade.replace('Minus', '-')}
                    </label>
                    <input
                      type="number"
                      step="any"
                      placeholder="Min Marks"
                      value={cutoffsForm[`cutoff${grade}` as keyof typeof cutoffsForm]}
                      onChange={(e) => setCutoffsForm(prev => ({ ...prev, [`cutoff${grade}`]: e.target.value }))}
                      className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                    />
                  </div>
                ))}
              </div>
              
              <div className="pt-4 flex gap-3">
                <button
                  type="button"
                  onClick={() => setEditingCutoffsPoll(null)}
                  className="flex-1 px-4 py-2 border border-[#d9cdc0] text-[#3d2817] rounded hover:bg-[#eae0d5] transition-colors"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={isSavingCutoffs}
                  className="flex-1 wax-seal text-[#f5ede0] py-2"
                >
                  {isSavingCutoffs ? 'Saving...' : 'Save Cutoffs'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}
