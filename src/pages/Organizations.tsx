import React, { useState, useEffect } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { Organization, OperationType } from '../types';
import { organizationService } from '../services/organizationService';
import { DIVISIONS, Division, District } from '../constants/locations';
import { handleFirestoreError } from '../lib/error-handler';
import { cn } from '../lib/utils';
import { Building2, Users, MapPin, Mail, Phone, ShieldCheck, Plus, Search, Globe, Info, CheckCircle2 } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';

export function Organizations() {
  const { user } = useAuth();
  const [organizations, setOrganizations] = useState<Organization[]>([]);
  const [loading, setLoading] = useState(true);
  const [showRegForm, setShowRegForm] = useState(false);
  const [searchTerm, setSearchTerm] = useState('');

  // Form state
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [phoneNumber, setPhoneNumber] = useState('');
  const [division, setDivision] = useState('');
  const [district, setDistrict] = useState('');
  const [upazila, setUpazila] = useState('');
  const [address, setAddress] = useState('');
  const [coverageArea, setCoverageArea] = useState('');
  const [description, setDescription] = useState('');
  const [memberCount, setMemberCount] = useState<string>('');
  const [type, setType] = useState<Organization['type']>('Club');
  const [saving, setSaving] = useState(false);

  // Filtered lists
  const [districts, setDistricts] = useState<District[]>([]);
  const [upazilas, setUpazilas] = useState<{ id: string; name: string; bnName: string }[]>([]);

  useEffect(() => {
    fetchOrganizations();
  }, []);

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

  const fetchOrganizations = async () => {
    setLoading(true);
    try {
      const data = await organizationService.getAll();
      setOrganizations(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoading(false);
    }
  };

  const handleRegister = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    setSaving(true);
    try {
      await organizationService.register({
        name,
        email,
        phoneNumber,
        location: { division, district, upazila },
        address,
        coverageArea,
        description,
        type,
        memberCount: parseInt(memberCount) || 1,
        adminUid: user.uid,
      });
      setShowRegForm(false);
      fetchOrganizations();
      // Reset form
      setName(''); setEmail(''); setPhoneNumber(''); setDivision(''); setDistrict(''); setUpazila(''); setDescription('');
      setAddress(''); setCoverageArea('');
    } catch (error) {
      handleFirestoreError(error, OperationType.WRITE, 'organizations');
    } finally {
      setSaving(false);
    }
  };

  const filteredOrgs = organizations.filter(org => 
    org.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    org.location.district.toLowerCase().includes(searchTerm.toLowerCase())
  );

  return (
    <div className="space-y-10 pb-20 animate-fade-in">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6">
        <div className="space-y-2">
          <h1 className="text-4xl font-black text-gray-900 dark:text-white tracking-tight">রক্তদান সংগঠনসমূহ</h1>
          <p className="text-gray-500 dark:text-gray-400 font-bold text-lg">আমাদের সাথে যুক্ত থাকা বিভিন্ন ক্লাব ও স্বেচ্ছাসেবী সংগঠন।</p>
        </div>
        <button 
          onClick={() => setShowRegForm(true)}
          className="px-8 py-4 bg-red-600 text-white rounded-2xl font-black text-lg shadow-xl shadow-red-500/20 hover:bg-red-700 transition-all flex items-center justify-center gap-2"
        >
          <Plus className="w-6 h-6" /> আপনার সংগঠন যুক্ত করুন
        </button>
      </div>

      <div className="grid lg:grid-cols-4 gap-8">
        <div className="lg:col-span-1 space-y-6">
          <div className="bg-white dark:bg-gray-900 p-6 rounded-[32px] shadow-xl border border-gray-100 dark:border-gray-800">
            <h3 className="text-lg font-black mb-4 flex items-center gap-2">
              <Search className="w-5 h-5 text-red-600" /> সার্চ করুন
            </h3>
            <input 
              type="text"
              placeholder="সংগঠনের নাম বা জেলা..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full px-4 py-3 bg-gray-50 dark:bg-gray-800 border-none rounded-xl focus:ring-2 focus:ring-red-500 outline-none font-bold"
            />
          </div>

          <div className="bg-indigo-600 rounded-[32px] p-8 text-white space-y-4 shadow-xl shadow-indigo-500/20 relative overflow-hidden">
             <Building2 className="absolute -bottom-10 -right-10 w-40 h-40 opacity-10 rotate-12" />
             <h3 className="text-2xl font-black relative z-10">সংগঠন কেন যুক্ত হবেন?</h3>
             <ul className="space-y-3 relative z-10 text-indigo-100 font-bold text-sm">
                <li className="flex items-start gap-2"><CheckCircle2 className="w-5 h-5 shrink-0" /> সংগঠনের পরিচিতি বাড়বে</li>
                <li className="flex items-start gap-2"><CheckCircle2 className="w-5 h-5 shrink-0" /> সরাসরি রিকোয়েস্ট ম্যানেজ করতে পারবেন</li>
                <li className="flex items-start gap-2"><CheckCircle2 className="w-5 h-5 shrink-0" /> মেম্বারদের ডোনেশন ট্র্যাক করা সহজ হবে</li>
             </ul>
          </div>
        </div>

        <div className="lg:col-span-3">
          {loading ? (
            <div className="grid md:grid-cols-2 gap-6">
              {[1, 2, 3, 4].map(i => <div key={i} className="h-48 bg-gray-100 dark:bg-gray-800 animate-pulse rounded-[32px]" />)}
            </div>
          ) : (
            <div className="grid md:grid-cols-2 gap-6">
              <AnimatePresence mode='popLayout'>
                {filteredOrgs.map((org) => (
                  <motion.div 
                    layout
                    initial={{ opacity: 0, scale: 0.9 }}
                    animate={{ opacity: 1, scale: 1 }}
                    exit={{ opacity: 0, scale: 0.9 }}
                    key={org.id} 
                    className="bg-white dark:bg-gray-900 p-8 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-xl hover:shadow-2xl transition-all group"
                  >
                    <div className="flex justify-between items-start mb-6">
                      <div className="w-16 h-16 bg-red-50 dark:bg-red-900/10 rounded-2xl flex items-center justify-center text-red-600">
                        <Building2 className="w-8 h-8" />
                      </div>
                      <div className="flex flex-col items-end gap-2">
                        <span className="px-3 py-1 bg-gray-100 dark:bg-gray-800 rounded-lg text-[10px] font-black uppercase tracking-widest text-gray-500">
                          {org.type}
                        </span>
                        {org.isVerified && (
                          <span className="flex items-center gap-1 text-blue-600 text-[10px] font-black uppercase">
                            <ShieldCheck className="w-3 h-3" /> ভেরিফাইড
                          </span>
                        )}
                      </div>
                    </div>

                    <div className="space-y-4">
                      <h3 className="text-2xl font-black text-gray-900 dark:text-white group-hover:text-red-600 transition-colors uppercase leading-none">
                        {org.name}
                      </h3>
                      <p className="text-gray-500 dark:text-gray-400 font-bold text-sm line-clamp-2">
                        {org.description || 'এই সংগঠন সম্পর্কে কোনো বিস্তারিত তথ্য নেই।'}
                      </p>
                      
                      <div className="grid grid-cols-2 gap-4 pt-4 border-t border-gray-50 dark:border-gray-800">
                        <div className="space-y-1">
                          <p className="text-[10px] uppercase font-black text-gray-400 tracking-widest">কাজের এলাকা</p>
                          <div className="flex items-center gap-2 text-gray-600 dark:text-gray-300 font-bold text-xs">
                            <MapPin className="w-4 h-4 text-red-500" /> {org.coverageArea || org.location.district}
                          </div>
                        </div>
                        <div className="space-y-1">
                          <p className="text-[10px] uppercase font-black text-gray-400 tracking-widest">সদস্য সংখ্যা</p>
                          <div className="flex items-center gap-2 text-gray-600 dark:text-gray-300 font-bold text-xs">
                            <Users className="w-4 h-4 text-indigo-500" /> {org.memberCount} জন
                          </div>
                        </div>
                      </div>

                      {org.address && (
                        <div className="flex items-start gap-2 text-gray-400 font-bold text-xs">
                          <Info className="w-4 h-4 text-gray-300 shrink-0 mt-0.5" />
                          <span>{org.address}</span>
                        </div>
                      )}

                      <div className="pt-6 flex flex-col sm:flex-row gap-3">
                         <Link 
                           to={`/organizations/${org.id}`} 
                           className="flex-1 py-3 bg-red-600 text-white rounded-xl text-center font-black transition-all hover:bg-red-700 active:scale-95 shadow-lg shadow-red-100 dark:shadow-none"
                         >
                            বিস্তারিত দেখুন
                         </Link>
                         <a href={`tel:${org.phoneNumber}`} className="flex-1 py-3 bg-gray-50 dark:bg-gray-800 hover:bg-gray-100 dark:hover:bg-gray-700 rounded-xl text-center font-black transition-all flex items-center justify-center gap-2">
                            <Phone className="w-4 h-4" /> ফোন
                         </a>
                      </div>
                    </div>
                  </motion.div>
                ))}
              </AnimatePresence>
              {filteredOrgs.length === 0 && (
                <div className="md:col-span-2 py-20 text-center text-gray-400 font-bold flex flex-col items-center gap-4">
                   <Users className="w-16 h-16 opacity-20" />
                   বর্তমানে কোনো সংগঠন পাওয়া যায়নি।
                </div>
              )}
            </div>
          )}
        </div>
      </div>

      {/* Registration Modal */}
      <AnimatePresence>
        {showRegForm && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setShowRegForm(false)}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            />
            <motion.div 
              initial={{ scale: 0.9, opacity: 0, y: 20 }}
              animate={{ scale: 1, opacity: 1, y: 0 }}
              exit={{ scale: 0.9, opacity: 0, y: 20 }}
              className="relative w-full max-w-2xl bg-white dark:bg-gray-900 rounded-[40px] shadow-2xl p-8 md:p-12 overflow-hidden max-h-[90vh] overflow-y-auto"
            >
              <div className="flex justify-between items-start mb-10">
                <div className="space-y-2">
                  <h2 className="text-3xl font-black text-gray-900 dark:text-white">সংগঠন নিবন্ধন</h2>
                  <p className="text-gray-500 font-bold">আপনার সংগঠনের তথ্য প্রদান করুন</p>
                </div>
                <button onClick={() => setShowRegForm(false)} className="w-12 h-12 rounded-2xl bg-gray-100 dark:bg-gray-800 text-gray-500 hover:bg-red-50 hover:text-red-500 transition-colors flex items-center justify-center font-black">✕</button>
              </div>

              <form onSubmit={handleRegister} className="space-y-6">
                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">সংগঠনের নাম</label>
                    <input 
                      required
                      value={name}
                      onChange={(e) => setName(e.target.value)}
                      placeholder="উদা: বন্ধন ব্লাড ক্লাব"
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">টাইপ</label>
                    <select 
                      value={type}
                      onChange={(e) => setType(e.target.value as any)}
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold"
                    >
                      <option value="Club">ক্লাব</option>
                      <option value="NGO">এনজিও</option>
                      <option value="Volunteer Group">স্বেচ্ছাসেবী সংগঠন</option>
                      <option value="Private">প্রাইভেট</option>
                    </select>
                  </div>
                </div>

                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">ফোন নম্বর</label>
                    <input 
                      required
                      type="tel"
                      value={phoneNumber}
                      onChange={(e) => setPhoneNumber(e.target.value)}
                      placeholder="০১৭XXXXXXXX"
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold"
                    />
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">মোট সদস্য সংখ্যা</label>
                    <input 
                      required
                      type="number"
                      value={memberCount}
                      onChange={(e) => setMemberCount(e.target.value)}
                      placeholder="উদা: ১০০"
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold"
                    />
                  </div>
                </div>

                <div className="grid md:grid-cols-2 gap-6">
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">কাজের এলাকা (Coverage Area)</label>
                    <input 
                      required
                      value={coverageArea}
                      onChange={(e) => setCoverageArea(e.target.value)}
                      placeholder="উদা: চট্টগ্রাম, লোহাগাড়া"
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold"
                    />
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">বিস্তারিত ঠিকানা</label>
                  <input 
                    required
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    placeholder="গ্রাম/রাস্তা, বিল্ডিং এর তথ্য"
                    className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold"
                  />
                </div>

                <div className="grid md:grid-cols-3 gap-4">
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">বিভাগ</label>
                    <select
                      required
                      value={division}
                      onChange={(e) => setDivision(e.target.value)}
                      className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-sm"
                    >
                      <option value="">নির্বাচন করুন</option>
                      {DIVISIONS.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">জেলা</label>
                    <select
                      required
                      disabled={!division}
                      value={district}
                      onChange={(e) => setDistrict(e.target.value)}
                      className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-sm disabled:opacity-50"
                    >
                      <option value="">নির্বাচন করুন</option>
                      {districts.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                    </select>
                  </div>
                  <div className="space-y-2">
                    <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">থানা</label>
                    <select
                      required
                      disabled={!district}
                      value={upazila}
                      onChange={(e) => setUpazila(e.target.value)}
                      className="w-full px-4 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-sm disabled:opacity-50"
                    >
                      <option value="">নির্বাচন করুন</option>
                      {upazilas.map(u => <option key={u.id} value={u.name}>{u.bnName}</option>)}
                    </select>
                  </div>
                </div>

                <div className="space-y-2">
                  <label className="text-xs font-black uppercase text-gray-400 tracking-widest pl-2">সংগঠন সম্পর্কে</label>
                  <textarea 
                    rows={4}
                    value={description}
                    onChange={(e) => setDescription(e.target.value)}
                    placeholder="সংগঠনের লক্ষ্য ও উদ্দেশ্য সম্পর্কে লিখুন..."
                    className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold resize-none"
                  />
                </div>

                <div className="p-6 bg-indigo-50 dark:bg-indigo-900/10 rounded-3xl border border-indigo-100 dark:border-indigo-800/50">
                  <p className="text-sm text-indigo-900 dark:text-indigo-400 font-bold leading-relaxed">
                    💡 <span className="text-red-600">পরামর্শ:</span> আপনার সংগঠনের সদস্যদের ব্যক্তিগতভাবে "রক্তদাতা" হিসেবে নিবন্ধন করতে উৎসাহিত করুন। এতে জরুরি প্রয়োজনে আপনাদের এলাকা থেকে দ্রুত রক্ত পাওয়ার সম্ভাবনা বৃদ্ধি পাবে।
                  </p>
                </div>

                <button 
                  type="submit" 
                  disabled={saving}
                  className="w-full py-5 bg-red-600 text-white rounded-[24px] font-black text-xl shadow-2xl shadow-red-500/30 hover:bg-red-700 transition-all disabled:opacity-50"
                >
                  {saving ? 'প্রক্রিয়াধীন...' : 'নিবন্ধন নিশ্চিত করুন'}
                </button>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
