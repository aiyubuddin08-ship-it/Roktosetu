import React from 'react';
import { Heart, ShieldCheck, Apple, Coffee, Info, AlertCircle, CheckCircle2, Moon, Activity, Droplets } from 'lucide-react';
import { motion } from 'motion/react';

const TIP_GROUPS = [
  {
    title: 'রক্তদানের আগে',
    icon: Coffee,
    color: 'bg-blue-50 text-blue-600',
    tips: [
      'রক্তদানের আগে প্রচুর পরিমাণে পানি পান করুন।',
      'আগের রাতে ভালো করে ঘুমানোর চেষ্টা করুন।',
      'লৌহ সমৃদ্ধ খাবার (যেমন: কচু শাক, কলিজা, ডিম) বেশি করে খান।',
      'রক্তদানের অন্তত ৩ ঘণ্টা আগে পুষ্টিকর খাবার গ্রহণ করুন।',
      'ধূমপান বা মদ্যপান থেকে বিরত থাকুন।'
    ]
  },
  {
    title: 'রক্তদানের সময়',
    icon: Activity,
    color: 'bg-green-50 text-green-600',
    tips: [
      'নিজেকে শান্ত রাখুন এবং স্বাভাবিক শ্বাস-প্রশ্বাস নিন।',
      'পরনের পোশাকটি যেন আরামদায়ক এবং ঢিলেঢালা হয়।',
      'যদি কোনো অস্বস্তি বোধ করেন, সাথে সাথে মেডিকেল স্টাফকে জানান।'
    ]
  },
  {
    title: 'রক্তদানের পরে',
    icon: Apple,
    color: 'bg-orange-50 text-orange-600',
    tips: [
      'কমপক্ষে ১৫-২০ মিনিট বিশ্রাম নিন।',
      'পরবর্তী কয়েক ঘণ্টা প্রচুর তরল খাবার পান করুন।',
      'ভারী কাজ বা দীর্ঘক্ষণ রোদে থাকা থেকে বিরত থাকুন।',
      'পরবর্তী ৩-৪ ঘণ্টা কোনো ধরণের যানবাহন চালানো থেকে বিরত থাকাই ভালো।'
    ]
  }
];

const ELIGIBILITY = [
  { label: 'বয়স', value: '১৮ থেকে ৬৫ বছর' },
  { label: 'ওজন', value: '৫০ কেজির উপরে' },
  { label: 'হিমোগ্লোবিন', value: '১৩.৫ (পুরুষ), ১২.৫ (মহিলা)' },
  { label: 'বিরতি', value: 'প্রতি ৯০ দিন বা ৩ মাস পরপর' }
];

export function DonationTips() {
  return (
    <div className="max-w-4xl mx-auto space-y-12 pb-20 animate-fade-in">
      <div className="text-center space-y-4">
        <div className="w-20 h-20 bg-red-100 dark:bg-red-900/20 rounded-[32px] flex items-center justify-center mx-auto shadow-inner">
           <Heart className="w-12 h-12 text-red-600 animate-pulse fill-current" />
        </div>
        <h1 className="text-4xl font-black text-gray-900 dark:text-white tracking-tight">রক্তদান গাইডলাইন</h1>
        <p className="text-gray-500 dark:text-gray-400 font-bold text-lg max-w-2xl mx-auto">
          আপনার রক্তদান হোক নিরাপদ এবং আনন্দদায়ক। এখানে রক্তদানের আগে ও পরের জরুরি স্বাস্থ্য টিপস দেওয়া হলো।
        </p>
      </div>

      <div className="grid md:grid-cols-2 lg:grid-cols-4 gap-4">
        {ELIGIBILITY.map((item, index) => (
          <div key={index} className="bg-white dark:bg-gray-900 p-6 rounded-3xl border border-gray-100 dark:border-gray-800 shadow-xl text-center space-y-1">
             <p className="text-[10px] font-black text-gray-400 uppercase tracking-widest">{item.label}</p>
             <p className="text-xl font-black text-red-600 dark:text-red-500">{item.value}</p>
          </div>
        ))}
      </div>

      <div className="bg-red-600 rounded-[40px] p-10 text-white relative overflow-hidden shadow-2xl">
         <AlertCircle className="absolute -bottom-10 -right-10 w-48 h-48 opacity-10" />
         <div className="relative z-10 space-y-6">
            <h2 className="text-3xl font-black flex items-center gap-3">
              <ShieldCheck className="w-8 h-8" /> কখন রক্ত দেবেন না?
            </h2>
            <div className="grid md:grid-cols-2 gap-6 font-bold text-red-50">
               <div className="flex items-start gap-3 bg-red-700/30 p-4 rounded-2xl border border-red-500/30">
                  <span className="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center shrink-0">✕</span>
                  আপনার যদি জ্বর বা ইনজেকশনজনিত কোনো অসুখ থাকে।
               </div>
               <div className="flex items-start gap-3 bg-red-700/30 p-4 rounded-2xl border border-red-500/30">
                  <span className="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center shrink-0">✕</span>
                  যদি গত ৩ মাসের মধ্যে বড় কোনো সার্জারি হয়ে থাকে।
               </div>
               <div className="flex items-start gap-3 bg-red-700/30 p-4 rounded-2xl border border-red-500/30">
                  <span className="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center shrink-0">✕</span>
                  যদি কোনো দীর্ঘমেয়াদী সংক্রামক ব্যাধি থাকে।
               </div>
               <div className="flex items-start gap-3 bg-red-700/30 p-4 rounded-2xl border border-red-500/30">
                  <span className="w-6 h-6 bg-white/20 rounded-full flex items-center justify-center shrink-0">✕</span>
                  অল্প বয়সে বা গর্ভকালীন সময়ে রক্তদান থেকে বিরত থাকুন।
               </div>
            </div>
         </div>
      </div>

      <div className="bg-amber-50 dark:bg-amber-900/10 border-2 border-amber-100 dark:border-amber-900/20 p-10 rounded-[40px] shadow-sm">
         <div className="flex items-center gap-4 mb-8">
            <div className="p-3 bg-amber-500 text-white rounded-2xl shadow-lg">
               <ShieldCheck className="w-6 h-6" />
            </div>
            <h2 className="text-3xl font-black text-amber-900 dark:text-amber-400">রক্তদাতার অধিকার ও নীতি</h2>
         </div>
         <div className="grid md:grid-cols-2 gap-8">
            <div className="space-y-4">
               <h4 className="text-lg font-black text-amber-800 dark:text-amber-500 flex items-center gap-2">
                  <CheckCircle2 className="w-5 h-5" /> ডোনারের অধিকার
               </h4>
               <ul className="space-y-3 text-amber-700 dark:text-amber-600/80 font-bold text-sm">
                  <li>• ডোনারের ব্যক্তিগত তথ্যের গোপনীয়তা রক্ষা করা।</li>
                  <li>• রক্তদানের আগে প্রয়োজনীয় স্বাস্থ্য পরীক্ষা ও পরামর্শ পাওয়া।</li>
                  <li>• কোনো কারণ ছাড়াই যেকোনো সময় রক্তদান থেকে বিরত থাকার অধিকার।</li>
                  <li>• রক্তদানের পর সম্মানজনক আচরণ ও পর্যাপ্ত মনোযোগ পাওয়া।</li>
               </ul>
            </div>
            <div className="space-y-4">
               <h4 className="text-lg font-black text-amber-800 dark:text-amber-500 flex items-center gap-2">
                  <AlertCircle className="w-5 h-5" /> রুগীর স্বজনদের দায়িত্ব
               </h4>
               <ul className="space-y-3 text-amber-700 dark:text-amber-600/80 font-bold text-sm">
                  <li>• ডোনারকে আসার জন্য প্রয়োজনীয় যাতায়াত খরচ বা সরাসরি যাতায়াত ব্যবস্থা করা।</li>
                  <li>• ডোনারকে ন্যূনতম নাস্তা ও পানীয় নিশ্চিত করা।</li>
                  <li>• রক্তদানের পর তাকে অন্তত ৩০ মিনিট বিশ্রামের সুযোগ দেওয়া।</li>
                  <li>• সর্বদা বিনয়ী ও কৃতজ্ঞতাপূর্ণ আচরণ করা।</li>
               </ul>
            </div>
         </div>
      </div>

      <div className="grid md:grid-cols-3 gap-8">
        {TIP_GROUPS.map((group, index) => (
          <motion.div 
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            key={index} 
            className="bg-white dark:bg-gray-900 p-8 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-xl space-y-6"
          >
            <div className={`w-14 h-14 ${group.color} rounded-2xl flex items-center justify-center`}>
              <group.icon className="w-7 h-7" />
            </div>
            <h3 className="text-2xl font-black text-gray-900 dark:text-white uppercase leading-none">{group.title}</h3>
            <ul className="space-y-4">
              {group.tips.map((tip, i) => (
                <li key={i} className="flex gap-3 text-gray-500 dark:text-gray-400 font-bold text-sm leading-relaxed">
                  <CheckCircle2 className="w-5 h-5 text-red-500 shrink-0" />
                  {tip}
                </li>
              ))}
            </ul>
          </motion.div>
        ))}
      </div>

      <div className="bg-indigo-50 dark:bg-indigo-900/10 border-2 border-indigo-100 dark:border-indigo-900/20 p-8 rounded-[40px] flex flex-col md:flex-row items-center gap-8 shadow-inner">
         <div className="w-24 h-24 bg-white dark:bg-gray-800 rounded-3xl flex items-center justify-center shadow-xl text-indigo-600">
            <Droplets className="w-12 h-12" />
         </div>
         <div className="flex-1 space-y-2 text-center md:text-left">
            <h4 className="text-2xl font-black text-indigo-900 dark:text-indigo-400">রক্তদান সম্পর্কে আরও জানতে চান?</h4>
            <p className="text-indigo-700 dark:text-indigo-500 font-bold">আমাদের এক্সপার্টদের সাথে কথা বলতে বা কোনো প্রশ্ন থাকলে সরাসরি কল করতে পারেন।</p>
         </div>
         <a href="tel:01897971573" className="px-10 py-5 bg-indigo-600 text-white rounded-[24px] font-black hover:bg-indigo-700 transition-all shadow-xl shadow-indigo-500/20">যোগাযোগ করুন</a>
      </div>
    </div>
  );
}
