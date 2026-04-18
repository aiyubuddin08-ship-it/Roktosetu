import React, { useState, useEffect, useRef } from 'react';
import { useAuth } from '../contexts/AuthContext';
import { doc, updateDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { OperationType, BloodGroup } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { cn } from '../lib/utils';
import { Heart, MapPin, Phone, User as UserIcon, Calendar, CheckCircle, Save, LogOut, Award, Star, TrendingUp, Camera, Loader2, Droplets } from 'lucide-react';
import { DIVISIONS, District } from '../constants/locations';
import { auth, storage } from '../lib/firebase';
import { signOut } from 'firebase/auth';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { motion, AnimatePresence } from 'motion/react';
import { DonorCard } from '../components/DonorCard';

const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

const getBadge = (count: number = 0) => {
  if (count >= 10) return { icon: Award, label: 'অভিভাবক দূত', color: 'text-purple-600', bg: 'bg-purple-50' };
  if (count >= 5) return { icon: Award, label: 'জীবন রক্ষাকারী', color: 'text-blue-600', bg: 'bg-blue-50' };
  if (count >= 1) return { icon: Award, label: 'রক্তদান বীর', color: 'text-green-600', bg: 'bg-green-50' };
  return { icon: Award, label: 'নতুন সদস্য', color: 'text-gray-400', bg: 'bg-gray-50' };
};

export function Profile() {
  const { profile, user } = useAuth();
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [uploading, setUploading] = useState(false);

  // Form State
  const [bloodGroup, setBloodGroup] = useState<BloodGroup | ''>(profile?.bloodGroup || '');
  const [division, setDivision] = useState(profile?.location?.division || '');
  const [district, setDistrict] = useState(profile?.location?.district || '');
  const [upazila, setUpazila] = useState(profile?.location?.upazila || '');
  const [address, setAddress] = useState(profile?.location?.address || '');
  const [phoneNumber, setPhoneNumber] = useState(profile?.phoneNumber || '');
  const [isDonor, setIsDonor] = useState(profile?.isDonor ?? true);
  const [lastDonated, setLastDonated] = useState(profile?.lastDonated?.split('T')[0] || '');

  const [districts, setDistricts] = useState<District[]>([]);
  const [upazilas, setUpazilas] = useState<{ id: string; name: string; bnName: string }[]>([]);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    const selectedDivision = DIVISIONS.find(d => d.name === division);
    setDistricts(selectedDivision ? selectedDivision.districts : []);
  }, [division]);

  useEffect(() => {
    const selectedDistrict = districts.find(d => d.name === district);
    setUpazilas(selectedDistrict ? selectedDistrict.upazilas : []);
  }, [district, districts]);

  const handleImageUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file || !user) return;

    // Check size (max 2MB)
    if (file.size > 2 * 1024 * 1024) {
      alert("ছবি ২ মেগাবাইটের কম হতে হবে।");
      return;
    }

    setUploading(true);
    try {
      const storageRef = ref(storage, `profiles/${user.uid}`);
      await uploadBytes(storageRef, file);
      const url = await getDownloadURL(storageRef);
      
      await updateDoc(doc(db, 'users', user.uid), {
        photoURL: url
      });
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (error) {
      console.error("Upload error:", error);
      alert("ছবি আপলোড ব্যর্থ হয়েছে।");
    } finally {
      setUploading(false);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setSaving(true);
    setSuccess(false);

    try {
      await updateDoc(doc(db, 'users', user.uid), {
        bloodGroup,
        location: { division, district, upazila, address },
        phoneNumber,
        isDonor,
        lastDonated: lastDonated ? new Date(lastDonated).toISOString() : null,
        role: profile.role || 'user' // Ensure role is preserved
      });
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `users/${user.uid}`);
    } finally {
      setSaving(false);
    }
  };

  if (!profile) return null;

  const badge = getBadge(profile?.donationsCount);

  return (
    <div className="max-w-4xl mx-auto space-y-10 pb-20 animate-fade-in relative z-0">
      <div className="grid md:grid-cols-3 gap-8 text-center sm:text-left">
        {/* Profile Info Card */}
        <div className="md:col-span-2 flex flex-col sm:flex-row items-center gap-8 bg-white dark:bg-gray-900 p-8 sm:p-10 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-2xl transition-colors">
          <div className="relative group shrink-0">
             <input
                type="file"
                className="hidden"
                ref={fileInputRef}
                accept="image/*"
                onChange={handleImageUpload}
                disabled={uploading}
              />
            {profile.photoURL ? (
              <img src={profile.photoURL} alt={profile.displayName} className="w-32 h-32 rounded-[40px] border-4 border-red-50 dark:border-red-900/30 object-cover shadow-2xl group-hover:scale-105 transition-transform" referrerPolicy="no-referrer" />
            ) : (
              <div className="w-32 h-32 bg-red-50 dark:bg-red-900/10 rounded-[40px] flex items-center justify-center text-red-600 shadow-inner group-hover:bg-red-100 dark:group-hover:bg-red-900/20 transition-all">
                <UserIcon className="w-16 h-16" />
              </div>
            )}
            
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={uploading}
              className="absolute -bottom-2 -right-2 w-11 h-11 bg-white dark:bg-gray-800 text-red-600 rounded-2xl flex items-center justify-center border-4 border-gray-50 dark:border-gray-900 shadow-xl hover:scale-110 transition-transform active:scale-95 disabled:opacity-50"
              title="ছবি পরিবর্তন"
            >
              {uploading ? <Loader2 className="w-5 h-5 animate-spin" /> : <Camera className="w-5 h-5" />}
            </button>
          </div>
          <div className="flex-1 space-y-3">
            <h1 className="text-3xl sm:text-4xl font-black text-gray-900 dark:text-white leading-tight">{profile.displayName}</h1>
            <p className="text-gray-500 dark:text-gray-400 font-bold">{profile.email}</p>
            <div className="flex flex-wrap justify-center sm:justify-start gap-2">
               <span className="px-5 py-2 bg-red-600 text-white rounded-2xl text-[10px] sm:text-xs font-black uppercase tracking-widest shadow-xl shadow-red-100 dark:shadow-none transition-shadow">
                 রক্তের গ্রুপ: {profile.bloodGroup || 'অজানা'}
               </span>
               {profile.isDonor && (
                 <span className="px-5 py-2 bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-500 rounded-2xl text-[10px] sm:text-xs font-black uppercase tracking-widest flex items-center gap-2 border border-green-200 dark:border-green-900/30">
                   <Heart className="w-4 h-4 fill-current" /> সক্রিয় রক্তদাতা
                 </span>
               )}
            </div>
          </div>
          <button
            onClick={() => signOut(auth)}
            className="p-4 text-gray-400 dark:text-gray-500 hover:text-red-600 dark:hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/10 rounded-3xl transition-all h-fit"
            title="লগ আউট"
          >
            <LogOut className="w-7 h-7" />
          </button>
        </div>

        {/* Stats Sidebar */}
        <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-2xl space-y-6 transition-colors">
           <div className="flex items-center gap-4 p-4 border border-gray-100 dark:border-gray-800 rounded-3xl">
              <div className={cn("p-3 rounded-2xl", badge.bg, badge.color)}>
                 <badge.icon className="w-6 h-6" />
              </div>
              <div>
                 <p className="text-[10px] font-black uppercase text-gray-400 dark:text-gray-500">আপনার লেভেল</p>
                 <p className="text-lg font-black text-gray-900 dark:text-white">{badge.label}</p>
              </div>
           </div>
           
           <div className="grid grid-cols-1 gap-4 text-left">
              <div className="bg-gray-50 dark:bg-gray-800/50 rounded-3xl p-5 flex items-center justify-between transition-colors">
                 <div className="flex items-center gap-3">
                    <TrendingUp className="w-5 h-5 text-green-500" />
                    <span className="font-bold text-gray-600 dark:text-gray-400">মোট রক্তদান</span>
                 </div>
                 <span className="text-xl font-black text-gray-900 dark:text-white">{profile.donationsCount || 0}</span>
              </div>
              <div className="bg-gray-50 dark:bg-gray-800/50 rounded-3xl p-5 flex items-center justify-between transition-colors">
                 <div className="flex items-center gap-3">
                    <Star className="w-5 h-5 text-yellow-500 fill-current" />
                    <span className="font-bold text-gray-600 dark:text-gray-400">গড় রেটিং</span>
                 </div>
                 <span className="text-xl font-black text-gray-900 dark:text-white">{profile.ratingAverage?.toFixed(1) || '0.0'}</span>
              </div>
              <div className="bg-gray-50 dark:bg-gray-800/50 rounded-3xl p-5 flex items-center justify-between transition-colors">
                 <div className="flex items-center gap-3">
                    <Award className="w-5 h-5 text-blue-500" />
                    <span className="font-bold text-gray-600 dark:text-gray-400">অর্জিত পয়েন্ট</span>
                 </div>
                 <span className="text-xl font-black text-gray-900 dark:text-white">{profile.points || 0}</span>
              </div>
           </div>
        </div>
      </div>

      {profile.isDonor && (
        <section className="bg-white dark:bg-gray-900 p-10 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-2xl transition-colors">
           <div className="flex flex-col md:flex-row gap-12 items-center">
              <div className="flex-1 space-y-6 text-center md:text-left">
                 <h2 className="text-3xl font-black text-gray-900 dark:text-white">আপনার ডিজিটাল ডোনার কার্ড</h2>
                 <p className="text-lg text-gray-500 dark:text-gray-400 font-medium font-bangla">
                    এই কার্ডটি আপনার রক্তদাতা হিসেবে একটি ভার্চুয়াল পরিচিতি। যেকোনো প্রয়োজনে এটি প্রদর্শন করতে পারবেন। 
                    নিচে থাকা কিউআর কোডটি স্ক্যান করলে আপনার প্রোফাইল ভেরিফাই করা যাবে।
                 </p>
                 <div className="flex items-center gap-4 p-4 bg-red-50 dark:bg-red-900/10 rounded-3xl border border-red-100 dark:border-red-900/20">
                    <Droplets className="w-8 h-8 text-red-600 grow-0 shrink-0" />
                    <p className="text-sm font-bold text-red-900 dark:text-red-400">রক্তদান বীর হিসেবে আপনি আমাদের গর্ব। নিয়মিত রক্ত দিন, জীবন বাঁচান।</p>
                 </div>
              </div>
              <div className="shrink-0 w-full md:w-auto">
                 <DonorCard donor={profile} />
              </div>
           </div>
        </section>
      )}

      <div className="bg-white dark:bg-gray-900 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-2xl overflow-hidden transition-colors">
        <div className="p-8 border-b border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-800/30 flex items-center justify-between">
          <div className="flex items-center gap-3">
             <div className="p-2 bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500 rounded-xl">
                <UserIcon className="w-5 h-5" />
             </div>
             <h2 className="text-xl font-black text-gray-900 dark:text-white">প্রোফাইল তথ্য পরিবর্তন</h2>
          </div>
          <AnimatePresence>
            {success && (
              <motion.span
                initial={{ opacity: 0, x: 20 }}
                animate={{ opacity: 1, x: 0 }}
                exit={{ opacity: 0, x: 20 }}
                className="text-green-600 dark:text-green-500 text-sm font-black flex items-center gap-2"
              >
                <CheckCircle className="w-5 h-5" /> সফলভাবে আপডেট হয়েছে
              </motion.span>
            )}
          </AnimatePresence>
        </div>

        <form onSubmit={handleSubmit} className="p-10 space-y-10">
          <div className="grid md:grid-cols-2 gap-10">
            <div className="space-y-4">
              <label className="block text-xs font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest">রক্তের গ্রুপ</label>
              <div className="grid grid-cols-4 gap-2">
                {BLOOD_GROUPS.map((group) => (
                  <button
                    key={group}
                    type="button"
                    onClick={() => setBloodGroup(group)}
                    className={cn(
                      "py-3 text-sm font-black rounded-2xl border-2 transition-all",
                      bloodGroup === group
                        ? "bg-red-600 border-red-600 text-white shadow-lg shadow-red-100 dark:shadow-none"
                        : "bg-white dark:bg-gray-800 border-gray-100 dark:border-gray-700 text-gray-500 dark:text-gray-400 hover:border-red-200"
                    )}
                  >
                    {group}
                  </button>
                ))}
              </div>
            </div>
            <div className="space-y-4">
               <label className="block text-xs font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest flex items-center gap-2">
                 <Phone className="w-4 h-4 text-red-600" /> ফোন নম্বর
               </label>
               <input
                 type="tel"
                 required
                 placeholder="০১৭XXXXXXXX"
                 value={phoneNumber}
                 onChange={(e) => setPhoneNumber(e.target.value)}
                 className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300 transition-colors"
               />
            </div>
          </div>

          <div className="space-y-6">
             <div className="flex items-center gap-3 mb-2">
                <MapPin className="w-5 h-5 text-red-600" />
                <label className="text-xs font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest">বর্তমান ঠিকানা</label>
             </div>
             <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <select
                  value={division}
                  onChange={(e) => setDivision(e.target.value)}
                  className="w-full px-5 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300 transition-colors"
                >
                  <option value="">বিভাগ</option>
                  {DIVISIONS.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                </select>
                <select
                  disabled={!division}
                  value={district}
                  onChange={(e) => setDistrict(e.target.value)}
                  className="w-full px-5 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300 transition-colors disabled:opacity-50"
                >
                  <option value="">জেলা</option>
                  {districts.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                </select>
                <select
                  disabled={!district}
                  value={upazila}
                  onChange={(e) => setUpazila(e.target.value)}
                  className="w-full px-5 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300 transition-colors disabled:opacity-50"
                >
                  <option value="">উপজেলা</option>
                  {upazilas.map(u => <option key={u.id} value={u.name}>{u.bnName}</option>)}
                </select>
             </div>
             <input
                type="text"
                placeholder="বিস্তারিত ঠিকানা (ঐচ্ছিক)"
                value={address}
                onChange={(e) => setAddress(e.target.value)}
                className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300 transition-colors"
             />
          </div>

          <div className="grid md:grid-cols-2 gap-10">
            <div className="space-y-4">
              <label className="block text-xs font-black text-gray-400 dark:text-gray-500 uppercase tracking-widest flex items-center gap-2">
                <Calendar className="w-4 h-4 text-red-600" /> শেষ রক্তদানের তারিখ
              </label>
              <input
                type="date"
                value={lastDonated}
                onChange={(e) => setLastDonated(e.target.value)}
                className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300 transition-colors"
              />
            </div>
            <div className="flex items-center gap-5 p-6 bg-red-50/50 dark:bg-red-900/10 rounded-[32px] border border-red-100 dark:border-red-900/20 group cursor-pointer" onClick={() => setIsDonor(!isDonor)}>
                <div className={cn(
                  "w-12 h-12 rounded-2xl flex items-center justify-center transition-all shadow-xl",
                  isDonor ? "bg-red-600 text-white shadow-red-100 dark:shadow-none" : "bg-white dark:bg-gray-800 text-gray-300 dark:text-gray-600"
                )}>
                  <Heart className={cn("w-6 h-6", isDonor && "fill-current")} />
                </div>
                <div className="flex-1">
                  <span className="block text-lg font-black text-red-900 dark:text-red-400 leading-none transition-colors">আমি রক্ত দিতে ইচ্ছুক</span>
                  <p className="text-xs text-red-600 dark:text-red-500 font-bold mt-1">সবাই আপনাকে রক্তদানের জন্য খুঁজে পাবে</p>
                </div>
            </div>
          </div>

          <button
            type="submit"
            disabled={saving}
            className="w-full py-6 bg-red-600 text-white font-black text-xl rounded-[32px] hover:bg-red-700 transition-all shadow-2xl shadow-red-500/30 dark:shadow-none flex items-center justify-center gap-3 disabled:opacity-50 active:scale-95"
          >
            <Save className="w-6 h-6" /> {saving ? 'আপডেট হচ্ছে...' : 'তথ্য সংরক্ষণ করুন'}
          </button>
        </form>
      </div>
    </div>
  );
}
