'use client';

import { useEffect, useState } from 'react';
import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { motion } from 'framer-motion';
import { CheckCircle2, XCircle, Clock, ArrowLeft, Unlock, Shield } from 'lucide-react';
import Link from 'next/link';

interface EditRequest {
  id: string;
  userId: string;
  markId: string;
  reason: string;
  status: string;
  approvedAt?: string;
  rejectionReason?: string;
  createdAt: string;
  user?: {
    name?: string;
    email: string;
    rollNumber?: string;
  };
}

export default function EditRequestsPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const [requests, setRequests] = useState<EditRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [filterStatus, setFilterStatus] = useState<'pending' | 'approved' | 'rejected'>('pending');
  const [selectedRequest, setSelectedRequest] = useState<EditRequest | null>(null);
  const [rejectionReason, setRejectionReason] = useState('');

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/login');
    } else if (status === 'authenticated' && session?.user?.role !== 'admin') {
      router.push('/');
    }
  }, [status, session, router]);

  useEffect(() => {
    if (status === 'authenticated') {
      fetchRequests();
    }
  }, [status, filterStatus]);

  const fetchRequests = async () => {
    try {
      const response = await fetch(`/api/admin/edit-requests?status=${filterStatus}`);
      if (!response.ok) throw new Error('Failed to fetch');
      const data = await response.json();
      setRequests(data.data || []);
    } catch (error) {
      console.error('Error fetching edit requests:', error);
    } finally {
      setLoading(false);
    }
  };

  const handleApprove = async (requestId: string) => {
    try {
      const response = await fetch(`/api/admin/edit-requests/${requestId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ action: 'approve' }),
      });

      if (response.ok) {
        setSelectedRequest(null);
        fetchRequests();
      }
    } catch (error) {
      console.error('Error approving request:', error);
    }
  };

  const handleReject = async (requestId: string) => {
    if (!rejectionReason.trim()) {
      alert('Please provide a rejection reason');
      return;
    }

    try {
      const response = await fetch(`/api/admin/edit-requests/${requestId}`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          action: 'reject',
          rejectionReason,
        }),
      });

      if (response.ok) {
        setSelectedRequest(null);
        setRejectionReason('');
        fetchRequests();
      }
    } catch (error) {
      console.error('Error rejecting request:', error);
    }
  };

  const getStatusIcon = (status: string) => {
    switch (status) {
      case 'approved':
        return <CheckCircle2 className="w-5 h-5 text-[#27ae60]" />;
      case 'rejected':
        return <XCircle className="w-5 h-5 text-[#e74c3c]" />;
      case 'pending':
        return <Clock className="w-5 h-5 text-[#f39c12]" />;
      default:
        return null;
    }
  };

  if (status === 'loading' || loading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-12 h-12 border-4 border-[#6b4423] border-t-[#a0704d] rounded-full animate-spin mx-auto mb-4"></div>
          <p className="text-[#6b5448]">Loading unlock requests...</p>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return null;
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#f5ede0] via-[#faf6f1] to-[#f5ede0] burnt-texture">
      <div className="max-w-6xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <div>
            <Link
              href="/admin"
              className="flex items-center gap-2 text-[#6b4423] hover:text-[#8b5a3c] transition-colors mb-4"
            >
              <ArrowLeft className="w-5 h-5" />
              <span>Back to Admin</span>
            </Link>
            <h1 className="text-3xl font-bold text-[#6b4423]" style={{ fontFamily: 'var(--font-playfair)' }}>
              Submission Unlock Requests
            </h1>
            <p className="text-sm text-[#8b6f47] mt-1">
              Approve requests to unlock student submissions so students can update their marks privately.
            </p>
          </div>
        </div>

        {/* Privacy Notice */}
        <div className="mb-6 p-4 rounded-lg bg-[#faf6f1] border border-[#d9cdc0] flex items-center gap-3">
          <Shield className="w-5 h-5 text-[#6b4423] flex-shrink-0" />
          <p className="text-sm text-[#6b5448]">
            <strong>Zero-Knowledge Approval:</strong> As an admin, you review the student's reason to unlock their submission window. You do not see or edit the student's marks. Once approved, the student updates marks directly.
          </p>
        </div>

        {/* Filter Tabs */}
        <div className="flex gap-2 mb-6 vintage-card">
          {['pending', 'approved', 'rejected'].map((status) => (
            <button
              key={status}
              onClick={() => {
                setFilterStatus(status as any);
                setLoading(true);
              }}
              className={`px-6 py-3 rounded-lg font-semibold transition-all ${
                filterStatus === status
                  ? 'bg-[#6b4423] text-[#f5ede0] shadow-lg'
                  : 'bg-[#e8d7c3] text-[#6b4423] hover:bg-[#d9cdc0]'
              }`}
            >
              {status.charAt(0).toUpperCase() + status.slice(1)}
            </button>
          ))}
        </div>

        {/* Requests List */}
        {requests.length === 0 ? (
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="vintage-card parchment-edge text-center py-12"
          >
            <p className="text-[#8b6f47] text-lg">No {filterStatus} unlock requests at this time.</p>
          </motion.div>
        ) : (
          <div className="space-y-4">
            {requests.map((request, index) => (
              <motion.div
                key={request.id}
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                transition={{ delay: index * 0.1 }}
                className="vintage-card parchment-edge hover:shadow-lg transition-shadow cursor-pointer"
                onClick={() => setSelectedRequest(request)}
              >
                <div className="flex items-center justify-between">
                  <div className="flex-1">
                    <div className="flex items-center gap-3 mb-2">
                      {getStatusIcon(request.status)}
                      <h3 className="font-semibold text-[#2a1810]">
                        {request.user?.name || 'Unknown Student'}
                      </h3>
                      <span className="text-sm text-[#8b6f47]">
                        ({request.user?.rollNumber || request.user?.email})
                      </span>
                    </div>
                    <p className="text-sm text-[#8b6f47]">
                      Reason: "{request.reason}" • {new Date(request.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                  <div className="text-right">
                    <span className={`inline-block px-3 py-1 rounded-full text-sm font-semibold ${
                      request.status === 'pending'
                        ? 'bg-[#f39c12] text-white'
                        : request.status === 'approved'
                        ? 'bg-[#27ae60] text-white'
                        : 'bg-[#e74c3c] text-white'
                    }`}>
                      {request.status.charAt(0).toUpperCase() + request.status.slice(1)}
                    </span>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        )}
      </div>

      {/* Modal */}
      {selectedRequest && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50">
          <motion.div
            initial={{ opacity: 0, scale: 0.95 }}
            animate={{ opacity: 1, scale: 1 }}
            className="bg-[#faf6f1] rounded-lg shadow-2xl max-w-2xl w-full p-6 max-h-[90vh] overflow-y-auto"
          >
            <h2 className="text-2xl font-bold text-[#6b4423] mb-4" style={{ fontFamily: 'var(--font-playfair)' }}>
              Unlock Request Details
            </h2>

            <div className="space-y-4 mb-6">
              <div>
                <p className="text-sm text-[#8b6f47]">Student</p>
                <p className="font-semibold text-[#2a1810]">
                  {selectedRequest.user?.name || 'Unknown'} ({selectedRequest.user?.rollNumber || selectedRequest.user?.email})
                </p>
              </div>

              <div>
                <p className="text-sm text-[#8b6f47]">Reason for Unlock Request</p>
                <p className="text-[#2a1810] p-3 bg-[#f5ede0] rounded border border-[#d9cdc0]">
                  {selectedRequest.reason}
                </p>
              </div>

              <div>
                <p className="text-sm text-[#8b6f47]">Submitted</p>
                <p className="text-[#2a1810]">
                  {new Date(selectedRequest.createdAt).toLocaleString()}
                </p>
              </div>

              {selectedRequest.status === 'rejected' && selectedRequest.rejectionReason && (
                <div>
                  <p className="text-sm text-[#8b6f47]">Rejection Reason</p>
                  <p className="text-[#e74c3c]">{selectedRequest.rejectionReason}</p>
                </div>
              )}
            </div>

            {selectedRequest.status === 'pending' && (
              <>
                <div className="mb-4">
                  <label className="block text-sm text-[#8b6f47] mb-2">
                    Rejection Reason (if rejecting)
                  </label>
                  <textarea
                    value={rejectionReason}
                    onChange={(e) => setRejectionReason(e.target.value)}
                    placeholder="Enter reason for rejection..."
                    className="w-full p-3 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d]"
                    rows={3}
                  />
                </div>

                <div className="flex gap-3">
                  <button
                    onClick={() => handleApprove(selectedRequest.id)}
                    className="flex-1 bg-[#27ae60] text-white px-4 py-2 rounded-lg hover:bg-[#229954] transition-colors font-semibold flex items-center justify-center gap-2"
                  >
                    <Unlock className="w-4 h-4" />
                    Approve Unlock
                  </button>
                  <button
                    onClick={() => handleReject(selectedRequest.id)}
                    className="flex-1 bg-[#e74c3c] text-white px-4 py-2 rounded-lg hover:bg-[#c0392b] transition-colors font-semibold"
                  >
                    Reject
                  </button>
                  <button
                    onClick={() => {
                      setSelectedRequest(null);
                      setRejectionReason('');
                    }}
                    className="flex-1 bg-[#8b6f47] text-[#f5ede0] px-4 py-2 rounded-lg hover:bg-[#6b5438] transition-colors font-semibold"
                  >
                    Close
                  </button>
                </div>
              </>
            )}

            {selectedRequest.status !== 'pending' && (
              <button
                onClick={() => {
                  setSelectedRequest(null);
                  setRejectionReason('');
                }}
                className="w-full bg-[#8b6f47] text-[#f5ede0] px-4 py-2 rounded-lg hover:bg-[#6b5438] transition-colors font-semibold"
              >
                Close
              </button>
            )}
          </motion.div>
        </div>
      )}
    </div>
  );
}
