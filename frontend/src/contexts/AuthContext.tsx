'use client'

import React, { createContext, useContext, useState, useEffect } from 'react'
import { authApi, type User } from '@/lib/api'
import Cookies from 'js-cookie'
import { useRouter } from 'next/navigation'

interface AuthContextType {
  user: User | null
  isLoading: boolean
  login: (credentials: { usernameOrEmail: string; password: string }) => Promise<void>
  register: (userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
  }) => Promise<void>
  logout: () => void
}

const AuthContext = createContext<AuthContextType | undefined>(undefined)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)
  const router = useRouter()

  useEffect(() => {
    const initAuth = async () => {
      const token = Cookies.get('authToken')
      if (token) {
        try {
          const userData = await authApi.getCurrentUser()
          setUser(userData)
        } catch (error) {
          console.error('Failed to get current user:', error)
          Cookies.remove('authToken')
        }
      }
      setIsLoading(false)
    }

    initAuth()
  }, [])

  const login = async (credentials: { usernameOrEmail: string; password: string }) => {
    try {
      setIsLoading(true)
      const response = await authApi.login(credentials)
      
      // Store token in cookie
      Cookies.set('authToken', response.accessToken, { 
        expires: 7, // 7 days
        secure: process.env.NODE_ENV === 'production',
        sameSite: 'strict'
      })

      // Set user data
      const userData: User = {
        id: response.id,
        username: response.username,
        email: response.email,
        role: response.role,
        createdAt: new Date().toISOString(), // Will be updated when we fetch user profile
      }
      
      setUser(userData)
      router.push('/dashboard')
    } catch (error: any) {
      console.error('Login failed:', error)
      throw new Error(error.response?.data?.message || 'Login failed')
    } finally {
      setIsLoading(false)
    }
  }

  const register = async (userData: {
    username: string
    email: string
    password: string
    firstName?: string
    lastName?: string
  }) => {
    try {
      setIsLoading(true)
      await authApi.register(userData)
      
      // Auto-login after registration
      await login({
        usernameOrEmail: userData.username,
        password: userData.password,
      })
    } catch (error: any) {
      console.error('Registration failed:', error)
      throw new Error(error.response?.data?.message || 'Registration failed')
    } finally {
      setIsLoading(false)
    }
  }

  const logout = () => {
    Cookies.remove('authToken')
    setUser(null)
    router.push('/auth/login')
  }

  return (
    <AuthContext.Provider value={{
      user,
      isLoading,
      login,
      register,
      logout,
    }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const context = useContext(AuthContext)
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider')
  }
  return context
}
