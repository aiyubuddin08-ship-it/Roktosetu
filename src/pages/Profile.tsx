import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { doc, updateDoc, collection, getDocs, query, where, orderBy, addDoc, deleteDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { OperationType, BloodGroup, DonationRecord, Organization, DonationEvent, UserProfile } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { cn } from '../lib/utils';
import { Heart, MapPin, Phone, User as UserIcon, Calendar, CheckCircle, Save, LogOut, Award, Star, TrendingUp, Camera, Loader2, Droplets, Building2, History, ShieldCheck, Trash2, Plus, Users } from 'lucide-react';
import { DIVISIONS, District } from '../constants/locations';
import { auth, storage } from '../lib/firebase';
import { signOut } from 'firebase/auth';
import { ref, uploadBytes, getDownloadURL } from 'firebase/storage';
import { motion, AnimatePresence } from 'motion/react';
import { DonorCard } from '../components/DonorCard';
import { donationService } from '../services/donationService';
import { organizationService } from '../services/organizationService';

const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

const BADGES = [
  { id: 'new', label: 'নতুন সদস্য', icon: Award, color: 'text-gray-400', bg: 'bg-gray-50', minDonations: 0 },
  { id: 'hero', label: 'রক্তদান বীর', icon: Heart, color: 'text-green-600', bg: 'bg-green-50', minDonations: 1 },
  { id: 'lifesaver', label: 'জীবন রক্ষাকারী', icon: Star, color: 'text-blue-600', bg: 'bg-blue-50', minDonations: 5 },
  { id: 'guardian', label: 'অভিভাবক দূত', icon: ShieldCheck, color: 'text-purple-600', bg: 'bg-purple-50', minDonations: 10 },
  { id: 'legend', label: 'কিংবদন্তি দাতা', icon: Award, color: 'text-orange-600', bg: 'bg-orange-50', minDonations: 20 },
];

export function Profile() {
  const navigate = useNavigate();
  const { profile, user } = useAuth();
  const [saving, setSaving] = useState(false);
  const [success, setSuccess] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [donations, setDonations] = useState<DonationRecord[]>([]);
  const [loadingDonations, setLoadingDonations] = useState(true);
  const [userOrg, setUserOrg] = useState<Organization | null>(null);
  const [addingEvent, setAddingEvent] = useState(false);
  const [myEvents, setMyEvents] = useState<DonationEvent[]>([]);
  const [memberProfiles, setMemberProfiles] = useState<UserProfile[]>([]);
  const [loadingMembers, setLoadingMembers] = useState(false);
  const [showMemberModal, setShowMemberModal] = useState(false);
  const [confirmDeleteId, setConfirmDeleteId] = useState<string | null>(null);
  const [joinedCampaigns, setJoinedCampaigns] = useState<any[]>([]);
  const [loadingParticipations, setLoadingParticipations] = useState(true);
  const [viewingParticipantsEventId, setViewingParticipantsEventId] = useState<string | null>(null);
  const [currentEventParticipants, setCurrentEventParticipants] = useState<any[]>([]);
  const [loadingEventParticipants, setLoadingEventParticipants] = useState(false);

  // Form State
  const [bloodGroup, setBloodGroup] = useState<BloodGroup | ''>(profile?.bloodGroup || '');
  const [division, setDivision] = useState(profile?.location?.division || '');
  const [district, setDistrict] = useState(profile?.location?.district || '');
  const [upazila, setUpazila] = useState(profile?.location?.upazila || '');
  const [address, setAddress] = useState(profile?.location?.address || '');
  const [phoneNumber, setPhoneNumber] = useState(profile?.phoneNumber || '');
  const [isDonor, setIsDonor] = useState(profile?.isDonor ?? true);
  const [lastDonated, setLastDonated] = useState(profile?.lastDonated?.split('T')[0] || '');
  const [orgName, setOrgName] = useState(profile?.organizationName || '');
  const [hidePhoneNumber, setHidePhoneNumber] = useState(profile?.privacySettings?.hidePhoneNumber ?? false);
  const [hideLocation, setHideLocation] = useState(profile?.privacySettings?.hideLocation ?? false);
  const [referralCode, setReferralCode] = useState(profile?.referralCode || '');

  const [districts, setDistricts] = useState<District[]>([]);
  const [upazilas, setUpazilas] = useState<{ id: string; name: string; bnName: string }[]>([]);

  const fileInputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    if (user) {
      fetchDonations();
      fetchUserOrg();
      fetchParticipations();
    }
  }, [user]);

  const fetchParticipations = async () => {
    if (!user) return;
    try {
      const q = query(collection(db, 'campaign_participants'), where('userId', '==', user.uid), orderBy('joinedAt', 'desc'));
      const snap = await getDocs(q);
      setJoinedCampaigns(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    } catch (err) {
      console.error('Error fetching participations:', err);
    } finally {
      setLoadingParticipations(false);
    }
  };

  const fetchEventParticipants = async (eventId: string) => {
    if (!user) return;
    setLoadingEventParticipants(true);
    setViewingParticipantsEventId(eventId);
    try {
      // Fetch participants for this specific event
      const q = query(
        collection(db, 'campaign_participants'), 
        where('campaignId', '==', eventId)
      );
      const snap = await getDocs(q);
      setCurrentEventParticipants(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    } catch (err) {
      console.error('Error fetching event participants:', err);
    } finally {
      setLoadingEventParticipants(false);
    }
  };

  const fetchUserOrg = async () => {
    if (!user) return;
    try {
      const q = query(collection(db, 'organizations'), where('adminUid', '==', user.uid));
      const snap = await getDocs(q);
      if (!snap.empty) {
        const orgData = { id: snap.docs[0].id, ...snap.docs[0].data() } as Organization;
        setUserOrg(orgData);
        
        // Fetch registered members
        fetchOrgMembers(orgData.id);
        
        // Also fetch my campaigns
        const eq = query(collection(db, 'events'), where('orgId', '==', orgData.id), orderBy('date', 'desc'));
        const eSnap = await getDocs(eq);
        setMyEvents(eSnap.docs.map(d => ({ id: d.id, ...d.data() } as DonationEvent)));
      }
    } catch (e) { console.error(e); }
  };

  const fetchOrgMembers = async (orgId: string) => {
    setLoadingMembers(true);
    try {
      const q = query(collection(db, 'users'), where('organizationId', '==', orgId));
      const snap = await getDocs(q);
      setMemberProfiles(snap.docs.map(doc => doc.data() as UserProfile));
    } catch (err) {
      console.error('Error fetching members:', err);
    } finally {
      setLoadingMembers(false);
    }
  };

  const handleAddEvent = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!userOrg) return;
    
    const formData = new FormData(e.currentTarget);
    const title = formData.get('title') as string;
    const date = formData.get('date') as string;
    const location = formData.get('location') as string;
    const description = formData.get('description') as string;

    try {
      await addDoc(collection(db, 'events'), {
        title, date, location, description,
        orgId: userOrg.id,
        orgName: userOrg.name,
        orgAdminUid: user.uid, // Store admin UID for easier permission checking
        contactNumber: userOrg.phoneNumber,
        createdAt: new Date().toISOString()
      });
      setSuccess(true);
      setTimeout(() => setSuccess(false), 3000);
      setAddingEvent(false);
      fetchUserOrg();
    } catch (err) {
      console.error(err);
    }
  };

  const handleDeleteEvent = async (eventId: string) => {
    try {
      await deleteDoc(doc(db, 'events', eventId));
      fetchUserOrg();
    } catch (err) {
      console.error(err);
    } finally {
      setConfirmDeleteId(null);
    }
  };

  useEffect(() => {
    if (profile && !profile.referralCode && user) {
      const newCode = `RS${user.uid.substring(0, 6).toUpperCase()}`;
      updateDoc(doc(db, 'users', user.uid), { referralCode: newCode });
      setReferralCode(newCode);
    }
  }, [profile, user]);

  const fetchDonations = async () => {
    if (!user) return;
    try {
      const data = await donationService.getUserDonations(user.uid);
      setDonations(data);
    } catch (error) {
      console.error(error);
    } finally {
      setLoadingDonations(false);
    }
  };

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
        organizationName: orgName || null,
        role: profile?.role || 'user',
        privacySettings: {
          hidePhoneNumber,
          hideLocation
        }
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

  const [isEditing, setIsEditing] = useState(false);

  const currentBadge = [...BADGES].reverse().find(b => (profile.donationsCount || 0) >= b.minDonations) || BADGES[0];

  if (!isEditing) {
    return (
      <div className="max-w-4xl mx-auto pb-32 -mt-8 -mx-4 sm:-mx-6 lg:-mx-8 animate-fade-in bg-gray-50 dark:bg-gray-950 min-h-screen">
        {/* Header Section */}
        <section className="bg-white dark:bg-gray-900 pt-12 pb-6 px-6 relative z-10 transition-colors">
           <div className="max-w-xl mx-auto flex items-center justify-between">
              <button onClick={() => navigate(-1)} className="p-2 -ml-2 text-gray-900 dark:text-white">
                 <LogOut className="w-6 h-6 rotate-180" />
              </button>
              <h1 className="text-xl font-black text-gray-900 dark:text-white">প্রোফাইল</h1>
              <button 
                onClick={() => setIsEditing(true)}
                className="p-2 -mr-2 text-gray-900 dark:text-white"
              >
                 <UserIcon className="w-6 h-6" />
              </button>
           </div>
        </section>

        {/* Member List Modal */}
        <AnimatePresence>
          {showMemberModal && (
            <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
              <motion.div
                initial={{ opacity: 0 }}
                animate={{ opacity: 1 }}
                exit={{ opacity: 0 }}
                onClick={() => setShowMemberModal(false)}
                className="absolute inset-0 bg-gray-950/80 backdrop-blur-md"
              />
              <motion.div
                initial={{ opacity: 0, scale: 0.9, y: 20 }}
                animate={{ opacity: 1, scale: 1, y: 0 }}
                exit={{ opacity: 0, scale: 0.9, y: 20 }}
                className="relative bg-white dark:bg-gray-900 w-full max-w-lg rounded-[40px] shadow-2xl overflow-hidden flex flex-col max-h-[80vh]"
              >
                <div className="p-8 border-b dark:border-gray-800 flex items-center justify-between">
                  <div>
                    <h3 className="text-2xl font-black text-gray-900 dark:text-white">নিবন্ধিত সদস্যরা</h3>
                    <p className="text-xs font-bold text-gray-400 mt-1 uppercase tracking-widest">{userOrg?.name} এর সদস্যরা</p>
                  </div>
                  <button onClick={() => setShowMemberModal(false)} className="p-2 bg-gray-50 dark:bg-gray-800 rounded-full">
                    <LogOut className="w-6 h-6 rotate-180" />
                  </button>
                </div>
                
                <div className="flex-1 overflow-y-auto p-4 space-y-3 custom-scrollbar">
                  {loadingMembers ? (
                    <div className="py-20 flex flex-col items-center gap-4">
                      <Loader2 className="w-10 h-10 text-red-600 animate-spin" />
                      <p className="text-sm font-bold text-gray-400">সদস্য তালিকা লোড হচ্ছে...</p>
                    </div>
                  ) : memberProfiles.length > 0 ? (
                    memberProfiles.map((member) => (
                      <div key={member.uid} className="flex items-center gap-4 p-4 bg-gray-50 dark:bg-gray-800/50 rounded-3xl border border-gray-100 dark:border-gray-800 group hover:shadow-lg transition-all">
                        <div className="w-12 h-12 bg-red-100 dark:bg-red-900/20 rounded-full flex items-center justify-center font-black text-red-600 shrink-0">
                          {member.photoURL ? (
                            <img src={member.photoURL} alt="" className="w-full h-full rounded-full object-cover" />
                          ) : (
                            member.displayName.substring(0, 2)
                          )}
                        </div>
                        <div className="flex-1 min-w-0">
                          <p className="font-black text-gray-900 dark:text-white truncate">{member.displayName}</p>
                          <p className="text-xs font-bold text-gray-400">{member.bloodGroup} • {member.location.upazila}</p>
                        </div>
                        <div className="text-right shrink-0">
                          <div className="flex items-center gap-1 text-red-600 font-black text-sm">
                            <Star className="w-3 h-3 fill-current" /> {member.points || 0}
                          </div>
                          <p className="text-[10px] font-bold text-gray-400 uppercase tracking-widest">পয়েন্ট</p>
                        </div>
                      </div>
                    ))
                  ) : (
                    <div className="py-20 text-center space-y-4">
                      <Users className="w-16 h-16 text-gray-100 dark:text-gray-800 mx-auto" />
                      <p className="text-gray-400 font-bold uppercase tracking-widest text-xs">এখনো কোনো সদস্য নিবন্ধিত হয়নি</p>
                    </div>
                  )}
                </div>
              </motion.div>
            </div>
          )}
        </AnimatePresence>

        <div className="max-w-xl mx-auto px-6 space-y-6">
           {/* Profile Badge Info */}
           <div className="bg-[#FEF2F2] dark:bg-red-900/10 p-10 rounded-[40px] flex flex-col items-center text-center space-y-4">
              <div className="relative">
                 <div className="w-32 h-32 bg-brand rounded-full flex items-center justify-center text-white text-4xl font-black shadow-xl">
                    {profile.photoURL ? (
                      <img src={profile.photoURL} alt={profile.displayName} className="w-full h-full rounded-full object-cover" />
                    ) : (
                      profile.displayName.substring(0, 2)
                    )}
                 </div>
              </div>
              <div className="space-y-1">
                 <h2 className="text-2xl font-black text-brand tracking-tight">{profile.displayName}</h2>
                 <div className="flex items-center gap-2 justify-center">
                    <span className="w-8 h-8 bg-brand text-white rounded-full flex items-center justify-center font-black text-xs shadow-lg">{profile.bloodGroup || '?'}</span>
                    <span className="text-gray-500 font-bold text-sm tracking-tight">{profile.email}</span>
                 </div>
              </div>

              {/* Stats Row */}
              <div className="grid grid-cols-3 gap-8 w-full pt-4 border-t border-brand/10">
                 {!userOrg ? (
                   <>
                     <div className="space-y-1">
                        <p className="text-2xl font-black text-brand">{profile.donationsCount || 0}</p>
                        <p className="text-gray-500 font-bold text-[10px] uppercase tracking-widest">রক্তদান</p>
                     </div>
                     <div className="space-y-1 border-x border-brand/10">
                        <p className="text-2xl font-black text-brand">{profile.points || 0}</p>
                        <p className="text-gray-500 font-bold text-[10px] uppercase tracking-widest">পয়েন্ট</p>
                     </div>
                     <div className="space-y-1">
                        <p className="text-lg font-black text-brand leading-none truncate">{currentBadge.label.split(' ')[0]}</p>
                        <p className="text-gray-500 font-bold text-[10px] uppercase tracking-widest mt-1">ব্যাজ</p>
                     </div>
                   </>
                 ) : (
                   <>
                     <div className="space-y-1" onClick={() => setShowMemberModal(true)}>
                        <p className="text-2xl font-black text-brand">{memberProfiles.length}</p>
                        <p className="text-gray-500 font-bold text-[10px] uppercase tracking-widest">নিবন্ধিত সদস্য</p>
                     </div>
                     <div className="space-y-1 border-x border-brand/10">
                        <p className="text-2xl font-black text-brand">{myEvents.length}</p>
                        <p className="text-gray-500 font-bold text-[10px] uppercase tracking-widest">ক্যাম্পেইন</p>
                     </div>
                     <div className="space-y-1">
                        <p className="text-lg font-black text-brand leading-none truncate">{memberProfiles.reduce((acc, m) => acc + (m.points || 0), 0)}</p>
                        <p className="text-gray-500 font-bold text-[10px] uppercase tracking-widest mt-1">মোট পয়েন্ট</p>
                     </div>
                   </>
                 )}
              </div>
           </div>

           {/* Badge Progress / Organization Overview */}
           {!userOrg ? (
             <div className="bg-white dark:bg-gray-900 p-6 rounded-[40px] shadow-lg card-shadow border border-gray-50 dark:border-gray-800 flex items-center justify-between">
                <div className="space-y-1">
                   <p className="text-gray-900 dark:text-white font-black">ব্যাজ অগ্রগতি</p>
                   <p className="text-xs text-gray-400 font-bold">পরবর্তী লেভেলের জন্য আরও কয়েকবার রক্ত দিন</p>
                </div>
                <div className="flex items-center gap-2 text-yellow-500 font-black">
                   <Award className="w-6 h-6" />
                   <span className="text-sm">গোল্ড দাতা</span>
                </div>
             </div>
           ) : (
             <div className="bg-gradient-to-r from-red-600 to-rose-700 p-8 rounded-[40px] shadow-2xl text-white relative overflow-hidden group">
                <div className="relative z-10 flex items-center justify-between">
                   <div className="space-y-2">
                      <div className="flex items-center gap-2">
                         <ShieldCheck className="w-5 h-5" />
                         <span className="text-xs font-black uppercase tracking-widest opacity-80">মানবিক সংগঠন স্বীকৃতি</span>
                      </div>
                      <h3 className="text-2xl font-black">রক্তসেতু পার্টনার</h3>
                      <p className="text-xs font-bold opacity-70">আপনার সংগঠনের মানবিক কাজের জন্য কৃতজ্ঞতা</p>
                   </div>
                   <Award className="w-16 h-16 opacity-20 group-hover:scale-110 transition-transform" />
                </div>
             </div>
           )}

           {/* Organization Management */}
           {userOrg && (
             <section className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-lg card-shadow border-2 border-red-100 dark:border-red-900/30 space-y-6 transition-colors">
                <div className="flex items-center justify-between">
                   <div className="flex items-center gap-3">
                      <div className="w-12 h-12 bg-red-600 rounded-2xl flex items-center justify-center text-white shadow-lg">
                         <Building2 className="w-6 h-6" />
                      </div>
                      <div>
                         <h3 className="text-xl font-black text-gray-900 dark:text-white uppercase tracking-tight">{userOrg.name}</h3>
                         <p className="text-xs font-bold text-gray-400">সংগঠন পরিচালনা প্যানেল</p>
                      </div>
                   </div>
                   {!userOrg.isVerified && (
                      <span className="px-3 py-1 bg-yellow-100 text-yellow-600 rounded-full text-[10px] font-black uppercase flex items-center gap-1"><Loader2 className="w-3 h-3 animate-spin"/> পেন্ডিং</span>
                   )}
                </div>

                {!userOrg.isVerified ? (
                  <div className="text-center py-6 bg-gray-50 dark:bg-gray-800/50 rounded-3xl">
                     <Building2 className="w-12 h-12 text-gray-300 mx-auto mb-4" />
                     <p className="text-gray-500 dark:text-gray-400 font-bold max-w-sm mx-auto text-sm">আপনার সংগঠনটি এখন অ্যাডমিন ভেরিফিকেশনের অপেক্ষায় আছে। ভেরিফাই সম্পন্ন হলে আপনি ক্যাম্পেইন পরিচালনা ও অন্যান্য ফিচার ব্যবহার করতে পারবেন।</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                     <div className="flex items-center justify-between px-2">
                        <h4 className="text-sm font-black text-gray-900 dark:text-white uppercase tracking-widest">আপনার ক্যাম্পেইনসমূহ</h4>
                        <button 
                          onClick={() => setAddingEvent(!addingEvent)}
                          className="p-2 bg-red-50 dark:bg-red-900/20 text-red-600 rounded-xl"
                        >
                          <Plus className="w-5 h-5" />
                        </button>
                     </div>

                     {addingEvent && (
                        <form 
                          className="bg-gray-50 dark:bg-gray-800/50 p-6 rounded-3xl space-y-4 animate-in slide-in-from-top-4" 
                          onSubmit={handleAddEvent}
                        >
                           <input name="title" placeholder="ক্যাম্পেইন টাইটেল" className="w-full bg-white dark:bg-gray-900 p-4 rounded-2xl border-none outline-none font-bold text-sm" required />
                           <input name="date" type="date" className="w-full bg-white dark:bg-gray-900 p-4 rounded-2xl border-none outline-none font-bold text-sm" required />
                           <input name="location" placeholder="স্থান" className="w-full bg-white dark:bg-gray-900 p-4 rounded-2xl border-none outline-none font-bold text-sm" required />
                           <textarea name="description" placeholder="বিস্তারিত..." className="w-full bg-white dark:bg-gray-900 p-4 rounded-2xl border-none outline-none font-bold text-sm" required />
                           <button type="submit" className="w-full py-4 bg-red-600 text-white rounded-2xl font-black shadow-xl">পাবলিশ করুন</button>
                        </form>
                     )}

                     <div className="space-y-3">
                        {myEvents.map(ev => (
                           <div key={ev.id} className="space-y-2">
                              <div className="p-5 bg-gray-50 dark:bg-gray-800/50 rounded-3xl flex items-center justify-between">
                                 <div className="space-y-1">
                                    <p className="font-black text-gray-900 dark:text-white text-sm uppercase">{ev.title}</p>
                                    <p className="text-[10px] text-gray-400 font-bold">
                                      {new Date(ev.date).toLocaleDateString('bn-BD', { day: 'numeric', month: 'long', year: 'numeric' })} • {ev.location}
                                    </p>
                                 </div>
                                 <div className="flex items-center gap-2">
                                    <button 
                                      onClick={() => fetchEventParticipants(ev.id)}
                                      className="p-2 text-brand hover:bg-brand/10 rounded-xl transition-colors"
                                      title="অংশগ্রহণকারী দেখুন"
                                    >
                                      <Users className="w-5 h-5" />
                                    </button>
                                    <button 
                                      onClick={async () => {
                                         if(confirm('আপনি কি নিশ্চিত যে আপনি এই ক্যাম্পেইনটি মুছে ফেলতে চান?')) {
                                            await deleteDoc(doc(db, 'events', ev.id));
                                            fetchUserOrg();
                                         }
                                      }}
                                      className="p-2 text-gray-300 hover:text-red-600 transition-colors"
                                    >
                                       <Trash2 className="w-5 h-5" />
                                    </button>
                                 </div>
                              </div>
                              
                              {viewingParticipantsEventId === ev.id && (
                                 <div className="p-6 bg-white dark:bg-gray-900 rounded-[32px] border border-gray-100 dark:border-gray-800 shadow-inner animate-in slide-in-from-top-2">
                                  <div className="flex items-center justify-between mb-4">
                                     <h5 className="text-[10px] font-black uppercase text-gray-400 tracking-widest">অংশগ্রহণকারী ({currentEventParticipants.length})</h5>
                                     <button onClick={() => setViewingParticipantsEventId(null)} className="text-[10px] font-black text-red-600 hover:scale-105 transition-transform">বন্ধ করুন</button>
                                  </div>
                                  {loadingEventParticipants ? (
                                     <div className="flex justify-center py-6">
                                        <Loader2 className="w-6 h-6 text-brand animate-spin" />
                                     </div>
                                  ) : currentEventParticipants.length > 0 ? (
                                     <div className="space-y-3 max-h-60 overflow-y-auto pr-2 custom-scrollbar">
                                        {currentEventParticipants.map(participant => (
                                           <div key={participant.id} className="flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800/30 rounded-2xl border border-gray-50 dark:border-gray-800">
                                              <div className="flex items-center gap-3">
                                                 <div className="w-8 h-8 bg-brand/10 text-brand rounded-full flex items-center justify-center text-[10px] font-black">
                                                    {participant.userName.substring(0, 2)}
                                                 </div>
                                                 <span className="text-sm font-bold text-gray-900 dark:text-white">{participant.userName}</span>
                                              </div>
                                              <span className="text-[10px] font-black text-gray-400 uppercase">{new Date(participant.joinedAt).toLocaleDateString('bn-BD', { day: 'numeric', month: 'short' })}</span>
                                           </div>
                                        ))}
                                     </div>
                                  ) : (
                                     <div className="text-center py-8">
                                        <Users className="w-10 h-10 text-gray-100 dark:text-gray-800 mx-auto mb-2" />
                                        <p className="text-xs font-bold text-gray-400 italic">কেউ এখনো যুক্ত হয়নি</p>
                                     </div>
                                  )}
                               </div>
                            )}
                         </div>
                      ))}
                      {myEvents.length === 0 && !addingEvent && (
                         <p className="text-center py-4 text-gray-400 font-bold text-xs italic">আপনার কোনো ক্যাম্পেইন নেই</p>
                      )}
                   </div>
                </div>
                )}
             </section>
           )}

           {/* Referral Section */}
           <div className="bg-gradient-to-br from-indigo-500 to-purple-600 p-8 rounded-[40px] shadow-xl text-white relative overflow-hidden">
              <div className="relative z-10 space-y-4">
                 <div className="flex items-center gap-3">
                    <Award className="w-8 h-8" />
                    <h3 className="text-xl font-black italic">বন্ধুদের আমন্ত্রণ জানান!</h3>
                 </div>
                 <p className="text-sm font-bold opacity-90 leading-relaxed">
                    আপনার ফ্রেন্ডরা আপনার রেফারেল কোড ব্যবহার করে সাইন আপ করলে দুজনেই পাবেন ৫০ পয়েন্ট!
                 </p>
                 <div className="flex items-center gap-3 pt-2">
                    <div className="flex-1 bg-white/20 backdrop-blur-md rounded-2xl p-4 border border-white/30 font-black text-center tracking-widest text-xl">
                       {referralCode || 'RS-######'}
                    </div>
                    <button 
                      onClick={() => {
                        if (referralCode) {
                           navigator.clipboard.writeText(referralCode);
                           setSuccess(true);
                           setTimeout(() => setSuccess(false), 3000);
                         }
                       }}
                       className="p-4 bg-white text-indigo-600 rounded-2xl shadow-lg active:scale-95 transition-transform"
                     >
                        <Save className="w-6 h-6" />
                     </button>
                  </div>
               </div>
               <TrendingUp className="absolute -right-10 -bottom-10 w-48 h-48 text-white/10 rotate-12" />
            </div>

            {/* Privacy Settings */}
            <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-lg card-shadow border border-gray-50 dark:border-gray-800 space-y-6">
               <h3 className="text-lg font-black text-gray-900 dark:text-white">গোপনীয়তা সেটিংস</h3>
               
               <div className="space-y-4 divide-y dark:divide-gray-800">
                  <div className="flex items-center justify-between pb-4">
                    <div className="flex items-center gap-4">
                       <div className="w-10 h-10 bg-brand-muted dark:bg-brand/10 rounded-2xl flex items-center justify-center text-brand">
                          <Phone className="w-5 h-5" />
                       </div>
                       <div>
                          <p className="font-black text-gray-900 dark:text-white text-sm">ফোন নম্বর দেখান</p>
                          <p className="text-[10px] text-gray-400 font-bold">অন্যরা আপনার ফোন নম্বর দেখতে পাবে</p>
                       </div>
                    </div>
                    <button 
                      onClick={async () => {
                        const newValue = !hidePhoneNumber;
                        setHidePhoneNumber(newValue);
                        if (user) {
                          try {
                            await updateDoc(doc(db, 'users', user.uid), {
                              'privacySettings.hidePhoneNumber': newValue
                            });
                          } catch (err) {
                            console.error(err);
                          }
                        }
                      }}
                      className={cn(
                        "w-12 h-7 rounded-full transition-all relative",
                        !hidePhoneNumber ? "bg-brand" : "bg-gray-200 dark:bg-gray-700"
                      )}
                    >
                       <div className={cn("absolute top-1 w-5 h-5 bg-white rounded-full transition-all shadow-sm", !hidePhoneNumber ? "left-6" : "left-1")} />
                    </button>
                 </div>

                 <div className="flex items-center justify-between py-4">
                    <div className="flex items-center gap-4">
                       <div className="w-10 h-10 bg-brand-muted dark:bg-brand/10 rounded-2xl flex items-center justify-center text-brand">
                          <MapPin className="w-5 h-5" />
                       </div>
                       <div>
                          <p className="font-black text-gray-900 dark:text-white text-sm">ঠিকানা দেখান</p>
                          <p className="text-[10px] text-gray-400 font-bold">অন্যরা আপনার ঠিকানা দেখতে পাবে</p>
                       </div>
                    </div>
                    <button 
                      onClick={async () => {
                        const newValue = !hideLocation;
                        setHideLocation(newValue);
                        if (user) {
                          try {
                            await updateDoc(doc(db, 'users', user.uid), {
                              'privacySettings.hideLocation': newValue
                            });
                          } catch (err) {
                            console.error(err);
                          }
                        }
                      }}
                      className={cn(
                        "w-12 h-7 rounded-full transition-all relative",
                        !hideLocation ? "bg-brand" : "bg-gray-200 dark:bg-gray-700"
                      )}
                    >
                       <div className={cn("absolute top-1 w-5 h-5 bg-white rounded-full transition-all shadow-sm", !hideLocation ? "left-6" : "left-1")} />
                    </button>
                 </div>

                 <div className="flex items-center justify-between pt-4">
                    <div className="flex items-center gap-4">
                       <div className="w-10 h-10 bg-brand-muted dark:bg-brand/10 rounded-2xl flex items-center justify-center text-brand">
                          <Heart className="w-5 h-5" />
                       </div>
                       <div>
                          <p className="font-black text-gray-900 dark:text-white text-sm">উপলব্ধ আছি</p>
                          <p className="text-[10px] text-gray-400 font-bold">রক্তদানের জন্য প্রস্তুত</p>
                       </div>
                    </div>
                    <button 
                      onClick={async () => {
                        const newValue = !isDonor;
                        setIsDonor(newValue);
                        if (user) {
                          try {
                            await updateDoc(doc(db, 'users', user.uid), {
                              isDonor: newValue
                            });
                          } catch (err) {
                            console.error(err);
                          }
                        }
                      }}
                      className={cn(
                        "w-12 h-7 rounded-full transition-all relative",
                        isDonor ? "bg-brand" : "bg-gray-200 dark:bg-gray-700"
                      )}
                    >
                       <div className={cn("absolute top-1 w-5 h-5 bg-white rounded-full transition-all shadow-sm", isDonor ? "left-6" : "left-1")} />
                    </button>
                 </div>
              </div>
           </div>

           {/* Action Buttons Row */}
           <div className="grid grid-cols-3 gap-3">
              <button 
                className="flex items-center justify-center gap-2 py-4 bg-accent/5 dark:bg-accent/10 text-accent rounded-3xl font-black text-xs hover:bg-accent/10 transition-colors"
                onClick={async () => {
                  const shareData = {
                    title: 'রক্তসেতু',
                    text: `আমি রক্তসেতু অ্যাপে ${profile.bloodGroup} রক্তদাতা হিসেবে নিবন্ধিত। আপনিও যুক্ত হোন!`,
                    url: window.location.origin
                  };

                  if (navigator.share) {
                    try {
                      await navigator.share(shareData);
                    } catch (error) {
                      if (error instanceof Error && error.name !== 'AbortError') {
                        console.error('Share failed:', error);
                      }
                    }
                  } else {
                    try {
                      await navigator.clipboard.writeText(`${shareData.text} ${shareData.url}`);
                      setSuccess(true);
                      setTimeout(() => setSuccess(false), 3000);
                    } catch (err) {
                      console.error('Clipboard copy failed:', err);
                    }
                  }
                }}
              >
                 <TrendingUp className="w-4 h-4 rotate-45" /> শেয়ার
              </button>
              <button 
                onClick={() => setIsEditing(true)}
                className="flex items-center justify-center gap-2 py-4 bg-accent/5 dark:bg-accent/10 text-accent rounded-3xl font-black text-xs hover:bg-accent/10 transition-colors"
              >
                 <UserIcon className="w-4 h-4" /> সেটিংস
              </button>
              <button className="flex items-center justify-center gap-2 py-4 bg-accent/5 dark:bg-accent/10 text-accent rounded-3xl font-black text-[10px] leading-tight hover:bg-accent/10 transition-colors">
                 <ShieldCheck className="w-4 h-4" /> পাসওয়ার্ড
              </button>
           </div>

           {/* Joined Campaigns List */}
           <section className="space-y-4">
              <div className="flex items-center justify-between px-2">
                 <h3 className="text-lg font-black text-gray-900 dark:text-white">আপনার অংশগ্রহণকৃত ক্যাম্পেইন</h3>
              </div>
              <div className="bg-white dark:bg-gray-900 rounded-[40px] shadow-lg card-shadow border border-gray-50 dark:border-gray-800 overflow-hidden divide-y dark:divide-gray-800">
                 {loadingParticipations ? (
                    <div className="p-8 text-center text-gray-400">লোড হচ্ছে...</div>
                 ) : joinedCampaigns.length > 0 ? (
                    joinedCampaigns.map(camp => (
                       <div key={camp.id} className="p-6 flex items-center gap-4">
                          <div className="w-12 h-12 bg-green-50 dark:bg-green-900/20 rounded-full flex items-center justify-center text-green-600">
                             <Star className="w-5 h-5 fill-current" />
                          </div>
                          <div className="flex-1">
                             <p className="font-black text-gray-900 dark:text-white text-sm">{camp.campaignTitle}</p>
                             <p className="text-[10px] text-gray-400 font-bold uppercase">{camp.campaignLocation}</p>
                          </div>
                          <div className="text-right">
                            <p className="text-[10px] text-gray-400 font-black uppercase">
                              {new Date(camp.campaignDate).toLocaleDateString('bn-BD', { day: 'numeric', month: 'short' })}
                            </p>
                          </div>
                       </div>
                    ))
                 ) : (
                    <p className="p-10 text-center text-gray-400 text-sm font-bold">এখনো কোনো ক্যাম্পেইনে অংশগ্রহণ করেননি</p>
                 )}
              </div>
           </section>

           {/* Recent Donations List */}
           <section className="space-y-4">
              <div className="flex items-center justify-between px-2">
                 <h3 className="text-lg font-black text-gray-900 dark:text-white">সাম্প্রতিক রক্তদান</h3>
                 <button className="text-brand text-xs font-black">সব দেখুন</button>
              </div>
              <div className="bg-white dark:bg-gray-900 rounded-[40px] shadow-lg card-shadow border border-gray-50 dark:border-gray-800 overflow-hidden divide-y dark:divide-gray-800">
                 {loadingDonations ? (
                    <div className="p-8 text-center text-gray-400">লোড হচ্ছে...</div>
                 ) : donations.length > 0 ? (
                    donations.map(don => (
                       <div key={don.id} className="p-6 flex items-center gap-4">
                          <div className="w-12 h-12 bg-brand-muted dark:bg-brand/10 rounded-full flex items-center justify-center text-brand">
                             <Heart className="w-5 h-5 fill-current" />
                          </div>
                          <div className="flex-1">
                             <p className="font-black text-gray-900 dark:text-white text-sm">{don.hospitalName}</p>
                             <p className="text-[10px] text-gray-400 font-bold uppercase">{don.location}</p>
                          </div>
                          <p className="text-[10px] text-gray-400 font-black uppercase">{new Date(don.date).toLocaleDateString('bn-BD', { month: 'short', year: 'numeric' })}</p>
                       </div>
                    ))
                 ) : (
                    <p className="p-10 text-center text-gray-400 text-sm font-bold">এখনো কোনো ডোনেশন হিস্ট্রি নেই</p>
                 )}
              </div>
           </section>

           <button 
             onClick={() => signOut(auth)}
             className="w-full py-5 bg-brand-muted dark:bg-brand/10 text-brand font-black rounded-[32px] text-sm uppercase tracking-widest mt-4"
           >
              লগআউট করুন
           </button>
        </div>
      </div>
    );
  }

  // Edit Mode (matching image 1 style)
  return (
    <div className="max-w-4xl mx-auto pb-32 -mt-8 -mx-4 sm:-mx-6 lg:-mx-8 animate-fade-in bg-gray-50 dark:bg-gray-950 min-h-screen">
       {/* Header Section */}
       <section className="bg-white dark:bg-gray-900 pt-12 pb-6 px-6 relative z-10 transition-colors">
           <div className="max-w-xl mx-auto flex items-center gap-4">
              <button onClick={() => setIsEditing(false)} className="p-2 -ml-2 text-gray-900 dark:text-white">
                 <LogOut className="w-6 h-6 rotate-180" />
              </button>
              <h1 className="text-xl font-black text-gray-900 dark:text-white">অ্যাকাউন্ট</h1>
           </div>
        </section>

        <div className="max-w-xl mx-auto px-6 space-y-8 mt-4">
           <div>
              <h2 className="text-2xl font-black text-gray-900 dark:text-white">প্রোফাইল সম্পূর্ণ করুন</h2>
              <p className="text-gray-400 font-bold text-sm">আপনার তথ্য দিয়ে রক্তদাতা হিসেবে নিবন্ধন করুন</p>
           </div>

           <form onSubmit={handleSubmit} className="space-y-8">
              {/* Profile Image */}
              <div className="flex flex-col items-center">
                 <div className="relative group">
                    <div className="w-24 h-24 bg-white dark:bg-gray-900 rounded-full flex items-center justify-center border-4 border-brand-muted dark:border-brand/20 shadow-xl overflow-hidden">
                       {profile.photoURL ? (
                         <img src={profile.photoURL} alt="profile" className="w-full h-full object-cover" />
                       ) : (
                         <UserIcon className="w-10 h-10 text-gray-300" />
                       )}
                       {uploading && (
                         <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                            <Loader2 className="w-6 h-6 text-white animate-spin" />
                         </div>
                       )}
                    </div>
                    <button 
                      type="button"
                      onClick={() => fileInputRef.current?.click()}
                      className="absolute bottom-0 right-0 w-8 h-8 bg-brand text-white rounded-full flex items-center justify-center border-2 border-white dark:border-gray-900 shadow-lg"
                    >
                       <Camera className="w-4 h-4" />
                    </button>
                    <input type="file" ref={fileInputRef} onChange={handleImageUpload} className="hidden" accept="image/*" />
                 </div>
              </div>

              {/* Name Field - Outlined Style matching Image 1 */}
              <div className="relative pt-2">
                 <div className="absolute -top-1 left-4 px-2 bg-gray-50 dark:bg-gray-950 text-[10px] font-black text-gray-400 uppercase tracking-widest z-10">পুরো নাম *</div>
                 <div className="flex items-center gap-4 bg-white dark:bg-gray-900 px-6 py-5 rounded-[20px] border border-gray-200 dark:border-gray-800 transition-all focus-within:border-brand">
                    <UserIcon className="w-5 h-5 text-gray-400" />
                    <input 
                      type="text" 
                      value={profile.displayName} 
                      readOnly
                      className="w-full bg-transparent outline-none font-bold text-gray-600 dark:text-gray-400"
                    />
                 </div>
              </div>

              {/* Email Field */}
              <div className="relative pt-2">
                 <div className="absolute -top-1 left-4 px-2 bg-gray-50 dark:bg-gray-950 text-[10px] font-black text-gray-400 uppercase tracking-widest z-10">ইমেইল (ঐচ্ছিক)</div>
                 <div className="flex items-center gap-4 bg-white dark:bg-gray-900 px-6 py-5 rounded-[20px] border border-gray-200 dark:border-gray-800 transition-all">
                    <EnvelopeIcon className="w-5 h-5 text-gray-400" />
                    <input 
                      type="email" 
                      value={profile.email} 
                      readOnly
                      className="w-full bg-transparent outline-none font-bold text-gray-600 dark:text-gray-400"
                    />
                 </div>
              </div>

              {/* Blood Group Selection */}
              <div className="space-y-4">
                 <label className="text-sm font-black text-gray-900 dark:text-white block px-2">রক্তের গ্রুপ *</label>
                 <div className="grid grid-cols-4 gap-3">
                    {BLOOD_GROUPS.map(group => (
                       <button
                         key={group}
                         type="button"
                         onClick={() => setBloodGroup(group)}
                         className={cn(
                           "py-4 rounded-[16px] text-sm font-black transition-all border-2",
                           bloodGroup === group 
                             ? "bg-accent/10 border-accent/20 text-accent" 
                             : "bg-white dark:bg-gray-900 border-gray-100 dark:border-gray-800 text-gray-400"
                         )}
                       >
                          {group}
                       </button>
                    ))}
                 </div>
              </div>

              {/* Address Fields */}
              <div className="space-y-4">
                 <label className="text-sm font-black text-gray-900 dark:text-white block px-2">ঠিকানা *</label>
                 
                 <div className="space-y-6">
                    {/* Division Select */}
                    <div className="relative">
                       <div className="absolute -top-1 left-4 px-2 bg-gray-50 dark:bg-gray-950 text-[10px] font-black text-gray-400 uppercase tracking-widest z-10">বিভাগ</div>
                       <select 
                         value={division} 
                         onChange={(e) => setDivision(e.target.value)}
                         className="w-full bg-white dark:bg-gray-900 px-6 py-5 rounded-[20px] border border-gray-200 dark:border-gray-800 outline-none font-bold text-gray-700 dark:text-gray-300 appearance-none transition-all focus:border-brand"
                       >
                          <option value="">বিভাগ নির্বাচন করুন</option>
                          {DIVISIONS.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                       </select>
                    </div>

                    {/* District Select */}
                    <div className="relative">
                       <div className="absolute -top-1 left-4 px-2 bg-gray-50 dark:bg-gray-950 text-[10px] font-black text-gray-400 uppercase tracking-widest z-10">জেলা</div>
                       <select 
                         value={district} 
                         disabled={!division}
                         onChange={(e) => setDistrict(e.target.value)}
                         className="w-full bg-white dark:bg-gray-900 px-6 py-5 rounded-[20px] border border-gray-200 dark:border-gray-800 outline-none font-bold text-gray-700 dark:text-gray-300 appearance-none disabled:opacity-50 transition-all focus:border-brand"
                       >
                          <option value="">জেলা নির্বাচন করুন</option>
                          {districts.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                       </select>
                    </div>

                    {/* Upazila Select */}
                    <div className="relative">
                       <div className="absolute -top-1 left-4 px-2 bg-gray-50 dark:bg-gray-950 text-[10px] font-black text-brand uppercase tracking-widest z-10">উপজেলা</div>
                       <select 
                         value={upazila} 
                         disabled={!district}
                         onChange={(e) => setUpazila(e.target.value)}
                         className="w-full bg-white dark:bg-gray-900 px-6 py-5 rounded-[20px] border-2 border-brand outline-none font-bold text-gray-700 dark:text-gray-300 appearance-none disabled:opacity-50 transition-all"
                       >
                          <option value="">উপজেলা নির্বাচন করুন</option>
                          {upazilas.map(u => <option key={u.id} value={u.name}>{u.bnName}</option>)}
                       </select>
                    </div>

                    {/* Detailed Address */}
                    <div className="relative">
                       <div className="flex items-start gap-4 bg-white dark:bg-gray-900 px-6 py-5 rounded-[20px] border border-gray-200 dark:border-gray-800">
                          <MapPin className="w-5 h-5 text-gray-400 mt-1" />
                          <textarea 
                            rows={3}
                            placeholder="বিস্তারিত ঠিকানা"
                            value={address}
                            onChange={(e) => setAddress(e.target.value)}
                            className="w-full bg-transparent outline-none font-bold text-gray-700 dark:text-gray-300 resize-none"
                          />
                       </div>
                    </div>
                 </div>
              </div>

              <div className="flex flex-col gap-4">
                 <button 
                   type="submit" 
                   disabled={saving}
                   className="w-full py-6 bg-brand text-white font-black text-lg rounded-[28px] shadow-2xl shadow-brand/30 hover:bg-brand/90 transition-all active:scale-95 disabled:opacity-50"
                 >
                    {saving ? 'সংরক্ষণ হচ্ছে...' : 'সংরক্ষণ করুন'}
                 </button>
                 <button 
                   type="button"
                   onClick={() => setIsEditing(false)}
                   className="w-full py-5 bg-gray-100 dark:bg-gray-800 text-gray-500 font-bold rounded-[28px]"
                 >
                    বাতিল করুন
                 </button>
              </div>
           </form>
           
           <AnimatePresence>
            {success && (
              <motion.div 
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                exit={{ opacity: 0 }}
                className="fixed bottom-10 left-1/2 -translate-x-1/2 bg-accent text-white px-8 py-4 rounded-3xl font-black shadow-2xl"
              >
                 সফলভাবে সংরক্ষণ করা হয়েছে!
              </motion.div>
            )}
           </AnimatePresence>
        </div>
    </div>
  );
};

// Helper mock icon for email
const EnvelopeIcon = ({ className }: { className?: string }) => (
  <svg className={className} fill="none" stroke="currentColor" viewBox="0 0 24 24">
    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
  </svg>
);
