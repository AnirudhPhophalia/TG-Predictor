'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { useSession } from 'next-auth/react';
import { 
  Shield, 
  ShieldAlert, 
  User as UserIcon, 
  Search, 
  ArrowLeft,
  Lock
} from 'lucide-react';
import Link from 'next/link';

interface User {
  id: string;
  name: string | null;
  email: string;
  role: 'student' | 'admin';
  createdAt: string;
}

export default function AdminUsersPage() {
  const { data: session } = useSession();
  const [searchTerm, setSearchTerm] = useState('');
  const [isUpdating, setIsUpdating] = useState<string | null>(null);

  const { data, mutate } = useSWR<{ users: User[] }>(
    '/api/admin/users',
    (url) => fetch(url).then((res) => res.json())
  );

  const handleRoleChange = async (userId: string, currentRole: 'student' | 'admin') => {
    const newRole = currentRole === 'admin' ? 'student' : 'admin';
    const confirmMessage = newRole === 'admin' 
      ? 'Are you sure you want to promote this user to Admin?' 
      : 'Are you sure you want to demote this Admin to Student?';

    if (!confirm(confirmMessage)) return;

    setIsUpdating(userId);
    try {
      const res = await fetch('/api/admin/users', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ userId, role: newRole }),
      });
      
      const data = await res.json();
      if (!res.ok) throw new Error(data.error || 'Failed to update role');
      
      mutate();
    } catch (err: any) {
      alert(err.message || 'An error occurred while updating the role.');
    } finally {
      setIsUpdating(null);
    }
  };

  const users = data?.users || [];
  const filteredUsers = users.filter((u) => 
    u.email.toLowerCase().includes(searchTerm.toLowerCase()) || 
    (u.name && u.name.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="min-h-screen bg-[#f5ede0] burnt-texture">
      <header className="bg-gradient-to-r from-[#3d2817] to-[#5a3f2b] text-[#f5ede0] shadow-lg parchment-edge mb-8">
        <div className="max-w-6xl mx-auto px-6 py-8">
          <Link
            href="/admin"
            className="inline-flex items-center gap-2 text-[#d4c4b0] hover:text-[#f5ede0] mb-4 font-medium text-sm"
          >
            <ArrowLeft className="w-4 h-4" />
            Back to Dashboard
          </Link>
          <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
            <div>
              <h1 className="text-3xl font-bold" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>
                User Management
              </h1>
              <p className="text-[#d4c4b0] mt-1">Manage roles and permissions for {users.length} total users</p>
            </div>
            
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-[#8b5a3c]" />
              <input
                type="text"
                placeholder="Search by name or email..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-9 pr-4 py-2 rounded-lg border border-[#a0704d] bg-[#f5ede0] text-[#2a1810] focus:outline-none focus:ring-2 focus:ring-[#a0704d] w-full md:w-64 placeholder-[#8b5a3c]"
              />
            </div>
          </div>
        </div>
      </header>

      <main className="max-w-6xl mx-auto px-6 pb-12">
        {/* Privacy Notice Banner */}
        <div className="mb-6 p-4 rounded-lg bg-[#faf6f1] border border-[#d9cdc0] flex items-center gap-3">
          <Lock className="w-5 h-5 text-[#6b4423] flex-shrink-0" />
          <div className="text-sm text-[#6b5448]">
            <strong className="text-[#3d2817]">Student Marks Privacy Active:</strong> Individual student scores are confidential. Administrators cannot view student marks. Marks are strictly accessible only by the student who submitted them and internal statistical calculation engines.
          </div>
        </div>

        <div className="vintage-card overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left">
              <thead className="border-b-2 border-[#d9cdc0]">
                <tr>
                  <th className="px-6 py-4 font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>User</th>
                  <th className="px-6 py-4 font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>Email</th>
                  <th className="px-6 py-4 font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>Joined</th>
                  <th className="px-6 py-4 font-bold text-[#2a1810]" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>Role</th>
                  <th className="px-6 py-4 font-bold text-[#2a1810] text-right" style={{ fontFamily: 'var(--font-playfair, sans-serif)' }}>Actions</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-[#d9cdc0]">
                {filteredUsers.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="px-6 py-8 text-center text-[#6b5448]">
                      No users found matching "{searchTerm}"
                    </td>
                  </tr>
                ) : (
                  filteredUsers.map((user) => (
                    <tr key={user.id} className="hover:bg-[#faf6f1] transition-colors">
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-[#eae0d5] flex items-center justify-center">
                            <UserIcon className="w-4 h-4 text-[#a0704d]" />
                          </div>
                          <span className="font-medium text-[#3d2817]">{user.name || 'Unknown'}</span>
                        </div>
                      </td>
                      <td className="px-6 py-4 text-[#6b5448]">{user.email}</td>
                      <td className="px-6 py-4 text-[#6b5448]">
                        {new Date(user.createdAt).toLocaleDateString()}
                      </td>
                      <td className="px-6 py-4">
                        <span className={`inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-medium ${
                          user.role === 'admin' 
                            ? 'bg-[#a0704d]/20 text-[#6b4423]' 
                            : 'bg-[#d9cdc0]/50 text-[#6b5448]'
                        }`}>
                          {user.role === 'admin' ? <ShieldAlert className="w-3 h-3" /> : <Shield className="w-3 h-3" />}
                          {user.role}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-right flex items-center justify-end gap-3">
                        <button
                          onClick={() => handleRoleChange(user.id, user.role)}
                          disabled={isUpdating === user.id || session?.user?.email === user.email}
                          className={`text-sm font-medium px-3 py-1 rounded ${
                            session?.user?.email === user.email 
                              ? 'text-gray-400 cursor-not-allowed' 
                              : user.role === 'admin'
                                ? 'bg-red-50 text-red-600 hover:bg-red-100'
                                : 'bg-[#e8d7c3] text-[#6b4423] hover:bg-[#d9cdc0]'
                          }`}
                        >
                          {isUpdating === user.id ? 'Updating...' : user.role === 'admin' ? 'Demote to Student' : 'Promote to Admin'}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </main>
    </div>
  );
}
