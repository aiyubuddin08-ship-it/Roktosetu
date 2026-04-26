import { collection, query, getDocs, addDoc, updateDoc, doc, serverTimestamp, where, orderBy, limit, setDoc } from 'firebase/firestore';
import { db } from '../lib/firebase';
import { Organization } from '../types';

export const organizationService = {
  async register(orgData: Omit<Organization, 'id' | 'createdAt' | 'isVerified'>) {
    const docRef = doc(collection(db, 'organizations'));
    await setDoc(docRef, {
      ...orgData,
      id: docRef.id,
      createdAt: new Date().toISOString(),
      isVerified: false,
    });
    return docRef.id;
  },

  async search(searchTerm: string) {
    const q = query(
      collection(db, 'organizations'),
      orderBy('name'),
      limit(10)
    );
    // Simple client-side filtering for better experience since Firestore starts-with is tricky with case sensitivity
    const snapshot = await getDocs(q);
    return snapshot.docs
      .map(doc => ({ id: doc.id, ...doc.data() } as Organization))
      .filter(org => org.name.toLowerCase().includes(searchTerm.toLowerCase()));
  },

  async getAll() {
    const snapshot = await getDocs(collection(db, 'organizations'));
    return snapshot.docs.map(doc => ({ id: doc.id, ...doc.data() } as Organization));
  },

  async getById(id: string) {
    const snapshot = await getDocs(query(collection(db, 'organizations'), where('id', '==', id)));
    if (snapshot.empty) return null;
    return { id: snapshot.docs[0].id, ...snapshot.docs[0].data() } as Organization;
  }
};
