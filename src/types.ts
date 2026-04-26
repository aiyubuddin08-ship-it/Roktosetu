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
  organizationId?: string;
  organizationName?: string;
  badges: string[];
  referralCode?: string;
  referredBy?: string;
  privacySettings?: {
    hidePhoneNumber: boolean;
    hideLocation: boolean;
  };
}

export interface Organization {
  id: string;
  name: string;
  email: string;
  phoneNumber: string;
  location: LocationData;
  description: string;
  logo?: string;
  type: 'Club' | 'NGO' | 'Government' | 'Private' | 'Volunteer Group';
  memberCount: number;
  createdAt: string;
  isVerified: boolean;
  adminUid: string;
  coverageArea?: string;
  address?: string;
}

export interface DonationRecord {
  id: string;
  userId: string;
  date: string;
  location: string;
  hospitalName?: string;
  bloodGroup: BloodGroup;
  notes?: string;
  pointsEarned: number;
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
  internalNotes?: string;
}

export interface Notification {
  id: string;
  userId: string;
  title: string;
  message: string;
  type: 'Eligibility' | 'New Request' | 'Badge Earned' | 'System';
  isRead: boolean;
  createdAt: string;
  requestId?: string;
  link?: string;
  linkText?: string;
}

export interface EmergencyResource {
  id: string;
  name: string;
  type: 'Blood Bank' | 'Ambulance';
  location: string;
  contactNumber: string;
  available24h: boolean;
  createdAt: string;
}

export interface HealthTip {
  id: string;
  title: string;
  content: string;
  imageUrl?: string;
  category: 'Pre-Donation' | 'Post-Donation' | 'General Health';
  createdAt: string;
}

export interface SuccessStory {
  id: string;
  authorId: string;
  authorName: string;
  authorPhoto?: string;
  content: string;
  imageUrl?: string;
  likes: number;
  likedByCount: number;
  createdAt: string;
  location?: string;
}

export interface HealthRecord {
  id: string;
  userId: string;
  date: string;
  bloodPressure: string;
  hemoglobin: number;
  weight: number;
  pulse?: number;
  createdAt: string;
}

export interface DonationCertificate {
  id: string;
  userId: string;
  donorName: string;
  bloodGroup: BloodGroup;
  donationDate: string;
  donationCount: number;
  issuedAt: string;
  certificateNo: string;
}

export interface DonationEvent {
  id: string;
  orgId: string;
  orgName: string;
  orgAdminUid?: string; // Admin who can manage this event
  title: string;
  description: string;
  date: string;
  location: string;
  contactNumber: string;
  imageUrl?: string;
  createdAt: string;
}

export interface BloodStockAlert {
  id: string;
  orgId: string;
  orgName: string;
  bloodGroup: BloodGroup;
  district: string;
  message: string;
  createdAt: string;
  expiresAt: string;
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
