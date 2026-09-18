/**
 * Patch applied to src/screens/chat/components/chat-sidebar.tsx
 *
 * Adds a "Voice" nav item linking to /voice, gated on the voice feature
 * being available (LiveKit + gateway + voice agent).
 *
 * Apply with: see docs/hermes-ultimate-brain/livekit-voice-mode-plan.md
 * and the install script in hermes-deploy/voice-mode/apply.sh.
 */
import { AiVoiceIcon } from '@hugeicons/core-free-icons'

// In workspaceItems array, add:
const voiceNavItem = {
  kind: 'link' as const,
  to: '/voice',
  icon: AiVoiceIcon,
  label: 'Voice',
  active: pathname === '/voice',
  dataTour: 'voice',
}
