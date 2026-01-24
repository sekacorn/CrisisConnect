import axios, { AxiosInstance, AxiosError } from 'axios';
import { LoginRequest, LoginResponse, CreateNeedRequest, UpdateNeedRequest } from '../types';

const API_BASE_URL = process.env.REACT_APP_API_URL || 'http://localhost:8080/api';

interface ErrorResponse {
  message?: string;
  error?: string;
  status?: number;
}

/**
 * API Client with enhanced error handling
 *
 * Features:
 * - Automatic JWT token attachment
 * - Rate limiting error handling
 * - 401 auto-logout
 * - No caching for sensitive endpoints
 */
class ApiClient {
  private client: AxiosInstance;

  constructor() {
    this.client = axios.create({
      baseURL: API_BASE_URL,
      headers: {
        'Content-Type': 'application/json',
        // No caching for security
        'Cache-Control': 'no-store, no-cache, must-revalidate',
        'Pragma': 'no-cache',
      },
    });

    // Request interceptor to add JWT token
    this.client.interceptors.request.use(
      (config) => {
        const token = localStorage.getItem('token');
        if (token) {
          config.headers.Authorization = `Bearer ${token}`;
        }
        return config;
      },
      (error) => Promise.reject(error)
    );

    // Response interceptor for enhanced error handling
    this.client.interceptors.response.use(
      (response) => response,
      (error: AxiosError<ErrorResponse>) => {
        // Handle 401 Unauthorized - logout
        if (error.response?.status === 401) {
          localStorage.removeItem('token');
          window.location.href = '/login';
        }

        // Handle 429 Too Many Requests - rate limiting
        if (error.response?.status === 429) {
          const message = error.response.data?.message || 'Too many requests. Please try again later.';
          error.message = message;
        }

        // Handle 404 - could be authorization denial (to prevent enumeration)
        if (error.response?.status === 404) {
          error.message = error.response.data?.message || 'Resource not found or access denied';
        }

        return Promise.reject(error);
      }
    );
  }

  // ==================== Auth Endpoints ====================

  /**
   * Login with rate limiting support
   * Rate limit: 5 failed attempts per 15 minutes
   *
   * @throws AxiosError with 429 status if rate limited
   * @throws AxiosError with 401 status if invalid credentials
   */
  async login(request: LoginRequest): Promise<LoginResponse> {
    const response = await this.client.post<LoginResponse>('/auth/login', request);
    return response.data;
  }

  /**
   * Get current authenticated user
   */
  async getCurrentUser() {
    const response = await this.client.get('/auth/me');
    return response.data;
  }

  // ==================== Need Endpoints ====================

  /**
   * Create a new need
   * Requires: FIELD_WORKER, NGO_STAFF, or ADMIN role
   */
  async createNeed(request: CreateNeedRequest) {
    const response = await this.client.post('/needs', request);
    return response.data;
  }

  /**
   * Get all needs (always returns redacted list)
   * Returns: Array of RedactedNeedResponse
   */
  async getAllNeeds() {
    const response = await this.client.get('/needs');
    return response.data;
  }

  /**
   * Get need by ID (redacted or full based on authorization)
   * Rate limit: 20 views per hour for non-admins
   *
   * Returns:
   * - RedactedNeedResponse if not authorized
   * - FullNeedResponse if authorized (creator, assigned NGO, admin)
   *
   * @throws 404 if need doesn't exist OR user doesn't have access
   * @throws 429 if rate limit exceeded
   */
  async getNeedById(id: string): Promise<any> {
    const response = await this.client.get(`/needs/${id}`);
    return response.data;
  }

  /**
   * Update need status
   * Requires: NGO_STAFF (from assigned org) or ADMIN
   */
  async updateNeed(id: string, request: UpdateNeedRequest) {
    const response = await this.client.patch(`/needs/${id}`, request);
    return response.data;
  }

  /**
   * Claim a need (assign to user's organization)
   * Requires: NGO_STAFF (from verified org in service area) or ADMIN
   *
   * Checks:
   * - Organization must be VERIFIED
   * - Need must be in organization's service area
   * - Need must be unclaimed (PENDING/NEW status)
   *
   * @throws 403 if not authorized or outside service area
   * @throws 404 if need doesn't exist
   */
  async claimNeed(id: string): Promise<void> {
    await this.client.post(`/needs/${id}/claim`);
  }

  // ==================== Organization Endpoints ====================

  /**
   * Get all organizations
   * Requires: NGO_STAFF or ADMIN role
   */
  async getAllOrganizations() {
    const response = await this.client.get('/organizations');
    return response.data;
  }

  // ==================== Admin Endpoints ====================

  /**
   * Get admin dashboard statistics
   * Requires: ADMIN role
   */
  async getAdminStats() {
    const response = await this.client.get('/admin/stats');
    return response.data;
  }

  /**
   * Get all organizations with pagination (admin)
   * Requires: ADMIN role
   */
  async getAdminOrganizations(page: number = 0, size: number = 20) {
    const response = await this.client.get('/admin/organizations', {
      params: { page, size }
    });
    return response.data;
  }

  /**
   * Get organizations by status (admin)
   * Requires: ADMIN role
   */
  async getOrganizationsByStatus(status: string) {
    const response = await this.client.get(`/admin/organizations/status/${status}`);
    return response.data;
  }

  /**
   * Update organization status (admin)
   * Requires: ADMIN role
   */
  async updateOrganization(id: string, status: string, verificationNotes?: string) {
    const response = await this.client.patch(`/admin/organizations/${id}`, {
      status,
      verificationNotes
    });
    return response.data;
  }

  /**
   * Get all users with pagination (admin)
   * Requires: ADMIN role
   */
  async getAdminUsers(page: number = 0, size: number = 20) {
    const response = await this.client.get('/admin/users', {
      params: { page, size }
    });
    return response.data;
  }

  /**
   * Update user (admin)
   * Requires: ADMIN role
   */
  async updateUser(id: string, updates: { name?: string; role?: string; isActive?: boolean }) {
    const response = await this.client.patch(`/admin/users/${id}`, updates);
    return response.data;
  }

  /**
   * Get user audit logs (admin)
   * Requires: ADMIN role
   */
  async getUserAuditLogs(userId: string, days: number = 30) {
    const response = await this.client.get(`/admin/users/${userId}/audit-logs`, {
      params: { days }
    });
    return response.data;
  }

  /**
   * Get audit logs with pagination (admin)
   * Requires: ADMIN role
   */
  async getAuditLogs(page: number = 0, size: number = 50, actionType?: string) {
    const response = await this.client.get('/admin/audit-logs', {
      params: { page, size, actionType }
    });
    return response.data;
  }

  /**
   * Get suspicious activities (admin)
   * Requires: ADMIN role
   */
  async getSuspiciousActivities(days: number = 30) {
    const response = await this.client.get('/admin/suspicious-activities', {
      params: { days }
    });
    return response.data;
  }
}

export const apiClient = new ApiClient();
