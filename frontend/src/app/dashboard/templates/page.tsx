'use client'

import { useState } from 'react'
import { useMutation } from 'react-query'
import { aiApi } from '@/lib/api'
import DashboardLayout from '@/components/layout/DashboardLayout'
import { Button } from '@/components/ui/Button'
import { toast } from '@/components/ui/Toaster'
import {
  FileTemplate,
  Download,
  Copy,
  Sparkles,
} from 'lucide-react'

const templateTypes = [
  {
    id: 'nda',
    name: 'Non-Disclosure Agreement (NDA)',
    description: 'Protect confidential information shared between parties',
    icon: '🤐',
  },
  {
    id: 'service-agreement',
    name: 'Service Agreement',
    description: 'Define terms for service provision between parties',
    icon: '🤝',
  },
  {
    id: 'employment-contract',
    name: 'Employment Contract',
    description: 'Standard employment agreement template',
    icon: '👔',
  },
  {
    id: 'rental-agreement',
    name: 'Rental Agreement',
    description: 'Property rental contract template',
    icon: '🏠',
  },
  {
    id: 'purchase-agreement',
    name: 'Purchase Agreement',
    description: 'Agreement for buying/selling goods or services',
    icon: '💼',
  },
  {
    id: 'partnership-agreement',
    name: 'Partnership Agreement',
    description: 'Business partnership terms and conditions',
    icon: '🤝',
  },
]

export default function TemplatesPage() {
  const [selectedTemplate, setSelectedTemplate] = useState('')
  const [requirements, setRequirements] = useState('')
  const [generatedTemplate, setGeneratedTemplate] = useState('')
  const [showForm, setShowForm] = useState(false)

  const generateMutation = useMutation(
    ({ templateType, requirements }: { templateType: string; requirements: string }) =>
      aiApi.generateTemplate(templateType, requirements),
    {
      onSuccess: (data) => {
        setGeneratedTemplate(data.result)
        toast.success('Template generated successfully!')
      },
      onError: (error: any) => {
        toast.error(error.response?.data?.message || 'Failed to generate template')
      },
    }
  )

  const handleGenerateTemplate = (e: React.FormEvent) => {
    e.preventDefault()
    if (!selectedTemplate || !requirements.trim()) {
      toast.error('Please select a template type and provide requirements')
      return
    }

    generateMutation.mutate({
      templateType: selectedTemplate,
      requirements: requirements.trim(),
    })
  }

  const copyToClipboard = (text: string) => {
    navigator.clipboard.writeText(text)
    toast.success('Template copied to clipboard!')
  }

  const downloadTemplate = (text: string, filename: string) => {
    const element = document.createElement('a')
    const file = new Blob([text], { type: 'text/plain' })
    element.href = URL.createObjectURL(file)
    element.download = `${filename}.txt`
    document.body.appendChild(element)
    element.click()
    document.body.removeChild(element)
    toast.success('Template downloaded!')
  }

  return (
    <DashboardLayout>
      <div className="space-y-6">
        {/* Header */}
        <div>
          <h1 className="text-2xl font-bold leading-7 text-gray-900 sm:text-3xl">
            Legal Templates
          </h1>
          <p className="mt-1 text-sm text-gray-500">
            Generate customized legal document templates using AI
          </p>
        </div>

        {!showForm ? (
          <>
            {/* Template Selection */}
            <div className="bg-white shadow rounded-lg p-6">
              <h2 className="text-lg font-medium text-gray-900 mb-4">
                Choose a Template Type
              </h2>
              
              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {templateTypes.map((template) => (
                  <button
                    key={template.id}
                    onClick={() => {
                      setSelectedTemplate(template.id)
                      setShowForm(true)
                    }}
                    className="text-left p-4 border border-gray-200 rounded-lg hover:border-primary-300 hover:bg-primary-50 transition-colors"
                  >
                    <div className="flex items-start space-x-3">
                      <div className="text-2xl">{template.icon}</div>
                      <div>
                        <h3 className="text-sm font-medium text-gray-900">
                          {template.name}
                        </h3>
                        <p className="mt-1 text-xs text-gray-500">
                          {template.description}
                        </p>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            </div>

            {/* Info Section */}
            <div className="bg-blue-50 border border-blue-200 rounded-lg p-6">
              <div className="flex">
                <div className="flex-shrink-0">
                  <Sparkles className="h-5 w-5 text-blue-400" />
                </div>
                <div className="ml-3">
                  <h3 className="text-sm font-medium text-blue-800">
                    AI-Powered Template Generation
                  </h3>
                  <div className="mt-2 text-sm text-blue-700">
                    <p>
                      Our AI generates customized legal templates based on your specific requirements.
                      Templates include standard clauses and placeholder fields for easy customization.
                    </p>
                    <ul className="list-disc list-inside mt-2 space-y-1">
                      <li>Tailored to your specific needs and jurisdiction</li>
                      <li>Includes standard legal language and clauses</li>
                      <li>Placeholder fields for easy customization</li>
                      <li>Professional formatting and structure</li>
                    </ul>
                  </div>
                </div>
              </div>
            </div>
          </>
        ) : (
          <>
            {/* Template Generation Form */}
            <div className="bg-white shadow rounded-lg p-6">
              <div className="flex items-center justify-between mb-6">
                <div>
                  <h2 className="text-lg font-medium text-gray-900">
                    Generate {templateTypes.find(t => t.id === selectedTemplate)?.name}
                  </h2>
                  <p className="text-sm text-gray-500">
                    Provide details about your requirements to generate a customized template
                  </p>
                </div>
                <Button
                  variant="outline"
                  onClick={() => {
                    setShowForm(false)
                    setSelectedTemplate('')
                    setRequirements('')
                    setGeneratedTemplate('')
                  }}
                >
                  Back to Templates
                </Button>
              </div>

              <form onSubmit={handleGenerateTemplate} className="space-y-4">
                <div>
                  <label className="block text-sm font-medium text-gray-700 mb-2">
                    Template Requirements
                  </label>
                  <textarea
                    value={requirements}
                    onChange={(e) => setRequirements(e.target.value)}
                    rows={6}
                    className="block w-full px-3 py-2 border border-gray-300 rounded-md shadow-sm focus:outline-none focus:ring-primary-500 focus:border-primary-500 sm:text-sm"
                    placeholder="Describe your specific requirements for this template. For example:
- Parties involved (company, individual, etc.)
- Key terms and conditions
- Specific clauses needed
- Duration or timeline
- Payment terms
- Jurisdiction or governing law
- Any special requirements or considerations"
                  />
                  <p className="mt-2 text-sm text-gray-500">
                    Be as specific as possible to get the best results. Include details about parties,
                    terms, conditions, and any special requirements.
                  </p>
                </div>

                <div className="flex justify-end">
                  <Button
                    type="submit"
                    loading={generateMutation.isLoading}
                    disabled={!requirements.trim() || generateMutation.isLoading}
                  >
                    <Sparkles className="h-4 w-4 mr-2" />
                    Generate Template
                  </Button>
                </div>
              </form>
            </div>

            {/* Generated Template */}
            {generatedTemplate && (
              <div className="bg-white shadow rounded-lg p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-lg font-medium text-gray-900">
                    Generated Template
                  </h3>
                  <div className="flex space-x-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => copyToClipboard(generatedTemplate)}
                    >
                      <Copy className="h-4 w-4 mr-1" />
                      Copy
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() =>
                        downloadTemplate(
                          generatedTemplate,
                          templateTypes.find(t => t.id === selectedTemplate)?.name.toLowerCase().replace(/\s+/g, '-') || 'template'
                        )
                      }
                    >
                      <Download className="h-4 w-4 mr-1" />
                      Download
                    </Button>
                  </div>
                </div>

                <div className="bg-gray-50 rounded-lg p-4 max-h-96 overflow-y-auto">
                  <pre className="text-sm text-gray-700 whitespace-pre-wrap font-mono">
                    {generatedTemplate}
                  </pre>
                </div>

                <div className="mt-4 p-4 bg-yellow-50 border border-yellow-200 rounded-lg">
                  <div className="flex">
                    <div className="flex-shrink-0">
                      <svg className="h-5 w-5 text-yellow-400" viewBox="0 0 20 20" fill="currentColor">
                        <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                      </svg>
                    </div>
                    <div className="ml-3">
                      <h3 className="text-sm font-medium text-yellow-800">
                        Legal Disclaimer
                      </h3>
                      <div className="mt-2 text-sm text-yellow-700">
                        <p>
                          This is an AI-generated template for informational purposes only.
                          Please consult with a qualified attorney before using this document
                          for any legal purposes. Laws vary by jurisdiction and individual circumstances.
                        </p>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            )}
          </>
        )}
      </div>
    </DashboardLayout>
  )
}
