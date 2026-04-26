import React, { createContext, useContext, useEffect, useState } from 'react';
import { 
  User, 
  onAuthStateChanged, 
  signInWithPopup, 
  signOut,
  signInWithEmailAndPassword,
  createUserWithEmailAndPassword,
  sendPasswordResetEmail,
  updateProfile
} from 'firebase/auth';
import { doc, getDoc, setDoc, onSnapshot } from 'firebase/firestore';
import { auth, db, googleProvider } from '../lib/firebase';
import { UserProfile } from '../types';

interface AuthContextType {
  user: User | null;
  profile: UserProfile | null;
  loading: boolean;
  loggingIn: boolean;
  login: () => Promise<void>;
  loginWithEmail: (email: string, password: string) => Promise<void>;
  registerWithEmail: (email: string, password: string, displayName: string) => Promise<void>;
  resetPassword: (email: string) => Promise<void>;
  logout: () => Promise<void>;
  isAuthReady: boolean;
  error: string | null;
  clearError: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null);
  const [profile, setProfile] = useState<UserProfile | null>(null);
  const [loading, setLoading] = useState(true);
  const [loggingIn, setLoggingIn] = useState(false);
  const [isAuthReady, setIsAuthReady] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const unsubscribeAuth = onAuthStateChanged(auth, async (user) => {
      setUser(user);
      setIsAuthReady(true);
      
      if (user) {
        // Sync profile
        const profileRef = doc(db, 'users', user.uid);
        const unsubscribeProfile = onSnapshot(profileRef, (doc) => {
          if (doc.exists()) {
            setProfile(doc.data() as UserProfile);
          } else {
            setProfile(null);
          }
          setLoading(false);
        }, (error) => {
          console.error("Profile sync error:", error);
          setLoading(false);
        });
        
        return () => unsubscribeProfile();
      } else {
        setProfile(null);
        setLoading(false);
      }
    });

    return () => unsubscribeAuth();
  }, []);

  const login = async () => {
    if (loggingIn) return;
    setLoggingIn(true);
    setError(null);
    try {
      await signInWithPopup(auth, googleProvider);
    } catch (err: any) {
      // Don't log as error if user just closed it, to keep console cleaner
      if (err.code === 'auth/popup-closed-by-user') {
        console.warn("Login popup closed by user");
        setError('লগইন উইন্ডোটি বন্ধ করা হয়েছে বা পপ-আপ ব্লক করা হয়েছে। অনুগ্রহ করে ব্রাউজারের পপ-আপ পারমিশন চেক করুন এবং পুনরায় চেষ্টা করুন।');
      } else if (err.code === 'auth/popup-blocked') {
        console.error("Popup blocked:", err);
        setError('ব্রাউজার পপ-আপ ইন্টারফেস ব্লক করেছে। অনুগ্রহ করে ব্রাউজার সেটিংসে পপ-আপ এলাউ করুন।');
      } else {
        console.error("Login failed:", err);
        setError('লগইন করতে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।');
      }
    } finally {
      setLoggingIn(false);
    }
  };

  const loginWithEmail = async (email: string, password: string) => {
    if (loggingIn) return;
    setLoggingIn(true);
    setError(null);
    try {
      await signInWithEmailAndPassword(auth, email, password);
    } catch (err: any) {
      console.error("Login with email failed:", err);
      const errorCode = err.code || (err.message && err.message.includes('auth/invalid-credential') ? 'auth/invalid-credential' : '');
      
      if (errorCode === 'auth/user-not-found' || errorCode === 'auth/wrong-password' || errorCode === 'auth/invalid-credential' || errorCode === 'auth/invalid-email') {
        setError('আপনার প্রদান করা ইমেইল বা পাসওয়ার্ডটি সঠিক নয়। অনুগ্রহ করে পুনরায় চেক করে চেষ্টা করুন।');
      } else if (errorCode === 'auth/too-many-requests') {
        setError('অতিরিক্ত ভুল প্রচেষ্টার কারণে এই একাউন্টটি সাময়িকভাবে বন্ধ করা হয়েছে। কিছুক্ষণ পর আবার চেষ্টা করুন।');
      } else if (errorCode === 'auth/network-request-failed') {
        setError('ইন্টারনেট সংযোগে সমস্যা হচ্ছে। আপনার কানেকশন চেক করুন।');
      } else {
        setError('লগইন করতে সমস্যা হয়েছে। আপনার তথ্যগুলো পুনরায় যাচাই করুন।');
      }
      throw err;
    } finally {
      setLoggingIn(false);
    }
  };

  const registerWithEmail = async (email: string, password: string, displayName: string) => {
    if (loggingIn) return;
    setLoggingIn(true);
    setError(null);
    try {
      const userCredential = await createUserWithEmailAndPassword(auth, email, password);
      await updateProfile(userCredential.user, { displayName });
    } catch (err: any) {
      console.error("Registration failed:", err);
      if (err.code === 'auth/email-already-in-use') {
        setError('এই ইমেইলটি ইতিপূর্বে ব্যবহৃত হয়েছে। অন্য একটি ইমেইল চেষ্টা করুন।');
      } else if (err.code === 'auth/weak-password') {
        setError('পাসওয়ার্ড অত্যন্ত দুর্বল। অন্তত ৬ অক্ষরের পাসওয়ার্ড ব্যবহার করুন।');
      } else {
        setError('নিবন্ধন করতে সমস্যা হয়েছে। অনুগ্রহ করে আবার চেষ্টা করুন।');
      }
      throw err;
    } finally {
      setLoggingIn(false);
    }
  };

  const resetPassword = async (email: string) => {
    setError(null);
    try {
      await sendPasswordResetEmail(auth, email);
    } catch (err: any) {
      console.error("Password reset failed:", err);
      if (err.code === 'auth/user-not-found') {
        setError('এই ইমেইল দিয়ে কোনো একাউন্ট পাওয়া যায়নি।');
      } else {
        setError('পাসওয়ার্ড রিসেট ইমেইল পাঠাতে সমস্যা হয়েছে।');
      }
      throw err;
    }
  };

  const clearError = () => setError(null);

  const logout = async () => {
    try {
      await signOut(auth);
      setProfile(null);
      setError(null);
    } catch (err) {
      console.error("Logout failed:", err);
    }
  };

  return (
    <AuthContext.Provider value={{ 
      user, 
      profile, 
      loading, 
      loggingIn, 
      login, 
      loginWithEmail,
      registerWithEmail,
      resetPassword,
      logout, 
      isAuthReady, 
      error, 
      clearError 
    }}>
      {children}
    </AuthContext.Provider>
  );
}

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
