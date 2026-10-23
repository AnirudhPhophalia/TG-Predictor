import { PrismaClient } from '@prisma/client';

const prisma = new PrismaClient();

async function main() {
  const email = 'anirudhphophalia@gmail.com';
  console.log(`Setting admin role for ${email}...`);
  
  const user = await prisma.user.upsert({
    where: { email },
    update: { role: 'admin' },
    create: {
      email,
      role: 'admin',
      name: 'Anirudh Phophalia',
    },
  });

  console.log(`User ${user.email} is now an admin!`);
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
