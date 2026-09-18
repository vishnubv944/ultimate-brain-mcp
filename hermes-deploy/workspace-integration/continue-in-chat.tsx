/**
 * ContinueInChatButton — surfaces a "Continue this in chat" affordance on the
 * voice screen. When clicked, it POSTs the full transcript to /api/chat/from-voice
 * (a server route that creates a new chat conversation seeded with the transcript)
 * and navigates to /chat?conversation=<id>.
 *
 * Why: voice turns are short and ephemeral. The user often wants to switch to
 * text to dig deeper, edit a long response, or share the conversation. This
 * button bridges that gap without copy-paste.
 */
import { useState } from 'react'
import { useNavigate } from '@tanstack/react-router'
import { Button } from '@/components/ui/button'
import { ArrowRight01Icon, Comment01Icon } from '@hugeicons/core-free-icons'
import { HugeiconsIcon } from '@hugeicons/react'

export interface TranscriptTurn {
  role: 'user' | 'assistant'
  text: string
  at: number
}

interface ContinueInChatButtonProps {
  turns: TranscriptTurn[]
  disabled?: boolean
}

export function ContinueInChatButton({ turns, disabled }: ContinueInChatButtonProps) {
  const navigate = useNavigate()
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  const handleClick = async () => {
    if (turns.length === 0 || busy) return
    setBusy(true)
    setError(null)
    try {
      const res = await fetch('/api/chat/from-voice', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ turns }),
      })
      if (!res.ok) {
        const body = (await res.json().catch(() => ({}))) as { error?: string }
        throw new Error(body.error ?? `HTTP ${res.status}`)
      }
      const { conversationId } = (await res.json()) as { conversationId: string }
      navigate({ to: '/chat', search: { conversation: conversationId } })
    } catch (err) {
      setError(err instanceof Error ? err.message : 'failed')
      setBusy(false)
    }
  }

  return (
    <div className="flex flex-col gap-1">
      <Button
        variant="outline"
        size="sm"
        onClick={handleClick}
        disabled={disabled || turns.length === 0 || busy}
      >
        <HugeiconsIcon icon={Comment01Icon} size={16} className="mr-1.5" />
        Continue in chat
        <HugeiconsIcon icon={ArrowRight01Icon} size={14} className="ml-1.5 opacity-60" />
      </Button>
      {error ? <p className="text-xs text-red-500">Failed: {error}</p> : null}
    </div>
  )
}
