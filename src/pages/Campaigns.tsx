import React, { useState, useEffect } from 'react';
import { collection, query, orderBy, onSnapshot, addDoc, where, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { DonationEvent } from '../types';
import { Users, Calendar, MapPin, Phone, Heart, ArrowRight, Star, Activity } from 'lucide-react';
import { motion } from 'motion/react';
import { cn } from '../lib/utils';
import { useAuth } from '../contexts/AuthContext';

export function Campaigns() {
  const { user } = useAuth();
  const [campaigns, setCampaigns] = useState<DonationEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [joinedIds, setJoinedIds] = useState<Set<string>>(new Set());
  const [joiningId, setJoiningId] = useState<string | null>(null);

  useEffect(() => {
    const q = query(collection(db, 'events'), orderBy('date', 'asc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      setCampaigns(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as DonationEvent)));
      setLoading(false);
    });
    return unsubscribe;
  }, []);

  useEffect(() => {
    if (!user) return;
    const fetchParticipations = async () => {
      try {
        const q = query(collection(db, 'campaign_participants'), where('userId', '==', user.uid));
        const snap = await getDocs(q);
        const ids = new Set(snap.docs.map(doc => doc.data().campaignId as string));
        setJoinedIds(ids);
      } catch (err) {
        console.error('Error fetching participations:', err);
      }
    };
    fetchParticipations();
  }, [user]);

  const handleJoinCampaign = async (camp: DonationEvent) => {
    if (!user) {
      alert('অংশগ্রহণ করতে অনুগ্রহ করে লগইন করুন।');
      return;
    }
    if (joinedIds.has(camp.id)) return;
    
    setJoiningId(camp.id);
    try {
      await addDoc(collection(db, 'campaign_participants'), {
        userId: user.uid,
        userName: user.displayName || 'ডোনার',
        campaignId: camp.id,
        orgId: camp.orgId || null, 
        orgAdminUid: camp.orgAdminUid || null, // Allow organizer to see participants
        campaignTitle: camp.title,
        campaignDate: camp.date,
        campaignLocation: camp.location,
        joinedAt: new Date().toISOString()
      });
      setJoinedIds(prev => new Set(prev).add(camp.id));
    } catch (err) {
      console.error('Error joining campaign:', err);
      alert('অংশগ্রহণে সমস্যা হয়েছে। আবার চেষ্টা করুন।');
    } finally {
      setJoiningId(null);
    }
  };

  return (
    <div className="space-y-8 animate-fade-in max-w-4xl mx-auto">
      <div className="text-center space-y-4">
        <div className="inline-flex items-center gap-2 px-4 py-2 bg-red-50 dark:bg-red-900/10 text-red-600 rounded-full font-black text-xs uppercase tracking-widest">
           <Users className="w-4 h-4" /> রক্তদান ক্যাম্পেইন
        </div>
        <h1 className="text-4xl font-black text-gray-900 dark:text-white transition-colors">আসন্ন রক্তদান কর্মসূচি</h1>
        <p className="text-gray-500 dark:text-gray-400 font-bold text-lg max-w-2xl mx-auto">সাতকানিয়া ও লোহাগাড়ার বিভিন্ন স্থানে রক্তদান ক্যাম্পেইনে অংশ নিন এবং জীবন বাঁচাতে এগিয়ে আসুন।</p>
      </div>

      {/* Campaign Details / FAQ Section */}
      <section className="bg-white dark:bg-gray-900 p-8 md:p-12 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-xl transition-all">
         <h2 className="text-2xl font-black text-gray-900 dark:text-white mb-6 border-l-4 border-red-600 pl-4">রক্তদান ক্যাম্পেইন কী ও কেন?</h2>
         <div className="grid md:grid-cols-2 gap-8 text-gray-600 dark:text-gray-400 font-bold leading-relaxed">
            <div className="space-y-4">
               <p><span className="text-red-600">ক্যাম্পেইনের কাজ:</span> রক্তদান ক্যাম্পেইন হলো একটি সুসংগঠিত কর্মসূচি যেখানে নির্দিষ্ট স্থানে ও সময়ে স্বেচ্ছায় রক্তদাতাদের কাছ থেকে রক্ত সংগ্রহ করা হয়। এটি সাধারণত বিভিন্ন সামাজিক সংগঠন, ব্লাড ব্যাংক বা ক্লাবের উদ্যোগে পরিচালিত হয়।</p>
               <p><span className="text-red-600">সুবিধা:</span> একসাথে অনেক ডোনার পাওয়া যায়, ফলে জরুরি প্রয়োজনে বড় অংকের রক্তের চাহিদা মেটানো সম্ভব হয়। এখানে পেশাদার ডাক্তার ও টেকনিশিয়ানের তত্ত্বাবধানে রক্ত সংগ্রহ করা হয় যা নিরাপদ।</p>
            </div>
            <div className="space-y-4">
               <p><span className="text-red-600">অংশগ্রহণ:</span> যে কেউ সুস্থ থাকলে নির্দিষ্ট মাপকাঠি পূরণ করে ক্যাম্পেইনে রক্ত দিতে পারেন। আপনার দেওয়া এক ব্যাগ রক্ত ৩ জন মানুষের জীবন বাঁচাতে সাহায্য করতে পারে।</p>
               <p><span className="text-red-600">প্রচার:</span> রক্তসেতু প্ল্যাটফর্মে ক্যাম্পেইন পাবলিশ করার মূল লক্ষ্য হলো সাধারণ মানুষের কাছে এই কর্মসূচির বার্তা পৌঁছে দেওয়া যাতে সবাই সঠিক সময়ে অংশ নিতে পারে।</p>
            </div>
         </div>
      </section>

      <div className="grid grid-cols-1 gap-8">
        {loading ? (
          <div className="flex flex-col items-center justify-center p-20 gap-4">
             <div className="w-12 h-12 border-4 border-brand border-t-transparent rounded-full animate-spin" />
             <p className="font-bold text-gray-400">ক্যাম্পেইন লোড হচ্ছে...</p>
          </div>
        ) : campaigns.length > 0 ? (
          campaigns.map((camp, idx) => (
            <motion.div
              key={camp.id}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: idx * 0.1 }}
              className="bg-white dark:bg-gray-900 rounded-[40px] shadow-2xl overflow-hidden border border-gray-100 dark:border-gray-800 transition-all hover:scale-[1.01] group"
            >
              <div className="flex flex-col md:flex-row">
                <div className="md:w-1/3 relative h-64 md:h-auto">
                  {camp.imageUrl ? (
                    <img src={camp.imageUrl} alt={camp.title} className="w-full h-full object-cover" />
                  ) : (
                    <div className="w-full h-full bg-red-600 flex items-center justify-center text-white">
                      <Users className="w-20 h-20 opacity-20" />
                    </div>
                  )}
                  <div className="absolute top-6 left-6 px-4 py-2 bg-white/90 dark:bg-gray-900/90 backdrop-blur-md rounded-2xl shadow-xl">
                     <p className="text-[10px] font-black text-red-600 uppercase tracking-widest">তারিখ</p>
                     <p className="font-black text-gray-900 dark:text-white">{new Date(camp.date).toLocaleDateString('bn-BD', { day: 'numeric', month: 'long' })}</p>
                  </div>
                </div>
                
                <div className="md:w-2/3 p-8 md:p-12 space-y-6 flex flex-col justify-between">
                  <div className="space-y-4">
                     <p className="text-[10px] font-black text-brand uppercase tracking-[3px]">{camp.orgName}</p>
                     <h2 className="text-3xl font-black text-gray-900 dark:text-white leading-tight">{camp.title}</h2>
                     <p className="text-gray-500 dark:text-gray-400 font-bold leading-relaxed">{camp.description}</p>
                  </div>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 pt-4 border-t border-gray-50 dark:border-gray-800">
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-blue-50 dark:bg-blue-900/10 text-blue-600 rounded-2xl flex items-center justify-center">
                        <MapPin className="w-6 h-6" />
                      </div>
                      <div>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">স্থান</p>
                        <p className="font-black text-gray-900 dark:text-white text-sm">{camp.location}</p>
                      </div>
                    </div>
                    <div className="flex items-center gap-4">
                      <div className="w-12 h-12 bg-green-50 dark:bg-green-900/10 text-green-600 rounded-2xl flex items-center justify-center">
                        <Phone className="w-6 h-6" />
                      </div>
                      <div>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">যোগাযোগ</p>
                        <p className="font-black text-gray-900 dark:text-white text-sm">{camp.contactNumber}</p>
                      </div>
                    </div>
                  </div>

                  <div className="pt-4">
                    <button 
                      onClick={() => handleJoinCampaign(camp)}
                      disabled={joiningId === camp.id || joinedIds.has(camp.id)}
                      className={cn(
                        "w-full md:w-auto px-8 py-4 rounded-2xl font-black shadow-xl transition-all flex items-center justify-center gap-3 active:scale-95 disabled:opacity-80",
                        joinedIds.has(camp.id) 
                          ? "bg-green-500 text-white shadow-green-100" 
                          : "bg-brand text-white shadow-red-200 dark:shadow-none hover:bg-brand/90"
                      )}
                    >
                       {joiningId === camp.id ? (
                         <>যুক্ত হচ্ছি... <Activity className="w-5 h-5 animate-spin" /></>
                       ) : joinedIds.has(camp.id) ? (
                         <>যুক্ত হয়েছেন <Star className="w-5 h-5 fill-current" /></>
                       ) : (
                         <>অংশগ্রহণ করুন <ArrowRight className="w-5 h-5" /></>
                       )}
                    </button>
                    {joinedIds.has(camp.id) && (
                      <p className="text-green-600 font-bold text-xs mt-3 flex items-center gap-2">
                        <Users className="w-4 h-4" /> শীঘ্রই আপনার সাথে যোগাযোগ করা হবে।
                      </p>
                    )}
                  </div>
                </div>
              </div>
            </motion.div>
          ))
        ) : (
          <div className="bg-white dark:bg-gray-900 p-20 rounded-[40px] text-center border border-dashed border-gray-200 dark:border-gray-700">
             <Star className="w-20 h-20 text-gray-100 dark:text-gray-800 mx-auto mb-6" />
             <h3 className="text-2xl font-black text-gray-900 dark:text-white mb-2">এই মুহূর্তে কোনো ক্যাম্পেইন নেই</h3>
             <p className="text-gray-500 font-bold max-w-sm mx-auto">আমরা শীঘ্রই নতুন ক্যাম্পেইন নিয়ে আসছি। আমাদের সাথেই থাকুন।</p>
          </div>
        )}
      </div>

      {/* CTA Section */}
      <section className="bg-indigo-600 p-12 rounded-[50px] shadow-2xl text-white relative overflow-hidden">
         <div className="relative z-10 space-y-6">
            <h2 className="text-3xl font-black">আপনার সংগঠন কি ক্যাম্পেইন করতে চায়?</h2>
            <p className="text-indigo-100 font-bold text-lg max-w-xl">রক্তসেতু প্ল্যাটফর্ম ব্যবহার করে আপনি আপনার সংগঠনের রক্তদান কর্মসূচি সবার কাছে পৌঁছে দিতে পারেন।</p>
            <button className="px-10 py-5 bg-white text-indigo-600 rounded-[28px] font-black shadow-xl hover:bg-indigo-50 transition-colors active:scale-95">
               আমাদের সাথে যোগাযোগ করুন
            </button>
         </div>
         <Heart className="absolute -right-16 -bottom-16 w-64 h-64 text-white/5 fill-current rotate-12" />
      </section>
    </div>
  );
}
