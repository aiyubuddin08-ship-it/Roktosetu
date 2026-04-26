import React from 'react';
import { 
  Shield, 
  Lock, 
  Eye, 
  FileText, 
  ChevronLeft, 
  Database, 
  UserCheck, 
  Bell, 
  Share2, 
  LifeBuoy,
  Scale,
  Trash2,
  Clock
} from 'lucide-react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'motion/react';

export function PrivacyPolicy() {
  const navigate = useNavigate();

  const sections = [
    {
      icon: <Database className="w-6 h-6" />,
      title: "১. তথ্যের সংগ্রহ (Information Collection)",
      content: "আমাদের সেবার মান উন্নয়ন এবং রক্তদাতার সাথে গ্রহীতার সঠিক যোগাযোগের সুবিধার্থে আমরা নির্দিষ্ট কিছু তথ্য সংগ্রহ করি। এর মূল উদ্দেশ্য হলো মানবতার সেবায় দ্রুত রক্ত পাওয়ার প্রক্রিয়া সহজ করা।",
      items: [
        "ব্যক্তিগত পরিচয়: আপনার নাম ও প্রোফাইল ছবি (আপনার সত্যতা প্রমাণের জন্য)।",
        "যোগাযোগ মাধ্যম: আপনার সক্রিয় মোবাইল নম্বর (যাতে গ্রহীতা সরাসরি যোগাযোগ করতে পারে)।",
        "চিকিৎসা সংক্রান্ত তথ্য: আপনার রক্তের গ্রুপ ও সর্বশেষ রক্তদানের তারিখ (রক্তদানের যোগ্যতা যাচাইয়ের জন্য)।",
        "ভৌগোলিক অবস্থান: আপনার জেলা, উপজেলা ও ইউনিয়ন (নিকটস্থ ডোনারকে দ্রুত খুঁজে পাওয়ার জন্য)।",
        "অ্যাকাউন্ট অ্যাক্টিভিটি: আপনার রক্তদান ও গ্রহীতার অনুরোধের ইতিহাস।"
      ]
    },
    {
      icon: <Eye className="w-6 h-6" />,
      title: "২. তথ্যের ব্যবহার ও দৃশ্যমানতা (Usage & Visibility)",
      content: "সংগৃহীত তথ্যগুলো রক্ত আদান-প্রদান প্রক্রিয়ার বাইরে অন্য কোনো বাণিজ্যিক উদ্দেশ্যে ব্যবহৃত হয় না। আপনার তথ্যের দৃশ্যমানতা নিম্নোক্ত উপায়ে সংরক্ষিত হয়:",
      items: [
        "জনসাধারণের জন্য: ডোনার সার্চ করার সময় আপনার নাম, রক্তের গ্রুপ এবং এলাকা প্রোফাইলে দৃশ্যমান হবে।",
        "ফোন নম্বরের নিয়ন্ত্রণ: আপনার প্রোফাইল সেটিংসে 'ফোন নম্বর লুকান' অপশন ব্যবহার করে এটি নিয়ন্ত্রণ করতে পারেন। অন্যথায়, লগইন করা ব্যবহারকারীরা আপনার নম্বর দেখতে পাবেন।",
        "গ্যামিফিকেশন: লিডারবোর্ডে আপনার নাম ও সফল রক্তদানের সংখ্যা অন্যদের অনুপ্রাণিত করার জন্য প্রদর্শিত হয়।",
        "প্রয়োজনীয় বার্তা: জরুরি রক্তের অনুরোধ বা ক্যাম্পেইনের খবর আপনাকে নোটিফিকেশনের মাধ্যমে জানানো হয়।"
      ]
    },
    {
      icon: <Lock className="w-6 h-6" />,
      title: "৩. তথ্য সুরক্ষা ও গোপনীয়তা (Security)",
      content: "আপনার ব্যক্তিগত তথ্যের নিরাপত্তা আমাদের সর্বোচ্চ অগ্রাধিকার। আমরা কোনোভাবেই আপনার তথ্যের অপব্যবহার করতে দিব না।",
      items: [
        "ডাটা সুরক্ষিত রাখা: আপনার সমস্ত ডাটা ইন্ডাস্ট্রি-স্ট্যান্ডার্ড এনক্রিপশনের মাধ্যমে ক্লাউড স্টোরেজে (Firebase) জমা রাখা হয়।",
        "তৃতীয় পক্ষ: আমরা আপনার ব্যক্তিগত মোবাইল নম্বর বা অন্য কোনো তথ্য কোনো মার্কেটিং কোম্পানি বা থার্ড পার্টির কাছে বিক্রি করি না।",
        "সার্ভার অ্যাক্সেস: শুধুমাত্র রক্তসেতুর অনুমোদিত কারিগরি দল সিস্টেম রক্ষণাবেক্ষণের জন্য সীমিত আকারে ডাটা অ্যাক্সেস করতে পারে।"
      ]
    },
    {
      icon: <Share2 className="w-6 h-6" />,
      title: "৪. তথ্য আদান-প্রদান (Sharing)",
      content: "রক্তসেতু একটি ওপেন নেটওয়ার্ক হওয়ার কারণে অন্য কোনো ব্যবহারকারী যখন রক্ত খুঁজবে, তখন আপনার প্রোফাইল তাদের সার্চে আসবে। তবে আপনার ব্যক্তিগত গোপনীয়তা বজায় রাখার জন্য আপনি নিজেই প্রোফাইল থেকে তথ্যের দৃশ্যমানতা অফ করে রাখতে পারেন। প্রোফাইলে আপনার অনুমতি ছাড়া অন্য কেউ কোনো তথ্য পরিবর্তন করতে পারবে না।"
    },
    {
      icon: <Trash2 className="w-6 h-6" />,
      title: "৫. আপনার অধিকার (User Rights)",
      content: "প্ল্যাটফর্মের ব্যবহারকারী হিসেবে আপনার ডাটার ওপর পূর্ণ নিয়ন্ত্রণ রয়েছে:",
      items: [
        "সংশোধন: প্রোফাইল সেটিংস থেকে যেকোনো সময় সকল তথ্য পরিবর্তন বা আপডেট করতে পারবেন।",
        "অ্যাকাউন্ট ডিলেশন: আপনি চাইলে যেকোনো সময় আপনার অ্যাকাউন্ট ডিলিট করতে পারেন। অ্যাকাউন্ট ডিলিট করার সাথে সাথে আমাদের সিস্টেম থেকে আপনার সকল ব্যক্তিগত তথ্য স্থায়ীভাবে মুছে যাবে।"
      ]
    },
    {
      icon: <Scale className="w-6 h-6" />,
      title: "৬. আইনি বাধ্যবাধকতা",
      content: "আমরা রক্ত কেনা-বেচা বা কোনো বেআইনি কাজের জন্য এই প্ল্যাটফর্ম ব্যবহার কঠোরভাবে নিষিদ্ধ করি। যদি দেশের প্রচলিত আইনের কোনো লঙ্ঘন ঘটে এবং কোনো বৈধ আইনি সংস্থা তদন্তের প্রয়োজনে তথ্য চায়, তবে আমরা তথ্য প্রদানে বাধ্য থাকতে পারি।"
    }
  ];

  return (
    <div className="min-h-screen bg-gray-50/50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-4xl mx-auto">
        <button 
          onClick={() => navigate(-1)}
          className="group flex items-center gap-2 text-gray-500 hover:text-red-600 transition-all mb-10 font-black uppercase text-sm tracking-widest"
        >
          <div className="w-8 h-8 rounded-full bg-white shadow-sm flex items-center justify-center group-hover:bg-red-50 group-hover:text-red-600 transition-all">
            <ChevronLeft className="w-5 h-5" />
          </div>
          পিছনে যান
        </button>

        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="bg-white rounded-[40px] shadow-[0_32px_128px_-32px_rgba(0,0,0,0.1)] border border-gray-100/50 overflow-hidden"
        >
          {/* Header Banner */}
          <div className="relative bg-red-600 px-10 py-16 text-white overflow-hidden">
            <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-[80px] -translate-y-1/2 translate-x-1/2" />
            <div className="absolute bottom-0 left-0 w-32 h-32 bg-red-900/40 rounded-full blur-[40px] -translate-x-1/2 translate-y-1/2" />
            
            <div className="relative z-10 text-center sm:text-left">
              <div className="inline-flex items-center justify-center w-16 h-16 bg-white/20 backdrop-blur-xl rounded-3xl mb-6 ring-1 ring-white/30">
                <Shield className="w-8 h-8 text-white" />
              </div>
              <h1 className="text-4xl sm:text-5xl font-black tracking-tight mb-4">প্রাইভেসি পলিসি</h1>
              <p className="text-lg opacity-80 font-bold max-w-2xl leading-relaxed">
                রক্তসেতু (RoktoSetu) অ্যাপ্লিকেশনে আপনার তথ্যের গোপনীয়তা এবং সুরক্ষা নিশ্চিত করা আমাদের নৈতিক প্রতিশ্রুতি।
              </p>
            </div>
          </div>

          <div className="p-8 sm:p-12">
            <div className="grid grid-cols-1 gap-12">
              {sections.map((section, idx) => (
                <motion.section 
                  key={idx}
                  initial={{ opacity: 0, x: -20 }}
                  whileInView={{ opacity: 1, x: 0 }}
                  viewport={{ once: true }}
                  transition={{ delay: idx * 0.1 }}
                  className="group"
                >
                  <div className="flex items-start gap-6">
                    <div className="flex-shrink-0 w-12 h-12 rounded-2xl bg-red-50 text-red-600 flex items-center justify-center group-hover:scale-110 group-hover:bg-red-600 group-hover:text-white transition-all duration-500 shadow-sm">
                      {section.icon}
                    </div>
                    <div className="flex-1 space-y-4">
                      <h2 className="text-xl font-black text-gray-900 tracking-tight group-hover:text-red-600 transition-colors">
                        {section.title}
                      </h2>
                      <p className="text-gray-600 leading-relaxed font-semibold text-base">
                        {section.content}
                      </p>
                      {section.items && (
                        <ul className="grid grid-cols-1 sm:grid-cols-1 gap-3 mt-4">
                          {section.items.map((item, i) => (
                            <li key={i} className="flex items-start gap-3 text-gray-500 font-medium text-sm">
                              <div className="w-1.5 h-1.5 rounded-full bg-red-400 mt-2 flex-shrink-0" />
                              {item}
                            </li>
                          ))}
                        </ul>
                      )}
                    </div>
                  </div>
                </motion.section>
              ))}
            </div>

            <div className="mt-20 pt-12 border-t border-gray-100">
              <div className="bg-gray-50 rounded-3xl p-8 flex flex-col sm:flex-row items-center justify-between gap-6">
                <div className="flex items-center gap-4">
                  <div className="w-12 h-12 rounded-2xl bg-white shadow-sm flex items-center justify-center text-red-600">
                    <LifeBuoy className="w-6 h-6" />
                  </div>
                  <div>
                    <h4 className="font-black text-gray-900">সহায়তা প্রয়োজন?</h4>
                    <p className="text-sm text-gray-500 font-bold">যেকোনো প্রশ্ন বা ফিডব্যাকের জন্য যোগাযোগ করুন</p>
                  </div>
                </div>
                <button className="px-8 py-3 bg-red-600 text-white font-black rounded-2xl shadow-xl shadow-red-200 hover:bg-red-700 hover:shadow-red-300 transition-all active:scale-95 text-sm uppercase tracking-widest">
                  যোগাযোগ করুন
                </button>
              </div>

              <div className="mt-12 flex flex-col sm:flex-row items-center justify-between text-gray-400 font-black text-[10px] uppercase tracking-[0.3em]">
                <div className="flex items-center gap-2 mb-4 sm:mb-0">
                  <Clock className="w-3 h-3" />
                  সর্বশেষ আপডেট: ১৮ এপ্রিল, ২০২৬
                </div>
                <span>© ২০২৬ রক্তসেতু আইনি বিভাগ</span>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
    </div>
  );
}
