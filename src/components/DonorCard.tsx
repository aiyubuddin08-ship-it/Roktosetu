import React, { useRef } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import { UserProfile } from '../types';
import { Heart, MapPin, Phone, User, Droplets, Download, CheckCircle, ShieldCheck, Mail, Award, Loader2 } from 'lucide-react';
import { motion } from 'motion/react';
import { cn } from '../lib/utils';
import { toPng } from 'html-to-image';
import { saveAs } from 'file-saver';

interface DonorCardProps {
  donor: UserProfile;
}

export function DonorCard({ donor }: DonorCardProps) {
  const cardRef = useRef<HTMLDivElement>(null);
  const [isDownloading, setIsDownloading] = React.useState(false);

  const handleDownload = async () => {
    if (cardRef.current === null) return;
    setIsDownloading(true);

    try {
      // Small delay to ensure all assets are rendered
      await new Promise(resolve => setTimeout(resolve, 500));
      
      const dataUrl = await toPng(cardRef.current, { 
        cacheBust: true, 
        pixelRatio: 4, // Ultra high quality
        backgroundColor: '#ffffff',
        style: {
          transform: 'scale(1)',
          transformOrigin: 'top left'
        }
      });
      
      saveAs(dataUrl, `RoktoSetu-ID-${donor.displayName.replace(/\s+/g, '-')}.png`);
    } catch (err) {
      console.error('Download failed:', err);
      alert('দুঃখিত, কার্ডটি ডাউনলোডে সমস্যা হয়েছে। দয়া করে স্ক্রিনশট নিন অথবা পরে চেষ্টা করুন।');
    } finally {
      setIsDownloading(false);
    }
  };

  return (
    <div className="flex flex-col items-center gap-10">
      <div className="relative group perspective-1000">
        {/* Aesthetic Glow */}
        <div className="absolute -inset-2 bg-gradient-to-tr from-red-600 via-red-900 to-black rounded-[44px] blur-xl opacity-20 group-hover:opacity-40 transition duration-1000"></div>
        
        {/* The Card */}
        <div 
          ref={cardRef}
          className="relative w-[360px] h-[580px] bg-white rounded-[32px] overflow-hidden flex flex-col shadow-[0_32px_64px_-16px_rgba(0,0,0,0.2)] border border-gray-100 select-none"
        >
          {/* Top Banner (Premium Header) */}
          <div className="h-40 bg-[#1a1a1a] relative overflow-hidden flex flex-col items-center justify-center text-white">
            {/* Geometric Pattern */}
            <div className="absolute inset-0 opacity-10" style={{ backgroundImage: `url("data:image/svg+xml,%3Csvg width='60' height='60' viewBox='0 0 60 60' xmlns='http://www.w3.org/2000/svg'%3E%3Cpath d='M30 0l30 30-30 30L0 30z' fill='%23ffffff' fill-opacity='1' fill-rule='evenodd'/%3E%3C/svg%3E")` }} />
            <div className="absolute top-0 right-0 w-32 h-32 bg-red-600 rounded-full blur-[60px] opacity-40 -translate-y-1/2 translate-x-1/2" />
            
            <div className="relative z-10 flex flex-col items-center">
              <div className="w-14 h-14 bg-red-600 rounded-2xl flex items-center justify-center mb-3 shadow-2xl shadow-red-900/50">
                <Heart className="w-8 h-8 text-white fill-current" />
              </div>
              <h3 className="text-2xl font-black tracking-widest uppercase italic">রক্তসেতু</h3>
              <div className="mt-1 px-3 py-0.5 bg-red-600/20 backdrop-blur-md rounded-full border border-red-500/30">
                <p className="text-[10px] font-black uppercase tracking-[0.4em] text-red-100">Official Membership ID</p>
              </div>
            </div>
          </div>

          {/* Identity Section */}
          <div className="flex-1 flex flex-col items-center p-8 -mt-10 relative z-20">
            {/* Profile Photo with Premium Border */}
            <div className="relative mb-6">
              <div className="w-36 h-36 rounded-[40px] p-1.5 bg-gradient-to-tr from-red-600 to-red-900 shadow-2xl">
                <div className="w-full h-full rounded-[34px] overflow-hidden bg-white">
                  {donor.photoURL ? (
                    <img 
                      src={donor.photoURL} 
                      alt={donor.displayName}
                      className="w-full h-full object-cover"
                      crossOrigin="anonymous"
                      referrerPolicy="no-referrer"
                    />
                  ) : (
                    <div className="w-full h-full flex items-center justify-center text-gray-200">
                      <User className="w-16 h-16" />
                    </div>
                  )}
                </div>
              </div>
              {/* Gold Verification Badge */}
              <div className="absolute -bottom-1 -right-1 bg-gradient-to-tr from-yellow-400 to-yellow-600 text-white p-2 rounded-2xl border-4 border-white shadow-xl">
                <Award className="w-6 h-6" />
              </div>
            </div>

            {/* Donor Basic Info */}
            <div className="text-center w-full mb-8">
              <h4 className="text-2xl font-black text-gray-900 tracking-tight leading-tight mb-1">{donor.displayName}</h4>
              <div className="flex items-center justify-center gap-2 text-gray-400">
                <ShieldCheck className="w-4 h-4 text-green-500" />
                <span className="text-xs font-bold tracking-wide uppercase">রক্তসেতু কর্তৃক স্বীকৃত</span>
              </div>
            </div>

            {/* Blood Group - Large & Impactful */}
            <div className="w-full flex gap-3 mb-8">
              <div className="flex-1 bg-red-600 rounded-3xl p-5 text-white flex flex-col items-center justify-center shadow-lg shadow-red-100">
                <span className="text-[10px] font-black uppercase tracking-widest opacity-70 mb-1">রক্তের গ্রুপ</span>
                <span className="text-4xl font-black">{donor.bloodGroup}</span>
              </div>
              <div className="w-20 bg-gray-50 rounded-3xl p-3 flex items-center justify-center border border-gray-100">
                <QRCodeSVG 
                  value={`${window.location.origin}/profile/${donor.uid}`} 
                  size={48}
                  level="H"
                />
              </div>
            </div>

            {/* Profile Statistics/Meta */}
            <div className="w-full space-y-4 text-left px-2">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-2xl bg-gray-100 flex items-center justify-center text-gray-400">
                  <Phone className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-[9px] font-black text-gray-400 uppercase tracking-widest leading-none mb-1">Call for help</p>
                  <p className="text-sm font-black text-gray-800 tracking-tight">{donor.phoneNumber}</p>
                </div>
              </div>
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 rounded-2xl bg-gray-100 flex items-center justify-center text-gray-400">
                  <MapPin className="w-4 h-4" />
                </div>
                <div>
                  <p className="text-[9px] font-black text-gray-400 uppercase tracking-widest leading-none mb-1">Last seen at</p>
                  <p className="text-sm font-black text-gray-800 truncate max-w-[180px]">{donor.location.upazila}, {donor.location.district}</p>
                </div>
              </div>
            </div>
          </div>

          {/* Footer Bar */}
          <div className="bg-red-600 py-3 px-8 flex items-center justify-between text-white">
            <span className="text-[9px] font-black uppercase tracking-widest">www.roktosetu.com</span>
            <div className="flex items-center gap-2">
              <div className="w-1 h-1 bg-white rounded-full animate-pulse" />
              <span className="text-[9px] font-mono opacity-80 uppercase">Verified Member</span>
            </div>
          </div>
        </div>
      </div>

      <button
        onClick={handleDownload}
        disabled={isDownloading}
        className={cn(
          "w-full max-w-[360px] py-5 rounded-[24px] font-black flex items-center justify-center gap-4 shadow-2xl transition-all duration-300 active:scale-95 disabled:opacity-50",
          isDownloading 
            ? "bg-gray-100 text-gray-400 cursor-not-allowed" 
            : "bg-gray-900 text-white hover:bg-black hover:shadow-black/20"
        )}
      >
        {isDownloading ? (
          <><Loader2 className="w-6 h-6 animate-spin" /> কার্ড তৈরি হচ্ছে...</>
        ) : (
          <><Download className="w-6 h-6" /> কার্ডটি সেভ করুন</>
        )}
      </button>
    </div>
  );
}
