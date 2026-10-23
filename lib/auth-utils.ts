/**
 * Authentication Utilities
 */

import { getServerSession } from 'next-auth';
import { authOptions } from './auth';
import prisma from './services/db';

/**
 * Get current user session
 */
export async function getCurrentSession() {
  const session = await getServerSession(authOptions);
  return session;
}

/**
 * Get current authenticated user
 */
export async function getCurrentUser() {
  const session = await getServerSession(authOptions);

  if (!session?.user?.email) {
    return null;
  }

  const user = await prisma.user.findUnique({
    where: { email: session.user.email },
  });

  return user;
}

/**
 * Check if user is admin
 */
export async function isUserAdmin(): Promise<boolean> {
  const user = await getCurrentUser();
  return user?.role === 'admin';
}

/**
 * Check if user is authenticated
 */
export async function isAuthenticated(): Promise<boolean> {
  const session = await getCurrentSession();
  return !!session?.user;
}

/**
 * Require authentication (throw if not authenticated)
 */
export async function requireAuth() {
  const user = await getCurrentUser();

  if (!user) {
    throw new Error('Unauthorized');
  }

  return user;
}

/**
 * Require admin role (throw if not admin)
 */
export async function requireAdmin() {
  const user = await requireAuth();

  if (user.role !== 'admin') {
    throw new Error('Forbidden: Admin access required');
  }

  return user;
}

/**
 * Verify email domain
 */
export function isValidThaparEmail(email: string): boolean {
  const domain = email.split('@')[1];
  return domain === process.env.THAPAR_EMAIL_DOMAIN;
}

/**
 * Extract student ID from email
 */
export function extractStudentId(email: string): string {
  const [localPart] = email.split('@');
  return localPart;
}
