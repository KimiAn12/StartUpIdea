'use client'

import { useState } from 'react'
import { useQuery } from 'react-query'
import { documentsApi, type Document } from '@/lib/api'
import DashboardLayout from '@/components/layout/DashboardLayout'
import { Button } from '@/components/ui/Button'
import { toast } from '@/components/ui/Toaster'
import Link from 'next/link'
import {
  FileText,
  Upload,
  Search,
  MoreVertical,
  Eye,
  Trash2,
  Download,
  Clock,
  CheckCircle,
  XCircle,
  AlertCircle,
} from 'lucide-react'
import { formatFileSize, formatDate, getFileIcon } from '@/lib/utils'

export default function DashboardPage() {
  const [searchTerm, setSearchTerm] = useState('')
  const [page, setPage] = useState(0)

  const {
    data: documentsData,
    isLoading,
    error,
    refetch,
  } = useQuery(
    ['documents', page, searchTerm],
    () => documentsApi.getDocuments(page, 10, searchTerm),
    {
      keepPreviousData: true,
      onError: (error: any) => {
        console.error('Dashboard documents error:', error)
        console.error('Error response:', error.response?.data)
        console.error('Error status:', error.response?.status)
        console.error('Error message:', error.message)
        
        // Check if JWT token exists in cookies (where the API stores it)
        const token = document.cookie.split('; ').find(row => row.startsWith('authToken='))?.split('=')[1]
        console.log('JWT token exists in cookies:', !!token)
        if (token) {
          console.log('Token length:', token.length)
          console.log('Token starts with:', token.substring(0, 20) + '...')
        }
        
        // Log request details
        console.log('Request config:', error.config)
        console.log('Request headers:', error.config?.headers)
      }
    }
  )

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    setPage(0)
    refetch()
  }

  const handleDeleteDocument = async (id: number) => {
    if (!confirm('Are you sure you want to delete this document?')) {
      return
    }

    try {
      await documentsApi.deleteDocument(id)
      toast.success('Document deleted successfully')
      refetch()
    } catch (error: any) {
      toast.error(error.response?.data?.message || 'Failed to delete document')
    }
  }

  const getStatusIcon = (status: Document['processingStatus']) => {
    switch (status) {
      case 'PENDING':
        return <Clock className="h-4 w-4 text-yellow-500" />
      case 'PROCESSING':
        return <AlertCircle className="h-4 w-4 text-blue-500 animate-pulse" />
      case 'COMPLETED':
        return <CheckCircle className="h-4 w-4 text-green-500" />
      case 'FAILED':
        return <XCircle className="h-4 w-4 text-red-500" />
      default:
        return <Clock className="h-4 w-4 text-gray-500" />
    }
  }

  const getStatusText = (status: Document['processingStatus']) => {
    switch (status) {
      case 'PENDING':
        return 'Pending'
      case 'PROCESSING':
        return 'Processing'
      case 'COMPLETED':
        return 'Ready'
      case 'FAILED':
        return 'Failed'
      default:
        return 'Unknown'
    }
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="md:flex md:items-center md:justify-between">
          <div className="flex-1 min-w-0">
            <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl sm:truncate">
              My Documents
            </h1>
            <p className="mt-1 text-sm text-gray-500">
              Upload and analyze your legal documents with AI
            </p>
          </div>
          <div className="mt-4 flex md:mt-0 md:ml-4">
            <Link href="/dashboard/upload">
              <Button className="inline-flex items-center">
                <Upload className="h-4 w-4 mr-2" />
                Upload Document
              </Button>
            </Link>
          </div>
        </div>

        {/* Search */}
        <div className="bg-white shadow rounded-lg p-6">
          <form onSubmit={handleSearch} className="flex space-x-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <input
                  type="text"
                  placeholder="Search documents..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  className="block w-full pl-10 pr-3 py-2 border border-gray-300 rounded-md leading-5 bg-white placeholder-gray-500 focus:outline-none focus:placeholder-gray-400 focus:ring-1 focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
                />
              </div>
            </div>
            <Button type="submit" variant="outline">
              Search
            </Button>
          </form>
        </div>

        {/* Documents List */}
        <div className="bg-white shadow rounded-lg">
          {isLoading ? (
            <div className="p-6 text-center">
              <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
              <p className="mt-2 text-sm text-gray-500">Loading documents...</p>
            </div>
          ) : error ? (
            <div className="p-6 text-center">
              <XCircle className="h-12 w-12 text-red-400 mx-auto" />
              <p className="mt-2 text-sm text-red-600">Failed to load documents</p>
              <Button onClick={() => refetch()} className="mt-4" variant="outline">
                Try Again
              </Button>
            </div>
          ) : documentsData?.content?.length === 0 ? (
            <div className="p-6 text-center">
              <FileText className="h-12 w-12 text-gray-400 mx-auto" />
              <h3 className="mt-2 text-sm font-medium text-gray-900">No documents</h3>
              <p className="mt-1 text-sm text-gray-500">
                Get started by uploading your first legal document.
              </p>
              <div className="mt-6">
                <Link href="/dashboard/upload">
                  <Button>
                    <Upload className="h-4 w-4 mr-2" />
                    Upload Document
                  </Button>
                </Link>
              </div>
            </div>
          ) : (
            <div className="overflow-hidden">
              <ul className="divide-y divide-gray-200">
                {documentsData?.content?.map((document: Document) => (
                  <li key={document.id} className="px-6 py-4 hover:bg-gray-50">
                    <div className="flex items-center justify-between">
                      <div className="flex items-center space-x-4 flex-1">
                        <div className="text-2xl">
                          {getFileIcon(document.contentType)}
                        </div>
                        <div className="flex-1 min-w-0">
                          <div className="flex items-center space-x-2">
                            <h3 className="text-sm font-medium text-gray-900 truncate">
                              {document.originalName}
                            </h3>
                            <div className="flex items-center space-x-1">
                              {getStatusIcon(document.processingStatus)}
                              <span className="text-xs text-gray-500">
                                {getStatusText(document.processingStatus)}
                              </span>
                            </div>
                          </div>
                          <div className="flex items-center space-x-4 mt-1">
                            <p className="text-sm text-gray-500">
                              {formatFileSize(document.fileSize)}
                            </p>
                            <p className="text-sm text-gray-500">
                              {formatDate(document.createdAt)}
                            </p>
                          </div>
                          {document.processingError && (
                            <p className="text-sm text-red-600 mt-1">
                              Error: {document.processingError}
                            </p>
                          )}
                        </div>
                      </div>
                      
                      <div className="flex items-center space-x-2">
                        {document.processingStatus === 'COMPLETED' && (
                          <Link href={`/dashboard/documents/${document.id}`}>
                            <Button variant="outline" size="sm">
                              <Eye className="h-4 w-4 mr-1" />
                              View
                            </Button>
                          </Link>
                        )}
                        
                        <div className="relative">
                          <Button
                            variant="ghost"
                            size="sm"
                            onClick={() => handleDeleteDocument(document.id)}
                          >
                            <Trash2 className="h-4 w-4 text-red-500" />
                          </Button>
                        </div>
                      </div>
                    </div>
                  </li>
                ))}
              </ul>
              
              {/* Pagination */}
              {documentsData && documentsData.totalPages > 1 && (
                <div className="px-6 py-3 border-t border-gray-200 flex items-center justify-between">
                  <div className="text-sm text-gray-700">
                    Showing page {documentsData.number + 1} of {documentsData.totalPages}
                  </div>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage(page - 1)}
                      disabled={page === 0}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage(page + 1)}
                      disabled={page >= documentsData.totalPages - 1}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </DashboardLayout>
  )
}
