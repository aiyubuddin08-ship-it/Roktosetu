import React from 'react';
import { Apple, Coffee, Moon, Activity, Heart, ShieldCheck } from 'lucide-react';

const tips = [
  {
    icon: <Apple className="w-6 h-6 text-orange-500" />,
    title: "পুষ্টিকর খাবার",
    desc: "রক্তদানের আগে আয়রন সমৃদ্ধ খাবার যেমন লাল মাংস, মাছ, ডিম, কলিজা, ডাল ও সবুজ শাকসবজি বেশি করে খান।"
  },
  {
    icon: <Coffee className="w-6 h-6 text-blue-500" />,
    title: "প্রচুর পানি পান করুন",
    desc: "রক্তদানের আগের এবং পরের ২৪ ঘণ্টায় সাধারণের চেয়ে বেশি পানি এবং ফলের রস পান করুন।"
  },
  {
    icon: <Moon className="w-6 h-6 text-purple-500" />,
    title: "পর্যাপ্ত বিশ্রাম",
    desc: "রক্তদানের আগের রাতে অন্তত ৭-৮ ঘণ্টা গভীর ঘুম নিশ্চিত করুন এবং শরীরকে সতেজ রাখুন।"
  },
  {
    icon: <Activity className="w-6 h-6 text-red-500" />,
    title: "ভারী কাজ এড়িয়ে চলুন",
    desc: "রক্তদানের পর অন্তত ২৪ ঘণ্টা কোনো ভারী ব্যায়াম বা কায়িক পরিশ্রমের কাজ থেকে বিরত থাকুন।"
  }
];

export function HealthTips() {
  return (
    <section className="space-y-8">
      <div className="flex items-center justify-between">
        <h2 className="text-2xl font-black text-gray-900 dark:text-white flex items-center gap-3">
          <Heart className="w-7 h-7 text-red-600" /> রক্তদাতার স্বাস্থ্য টিপস
        </h2>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
        {tips.map((tip, idx) => (
          <div key={idx} className="bg-white dark:bg-gray-900 p-8 rounded-[40px] border border-gray-100 dark:border-gray-800 shadow-xl hover:shadow-2xl transition-all group">
            <div className="w-14 h-14 bg-gray-50 dark:bg-gray-800 rounded-2xl flex items-center justify-center mb-6 shadow-inner group-hover:scale-110 transition-transform">
              {tip.icon}
            </div>
            <h3 className="text-xl font-black text-gray-900 dark:text-white mb-3">{tip.title}</h3>
            <p className="text-gray-500 dark:text-gray-400 font-bold text-sm leading-relaxed">{tip.desc}</p>
          </div>
        ))}
      </div>
      
      <div className="bg-red-50 dark:bg-red-900/10 p-8 rounded-[40px] border border-red-100 dark:border-red-900/20 flex flex-col md:flex-row items-center gap-6">
        <div className="w-16 h-16 bg-red-600 text-white rounded-3xl flex items-center justify-center shadow-lg">
           <ShieldCheck className="w-8 h-8" />
        </div>
        <div className="flex-1 text-center md:text-left">
           <h4 className="text-lg font-black text-red-900 dark:text-red-400">একটি অনুরোধ</h4>
           <p className="text-red-700 dark:text-red-500 font-bold">আপনার রক্ত আপনার পরিচয়। সুস্থ জীবনযাপন করুন এবং নিয়মিত রক্তদান করে অন্যের জীবন বাঁচাতে এগিয়ে আসুন।</p>
        </div>
      </div>
    </section>
  );
}
