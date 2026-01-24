export enum UserRole {
  BENEFICIARY = 'BENEFICIARY',
  FIELD_WORKER = 'FIELD_WORKER',
  NGO_STAFF = 'NGO_STAFF',
  ADMIN = 'ADMIN'
}

export enum NeedStatus {
  NEW = 'NEW',
  ASSIGNED = 'ASSIGNED',
  IN_PROGRESS = 'IN_PROGRESS',
  RESOLVED = 'RESOLVED',
  REJECTED = 'REJECTED'
}

export enum NeedCategory {
  FOOD = 'FOOD',
  SHELTER = 'SHELTER',
  LEGAL = 'LEGAL',
  MEDICAL = 'MEDICAL',
  DOCUMENTS = 'DOCUMENTS',
  OTHER = 'OTHER'
}

export enum UrgencyLevel {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  CRITICAL = 'CRITICAL'
}

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  organizationId?: string;
  isActive: boolean;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  token: string;
  type: string;
  userId: string;
  email: string;
  name: string;
  role: string;
  organizationId?: string;
}

export interface RedactedNeedResponse {
  id: string;
  status: NeedStatus;
  category: NeedCategory;
  country: string;
  regionOrState?: string;
  urgencyLevel: UrgencyLevel;
  generalizedVulnerabilityFlags?: string;
  createdAt: string;
}

export interface FullNeedResponse extends RedactedNeedResponse {
  createdByUserId: string;
  organizationId?: string;
  description: string;
  city?: string;
  locationText?: string;
  locationLat?: number;
  locationLng?: number;
  vulnerabilityFlags?: string;
  assignedOrganizationId?: string;
  assignedAt?: string;
  resolvedAt?: string;
  updatedAt: string;
  beneficiaryName?: string;
  beneficiaryPhone?: string;
  beneficiaryEmail?: string;
  sensitiveNotes?: string;
}

export interface CreateNeedRequest {
  category: NeedCategory;
  description: string;
  country: string;
  regionOrState?: string;
  city?: string;
  locationText?: string;
  urgencyLevel: UrgencyLevel;
  vulnerabilityFlags?: string;
  beneficiaryName?: string;
  beneficiaryPhone?: string;
  beneficiaryEmail?: string;
  sensitiveNotes?: string;
}

export interface UpdateNeedRequest {
  status: NeedStatus;
  comment?: string;
}

// Type guard to check if response is FullNeedResponse
export function isFullNeedResponse(response: RedactedNeedResponse | FullNeedResponse): response is FullNeedResponse {
  return 'description' in response && 'createdByUserId' in response;
}

// Helper to check if user can claim needs
export function canClaimNeeds(role: string): boolean {
  return role === UserRole.NGO_STAFF || role === UserRole.ADMIN;
}

// Helper to check if user can update needs
export function canUpdateNeeds(role: string): boolean {
  return role === UserRole.NGO_STAFF || role === UserRole.ADMIN;
}

// Helper to check if user is admin
export function isAdmin(role: string): boolean {
  return role === UserRole.ADMIN;
}

// ==================== Admin Types ====================

export interface AdminStats {
  // User statistics
  totalUsers: number;
  activeUsers: number;
  usersByRole: Record<string, number>;

  // Organization statistics
  totalOrganizations: number;
  verifiedOrganizations: number;
  pendingOrganizations: number;

  // Need statistics
  totalNeeds: number;
  needsByStatus: Record<string, number>;
  needsByCategory: Record<string, number>;
  needsByUrgency: Record<string, number>;

  // Activity statistics
  recentLogins24h: number;
  recentNeedsCreated24h: number;
  recentNeedsClaimed24h: number;

  // Security statistics
  suspiciousActivities30d: number;
  rateLimitViolations24h: number;
  failedLoginAttempts24h: number;
}

export interface AuditLogEntry {
  id: string;
  userId: string;
  userEmail: string;
  userName: string;
  actionType: string;
  entityType: string;
  entityId: string;
  details: string;
  ipAddress: string;
  createdAt: string;
}

export interface SuspiciousActivity {
  id: string;
  userId: string;
  userEmail: string;
  userName: string;
  userRole: string;
  activityType: string;
  description: string;
  activityCount: number;
  detectedAt: string;
  severity: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL';
}

export enum OrganizationStatus {
  PENDING = 'PENDING',
  VERIFIED = 'VERIFIED',
  REJECTED = 'REJECTED'
}

export interface Organization {
  id: string;
  name: string;
  email: string;
  phone: string;
  country: string;
  status: OrganizationStatus;
  verificationNotes?: string;
  createdAt: string;
}
