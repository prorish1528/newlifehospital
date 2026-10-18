export type Role = 'ADMIN' | 'STAFF';
export type IdType = 'AADHAAR' | 'PASSPORT' | 'VOTER_ID' | 'DRIVING_LICENSE' | 'OTHER';
export type Gender = 'MALE' | 'FEMALE' | 'OTHER';
export type PaymentStatus = 'PARTIAL' | 'FULL';

export interface SessionUser {
  username: string;
  role: Role;
  fullName: string;
  expiresAt?: string;
}

export interface FieldRule {
  formName: string;
  fieldName: string;
  fieldLabel: string;
  dataType: 'STRING' | 'NUMBER' | 'DECIMAL' | 'DATE' | 'EMAIL' | 'PHONE';
  required: boolean;
  regexPattern?: string;
  minLength?: number;
  maxLength?: number;
  errorMessage: string;
}

/** Rules for one form, keyed by field name. */
export type FormRules = Record<string, FieldRule>;

export interface Problem {
  status: number;
  detail: string;
  errors?: Record<string, string>;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface Doctor {
  id: number;
  name: string;
  specialization: string;
  qualification?: string;
  experienceYears?: number;
  photoUrl?: string;
  active: boolean;
}

export interface HospitalService {
  id: number;
  serviceName: string;
  category: string;
  cost: number;
  description?: string;
  active: boolean;
}

export interface PatientSummary {
  id: number;
  patientCode: string;
  name: string;
  age: number;
  gender: Gender;
  contactNumber: string;
  idType: IdType;
  maskedIdNumber: string;
}

export interface PatientDetail extends PatientSummary {
  idTypeOther?: string;
  dob: string;
  address?: string;
  createdBy: string;
  createdAt: string;
  updatedAt: string;
}

export interface InvoiceItem {
  serviceId: number;
  serviceName: string;
  unitPrice: number;
  quantity: number;
  amount: number;
}

export interface InvoiceSummary {
  id: number;
  invoiceNumber: string;
  invoiceDate: string;
  visitAt: string;
  patientCode: string;
  patientName: string;
  doctorName?: string;
  staffName: string;
  netAmount: number;
  paidAmount: number;
  balanceAmount: number;
  paymentStatus: PaymentStatus;
  createdAt: string;
}

export interface InvoiceDetail {
  id: number;
  invoiceNumber: string;
  invoiceDate: string;
  visitAt: string;
  patient: PatientSummary;
  doctorId?: number;
  doctorName?: string;
  doctorSpecialization?: string;
  staffUsername: string;
  staffName: string;
  items: InvoiceItem[];
  totalAmount: number;
  discount: number;
  netAmount: number;
  paidAmount: number;
  balanceAmount: number;
  paymentStatus: PaymentStatus;
  createdAt: string;
  updatedAt: string;
}

export interface StaffAccount {
  id: number;
  username: string;
  fullName: string;
  email?: string;
  phoneNumber?: string;
  enabled: boolean;
  createdAt: string;
}

export interface CmsEntry {
  sectionKey: string;
  label: string;
  contentText: string;
  updatedBy?: string;
  updatedAt: string;
}

export interface Totals {
  invoiceCount: number;
  billed: number;
  collected: number;
  outstanding: number;
}

export interface RevenuePoint {
  label: string;
  invoiceCount: number;
  billed: number;
  collected: number;
}

export interface Dashboard {
  today: Totals;
  thisMonth: Totals;
  thisYear: Totals;
  daily: RevenuePoint[];
  monthly: RevenuePoint[];
  yearly: RevenuePoint[];
}

export const ID_TYPE_LABELS: Record<IdType, string> = {
  AADHAAR: 'Aadhaar',
  PASSPORT: 'Passport',
  VOTER_ID: 'Voter ID',
  DRIVING_LICENSE: 'Driving License',
  OTHER: 'Other',
};

export const GENDER_LABELS: Record<Gender, string> = { MALE: 'Male', FEMALE: 'Female', OTHER: 'Other' };
