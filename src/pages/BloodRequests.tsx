import React, { useState, useEffect } from 'react';
import { collection, query, orderBy, onSnapshot, addDoc, deleteDoc, doc, updateDoc, getDocs, where, increment, getDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { BloodRequest, BloodGroup, Urgency, OperationType, UserProfile } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Plus, Heart, MapPin, Phone, Calendar, Clock, X, AlertCircle, User, MessageSquare, Building2, Star, CheckCircle2, Share2 } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { bn } from 'date-fns/locale';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';
import { DIVISIONS, Division, District } from '../constants/locations';
import { sendNotification } from '../lib/notifications';

const BLOOD_GROUPS: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];
const URGENCY_LEVELS: Urgency[] = ['Low', 'Normal', 'High', 'Critical'];

const URGENCY_MAP: Record<Urgency, string> = {
  'Low': 'সাধারণ',
  'Normal': 'জরুরি',
  'High': 'খুব জরুরি',
  'Critical': 'সুপার ইমারজেন্সি'
};

const FinancialWarning = () => (
  <div className="bg-orange-50 border border-orange-100 p-6 rounded-[32px] flex gap-4 items-start mb-8">
    <AlertCircle className="w-6 h-6 text-orange-500 shrink-0 mt-1" />
    <div>
      <h4 className="text-orange-900 font-black text-lg">আর্থিক লেনদেন থেকে সাবধান!</h4>
      <p className="text-orange-700 text-sm font-bold">রক্তদান একটি মহৎ কাজ। দয়া করে এর বিনিময়ে কোনো অর্থ লেনদেন করবেন না। কেউ টাকা চাইলে সাথে সাথে কর্তৃপক্ষের সাহায্য নিন।</p>
    </div>
  </div>
);

export function BloodRequests() {
  const { user, profile } = useAuth();
  const [requests, setRequests] = useState<BloodRequest[]>([]);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [loading, setLoading] = useState(true);

  // Completion Modal State
  const [completingId, setCompletingId] = useState<string | null>(null);
  const [rating, setRating] = useState(5);
  const [donors, setDonors] = useState<UserProfile[]>([]);
  const [selectedDonorId, setSelectedDonorId] = useState<string>('');
  const [processingCompletion, setProcessingCompletion] = useState(false);

  const handleShare = async (req: BloodRequest) => {
    const text = `রক্তের অনুরোধ: ${req.bloodGroup} রক্ত প্রয়োজন। \nহাসপাতাল: ${req.hospitalName}\nঅবস্থান: ${req.location.upazila}, ${req.location.district}\nযোগাযোগ: ${req.contactNumber}\n\nরক্তসেতু অ্যাপের মাধ্যমে রক্ত দান করুন।`;
    if (navigator.share) {
      try {
        await navigator.share({
          title: 'রক্তের অনুরোধ - রক্তসেতু',
          text: text,
          url: window.location.href
        });
      } catch (err) {
        console.error('Error sharing:', err);
      }
    } else {
      navigator.clipboard.writeText(text);
      alert('সফলভাবে কপি হয়েছে!');
    }
  };

  // Form State
  const [bloodGroup, setBloodGroup] = useState<BloodGroup | ''>('');
  const [urgency, setUrgency] = useState<Urgency>('Normal');
  const [division, setDivision] = useState('');
  const [district, setDistrict] = useState('');
  const [upazila, setUpazila] = useState('');
  const [address, setAddress] = useState('');
  const [hospitalName, setHospitalName] = useState('');
  const [patientName, setPatientName] = useState('');
  const [condition, setCondition] = useState('');
  const [contactNumber, setContactNumber] = useState('');
  const [requiredDate, setRequiredDate] = useState('');
  const [message, setMessage] = useState('');
  const [submitting, setSubmitting] = useState(false);

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

  useEffect(() => {
    const q = query(
      collection(db, 'requests'),
      where('status', '==', 'Active'),
      orderBy('createdAt', 'desc')
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      const reqs = snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as BloodRequest));
      setRequests(reqs);
      setLoading(false);
    }, (error) => {
      handleFirestoreError(error, OperationType.LIST, 'requests');
    });

    return () => unsubscribe();
  }, []);

  useEffect(() => {
    if (completingId) {
      // Fetch some potential donors to suggest
      const fetchDonors = async () => {
        const q = query(collection(db, 'users'), where('isDonor', '==', true));
        const snap = await getDocs(q);
        setDonors(snap.docs.map(doc => doc.data() as UserProfile));
      };
      fetchDonors().catch(console.error);
    }
  }, [completingId]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !bloodGroup || !upazila) return;

    setSubmitting(true);
    try {
      const newRequest: Omit<BloodRequest, 'id'> = {
        requesterId: user.uid,
        requesterName: profile?.displayName || user.displayName || 'Anonymous',
        bloodGroup: bloodGroup as BloodGroup,
        urgency,
        location: { division, district, upazila, address },
        hospitalName,
        patientName,
        condition,
        contactNumber,
        requiredDate,
        message,
        status: 'Active',
        createdAt: new Date().toISOString()
      };

      const requestDoc = await addDoc(collection(db, 'requests'), newRequest);
      
      // Notify matching donors in the same district
      const donorsQuery = query(
        collection(db, 'users'),
        where('bloodGroup', '==', bloodGroup),
        where('isDonor', '==', true),
        where('location.district', '==', district)
      );
      const donorsSnap = await getDocs(donorsQuery);
      
      const notificationPromises = donorsSnap.docs
        .filter(doc => doc.id !== user.uid)
        .map(donorDoc => sendNotification({
          userId: donorDoc.id,
          title: 'নতুন রক্তের অনুরোধ!',
          message: `${district} জেলায় ${bloodGroup} রক্তের জরুরি প্রয়োজন।`,
          type: 'warning',
          isRead: false,
          requestId: requestDoc.id,
          link: '/requests'
        }));
      
      await Promise.all(notificationPromises);

      setIsModalOpen(false);
      resetForm();
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'requests');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCompleteRequest = async () => {
    if (!completingId || !selectedDonorId) return;

    setProcessingCompletion(true);
    try {
      // 1. Update request status
      await updateDoc(doc(db, 'requests', completingId), {
        status: 'Fulfilled',
        donorId: selectedDonorId,
        rating: rating,
        completedAt: new Date().toISOString()
      });

      // 2. Update donor profile (points, rating, donationsCount)
      const donorRef = doc(db, 'users', selectedDonorId);
      const donorSnap = await getDoc(donorRef);
      if (donorSnap.exists()) {
        const donorData = donorSnap.data() as UserProfile;
        const newRatingCount = (donorData.ratingCount || 0) + 1;
        const newRatingAverage = ((donorData.ratingAverage || 0) * (donorData.ratingCount || 0) + rating) / newRatingCount;
        
        await updateDoc(donorRef, {
          donationsCount: increment(1),
          points: increment(100), // Default points per donation
          ratingCount: newRatingCount,
          ratingAverage: newRatingAverage,
          lastDonated: new Date().toISOString()
        });

        // 3. Send notification to donor
        await sendNotification({
          userId: selectedDonorId,
          title: 'রক্তদান সম্পন্ন হয়েছে!',
          message: `আপনার রক্তদানের জন্য ধন্যবাদ। আপনি ১০০ পয়েন্ট অর্জন করেছেন। আপনার গড় রেটিং এখন ${newRatingAverage.toFixed(1)}।`,
          type: 'success',
          isRead: false,
          requestId: completingId,
          link: '/profile'
        });
      }

      setCompletingId(null);
      setSelectedDonorId('');
      setRating(5);
    } catch (error) {
      handleFirestoreError(error, OperationType.UPDATE, `requests/${completingId}`);
    } finally {
      setProcessingCompletion(false);
    }
  };

  const resetForm = () => {
    setBloodGroup('');
    setUrgency('Normal');
    setDivision('');
    setDistrict('');
    setUpazila('');
    setAddress('');
    setHospitalName('');
    setPatientName('');
    setCondition('');
    setContactNumber('');
    setRequiredDate('');
    setMessage('');
  };

  const [deletingId, setDeletingId] = useState<string | null>(null);

  const handleDelete = async (requestId: string) => {
    try {
      await deleteDoc(doc(db, 'requests', requestId));
      setDeletingId(null);
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, `requests/${requestId}`);
    }
  };

  return (
    <div className="space-y-8 pb-20">
      <FinancialWarning />
      
      <div className="flex flex-col sm:flex-row items-center justify-between gap-6">
        <div>
          <h1 className="text-4xl font-black text-gray-900">রক্তের অনুরোধ</h1>
          <p className="text-gray-500 mt-2 text-lg">মানুষের জীবন বাঁচাতে এগিয়ে আসুন</p>
        </div>
        <button
          onClick={() => setIsModalOpen(true)}
          className="flex items-center gap-3 px-8 py-4 bg-red-600 text-white rounded-3xl font-black text-lg hover:bg-red-700 transition shadow-2xl shadow-red-200"
        >
          <Plus className="w-6 h-6" /> নতুন অনুরোধ
        </button>
      </div>

      {loading ? (
        <div className="grid md:grid-cols-2 gap-8">
          {[1, 2, 3, 4].map(i => (
            <div key={i} className="h-64 bg-white rounded-[40px] shimmer border border-gray-100" />
          ))}
        </div>
      ) : requests.length === 0 ? (
        <div className="bg-white p-20 rounded-[40px] text-center border-2 border-dashed border-gray-200">
          <Heart className="w-20 h-20 text-gray-200 mx-auto mb-6" />
          <p className="text-gray-400 text-xl font-bold">বর্তমানে কোনো সক্রিয় রক্তের অনুরোধ নেই</p>
        </div>
      ) : (
        <div className="grid md:grid-cols-2 gap-8">
          <AnimatePresence>
            {requests.map((req) => (
              <motion.div
                key={req.id}
                layout
                initial={{ opacity: 0, scale: 0.9 }}
                animate={{ opacity: 1, scale: 1 }}
                exit={{ opacity: 0, scale: 0.9 }}
                className="bg-white rounded-[40px] shadow-xl border border-gray-50 overflow-hidden group hover:shadow-2xl transition-all"
              >
                <div className="p-8">
                  <div className="flex items-start justify-between mb-8">
                    <div className="flex items-center gap-5">
                      <div className={cn(
                        "w-20 h-20 rounded-3xl flex flex-col items-center justify-center font-black",
                        req.urgency === 'Critical' ? "bg-red-600 text-white shadow-2xl shadow-red-200" : "bg-red-50 text-red-600 shadow-inner"
                      )}>
                        <span className="text-3xl">{req.bloodGroup}</span>
                        <span className="text-[10px] uppercase tracking-widest mt-1 opacity-80">গ্রুপ</span>
                      </div>
                      <div>
                        <h3 className="text-2xl font-black text-gray-900">{req.hospitalName}</h3>
                        <div className="flex items-center gap-2 mt-2">
                           <span className={cn(
                             "px-4 py-1.5 rounded-full text-xs font-black uppercase tracking-widest",
                             req.urgency === 'Critical' ? "bg-red-100 text-red-700" : "bg-gray-100 text-gray-600"
                           )}>
                             {URGENCY_MAP[req.urgency]}
                           </span>
                           <span className="text-sm text-gray-400 font-bold flex items-center gap-1.5 ml-2">
                             <Clock className="w-4 h-4" /> {formatDistanceToNow(new Date(req.createdAt), { locale: bn })} আগে
                           </span>
                        </div>
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-2 gap-6 mb-8 bg-gray-50 p-6 rounded-3xl">
                    <div className="space-y-1">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">রোগীর নাম</p>
                      <p className="text-gray-700 font-bold flex items-center gap-2 font-bangla"><User className="w-4 h-4 text-red-400" /> {req.patientName || 'গোপন'}</p>
                    </div>
                    <div className="space-y-1">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">অবস্থান</p>
                      <p className="text-gray-700 font-bold flex items-center gap-2 truncate"><MapPin className="w-4 h-4 text-red-400" /> {req.location?.upazila}, {req.location?.district}</p>
                    </div>
                    <div className="space-y-1">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">বর্তমান অবস্থা</p>
                      <p className="text-gray-700 font-bold flex items-center gap-2 italic">{req.condition || 'জরুরি'}</p>
                    </div>
                    <div className="space-y-1">
                      <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">প্রয়োজন</p>
                      <p className="text-gray-700 font-bold flex items-center gap-2"><Calendar className="w-4 h-4 text-red-400" /> {req.requiredDate}</p>
                    </div>
                  </div>

                  {req.message && (
                    <div className="mb-8 p-5 bg-yellow-50/50 border border-yellow-100 rounded-2xl flex gap-3 italic text-gray-600 leading-relaxed font-medium">
                      <MessageSquare className="w-5 h-5 text-yellow-500 shrink-0" />
                      {req.message}
                    </div>
                  )}

                  <div className="flex flex-wrap items-center justify-between gap-4 pt-6 border-t border-gray-100">
                    <div className="flex items-center gap-4">
                      <a
                        href={`tel:${req.contactNumber}`}
                        className="flex items-center gap-3 px-6 py-4 bg-green-600 text-white rounded-2xl font-black shadow-xl shadow-green-100 hover:bg-green-700 transition-all hover:scale-105"
                      >
                        <Phone className="w-5 h-5" /> কল করুন
                      </a>
                      <button
                        onClick={() => handleShare(req)}
                        className="p-4 bg-gray-50 text-gray-500 rounded-2xl hover:bg-gray-100 hover:text-red-600 transition-all font-black flex items-center gap-2"
                      >
                        <Share2 className="w-5 h-5" /> শেয়ার
                      </button>
                      {user?.uid === req.requesterId && (
                        <div className="flex items-center gap-2">
                           <button
                             onClick={() => setCompletingId(req.id)}
                             className="flex items-center gap-2 px-6 py-4 bg-blue-600 text-white rounded-2xl font-black shadow-xl shadow-blue-100 hover:bg-blue-700 transition-all"
                           >
                             <CheckCircle2 className="w-5 h-5" /> ডোনেট করা হয়েছে
                           </button>
                           <button
                             onClick={() => setDeletingId(req.id)}
                             className="w-12 h-12 flex items-center justify-center text-gray-300 hover:text-red-600 hover:bg-red-50 rounded-2xl transition-all"
                           >
                             <X className="w-6 h-6" />
                           </button>
                        </div>
                      )}
                    </div>
                    <div className="text-right">
                       <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mb-1">আবেদনকারী</p>
                       <p className="text-gray-900 font-black">{req.requesterName}</p>
                    </div>
                  </div>
                </div>
              </motion.div>
            ))}
          </AnimatePresence>
        </div>
      )}

      {/* Complete & Rate Modal */}
      <AnimatePresence>
        {completingId && (
          <div className="fixed inset-0 z-[70] flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setCompletingId(null)}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            />
            <motion.div
              initial={{ opacity: 0, y: 50 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 50 }}
              className="relative bg-white p-10 rounded-[40px] max-w-md w-full shadow-2xl"
            >
              <h3 className="text-2xl font-black text-gray-900 mb-6 text-center">রক্তদান সম্পন্ন হয়েছে?</h3>
              
              <div className="space-y-8">
                <div>
                   <label className="block text-xs font-black text-gray-400 uppercase tracking-widest mb-3">কাকে রেটিং দিতে চান?</label>
                   <select
                     value={selectedDonorId}
                     onChange={(e) => setSelectedDonorId(e.target.value)}
                     className="w-full px-6 py-4 bg-gray-50 border-none rounded-3xl outline-none font-bold text-gray-700"
                   >
                     <option value="">রক্তদাতা নির্বাচন করুন</option>
                     {donors.map(donor => (
                       <option key={donor.uid} value={donor.uid}>{donor.displayName} ({donor.bloodGroup})</option>
                     ))}
                   </select>
                </div>

                <div>
                   <label className="block text-xs font-black text-gray-400 uppercase tracking-widest mb-3 text-center">ডোনারের রেটিং দিন</label>
                   <div className="flex justify-center gap-2">
                     {[1, 2, 3, 4, 5].map(star => (
                       <button
                         key={star}
                         onClick={() => setRating(star)}
                         className="p-2 transition-transform active:scale-90"
                       >
                         <Star className={cn(
                           "w-10 h-10 transition-colors",
                           star <= rating ? "fill-yellow-400 text-yellow-400" : "text-gray-200"
                         )} />
                       </button>
                     ))}
                   </div>
                </div>

                <div className="flex gap-4 pt-4">
                   <button
                     onClick={() => setCompletingId(null)}
                     className="flex-1 py-4 border border-gray-100 rounded-3xl font-black text-gray-500 hover:bg-gray-50 transition-all"
                   >
                     বাতিল
                   </button>
                   <button
                     disabled={!selectedDonorId || processingCompletion}
                     onClick={handleCompleteRequest}
                     className="flex-1 py-4 bg-green-600 text-white rounded-3xl font-black shadow-xl shadow-green-100 hover:bg-green-700 transition-all disabled:opacity-50"
                   >
                     {processingCompletion ? 'প্রসেস হচ্ছে...' : 'সম্পন্ন করুন'}
                   </button>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* Delete Confirmation Modal */}
      <AnimatePresence>
        {deletingId && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setDeletingId(null)}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            />
            <motion.div
              initial={{ opacity: 0, scale: 0.9 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.9 }}
              className="relative bg-white p-10 rounded-[40px] max-w-sm w-full text-center shadow-2xl"
            >
              <div className="w-20 h-20 bg-red-100 rounded-3xl flex items-center justify-center text-red-600 mx-auto mb-6 shadow-inner">
                <AlertCircle className="w-10 h-10" />
              </div>
              <h3 className="text-2xl font-black text-gray-900 mb-3">অনুরোধ মুছে ফেলবেন?</h3>
              <p className="text-gray-500 mb-8 font-medium">এটি স্থায়ীভাবে মুছে ফেলা হবে। আপনি কি নিশ্চিত?</p>
              <div className="flex gap-4">
                <button
                  onClick={() => setDeletingId(null)}
                  className="flex-1 px-6 py-4 border border-gray-100 rounded-2xl font-black text-gray-600 hover:bg-gray-50 transition-all"
                >
                  না
                </button>
                <button
                  onClick={() => deletingId && handleDelete(deletingId)}
                  className="flex-1 px-6 py-4 bg-red-600 text-white rounded-2xl font-black hover:bg-red-700 shadow-xl shadow-red-100 transition-all"
                >
                  হ্যাঁ
                </button>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>

      {/* New Request Modal */}
      <AnimatePresence>
        {isModalOpen && (
          <div className="fixed inset-0 z-[60] flex items-center justify-center p-4">
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              onClick={() => setIsModalOpen(false)}
              className="absolute inset-0 bg-black/60 backdrop-blur-sm"
            />
            <motion.div
              initial={{ opacity: 0, y: 50, scale: 0.95 }}
              animate={{ opacity: 1, y: 0, scale: 1 }}
              exit={{ opacity: 0, y: 50, scale: 0.95 }}
              className="relative bg-white rounded-[40px] max-w-2xl w-full shadow-2xl overflow-hidden max-h-[90vh] flex flex-col"
            >
              <div className="p-8 border-b flex items-center justify-between bg-red-600 text-white shrink-0">
                <h2 className="text-2xl font-black">নতুন রক্তের অনুরোধ</h2>
                <button 
                  onClick={() => setIsModalOpen(false)}
                  className="p-2 hover:bg-red-700 rounded-xl transition-colors"
                >
                  <X className="w-6 h-6" />
                </button>
              </div>

              <div className="p-10 overflow-y-auto custom-scrollbar flex-1">
                <form onSubmit={handleSubmit} className="space-y-8">
                  <FinancialWarning />
                  
                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <div>
                      <label className="block text-xs font-black text-gray-400 uppercase tracking-widest mb-3">রক্তের গ্রুপ</label>
                      <div className="grid grid-cols-4 gap-2">
                        {BLOOD_GROUPS.map((group) => (
                          <button
                            key={group}
                            type="button"
                            onClick={() => setBloodGroup(group)}
                            className={cn(
                              "py-3 text-sm font-black rounded-2xl border-2 transition-all",
                              bloodGroup === group
                                ? "bg-red-600 border-red-600 text-white shadow-lg"
                                : "bg-white border-gray-100 text-gray-500 hover:border-red-200"
                            )}
                          >
                            {group}
                          </button>
                        ))}
                      </div>
                    </div>
                    <div>
                      <label className="block text-xs font-black text-gray-400 uppercase tracking-widest mb-3">জরুরি অবস্থা</label>
                      <div className="grid grid-cols-2 gap-2">
                        {URGENCY_LEVELS.map((level) => (
                          <button
                            key={level}
                            type="button"
                            onClick={() => setUrgency(level)}
                            className={cn(
                              "py-3 text-sm font-black rounded-2xl border-2 transition-all",
                              urgency === level
                                ? "bg-red-600 border-red-600 text-white shadow-lg"
                                : "bg-white border-gray-100 text-gray-500 hover:border-red-200"
                            )}
                          >
                            {URGENCY_MAP[level]}
                          </button>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                    <div className="space-y-2">
                      <label className="block text-xs font-black text-gray-400 uppercase tracking-widest">বিভাগ</label>
                      <select
                        required
                        value={division}
                        onChange={(e) => setDivision(e.target.value)}
                        className="w-full px-5 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700"
                      >
                        <option value="">নির্বাচন করুন</option>
                        {DIVISIONS.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <label className="block text-xs font-black text-gray-400 uppercase tracking-widest">জেলা</label>
                      <select
                        required
                        disabled={!division}
                        value={district}
                        onChange={(e) => setDistrict(e.target.value)}
                        className="w-full px-5 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 disabled:opacity-50"
                      >
                        <option value="">নির্বাচন করুন</option>
                        {districts.map(d => <option key={d.id} value={d.name}>{d.bnName}</option>)}
                      </select>
                    </div>
                    <div className="space-y-2">
                      <label className="block text-xs font-black text-gray-400 uppercase tracking-widest">থানা/উপজেলা</label>
                      <select
                        required
                        disabled={!district}
                        value={upazila}
                        onChange={(e) => setUpazila(e.target.value)}
                        className="w-full px-5 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 disabled:opacity-50"
                      >
                        <option value="">নির্বাচন করুন</option>
                        {upazilas.map(u => <option key={u.id} value={u.name}>{u.bnName}</option>)}
                      </select>
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <div className="space-y-4">
                      <label className="block text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                         <Building2 className="w-4 h-4" /> হাসপাতালের নাম
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="উদা: ঢাকা মেডিকেল কলেজ"
                        value={hospitalName}
                        onChange={(e) => setHospitalName(e.target.value)}
                        className="w-full px-6 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700"
                      />
                    </div>
                    <div className="space-y-4">
                      <label className="block text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                         <User className="w-4 h-4" /> রোগীর নাম
                      </label>
                      <input
                        type="text"
                        required
                        placeholder="উদা: আবুল হাসান"
                        value={patientName}
                        onChange={(e) => setPatientName(e.target.value)}
                        className="w-full px-6 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700"
                      />
                    </div>
                  </div>

                  <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                     <div className="space-y-4">
                        <label className="block text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                           <Phone className="w-4 h-4" /> ফোন নম্বর
                        </label>
                        <input
                          type="tel"
                          required
                          placeholder="০১৭XXXXXXXX"
                          value={contactNumber}
                          onChange={(e) => setContactNumber(e.target.value)}
                          className="w-full px-6 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700"
                        />
                      </div>
                      <div className="space-y-4">
                        <label className="block text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                           <Calendar className="w-4 h-4" /> সময়সীমা
                        </label>
                        <input
                          type="date"
                          required
                          value={requiredDate}
                          onChange={(e) => setRequiredDate(e.target.value)}
                          className="w-full px-6 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700"
                        />
                      </div>
                  </div>

                  <div className="space-y-4">
                    <label className="block text-xs font-black text-gray-400 uppercase tracking-widest flex items-center gap-2">
                       <MessageSquare className="w-4 h-4" /> বিস্তারিত (ঐচ্ছিক)
                    </label>
                    <textarea
                      placeholder="অতিরিক্ত তথ্য বা বিশেষ কোনো নির্দেশ থাকলে এখানে লিখুন..."
                      value={message}
                      onChange={(e) => setMessage(e.target.value)}
                      rows={3}
                      className="w-full px-6 py-4 bg-gray-50 border-none rounded-3xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 resize-none"
                    />
                  </div>

                  <div className="flex gap-4 pt-6">
                    <button
                      type="button"
                      onClick={() => setIsModalOpen(false)}
                      className="flex-1 py-5 border-2 border-gray-100 rounded-3xl font-black text-lg text-gray-500 hover:bg-gray-50 transition-all"
                    >
                      বাতিল
                    </button>
                    <button
                      type="submit"
                      disabled={submitting}
                      className="flex-1 py-5 bg-red-600 text-white rounded-3xl font-black text-lg hover:bg-red-700 transition-all shadow-2xl shadow-red-200 disabled:opacity-50"
                    >
                      {submitting ? 'পাবলিশ হচ্ছে...' : 'অনুরোধ পাবলিশ করুন'}
                    </button>
                  </div>
                </form>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
