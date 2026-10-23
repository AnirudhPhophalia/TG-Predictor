/**
 * Grade Prediction Engine
 * Calculates predicted grades based on percentile ranking
 */

export type Grade = 'A' | 'A-' | 'B' | 'B-' | 'C' | 'D' | 'E';

interface GradeRuleSet {
  cutoffA: number;
  cutoffAMinus: number;
  cutoffB: number;
  cutoffBMinus: number;
  cutoffC: number;
  cutoffD: number;
}

// Default grade boundaries (percentile-based)
const PERCENTILE_GRADES: Record<Grade, number> = {
  'A': 90,      // Top 10%
  'A-': 80,     // 10-20%
  'B': 55,      // 20-45%
  'B-': 30,     // 45-70%
  'C': 20,      // 70-80%
  'D': 0,       // 80-100%
  'E': -1,      // Below 35 marks
};

/**
 * Calculate percentile rank for a student
 * @param studentMarks - Student's total marks
 * @param allMarks - Array of all student marks for the subject
 * @returns Percentile (0-100)
 */
export function calculatePercentile(studentMarks: number, allMarks: number[]): number {
  if (allMarks.length === 0) return 0;

  const sortedMarks = [...allMarks].sort((a, b) => a - b);
  const count = sortedMarks.filter((m) => m < studentMarks).length;
  const percentile = (count / sortedMarks.length) * 100;

  return Math.round(percentile);
}

/**
 * Predict grade using percentile-based system
 * @param studentMarks - Student's total marks (0-100)
 * @param percentile - Percentile rank (0-100)
 * @returns Predicted grade
 */
export function predictGradeByPercentile(
  studentMarks: number,
  percentile: number
): Grade {
  // Special rule: If marks < 35, grade is E regardless of percentile
  if (studentMarks < 35) {
    return 'E';
  }

  // Percentile-based grading
  if (percentile >= 90) return 'A';      // Top 10%
  if (percentile >= 80) return 'A-';     // 10-20%
  if (percentile >= 55) return 'B';      // 20-45%
  if (percentile >= 30) return 'B-';     // 45-70%
  if (percentile >= 20) return 'C';      // 70-80%
  if (percentile >= 0) return 'D';       // 80-100%

  return 'E';
}

/**
 * Predict grade using historical cutoffs (fallback method)
 * @param totalMarks - Student's total marks
 * @param cutoffs - Grade cutoff boundaries
 * @returns Predicted grade
 */
export function predictGradeByHistoricalCutoffs(
  totalMarks: number,
  cutoffs: GradeRuleSet
): Grade {
  if (totalMarks >= cutoffs.cutoffA) return 'A';
  if (totalMarks >= cutoffs.cutoffAMinus) return 'A-';
  if (totalMarks >= cutoffs.cutoffB) return 'B';
  if (totalMarks >= cutoffs.cutoffBMinus) return 'B-';
  if (totalMarks >= cutoffs.cutoffC) return 'C';
  if (totalMarks >= cutoffs.cutoffD) return 'D';

  return 'E';
}

/**
 * Calculate statistics from an array of marks
 */
export function calculateStatistics(marks: number[]) {
  if (marks.length === 0) {
    return {
      count: 0,
      average: 0,
      median: 0,
      mode: 0,
      min: 0,
      max: 0,
      stdDev: 0,
      percentile25: 0,
      percentile75: 0,
    };
  }

  const sorted = [...marks].sort((a, b) => a - b);
  const count = sorted.length;
  const sum = sorted.reduce((a, b) => a + b, 0);
  const average = sum / count;

  // Median
  const median =
    count % 2 === 0
      ? (sorted[count / 2 - 1] + sorted[count / 2]) / 2
      : sorted[Math.floor(count / 2)];

  // Mode (most frequent value)
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

  // Standard deviation
  const variance =
    sorted.reduce((sum, mark) => sum + Math.pow(mark - average, 2), 0) / count;
  const stdDev = Math.sqrt(variance);

  // Percentiles
  const percentile25 = sorted[Math.floor(count * 0.25)];
  const percentile75 = sorted[Math.floor(count * 0.75)];

  return {
    count,
    average: Math.round(average * 100) / 100,
    median: Math.round(median * 100) / 100,
    mode,
    min: sorted[0],
    max: sorted[count - 1],
    stdDev: Math.round(stdDev * 100) / 100,
    percentile25,
    percentile75,
  };
}

/**
 * Generate percentile distribution buckets
 */
export function generatePercentileDistribution(marks: number[]) {
  const buckets = {
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

  for (const mark of marks) {
    const bucket = Math.floor(mark / 10) * 10;
    const key = `${bucket}-${bucket + 10}`;
    if (key in buckets) {
      buckets[key as keyof typeof buckets]++;
    }
  }

  return buckets;
}

/**
 * Calculate grade distribution
 */
export function calculateGradeDistribution(
  marks: number[],
  usePercentile: boolean,
  percentileData?: { marks: number; percentile: number }[],
  cutoffs?: GradeRuleSet
) {
  const distribution = {
    A: 0,
    'A-': 0,
    B: 0,
    'B-': 0,
    C: 0,
    D: 0,
    E: 0,
  };

  if (usePercentile && percentileData) {
    // Use percentile-based grading
    for (const data of percentileData) {
      const grade = predictGradeByPercentile(data.marks, data.percentile);
      distribution[grade]++;
    }
  } else if (cutoffs) {
    // Use historical cutoff-based grading
    for (const mark of marks) {
      const grade = predictGradeByHistoricalCutoffs(mark, cutoffs);
      distribution[grade]++;
    }
  }

  return distribution;
}

/**
 * Check if prediction can be made (has minimum submissions)
 */
export function canMakePrediction(submissionCount: number): boolean {
  const minSubmissions = parseInt(
    process.env.MINIMUM_SUBMISSIONS_FOR_PREDICTION || '100'
  );
  return submissionCount >= minSubmissions;
}

/**
 * Get prediction confidence score (0-100)
 * Based on submission count and data variance
 */
export function getPredictionConfidence(
  submissionCount: number,
  stdDev: number
): number {
  const minSubmissions = parseInt(
    process.env.MINIMUM_SUBMISSIONS_FOR_PREDICTION || '100'
  );

  // Base confidence from submission count
  const submissionConfidence = Math.min(
    (submissionCount / (minSubmissions * 2)) * 100,
    100
  );

  // Variance confidence (lower variance = higher confidence)
  const varianceConfidence = Math.max(100 - stdDev * 2, 0);

  // Weighted average
  return Math.round(submissionConfidence * 0.6 + varianceConfidence * 0.4);
}
