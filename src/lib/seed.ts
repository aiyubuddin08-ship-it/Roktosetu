import { collection, doc, writeBatch, getDocs, query, where } from 'firebase/firestore';
import { db } from './firebase';
import { BloodRequest, UserProfile } from '../types';

export const seedSampleData = async () => {
  const batch = writeBatch(db);

  // Sample Blood Requests
  const requests = [
    {
      requesterId: 'system-seed-1',
      requesterName: 'Abul Hasan',
      patientName: 'Abul Hasan',
      bloodGroup: 'A+',
      location: { division: 'Dhaka', district: 'Dhaka', upazila: 'Savar', address: 'ঢাকা মেডিকেল কলেজ' },
      hospitalName: 'ঢাকা মেডিকেল কলেজ',
      urgency: 'Critical',
      condition: 'জরুরি অস্ত্রোপচার',
      requiredDate: '2025-01-18',
      message: 'Urgent surgery patient',
      contactNumber: '01711-111222',
      status: 'Active',
      createdAt: new Date().toISOString()
    },
    {
      requesterId: 'system-seed-2',
      requesterName: 'Rima Begum',
      patientName: 'Rima Begum',
      bloodGroup: 'O-',
      location: { division: 'Chattogram', district: 'Chattogram', upazila: 'Boalkhali', address: 'চট্টগ্রাম জেনারেল হাসপাতাল' },
      hospitalName: 'চট্টগ্রাম জেনারেল হাসপাতাল',
      urgency: 'High',
      condition: 'প্রসব জটিলতা',
      requiredDate: '2025-01-18',
      message: 'Delivery complications',
      contactNumber: '01812-222333',
      status: 'Active',
      createdAt: new Date().toISOString()
    }
  ];

  // Sample Donors
  const donors = [
    {
      uid: 'donor-seed-1',
      displayName: 'রহিম আহমেদ',
      email: 'rahim@example.com',
      bloodGroup: 'A+',
      location: { division: 'Dhaka', district: 'Dhaka', upazila: 'Savar', address: 'ঢাকা' },
      isDonor: true,
      phoneNumber: '01711-234567',
      lastDonated: '2024-11-10T00:00:00Z',
      createdAt: new Date().toISOString()
    },
    {
      uid: 'donor-seed-2',
      displayName: 'সুমাইয়া বেগম',
      email: 'sumaiya@example.com',
      bloodGroup: 'O+',
      location: { division: 'Sylhet', district: 'Sylhet', upazila: 'Balaganj', address: 'সিলেট' },
      isDonor: true,
      phoneNumber: '01913-456789',
      lastDonated: '2024-12-05T00:00:00Z',
      createdAt: new Date().toISOString()
    }
  ];

  // Logic to prevent duplicate seeds
  const existingRequests = await getDocs(query(collection(db, 'requests'), where('requesterId', '==', 'system-seed-1')));
  if (existingRequests.empty) {
    requests.forEach(req => {
      const newRef = doc(collection(db, 'requests'));
      batch.set(newRef, req);
    });
  }

  const existingDonors = await getDocs(query(collection(db, 'users'), where('uid', '==', 'donor-seed-1')));
  if (existingDonors.empty) {
    donors.forEach(donor => {
      batch.set(doc(db, 'users', donor.uid), donor);
    });
  }

  await batch.commit();
};
