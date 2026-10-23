'use client';

import { useSession } from 'next-auth/react';
import { useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import useSWR from 'swr';
import { ArrowLeft, BookOpen, Plus, Filter } from 'lucide-react';
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
  };
}

interface Mark {
  subjectId: string;
}

export default function SubjectsPage() {
  const { data: session, status } = useSession();
  const router = useRouter();
  const [submittedSubjects, setSubmittedSubjects] = useState<Set<string>>(new Set());
  const [selectedSemester, setSelectedSemester] = useState<number | null>(null);

  // Fetch subjects
  const { data: subjectsData, isLoading: subjectsLoading } = useSWR(
    '/api/subjects',
    (url) => fetch(url).then((r) => r.json()),
    { revalidateOnFocus: false }
  );

  // Fetch marks
  const { data: marksData } = useSWR(
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
      const submitted = new Set(
        marksData.marks.map((m: Mark) => m.subjectId)
      );
      setSubmittedSubjects(submitted);
    }
  }, [marksData]);

  if (status === 'loading' || subjectsLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading subjects...</p>
        </div>
      </div>
    );
  }

  const maxSemester = Math.min(4, getStudentSemester(session?.user?.email));
  
  const formatSemester = (sem: number) => {
    if (sem === 1) return 'Pool A';
    if (sem === 2) return 'Pool B';
    return `Semester ${sem}`;
  };

  const subjects: Subject[] = (subjectsData?.subjects || []).filter((s: Subject) => {
    if (s.semester <= 2) return true;
    return s.semester <= maxSemester;
  });
  const semesters = [...new Set(subjects.map((s) => s.semester))].sort();
  
  const filteredSubjects = selectedSemester
    ? subjects.filter((s) => s.semester === selectedSemester)
    : subjects;

  const availableSubjects = filteredSubjects.filter(
    (s) => !submittedSubjects.has(s.id)
  );
  const submittedForSemester = filteredSubjects.filter((s) =>
    submittedSubjects.has(s.id)
  );

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
          <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
            All Subjects
          </h1>
          <p className="text-[#d4c4b0] mt-1">
            {subjects.length} subjects available • {submittedSubjects.size} submitted
          </p>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 pb-12">
        {/* Filter Section */}
        <div className="vintage-card mb-8">
          <div className="flex items-center gap-4">
            <Filter className="w-5 h-5 text-[#6b4423]" />
            <div className="flex flex-wrap gap-2">
              <button
                onClick={() => setSelectedSemester(null)}
                className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                  selectedSemester === null
                    ? 'bg-[#a0704d] text-[#f5ede0]'
                    : 'bg-[#d9cdc0] text-[#3d2817] hover:bg-[#c9a876]'
                }`}
              >
                All Semesters
              </button>
              {semesters.map((sem) => (
                <button
                  key={sem}
                  onClick={() => setSelectedSemester(sem)}
                  className={`px-4 py-2 rounded-lg font-medium transition-colors ${
                    selectedSemester === sem
                      ? 'bg-[#a0704d] text-[#f5ede0]'
                      : 'bg-[#d9cdc0] text-[#3d2817] hover:bg-[#c9a876]'
                  }`}
                >
                  {formatSemester(sem)}
                </button>
              ))}
            </div>
          </div>
        </div>

        {/* Available Subjects */}
        {availableSubjects.length > 0 && (
          <div className="mb-12">
            <h2 className="text-2xl font-bold text-[#2a1810] mb-6" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Available to Submit
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {availableSubjects.map((subject) => (
                <Link
                  key={subject.id}
                  href={`/student/submit/${subject.id}`}
                  className="vintage-card group hover:shadow-lg transition-all hover:bg-[#faf6f1] cursor-pointer"
                >
                  <div className="flex items-start justify-between mb-4">
                    <div className="w-10 h-10 bg-gradient-to-br from-[#a0704d] to-[#8b5a3c] rounded-lg flex items-center justify-center group-hover:scale-110 transition-transform">
                      <BookOpen className="w-5 h-5 text-[#f5ede0]" />
                    </div>
                    <Plus className="w-5 h-5 text-[#a0704d] opacity-0 group-hover:opacity-100 transition-opacity" />
                  </div>
                  <h3 className="font-bold text-[#2a1810] group-hover:text-[#6b4423]">
                    {subject.code}
                  </h3>
                  <p className="text-sm text-[#6b5448] mt-1">{subject.name}</p>
                  <div className="flex justify-between items-center mt-4 pt-4 border-t border-[#d9cdc0]">
                    <span className="text-xs text-[#6b5448]">{formatSemester(subject.semester)}</span>
                    <span className="text-xs font-medium text-[#6b4423]">{subject.credits} credits</span>
                  </div>
                </Link>
              ))}
            </div>
          </div>
        )}

        {/* Submitted Subjects */}
        {submittedForSemester.length > 0 && (
          <div>
            <h2 className="text-2xl font-bold text-[#2a1810] mb-6" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
              Already Submitted
            </h2>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
              {submittedForSemester.map((subject) => (
                <div
                  key={subject.id}
                  className="vintage-card opacity-75 border-2 border-dashed border-[#d9cdc0]"
                >
                  <div className="flex items-start justify-between mb-4">
                    <div className="w-10 h-10 bg-gradient-to-br from-[#d4c4b0] to-[#c9a876] rounded-lg flex items-center justify-center">
                      <BookOpen className="w-5 h-5 text-[#6b5448]" />
                    </div>
                    <span className="text-xs font-medium bg-green-100 text-green-700 px-2 py-1 rounded">
                      Submitted
                    </span>
                  </div>
                  <h3 className="font-bold text-[#3d2817]">{subject.code}</h3>
                  <p className="text-sm text-[#6b5448] mt-1">{subject.name}</p>
                  <div className="flex justify-between items-center mt-4 pt-4 border-t border-[#d9cdc0]">
                    <span className="text-xs text-[#6b5448]">{formatSemester(subject.semester)}</span>
                    <span className="text-xs font-medium text-[#6b5448]">{subject.credits} credits</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* No subjects message */}
        {availableSubjects.length === 0 && submittedForSemester.length === 0 && (
          <div className="vintage-card text-center py-12">
            <p className="text-[#6b5448]">
              {selectedSemester
                ? `No subjects found for Semester ${selectedSemester}`
                : 'No subjects available'}
            </p>
          </div>
        )}
      </main>
    </div>
  );
}
