'use client';

import { useState } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { ChevronDown, ArrowLeft, Mail } from 'lucide-react';
import Link from 'next/link';

const faqs = [
  {
    category: 'Marks Submission',
    questions: [
      {
        q: 'When can I submit my marks?',
        a: 'You can submit marks as soon as the subject is active in the system. Once submitted, your marks are locked and cannot be changed unless you request a modification through the edit request system.',
      },
      {
        q: 'Can I edit my submitted marks?',
        a: 'After your initial submission, marks are locked. To make changes, click "Request Edit" on your marks card and provide a reason. An administrator will review your request and approve or reject it.',
      },
      {
        q: 'What if I submit wrong marks by mistake?',
        a: 'You can submit an edit request explaining the error. Administrators review all edit requests and will approve corrections that are justified.',
      },
    ],
  },
  {
    category: 'Grade Prediction',
    questions: [
      {
        q: 'How is my grade predicted?',
        a: 'Your grade is based on your percentile ranking among all students in the class. The system calculates your position in the distribution (Top 10%, 10-20%, etc.) and assigns grades accordingly. Special rule: Any marks below 35 automatically result in grade E.',
      },
      {
        q: 'When will my grade prediction be available?',
        a: 'Grade predictions become available only after 100+ students have submitted marks for a subject. Before that, the system shows historical grade boundaries for reference.',
      },
      {
        q: 'What is percentile ranking?',
        a: 'Percentile ranking shows your position among all students. For example, if you are in the 85th percentile, it means you scored better than 85% of your classmates.',
      },
    ],
  },
  {
    category: 'System Features',
    questions: [
      {
        q: 'What information can I see in the Analytics section?',
        a: 'You can view class statistics including average marks, median, distribution charts, your percentile ranking, and how your marks compare to the class average.',
      },
      {
        q: 'Can I download my marks?',
        a: 'Currently, marks are viewable in your dashboard. Contact your administrator if you need an official document.',
      },
      {
        q: 'Is my data secure?',
        a: 'Yes. The system uses secure authentication (Google OAuth) with domain verification. Only authenticated Thapar students can access their data, which is encrypted at rest.',
      },
    ],
  },
  {
    category: 'Account & Access',
    questions: [
      {
        q: 'Why can\'t I login?',
        a: 'You must login using your official Thapar Institute email (@thapar.edu). If you still have issues, check that your email is verified and contact your administrator.',
      },
      {
        q: 'What if I forget to logout?',
        a: 'For security, sessions automatically expire after a period of inactivity. You can manually logout from your profile page.',
      },
      {
        q: 'Can I change my profile information?',
        a: 'Currently, profile information is pulled from your Thapar account. To update information, contact your administrative office.',
      },
    ],
  },
];

export default function HelpPage() {
  const [expandedCategory, setExpandedCategory] = useState<string | null>(null);
  const [expandedQuestion, setExpandedQuestion] = useState<string | null>(null);

  return (
    <div className="min-h-screen bg-gradient-to-br from-[#f5ede0] via-[#faf6f1] to-[#f5ede0] burnt-texture">
      <div className="max-w-4xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Header */}
        <Link
          href="/student"
          className="flex items-center gap-2 text-[#6b4423] hover:text-[#8b5a3c] transition-colors mb-8"
        >
          <ArrowLeft className="w-5 h-5" />
          <span>Back to Dashboard</span>
        </Link>

        <motion.div
          initial={{ opacity: 0, y: -20 }}
          animate={{ opacity: 1, y: 0 }}
          className="mb-12"
        >
          <h1 className="text-4xl font-bold text-[#6b4423] mb-2" style={{ fontFamily: 'var(--font-playfair)' }}>
            Help & FAQ
          </h1>
          <p className="text-[#8b6f47]">
            Find answers to common questions about the Thapar Grade Predictor
          </p>
        </motion.div>

        {/* FAQ Categories */}
        <div className="space-y-6">
          {faqs.map((category, categoryIndex) => (
            <motion.div
              key={category.category}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: categoryIndex * 0.1 }}
              className="vintage-card parchment-edge"
            >
              {/* Category Header */}
              <button
                onClick={() =>
                  setExpandedCategory(
                    expandedCategory === category.category ? null : category.category
                  )
                }
                className="w-full flex items-center justify-between p-6 hover:bg-[#f0e6d8] transition-colors"
              >
                <h2 className="text-xl font-bold text-[#6b4423]" style={{ fontFamily: 'var(--font-playfair)' }}>
                  {category.category}
                </h2>
                <motion.div
                  animate={{
                    rotate:
                      expandedCategory === category.category ? 180 : 0,
                  }}
                  transition={{ duration: 0.3 }}
                >
                  <ChevronDown className="w-6 h-6 text-[#a0704d]" />
                </motion.div>
              </button>

              {/* Questions */}
              <AnimatePresence>
                {expandedCategory === category.category && (
                  <motion.div
                    initial={{ opacity: 0, height: 0 }}
                    animate={{ opacity: 1, height: 'auto' }}
                    exit={{ opacity: 0, height: 0 }}
                    transition={{ duration: 0.3 }}
                    className="border-t border-[#d9cdc0]"
                  >
                    <div className="p-6 space-y-4">
                      {category.questions.map((item, qIndex) => (
                        <motion.div
                          key={qIndex}
                          initial={{ opacity: 0, y: 10 }}
                          animate={{ opacity: 1, y: 0 }}
                          transition={{ delay: qIndex * 0.1 }}
                          className="p-4 bg-gradient-to-r from-[#f0e6d8] to-transparent rounded-lg"
                        >
                          <button
                            onClick={() =>
                              setExpandedQuestion(
                                expandedQuestion === `${category.category}-${qIndex}`
                                  ? null
                                  : `${category.category}-${qIndex}`
                              )
                            }
                            className="w-full flex items-center justify-between hover:text-[#8b5a3c] transition-colors"
                          >
                            <p className="font-semibold text-[#6b4423] text-left">
                              {item.q}
                            </p>
                            <motion.div
                              animate={{
                                rotate:
                                  expandedQuestion ===
                                  `${category.category}-${qIndex}`
                                    ? 180
                                    : 0,
                              }}
                              transition={{ duration: 0.3 }}
                            >
                              <ChevronDown className="w-5 h-5 text-[#a0704d] flex-shrink-0 ml-4" />
                            </motion.div>
                          </button>

                          <AnimatePresence>
                            {expandedQuestion ===
                              `${category.category}-${qIndex}` && (
                              <motion.div
                                initial={{ opacity: 0, height: 0 }}
                                animate={{ opacity: 1, height: 'auto' }}
                                exit={{ opacity: 0, height: 0 }}
                                transition={{ duration: 0.3 }}
                                className="mt-3 pt-3 border-t border-[#d9cdc0]"
                              >
                                <p className="text-[#6b5448] leading-relaxed">
                                  {item.a}
                                </p>
                              </motion.div>
                            )}
                          </AnimatePresence>
                        </motion.div>
                      ))}
                    </div>
                  </motion.div>
                )}
              </AnimatePresence>
            </motion.div>
          ))}
        </div>

        {/* Contact Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.4 }}
          className="mt-12 vintage-card parchment-edge text-center"
        >
          <Mail className="w-12 h-12 text-[#a0704d] mx-auto mb-4" />
          <h3 className="text-2xl font-bold text-[#6b4423] mb-2" style={{ fontFamily: 'var(--font-playfair)' }}>
            Didn&apos;t find your answer?
          </h3>
          <p className="text-[#8b6f47] mb-6">
            Contact your course administrator or the IT helpdesk for further assistance.
          </p>
          <a
            href="mailto:support@thapar.edu"
            className="inline-block bg-[#a0704d] text-[#f5ede0] px-6 py-3 rounded-lg hover:bg-[#8b5a3c] transition-colors font-semibold"
          >
            Send Email
          </a>
        </motion.div>
      </div>
    </div>
  );
}
