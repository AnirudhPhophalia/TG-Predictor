import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

const subjects = [
  // Semester I
  { code: 'UCB009', name: 'Chemistry', semester: 1, branch: 'COE', credits: 4 },
  { code: 'UES103', name: 'Programming for Problem Solving', semester: 1, branch: 'COE', credits: 4 },
  { code: 'UES013', name: 'Electrical & Electronics Engineering', semester: 1, branch: 'COE', credits: 4.5 },
  { code: 'UEN008', name: 'Energy and Environment', semester: 1, branch: 'COE', credits: 2 },
  { code: 'UMA022', name: 'Calculus for Engineers', semester: 1, branch: 'COE', credits: 3.5 },

  // Semester II
  { code: 'UPH013', name: 'Physics', semester: 2, branch: 'COE', credits: 4.5 },
  { code: 'UES101', name: 'Engineering Drawing', semester: 2, branch: 'COE', credits: 4 },
  { code: 'UHU003', name: 'Professional Communication', semester: 2, branch: 'COE', credits: 3 },
  { code: 'UES102', name: 'Manufacturing Processes', semester: 2, branch: 'COE', credits: 3 },
  { code: 'UMA023', name: 'Differential Equations and Linear Algebra', semester: 2, branch: 'COE', credits: 3.5 },

  // Semester III
  { code: 'UCS303', name: 'Operating System', semester: 3, branch: 'COE', credits: 4 },
  { code: 'UTA018', name: 'Object Oriented Programming', semester: 3, branch: 'COE', credits: 4 },
  { code: 'UCS301', name: 'Data Structures', semester: 3, branch: 'COE', credits: 4 },
  { code: 'UCS405', name: 'Discrete Mathematical Structures', semester: 3, branch: 'COE', credits: 3.5 },
  { code: 'UTA016', name: 'Engineering Design Project I', semester: 3, branch: 'COE', credits: 3 },
  { code: 'UMA021', name: 'Numerical Linear Algebra', semester: 3, branch: 'COE', credits: 4 },
  { code: 'UHU052', name: 'The Evolutionary Basis of Human Behaviour for Engineers', semester: 3, branch: 'COE', credits: 1 },
  { code: 'UCS320', name: 'Introduction to Sustainable Green Computing', semester: 3, branch: 'COE', credits: 1 },

  // Semester IV
  { code: 'UCS415', name: 'Design and Analysis of Algorithms', semester: 4, branch: 'COE', credits: 4 },
  { code: 'UCS310', name: 'Database Management Systems', semester: 4, branch: 'COE', credits: 4 },
  { code: 'UCS414', name: 'Computer Networks', semester: 4, branch: 'COE', credits: 4 },
  { code: 'UCS321', name: 'AI for Engineers', semester: 4, branch: 'COE', credits: 3 },
  { code: 'UMA401', name: 'Probability and Statistics', semester: 4, branch: 'COE', credits: 4 },
  { code: 'UTA024', name: 'Engineering Design Project II', semester: 4, branch: 'COE', credits: 3 },
  { code: 'UTD003', name: 'Aptitude Skills Building', semester: 4, branch: 'COE', credits: 2 },

  // Semester V
  { code: 'UML501', name: 'MACHINE LEARNING', semester: 5, branch: 'COE', credits: 4.0 },
  { code: 'UCS553', name: 'ENTERPRISE WEB APPLICATION', semester: 5, branch: 'COE', credits: 4.0 },
  { code: 'UCS615', name: 'IMAGE PROCESSING', semester: 5, branch: 'COE', credits: 4.0 },
  { code: 'UCS503', name: 'SOFTWARE ENGINEERING', semester: 5, branch: 'COE', credits: 4.0 },
  { code: 'UCS510', name: 'COMPUTER ARCHITECTURE AND ORGANIZATION', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCSXXX_AI', name: 'Ethics and Risk Mitigation in AI', semester: 5, branch: 'COE', credits: 3 }, // Note: UCSXXX is a placeholder, made unique

  // Semester VI
  { code: 'UCS701', name: 'THEORY OF COMPUTATION', semester: 6, branch: 'COE', credits: 3.5 },
  { code: 'UMA035', name: 'NUMERICAL OPTIMIZATION', semester: 6, branch: 'COE', credits: 4.0 },
  { code: 'UTA025', name: 'INNOVATION AND ENTREPRENEURSHIP', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCSXXX_DOMAIN', name: 'DOMAIN SPECIFIC APPLICATIONS FOR ENGINEERING GRADUATES', semester: 6, branch: 'COE', credits: 3.0 }, // Note: UCSXXX

  // Semester VII
  { code: 'UCS802', name: 'COMPILER CONSTRUCTION', semester: 7, branch: 'COE', credits: 4.0 },
  { code: 'UHU005', name: 'HUMANITIES FOR ENGINEERS', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS714', name: 'Agentic AI', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS797', name: 'CAPSTONE PROJECT', semester: 7, branch: 'COE', credits: 8.0 },

  // Semester VIII
  { code: 'UCS898', name: 'PROJECT SEMESTER', semester: 8, branch: 'COE', credits: 15.0 },
  { code: 'UCS813', name: 'SOCIAL NETWORK ANALYSIS', semester: 8, branch: 'COE', credits: 3.0 },
  { code: 'UCS806', name: 'ETHICAL HACKING', semester: 8, branch: 'COE', credits: 4.0 },
  { code: 'UCS899', name: 'PROJECT', semester: 8, branch: 'COE', credits: 8.0 },
  { code: 'UCS900', name: 'START- UP SEMESTER', semester: 8, branch: 'COE', credits: 15.0 },

  // Elective I
  { code: 'UCS531', name: 'CLOUD COMPUTING', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS532', name: 'COMPUTER VISION', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS534', name: 'COMPUTER & NETWORK SECURITY', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UMC512', name: 'MATHEMATIC MODELING AND SIMULATION', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS548', name: 'FOUNDATION OF DATA SCIENCE', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS539', name: 'FINANCE, ACCOUNTING AND VALUATION', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS537', name: 'SOURCE CODE MANAGEMENT', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS542', name: 'UI & UX SPECIALIST', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS551', name: 'CONVERSATIONAL AI: ACCELERATED DATA SCIENCE', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS547', name: 'EDGE AI AND ROBOTICS: ACCELERATED DATA SCIENCE', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UCS550', name: 'NETWORK DEFENCE', semester: 5, branch: 'COE', credits: 3.0 },
  { code: 'UEC646', name: 'NETWORK AND COMMUNICATION FOR CONNECTED VEHICLES', semester: 5, branch: 'COE', credits: 3.0 },

  // Elective II
  { code: 'UCS635', name: 'GPU COMPUTING', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS636', name: '3D MODELLING AND ANIMATION', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS638', name: 'SECURE CODING', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UMC622', name: 'MATRIX COMPUTATION', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS654', name: 'PREDICTIVE ANALYTICS USING STATISTICS', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS675', name: 'FINANCIAL MARKETS AND PORTFOLIO THEORY', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS659', name: 'BUILD AND RELEASE MANAGEMENT', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS661', name: 'DATABASE ENGINEER', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS664', name: 'CONVERSATIONAL AI: NATURAL LANGUAGE PROCESSING', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS668', name: 'EDGE AI AND ROBOTICS: DATA CENTRE VISION', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS673', name: 'ETHICAL HACKING-1', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS678', name: 'INTELLIGENT TRANSPORTATION SYSTEMS', semester: 6, branch: 'COE', credits: 3.0 },

  // Elective III
  { code: 'UCS645', name: 'PARALLEL & DISTRIBUTED COMPUTING', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS646', name: 'GAME DESIGN & DEVELOPMENT', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS648', name: 'CYBER FORENSICS', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UMC632', name: 'FINANCIAL MATHEMATICS', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS761', name: 'DEEP LEARNING', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS658', name: 'DERIVATIVES PRICING, TRADING AND STRATEGIES', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS660', name: 'CONTINUOUS INTEGRATION AND CONTINUOUS DEPLOYMENT', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS662', name: 'TEST AUTOMATION', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS749', name: 'CONVERSATIONAL AI: SPEECH PROCESSING & SYNTHESIS', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS671', name: 'EDGE AI AND ROBOTICS: EMBEDDED VISION', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS674', name: 'ETHICAL HACKING-II', semester: 6, branch: 'COE', credits: 3.0 },
  { code: 'UCS679', name: 'DATA ANALYTICS IN AUTOMOBILE ENGINEERING', semester: 6, branch: 'COE', credits: 3.0 },

  // Elective IV
  { code: 'UCS751', name: 'SIMULATION & MODELLING', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS752', name: 'AUGMENTED AND VIRTUAL REALITY', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS754', name: 'BLOCKCHAIN TECHNOLOGY AND APPLICATIONS', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UMC742', name: 'COMPUTATIONAL NUMBER THEORY', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS772', name: 'DATA SCIENCE: COMPUTER VISION & NLP', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UMC743', name: 'QUANTITATIVE AND STATISTICAL METHODS FOR FINANCE', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS758', name: 'SYSTEM PROVISIONING AND CONFIGURATION MANAGEMENT', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS745', name: 'CLOUD & DEVOPS', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS748', name: 'GENERATIVE AI', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS760', name: 'EDGE AI AND ROBOTICS: REINFORCEMENT LEARNING & CONVERSATIONAL AI', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCS750', name: 'COMPUTER HACKING AND FORENSIC INVESTIGATION', semester: 7, branch: 'COE', credits: 3.0 },
  { code: 'UCSXXX_CYBER', name: 'CYBER SECURITY FOR MOBILITY SYSTEMS', semester: 7, branch: 'COE', credits: 3.0 },

  // Generic Elective
  { code: 'UHU016', name: 'Introductory Course in French', semester: 6, branch: 'COE', credits: 2.0 }, // assumed sem 6 based on document order
  { code: 'UHU017', name: 'Introduction to Cognitive Science', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UHU018', name: 'Introduction to Corporate Finance', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UCS002', name: 'Introduction to Cyber Security', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UPH064', name: 'Nanoscience and Nanomaterials', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UEN006', name: 'Technologies for Sustainable Development', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UMA069', name: 'Graph Theory and Applications', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UBT510', name: 'Biology for Engineers', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UMA070', name: 'Advanced Numerical Methods', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UTD004', name: 'Campus 2 Corporate', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UHUXXX_GENDER', name: 'Gender, Identity and Technology', semester: 6, branch: 'COE', credits: 2.0 },
  { code: 'UHU051', name: 'Creative Writing', semester: 6, branch: 'COE', credits: 2.0 },
];

async function main() {
  console.log('Seeding subjects from BECOE_2025_Scheme.pdf...');
  for (const subject of subjects) {
    await prisma.subject.upsert({
      where: { code: subject.code },
      update: {
        name: subject.name,
        semester: subject.semester,
        branch: subject.branch,
        credits: subject.credits,
      },
      create: {
        code: subject.code,
        name: subject.name,
        semester: subject.semester,
        branch: subject.branch,
        credits: subject.credits,
      },
    });
  }
  console.log('Database seeded successfully.');
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
