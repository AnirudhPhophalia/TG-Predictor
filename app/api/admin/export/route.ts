import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth/next';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/prisma';
import { parse as json2csv } from 'papaparse';
import jsPDF from 'jspdf';

export async function POST(request: NextRequest) {
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

    const body = await request.json();
    const { format } = body;

    if (!['csv', 'excel', 'pdf'].includes(format)) {
      return NextResponse.json({ error: 'Invalid export format' }, { status: 400 });
    }

    // PRIVACY POLICY ENFORCEMENT:
    // Individual student marks and personal identification are strictly confidential.
    // Export only generates aggregated Subject-level statistical reports.
    const statistics = await prisma.statistics.findMany({
      include: {
        subject: {
          select: {
            code: true,
            name: true,
            semester: true,
            branch: true,
            credits: true,
          },
        },
      },
      orderBy: { subject: { code: 'asc' } },
    });

    const exportData = statistics.map((stat) => ({
      'Subject Code': stat.subject.code,
      'Subject Name': stat.subject.name,
      'Semester': stat.subject.semester,
      'Branch': stat.subject.branch,
      'Credits': stat.subject.credits,
      'Total Submissions': stat.totalResponses,
      'Average Marks': stat.averageMarks,
      'Median Marks': stat.medianMarks,
      'Highest Marks': stat.highestMarks ?? '-',
      'Lowest Marks': stat.lowestMarks ?? '-',
      'Std Deviation': stat.standardDeviation,
      'Pass Rate (%)': `${stat.passPercentage}%`,
      'Fail Rate (%)': `${stat.failurePercentage}%`,
      'Grade A Count': stat.gradeA,
      'Grade A- Count': stat.gradeAMinus,
      'Grade B Count': stat.gradeB,
      'Grade B- Count': stat.gradeBMinus,
      'Grade C Count': stat.gradeC,
      'Grade D Count': stat.gradeD,
      'Grade E Count': stat.gradeE,
      'Last Updated': new Date(stat.lastUpdated).toLocaleString(),
    }));

    if (format === 'csv' || format === 'excel') {
      const csv = json2csv({
        data: exportData,
        header: true,
      });

      const filename = format === 'csv' ? 'subject_statistics.csv' : 'subject_statistics.xlsx';
      const contentType = format === 'csv' ? 'text/csv; charset=utf-8' : 'application/vnd.ms-excel; charset=utf-8';

      return new NextResponse(csv, {
        headers: {
          'Content-Type': contentType,
          'Content-Disposition': `attachment; filename="${filename}"`,
        },
      });
    } else if (format === 'pdf') {
      const doc = new jsPDF();
      const pageWidth = doc.internal.pageSize.getWidth();
      let yPosition = 20;

      // Title
      doc.setFontSize(16);
      doc.text('Thapar Grade Predictor - Subject Statistics Report', pageWidth / 2, yPosition, {
        align: 'center',
      });
      yPosition += 10;

      doc.setFontSize(9);
      doc.setTextColor(100);
      doc.text('Privacy Notice: This report contains aggregated statistics only. Individual student marks are protected.', 15, yPosition);
      yPosition += 6;
      doc.text(`Generated on: ${new Date().toLocaleString()}`, 15, yPosition);
      yPosition += 10;

      doc.setFontSize(10);
      doc.setTextColor(0);
      doc.setFont(undefined, 'bold');
      doc.text(`Total Subjects: ${exportData.length}`, 15, yPosition);
      yPosition += 8;

      // Table headers
      const headers = ['Code', 'Subject Name', 'Responses', 'Average', 'Median', 'Pass %'];
      const colWidths = [25, 60, 25, 25, 25, 25];
      let xPosition = 15;

      doc.setFontSize(9);
      headers.forEach((h, i) => {
        doc.text(h, xPosition, yPosition);
        xPosition += colWidths[i];
      });
      yPosition += 6;
      doc.setFont(undefined, 'normal');

      exportData.forEach((row) => {
        if (yPosition > 270) {
          doc.addPage();
          yPosition = 20;
        }

        xPosition = 15;
        const dataRow = [
          row['Subject Code'],
          row['Subject Name'].substring(0, 30),
          String(row['Total Submissions']),
          String(row['Average Marks']),
          String(row['Median Marks']),
          row['Pass Rate (%)'],
        ];

        dataRow.forEach((cell, i) => {
          doc.text(cell, xPosition, yPosition);
          xPosition += colWidths[i];
        });
        yPosition += 6;
      });

      const pdfBuffer = Buffer.from(doc.output('arraybuffer'));
      return new NextResponse(pdfBuffer, {
        headers: {
          'Content-Type': 'application/pdf',
          'Content-Disposition': 'attachment; filename="subject_statistics.pdf"',
        },
      });
    }

    return NextResponse.json({ error: 'Invalid format' }, { status: 400 });
  } catch (error) {
    console.error('Error exporting statistics:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
