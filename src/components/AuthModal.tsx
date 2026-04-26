import React, { useState } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { motion, AnimatePresence } from 'motion/react';
import { X, Mail, Lock, User as UserIcon, LogIn, UserPlus, ArrowRight, ChevronLeft } from 'lucide-react';

interface AuthModalProps {
  isOpen: boolean;
  onClose: () => void;
  initialMode?: 'login' | 'register' | 'forgot';
}

export function AuthModal({ isOpen, onClose, initialMode = 'login' }: AuthModalProps) {
  const { 
    login, 
    loginWithEmail, 
    registerWithEmail, 
    resetPassword,
    loggingIn, 
    error, 
    clearError 
  } = useAuth();
  
  const [mode, setMode] = useState<'login' | 'register' | 'forgot'>(initialMode);
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [displayName, setDisplayName] = useState('');
  const [agreedToPolicy, setAgreedToPolicy] = useState(false);
  const [statusMessage, setStatusMessage] = useState<string | null>(null);
  const [showPassword, setShowPassword] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (mode === 'register' && !agreedToPolicy) {
      return;
    }
    setStatusMessage(null);
    try {
      if (mode === 'login') {
        await loginWithEmail(email, password);
        onClose();
      } else if (mode === 'register') {
        await registerWithEmail(email, password, displayName);
        onClose();
      } else if (mode === 'forgot') {
        await resetPassword(email);
        setStatusMessage('পাসওয়ার্ড রিসেট ইমেইল পাঠানো হয়েছে। ইনবক্স চেক করুন।');
      }
    } catch (err) {
      // Handled in context
    }
  };

  const handleGoogleLogin = async () => {
    try {
      await login();
      onClose();
    } catch (err) {
      // Error handled in AuthContext
    }
  };

  if (!isOpen) return null;

  return (
    <AnimatePresence>
      <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
        {/* Backdrop */}
        <motion.div 
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          onClick={onClose}
          className="absolute inset-0 bg-black/60 backdrop-blur-sm"
        />

        {/* Modal */}
        <motion.div 
          initial={{ opacity: 0, scale: 0.9, y: 20 }}
          animate={{ opacity: 1, scale: 1, y: 0 }}
          exit={{ opacity: 0, scale: 0.9, y: 20 }}
          className="relative w-full max-w-md bg-white dark:bg-gray-900 rounded-[40px] shadow-2xl overflow-hidden"
        >
          {/* Header */}
          <div className="p-8 pb-0 flex justify-between items-start">
            <div>
              <div className="flex items-center gap-2">
                {mode === 'forgot' ? (
                  <button 
                    onClick={() => {
                      setMode('login');
                      setStatusMessage(null);
                    }} 
                    className="p-1 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full"
                  >
                    <ChevronLeft className="w-5 h-5 text-gray-400" />
                  </button>
                ) : null}
                <h2 className="text-3xl font-black text-gray-900 dark:text-white uppercase tracking-tighter">
                  {mode === 'login' ? 'স্বাগতম' : mode === 'register' ? 'নতুন একাউন্ট' : 'পাসওয়ার্ড রিসেট'}
                </h2>
              </div>
              <p className="text-gray-500 dark:text-gray-400 font-bold mt-1">
                {mode === 'login' ? 'আপনার একাউন্টে লগইন করুন' : 
                 mode === 'register' ? 'রক্তসেতু কমিউনিটিতে যোগ দিন' : 
                 'আপনার ইমেইল ঠিকানাটি লিখুন'}
              </p>
            </div>
            <button 
              onClick={onClose}
              className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-2xl transition-colors"
            >
              <X className="w-6 h-6 text-gray-400" />
            </button>
          </div>

          <div className="p-8 space-y-6">
            {/* Social Login - Only show for login/register modes */}
            {(mode === 'login' || mode === 'register') && (
              <div className="grid grid-cols-1 gap-4">
                <button 
                  onClick={handleGoogleLogin}
                  disabled={loggingIn}
                  className="py-4 px-4 bg-white dark:bg-gray-800 border-2 border-gray-100 dark:border-gray-700 rounded-2xl font-bold text-gray-700 dark:text-white flex items-center justify-center gap-2 hover:border-red-600 transition-all active:scale-95 disabled:opacity-50 text-base"
                >
                  <img src="https://www.google.com/favicon.ico" className="w-5 h-5" alt="Google" />
                  গুগল দিয়ে প্রবেশ করুন
                </button>
              </div>
            )}

            {(mode === 'login' || mode === 'register') && (
              <div className="relative">
                <div className="absolute inset-0 flex items-center">
                  <div className="w-full border-t border-gray-100 dark:border-gray-800"></div>
                </div>
                <div className="relative flex justify-center text-xs uppercase">
                  <span className="bg-white dark:bg-gray-900 px-4 text-gray-400 font-bold tracking-widest leading-none">অথবা ইমেইল দিয়ে</span>
                </div>
              </div>
            )}

            {/* Messages */}
            {error && (
              <div className="p-4 bg-red-50 dark:bg-red-900/10 border border-red-100 dark:border-red-900/20 text-red-600 dark:text-red-400 rounded-2xl text-sm font-bold animate-shake">
                {error}
              </div>
            )}
            {statusMessage && (
              <div className="p-4 bg-blue-50 dark:bg-blue-900/10 border border-blue-100 dark:border-blue-900/20 text-blue-600 dark:text-blue-400 rounded-2xl text-sm font-bold">
                {statusMessage}
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit} className="space-y-4">
              {mode === 'register' && (
                <div className="space-y-2">
                  <label className="text-xs font-black text-gray-400 uppercase tracking-widest ml-1">পূর্ণ নাম</label>
                  <div className="relative">
                    <UserIcon className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input 
                      type="text"
                      required
                      value={displayName}
                      onChange={(e) => setDisplayName(e.target.value)}
                      placeholder="আপনার নাম লিখুন"
                      className="w-full pl-12 pr-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                    />
                  </div>
                </div>
              )}

              <div className="space-y-2">
                <label className="text-xs font-black text-gray-400 uppercase tracking-widest ml-1">ইমেইল ঠিকানা</label>
                <div className="relative">
                  <Mail className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  <input 
                    type="email"
                    required
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    placeholder="example@mail.com"
                    className="w-full pl-12 pr-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                  />
                </div>
              </div>

              {mode !== 'forgot' && (
                <div className="space-y-2">
                  <div className="flex justify-between items-center px-1">
                    <label className="text-xs font-black text-gray-400 uppercase tracking-widest">পাসওয়ার্ড</label>
                    {mode === 'login' && (
                      <button 
                        type="button"
                        onClick={() => {
                          setMode('forgot');
                          clearError();
                        }}
                        className="text-[10px] font-black text-red-600 uppercase tracking-widest hover:underline"
                      >
                        ভুলে গেছেন?
                      </button>
                    )}
                  </div>
                  <div className="relative">
                    <Lock className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                    <input 
                      type={showPassword ? 'text' : 'password'}
                      required
                      value={password}
                      onChange={(e) => setPassword(e.target.value)}
                      placeholder="••••••••"
                      className="w-full pl-12 pr-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                    />
                  </div>
                </div>
              )}

              {mode === 'register' && (
                <div className="flex items-start gap-3 px-1 py-2">
                  <input 
                    type="checkbox"
                    id="privacy-agree"
                    checked={agreedToPolicy}
                    onChange={(e) => setAgreedToPolicy(e.target.checked)}
                    className="mt-1 w-4 h-4 rounded border-gray-300 text-red-600 focus:ring-red-600 cursor-pointer"
                  />
                  <label htmlFor="privacy-agree" className="text-sm font-bold text-gray-500 cursor-pointer select-none">
                    আমি রক্তসেতুর <a href="/privacy" target="_blank" className="text-red-600 hover:underline">প্রাইভেসি পলিসি</a> এর সাথে একমত।
                  </label>
                </div>
              )}

              <button 
                type="submit"
                disabled={loggingIn || (mode === 'register' && !agreedToPolicy)}
                className="w-full py-5 bg-red-600 text-white rounded-[24px] font-black text-xl hover:bg-red-700 transition shadow-2xl shadow-red-200 active:scale-95 disabled:opacity-50 flex items-center justify-center gap-3"
              >
                {loggingIn ? (
                  <span className="flex items-center gap-2">
                    <span className="w-2 h-2 bg-white rounded-full animate-bounce" />
                    <span className="w-2 h-2 bg-white rounded-full animate-bounce [animation-delay:0.2s]" />
                    <span className="w-2 h-2 bg-white rounded-full animate-bounce [animation-delay:0.4s]" />
                  </span>
                ) : (
                  <>
                    {mode === 'login' ? 'লগইন করুন' : mode === 'register' ? 'নিবন্ধন করুন' : 'রিসেট কোড পাঠান'}
                    <ArrowRight className="w-5 h-5" />
                  </>
                )}
              </button>
            </form>

            <div className="flex flex-col gap-3 text-center pt-2">
              <button 
                onClick={() => {
                  setMode(mode === 'login' ? 'register' : 'login');
                  setStatusMessage(null);
                  clearError();
                }}
                className="text-sm font-bold text-gray-500 hover:text-red-600 transition-colors py-2"
              >
                {mode === 'login' ? 'নতুন একাউন্ট খুলতে চান? নিবন্ধন করুন' : mode === 'register' ? 'ইতিপূর্বে একাউন্ট খুলেছেন? লগইন করুন' : ''}
                {mode === 'forgot' && 'লগইন পেজে ফিরে যান'}
              </button>
            </div>
          </div>
        </motion.div>
      </div>
    </AnimatePresence>
  );
}
