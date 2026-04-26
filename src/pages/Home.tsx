import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Link } from 'react-router-dom';
import { Heart, Shield, Zap, Search, Users, MapPin, Quote, XCircle, AlertCircle, Award, Star, CheckCircle2, Calendar, User, ArrowRight } from 'lucide-react';
import { motion, AnimatePresence } from 'motion/react';
import { QRCodeSVG } from 'qrcode.react';

import { DIVISIONS } from '../constants/locations';
import { handleFirestoreError } from '../lib/error-handler';
import { OperationType, UserProfile, HealthTip } from '../types';
import { db } from '../lib/firebase';
import { collection, query, where, orderBy, limit, onSnapshot } from 'firebase/firestore';
import { AuthModal } from '../components/AuthModal';

export function Home() {
  const { login, loggingIn, error, clearError } = useAuth();
  const [healthTips, setHealthTips] = React.useState<HealthTip[]>([]);
  const [liveRequests, setLiveRequests] = React.useState<any[]>([]);
  const [upcomingEvents, setUpcomingEvents] = React.useState<any[]>([]);
  const [isAuthModalOpen, setIsAuthModalOpen] = React.useState(false);

  const openAuth = () => {
    setIsAuthModalOpen(true);
    clearError();
  };

  React.useEffect(() => {
    // Fetch live requests
    const requestsQuery = query(
      collection(db, 'requests'),
      where('status', '==', 'Active'),
      orderBy('createdAt', 'desc'),
      limit(3)
    );
    const unsubRequests = onSnapshot(requestsQuery, (snap) => {
      setLiveRequests(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    // Fetch upcoming events
    const eventsQuery = query(
      collection(db, 'events'),
      orderBy('date', 'asc'),
      limit(3)
    );
    const unsubEvents = onSnapshot(eventsQuery, (snap) => {
      setUpcomingEvents(snap.docs.map(doc => ({ id: doc.id, ...doc.data() })));
    });

    // Fetch health tips
    const tipsQuery = query(collection(db, 'health_tips'), limit(3));
    const unsubTips = onSnapshot(tipsQuery, (snap) => {
      setHealthTips(snap.docs.map(doc => ({ id: doc.id, ...doc.data() } as HealthTip)));
    });

    return () => {
      unsubRequests();
      unsubEvents();
      unsubTips();
    };
  }, []);

  const features = [
    { icon: Heart, title: 'স্বেচ্ছায় রক্তদান', desc: 'সহজেই ডোনার খুঁজে বের করুন।' },
    { icon: Shield, title: 'নিরাপদ ডেটা', desc: 'আপনার তথ্য সম্পূর্ণ সুরক্ষিত।' },
    { icon: Award, title: 'ডিজিটাল সার্টিফিকেট', desc: 'রক্তদানের পর পান স্বীকৃতি ও সম্মাননা।' },
    { icon: Users, title: 'কমিউনিটি', desc: 'হাজার হাজার রক্তদাতার সাথে যুক্ত হোন।' },
  ];

  return (
    <div className="space-y-20 pb-20">
      {/* Hero */}
      <section className="relative h-[90vh] flex items-center overflow-hidden -mx-4 sm:-mx-6 lg:-mx-8 mb-20">
        <div className="absolute inset-0 z-0 dark:opacity-50">
          <img 
            src="https://images.unsplash.com/photo-1615461066841-6116ecaaba7f?auto=format&fit=crop&q=80&w=2000" 
            alt="" 
            className="w-full h-full object-cover opacity-20 text-transparent"
            referrerPolicy="no-referrer"
            aria-hidden="true"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-transparent to-red-50/30 dark:to-transparent" />
        </div>

        <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center sm:text-left flex flex-col md:flex-row items-center justify-between gap-12">
          <motion.div 
            initial={{ opacity: 0, x: -50 }}
            animate={{ opacity: 1, x: 0 }}
            className="md:w-1/2 space-y-8 pt-12 md:pt-0"
          >
            <h1 className="text-6xl lg:text-8xl font-black text-gray-900 dark:text-white leading-tight">
              এক ফোঁটা রক্ত, <br/>একটি <span className="text-red-600">জীবন</span>।
            </h1>
            <p className="text-2xl text-gray-700 dark:text-gray-300 max-w-lg leading-relaxed">
              রক্তসেতু একটি আধুনিক প্ল্যাটফর্ম যা রক্তদাতা এবং গ্রহীতাদের মধ্যে সেতুবন্ধন তৈরি করে। আজই আমাদের মিশনে যোগ দিন।
            </p>
            <AnimatePresence>
              {error && (
                <motion.div 
                  initial={{ opacity: 0, y: -20, scale: 0.95 }}
                  animate={{ opacity: 1, y: 0, scale: 1 }}
                  exit={{ opacity: 0, scale: 0.95 }}
                  className="p-5 bg-red-50 border-2 border-red-100 text-red-700 rounded-3xl text-sm font-bold mb-8 flex items-start gap-4 shadow-xl shadow-red-100/50"
                >
                  <AlertCircle className="w-6 h-6 shrink-0 text-red-500" />
                  <div className="flex-1 text-left">
                    <p className="leading-relaxed">{error}</p>
                    <button 
                      onClick={openAuth}
                      className="mt-3 text-red-600 underline hover:text-red-800 transition-colors block"
                    >
                      আবার চেষ্টা করুন
                    </button>
                  </div>
                  <button 
                    onClick={clearError}
                    className="p-1 hover:bg-red-100 rounded-lg transition-colors text-red-400"
                  >
                    <XCircle className="w-5 h-5" />
                  </button>
                </motion.div>
              )}
            </AnimatePresence>
            <div className="flex flex-col sm:flex-row gap-6 pt-4">
              <button 
                onClick={openAuth}
                disabled={loggingIn}
                className="px-10 py-5 bg-red-600 text-white rounded-3xl font-bold text-xl hover:bg-red-700 transition shadow-2xl shadow-red-200 active:scale-95 transition-transform disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-3"
              >
                {loggingIn ? 'অপেক্ষা করুন...' : 'রক্তদাতা হিসেবে যোগ দিন'}
              </button>
              <button 
                onClick={openAuth}
                disabled={loggingIn}
                className="px-10 py-5 bg-white dark:bg-gray-800 text-gray-900 dark:text-white border-2 border-gray-200 dark:border-gray-700 rounded-3xl font-bold text-xl hover:border-red-600 dark:hover:border-red-500 transition active:scale-95 transition-transform disabled:opacity-50 disabled:cursor-not-allowed"
              >
                রক্তের জন্য আবেদন করুন
              </button>
            </div>
          </motion.div>

          <motion.div 
            initial={{ opacity: 0, scale: 0.8 }}
            animate={{ opacity: 1, scale: 1 }}
            className="md:w-1/2 relative"
          >
            <div className="w-full aspect-square bg-red-100/50 dark:bg-red-900/10 rounded-full flex items-center justify-center blood-pulse relative shadow-inner">
               <Heart className="w-48 h-48 text-red-600 fill-current drop-shadow-lg" />
            </div>
            {/* Floating stats */}
            <div className="absolute top-10 right-0 bg-white dark:bg-gray-800 p-6 rounded-3xl shadow-2xl flex items-center gap-4 animate-bounce">
              <div className="w-12 h-12 bg-green-100 dark:bg-green-900/30 rounded-2xl flex items-center justify-center text-green-600 dark:text-green-400">
                <Users className="w-8 h-8" />
              </div>
              <div>
                <p className="text-sm font-bold text-gray-400 uppercase">রক্তদাতা</p>
                <p className="text-2xl font-black text-gray-900 dark:text-white">১২,৪০০+</p>
              </div>
            </div>
          </motion.div>
        </div>
      </section>

      {/* Special Message Section */}
      <section className="max-w-7xl mx-auto px-4">
        <motion.div 
          initial={{ opacity: 0, scale: 0.95 }}
          whileInView={{ opacity: 1, scale: 1 }}
          viewport={{ once: true }}
          className="bg-gradient-to-r from-red-600 to-red-800 rounded-[50px] p-8 md:p-16 text-white shadow-2xl relative overflow-hidden"
        >
          <Quote className="absolute top-10 left-10 w-32 h-32 text-white/10 -rotate-12" />
          <div className="relative z-10 space-y-8">
            <h2 className="text-3xl md:text-5xl font-black leading-tight border-l-8 border-white/30 pl-8">
               রক্তদাতার সম্মানই হোক <br/> আমাদের অঙ্গীকার
            </h2>
            <div className="grid md:grid-cols-2 gap-12">
               <div className="space-y-6 text-lg md:text-xl font-medium leading-relaxed text-red-50">
                  <p>এক ব্যাগ রক্তের বিনিময় কি কিছু সামান্য অর্থ হতে পারে? একটি অমূল্য প্রাণের সুরক্ষা কি টাকায় পরিমাপ করা সম্ভব? কখনোই নয়।</p>
                  <p>অনেকেই ভাবেন রক্তদাতাকে সামান্য কিছু অর্থ দিলেই হয়তো তার প্রতিদান দেওয়া সম্পন্ন হয়ে গেল। কিন্তু একবার ভাবুন তো, আপনার প্রিয় মানুষটির সংকটের মুহূর্তে যে ব্যক্তিটি নিঃস্বার্থভাবে নিজের শরীরের রক্ত দিয়ে এগিয়ে এলেন, তার সেই মহানুভবতাকে কি অর্থের নিক্তিতে বিচার করা যায়? সেই রক্তে যখন আপনার আপনজন হাসছেন, সুস্থ আছেন—এই আনন্দ কি কোনো টাকা দিয়ে কেনা সম্ভব?</p>
               </div>
               <div className="space-y-6 text-lg md:text-xl font-medium leading-relaxed text-red-50">
                  <p>আপনার প্রিয়জনের জরুরি প্রয়োজনে যখন রক্তের জন্য ব্যাকুল হয়ে এদিক-ওদিক ছুটতে হয়, তখন রক্তদাতার একটি আশ্বাসই মনে প্রশান্তি আনে। সেই আশার আলো কি লাখ টাকা দিয়ে কেনা যায়? তাই আসুন, রক্তদানকে আমরা টাকার বিনিময়ে পাওয়া কোনো পণ্য হিসেবে নয়, বরং নিঃস্বার্থ ভালোবাসা ও মানবিকতার বহিঃপ্রকাশ হিসেবে দেখি।</p>
                  <p>রক্তদাতার কুশল বিনিময় করি এবং তাদের যথাযথ শ্রদ্ধা জানাই। মনে রাখবেন, রক্তদাতার রক্ত অমূল্য, আর তাদের সম্মান করা আমাদের নৈতিক দায়িত্ব।</p>
                  <div className="pt-8 border-t border-white/20">
                     <p className="font-black text-2xl md:text-3xl">"ভালোবেসে করি রক্ত দান, আমাদের রক্তে বাঁচুক প্রাণ..🩸"</p>
                     <p className="mt-4 font-black italic text-xl opacity-80">— রক্ত সেতু</p>
                  </div>
               </div>
            </div>
          </div>
          <div className="absolute bottom-0 right-0 w-64 h-64 bg-white/5 rounded-full blur-3xl -mr-32 -mb-32" />
        </motion.div>
      </section>

      {/* Certificate Showcase Section */}
      <section className="py-24 bg-gray-50 dark:bg-black overflow-hidden -mx-4 sm:-mx-6 lg:-mx-8">
         <div className="max-w-7xl mx-auto px-4">
            <div className="text-center mb-16 space-y-4">
               <h2 className="text-4xl md:text-5xl font-black text-gray-900 dark:text-white uppercase tracking-tighter">স্বীকৃতি ও সম্মাননা</h2>
               <p className="text-xl text-gray-500 dark:text-gray-400 font-bold max-w-2xl mx-auto">
                  আপনার প্রতিটি রক্তবিন্দু এক একটি জীবনের গল্প। রক্তসেতু আপনার এই মহানুভবতাকে সম্মান জানায় ডিজিটাল সার্টিফিকেটের মাধ্যমে।
               </p>
            </div>

            <div className="flex flex-col lg:flex-row items-center gap-16">
               {/* Left: Certificate Graphic */}
               <motion.div 
                  initial={{ opacity: 0, x: -50 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  className="w-full lg:w-3/5"
               >
                  <div className="relative group">
                     <div className="absolute -inset-4 bg-gradient-to-r from-red-600 to-amber-500 rounded-[50px] blur-2xl opacity-20 group-hover:opacity-40 transition-opacity"></div>
                     
                     {/* Certificate UI */}
                     <div className="relative aspect-[1.414/1] bg-[#FCF8F2] rounded-[2px] border-[8px] border-[#6D0005] p-2 md:p-6 shadow-2xl overflow-hidden select-none flex flex-col font-serif text-[#333]">
                        
                        {/* Inner Gold Border */}
                        <div className="absolute inset-4 md:inset-6 border-[1.5px] border-[#C3A96A] pointer-events-none z-10 flex flex-col">
                           
                           {/* Watermark Logo */}
                           <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-[80%] h-[80%] opacity-[0.03] pointer-events-none flex items-center justify-center">
                              <Heart className="w-[80%] h-[80%] text-[#8B0000] rotate-[30deg]" />
                           </div>

                           {/* Metadata */}
                           <div className="absolute top-3 right-4 text-[7px] md:text-[10px] font-sans font-bold text-[#8B4513]/60 tracking-wider">
                              সিরিয়াল: RS-20260419-XEY9
                           </div>

                           {/* Content Container */}
                           <div className="flex-1 flex flex-col justify-between p-4 md:p-8 pt-6 md:pt-10 z-20">
                              
                              {/* Header */}
                              <div className="flex flex-col items-center">
                                 <div className="flex items-center justify-center gap-4 md:gap-6 mb-4 md:mb-6">
                                    {/* Logo */}
                                    <div className="w-12 h-12 md:w-16 md:h-16 bg-[#6D0005] rounded-full flex items-center justify-center shadow-lg relative shrink-0">
                                       <div className="w-4 h-6 md:w-6 md:h-8 bg-white rounded-t-full rounded-b-md"></div>
                                       <div className="absolute inset-0 shadow-[inset_0_-2px_10px_rgba(0,0,0,0.5)] rounded-full rounded-t-full"></div>
                                    </div>
                                    
                                    {/* Title Text */}
                                    <div className="text-center md:text-left mt-2">
                                       <h3 className="text-3xl md:text-[45px] font-black text-[#6D0005] leading-none font-display pb-1 md:pb-2">Roktostu</h3>
                                       <p className="text-[7.5px] md:text-[10.5px] font-bold text-[#555] uppercase tracking-[0.25em] md:tracking-[0.3em]">স্বেচ্ছাসেবী রক্তদান সংগঠন — VOLUNTARY BLOOD DONATION</p>
                                    </div>
                                 </div>
                                 
                                 {/* Divider Line */}
                                 <div className="w-[85%] h-[1px] bg-[#C3A96A] relative mb-6 md:mb-10">
                                    <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-[#FCF8F2] border border-[#C3A96A] rotate-45"></div>
                                 </div>
                              </div>

                              {/* Body */}
                              <div className="text-center space-y-3 md:space-y-5">
                                 <div>
                                    <h2 className="text-3xl md:text-[50px] font-black text-[#6D0005] mb-1 md:mb-3 leading-tight">রক্তদানের সনদপত্র</h2>
                                    <p className="text-[9px] md:text-sm font-semibold text-[#8B4513]/70 tracking-[0.4em] uppercase italic">CERTIFICATE OF BLOOD DONATION</p>
                                 </div>
                                 
                                 <p className="text-sm md:text-lg font-bold text-[#333] pt-2 md:pt-4">এই মর্মে প্রত্যয়ন করা যাচ্ছে যে</p>
                                 
                                 <div className="py-2 md:py-3 relative inline-block">
                                    <h1 className="text-2xl md:text-[40px] font-black text-[#6D0005] border-b-[2px] border-[#6D0005]/20 pb-1 md:pb-3 px-4 md:px-8 inline-block tracking-tight">Aiyub Uddin Bin Ferdaus</h1>
                                 </div>

                                 <div className="flex justify-center my-3">
                                    <div className="flex items-center gap-4 md:gap-6 px-4 md:px-8 py-2 bg-[#FCF8F2] rounded-md border-[1.5px] border-[#E8D9CE]">
                                       <div className="flex items-center gap-2">
                                          <span className="text-[10px] md:text-sm font-bold text-[#333]">ফোন:</span>
                                          <span className="text-sm md:text-xl font-bold text-[#6D0005]">01897971573</span>
                                       </div>
                                       <div className="w-[1px] h-4 md:h-6 bg-[#E8D9CE]"></div>
                                       <div className="flex items-center gap-2">
                                          <span className="text-[10px] md:text-sm font-bold text-[#333]">রক্তের গ্রুপ:</span>
                                          <span className="text-sm md:text-xl font-bold text-[#6D0005] bg-[#F7E5E5] px-2 md:px-3 py-0.5 rounded border border-[#E8D9CE]">A+</span>
                                       </div>
                                    </div>
                                 </div>

                                 <p className="text-sm md:text-lg font-bold text-[#333]">মানবতার সেবায় স্বেচ্ছায় রক্তদান করেছেন।</p>
                                 
                                 <div className="flex justify-center my-3 md:my-5">
                                    <div className="inline-flex items-center gap-2 md:gap-3 bg-[#9E3E4C] text-white px-4 md:px-6 py-2 rounded shadow-md">
                                       <Calendar className="w-4 h-4 md:w-5 md:h-5 text-white" />
                                       <p className="text-xs md:text-base font-bold">রক্তদানের তারিখ: 19 এপ্রিল, 2026</p>
                                    </div>
                                 </div>

                                 <p className="text-[10px] md:text-[15px] font-medium text-[#444] pt-2 md:pt-4 pb-4 md:pb-8 italic">
                                    আমরা তাঁর এই মহৎ মানবিক আবদানের জন্য আন্তরিক কৃতজ্ঞতা ও সম্মান জানাই।
                                 </p>
                              </div>

                              {/* Footer */}
                              <div className="flex justify-between items-end mt-auto px-4">
                                 {/* QR Code */}
                                 <div className="mb-2 opacity-60">
                                    <QRCodeSVG value="https://roktosetu.com/verify/RS-20260419-XEY9" size={50} bgColor="transparent" />
                                 </div>

                                 {/* Signature */}
                                 <div className="text-center relative">
                                    <div className="absolute -top-12 md:-top-16 left-1/2 -translate-x-1/2 w-48 flex justify-center pointer-events-none">
                                       <span className="text-4xl md:text-6xl font-signature text-[#6D0005] -rotate-12">Aiyub</span>
                                    </div>
                                    <div className="w-32 md:w-48 h-[1.5px] bg-[#6D0005] mb-2 md:mb-3"></div>
                                    <h4 className="text-xs md:text-base font-black text-[#6D0005]">আইয়ুব উদ্দিন ফেরদৌস</h4>
                                    <p className="text-[8px] md:text-[10px] font-bold text-[#666] tracking-wider mt-0.5">প্রতিষ্ঠাতা ও অ্যাডমিন, রক্তসেতু</p>
                                 </div>
                              </div>
                           </div>
                        </div>
                     </div>
                  </div>
               </motion.div>

               {/* Right: Info */}
               <motion.div 
                  initial={{ opacity: 0, x: 50 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  className="w-full lg:w-2/5 space-y-8"
               >
                  <div className="space-y-6">
                     {[
                        { title: 'স্বয়ংক্রিয় ট্র্যাকিং', desc: 'আপনার প্রতিটি রক্তদানের রেকর্ড আমাদের সিস্টেমে সংরক্ষিত থাকে।' },
                        { title: 'ডিজিটাল প্রোফাইল', desc: 'আপনার প্রোফাইলে অর্জিত ব্যাজ এবং মেডেল প্রদর্শিত হবে।' },
                        { title: 'ভেরিফাইড সম্মাননা', desc: 'সহজেই কিউআর কোড স্ক্যান করে সার্টিফিকেটের সত্যতা যাচাই করা যাবে।' }
                     ].map((item, idx) => (
                        <div key={idx} className="flex gap-4">
                           <div className="w-10 h-10 bg-red-50 dark:bg-red-900/20 rounded-xl flex items-center justify-center shrink-0">
                              <CheckCircle2 className="w-5 h-5 text-red-600" />
                           </div>
                           <div>
                              <h4 className="font-black text-gray-900 dark:text-white text-lg">{item.title}</h4>
                              <p className="text-gray-500 dark:text-gray-400 font-medium">{item.desc}</p>
                           </div>
                        </div>
                     ))}
                  </div>
                  <button 
                     onClick={openAuth}
                     className="w-full py-6 bg-red-600 text-white rounded-[30px] font-black text-xl hover:bg-red-700 transition shadow-2xl shadow-red-500/20"
                  >
                     এখনই ডোনার হিসেবে নিবন্ধন করুন
                  </button>
               </motion.div>
            </div>
         </div>
      </section>

      {/* Live Blood Requests */}
      <section className="max-w-7xl mx-auto px-4">
        <div className="flex flex-col md:flex-row items-end justify-between gap-6 mb-12">
          <div className="space-y-4">
            <h2 className="text-4xl md:text-5xl font-black text-gray-900 dark:text-white uppercase tracking-tighter flex items-center gap-4">
              জরিুরি রক্তের আবেদন
              <span className="flex h-3 w-3">
                <span className="animate-ping absolute inline-flex h-3 w-3 rounded-full bg-red-400 opacity-75"></span>
                <span className="relative inline-flex rounded-full h-3 w-3 bg-red-500"></span>
              </span>
            </h2>
            <p className="text-xl text-gray-500 font-bold max-w-xl">মুহূর্তের মধ্যে রক্তদাতা খুঁজে পেতে আবেদনগুলো দেখুন।</p>
          </div>
          <Link to="/requests" className="px-8 py-4 bg-red-600 text-white rounded-3xl font-black text-sm hover:bg-red-700 transition-all flex items-center gap-2 shadow-xl shadow-red-200">
            সবগুলো দেখুন <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-3 gap-8">
          {liveRequests.length > 0 ? liveRequests.map((request, i) => (
            <motion.div
              key={request.id}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-xl border-2 border-red-50 dark:border-red-900/10 relative overflow-hidden"
            >
              <div className="absolute top-0 right-0 p-6">
                <div className="w-16 h-16 bg-red-50 dark:bg-red-900/20 rounded-2xl flex flex-col items-center justify-center border-2 border-red-100 dark:border-red-800">
                  <span className="text-2xl font-black text-red-600 leading-none">{request.bloodGroup}</span>
                </div>
              </div>
              <div className="space-y-4">
                <div className="flex items-center gap-2 text-[10px] font-black text-red-500 uppercase tracking-widest">
                  <AlertCircle className="w-4 h-4" /> জরুরি প্রয়োজন
                </div>
                <h4 className="text-xl font-black text-gray-900 dark:text-white">{request.patientName}</h4>
                <div className="space-y-2">
                  <div className="flex items-center gap-2 text-sm font-bold text-gray-500">
                    <MapPin className="w-4 h-4" /> {request.location?.upazila}, {request.location?.district}
                  </div>
                  <div className="flex items-center gap-2 text-sm font-bold text-gray-500">
                    <Calendar className="w-4 h-4" /> {request.deadline}
                  </div>
                </div>
                <Link 
                  to="/requests"
                  className="block w-full py-4 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-white text-center rounded-2xl font-black text-sm hover:bg-red-600 hover:text-white transition-all"
                >
                  বিস্তারিত দেখুন
                </Link>
              </div>
            </motion.div>
          )) : (
            <div className="col-span-1 md:col-span-3 bg-gray-50 dark:bg-gray-800/50 rounded-[40px] p-12 text-center border-2 border-dashed border-gray-200 dark:border-gray-700">
              <p className="text-gray-400 font-bold">বর্তমানে কোনো জরুরি আবেদন পাওয়া যায়নি।</p>
            </div>
          )}
        </div>
      </section>

      {/* Upcoming Campaigns */}
      <section className="max-w-7xl mx-auto px-4">
        <div className="bg-gray-900 rounded-[60px] p-8 md:p-16 text-white overflow-hidden relative">
          <div className="absolute top-0 right-0 w-96 h-96 bg-red-600/10 rounded-full blur-[100px] -mr-48 -mt-48" />
          
          <div className="relative z-10 grid lg:grid-cols-2 gap-16 items-center">
            <div className="space-y-8">
              <h2 className="text-4xl md:text-6xl font-black leading-tight uppercase tracking-tighter">
                আসন্ন রক্তদান <br/> <span className="text-red-500">ক্যাম্পেইনসমূহ</span>
              </h2>
              <p className="text-xl text-gray-400 font-medium leading-relaxed">
                আপনার নিকটস্থ এলাকার ব্লাড ক্যাম্পেইনগুলোতে যোগ দিন এবং মানবতার সেবায় অংশ নিন। সঠিক সময়ে রক্তদান আপনার সুস্বাস্থ্য বজায় রাখতেও সাহায্য করে।
              </p>
              <Link to="/campaigns" className="inline-flex items-center gap-4 px-10 py-5 bg-white text-gray-900 rounded-3xl font-black text-lg hover:bg-red-600 hover:text-white transition-all shadow-2xl shadow-black/50">
                সবগুলো ক্যাম্পেইন দেখুন <ArrowRight className="w-5 h-5" />
              </Link>
            </div>

            <div className="space-y-6">
              {upcomingEvents.length > 0 ? upcomingEvents.map((event, i) => (
                <motion.div
                  key={event.id}
                  initial={{ opacity: 0, x: 20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  transition={{ delay: i * 0.1 }}
                  className="bg-white/5 backdrop-blur-md border border-white/10 p-6 rounded-3xl flex items-center gap-6 hover:bg-white/10 transition-all cursor-pointer"
                >
                  <div className="w-16 h-16 bg-red-600 rounded-2xl flex flex-col items-center justify-center shrink-0">
                    <span className="text-xs font-black uppercase">{new Date(event.date).toLocaleDateString('bn-BD', { month: 'short' })}</span>
                    <span className="text-xl font-black leading-none">{new Date(event.date).toLocaleDateString('bn-BD', { day: 'numeric' })}</span>
                  </div>
                  <div>
                    <h4 className="text-xl font-black mb-1">{event.title}</h4>
                    <div className="flex items-center gap-2 text-sm font-bold text-gray-400">
                      <MapPin className="w-4 h-4" /> {event.location}
                    </div>
                  </div>
                </motion.div>
              )) : (
                <div className="p-8 text-center text-gray-500 italic border border-white/5 rounded-3xl">
                  বর্তমানে কোনো আসন্ন ক্যাম্পেইন নেই।
                </div>
              )}
            </div>
          </div>
        </div>
      </section>

      {/* Features */}
      <section className="max-w-7xl mx-auto px-4">
        <h2 className="text-4xl font-black text-center mb-20 text-gray-900">কেন রক্তসেতু?</h2>
        <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-12">
          {features.map((f, i) => (
            <motion.div 
              key={f.title}
              initial={{ opacity: 0, y: 20 }}
              whileInView={{ opacity: 1, y: 0 }}
              transition={{ delay: i * 0.1 }}
              viewport={{ once: true }}
              className="bg-white p-10 rounded-[40px] border border-gray-100 shadow-xl hover:shadow-2xl transition-all text-center group"
            >
              <div className="w-16 h-16 bg-red-50 text-red-600 rounded-3xl flex items-center justify-center mx-auto mb-8 group-hover:scale-110 transition-transform">
                <f.icon className="w-8 h-8" />
              </div>
              <h3 className="text-2xl font-black mb-4 text-gray-900">{f.title}</h3>
              <p className="text-gray-600 leading-relaxed text-lg">{f.desc}</p>
            </motion.div>
          ))}
        </div>
      </section>

      {/* Health Tips Section */}
      <section className="bg-white dark:bg-gray-950 py-24">
         <div className="max-w-7xl mx-auto px-4">
            <div className="text-center mb-16">
               <h2 className="text-4xl font-black text-gray-900 dark:text-white uppercase tracking-tighter">স্বাস্থ্য নির্দেশিকা</h2>
               <p className="text-xl text-gray-500 font-bold mt-4">রক্তদানের আগে ও পরের কিছু গুরুত্বপূর্ণ তথ্য</p>
            </div>

            <div className="grid md:grid-cols-3 gap-8">
               {healthTips.length > 0 ? healthTips.map((tip, i) => (
                  <motion.div
                    key={tip.id}
                    initial={{ opacity: 0, scale: 0.9 }}
                    whileInView={{ opacity: 1, scale: 1 }}
                    transition={{ delay: i * 0.1 }}
                    className="bg-gray-50 dark:bg-gray-900 p-8 rounded-[40px] border border-transparent hover:border-red-100 transition-all group"
                  >
                     <div className="w-14 h-14 bg-white dark:bg-gray-800 rounded-2xl flex items-center justify-center mb-6 shadow-sm group-hover:scale-110 transition-transform">
                        <Zap className="w-6 h-6 text-red-600" />
                     </div>
                     <span className="text-[10px] font-black text-red-500 uppercase tracking-[0.2em]">{tip.category}</span>
                     <h4 className="text-xl font-black mt-2 mb-4 text-gray-900 dark:text-white">{tip.title}</h4>
                     <p className="text-gray-500 dark:text-gray-400 font-medium leading-relaxed">{tip.content}</p>
                  </motion.div>
               )) : (
                 <div className="md:col-span-3 grid md:grid-cols-3 gap-8">
                    {[
                      { t: 'রক্তদানের আগে', c: 'রক্তদানের আগে প্রচুর পরিমাণে পানি পান করুন এবং পুষ্টিকর খাবার খান।' },
                      { t: 'রক্তদানের সময়', c: 'শান্ত থাকুন এবং গভীর শ্বাস নিন। এটি আপনার রক্তচাপ স্বাভাবিক রাখতে সাহায্য করবে।' },
                      { t: 'রক্তদানের পরে', c: 'রক্তদানের পর অন্তত ১০-১৫ মিনিট বিশ্রাম নিন এবং ভারী কাজ থেকে বিরত থাকুন।' }
                    ].map((tip, i) => (
                      <div key={i} className="bg-gray-50 dark:bg-gray-900 p-8 rounded-[40px] border border-transparent">
                         <div className="w-14 h-14 bg-white dark:bg-gray-800 rounded-2xl flex items-center justify-center mb-6">
                            <Zap className="w-6 h-6 text-red-600" />
                         </div>
                         <h4 className="text-xl font-black mb-4 text-gray-900 dark:text-white">{tip.t}</h4>
                         <p className="text-gray-500 dark:text-gray-400 font-medium">{tip.c}</p>
                      </div>
                    ))}
                 </div>
               )}
            </div>
         </div>
      </section>

      {/* Honor Section */}
      <section className="bg-red-50/50 py-20 px-4 -mx-4 sm:-mx-6 lg:-mx-8">
        <div className="max-w-7xl mx-auto flex flex-col md:flex-row items-center gap-16">
          <div className="md:w-1/2">
            <div className="grid grid-cols-2 gap-6 relative">
              <div className="absolute inset-0 bg-red-100/30 blur-[100px] -z-10" />
              <img src="https://picsum.photos/seed/donor1/400/500" className="rounded-[40px] shadow-2xl mt-12" referrerPolicy="no-referrer" />
              <img src="https://picsum.photos/seed/donor2/400/500" className="rounded-[40px] shadow-2xl" referrerPolicy="no-referrer" />
            </div>
          </div>
          <div className="md:w-1/2 space-y-8">
            <h2 className="text-5xl font-black text-gray-900 leading-tight">
              রক্তদাতারা সমাজের <br/> <span className="text-red-600 italic">প্রকৃত হিরো</span>
            </h2>
            <p className="text-xl text-gray-700 leading-relaxed font-bold">
              রক্তদাতারা কোনো প্রতিদান ছাড়াই অন্যের জন্য জীবন উৎসর্গ করেন। রুগীর স্বজনদের উচিত তাদের প্রতি সর্বোচ্চ সম্মান প্রদর্শন করা। 
              একটি সুন্দর হাসি এবং একটু কৃতজ্ঞতা একজন ডোনারের জন্য সবচেয়ে বড় পাওনা।
            </p>
            <ul className="space-y-4">
              {['ডোনারদের সম্মান করুন', 'তাদের প্রয়োজনীয় বিশ্রামের সুযোগ দিন', 'তাদের যাতায়াতে সহায়তা করুন', 'কৃতজ্ঞতা প্রকাশ করুন'].map((item, i) => (
                <li key={i} className="flex items-center gap-4 text-gray-800 font-black text-lg">
                  <div className="w-8 h-8 bg-red-600 text-white rounded-full flex items-center justify-center shrink-0">✓</div>
                  {item}
                </li>
              ))}
            </ul>
          </div>
        </div>
      </section>

      {/* CTA Section */}
      <section className="bg-gray-900 text-white rounded-[60px] p-20 text-center relative overflow-hidden shadow-2xl mx-4">
        <div className="relative z-10 max-w-3xl mx-auto">
          <h2 className="text-5xl font-black mb-8 leading-tight">আপনি কি রক্তের মাধ্যমে জীবন বাঁচাতে প্রস্তুত?</h2>
          <p className="text-gray-400 text-xl mb-12 leading-relaxed">
            প্রতিটি রক্তদাতা একজন হিরো। সবচেয়ে নির্ভরযোগ্য রক্ত নেটওয়ার্কের অংশ হতে এখন নিবন্ধন করুন।
          </p>
          <button 
            onClick={openAuth}
            disabled={loggingIn}
            className="px-12 py-6 bg-red-600 text-white rounded-[30px] font-black text-2xl hover:bg-red-700 transition shadow-2xl shadow-red-500/30 disabled:opacity-50"
          >
            {loggingIn ? 'অপেক্ষা করুন...' : 'এখনই শুরু করুন'}
          </button>
          
          <div className="mt-12 flex items-center justify-center gap-6">
            <Link to="/privacy" className="text-gray-500 font-bold hover:text-white transition-colors text-sm">প্রাইভেসি পলিসি</Link>
          </div>
        </div>
        <div className="absolute top-0 right-0 w-96 h-96 bg-red-600/10 rounded-full blur-[120px] -mr-48 -mt-48" />
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-red-600/10 rounded-full blur-[120px] -ml-48 -mb-48" />
      </section>

      <AuthModal 
        isOpen={isAuthModalOpen} 
        onClose={() => setIsAuthModalOpen(false)} 
      />
    </div>
  );
}
