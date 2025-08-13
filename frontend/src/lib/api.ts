import axios from 'axios'
import Cookies from 'js-cookie'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

// Create axios instance
export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  // Remove default Content-Type to avoid conflicts with file uploads
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = Cookies.get('authToken')
    console.log('=== API Request Debug ===')
    console.log('Request URL:', config.url)
    console.log('Request method:', config.method)
    console.log('Request data type:', config.data ? (config.data instanceof FormData ? 'FormData' : typeof config.data) : 'No data')
    console.log('Token present:', !!token)
    console.log('Token preview:', token ? token.substring(0, 20) + '...' : 'NO TOKEN')
    
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
      console.log('Authorization header set:', `Bearer ${token.substring(0, 20)}...`)
      
      // Log the full token for debugging (remove in production)
      console.log('Full token:', token)
    } else {
      console.log('No token found in cookies')
    }
    
    // IMPORTANT: Don't set Content-Type for FormData (file uploads)
    // Let the browser set it automatically with the correct boundary
    if (!config.headers['Content-Type'] && config.data && !(config.data instanceof FormData)) {
      config.headers['Content-Type'] = 'application/json'
    }
    
    // For FormData, explicitly remove Content-Type to let browser handle it
    if (config.data instanceof FormData) {
      delete config.headers['Content-Type']
      console.log('FormData detected - Content-Type removed for proper file upload')
    }
    
    console.log('Final headers:', config.headers)
    console.log('=== End API Request Debug ===')
    return config
  },
  (error) => {
    console.error('Request interceptor error:', error)
    return Promise.reject(error)
  }
)

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => {
    console.log('=== API Response Success ===')
    console.log('Response status:', response.status)
    console.log('Response URL:', response.config.url)
    return response
  },
  (error) => {
    console.log('=== API Response Error ===')
    console.log('Error status:', error.response?.status)
    console.log('Error URL:', error.config?.url)
    console.log('Error message:', error.response?.data?.message || error.message)
    console.log('Error response data:', error.response?.data)
    console.log('Error config:', error.config)
    
    // Only handle 401 errors that are actual authentication failures
    if (error.response?.status === 401) {
      console.log('401 Unauthorized Error Details:')
      console.log('- Response headers:', error.response.headers)
      console.log('- Request headers:', error.config?.headers)
      console.log('- Request data:', error.config?.data)
      
      if (error.response?.data?.message?.includes('JWT')) {
        console.log('JWT token expired or invalid, redirecting to login')
        Cookies.remove('authToken')
        window.location.href = '/auth/login'
      } else {
        console.log('401 error but not JWT-related - may be permission issue')
      }
    }
    
    console.log('=== End API Response Error ===')
    return Promise.reject(error)
  }
)

// Auth API
export const authApi = {
  login: async (credentials: { usernameOrEmail: string; password: string }) => {
    const response = await api.post('/api/auth/signin', credentials)
    return response.data
  },

  register: async (userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
  }) => {
    const response = await api.post('/api/auth/signup', userData)
    return response.data
  },

  getCurrentUser: async () => {
    const response = await api.get('/api/auth/me')
    return response.data
  },
}

// Documents API
export const documentsApi = {
  uploadDocument: async (file: File) => {
    console.log('=== Starting Document Upload ===')
    console.log('File name:', file.name)
    console.log('File size:', file.size)
    console.log('File type:', file.type)
    
    const formData = new FormData()
    formData.append('file', file)
    
    // Check if token exists before upload
    const token = Cookies.get('authToken')
    if (!token) {
      console.error('No auth token found for upload')
      throw new Error('Authentication required')
    }
    
    console.log('Token found, proceeding with upload')
    
    // Explicitly set headers for file upload
    const config = {
      headers: {
        'Content-Type': undefined, // Let browser set this for FormData
        'Authorization': `Bearer ${token}`
      }
    }
    
    console.log('Upload config:', config)
    console.log('FormData created:', formData.has('file'))
    
    try {
      const response = await api.post('/api/documents/upload', formData, config)
      console.log('Upload successful:', response.data)
      return response.data
    } catch (error) {
      console.error('Upload failed:', error)
      console.error('Error response:', error.response?.data)
      console.error('Error status:', error.response?.status)
      throw error
    }
  },

  getDocuments: async (page = 0, size = 10, search?: string) => {
    const params = new URLSearchParams({
      page: page.toString(),
      size: size.toString(),
    })
    if (search) params.append('search', search)
    
    const response = await api.get(`/api/documents?${params}`)
    return response.data
  },

  getDocument: async (id: number) => {
    const response = await api.get(`/api/documents/${id}`)
    return response.data
  },

  deleteDocument: async (id: number) => {
    const response = await api.delete(`/api/documents/${id}`)
    return response.data
  },

  // AI Analysis Methods
  summarizeDocument: async (documentId: number) => {
    const response = await api.post(`/api/ai/documents/${documentId}/summarize`)
    return response.data
  },

  extractClauses: async (documentId: number) => {
    const response = await api.post(`/api/ai/documents/${documentId}/extract-clauses`)
    return response.data
  },

  askQuestion: async (documentId: number, question: string) => {
    const response = await api.post(`/api/ai/documents/${documentId}/question`, { question })
    return response.data
  },

  getDocumentAnalyses: async (documentId: number) => {
    const response = await api.get(`/api/ai/documents/${documentId}/analyses`)
    return response.data
  },

  getDocumentClauses: async (documentId: number) => {
    const response = await api.get(`/api/ai/documents/${documentId}/clauses`)
    return response.data
  },
}

// AI API
export const aiApi = {
  generateTemplate: async (templateType: string, requirements: string) => {
    const response = await api.post('/api/ai/templates/generate', {
      templateType,
      requirements,
    })
    return response.data
  },
}

// Types
export interface User {
  id: number
  username: string
  email: string
  firstName?: string
  lastName?: string
  role: string
  createdAt: string
}

export interface Document {
  id: number
  fileName: string
  originalName: string
  fileSize: number
  contentType: string
  processingStatus: 'PENDING' | 'PROCESSING' | 'COMPLETED' | 'FAILED'
  processingError?: string
  createdAt: string
  updatedAt: string
  hasExtractedText: boolean
}

export interface DocumentAnalysis {
  id: number
  analysisType: string
  result: string
  prompt?: string
  confidenceScore?: number
  status: string
  errorMessage?: string
  createdAt: string
}

export interface ExtractedClause {
  id: number
  clauseType: string
  clauseText: string
  plainEnglishExplanation?: string
  importanceLevel: 'LOW' | 'MEDIUM' | 'HIGH' | 'CRITICAL'
  confidenceScore?: number
  createdAt: string
}

export interface ApiError {
  message: string
  status?: number
}
