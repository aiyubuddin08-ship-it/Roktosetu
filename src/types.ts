export type BloodGroup = 'A+' | 'A-' | 'B+' | 'B-' | 'AB+' | 'AB-' | 'O+' | 'O-';
export type Urgency = 'Low' | 'Normal' | 'High' | 'Critical';
export type RequestStatus = 'Active' | 'Fulfilled' | 'Cancelled';

export interface LocationData {
  division: string;
  district: string;
  upazila: string;
  address?: string;
}

export interface UserProfile {
  uid: string;
  displayName: string;
  email: string;
  photoURL?: string;
  bloodGroup: BloodGroup;
  location: LocationData;
  isDonor: boolean;
  phoneNumber?: string;
  lastDonated?: string;
  createdAt: string;
  donationsCount: number;
  points: number;
  ratingAverage: number;
  ratingCount: number;
  role: 'user' | 'admin';
}

export interface BloodRequest {
  id: string;
  requesterId: string;
  requesterName: string;
  bloodGroup: BloodGroup;
  location: LocationData;
  hospitalName: string;
  urgency: Urgency;
  requiredDate: string;
  message: string;
  contactNumber: string;
  status: RequestStatus;
  createdAt: string;
  patientName?: string;
  condition?: string;
  donorId?: string;
  rating?: number;
  completedAt?: string;
}

export interface Notification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: 'info' | 'success' | 'warning' | 'error';
  isRead: boolean;
  createdAt: string;
  requestId?: string;
  link?: string;
}

export enum OperationType {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LIST = 'list',
  GET = 'get',
  WRITE = 'write',
}

export interface FirestoreErrorInfo {
  error: string;
  operationType: OperationType;
  path: string | null;
  authInfo: any;
}
