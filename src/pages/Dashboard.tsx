import React, { useState, useEffect } from 'react';
import { collection, query, where, orderBy, limit, onSnapshot, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { BloodRequest, OperationType, UserProfile } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Heart, Activity, MapPin, Clock, Search, ExternalLink, Award, Star, TrendingUp, User, Bell, ShieldCheck } from 'lucide-react';
import { formatDistanceToNow, differenceInDays, addDays, format } from 'date-fns';
import { bn } from 'date-fns/locale';
import { Link } from 'react-router-dom';
import { cn } from '../lib/utils';
import { motion } from 'motion/react';
import { EligibilityChecker } from '../components/EligibilityChecker';

const getBadge = (count: number = 0) => {
  if (count >= 10) return { icon: Award, label: 'অভিভাবক দূত', color: 'text-purple-600', bg: 'bg-purple-50' };
  if (count >= 5) return { icon: Award, label: 'জীবন রক্ষাকারী', color: 'text-blue-600', bg: 'bg-blue-50' };
  if (count >= 1) return { icon: Award, label: 'রক্তদান বীর', color: 'text-green-600', bg: 'bg-green-50' };
  return { icon: Award, label: 'নতুন সদস্য', color: 'text-gray-400', bg: 'bg-gray-50' };
};

export function Dashboard() {
  const { profile } = useAuth();
  const [recentRequests, setRecentRequests] = useState<BloodRequest[]>([]);
  const [matchingRequests, setMatchingRequests] = useState<BloodRequest[]>([]);
  const [stats, setStats] = useState({ active: 0, critical: 0, totalDonors: 0 });
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

    return () => unsubscribeRequests();
  }, [profile]);

  const badge = getBadge(profile?.donationsCount);

  // Calculate next donation eligibility (4 months = 120 days)
  const lastDonatedDate = profile?.lastDonated ? new Date(profile.lastDonated) : null;
  const daysSinceDonation = lastDonatedDate ? differenceInDays(new Date(), lastDonatedDate) : null;
  const isEligible = !lastDonatedDate || daysSinceDonation! >= 120;
  const nextEligibleDate = lastDonatedDate ? addDays(lastDonatedDate, 120) : null;

  return (
    <div className="space-y-10 animate-fade-in">
      {/* Welcome Banner */}
      <section className="bg-red-600 rounded-[40px] p-10 text-white relative overflow-hidden shadow-2xl shadow-red-200">
        <div className="relative z-10 grid md:grid-cols-2 gap-10 items-center">
          <div className="space-y-6">
            <h1 className="text-4xl font-black mb-3">সালাম, {profile?.displayName}!</h1>
            <p className="text-red-100 max-w-lg text-lg leading-relaxed">
              আপনার রক্তের গ্রুপ <span className="font-black underline text-white">{profile?.bloodGroup || 'অজানা'}</span>। 
              {profile?.isDonor ? ' আপনি একজন গর্বিত রক্তদাতা! আপনার অবদানে জীবন রক্ষা পাচ্ছে।' : ' আজই রক্তদাতা হিসেবে নিবন্ধন করুন।'}
            </p>
            <div className="flex flex-wrap gap-4 pt-4">
              <Link to="/requests" className="px-8 py-4 bg-white text-red-600 rounded-3xl font-black text-lg hover:bg-gray-100 transition-all shadow-xl">
                রক্তের অনুরোধ করুন
              </Link>
              <Link to="/search" className="px-8 py-4 bg-red-700/50 text-white border-2 border-red-500 rounded-3xl font-black text-lg hover:bg-red-700/80 transition-all">
                রক্তদাতা খুঁজুন
              </Link>
            </div>
          </div>

          <div className="bg-white/10 backdrop-blur-md rounded-[40px] p-8 space-y-6 border border-white/20">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <div className={cn("p-4 rounded-3xl shadow-lg", badge.bg, badge.color)}>
                  <badge.icon className="w-8 h-8" />
                </div>
                <div>
                  <p className="text-red-200 text-xs font-black uppercase tracking-widest">আপনার লেভেল</p>
                  <p className="text-xl font-black">{badge.label}</p>
                </div>
              </div>
              <div className="text-right">
                <p className="text-red-200 text-xs font-black uppercase tracking-widest">পয়েন্ট</p>
                <p className="text-3xl font-black">{profile?.points || 0}</p>
              </div>
            </div>
            
            <div className="grid grid-cols-2 gap-4">
               <div className="bg-white/95 rounded-3xl p-4 text-gray-900 shadow-xl">
                 <div className="flex items-center justify-between mb-1">
                    <TrendingUp className="w-4 h-4 text-green-500" />
                    <span className="text-[10px] font-black uppercase text-gray-400">রক্তদান</span>
                 </div>
                 <p className="text-2xl font-black">{profile?.donationsCount || 0} বার</p>
               </div>
               <div className="bg-white/95 rounded-3xl p-4 text-gray-900 shadow-xl">
                  <div className="flex items-center justify-between mb-1">
                    <Star className="w-4 h-4 text-yellow-500 fill-current" />
                    <span className="text-[10px] font-black uppercase text-gray-400">রেটিং</span>
                  </div>
                  <p className="text-2xl font-black">{profile?.ratingAverage?.toFixed(1) || '0.0'}</p>
               </div>
            </div>
          </div>
        </div>
        <Heart className="absolute -right-16 -bottom-16 w-80 h-80 text-red-500 opacity-20 transform -rotate-12 fill-current" />
      </section>

      {/* Personal Alerts & Eligibility */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
        <div className="lg:col-span-2 space-y-6">
          <div className="flex items-center justify-between">
            <h2 className="text-2xl font-black text-gray-900 flex items-center gap-3">
              <Bell className="w-7 h-7 text-red-600" /> আপনার জন্য বিশেষ অনুরোধ
            </h2>
            {matchingRequests.length > 0 && (
               <span className="bg-red-600 text-white text-[10px] font-black px-3 py-1 rounded-full animate-pulse uppercase tracking-widest">
                 {matchingRequests.length} টি নতুন
               </span>
            )}
          </div>

          {matchingRequests.length > 0 ? (
            <div className="bg-red-50/50 p-6 rounded-[40px] border border-red-100 space-y-4">
              {matchingRequests.slice(0, 2).map(req => (
                <Link 
                  key={req.id} 
                  to="/requests" 
                  className="block bg-white p-6 rounded-3xl shadow-sm border border-red-100 hover:shadow-xl transition-all group"
                >
                  <div className="flex items-center gap-5">
                    <div className="w-14 h-14 bg-red-600 text-white rounded-2xl flex items-center justify-center font-black text-xl shadow-lg">
                      {req.bloodGroup}
                    </div>
                    <div className="flex-1">
                      <p className="font-black text-gray-900 group-hover:text-red-600 transition-colors">{req.hospitalName}</p>
                      <p className="text-sm text-gray-500 font-bold">{req.location.upazila}, {req.location.district}</p>
                    </div>
                    <ExternalLink className="w-5 h-5 text-gray-300 group-hover:text-red-600" />
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div className="bg-gray-50 p-8 rounded-[40px] text-center border border-dashed border-gray-200">
              <p className="text-gray-400 font-bold">আপনার এলাকার জন্য নতুন কোনো অনুরোধ নেই</p>
            </div>
          )}
        </div>

        <div className="space-y-6">
           <h2 className="text-2xl font-black text-gray-900 flex items-center gap-3">
            <ShieldCheck className="w-7 h-7 text-green-600" /> রক্তদান যোগ্যতা
          </h2>
          <div className={cn(
            "p-8 rounded-[40px] shadow-2xl relative overflow-hidden group",
            isEligible ? "bg-green-600 text-white shadow-green-200" : "bg-orange-500 text-white shadow-orange-200"
          )}>
            <div className="relative z-10 space-y-4 text-center">
              <div className="w-16 h-16 bg-white/20 rounded-2xl flex items-center justify-center mx-auto backdrop-blur-md">
                <Activity className="w-8 h-8" />
              </div>
              <div>
                <p className="text-white/70 text-[10px] font-black uppercase tracking-widest mb-1">অবস্থা</p>
                <p className="text-2xl font-black">{isEligible ? 'আপনি এখন রক্তদান করতে পারবেন' : 'পরবর্তী রক্তদানের সময়'}</p>
              </div>
              {!isEligible && nextEligibleDate && (
                <p className="text-orange-100 font-black text-lg bg-orange-600/30 py-2 px-4 rounded-xl inline-block">
                  {format(nextEligibleDate, 'dd MMMM, yyyy', { locale: bn })}
                </p>
              )}
              <button 
                onClick={() => setIsCheckerOpen(true)}
                className="w-full mt-4 py-4 bg-white text-gray-900 rounded-[24px] font-black text-sm uppercase tracking-widest hover:bg-gray-100 transition-all shadow-xl shadow-black/10"
              >
                যোগ্যতা যাচাই করুন
              </button>
            </div>
            <Heart className="absolute -right-8 -bottom-8 w-40 h-40 text-white/10 fill-current group-hover:scale-125 transition-transform" />
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <div className="bg-white p-8 rounded-[40px] border border-gray-100 shadow-xl flex items-center gap-6 group hover:scale-105 transition-all">
          <div className="w-16 h-16 bg-red-50 rounded-2xl flex items-center justify-center text-red-600 shadow-inner group-hover:bg-red-600 group-hover:text-white transition-all">
            <Activity className="w-8 h-8" />
          </div>
          <div>
            <p className="text-sm text-gray-400 font-bold uppercase tracking-widest">সক্রিয় অনুরোধ</p>
            <p className="text-4xl font-black text-gray-900">{stats.active}</p>
          </div>
        </div>
        <div className="bg-white p-8 rounded-[40px] border border-gray-100 shadow-xl flex items-center gap-6 group hover:scale-105 transition-all">
          <div className="w-16 h-16 bg-orange-50 rounded-2xl flex items-center justify-center text-orange-600 shadow-inner group-hover:bg-orange-500 group-hover:text-white transition-all">
            <Activity className="w-8 h-8" />
          </div>
          <div>
            <p className="text-sm text-gray-400 font-bold uppercase tracking-widest">জরুরি অবস্থা</p>
            <p className="text-4xl font-black text-gray-900">{stats.critical}</p>
          </div>
        </div>
        <div className="bg-white p-8 rounded-[40px] border border-gray-100 shadow-xl flex items-center gap-6 group hover:scale-105 transition-all">
          <div className="w-16 h-16 bg-blue-50 rounded-2xl flex items-center justify-center text-blue-600 shadow-inner group-hover:bg-blue-600 group-hover:text-white transition-all">
            <User className="w-8 h-8" />
          </div>
          <div>
            <p className="text-sm text-gray-400 font-bold uppercase tracking-widest">মোট রক্তদাতা</p>
            <p className="text-4xl font-black text-gray-900">{stats.totalDonors}</p>
          </div>
        </div>
      </div>

      {/* Recent Requests */}
      <section className="space-y-6">
        <div className="flex items-center justify-between">
          <h2 className="text-2xl font-black text-gray-900">সাম্প্রতিক রক্তের অনুরোধ</h2>
          <Link to="/requests" className="text-red-600 text-sm font-black flex items-center gap-2 hover:underline bg-red-50 px-6 py-3 rounded-2xl hover:bg-red-100 transition-all">
            সবগুলো দেখুন <ExternalLink className="w-4 h-4" />
          </Link>
        </div>

        {loading ? (
          <div className="grid gap-6">
            {[1, 2, 3].map(i => (
              <div key={i} className="h-28 bg-white rounded-[40px] shimmer border border-gray-100" />
            ))}
          </div>
        ) : recentRequests.length > 0 ? (
          <div className="grid gap-6">
            {recentRequests.map((req) => (
              <motion.div
                key={req.id}
                layout
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="bg-white p-8 rounded-[40px] border border-gray-100 shadow-xl hover:shadow-2xl transition-all flex flex-col sm:flex-row items-center justify-between gap-6 group"
              >
                <div className="flex items-center gap-6 w-full sm:w-auto">
                  <div className={cn(
                    "w-20 h-20 rounded-3xl flex items-center justify-center font-black text-3xl shadow-lg shrink-0 transition-transform group-hover:scale-110",
                    req.urgency === 'Critical' ? "bg-red-600 text-white shadow-red-200" : "bg-red-50 text-red-600 shadow-inner"
                  )}>
                    {req.bloodGroup}
                  </div>
                  <div>
                    <h3 className="text-2xl font-black text-gray-900 group-hover:text-red-600 transition-colors">{req.hospitalName}</h3>
                    <div className="flex flex-wrap items-center gap-6 text-base text-gray-500 mt-2 font-bold">
                      <span className="flex items-center gap-2 bg-gray-50 px-3 py-1 rounded-xl"><MapPin className="w-4 h-4 text-red-400" /> {req.location.upazila}, {req.location.district}</span>
                      <span className="flex items-center gap-2"><Clock className="w-4 h-4 text-gray-400" /> {formatDistanceToNow(new Date(req.createdAt), { locale: bn })} আগে</span>
                    </div>
                  </div>
                </div>
                <div className="flex items-center gap-6 w-full sm:w-auto">
                   <div className="text-right hidden md:block">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">প্রয়োজন</p>
                      <p className="text-gray-900 font-black">{req.requiredDate}</p>
                   </div>
                   <span className={cn(
                     "px-6 py-3 rounded-2xl text-xs font-black uppercase tracking-widest shadow-sm",
                     req.urgency === 'Critical' ? "bg-red-100 text-red-700" : "bg-gray-100 text-gray-600"
                   )}>
                     {req.urgency === 'Critical' ? 'সুপার ইমারজেন্সি' : 'জরুরি'}
                   </span>
                   <Link to={`/requests`} className="p-4 bg-gray-50 rounded-3xl hover:bg-red-600 hover:text-white transition-all shadow-inner">
                      <ExternalLink className="w-7 h-7" />
                   </Link>
                </div>
              </motion.div>
            ))}
          </div>
        ) : (
          <div className="bg-white p-20 rounded-[40px] text-center border-2 border-dashed border-gray-200">
            <Heart className="w-20 h-20 text-gray-200 mx-auto mb-6" />
            <p className="text-gray-400 text-xl font-bold">বর্তমানে কোনো রক্তের অনুরোধ নেই</p>
          </div>
        )}
      </section>

      <EligibilityChecker isOpen={isCheckerOpen} onClose={() => setIsCheckerOpen(false)} />
    </div>
  );
}
