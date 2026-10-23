/**
 * NextAuth Configuration
 * Handles Google OAuth with Thapar Institute email domain restriction
 */

import type { NextAuthOptions } from 'next-auth';
import GoogleProvider from 'next-auth/providers/google';
import prisma, { upsertUser } from './services/db';
import { sendEmail, emailTemplates } from './services/email';

const THAPAR_EMAIL_DOMAIN = process.env.THAPAR_EMAIL_DOMAIN || 'thapar.edu';

export const authOptions: NextAuthOptions = {
  providers: [
    GoogleProvider({
      clientId: process.env.GOOGLE_CLIENT_ID || '',
      clientSecret: process.env.GOOGLE_CLIENT_SECRET || '',
    }),
  ],
  callbacks: {
    async signIn({ user, account }) {
      // Verify email domain
      if (!user.email) {
        return false;
      }

      const email = user.email.toLowerCase();
      const emailDomain = email.split('@')[1];
      
      const isAdmin = email === 'anirudhphophalia@gmail.com' || email === 'aphophalia_be24@thapar.edu';

      if (!isAdmin) {
        if (emailDomain !== THAPAR_EMAIL_DOMAIN) {
          console.log(`[Auth] Access denied: ${email} is not a Thapar Institute email`);
          return '/login?error=AccessDenied';
        }

        if (!email.includes('be24')) {
          console.log(`[Auth] Access denied: ${email} is not a Batch 2028 email`);
          return '/login?error=BatchRestriction';
        }
      }

      // Create or update user in database
      try {
        await upsertUser(user.email, user.name || '', user.image || undefined);
      } catch (error) {
        console.error('[Auth] Error creating/updating user:', error);
        return false;
      }

      return true;
    },

    async session({ session, token }) {
      if (session.user) {
        session.user.id = token.sub as string || '';
        session.user.email = token.email as string || '';
        session.user.role = token.role as string || 'student';
      }
      return session;
    },

    async jwt({ token, user }) {
      if (user) {
        token.id = user.id;
      }
      // Fetch role from DB to keep it updated in token
      if (token.email) {
        const dbUser = await prisma.user.findUnique({
          where: { email: token.email as string },
        });
        if (dbUser) {
          token.role = dbUser.role;
        }
      }
      return token;
    },
  },

  pages: {
    signIn: '/login',
    error: '/auth/error',
  },

  session: {
    strategy: 'jwt',
    maxAge: 30 * 24 * 60 * 60, // 30 days
    updateAge: 24 * 60 * 60,   // 24 hours
  },

  jwt: {
    secret: process.env.NEXTAUTH_SECRET,
    maxAge: 30 * 24 * 60 * 60, // 30 days
  },

  events: {
    async signIn({ user, account, profile, isNewUser }) {
      console.log(`[Auth] User signed in: ${user.email}`);
      if (isNewUser && user.email) {
        await sendEmail({
          to: 'aphophalia_be24@thapar.edu',
          ...emailTemplates.newRegistration(user.email, user.name || undefined)
        });
      }
    },
    async signOut({ token }) {
      console.log(`[Auth] User signed out: ${token.email}`);
    },
  },

  debug: process.env.NODE_ENV === 'development',
};

// Type augmentation for NextAuth session
declare module 'next-auth' {
  interface Session {
    user: {
      id: string;
      email: string;
      name?: string;
      image?: string;
      role?: string;
    };
  }

  interface User {
    id: string;
    email: string;
    name?: string;
    image?: string;
    role?: string;
  }
}
