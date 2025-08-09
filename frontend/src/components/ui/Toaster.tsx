'use client'

import { useState, useEffect } from 'react'
import { X, CheckCircle, AlertCircle, Info, AlertTriangle } from 'lucide-react'

interface Toast {
  id: string
  type: 'success' | 'error' | 'warning' | 'info'
  message: string
  duration?: number
}

let toastCounter = 0
const toasts: Toast[] = []
const listeners: ((toasts: Toast[]) => void)[] = []

const notify = (toast: Omit<Toast, 'id'>) => {
  const id = (++toastCounter).toString()
  const newToast: Toast = { ...toast, id }
  toasts.push(newToast)
  
  listeners.forEach(listener => listener([...toasts]))
  
  // Auto remove after duration
  setTimeout(() => {
    removeToast(id)
  }, toast.duration || 5000)
}

const removeToast = (id: string) => {
  const index = toasts.findIndex(toast => toast.id === id)
  if (index > -1) {
    toasts.splice(index, 1)
    listeners.forEach(listener => listener([...toasts]))
  }
}

export const toast = {
  success: (message: string, duration?: number) => notify({ type: 'success', message, duration }),
  error: (message: string, duration?: number) => notify({ type: 'error', message, duration }),
  warning: (message: string, duration?: number) => notify({ type: 'warning', message, duration }),
  info: (message: string, duration?: number) => notify({ type: 'info', message, duration }),
}

export function Toaster() {
  const [toastList, setToastList] = useState<Toast[]>([])

  useEffect(() => {
    listeners.push(setToastList)
    return () => {
      const index = listeners.indexOf(setToastList)
      if (index > -1) {
        listeners.splice(index, 1)
      }
    }
  }, [])

  const getIcon = (type: Toast['type']) => {
    switch (type) {
      case 'success':
        return <CheckCircle className="h-5 w-5" />
      case 'error':
        return <AlertCircle className="h-5 w-5" />
      case 'warning':
        return <AlertTriangle className="h-5 w-5" />
      case 'info':
        return <Info className="h-5 w-5" />
    }
  }

  const getStyles = (type: Toast['type']) => {
    switch (type) {
      case 'success':
        return 'bg-green-50 text-green-800 border-green-200'
      case 'error':
        return 'bg-red-50 text-red-800 border-red-200'
      case 'warning':
        return 'bg-yellow-50 text-yellow-800 border-yellow-200'
      case 'info':
        return 'bg-blue-50 text-blue-800 border-blue-200'
    }
  }

  return (
    <div className="fixed top-4 right-4 z-50 space-y-2">
      {toastList.map((toast) => (
        <div
          key={toast.id}
          className={`max-w-sm w-full border rounded-lg shadow-lg p-4 flex items-start space-x-3 ${getStyles(toast.type)}`}
        >
          {getIcon(toast.type)}
          <div className="flex-1 text-sm font-medium">
            {toast.message}
          </div>
          <button
            onClick={() => removeToast(toast.id)}
            className="flex-shrink-0 ml-2"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      ))}
    </div>
  )
}
