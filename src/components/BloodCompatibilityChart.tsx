import React from 'react';
import { BloodGroup } from '../types';
import { Check, X, ShieldCheck, Heart } from 'lucide-react';
import { cn } from '../lib/utils';
import { motion } from 'motion/react';

const compatibility = {
  'O-': { give: ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'], receive: ['O-'] },
  'O+': { give: ['O+', 'A+', 'B+', 'AB+'], receive: ['O-', 'O+'] },
  'A-': { give: ['A-', 'A+', 'AB-', 'AB+'], receive: ['O-', 'A-'] },
  'A+': { give: ['A+', 'AB+'], receive: ['O-', 'O+', 'A-', 'A+'] },
  'B-': { give: ['B-', 'B+', 'AB-', 'AB+'], receive: ['O-', 'B-'] },
  'B+': { give: ['B+', 'AB+'], receive: ['O-', 'O+', 'B-', 'B+'] },
  'AB-': { give: ['AB-', 'AB+'], receive: ['O-', 'A-', 'B-', 'AB-'] },
  'AB+': { give: ['AB+'], receive: ['O-', 'O+', 'A-', 'A+', 'B-', 'B+', 'AB-', 'AB+'] },
};

const bloodGroups: BloodGroup[] = ['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'];

export function BloodCompatibilityChart() {
  const [selectedGroup, setSelectedGroup] = React.useState<BloodGroup | null>(null);

  return (
    <section className="bg-white dark:bg-gray-900 rounded-[40px] p-8 sm:p-10 border border-gray-100 dark:border-gray-800 shadow-2xl transition-colors">
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-6 mb-10">
        <div>
          <h2 className="text-3xl font-black text-gray-900 dark:text-white flex items-center gap-3">
             <ShieldCheck className="w-8 h-8 text-red-600" /> রক্তদান কম্প্যাটিবিলিটি
          </h2>
          <p className="text-gray-500 dark:text-gray-400 font-bold mt-2">আপনার রক্তের গ্রুপ অনুযায়ী কে আপনাকে রক্ত দিতে পারবে অথবা আপনি কাকে দিতে পারবেন তা জানুন।</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {bloodGroups.map(group => (
            <button
              key={group}
              onClick={() => setSelectedGroup(group === selectedGroup ? null : group)}
              className={cn(
                "w-12 h-12 rounded-2xl font-black text-sm transition-all shadow-md",
                selectedGroup === group 
                  ? "bg-red-600 text-white shadow-xl shadow-red-200 scale-110" 
                  : "bg-gray-100 dark:bg-gray-800 text-gray-400 hover:bg-red-50 dark:hover:bg-red-900/10 hover:text-red-600"
              )}
            >
              {group}
            </button>
          ))}
        </div>
      </div>

      {!selectedGroup ? (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-6">
           <div className="bg-red-50 dark:bg-red-900/10 p-6 rounded-3xl border border-red-100 dark:border-red-900/20 text-center space-y-2">
              <Heart className="w-8 h-8 text-red-600 mx-auto fill-current" />
              <p className="font-black text-red-900 dark:text-red-400">ইউনিভার্সাল ডোনার</p>
              <p className="text-4xl font-black text-red-600 tracking-tighter">O- NEGATIVE</p>
              <p className="text-xs text-red-500 font-bold">যেকোনো গ্রুপের মানুষকে রক্ত দিতে পারেন</p>
           </div>
           <div className="bg-blue-50 dark:bg-blue-900/10 p-6 rounded-3xl border border-blue-100 dark:border-blue-900/20 text-center space-y-2">
              <Heart className="w-8 h-8 text-blue-600 mx-auto fill-current" />
              <p className="font-black text-blue-900 dark:text-blue-400">ইউনিভার্সাল গ্রহীতা</p>
              <p className="text-4xl font-black text-blue-600 tracking-tighter">AB+ POSITIVE</p>
              <p className="text-xs text-blue-500 font-bold">সবার থেকে রক্ত গ্রহণ করতে পারেন</p>
           </div>
           <div className="sm:col-span-2 flex items-center justify-center p-8 bg-gray-50 dark:bg-gray-800/50 rounded-3xl border border-dashed border-gray-200 dark:border-gray-700">
              <p className="text-gray-400 font-black text-center uppercase tracking-widest leading-loose">
                বিস্তারিত দেখতে উপরের যেকোনো <br/> একটি রক্তের গ্রুপ নির্বাচন করুন
              </p>
           </div>
        </div>
      ) : (
        <motion.div 
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="grid md:grid-cols-2 gap-10"
        >
          <div className="space-y-6">
            <h3 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-2">
              <span className="w-1.5 h-6 bg-green-500 rounded-full" />
              রক্ত দান করতে পারবেন ({selectedGroup})
            </h3>
            <div className="grid grid-cols-4 gap-4">
              {bloodGroups.map(target => {
                const canGive = compatibility[selectedGroup].give.includes(target);
                return (
                  <div 
                    key={target}
                    className={cn(
                      "flex flex-col items-center gap-2 p-4 rounded-3xl transition-all",
                      canGive ? "bg-green-50 dark:bg-green-900/10 text-green-600" : "bg-gray-50 dark:bg-gray-800 text-gray-300 opacity-50"
                    )}
                  >
                    <span className="font-black text-lg">{target}</span>
                    {canGive ? <Check className="w-4 h-4" /> : <X className="w-4 h-4" />}
                  </div>
                );
              })}
            </div>
          </div>

          <div className="space-y-6">
            <h3 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-2">
              <span className="w-1.5 h-6 bg-blue-500 rounded-full" />
              রক্ত গ্রহণ করতে পারবেন ({selectedGroup})
            </h3>
            <div className="grid grid-cols-4 gap-4">
              {bloodGroups.map(source => {
                const canReceive = compatibility[selectedGroup].receive.includes(source);
                return (
                  <div 
                    key={source}
                    className={cn(
                      "flex flex-col items-center gap-2 p-4 rounded-3xl transition-all",
                      canReceive ? "bg-blue-50 dark:bg-blue-900/10 text-blue-600" : "bg-gray-50 dark:bg-gray-800 text-gray-300 opacity-50"
                    )}
                  >
                    <span className="font-black text-lg">{source}</span>
                    {canReceive ? <Check className="w-4 h-4" /> : <X className="w-4 h-4" />}
                  </div>
                );
              })}
            </div>
          </div>
        </motion.div>
      )}
    </section>
  );
}
