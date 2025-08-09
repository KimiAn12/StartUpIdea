'use client'

import { useState, useCallback } from 'react'
import { useRouter } from 'next/navigation'
import { documentsApi } from '@/lib/api'
import DashboardLayout from '@/components/layout/DashboardLayout'
import { Button } from '@/components/ui/Button'
import { toast } from '@/components/ui/Toaster'
import {
  Upload,
  FileText,
  X,
  CheckCircle,
  AlertCircle,
} from 'lucide-react'
import { formatFileSize } from '@/lib/utils'

export default function UploadPage() {
  const router = useRouter()
  const [dragActive, setDragActive] = useState(false)
  const [files, setFiles] = useState<File[]>([])
  const [uploading, setUploading] = useState(false)
  const [uploadProgress, setUploadProgress] = useState<{ [key: string]: number }>({})

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true)
    } else if (e.type === 'dragleave') {
      setDragActive(false)
    }
  }, [])

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault()
    e.stopPropagation()
    setDragActive(false)

    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      const droppedFiles = Array.from(e.dataTransfer.files)
      addFiles(droppedFiles)
    }
  }, [])

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files) {
      const selectedFiles = Array.from(e.target.files)
      addFiles(selectedFiles)
    }
  }

  const addFiles = (newFiles: File[]) => {
    const validFiles = newFiles.filter(file => {
      const isValidType = file.type === 'application/pdf' ||
        file.type === 'application/msword' ||
        file.type === 'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      
      const isValidSize = file.size <= 50 * 1024 * 1024 // 50MB

      if (!isValidType) {
        toast.error(`${file.name}: Only PDF and Word documents are supported`)
        return false
      }

      if (!isValidSize) {
        toast.error(`${file.name}: File size must be less than 50MB`)
        return false
      }

      return true
    })

    setFiles(prev => [...prev, ...validFiles])
  }

  const removeFile = (index: number) => {
    setFiles(prev => prev.filter((_, i) => i !== index))
  }

  const uploadFiles = async () => {
    if (files.length === 0) {
      toast.error('Please select files to upload')
      return
    }

    setUploading(true)
    const results = []

    for (let i = 0; i < files.length; i++) {
      const file = files[i]
      try {
        setUploadProgress(prev => ({ ...prev, [file.name]: 0 }))
        
        const result = await documentsApi.uploadDocument(file)
        
        setUploadProgress(prev => ({ ...prev, [file.name]: 100 }))
        results.push({ file: file.name, success: true, result })
        
        toast.success(`${file.name} uploaded successfully`)
      } catch (error: any) {
        setUploadProgress(prev => ({ ...prev, [file.name]: -1 }))
        results.push({ 
          file: file.name, 
          success: false, 
          error: error.response?.data?.message || 'Upload failed' 
        })
        
        toast.error(`Failed to upload ${file.name}: ${error.response?.data?.message || 'Upload failed'}`)
      }
    }

    setUploading(false)
    
    // If any uploads were successful, redirect to dashboard after a short delay
    if (results.some(r => r.success)) {
      setTimeout(() => {
        router.push('/dashboard')
      }, 2000)
    }
  }

  const getFileIcon = (file: File) => {
    if (file.type.includes('pdf')) return '📄'
    if (file.type.includes('word') || file.type.includes('document')) return '📝'
    return '📄'
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl">
            Upload Documents
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Upload PDF or Word documents for AI-powered analysis
          </p>
        </div>

        {/* Upload Area */}
        <div className="bg-white shadow rounded-lg p-6">
          <div
            className={`relative border-2 border-dashed rounded-lg p-12 text-center hover:border-gray-400 focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-primary-500 transition-colors ${
              dragActive ? 'border-primary-500 bg-primary-50' : 'border-gray-300'
            }`}
            onDragEnter={handleDrag}
            onDragLeave={handleDrag}
            onDragOver={handleDrag}
            onDrop={handleDrop}
          >
            <input
              type="file"
              multiple
              accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
              onChange={handleFileSelect}
              className="absolute inset-0 w-full h-full opacity-0 cursor-pointer"
              disabled={uploading}
            />
            
            <Upload className="mx-auto h-12 w-12 text-gray-400" />
            <h3 className="mt-2 text-sm font-medium text-gray-900">
              {dragActive ? 'Drop files here' : 'Upload documents'}
            </h3>
            <p className="mt-1 text-sm text-gray-500">
              Drag and drop files here, or click to select files
            </p>
            <p className="mt-1 text-xs text-gray-400">
              Supports PDF and Word documents up to 50MB
            </p>
          </div>
        </div>

        {/* File List */}
        {files.length > 0 && (
          <div className="bg-white shadow rounded-lg p-6">
            <h3 className="text-lg font-medium text-gray-900 mb-4">
              Selected Files ({files.length})
            </h3>
            
            <div className="space-y-3">
              {files.map((file, index) => {
                const progress = uploadProgress[file.name]
                const isUploading = uploading && progress !== undefined && progress >= 0
                const isCompleted = progress === 100
                const isFailed = progress === -1

                return (
                  <div
                    key={`${file.name}-${index}`}
                    className="flex items-center justify-between p-3 border border-gray-200 rounded-lg"
                  >
                    <div className="flex items-center space-x-3 flex-1">
                      <div className="text-2xl">{getFileIcon(file)}</div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium text-gray-900 truncate">
                          {file.name}
                        </p>
                        <p className="text-sm text-gray-500">
                          {formatFileSize(file.size)}
                        </p>
                        
                        {/* Progress bar */}
                        {isUploading && (
                          <div className="mt-2">
                            <div className="bg-gray-200 rounded-full h-2">
                              <div
                                className="bg-primary-600 h-2 rounded-full transition-all duration-300"
                                style={{ width: `${progress}%` }}
                              />
                            </div>
                          </div>
                        )}
                      </div>
                      
                      <div className="flex items-center">
                        {isCompleted && (
                          <CheckCircle className="h-5 w-5 text-green-500" />
                        )}
                        {isFailed && (
                          <AlertCircle className="h-5 w-5 text-red-500" />
                        )}
                        {!uploading && (
                          <button
                            onClick={() => removeFile(index)}
                            className="text-gray-400 hover:text-gray-600"
                          >
                            <X className="h-5 w-5" />
                          </button>
                        )}
                      </div>
                    </div>
                  </div>
                )
              })}
            </div>

            {/* Upload Button */}
            <div className="mt-6 flex justify-end space-x-3">
              <Button
                variant="outline"
                onClick={() => setFiles([])}
                disabled={uploading}
              >
                Clear All
              </Button>
              <Button
                onClick={uploadFiles}
                loading={uploading}
                disabled={uploading || files.length === 0}
              >
                {uploading ? 'Uploading...' : `Upload ${files.length} file${files.length !== 1 ? 's' : ''}`}
              </Button>
            </div>
          </div>
        )}

        {/* Guidelines */}
        <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
          <h3 className="text-sm font-medium text-blue-900 mb-2">
            Upload Guidelines
          </h3>
          <ul className="text-sm text-blue-800 space-y-1">
            <li>• Supported formats: PDF (.pdf), Word (.doc, .docx)</li>
            <li>• Maximum file size: 50MB per file</li>
            <li>• Documents will be processed automatically after upload</li>
            <li>• Processing time varies based on document size and complexity</li>
            <li>• All uploaded documents are encrypted and stored securely</li>
          </ul>
        </div>
      </div>
    </DashboardLayout>
  )
}
