import React, { useState, useEffect } from 'react';
import { collection, query, where, orderBy, onSnapshot, addDoc, serverTimestamp, doc, updateDoc, increment, arrayUnion } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { useAuth } from '../contexts/AuthContext';
import { SuccessStory, DonationEvent, OperationType, BloodStockAlert, BloodGroup, Organization } from '../types';
import { handleFirestoreError } from '../lib/error-handler';
import { Heart, Calendar, MessageSquare, ThumbsUp, Plus, Image as ImageIcon, MapPin, Phone, Users, Share2, Megaphone, Clock } from 'lucide-react';
import { formatDistanceToNow } from 'date-fns';
import { bn } from 'date-fns/locale';
import { motion, AnimatePresence } from 'motion/react';
import { cn } from '../lib/utils';

export function Community() {
  const { user, profile } = useAuth();
  const [stories, setStories] = useState<SuccessStory[]>([]);
  const [events, setEvents] = useState<DonationEvent[]>([]);
  const [alerts, setAlerts] = useState<BloodStockAlert[]>([]);
  const [activeTab, setActiveTab] = useState<'stories' | 'events' | 'alerts'>('stories');
  const [isPosting, setIsPosting] = useState(false);
  const [isPostingEvent, setIsPostingEvent] = useState(false);
  const [isPostingAlert, setIsPostingAlert] = useState(false);
  
  // Alert state
  const [alertBloodGroup, setAlertBloodGroup] = useState<BloodGroup>('A+');
  const [alertDistrict, setAlertDistrict] = useState('');
  const [alertMessage, setAlertMessage] = useState('');

  const [newContent, setNewContent] = useState('');
  const [imageUrl, setImageUrl] = useState('');
  
  // Event state
  const [eventTitle, setEventTitle] = useState('');
  const [eventDate, setEventDate] = useState('');
  const [eventLocation, setEventLocation] = useState('');
  const [eventDescription, setEventDescription] = useState('');
  const [eventContact, setEventContact] = useState('');
  
  const [userOrganizations, setUserOrganizations] = useState<Organization[]>([]);
  const [selectedOrgId, setSelectedOrgId] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const qStories = query(collection(db, 'stories'), orderBy('createdAt', 'desc'));
    const qEvents = query(collection(db, 'events'), orderBy('date', 'asc'));
    const qAlerts = query(collection(db, 'alerts'), orderBy('createdAt', 'desc'));

    const unsubStories = onSnapshot(qStories, (snap) => {
      setStories(snap.docs.map(d => ({ id: d.id, ...d.data() } as SuccessStory)));
      setLoading(false);
    });

    const unsubEvents = onSnapshot(qEvents, (snap) => {
      setEvents(snap.docs.map(d => ({ id: d.id, ...d.data() } as DonationEvent)));
    });

    const unsubAlerts = onSnapshot(qAlerts, (snap) => {
       setAlerts(snap.docs.map(d => ({ id: d.id, ...d.data() } as BloodStockAlert)));
    });

    // Fetch managed organizations
    if (user) {
      const qOrgs = query(collection(db, 'organizations'), where('adminUid', '==', user.uid));
      const unsubOrgs = onSnapshot(qOrgs, (snap) => {
        const orgs = snap.docs.map(d => ({ id: d.id, ...d.data() } as Organization));
        setUserOrganizations(orgs);
        if (orgs.length > 0) {
          setSelectedOrgId(orgs[0].id);
          setAlertDistrict(orgs[0].location.district);
        }
      });
      return () => {
        unsubStories();
        unsubEvents();
        unsubAlerts();
        unsubOrgs();
      };
    }

    return () => {
      unsubStories();
      unsubEvents();
      unsubAlerts();
    };
  }, [user]);

  const handlePostStory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !profile || !newContent.trim()) return;

    try {
      await addDoc(collection(db, 'stories'), {
        authorId: user.uid,
        authorName: profile.displayName,
        authorPhoto: profile.photoURL || null,
        content: newContent,
        imageUrl: imageUrl || null,
        likes: 0,
        likedByCount: 0,
        createdAt: new Date().toISOString(),
        location: profile.location.district
      });
      setNewContent('');
      setImageUrl('');
      setIsPosting(false);
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'stories');
    }
  };

  const handlePostEvent = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !selectedOrgId) return;

    const org = userOrganizations.find(o => o.id === selectedOrgId);
    if (!org) return;

    try {
      await addDoc(collection(db, 'events'), {
        orgId: org.id,
        orgName: org.name,
        orgAdminUid: org.adminUid,
        title: eventTitle,
        description: eventDescription,
        date: eventDate,
        location: eventLocation,
        contactNumber: eventContact,
        imageUrl: imageUrl || null,
        createdAt: new Date().toISOString()
      });
      setIsPostingEvent(false);
      // Reset
      setEventTitle('');
      setEventDate('');
      setEventLocation('');
      setEventDescription('');
      setEventContact('');
      setImageUrl('');
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'events');
    }
  };

  const handlePostAlert = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!user || !selectedOrgId) return;

    const org = userOrganizations.find(o => o.id === selectedOrgId);
    if (!org) return;

    try {
      await addDoc(collection(db, 'alerts'), {
        orgId: org.id,
        orgName: org.name,
        orgAdminUid: org.adminUid,
        bloodGroup: alertBloodGroup,
        district: alertDistrict,
        message: alertMessage,
        createdAt: new Date().toISOString()
      });
      setIsPostingAlert(false);
      setAlertMessage('');
    } catch (error) {
      handleFirestoreError(error, OperationType.CREATE, 'alerts');
    }
  };

  const handleLike = async (storyId: string) => {
    if (!user) return;
    try {
      const storyRef = doc(db, 'stories', storyId);
      await updateDoc(storyRef, {
        likes: increment(1)
      });
    } catch (error) {
       console.error("Like failed:", error);
    }
  };

  return (
    <div className="max-w-4xl mx-auto pb-32 -mt-8 -mx-4 sm:-mx-6 lg:-mx-8 animate-fade-in bg-gray-50 dark:bg-gray-950 min-h-screen font-sans">
      {/* Header Section */}
      <section className="bg-brand pt-16 pb-12 px-6 relative overflow-hidden">
        <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full -mr-32 -mt-32 blur-3xl" />
        <div className="max-w-xl mx-auto relative z-10 text-center">
          <h1 className="text-3xl font-black text-white mb-2">রক্তসেতু কমিউনিটি</h1>
          <p className="text-white/80 font-medium text-sm">সফলতার গল্প ভাগাভাগি করুন এবং নতুন ইভেন্টে অংশ নিন।</p>
        </div>
      </section>

      <div className="max-w-4xl mx-auto px-6 -mt-8 space-y-10">
        {/* Tabs */}
        <div className="flex bg-white/10 backdrop-blur-md p-1.5 rounded-[32px] w-full max-w-lg mx-auto shadow-2xl overflow-x-auto sm:overflow-x-visible border border-white/20">
          <button
            onClick={() => setActiveTab('stories')}
            className={cn(
              "flex-1 py-3 px-4 rounded-[24px] font-black transition-all flex items-center justify-center gap-2 whitespace-nowrap text-sm sm:text-base",
              activeTab === 'stories' ? "bg-white text-brand shadow-xl" : "text-white/80 hover:text-white"
            )}
          >
            <Heart className="w-4 h-4 sm:w-5 sm:h-5" /> সফলতার গল্প
          </button>
          <button
            onClick={() => setActiveTab('events')}
            className={cn(
              "flex-1 py-3 px-4 rounded-[24px] font-black transition-all flex items-center justify-center gap-2 whitespace-nowrap text-sm sm:text-base",
              activeTab === 'events' ? "bg-white text-brand shadow-xl" : "text-white/80 hover:text-white"
            )}
          >
            <Calendar className="w-4 h-4 sm:w-5 sm:h-5" /> ইভেন্ট
          </button>
          <button
            onClick={() => setActiveTab('alerts')}
            className={cn(
              "flex-1 py-3 px-4 rounded-[24px] font-black transition-all flex items-center justify-center gap-2 whitespace-nowrap text-sm sm:text-base",
              activeTab === 'alerts' ? "bg-white text-brand shadow-xl" : "text-white/80 hover:text-white"
            )}
          >
            <Megaphone className="w-4 h-4 sm:w-5 sm:h-5" /> এলার্ট
          </button>
        </div>

        <div className="space-y-8">
          {activeTab === 'stories' ? (
            <>
              {/* Post Story Button */}
              <div className="bg-white dark:bg-gray-900 p-8 rounded-[40px] shadow-2xl card-shadow border border-gray-50 dark:border-gray-800 flex items-center gap-6 group hover:border-brand transition-all cursor-pointer" onClick={() => setIsPosting(true)}>
                <div className="w-16 h-16 bg-red-50 dark:bg-red-900/20 rounded-3xl flex items-center justify-center text-brand group-hover:scale-110 transition-transform">
                  <Plus className="w-8 h-8" />
                </div>
                <div>
                  <h3 className="text-2xl font-black text-gray-900 dark:text-white uppercase">আপনার গল্প শেয়ার করুন</h3>
                  <p className="text-gray-500 font-bold">অন্যদের অনুপ্রাণিত করতে আপনার রক্তদানের অভিজ্ঞতা লিখুন।</p>
                </div>
              </div>

            {/* Posting Modal */}
            <AnimatePresence>
              {isPosting && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                  <motion.div 
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    className="bg-white dark:bg-gray-900 w-full max-w-xl rounded-[40px] p-10 shadow-2xl"
                  >
                    <h2 className="text-3xl font-black mb-8 text-gray-900 dark:text-white">সফলতার গল্প</h2>
                    <form onSubmit={handlePostStory} className="space-y-6">
                      <div>
                        <textarea
                          required
                          value={newContent}
                          onChange={(e) => setNewContent(e.target.value)}
                          placeholder="আপনার অভিজ্ঞতা বিস্তারিত লিখুন..."
                          className="w-full h-40 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl p-6 focus:ring-2 focus:ring-red-500 outline-none font-medium resize-none text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div>
                        <input
                          type="url"
                          value={imageUrl}
                          onChange={(e) => setImageUrl(e.target.value)}
                          placeholder="ছবির লিংক (ঐচ্ছিক)"
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div className="flex gap-4">
                        <button
                          type="button"
                          onClick={() => setIsPosting(false)}
                          className="flex-1 py-4 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded-2xl font-black hover:bg-gray-200 transition-all"
                        >
                          বাতিল
                        </button>
                        <button
                          type="submit"
                          className="flex-2 py-4 bg-red-600 text-white rounded-2xl font-black hover:bg-red-700 transition-all shadow-xl shadow-red-500/30"
                        >
                          শেয়ার করুন
                        </button>
                      </div>
                    </form>
                  </motion.div>
                </div>
              )}
            </AnimatePresence>

            {/* Stories Feed */}
            {stories.length > 0 ? (
              <div className="space-y-12">
                {stories.map(story => (
                  <motion.div 
                    initial={{ opacity: 0, y: 20 }}
                    whileInView={{ opacity: 1, y: 0 }}
                    viewport={{ once: true }}
                    key={story.id} 
                    className="bg-white dark:bg-gray-900 rounded-[48px] border border-gray-100 dark:border-gray-800 shadow-2xl overflow-hidden group"
                  >
                    {story.imageUrl && (
                      <div className="aspect-[21/9] overflow-hidden">
                        <img 
                          src={story.imageUrl} 
                          alt="Story" 
                          className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-700" 
                          referrerPolicy="no-referrer"
                        />
                      </div>
                    )}
                    <div className="p-10 space-y-6">
                      <div className="flex items-center gap-4">
                        <div className="w-14 h-14 bg-red-600 text-white rounded-2xl flex items-center justify-center font-black text-xl shadow-lg">
                          {story.authorName[0]}
                        </div>
                        <div>
                          <p className="font-black text-xl text-gray-900 dark:text-white uppercase tracking-tight">{story.authorName}</p>
                          <p className="text-sm text-gray-500 font-bold">{story.location} • {formatDistanceToNow(new Date(story.createdAt), { locale: bn })} আগে</p>
                        </div>
                      </div>
                      <p className="text-gray-700 dark:text-gray-300 text-xl font-medium leading-relaxed">
                        {story.content}
                      </p>
                      <div className="pt-6 border-t border-gray-50 flex items-center justify-between">
                        <button 
                          onClick={() => handleLike(story.id)}
                          className="flex items-center gap-3 px-8 py-4 bg-red-50 text-red-600 rounded-2xl font-black hover:bg-red-600 hover:text-white transition-all shadow-sm"
                        >
                          <ThumbsUp className="w-6 h-6" /> {story.likes || 0} লাইক
                        </button>
                        <button className="p-4 bg-gray-50 text-gray-400 rounded-2xl hover:bg-gray-100 transition-all">
                          <Share2 className="w-6 h-6" />
                        </button>
                      </div>
                    </div>
                  </motion.div>
                ))}
              </div>
            ) : (
              <div className="bg-white p-20 rounded-[40px] text-center border-2 border-dashed border-gray-100">
                <MessageSquare className="w-20 h-20 text-gray-200 mx-auto mb-6" />
                <p className="text-gray-400 text-xl font-bold">এখনো কোনো গল্প নেই। প্রথম গল্পটি আপনি শেয়ার করুন!</p>
              </div>
            )}
          </>
        ) : activeTab === 'events' ? (
          <div className="grid gap-12">
            {/* Post Event Button for Admins */}
            {userOrganizations.length > 0 && (
              <div className="bg-red-600 p-8 rounded-[40px] text-white shadow-2xl space-y-4 flex flex-col md:flex-row items-center justify-between gap-6">
                <div>
                  <h3 className="text-2xl font-black italic">নতুন ব্লাড ক্যাম্প আয়োজন করছেন?</h3>
                  <p className="text-red-100 font-bold">আপনার সংগঠনের ইভেন্টটি সবার সামনে প্রচার করুন।</p>
                </div>
                <button 
                  onClick={() => setIsPostingEvent(true)}
                  className="px-10 py-5 bg-white text-red-600 rounded-[28px] font-black text-xl hover:bg-gray-100 transition-all flex items-center gap-2 shadow-xl"
                >
                  <Plus className="w-6 h-6" /> ইভেন্ট যোগ করুন
                </button>
              </div>
            )}

            {/* Event Posting Modal */}
            <AnimatePresence>
              {isPostingEvent && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                  <motion.div 
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    className="bg-white dark:bg-gray-900 w-full max-w-2xl rounded-[40px] p-10 shadow-2xl max-h-[90vh] overflow-y-auto"
                  >
                    <h2 className="text-3xl font-black mb-8 text-gray-900 dark:text-white">নতুন ইভেন্ট/ক্যাম্প</h2>
                    <form onSubmit={handlePostEvent} className="grid grid-cols-1 md:grid-cols-2 gap-6">
                      <div className="md:col-span-2">
                        <label className="block text-xs font-black text-gray-400 uppercase mb-2">সংগঠন</label>
                        <select 
                          value={selectedOrgId}
                          onChange={(e) => setSelectedOrgId(e.target.value)}
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-bold text-gray-700 dark:text-gray-300"
                        >
                          {userOrganizations.map(org => <option key={org.id} value={org.id}>{org.name}</option>)}
                        </select>
                      </div>
                      <div className="md:col-span-2">
                        <input
                          required
                          value={eventTitle}
                          onChange={(e) => setEventTitle(e.target.value)}
                          placeholder="ইভেন্টের শিরোনাম (যেমন: সাভার ব্লাড ক্যাম্প)"
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div>
                        <input
                          required
                          type="date"
                          value={eventDate}
                          onChange={(e) => setEventDate(e.target.value)}
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div>
                        <input
                          required
                          value={eventLocation}
                          onChange={(e) => setEventLocation(e.target.value)}
                          placeholder="অবস্থান (উপজেলা, জেলা)"
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div className="md:col-span-2">
                        <input
                          required
                          value={eventContact}
                          onChange={(e) => setEventContact(e.target.value)}
                          placeholder="যোগাযোগের নম্বর"
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div className="md:col-span-2">
                        <textarea
                          required
                          value={eventDescription}
                          onChange={(e) => setEventDescription(e.target.value)}
                          placeholder="ইভেন্ট সম্পর্কে বিস্তারিত..."
                          className="w-full h-32 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl p-6 focus:ring-2 focus:ring-red-500 outline-none font-medium resize-none text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div className="md:col-span-2">
                        <input
                          type="url"
                          value={imageUrl}
                          onChange={(e) => setImageUrl(e.target.value)}
                          placeholder="ছবির লিংক (ব্যানার)"
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-red-500 outline-none font-medium text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div className="md:col-span-2 flex gap-4">
                        <button
                          type="button"
                          onClick={() => setIsPostingEvent(false)}
                          className="flex-1 py-4 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded-2xl font-black hover:bg-gray-200 transition-all"
                        >
                          বাতিল
                        </button>
                        <button
                          type="submit"
                          className="flex-2 py-4 bg-red-600 text-white rounded-2xl font-black hover:bg-red-700 transition-all shadow-xl shadow-red-500/30"
                        >
                          পাবলিশ করুন
                        </button>
                      </div>
                    </form>
                  </motion.div>
                </div>
              )}
            </AnimatePresence>

            {events.length > 0 ? (
              events.map(event => (
                <motion.div 
                  initial={{ opacity: 0, x: -20 }}
                  animate={{ opacity: 1, x: 0 }}
                  key={event.id} 
                  className="bg-white dark:bg-gray-900 flex flex-col md:flex-row gap-10 p-10 rounded-[48px] border border-gray-100 dark:border-gray-800 shadow-2xl group transition-all"
                >
                  <div className="md:w-1/2 rounded-[32px] overflow-hidden bg-red-50 aspect-video shadow-inner flex items-center justify-center">
                    {event.imageUrl ? (
                      <img src={event.imageUrl} className="w-full h-full object-cover" referrerPolicy="no-referrer" />
                    ) : (
                      <Calendar className="w-24 h-24 text-red-200" />
                    )}
                  </div>
                  <div className="md:w-1/2 space-y-6">
                    <div className="space-y-2">
                       <span className="bg-red-600 text-white text-[10px] font-black px-3 py-1 rounded-full uppercase tracking-widest leading-none">ক্যাম্প</span>
                       <h3 className="text-3xl font-black text-gray-900 dark:text-white group-hover:text-red-600 transition-colors uppercase leading-tight">{event.title}</h3>
                    </div>
                    <div className="space-y-4">
                       <div className="flex items-center gap-4 text-gray-600 dark:text-gray-400 font-bold">
                          <Calendar className="w-6 h-6 text-red-500" /> {event.date}
                       </div>
                       <div className="flex items-center gap-4 text-gray-600 dark:text-gray-400 font-bold">
                          <MapPin className="w-6 h-6 text-red-500" /> {event.location}
                       </div>
                    </div>
                    <p className="text-gray-500 dark:text-gray-400 font-medium leading-relaxed">{event.description}</p>
                    <div className="pt-4 flex gap-4">
                       <a href={`tel:${event.contactNumber}`} className="flex-1 py-4 bg-gray-900 text-white rounded-2xl font-black flex items-center justify-center gap-2 hover:bg-gray-800 transition-all shadow-xl">
                          <Phone className="w-5 h-5" /> কল করুন
                       </a>
                    </div>
                  </div>
                </motion.div>
              ))
            ) : (
              <div className="bg-white p-20 rounded-[40px] text-center border-2 border-dashed border-gray-100">
                <Calendar className="w-20 h-20 text-gray-200 mx-auto mb-6" />
                <p className="text-gray-400 text-xl font-bold">বর্তমানে কোনো ইভেন্ট নেই।</p>
              </div>
            )}
          </div>
        ) : (
          <div className="grid gap-8">
            {/* Alerts Section */}
            {userOrganizations.length > 0 && (
              <div className="bg-orange-600 p-8 rounded-[40px] text-white shadow-2xl space-y-4 flex flex-col md:flex-row items-center justify-between gap-6">
                <div>
                  <h3 className="text-2xl font-black italic">জরুরি রক্তের সংকট?</h3>
                  <p className="text-orange-100 font-bold">ব্রডকাস্টিং মেসেজ পাঠিয়ে ডোনারদের এলার্ট দিন।</p>
                </div>
                <button 
                  onClick={() => setIsPostingAlert(true)}
                  className="px-10 py-5 bg-white text-orange-600 rounded-[28px] font-black text-xl hover:bg-gray-100 transition-all flex items-center gap-2 shadow-xl"
                >
                  <Megaphone className="w-6 h-6" /> এলার্ট পাঠান
                </button>
              </div>
            )}

            <AnimatePresence>
              {isPostingAlert && (
                <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm">
                  <motion.div 
                    initial={{ scale: 0.9, opacity: 0 }}
                    animate={{ scale: 1, opacity: 1 }}
                    exit={{ scale: 0.9, opacity: 0 }}
                    className="bg-white dark:bg-gray-900 w-full max-w-xl rounded-[40px] p-10 shadow-2xl"
                  >
                    <h2 className="text-3xl font-black mb-8 text-gray-900 dark:text-white">জরুরি এলার্ট পাঠান</h2>
                    <form onSubmit={handlePostAlert} className="space-y-6">
                      <div>
                        <label className="block text-xs font-black text-gray-400 uppercase mb-2">সংগঠন</label>
                        <select 
                          value={selectedOrgId}
                          onChange={(e) => setSelectedOrgId(e.target.value)}
                          className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-orange-500 outline-none font-bold text-gray-700 dark:text-gray-300"
                        >
                          {userOrganizations.map(org => <option key={org.id} value={org.id}>{org.name}</option>)}
                        </select>
                      </div>
                      <div className="grid grid-cols-2 gap-4">
                        <div>
                          <label className="block text-xs font-black text-gray-400 uppercase mb-2">রক্তের গ্রুপ</label>
                          <select 
                            value={alertBloodGroup}
                            onChange={(e) => setAlertBloodGroup(e.target.value as BloodGroup)}
                            className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-orange-500 outline-none font-bold text-gray-700 dark:text-gray-300"
                          >
                            {['A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-'].map(bg => <option key={bg} value={bg}>{bg}</option>)}
                          </select>
                        </div>
                        <div>
                          <label className="block text-xs font-black text-gray-400 uppercase mb-2">জেলা</label>
                          <input
                            required
                            value={alertDistrict}
                            onChange={(e) => setAlertDistrict(e.target.value)}
                            className="w-full px-6 py-4 bg-gray-50 dark:bg-gray-800 border-none rounded-2xl focus:ring-2 focus:ring-orange-500 outline-none font-bold text-gray-700 dark:text-gray-300"
                          />
                        </div>
                      </div>
                      <div>
                        <textarea
                          required
                          value={alertMessage}
                          onChange={(e) => setAlertMessage(e.target.value)}
                          placeholder="আপনার বার্তা লিখুন (যেমন: অতি জরুরি A+ রক্ত প্রয়োজন...)"
                          className="w-full h-32 bg-gray-50 dark:bg-gray-800 border-none rounded-3xl p-6 focus:ring-2 focus:ring-orange-500 outline-none font-medium resize-none text-gray-700 dark:text-gray-300"
                        />
                      </div>
                      <div className="flex gap-4">
                        <button
                          type="button"
                          onClick={() => setIsPostingAlert(false)}
                          className="flex-1 py-4 bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 rounded-2xl font-black hover:bg-gray-200 transition-all"
                        >
                          বাতিল
                        </button>
                        <button
                          type="submit"
                          className="flex-2 py-4 bg-orange-600 text-white rounded-2xl font-black hover:bg-orange-700 transition-all shadow-xl shadow-orange-500/30"
                        >
                          এলার্ট দিন
                        </button>
                      </div>
                    </form>
                  </motion.div>
                </div>
              )}
            </AnimatePresence>

            {alerts.length > 0 ? (
              <div className="space-y-6">
                {alerts.map(alert => (
                  <motion.div 
                    layout
                    key={alert.id} 
                    className="bg-white dark:bg-gray-900 p-8 rounded-[40px] border-l-8 border-orange-500 border-y border-r border-gray-100 dark:border-gray-800 shadow-xl space-y-4"
                  >
                    <div className="flex items-center justify-between">
                       <div className="flex items-center gap-3">
                          <div className="w-12 h-12 bg-orange-100 rounded-xl flex items-center justify-center text-orange-600">
                             <Megaphone className="w-6 h-6" />
                          </div>
                          <div>
                             <p className="font-black text-gray-900 dark:text-white uppercase tracking-tight">{alert.orgName}</p>
                             <p className="text-[10px] text-gray-400 font-bold uppercase">{alert.district} • {formatDistanceToNow(new Date(alert.createdAt), { locale: bn })} আগে</p>
                          </div>
                       </div>
                       <div className="bg-orange-50 text-orange-600 px-4 py-2 rounded-xl font-black text-xl">
                          {alert.bloodGroup}
                       </div>
                    </div>
                    <p className="text-gray-700 dark:text-gray-300 text-lg font-bold leading-relaxed">
                       {alert.message}
                    </p>
                  </motion.div>
                ))}
              </div>
            ) : (
              <div className="bg-white p-20 rounded-[40px] text-center border-2 border-dashed border-gray-100">
                <Megaphone className="w-20 h-20 text-gray-200 mx-auto mb-6" />
                <p className="text-gray-400 text-xl font-bold">বর্তমানে কোনো জরুরি বার্তা নেই।</p>
              </div>
            )}
          </div>
        )}
      </div>
    </div>
  </div>
);
}
