'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import  useSWR from 'swr';
import { BookOpen, Plus, TrendingUp, BarChart3 } from 'lucide-react';
import Link from 'next/link';
import { getStudentSemester } from '@/lib/utils';

interface Subject {
  id: string;
  code: string;
  name: string;
  semester: number;
  branch: string;
  credits: number;
  statistics?: {
    totalResponses: number;
    averageMarks: number;
  };
}

interface Mark {
  id: string;
  subjectId: string;
  totalMarks: number;
  predictedGrade: string;
  subject: Subject;
  submittedAt: string;
}

export default function StudentDashboard() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const [marks, setMarks] = useState<Mark[]>([]);
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);

  // Fetch subjects
  const { data: subjectsData, isLoading: subjectsLoading } = useSWR(
    '/api/subjects',
    (url) => fetch(url).then((r) => r.json()),
    { revalidateOnFocus: false }
  );

  // Fetch marks
  const { data: marksData, isLoading: marksLoading } = useSWR(
    session?.user?.email ? '/api/student/marks' : null,
    (url) => fetch(url).then((r) => r.json()),
    { revalidateOnFocus: false }
  );

  useEffect(() => {
    if (status === 'unauthenticated') {
      router.push('/login');
    }
  }, [status, router]);

  useEffect(() => {
    if (marksData?.marks) {
      setMarks(marksData.marks);
    }
  }, [marksData]);

  if (status === 'loading' || subjectsLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading your dashboard...</p>
        </div>
      </div>
    );
  }

  if (status === 'unauthenticated') {
    return null;
  }

  const maxSemester = Math.min(4, getStudentSemester(session?.user?.email));

  const formatSemester = (sem: number) => {
    if (sem === 1) return 'Pool A';
    if (sem === 2) return 'Pool B';
    return `Semester ${sem}`;
  };

  const getGradePoint = (grade: string) => {
    if (grade === 'A') return 10;
    if (grade === 'A-') return 9;
    if (grade === 'B') return 8;
    if (grade === 'B-') return 7;
    if (grade === 'C') return 6;
    if (grade === 'D') return 5;
    return 0;
  };

  const totalCredits = marks.reduce((sum, m) => sum + (m.subject?.credits || 0), 0);
  const weightedGradePoints = marks.reduce((sum, m) => sum + (getGradePoint(m.predictedGrade) * (m.subject?.credits || 0)), 0);
  const averageGrade = totalCredits > 0 ? (weightedGradePoints / totalCredits).toFixed(2) : 'N/A';

  const submittedSubjects = new Set(marks.map((m) => m.subjectId));
  const availableSubjects = subjectsData?.subjects?.filter(
    (s: Subject) => {
      if (submittedSubjects.has(s.id)) return false;
      if (s.semester <= 2) return true;
      return s.semester <= maxSemester;
    }
  ) || [];

  const semesters = [...new Set(subjectsData?.subjects?.map((s: Subject) => s.semester) || [])].sort();
  const filteredMarks = selectedSemester
    ? marks.filter((m) => m.subject.semester === selectedSemester)
    : marks;

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      {/* Header */}
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge">
        <div className="max-w-7xl mx-auto px-6 py-8">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-[#a0704d] rounded-full flex items-center justify-center">
                <BookOpen className="w-6 h-6" />
              </div>
              <div>
                <h1 className="text-2xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                  Grade Predictor
                </h1>
                <p className="text-[#d4c4b0] text-sm">Welcome back, {session?.user?.name}</p>
              </div>
            </div>
            <div className="flex gap-3">
              <Link
                href="/student/polls"
                className="px-4 py-2 rounded-lg border border-[#a0704d] text-[#a0704d] hover:bg-[#a0704d] hover:text-[#f5ede0] transition-colors text-sm font-medium"
              >
                Subject Polls
              </Link>
              <Link
                href="/student/edit-requests"
                className="px-4 py-2 rounded-lg border border-[#a0704d] text-[#a0704d] hover:bg-[#a0704d] hover:text-[#f5ede0] transition-colors text-sm font-medium"
              >
                Edit Requests
              </Link>
              <Link
                href="/student/profile"
                className="px-4 py-2 rounded-lg bg-[#a0704d] text-[#f5ede0] hover:bg-[#8b5a3c] transition-colors text-sm font-medium"
              >
                Profile
              </Link>
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 py-12">
        {/* Disclaimer */}
        <div className="mb-8 p-4 bg-yellow-50 border border-yellow-200 rounded-lg flex items-start gap-3">
          <svg className="w-5 h-5 text-yellow-600 flex-shrink-0 mt-0.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z" />
          </svg>
          <div className="text-sm text-yellow-800">
            <strong>Disclaimer:</strong> Marks and grades shown here may not be 100% correct. This portal is not backed by the university; it is a student-run initiative to help students estimate their performance.
          </div>
        </div>

        {/* Stats Overview */}
        <div className="grid grid-cols-1 md:grid-cols-4 gap-6 mb-12">
          <div className="vintage-card lantern-glow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[#6b5448] text-sm font-medium">Total Submissions</p>
                <p className="text-3xl font-bold text-[#2a1810] mt-1">{marks.length}</p>
              </div>
              <BarChart3 className="w-8 h-8 text-[#a0704d] opacity-50" />
            </div>
          </div>

          <div className="vintage-card lantern-glow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[#6b5448] text-sm font-medium">Average Grade (Out of 10)</p>
                <p className="text-3xl font-bold text-[#2a1810] mt-1">
                  {averageGrade}
                </p>
              </div>
              <TrendingUp className="w-8 h-8 text-[#a0704d] opacity-50" />
            </div>
          </div>

          <div className="vintage-card lantern-glow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[#6b5448] text-sm font-medium">Can Submit For</p>
                <p className="text-3xl font-bold text-[#2a1810] mt-1">{availableSubjects.length}</p>
              </div>
              <Plus className="w-8 h-8 text-[#a0704d] opacity-50" />
            </div>
          </div>

          <div className="vintage-card lantern-glow">
            <div className="flex items-center justify-between">
              <div>
                <p className="text-[#6b5448] text-sm font-medium">Active Subjects</p>
                <p className="text-3xl font-bold text-[#2a1810] mt-1">{subjectsData?.subjects?.length || 0}</p>
              </div>
              <BookOpen className="w-8 h-8 text-[#a0704d] opacity-50" />
            </div>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Left Column - Submitted Marks */}
          <div className="lg:col-span-2">
            <div className="vintage-card">
              <div className="flex items-center justify-between mb-6 pb-4 border-b border-[#d9cdc0]">
                <h2 className="text-xl font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                  Your Submissions
                </h2>
                <select
                  value={selectedSemester || ''}
                  onChange={(e) => setSelectedSemester(e.target.value ? parseInt(e.target.value) : null)}
                  className="px-3 py-2 border border-[#d9cdc0] rounded-lg text-sm text-[#3d2817] bg-[#f5ede0] focus:outline-none focus:ring-2 focus:ring-[#a0704d]"
                >
                  <option value="">All Semesters</option>
                  {semesters.map((sem) => (
                    <option key={sem} value={sem}>
                      {formatSemester(sem)}
                    </option>
                  ))}
                </select>
              </div>

              {marksLoading ? (
                <div className="text-center py-8 text-[#6b5448]">Loading submissions...</div>
              ) : filteredMarks.length > 0 ? (
                <div className="space-y-4">
                  {filteredMarks.map((mark) => (
                    <Link
                      key={mark.id}
                      href={`/student/marks/${mark.id}`}
                      className="block p-4 border border-[#d9cdc0] rounded-lg hover:bg-[#faf6f1] transition-colors group"
                    >
                      <div className="flex items-start justify-between">
                        <div>
                          <p className="font-semibold text-[#3d2817] group-hover:text-[#6b4423]">
                            {mark.subject.code} - {mark.subject.name}
                          </p>
                          <p className="text-sm text-[#6b5448] mt-1">
                            {formatSemester(mark.subject.semester)} • Marks: {mark.totalMarks}/100
                          </p>
                        </div>
                        <div className="text-right">
                          <p className="text-2xl font-bold text-[#6b4423]">{mark.predictedGrade}</p>
                          <p className="text-xs text-[#6b5448] mt-1">Predicted Grade</p>
                        </div>
                      </div>
                    </Link>
                  ))}
                </div>
              ) : (
                <div className="text-center py-8 text-[#6b5448]">
                  {marks.length === 0 ? 'No submissions yet. Start by submitting marks!' : 'No submissions in this semester.'}
                </div>
              )}
            </div>
          </div>

          {/* Right Column - Submit Marks */}
          <div>
            <div className="vintage-card sticky top-6">
              <h3 className="text-lg font-bold text-[#2a1810] mb-4" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                Submit Marks
              </h3>
              {availableSubjects.length > 0 ? (
                <div className="space-y-3">
                  {availableSubjects.slice(0, 5).map((subject: Subject) => (
                    <Link
                      key={subject.id}
                      href={`/student/submit/${subject.id}`}
                      className="block p-3 bg-gradient-to-r from-[#a0704d] to-[#8b5a3c] text-[#f5ede0] rounded-lg hover:shadow-lg transition-all text-sm font-medium text-center"
                    >
                      {subject.code}
                    </Link>
                  ))}
                  {availableSubjects.length > 5 && (
                    <Link
                      href="/student/subjects"
                      className="block p-3 bg-[#d4c4b0] text-[#2a1810] rounded-lg hover:bg-[#c9a876] transition-colors text-sm font-medium text-center"
                    >
                      View All ({availableSubjects.length} available)
                    </Link>
                  )}
                </div>
              ) : (
                <p className="text-sm text-[#6b5448] text-center py-4">
                  You have submitted marks for all available subjects!
                </p>
              )}
            </div>
          </div>
        </div>
      </main>
    </div>
  );
}
