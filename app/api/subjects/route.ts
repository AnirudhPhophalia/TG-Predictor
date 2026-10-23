/**
 * Subjects API
 * GET: Fetch active subjects
 */

import { NextResponse } from 'next/server';
import prisma from '@/lib/services/db';

export async function GET() {
  try {
    const subjects = await prisma.subject.findMany({
      where: {
        isActive: true,
      },
      include: {
        statistics: true,
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
    console.error('[Subjects API] Error:', error);
    return NextResponse.json(
      { error: 'Failed to fetch subjects' },
      { status: 500 }
    );
  }
}
