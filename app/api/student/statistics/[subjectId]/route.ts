/**
 * Student Statistics API
 * GET: Get statistics for a specific subject
 */

import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';
import { canMakePrediction } from '@/lib/services/grade-predictor';

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ subjectId: string }> }
) {
  try {
    const session = await getServerSession(authOptions);

    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const { subjectId } = await params;

    // Get subject
    const subject = await prisma.subject.findUnique({
      where: { id: subjectId },
      include: {
        statistics: true,
        historicalCutoffs: true,
      },
    });

    if (!subject) {
      return NextResponse.json({ error: 'Subject not found' }, { status: 404 });
    }

    // Get all marks for this subject
    const marks = await prisma.mark.findMany({
      where: {
        subjectId,
        isSubmitted: true,
      },
      select: {
        id: true,
        userId: true,
        totalMarks: true,
        estMarks: true,
        mstMarks: true,
        quizMarks: true,
        assignmentMarks: true,
        labMarks: true,
        practicalMarks: true,
        predictedGrade: true,
      },
      orderBy: {
        totalMarks: 'desc',
      },
    });

    // Get user's own mark
    const user = await prisma.user.findUnique({
      where: { email: session.user.email },
    });

    const userMark = marks.find((m) => m.userId === user?.id);

    // Calculate percentiles for each mark
    const marksArray = marks.map((m) => m.totalMarks);
    const percentiles = marksArray.map((mark) => {
      const count = marksArray.filter((m) => m < mark).length;
      return Math.round((count / marksArray.length) * 100);
    });

    // Generate distribution data
    const distribution: Record<string, number> = {
      '0-10': 0,
      '10-20': 0,
      '20-30': 0,
      '30-40': 0,
      '40-50': 0,
      '50-60': 0,
      '60-70': 0,
      '70-80': 0,
      '80-90': 0,
      '90-100': 0,
    };

    marksArray.forEach((mark) => {
      const bucket = Math.floor(mark / 10);
      const key = `${bucket * 10}-${bucket * 10 + 10}`;
      if (key in distribution) {
        distribution[key]++;
      }
    });

    // Grade distribution
    const gradeDistribution = {
      A: 0,
      'A-': 0,
      B: 0,
      'B-': 0,
      C: 0,
      D: 0,
      E: 0,
    };

    marks.forEach((mark) => {
      const grade = mark.predictedGrade as any;
      if (grade in gradeDistribution) {
        gradeDistribution[grade]++;
      }
    });

    // Calculate percentile curve data
    const percentileCurveData = marks
      .map((mark, index) => ({
        marks: mark.totalMarks,
        percentile: percentiles[index],
      }))
      .sort((a, b) => a.marks - b.marks);

    const canPredict = canMakePrediction(marks.length);

    return NextResponse.json({
      subject: {
        id: subject.id,
        code: subject.code,
        name: subject.name,
        semester: subject.semester,
      },
      statistics: {
        totalResponses: marks.length,
        averageMarks: subject.statistics?.averageMarks || 0,
        medianMarks: subject.statistics?.medianMarks || 0,
        modeMarks: subject.statistics?.modeMarks || 0,
        highestMarks: subject.statistics?.highestMarks || 0,
        lowestMarks: subject.statistics?.lowestMarks || 0,
        standardDeviation: subject.statistics?.standardDeviation || 0,
        passPercentage: subject.statistics?.passPercentage || 0,
        failurePercentage: subject.statistics?.failurePercentage || 0,
        gradeDistribution,
        distribution,
        percentileCurveData,
      },
      userMark: userMark ? {
        totalMarks: userMark.totalMarks,
        predictedGrade: userMark.predictedGrade,
        percentile: percentiles[marks.findIndex((m) => m.id === userMark.id)],
        estMarks: userMark.estMarks,
        mstMarks: userMark.mstMarks,
        quizMarks: userMark.quizMarks,
        assignmentMarks: userMark.assignmentMarks,
        labMarks: userMark.labMarks,
        practicalMarks: userMark.practicalMarks,
      } : null,
      predictionActive: canPredict,
      minSubmissionsRequired: parseInt(
        process.env.MINIMUM_SUBMISSIONS_FOR_PREDICTION || '100'
      ),
    });
  } catch (error) {
    console.error('[Statistics API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch statistics' },
      { status: 500 }
    );
  }
}
