import React, { useState, useEffect } from 'react';
import { collection, query, where, onSnapshot, getDocs, addDoc, Timestamp, doc, setDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { useNavigate } from 'react-router-dom';
import { UserProfile, BloodGroup, OperationType } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Search, MapPin, Heart, Phone, Filter, Users, LogOut } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { DIVISIONS, Division, District } from '../constants/locations';

const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export function DonorSearch() {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [donors, setDonors] = useState<UserProfile[]>([]);
  const [loading, setLoading] = useState(false);
  const [hasSearched, setHasSearched] = useState(false);

  const [bloodGroup, setBloodGroup] = useState<BloodGroup | ''>('');
  const [division, setDivision] = useState('');
  const [district, setDistrict] = useState('');
  const [upazila, setUpazila] = useState('');

  const [districts, setDistricts] = useState<District[]>([]);
  const [upazilas, setUpazilas] = useState<{ id: string; name: string; bnName: string }[]>([]);

  useEffect(() => {
    const selectedDivision = DIVISIONS.find(d => d.name === division);
    setDistricts(selectedDivision ? selectedDivision.districts : []);
    setDistrict('');
    setUpazila('');
  }, [division]);

  useEffect(() => {
    const selectedDistrict = districts.find(d => d.name === district);
    setUpazilas(selectedDistrict ? selectedDistrict.upazilas : []);
    setUpazila('');
  }, [district, districts]);

  const handleSearch = () => {
    setLoading(true);
    setHasSearched(true);

    let q = query(collection(db, 'users'), where('isDonor', '==', true));

    if (bloodGroup) {
      q = query(q, where('bloodGroup', '==', bloodGroup));
    }
    
    if (division) {
      q = query(q, where('location.division', '==', division));
    }
    if (district) {
      q = query(q, where('location.district', '==', district));
    }
    if (upazila) {
      q = query(q, where('location.upazila', '==', upazila));
    }

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const data = snapshot.docs
        .map(doc => doc.data() as UserProfile)
        .filter(d => d.uid !== user?.uid);
      setDonors(data);
      setLoading(false);
    }, (error) => {
      handleFirestoreError(error, OperationType.LIST, 'users');
      setLoading(false);
    });

    return unsubscribe;
  };

  return (
    <div className="max-w-4xl mx-auto pb-32 -mt-8 -mx-4 sm:-mx-6 lg:-mx-8 animate-fade-in bg-gray-50 dark:bg-gray-950 min-h-screen">
       {/* Header Section */}
       <section className="bg-brand pt-16 pb-8 px-6 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full -mr-32 -mt-32 blur-3xl" />
          <div className="max-w-xl mx-auto relative z-10">
             <div className="flex items-center gap-4 mb-4">
                <button onClick={() => navigate(-1)} className="p-2 -ml-2 text-white">
                   <LogOut className="w-6 h-6 rotate-180" />
                </button>
                <h1 className="text-2xl font-black text-white">রক্তদাতা খুঁজুন</h1>
             </div>
             <p className="text-white/80 font-medium text-sm">নিকটস্থ রক্তদাতাদের সাথে যোগাযোগ করুন এবং সঠিক সময়ে রক্ত সংগ্রহ নিশ্চিত করুন।</p>
          </div>
       </section>

       <div className="max-w-xl mx-auto px-6 -mt-6">
          {/* Search Filters Card */}
          <div className="bg-white dark:bg-gray-900 rounded-[40px] shadow-2xl card-shadow border border-gray-50 dark:border-gray-800 p-8 space-y-8 relative z-20">
             {/* Blood Group Selection */}
             <div className="space-y-4">
                <label className="text-xs font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest block px-2">রক্তের গ্রুপ *</label>
                <div className="grid grid-cols-4 gap-2">
                   {BLOOD_GROUPS.map(group => (
                      <button
                        key={group}
                        type="button"
                        onClick={() => setBloodGroup(group)}
                        className={cn(
                          "py-3 rounded-[16px] text-sm font-black transition-all border-2",
                          bloodGroup === group 
                            ? "bg-brand border-brand text-white shadow-lg shadow-brand/20" 
                            : "bg-gray-50 dark:bg-gray-800 border-transparent text-gray-400"
                        )}
                      >
                         {group}
                      </button>
                   ))}
                </div>
             </div>

             {/* Location Filters */}
             <div className="space-y-6">
                <div className="relative">
                   <div className="absolute -top-1 left-4 px-2 bg-white dark:bg-gray-900 text-[10px] font-black text-gray-400 uppercase tracking-widest z-10 italic">বিভাগ</div>
                   <select 
                     value={division} 
                     onChange={(e) => setDivision(e.target.value)}
                     className="w-full bg-gray-50 dark:bg-gray-800/50 px-6 py-5 rounded-[24px] border border-gray-100 dark:border-gray-800 outline-none font-bold text-gray-700 dark:text-gray-300 appearance-none transition-all focus:border-brand"
                   >
                      <option value="">বিভাগ নির্বাচন করুন</option>
                      {DIVISIONS.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                   </select>
                </div>

                <div className="relative">
                   <div className="absolute -top-1 left-4 px-2 bg-white dark:bg-gray-900 text-[10px] font-black text-gray-400 uppercase tracking-widest z-10 italic">জেলা</div>
                   <select 
                     value={district} 
                     disabled={!division}
                     onChange={(e) => setDistrict(e.target.value)}
                     className="w-full bg-gray-50 dark:bg-gray-800/50 px-6 py-5 rounded-[24px] border border-gray-100 dark:border-gray-800 outline-none font-bold text-gray-700 dark:text-gray-300 appearance-none disabled:opacity-50 transition-all focus:border-brand"
                   >
                      <option value="">জেলা নির্বাচন করুন</option>
                      {districts.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                   </select>
                </div>

                <div className="relative">
                   <div className="absolute -top-1 left-4 px-2 bg-white dark:bg-gray-900 text-[10px] font-black text-gray-400 uppercase tracking-widest z-10 italic">উপজেলা</div>
                   <select 
                     value={upazila} 
                     disabled={!district}
                     onChange={(e) => setUpazila(e.target.value)}
                     className="w-full bg-gray-50 dark:bg-gray-800/50 px-6 py-5 rounded-[24px] border border-gray-100 dark:border-gray-800 outline-none font-bold text-gray-700 dark:text-gray-300 appearance-none disabled:opacity-50 transition-all focus:border-brand"
                   >
                      <option value="">উপজেলা নির্বাচন করুন</option>
                      {upazilas.map(u => <option key={u.id} value={u.name}>{u.bnName}</option>)}
                   </select>
                </div>
             </div>

             <button
               onClick={handleSearch}
               disabled={loading}
               className="w-full py-6 bg-brand text-white font-black text-lg rounded-[28px] shadow-2xl shadow-brand/30 hover:bg-brand/90 transition-all active:scale-95 disabled:opacity-50 flex items-center justify-center gap-3"
             >
                <Search className="w-6 h-6" /> {loading ? 'অনুসন্ধান হচ্ছে...' : 'ডোনার খুঁজুন'}
             </button>
          </div>

          {/* Results Section */}
          <div className="mt-12 space-y-6">
             <div className="flex items-center justify-between px-2">
                <h3 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                   <Users className="w-6 h-6 text-brand" /> {hasSearched ? `ফলাফল (${donors.length})` : 'রক্তদাতা তালিকা'}
                </h3>
             </div>

             {loading ? (
                <div className="space-y-4">
                   {[1, 2, 3].map(i => <div key={i} className="h-32 bg-white dark:bg-gray-900 rounded-[40px] animate-pulse shadow-sm" />)}
                </div>
             ) : donors.length > 0 ? (
                <div className="space-y-4">
                   <AnimatePresence>
                      {donors.map((donor) => (
                         <motion.div
                           key={donor.uid}
                           layout
                           initial={{ opacity: 0, y: 20 }}
                           animate={{ opacity: 1, y: 0 }}
                           className="bg-white dark:bg-gray-900 rounded-[40px] p-6 shadow-lg card-shadow border border-gray-50 dark:border-gray-800 flex items-center gap-4 group"
                         >
                            <div className="w-16 h-16 bg-[#FEF2F2] dark:bg-brand/10 rounded-[20px] flex items-center justify-center text-brand font-black text-2xl shadow-sm group-hover:bg-brand group-hover:text-white transition-all duration-300">
                               {donor.bloodGroup}
                            </div>
                            <div className="flex-1">
                               <h4 className="text-lg font-black text-gray-900 dark:text-white line-clamp-1">{donor.displayName}</h4>
                               <div className="flex items-center gap-3 mt-1">
                                  <div className="flex items-center gap-1.5 text-brand/60 font-bold text-[10px] uppercase tracking-wider">
                                     <MapPin className="w-3 h-3" />
                                     <span>{donor.privacySettings?.hideLocation 
                                          ? "ঠিকানা লুকানো" 
                                          : `${donor.location.upazila}, ${donor.location.district}`}</span>
                                  </div>
                                  <div className="flex items-center gap-1.5 text-green-600 font-bold text-[10px] uppercase tracking-wider">
                                     <Heart className="w-3 h-3 fill-current" />
                                     <span>{donor.donationsCount || 0} বার</span>
                                  </div>
                               </div>
                               <div className="mt-2">
                                  {donor.lastDonated && (
                                     <span className={cn(
                                       "text-[8px] font-black uppercase tracking-widest px-2.5 py-1 rounded-full",
                                       (() => {
                                         const lastDonatedDate = new Date(donor.lastDonated);
                                         const fourMonthsAgo = new Date();
                                         fourMonthsAgo.setMonth(fourMonthsAgo.getMonth() - 4);
                                         return lastDonatedDate < fourMonthsAgo 
                                           ? "bg-accent/10 text-accent border border-accent/20"
                                           : "bg-orange-100 dark:bg-orange-900/20 text-orange-700 dark:text-orange-500 border border-orange-200 dark:border-orange-900/30";
                                       })()
                                     )}>
                                       {(() => {
                                         const lastDonatedDate = new Date(donor.lastDonated);
                                         const fourMonthsAgo = new Date();
                                         fourMonthsAgo.setMonth(fourMonthsAgo.getMonth() - 4);
                                         return lastDonatedDate < fourMonthsAgo 
                                           ? "রক্তদানের যোগ্য"
                                           : "অপেক্ষা করুন";
                                       })()}
                                     </span>
                                  )}
                               </div>
                            </div>
                            <div className="flex items-center gap-2">
                               {!donor.privacySettings?.hidePhoneNumber && (
                                  <a href={`tel:${donor.phoneNumber}`} className="w-12 h-12 bg-accent/10 dark:bg-accent/20 text-accent rounded-full flex items-center justify-center hover:scale-110 transition-transform shadow-xl shadow-accent/10">
                                     <Phone className="w-5 h-5" />
                                  </a>
                                )}
                            </div>
                         </motion.div>
                      ))}
                   </AnimatePresence>
                </div>
             ) : hasSearched ? (
                <div className="p-20 text-center bg-white dark:bg-gray-900 rounded-[40px] shadow-sm border border-gray-100 dark:border-gray-800">
                   <div className="w-20 h-20 bg-gray-50 dark:bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-6">
                      <Search className="w-8 h-8 text-gray-300" />
                   </div>
                   <p className="text-gray-400 font-bold">দুঃখিত, কোনো ম্যাচিং ডোনার পাওয়া যায়নি</p>
                </div>
             ) : (
                <div className="p-20 text-center bg-white dark:bg-gray-900 rounded-[40px] shadow-sm border border-gray-100 dark:border-gray-800">
                   <div className="w-20 h-20 bg-gray-50 dark:bg-gray-800 rounded-full flex items-center justify-center mx-auto mb-6">
                      <Users className="w-8 h-8 text-gray-200" />
                   </div>
                   <p className="text-gray-300 font-bold">ডোনার খুঁজতে উপরের ফিল্টার ব্যবহার করুন</p>
                </div>
             )}
          </div>
       </div>
    </div>
  );
}
