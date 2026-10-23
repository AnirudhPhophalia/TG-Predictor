import nodemailer from 'nodemailer';

// Configure your email transporter
const transporter = nodemailer.createTransport({
  host: process.env.EMAIL_HOST,
  port: parseInt(process.env.EMAIL_PORT || '587'),
  secure: process.env.EMAIL_PORT === '465', // true for 465, false for other ports
  auth: {
    user: process.env.EMAIL_USER,
    pass: process.env.EMAIL_PASSWORD,
  },
});

interface EmailOptions {
  to: string;
  subject: string;
  html: string;
  text?: string;
}

export async function sendEmail(options: EmailOptions): Promise<boolean> {
  try {
    const mailOptions = {
      from: process.env.EMAIL_FROM || process.env.EMAIL_USER,
      to: options.to,
      subject: options.subject,
      html: options.html,
      text: options.text,
    };

    await transporter.sendMail(mailOptions);
    console.log(`Email sent to ${options.to}`);
    return true;
  } catch (error) {
    console.error('Email sending failed:', error);
    return false;
  }
}

// Email Templates
export const emailTemplates = {
  submissionSuccess: (studentName: string, subjectName: string, totalMarks: number) => ({
    subject: `Marks Submitted Successfully - ${subjectName}`,
    html: `
      <div style="font-family: 'Crimson Text', serif; color: #2a1810; background-color: #f5ede0; padding: 20px;">
        <div style="max-width: 600px; margin: 0 auto; background-color: #faf6f1; border: 1px solid #d9cdc0; border-radius: 8px; padding: 30px;">
          <h1 style="color: #6b4423; font-family: 'Playfair Display', serif; margin-bottom: 10px;">Marks Submitted</h1>
          <p style="margin: 15px 0;">Dear ${studentName},</p>
          <p style="margin: 15px 0;">Your marks for <strong>${subjectName}</strong> have been successfully submitted.</p>
          <div style="background-color: #f0e6d8; padding: 15px; border-left: 4px solid #a0704d; margin: 20px 0;">
            <p style="margin: 5px 0;"><strong>Total Marks:</strong> ${totalMarks}</p>
          </div>
          <p style="margin: 15px 0;">Your submission is now locked. If you need to request any changes, please contact your course administrator.</p>
          <p style="margin: 15px 0;">Best regards,<br/>Thapar Grade Predictor System</p>
        </div>
      </div>
    `,
  }),

  editRequestReceived: (adminEmail: string, studentName: string, subjectName: string) => ({
    subject: `Edit Request Received - ${studentName} (${subjectName})`,
    html: `
      <div style="font-family: 'Crimson Text', serif; color: #2a1810; background-color: #f5ede0; padding: 20px;">
        <div style="max-width: 600px; margin: 0 auto; background-color: #faf6f1; border: 1px solid #d9cdc0; border-radius: 8px; padding: 30px;">
          <h1 style="color: #6b4423; font-family: 'Playfair Display', serif; margin-bottom: 10px;">New Edit Request</h1>
          <p style="margin: 15px 0;">An edit request has been received for review.</p>
          <div style="background-color: #f0e6d8; padding: 15px; border-left: 4px solid #a0704d; margin: 20px 0;">
            <p style="margin: 5px 0;"><strong>Student:</strong> ${studentName}</p>
            <p style="margin: 5px 0;"><strong>Subject:</strong> ${subjectName}</p>
          </div>
          <p style="margin: 15px 0;">Please log in to the admin dashboard to review and approve/reject this request.</p>
          <p style="margin: 15px 0;">Best regards,<br/>Thapar Grade Predictor System</p>
        </div>
      </div>
    `,
  }),

  editRequestApproved: (studentName: string, subjectName: string) => ({
    subject: `Edit Request Approved - ${subjectName}`,
    html: `
      <div style="font-family: 'Crimson Text', serif; color: #2a1810; background-color: #f5ede0; padding: 20px;">
        <div style="max-width: 600px; margin: 0 auto; background-color: #faf6f1; border: 1px solid #d9cdc0; border-radius: 8px; padding: 30px;">
          <h1 style="color: #6b4423; font-family: 'Playfair Display', serif; margin-bottom: 10px;">Edit Request Approved</h1>
          <p style="margin: 15px 0;">Dear ${studentName},</p>
          <p style="margin: 15px 0;">Your edit request for <strong>${subjectName}</strong> has been <span style="color: #27ae60; font-weight: bold;">approved</span>.</p>
          <p style="margin: 15px 0;">You now have a limited time window to update your marks. Please log in to your dashboard to make the necessary changes.</p>
          <p style="margin: 15px 0;">Best regards,<br/>Thapar Grade Predictor System</p>
        </div>
      </div>
    `,
  }),

  editRequestRejected: (studentName: string, subjectName: string, reason: string) => ({
    subject: `Edit Request Update - ${subjectName}`,
    html: `
      <div style="font-family: 'Crimson Text', serif; color: #2a1810; background-color: #f5ede0; padding: 20px;">
        <div style="max-width: 600px; margin: 0 auto; background-color: #faf6f1; border: 1px solid #d9cdc0; border-radius: 8px; padding: 30px;">
          <h1 style="color: #6b4423; font-family: 'Playfair Display', serif; margin-bottom: 10px;">Edit Request Update</h1>
          <p style="margin: 15px 0;">Dear ${studentName},</p>
          <p style="margin: 15px 0;">Your edit request for <strong>${subjectName}</strong> has been reviewed.</p>
          <div style="background-color: #f0e6d8; padding: 15px; border-left: 4px solid #a0704d; margin: 20px 0;">
            <p style="margin: 5px 0;"><strong>Reason:</strong> ${reason}</p>
          </div>
          <p style="margin: 15px 0;">If you have any questions, please contact your course administrator.</p>
          <p style="margin: 15px 0;">Best regards,<br/>Thapar Grade Predictor System</p>
        </div>
      </div>
    `,
  }),

  gradePredictionActivated: (studentName: string, subjectName: string) => ({
    subject: `Grade Prediction Active - ${subjectName}`,
    html: `
      <div style="font-family: 'Crimson Text', serif; color: #2a1810; background-color: #f5ede0; padding: 20px;">
        <div style="max-width: 600px; margin: 0 auto; background-color: #faf6f1; border: 1px solid #d9cdc0; border-radius: 8px; padding: 30px;">
          <h1 style="color: #6b4423; font-family: 'Playfair Display', serif; margin-bottom: 10px;">Grade Prediction Now Active</h1>
          <p style="margin: 15px 0;">Dear ${studentName},</p>
          <p style="margin: 15px 0;">Grade predictions for <strong>${subjectName}</strong> are now available! Sufficient student data has been collected to generate accurate predictions based on percentile rankings.</p>
          <p style="margin: 15px 0;">Log in to your dashboard to view your predicted grade.</p>
          <p style="margin: 15px 0;">Best regards,<br/>Thapar Grade Predictor System</p>
        </div>
      </div>
    `,
  }),

  // newRegistration: (userEmail: string, userName?: string) => ({
  //   subject: `New User Registered on Grade Predictor Platform`,
  //   html: `
  //     <div style="font-family: 'Crimson Text', serif; color: #2a1810; background-color: #f5ede0; padding: 20px;">
  //       <div style="max-width: 600px; margin: 0 auto; background-color: #faf6f1; border: 1px solid #d9cdc0; border-radius: 8px; padding: 30px;">
  //         <h1 style="color: #6b4423; font-family: 'Playfair Display', serif; margin-bottom: 10px;">New User Registration</h1>
  //         <p style="margin: 15px 0;">A new user has just registered and logged in for the first time.</p>
  //         <div style="background-color: #f0e6d8; padding: 15px; border-left: 4px solid #a0704d; margin: 20px 0;">
  //           <p style="margin: 5px 0;"><strong>Name:</strong> ${userName || 'N/A'}</p>
  //           <p style="margin: 5px 0;"><strong>Email:</strong> ${userEmail}</p>
  //           <p style="margin: 5px 0;"><strong>Time:</strong> ${new Date().toLocaleString()}</p>
  //         </div>
  //       </div>
  //     </div>
  //   `,
  // }),
};
