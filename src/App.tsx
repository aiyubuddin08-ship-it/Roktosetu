/**
 * @license
 * SPDX-License-Identifier: Apache-2.0
 */

import React, { useEffect } from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { SpeedInsights } from '@vercel/speed-insights/react';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import { ErrorBoundary } from './components/ErrorBoundary';
import { Layout } from './components/Layout';
import { Home } from './pages/Home';
import { Dashboard } from './pages/Dashboard';
import { Onboarding } from './pages/Onboarding';
import { BloodRequests } from './pages/BloodRequests';
import { DonorSearch } from './pages/DonorSearch';
import { Profile } from './pages/Profile';
import { Leaderboard } from './pages/Leaderboard';
import { Directory } from './pages/Directory';
import { AdminDashboard } from './pages/AdminDashboard';
import { PrivacyPolicy } from './pages/PrivacyPolicy';
import { Heart } from 'lucide-react';

import { seedSampleData } from './lib/seed';

function AppContent() {
  const { user, profile, loading, isAuthReady } = useAuth();

  useEffect(() => {
    if (isAuthReady && user) {
      seedSampleData().catch(err => {
        console.warn("Sample data seeding skipped or failed (likely permissions):", err.message);
      });
    }
  }, [isAuthReady, user]);

  if (!isAuthReady || loading) {
    return (
      <div className="min-h-screen flex items-center justify-center bg-white">
        <div className="flex flex-col items-center">
          <div className="w-16 h-16 bg-red-600 rounded-full flex items-center justify-center blood-pulse shadow-xl shadow-red-200">
            <Heart className="w-10 h-10 text-white fill-current" />
          </div>
          <p className="mt-4 text-gray-800 font-bold animate-pulse text-lg">রক্তসেতু তে যুক্ত হচ্ছি...</p>
        </div>
      </div>
    );
  }

  // If not logged in, show Home (Landing/Login)
  if (!user) {
    return (
      <Router>
        <Routes>
          <Route path="/" element={<Home />} />
          <Route path="/privacy" element={<PrivacyPolicy />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Router>
    );
  }

  // If logged in but no profile, show Onboarding
  if (!profile) {
    return (
      <div className="min-h-screen bg-gray-50 flex items-center justify-center p-4">
        <Onboarding />
      </div>
    );
  }

  // Normal app flow
  return (
    <Router>
      <Layout>
        <Routes>
          <Route path="/" element={<Dashboard />} />
          <Route path="/requests" element={<BloodRequests />} />
          <Route path="/search" element={<DonorSearch />} />
          <Route path="/leaderboard" element={<Leaderboard />} />
          <Route path="/directory" element={<Directory />} />
          <Route path="/admin-panel" element={<AdminDashboard />} />
          <Route path="/profile" element={<Profile />} />
          <Route path="/privacy" element={<PrivacyPolicy />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </Layout>
    </Router>
  );
}

import { ThemeProvider } from './contexts/ThemeContext';
import { NotificationProvider } from './contexts/NotificationContext';

export default function App() {
  return (
    <ErrorBoundary>
      <ThemeProvider>
        <AuthProvider>
          <NotificationProvider>
            <AppContent />
            <SpeedInsights />
          </NotificationProvider>
        </AuthProvider>
      </ThemeProvider>
    </ErrorBoundary>
  );
}

