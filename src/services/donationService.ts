import { collection, query, getDocs, addDoc, doc, updateDoc, increment, orderBy, where } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { DonationRecord } from '../types';

export const donationService = {
  async getUserDonations(userId: string) {
    const q = query(
      collection(db, 'users', userId, 'donations'),
      orderBy('date', 'desc')
    );
    const snapshot = await getDocs(q);
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as DonationRecord));
  },

  async addDonation(userId: string, data: Omit<DonationRecord, 'id' | 'userId' | 'pointsEarned'>) {
    const pointsEarned = 100;
    const donationRef = await addDoc(collection(db, 'users', userId, 'donations'), {
      ...data,
      userId,
      pointsEarned,
    });

    // Update user stats
    const userRef = doc(db, 'users', userId);
    await updateDoc(userRef, {
      donationsCount: increment(1),
      points: increment(pointsEarned),
      lastDonated: data.date
    });

    return donationRef.id;
  }
};
