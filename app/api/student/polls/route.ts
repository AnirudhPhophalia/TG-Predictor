import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const user = await prisma.user.findUnique({ where: { email: session.user.email } });
    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    const polls = await prisma.poll.findMany({
      where: { isActive: true },
      include: {
        subject: true,
        responses: {
          where: { userId: user.id }
        }
      },
      orderBy: { createdAt: 'desc' }
    });

    const now = new Date();
    
    // Map polls to include a calculated state and whether user has submitted
    const formattedPolls = polls.map((poll: any) => {
      const hasSubmitted = poll.responses.length > 0;
      const myResponse = hasSubmitted ? poll.responses[0].marks : null;
      
      const endTime = new Date(poll.endTime);
      const isEnded = now >= endTime;
      const resultsAvailableTime = new Date(endTime.getTime() + 10 * 60000); // 10 mins after end
      const resultsAvailable = now >= resultsAvailableTime;

      return {
        id: poll.id,
        subject: poll.subject,
        question: poll.question,
        endTime: poll.endTime,
        isEnded,
        resultsAvailable,
        resultsAvailableTime,
        hasSubmitted,
        myResponse,
      };
    });

    return NextResponse.json({ polls: formattedPolls });
  } catch (error) {
    console.error('Error fetching polls:', error);
    return NextResponse.json({ error: 'Failed to fetch polls' }, { status: 500 });
  }
}
