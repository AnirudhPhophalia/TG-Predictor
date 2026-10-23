/**
 * Prisma Seed Script
 * Run with: npx prisma db seed
 */

import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  console.log('Starting seed...');

  // Create semesters
  const sem1 = await prisma.semester.upsert({
    where: { name: 'Fall 2024' },
    update: {},
    create: {
      name: 'Fall 2024',
      startDate: new Date('2024-08-01'),
      endDate: new Date('2024-12-31'),
      isActive: true,
    },
  });

  const sem2 = await prisma.semester.upsert({
    where: { name: 'Spring 2025' },
    update: {},
    create: {
      name: 'Spring 2025',
      startDate: new Date('2025-01-15'),
      endDate: new Date('2025-05-31'),
      isActive: false,
    },
  });

  console.log('Created semesters:', sem1, sem2);

  // Create sample subjects
  const subjects = [
    {
      code: 'CS101',
      name: 'Data Structures',
      semester: 1,
      branch: 'CSE',
      credits: 4,
    },
    {
      code: 'CS102',
      name: 'Algorithms',
      semester: 1,
      branch: 'CSE',
      credits: 4,
    },
    {
      code: 'CS201',
      name: 'Database Systems',
      semester: 2,
      branch: 'CSE',
      credits: 3,
    },
    {
      code: 'CS202',
      name: 'Web Development',
      semester: 2,
      branch: 'CSE',
      credits: 3,
    },
    {
      code: 'ME101',
      name: 'Mechanics',
      semester: 1,
      branch: 'ME',
      credits: 4,
    },
    {
      code: 'ME102',
      name: 'Thermodynamics',
      semester: 1,
      branch: 'ME',
      credits: 4,
    },
  ];

  for (const subject of subjects) {
    const existingSubject = await prisma.subject.findUnique({
      where: { code: subject.code },
    });

    if (!existingSubject) {
      const createdSubject = await prisma.subject.create({
        data: {
          ...subject,
          isActive: true,
        },
      });

      // Create historical cutoffs
      await prisma.historicalCutoff.create({
        data: {
          subjectId: createdSubject.id,
          cutoffA: 85,
          cutoffAMinus: 75,
          cutoffB: 65,
          cutoffBMinus: 55,
          cutoffC: 45,
          cutoffD: 35,
        },
      });

      // Create statistics
      await prisma.statistics.create({
        data: {
          subjectId: createdSubject.id,
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

      console.log('Created subject:', createdSubject.code);
    }
  }

  // Create sample admin user
  const adminEmail = 'admin@thapar.edu';
  const existingAdmin = await prisma.user.findUnique({
    where: { email: adminEmail },
  });

  if (!existingAdmin) {
    await prisma.user.create({
      data: {
        email: adminEmail,
        name: 'Admin User',
        role: 'admin',
        verifiedAt: new Date(),
      },
    });
    console.log('Created admin user:', adminEmail);
  }

  // Create sample student users
  for (let i = 1; i <= 5; i++) {
    const studentEmail = `student${i}@thapar.edu`;
    const existingStudent = await prisma.user.findUnique({
      where: { email: studentEmail },
    });

    if (!existingStudent) {
      await prisma.user.create({
        data: {
          email: studentEmail,
          name: `Student ${i}`,
          studentId: `TU${String(i).padStart(5, '0')}`,
          rollNumber: String(2000 + i),
          branch: i % 2 === 0 ? 'CSE' : 'ME',
          semester: i,
          role: 'student',
          verifiedAt: new Date(),
        },
      });
      console.log('Created student user:', studentEmail);
    }
  }

  console.log('Seed completed successfully!');
}

main()
  .catch((e) => {
    console.error('Seed failed:', e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
