import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, catchError, of, shareReplay } from 'rxjs';
import {
  CmsEntry,
  Dashboard,
  Doctor,
  HospitalService,
  InvoiceDetail,
  InvoiceSummary,
  Page,
  PatientDetail,
  PatientSummary,
  PaymentStatus,
  StaffAccount,
} from '../models';

function params(values: Record<string, string | number | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(values)) {
    if (v !== null && v !== undefined && v !== '') p = p.set(k, String(v));
  }
  return p;
}

export interface InvoiceFilter {
  q?: string;
  from?: string;
  to?: string;
  status?: PaymentStatus | '';
  patientId?: number;
  page?: number;
  size?: number;
}

/** Public website data. */
@Injectable({ providedIn: 'root' })
export class PublicApi {
  private readonly http = inject(HttpClient);
  private cms$?: Observable<Record<string, string>>;

  /** CMS text blocks; resolves to an empty map on failure so page chrome still renders. */
  cms(): Observable<Record<string, string>> {
    return (this.cms$ ??= this.http.get<Record<string, string>>('/api/public/cms').pipe(
      catchError(() => {
        this.cms$ = undefined; // retry on next use
        return of({} as Record<string, string>);
      }),
      shareReplay(1),
    ));
  }

  doctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>('/api/public/doctors');
  }

  services(): Observable<HospitalService[]> {
    return this.http.get<HospitalService[]>('/api/public/services');
  }
}

/** Staff portal: patients and billing. */
@Injectable({ providedIn: 'root' })
export class StaffApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/staff';

  registerPatient(body: Record<string, unknown>): Observable<PatientDetail> {
    return this.http.post<PatientDetail>(`${this.base}/patients`, body);
  }

  suggest(q: string): Observable<PatientSummary[]> {
    return this.http.get<PatientSummary[]>(`${this.base}/patients/suggest`, { params: { q } });
  }

  checkId(idType: string, idNumber: string, idTypeOther?: string) {
    return this.http.post<{ exists: boolean; patient?: PatientSummary }>(`${this.base}/patients/check-id`, {
      idType,
      idNumber,
      idTypeOther,
    });
  }

  searchPatients(q: string, page = 0, size = 20): Observable<Page<PatientSummary>> {
    return this.http.get<Page<PatientSummary>>(`${this.base}/patients`, { params: params({ q, page, size }) });
  }

  patient(id: number): Observable<PatientDetail> {
    return this.http.get<PatientDetail>(`${this.base}/patients/${id}`);
  }

  updateContact(id: number, contactNumber: string, address: string | null): Observable<PatientDetail> {
    return this.http.patch<PatientDetail>(`${this.base}/patients/${id}`, { contactNumber, address });
  }

  createInvoice(body: Record<string, unknown>): Observable<InvoiceDetail> {
    return this.http.post<InvoiceDetail>(`${this.base}/invoices`, body);
  }

  invoices(filter: InvoiceFilter): Observable<Page<InvoiceSummary>> {
    return this.http.get<Page<InvoiceSummary>>(`${this.base}/invoices`, { params: params({ ...filter }) });
  }

  invoice(id: number): Observable<InvoiceDetail> {
    return this.http.get<InvoiceDetail>(`${this.base}/invoices/${id}`);
  }

  recordPayment(id: number, amount: number): Observable<InvoiceDetail> {
    return this.http.post<InvoiceDetail>(`${this.base}/invoices/${id}/payments`, { amount });
  }
}

/** Admin portal. */
@Injectable({ providedIn: 'root' })
export class AdminApi {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/admin';

  staff(): Observable<StaffAccount[]> {
    return this.http.get<StaffAccount[]>(`${this.base}/staff`);
  }

  createStaff(body: Record<string, unknown>): Observable<StaffAccount> {
    return this.http.post<StaffAccount>(`${this.base}/staff`, body);
  }

  updateStaff(id: number, body: Record<string, unknown>): Observable<StaffAccount> {
    return this.http.put<StaffAccount>(`${this.base}/staff/${id}`, body);
  }

  deleteStaff(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/staff/${id}`);
  }

  resetPassword(id: number, newPassword: string): Observable<void> {
    return this.http.post<void>(`${this.base}/staff/${id}/reset-password`, { newPassword });
  }

  resetUsername(id: number, newUsername: string): Observable<StaffAccount> {
    return this.http.post<StaffAccount>(`${this.base}/staff/${id}/reset-username`, { newUsername });
  }

  services(): Observable<HospitalService[]> {
    return this.http.get<HospitalService[]>(`${this.base}/services`);
  }

  saveService(id: number | null, body: Record<string, unknown>): Observable<HospitalService> {
    return id == null
      ? this.http.post<HospitalService>(`${this.base}/services`, body)
      : this.http.put<HospitalService>(`${this.base}/services/${id}`, body);
  }

  doctors(): Observable<Doctor[]> {
    return this.http.get<Doctor[]>(`${this.base}/doctors`);
  }

  saveDoctor(id: number | null, body: Record<string, unknown>): Observable<Doctor> {
    return id == null
      ? this.http.post<Doctor>(`${this.base}/doctors`, body)
      : this.http.put<Doctor>(`${this.base}/doctors/${id}`, body);
  }

  uploadDoctorPhoto(id: number, file: File): Observable<Doctor> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<Doctor>(`${this.base}/doctors/${id}/photo`, form);
  }

  cms(): Observable<CmsEntry[]> {
    return this.http.get<CmsEntry[]>(`${this.base}/cms`);
  }

  updateCms(key: string, contentText: string): Observable<CmsEntry> {
    return this.http.put<CmsEntry>(`${this.base}/cms/${encodeURIComponent(key)}`, { contentText });
  }

  invoices(filter: InvoiceFilter): Observable<Page<InvoiceSummary>> {
    return this.http.get<Page<InvoiceSummary>>(`${this.base}/invoices`, { params: params({ ...filter }) });
  }

  invoice(id: number): Observable<InvoiceDetail> {
    return this.http.get<InvoiceDetail>(`${this.base}/invoices/${id}`);
  }

  dashboard(): Observable<Dashboard> {
    return this.http.get<Dashboard>(`${this.base}/dashboard`);
  }
}
