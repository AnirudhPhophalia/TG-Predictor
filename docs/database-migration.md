# Legacy database mapping

The legacy Prisma schema describes MongoDB collections with ObjectId keys. The Java application uses relational JPA entities with generated numeric keys. A MongoDB connection string cannot be supplied as DB_URL; DB_URL must be a JDBC H2 or PostgreSQL URL.

| Original collection | Java table/entity | Notes |
| --- | --- | --- |
| User | users / User | Verified Google identity, admission year and last login are added. Passwords are not imported; users sign in through Google. |
| Subject | subjects / Subject | Curriculum and active status. |
| Mark | marks / Mark | Unique student/subject, optional breakdown and lock state. |
| Statistics | subject_statistics / SubjectStatistics | Recomputed from imported marks, never trusted blindly. |
| HistoricalCutoff | historical_cutoffs / FallbackCutoff | Current fallback boundaries. |
| New year archive | past_year_cutoffs / PastYearCutoff | Unique subject/year; no invented historical values. |
| EditRequest | edit_requests / UnlockRequest | An approval unlocks the student's record; an admin never supplies replacement marks. |
| Poll / PollResponse | polls / Poll, poll_responses / PollResponse | One response per student/poll; manual boundaries belong to the poll. |
| New settings | platform_settings / PlatformSettings | Maintenance mode, unlock requests and contact email. |

The old schema's Semester, Notification, EditHistory and AuditLog collections do not have a direct import implementation in this release. The existing React app had no complete working flows for all of these collections. Unlock-request status is visible on the student's requests page; optional exam components remain on their private marks page. No external email notifications are sent.

## Moving actual data

Migration has not been run. No database URI, export or verified historical records were provided.

For a real migration, first back up and export the MongoDB collections. Use a dedicated Java migration job to create an ObjectId-to-numeric-ID mapping, then import subjects and users, marks, cutoffs, polls, responses and requests in foreign-key order. Reject invalid scores, invalid domains and duplicate student/subject or student/poll combinations. Reconcile roles to ADMIN_EMAIL, link verified users on first Google login, recalculate grades/statistics and compare aggregate counts. Import known academic years explicitly; the original cutoff schema did not include a year, so a year cannot be inferred reliably.

Retain the original export and ID map until counts, relationships, owner access and aggregate privacy checks pass. This document is a migration plan, not an automated importer or a claim that the legacy database is already connected.
