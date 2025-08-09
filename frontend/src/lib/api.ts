import axios from 'axios'
import Cookies from 'js-cookie'

const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'http://localhost:8080'

// Create axios instance
export const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// Request interceptor to add auth token
api.interceptors.request.use(
  (config) => {
    const token = Cookies.get('authToken')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => {
    return Promise.reject(error)
  }
)

// Response interceptor to handle auth errors
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      Cookies.remove('authToken')
      window.location.href = '/auth/login'
    }
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
    const formData = new FormData()
    formData.append('file', file)
    
    const response = await api.post('/api/documents/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
    return response.data
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
}

// AI API
export const aiApi = {
  summarizeDocument: async (documentId: number) => {
    const response = await api.post(`/api/ai/documents/${documentId}/summarize`)
    return response.data
  },

  extractClauses: async (documentId: number) => {
    const response = await api.post(`/api/ai/documents/${documentId}/extract-clauses`)
    return response.data
  },

  askQuestion: async (documentId: number, question: string) => {
    const response = await api.post(`/api/ai/documents/${documentId}/question`, {
      question,
    })
    return response.data
  },

  generateTemplate: async (templateType: string, requirements: string) => {
    const response = await api.post('/api/ai/templates/generate', {
      templateType,
      requirements,
    })
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
