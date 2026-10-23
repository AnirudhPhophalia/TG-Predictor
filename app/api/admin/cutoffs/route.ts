import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';
import { createAuditLog, updateSubjectStatistics } from '@/lib/services/db';
import { canMakePrediction, calculatePercentile, predictGradeByPercentile, predictGradeByHistoricalCutoffs } from '@/lib/services/grade-predictor';

async function isAdmin(email: string): Promise<boolean> {
  const user = await prisma.user.findUnique({
    where: { email },
  });
  return user?.role === 'admin';
}

export async function PATCH(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);

    if (!session?.user?.email || !(await isAdmin(session.user.email))) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { subjectId, cutoffA, cutoffAMinus, cutoffB, cutoffBMinus, cutoffC, cutoffD } = body;

    if (!subjectId) {
      return NextResponse.json(
        { error: 'Subject ID is required' },
        { status: 400 }
      );
    }

    const cA = Number(cutoffA);
    const cAMinus = Number(cutoffAMinus);
    const cB = Number(cutoffB);
    const cBMinus = Number(cutoffBMinus);
    const cC = Number(cutoffC);
    const cD = Number(cutoffD);

    if (isNaN(cA) || isNaN(cAMinus) || isNaN(cB) || isNaN(cBMinus) || isNaN(cC) || isNaN(cD)) {
      return NextResponse.json({ error: 'Invalid cutoff values' }, { status: 400 });
    }

    // Validate that cutoffs are strictly decreasing
    if (
      cA <= cAMinus ||
      cAMinus <= cB ||
      cB <= cBMinus ||
      cBMinus <= cC ||
      cC <= cD ||
      cD < 35
    ) {
      return NextResponse.json(
        { error: 'Invalid cutoffs: Must be strictly decreasing and D must be >= 35.' },
        { status: 400 }
      );
    }

    const updatedCutoff = await prisma.historicalCutoff.upsert({
      where: { subjectId },
      update: {
        cutoffA: cA,
        cutoffAMinus: cAMinus,
        cutoffB: cB,
        cutoffBMinus: cBMinus,
        cutoffC: cC,
        cutoffD: cD,
      },
      create: {
        subjectId,
        cutoffA: cA,
        cutoffAMinus: cAMinus,
        cutoffB: cB,
        cutoffBMinus: cBMinus,
        cutoffC: cC,
        cutoffD: cD,
      },
    });

    const adminUser = await prisma.user.findUnique({
      where: { email: session.user.email },
    });

    if (adminUser) {
      await createAuditLog(
        adminUser.id,
        'cutoff_updated',
        'historicalCutoff',
        updatedCutoff.id,
        { cutoffA, cutoffAMinus, cutoffB, cutoffBMinus, cutoffC, cutoffD },
        request.headers.get('x-forwarded-for') || '',
        request.headers.get('user-agent') || ''
      );
    }

    // Recalculate grades for all existing marks
    const allMarks = await prisma.mark.findMany({
      where: { subjectId: updatedCutoff.subjectId, isSubmitted: true }
    });
    
    if (allMarks.length > 0) {
      const submissionCount = allMarks.length;
      const marksArray = allMarks.map(m => m.totalMarks);
      
      const updates = allMarks.map(mark => {
        let predictedGrade = mark.predictedGrade;
        if (canMakePrediction(submissionCount)) {
          const percentile = calculatePercentile(mark.totalMarks, marksArray);
          predictedGrade = predictGradeByPercentile(mark.totalMarks, percentile);
        } else {
          predictedGrade = predictGradeByHistoricalCutoffs(mark.totalMarks, {
            cutoffA: cA, cutoffAMinus: cAMinus, cutoffB: cB, cutoffBMinus: cBMinus, cutoffC: cC, cutoffD: cD
          });
        }
        
        return prisma.mark.update({
          where: { id: mark.id },
          data: { predictedGrade }
        });
      });
      
      await prisma.$transaction(updates);
      
      // Update statistics after recalculating grades
      await updateSubjectStatistics(updatedCutoff.subjectId);
    }

    return NextResponse.json({ success: true, cutoff: updatedCutoff });
  } catch (error) {
    console.error('[Admin Cutoffs PATCH API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to update cutoffs' },
      { status: 500 }
    );
  }
}
