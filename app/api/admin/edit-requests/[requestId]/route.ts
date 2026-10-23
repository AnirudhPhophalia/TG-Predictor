import { NextRequest, NextResponse } from 'next/server';
import { getServerSession } from 'next-auth/next';
import { authOptions } from '@/lib/auth';
import prisma from '@/lib/prisma';
import { sendEmail, emailTemplates } from '@/lib/services/email';

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ requestId: string }> }
) {
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
    const { action, rejectionReason } = body;

    if (!['approve', 'reject'].includes(action)) {
      return NextResponse.json({ error: 'Invalid action' }, { status: 400 });
    }

    const { requestId } = await params;

    // Get edit request
    const editRequest = await prisma.editRequest.findUnique({
      where: { id: requestId },
      include: { user: true },
    });

    if (!editRequest) {
      return NextResponse.json({ error: 'Edit request not found' }, { status: 404 });
    }

    if (editRequest.status !== 'pending') {
      return NextResponse.json(
        { error: `Request is already ${editRequest.status}` },
        { status: 400 }
      );
    }

    if (action === 'approve') {
      // PRIVACY ENFORCEMENT:
      // Approval unlocks the submission for the student. Admin does NOT see or set the marks.
      // The mark record is set to isLocked = false so the student can update it themselves.
      const updatedRequest = await prisma.editRequest.update({
        where: { id: requestId },
        data: {
          status: 'approved',
          approvedBy: adminUser.id,
          approvedAt: new Date(),
        },
      });

      const mark = await prisma.mark.findUnique({
        where: { id: editRequest.markId },
        include: { subject: true },
      });

      if (mark) {
        // Unlock mark so student can edit
        await prisma.mark.update({
          where: { id: mark.id },
          data: {
            isLocked: false,
          },
        });

        // Audit log (without logging raw mark values to admin)
        await prisma.auditLog.create({
          data: {
            userId: adminUser.id,
            action: 'unlock_approved',
            resourceType: 'mark',
            resourceId: mark.id,
            changes: JSON.stringify({
              approvalReason: editRequest.reason,
            }),
          },
        });

        // Send approval email
        try {
          const emailTemplate = emailTemplates.editRequestApproved(
            editRequest.user.name || 'Student',
            mark.subject.name
          );
          await sendEmail({
            to: editRequest.user.email,
            subject: emailTemplate.subject,
            html: emailTemplate.html,
          });
        } catch (mailErr) {
          console.warn('Mail notification failed:', mailErr);
        }
      }

      return NextResponse.json(updatedRequest);
    } else {
      // Reject request
      const updatedRequest = await prisma.editRequest.update({
        where: { id: requestId },
        data: {
          status: 'rejected',
          approvedBy: adminUser.id,
          approvedAt: new Date(),
          rejectionReason: rejectionReason || 'Request does not meet approval criteria',
        },
      });

      const mark = await prisma.mark.findUnique({
        where: { id: editRequest.markId },
        include: { subject: true },
      });

      if (mark) {
        await prisma.auditLog.create({
          data: {
            userId: adminUser.id,
            action: 'unlock_rejected',
            resourceType: 'mark',
            resourceId: mark.id,
            changes: JSON.stringify({
              reason: rejectionReason,
            }),
          },
        });

        try {
          const emailTemplate = emailTemplates.editRequestRejected(
            editRequest.user.name || 'Student',
            mark.subject.name,
            rejectionReason || 'Request does not meet approval criteria'
          );
          await sendEmail({
            to: editRequest.user.email,
            subject: emailTemplate.subject,
            html: emailTemplate.html,
          });
        } catch (mailErr) {
          console.warn('Mail notification failed:', mailErr);
        }
      }

      return NextResponse.json(updatedRequest);
    }
  } catch (error) {
    console.error('Error processing edit request:', error);
    return NextResponse.json({ error: 'Internal server error' }, { status: 500 });
  }
}
