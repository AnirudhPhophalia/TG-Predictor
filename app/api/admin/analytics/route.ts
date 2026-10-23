import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth/next';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/prisma';

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    // Check if user is admin
    const adminUser = await prisma.user.findUnique({
      where: { email: session.user.email },
    });

    if (!adminUser || adminUser.role !== 'admin') {
      return NextResponse.json({ error: 'Admin access required' }, { status: 403 });
    }

    // Get dashboard statistics
    const totalSubjects = await prisma.subject.count();
    const activeSubjects = await prisma.subject.count({
      where: { isActive: true },
    });
    const totalSubmissions = await prisma.mark.count();
    const totalStudents = await prisma.user.count({
      where: { role: 'student' },
    });
    const pendingEditRequests = await prisma.editRequest.count({
      where: { status: 'pending' },
    });

    // PRIVACY ENFORCEMENT:
    // Recent submissions only return anonymized subject activity without student names, emails, or individual marks.
    const recentSubmissions = await prisma.mark.findMany({
      orderBy: { submittedAt: 'desc' },
      take: 10,
      select: {
        id: true,
        submittedAt: true,
        subject: {
          select: { name: true, code: true },
        },
      },
    });

    // Get grade distribution
    const grades = await prisma.mark.groupBy({
      by: ['predictedGrade'],
      _count: true,
      where: { predictedGrade: { not: null } },
    });

    // Get top performing subjects (by average marks)
    const subjectStats = await prisma.statistics.findMany({
      orderBy: { averageMarks: 'desc' },
      take: 5,
      include: {
        subject: {
          select: { name: true, code: true },
        },
      },
    });

    return NextResponse.json({
      dashboard: {
        totalSubjects,
        activeSubjects,
        totalSubmissions,
        totalStudents,
        pendingEditRequests,
      },
      recentSubmissions,
      gradeDistribution: grades.reduce(
        (acc, item) => {
          acc[item.predictedGrade || 'N/A'] = item._count;
          return acc;
        },
        {} as Record<string, number>
      ),
      topSubjects: subjectStats,
    });
  } catch (error) {
    console.error('Error fetching analytics:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
