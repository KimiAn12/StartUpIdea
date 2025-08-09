'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from 'react-query'
import { useParams } from 'next/navigation'
import { documentsApi, aiApi, type Document, type DocumentAnalysis, type ExtractedClause } from '@/lib/api'
import DashboardLayout from '@/components/layout/DashboardLayout'
import { Button } from '@/components/ui/Button'
import { toast } from '@/components/ui/Toaster'
import {
  FileText,
  Brain,
  MessageCircle,
  Search,
  Download,
  Clock,
  CheckCircle,
  AlertTriangle,
  Send,
  Copy,
  Eye,
  EyeOff,
} from 'lucide-react'
import { formatDate, getImportanceColor } from '@/lib/utils'

export default function DocumentDetailPage() {
  const params = useParams()
  const queryClient = useQueryClient()
  const documentId = parseInt(params.id as string)
  
  const [activeTab, setActiveTab] = useState<'summary' | 'clauses' | 'qa'>('summary')
  const [question, setQuestion] = useState('')
  const [showFullText, setShowFullText] = useState(false)

  // Fetch document
  const { data: document, isLoading: documentLoading } = useQuery(
    ['document', documentId],
    () => documentsApi.getDocument(documentId),
    { enabled: !!documentId }
  )

  // Fetch analyses
  const { data: analyses, isLoading: analysesLoading } = useQuery(
    ['analyses', documentId],
    () => aiApi.getDocumentAnalyses(documentId),
    { enabled: !!documentId }
  )

  // Fetch clauses
  const { data: clauses, isLoading: clausesLoading } = useQuery(
    ['clauses', documentId],
    () => aiApi.getDocumentClauses(documentId),
    { enabled: !!documentId }
  )

  // Mutations
  const summarizeMutation = useMutation(
    () => aiApi.summarizeDocument(documentId),
    {
      onSuccess: () => {
        queryClient.invalidateQueries(['analyses', documentId])
        toast.success('Document summary generated!')
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || 'Failed to generate summary')
      },
    }
  )

  const extractClausesMutation = useMutation(
    () => aiApi.extractClauses(documentId),
    {
      onSuccess: () => {
        queryClient.invalidateQueries(['clauses', documentId])
        toast.success('Clauses extracted successfully!')
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || 'Failed to extract clauses')
      },
    }
  )

  const askQuestionMutation = useMutation(
    (question: string) => aiApi.askQuestion(documentId, question),
    {
      onSuccess: () => {
        queryClient.invalidateQueries(['analyses', documentId])
        setQuestion('')
        toast.success('Question answered!')
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || 'Failed to answer question')
      },
    }
  )

  const handleAskQuestion = (e: React.FormEvent) => {
    e.preventDefault()
    if (question.trim()) {
      askQuestionMutation.mutate(question.trim())
    }
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    toast.success('Copied to clipboard!')
  }

  if (documentLoading) {
    return (
      <DashboardLayout>
        <div className="flex items-center justify-center h-64">
          <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600"></div>
        </div>
      </DashboardLayout>
    )
  }

  if (!document) {
    return (
      <DashboardLayout>
        <div className="text-center py-12">
          <FileText className="h-12 w-12 text-gray-400 mx-auto" />
          <h3 className="mt-2 text-sm font-medium text-gray-900">Document not found</h3>
        </div>
      </DashboardLayout>
    )
  }

  const summary = analyses?.find((a: DocumentAnalysis) => a.analysisType === 'SUMMARY')
  const qaResponses = analyses?.filter((a: DocumentAnalysis) => a.analysisType === 'QUESTION_ANSWER') || []

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="bg-white shadow rounded-lg p-6">
          <div className="flex items-start justify-between">
            <div className="flex items-start space-x-4">
              <div className="text-3xl">
                {document.contentType.includes('pdf') ? '📄' : '📝'}
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">
                  {document.originalName}
                </h1>
                <div className="mt-1 flex items-center space-x-4 text-sm text-gray-500">
                  <span>{formatDate(document.createdAt)}</span>
                  <span>•</span>
                  <span>{Math.round(document.fileSize / 1024)} KB</span>
                  <span>•</span>
                  <div className="flex items-center space-x-1">
                    <CheckCircle className="h-4 w-4 text-green-500" />
                    <span>Processed</span>
                  </div>
                </div>
              </div>
            </div>
            
            <div className="flex space-x-2">
              <Button variant="outline" size="sm">
                <Download className="h-4 w-4 mr-1" />
                Download
              </Button>
            </div>
          </div>
        </div>

        {/* Tabs */}
        <div className="bg-white shadow rounded-lg">
          <div className="border-b border-gray-200">
            <nav className="-mb-px flex space-x-8 px-6">
              {[
                { id: 'summary', name: 'Summary', icon: Brain },
                { id: 'clauses', name: 'Key Clauses', icon: Search },
                { id: 'qa', name: 'Q&A', icon: MessageCircle },
              ].map((tab) => (
                <button
                  key={tab.id}
                  onClick={() => setActiveTab(tab.id as any)}
                  className={`${
                    activeTab === tab.id
                      ? 'border-primary-500 text-primary-600'
                      : 'border-transparent text-gray-500 hover:text-gray-700 hover:border-gray-300'
                  } whitespace-nowrap py-4 px-1 border-b-2 font-medium text-sm flex items-center space-x-2`}
                >
                  <tab.icon className="h-4 w-4" />
                  <span>{tab.name}</span>
                </button>
              ))}
            </nav>
          </div>

          <div className="p-6">
            {/* Summary Tab */}
            {activeTab === 'summary' && (
              <div className="space-y-6">
                {!summary ? (
                  <div className="text-center py-8">
                    <Brain className="h-12 w-12 text-gray-400 mx-auto" />
                    <h3 className="mt-2 text-sm font-medium text-gray-900">
                      No summary generated yet
                    </h3>
                    <p className="mt-1 text-sm text-gray-500">
                      Generate an AI-powered summary of this document
                    </p>
                    <div className="mt-6">
                      <Button
                        onClick={() => summarizeMutation.mutate()}
                        loading={summarizeMutation.isLoading}
                      >
                        <Brain className="h-4 w-4 mr-2" />
                        Generate Summary
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-medium text-gray-900">
                        Document Summary
                      </h3>
                      <div className="flex space-x-2">
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => copyToClipboard(summary.result)}
                        >
                          <Copy className="h-4 w-4 mr-1" />
                          Copy
                        </Button>
                        <Button
                          variant="outline"
                          size="sm"
                          onClick={() => summarizeMutation.mutate()}
                          loading={summarizeMutation.isLoading}
                        >
                          <Brain className="h-4 w-4 mr-1" />
                          Regenerate
                        </Button>
                      </div>
                    </div>
                    
                    <div className="bg-gray-50 rounded-lg p-4">
                      <p className="text-gray-700 whitespace-pre-wrap">
                        {summary.result}
                      </p>
                    </div>
                    
                    <div className="text-xs text-gray-500">
                      Generated on {formatDate(summary.createdAt)}
                    </div>
                  </div>
                )}

                {/* Original Text Preview */}
                <div className="border-t pt-6">
                  <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-medium text-gray-900">
                      Original Text
                    </h3>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setShowFullText(!showFullText)}
                    >
                      {showFullText ? (
                        <>
                          <EyeOff className="h-4 w-4 mr-1" />
                          Hide Full Text
                        </>
                      ) : (
                        <>
                          <Eye className="h-4 w-4 mr-1" />
                          Show Full Text
                        </>
                      )}
                    </Button>
                  </div>
                  
                  <div className="bg-gray-50 rounded-lg p-4 max-h-96 overflow-y-auto">
                    <p className="text-sm text-gray-700 whitespace-pre-wrap">
                      {showFullText
                        ? document.extractedText || 'No text extracted'
                        : (document.extractedText?.substring(0, 500) || 'No text extracted') +
                          (document.extractedText && document.extractedText.length > 500 ? '...' : '')
                      }
                    </p>
                  </div>
                </div>
              </div>
            )}

            {/* Clauses Tab */}
            {activeTab === 'clauses' && (
              <div className="space-y-6">
                {clausesLoading ? (
                  <div className="text-center py-8">
                    <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-primary-600 mx-auto"></div>
                    <p className="mt-2 text-sm text-gray-500">Loading clauses...</p>
                  </div>
                ) : !clauses || clauses.length === 0 ? (
                  <div className="text-center py-8">
                    <Search className="h-12 w-12 text-gray-400 mx-auto" />
                    <h3 className="mt-2 text-sm font-medium text-gray-900">
                      No clauses extracted yet
                    </h3>
                    <p className="mt-1 text-sm text-gray-500">
                      Extract key legal clauses from this document
                    </p>
                    <div className="mt-6">
                      <Button
                        onClick={() => extractClausesMutation.mutate()}
                        loading={extractClausesMutation.isLoading}
                      >
                        <Search className="h-4 w-4 mr-2" />
                        Extract Clauses
                      </Button>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between">
                      <h3 className="text-lg font-medium text-gray-900">
                        Key Legal Clauses ({clauses.length})
                      </h3>
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={() => extractClausesMutation.mutate()}
                        loading={extractClausesMutation.isLoading}
                      >
                        <Search className="h-4 w-4 mr-1" />
                        Re-extract
                      </Button>
                    </div>

                    <div className="space-y-4">
                      {clauses.map((clause: ExtractedClause) => (
                        <div key={clause.id} className="border border-gray-200 rounded-lg p-4">
                          <div className="flex items-start justify-between mb-3">
                            <div className="flex items-center space-x-2">
                              <h4 className="font-medium text-gray-900">
                                {clause.clauseType}
                              </h4>
                              <span
                                className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium border ${getImportanceColor(
                                  clause.importanceLevel
                                )}`}
                              >
                                {clause.importanceLevel}
                              </span>
                            </div>
                            <Button
                              variant="ghost"
                              size="sm"
                              onClick={() => copyToClipboard(clause.clauseText)}
                            >
                              <Copy className="h-4 w-4" />
                            </Button>
                          </div>
                          
                          <div className="space-y-3">
                            <div>
                              <p className="text-sm font-medium text-gray-700 mb-1">
                                Original Text:
                              </p>
                              <p className="text-sm text-gray-600 bg-gray-50 p-3 rounded">
                                {clause.clauseText}
                              </p>
                            </div>
                            
                            {clause.plainEnglishExplanation && (
                              <div>
                                <p className="text-sm font-medium text-gray-700 mb-1">
                                  Plain English:
                                </p>
                                <p className="text-sm text-gray-600">
                                  {clause.plainEnglishExplanation}
                                </p>
                              </div>
                            )}
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
              </div>
            )}

            {/* Q&A Tab */}
            {activeTab === 'qa' && (
              <div className="space-y-6">
                {/* Ask Question Form */}
                <form onSubmit={handleAskQuestion} className="space-y-4">
                  <div>
                    <label className="block text-sm font-medium text-gray-700 mb-2">
                      Ask a question about this document
                    </label>
                    <div className="flex space-x-2">
                      <input
                        type="text"
                        value={question}
                        onChange={(e) => setQuestion(e.target.value)}
                        placeholder="e.g., What are the payment terms?"
                        className="flex-1 block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
                      />
                      <Button
                        type="submit"
                        loading={askQuestionMutation.isLoading}
                        disabled={!question.trim() || askQuestionMutation.isLoading}
                      >
                        <Send className="h-4 w-4 mr-1" />
                        Ask
                      </Button>
                    </div>
                  </div>
                </form>

                {/* Q&A History */}
                {qaResponses.length > 0 && (
                  <div className="space-y-4">
                    <h3 className="text-lg font-medium text-gray-900">
                      Previous Questions & Answers
                    </h3>
                    
                    <div className="space-y-4">
                      {qaResponses.map((qa: DocumentAnalysis) => (
                        <div key={qa.id} className="border border-gray-200 rounded-lg p-4">
                          <div className="space-y-3">
                            <div>
                              <p className="text-sm font-medium text-gray-700 mb-1">
                                Question:
                              </p>
                              <p className="text-sm text-gray-900 bg-blue-50 p-3 rounded">
                                {qa.prompt}
                              </p>
                            </div>
                            
                            <div>
                              <div className="flex items-center justify-between mb-1">
                                <p className="text-sm font-medium text-gray-700">
                                  Answer:
                                </p>
                                <Button
                                  variant="ghost"
                                  size="sm"
                                  onClick={() => copyToClipboard(qa.result)}
                                >
                                  <Copy className="h-4 w-4" />
                                </Button>
                              </div>
                              <p className="text-sm text-gray-600 whitespace-pre-wrap">
                                {qa.result}
                              </p>
                            </div>
                            
                            <div className="text-xs text-gray-500">
                              {formatDate(qa.createdAt)}
                            </div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}

                {qaResponses.length === 0 && (
                  <div className="text-center py-8">
                    <MessageCircle className="h-12 w-12 text-gray-400 mx-auto" />
                    <h3 className="mt-2 text-sm font-medium text-gray-900">
                      No questions asked yet
                    </h3>
                    <p className="mt-1 text-sm text-gray-500">
                      Ask questions about this document to get AI-powered answers
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>
        </div>
      </div>
    </DashboardLayout>
  )
}
