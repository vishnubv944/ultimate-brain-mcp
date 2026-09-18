/**
 * useVoiceHotkeys — global keyboard shortcuts for voice mode.
 *
 *   Space (hold)   : push-to-talk — hold Space to record, release to send
 *   Space (tap)    : toggle mic (when voice session is connected)
 *   Esc            : end voice session
 *   M              : mute / unmute
 *
 * Honors <input>, <textarea>, and contentEditable focus — Space in a text
 * field stays a space character, not a hotkey.
 */
import { useEffect } from 'react'

export interface VoiceHotkeyHandlers {
  onToggleMic?: () => void
  onEndSession?: () => void
  onPushToTalkStart?: () => void
  onPushToTalkStop?: () => void
  enabled?: boolean
}

function isTypingTarget(el: EventTarget | null): boolean {
  if (!(el instanceof HTMLElement)) return false
  const tag = el.tagName
  if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return true
  if (el.isContentEditable) return true
  return false
}

export function useVoiceHotkeys(handlers: VoiceHotkeyHandlers) {
  const { enabled = true, onToggleMic, onEndSession, onPushToTalkStart, onPushToTalkStop } = handlers

  useEffect(() => {
    if (!enabled) return

    let pttActive = false

    const onKeyDown = (e: KeyboardEvent) => {
      if (e.metaKey || e.ctrlKey || e.altKey) return
      if (isTypingTarget(e.target)) return

      if (e.code === 'Space' && !pttActive) {
        // Tap-to-toggle only when not held; press-and-hold → PTT.
        if (onPushToTalkStart) {
          e.preventDefault()
          pttActive = true
          onPushToTalkStart()
        }
      }
      if (e.code === 'Escape' && onEndSession) {
        e.preventDefault()
        onEndSession()
      }
      if (e.code === 'KeyM' && onToggleMic) {
        e.preventDefault()
        onToggleMic()
      }
    }

    const onKeyUp = (e: KeyboardEvent) => {
      if (isTypingTarget(e.target)) return
      if (e.code === 'Space' && pttActive) {
        pttActive = false
        if (onPushToTalkStop) {
          e.preventDefault()
          onPushToTalkStop()
        } else if (onToggleMic) {
          // No PTT handler — fall back to tap-to-toggle.
          onToggleMic()
        }
      }
    }

    window.addEventListener('keydown', onKeyDown)
    window.addEventListener('keyup', onKeyUp)
    return () => {
      window.removeEventListener('keydown', onKeyDown)
      window.removeEventListener('keyup', onKeyUp)
    }
  }, [enabled, onToggleMic, onEndSession, onPushToTalkStart, onPushToTalkStop])
}
