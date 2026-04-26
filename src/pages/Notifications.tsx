import React, { useEffect, useState } from 'react';
import { collection, query, where, orderBy, onSnapshot, updateDoc, doc, deleteDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { Notification, OperationType } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Bell, Trash2, CheckCircle, Clock, ExternalLink, ShieldCheck, Heart, Award } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { bn } from 'date-fns/locale';
import { Link } from 'react-router-dom';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';

export function Notifications() {
  const { user } = useAuth();
  const [notifications, setNotifications] = useState<Notification[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    if (!user) return;

    const q = query(
      collection(db, 'notifications'),
      where('userId', '==', user.uid),
      orderBy('createdAt', 'desc')
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const data = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Notification));
      setNotifications(data);
      setLoading(false);
    }, (error) => {
      handleFirestoreError(error, OperationType.LIST, 'notifications');
      setLoading(false);
    });

    return () => unsubscribe();
  }, [user]);

  const markAsRead = async (id: string) => {
    try {
      await updateDoc(doc(db, 'notifications', id), { isRead: true });
    } catch (error) {
      console.error(error);
    }
  };

  const deleteNotification = async (id: string) => {
    try {
      await deleteDoc(doc(db, 'notifications', id));
    } catch (error) {
      console.error(error);
    }
  };

  const markAllRead = async () => {
    const unread = notifications.filter(n => !n.isRead);
    for (const n of unread) {
      updateDoc(doc(db, 'notifications', n.id), { isRead: true });
    }
  };

  const getIcon = (type: Notification['type']) => {
    switch (type) {
      case 'Badge Earned': return <Award className="w-6 h-6 text-yellow-500" />;
      case 'New Request': return <Heart className="w-6 h-6 text-red-500" />;
      case 'Eligibility': return <ShieldCheck className="w-6 h-6 text-green-500" />;
      default: return <Bell className="w-6 h-6 text-blue-500" />;
    }
  };

  return (
    <div className="max-w-3xl mx-auto space-y-8 pb-20 animate-fade-in">
      <div className="flex flex-col sm:flex-row items-center justify-between gap-4">
        <div className="flex items-center gap-4">
          <div className="w-16 h-16 bg-red-50 dark:bg-red-900/10 rounded-[28px] flex items-center justify-center text-red-600 shadow-inner">
             <Bell className="w-8 h-8" />
          </div>
          <div>
            <h1 className="text-3xl font-black text-gray-900 dark:text-white leading-none">নোটিফিকেশন</h1>
            <p className="text-gray-500 dark:text-gray-400 font-bold mt-2">আপনার গুরুত্বপূর্ণ আপডেটগুলো এখানে পাবেন</p>
          </div>
        </div>
        {notifications.some(n => !n.isRead) && (
          <button 
            onClick={markAllRead}
            className="px-6 py-3 bg-red-50 dark:bg-red-900/10 text-red-600 dark:text-red-400 rounded-2xl font-black text-sm uppercase tracking-widest hover:bg-red-100 transition-all flex items-center gap-2"
          >
            <CheckCircle className="w-5 h-5" /> সবগুলো পঠিত হিসেবে চিহ্নিত করুন
          </button>
        )}
      </div>

      {loading ? (
        <div className="space-y-4">
          {[1, 2, 3].map(i => <div key={i} className="h-24 bg-white dark:bg-gray-900 rounded-[40px] shimmer border border-gray-100 dark:border-gray-800" />)}
        </div>
      ) : notifications.length === 0 ? (
        <div className="bg-white dark:bg-gray-900 p-20 rounded-[40px] text-center border-2 border-dashed border-gray-100 dark:border-gray-800 space-y-6">
           <Bell className="w-20 h-20 text-gray-200 dark:text-gray-800 mx-auto" />
           <p className="text-gray-400 font-bold text-xl">এখন পর্যন্ত কোনো নোটিফিকেশন নেই</p>
        </div>
      ) : (
        <div className="grid gap-4">
          <AnimatePresence>
            {notifications.map((notif) => (
              <motion.div
                key={notif.id}
                layout
                initial={{ opacity: 0, x: -20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, scale: 0.95 }}
                className={cn(
                  "bg-white dark:bg-gray-900 p-6 rounded-[40px] border transition-all flex items-start gap-6 group relative",
                  notif.isRead 
                    ? "border-gray-100 dark:border-gray-800 opacity-60" 
                    : "border-red-100 dark:border-red-900/20 shadow-xl shadow-red-500/5 bg-red-50/10 dark:bg-red-900/5"
                )}
              >
                <div className={cn(
                  "w-14 h-14 rounded-2xl flex items-center justify-center shrink-0 shadow-inner",
                  notif.isRead ? "bg-gray-50 dark:bg-gray-800" : "bg-red-50 dark:bg-red-900/10"
                )}>
                  {getIcon(notif.type)}
                </div>
                <div className="flex-1 space-y-2">
                  <div className="flex items-center justify-between gap-4">
                    <h3 className={cn("text-lg font-black leading-tight", notif.isRead ? "text-gray-700 dark:text-gray-300" : "text-gray-900 dark:text-white")}>
                      {notif.title}
                    </h3>
                    {!notif.isRead && <div className="w-2.5 h-2.5 bg-red-600 rounded-full animate-pulse shrink-0" />}
                  </div>
                  <p className="text-gray-500 dark:text-gray-400 font-bold leading-relaxed">{notif.message}</p>
                  
                  <div className="flex flex-wrap items-center gap-6 pt-2">
                    <div className="flex items-center gap-2 text-gray-400 text-xs font-bold">
                       <Clock className="w-4 h-4" /> {formatDistanceToNow(new Date(notif.createdAt), { locale: bn, addSuffix: true })}
                    </div>
                    
                    {notif.link && (
                      <Link to={notif.link} className="text-xs font-black text-red-600 dark:text-red-500 flex items-center gap-1.5 hover:underline">
                        {notif.linkText || 'বিস্তারিত দেখুন'} <ExternalLink className="w-3.5 h-3.5" />
                      </Link>
                    )}
                  </div>
                </div>

                <div className="flex items-center gap-2 ml-4">
                  {!notif.isRead && (
                    <button 
                      onClick={() => markAsRead(notif.id)}
                      className="p-3 bg-white dark:bg-gray-800 text-gray-400 hover:text-green-600 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-800 transition-all"
                      title="পঠিত হিসেবে চিহ্নিত করুন"
                    >
                      <CheckCircle className="w-5 h-5" />
                    </button>
                  )}
                  <button 
                    onClick={() => deleteNotification(notif.id)}
                    className="p-3 bg-white dark:bg-gray-800 text-gray-400 hover:text-red-600 rounded-2xl shadow-sm border border-gray-100 dark:border-gray-800 transition-all"
                    title="মুছে ফেলুন"
                  >
                    <Trash2 className="w-5 h-5" />
                  </button>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      )}
    </div>
  );
}
