import React, { useState, useEffect } from 'react';
import { doc, setDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { BloodGroup, UserProfile, OperationType } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { cn } from '../lib/utils';
import { Heart, MapPin, Phone, Building } from 'lucide-react';
import { DIVISIONS, Division, District } from '../constants/locations';

const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export function Onboarding() {
  const { user } = useAuth();
  const [bloodGroup, setBloodGroup] = useState<BloodGroup | ''>('');
  const [division, setDivision] = useState('');
  const [district, setDistrict] = useState('');
  const [upazila, setUpazila] = useState('');
  const [address, setAddress] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [isDonor, setIsDonor] = useState(true);
  const [saving, setSaving] = useState(false);

  // Filtered lists
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

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !bloodGroup || !division || !district || !upazila) return;

    setSaving(true);
    const path = `users/${user.uid}`;
    try {
      const profile: UserProfile = {
        uid: user.uid,
        displayName: user.displayName || 'Anonymous',
        email: user.email || '',
        photoURL: user.photoURL || undefined,
        bloodGroup: bloodGroup as BloodGroup,
        location: {
          division,
          district,
          upazila,
          address
        },
        isDonor,
        phoneNumber,
        createdAt: new Date().toISOString(),
        donationsCount: 0,
        points: 0,
        ratingAverage: 0,
        ratingCount: 0,
        role: 'user'
      };

      await setDoc(doc(db, 'users', user.uid), profile);
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, path);
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="max-w-xl mx-auto bg-white dark:bg-gray-900 p-10 rounded-[40px] shadow-2xl border border-gray-100 dark:border-gray-800 animate-slide-up transition-colors">
      <div className="text-center mb-10">
        <div className="w-20 h-20 bg-red-100 dark:bg-red-900/20 rounded-3xl flex items-center justify-center mx-auto mb-6 shadow-inner transition-colors">
          <Heart className="w-10 h-10 text-red-600 animate-pulse fill-current" />
        </div>
        <h1 className="text-3xl font-black text-gray-900 dark:text-white">রক্তসেতু তে স্বাগতম</h1>
        <p className="text-gray-500 dark:text-gray-400 mt-3 text-lg">আপনার প্রোফাইলটি সম্পন্ন করুন</p>
      </div>

      <form onSubmit={handleSubmit} className="space-y-8">
        <div>
          <label className="block text-sm font-bold text-gray-500 dark:text-gray-400 uppercase mb-3 tracking-widest">রক্তের গ্রুপ</label>
          <div className="grid grid-cols-4 gap-3">
            {BLOOD_GROUPS.map((group) => (
              <button
                key={group}
                type="button"
                onClick={() => setBloodGroup(group)}
                className={cn(
                  "py-3 text-lg font-black rounded-2xl border-2 transition-all",
                  bloodGroup === group
                    ? "bg-red-600 border-red-600 text-white shadow-xl scale-105"
                    : "bg-white dark:bg-gray-800 border-gray-100 dark:border-gray-700 text-gray-600 dark:text-gray-300 hover:border-red-200"
                )}
              >
                {group}
              </button>
            ))}
          </div>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          <div>
            <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2">বিভাগ</label>
            <select
              required
              value={division}
              onChange={(e) => setDivision(e.target.value)}
              className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300 transition-colors"
            >
              <option value="">নির্বাচন করুন</option>
              {DIVISIONS.map(d => <option key={d.id} value={d.name} className="dark:bg-gray-900">{d.bnName}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2">জেলা</label>
            <select
              required
              disabled={!division}
              value={district}
              onChange={(e) => setDistrict(e.target.value)}
              className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300 disabled:opacity-50 transition-colors"
            >
              <option value="">নির্বাচন করুন</option>
              {districts.map(d => <option key={d.id} value={d.name} className="dark:bg-gray-900">{d.bnName}</option>)}
            </select>
          </div>
          <div>
            <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2">থানা/উপজেলা</label>
            <select
              required
              disabled={!district}
              value={upazila}
              onChange={(e) => setUpazila(e.target.value)}
              className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300 disabled:opacity-50 transition-colors"
            >
              <option value="">নির্বাচন করুন</option>
              {upazilas.map(u => <option key={u.id} value={u.name} className="dark:bg-gray-900">{u.bnName}</option>)}
            </select>
          </div>
        </div>

        <div>
          <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2 flex items-center gap-2">
            <MapPin className="w-4 h-4" /> বিস্তারিত ঠিকানা (ঐচ্ছিক)
          </label>
          <input
            type="text"
            placeholder="গ্রাম, রাস্তা বা এলাকার নাম"
            value={address}
            onChange={(e) => setAddress(e.target.value)}
            className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300 transition-colors"
          />
        </div>

        <div>
           <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2 flex items-center gap-2">
            <Phone className="w-4 h-4" /> ফোন নম্বর
          </label>
          <input
            type="tel"
            required
            placeholder="০১৭XXXXXXXX"
            value={phoneNumber}
            onChange={(e) => setPhoneNumber(e.target.value)}
            className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300 transition-colors"
          />
        </div>

        <div className="flex items-center gap-4 p-5 bg-red-50/50 dark:bg-red-900/10 rounded-3xl border border-red-100 dark:border-red-900/20 transition-colors">
          <input
            type="checkbox"
            id="isDonor"
            checked={isDonor}
            onChange={(e) => setIsDonor(e.target.checked)}
            className="w-6 h-6 text-red-600 border-red-200 dark:border-red-800 rounded-lg focus:ring-red-500 cursor-pointer"
          />
          <div className="flex-1">
            <label htmlFor="isDonor" className="text-sm font-black text-red-900 dark:text-red-400 cursor-pointer">
              আমি স্বেচ্ছায় রক্তদানে ইচ্ছুক
            </label>
            <p className="text-xs text-red-600 dark:text-red-500 mt-0.5">আপনার রক্ত দান অন্য কারো জীবন বাঁচাতে পারে</p>
          </div>
        </div>

        <button
          type="submit"
          disabled={saving || !bloodGroup || !upazila}
          className={cn(
            "w-full py-5 px-6 rounded-[24px] font-black text-xl text-white transition-all shadow-2xl",
            saving || !bloodGroup || !upazila
              ? "bg-gray-200 cursor-not-allowed text-gray-400"
              : "bg-red-600 hover:bg-red-700 shadow-red-500/30"
          )}
        >
          {saving ? 'প্রোফাইল তৈরি হচ্ছে...' : 'নিবন্ধন সম্পন্ন করুন'}
        </button>
      </form>
    </div>
  );
}
