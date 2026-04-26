import React, { useState, useEffect, useMemo } from 'react';
import { collection, query, orderBy, onSnapshot } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { EmergencyResource } from '../types';
import { Activity, Phone, MapPin, Clock, Heart, Search, Filter, PhoneCall, Shield, Flame, Truck, Building2, Info, ExternalLink } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { EMERGENCY_SERVICES, EmergencyService } from '../constants/emergencyData';

type CombinedType = EmergencyResource['type'] | EmergencyService['type'];
type CombinedItem = (EmergencyResource | EmergencyService) & { isStatic?: boolean };

export function EmergencyContact() {
  const [dbResources, setDbResources] = useState<EmergencyResource[]>([]);
  const [loading, setLoading] = useState(true);
  const [filter, setFilter] = useState<'All' | CombinedType>('All');
  const [searchTerm, setSearchTerm] = useState('');

  useEffect(() => {
    const q = query(collection(db, 'resources'), orderBy('name', 'asc'));
    const unsubscribe = onSnapshot(q, (snapshot) => {
      setDbResources(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as EmergencyResource)));
      setLoading(false);
    });
    return unsubscribe;
  }, []);

  const combinedItems = useMemo(() => {
    const staticItems = EMERGENCY_SERVICES.map(item => ({
      ...item,
      location: `${item.district}, ${item.division}`,
      contactNumber: item.phone,
      isStatic: true
    }));
    
    // Normalize DB resources to match common fields if needed
    const normalizedDb = dbResources.map(res => ({
      ...res,
      isStatic: false
    }));

    return [...normalizedDb, ...staticItems];
  }, [dbResources]);

  const filteredItems = combinedItems.filter(item => {
    const matchesFilter = filter === 'All' || item.type === filter;
    const matchesSearch = item.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                         item.location.toLowerCase().includes(searchTerm.toLowerCase());
    return matchesFilter && matchesSearch;
  });

  const getIcon = (type: CombinedType) => {
    switch (type) {
      case 'Blood Bank': return <Heart className="w-8 h-8 fill-current" />;
      case 'Ambulance': return <Truck className="w-8 h-8" />;
      case 'Fire Service': return <Flame className="w-8 h-8" />;
      case 'Police': return <Shield className="w-8 h-8" />;
      default: return <Info className="w-8 h-8" />;
    }
  };

  const getColorClass = (type: CombinedType) => {
    switch (type) {
      case 'Blood Bank': return "bg-red-50 dark:bg-red-900/10 text-red-600";
      case 'Ambulance': return "bg-blue-50 dark:bg-blue-900/10 text-blue-600";
      case 'Fire Service': return "bg-orange-50 dark:bg-orange-900/10 text-orange-600";
      case 'Police': return "bg-indigo-50 dark:bg-indigo-900/10 text-indigo-600";
      default: return "bg-gray-50 dark:bg-gray-800 text-gray-600";
    }
  };

  const getLabel = (type: CombinedType) => {
    switch (type) {
      case 'Blood Bank': return 'ব্লাড ব্যাংক';
      case 'Ambulance': return 'অ্যাম্বুলেন্স';
      case 'Fire Service': return 'ফায়ার সার্ভিস';
      case 'Police': return 'পুলিশ/জরুরি';
      default: return 'অন্যান্য';
    }
  };

  return (
    <div className="space-y-10 animate-fade-in max-w-7xl mx-auto pb-20">
      <div className="flex flex-col md:flex-row md:items-end justify-between gap-8">
        <div className="space-y-4">
           <div className="inline-flex items-center gap-2 px-4 py-2 bg-red-50 dark:bg-red-900/10 text-red-600 rounded-full font-black text-xs uppercase tracking-widest transition-colors">
              <Activity className="w-4 h-4" /> জরুরি ডিরেক্টরি
           </div>
           <h1 className="text-4xl md:text-5xl font-black text-gray-900 dark:text-white transition-colors tracking-tight">সব জরুরি যোগাযোগ নম্বর</h1>
           <p className="text-gray-500 dark:text-gray-400 font-bold text-lg max-w-2xl">ব্লাড ব্যাংক, অ্যাম্বুলেন্স, ফায়ার সার্ভিস এবং পুলিশের তথ্য এক জায়গায়।</p>
        </div>

        <div className="flex flex-wrap gap-2 bg-white dark:bg-gray-900 p-2 rounded-[32px] shadow-xl border border-gray-100 dark:border-gray-800 transition-all">
          {(['All', 'Blood Bank', 'Ambulance', 'Fire Service', 'Police'] as const).map((t) => (
            <button
              key={t}
              onClick={() => setFilter(t)}
              className={cn(
                "px-5 py-3 rounded-2xl font-black text-[10px] transition-all uppercase tracking-widest",
                filter === t 
                  ? "bg-brand text-white shadow-lg" 
                  : "text-gray-400 hover:text-gray-700 dark:hover:text-gray-300"
              )}
            >
              {t === 'All' ? 'সব' : getLabel(t as CombinedType)}
            </button>
          ))}
        </div>
      </div>

      {/* 999 Special Card */}
      <div className="bg-red-600 rounded-[50px] p-8 md:p-12 text-white shadow-2xl relative overflow-hidden group">
        <div className="relative z-10 flex flex-col md:flex-row items-center gap-10">
          <div className="w-32 h-32 bg-white rounded-full flex items-center justify-center blood-pulse shadow-2xl shrink-0">
             <span className="text-red-600 text-5xl font-black">999</span>
          </div>
          <div className="text-center md:text-left space-y-4">
             <h2 className="text-3xl md:text-5xl font-black">ন্যাশনাল ইমার্জেন্সি সার্ভিস</h2>
             <p className="text-red-100 text-lg font-bold max-w-xl">
               যেকোনো জরুরি প্রয়োজনে (পুলিশ, ফায়ার সার্ভিস বা অ্যাম্বুলেন্স) সরাসরি ৯৯৯ নম্বরে টোল ফ্রি কল করুন। এটি ২৪ ঘণ্টা খোলা থাকে।
             </p>
             <div className="flex flex-wrap justify-center md:justify-start gap-4 pt-2">
                <a href="tel:999" className="px-10 py-5 bg-white text-red-600 rounded-[28px] font-black text-xl hover:scale-105 transition-transform flex items-center gap-3 active:scale-95 shadow-xl">
                   <Phone className="w-6 h-6" /> কল করুন
                </a>
             </div>
          </div>
        </div>
        <Shield className="absolute -bottom-20 -right-20 w-80 h-80 text-white/10 rotate-12" />
      </div>

      <div className="relative">
        <Search className="absolute left-6 top-1/2 -translate-y-1/2 text-brand w-6 h-6" />
        <input
          type="text"
          placeholder="হাসপাতাল, অ্যাম্বুলেন্স বা এলাকা দিয়ে খুঁজুন..."
          value={searchTerm}
          onChange={(e) => setSearchTerm(e.target.value)}
          className="w-full pl-16 pr-8 py-8 bg-white dark:bg-gray-900 rounded-[40px] border-none shadow-2xl focus:ring-4 focus:ring-red-500/20 outline-none font-black text-gray-700 dark:text-gray-300 text-xl transition-all"
        />
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-8">
        <AnimatePresence mode="popLayout">
          {loading && dbResources.length === 0 ? (
             Array.from({ length: 9 }).map((_, i) => (
               <div key={i} className="h-72 bg-gray-100 dark:bg-gray-800 animate-pulse rounded-[40px]" />
             ))
          ) : filteredItems.length > 0 ? (
            filteredItems.map((res, idx) => (
              <motion.div
                key={res.id}
                layout
                initial={{ opacity: 0, y: 20 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.95 }}
                transition={{ delay: Math.min(idx * 0.03, 0.3) }}
                className="bg-white dark:bg-gray-900 rounded-[40px] p-8 shadow-xl border border-gray-100 dark:border-gray-800 flex flex-col justify-between group hover:shadow-2xl transition-all border-b-4 border-b-transparent hover:border-b-brand"
              >
                <div className="space-y-6">
                  <div className="flex justify-between items-start">
                    <div className={cn(
                      "w-16 h-16 rounded-3xl flex items-center justify-center shadow-lg group-hover:scale-110 transition-transform",
                      getColorClass(res.type)
                    )}>
                      {getIcon(res.type)}
                    </div>
                    {((res as EmergencyResource).available24h || (res as EmergencyService).availability === '২৪ ঘণ্টা') && (
                      <div className="flex items-center gap-1 bg-green-50 dark:bg-green-900/20 text-green-600 px-3 py-1.5 rounded-xl text-[10px] font-black uppercase tracking-wider">
                         <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" /> 24/7
                      </div>
                    )}
                  </div>

                  <div className="space-y-2">
                    <p className="text-[10px] font-black uppercase tracking-[3px] text-gray-400">{getLabel(res.type)}</p>
                    <h3 className="text-xl font-black text-gray-900 dark:text-white leading-tight line-clamp-2 min-h-[3.5rem]">{res.name}</h3>
                    <div className="flex items-start gap-2 text-gray-500 dark:text-gray-400 font-bold text-sm pt-2">
                      <MapPin className="w-4 h-4 mt-1 flex-shrink-0 text-brand" />
                      <span className="line-clamp-2">{res.location}</span>
                    </div>
                  </div>
                </div>

                <div className="pt-6 border-t border-gray-50 dark:border-gray-800 mt-6 space-y-3">
                   <a 
                     href={`tel:${res.contactNumber || (res as EmergencyService).phone}`}
                     className="flex items-center justify-center gap-3 w-full py-5 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white rounded-2xl font-black hover:bg-brand hover:text-white transition-all shadow-sm group-hover:shadow-lg active:scale-95"
                   >
                     <PhoneCall className="w-5 h-5" /> {res.contactNumber || (res as EmergencyService).phone}
                   </a>
                </div>
              </motion.div>
            ))
          ) : (
            <div className="col-span-full py-32 text-center space-y-6">
               <Activity className="w-20 h-20 text-gray-100 dark:text-gray-800 mx-auto" />
               <p className="text-gray-400 font-black text-xl uppercase tracking-widest">বর্তমানে কোনো তথ্য পাওয়া যায়নি</p>
            </div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
