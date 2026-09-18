import { Suspense, lazy } from 'react'
import { createFileRoute } from '@tanstack/react-router'
import { usePageTitle } from '@/hooks/use-page-title'

const VoiceScreen = lazy(async () => {
  const module = await import('@/screens/voice/voice-screen')
  return { default: module.VoiceScreen }
})

export const Route = createFileRoute('/voice')({
  ssr: false,
  component: function VoiceRoute() {
    usePageTitle('Voice')
    return (
      <Suspense fallback={<div className="p-8 text-sm text-neutral-500">Loading voice mode…</div>}>
        <VoiceScreen />
      </Suspense>
    )
  },
})
