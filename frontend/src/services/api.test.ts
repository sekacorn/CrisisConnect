import axios from 'axios';
import { NeedCategory, UrgencyLevel } from '../types';

jest.mock('axios', () => ({
  __esModule: true,
  default: {
    create: jest.fn(() => ({
      interceptors: {
        request: { use: jest.fn(), eject: jest.fn() },
        response: { use: jest.fn(), eject: jest.fn() },
      },
      get: jest.fn(),
      post: jest.fn(),
      put: jest.fn(),
      patch: jest.fn(),
      delete: jest.fn(),
    })),
  },
}));

// Import after mocking
import { apiClient } from './api';

const mockedAxios = axios as jest.Mocked<typeof axios>;
const mockAxiosInstance = (axios.create as jest.Mock).mock.results[0].value;

describe('ApiClient', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });

  describe('login', () => {
    it('should login successfully', async () => {
      const mockResponse = {
        data: {
          token: 'test-token',
          userId: 'test-user-id',
          email: 'test@example.com',
          name: 'Test User',
          role: 'ADMIN',
        },
      };

      mockAxiosInstance.post.mockResolvedValue(mockResponse);

      const result = await apiClient.login({
        email: 'test@example.com',
        password: 'password',
      });

      expect(result).toEqual(mockResponse.data);
    });
  });

  describe('createNeed', () => {
    it('should create need successfully', async () => {
      const mockRequest = {
        category: NeedCategory.FOOD,
        description: 'Test need',
        country: 'TestCountry',
        urgencyLevel: UrgencyLevel.HIGH,
      };

      const mockResponse = {
        data: {
          id: 'test-need-id',
          ...mockRequest,
        },
      };

      mockAxiosInstance.post.mockResolvedValue(mockResponse);

      const result = await apiClient.createNeed(mockRequest);

      expect(result).toEqual(mockResponse.data);
    });
  });
});
