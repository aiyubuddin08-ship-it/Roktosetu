import React, { useState } from 'react';
import { motion, AnimatePresence } from 'motion/react';
import { X, CheckCircle, AlertCircle, Calendar, Weight, Thermometer, Heart } from 'lucide-react';
import { cn } from '../lib/utils';

interface EligibilityCheckerProps {
  isOpen: boolean;
  onClose: () => void;
}

export function EligibilityChecker({ isOpen, onClose }: EligibilityCheckerProps) {
  const [step, setStep] = useState(0);
  const [results, setResults] = useState<boolean[]>([]);

  const questions = [
    { title: "আপনার বয়স কি ১৮ থেকে ৬০ বছরের মধ্যে?", icon: Calendar },
    { title: "আপনার ওজন কি ৫০ কেজির বেশি?", icon: Weight },
    { title: "আপনি কি গত ৩ মাসে রক্ত দিয়েছেন?", icon: Heart, reverse: true }, // No is eligible
    { title: "আপনার কি বর্তমানে কোনো বড় ধরণের অসুস্থতা (জ্বর, কাশি) আছে?", icon: AlertCircle, reverse: true },
    { title: "আপনি কি গত ২ সপ্তাহে কোনো ওষুধ খেলেছেন?", icon: Thermometer, reverse: true }
  ];

  const handleAnswer = (answer: boolean) => {
    const isEligible = questions[step].reverse ? !answer : answer;
    const newResults = [...results, isEligible];
    setResults(newResults);
    if (step < questions.length - 1) {
      setStep(step + 1);
    } else {
      setStep(step + 1); // Switch to result
    }
  };

  const isOverallEligible = results.every(r => r === true);

  const reset = () => {
    setStep(0);
    setResults([]);
  };

  return (
    <AnimatePresence>
      {isOpen && (
        <div className="fixed inset-0 z-[100] flex items-center justify-center p-4">
          <motion.div
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            onClick={onClose}
            className="absolute inset-0 bg-black/60 backdrop-blur-sm"
          />
          <motion.div
            initial={{ opacity: 0, scale: 0.9, y: 20 }}
            animate={{ opacity: 1, scale: 1, y: 0 }}
            exit={{ opacity: 0, scale: 0.9, y: 20 }}
            className="relative bg-white w-full max-w-xl rounded-[40px] shadow-2xl overflow-hidden"
          >
            <div className="bg-red-600 p-8 text-white flex items-center justify-between">
              <h2 className="text-2xl font-black">যোগ্যতা যাচাই করুন</h2>
              <button onClick={onClose} className="p-2 hover:bg-red-700 rounded-xl transition-colors">
                <X className="w-6 h-6" />
              </button>
            </div>

            <div className="p-10">
              {step < questions.length ? (
                <div className="space-y-8">
                  <div className="flex flex-col items-center text-center space-y-6">
                    <div className="w-24 h-24 bg-red-50 rounded-[32px] flex items-center justify-center text-red-600 shadow-inner">
                      {React.createElement(questions[step].icon, { className: "w-12 h-12" })}
                    </div>
                    <h3 className="text-2xl font-black text-gray-900 leading-tight">
                      {questions[step].title}
                    </h3>
                  </div>

                  <div className="grid grid-cols-2 gap-4">
                    <button
                      onClick={() => handleAnswer(true)}
                      className="py-5 bg-green-500 text-white rounded-[24px] font-black text-xl hover:bg-green-600 transition-all shadow-xl shadow-green-100 active:scale-95"
                    >
                      হ্যাঁ
                    </button>
                    <button
                      onClick={() => handleAnswer(false)}
                      className="py-5 bg-gray-100 text-gray-500 rounded-[24px] font-black text-xl hover:bg-gray-200 transition-all active:scale-95"
                    >
                      না
                    </button>
                  </div>

                  <div className="flex justify-center gap-2">
                    {questions.map((_, i) => (
                      <div
                        key={i}
                        className={cn(
                          "h-2 rounded-full transition-all duration-300",
                          i === step ? "w-8 bg-red-600" : "w-2 bg-gray-200"
                        )}
                      />
                    ))}
                  </div>
                </div>
              ) : (
                <div className="text-center space-y-8 py-4">
                  <div className={cn(
                    "w-32 h-32 rounded-[40px] flex items-center justify-center mx-auto shadow-2xl transition-transform duration-500 scale-110",
                    isOverallEligible ? "bg-green-500 text-white shadow-green-200" : "bg-red-50 text-red-600 shadow-red-50"
                  )}>
                    {isOverallEligible ? <CheckCircle className="w-20 h-20" /> : <AlertCircle className="w-20 h-20" />}
                  </div>

                  <div className="space-y-3">
                    <h3 className={cn(
                      "text-3xl font-black px-4",
                      isOverallEligible ? "text-green-600" : "text-red-600"
                    )}>
                      {isOverallEligible ? "আপনি রক্তদানের জন্য যোগ্য!" : "আপনি বর্তমানে রক্তদানের জন্য যোগ্য নন।"}
                    </h3>
                    <p className="text-gray-500 font-bold max-w-sm mx-auto">
                      {isOverallEligible 
                        ? "আপনার রক্ত দান অন্য একজনের প্রাণ বাঁচাতে পারে। আজই ডোনার হিসেবে এগিয়ে আসুন।" 
                        : "দয়া করে রক্তদানের আগে স্থানীয় চিকিৎসকের পরামর্শ নিন বা সুস্থ হওয়া পর্যন্ত অপেক্ষা করুন।"}
                    </p>
                  </div>

                  <div className="flex flex-col gap-3">
                    <button
                      onClick={onClose}
                      className="w-full py-5 bg-gray-900 text-white rounded-[24px] font-black text-lg hover:bg-black transition-all shadow-xl"
                    >
                      বন্ধ করুন
                    </button>
                    <button
                      onClick={reset}
                      className="w-full py-3 text-red-600 font-bold hover:underline"
                    >
                      আবার যাচাই করুন
                    </button>
                  </div>
                </div>
              )}
            </div>
          </motion.div>
        </div>
      )}
    </AnimatePresence>
  );
}
