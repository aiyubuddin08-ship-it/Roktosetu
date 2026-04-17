import React, { useState, useEffect } from 'react';
import { collection, query, orderBy, onSnapshot, doc, updateDoc, deleteDoc, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { UserProfile, BloodRequest, OperationType } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Shield, Users, FileText, Trash2, ShieldAlert, CheckCircle, XCircle, Search, Settings } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { sendNotification } from '../lib/notifications';

export function AdminDashboard() {
  const { profile, user } = useAuth();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [requests, setRequests] = useState<BloodRequest[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'users' | 'requests'>('users');
  const [searchTerm, setSearchTerm] = useState('');

  const isAdmin = profile?.role === 'admin' || user?.email === 'aiyubuddin08@gmail.com';

  useEffect(() => {
    if (!isAdmin) return;

    const unsubscribeUsers = onSnapshot(collection(db, 'users'), (snapshot) => {
      setUsers(snapshot.docs.map(doc => doc.data() as UserProfile));
    });

    const unsubscribeRequests = onSnapshot(query(collection(db, 'requests'), orderBy('createdAt', 'desc')), (snapshot) => {
      setRequests(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as BloodRequest)));
      setLoading(false);
    });

    return () => {
      unsubscribeUsers();
      unsubscribeRequests();
    };
  }, [profile]);

  if (!isAdmin) {
    return (
      <div className="flex flex-col items-center justify-center min-h-[60vh] text-center p-10 bg-white dark:bg-gray-900 rounded-[40px] border border-gray-100 dark:border-gray-800 transition-colors">
        <ShieldAlert className="w-20 h-20 text-red-100 dark:text-red-900 mb-6" />
        <h1 className="text-3xl font-black text-gray-900 dark:text-white mb-2">প্রবেশাধিকার নেই</h1>
        <p className="text-gray-500 dark:text-gray-400 font-bold">এই পেজটি শুধুমাত্র অ্যাডমিনদের জন্য।</p>
      </div>
    );
  }

  const handleUpdateRole = async (userId: string, newRole: 'user' | 'admin') => {
    if (!profile) return;
    if (userId === profile.uid) {
      alert('আপনি নিজের রোল পরিবর্তন করতে পারবেন না।');
      return;
    }
    try {
      await updateDoc(doc(db, 'users', userId), { role: newRole });
      
      await sendNotification({
        userId,
        title: 'আপনার রোল পরিবর্তন হয়েছে',
        message: `অ্যাডমিন আপনার অ্যাকাউন্ট রোল পরিবর্তন করে '${newRole === 'admin' ? 'অ্যাডমিন' : 'ইউজার'}' করেছেন।`,
        type: 'info',
        isRead: false,
        link: '/profile'
      });
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users/${userId}`);
    }
  };

  const handleDeleteRequest = async (requestId: string) => {
    if (!confirm('আপনি কি নিশ্চিত যে আপনি এই অনুরোধটি মুছে ফেলতে চান?')) return;
    try {
      await deleteDoc(doc(db, 'requests', requestId));
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `requests/${requestId}`);
    }
  };

  const filteredUsers = users.filter(u => 
    u.displayName.toLowerCase().includes(searchTerm.toLowerCase()) || 
    u.email.toLowerCase().includes(searchTerm.toLowerCase()) ||
    u.bloodGroup.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const filteredRequests = requests.filter(r => 
    r.hospitalName.toLowerCase().includes(searchTerm.toLowerCase()) || 
    r.bloodGroup.toLowerCase().includes(searchTerm.toLowerCase()) ||
    r.patientName?.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-8 animate-fade-in relative z-0">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div>
          <h1 className="text-4xl font-black text-gray-900 dark:text-white flex items-center gap-3">
            <Shield className="w-10 h-10 text-red-600" /> অ্যাডমিন প্যানেল
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-2 text-lg">পুরো সিস্টেম ম্যানেজ করুন এখান থেকে</p>
        </div>
        
        <div className="flex bg-white dark:bg-gray-900 p-2 rounded-3xl shadow-lg border border-gray-100 dark:border-gray-800 transition-colors">
          <button
            onClick={() => setActiveTab('users')}
            className={cn(
              "px-8 py-3 rounded-2xl font-black transition-all flex items-center gap-2",
              activeTab === 'users' ? "bg-red-600 text-white shadow-xl shadow-red-200" : "text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            )}
          >
            <Users className="w-5 h-5" /> ইউজার ম্যানেজমেন্ট
          </button>
          <button
            onClick={() => setActiveTab('requests')}
            className={cn(
              "px-8 py-3 rounded-2xl font-black transition-all flex items-center gap-2",
              activeTab === 'requests' ? "bg-red-600 text-white shadow-xl shadow-red-200" : "text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
            )}
          >
            <FileText className="w-5 h-5" /> রিকোয়েস্ট ম্যানেজমেন্ট
          </button>
        </div>
      </div>

      <div className="relative">
        <Search className="absolute left-6 top-1/2 -translate-y-1/2 text-gray-400 w-6 h-6" />
        <input
          type="text"
          placeholder="সার্চ করুন..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-16 pr-8 py-5 bg-white dark:bg-gray-900 rounded-[32px] border-none shadow-xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300 transition-colors"
        />
      </div>

      <div className="bg-white dark:bg-gray-900 rounded-[40px] shadow-2xl overflow-hidden border border-gray-100 dark:border-gray-800 transition-colors">
        <div className="overflow-x-auto">
          {activeTab === 'users' ? (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 dark:bg-gray-800 border-b border-gray-100 dark:border-gray-800 transition-colors">
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest">নাম ও ইমেইল</th>
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest text-center">গ্রুপ</th>
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest text-center">রোল</th>
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest text-center">একশন</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50 dark:divide-gray-800 transition-colors">
                {filteredUsers.map(u => (
                  <tr key={u.uid} className="hover:bg-gray-50/50 dark:hover:bg-gray-800 transition-colors">
                    <td className="px-8 py-6">
                      <div className="flex items-center gap-4">
                        <div className="w-12 h-12 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center font-black text-gray-400 dark:text-gray-500 overflow-hidden shadow-inner">
                          {u.photoURL ? (
                            <img src={u.photoURL} alt={u.displayName} className="w-full h-full object-cover" />
                          ) : (
                            u.displayName[0]
                          )}
                        </div>
                        <div>
                          <p className="font-black text-gray-900 dark:text-white">{u.displayName}</p>
                          <p className="text-xs text-gray-400 dark:text-gray-500">{u.email}</p>
                        </div>
                      </div>
                    </td>
                    <td className="px-8 py-6 text-center">
                      <span className="px-4 py-2 bg-red-50 dark:bg-red-900/10 text-red-600 dark:text-red-500 rounded-xl font-black text-sm">
                        {u.bloodGroup}
                      </span>
                    </td>
                    <td className="px-8 py-6 text-center">
                      <span className={cn(
                        "px-4 py-2 rounded-xl font-black text-[10px] uppercase tracking-widest",
                        u.role === 'admin' ? "bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400" : "bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400"
                      )}>
                        {u.role === 'admin' ? 'অ্যাডমিন' : 'মেম্বার'}
                      </span>
                    </td>
                    <td className="px-8 py-6 text-center">
                      <select
                        value={u.role}
                        onChange={(e) => handleUpdateRole(u.uid, e.target.value as 'user' | 'admin')}
                        disabled={u.uid === profile?.uid}
                        className="bg-gray-50 dark:bg-gray-800 border-none rounded-xl px-4 py-2 font-bold text-xs outline-none focus:ring-2 focus:ring-red-500 text-gray-700 dark:text-gray-300 disabled:opacity-50"
                      >
                        <option value="user">মেম্বার বানান</option>
                        <option value="admin">অ্যাডমিন বানান</option>
                      </select>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          ) : (
            <table className="w-full text-left border-collapse">
              <thead>
                <tr className="bg-gray-50 dark:bg-gray-800 border-b border-gray-100 dark:border-gray-800 transition-colors">
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest">হাসপাতাল ও রোগী</th>
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest text-center">গ্রুপ</th>
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest text-center">অবস্থা</th>
                  <th className="px-8 py-6 text-[10px] font-black uppercase text-gray-400 dark:text-gray-500 tracking-widest text-center">একশন</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50 dark:divide-gray-800 transition-colors">
                {filteredRequests.map(r => (
                  <tr key={r.id} className="hover:bg-gray-50/50 dark:hover:bg-gray-800 transition-colors">
                    <td className="px-8 py-6">
                      <p className="font-black text-gray-900 dark:text-white">{r.hospitalName}</p>
                      <p className="text-xs text-gray-400 dark:text-gray-500">রোগী: {r.patientName || 'Anonymous'}</p>
                    </td>
                    <td className="px-8 py-6 text-center">
                      <span className="w-10 h-10 inline-flex items-center justify-center bg-red-600 text-white rounded-xl font-black transition-transform hover:scale-110">
                        {r.bloodGroup}
                      </span>
                    </td>
                    <td className="px-8 py-6 text-center">
                      <span className={cn(
                        "px-4 py-2 rounded-xl font-black text-[10px] uppercase tracking-widest transition-colors",
                        r.status === 'Active' ? "bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-500" : "bg-gray-100 dark:bg-gray-800 text-gray-500 dark:text-gray-400"
                      )}>
                        {r.status === 'Active' ? 'সক্রিয়' : 'সম্পন্ন'}
                      </span>
                    </td>
                    <td className="px-8 py-6 text-center">
                      <button
                        onClick={() => handleDeleteRequest(r.id)}
                        className="p-3 text-red-200 dark:text-red-900/40 hover:text-red-600 dark:hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/10 rounded-xl transition-all"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </div>
      </div>
    </div>
  );
}
