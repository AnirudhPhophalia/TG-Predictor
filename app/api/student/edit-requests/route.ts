import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth/next';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/prisma';

export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { markId, reason, newMarks } = body;

    // Validate input
    if (!markId || !reason) {
      return NextResponse.json({ error: 'Missing required fields: markId, reason' }, { status: 400 });
    }

    // Get user
    const user = await prisma.user.findUnique({
      where: { email: session.user.email },
    });

    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    // Check if mark exists and belongs to user
    const mark = await prisma.mark.findUnique({
      where: { id: markId },
      include: { subject: true },
    });

    if (!mark || mark.userId !== user.id) {
      return NextResponse.json({ error: 'Mark not found or access denied' }, { status: 404 });
    }

    // Check if there's already a pending unlock request
    const existingRequest = await prisma.editRequest.findFirst({
      where: {
        userId: user.id,
        markId: markId,
        status: 'pending',
      },
    });

    if (existingRequest) {
      return NextResponse.json(
        { error: 'You already have a pending unlock request for this subject' },
        { status: 400 }
      );
    }

    // Create unlock request (newMarks is optional/defaulted to protect privacy)
    const editRequest = await prisma.editRequest.create({
      data: {
        userId: user.id,
        markId: markId,
        newMarks: newMarks ? Number(newMarks) : 0,
        reason: reason,
        status: 'pending',
      },
    });

    // Log audit without exposing raw marks to admin
    await prisma.auditLog.create({
      data: {
        userId: user.id,
        action: 'unlock_requested',
        resourceType: 'mark',
        resourceId: markId,
        changes: JSON.stringify({
          reason: reason,
        }),
      },
    });

    return NextResponse.json(editRequest, { status: 201 });
  } catch (error) {
    console.error('Error creating unlock request:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const user = await prisma.user.findUnique({
      where: { email: session.user.email },
    });

    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    // Get all edit requests for this user
    const editRequests = await prisma.editRequest.findMany({
      where: { userId: user.id },
      orderBy: { createdAt: 'desc' },
    });

    // Fetch marks to get subject info
    const marks = await prisma.mark.findMany({
      where: { userId: user.id },
      include: { subject: true },
    });

    const requestsWithDetails = editRequests.map((req) => {
      const mark = marks.find((m) => m.id === req.markId);
      return {
        ...req,
        subjectId: mark?.subjectId,
        subjectCode: mark?.subject.code || 'Unknown',
        subjectName: mark?.subject.name || 'Unknown Subject',
        isLocked: mark?.isLocked ?? true,
      };
    });

    return NextResponse.json(requestsWithDetails);
  } catch (error) {
    console.error('Error fetching unlock requests:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
