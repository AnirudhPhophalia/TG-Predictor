'use client';

import { useState } from 'react';
import { useParams, useRouter } from 'next/navigation';
import { ArrowLeft, Send } from 'lucide-react';
import Link from 'next/link';

export default function SubmitMarksPage() {
  const params = useParams();
  const router = useRouter();
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState(false);
  
  const [formData, setFormData] = useState({
    totalMarks: '',
    estMarks: '',
    mstMarks: '',
    quizMarks: '',
    assignmentMarks: '',
    labMarks: '',
    practicalMarks: '',
  });

  const handleChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const { name, value } = e.target;
    setFormData((prev) => ({
      ...prev,
      [name]: value === '' ? '' : Math.min(parseFloat(value) || 0, 100),
    }));
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsLoading(true);
    setError(null);

    try {
      const response = await fetch('/api/student/marks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          subjectId: params.subjectId,
          totalMarks: parseFloat(formData.totalMarks),
          estMarks: formData.estMarks ? parseFloat(formData.estMarks) : undefined,
          mstMarks: formData.mstMarks ? parseFloat(formData.mstMarks) : undefined,
          quizMarks: formData.quizMarks ? parseFloat(formData.quizMarks) : undefined,
          assignmentMarks: formData.assignmentMarks ? parseFloat(formData.assignmentMarks) : undefined,
          labMarks: formData.labMarks ? parseFloat(formData.labMarks) : undefined,
          practicalMarks: formData.practicalMarks ? parseFloat(formData.practicalMarks) : undefined,
        }),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || 'Failed to submit marks');
      }

      const data = await response.json();
      setSuccess(true);
      
      setTimeout(() => {
        router.push('/student');
      }, 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setIsLoading(false);
    }
  };

  if (success) {
    return (
      <div className="min-h-screen bg-[#f5ede0] burnt-texture flex items-center justify-center">
        <div className="vintage-card text-center max-w-md">
          <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <svg className="w-8 h-8 text-green-600" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M5 13l4 4L19 7" />
            </svg>
          </div>
          <h2 className="text-2xl font-bold text-[#2a1810] mb-2">Marks Submitted!</h2>
          <p className="text-[#6b5448] mb-6">Your marks have been recorded successfully.</p>
          <p className="text-sm text-[#6b5448]">Redirecting to dashboard...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      <div className="max-w-2xl mx-auto px-6 py-12">
        <Link
          href="/student"
          className="inline-flex items-center gap-2 text-[#6b4423] hover:text-[#3d2817] mb-8 font-medium"
        >
          <ArrowLeft className="w-4 h-4" />
          Back to Dashboard
        </Link>

        <div className="vintage-card fade-in-edges">
          <h1 className="text-3xl font-bold text-[#2a1810] mb-2" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
            Submit Marks
          </h1>
          <p className="text-[#6b5448] mb-8">Enter your marks for the subject</p>

          {error && (
            <div className="p-4 bg-red-50 border border-red-200 rounded-lg mb-6 text-red-700 text-sm">
              {error}
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-6">
            {/* Total Marks - Required */}
            <div>
              <label className="block text-sm font-semibold text-[#3d2817] mb-2">
                Total Marks *
              </label>
              <div className="relative">
                <input
                  type="number"
                  name="totalMarks"
                  value={formData.totalMarks}
                  onChange={handleChange}
                  min="0"
                  max="100"
                  step="0.5"
                  required
                  className="w-full px-4 py-3 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1] text-[#3d2817]"
                  placeholder="0-100"
                />
                <span className="absolute right-4 top-3 text-[#6b5448] text-sm">/100</span>
              </div>
              <p className="text-xs text-[#6b5448] mt-1">Used for grade prediction</p>
            </div>

            {/* Internal Breakdown - Optional */}
            <div className="pt-6 border-t border-[#d9cdc0]">
              <p className="text-sm font-semibold text-[#3d2817] mb-4">
                Internal Breakdown (Optional)
              </p>
              <p className="text-xs text-[#6b5448] mb-4">These marks are for statistical analysis only</p>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                {[
                  { label: 'EST Marks', name: 'estMarks', max: 30 },
                  { label: 'MST Marks', name: 'mstMarks', max: 40 },
                  { label: 'Quiz Marks', name: 'quizMarks', max: 10 },
                  { label: 'Assignment Marks', name: 'assignmentMarks', max: 20 },
                  { label: 'Lab Marks', name: 'labMarks', max: 20 },
                  { label: 'Practical Marks', name: 'practicalMarks', max: 20 },
                ].map((field) => (
                  <div key={field.name}>
                    <label className="block text-sm font-medium text-[#3d2817] mb-1">
                      {field.label}
                    </label>
                    <div className="relative">
                      <input
                        type="number"
                        name={field.name}
                        value={formData[field.name as keyof typeof formData]}
                        onChange={handleChange}
                        min="0"
                        max={field.max}
                        step="0.5"
                        className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1] text-[#3d2817] text-sm"
                        placeholder="0"
                      />
                      <span className="absolute right-3 top-2 text-[#6b5448] text-xs">/{field.max}</span>
                    </div>
                  </div>
                ))}
              </div>
            </div>

            {/* Submit Button */}
            <div className="pt-4 pb-2">
              <div className="p-4 bg-blue-50 border border-blue-200 rounded-lg flex items-start gap-3 mb-6">
                <svg className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" />
                </svg>
                <p className="text-sm text-blue-800">
                  <strong>Privacy Guaranteed:</strong> Your marks are not stored in the database directly. They are stored as a cryptographic hash (just like passwords) to maintain complete anonymity. No one can see your actual marks.
                </p>
              </div>
            </div>

            <div className="pt-2 flex gap-4">
              <button
                type="submit"
                disabled={isLoading}
                className="flex-1 wax-seal text-[#f5ede0] flex items-center justify-center gap-2 disabled:opacity-50"
              >
                {isLoading ? (
                  <>
                    <div className="w-4 h-4 border-2 border-[#f5ede0] border-t-transparent rounded-full animate-spin"></div>
                    Submitting...
                  </>
                ) : (
                  <>
                    <Send className="w-4 h-4" />
                    Submit Marks
                  </>
                )}
              </button>
            </div>
          </form>

          <p className="text-xs text-[#6b5448] text-center mt-6">
            Once submitted, your marks will be locked. You can request edits through the admin.
          </p>
        </div>
      </div>
    </div>
  );
}
