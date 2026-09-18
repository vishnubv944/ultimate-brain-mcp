/**
 * GET /api/voice/status — surface voice mode readiness to the UI.
 *
 * Reports whether LiveKit, the voice agent, and the gateway are reachable.
 * Used by the voice screen to show a "voice unavailable" state instead of
 * a confusing WebRTC failure.
 */
import { createFileRoute } from '@tanstack/react-router'
import { execFile } from 'node:child_process'
import { promisify } from 'node:util'

const execFileAsync = promisify(execFile)

const LIVEKIT_URL = process.env.LIVEKIT_URL ?? 'ws://localhost:7880'
// The workspace is served on the Tailscale IP, so prefer HERMES_API_URL
// (set by systemd on the workspace unit) and fall back to localhost.
const GATEWAY_HTTP =
  process.env.HERMES_API_URL ?? process.env.HERMES_GATEWAY_URL ?? 'http://localhost:8642/v1'
const GATEWAY_HEALTH = GATEWAY_HTTP.replace(/\/v1$/, '/health')

type ServiceState = 'up' | 'down' | 'unknown'

async function checkHttp(url: string, timeoutMs = 1500): Promise<ServiceState> {
  try {
    const controller = new AbortController()
    const timer = setTimeout(() => controller.abort(), timeoutMs)
    const res = await fetch(url, { signal: controller.signal })
    clearTimeout(timer)
    // LiveKit root returns 404; that's still "up". Anything that responds is up.
    return res.status > 0 ? 'up' : 'down'
  } catch {
    return 'down'
  }
}

async function checkProcess(name: string): Promise<ServiceState> {
  try {
    const { stdout } = await execFileAsync('pgrep', ['-f', name], {
      timeout: 1500,
    })
    return stdout.trim().length > 0 ? 'up' : 'down'
  } catch {
    return 'down'
  }
}

export const Route = createFileRoute('/api/voice/status')({
  server: {
    handlers: {
      GET: async () => {
        const [livekit, gateway, voiceAgent] = await Promise.all([
          checkHttp(LIVEKIT_URL.replace(/^ws/, 'http')),
          checkHttp(GATEWAY_HEALTH),
          checkProcess('hermes-voice-agent|agent.py'),
        ])

        const available = livekit === 'up' && gateway === 'up'

        return Response.json({
          available,
          livekit,
          gateway,
          voiceAgent,
          livekitUrl: LIVEKIT_URL,
          gatewayUrl: GATEWAY_HTTP,
          // Surfaces the warning only when a real failure was detected —
          // a missing voice agent is acceptable (it'll start on demand).
          warning:
            !available
              ? livekit === 'down'
                ? 'LiveKit is not reachable. Run `docker compose -f ~/.hermes/livekit/docker-compose.yml up -d`.'
                : 'Hermes gateway is not reachable on port 8642.'
              : null,
        })
      },
    },
  },
})
