import React, { useEffect, useState } from 'react';
import { useParams, Link } from 'react-router-dom';
import { doc, getDoc, collection, query, where, getDocs, orderBy, addDoc, serverTimestamp } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Organization, DonationEvent } from '../types';
import { MapPin, Users, Calendar, Phone, Mail, ShieldCheck, ArrowLeft, Building2, ExternalLink, Info, Award, Plus, X, Loader2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { useAuth } from '../contexts/AuthContext';

export function OrganizationProfile() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const [org, setOrg] = useState<Organization | null>(null);
  const [events, setEvents] = useState<DonationEvent[]>([]);
  const [topDonors, setTopDonors] = useState<any[]>([]);
  const [registeredMemberCount, setRegisteredMemberCount] = useState(0);
  const [loading, setLoading] = useState(true);
  
  // Event creation state
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [newEvent, setNewEvent] = useState({
    title: '',
    description: '',
    date: '',
    location: '',
    contactNumber: ''
  });

  const fetchEvents = async (orgName: string) => {
    try {
      const eventsQuery = query(
        collection(db, 'events'),
        where('orgName', '==', orgName),
        orderBy('date', 'desc')
      );
      const eventsSnap = await getDocs(eventsQuery);
      setEvents(eventsSnap.docs.map(d => ({ id: d.id, ...d.data() } as DonationEvent)));
    } catch (err) {
      console.error("Error fetching events:", err);
    }
  };

  useEffect(() => {
    const fetchOrgData = async () => {
      if (!id) return;
      try {
        const orgDoc = await getDoc(doc(db, 'organizations', id));
        if (orgDoc.exists()) {
          const orgData = { id: orgDoc.id, ...orgDoc.data() } as Organization;
          setOrg(orgData);
          
          await fetchEvents(orgData.name);

          // Fetch registered member count
          const orgMembersQuery = query(
            collection(db, 'users'),
            where('organizationId', '==', id)
          );
          const membersSnap = await getDocs(orgMembersQuery);
          setRegisteredMemberCount(membersSnap.size);

          // Fetch some associated members/donors
          const donorsQuery = query(
            collection(db, 'users'),
            where('organizationId', '==', id),
            orderBy('points', 'desc')
          );
          const donorsSnap = await getDocs(donorsQuery);
          setTopDonors(donorsSnap.docs.map(d => d.data()).slice(0, 5));
        }
      } catch (error) {
        console.error("Error fetching organization data:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchOrgData();
  }, [id]);

  const handleCreateEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!org || !user) return;

    setSubmitting(true);
    try {
      await addDoc(collection(db, 'events'), {
        ...newEvent,
        orgId: org.id,
        orgName: org.name,
        orgAdminUid: user.uid,
        createdAt: new Date().toISOString()
      });

      setIsModalOpen(false);
      setNewEvent({
        title: '',
        description: '',
        date: '',
        location: '',
        contactNumber: ''
      });
      
      // Refresh events
      await fetchEvents(org.name);
    } catch (err) {
      console.error("Error creating event:", err);
      alert('ইভেন্ট তৈরি করতে সমস্যা হয়েছে। আবার চেষ্টা করুন।');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[60vh]">
        <div className="w-12 h-12 border-4 border-red-600 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!org) {
    return (
      <div className="text-center py-20">
        <Building2 className="w-20 h-20 text-gray-200 mx-auto mb-6" />
        <h2 className="text-3xl font-black text-gray-900 dark:text-white">সংগঠনটি পাওয়া যায়নি</h2>
        <Link to="/organizations" className="mt-8 inline-block text-red-600 font-bold hover:underline">সকল সংগঠন দেখুন</Link>
      </div>
    );
  }

  const isAdmin = user?.uid === org.adminUid;

  return (
    <div className="max-w-6xl mx-auto px-4 pb-20">
      {/* Back Button */}
      <Link to="/organizations" className="inline-flex items-center gap-2 text-gray-500 hover:text-red-600 font-bold mb-8 group transition-colors">
        <ArrowLeft className="w-5 h-5 group-hover:-translate-x-1 transition-transform" />
        <span>পূর্ববর্তী পৃষ্ঠায় ফিরে যান</span>
      </Link>

      <div className="grid lg:grid-cols-3 gap-12">
        {/* Left Column: Profile Card */}
        <div className="lg:col-span-1 space-y-6">
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            className="bg-white dark:bg-gray-900 rounded-[40px] p-8 shadow-2xl border border-gray-100 dark:border-gray-800 space-y-8 sticky top-8 transition-colors"
          >
            <div className="relative inline-block">
               <div className="w-24 h-24 bg-red-50 dark:bg-red-900/20 rounded-[30px] flex items-center justify-center">
                  <Building2 className="w-12 h-12 text-red-600" />
               </div>
               {org.isVerified && (
                 <div className="absolute -bottom-2 -right-2 bg-green-500 text-white p-1.5 rounded-full shadow-lg border-2 border-white dark:border-gray-900">
                   <ShieldCheck className="w-4 h-4" />
                 </div>
               )}
            </div>

            <div>
              <h1 className="text-3xl font-black text-gray-900 dark:text-white uppercase tracking-tight">{org.name}</h1>
              <p className="text-red-600 font-bold text-sm mt-1">{org.type}</p>
            </div>

            <div className="space-y-4">
              <div className="flex items-start gap-4">
                <MapPin className="w-5 h-5 text-gray-400 shrink-0 mt-1" />
                <div>
                  <p className="text-xs font-black uppercase text-gray-400 tracking-widest">অবস্থান</p>
                  <p className="font-bold text-gray-700 dark:text-gray-300">{org.location.district}, {org.location.division}</p>
                </div>
              </div>
              <div className="flex items-start gap-4">
                <Users className="w-5 h-5 text-gray-400 shrink-0 mt-1" />
                <div>
                  <p className="text-xs font-black uppercase text-gray-400 tracking-widest">নিবন্ধিত দাতা সংখ্যা</p>
                  <p className="font-bold text-gray-700 dark:text-gray-300">{registeredMemberCount} জন দাতা</p>
                </div>
              </div>
              {org.coverageArea && (
                <div className="flex items-start gap-4">
                  <ExternalLink className="w-5 h-5 text-gray-400 shrink-0 mt-1" />
                  <div>
                    <p className="text-xs font-black uppercase text-gray-400 tracking-widest">কভারেজ এলাকা</p>
                    <p className="font-bold text-gray-700 dark:text-gray-300">{org.coverageArea}</p>
                  </div>
                </div>
              )}
            </div>

            <div className="pt-4 space-y-3">
               <a href={`tel:${org.phoneNumber}`} className="flex items-center justify-center gap-3 w-full py-4 bg-red-600 text-white rounded-2xl font-black shadow-xl shadow-red-200 hover:bg-red-700 transition-all active:scale-95">
                 <Phone className="w-5 h-5" />
                 কল করুন
               </a>
               {org.email && (
                 <a href={`mailto:${org.email}`} className="flex items-center justify-center gap-3 w-full py-4 bg-gray-50 dark:bg-gray-800 text-gray-700 dark:text-gray-300 rounded-2xl font-black hover:bg-gray-100 transition-all">
                   <Mail className="w-5 h-5" />
                   মেইল করুন
                 </a>
               )}
            </div>
          </motion.div>
        </div>

        {/* Right Column: Details & Events */}
        <div className="lg:col-span-2 space-y-12">
          {/* About Section */}
          <motion.section 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.1 }}
            className="space-y-6"
          >
            <h2 className="text-2xl font-black flex items-center gap-3 text-gray-900 dark:text-white">
              <Info className="w-6 h-6 text-red-600" />
              সংগঠন সম্পর্কে
            </h2>
            <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-sm border border-gray-100 dark:border-gray-800 transition-colors">
              <p className="text-gray-600 dark:text-gray-400 leading-relaxed text-lg whitespace-pre-wrap">
                {org.description || 'এই সংগঠন সম্পর্কে কোনো বিস্তারিত তথ্য যোগ করা হয়নি।'}
              </p>
              {org.address && (
                <div className="mt-8 pt-8 border-t border-gray-50 dark:border-gray-800">
                  <p className="text-xs font-black uppercase text-gray-400 tracking-widest mb-2">ঠিকানা</p>
                  <p className="font-bold text-gray-800 dark:text-gray-200">{org.address}</p>
                </div>
              )}
            </div>
          </motion.section>

          {/* Events Section */}
          <motion.section 
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            transition={{ delay: 0.2 }}
            className="space-y-6"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-2xl font-black flex items-center gap-3 text-gray-900 dark:text-white">
                <Calendar className="w-6 h-6 text-red-600" />
                আসন্ন ক্যাম্পেইনসমূহ
              </h2>
              {isAdmin && (
                <button 
                  onClick={() => setIsModalOpen(true)}
                  className="flex items-center gap-2 px-4 py-2 bg-red-600 text-white rounded-xl font-bold text-sm hover:bg-red-700 transition-all active:scale-95"
                >
                  <Plus className="w-4 h-4" />
                  নতুন ক্যাম্পেইন
                </button>
              )}
            </div>
            
            {events.length > 0 ? (
              <div className="grid md:grid-cols-2 gap-6">
                {events.map((event) => (
                  <div key={event.id} className="bg-white dark:bg-gray-900 p-6 rounded-[32px] border border-gray-100 dark:border-gray-800 hover:shadow-xl transition-all group">
                    <div className="flex justify-between items-start mb-4">
                      <div className="bg-red-50 dark:bg-red-900/10 p-3 rounded-2xl">
                         <Calendar className="w-6 h-6 text-red-600" />
                      </div>
                      <span className="text-[10px] font-black uppercase bg-gray-100 dark:bg-gray-800 px-3 py-1 rounded-full text-gray-500">
                        {new Date(event.date).toLocaleDateString('bn-BD', { day: 'numeric', month: 'long', year: 'numeric' })}
                      </span>
                    </div>
                    <h3 className="font-black text-gray-900 dark:text-white mb-2 uppercase">{event.title}</h3>
                    <p className="text-sm text-gray-500 dark:text-gray-400 mb-4 line-clamp-2">{event.description}</p>
                    <div className="space-y-2">
                      <div className="flex items-center gap-2 text-xs font-bold text-gray-400">
                        <MapPin className="w-4 h-4" /> {event.location}
                      </div>
                      <div className="flex items-center gap-2 text-xs font-bold text-gray-400">
                        <Phone className="w-4 h-4" /> {event.contactNumber}
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="bg-gray-50 dark:bg-gray-800/50 rounded-[40px] p-12 text-center border-2 border-dashed border-gray-200 dark:border-gray-700">
                 <p className="text-gray-400 font-bold">বর্তমানে কোনো ক্যাম্পেইন পাওয়া যায়নি।</p>
                 {isAdmin && (
                   <button 
                     onClick={() => setIsModalOpen(true)}
                     className="mt-4 text-red-600 font-black hover:underline text-sm"
                   >
                     প্রথম ক্যাম্পেইন যোগ করুন
                   </button>
                 )}
              </div>
            )}
          </motion.section>

          {/* Top Members/Donors */}
          {topDonors.length > 0 && (
            <motion.section 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              transition={{ delay: 0.3 }}
              className="space-y-6"
            >
              <h2 className="text-2xl font-black flex items-center gap-3 text-gray-900 dark:text-white">
                <Award className="w-6 h-6 text-yellow-500" />
                শীর্ষ ডোনারবৃন্দ
              </h2>
              <div className="bg-white dark:bg-gray-900 rounded-[40px] shadow-sm border border-gray-100 dark:border-gray-800 divide-y divide-gray-50 dark:divide-gray-800 overflow-hidden transition-colors">
                {topDonors.map((donor, idx) => (
                  <div key={idx} className="p-6 flex items-center gap-4 hover:bg-gray-50 dark:hover:bg-gray-800 transition-colors">
                    <div className="w-10 h-10 rounded-full bg-red-100 dark:bg-red-900/20 flex items-center justify-center font-black text-red-600">
                       {idx + 1}
                    </div>
                    <div className="flex-1">
                      <p className="font-black text-gray-900 dark:text-white">{donor.displayName}</p>
                      <p className="text-xs text-gray-400 font-bold">{donor.location.district} • {donor.bloodGroup}</p>
                    </div>
                    <div className="text-right">
                       <p className="text-sm font-black text-red-600">{donor.points} PTS</p>
                    </div>
                  </div>
                ))}
              </div>
            </motion.section>
          )}
        </div>
      </div>

      {/* Add Event Modal */}
      <AnimatePresence>
        {isModalOpen && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => !submitting && setIsModalOpen(false)}
              className="absolute inset-0 bg-gray-900/60 backdrop-blur-sm"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.9, y: 20 }}
              animate={{ opacity: 1, scale: 1, y: 0 }}
              exit={{ opacity: 0, scale: 0.9, y: 20 }}
              className="relative bg-white dark:bg-gray-900 w-full max-w-lg rounded-[40px] shadow-2xl p-8 space-y-6"
            >
              <div className="flex justify-between items-center">
                <h3 className="text-2xl font-black text-gray-900 dark:text-white">নতুন ক্যাম্পেইন</h3>
                <button 
                  onClick={() => setIsModalOpen(false)}
                  disabled={submitting}
                  className="p-2 hover:bg-gray-100 dark:hover:bg-gray-800 rounded-full transition-colors"
                >
                  <X className="w-6 h-6 text-gray-400" />
                </button>
              </div>

              <form onSubmit={handleCreateEvent} className="space-y-4">
                <div className="space-y-2">
                  <label className="text-xs font-black text-gray-400 uppercase tracking-widest ml-1">ক্যাম্পেইন শিরোনাম</label>
                  <input 
                    type="text"
                    required
                    value={newEvent.title}
                    onChange={(e) => setNewEvent(prev => ({ ...prev, title: e.target.value }))}
                    placeholder="যেমনঃ স্বেচ্ছায় রক্তদান কর্মসূচি ২০২৪"
                    className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                  />
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-black text-gray-400 uppercase tracking-widest ml-1">বিস্তারিত বিবরণ</label>
                  <textarea 
                    required
                    value={newEvent.description}
                    onChange={(e) => setNewEvent(prev => ({ ...prev, description: e.target.value }))}
                    placeholder="ক্যাম্পেইন সম্পর্কে বিস্তারিত লিখুন..."
                    rows={3}
                    className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                  />
                </div>

                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <label className="text-xs font-black text-gray-400 uppercase tracking-widest ml-1">তারিখ</label>
                    <input 
                      type="date"
                      required
                      value={newEvent.date}
                      onChange={(e) => setNewEvent(prev => ({ ...prev, date: e.target.value }))}
                      className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-black text-gray-400 uppercase tracking-widest ml-1">যোগাযোগ</label>
                    <input 
                      type="tel"
                      required
                      value={newEvent.contactNumber}
                      onChange={(e) => setNewEvent(prev => ({ ...prev, contactNumber: e.target.value }))}
                      placeholder="মোবাইল নম্বর"
                      className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-black text-gray-400 uppercase tracking-widest ml-1">স্থান</label>
                  <input 
                    type="text"
                    required
                    value={newEvent.location}
                    onChange={(e) => setNewEvent(prev => ({ ...prev, location: e.target.value }))}
                    placeholder="যেমনঃ সাতকানিয়া সরকারি কলেজ প্রাঙ্গণ"
                    className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-2 border-transparent focus:border-red-600 outline-none rounded-2xl font-bold text-gray-900 dark:text-white transition-all"
                  />
                </div>

                <button 
                  type="submit"
                  disabled={submitting}
                  className="w-full py-4 bg-red-600 text-white rounded-2xl font-black shadow-xl shadow-red-200 hover:bg-red-700 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-3"
                >
                  {submitting ? (
                    <>তৈরি করা হচ্ছে... <Loader2 className="w-5 h-5 animate-spin" /></>
                  ) : (
                    <>ক্যাম্পেইন পাবলিশ করুন</>
                  )}
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
