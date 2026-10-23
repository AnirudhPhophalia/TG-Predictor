# Thapar Grade Predictor

A production-ready grade prediction platform for Thapar Institute of Engineering and Technology students. Predicts student grades based on percentile ranking and comparative performance analysis.

![Build Status](https://img.shields.io/badge/status-production--ready-green)
![License](https://img.shields.io/badge/license-proprietary-red)
![Next.js](https://img.shields.io/badge/Next.js-16-black)
![TypeScript](https://img.shields.io/badge/TypeScript-5.7-blue)
![MongoDB](https://img.shields.io/badge/MongoDB-Atlas-green)

## Features

### Student Features
- ✅ Secure Google OAuth authentication (Thapar domain restricted)
- ✅ One-time marks submission per subject
- ✅ Grade prediction based on percentile ranking
- ✅ Historical grade boundaries fallback
- ✅ Real-time statistics and analytics
- ✅ Comparative performance analysis
- ✅ Internal marks breakdown tracking
- ✅ Edit request workflow for mark modifications
- ✅ In-app notifications and email alerts
- ✅ Detailed statistical visualizations

### Admin Features
- ✅ Subject and curriculum management
- ✅ Grade cutoff configuration
- ✅ Edit request approval/rejection
- ✅ User account management
- ✅ System-wide analytics dashboard
- ✅ Comprehensive audit logging
- ✅ Data export (CSV, Excel, PDF)
- ✅ Statistics caching and optimization

### Technical Features
- ✅ Multi-role access control
- ✅ Audit trail logging
- ✅ Advanced grade prediction engine
- ✅ Statistical analysis (median, mode, std dev, percentiles)
- ✅ Responsive design (mobile-first)
- ✅ Ancient parchment academic theme
- ✅ Real-time data visualization with Recharts
- ✅ RESTful API with validation

## Tech Stack

### Frontend
- **Framework**: Next.js 16 with App Router
- **Language**: TypeScript 5.7
- **UI Framework**: ShadCN UI
- **Styling**: Tailwind CSS v4
- **State Management**: SWR for data fetching
- **Charts**: Recharts
- **Authentication**: NextAuth.js
- **Fonts**: Playfair Display (headings), Crimson Text (body)

### Backend
- **Runtime**: Node.js 18+
- **Framework**: Next.js 16 (Route Handlers)
- **Database**: MongoDB Atlas
- **ORM**: Prisma
- **Validation**: Zod
- **Authentication**: NextAuth.js with Google OAuth

### DevOps
- **Hosting**: Vercel (Frontend + API)
- **Database Hosting**: MongoDB Atlas
- **Package Manager**: pnpm

## Project Structure

```
thapar-grade-predictor/
├── app/                          # Next.js app directory
│   ├── api/                      # API route handlers
│   │   ├── auth/                 # NextAuth routes
│   │   ├── admin/                # Admin API endpoints
│   │   └── student/              # Student API endpoints
│   ├── admin/                    # Admin pages
│   ├── student/                  # Student pages
│   ├── login/                    # Login page
│   ├── layout.tsx                # Root layout with SessionProvider
│   ├── page.tsx                  # Home page (redirects to dashboard)
│   ├── globals.css               # Global styles & theme
│   └── providers.tsx             # NextAuth provider wrapper
├── lib/
│   ├── auth.ts                   # NextAuth configuration
│   ├── auth-utils.ts             # Authentication utilities
│   ├── services/
│   │   ├── db.ts                 # Database utility functions
│   │   └── grade-predictor.ts    # Grade prediction engine
│   ├── utils.ts                  # Utility functions
│   └── generated/prisma.ts       # Prisma client (generated)
├── prisma/
│   ├── schema.prisma             # Database schema (10+ models)
│   └── seed.ts                   # Database seed script
├── public/                       # Static assets
├── components/ui/               # ShadCN UI components
├── middleware.ts                 # Next.js middleware
├── DEPLOYMENT.md                 # Deployment guide
├── README.md                     # This file
├── next.config.mjs              # Next.js config
├── tsconfig.json                # TypeScript config
└── package.json                 # Dependencies

```

## Database Schema

### Core Models
- **User**: Students and admins
- **Subject**: Courses/subjects
- **Semester**: Academic periods
- **Mark**: Mark submissions (unique per student-subject)
- **Statistics**: Cached statistics per subject
- **HistoricalCutoff**: Grade boundaries per subject
- **EditRequest**: Mark modification workflow
- **EditHistory**: Audit trail for mark changes
- **Notification**: User notifications
- **AuditLog**: System audit trail

## Getting Started

### Prerequisites
- Node.js 18+
- MongoDB Atlas account
- Google OAuth credentials
- pnpm (recommended) or npm

### Installation

1. **Clone repository**
```bash
git clone <repo-url>
cd thapar-grade-predictor
```

2. **Install dependencies**
```bash
pnpm install
```

3. **Setup environment variables**
```bash
cp .env.example .env.local
# Edit .env.local with your credentials
```

4. **Initialize database**
```bash
# Generate Prisma Client
pnpm db:generate

# Push schema to MongoDB
pnpm db:push

# Seed initial data (optional)
pnpm db:seed
```

5. **Start development server**
```bash
pnpm dev
```

Visit http://localhost:3000

## Environment Variables

Create `.env.local`:

```bash
# Database
DATABASE_URL=mongodb+srv://username:password@cluster.mongodb.net/database

# NextAuth
NEXTAUTH_URL=http://localhost:3000
NEXTAUTH_SECRET=$(openssl rand -base64 32)

# Google OAuth
GOOGLE_CLIENT_ID=your-client-id
GOOGLE_CLIENT_SECRET=your-client-secret

# Email (optional)
EMAIL_HOST=smtp.gmail.com
EMAIL_PORT=587
EMAIL_USER=your-email@gmail.com
EMAIL_PASSWORD=your-app-password
EMAIL_FROM=noreply@thapar-grade-predictor.com

# Application
NEXT_PUBLIC_APP_URL=http://localhost:3000
NEXT_PUBLIC_API_BASE_URL=http://localhost:3000/api
THAPAR_EMAIL_DOMAIN=thapar.edu
MINIMUM_SUBMISSIONS_FOR_PREDICTION=100
```

## Grade Prediction Algorithm

### Percentile-Based Prediction (When ≥100 submissions)
```
Top 10% (90-100 percentile)     → Grade A
10-20% (80-90 percentile)       → Grade A-
20-45% (55-80 percentile)       → Grade B
45-70% (30-55 percentile)       → Grade B-
70-80% (20-30 percentile)       → Grade C
80-100% (0-20 percentile)       → Grade D
Marks < 35 (always)             → Grade E
```

### Historical Cutoff Fallback (When <100 submissions)
```
Marks ≥ 85 → A
Marks ≥ 75 → A-
Marks ≥ 65 → B
Marks ≥ 55 → B-
Marks ≥ 45 → C
Marks ≥ 35 → D
Marks < 35 → E
```

## API Endpoints

### Authentication
- `GET/POST /api/auth/[...nextauth]` - NextAuth routes

### Public
- `GET /api/subjects` - List active subjects

### Student
- `GET /api/student/marks` - Get student marks
- `POST /api/student/marks` - Submit marks
- `GET /api/student/statistics/[subjectId]` - Get subject statistics
- `POST /api/student/marks/[id]/edit-request` - Request mark edit

### Admin (Protected)
- `GET /api/admin/subjects` - List all subjects
- `POST /api/admin/subjects` - Create subject
- `GET /api/admin/statistics` - System statistics
- `GET /api/admin/edit-requests` - Edit requests
- `POST /api/admin/edit-requests/[id]/approve` - Approve edit
- `GET /api/admin/users` - List users
- `GET /api/admin/audit-logs` - Audit logs

## Deployment

### Quick Deploy to Vercel

```bash
# 1. Push to GitHub
git add .
git commit -m "Initial commit"
git push origin main

# 2. Import in Vercel
# Go to vercel.com/new and select GitHub repository

# 3. Set environment variables in Vercel dashboard

# 4. Deploy
```

### Full Deployment Guide
See [DEPLOYMENT.md](./DEPLOYMENT.md) for detailed instructions.

## Development

### Database Commands
```bash
# Generate Prisma Client
pnpm db:generate

# Push schema to database
pnpm db:push

# Reset database (careful!)
pnpm prisma migrate reset

# Seed data
pnpm db:seed

# Open Prisma Studio
pnpm prisma studio
```

### Code Quality
```bash
# Lint
pnpm lint

# Type check
pnpm tsc --noEmit
```

## Testing the Application

### Test Login
1. Visit http://localhost:3000
2. Click "Continue with Google"
3. Use a test @thapar.edu email (or configure for your domain)

### Test Marks Submission
1. Log in as a student
2. Click "Submit Marks" on a subject
3. Enter marks (0-100)
4. Submit

### Test Admin Features
1. Set user role to 'admin' in database
2. Visit /admin
3. Manage subjects, cutoffs, and requests

## Performance Optimization

### Implemented
- [x] SWR caching with revalidation
- [x] Prisma query optimization
- [x] Database indexing on frequently queried fields
- [x] CSS optimization with Tailwind
- [x] Image optimization
- [x] Statistics caching

### Recommendations
- [ ] Implement Redis for statistics caching
- [ ] Add API rate limiting
- [ ] Implement pagination for large datasets
- [ ] Use database views for complex queries
- [ ] CDN for static assets (Vercel Edge)

## Security

### Implemented
- ✅ Domain-restricted authentication (@thapar.edu)
- ✅ JWT session management
- ✅ HTTPS/TLS enforced
- ✅ CSRF protection via NextAuth
- ✅ Input validation with Zod
- ✅ Row-level access control
- ✅ Comprehensive audit logging
- ✅ Secure password handling

### Best Practices
- Rotate NEXTAUTH_SECRET regularly
- Keep dependencies updated
- Monitor audit logs
- Enable database backups
- Use strong database credentials

## Troubleshooting

### Common Issues

**Google OAuth Error**
- Verify redirect URIs in Google Cloud Console
- Clear browser cookies
- Check GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET

**Database Connection**
- Test MongoDB connection string
- Verify IP whitelist in MongoDB Atlas
- Check network connectivity

**Prisma Errors**
```bash
# Clear Prisma cache
rm -rf node_modules/.prisma

# Regenerate
pnpm db:generate
```

## Contributing

This is a proprietary project for Thapar Institute. For contributions, please follow:
1. Create feature branches
2. Follow TypeScript and Prettier standards
3. Add tests for new features
4. Update documentation
5. Submit pull request for review

## Support

For issues or questions:
1. Check the [DEPLOYMENT.md](./DEPLOYMENT.md) guide
2. Review error logs in Vercel dashboard
3. Check database logs in MongoDB Atlas
4. Review API documentation above

## License

This project is proprietary to Thapar Institute of Engineering and Technology.
Unauthorized copying or distribution is prohibited.

## Version History

### v1.0.0 (Current)
- Initial production release
- Complete authentication system
- Grade prediction engine
- Analytics and visualization
- Admin dashboard
- Audit logging

---

**Last Updated**: June 2026
**Maintained By**: Development Team
**Status**: Production Ready
