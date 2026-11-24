# Deploying the Grade Predictor (Vercel + Neon)

This guide puts the app online with:

| Piece | Where it runs | Cost |
| --- | --- | --- |
| The app (pages and Java code) | **Vercel**, using the repo's `Dockerfile` | Free (Hobby plan) |
| The database (PostgreSQL) | **Neon**, created from inside Vercel | Free plan |
| Plan B for the app | **Render**, if Vercel gives you trouble | Free |

**Why there is no separate "frontend" deploy:** this is a Spring Boot app. The Java code builds every HTML page on the server (Thymeleaf), so the pages and the backend are one program and deploy together. Vercel can run it because Vercel now runs Docker containers. That feature is called the **Container runtime** and is still in **Beta**. If it misbehaves, use [Plan B: Render](#plan-b-run-the-app-on-render) for the app and keep Neon for the database.

The live site uses **Google sign-in only**. The demo accounts (`aarav@thapar.edu` and the rest) exist only when you run the demo on your laptop.

## Before you start

- Push the code to a GitHub repository you can access.
- Have a **Vercel** account (sign up with GitHub) and a **Google Cloud** account.
- Decide the admin email. It must be an `@thapar.edu` address. Only this address gets the admin pages.

## Step 1: Import the project into Vercel

1. On [vercel.com](https://vercel.com), click **Add New → Project** and import your GitHub repository.
2. **Framework Preset:** Vercel sees the `Dockerfile` and picks **Container**. If it shows something else, choose **Container**.
3. Open **Environment Variables** and add these:

   | Name | Value |
   | --- | --- |
   | `PORT` | `8080` |
   | `ADMIN_EMAIL` | your admin address, e.g. `yourname@thapar.edu` |
   | `GOOGLE_LOGIN_ENABLED` | `false` for now (Step 3 turns it on) |

4. Click **Deploy**. The build compiles the Java code and runs the tests, which takes a few minutes. The deployment can finish while the site still shows an error. That is expected: there is no database yet.

`PORT=8080` matters. Vercel sends traffic to port 80 unless `PORT` says otherwise, and the app listens on whatever `PORT` says.

## Step 2: Create the Neon database inside Vercel

1. In your Vercel project, open the **Storage** tab, then **Create Database** (or **Browse Marketplace**), and choose **Neon**.
2. Pick the **Free** plan.
3. **Region:** choose one close to where Vercel runs your app. Every page load talks to the database a few times, so a distant database makes every page slow. Vercel runs code in Washington, D.C. (`iad1`) unless you change it under **Settings → Functions**, so **AWS US East (N. Virginia)** is the matching Neon region. If you change both, keep them close to each other.
4. Leave the **environment variable prefix** empty, then click **Create**.
5. **Connect** the database to your project for all environments (Production, Preview, Development). If you skipped this, open **Storage → your database → Connect Project**.
6. Redeploy: **Deployments → ⋯ (on the latest one) → Redeploy**. Vercel only passes new variables to new deployments.

You don't type any database settings. Neon adds `PGHOST_UNPOOLED`, `PGDATABASE`, `PGUSER` and `PGPASSWORD` to the project, and the app's `postgres` profile builds its connection from them (see `src/main/resources/application-postgres.properties`). On first start, the app creates its tables and loads the 104 curriculum subjects.

Open `https://<your-project>.vercel.app`. The sign-in page should now load.

## Step 3: Turn on Google sign-in

1. In [Google Cloud Console](https://console.cloud.google.com), create a project (or reuse one).
2. **APIs & Services → OAuth consent screen:**
   - Fill in the app name and your email.
   - If you can't choose **Internal**, which needs a thapar.edu Google Workspace project, choose **External**.
   - While the app is in **Testing**, add each person who will sign in under **Test users**.
3. **APIs & Services → Credentials → Create credentials → OAuth client ID → Web application.**
4. Under **Authorized redirect URIs**, add exactly:

   ```text
   https://<your-project>.vercel.app/login/oauth2/code/google
   ```

   Also add `http://localhost:8080/login/oauth2/code/google` if you want Google sign-in on your laptop.
5. Copy the **Client ID** and **Client secret**.
6. In Vercel, go to **Settings → Environment Variables** and add or change these:

   | Name | Value |
   | --- | --- |
   | `GOOGLE_CLIENT_ID` | the client ID |
   | `GOOGLE_CLIENT_SECRET` | the client secret |
   | `GOOGLE_LOGIN_ENABLED` | `true` |

7. Redeploy as in Step 2.

**Check it:** sign in with the admin address and you should land on **/admin**. Sign in with any other verified `@thapar.edu` account and you land on **/student**. Accounts outside `@thapar.edu` are refused.

From now on, every `git push` to the main branch redeploys the site automatically.

## What to expect on Vercel

- **Slow first visit after a quiet spell.** Vercel stops the container after about 5 minutes without traffic. The next visitor waits while Spring Boot starts again. Later pages are fast.
- **Nobody gets signed out by this.** Sign-ins are stored in the database (the `SPRING_SESSION` table), not in the container's memory. They survive restarts and work when Vercel runs more than one copy of the app.
- **Logs:** open a deployment, then **Logs**. Spring Boot's startup messages and any errors show up there.

## Plan B: run the app on Render

Use this if the Vercel Container runtime gives you trouble. Keep the Neon database from Step 2.

1. **Get the database values.** In Vercel, open **Storage → your Neon database** and copy `PGHOST_UNPOOLED`, `PGDATABASE`, `PGUSER` and `PGPASSWORD`. You can also get them in the Neon console under **Connect**, with **connection pooling switched off**.
2. On [render.com](https://render.com), click **New → Web Service** and connect your GitHub repository.
3. **Language:** Docker (Render finds the `Dockerfile`). **Instance type:** Free.
4. Add these environment variables:

   | Name | Value |
   | --- | --- |
   | `PGHOST_UNPOOLED`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | the four values from Neon |
   | `PGSSLMODE` | `require` |
   | `ADMIN_EMAIL` | your admin address |
   | `GOOGLE_LOGIN_ENABLED` | `true` |
   | `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | from Step 3 |

   Don't set `PORT`. Render sets it itself, and the app follows it.
5. Click **Create Web Service**, then add `https://<your-service>.onrender.com/login/oauth2/code/google` to the Google redirect URIs.

Render's free plan stops the app after 15 minutes without traffic, and waking it takes about a minute.

**Why not a Render database?** A free Render PostgreSQL database expires 30 days after you create it. After a 14-day grace period it is deleted with all its data, so Neon is the better free choice. If you still want Render's database: create **New → Postgres (Free)** and set `DB_URL=jdbc:postgresql://<host>:5432/<database>`, `DB_USER` and `DB_PASSWORD` from its **Connections** panel. Those three override the `PG...` values.

## All settings the app reads

| Variable | Needed | What it does |
| --- | --- | --- |
| `PORT` | Vercel: yes (`8080`) | Port the app listens on. Render sets it for you. |
| `ADMIN_EMAIL` | yes | The single `@thapar.edu` admin account. |
| `GOOGLE_LOGIN_ENABLED` | yes | `true` turns on Google sign-in. |
| `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET` | with Google on | From Google Cloud. |
| `PGHOST_UNPOOLED`, `PGDATABASE`, `PGUSER`, `PGPASSWORD` | set by Neon on Vercel | Database connection. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | no | Use instead of the `PG...` values for any other PostgreSQL. |
| `PGSSLMODE` | no | `require` forces an encrypted database connection (default `prefer`; Neon accepts only encrypted connections either way). |
| `DB_POOL_SIZE` | no | Database connections per copy of the app (default 5). |
| `SECURE_COOKIES` | no | Default `true`, which suits HTTPS hosts. Set `false` only for plain-HTTP testing. |

The Docker image already sets `SPRING_PROFILES_ACTIVE=postgres`, so you don't need to set it.

## When something goes wrong

| What you see | Likely cause and fix |
| --- | --- |
| `Could not resolve placeholder 'PGPASSWORD'` in the logs | The Neon database isn't connected to this project, or you didn't redeploy after connecting it. |
| `Google login requires GOOGLE_CLIENT_ID and GOOGLE_CLIENT_SECRET` | `GOOGLE_LOGIN_ENABLED` is `true` but the client ID or secret is empty. |
| Google says `redirect_uri_mismatch` | The redirect URI in Google Cloud must match the site address exactly: `https`, same domain, ending in `/login/oauth2/code/google`. |
| Back on the sign-in page with an error after choosing a Google account | The account isn't a verified `@thapar.edu` Google account, or it isn't a **Test user** while the consent screen is in Testing. |
| The site answers with an error on Vercel but the build passed | Check `PORT=8080` is set, then read the deployment's **Logs**. |
| Pages are slow every time | The database region is far from the app region. Recreate the Neon database near the app (Step 2). |

## Running it on your laptop (unchanged)

```sh
./run.sh demo          # sample data and demo accounts, nothing to configure
./run.sh               # real mode with Google sign-in, using values from .env
docker compose up      # app plus a local PostgreSQL, like production
```
