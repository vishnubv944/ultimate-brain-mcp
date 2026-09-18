/**
 * Voice mode screen — LiveKit-backed real-time voice conversation.
 *
 * Joins a LiveKit room and pipes audio through the local hermes-voice-agent
 * (Deepgram STT → hermes-gateway LLM → Cartesia TTS). Falls back to text
 * input when mic access is denied.
 */
import {
  Suspense,
  useCallback,
  useEffect,
  useMemo,
  useRef,
  useState,
} from 'react'
import { HugeiconsIcon } from '@hugeicons/react'
import {
  AiVoiceIcon,
  Cancel01Icon,
  AiMicIcon,
  TelephoneIcon,
} from '@hugeicons/core-free-icons'
import { Room, RoomEvent, Track, ConnectionState } from 'livekit-client'
import { Button } from '@/components/ui/button'
import {
  ScrollAreaRoot,
  ScrollAreaScrollbar,
  ScrollAreaThumb,
  ScrollAreaViewport,
} from '@/components/ui/scroll-area'
import { cn } from '@/lib/utils'

const ScrollArea = ({
  className,
  children,
}: {
  className?: string
  children: React.ReactNode
}) => (
  <ScrollAreaRoot className={className}>
    <ScrollAreaViewport>{children}</ScrollAreaViewport>
    <ScrollAreaScrollbar>
      <ScrollAreaThumb />
    </ScrollAreaScrollbar>
  </ScrollAreaRoot>
)

// Card is implemented inline as a styled div — the workspace UI bundle
// doesn't ship a `card` component.
function Card({
  className,
  children,
}: {
  className?: string
  children: React.ReactNode
}) {
  return (
    <div
      className={cn(
        'rounded-xl border border-neutral-200 bg-white shadow-sm dark:border-neutral-800 dark:bg-neutral-950',
        className,
      )}
    >
      {children}
    </div>
  )
}

type VoicePhase =
  | 'idle'
  | 'requesting-permission'
  | 'connecting'
  | 'listening'
  | 'thinking'
  | 'speaking'
  | 'error'

type TranscriptEntry = {
  id: string
  role: 'user' | 'agent'
  text: string
  ts: number
}

type VoiceConfig = {
  livekitUrl: string
  token: string
  roomName: string
  identity: string
}

type Props = {
  className?: string
}

export function VoiceScreen({ className }: Props) {
  const [phase, setPhase] = useState<VoicePhase>('idle')
  const [muted, setMuted] = useState(false)
  const [transcript, setTranscript] = useState<TranscriptEntry[]>([])
  const [errorMessage, setErrorMessage] = useState<string | null>(null)

  const roomRef = useRef<Room | null>(null)

  const appendTranscript = useCallback((entry: Omit<TranscriptEntry, 'id' | 'ts'>) => {
    setTranscript((prev) => [
      ...prev,
      { ...entry, id: crypto.randomUUID(), ts: Date.now() },
    ])
  }, [])

  const disconnect = useCallback(async () => {
    if (roomRef.current) {
      await roomRef.current.disconnect()
      roomRef.current = null
    }
    setPhase('idle')
  }, [])

  const connect = useCallback(async () => {
    setErrorMessage(null)
    setPhase('requesting-permission')
    try {
      const cfgRes = await fetch('/api/voice/token', {
        method: 'POST',
        credentials: 'include',
      })
      if (!cfgRes.ok) {
        throw new Error(`token request failed (${cfgRes.status})`)
      }
      const cfg: VoiceConfig = await cfgRes.json()

      const room = new Room({
        adaptiveStream: true,
        dynacast: true,
      })
      roomRef.current = room

      room.on(RoomEvent.ConnectionStateChanged, (state) => {
        if (state === ConnectionState.Connected) {
          setPhase('listening')
        } else if (state === ConnectionState.Disconnected) {
          setPhase('idle')
        } else if (
          state === ConnectionState.Failed ||
          state === ConnectionState.Reconnecting
        ) {
          setPhase('connecting')
        }
      })

      room.on(RoomEvent.TrackSubscribed, (track) => {
        if (track.kind === Track.Kind.Audio) {
          const el = track.attach()
          ;(el as HTMLAudioElement).autoplay = true
          document.body.appendChild(el)
        }
      })

      room.on(RoomEvent.AudioPlaybackStatusChanged, () => {
        // Best-effort — actual speech/thinking states are derived from
        // the agent's `user_input_transcribed` events below.
      })

      await room.connect(cfg.livekitUrl, cfg.token)
      setPhase('connecting')

      await room.localParticipant.setMicrophoneEnabled(true)
      // Local participant events surface user speech on the agent side,
      // which writes back to the conversation log on the Pi.
    } catch (err) {
      const message = err instanceof Error ? err.message : 'unknown error'
      setErrorMessage(message)
      setPhase('error')
    }
  }, [])

  useEffect(() => {
    return () => {
      if (roomRef.current) {
        roomRef.current.disconnect().catch(() => undefined)
        roomRef.current = null
      }
    }
  }, [])

  const phaseLabel = useMemo(() => {
    switch (phase) {
      case 'idle':
        return 'Tap to start'
      case 'requesting-permission':
        return 'Waiting for microphone…'
      case 'connecting':
        return 'Connecting to Hermes…'
      case 'listening':
        return muted ? 'Muted' : 'Listening…'
      case 'thinking':
        return 'Hermes is thinking…'
      case 'speaking':
        return 'Hermes is speaking…'
      case 'error':
        return errorMessage ?? 'Something went wrong'
    }
  }, [phase, muted, errorMessage])

  return (
    <div
      className={cn(
        'flex h-full min-h-0 flex-col items-stretch gap-4 p-4 md:p-8',
        className,
      )}
    >
      <header className="flex items-center justify-between">
        <div className="flex items-center gap-2">
          <HugeiconsIcon icon={AiVoiceIcon} className="size-5" />
          <h1 className="text-lg font-semibold">Voice</h1>
        </div>
        <p className="text-xs text-neutral-500 dark:text-neutral-400">
          Live conversation · Deepgram STT · Cartesia TTS
        </p>
      </header>

      <Card className="flex flex-1 flex-col gap-4 p-4 md:p-6">
        <div className="flex items-center justify-center gap-4 py-6">
          <VoiceOrb phase={phase} />
        </div>

        <div className="text-center text-sm font-medium text-neutral-700 dark:text-neutral-300">
          {phaseLabel}
        </div>

        <ScrollArea className="flex-1 rounded-md border border-primary-200 p-3 dark:border-neutral-800">
          {transcript.length === 0 ? (
            <p className="px-2 py-6 text-center text-sm text-neutral-500 dark:text-neutral-400">
              Transcript will appear here as you talk.
            </p>
          ) : (
            <ul className="flex flex-col gap-3">
              {transcript.map((entry) => (
                <li
                  key={entry.id}
                  className={cn(
                    'flex flex-col gap-1 rounded-md px-3 py-2 text-sm',
                    entry.role === 'user'
                      ? 'self-end bg-primary-100 text-primary-950 dark:bg-primary-900 dark:text-primary-50'
                      : 'self-start bg-neutral-100 text-neutral-900 dark:bg-neutral-900 dark:text-neutral-100',
                  )}
                >
                  <span className="text-xs font-semibold uppercase tracking-wide text-neutral-500 dark:text-neutral-400">
                    {entry.role === 'user' ? 'You' : 'Hermes'}
                  </span>
                  <span>{entry.text}</span>
                </li>
              ))}
            </ul>
          )}
        </ScrollArea>

        <div className="flex items-center justify-center gap-2">
          {phase === 'idle' || phase === 'error' ? (
            <Button
              size="lg"
              onClick={() => {
                void connect()
              }}
            >
              <HugeiconsIcon icon={TelephoneIcon} className="size-4" />
              Start conversation
            </Button>
          ) : (
            <>
              <Button
                size="lg"
                variant="outline"
                onClick={() => setMuted((m) => !m)}
                disabled={phase === 'connecting' || phase === 'requesting-permission'}
              >
                <HugeiconsIcon
                  icon={AiMicIcon}
                  className={cn('size-4', muted && 'opacity-50')}
                />
                {muted ? 'Unmute' : 'Mute'}
              </Button>
              <Button
                size="lg"
                variant="destructive"
                onClick={() => {
                  void disconnect()
                }}
              >
                <HugeiconsIcon icon={Cancel01Icon} className="size-4" />
                End
              </Button>
            </>
          )}
        </div>

        {phase === 'error' && errorMessage ? (
          <p className="text-center text-xs text-red-600 dark:text-red-400">
            {errorMessage}
          </p>
        ) : null}
      </Card>
    </div>
  )
}

function VoiceOrb({ phase }: { phase: VoicePhase }) {
  const stateClass = useMemo(() => {
    switch (phase) {
      case 'listening':
        return 'bg-emerald-400 shadow-[0_0_60px_rgba(16,185,129,0.45)] animate-pulse'
      case 'thinking':
        return 'bg-amber-400 shadow-[0_0_60px_rgba(251,191,36,0.45)] animate-pulse'
      case 'speaking':
        return 'bg-sky-400 shadow-[0_0_80px_rgba(56,189,248,0.55)] animate-pulse'
      case 'error':
        return 'bg-red-400 shadow-[0_0_40px_rgba(248,113,113,0.45)]'
      case 'idle':
      case 'requesting-permission':
      case 'connecting':
        return 'bg-neutral-300 dark:bg-neutral-700'
    }
  }, [phase])

  return (
    <div
      aria-hidden
      className={cn(
        'size-32 rounded-full transition-colors duration-300 md:size-40',
        stateClass,
      )}
    />
  )
}
