import React, { useState, useEffect } from 'react';
import { collection, query, where, onSnapshot } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { UserProfile, BloodGroup, OperationType } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Search, MapPin, Heart, Phone, Filter, Users } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { DIVISIONS, Division, District } from '../constants/locations';

const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export function DonorSearch() {
  const { user } = useAuth();
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
    <div className="space-y-10 pb-20">
      <section className="text-center space-y-4 max-w-2xl mx-auto">
        <h1 className="text-4xl font-black text-gray-900 leading-tight">রক্তদাতা খুঁজুন</h1>
        <p className="text-gray-500 text-lg font-medium">আপনার নিকটস্থ রক্তদাতাদের সাথে যোগাযোগ করুন এবং জীবন বাঁচান।</p>
      </section>

      {/* Filters Card */}
      <div className="bg-white p-10 rounded-[40px] shadow-2xl border border-gray-100 animate-slide-up">
        <div className="flex items-center gap-3 mb-8 text-red-600">
           <Filter className="w-6 h-6" />
           <h2 className="text-xl font-black uppercase tracking-widest">ফিল্টার করুন</h2>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-8">
          <div>
            <label className="block text-xs font-black text-gray-400 uppercase tracking-widest mb-3">রক্তের গ্রুপ</label>
            <div className="grid grid-cols-4 gap-2">
              {BLOOD_GROUPS.map((group) => (
                <button
                  key={group}
                  onClick={() => setBloodGroup(group)}
                  className={cn(
                    "py-2 text-sm font-black rounded-xl border-2 transition-all",
                    bloodGroup === group
                      ? "bg-red-600 border-red-600 text-white shadow-lg"
                      : "bg-white border-gray-50 text-gray-500 hover:border-red-200"
                  )}
                >
                  {group}
                </button>
              ))}
            </div>
          </div>

          <div className="space-y-3">
             <label className="block text-xs font-black text-gray-400 uppercase tracking-widest">বিভাগ</label>
             <select
                value={division}
                onChange={(e) => setDivision(e.target.value)}
                className="w-full px-5 py-3.5 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700"
              >
                <option value="">সবগুলো</option>
                {DIVISIONS.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
              </select>
          </div>

          <div className="space-y-3">
             <label className="block text-xs font-black text-gray-400 uppercase tracking-widest">জেলা</label>
             <select
                disabled={!division}
                value={district}
                onChange={(e) => setDistrict(e.target.value)}
                className="w-full px-5 py-3.5 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 disabled:opacity-50"
              >
                <option value="">সবগুলো</option>
                {districts.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
              </select>
          </div>

          <div className="space-y-3">
             <label className="block text-xs font-black text-gray-400 uppercase tracking-widest">উপজেলা</label>
             <select
                disabled={!district}
                value={upazila}
                onChange={(e) => setUpazila(e.target.value)}
                className="w-full px-5 py-3.5 bg-gray-50 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 disabled:opacity-50"
              >
                <option value="">সবগুলো</option>
                {upazilas.map(u => <option key={u.id} value={u.name}>{u.bnName}</option>)}
              </select>
          </div>
        </div>

        <button
          onClick={handleSearch}
          className="w-full mt-10 py-5 bg-red-600 text-white rounded-[24px] font-black text-xl hover:bg-red-700 transition shadow-2xl shadow-red-500/30 flex items-center justify-center gap-3 active:scale-95"
        >
          <Search className="w-6 h-6" /> ডোনার খুঁজুন
        </button>
      </div>

      {/* Results */}
      <div className="space-y-8">
        {hasSearched && (
          <h3 className="text-xl font-black text-gray-900 px-2 flex items-center gap-2">
            <Users className="w-6 h-6 text-red-600" /> অনুসন্ধান ফলাফল ({donors.length})
          </h3>
        )}

        {loading ? (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8 text-center">
            {[1, 2, 3].map(i => (
              <div key={i} className="h-48 bg-white rounded-[40px] shimmer border border-gray-100" />
            ))}
          </div>
        ) : donors.length > 0 ? (
          <div className="grid md:grid-cols-2 lg:grid-cols-3 gap-8">
            <AnimatePresence>
              {donors.map((donor) => (
                <motion.div
                  key={donor.uid}
                  layout
                  initial={{ opacity: 0, scale: 0.9 }}
                  animate={{ opacity: 1, scale: 1 }}
                  className="bg-white rounded-[40px] shadow-xl border border-gray-50 overflow-hidden group hover:shadow-2xl transition-all"
                >
                  <div className="p-8">
                    <div className="flex items-center gap-5 mb-6">
                      <div className="w-20 h-20 rounded-3xl bg-red-50 flex items-center justify-center text-red-600 font-black text-3xl shadow-inner group-hover:bg-red-600 group-hover:text-white transition-all duration-500">
                        {donor.bloodGroup}
                      </div>
                      <div>
                        <h4 className="text-xl font-black text-gray-900 line-clamp-1">{donor.displayName}</h4>
                        <div className="flex items-center gap-1.5 text-red-600 mt-1">
                           <Heart className="w-4 h-4 fill-current" />
                           <span className="text-xs font-black uppercase tracking-widest leading-none mt-0.5">রক্তদাতা</span>
                        </div>
                      </div>
                    </div>

                    <div className="space-y-4 pt-6 border-t border-gray-100">
                      <div className="flex items-center gap-3 text-gray-500 font-bold text-sm">
                        <MapPin className="w-4 h-4 text-red-400" />
                        <span className="truncate">{donor.location.upazila}, {donor.location.district}</span>
                      </div>
                      <a
                        href={`tel:${donor.phoneNumber}`}
                        className="flex items-center gap-3 w-full justify-center py-4 bg-green-600 text-white rounded-2xl font-black hover:bg-green-700 transition shadow-xl shadow-green-100 active:scale-95"
                      >
                        <Phone className="w-4 h-4" /> কল করুন
                      </a>
                    </div>
                  </div>
                </motion.div>
              ))}
            </AnimatePresence>
          </div>
        ) : hasSearched ? (
          <div className="bg-white p-20 rounded-[40px] text-center border-2 border-dashed border-gray-200">
            <div className="w-24 h-24 bg-gray-50 rounded-full flex items-center justify-center mx-auto mb-6">
               <Search className="w-10 h-10 text-gray-300" />
            </div>
            <p className="text-gray-400 text-xl font-bold">দুঃখিত, কোনো ম্যাচিং ডোনার পাওয়া যায়নি</p>
            <p className="text-gray-400 mt-2">অন্য কোনো এলাকা বা রক্তের গ্রুপ দিয়ে চেষ্টা করুন</p>
          </div>
        ) : (
          <div className="bg-white p-20 rounded-[40px] text-center border border-gray-100 opacity-60 grayscale">
            <Search className="w-16 h-16 text-gray-200 mx-auto mb-6" />
            <p className="text-gray-400 text-lg font-bold">ডোনার খুঁজতে উপরের ফিল্টার ব্যবহার করুন</p>
          </div>
        )}
      </div>
    </div>
  );
}
