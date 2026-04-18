import React from 'react';
import { Shield, Lock, Eye, FileText, ChevronLeft } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export function PrivacyPolicy() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-gray-50 py-12 px-4 sm:px-6 lg:px-8">
      <div className="max-w-3xl mx-auto">
        <button 
          onClick={() => navigate(-1)}
          className="flex items-center gap-2 text-gray-500 hover:text-red-600 transition-colors mb-8 font-bold"
        >
          <ChevronLeft className="w-5 h-5" /> পিছনে যান
        </button>

        <div className="bg-white rounded-[40px] shadow-2xl border border-gray-100 overflow-hidden">
          <div className="bg-red-600 p-10 text-white">
            <div className="flex items-center gap-4 mb-4">
              <Shield className="w-10 h-10" />
              <h1 className="text-3xl font-black">প্রাইভেসি পলিসি</h1>
            </div>
            <p className="opacity-90 font-medium">রক্তসেতু (RoktoSetu) অ্যাপ্লিকেশনের গোপনীয়তা রক্ষা আমাদের অঙ্গীকার।</p>
          </div>

          <div className="p-10 space-y-12">
            <section className="space-y-4">
              <div className="flex items-center gap-3 text-red-600">
                <FileText className="w-6 h-6" />
                <h2 className="text-xl font-black uppercase tracking-widest">তথ্য সংগ্রহ</h2>
              </div>
              <p className="text-gray-600 leading-relaxed font-medium">
                আমরা আমাদের সেবার মান উন্নয়ন এবং রক্তদাতার সাথে গ্রহীতার সরাসরি যোগাযোগের সুবিধার্থে আপনার নাম, ফোন নম্বর, রক্তের গ্রুপ, বর্তমান অবস্থান এবং প্রোফাইল ছবি সংগ্রহ করি। এই তথ্যগুলো শুধুমাত্র রক্তদানের প্রয়োজনেই ব্যবহৃত হয়।
              </p>
            </section>

            <section className="space-y-4">
              <div className="flex items-center gap-3 text-red-600">
                <Eye className="w-6 h-6" />
                <h2 className="text-xl font-black uppercase tracking-widest">তথ্যের দৃশ্যমানতা</h2>
              </div>
              <p className="text-gray-600 leading-relaxed font-medium">
                আপনার রক্তের গ্রুপ এবং সাধারণ অবস্থান (জেলা/উপজেলা) অন্য ব্যবহারকারীরা দেখতে পারবেন যেন প্রয়োজনে আপনার সাথে যোগাযোগ করা যায়। তবে ফোনে সরাসরি কল বা চ্যাট করার মাধ্যমেই শুধুমাত্র নির্দিষ্ট ব্যক্তিরা আপনার সাথে যুক্ত হতে পারবেন।
              </p>
            </section>

            <section className="space-y-4">
              <div className="flex items-center gap-3 text-red-600">
                <Lock className="w-6 h-6" />
                <h2 className="text-xl font-black uppercase tracking-widest">নিরাপত্তা</h2>
              </div>
              <p className="text-gray-600 leading-relaxed font-medium">
                আপনার ব্যক্তিগত তথ্যের নিরাপত্তা আমাদের কাছে অত্যন্ত গুরুত্বপূর্ণ। আমরা আপনার মেসেজ এবং প্রোফাইল ডেটা এনক্রিপ্টেড অবস্থায় সুরক্ষিত সার্ভারে সংরক্ষণ করি। অযাচিত কোনো তৃতীয় পক্ষের কাছে আপনার তথ্য শেয়ার করা হয় না।
              </p>
            </section>

            <section className="space-y-4">
               <h2 className="text-xl font-black text-gray-900 leading-relaxed">আপনার অধিকার</h2>
               <p className="text-gray-600 leading-relaxed font-medium">
                 আপনি যেকোনো সময় আপনার প্রোফাইল তথ্য আপডেট করতে পারবেন অথবা আপনার ডোনার স্ট্যাটাস পরিবর্তন করতে পারবেন।
               </p>
            </section>

            <div className="pt-10 border-t border-gray-100 text-center">
              <p className="text-gray-400 text-sm font-bold">সর্বশেষ আপডেট: ১৭ এপ্রিল, ২০২৬</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
