import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ pollId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email || session.user.email !== 'anirudhphophalia@gmail.com') {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const { pollId } = await params;
    const body = await request.json();
    
    // Parse the fields from the body (allow null to clear them)
    const { cutoffA, cutoffAMinus, cutoffB, cutoffBMinus, cutoffC, cutoffD } = body;

    const poll = await prisma.poll.findUnique({ where: { id: pollId } });
    if (!poll) {
      return NextResponse.json({ error: 'Poll not found' }, { status: 404 });
    }

    const updatedPoll = await prisma.poll.update({
      where: { id: pollId },
      data: {
        cutoffA: cutoffA !== undefined ? cutoffA : undefined,
        cutoffAMinus: cutoffAMinus !== undefined ? cutoffAMinus : undefined,
        cutoffB: cutoffB !== undefined ? cutoffB : undefined,
        cutoffBMinus: cutoffBMinus !== undefined ? cutoffBMinus : undefined,
        cutoffC: cutoffC !== undefined ? cutoffC : undefined,
        cutoffD: cutoffD !== undefined ? cutoffD : undefined,
      }
    });

    return NextResponse.json({ success: true, poll: updatedPoll });
  } catch (error) {
    console.error('Error updating poll cutoffs:', error);
    return NextResponse.json({ error: 'Failed to update cutoffs' }, { status: 500 });
  }
}
