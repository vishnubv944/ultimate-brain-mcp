/**
 * POST /api/voice/token — mint a LiveKit access token for the current user.
 *
 * Auth: same Hermes password as the rest of the workspace (enforced by
 * requireLocalOrAuth). Returns the LiveKit URL, a short-lived JWT, the room
 * name, and the user's identity (used by the agent to greet by name).
 *
 * LiveKit config is read from environment:
 *   LIVEKIT_URL          — ws://localhost:7880 by default
 *   LIVEKIT_API_KEY      — required (no default — must be set in workspace env)
 *   LIVEKIT_API_SECRET   — required (no default — must be set in workspace env)
 */
import jwt from 'jsonwebtoken'
import { createFileRoute } from '@tanstack/react-router'
import { requireLocalOrAuth } from '@/server/auth-middleware'

const LIVEKIT_URL = process.env.LIVEKIT_URL ?? 'ws://localhost:7880'
// Credentials are required at runtime. Set them in the workspace's .env
// (or systemd EnvironmentFile). Defaulting to a known string here would
// leak the dev token into the workspace bundle.
const API_KEY = process.env.LIVEKIT_API_KEY
const API_SECRET = process.env.LIVEKIT_API_SECRET

export const Route = createFileRoute('/api/voice/token')({
  server: {
    handlers: {
      POST: async ({ request }) => {
        if (!requireLocalOrAuth(request)) {
          return Response.json({ error: 'unauthorized' }, { status: 401 })
        }
        if (!API_KEY || !API_SECRET) {
          return Response.json(
            {
              error:
                'LIVEKIT_API_KEY / LIVEKIT_API_SECRET not configured on the workspace.',
            },
            { status: 500 },
          )
        }

        const body = (await request.json().catch(() => ({}))) as {
          room?: string
          identity?: string
        }

        const identity =
          body.identity?.trim() ||
          `user-${Math.random().toString(36).slice(2, 10)}`
        const room = body.room?.trim() || `voice-${Date.now()}`

        const now = Math.floor(Date.now() / 1000)
        const ttl = 60 * 60 // 1 hour
        const token = jwt.sign(
          {
            iss: API_KEY,
            sub: identity,
            iat: now,
            exp: now + ttl,
            video: {
              room,
              roomJoin: true,
              canPublish: true,
              canSubscribe: true,
              canPublishData: true,
            },
          },
          API_SECRET,
          { algorithm: 'HS256' },
        )

        return Response.json({
          livekitUrl: LIVEKIT_URL,
          token,
          roomName: room,
          identity,
          ttlSeconds: ttl,
        })
      },
    },
  },
})
