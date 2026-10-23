/**
 * Admin Subjects API
 * POST: Create new subject
 * GET: List all subjects
 */

import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';
import { createAuditLog } from '@/lib/services/db';

async function isAdmin(email: string): Promise<boolean> {
  const user = await prisma.user.findUnique({
    where: { email },
  });
  return user?.role === 'admin';
}

export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);

    if (!session?.user?.email || !(await isAdmin(session.user.email))) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { code, name, semester, branch, credits } = body;

    // Validation
    if (!code || !name || semester === undefined || !branch) {
      return NextResponse.json(
        { error: 'Missing required fields' },
        { status: 400 }
      );
    }

    // Check if subject already exists
    const existing = await prisma.subject.findUnique({
      where: { code },
    });

    if (existing) {
      return NextResponse.json(
        { error: 'Subject with this code already exists' },
        { status: 400 }
      );
    }

    const subject = await prisma.subject.create({
      data: {
        code,
        name,
        semester,
        branch,
        credits: credits || 3,
        isActive: true,
      },
    });

    // Create historical cutoff with default values
    await prisma.historicalCutoff.create({
      data: {
        subjectId: subject.id,
        cutoffA: 85,
        cutoffAMinus: 75,
        cutoffB: 65,
        cutoffBMinus: 55,
        cutoffC: 45,
        cutoffD: 35,
      },
    });

    // Create empty statistics record
    await prisma.statistics.create({
      data: {
        subjectId: subject.id,
        totalResponses: 0,
        averageMarks: 0,
        medianMarks: 0,
        modeMarks: 0,
        highestMarks: 0,
        lowestMarks: 0,
        standardDeviation: 0,
        passPercentage: 0,
        failurePercentage: 0,
      },
    });

    const user = await prisma.user.findUnique({
      where: { email: session.user.email },
    });

    if (user) {
      await createAuditLog(
        user.id,
        'subject_created',
        'subject',
        subject.id,
        { code, name, semester, branch, credits },
        request.headers.get('x-forwarded-for') || '',
        request.headers.get('user-agent') || ''
      );
    }

    return NextResponse.json({
      success: true,
      subject,
    });
  } catch (error) {
    console.error('[Admin Subjects API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to create subject' },
      { status: 500 }
    );
  }
}

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);

    if (!session?.user?.email || !(await isAdmin(session.user.email))) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const subjects = await prisma.subject.findMany({
      include: {
        statistics: true,
        historicalCutoffs: true,
      },
      orderBy: [
        { semester: 'asc' },
        { code: 'asc' },
      ],
    });

    return NextResponse.json({
      subjects,
      count: subjects.length,
    });
  } catch (error) {
    console.error('[Admin Subjects GET API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch subjects' },
      { status: 500 }
    );
  }
}
