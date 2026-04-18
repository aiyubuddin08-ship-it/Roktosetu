import React, { useState } from 'react';
import { Phone, MapPin, Search, Building2, Truck, ExternalLink } from 'lucide-react';
import { cn } from '../lib/utils';

interface DirectoryItem {
  name: string;
  type: 'Blood Bank' | 'Ambulance';
  location: string;
  phone: string;
  availability: string;
}

const DIRECTORY_DATA: DirectoryItem[] = [
  { name: 'রেড ক্রিসেন্ট ব্লাড ব্যাংক', type: 'Blood Bank', location: 'ঢাকা (মোহাম্মদপুর)', phone: '০২-৯১১২২৩৩', availability: '২৪ ঘণ্টা' },
  { name: 'কোয়ান্টাম ব্লাড ল্যাব', type: 'Blood Bank', location: 'ঢাকা (শান্তিনগর)', phone: '০২-৯৩৫৩৩৩৩', availability: '২৪ ঘণ্টা' },
  { name: 'সন্ধানী ব্লাড ব্যাংক', type: 'Blood Bank', location: 'ঢাকা মেডিকেল কলেজ', phone: '০২-৫৫০৬১২৩৪', availability: '২৪ ঘণ্টা' },
  { name: 'পুলিশ ব্লাড ব্যাংক', type: 'Blood Bank', location: 'রাজারবাগ, ঢাকা', phone: '০১৭৬৯৬৯০০০০', availability: '২৪ ঘণ্টা' },
  { name: 'আঞ্জুমান মফিদুল ইসলাম', type: 'Ambulance', location: 'ঢাকা (কাকরাইল)', phone: '০২-৯৩৩৩৩০১', availability: '২৪ ঘণ্টা' },
  { name: 'ফায়ার সার্ভিস কন্ট্রোল রুম', type: 'Ambulance', location: 'সারাদেশ', phone: '০২-৯৫৫৫৫৫৫', availability: '২৪ ঘণ্টা' },
  { name: 'ঢাকা মেডিকেল অ্যাম্বুলেন্স', type: 'Ambulance', location: 'ঢাকা', phone: '০১৯১১২২৩৩৪৪', availability: '২৪ ঘণ্টা' },
  { name: 'বরিশাল ব্লাড ব্যাংক', type: 'Blood Bank', location: 'বরিশাল সদর', phone: '০১৭৫২৫ ২৫২৫', availability: '২৪ ঘণ্টা' },
  { name: 'রাজশাহী ব্লাড ব্যাংক', type: 'Blood Bank', location: 'রাজশাহী মেডিকেল', phone: '০১৮১৬০ ৪৪৯৪৯', availability: '২৪ ঘণ্টা' },
  { name: 'চট্টগ্রাম ব্লাড ব্যাংক', type: 'Blood Bank', location: 'আন্দরকিল্লা, চট্টগ্রাম', phone: '০৩১-৬১২৩৪৪', availability: '২৪ ঘণ্টা' },
  { name: 'সেন্ট জন অ্যাম্বুলেন্স', type: 'Ambulance', location: 'ঢাকা', phone: '০২-৯১৩১০১০', availability: '২৪ ঘণ্টা' },
  { name: 'আল-মারকাজুল ইসলামি', type: 'Ambulance', location: 'ঢাকা (শ্যামলী)', phone: '০২-৯১২৭৮৬৭', availability: '২৪ ঘণ্টা' },
];

export function Directory() {
  const [searchTerm, setSearchTerm] = useState('');
  const [filter, setFilter] = useState<'All' | 'Blood Bank' | 'Ambulance'>('All');

  const filteredItems = DIRECTORY_DATA.filter(item => {
    const matchesSearch = item.name.toLowerCase().includes(searchTerm.toLowerCase()) || 
                         item.location.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesFilter = filter === 'All' || item.type === filter;
    return matchesSearch && matchesFilter;
  });

  return (
    <div className="space-y-10 pb-20 animate-fade-in">
       <div className="text-center space-y-4">
        <h1 className="text-4xl font-black text-gray-900 tracking-tight">জরুরি ডিরেক্টরি</h1>
        <p className="text-gray-500 font-bold text-lg max-w-2xl mx-auto">
          আপনার প্রয়োজনে নিকটস্থ ব্লাড ব্যাংক এবং অ্যাম্বুলেন্স সার্ভিসের তথ্য এখানে পাবেন।
        </p>
      </div>

      <div className="bg-white p-8 rounded-[40px] shadow-2xl border border-gray-100 space-y-8">
        <div className="flex flex-col md:flex-row gap-6">
          <div className="flex-1 relative">
            <Search className="absolute left-6 top-1/2 -translate-y-1/2 w-6 h-6 text-gray-400" />
            <input
              type="text"
              placeholder="নাম বা এলাকা দিয়ে খুঁজুন..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-16 pr-8 py-5 bg-gray-50 border-none rounded-[32px] focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 text-lg shadow-inner"
            />
          </div>
          <div className="flex gap-2 p-2 bg-gray-50 rounded-[32px] shadow-inner">
            {(['All', 'Blood Bank', 'Ambulance'] as const).map((type) => (
              <button
                key={type}
                onClick={() => setFilter(type)}
                className={cn(
                  "px-8 py-3 rounded-2xl font-black text-sm uppercase tracking-widest transition-all",
                  filter === type ? "bg-white text-red-600 shadow-md scale-105" : "text-gray-400 hover:text-gray-600"
                )}
              >
                {type === 'All' ? 'সব' : type === 'Blood Bank' ? 'ব্লাড ব্যাংক' : 'অ্যাম্বুলেন্স'}
              </button>
            ))}
          </div>
        </div>

        <div className="grid md:grid-cols-2 gap-6">
          {filteredItems.map((item, index) => (
            <div key={index} className="bg-white p-8 rounded-[40px] border border-gray-100 shadow-xl hover:shadow-2xl transition-all group flex gap-6">
              <div className={cn(
                "w-20 h-20 rounded-3xl flex items-center justify-center shrink-0 shadow-lg group-hover:scale-110 transition-transform",
                item.type === 'Blood Bank' ? "bg-red-50 text-red-600" : "bg-blue-50 text-blue-600"
              )}>
                {item.type === 'Blood Bank' ? <Building2 className="w-10 h-10" /> : <Truck className="w-10 h-10" />}
              </div>
              <div className="flex-1 space-y-3">
                <div className="flex items-center justify-between">
                  <span className={cn(
                    "px-3 py-1 rounded-lg text-[10px] font-black uppercase tracking-widest",
                    item.type === 'Blood Bank' ? "bg-red-100 text-red-700" : "bg-blue-100 text-blue-700"
                  )}>
                    {item.type === 'Blood Bank' ? 'ব্লাড ব্যাংক' : 'অ্যাম্বুলেন্স'}
                  </span>
                  <span className="text-xs text-green-600 font-bold flex items-center gap-1">
                    <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse" /> {item.availability}
                  </span>
                </div>
                <h3 className="text-xl font-black text-gray-900 leading-tight">{item.name}</h3>
                <p className="text-gray-500 font-bold flex items-center gap-2">
                  <MapPin className="w-4 h-4 text-gray-400" /> {item.location}
                </p>
                <a
                  href={`tel:${item.phone}`}
                  className="flex items-center justify-between p-4 bg-gray-50 group-hover:bg-red-600 transition-colors rounded-2xl group-hover:text-white"
                >
                  <span className="font-black flex items-center gap-2"><Phone className="w-5 h-5" /> {item.phone}</span>
                  <ExternalLink className="w-5 h-5 opacity-0 group-hover:opacity-100 transition-opacity" />
                </a>
              </div>
            </div>
          ))}
          {filteredItems.length === 0 && (
            <div className="md:col-span-2 py-20 text-center text-gray-400 font-bold">
               বর্তমানে কোনো তথ্য পাওয়া যায়নি।
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
