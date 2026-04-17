import { collection, addDoc, serverTimestamp } from 'firebase/firestore';
import { db } from './firebase';
import { Notification } from '../types';

export async function sendNotification(notification: Omit<Notification, 'id' | 'createdAt'>) {
  try {
    await addDoc(collection(db, 'notifications'), {
      ...notification,
      createdAt: new Date().toISOString()
    });
  } catch (error) {
    console.error("Error sending notification:", error);
  }
}
