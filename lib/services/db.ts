/**
 * Database Service
 * Handles all database operations using Prisma
 */

import { PrismaClient } from '@prisma/client';

let prisma: PrismaClient;

if (process.env.NODE_ENV === 'production') {
  prisma = new PrismaClient();
} else {
  if (!(global as any).prisma) {
    (global as any).prisma = new PrismaClient();
  }
  prisma = (global as any).prisma;
}

export default prisma;

/**
 * Create or update statistics for a subject
 */
export async function updateSubjectStatistics(subjectId: string) {
  const marks = await prisma.mark.findMany({
    where: {
      subjectId,
      isSubmitted: true,
    },
  });

  if (marks.length === 0) {
    return null;
  }

  const totalMarks = marks.map((m) => m.totalMarks);
  const count = totalMarks.length;
  const sum = totalMarks.reduce((a, b) => a + b, 0);
  const average = sum / count;

  // Median
  const sorted = [...totalMarks].sort((a, b) => a - b);
  const median =
    count % 2 === 0
      ? (sorted[count / 2 - 1] + sorted[count / 2]) / 2
      : sorted[Math.floor(count / 2)];

  // Standard deviation
  const variance = totalMarks.reduce(
    (sum, mark) => sum + Math.pow(mark - average, 2),
    0
  ) / count;
  const stdDev = Math.sqrt(variance);

  // Mode
  const frequency: Record<number, number> = {};
  let mode = sorted[0];
  let maxFreq = 1;

  for (const mark of sorted) {
    frequency[mark] = (frequency[mark] || 0) + 1;
    if (frequency[mark] > maxFreq) {
      maxFreq = frequency[mark];
      mode = mark;
    }
  }

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

  let passCount = 0;
  const failureThreshold = 35;

  for (const mark of totalMarks) {
    if (mark < failureThreshold) {
      gradeDistribution['E']++;
    } else if (mark >= failureThreshold) {
      passCount++;
    }
  }

  const passPercentage = (passCount / count) * 100;
  const failurePercentage = 100 - passPercentage;

  // Percentile distribution
  const percentileBuckets: Record<string, number> = {
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

  for (const mark of totalMarks) {
    const bucket = Math.floor(mark / 10) * 10;
    const key = `${bucket}-${bucket + 10}`;
    if (key in percentileBuckets) {
      percentileBuckets[key]++;
    }
  }

  const stats = await prisma.statistics.upsert({
    where: { subjectId },
    create: {
      subjectId,
      totalResponses: count,
      averageMarks: Math.round(average * 100) / 100,
      medianMarks: Math.round(median * 100) / 100,
      modeMarks: mode,
      highestMarks: sorted[count - 1],
      lowestMarks: sorted[0],
      standardDeviation: Math.round(stdDev * 100) / 100,
      gradeA: gradeDistribution['A'],
      gradeAMinus: gradeDistribution['A-'],
      gradeB: gradeDistribution['B'],
      gradeBMinus: gradeDistribution['B-'],
      gradeC: gradeDistribution['C'],
      gradeD: gradeDistribution['D'],
      gradeE: gradeDistribution['E'],
      passPercentage: Math.round(passPercentage * 100) / 100,
      failurePercentage: Math.round(failurePercentage * 100) / 100,
      percentileData: JSON.stringify(percentileBuckets),
    },
    update: {
      totalResponses: count,
      averageMarks: Math.round(average * 100) / 100,
      medianMarks: Math.round(median * 100) / 100,
      modeMarks: mode,
      highestMarks: sorted[count - 1],
      lowestMarks: sorted[0],
      standardDeviation: Math.round(stdDev * 100) / 100,
      gradeA: gradeDistribution['A'],
      gradeAMinus: gradeDistribution['A-'],
      gradeB: gradeDistribution['B'],
      gradeBMinus: gradeDistribution['B-'],
      gradeC: gradeDistribution['C'],
      gradeD: gradeDistribution['D'],
      gradeE: gradeDistribution['E'],
      passPercentage: Math.round(passPercentage * 100) / 100,
      failurePercentage: Math.round(failurePercentage * 100) / 100,
      percentileData: JSON.stringify(percentileBuckets),
      lastUpdated: new Date(),
    },
  });

  return stats;
}

/**
 * Create audit log entry
 */
export async function createAuditLog(
  userId: string,
  action: string,
  resourceType: string,
  resourceId?: string,
  changes?: Record<string, any>,
  ipAddress?: string,
  userAgent?: string
) {
  return prisma.auditLog.create({
    data: {
      userId,
      action,
      resourceType,
      resourceId,
      changes: changes ? JSON.stringify(changes) : null,
      ipAddress,
      userAgent,
    },
  });
}

/**
 * Create notification
 */
export async function createNotification(
  userId: string,
  type: string,
  title: string,
  message: string,
  relatedMarkId?: string
) {
  return prisma.notification.create({
    data: {
      userId,
      type,
      title,
      message,
      relatedMarkId,
    },
  });
}

/**
 * Get user by email
 */
export async function getUserByEmail(email: string) {
  return prisma.user.findUnique({
    where: { email },
  });
}

/**
 * Create or update user
 */
export async function upsertUser(
  email: string,
  name: string,
  profileImage?: string
) {
  return prisma.user.upsert({
    where: { email },
    update: {
      name,
      profileImage,
      lastLogin: new Date(),
      verifiedAt: new Date(), // Mark as verified on login
    },
    create: {
      email,
      name,
      profileImage,
      role: 'student',
      verifiedAt: new Date(),
      lastLogin: new Date(),
    },
  });
}
