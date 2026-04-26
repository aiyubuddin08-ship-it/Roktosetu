import React, { useState, useEffect } from 'react';
import { doc, setDoc, updateDoc, increment, collection, query, where, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { BloodGroup, UserProfile, OperationType, Organization } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { cn } from '../lib/utils';
import { Heart, MapPin, Phone, Building2, Users, Search, CheckCircle2, Award, ArrowRight } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { DIVISIONS, Division, District } from '../constants/locations';
import { organizationService } from '../services/organizationService';
import { sendNotification } from '../lib/notifications';

const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export function Onboarding() {
  const { user } = useAuth();
  const [registrationType, setRegistrationType] = useState<'Donor' | 'Organization'>('Donor');
  const [bloodGroup, setBloodGroup] = useState<BloodGroup | ''>('');
  const [division, setDivision] = useState('');
  const [district, setDistrict] = useState('');
  const [upazila, setUpazila] = useState('');
  const [address, setAddress] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [referralCode, setReferralCode] = useState('');
  const [isDonor, setIsDonor] = useState(true);
  const [saving, setSaving] = useState(false);

  // Organization Specific state
  const [orgName, setOrgName] = useState('');
  const [coverageArea, setCoverageArea] = useState('');
  const [orgDescription, setOrgDescription] = useState('');
  const [orgType, setOrgType] = useState<Organization['type']>('Volunteer Group');
  const [memberCount, setMemberCount] = useState<string>('');

  // Organization Search state (for donors)
  const [orgSearch, setOrgSearch] = useState('');
  const [searchResults, setSearchResults] = useState<Organization[]>([]);
  const [selectedOrg, setSelectedOrg] = useState<Organization | null>(null);
  const [isSearching, setIsSearching] = useState(false);

  // Filtered lists
  const [districts, setDistricts] = useState<District[]>([]);
  const [upazilas, setUpazilas] = useState<{ id: string; name: string; bnName: string }[]>([]);

  useEffect(() => {
    if (user) {
      if (user.phoneNumber) setPhoneNumber(user.phoneNumber);
      if (user.displayName && registrationType === 'Donor') {
        // No displayName state for Donor, but we could add one if needed.
        // For now, we just use user.displayName in handleSubmit if profileData.displayName is not set.
      }
    }
  }, [user]);

  useEffect(() => {
    const searchOrgs = async () => {
      if (orgSearch.length > 2) {
        setIsSearching(true);
        const results = await organizationService.search(orgSearch);
        setSearchResults(results);
        setIsSearching(false);
      } else {
        setSearchResults([]);
      }
    };
    const timer = setTimeout(searchOrgs, 300);
    return () => clearTimeout(timer);
  }, [orgSearch]);

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
    if (!user || !division || !district || !upazila) return;
    if (registrationType === 'Donor' && !bloodGroup) return;

    setSaving(true);
    const path = `users/${user.uid}`;
    try {
      let finalPoints = 100; // Welcome Bonus
      let referredBy = '';

      // Check Referral Code (Only for donors)
      if (registrationType === 'Donor' && referralCode.trim()) {
        const q = query(collection(db, 'users'), where('referralCode', '==', referralCode.trim()));
        const snapshot = await getDocs(q);
        
        if (!snapshot.empty) {
          const referrerDoc = snapshot.docs[0];
          referredBy = referrerDoc.id;
          
          await updateDoc(doc(db, 'users', referredBy), {
            points: increment(50)
          });
          finalPoints += 50;

          await sendNotification({
            userId: referredBy,
            title: 'রেফারেল বোনাস! 🎁',
            message: 'আপনার রেফারেল কোড ব্যবহার করে কেউ জয়েন করেছেন। আপনি ৫০ পয়েন্ট পেয়েছেন!',
            type: 'Badge Earned',
            isRead: false,
            link: '/leaderboard'
          });
        }
      }

      const profileData: any = {
        uid: user.uid,
        displayName: registrationType === 'Organization' ? orgName : user.displayName || 'Anonymous',
        email: user.email || '',
        bloodGroup: (registrationType === 'Organization' ? 'O+' : bloodGroup) as BloodGroup,
        location: { division, district, upazila, address },
        isDonor: registrationType === 'Donor' ? isDonor : false,
        phoneNumber,
        createdAt: new Date().toISOString(),
        donationsCount: 0,
        points: finalPoints,
        ratingAverage: 0,
        ratingCount: 0,
        role: 'user',
        badges: ['New Member'],
        referralCode: `RS${user.uid.substring(0, 6).toUpperCase()}`,
        referredBy
      };

      if (user.photoURL) profileData.photoURL = user.photoURL;
      if (selectedOrg?.id) profileData.organizationId = selectedOrg.id;
      if (selectedOrg?.name) {
        profileData.organizationName = selectedOrg.name;
      } else if (orgSearch) {
        profileData.organizationName = orgSearch;
      }

      const profile = profileData as UserProfile;

      if (registrationType === 'Organization') {
        const orgId = `ORG-${Date.now()}`;
        const orgData: Organization = {
          id: orgId,
          name: orgName,
          email: user.email || '',
          phoneNumber: phoneNumber,
          location: { division, district, upazila },
          address,
          description: orgDescription,
          coverageArea,
          type: orgType,
          memberCount: parseInt(memberCount) || 1,
          createdAt: new Date().toISOString(),
          isVerified: false,
          adminUid: user.uid
        };
        await setDoc(doc(db, 'organizations', orgId), orgData);
        profile.organizationId = orgId;
        profile.organizationName = orgName;
        profile.role = 'user'; // Or maybe an 'org_admin' role if we add one later
      }

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

      <div className="flex bg-gray-100 dark:bg-gray-800 p-2 rounded-3xl mb-8">
        <button
          type="button"
          onClick={() => setRegistrationType('Donor')}
          className={cn(
            "flex-1 py-3 rounded-2xl font-black text-sm transition-all",
            registrationType === 'Donor' ? "bg-white dark:bg-gray-900 shadow-lg text-red-600" : "text-gray-400"
          )}
        >
          ডোনার হিসেবে
        </button>
        <button
          type="button"
          onClick={() => setRegistrationType('Organization')}
          className={cn(
            "flex-1 py-3 rounded-2xl font-black text-sm transition-all",
            registrationType === 'Organization' ? "bg-white dark:bg-gray-900 shadow-lg text-red-600" : "text-gray-400"
          )}
        >
          সংগঠন হিসেবে
        </button>
      </div>

      <form onSubmit={handleSubmit} className="space-y-8">
        <AnimatePresence mode="wait">
          {registrationType === 'Organization' ? (
            <motion.div
              key="organization-fields"
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2 flex items-center gap-2">
                  <Building2 className="w-4 h-4" /> সংগঠনের নাম
                </label>
                <input
                  required
                  type="text"
                  placeholder="উদা: সাতকানিয়া ব্লাড ডোনার সোসাইটি"
                  value={orgName}
                  onChange={(e) => setOrgName(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-black text-gray-700 dark:text-gray-300"
                />
              </div>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2 flex items-center gap-2">
                    <MapPin className="w-4 h-4" /> কাজের এলাকা
                  </label>
                  <input
                    required
                    type="text"
                    placeholder="উদা: বাঁশখালী, চকরিয়া"
                    value={coverageArea}
                    onChange={(e) => setCoverageArea(e.target.value)}
                    className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300"
                  />
                </div>
                <div>
                  <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2 flex items-center gap-2">
                    <Users className="w-4 h-4" /> সদস্য সংখ্যা
                  </label>
                  <input
                    required
                    type="number"
                    placeholder="উদা: ১০০"
                    value={memberCount}
                    onChange={(e) => setMemberCount(e.target.value)}
                    className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300"
                  />
                </div>
              </div>
              <div>
                <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2">সংগঠনের ধরণ</label>
                <select
                  value={orgType}
                  onChange={(e) => setOrgType(e.target.value as any)}
                  className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300"
                >
                  <option value="Volunteer Group">স্বেচ্ছাসেবী সংগঠন</option>
                  <option value="Club">ক্লাব</option>
                  <option value="NGO">এনজিও</option>
                  <option value="Private">প্রাইভেট</option>
                </select>
              </div>
            </motion.div>
          ) : (
            <motion.div
              key="donor-fields"
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: 20 }}
              className="space-y-6"
            >
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
            </motion.div>
          )}
        </AnimatePresence>

        <div className="space-y-6">
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
        </div>

        <AnimatePresence>
          {registrationType === 'Organization' && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden space-y-4"
            >
              <div>
                <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2">সংগঠনের বিস্তারিত বিবরণ</label>
                <textarea
                  required
                  placeholder="আপনার সংগঠন কি নিয়ে কাজ করে বা লক্ষ্য কী তা লিখুন..."
                  value={orgDescription}
                  onChange={(e) => setOrgDescription(e.target.value)}
                  className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300 min-h-[100px]"
                />
              </div>
              
              <div className="p-6 bg-indigo-50 dark:bg-indigo-900/20 rounded-3xl border border-indigo-100 dark:border-indigo-800">
                <div className="flex items-center gap-3 mb-2">
                  <CheckCircle2 className="w-5 h-5 text-indigo-600" />
                  <h4 className="font-black text-indigo-900 dark:text-indigo-400">একটি মানবিক অনুরোধ</h4>
                </div>
                <p className="text-sm text-indigo-800 dark:text-indigo-500 font-bold leading-relaxed">
                  আপনার সংগঠনের সদস্যদের রক্তসেতু-তে "রক্তদাতা" হিসেবে নিবন্ধন করতে উৎসাহিত করুন। এতে জরুরি প্রয়োজনে আপনাদের এলাকা থেকে দ্রুত রক্তদাতা খুঁজে পাওয়া সহজ হবে।
                </p>
              </div>
            </motion.div>
          )}

          {registrationType === 'Donor' && (
            <motion.div
              initial={{ height: 0, opacity: 0 }}
              animate={{ height: 'auto', opacity: 1 }}
              exit={{ height: 0, opacity: 0 }}
              className="overflow-hidden space-y-6"
            >
              <div className="relative">
                <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2 flex items-center gap-2">
                  <Building2 className="w-4 h-4" /> সংস্থান/সংগঠন (যদি থাকে)
                </label>
                <div className="relative">
                  <input
                    type="text"
                    placeholder="সংগঠনের নাম লিখুন..."
                    value={orgSearch}
                    onChange={(e) => {
                      setOrgSearch(e.target.value);
                      if (selectedOrg) setSelectedOrg(null);
                    }}
                    className="w-full pl-12 pr-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300 transition-colors"
                  />
                  <Search className="absolute left-4 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400" />
                  {selectedOrg && (
                    <CheckCircle2 className="absolute right-4 top-1/2 -translate-y-1/2 w-5 h-5 text-green-500" />
                  )}
                </div>
                
                {/* Autocomplete Results */}
                {searchResults.length > 0 && !selectedOrg && (
                  <div className="absolute z-20 w-full mt-2 bg-white dark:bg-gray-800 rounded-2xl shadow-2xl border border-gray-100 dark:border-gray-700 max-h-48 overflow-y-auto overflow-x-hidden">
                    {searchResults.map((org) => (
                      <button
                        key={org.id}
                        type="button"
                        onClick={() => {
                          setSelectedOrg(org);
                          setOrgSearch(org.name);
                          setSearchResults([]);
                        }}
                        className="w-full px-6 py-4 text-left hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors flex items-center justify-between group"
                      >
                        <div>
                          <p className="font-black text-gray-900 dark:text-white group-hover:text-red-600">{org.name}</p>
                          <p className="text-xs text-gray-400">{org.location.district}, {org.type}</p>
                        </div>
                        <Building2 className="w-5 h-5 text-gray-300 group-hover:text-red-400" />
                      </button>
                    ))}
                  </div>
                )}
                <p className="text-[10px] text-gray-400 mt-2 px-1">আপনি যদি কোনো রক্তদান সংগঠনের সদস্য হন তবে তা উল্লেখ করুন।</p>
              </div>

              <div>
                <label className="block text-xs font-bold text-gray-400 dark:text-gray-500 uppercase mb-2 flex items-center gap-2">
                  <Award className="w-4 h-4" /> রেফারেল কোড (যদি থাকে)
                </label>
                <input
                  type="text"
                  placeholder="উদা: RSABC123"
                  value={referralCode}
                  onChange={(e) => setReferralCode(e.target.value.toUpperCase())}
                  className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-black text-gray-700 dark:text-gray-300 transition-colors uppercase tracking-widest"
                />
                <p className="text-[10px] text-gray-400 mt-2 px-1">রেফারেল কোড ব্যবহারে ৫০ অতিরিক্ত পয়েন্ট পাওয়া যায়।</p>
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
            </motion.div>
          )}
        </AnimatePresence>

        <button
          type="submit"
          disabled={
            saving || 
            !upazila || 
            (registrationType === 'Donor' && !bloodGroup) ||
            (registrationType === 'Organization' && (!orgName || !memberCount || !coverageArea || !orgDescription))
          }
          className={cn(
            "w-full py-5 px-6 rounded-[24px] font-black text-xl text-white transition-all shadow-2xl",
            saving || 
            !upazila || 
            (registrationType === 'Donor' && !bloodGroup) ||
            (registrationType === 'Organization' && (!orgName || !memberCount || !coverageArea || !orgDescription))
              ? "bg-gray-200 cursor-not-allowed text-gray-400"
              : "bg-red-600 hover:bg-red-700 shadow-red-500/30 active:scale-95"
          )}
        >
          {saving ? 'প্রোফাইল তৈরি হচ্ছে...' : 'নিবন্ধন সম্পন্ন করুন'}
        </button>
      </form>
    </div>
  );
}
