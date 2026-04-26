import React, { useState, useEffect } from 'react';
import { collection, query, where, orderBy, limit, onSnapshot, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { BloodRequest, OperationType, UserProfile, DonationRecord } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Heart, Activity, MapPin, Clock, Search, ExternalLink, Award, Star, TrendingUp, User, Bell, ShieldCheck, Users, Map, BarChart2 } from 'lucide-react';
import { formatDistanceToNow, differenceInDays, addDays, format } from 'date-fns';
import { bn } from 'date-fns/locale';
import { Link } from 'react-router-dom';
import { cn } from '../lib/utils';
import { motion } from 'motion/react';
import { BarChart, Bar, XAxis, Tooltip, ResponsiveContainer, YAxis } from 'recharts';
import { EligibilityChecker } from '../components/EligibilityChecker';
import { BloodCompatibilityChart } from '../components/BloodCompatibilityChart';
import { HealthTips } from '../components/HealthTips';

const getBadge = (count: number = 0) => {
  if (count >= 10) return { icon: Award, label: 'অভিভাবক দূত', color: 'text-purple-600', bg: 'bg-purple-50' };
  if (count >= 5) return { icon: Award, label: 'জীবন রক্ষাকারী', color: 'text-blue-600', bg: 'bg-blue-50' };
  if (count >= 1) return { icon: Award, label: 'রক্তদান বীর', color: 'text-green-600', bg: 'bg-green-50' };
  return { icon: Award, label: 'নতুন সদস্য', color: 'text-gray-400', bg: 'bg-gray-50' };
};

const ActionIcon = ({ icon: Icon }: { icon: any }) => <Icon className="w-8 h-8 text-gray-600 dark:text-gray-400" />;

export function Dashboard() {
  const { profile } = useAuth();
  const [recentRequests, setRecentRequests] = useState<BloodRequest[]>([]);
  const [matchingRequests, setMatchingRequests] = useState<BloodRequest[]>([]);
  const [stats, setStats] = useState({ active: 0, critical: 0, totalDonors: 0 });
  const [donationRecords, setDonationRecords] = useState<DonationRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [isCheckerOpen, setIsCheckerOpen] = useState(false);

  useEffect(() => {
    const qRequests = query(
      collection(db, 'requests'),
      where('status', '==', 'Active'),
      orderBy('createdAt', 'desc')
    );

    const unsubscribeRequests = onSnapshot(qRequests, (snapshot) => {
      const allReqs = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as BloodRequest));
      setRecentRequests(allReqs.slice(0, 5));
      
      const active = snapshot.size;
      const critical = allReqs.filter(r => r.urgency === 'Critical').length;
      
      // Matching requests: Same blood group and nearby (same district)
      if (profile) {
        const matches = allReqs.filter(r => 
          r.bloodGroup === profile.bloodGroup && 
          r.location.district === profile.location.district
        );
        setMatchingRequests(matches);
      }

      setStats(prev => ({ ...prev, active, critical }));
      setLoading(false);
    }, (error) => {
      handleFirestoreError(error, OperationType.LIST, 'requests');
    });

    try {
      getDocs(query(collection(db, 'users'), where('isDonor', '==', true)))
        .then(snap => setStats(prev => ({ ...prev, totalDonors: snap.size })))
        .catch(err => console.warn("Could not fetch total donors count:", err.message));
    } catch (e) {
      console.warn("Donor stats fetch failed:", e);
    }
    
    if (profile) {
      try {
        const qDonations = query(
          collection(db, 'users', profile.uid, 'donations'),
          orderBy('date', 'asc')
        );
        getDocs(qDonations)
          .then(snap => {
            const history = snap.docs.map(doc => ({ id: doc.id, ...doc.data() } as DonationRecord));
            setDonationRecords(history);
          })
          .catch(err => {
            console.warn("Could not fetch donations:", err);
          });
      } catch (err) {
        console.warn("Failed fetching donations config:", err);
      }
    }

    return () => unsubscribeRequests();
  }, [profile]);

  const badge = getBadge(profile?.donationsCount);

  // Calculate next donation eligibility (4 months = 120 days)
  const lastDonatedDate = profile?.lastDonated ? new Date(profile.lastDonated) : null;
  const daysSinceDonation = lastDonatedDate ? differenceInDays(new Date(), lastDonatedDate) : null;
  const isEligible = !lastDonatedDate || daysSinceDonation! >= 120;
  const nextEligibleDate = lastDonatedDate ? addDays(lastDonatedDate, 120) : null;

  const chartData = donationRecords.map(record => ({
    name: format(new Date(record.date), 'MMM yyyy', { locale: bn }),
    points: record.pointsEarned || 0,
    donations: 1
  }));

  return (
    <div className="pb-32 animate-fade-in bg-gray-50 dark:bg-gray-950 min-h-screen">
      <div className="max-w-4xl mx-auto px-6 pt-10 space-y-8">
        {/* Welcome Card */}
        <div className="bg-white dark:bg-gray-900 p-6 rounded-[40px] shadow-xl card-shadow border border-gray-100 dark:border-gray-800 flex items-center justify-between group">
           <div className="space-y-2">
              <p className="text-gray-400 font-bold text-base">স্বাগতম, <span className="text-gray-900 dark:text-white">{profile?.displayName || 'বন্ধু'}</span></p>
              <p className="text-gray-900 dark:text-white font-black text-xl">{profile?.donationsCount || 0} বার রক্তদান</p>
              
              {!isEligible && (
                <div className="flex items-center gap-2 text-brand font-black text-sm pt-2">
                   <Clock className="w-4 h-4" /> 
                   <span>পরবর্তী রক্তদানের জন্য {daysSinceDonation !== null ? Math.max(0, 120 - daysSinceDonation) : 120} দিন অপেক্ষা করুন</span>
                </div>
              )}
           </div>
           <div className="w-20 h-20 bg-brand text-white rounded-full flex items-center justify-center font-black text-2xl shadow-xl shadow-red-200">
              {profile?.bloodGroup || '?'}
           </div>
        </div>

        {/* Info Grid */}
        <div className="grid grid-cols-2 gap-4">
           {/* Points */}
           <div className="bg-white dark:bg-gray-900 p-6 rounded-[40px] shadow-xl card-shadow border border-gray-100 dark:border-gray-800 space-y-4">
              <div className="w-12 h-12 bg-yellow-50 dark:bg-yellow-900/20 rounded-2xl flex items-center justify-center text-yellow-500">
                 <Star className="w-6 h-6 fill-current" />
              </div>
              <div>
                 <p className="text-2xl font-black text-gray-900 dark:text-white">{profile?.points || 0}</p>
                 <p className="text-gray-400 font-bold text-sm">পয়েন্ট</p>
              </div>
           </div>
           {/* Badge */}
           <div className="bg-white dark:bg-gray-900 p-6 rounded-[40px] shadow-xl card-shadow border border-gray-100 dark:border-gray-800 space-y-4">
              <div className={cn("w-12 h-12 rounded-2xl flex items-center justify-center", badge.bg, badge.color)}>
                 <badge.icon className="w-6 h-6" />
              </div>
              <div>
                 <p className="text-xl font-black text-gray-900 dark:text-white leading-tight">{badge.label}</p>
                 <p className="text-gray-400 font-bold text-sm">ব্যাজ</p>
              </div>
           </div>
           {/* Total Donors */}
           <div className="bg-white dark:bg-gray-900 p-6 rounded-[40px] shadow-xl card-shadow border border-gray-100 dark:border-gray-800 space-y-4">
              <div className="w-12 h-12 bg-red-50 dark:bg-red-900/20 rounded-2xl flex items-center justify-center text-brand">
                 <Users className="w-6 h-6" />
              </div>
              <div>
                 <p className="text-2xl font-black text-gray-900 dark:text-white">{stats.totalDonors}</p>
                 <p className="text-gray-400 font-bold text-sm">মোট দাতা</p>
              </div>
           </div>
           {/* Requests Today */}
           <div className="bg-white dark:bg-gray-900 p-6 rounded-[40px] shadow-xl card-shadow border border-gray-100 dark:border-gray-800 space-y-4">
              <div className="w-12 h-12 bg-brand-muted dark:bg-brand/10 rounded-2xl flex items-center justify-center text-brand">
                 <Activity className="w-6 h-6" />
              </div>
              <div>
                 <p className="text-2xl font-black text-gray-900 dark:text-white">{stats.active}</p>
                 <p className="text-gray-400 font-bold text-sm">আজকের আবেদন</p>
              </div>
           </div>
        </div>

        {/* Donation Goal & Impact (Item 6) */}
        <section className="bg-gradient-to-br from-indigo-600 to-blue-700 p-8 rounded-[40px] text-white shadow-2xl relative overflow-hidden">
           <div className="relative z-10 space-y-6">
              <div className="flex items-center justify-between">
                 <div>
                    <p className="text-indigo-100 font-black uppercase tracking-widest text-[10px] mb-1">জীবনের হিসাব</p>
                    <h3 className="text-3xl font-black">আপনার প্রভাব</h3>
                 </div>
                 <div className="w-16 h-16 bg-white/20 backdrop-blur-md rounded-3xl flex items-center justify-center border border-white/30">
                    <Heart className="w-8 h-8 fill-current" />
                 </div>
              </div>
              
              <div className="grid grid-cols-2 gap-6">
                 <div className="space-y-1">
                    <p className="text-4xl font-black">{(profile?.donationsCount || 0) * 3}</p>
                    <p className="text-sm font-bold opacity-80">মানুষের জীবন বেঁচেছে</p>
                 </div>
                 <div className="space-y-1">
                    <p className="text-4xl font-black">{profile?.donationsCount || 0}</p>
                    <p className="text-sm font-bold opacity-80">রক্তদান করেছেন</p>
                 </div>
              </div>

              <div className="pt-4 space-y-3">
                 <div className="flex justify-between text-xs font-black uppercase tracking-wider">
                    <span>পরবর্তী লক্ষ্য: {((profile?.donationsCount || 0) + 1)} বার রক্তদান</span>
                    <span>{Math.min(100, ((profile?.points || 0) % 100))} %</span>
                 </div>
                 <div className="h-3 bg-white/10 rounded-full overflow-hidden border border-white/10">
                    <motion.div 
                      initial={{ width: 0 }}
                      animate={{ width: `${Math.min(100, ((profile?.points || 0) % 100))}%` }}
                      className="h-full bg-white shadow-lg shadow-white/20"
                    />
                 </div>
                 <p className="text-[10px] font-bold opacity-70">আরও {(100 - ((profile?.points || 0) % 100))} পয়েন্ট হলে আপনি নতুন ব্যাজ পাবেন!</p>
              </div>
           </div>
           <Award className="absolute -bottom-10 -right-10 w-48 h-48 text-white/5 rotate-12" />
        </section>

        {/* Matching Requests Section (Item 3) */}
        {matchingRequests.length > 0 && (
          <section className="space-y-4">
             <div className="flex items-center justify-between px-2">
                <h3 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                  <div className="w-2 h-2 bg-brand rounded-full animate-ping" />
                  আপনার গ্রুপের জরুরি আবেদন ({matchingRequests.length})
                </h3>
                <Link to="/requests" className="text-brand text-sm font-black">সব দেখুন</Link>
             </div>
             
             <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
               {matchingRequests.slice(0, 2).map((req) => (
                 <Link 
                   key={req.id} 
                   to="/requests" 
                   className="bg-brand text-white p-6 rounded-[32px] shadow-xl shadow-red-200 dark:shadow-none border border-transparent flex flex-col gap-4 relative overflow-hidden group hover:scale-[1.02] transition-transform"
                 >
                    <div className="relative z-10 flex justify-between items-start">
                       <div className="space-y-1">
                          <p className="text-[10px] font-black uppercase tracking-widest text-red-100">ম্যাচিং রিকোয়েস্ট</p>
                          <h4 className="text-lg font-black truncate">{req.bloodGroup} রক্ত প্রয়োজন</h4>
                       </div>
                       <div className="w-10 h-10 bg-white/20 rounded-xl flex items-center justify-center">
                          <Heart className="w-6 h-6 fill-current" />
                       </div>
                    </div>
                    <div className="relative z-10 space-y-2">
                       <p className="text-sm font-bold opacity-90"><MapPin className="w-4 h-4 inline mr-1" /> {req.hospitalName}</p>
                       <p className="text-xs font-bold opacity-80">{req.location.upazila}, {req.location.district}</p>
                    </div>
                    <Heart className="absolute -right-10 -bottom-10 w-32 h-32 text-white/5 fill-current group-hover:scale-110 transition-transform" />
                 </Link>
               ))}
             </div>
          </section>
        )}

        {/* Donation Trend Graphic */}
        {(profile?.donationsCount ?? 0) > 0 && (
          <section className="space-y-4">
             <div className="flex items-center justify-between px-2">
                <h3 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                  <BarChart2 className="w-5 h-5 text-brand" />
                  রক্তদানের ধারা
                </h3>
             </div>
             <div className="bg-white dark:bg-gray-900 p-6 rounded-[40px] shadow-xl card-shadow border border-gray-100 dark:border-gray-800">
               {chartData.length > 0 ? (
                 <div className="h-64 w-full pt-4">
                   <ResponsiveContainer width="100%" height="100%">
                     <BarChart data={chartData}>
                       <XAxis dataKey="name" fontSize={12} tickLine={false} axisLine={false} />
                       <YAxis fontSize={12} tickLine={false} axisLine={false} />
                       <Tooltip 
                         contentStyle={{ borderRadius: '16px', border: 'none', boxShadow: '0 10px 15px -3px rgb(0 0 0 / 0.1)' }}
                         cursor={{ fill: 'rgba(220, 38, 38, 0.05)' }}
                       />
                       <Bar dataKey="points" name="অর্জিত পয়েন্ট" fill="#DC2626" radius={[6, 6, 0, 0]} />
                     </BarChart>
                   </ResponsiveContainer>
                 </div>
               ) : (
                 <div className="h-64 flex flex-col items-center justify-center text-gray-400 space-y-3">
                    <BarChart2 className="w-12 h-12 opacity-20" />
                    <p className="font-bold text-sm">গ্রাফ লোড হচ্ছে...</p>
                    <p className="text-xs">অপেক্ষা করুন</p>
                 </div>
               )}
             </div>
          </section>
        )}

        {/* Quick Actions */}
        <section className="space-y-4">
           <h3 className="text-xl font-black text-gray-900 dark:text-white px-2">দ্রুত কাজ</h3>
           <div className="grid grid-cols-3 gap-4">
              {[
                { to: '/search', icon: Search, label: 'রক্ত খুঁজুন' },
                { to: '/leaderboard', icon: TrendingUp, label: 'লিডারবোর্ড' },
                { to: '/campaigns', icon: Users, label: 'ক্যাম্পেইন' },
                { to: '/emergency', icon: Activity, label: 'জরুরি সেবা' },
                { to: '/certificates', icon: Award, label: 'সার্টিফিকেট' },
                { to: '/map', icon: Map, label: 'ডোনার ম্যাপ' },
              ].map((action, idx) => (
                <Link 
                  key={idx}
                  to={action.to}
                  className="bg-white dark:bg-gray-900 p-6 rounded-[32px] shadow-lg border border-gray-50 dark:border-gray-800 flex flex-col items-center gap-3 hover:scale-105 transition-transform"
                >
                   <ActionIcon icon={action.icon} />
                   <p className="text-xs font-black text-gray-500 dark:text-gray-400 uppercase tracking-tight text-center">{action.label}</p>
                </Link>
              ))}
           </div>
        </section>

        {/* Active Emergency Requests */}

        {/* Active Emergency Requests */}
        <section className="space-y-4">
           <div className="flex items-center justify-between px-2">
              <h3 className="text-xl font-black text-gray-900 dark:text-white">সক্রিয় জরুরি আবেদন</h3>
           </div>
           
           <div className="space-y-4">
              {recentRequests.length > 0 ? (
                recentRequests.map(req => (
                  <Link 
                    key={req.id} 
                    to="/requests" 
                    className="flex items-center gap-4 bg-white dark:bg-gray-900 p-4 rounded-[32px] shadow-lg border border-gray-50 dark:border-gray-800 group"
                  >
                     <div className="w-16 h-16 bg-red-50 dark:bg-red-900/10 text-brand rounded-full flex items-center justify-center font-black text-xl shrink-0 group-hover:scale-110 transition-transform">
                        {req.bloodGroup}
                     </div>
                     <div className="flex-1 min-w-0">
                        <h4 className="font-black text-gray-900 dark:text-white truncate">{req.patientName || 'জরুরি রক্ত'} - {req.bloodGroup}</h4>
                        <p className="text-xs text-gray-500 font-bold truncate">{req.hospitalName}</p>
                        <p className="text-[10px] text-gray-400 font-bold"><MapPin className="w-2.5 h-2.5 inline mr-1" /> {req.location.district}, {req.location.upazila}</p>
                     </div>
                     <div className="w-12 h-12 bg-green-50 dark:bg-green-900/10 text-green-600 rounded-2xl flex items-center justify-center shrink-0">
                        <ExternalLink className="w-6 h-6" />
                     </div>
                  </Link>
                ))
              ) : (
                <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] text-center border-2 border-dashed border-gray-100 dark:border-gray-800">
                   <p className="text-gray-400 font-bold">বর্তমানে কোনো জরুরি আবেদন নেই</p>
                </div>
              )}
           </div>
        </section>

        {/* Recent Donations */}
        <section className="space-y-4">
           <div className="flex items-center justify-between px-2">
              <h3 className="text-xl font-black text-gray-900 dark:text-white">সাম্প্রতিক রক্তদান</h3>
              <button className="text-brand text-sm font-black">সব দেখুন</button>
           </div>
           
           <div className="bg-white dark:bg-gray-900 rounded-[40px] shadow-lg border border-gray-50 dark:border-gray-800 overflow-hidden divide-y dark:divide-gray-800">
              {/* Mocking recent donation list since we don't have a direct collections for "global donations" yet, usually we show user's own or top donors */}
              {[1, 2].map((_, idx) => (
                <div key={idx} className="p-6 flex items-center gap-4">
                   <div className="w-12 h-12 bg-red-50 dark:bg-red-900/10 rounded-full flex items-center justify-center text-brand">
                      <Heart className="w-6 h-6 fill-current" />
                   </div>
                   <div className="flex-1">
                      <p className="font-black text-gray-900 dark:text-white">{idx === 0 ? 'মোহাম্মদ রহিম' : 'ফাতেমা বেগম'}</p>
                      <p className="text-xs text-gray-500 font-bold">{idx === 0 ? 'স্কয়ার হাসপাতাল' : 'আনোয়ার খান মডার্ন হাসপাতাল'}</p>
                   </div>
                   <p className="text-[10px] text-gray-400 font-black uppercase">{idx === 0 ? '২৫ মার্চ ২০২৬' : '২৫ নভে ২০২৫'}</p>
                </div>
              ))}
           </div>
        </section>

        {/* Health Tips (Item 6) */}
        <HealthTips />
      </div>

      {/* Floating Action Button */}
      <Link 
        to="/requests" 
        className="fixed bottom-8 right-8 bg-brand text-white px-8 py-4 rounded-[28px] font-black shadow-2xl shadow-brand/40 flex items-center gap-3 hover:scale-105 transition-transform z-40 active:scale-95"
      >
         <span className="text-xl">✱</span> জরুরি রক্ত
      </Link>

      <EligibilityChecker isOpen={isCheckerOpen} onClose={() => setIsCheckerOpen(false)} />
    </div>
  );
}
