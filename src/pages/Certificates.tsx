import React, { useState, useEffect, useRef } from 'react';
import { collection, query, where, orderBy, onSnapshot, addDoc, getDocs } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { DonationCertificate, DonationRecord, OperationType } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Award, Download, Share2, Calendar, User, Heart, ShieldCheck, Printer, ExternalLink, QrCode as QrIcon, MapPin, Phone } from 'lucide-react';
import { format } from 'date-fns';
import { bn } from 'date-fns/locale';
import { motion, AnimatePresence } from 'motion/react';
import { QRCodeCanvas } from 'qrcode.react';
import { toPng } from 'html-to-image';
import { cn } from '../lib/utils';

export function Certificates() {
  const [certificates, setCertificates] = useState<DonationCertificate[]>([]);
  const [availableRecords, setAvailableRecords] = useState<DonationRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedCert, setSelectedCert] = useState<DonationCertificate | null>(null);
  const [showOrgCert, setShowOrgCert] = useState(false);
  const [isDownloading, setIsDownloading] = useState(false);
  const [isOrgDownloading, setIsOrgDownloading] = useState(false);
  const [userOrg, setUserOrg] = useState<any>(null);
  const certificateRef = useRef<HTMLDivElement>(null);
  const orgCertificateRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!user) return;

    const checkOrg = async () => {
      const q = query(collection(db, 'organizations'), where('adminUid', '==', user.uid));
      const snap = await getDocs(q);
      if (!snap.empty) {
        setUserOrg({ id: snap.docs[0].id, ...snap.docs[0].data() });
      }
    };
    checkOrg();

    const qCerts = query(
      collection(db, 'certificates'),
      where('userId', '==', user.uid),
      orderBy('issuedAt', 'desc')
    );

    const unsubscribeCerts = onSnapshot(qCerts, (snapshot) => {
      setCertificates(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as DonationCertificate)));
      setLoading(false);
    });

    const fetchRecords = async () => {
      const qRecords = query(
        collection(db, `users/${user.uid}/donations`),
        orderBy('date', 'desc')
      );
      const snap = await getDocs(qRecords);
      setAvailableRecords(snap.docs.map(doc => ({ id: doc.id, ...doc.data() } as DonationRecord)));
    };

    fetchRecords();
    return () => unsubscribeCerts();
  }, [user]);

  const handleClaimCertificate = async (record: DonationRecord) => {
    if (!user || !profile) return;
    
    if (certificates.some(c => c.donationDate === record.date)) {
      alert('এই রক্তদানের জন্য অলরেডি সার্টিফিকেট নেওয়া হয়েছে।');
      return;
    }

    try {
      const donationDateObj = new Date(record.date);
      const certNo = `RS-${format(donationDateObj, 'yyyyMMdd')}-${user.uid.slice(0, 4).toUpperCase()}`;
      
      const newCert = {
        userId: user.uid,
        donorName: profile.displayName,
        bloodGroup: profile.bloodGroup,
        donationDate: record.date,
        donationCount: profile.donationsCount || 1,
        issuedAt: new Date().toISOString(),
        certificateNo: certNo
      };

      await addDoc(collection(db, 'certificates'), newCert);
      alert('সফলভাবে সার্টিফিকেট তৈরি হয়েছে!');
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'certificates');
    }
  };

  const downloadPdf = async () => {
    if (!certificateRef.current || isDownloading) return;

    setIsDownloading(true);

    // Minor delay to ensure modal transitions are advanced enough
    await new Promise(resolve => setTimeout(resolve, 100));

    try {
      const element = certificateRef.current;
      
      const dataUrl = await Promise.race([
        toPng(element, {
          pixelRatio: 2,
          backgroundColor: '#FDF8F0',
          style: {
            transform: 'scale(1)',
          },
        }),
        new Promise<string>((_, reject) => setTimeout(() => reject(new Error("Timeout generating image")), 12000))
      ]);

      // Mobile/iframe friendly Blob download
      const arr = dataUrl.split(',');
      const mimeMatch = arr[0].match(/:(.*?);/);
      const mime = mimeMatch ? mimeMatch[1] : 'image/png';
      const bstr = atob(arr[1]);
      let n = bstr.length;
      const u8arr = new Uint8Array(n);
      while(n--){
          u8arr[n] = bstr.charCodeAt(n);
      }
      const blob = new Blob([u8arr], {type:mime});
      const objectUrl = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.download = `Roktosetu_Certificate_${selectedCert?.certificateNo}.png`;
      link.href = objectUrl;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      setTimeout(() => URL.revokeObjectURL(objectUrl), 2000);
    } catch (error) {
      console.error('Image generation failed:', error);
      alert('দুঃখিত, আপনার ব্রাউজারে সার্টিফিকেট ডাউনলোড করা সম্ভব হচ্ছে না। অনুগ্রহ করে একটি স্ক্রিনশট নিন।');
    } finally {
      setIsDownloading(false);
    }
  };

  const downloadOrgPdf = async () => {
    if (!orgCertificateRef.current || isOrgDownloading) return;

    setIsOrgDownloading(true);

    await new Promise(resolve => setTimeout(resolve, 100));

    try {
      const element = orgCertificateRef.current;
      
      const dataUrl = await Promise.race([
        toPng(element, {
          pixelRatio: 2, 
          backgroundColor: '#FDF8F0',
          style: { transform: 'scale(1)' },
        }),
        new Promise<string>((_, reject) => setTimeout(() => reject(new Error("Timeout generating image")), 12000))
      ]);

      const arr = dataUrl.split(',');
      const mimeMatch = arr[0].match(/:(.*?);/);
      const mime = mimeMatch ? mimeMatch[1] : 'image/png';
      const bstr = atob(arr[1]);
      let n = bstr.length;
      const u8arr = new Uint8Array(n);
      while(n--){ u8arr[n] = bstr.charCodeAt(n); }
      const blob = new Blob([u8arr], {type:mime});
      const objectUrl = URL.createObjectURL(blob);

      const link = document.createElement('a');
      link.download = `Roktosetu_Org_Certificate_${userOrg?.name}.png`;
      link.href = objectUrl;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      
      setTimeout(() => URL.revokeObjectURL(objectUrl), 2000);
    } catch (error) {
      console.error('Image generation failed:', error);
      alert('দুঃখিত, আপনার ব্রাউজারে সার্টিফিকেট ডাউনলোড করা সম্ভব হচ্ছে না। অনুগ্রহ করে একটি স্ক্রিনশট নিন।');
    } finally {
      setIsOrgDownloading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto pb-32 -mt-8 -mx-4 sm:-mx-6 lg:-mx-8 animate-fade-in bg-gray-50 dark:bg-gray-950 min-h-screen font-sans">
       {/* Header Section */}
       <section className="bg-brand pt-16 pb-12 px-6 relative overflow-hidden">
          <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full -mr-32 -mt-32 blur-3xl" />
          <div className="max-w-xl mx-auto relative z-10 text-center">
             <h1 className="text-3xl font-black text-white mb-2">
               {userOrg ? 'সংগঠনের স্বীকৃতি' : 'আপনার স্বীকৃতি'}
             </h1>
             <p className="text-white/80 font-medium text-sm">
               {userOrg 
                 ? `${userOrg.name} এর মানবিক কার্যক্রমের জন্য আমরা কৃতজ্ঞ।`
                 : 'আপনার প্রতিটি রক্তদান একটি মহৎ কাজ। আমরা আপনার এই ত্যাগকে সম্মান জানাই।'}
             </p>
          </div>
       </section>

       <div className="max-w-4xl mx-auto px-6 -mt-8 space-y-10">
          {userOrg ? (
            <div className="space-y-8">
               <div className="bg-white dark:bg-gray-900 p-8 sm:p-12 rounded-[50px] shadow-2xl border border-red-50 dark:border-red-900/10 text-center space-y-8">
                  <div className="flex justify-center">
                     <div className="w-24 h-24 bg-red-50 dark:bg-red-900/20 rounded-[30px] flex items-center justify-center relative">
                        <Award className="w-12 h-12 text-red-600" />
                        <div className="absolute -bottom-2 -right-2 bg-green-500 text-white p-1.5 rounded-full shadow-lg border-2 border-white dark:border-gray-900">
                           <ShieldCheck className="w-4 h-4" />
                        </div>
                     </div>
                  </div>
                  <div>
                     <h2 className="text-3xl font-black text-gray-900 dark:text-white uppercase tracking-tight">{userOrg.name}</h2>
                     <p className="text-red-600 font-black text-xs uppercase tracking-widest mt-2 italic">অফিশিয়াল পার্টনার • রক্তসেতু</p>
                  </div>
                  <div className="bg-gray-50 dark:bg-gray-800/50 p-6 rounded-[32px] text-sm text-gray-600 dark:text-gray-400 font-medium leading-relaxed italic">
                    "সংগঠন হিসেবে আপনাদের নিরলস প্রচেষ্টা অসংখ্য মুমূর্ষু রোগীর জীবন বাঁচাতে সাহায্য করছে। এই মানবিক যাত্রায় রক্তসেতু আপনাদের পাশে থাকতে পেরে গর্বিত।"
                  </div>
                  <div className="grid grid-cols-2 gap-4">
                     <div className="p-6 bg-white dark:bg-gray-900 rounded-3xl border border-gray-100 dark:border-gray-800">
                        <p className="text-2xl font-black text-red-600">ভেরিফাইড</p>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">সংগঠন স্ট্যাটাস</p>
                     </div>
                     <div className="p-6 bg-white dark:bg-gray-900 rounded-3xl border border-gray-100 dark:border-gray-800">
                        <p className="text-2xl font-black text-red-600">{userOrg.isVerified ? 'সম্মানিত' : 'নিবন্ধিত'}</p>
                        <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-1">পুরস্কার লেভেল</p>
                     </div>
                  </div>
                  <button 
                    onClick={() => setShowOrgCert(true)}
                    className="flex flex-col sm:flex-row items-center justify-center gap-3 w-full py-5 bg-red-600 text-white rounded-[32px] font-black shadow-xl shadow-red-200 hover:bg-red-700 transition-all active:scale-95"
                  >
                     <Award className="w-5 h-5" />
                     সংগঠনের স্বীকৃতিপত্র দেখুন ও ডাউনলোড করুন
                  </button>
               </div>
            </div>
          ) : (
            <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
            <div className="lg:col-span-2 space-y-8">
              <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl card-shadow border border-gray-50 dark:border-gray-800">
                 <h2 className="text-2xl font-black text-gray-900 dark:text-white mb-8 flex items-center gap-3">
                  <Award className="w-7 h-7 text-brand" /> আমার সার্টিফিকেটসমূহ
                </h2>
                {certificates.length > 0 ? (
                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-6">
                     {certificates.map(cert => (
                        <motion.div 
                          whileHover={{ scale: 1.02 }}
                          key={cert.id} 
                          onClick={() => setSelectedCert(cert)}
                          className="cursor-pointer bg-gradient-to-br from-brand to-red-800 p-6 rounded-[32px] text-white shadow-xl relative overflow-hidden group"
                        >
                           <div className="relative z-10 space-y-4">
                              <div className="w-12 h-12 bg-white/20 rounded-xl flex items-center justify-center">
                                 <Award className="w-6 h-6" />
                              </div>
                              <div>
                                 <p className="text-[10px] font-black uppercase tracking-widest text-red-200">সার্টিফিকেট নং: {cert.certificateNo}</p>
                                 <h3 className="text-lg font-black">{format(new Date(cert.donationDate), 'dd MMMM, yyyy', { locale: bn })}</h3>
                              </div>
                              <div className="flex items-center justify-between text-xs font-bold text-red-100">
                                 <span>রক্তসেতু সম্মাননা</span>
                                 <div className="flex items-center gap-1"><ShieldCheck className="w-4 h-4" /> ভেরিফাইড</div>
                              </div>
                           </div>
                           <Award className="absolute -right-8 -bottom-8 w-32 h-32 text-white/10 fill-current group-hover:rotate-12 transition-transform" />
                        </motion.div>
                     ))}
                  </div>
                ) : (
                  <div className="bg-gray-50 dark:bg-gray-800 p-20 rounded-[40px] text-center border-2 border-dashed border-gray-100 dark:border-gray-800">
                    <Award className="w-20 h-20 text-gray-200 dark:text-gray-700 mx-auto mb-6" />
                    <p className="text-gray-400 font-bold">এখনো কোনো সার্টিফিকেট ইস্যু করা হয়নি।</p>
                  </div>
                )}
              </div>

              <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl card-shadow border border-gray-50 dark:border-gray-800">
                 <h2 className="text-2xl font-black text-gray-900 dark:text-white mb-8 flex items-center gap-3">
                  <Heart className="w-7 h-7 text-brand" /> সার্টিফিকেট সংগ্রহ করুন
                </h2>
                <p className="text-gray-500 font-medium mb-6">আপনার রক্তদান রেকর্ডগুলো থেকে সার্টিফিকেট ডাউনলোড করুন।</p>
                {availableRecords.length > 0 ? (
                  <div className="space-y-4">
                     {availableRecords.map(record => {
                        const isClaimed = certificates.some(c => c.donationDate === record.date);
                        return (
                          <div key={record.id} className="bg-gray-50 dark:bg-gray-800 p-6 rounded-3xl flex items-center justify-between gap-6">
                            <div className="flex items-center gap-4">
                               <div className="w-12 h-12 bg-white dark:bg-gray-900 rounded-xl flex items-center justify-center text-brand">
                                 <Calendar className="w-6 h-6" />
                               </div>
                               <div>
                                  <p className="font-black text-gray-900 dark:text-white">{format(new Date(record.date), 'dd MMM, yyyy', { locale: bn })}</p>
                                  <p className="text-xs text-gray-400 font-bold">{record.hospitalName ? record.hospitalName : 'অজ্ঞাত স্থান'}</p>
                               </div>
                            </div>
                            {isClaimed ? (
                              <span className="px-4 py-2 bg-green-100 dark:bg-green-900/20 text-green-700 dark:text-green-500 rounded-xl font-black text-xs uppercase tracking-widest">সংগৃহীত</span>
                            ) : (
                              <button 
                                onClick={() => handleClaimCertificate(record)}
                                className="px-6 py-2 bg-brand text-white rounded-xl font-black text-xs uppercase tracking-widest hover:bg-brand/90 transition-colors shadow-lg shadow-brand/20"
                              >
                                ক্লেইম করুন
                              </button>
                            )}
                          </div>
                        );
                     })}
                  </div>
                ) : (
                  <div className="p-8 text-center text-gray-400 font-bold">
                     কোনো রক্তদানের রেকর্ড পাওয়া যায়নি। প্রোফাইলে গিয়ে নতুন রেকর্ড যোগ করুন।
                  </div>
                )}
              </div>
            </div>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl card-shadow border border-gray-50 dark:border-gray-800 space-y-6">
                <h3 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-2">
                  <ShieldCheck className="w-6 h-6 text-brand" /> সার্টিফিকেটের গুরুত্ব
                </h3>
                <ul className="space-y-4">
                  <li className="flex gap-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl">
                    <div className="w-2 h-2 mt-2 bg-brand rounded-full shrink-0" />
                    <p className="text-sm text-gray-600 dark:text-gray-400 font-medium italic">"মানুষ মানুষের জন্য" - আপনার এই ত্যাগই হতে পারে একজনের বেঁচে থাকার প্রেরণা।</p>
                  </li>
                  <li className="flex gap-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl">
                    <div className="w-2 h-2 mt-2 bg-brand rounded-full shrink-0" />
                    <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">এটি আপনার রক্তদানের একটি ডিজিটাল স্বীকৃতি। প্রতিটি কার্ডে একটি ইউনিক ভেরিফিকেশন নম্বর থাকে।</p>
                  </li>
                  <li className="flex gap-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl">
                    <div className="w-2 h-2 mt-2 bg-brand rounded-full shrink-0" />
                    <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">সনদটি ডিভাইসে ছবি (Image) হিসেবে ডাউনলোড করে সংরক্ষণ করুন।</p>
                  </li>
                </ul>
            </div>
          </div>
        )}
       </div>

      <AnimatePresence>
        {selectedCert && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md overflow-y-auto">
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="w-full max-w-5xl my-8 space-y-6 flex flex-col items-center"
            >
              <div className="flex justify-end gap-3 w-full max-w-[900px]">
                 <button 
                  disabled={isDownloading}
                  onClick={downloadPdf} 
                  className={cn(
                    "bg-white text-gray-900 px-4 sm:px-6 py-2.5 sm:py-3 rounded-2xl font-black flex items-center gap-2 transition-all shadow-xl text-sm sm:text-base",
                    isDownloading ? "opacity-50 cursor-wait" : "hover:bg-gray-100"
                  )}
                 >
                   <Download className={cn("w-4 h-4 sm:w-5 sm:h-5 text-[#9B1B30]", isDownloading && "animate-bounce")} /> 
                   {isDownloading ? "ডাউনলোড হচ্ছে..." : "সনদ ডাউনলোড"}
                 </button>
                 <button 
                  disabled={isDownloading}
                  onClick={() => setSelectedCert(null)} 
                  className="bg-white/20 text-white px-4 sm:px-6 py-2.5 sm:py-3 rounded-2xl font-black hover:bg-white/30 transition-all backdrop-blur-md disabled:opacity-30 text-sm sm:text-base"
                 >
                   বন্ধ করুন
                 </button>
              </div>

              {/* Exact Design from User's HTML/CSS */}
              <div className="w-full overflow-x-auto flex justify-start lg:justify-center p-2 sm:p-4">
                <div 
                  ref={certificateRef}
                  style={{ 
                    width: '900px', 
                    minHeight: '630px', 
                    background: '#FDF8F0', 
                    position: 'relative', 
                    overflow: 'hidden',
                    border: '6px solid #C9A84C',
                    boxShadow: '0 0 0 2px #6B0F20, 0 0 0 10px #C9A84C',
                    color: '#1A1008'
                  }}
                  className="font-serif shadow-2xl rounded-sm shrink-0"
                >
                  {/* Decorative Corner Ornaments */}
                  <div className="absolute top-4 left-4 w-32 h-32 z-10 pointer-events-none border-t-2 border-l-2 border-[#C9A84C]"></div>
                  <div className="absolute bottom-4 right-4 w-32 h-32 z-10 pointer-events-none border-b-2 border-r-2 border-[#C9A84C]"></div>
                  <div className="absolute bottom-4 left-4 w-32 h-32 z-10 pointer-events-none border-b-2 border-l-2 border-[#C9A84C]"></div>
                  <div className="absolute top-4 right-4 w-32 h-32 z-10 pointer-events-none border-t-2 border-r-2 border-[#C9A84C]"></div>
                  
                  {/* Top red ribbon */}
                  <div className="h-2 w-full bg-gradient-to-r from-[#6B0F20] to-[#9B1B30]"></div>

                  {/* Watermark */}
                  <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-0">
                    <span className="text-[130px] font-black text-[#9B1B30]/[0.04] -rotate-30 select-none whitespace-nowrap font-display">রক্তদান</span>
                  </div>

                  {/* Serial */}
                  <div className="absolute top-5 right-16 text-[10px] tracking-[0.15em] text-[#7A6050] z-10 select-none">
                     সিরিয়াল: {selectedCert.certificateNo}
                  </div>

                  <div className="relative z-10 px-14 pt-8 pb-16 flex flex-col items-center">
                    {/* Header */}
                    <div className="flex items-center justify-center gap-5 mb-2">
                       <div className="w-[72px] h-[72px] rounded-full bg-gradient-to-r from-[#6B0F20] to-[#9B1B30] border-3 border-[#C9A84C] flex items-center justify-center shrink-0 shadow-lg">
                          <svg viewBox="0 0 60 72" className="w-[38px] h-[38px]" fill="none" xmlns="http://www.w3.org/2000/svg">
                            <path d="M30 4 C30 4 6 34 6 48 C6 61.3 17.2 72 30 72 C42.8 72 54 61.3 54 48 C54 34 30 4 30 4Z" fill="#FDF8F0" opacity="0.9"/>
                            <path d="M36 38 C36 38 22 38 22 48 C22 54.6 25.4 58 30 58" stroke="#FDF8F0" strokeWidth="2.5" strokeLinecap="round" opacity="0.5"/>
                          </svg>
                       </div>
                       <div className="text-center">
                          <div className="text-[28px] text-[#6B0F20] font-black tracking-wider leading-none font-display">Roktostu</div>
                          <div className="text-[11px] text-[#7A6050] tracking-[0.2em] uppercase mt-1">স্বেচ্ছাসেবী রক্তদান সংগঠন — Voluntary Blood Donation</div>
                       </div>
                    </div>

                    {/* Divider */}
                    <div className="w-full flex items-center gap-3 my-4">
                       <div className="flex-1 h-[1px] bg-gradient-to-r from-transparent via-[#C9A84C] to-transparent"></div>
                       <div className="w-2 h-2 bg-[#C9A84C] rotate-45 shrink-0"></div>
                       <div className="flex-1 h-[1px] bg-gradient-to-l from-transparent via-[#C9A84C] to-transparent"></div>
                    </div>

                    {/* Title */}
                    <h1 className="text-[38px] text-[#6B0F20] font-black tracking-wider leading-tight mb-1 font-display">রক্তদানের সনদপত্র</h1>
                    <div className="text-[14px] text-[#7A6050] tracking-[0.25em] uppercase mb-5 italic font-serif">Certificate of Blood Donation</div>

                    {/* Body */}
                    <div className="text-center text-[17px] text-[#1A1008] leading-[2] px-5 space-y-2">
                       <p>এই মর্মে প্রত্যয়ন করা যাচ্ছে যে</p>
                       <p className="my-2">
                         <span className="font-bold text-[20px] text-[#6B0F20] border-b-2 border-[#C9A84C] pb-[1px]">{selectedCert.donorName}</span>
                       </p>
                       <div className="flex items-center justify-center gap-1">
                          ফোন: <span className="inline-block bg-[#9B1B30]/[0.08] border border-[#9B1B30]/20 rounded-sm px-2 py-0 text-[16px] font-bold text-[#9B1B30] mx-1">{profile?.phoneNumber || 'সংরক্ষিত নয়'}</span>
                          &nbsp;&nbsp;
                          রক্তের গ্রুপ: <span className="inline-block bg-[#9B1B30]/[0.08] border border-[#9B1B30]/20 rounded-sm px-2 py-0 text-[16px] font-bold text-[#9B1B30] mx-1">{selectedCert.bloodGroup}</span>
                       </div>
                       <p className="mt-2">মানবতার সেবায় স্বেচ্ছায় রক্তদান করেছেন।</p>
                       <div className="mt-2">
                          <span className="inline-flex items-center gap-2 bg-gradient-to-r from-[#6B0F20] to-[#9B1B30] text-[#FDF8F0] rounded-sm px-4 py-1 text-[15px] font-bold border border-[#C9A84C]">
                             📅 রক্তদানের তারিখ: {format(new Date(selectedCert.donationDate), 'd MMMM, yyyy', { locale: bn })}
                          </span>
                       </div>
                       <p className="mt-4 italic text-[16px] text-[#7A6050]">আমরা তাঁর এই মহৎ মানবিক অবদানের জন্য আন্তরিক কৃতজ্ঞতা ও সম্মান জানাই।</p>
                    </div>

                    {/* Divider */}
                    <div className="w-full flex items-center gap-3 mt-5">
                       <div className="flex-1 h-[1px] bg-gradient-to-r from-transparent via-[#C9A84C] to-transparent"></div>
                       <div className="w-2 h-2 bg-[#C9A84C] rotate-45 shrink-0"></div>
                       <div className="flex-1 h-[1px] bg-gradient-to-l from-transparent via-[#C9A84C] to-transparent"></div>
                    </div>

                    {/* Footer Row */}
                    <div className="absolute bottom-7 left-14 right-14 flex justify-between items-end">
                       <div className="flex flex-col items-center">
                          <div className="scale-75 grayscale opacity-30">
                            <QRCodeCanvas value={`https://rokto-setu.app/verify/${selectedCert.certificateNo}`} size={40} />
                          </div>
                       </div>

                       <div className="flex flex-col items-center text-center">
                          {/* Signature Image and Lines */}
                          <div className="relative mb-2 flex flex-col items-center">
                             <div className="h-10 flex items-center justify-center">
                                <span className="font-signature text-[40px] text-[#6B0F20] relative z-10 select-none">
                                  Aiyub
                                </span>
                             </div>
                             <div className="w-44 h-[1.5px] bg-[#C9A84C] mt-2"></div>
                          </div>
                          <p className="text-[14px] text-[#6B0F20] font-bold mt-1 font-display">আইয়ুব উদ্দীন ফেরদৌস</p>
                          <span className="text-[10px] tracking-[0.15em] uppercase text-[#7A6050] mt-0.5">প্রতিষ্ঠাতা ও অ্যাডমিন, রক্তসেতু</span>
                       </div>
                    </div>
                  </div>
                  
                  {/* Bottom Ribbon */}
                  <div className="absolute bottom-0 h-2 w-full bg-gradient-to-r from-[#6B0F20] to-[#9B1B30]"></div>
              </div>
            </div>
          </motion.div>
        </div>
      )}
      </AnimatePresence>
      <AnimatePresence>
        {showOrgCert && userOrg && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-md overflow-y-auto">
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="w-full max-w-5xl my-8 space-y-6 flex flex-col items-center"
            >
              <div className="flex justify-end gap-3 w-full max-w-[900px]">
                 <button 
                  disabled={isOrgDownloading}
                  onClick={downloadOrgPdf} 
                  className={cn(
                    "bg-white text-gray-900 px-4 sm:px-6 py-2.5 sm:py-3 rounded-2xl font-black flex items-center gap-2 transition-all shadow-xl text-sm sm:text-base",
                    isOrgDownloading ? "opacity-50 cursor-wait" : "hover:bg-gray-100"
                  )}
                 >
                   <Download className={cn("w-4 h-4 sm:w-5 sm:h-5 text-[#9B1B30]", isOrgDownloading && "animate-bounce")} /> 
                   {isOrgDownloading ? "ডাউনলোড হচ্ছে..." : "সনদ ডাউনলোড"}
                 </button>
                 <button 
                  disabled={isOrgDownloading}
                  onClick={() => setShowOrgCert(false)} 
                  className="bg-white/20 text-white px-4 sm:px-6 py-2.5 sm:py-3 rounded-2xl font-black hover:bg-white/30 transition-all backdrop-blur-md disabled:opacity-30 text-sm sm:text-base"
                 >
                   বন্ধ করুন
                 </button>
              </div>

              <div className="w-full overflow-x-auto flex justify-start lg:justify-center p-2 sm:p-4">
                <div 
                  ref={orgCertificateRef}
                  style={{ 
                    width: '900px', 
                    minHeight: '630px', 
                    background: '#FDF8F0', 
                    position: 'relative', 
                    overflow: 'hidden',
                    border: '6px solid #C9A84C',
                    boxShadow: '0 0 0 2px #6B0F20, 0 0 0 10px #C9A84C',
                    color: '#1A1008'
                  }}
                  className="font-serif shadow-2xl rounded-sm shrink-0"
                >
                   {/* Decorative Corner Ornaments */}
                   <div className="absolute top-4 left-4 w-32 h-32 z-10 pointer-events-none border-t-2 border-l-2 border-[#C9A84C]"></div>
                   <div className="absolute bottom-4 right-4 w-32 h-32 z-10 pointer-events-none border-b-2 border-r-2 border-[#C9A84C]"></div>
                   <div className="absolute bottom-4 left-4 w-32 h-32 z-10 pointer-events-none border-b-2 border-l-2 border-[#C9A84C]"></div>
                   <div className="absolute top-4 right-4 w-32 h-32 z-10 pointer-events-none border-t-2 border-r-2 border-[#C9A84C]"></div>
                   
                   {/* Top red ribbon */}
                   <div className="h-2 w-full bg-gradient-to-r from-[#6B0F20] to-[#9B1B30]"></div>

                   {/* Watermark */}
                   <div className="absolute inset-0 flex items-center justify-center pointer-events-none z-0">
                     <span className="text-[130px] font-black text-[#9B1B30]/[0.04] -rotate-30 select-none whitespace-nowrap font-display">মানবিক সংগঠন</span>
                   </div>

                   <div className="relative z-10 px-14 pt-8 pb-16 flex flex-col items-center h-full justify-between">
                      {/* Header */}
                      <div className="flex flex-col items-center">
                        <div className="flex items-center justify-center gap-5 mb-2">
                           <div className="w-[72px] h-[72px] rounded-full bg-gradient-to-r from-[#6B0F20] to-[#9B1B30] border-3 border-[#C9A84C] flex items-center justify-center shrink-0 shadow-lg relative">
                              <svg viewBox="0 0 60 72" className="w-[38px] h-[38px]" fill="none" xmlns="http://www.w3.org/2000/svg">
                                <path d="M30 4 C30 4 6 34 6 48 C6 61.3 17.2 72 30 72 C42.8 72 54 61.3 54 48 C54 34 30 4 30 4Z" fill="#FDF8F0" opacity="0.9"/>
                                <path d="M36 38 C36 38 22 38 22 48 C22 54.6 25.4 58 30 58" stroke="#FDF8F0" strokeWidth="2.5" strokeLinecap="round" opacity="0.5"/>
                              </svg>
                           </div>
                           <div className="text-center">
                              <div className="text-[28px] text-[#6B0F20] font-black tracking-wider leading-none font-display">Roktostu</div>
                              <div className="text-[11px] text-[#7A6050] tracking-[0.2em] uppercase mt-1">স্বেচ্ছাসেবী রক্তদান সংগঠন — Voluntary Blood Donation</div>
                           </div>
                        </div>

                        <div className="w-full flex items-center gap-3 my-4">
                           <div className="flex-1 h-[1px] bg-gradient-to-r from-transparent via-[#C9A84C] to-transparent"></div>
                           <div className="w-2 h-2 bg-[#C9A84C] rotate-45 shrink-0"></div>
                           <div className="flex-1 h-[1px] bg-gradient-to-l from-transparent via-[#C9A84C] to-transparent"></div>
                        </div>

                        {/* Title */}
                        <h1 className="text-[38px] text-[#6B0F20] font-black tracking-wider leading-tight mb-1 font-display">মানবিক সংগঠন স্বীকৃতি</h1>
                        <div className="text-[14px] text-[#7A6050] tracking-[0.25em] uppercase mb-5 italic font-serif">Certificate of Humanitarian Organization</div>
                      </div>

                      {/* Body */}
                      <div className="text-center text-[17px] text-[#1A1008] leading-[2] px-5 space-y-2 mt-4">
                         <p>এই মর্মে প্রত্যয়ন করা যাচ্ছে যে</p>
                         <p className="my-2">
                           <span className="font-bold text-[28px] text-[#6B0F20] border-b-2 border-[#C9A84C] pb-[1px] block">{userOrg.name}</span>
                         </p>
                         <p className="mt-2 text-lg">সংগঠনটি রক্তসেতু প্ল্যাটফর্মে একটি ভেরিফাইড অফিশিয়াল পার্টনার হিসেবে যুক্ত হয়েছে।</p>
                         <div className="mt-4">
                            <span className="inline-flex items-center gap-2 bg-[#9B1B30]/[0.05] border border-[#C9A84C]/50 rounded-lg px-6 py-3 text-[16px] font-bold text-[#6B0F20]">
                               📍 {userOrg.location?.district} • {userOrg.type} • {userOrg.isVerified ? 'সম্মানিত' : 'নিবন্ধিত'}
                            </span>
                         </div>
                         <p className="mt-6 italic text-[18px] text-[#7A6050] font-medium max-w-[600px] mx-auto">"অফিশিয়াল পার্টনার হিসেবে আপনাদের নিরলস প্রচেষ্টা অসংখ্য মুমূর্ষু রোগীর জীবন বাঁচাতে সাহায্য করছে। এই মানবিক যাত্রায় রক্তসেতু আপনাদের পাশে থাকতে পেরে গর্বিত।"</p>
                      </div>

                      {/* Footer Row */}
                      <div className="absolute bottom-7 left-14 right-14 flex justify-between items-end">
                         <div className="flex flex-col items-center">
                            <div className="scale-[0.8] grayscale opacity-40">
                              <QRCodeCanvas 
                                value={`https://roktosetu.com/verify/org/${userOrg?.id || 'unknown'}`}
                                size={50}
                                level="M"
                                includeMargin={false}
                                fgColor="#6B0F20"
                                bgColor="transparent"
                              />
                            </div>
                         </div>

                         <div className="flex flex-col items-center text-center">
                            {/* Signature Image and Lines */}
                            <div className="relative mb-2 flex flex-col items-center">
                               <div className="h-10 flex items-center justify-center">
                                  <span className="font-signature text-[40px] text-[#6B0F20] relative z-10 select-none">
                                    Aiyub
                                  </span>
                               </div>
                               <div className="w-44 h-[1.5px] bg-[#C9A84C] mt-2"></div>
                            </div>
                            <p className="text-[14px] text-[#6B0F20] font-bold mt-1 font-display">আইয়ুব উদ্দীন ফেরদৌস</p>
                            <span className="text-[10px] tracking-[0.15em] uppercase text-[#7A6050] mt-0.5">প্রতিষ্ঠাতা ও অ্যাডমিন, রক্তসেতু</span>
                         </div>
                      </div>
                   </div>
                   
                   {/* Bottom Ribbon */}
                   <div className="absolute bottom-0 h-2 w-full bg-gradient-to-r from-[#6B0F20] to-[#9B1B30]"></div>
                </div>
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
