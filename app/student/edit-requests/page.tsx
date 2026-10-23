'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect } from 'react';
import useSWR from 'swr';
import { ArrowLeft, Clock, CheckCircle, XCircle, Unlock, ShieldCheck } from 'lucide-react';
import Link from 'next/link';

interface EditRequest {
  id: string;
  markId: string;
  subjectId?: string;
  reason: string;
  status: 'pending' | 'approved' | 'rejected';
  rejectionReason?: string;
  createdAt: string;
  subjectCode: string;
  subjectName: string;
  isLocked: boolean;
}

export default function StudentEditRequestsPage() {
  const { data: session, status: sessionStatus } = useSession();
  const router = useRouter();

  const { data: requests, isLoading, error } = useSWR<EditRequest[]>(
    session?.user?.email ? '/api/student/edit-requests' : null,
    (url) => fetch(url).then((r) => r.json()),
    { revalidateOnFocus: false }
  );

  useEffect(() => {
    if (sessionStatus === 'unauthenticated') {
      router.push('/login');
    }
  }, [sessionStatus, router]);

  if (sessionStatus === 'loading' || isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen bg-[#f5ede0] burnt-texture">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading requests...</p>
        </div>
      </div>
    );
  }

  if (sessionStatus === 'unauthenticated') {
    return null;
  }

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'pending':
        return <Clock className="w-5 h-5 text-yellow-600" />;
      case 'approved':
        return <CheckCircle className="w-5 h-5 text-green-600" />;
      case 'rejected':
        return <XCircle className="w-5 h-5 text-red-600" />;
      default:
        return null;
    }
  };

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
          <div>
            <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Submission Unlock Requests
            </h1>
            <p className="text-[#d4c4b0] mt-1">Track the status of your marks unlock requests</p>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 pb-12">
        {/* Privacy Note */}
        <div className="mb-6 p-4 rounded-lg bg-[#faf6f1] border border-[#d9cdc0] flex items-center gap-3">
          <ShieldCheck className="w-5 h-5 text-green-700 flex-shrink-0" />
          <p className="text-sm text-[#6b5448]">
            <strong>Privacy Protection:</strong> When you request an unlock, administrators only review your justification. Administrators cannot see your marks. Once approved, the submission unlocks so you can update marks privately.
          </p>
        </div>

        {error ? (
          <div className="vintage-card text-center py-12">
            <p className="text-red-600">Failed to load unlock requests.</p>
          </div>
        ) : !requests || requests.length === 0 ? (
          <div className="vintage-card text-center py-16">
            <div className="w-16 h-16 bg-[#eae0d5] rounded-full flex items-center justify-center mx-auto mb-4">
              <Clock className="w-8 h-8 text-[#a0704d]" />
            </div>
            <h3 className="text-xl font-bold text-[#2a1810] mb-2" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              No Requests Yet
            </h3>
            <p className="text-[#6b5448] max-w-md mx-auto">
              You haven't submitted any unlock requests. If you need to modify submitted marks, open the subject details and click "Request Change".
            </p>
          </div>
        ) : (
          <div className="space-y-6">
            {requests.map((request) => (
              <div key={request.id} className="vintage-card relative overflow-hidden">
                <div className={`absolute top-0 left-0 w-1 h-full ${
                  request.status === 'pending' ? 'bg-yellow-500' :
                  request.status === 'approved' ? 'bg-green-500' : 'bg-red-500'
                }`} />

                <div className="pl-4">
                  <div className="flex flex-col sm:flex-row sm:items-start justify-between gap-4 mb-4">
                    <div>
                      <h3 className="text-lg font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                        {request.subjectCode} - {request.subjectName}
                      </h3>
                      <p className="text-sm text-[#6b5448]">
                        Submitted on {new Date(request.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                    <div className="flex items-center gap-2 px-3 py-1.5 rounded-full bg-[#eae0d5] w-fit">
                      {getStatusIcon(request.status)}
                      <span className="text-sm font-medium capitalize text-[#3d2817]">
                        {request.status}
                      </span>
                    </div>
                  </div>

                  <div className="bg-[#fcf9f5] p-4 rounded-lg border border-[#d9cdc0] mt-4">
                    <p className="text-sm font-medium text-[#6b5448] mb-1">Reason for Unlock</p>
                    <p className="text-sm text-[#3d2817] italic">"{request.reason}"</p>
                  </div>

                  {request.status === 'rejected' && request.rejectionReason && (
                    <div className="mt-4 p-4 bg-red-50/50 rounded-lg border border-red-100">
                      <p className="text-sm font-medium text-red-800 mb-1">Rejection Reason</p>
                      <p className="text-sm text-red-700">{request.rejectionReason}</p>
                    </div>
                  )}
                  
                  {request.status === 'approved' && request.subjectId && (
                    <div className="mt-4 flex items-center justify-between bg-green-50 p-4 rounded-lg border border-green-200">
                      <div className="flex items-center gap-2 text-sm text-green-800">
                        <Unlock className="w-4 h-4 text-green-700" />
                        <span>Submission unlocked! You may now update your marks securely.</span>
                      </div>
                      <Link
                        href={`/student/submit/${request.subjectId}`}
                        className="px-4 py-2 bg-[#a0704d] text-white rounded-lg hover:bg-[#8b5a3c] text-sm font-medium shadow"
                      >
                        Update Marks Now
                      </Link>
                    </div>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </main>
    </div>
  );
}
