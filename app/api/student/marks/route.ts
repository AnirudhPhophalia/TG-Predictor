/**
 * Student Marks API
 * POST: Submit or update marks for a subject (when unlocked)
 * GET: Get student's marks submissions (only for the authenticated student)
 */

import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/services/db';
import { createAuditLog, createNotification, updateSubjectStatistics } from '@/lib/services/db';
import { calculatePercentile, predictGradeByPercentile, canMakePrediction, predictGradeByHistoricalCutoffs } from '@/lib/services/grade-predictor';

export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);

    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    const body = await request.json();
    const { subjectId, totalMarks, estMarks, mstMarks, quizMarks, assignmentMarks, labMarks, practicalMarks } = body;

    // Validation
    if (!subjectId || totalMarks === undefined) {
      return NextResponse.json(
        { error: 'Missing required fields: subjectId, totalMarks' },
        { status: 400 }
      );
    }

    if (totalMarks < 0 || totalMarks > 100) {
      return NextResponse.json(
        { error: 'Total marks must be between 0 and 100' },
        { status: 400 }
      );
    }

    // Get current user
    const user = await prisma.user.findUnique({
      where: { email: session.user.email },
    });

    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    // Check if student already submitted marks for this subject
    const existingMark = await prisma.mark.findUnique({
      where: {
        userId_subjectId: {
          userId: user.id,
          subjectId,
        },
      },
    });

    // If existing and locked, block submission
    if (existingMark && existingMark.isLocked) {
      return NextResponse.json(
        { error: 'Your marks submission for this subject is locked. Please submit an unlock request to make modifications.' },
        { status: 400 }
      );
    }

    // Get subject
    const subject = await prisma.subject.findUnique({
      where: { id: subjectId },
    });

    if (!subject || !subject.isActive) {
      return NextResponse.json({ error: 'Subject not found or inactive' }, { status: 404 });
    }

    // Get all marks for this subject to calculate percentile
    const allSubjectMarks = await prisma.mark.findMany({
      where: {
        subjectId,
        isSubmitted: true,
      },
      select: { totalMarks: true },
    });

    const marksArray = allSubjectMarks.map((m) => m.totalMarks);
    
    // Determine grade
    let predictedGrade = 'E';
    let percentile = 0;

    const submissionCount = existingMark ? allSubjectMarks.length : allSubjectMarks.length + 1;
    const historicalCutoff = await prisma.historicalCutoff.findUnique({
      where: { subjectId },
    });

    if (canMakePrediction(submissionCount) && marksArray.length > 0) {
      percentile = calculatePercentile(totalMarks, marksArray);
      predictedGrade = predictGradeByPercentile(totalMarks, percentile);
    } else if (historicalCutoff) {
      predictedGrade = predictGradeByHistoricalCutoffs(totalMarks, {
        cutoffA: historicalCutoff.cutoffA,
        cutoffAMinus: historicalCutoff.cutoffAMinus,
        cutoffB: historicalCutoff.cutoffB,
        cutoffBMinus: historicalCutoff.cutoffBMinus,
        cutoffC: historicalCutoff.cutoffC,
        cutoffD: historicalCutoff.cutoffD,
      });
    } else {
      predictedGrade = predictGradeByHistoricalCutoffs(totalMarks, {
        cutoffA: 85,
        cutoffAMinus: 75,
        cutoffB: 65,
        cutoffBMinus: 55,
        cutoffC: 45,
        cutoffD: 35,
      });
    }

    let mark;
    if (existingMark) {
      // Record edit history
      await prisma.editHistory.create({
        data: {
          markId: existingMark.id,
          originalMarks: existingMark.totalMarks,
          updatedMarks: totalMarks,
          reason: 'Student updated unlocked submission',
          approvedAt: new Date(),
        },
      });

      // Update and re-lock
      mark = await prisma.mark.update({
        where: { id: existingMark.id },
        data: {
          totalMarks,
          estMarks: estMarks || 0,
          mstMarks: mstMarks || 0,
          quizMarks: quizMarks || 0,
          assignmentMarks: assignmentMarks || 0,
          labMarks: labMarks || 0,
          practicalMarks: practicalMarks || 0,
          predictedGrade,
          percentile: canMakePrediction(submissionCount) ? percentile : null,
          isLocked: true,
          submittedAt: new Date(),
        },
      });
    } else {
      // Create new mark record
      mark = await prisma.mark.create({
        data: {
          userId: user.id,
          subjectId,
          totalMarks,
          estMarks: estMarks || 0,
          mstMarks: mstMarks || 0,
          quizMarks: quizMarks || 0,
          assignmentMarks: assignmentMarks || 0,
          labMarks: labMarks || 0,
          practicalMarks: practicalMarks || 0,
          predictedGrade,
          percentile: canMakePrediction(submissionCount) ? percentile : null,
          isSubmitted: true,
          isLocked: true,
        },
      });
    }

    // Update statistics
    await updateSubjectStatistics(subjectId);

    // Create audit log
    await createAuditLog(
      user.id,
      existingMark ? 'mark_updated' : 'mark_submitted',
      'mark',
      mark.id,
      {
        totalMarks,
        predictedGrade,
        percentile,
      },
      request.headers.get('x-forwarded-for') || request.headers.get('x-real-ip') || '',
      request.headers.get('user-agent') || ''
    );

    // Create notification
    await createNotification(
      user.id,
      'submission_success',
      existingMark ? 'Marks Updated Successfully' : 'Marks Submitted Successfully',
      `Your marks for ${subject.code} have been securely recorded. Predicted Grade: ${predictedGrade}`,
      mark.id
    );

    return NextResponse.json({
      success: true,
      mark: {
        id: mark.id,
        subjectId: mark.subjectId,
        totalMarks: mark.totalMarks,
        predictedGrade: mark.predictedGrade,
        percentile: mark.percentile,
        message: canMakePrediction(submissionCount)
          ? `Grade predicted based on ${submissionCount} submissions`
          : `Using historical grade boundaries (${submissionCount} submissions so far)`,
      },
    });
  } catch (error) {
    console.error('[Marks API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to submit marks' },
      { status: 500 }
    );
  }
}

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);

    if (!session?.user?.email) {
      return NextResponse.json({ error: 'Unauthorized' }, { status: 401 });
    }

    // PRIVACY ENFORCEMENT:
    // Only the authenticated student can retrieve their own marks.
    const user = await prisma.user.findUnique({
      where: { email: session.user.email },
      include: {
        marks: {
          include: {
            subject: {
              select: {
                id: true,
                code: true,
                name: true,
                semester: true,
                branch: true,
                credits: true,
              },
            },
          },
          orderBy: {
            submittedAt: 'desc',
          },
        },
      },
    });

    if (!user) {
      return NextResponse.json({ error: 'User not found' }, { status: 404 });
    }

    return NextResponse.json({
      marks: user.marks,
    });
  } catch (error) {
    console.error('[Marks GET API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch marks' },
      { status: 500 }
    );
  }
}
