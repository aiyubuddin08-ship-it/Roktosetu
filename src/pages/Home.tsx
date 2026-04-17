import React from 'react';
import { useAuth } from '../contexts/AuthContext';
import { Heart, Shield, Zap, Search, Users, MapPin } from 'lucide-react';
import { motion } from 'motion/react';

export function Home() {
  const { login } = useAuth();

  const features = [
    { icon: Zap, title: 'জরুরি এলার্ট', desc: 'রক্তের প্রয়োজনে তাৎক্ষণিক নোটিফিকেশন।' },
    { icon: Search, title: 'সহজ সার্চ', desc: 'গ্রুপ এবং এলাকা অনুযায়ী ডোনার খুঁজে নিন।' },
    { icon: Shield, title: 'নিরাপদ', desc: 'যাচাইকৃত ডোনার প্রোফাইল এবং নিরাপদ ডাটা।' },
    { icon: Users, title: 'কমিউনিটি', desc: 'হাজার হাজার জীবন রক্ষাকারীর সাথে যুক্ত হোন।' },
  ];

  return (
    <div className="space-y-20 pb-20">
      {/* Hero */}
      <section className="relative h-[90vh] flex items-center overflow-hidden -mx-4 sm:-mx-6 lg:-mx-8">
        <div className="absolute inset-0 z-0">
          <img 
            src="https://picsum.photos/seed/blood/1920/1080?blur=4" 
            alt="Hero Background" 
            className="w-full h-full object-cover opacity-20"
            referrerPolicy="no-referrer"
          />
          <div className="absolute inset-0 bg-gradient-to-b from-transparent to-red-50/30" />
        </div>

        <div className="relative z-10 max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 text-center sm:text-left flex flex-col md:flex-row items-center justify-between gap-12">
          <motion.div 
            initial={{ opacity: 0, x: -50 }}
            animate={{ opacity: 1, x: 0 }}
            className="md:w-1/2 space-y-8"
          >
            <h1 className="text-6xl lg:text-8xl font-black text-gray-900 leading-tight">
              এক ফোঁটা রক্ত, <br/>একটি <span className="text-red-600">জীবন</span>।
            </h1>
            <p className="text-2xl text-gray-700 max-w-lg leading-relaxed">
              রক্তসেতু একটি আধুনিক প্ল্যাটফর্ম যা রক্তদাতা এবং গ্রহীতাদের মধ্যে সেতুবন্ধন তৈরি করে। আজই আমাদের মিশনে যোগ দিন।
            </p>
            <div className="flex flex-col sm:flex-row gap-6 pt-4">
              <button 
                onClick={login}
                className="px-10 py-5 bg-red-600 text-white rounded-3xl font-bold text-xl hover:bg-red-700 transition shadow-2xl shadow-red-200"
              >
                রক্তদাতা হিসেবে যোগ দিন
              </button>
              <button 
                onClick={login}
                className="px-10 py-5 bg-white text-gray-900 border-2 border-gray-200 rounded-3xl font-bold text-xl hover:border-red-600 transition"
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
            <div className="w-full aspect-square bg-red-100/50 rounded-full flex items-center justify-center blood-pulse relative shadow-inner">
               <Heart className="w-48 h-48 text-red-600 fill-current drop-shadow-lg" />
            </div>
            {/* Floating stats */}
            <div className="absolute top-10 right-0 bg-white p-6 rounded-3xl shadow-2xl flex items-center gap-4 animate-bounce">
              <div className="w-12 h-12 bg-green-100 rounded-2xl flex items-center justify-center text-green-600">
                <Users className="w-8 h-8" />
              </div>
              <div>
                <p className="text-sm font-bold text-gray-400 uppercase">রক্তদাতা</p>
                <p className="text-2xl font-black text-gray-900">১২,৪০০+</p>
              </div>
            </div>
          </motion.div>
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

      {/* CTA Section */}
      <section className="bg-gray-900 text-white rounded-[60px] p-20 text-center relative overflow-hidden shadow-2xl mx-4">
        <div className="relative z-10 max-w-3xl mx-auto">
          <h2 className="text-5xl font-black mb-8 leading-tight">আপনি কি রক্তের মাধ্যমে জীবন বাঁচাতে প্রস্তুত?</h2>
          <p className="text-gray-400 text-xl mb-12 leading-relaxed">
            প্রতিটি রক্তদাতা একজন হিরো। সবচেয়ে নির্ভরযোগ্য রক্ত নেটওয়ার্কের অংশ হতে এখন নিবন্ধন করুন।
          </p>
          <button 
            onClick={login}
            className="px-12 py-6 bg-red-600 text-white rounded-[30px] font-black text-2xl hover:bg-red-700 transition shadow-2xl shadow-red-500/30"
          >
            এখনই শুরু করুন
          </button>
        </div>
        <div className="absolute top-0 right-0 w-96 h-96 bg-red-600/10 rounded-full blur-[120px] -mr-48 -mt-48" />
        <div className="absolute bottom-0 left-0 w-96 h-96 bg-red-600/10 rounded-full blur-[120px] -ml-48 -mb-48" />
      </section>
    </div>
  );
}
