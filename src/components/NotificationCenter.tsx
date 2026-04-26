import React, { useState, useRef, useEffect } from 'react';
import { Bell, Check, Trash2, ExternalLink, Calendar, Info, CheckCircle, AlertTriangle, XCircle, Award, Heart, ShieldCheck } from 'lucide-react';
import { useNotifications } from '../contexts/NotificationContext';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { formatDistanceToNow } from 'date-fns';
import { bn } from 'date-fns/locale';
import { useNavigate } from 'react-router-dom';

export function NotificationCenter() {
  const { notifications, unreadCount, markAsRead, markAllAsRead } = useNotifications();
  const [isOpen, setIsOpen] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const navigate = useNavigate();

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const getIcon = (type: string) => {
    switch (type) {
      case 'Badge Earned': return <Award className="w-5 h-5 text-yellow-500" />;
      case 'New Request': return <Heart className="w-5 h-5 text-red-500" />;
      case 'Eligibility': return <ShieldCheck className="w-5 h-5 text-green-500" />;
      case 'success': return <CheckCircle className="w-5 h-5 text-green-500" />;
      case 'warning': return <AlertTriangle className="w-5 h-5 text-orange-500" />;
      case 'error': return <XCircle className="w-5 h-5 text-red-500" />;
      default: return <Info className="w-5 h-5 text-blue-500" />;
    }
  };

  const handleNotificationClick = (notification: any) => {
    markAsRead(notification.id);
    if (notification.link) {
      navigate(notification.link);
      setIsOpen(false);
    }
  };

  return (
    <div className="relative" ref={containerRef}>
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="relative p-2 text-gray-400 hover:text-red-600 transition-colors"
      >
        <Bell className={cn("w-6 h-6", isOpen && "scale-110")} />
        {unreadCount > 0 && (
          <span className="absolute top-1.5 right-1.5 w-5 h-5 bg-red-600 text-white text-[10px] font-black rounded-full flex items-center justify-center border-2 border-white dark:border-gray-900 shadow-lg animate-pulse">
            {unreadCount > 9 ? '9+' : unreadCount}
          </span>
        )}
      </button>

      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, scale: 0.95, y: 10 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.95, y: 10 }}
            className="absolute right-0 mt-3 w-80 sm:w-96 bg-white dark:bg-gray-900 rounded-[32px] shadow-2xl border border-gray-100 dark:border-gray-800 overflow-hidden z-[60]"
          >
            <div className="p-6 border-b border-gray-100 dark:border-gray-800 flex items-center justify-between bg-gray-50/50 dark:bg-gray-800/50">
              <h3 className="text-lg font-black text-gray-900 dark:text-white">নোটিফিকেশন</h3>
              {unreadCount > 0 && (
                <button
                  onClick={() => markAllAsRead()}
                  className="text-xs font-black text-red-600 hover:text-red-700 transition-colors uppercase tracking-widest"
                >
                  সবগুলো পঠিত
                </button>
              )}
            </div>

            <div className="max-h-[70vh] overflow-y-auto custom-scrollbar">
              {notifications.length > 0 ? (
                <div className="divide-y divide-gray-50 dark:divide-gray-800">
                  {notifications.slice(0, 5).map((notification) => (
                    <div
                      key={notification.id}
                      onClick={() => handleNotificationClick(notification)}
                      className={cn(
                        "p-5 flex gap-4 cursor-pointer transition-colors group",
                        notification.isRead ? "bg-white dark:bg-gray-900 hover:bg-gray-50 dark:hover:bg-gray-800" : "bg-red-50/30 dark:bg-red-900/10 hover:bg-red-50/50 dark:hover:bg-red-900/20"
                      )}
                    >
                      <div className="mt-1">
                        {getIcon(notification.type)}
                      </div>
                      <div className="flex-1 space-y-1">
                        <p className={cn("text-sm font-black", notification.isRead ? "text-gray-700 dark:text-gray-300" : "text-gray-900 dark:text-white")}>
                          {notification.title}
                        </p>
                        <p className="text-sm text-gray-500 dark:text-gray-400 font-bold leading-relaxed line-clamp-2">
                          {notification.message}
                        </p>
                        <p className="text-[10px] text-gray-400 dark:text-gray-500 font-bold uppercase tracking-wider flex items-center gap-1.5 min-w-0">
                           <Calendar className="w-3 h-3" />
                           {formatDistanceToNow(new Date(notification.createdAt), { addSuffix: true, locale: bn })}
                        </p>
                      </div>
                      {!notification.isRead && (
                        <div className="w-2 h-2 bg-red-600 rounded-full mt-2 flex-shrink-0" />
                      )}
                    </div>
                  ))}
                </div>
              ) : (
                <div className="py-20 text-center px-10">
                  <div className="w-16 h-16 bg-gray-50 dark:bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-4 grayscale opacity-30">
                    <Bell className="w-8 h-8 text-gray-400" />
                  </div>
                  <p className="text-gray-400 dark:text-gray-600 font-black">কোনো নোটিফিকেশন নেই</p>
                </div>
              )}
            </div>

            {notifications.length > 0 && (
               <button 
                onClick={() => {
                  navigate('/notifications');
                  setIsOpen(false);
                }}
                className="w-full py-4 bg-gray-50 dark:bg-gray-800/50 text-red-600 dark:text-red-400 font-black text-sm uppercase tracking-widest border-t border-gray-100 dark:border-gray-800 hover:bg-red-50 dark:hover:bg-red-900/10 transition-all"
               >
                 সবগুলো দেখুন
               </button>
            )}
          </motion.div>
        )}
      </AnimatePresence>
    </div>
  );
}
