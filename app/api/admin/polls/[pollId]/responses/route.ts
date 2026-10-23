import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ pollId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email || session.user.role !== 'admin') {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const { pollId } = await params;

    const poll = await prisma.poll.findUnique({
      where: { id: pollId },
      include: {
        responses: {
          select: {
            marks: true,
          }
        }
      }
    });

    if (!poll) {
      return NextResponse.json({ error: 'Poll not found' }, { status: 404 });
    }

    // PRIVACY ENFORCEMENT:
    // Individual student identities and responses are never revealed to administrators.
    // Only aggregate statistical data is provided.
    const marks = poll.responses.map(r => r.marks);
    const totalResponses = marks.length;

    if (totalResponses === 0) {
      return NextResponse.json({
        summary: {
          totalResponses: 0,
          averageMarks: 0,
          highestMarks: 0,
          lowestMarks: 0,
          distribution: {
            '0-20': 0,
            '21-40': 0,
            '41-60': 0,
            '61-80': 0,
            '81-100': 0,
          }
        }
      });
    }

    const sum = marks.reduce((acc, m) => acc + m, 0);
    const avg = Math.round((sum / totalResponses) * 10) / 10;
    const sorted = [...marks].sort((a, b) => a - b);
    const highest = sorted[sorted.length - 1];
    const lowest = sorted[0];

    const distribution: Record<string, number> = {
      '0-20': 0,
      '21-40': 0,
      '41-60': 0,
      '61-80': 0,
      '81-100': 0,
    };

    for (const m of marks) {
      if (m <= 20) distribution['0-20']++;
      else if (m <= 40) distribution['21-40']++;
      else if (m <= 60) distribution['41-60']++;
      else if (m <= 80) distribution['61-80']++;
      else distribution['81-100']++;
    }

    return NextResponse.json({
      summary: {
        totalResponses,
        averageMarks: avg,
        highestMarks: highest,
        lowestMarks: lowest,
        distribution,
      }
    });
  } catch (error) {
    console.error('Error fetching poll summary:', error);
    return NextResponse.json({ error: 'Failed to fetch poll summary' }, { status: 500 });
  }
}
