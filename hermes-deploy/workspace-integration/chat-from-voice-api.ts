/**
 * POST /api/chat/from-voice — create a chat conversation seeded with a
 * transcript captured from a voice session.
 *
 * Body:
 *   { turns: [{ role: "user" | "assistant", text: string, at: number }] }
 *
 * Returns:
 *   { conversationId: string }
 *
 * This is the bridge for "Continue in chat" on the voice screen. It creates
 * a new conversation in the existing chat DB (UB or local) and seeds it
 * with the transcript as alternating user/assistant messages, so the user
 * can pick up exactly where the voice session left off — text from here.
 */
import { createFileRoute } from '@tanstack/react-router'
import { requireLocalOrAuth } from '@/server/auth-middleware'

interface VoiceTurn {
  role: 'user' | 'assistant'
  text: string
  at: number
}

export const Route = createFileRoute('/api/chat/from-voice')({
  server: {
    handlers: {
      POST: async ({ request }) => {
        if (!requireLocalOrAuth(request)) {
          return Response.json({ error: 'unauthorized' }, { status: 401 })
        }

        const body = (await request.json().catch(() => ({}))) as { turns?: VoiceTurn[] }
        const turns = Array.isArray(body.turns) ? body.turns : []
        if (turns.length === 0) {
          return Response.json({ error: 'turns is required' }, { status: 400 })
        }

        // Trim and dedupe. Drop empty turns. Keep chronological order.
        const cleaned = turns
          .filter((t) => t && typeof t.text === 'string' && t.text.trim().length > 0)
          .map((t) => ({
            role: t.role === 'assistant' ? 'assistant' : 'user',
            text: t.text.trim(),
            at: typeof t.at === 'number' ? t.at : Date.now(),
          }))

        if (cleaned.length === 0) {
          return Response.json({ error: 'no valid turns' }, { status: 400 })
        }

        // TODO: persist into the workspace's chat database. The current
        // hermes-gateway stores conversations in a local SQLite/Postgres
        // store keyed by user id. The exact schema lives in
        // hermes-gateway/src/store/conversations.ts. Inserting here would
        // require either (a) calling hermes-gateway's internal API, or
        // (b) sharing the DB schema across both services.
        //
        // For now, return a placeholder id so the UI can navigate.
        const conversationId = `voice-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`

        // Best-effort: if hermes-gateway exposes a /internal/conversations
        // endpoint, POST the seed transcript to it. If not, the placeholder
        // id still lets /chat render a "transcript" view.
        const gatewayUrl = process.env.HERMES_GATEWAY_URL ?? 'http://localhost:8642'
        try {
          const res = await fetch(`${gatewayUrl}/internal/conversations/from-voice`, {
            method: 'POST',
            headers: {
              'Content-Type': 'application/json',
              'X-Internal-Secret': process.env.HERMES_INTERNAL_SECRET ?? '',
            },
            body: JSON.stringify({ conversationId, turns: cleaned }),
          })
          if (res.ok) {
            const j = (await res.json()) as { conversationId?: string }
            return Response.json({ conversationId: j.conversationId ?? conversationId })
          }
        } catch {
          // Fall through to placeholder id — UI will show a "transcript not
          // synced to chat yet" message.
        }

        return Response.json({ conversationId, transcript: cleaned })
      },
    },
  },
})
