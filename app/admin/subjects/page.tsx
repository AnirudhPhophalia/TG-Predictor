'use client';

import { useState, useEffect } from 'react';
import { ArrowLeft, Plus, Edit2, Trash2, Save } from 'lucide-react';
import Link from 'next/link';

interface Subject {
  id: string;
  code: string;
  name: string;
  semester: number;
  branch: string;
  credits: number;
  isActive: boolean;
  statistics?: {
    totalResponses: number;
  };
}

export default function AdminSubjectsPage() {
  const [subjects, setSubjects] = useState<Subject[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [showForm, setShowForm] = useState(false);
  const [formData, setFormData] = useState({
    code: '',
    name: '',
    semester: 1,
    branch: 'CSE',
    credits: 3,
  });
  const [error, setError] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  useEffect(() => {
    fetchSubjects();
  }, []);

  const fetchSubjects = async () => {
    try {
      setIsLoading(true);
      const response = await fetch('/api/admin/subjects');
      if (!response.ok) throw new Error('Failed to fetch subjects');
      const data = await response.json();
      setSubjects(data.subjects);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    } finally {
      setIsLoading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setError(null);

    try {
      const response = await fetch('/api/admin/subjects', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(formData),
      });

      if (!response.ok) {
        const data = await response.json();
        throw new Error(data.error || 'Failed to create subject');
      }

      setSuccessMessage('Subject created successfully!');
      setFormData({ code: '', name: '', semester: 1, branch: 'CSE', credits: 3 });
      setShowForm(false);
      
      setTimeout(() => {
        setSuccessMessage(null);
        fetchSubjects();
      }, 2000);
    } catch (err) {
      setError(err instanceof Error ? err.message : 'An error occurred');
    }
  };

  if (isLoading) {
    return (
      <div className="flex items-center justify-center min-h-screen">
        <div className="text-center">
          <div className="w-12 h-12 bg-gradient-to-br from-[#a0704d] to-[#6b4423] rounded-full mx-auto mb-4 animate-pulse"></div>
          <p className="text-[#6b5448]">Loading subjects...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      {/* Header */}
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge mb-8">
        <div className="max-w-7xl mx-auto px-6 py-8">
          <Link
            href="/admin"
            className="inline-flex items-center gap-2 text-[#d4c4b0] hover:text-[#f5ede0] mb-4 font-medium text-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Admin
          </Link>
          <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
            Manage Subjects
          </h1>
          <p className="text-[#d4c4b0] mt-1">Total: {subjects.length} subjects</p>
        </div>
      </header>

      <main className="max-w-7xl mx-auto px-6 pb-12">
        {/* Create Subject Form */}
        {showForm && (
          <div className="vintage-card mb-8">
            <h2 className="text-xl font-bold text-[#2a1810] mb-6">Create New Subject</h2>
            
            {error && (
              <div className="p-4 bg-red-50 border border-red-200 rounded-lg mb-6 text-red-700 text-sm">
                {error}
              </div>
            )}

            {successMessage && (
              <div className="p-4 bg-green-50 border border-green-200 rounded-lg mb-6 text-green-700 text-sm">
                {successMessage}
              </div>
            )}

            <form onSubmit={handleSubmit} className="space-y-4">
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label className="block text-sm font-medium text-[#3d2817] mb-1">
                    Subject Code *
                  </label>
                  <input
                    type="text"
                    value={formData.code}
                    onChange={(e) =>
                      setFormData({ ...formData, code: e.target.value.toUpperCase() })
                    }
                    required
                    placeholder="e.g., CS101"
                    className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3d2817] mb-1">
                    Subject Name *
                  </label>
                  <input
                    type="text"
                    value={formData.name}
                    onChange={(e) =>
                      setFormData({ ...formData, name: e.target.value })
                    }
                    required
                    placeholder="e.g., Data Structures"
                    className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3d2817] mb-1">
                    Semester *
                  </label>
                  <input
                    type="number"
                    value={formData.semester}
                    onChange={(e) =>
                      setFormData({ ...formData, semester: parseInt(e.target.value) })
                    }
                    required
                    min="1"
                    max="8"
                    className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                  />
                </div>
                <div>
                  <label className="block text-sm font-medium text-[#3d2817] mb-1">
                    Branch *
                  </label>
                  <select
                    value={formData.branch}
                    onChange={(e) =>
                      setFormData({ ...formData, branch: e.target.value })
                    }
                    className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                  >
                    <option value="CSE">CSE</option>
                    <option value="ME">ME</option>
                    <option value="ECE">ECE</option>
                    <option value="EEE">EEE</option>
                    <option value="CIVIL">CIVIL</option>
                  </select>
                </div>
              </div>
              <div>
                <label className="block text-sm font-medium text-[#3d2817] mb-1">
                  Credits *
                </label>
                <input
                  type="number"
                  value={formData.credits}
                  onChange={(e) =>
                    setFormData({ ...formData, credits: parseInt(e.target.value) })
                  }
                  required
                  min="1"
                  max="6"
                  className="w-full px-3 py-2 border border-[#d9cdc0] rounded-lg focus:outline-none focus:ring-2 focus:ring-[#a0704d] bg-[#faf6f1]"
                />
              </div>

              <div className="flex gap-4">
                <button
                  type="submit"
                  className="px-6 py-2 bg-gradient-to-br from-[#a0704d] to-[#8b5a3c] text-[#f5ede0] rounded-lg hover:shadow-lg transition-all font-medium flex items-center gap-2"
                >
                  <Save className="w-4 h-4" />
                  Create Subject
                </button>
                <button
                  type="button"
                  onClick={() => setShowForm(false)}
                  className="px-6 py-2 bg-[#d9cdc0] text-[#3d2817] rounded-lg hover:bg-[#c9a876] transition-colors font-medium"
                >
                  Cancel
                </button>
              </div>
            </form>
          </div>
        )}

        {/* Subjects Table */}
        <div className="vintage-card">
          <div className="flex items-center justify-between mb-6 pb-4 border-b border-[#d9cdc0]">
            <h2 className="text-xl font-bold text-[#2a1810]">Subjects List</h2>
            {!showForm && (
              <button
                onClick={() => setShowForm(true)}
                className="px-4 py-2 bg-gradient-to-br from-[#a0704d] to-[#8b5a3c] text-[#f5ede0] rounded-lg hover:shadow-lg transition-all font-medium flex items-center gap-2"
              >
                <Plus className="w-4 h-4" />
                Add Subject
              </button>
            )}
          </div>

          {subjects.length > 0 ? (
            <div className="overflow-x-auto">
              <table className="w-full">
                <thead>
                  <tr className="border-b border-[#d9cdc0]">
                    <th className="text-left px-4 py-3 font-semibold text-[#3d2817]">Code</th>
                    <th className="text-left px-4 py-3 font-semibold text-[#3d2817]">Name</th>
                    <th className="text-left px-4 py-3 font-semibold text-[#3d2817]">Sem</th>
                    <th className="text-left px-4 py-3 font-semibold text-[#3d2817]">Branch</th>
                    <th className="text-left px-4 py-3 font-semibold text-[#3d2817]">Credits</th>
                    <th className="text-left px-4 py-3 font-semibold text-[#3d2817]">Submissions</th>
                    <th className="text-left px-4 py-3 font-semibold text-[#3d2817]">Actions</th>
                  </tr>
                </thead>
                <tbody>
                  {subjects.map((subject) => (
                    <tr key={subject.id} className="border-b border-[#e5d9cf] hover:bg-[#faf6f1]">
                      <td className="px-4 py-3 font-medium text-[#3d2817]">{subject.code}</td>
                      <td className="px-4 py-3 text-[#3d2817]">{subject.name}</td>
                      <td className="px-4 py-3 text-[#6b5448]">{subject.semester}</td>
                      <td className="px-4 py-3 text-[#6b5448]">{subject.branch}</td>
                      <td className="px-4 py-3 text-[#6b5448]">{subject.credits}</td>
                      <td className="px-4 py-3 text-[#6b5448]">
                        {subject.statistics?.totalResponses || 0}
                      </td>
                      <td className="px-4 py-3">
                        <div className="flex gap-2">
                          <button className="p-2 hover:bg-[#e5d9cf] rounded-lg transition-colors">
                            <Edit2 className="w-4 h-4 text-[#6b4423]" />
                          </button>
                          <button className="p-2 hover:bg-red-100 rounded-lg transition-colors">
                            <Trash2 className="w-4 h-4 text-red-600" />
                          </button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <div className="text-center py-12 text-[#6b5448]">
              No subjects found. Create one to get started!
            </div>
          )}
        </div>
      </main>
    </div>
  );
}
