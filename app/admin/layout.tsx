import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import { notFound } from 'next/navigation';

export default async function AdminLayout({ children }: { children: React.ReactNode }) {
  const session = await getServerSession(authOptions);
  
  if (!session?.user?.email || session.user.email !== 'anirudhphophalia@gmail.com') {
    notFound();
  }

  return <>{children}</>;
}
