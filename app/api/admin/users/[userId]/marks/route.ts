import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ userId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email || session.user.role !== 'admin') {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    // PRIVACY POLICY ENFORCEMENT:
    // Individual student marks are strictly private and confidential.
    // Administrators are restricted from viewing any student's individual marks.
    // Marks are only accessible by the student who uploaded them and system calculation engines.
    return NextResponse.json(
      { 
        error: 'Access Denied: Student marks are confidential and protected by platform privacy regulations. Administrators cannot inspect individual student marks. Only the student who uploaded them and the internal prediction engine have access.' 
      }, 
      { status: 403 }
    );
  } catch (error) {
    console.error('Error in user marks route:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
