import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';

export async function POST(
  request: NextRequest,
  { params }: { params: Promise<{ pollId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const user = await prisma.user.findUnique({ where: { email: session.user.email } });
    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const { marks } = await request.json();

    if (marks === undefined || marks === null || isNaN(marks)) {
      return NextResponse.json({ error: 'Invalid marks' }, { status: 400 });
    }

    const { pollId } = await params;
    const poll = await prisma.poll.findUnique({ where: { id: pollId } });
    if (!poll) {
      return NextResponse.json({ error: 'Poll not found' }, { status: 404 });
    }

    const now = new Date();
    if (now >= new Date(poll.endTime)) {
      return NextResponse.json({ error: 'Poll has ended' }, { status: 403 });
    }

    const response = await prisma.pollResponse.create({
      data: {
        pollId: poll.id,
        userId: user.id,
        marks: parseFloat(marks),
      }
    });

    return NextResponse.json({ success: true, response });
  } catch (error: any) {
    console.error('Error submitting poll:', error);
    if (error.code === 'P2002') {
      return NextResponse.json({ error: 'You have already submitted this poll' }, { status: 400 });
    }
    return NextResponse.json({ error: 'Failed to submit poll' }, { status: 500 });
  }
}

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ pollId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const user = await prisma.user.findUnique({ where: { email: session.user.email } });
    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const { pollId } = await params;

    const poll = await prisma.poll.findUnique({
      where: { id: pollId },
      include: {
        responses: true,
      }
    });

    if (!poll) {
      return NextResponse.json({ error: 'Poll not found' }, { status: 404 });
    }

    const now = new Date();
    const resultsAvailableTime = new Date(new Date(poll.endTime).getTime() + 10 * 60000);
    
    // Purge fake/invalid responses after the poll ends
    if (now >= new Date(poll.endTime)) {
      await prisma.pollResponse.deleteMany({
        where: {
          pollId,
          OR: [
            { marks: { gt: 95 } },
            { marks: { lt: 25 } }
          ]
        }
      });
    }

    if (now < resultsAvailableTime) {
      return NextResponse.json({ error: 'Results are not available yet' }, { status: 403 });
    }

    // Re-fetch responses to ensure we don't include the deleted ones in the current request
    const cleanPoll = await prisma.poll.findUnique({
      where: { id: pollId },
      include: { responses: true }
    });
    
    if (!cleanPoll) {
      return NextResponse.json({ error: 'Poll not found' }, { status: 404 });
    }

    const myResponse = cleanPoll.responses.find(r => r.userId === user.id);
    if (!myResponse) {
      return NextResponse.json({ error: 'You did not participate in this poll' }, { status: 403 });
    }

    // Calculate grades based on all poll responses natively
    const allMarks = cleanPoll.responses.map(r => r.marks).sort((a, b) => a - b);
    const count = allMarks.length;
    
    // Very basic percentile and grade calculation based on poll data
    const myMarks = myResponse.marks;
    const rank = allMarks.filter(m => m <= myMarks).length;
    const percentile = count > 0 ? (rank / count) * 100 : 0;
    
    let grade = 'E';
    
    // Check if admin has set custom manual cutoffs for this poll
    const hasManualCutoffs = cleanPoll.cutoffA !== null || cleanPoll.cutoffB !== null || cleanPoll.cutoffC !== null;
    
    if (hasManualCutoffs) {
      if (cleanPoll.cutoffA !== null && myMarks >= cleanPoll.cutoffA) grade = 'A';
      else if (cleanPoll.cutoffAMinus !== null && myMarks >= cleanPoll.cutoffAMinus) grade = 'A-';
      else if (cleanPoll.cutoffB !== null && myMarks >= cleanPoll.cutoffB) grade = 'B';
      else if (cleanPoll.cutoffBMinus !== null && myMarks >= cleanPoll.cutoffBMinus) grade = 'B-';
      else if (cleanPoll.cutoffC !== null && myMarks >= cleanPoll.cutoffC) grade = 'C';
      else if (cleanPoll.cutoffD !== null && myMarks >= cleanPoll.cutoffD) grade = 'D';
    } else {
      // Fallback to dynamic percentile calculation
      if (percentile >= 90) grade = 'A';
      else if (percentile >= 75) grade = 'A-';
      else if (percentile >= 60) grade = 'B';
      else if (percentile >= 45) grade = 'B-';
      else if (percentile >= 30) grade = 'C';
      else if (percentile >= 15) grade = 'D';
    }

    return NextResponse.json({ 
      grade, 
      percentile: percentile.toFixed(2), 
      totalParticipants: count,
      averageMarks: (allMarks.reduce((a,b)=>a+b, 0) / count).toFixed(2),
      myMarks
    });

  } catch (error) {
    console.error('Error fetching poll results:', error);
    return NextResponse.json({ error: 'Failed to fetch poll results' }, { status: 500 });
  }
}
