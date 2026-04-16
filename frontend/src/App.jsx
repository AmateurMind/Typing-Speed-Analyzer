import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import axios from 'axios'
import { motion } from 'framer-motion'
import { ArrowClockwise, ChartLineUp, Trophy } from 'phosphor-react'

const api = axios.create({
  baseURL: 'http://localhost:8080',
})

const fallbackPrompts = [
  'The quick brown fox jumps over the lazy dog.',
  'Practice makes perfect when learning a new skill.',
  'Actions speak louder than words in most situations.',
  'Where there is a will, there is a way.',
  'Better late than never.',
]

function calculateStopLength(text) {
  let i = text.length
  while (i > 0) {
    const c = text.charAt(i - 1)
    if (/^[a-z0-9]$/i.test(c)) break
    i -= 1
  }
  return i
}

function App() {
  const canvasRef = useRef(null)
  const inputRef = useRef(null)

  const [prompt, setPrompt] = useState('Loading prompt...')
  const [typedText, setTypedText] = useState('')
  const [running, setRunning] = useState(false)
  const [startMs, setStartMs] = useState(0)
  const [elapsedMs, setElapsedMs] = useState(0)
  const [wpm, setWpm] = useState(0)
  const [accuracy, setAccuracy] = useState(100)
  const [scores, setScores] = useState([])
  const [status, setStatus] = useState('Ready')

  const stopLength = useMemo(() => calculateStopLength(prompt), [prompt])

  const updateStats = useCallback(
    (text, nowMs) => {
      if (!startMs) return
      const elapsed = Math.max(1, nowMs - startMs)
      setElapsedMs(elapsed)

      const wordsTyped = text.trim() ? text.trim().split(/\s+/).length : 0
      const nextWpm = wordsTyped > 0 ? (wordsTyped / (elapsed / 60000)) : 0
      setWpm(nextWpm)

      let correct = 0
      const minLen = Math.min(text.length, prompt.length)
      for (let i = 0; i < minLen; i += 1) {
        if (text[i] === prompt[i]) correct += 1
      }
      const nextAccuracy = text.length === 0 ? 100 : (correct / text.length) * 100
      setAccuracy(nextAccuracy)
    },
    [prompt, startMs]
  )

  const loadPrompt = useCallback(async () => {
    try {
      const res = await api.get('/prompt')
      setPrompt(String(res.data || fallbackPrompts[Math.floor(Math.random() * fallbackPrompts.length)]))
      setStatus('Prompt loaded from backend')
    } catch {
      setPrompt(fallbackPrompts[Math.floor(Math.random() * fallbackPrompts.length)])
      setStatus('Backend unavailable, using local prompt')
    }
  }, [])

  const loadScores = useCallback(async () => {
    try {
      const res = await api.get('/scores')
      setScores(Array.isArray(res.data) ? res.data : [])
    } catch {
      setScores([])
    }
  }, [])

  const finishTest = useCallback(
    async (finalText, endMs) => {
      setRunning(false)
      updateStats(finalText, endMs)

      const wordsTyped = finalText.trim() ? finalText.trim().split(/\s+/).length : 0
      const duration = Math.max(1, endMs - startMs)
      const finalWpm = wordsTyped > 0 ? (wordsTyped / (duration / 60000)) : 0

      try {
        await api.post('/result', { speed: Number(finalWpm.toFixed(2)) })
        setStatus('Result saved to backend')
      } catch {
        setStatus('Could not save result (backend unavailable)')
      }

      await loadScores()
    },
    [loadScores, startMs, updateStats]
  )

  const resetTest = useCallback(async () => {
    setTypedText('')
    setRunning(false)
    setStartMs(0)
    setElapsedMs(0)
    setWpm(0)
    setAccuracy(100)
    await loadPrompt()
    requestAnimationFrame(() => inputRef.current?.focus())
  }, [loadPrompt])

  const onInputChange = async (e) => {
    const value = e.target.value
    const now = Date.now()
    setTypedText(value)

    if (!running && value.length > 0) {
      setRunning(true)
      setStartMs(now)
    }

    if (running || value.length > 0) {
      updateStats(value, now)
    }

    if (value.length >= stopLength && stopLength > 0) {
      await finishTest(value, now)
    }
  }

  useEffect(() => {
    resetTest()
    loadScores()
  }, [loadScores, resetTest])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const width = canvas.width
    const padding = 26
    const lineHeight = 34
    const maxWidth = width - padding * 2

    ctx.fillStyle = '#0f172a'
    ctx.fillRect(0, 0, canvas.width, canvas.height)
    ctx.font = '700 28px Consolas, Monaco, monospace'
    ctx.textBaseline = 'top'

    let x = padding
    let y = padding

    for (let i = 0; i < prompt.length; i += 1) {
      const ch = prompt[i]
      const w = ctx.measureText(ch).width
      if (x + w > padding + maxWidth) {
        x = padding
        y += lineHeight
      }

      if (i < typedText.length) {
        ctx.fillStyle = typedText[i] === ch ? '#ffffff' : '#ef4444'
      } else if (i === typedText.length) {
        ctx.fillStyle = '#facc15'
      } else {
        ctx.fillStyle = '#64748b'
      }

      ctx.fillText(ch, x, y)
      x += w
    }
  }, [prompt, typedText])

  return (
    <main className="min-h-screen bg-gradient-to-br from-slate-950 via-slate-900 to-zinc-900 px-4 py-8">
      <motion.section
        initial={{ opacity: 0, y: 18 }}
        animate={{ opacity: 1, y: 0 }}
        transition={{ duration: 0.35 }}
        className="mx-auto max-w-5xl rounded-3xl border border-slate-700/70 bg-slate-900/75 p-6 shadow-2xl backdrop-blur"
      >
        <header className="mb-6 flex items-center justify-between gap-4">
          <div>
            <h1 className="text-3xl font-bold text-white">Typing Speed Engine</h1>
            <p className="text-sm text-slate-300">Canvas rendering: white = correct, yellow = current, red = wrong</p>
          </div>
          <div className="flex gap-2">
            <button
              type="button"
              onClick={resetTest}
              className="inline-flex items-center gap-2 rounded-xl bg-slate-800 px-4 py-2 text-sm font-semibold text-slate-100 hover:bg-slate-700"
            >
              <ArrowClockwise size={18} />
              Restart
            </button>
            <button
              type="button"
              onClick={loadScores}
              className="inline-flex items-center gap-2 rounded-xl bg-emerald-700 px-4 py-2 text-sm font-semibold text-white hover:bg-emerald-600"
            >
              <ChartLineUp size={18} />
              Refresh Scores
            </button>
          </div>
        </header>

        <canvas
          ref={canvasRef}
          width={1000}
          height={240}
          className="w-full rounded-2xl border border-slate-700 bg-slate-950"
        />

        <textarea
          ref={inputRef}
          value={typedText}
          onChange={onInputChange}
          placeholder="Start typing here..."
          className="mt-4 h-32 w-full resize-none rounded-2xl border border-slate-700 bg-slate-950/70 p-4 font-mono text-lg text-slate-100 outline-none focus:border-yellow-400"
        />

        <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
          <div className="rounded-xl bg-slate-800 p-3 text-slate-100">Time: {(elapsedMs / 1000).toFixed(1)}s</div>
          <div className="rounded-xl bg-slate-800 p-3 text-slate-100">WPM: {wpm.toFixed(2)}</div>
          <div className="rounded-xl bg-slate-800 p-3 text-slate-100">Accuracy: {accuracy.toFixed(1)}%</div>
        </div>

        <p className="mt-3 text-sm text-slate-400">Status: {status}</p>

        <section className="mt-8 rounded-2xl border border-slate-700 p-4">
          <h2 className="mb-3 flex items-center gap-2 text-lg font-semibold text-white">
            <Trophy size={20} weight="fill" />
            Latest Scores
          </h2>
          <ul className="space-y-2 text-sm text-slate-200">
            {scores.length === 0 && <li className="text-slate-400">No scores yet.</li>}
            {scores.map((row, idx) => (
              <li key={`${row.date || 'd'}-${idx}`} className="rounded-lg bg-slate-800 px-3 py-2">
                {Number(row.speed).toFixed(2)} WPM - {row.date || 'N/A'}
              </li>
            ))}
          </ul>
        </section>
      </motion.section>
    </main>
  )
}

export default App
