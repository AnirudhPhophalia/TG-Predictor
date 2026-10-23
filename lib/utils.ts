import { clsx, type ClassValue } from 'clsx'
import { twMerge } from 'tailwind-merge'

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

export function getStudentSemester(email: string | null | undefined): number {
  if (!email) return 1;

  const emailLower = email.toLowerCase();
  
  // Extract year from email like aphophalia_be24@thapar.edu
  const yearMatch = emailLower.match(/be(\d{2})@/);
  if (!yearMatch) return 1; // Default to 1 if no year found

  const admissionYear = 2000 + parseInt(yearMatch[1], 10);
  
  const currentDate = new Date();
  const currentYear = currentDate.getFullYear();
  const currentMonth = currentDate.getMonth(); // 0-11
  
  // Calculate years elapsed since admission
  // Admission typically happens in August (month 7)
  let yearsElapsed = currentYear - admissionYear;
  
  // If we are before August in the current year, one less academic year has passed
  if (currentMonth < 7) {
    yearsElapsed -= 1;
  }
  
  let currentSemester = yearsElapsed * 2;
  
  // If we are in Fall (August - December), it's odd semester
  if (currentMonth >= 7) {
    currentSemester += 1;
  } else {
    // Spring (Jan - June), it's even semester
    currentSemester += 2;
  }
  
  // Cap at 8 semesters typically
  return Math.max(1, Math.min(8, currentSemester));
}
