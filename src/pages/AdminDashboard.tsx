import React, { useState, useEffect } from 'react';
import { collection, query, orderBy, onSnapshot, doc, updateDoc, deleteDoc, getDocs, addDoc, getDoc, increment, setDoc, where, Timestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { UserProfile, BloodRequest, OperationType, DonationCertificate, DonationEvent, EmergencyResource, Organization } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Shield, Users, FileText, Trash2, ShieldAlert, CheckCircle, XCircle, Search, Settings, MessageSquare, Save, Heart, BarChart3, Calendar, Activity, Megaphone, Plus, MapPin, Phone, Building2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { sendNotification } from '../lib/notifications';
import { sendSMS } from '../services/smsService';

type AdminTab = 'analytics' | 'users' | 'requests' | 'campaigns' | 'resources' | 'organizations' | 'broadcast';

export function AdminDashboard() {
  const { profile, user } = useAuth();
  const [users, setUsers] = useState<UserProfile[]>([]);
  const [requests, setRequests] = useState<BloodRequest[]>([]);
  const [events, setEvents] = useState<DonationEvent[]>([]);
  const [resources, setResources] = useState<EmergencyResource[]>([]);
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<AdminTab>('analytics');
  const [searchTerm, setSearchTerm] = useState('');
  const [editingNotesId, setEditingNotesId] = useState<string | null>(null);
  const [noteContent, setNoteContent] = useState('');

  // Broadcast state
  const [broadcastMessage, setBroadcastMessage] = useState('');
  const [isBroadcasting, setIsBroadcasting] = useState(false);
  const [broadcastType, setBroadcastType] = useState<'app' | 'sms'>('app');
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);

  // Item Creation States
  const [showAddEvent, setShowAddEvent] = useState(false);
  const [newEvent, setNewEvent] = useState({ title: '', orgName: '', description: '', location: '', date: '', contactNumber: '' });
  
  const [showAddResource, setShowAddResource] = useState(false);
  const [newResource, setNewResource] = useState({ name: '', type: 'Blood Bank' as 'Blood Bank' | 'Ambulance', location: '', contactNumber: '', available24h: true });

  const handleAddEvent = async () => {
    try {
      await addDoc(collection(db, 'events'), { ...newEvent, createdAt: new Date().toISOString() });
      setShowAddEvent(false);
      setNewEvent({ title: '', orgName: '', description: '', location: '', date: '', contactNumber: '' });
      alert('ক্যাম্পেইন যোগ করা হয়েছে!');
    } catch (e) { alert('Error adding event'); }
  };

  const handleAddResource = async () => {
    try {
      await addDoc(collection(db, 'resources'), { ...newResource, createdAt: new Date().toISOString() });
      setShowAddResource(false);
      setNewResource({ name: '', type: 'Blood Bank', location: '', contactNumber: '', available24h: true });
      alert('রিসোর্স যোগ করা হয়েছে!');
    } catch (e) { alert('Error adding resource'); }
  };

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

    const unsubscribeEvents = onSnapshot(query(collection(db, 'events'), orderBy('date', 'desc')), (snapshot) => {
      setEvents(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as DonationEvent)));
    });

    const unsubscribeResources = onSnapshot(collection(db, 'resources'), (snapshot) => {
      setResources(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as EmergencyResource)));
    });

    const unsubscribeOrgs = onSnapshot(collection(db, 'organizations'), (snapshot) => {
      setOrganizations(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Organization)));
    });

    return () => {
      unsubscribeUsers();
      unsubscribeRequests();
      unsubscribeEvents();
      unsubscribeResources();
      unsubscribeOrgs();
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

  // --- Actions ---
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
        type: 'System',
        isRead: false,
        link: '/profile'
      });
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users/${userId}`);
    }
  };

  const handleDeleteItem = async (col: string, id: string) => {
    setDeletingId(id);
    try {
      await deleteDoc(doc(db, col, id));
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `${col}/${id}`);
    } finally {
      setDeletingId(null);
      setConfirmDeleteId(null);
    }
  };

  const handleSupportClick = () => {
    window.open('https://wa.me/8801897971573', '_blank');
  };

  const handleHelpDeskClick = () => {
    // Using a more visible way to show the message
    const email = 'aiyubuddinferdaus@gmail.com';
    const subject = 'Emergency Help Desk Request';
    window.location.href = `mailto:${email}?subject=${encodeURIComponent(subject)}`;
  };

  const handleBroadcast = async () => {
    if (!broadcastMessage.trim()) return;
    if (!confirm('আপনি কি নিশ্চিত যে আপনি সব ইউজারকে এই মেসেজটি পাঠাতে চান?')) return;

    setIsBroadcasting(true);
    try {
      const userDocs = await getDocs(collection(db, 'users'));
      
      if (broadcastType === 'app') {
        const batch = userDocs.docs.map(u => sendNotification({
          userId: u.id,
          title: 'সিস্টেম ব্রডকাস্ট 📢',
          message: broadcastMessage,
          type: 'System',
          isRead: false,
          link: '/'
        }));
        await Promise.all(batch);
      } else {
        const donorPhones = userDocs.docs
          .map(u => (u.data() as UserProfile).phoneNumber)
          .filter((p): p is string => !!p && p.length >= 11);
          
        const smsBatch = donorPhones.map(phone => sendSMS({ to: phone, message: broadcastMessage }));
        await Promise.all(smsBatch);
      }

      setBroadcastMessage('');
      alert('সফলভাবে সব ইউজারকে জানানো হয়েছে!');
    } catch (error) {
      console.error(error);
      alert('ব্রডকাস্ট করতে সমস্যা হয়েছে।');
    } finally {
      setIsBroadcasting(false);
    }
  };

  const handleVerifyDonation = async (request: BloodRequest) => {
    if (!request.donorId) {
      alert('এই অনুরোধের জন্য কোনো ডোনার নিযুক্ত নেই।');
      return;
    }
    if (!confirm('আপনি কি নিশ্চিত যে আপনি এই ডোনেশনটি সফল হিসেবে ভেরিফাই করতে চান?')) return;

    try {
      await updateDoc(doc(db, 'requests', request.id), { status: 'Fulfilled', completedAt: new Date().toISOString() });
      const donorRef = doc(db, 'users', request.donorId);
      const donorDoc = await getDoc(donorRef);
      const donorData = donorDoc.data() as UserProfile;
      const newDonationCount = (donorData.donationsCount || 0) + 1;
      await updateDoc(donorRef, { donationsCount: increment(1), points: increment(50), lastDonated: new Date().toISOString() });

      const certId = `CERT-${Date.now()}-${request.donorId.substring(0, 5)}`;
      await setDoc(doc(db, 'certificates', certId), {
        id: certId, userId: request.donorId, donorName: donorData.displayName, bloodGroup: donorData.bloodGroup,
        donationDate: new Date().toISOString(), donationCount: newDonationCount, issuedAt: new Date().toISOString(),
        certificateNo: certId.replace('CERT-', 'RS-')
      });

      await sendNotification({
        userId: request.donorId, title: 'ডোনেশন ভেরিফাই করা হয়েছে', message: `আপনার রক্তদান সফলভাবে ভেরিফাই করা হয়েছে। আপনি ৫০ পয়েন্ট পেয়েছেন।`,
        type: 'Badge Earned', isRead: false, link: '/certificates'
      });
      alert('ভেরিফাই করা হয়েছে!');
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `requests/${request.id}`);
    }
  };

  const handleVerifyOrg = async (orgId: string, status: boolean) => {
    try {
      await updateDoc(doc(db, 'organizations', orgId), { isVerified: status });
      const org = organizations.find(o => o.id === orgId);
      if (org) {
        await sendNotification({
          userId: org.adminUid,
          title: status ? 'সংগঠন ভেরিফাইড! ✅' : 'সংগঠন আনভেরিফাইড',
          message: status ? `আপনার সংগঠন '${org.name}' সফলভাবে ভেরিফাই করা হয়েছে।` : `আপনার সংগঠনের ভেরিফিকেশন তুলে নেয়া হয়েছে।`,
          type: 'System',
          isRead: false,
          link: '/organizations'
        });
      }
      alert(status ? 'ভেরিফাই করা হয়েছে!' : 'আনভেরিফাই করা হয়েছে!');
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `organizations/${orgId}`);
    }
  };

  // --- Filtering ---
  const filteredUsers = users.filter(u => u.displayName.toLowerCase().includes(searchTerm.toLowerCase()) || (u.bloodGroup && u.bloodGroup.includes(searchTerm)));
  const filteredRequests = requests.filter(r => r.hospitalName.toLowerCase().includes(searchTerm.toLowerCase()) || r.bloodGroup.includes(searchTerm));

  // --- Analytics Logic ---
  const bloodGroupsCount = users.reduce((acc, u) => {
    if (u.bloodGroup) {
      acc[u.bloodGroup] = (acc[u.bloodGroup] || 0) + 1;
    }
    return acc;
  }, {} as Record<string, number>);

  return (
    <div className="space-y-8 animate-fade-in pb-20">
      {/* Header */}
      <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-xl border border-gray-100 dark:border-gray-800 flex flex-col lg:flex-row lg:items-center justify-between gap-8">
        <div>
          <h1 className="text-4xl font-black text-gray-900 dark:text-white flex items-center gap-3 transition-colors">
             <Shield className="w-10 h-10 text-red-600" /> অ্যাডমিন কমান্ড সেন্টার
          </h1>
          <p className="text-gray-500 dark:text-gray-400 mt-2 text-lg">পুরো প্ল্যাটফর্মের পূর্ণ নিয়ন্ত্রণ</p>
        </div>
        
        {/* Navigation Tabs */}
        <div className="grid grid-cols-3 md:flex bg-white dark:bg-gray-900 p-2 rounded-[32px] shadow-xl border border-gray-100 dark:border-gray-800 shrink-0 transition-colors">
          {(['analytics', 'users', 'requests', 'campaigns', 'resources', 'organizations', 'broadcast'] as AdminTab[]).map((tab) => (
            <button
              key={tab}
              onClick={() => setActiveTab(tab)}
              className={cn(
                "px-4 py-3 rounded-2xl font-black text-[10px] uppercase tracking-widest transition-all flex flex-col md:flex-row items-center gap-2",
                activeTab === tab ? "bg-red-600 text-white shadow-xl shadow-red-200" : "text-gray-400 hover:text-gray-600 dark:hover:text-gray-300"
              )}
            >
              {tab === 'analytics' && <BarChart3 className="w-4 h-4" />}
              {tab === 'users' && <Users className="w-4 h-4" />}
              {tab === 'requests' && <FileText className="w-4 h-4" />}
              {tab === 'campaigns' && <Calendar className="w-4 h-4" />}
              {tab === 'resources' && <Activity className="w-4 h-4" />}
              {tab === 'organizations' && <Building2 className="w-4 h-4" />}
              {tab === 'broadcast' && <Megaphone className="w-4 h-4" />}
              <span className="hidden md:inline">
                {tab === 'analytics' ? 'অ্যানালিটিক্স' : 
                 tab === 'users' ? 'ইউজার' : 
                 tab === 'requests' ? 'রিকোয়েস্ট' : 
                 tab === 'campaigns' ? 'ক্যাম্পেইন' : 
                 tab === 'resources' ? 'রিসোর্স' : 
                 tab === 'organizations' ? 'সংগঠন' :
                 'ব্রডকাস্ট'}
              </span>
            </button>
          ))}
        </div>
      </div>

      <AnimatePresence mode="wait">
        {activeTab === 'analytics' && (
          <motion.div key="analytics" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }} className="space-y-8">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
               {[
                 { label: 'মোট ইউজার', value: users.length, icon: Users, color: 'text-blue-600', bg: 'bg-blue-50' },
                 { label: 'মোট রক্তদান', value: users.reduce((sum, u) => sum + (u.donationsCount || 0), 0), icon: Heart, color: 'text-red-600', bg: 'bg-red-50' },
                 { label: 'সক্রিয় রিকোয়েস্ট', value: requests.filter(r => r.status === 'Active').length, icon: Activity, color: 'text-orange-600', bg: 'bg-orange-50' },
                 { label: 'মোট পয়েন্ট', value: users.reduce((sum, u) => sum + (u.points || 0), 0), icon: Megaphone, color: 'text-purple-600', bg: 'bg-purple-50' },
               ].map((stat, i) => (
                 <div key={i} className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-xl border border-gray-100 dark:border-gray-800 transition-colors">
                    <div className={cn("w-12 h-12 rounded-2xl flex items-center justify-center mb-6", stat.bg)}>
                       <stat.icon className={cn("w-6 h-6", stat.color)} />
                    </div>
                    <p className="text-[10px] font-black uppercase text-gray-400 tracking-widest">{stat.label}</p>
                    <p className="text-4xl font-black text-gray-900 dark:text-white mt-2">{stat.value}</p>
                 </div>
               ))}
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-[50px] shadow-xl border border-gray-100 dark:border-gray-800 transition-colors">
               <h3 className="text-xl font-black mb-8 px-4 text-gray-900 dark:text-white">গ্রুপ ভিত্তিক রক্তদাতা</h3>
               <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-8 gap-4 text-center">
                  {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map(group => (
                    <div key={group} className="space-y-3 p-6 bg-gray-50 dark:bg-gray-800/50 rounded-[32px] border border-transparent hover:border-red-100 dark:hover:border-red-900 transition-all group">
                       <p className="text-lg font-black text-red-600 group-hover:scale-110 transition-transform">{group}</p>
                       <p className="text-3xl font-black text-gray-900 dark:text-white">{bloodGroupsCount[group] || 0}</p>
                       <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">ডোনার</p>
                    </div>
                  ))}
               </div>
            </div>
          </motion.div>
        )}

        {activeTab === 'organizations' && (
          <motion.div key="organizations" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
            <div className="bg-white dark:bg-gray-900 rounded-[40px] shadow-2xl overflow-hidden border border-gray-100 dark:border-gray-800 transition-colors">
              <div className="overflow-x-auto">
                <table className="w-full text-left">
                  <thead>
                    <tr className="bg-gray-50 dark:bg-gray-800 text-[10px] font-black uppercase text-gray-400 tracking-widest">
                      <th className="px-8 py-6">সংগঠন</th>
                      <th className="px-8 py-6">লোকেশন ও এলাকা</th>
                      <th className="px-8 py-6">যোগাযোগ</th>
                      <th className="px-8 py-6 text-center">স্ট্যাটাস</th>
                      <th className="px-8 py-6 text-center">একশন</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-gray-50 dark:divide-gray-800">
                    {organizations.map(org => (
                      <tr key={org.id} className="hover:bg-gray-50/30 transition-colors">
                        <td className="px-8 py-6">
                          <div>
                            <p className="font-black text-gray-900 dark:text-white uppercase">{org.name}</p>
                            <p className="text-[10px] text-gray-400 font-bold bg-gray-100 dark:bg-gray-800 inline-block px-2 py-0.5 rounded-md mt-1">{org.type}</p>
                          </div>
                        </td>
                        <td className="px-8 py-6">
                           <div className="space-y-1">
                             <p className="text-xs font-bold text-gray-600 dark:text-gray-300">📍 {org.location.district}</p>
                             <p className="text-[10px] text-gray-400">এলাকা: {org.coverageArea || 'N/A'}</p>
                           </div>
                        </td>
                        <td className="px-8 py-6">
                           <p className="text-xs font-black text-gray-700 dark:text-gray-300">📞 {org.phoneNumber}</p>
                           <p className="text-[10px] text-gray-400">{org.email}</p>
                        </td>
                        <td className="px-8 py-6 text-center">
                          {org.isVerified ? (
                            <span className="px-3 py-1 bg-green-100 text-green-600 rounded-full text-[10px] font-black uppercase">ভেরিফাইড</span>
                          ) : (
                            <span className="px-3 py-1 bg-yellow-100 text-yellow-600 rounded-full text-[10px] font-black uppercase">পেন্ডিং</span>
                          )}
                        </td>
                        <td className="px-8 py-6 text-center">
                           <div className="flex items-center justify-center gap-2">
                             <button 
                               onClick={() => handleVerifyOrg(org.id, !org.isVerified)} 
                               className={cn(
                                 "p-2 rounded-xl transition-all",
                                 org.isVerified ? "text-red-400 hover:bg-red-50" : "text-green-600 hover:bg-green-50"
                               )}
                               title={org.isVerified ? "আনভেরিফাই করুন" : "ভেরিফাই করুন"}
                             >
                               {org.isVerified ? <XCircle className="w-5 h-5" /> : <CheckCircle className="w-5 h-5" />}
                             </button>
                             
                             {confirmDeleteId === org.id ? (
                               <div className="flex items-center gap-1">
                                 <button onClick={() => handleDeleteItem('organizations', org.id)} className="p-2 text-red-600 bg-red-50 rounded-lg text-xs font-bold shrink-0">হ্যাঁ</button>
                                 <button onClick={() => setConfirmDeleteId(null)} className="p-2 text-gray-400 bg-gray-50 rounded-lg text-xs font-bold shrink-0">না</button>
                               </div>
                             ) : (
                               <button 
                                 onClick={() => setConfirmDeleteId(org.id)} 
                                 disabled={deletingId === org.id}
                                 className="p-2 rounded-xl text-gray-400 hover:text-red-600 hover:bg-red-50 transition-all"
                               >
                                 <Trash2 className={cn("w-5 h-5", deletingId === org.id && "animate-pulse")} />
                               </button>
                             )}
                           </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          </motion.div>
        )}

        {activeTab === 'broadcast' && (
          <motion.div key="broadcast" initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -20 }} className="max-w-2xl mx-auto">
            <div className="bg-white dark:bg-gray-900 p-12 rounded-[50px] shadow-xl border border-gray-100 dark:border-gray-800 space-y-8 transition-colors">
               <div className="text-center space-y-4">
                  <div className="w-20 h-20 bg-purple-50 dark:bg-purple-900/10 text-purple-600 rounded-[32px] flex items-center justify-center mx-auto shadow-inner">
                     <Megaphone className="w-10 h-10" />
                  </div>
                  <h2 className="text-3xl font-black italic text-gray-900 dark:text-white">সিস্টেম ব্রডকাস্ট</h2>
                  <p className="text-gray-500 font-bold">এক ক্লিকেই সব ইউজারকে জরুরি মেসেজ বা ঘোষণা জানান।</p>
               </div>

               <div className="flex justify-center gap-4">
                  {(['app', 'sms'] as const).map((type) => (
                    <button
                      key={type}
                      onClick={() => setBroadcastType(type)}
                      className={cn(
                        "px-8 py-3 rounded-2xl font-black text-xs uppercase tracking-widest transition-all",
                        broadcastType === type ? "bg-gray-900 text-white shadow-xl" : "bg-gray-100 text-gray-400 hover:text-gray-500"
                      )}
                    >
                      {type === 'app' ? 'In-App Message' : 'Direct SMS'}
                    </button>
                  ))}
               </div>

               <div className="space-y-4">
                  <textarea
                    value={broadcastMessage}
                    onChange={(e) => setBroadcastMessage(e.target.value)}
                    placeholder="আপনার মেসেজ এখানে লিখুন..."
                    className="w-full bg-gray-50 dark:bg-gray-800/50 border-none rounded-[32px] p-8 text-lg font-bold min-h-[200px] outline-none focus:ring-4 focus:ring-purple-500/20 text-gray-700 dark:text-gray-300"
                  />
                  <button
                    onClick={handleBroadcast}
                    disabled={isBroadcasting || !broadcastMessage.trim()}
                    className="w-full py-6 bg-purple-600 text-white rounded-[32px] font-black text-xl shadow-2xl hover:scale-[1.02] active:scale-95 transition-all disabled:opacity-50"
                  >
                    {isBroadcasting ? 'পাঠানো হচ্ছে...' : 'সবাইকে পাঠান'}
                  </button>
               </div>
            </div>
          </motion.div>
        )}

        {(activeTab === 'users' || activeTab === 'requests') && (
           <motion.div key={activeTab} initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
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
                      <table className="w-full text-left">
                        <thead>
                          <tr className="bg-gray-50 dark:bg-gray-800 text-[10px] font-black uppercase text-gray-400 tracking-widest shrink-0">
                            <th className="px-8 py-6">প্রোফাইল</th>
                            <th className="px-8 py-6 text-center">রক্তের গ্রুপ</th>
                            <th className="px-8 py-6 text-center">পয়েন্ট</th>
                            <th className="px-8 py-6 text-center">একশন</th>
                          </tr>
                        </thead>
                        <tbody className="divide-y divide-gray-50 dark:divide-gray-800">
                          {filteredUsers.map(u => (
                            <tr key={u.uid} className="hover:bg-gray-50/30 transition-colors">
                              <td className="px-8 py-6">
                                <div className="flex items-center gap-4">
                                  <div className="w-12 h-12 rounded-2xl bg-gray-100 dark:bg-gray-800 flex items-center justify-center font-black">
                                    {u.photoURL ? <img src={u.photoURL} className="w-full h-full object-cover" /> : u.displayName[0]}
                                  </div>
                                  <div>
                                    <p className="font-black text-gray-900 dark:text-white">{u.displayName}</p>
                                    <p className="text-xs font-bold text-gray-400">{u.email}</p>
                                  </div>
                                </div>
                              </td>
                              <td className="px-8 py-6 text-center">
                                <span className="px-4 py-2 bg-red-50 dark:bg-red-900/10 text-red-600 rounded-xl font-black">{u.bloodGroup}</span>
                              </td>
                              <td className="px-8 py-6 text-center font-black text-gray-900 dark:text-white">{u.points || 0}</td>
                              <td className="px-8 py-6 text-center">
                                <select
                                  value={u.role}
                                  onChange={(e) => handleUpdateRole(u.uid, e.target.value as 'user' | 'admin')}
                                  className="bg-transparent border-none font-bold text-xs outline-none text-gray-900 dark:text-white cursor-pointer"
                                >
                                  <option value="user">ইউজার</option>
                                  <option value="admin">অ্যাডমিন</option>
                                </select>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    ) : (
                      <table className="w-full text-left">
                         <thead>
                           <tr className="bg-gray-50 dark:bg-gray-800 text-[10px] font-black uppercase text-gray-400 tracking-widest shrink-0">
                             <th className="px-8 py-6">হাসপাতাল</th>
                             <th className="px-8 py-6 text-center">গ্রুপ</th>
                             <th className="px-8 py-6 text-center">ধরণ</th>
                             <th className="px-8 py-6 text-center">একশন</th>
                           </tr>
                         </thead>
                         <tbody className="divide-y divide-gray-50 dark:divide-gray-800">
                           {filteredRequests.map(r => (
                             <tr key={r.id} className="hover:bg-gray-50/30 transition-colors">
                               <td className="px-8 py-6">
                                  <p className="font-black text-gray-900 dark:text-white">{r.hospitalName}</p>
                                  <p className="text-xs font-bold text-gray-400">রোগী: {r.patientName}</p>
                               </td>
                               <td className="px-8 py-6 text-center font-black text-red-600">{r.bloodGroup}</td>
                               <td className="px-8 py-6 text-center">
                                 <span className={cn(
                                   "px-3 py-1 rounded-full text-[8px] font-black uppercase tracking-widest",
                                   (r.urgency === 'High' || r.urgency === 'Critical') ? "bg-red-100 text-red-600" : "bg-blue-100 text-blue-600"
                                 )}>{(r.urgency === 'High' || r.urgency === 'Critical') ? 'জরুরি' : 'সাধারণ'}</span>
                               </td>
                               <td className="px-8 py-6 text-center">
                                  <div className="flex items-center justify-center gap-2">
                                    {r.status === 'Active' && r.donorId && (
                                      <button onClick={() => handleVerifyDonation(r)} className="p-2 text-green-600 hover:bg-green-50 rounded-xl" title="ভেরিফাই"><CheckCircle className="w-5 h-5" /></button>
                                    )}
                                    
                                    {confirmDeleteId === r.id ? (
                                      <div className="flex items-center gap-1">
                                        <button onClick={() => handleDeleteItem('requests', r.id)} className="p-2 text-red-600 bg-red-50 rounded-lg text-[10px] font-black italic">নিশ্চিত</button>
                                        <button onClick={() => setConfirmDeleteId(null)} className="p-2 text-gray-400 bg-gray-50 rounded-lg text-[10px] font-black italic">না</button>
                                      </div>
                                    ) : (
                                      <button 
                                        onClick={() => setConfirmDeleteId(r.id)} 
                                        disabled={deletingId === r.id}
                                        className={cn(
                                          "p-2 rounded-xl transition-all text-red-200 hover:text-red-600 hover:bg-red-50"
                                        )}
                                      >
                                        <Trash2 className={cn("w-5 h-5", deletingId === r.id && "animate-pulse")} />
                                      </button>
                                    )}
                                  </div>
                               </td>
                             </tr>
                           ))}
                         </tbody>
                      </table>
                    )}
                </div>
              </div>
           </motion.div>
        )}

        {activeTab === 'campaigns' && (
          <motion.div key="campaigns" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
             <div className="flex justify-between items-center px-4">
                <h2 className="text-2xl font-black text-gray-900 dark:text-white">সব ক্যাম্পেইন</h2>
                <button 
                  onClick={() => setShowAddEvent(!showAddEvent)} 
                  className={cn(
                    "flex items-center gap-2 px-6 py-3 rounded-2xl font-black text-sm shadow-xl active:scale-95 transition-all text-white",
                    showAddEvent ? "bg-gray-600" : "bg-red-600"
                  )}
                >
                   {showAddEvent ? <XCircle className="w-5 h-5" /> : <Plus className="w-5 h-5" />}
                   {showAddEvent ? 'বন্ধ করুন' : 'নতুন ক্যাম্পেইন'}
                </button>
             </div>

             {showAddEvent && (
                <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl border border-red-100 dark:border-red-900 grid grid-cols-1 md:grid-cols-2 gap-4 animate-in slide-in-from-top-4">
                   <input placeholder="ক্যাম্পেইন টাইটেল" className="admin-input" value={newEvent.title} onChange={e => setNewEvent({...newEvent, title: e.target.value})} />
                   <input placeholder="সংগঠনের নাম" className="admin-input" value={newEvent.orgName} onChange={e => setNewEvent({...newEvent, orgName: e.target.value})} />
                   <input type="date" className="admin-input" value={newEvent.date} onChange={e => setNewEvent({...newEvent, date: e.target.value})} />
                   <input placeholder="ফোন নাম্বার" className="admin-input" value={newEvent.contactNumber} onChange={e => setNewEvent({...newEvent, contactNumber: e.target.value})} />
                   <input placeholder="লোকেশন" className="admin-input md:col-span-2" value={newEvent.location} onChange={e => setNewEvent({...newEvent, location: e.target.value})} />
                   <textarea placeholder="বিস্তারিত বিবরণ..." className="admin-input md:col-span-2 min-h-[100px]" value={newEvent.description} onChange={e => setNewEvent({...newEvent, description: e.target.value})} />
                   <button onClick={handleAddEvent} className="md:col-span-2 py-4 bg-red-600 text-white rounded-2xl font-black shadow-lg">ক্যাম্পেইন পাবলিশ করুন</button>
                </div>
             )}
             
             <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                {events.map(e => (
                  <div key={e.id} className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-xl border border-gray-100 dark:border-gray-800 space-y-4 transition-colors">
                     <div className="flex justify-between items-start">
                        <div>
                           <p className="text-[10px] font-black text-red-600 uppercase tracking-widest">{e.orgName}</p>
                           <h3 className="text-xl font-black mt-1 text-gray-900 dark:text-white">{e.title}</h3>
                        </div>
                        
                        {confirmDeleteId === e.id ? (
                           <div className="flex items-center gap-2">
                             <button onClick={() => handleDeleteItem('events', e.id)} className="px-3 py-1 bg-red-600 text-white rounded-lg text-[10px] font-black">ডিলিট</button>
                             <button onClick={() => setConfirmDeleteId(null)} className="px-3 py-1 bg-gray-100 text-gray-500 rounded-lg text-[10px] font-black">না</button>
                           </div>
                        ) : (
                           <button 
                             onClick={() => setConfirmDeleteId(e.id)} 
                             disabled={deletingId === e.id}
                             className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 rounded-xl transition-colors"
                           >
                             <Trash2 className={cn("w-5 h-5", deletingId === e.id && "animate-pulse")} />
                           </button>
                        )}
                     </div>
                     <div className="flex items-center gap-3 text-sm font-bold text-gray-500">
                        <Calendar className="w-4 h-4" /> {new Date(e.date).toLocaleDateString('bn-BD')}
                     </div>
                  </div>
                ))}
             </div>
          </motion.div>
        )}

        {activeTab === 'resources' && (
          <motion.div key="resources" initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="space-y-6">
             <div className="flex justify-between items-center px-4">
                <h2 className="text-2xl font-black text-gray-900 dark:text-white">সব রিসোর্স</h2>
                <button 
                  onClick={() => setShowAddResource(!showAddResource)} 
                  className={cn(
                    "flex items-center gap-2 px-6 py-3 rounded-2xl font-black text-sm shadow-xl active:scale-95 transition-all text-white",
                    showAddResource ? "bg-gray-600" : "bg-red-600"
                  )}
                >
                   {showAddResource ? <XCircle className="w-5 h-5" /> : <Plus className="w-5 h-5" />}
                   {showAddResource ? 'বন্ধ করুন' : 'নতুন রিসোর্স'}
                </button>
             </div>

             {showAddResource && (
                <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl border border-red-100 dark:border-red-900 grid grid-cols-1 md:grid-cols-2 gap-4 animate-in slide-in-from-top-4">
                   <input placeholder="রিসোর্স এর নাম (উদা: সাতকানিয়া ব্লাড ব্যাংক)" className="admin-input" value={newResource.name} onChange={e => setNewResource({...newResource, name: e.target.value})} />
                   <select className="admin-input" value={newResource.type} onChange={e => setNewResource({...newResource, type: e.target.value as any})}>
                      <option value="Blood Bank">ব্লাড ব্যাংক</option>
                      <option value="Ambulance">অ্যাম্বুলেন্স</option>
                   </select>
                   <input placeholder="লোকেশন" className="admin-input" value={newResource.location} onChange={e => setNewResource({...newResource, location: e.target.value})} />
                   <input placeholder="যোগাযোগের নম্বর" className="admin-input" value={newResource.contactNumber} onChange={e => setNewResource({...newResource, contactNumber: e.target.value})} />
                   <label className="flex items-center gap-3 px-4 font-bold text-sm text-gray-400">
                      <input type="checkbox" checked={newResource.available24h} onChange={e => setNewResource({...newResource, available24h: e.target.checked})} className="w-5 h-5 rounded-lg text-red-600" />
                      ২৪ ঘণ্টা খোলা থাকে?
                   </label>
                   <div className="md:col-span-2">
                      <button onClick={handleAddResource} className="w-full py-4 bg-red-600 text-white rounded-2xl font-black shadow-lg">রিসোর্স যোগ করুন</button>
                   </div>
                </div>
             )}

             <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                {resources.map(r => (
                  <div key={r.id} className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-xl border border-gray-100 dark:border-gray-800 space-y-6 transition-colors">
                     <div className="w-12 h-12 bg-gray-50 dark:bg-gray-800 flex items-center justify-center rounded-2xl">
                        {r.type === 'Blood Bank' ? <Heart className="w-6 h-6 text-red-600 fill-current" /> : <Activity className="w-6 h-6 text-blue-600" />}
                     </div>
                     <div>
                        <h3 className="font-black text-lg text-gray-900 dark:text-white">{r.name}</h3>
                        <p className="text-xs font-bold text-gray-400 mt-1"><MapPin className="w-3 h-3 inline mr-1" /> {r.location}</p>
                     </div>
                     <div className="pt-4 border-t border-gray-50 dark:border-gray-800 flex items-center justify-between">
                        <span className="text-sm font-black text-gray-900 dark:text-white"><Phone className="w-4 h-4 inline mr-2 text-green-600" /> {r.contactNumber}</span>
                        
                        {confirmDeleteId === r.id ? (
                           <div className="flex items-center gap-2">
                             <button onClick={() => handleDeleteItem('resources', r.id)} className="px-3 py-1 bg-red-600 text-white rounded-lg text-[10px] font-black">ডিলিট</button>
                             <button onClick={() => setConfirmDeleteId(null)} className="px-3 py-1 bg-gray-100 text-gray-500 rounded-lg text-[10px] font-black">না</button>
                           </div>
                        ) : (
                           <button 
                             onClick={() => setConfirmDeleteId(r.id)} 
                             disabled={deletingId === r.id}
                             className="p-2 text-gray-400 hover:text-red-600 hover:bg-red-50 dark:hover:bg-red-900/20 rounded-xl transition-colors"
                           >
                             <Trash2 className={cn("w-5 h-5", deletingId === r.id && "animate-pulse")} />
                           </button>
                        )}
                     </div>
                  </div>
                ))}
             </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Admin Quick Help */}
      <section className="bg-gray-900 text-white p-12 rounded-[50px] shadow-2xl relative overflow-hidden mt-12">
         <div className="relative z-10 flex flex-col md:flex-row items-center justify-between gap-8">
            <div className="space-y-4">
               <h2 className="text-3xl font-black italic">অ্যাডমিন গাইডলাইন 🛡️</h2>
               <p className="text-gray-400 font-bold max-w-xl">সতর্কতার সাথে সব কার্যক্রম সম্পাদন করুন। আপনার প্রতিটি একশন সিস্টেমে ইমপ্যাক্ট তৈরি করে।</p>
            </div>
            <div className="flex gap-4">
               <a 
                 href="https://wa.me/8801897971573"
                 target="_blank"
                 rel="noopener noreferrer"
                 className="px-8 py-4 bg-white/10 backdrop-blur-md rounded-2xl font-black text-sm border border-white/20 hover:bg-white/20 transition-all active:scale-95 flex items-center justify-center"
               >
                 সাপোর্ট
               </a>
               <button 
                 onClick={handleHelpDeskClick}
                 className="px-8 py-4 bg-red-600 rounded-2xl font-black text-sm shadow-xl active:scale-95 transition-all hover:bg-red-700"
               >
                 হেল্প ডেক্স
               </button>
            </div>
         </div>
         <Shield className="absolute -right-16 -bottom-16 w-64 h-64 text-white/5 rotate-12" />
      </section>
    </div>
  );
}
