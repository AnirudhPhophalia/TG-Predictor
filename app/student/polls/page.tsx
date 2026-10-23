'use client';

import { useState } from 'react';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import useSWR from 'swr';
import { ArrowLeft, Clock, BarChart3, AlertCircle } from 'lucide-react';
import Link from 'next/link';

export default function StudentPollsPage() {
  const { data: session } = useSession();
  const router = useRouter();

  const { data: pollsData, mutate: mutatePolls } = useSWR('/api/student/polls', (url) => fetch(url).then(r => r.json()));
  const [submittingPollId, setSubmittingPollId] = useState<string | null>(null);
  const [marksInputs, setMarksInputs] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [resultsData, setResultsData] = useState<Record<string, any>>({});
  const [loadingResults, setLoadingResults] = useState<string | null>(null);

  const handleMarksChange = (pollId: string, value: string) => {
    setMarksInputs(prev => ({ ...prev, [pollId]: value }));
  };

  const handleSubmit = async (pollId: string) => {
    setSubmittingPollId(pollId);
    setError(null);
    try {
      const marks = parseFloat(marksInputs[pollId]);
      if (isNaN(marks)) throw new Error("Please enter valid marks");
      
      const res = await fetch(`/api/student/polls/${pollId}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ marks })
      });
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to submit');
      
      mutatePolls();
    } catch (err: any) {
      setError(err.message);
    } finally {
      setSubmittingPollId(null);
    }
  };

  const fetchResults = async (pollId: string) => {
    setLoadingResults(pollId);
    try {
      const res = await fetch(`/api/student/polls/${pollId}`);
      const data = await res.json();
      if (!res.ok) throw new Error(data.error);
      setResultsData(prev => ({ ...prev, [pollId]: data }));
    } catch (err: any) {
      alert(err.message);
    } finally {
      setLoadingResults(null);
    }
  };

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge">
        <div className="max-w-6xl mx-auto px-6 py-6">
          <Link href="/student" className="inline-flex items-center gap-2 text-[#d4c4b0] hover:text-[#f5ede0] mb-4 text-sm font-medium">
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <h1 className="text-3xl font-bold font-playfair">Subject Polls</h1>
          <p className="text-[#d4c4b0] mt-1 text-sm">Participate in polls to get an estimated grade dynamically based on everyone's input.</p>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 py-8">
        {error && <div className="p-4 bg-red-50 text-red-700 border border-red-200 rounded-lg mb-6">{error}</div>}

        <div className="space-y-6">
          {pollsData?.polls?.length === 0 ? (
            <div className="text-center py-12 vintage-card">
              <p className="text-[#6b5448]">No active polls available.</p>
            </div>
          ) : (
            pollsData?.polls?.map((poll: any) => (
              <div key={poll.id} className="vintage-card">
                <div className="flex flex-col md:flex-row justify-between md:items-start gap-4">
                  <div>
                    <h2 className="text-xl font-bold text-[#2a1810] font-playfair mb-1">
                      {poll.subject.code} - {poll.subject.name}
                    </h2>
                    <p className="text-[#6b5448] mb-4">{poll.question}</p>
                    <div className="flex flex-col gap-1 text-sm text-[#8b5a3c]">
                      <div className="flex items-center gap-2">
                        <Clock className="w-4 h-4" />
                        {poll.isEnded ? (
                          <span className="text-red-700 font-medium">Ended on {new Date(poll.endTime).toLocaleString()}</span>
                        ) : (
                          <span>Ends on {new Date(poll.endTime).toLocaleString()}</span>
                        )}
                      </div>
                      <div className="flex items-center gap-2">
                        <BarChart3 className="w-4 h-4" />
                        <span>Results available at {new Date(poll.resultsAvailableTime).toLocaleString()}</span>
                      </div>
                    </div>
                  </div>

                  <div className="bg-[#f5ede0] p-4 rounded-lg border border-[#d9cdc0] min-w-[250px]">
                    {poll.hasSubmitted ? (
                      <div>
                        <div className="flex items-center gap-2 text-green-700 font-medium mb-2">
                          <AlertCircle className="w-4 h-4" />
                          Marks Submitted: {poll.myResponse}
                        </div>
                        
                        {poll.resultsAvailable ? (
                          resultsData[poll.id] ? (
                            <div className="mt-4 pt-4 border-t border-[#d9cdc0]">
                              <p className="text-sm font-medium text-[#6b5448]">Predicted Grade</p>
                              <p className="text-3xl font-bold text-[#2a1810] mb-2">{resultsData[poll.id].grade}</p>
                              <div className="grid grid-cols-1 gap-2 text-xs text-[#6b5448]">
                                <div>Participants: {resultsData[poll.id].totalParticipants}</div>
                              </div>
                            </div>
                          ) : (
                            <button
                              onClick={() => fetchResults(poll.id)}
                              disabled={loadingResults === poll.id}
                              className="w-full wax-seal text-[#f5ede0] text-sm py-2 mt-2"
                            >
                              {loadingResults === poll.id ? 'Loading...' : 'View Results'}
                            </button>
                          )
                        ) : (
                          <div className="text-sm text-[#8b5a3c] mt-2 italic">
                            Results available at {new Date(poll.resultsAvailableTime).toLocaleTimeString()}
                          </div>
                        )}
                      </div>
                    ) : poll.isEnded ? (
                      <div className="text-[#6b5448] text-center italic">
                        Poll has ended. You did not participate.
                      </div>
                    ) : (
                      <div className="flex flex-col gap-2">
                        <input
                          type="number"
                          placeholder="Your marks..."
                          value={marksInputs[poll.id] || ''}
                          onChange={(e) => handleMarksChange(poll.id, e.target.value)}
                          className="px-3 py-2 border border-[#d9cdc0] rounded focus:outline-none focus:ring-1 focus:ring-[#a0704d] bg-white text-[#3d2817]"
                        />
                        <button
                          onClick={() => handleSubmit(poll.id)}
                          disabled={submittingPollId === poll.id || !marksInputs[poll.id]}
                          className="w-full bg-[#6b4423] text-white py-2 rounded hover:bg-[#8b5a3c] disabled:opacity-50 text-sm font-medium transition-colors"
                        >
                          {submittingPollId === poll.id ? 'Submitting...' : 'Submit Marks'}
                        </button>
                      </div>
                    )}
                  </div>
                </div>
              </div>
            ))
          )}
        </div>
      </main>
    </div>
  );
}
