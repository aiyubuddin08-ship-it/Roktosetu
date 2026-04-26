import React, { useState, useEffect } from 'react';
import { collection, query, where, orderBy, onSnapshot, addDoc, deleteDoc, doc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { HealthRecord, OperationType } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Activity, Plus, Trash2, Calendar, Weight, Thermometer, Heart, TrendingUp, History, Info } from 'lucide-react';
import { format } from 'date-fns';
import { bn } from 'date-fns/locale';
import { motion, AnimatePresence } from 'motion/react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Legend } from 'recharts';
import { cn } from '../lib/utils';

export function HealthTracker() {
  const { user } = useAuth();
  const [records, setRecords] = useState<HealthRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [isAdding, setIsAdding] = useState(false);

  // Form state
  const [date, setDate] = useState(format(new Date(), 'yyyy-MM-dd'));
  const [bp, setBp] = useState('');
  const [hemoglobin, setHemoglobin] = useState('');
  const [weight, setWeight] = useState('');
  const [pulse, setPulse] = useState('');

  useEffect(() => {
    if (!user) return;

    const q = query(
      collection(db, 'health_records'),
      where('userId', '==', user.uid),
      orderBy('date', 'desc')
    );

    const unsubscribe = onSnapshot(q, (snapshot) => {
      setRecords(snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as HealthRecord)));
      setLoading(false);
    }, (error) => {
      handleFirestoreError(error, OperationType.LIST, 'health_records');
    });

    return () => unsubscribe();
  }, [user]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user) return;

    try {
      await addDoc(collection(db, 'health_records'), {
        userId: user.uid,
        date,
        bloodPressure: bp,
        hemoglobin: parseFloat(hemoglobin),
        weight: parseFloat(weight),
        pulse: pulse ? parseFloat(pulse) : null,
        createdAt: new Date().toISOString()
      });
      setIsAdding(false);
      setBp('');
      setHemoglobin('');
      setWeight('');
      setPulse('');
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'health_records');
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('আপনি কি এই রেকর্ডটি মুছে ফেলতে চান?')) return;
    try {
      await deleteDoc(doc(db, 'health_records', id));
    } catch (error) {
      handleFirestoreError(error, OperationType.DELETE, 'health_records');
    }
  };

  const chartData = [...records].reverse().map(r => ({
    date: format(new Date(r.date), 'dd/MM'),
    'হিমোগ্লোবিন': r.hemoglobin,
    'ওজন': r.weight
  }));

  return (
    <div className="max-w-4xl mx-auto pb-32 -mt-8 -mx-4 sm:-mx-6 lg:-mx-8 animate-fade-in bg-gray-50 dark:bg-gray-950 min-h-screen font-sans">
      {/* Header Section */}
      <section className="bg-brand pt-16 pb-12 px-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full -mr-32 -mt-32 blur-3xl" />
        <div className="max-w-xl mx-auto relative z-10 text-center">
          <h1 className="text-3xl font-black text-white mb-2">হেলথ ট্র্যাকার</h1>
          <p className="text-white/80 font-medium text-sm">আপনার স্বাস্থ্য ডাইরি। রক্তদানের আগে ও পরে শারীরিক পরিবর্তনের দিকে নজর রাখুন।</p>
        </div>
      </section>

      <div className="max-w-4xl mx-auto px-6 -mt-8 space-y-10">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* Main Tracker Card */}
          <div className="lg:col-span-2 space-y-8">
            {/* Charts Section */}
            <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl card-shadow border border-gray-50 dark:border-gray-800">
              <h2 className="text-2xl font-black text-gray-900 dark:text-white mb-8 flex items-center gap-3">
                <TrendingUp className="w-7 h-7 text-brand" /> স্বাস্থ্যের পরিবর্তন
              </h2>
              {chartData.length >= 2 ? (
                <div className="h-[300px] w-full">
                  <ResponsiveContainer width="100%" height="100%">
                    <LineChart data={chartData}>
                      <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f3f4f6" />
                      <XAxis dataKey="date" axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontWeight: 'bold'}} dy={10} />
                      <YAxis axisLine={false} tickLine={false} tick={{fill: '#9ca3af', fontWeight: 'bold'}} dx={-10} />
                      <Tooltip 
                        contentStyle={{borderRadius: '20px', border: 'none', boxShadow: '0 20px 25px -5px rgb(0 0 0 / 0.1)'}}
                        itemStyle={{fontWeight: 'bold'}}
                      />
                      <Legend iconType="circle" />
                      <Line type="monotone" dataKey="হিমোগ্লোবিন" stroke="#EF4444" strokeWidth={4} dot={{r: 6, fill: '#EF4444', strokeWidth: 2, stroke: '#fff'}} activeDot={{r: 8}} />
                      <Line type="monotone" dataKey="ওজন" stroke="#3B82F6" strokeWidth={4} dot={{r: 6, fill: '#3B82F6', strokeWidth: 2, stroke: '#fff'}} activeDot={{r: 8}} />
                    </LineChart>
                  </ResponsiveContainer>
                </div>
              ) : (
                <div className="h-[300px] flex flex-col items-center justify-center text-center space-y-4 bg-gray-50 dark:bg-gray-800/50 rounded-[32px] border-2 border-dashed border-gray-100 dark:border-gray-800">
                  <TrendingUp className="w-16 h-16 text-gray-200 dark:text-gray-700" />
                  <p className="text-gray-400 font-bold px-10">গত কয়েকদিনের ডাটা এন্ট্রি করলে এখানে আপনার স্বাস্থ্যের গ্রাফ দেখা যাবে।</p>
                </div>
              )}
            </div>

            {/* History */}
            <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl card-shadow border border-gray-50 dark:border-gray-800">
              <h2 className="text-2xl font-black text-gray-900 dark:text-white mb-8 flex items-center gap-3">
                <History className="w-7 h-7 text-brand" /> রেকর্ড হিস্ট্রি
              </h2>
              {records.length > 0 ? (
                <div className="space-y-4">
                  {records.map(record => (
                    <div key={record.id} className="bg-gray-50 dark:bg-gray-800 p-6 rounded-3xl flex flex-wrap items-center justify-between gap-6 group hover:bg-white dark:hover:bg-gray-700 transition-all border border-transparent hover:border-brand/10">
                      <div className="flex items-center gap-4 min-w-[150px]">
                        <div className="w-12 h-12 bg-red-50 dark:bg-red-900/20 rounded-xl flex items-center justify-center text-brand">
                          <Calendar className="w-6 h-6" />
                        </div>
                        <div>
                          <p className="font-black text-gray-900 dark:text-white uppercase tracking-tight">{format(new Date(record.date), 'dd MMM, yyyy', { locale: bn })}</p>
                          <p className="text-[10px] text-gray-400 font-bold uppercase tracking-widest">রেকর্ড করা হয়েছে</p>
                        </div>
                      </div>
                      <div className="flex flex-wrap gap-8 items-center flex-1">
                        <div className="flex items-center gap-2">
                           <Activity className="w-4 h-4 text-brand" />
                           <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest mr-1">BP:</span>
                           <span className="font-black text-gray-800 dark:text-gray-200">{record.bloodPressure}</span>
                        </div>
                        <div className="flex items-center gap-2">
                           <Thermometer className="w-4 h-4 text-orange-500" />
                           <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest mr-1">HGB:</span>
                           <span className="font-black text-gray-800 dark:text-gray-200">{record.hemoglobin} g/dL</span>
                        </div>
                        <div className="flex items-center gap-2">
                           <Weight className="w-4 h-4 text-blue-500" />
                           <span className="text-[10px] font-black text-gray-400 uppercase tracking-widest mr-1">WT:</span>
                           <span className="font-black text-gray-800 dark:text-gray-200">{record.weight} kg</span>
                        </div>
                      </div>
                      <button 
                        onClick={() => handleDelete(record.id)}
                        className="p-3 text-gray-300 hover:text-brand transition-colors opacity-0 group-hover:opacity-100"
                      >
                        <Trash2 className="w-5 h-5" />
                      </button>
                    </div>
                  ))}
                </div>
              ) : (
                <div className="bg-gray-50 dark:bg-gray-800/50 p-20 rounded-[40px] text-center border-2 border-dashed border-gray-100 dark:border-gray-800">
                  <History className="w-20 h-20 text-gray-200 dark:text-gray-700 mx-auto mb-6" />
                  <p className="text-gray-400 font-bold">এখনো কোনো রেকর্ড যোগ করা হয়নি।</p>
                </div>
              )}
            </div>
          </div>

          {/* Action Sidebar */}
          <div className="space-y-8">
            <button 
              onClick={() => setIsAdding(true)}
              className="w-full bg-brand hover:bg-brand/90 p-8 rounded-[40px] text-white shadow-2xl flex flex-col items-center gap-4 transition-all scale-100 hover:scale-[1.02] active:scale-95 shadow-brand/20 group"
            >
              <div className="w-16 h-16 bg-white/20 rounded-2xl flex items-center justify-center group-hover:scale-110 transition-transform">
                 <Plus className="w-8 h-8" />
              </div>
              <span className="text-xl font-black uppercase tracking-widest">তথ্য যোগ করুন</span>
            </button>

            <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl border border-gray-100 dark:border-gray-800 space-y-6">
            <h3 className="text-xl font-black text-gray-900 dark:text-white flex items-center gap-2">
              <Info className="w-6 h-6 text-brand" /> তথ্য ও সতর্কতা
            </h3>
            <ul className="space-y-4">
              <li className="flex gap-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl">
                <div className="w-2 h-2 mt-2 bg-brand rounded-full shrink-0" />
                <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">সাধারণত হিমোগ্লোবিন পুরুষের জন্য ১৩.৫-১৭.৫ এবং মহিলাদের জন্য ১২.০-১৫.৫ g/dL থাকা স্বাভাবিক।</p>
              </li>
              <li className="flex gap-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl">
                <div className="w-2 h-2 mt-2 bg-brand rounded-full shrink-0" />
                <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">রক্তদানের পর পর্যাপ্ত আয়রন জাতীয় খাবার এবং পানি পান করুন।</p>
              </li>
              <li className="flex gap-4 p-4 bg-gray-50 dark:bg-gray-800 rounded-2xl">
                <div className="w-2 h-2 mt-2 bg-brand rounded-full shrink-0" />
                <p className="text-sm text-gray-600 dark:text-gray-400 font-medium">ওজন ৫০ কেজির নিচে হলে রক্তদান করা নিরুৎসাহিত করা হয়।</p>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </div>

      {/* Add Modal */}
      <AnimatePresence>
        {isAdding && (
          <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
            <motion.div 
              initial={{ scale: 0.9, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.9, opacity: 0 }}
              className="bg-white dark:bg-gray-900 w-full max-w-xl rounded-[40px] p-10 shadow-2xl overflow-y-auto max-h-[90vh]"
            >
              <h2 className="text-3xl font-black mb-8 text-gray-900 dark:text-white flex items-center gap-3">
                <Activity className="w-8 h-8 text-brand" /> নতুন রেকর্ড যোগ করুন
              </h2>
              <form onSubmit={handleSubmit} className="space-y-6">
                <div>
                  <label className="block text-xs font-black text-gray-400 uppercase mb-2">তারিখ</label>
                  <input
                    type="date"
                    required
                    value={date}
                    onChange={(e) => setDate(e.target.value)}
                    className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-brand outline-none font-bold text-gray-700 dark:text-gray-300"
                  />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-black text-gray-400 uppercase mb-2">সারীরিক চাপ (BP)</label>
                    <input
                      required
                      placeholder="120/80"
                      value={bp}
                      onChange={(e) => setBp(e.target.value)}
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-brand outline-none font-bold text-gray-700 dark:text-gray-300"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-black text-gray-400 uppercase mb-2">ওজন (KG)</label>
                    <input
                      type="number"
                      step="0.1"
                      required
                      placeholder="70"
                      value={weight}
                      onChange={(e) => setWeight(e.target.value)}
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-brand outline-none font-bold text-gray-700 dark:text-gray-300"
                    />
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-black text-gray-400 uppercase mb-2">হিমোগ্লোবিন (g/dL)</label>
                    <input
                      type="number"
                      step="0.1"
                      required
                      placeholder="14.5"
                      value={hemoglobin}
                      onChange={(e) => setHemoglobin(e.target.value)}
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-brand outline-none font-bold text-gray-700 dark:text-gray-300"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-black text-gray-400 uppercase mb-2">পালস (ঐচ্ছিক)</label>
                    <input
                      type="number"
                      placeholder="72"
                      value={pulse}
                      onChange={(e) => setPulse(e.target.value)}
                      className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-brand outline-none font-bold text-gray-700 dark:text-gray-300"
                    />
                  </div>
                </div>
                <div className="flex gap-4 pt-4">
                  <button
                    type="button"
                    onClick={() => setIsAdding(false)}
                    className="flex-1 py-4 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded-2xl font-black hover:bg-gray-200 transition-all font-sans"
                  >
                    বাতিল
                  </button>
                  <button
                    type="submit"
                    className="flex-2 py-4 bg-brand text-white rounded-2xl font-black hover:bg-brand/90 transition-all shadow-xl shadow-brand/30"
                  >
                    সেভ করুন
                  </button>
                </div>
              </form>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
