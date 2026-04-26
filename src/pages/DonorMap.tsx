import React, { useState, useEffect, useMemo } from 'react';
import { collection, query, where, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { DIVISIONS, Division, District } from '../constants/locations';
import { MapPin, Users, ChevronRight, Map as MapIcon, Layers, BarChart3, Award, Flame, Truck, Shield } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { EMERGENCY_SERVICES } from '../constants/emergencyData';

interface LocationStats {
  [key: string]: number;
}

export function DonorMap() {
  const [divisionStats, setDivisionStats] = useState<LocationStats>({});
  const [districtStats, setDistrictStats] = useState<LocationStats>({});
  const [upazilaStats, setUpazilaStats] = useState<LocationStats>({});
  const [loading, setLoading] = useState(true);
  
  const [selectedDivision, setSelectedDivision] = useState<Division | null>(null);
  const [selectedDistrict, setSelectedDistrict] = useState<District | null>(null);

  // Calculate emergency services stats
  const serviceStats = useMemo(() => {
    const dStats: LocationStats = {};
    const disStats: LocationStats = {};
    
    EMERGENCY_SERVICES.forEach(service => {
      if (service.division) {
        dStats[service.division] = (dStats[service.division] || 0) + 1;
      }
      if (service.district) {
        disStats[service.district] = (disStats[service.district] || 0) + 1;
      }
    });
    
    return { division: dStats, district: disStats };
  }, []);

  useEffect(() => {
    const fetchStats = async () => {
      setLoading(true);
      try {
        const q = query(collection(db, 'users'), where('isDonor', '==', true));
        const snapshot = await getDocs(q);
        
        const dStats: LocationStats = {};
        const disStats: LocationStats = {};
        const uStats: LocationStats = {};

        snapshot.forEach(doc => {
          const data = doc.data();
          if (data.privacySettings?.hideLocation) return;
          
          const { division, district, upazila } = data.location || {};
          
          if (division) dStats[division] = (dStats[division] || 0) + 1;
          if (district) disStats[district] = (disStats[district] || 0) + 1;
          if (upazila) uStats[upazila] = (uStats[upazila] || 0) + 1;
        });

        setDivisionStats(dStats);
        setDistrictStats(disStats);
        setUpazilaStats(uStats);
      } catch (error) {
        console.error("Error fetching map stats:", error);
      } finally {
        setLoading(false);
      }
    };

    fetchStats();
  }, []);

  const totalDonors = Object.values(divisionStats).reduce((a, b) => a + b, 0);

  const topDistricts = Object.entries(districtStats)
    .sort(([, a], [, b]) => b - a)
    .slice(0, 5);

  return (
    <div className="space-y-10 pb-20">
      <section className="text-center space-y-4 max-w-2xl mx-auto">
        <motion.div 
          initial={{ scale: 0.8, opacity: 0 }}
          animate={{ scale: 1, opacity: 1 }}
          className="w-20 h-20 bg-red-100 rounded-full flex items-center justify-center mx-auto mb-4"
        >
          <MapIcon className="w-10 h-10 text-red-600" />
        </motion.div>
        <h1 className="text-4xl font-black text-gray-900 dark:text-white leading-tight">ডোনার ম্যাপ এক্সপ্লোরার</h1>
        <p className="text-gray-500 dark:text-gray-400 text-lg font-medium">বাংলাদেশের প্রতিটি কোণায় আমাদের গর্বিত রক্তদাতাদের অবস্থান জানুন।</p>
      </section>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-10">
        {/* Sidebar / List */}
        <div className="lg:col-span-4 space-y-6">
          <div className="bg-white dark:bg-gray-900 rounded-[40px] p-8 border border-gray-100 dark:border-gray-800 shadow-2xl">
            <div className="flex items-center justify-between mb-8">
              <h2 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-3">
                <Layers className="w-6 h-6 text-red-600" /> বিভাগসমূহ
              </h2>
              <span className="bg-red-50 dark:bg-red-900/10 text-red-600 px-4 py-1 rounded-full text-xs font-black">
                সর্বমোট: {totalDonors}
              </span>
            </div>

            <div className="space-y-3">
              {DIVISIONS.map((div) => (
                <button
                  key={div.id}
                  onClick={() => {
                    setSelectedDivision(div);
                    setSelectedDistrict(null);
                  }}
                  className={cn(
                    "w-full flex items-center justify-between p-5 rounded-3xl transition-all border group",
                    selectedDivision?.id === div.id
                      ? "bg-red-600 border-red-600 text-white shadow-xl shadow-red-100 dark:shadow-none translate-x-2"
                      : "bg-gray-50 dark:bg-gray-800 border-transparent text-gray-600 dark:text-gray-400 hover:bg-white dark:hover:bg-gray-700 hover:border-red-100 dark:hover:border-gray-600"
                  )}
                >
                  <div className="flex items-center gap-4">
                    <div className={cn(
                      "w-10 h-10 rounded-2xl flex items-center justify-center font-black",
                      selectedDivision?.id === div.id ? "bg-white/20" : "bg-white dark:bg-gray-900 shadow-sm"
                    )}>
                      {div.bnName[0]}
                    </div>
                    <div className="text-left">
                      <p className="font-black leading-none">{div.bnName}</p>
                      <p className={cn("text-xs mt-1 font-bold", selectedDivision?.id === div.id ? "text-red-100" : "text-gray-400")}>
                        {div.name}
                      </p>
                    </div>
                  </div>
                  <div className="flex items-center gap-3">
                    <span className={cn(
                      "font-black text-lg",
                      selectedDivision?.id === div.id ? "text-white" : "text-gray-900 dark:text-gray-300"
                    )}>
                      {divisionStats[div.name] || 0}
                    </span>
                    <ChevronRight className={cn("w-4 h-4 transition-transform", selectedDivision?.id === div.id ? "rotate-90" : "group-hover:translate-x-1")} />
                  </div>
                </button>
              ))}
            </div>
          </div>

          {/* Top Districts Summary */}
          <div className="bg-gray-900 rounded-[40px] p-8 text-white shadow-2xl overflow-hidden relative">
            <h3 className="text-lg font-black mb-6 flex items-center gap-3 relative z-10">
              <Award className="w-6 h-6 text-yellow-500" /> শীর্ষ ৫ জেলা
            </h3>
            <div className="space-y-4 relative z-10">
              {topDistricts.map(([name, count], idx) => (
                <div key={name} className="flex items-center justify-between">
                  <div className="flex items-center gap-3">
                    <span className="text-gray-500 font-black">#0{idx + 1}</span>
                    <span className="font-bold">{name}</span>
                  </div>
                  <span className="bg-white/10 px-3 py-1 rounded-xl font-black text-xs">
                    {count} ডোনার
                  </span>
                </div>
              ))}
              {topDistricts.length === 0 && (
                <p className="text-gray-500 text-sm italic">এখনো ডাটা পাওয়া যায়নি</p>
              )}
            </div>
            <BarChart3 className="absolute -bottom-6 -right-6 w-32 h-32 text-white/5 rotate-12" />
          </div>
        </div>

        {/* Main Explorer Area */}
        <div className="lg:col-span-8">
          <AnimatePresence mode="wait">
            {!selectedDivision ? (
              <motion.div
                key="empty"
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95 }}
                className="h-full min-h-[500px] bg-red-50/50 dark:bg-red-900/5 rounded-[60px] border-4 border-dashed border-red-100 dark:border-red-900/10 flex flex-col items-center justify-center p-20 text-center"
              >
                 <div className="w-32 h-32 bg-white dark:bg-gray-900 rounded-full flex items-center justify-center shadow-2xl mb-10 blood-pulse">
                    <MapPin className="w-16 h-16 text-red-600" />
                 </div>
                 <h2 className="text-3xl font-black text-gray-900 dark:text-gray-100 mb-4">বিস্তারিত তথ্য দেখতে বিভাগ নির্বাচন করুন</h2>
                 <p className="text-gray-500 dark:text-gray-400 max-w-sm font-bold">বামদিকের তালিকা থেকে যেকোনো বিভাগ নির্বাচন করলে সেই বিভাগের জেলা ও উপজেলা ভিত্তিক ডোনারের সংখ্যা দেখা যাবে।</p>
              </motion.div>
            ) : (
              <motion.div
                key={selectedDivision.id}
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                className="space-y-8"
              >
                {/* Division Header */}
                <div className="bg-red-600 rounded-[40px] p-10 text-white shadow-2xl shadow-red-200 dark:shadow-none relative overflow-hidden">
                  <div className="relative z-10 flex flex-col md:flex-row md:items-center justify-between gap-6">
                    <div className="space-y-2">
                       <p className="text-red-100 font-black uppercase tracking-widest text-sm">এক্সপ্লোর করছেন</p>
                       <h2 className="text-5xl font-black">{selectedDivision.bnName} বিভাগ</h2>
                       <div className="flex flex-wrap gap-4 mt-2">
                          <p className="font-bold opacity-80">{selectedDivision.districts.length}টি জেলা | {Object.values(districtStats).reduce((a,b) => a+b, 0)} জন ডোনার</p>
                          <div className="flex items-center gap-2 bg-black/10 px-3 py-1 rounded-full text-xs font-black backdrop-blur-sm">
                             <Flame className="w-3.5 h-3.5" /> {serviceStats.division[selectedDivision.name] || 0}টি ফায়ার স্টেশন
                          </div>
                       </div>
                    </div>
                    <div className="bg-white/20 backdrop-blur-xl p-6 rounded-3xl border border-white/30 text-center">
                       <p className="text-xs font-black uppercase tracking-wider mb-1">মোট ডোনার</p>
                       <p className="text-4xl font-black leading-none">{divisionStats[selectedDivision.name] || 0}</p>
                    </div>
                  </div>
                  <BarChart3 className="absolute -bottom-10 -right-10 w-64 h-64 text-white opacity-10" />
                </div>

                {/* District Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                  {selectedDivision.districts.map((dist) => (
                    <motion.div
                      key={dist.id}
                      layoutId={dist.id}
                      onClick={() => setSelectedDistrict(dist === selectedDistrict ? null : dist)}
                      className={cn(
                        "bg-white dark:bg-gray-900 p-8 rounded-[40px] border transition-all cursor-pointer group hover:shadow-2xl",
                        selectedDistrict?.id === dist.id 
                          ? "border-red-600 ring-4 ring-red-50 dark:ring-red-900/10" 
                          : "border-gray-100 dark:border-gray-800 hover:border-red-200 dark:hover:border-red-900/30"
                      )}
                    >
                      <div className="flex items-start justify-between mb-6">
                        <div className="w-14 h-14 bg-gray-50 dark:bg-gray-800 rounded-2xl flex items-center justify-center shadow-inner group-hover:scale-110 transition-transform">
                          <MapPin className={cn("w-7 h-7", selectedDistrict?.id === dist.id ? "text-red-600" : "text-gray-400")} />
                        </div>
                        <div className="text-right">
                          <p className="text-xs font-black text-gray-400 uppercase tracking-widest mb-1">ডোনার সংখ্যা</p>
                          <p className="text-3xl font-black text-gray-900 dark:text-white leading-none">
                            {districtStats[dist.name] || 0}
                          </p>
                        </div>
                      </div>
                      <h3 className="text-2xl font-black text-gray-900 dark:text-white mb-2">{dist.bnName}</h3>
                      <p className="text-gray-400 font-bold text-sm mb-4">{dist.upazilas.length}টি উপজেলা</p>
                      
                      {/* Emergency Services Count for District */}
                      <div className="flex gap-2 mb-6">
                        <div className="bg-orange-50 dark:bg-orange-900/10 px-3 py-1.5 rounded-xl flex items-center gap-2 border border-orange-100 dark:border-orange-900/20">
                           <Flame className="w-3.5 h-3.5 text-orange-600" />
                           <span className="text-orange-700 dark:text-orange-400 text-xs font-black">{serviceStats.district[dist.name] || 0}টি ফায়ার স্টেশন</span>
                        </div>
                        <div className="bg-blue-50 dark:bg-blue-900/10 px-3 py-1.5 rounded-xl flex items-center gap-2 border border-blue-100 dark:border-blue-900/20">
                           <Truck className="w-3.5 h-3.5 text-blue-600" />
                           <span className="text-blue-700 dark:text-blue-400 text-xs font-black">অ্যাম্বুলেন্স</span>
                        </div>
                      </div>

                      <div className="flex items-center gap-2 text-red-600 font-black text-xs uppercase tracking-widest">
                         {selectedDistrict?.id === dist.id ? 'উপজেলাসমূহ দেখুন' : 'বিস্তারিত দেখতে ক্লিক করুন'}
                         <ChevronRight className={cn("w-4 h-4 transition-transform", selectedDistrict?.id === dist.id && "rotate-90")} />
                      </div>

                      {/* Upazila List - Drilldown */}
                      <AnimatePresence>
                        {selectedDistrict?.id === dist.id && (
                          <motion.div
                            initial={{ height: 0, opacity: 0 }}
                            animate={{ height: 'auto', opacity: 1 }}
                            exit={{ height: 0, opacity: 0 }}
                            className="overflow-hidden pt-6 mt-6 border-t border-dashed border-gray-100 dark:border-gray-800"
                          >
                             <div className="grid grid-cols-2 gap-3">
                               {dist.upazilas.map(u => (
                                 <div key={u.id} className="bg-gray-50 dark:bg-gray-800/50 p-4 rounded-2xl flex items-center justify-between border border-transparent hover:border-red-100 dark:hover:border-red-900/20 transition-all">
                                   <span className="font-bold text-gray-600 dark:text-gray-400 text-sm">{u.bnName}</span>
                                   <span className="bg-white dark:bg-gray-900 px-2 py-1 rounded-lg text-xs font-black text-red-600 shadow-sm border border-gray-100 dark:border-gray-800">
                                      {upazilaStats[u.name] || 0}
                                   </span>
                                 </div>
                               ))}
                             </div>
                          </motion.div>
                        )}
                      </AnimatePresence>
                    </motion.div>
                  ))}
                </div>
              </motion.div>
            )}
          </AnimatePresence>
        </div>
      </div>
    </div>
  );
}
