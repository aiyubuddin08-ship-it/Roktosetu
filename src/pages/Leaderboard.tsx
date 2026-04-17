import React, { useEffect, useState } from 'react';
import { collection, query, orderBy, limit, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { UserProfile } from '../types';
import { Award, Star, TrendingUp, Medal } from 'lucide-react';
import { cn } from '../lib/utils';
import { motion } from 'motion/react';

const getBadge = (count: number = 0) => {
  if (count >= 10) return { icon: Award, label: 'অভিভাবক দূত', color: 'text-purple-600', bg: 'bg-purple-50' };
  if (count >= 5) return { icon: Award, label: 'জীবন রক্ষাকারী', color: 'text-blue-600', bg: 'bg-blue-50' };
  if (count >= 1) return { icon: Award, label: 'রক্তদান বীর', color: 'text-green-600', bg: 'bg-green-50' };
  return { icon: Award, label: 'নতুন সদস্য', color: 'text-gray-400', bg: 'bg-gray-50' };
};

export function Leaderboard() {
  const [topDonors, setTopDonors] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchLeaderboard = async () => {
      const q = query(
        collection(db, 'users'),
        orderBy('points', 'desc'),
        limit(20)
      );
      const snap = await getDocs(q);
      setTopDonors(snap.docs.map(doc => doc.data() as UserProfile));
      setLoading(false);
    };
    fetchLeaderboard();
  }, []);

  return (
    <div className="space-y-10 pb-20 animate-fade-in">
      <div className="text-center space-y-4">
        <h1 className="text-4xl font-black text-gray-900 tracking-tight">সেরা রক্তদাতা তালিকা</h1>
        <p className="text-gray-500 font-bold text-lg max-w-2xl mx-auto">
          যাঁরা নিজেদের রক্তের মাধ্যমে অন্যের মুখে হাসি ফুটিয়েছেন, তাঁদের জানাই বিনম্র শ্রদ্ধা।
        </p>
      </div>

      {loading ? (
        <div className="grid gap-6">
          {[1, 2, 3, 4, 5].map(i => (
            <div key={i} className="h-24 bg-white rounded-[40px] shimmer border border-gray-100" />
          ))}
        </div>
      ) : (
        <div className="bg-white rounded-[40px] border border-gray-100 shadow-2xl overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full">
              <thead>
                <tr className="bg-gray-50/50 border-b border-gray-100 italic font-bold text-gray-400 text-sm uppercase tracking-widest text-left">
                  <th className="px-8 py-5">অবস্থান</th>
                  <th className="px-8 py-5">ডোনার</th>
                  <th className="px-8 py-5">রক্তদান</th>
                  <th className="px-8 py-5">রেটিং</th>
                  <th className="px-8 py-5 text-right">পয়েন্ট</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-50">
                {topDonors.map((donor, index) => {
                  const badge = getBadge(donor.donationsCount);
                  return (
                    <motion.tr
                      initial={{ opacity: 0, x: -20 }}
                      animate={{ opacity: 1, x: 0 }}
                      transition={{ delay: index * 0.05 }}
                      key={donor.uid}
                      className="group hover:bg-red-50/30 transition-colors"
                    >
                      <td className="px-8 py-6">
                        <div className={cn(
                          "w-12 h-12 rounded-2xl flex items-center justify-center font-black text-xl",
                          index === 0 ? "bg-yellow-100 text-yellow-700 shadow-lg shadow-yellow-100" :
                          index === 1 ? "bg-gray-100 text-gray-500 shadow-lg shadow-gray-100" :
                          index === 2 ? "bg-orange-100 text-orange-700 shadow-lg shadow-orange-100" :
                          "text-gray-400"
                        )}>
                          {index < 3 ? <Medal className="w-6 h-6" /> : index + 1}
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-4">
                          {donor.photoURL ? (
                            <img src={donor.photoURL} alt="" className="w-12 h-12 rounded-2xl object-cover shadow-md" referrerPolicy="no-referrer" />
                          ) : (
                            <div className="w-12 h-12 bg-red-50 rounded-2xl flex items-center justify-center text-red-600 font-black">
                              {donor.bloodGroup}
                            </div>
                          )}
                          <div>
                            <p className="font-black text-gray-900 group-hover:text-red-600 transition-colors">{donor.displayName}</p>
                            <div className="flex items-center gap-2 mt-0.5">
                              <span className={cn("text-[10px] font-black uppercase px-2 py-0.5 rounded-md", badge.bg, badge.color)}>
                                {badge.label}
                              </span>
                            </div>
                          </div>
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-2 font-black text-gray-700">
                          <TrendingUp className="w-4 h-4 text-green-500" /> {donor.donationsCount || 0} বার
                        </div>
                      </td>
                      <td className="px-8 py-6">
                        <div className="flex items-center gap-2 font-black text-gray-700">
                          <Star className="w-4 h-4 text-yellow-500 fill-current" /> {donor.ratingAverage?.toFixed(1) || '0.0'}
                        </div>
                      </td>
                      <td className="px-8 py-6 text-right">
                        <span className="text-2xl font-black text-red-600">{donor.points || 0}</span>
                      </td>
                    </motion.tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      )}
    </div>
  );
}
