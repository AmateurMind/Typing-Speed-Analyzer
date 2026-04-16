import { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import axios from 'axios'
import { motion } from 'framer-motion'
import { ArrowClockwise, ChartLineUp, Trophy } from 'phosphor-react'
import { ToastContainer, toast } from 'react-toastify'
import 'react-toastify/dist/ReactToastify.css'

const api = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080',
})

const sentencePrompts = [
  'The quick brown fox jumps over the lazy dog.',
  'Practice makes perfect when learning a new skill.',
  'Actions speak louder than words in most situations.',
  'Where there is a will, there is a way.',
  'Better late than never.',
]
const timedPromptChunks = [
  'Typing quickly and accurately takes focus, rhythm, and a steady hand on the keyboard.',
  'A good test should feel long enough to build speed while still rewarding careful correction.',
  'Keep your eyes on the words, your fingers relaxed, and your pace consistent from start to finish.',
  'The goal is not only to type fast, but to maintain accuracy even when the passage gets longer.',
  'When the timer is running, try to avoid rushing too early because mistakes can slow you down later.',
  'Longer practice passages help train endurance, attention, and overall typing confidence.',
  'Progress in typing is rarely linear, so steady daily practice with varied paragraph styles improves consistency over time.',
  'Precision under pressure matters more than bursts of speed, because clean typing reduces backtracking and wasted seconds.',
  'Develop a smooth cadence by striking each key lightly, keeping wrists neutral, and maintaining a comfortable seated posture.',
  'Real-world writing often includes punctuation and mixed sentence lengths, so your training should include both simple and complex patterns.',
  'As your confidence grows, challenge yourself with unfamiliar vocabulary to build adaptability and reduce hesitation mid-sentence.',
  'Strong typists scan two or three words ahead, allowing their fingers to move with intention instead of reacting one letter at a time.',
  'If accuracy drops, slow down briefly, restore control, and then increase pace again with deliberate and readable keystrokes.',
  'Focused repetition creates muscle memory, but mindful correction is what transforms raw speed into dependable performance.',
  'A calm rhythm can outperform frantic typing, especially when long passages demand concentration for the full duration of a round.',
]

function pickRandom(items) {
  return items[Math.floor(Math.random() * items.length)]
}

const testModes = {
  sentence: {
    label: 'Sentence mode',
    helper: 'Complete the full sentence. No timer.',
  },
  timed: {
    label: '30 second test',
    helper: 'Longer prompt. The round ends after 30 seconds.',
  },
}

function normalizeText(text) {
  return text.replace(/\s+/g, ' ').trim()
}

function splitWords(text) {
  return text.match(/\S+/g) ?? []
}

function calculateAccuracyPercent(typedText, promptText) {
  const typedWords = splitWords(typedText)
  const promptWords = splitWords(promptText)
  const compareCount = Math.max(promptWords.length, typedWords.length, 1)
  let correct = 0
  for (let i = 0; i < compareCount; i += 1) {
    if (typedWords[i] && typedWords[i] === promptWords[i]) {
      correct += 1
    }
  }
  return (correct / compareCount) * 100
}

function formatScoreTimestamp(row) {
  return row.timestamp || row.date || 'N/A'
}

function formatScoreAccuracy(row) {
  if (row.accuracy === null || row.accuracy === undefined || Number.isNaN(Number(row.accuracy))) {
    return 'N/A'
  }
  return `${Number(row.accuracy).toFixed(1)}%`
}

function buildTimedPrompt(basePrompt) {
  const shuffledChunks = [...timedPromptChunks].sort(() => Math.random() - 0.5)
  const chosenChunks = [basePrompt, ...shuffledChunks]
  let totalWords = 0
  const result = []

  for (const chunk of chosenChunks) {
    result.push(chunk)
    totalWords += splitWords(chunk).length
    if (totalWords >= 80) break
  }

  return result.join(' ')
}

function getOrCreateUserId() {
  const key = 'typing_speed_user_id'
  const existing = window.localStorage.getItem(key)
  if (existing && existing.trim()) return existing
  const generated = `user-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`
  window.localStorage.setItem(key, generated)
  return generated
}

function App() {
  const canvasRef = useRef(null)
  const inputRef = useRef(null)
  const timerRef = useRef(null)
  const countdownRef = useRef(null)
  const finishLockRef = useRef(false)
  const typedTextRef = useRef('')

  const [mode, setMode] = useState('sentence')
  const [prompt, setPrompt] = useState('Loading prompt...')
  const [typedText, setTypedText] = useState('')
  const [running, setRunning] = useState(false)
  const [startMs, setStartMs] = useState(0)
  const [elapsedMs, setElapsedMs] = useState(0)
  const [timedElapsed, setTimedElapsed] = useState(0)
  const [wpm, setWpm] = useState(0)
  const [accuracy, setAccuracy] = useState(100)
  const [scores, setScores] = useState([])
  const [status, setStatus] = useState('Ready')
  const userIdRef = useRef('')

  const promptWords = useMemo(() => splitWords(prompt), [prompt])

  const stopTimers = useCallback(() => {
    if (timerRef.current) {
      clearTimeout(timerRef.current)
      timerRef.current = null
    }
    if (countdownRef.current) {
      clearInterval(countdownRef.current)
      countdownRef.current = null
    }
  }, [])

  const fetchPrompt = useCallback(async () => {
    try {
      const res = await api.get('/prompt')
      const text = String(res.data ?? '').trim()
      return text || pickRandom(sentencePrompts)
    } catch {
      return pickRandom(sentencePrompts)
    }
  }, [])

  const updateStats = useCallback(
    (text, nowMs) => {
      const startedAt = startMs || nowMs
      const elapsed = Math.max(1, nowMs - startedAt)
      setElapsedMs(elapsed)

      const wordsTyped = splitWords(text).length
      const nextWpm = wordsTyped > 0 ? (wordsTyped / (elapsed / 60000)) : 0
      setWpm(nextWpm)

      const typedWords = splitWords(text)
      const compareCount = Math.max(promptWords.length, typedWords.length, 1)
      let correct = 0
      for (let i = 0; i < compareCount; i += 1) {
        if (typedWords[i] && typedWords[i] === promptWords[i]) {
          correct += 1
        }
      }
      const nextAccuracy = (correct / compareCount) * 100
      setAccuracy(nextAccuracy)
    },
    [promptWords, startMs]
  )

  const loadPrompt = useCallback(
    async (nextMode = mode) => {
      const basePrompt = await fetchPrompt()
      const finalPrompt = nextMode === 'timed' ? buildTimedPrompt(basePrompt) : basePrompt

      setPrompt(finalPrompt)
      setStatus(nextMode === 'timed' ? '30 second test ready' : 'Sentence mode ready')
    },
    [fetchPrompt, mode]
  )

  const loadScores = useCallback(async () => {
    try {
      const res = await api.get('/scores', {
        params: { userId: userIdRef.current },
      })
      setScores(Array.isArray(res.data) ? res.data : [])
    } catch {
      setScores([])
    }
  }, [])

  const finishTest = useCallback(
    async (finalText, endMs) => {
      if (finishLockRef.current) return
      finishLockRef.current = true
      stopTimers()
      setRunning(false)
      updateStats(finalText, endMs)

      const wordsTyped = splitWords(finalText).length
      const duration = mode === 'timed' ? 30000 : Math.max(1, endMs - startMs)
      const finalWpm = wordsTyped > 0 ? (wordsTyped / (duration / 60000)) : 0
      const finalAccuracy = calculateAccuracyPercent(finalText, prompt)
      setAccuracy(finalAccuracy)

      if (mode === 'timed') {
        setTimedElapsed(30)
        toast.success('This is done okayyy')
      }

      try {
        await api.post('/result', {
          wpm: Number(finalWpm.toFixed(2)),
          accuracy: Number(finalAccuracy.toFixed(2)),
          userId: userIdRef.current,
        })
        await loadScores()

        if (mode === 'timed') {
          toast.success('Score stored okayy')
          setStatus('30 second result saved and scores refreshed')
        } else {
          setStatus('Sentence completed, saved, and scores refreshed')
        }
      } catch {
        setStatus('Could not save result (backend unavailable)')
      }
    },
    [loadScores, mode, prompt, startMs, stopTimers, updateStats]
  )

  const resetTest = useCallback(async (nextMode = mode) => {
    stopTimers()
    finishLockRef.current = false
    typedTextRef.current = ''
    setTypedText('')
    setRunning(false)
    setStartMs(0)
    setElapsedMs(0)
    setTimedElapsed(0)
    setWpm(0)
    setAccuracy(100)
    await loadPrompt(nextMode)
    requestAnimationFrame(() => inputRef.current?.focus())
  }, [loadPrompt, mode, stopTimers])

  const changeMode = useCallback(
    async (nextMode) => {
      if (nextMode === mode) return
      setMode(nextMode)
      await resetTest(nextMode)
    },
    [mode, resetTest]
  )

  const onInputChange = (e) => {
    const value = e.target.value
    const now = Date.now()
    typedTextRef.current = value
    setTypedText(value)

    if (!running && value.length > 0) {
      setRunning(true)
      setStartMs(now)
      if (mode === 'timed') {
        stopTimers()
        setTimedElapsed(0)
        countdownRef.current = window.setInterval(() => {
          setTimedElapsed((current) => {
            if (current >= 30) {
              stopTimers()
              finishTest(typedTextRef.current, Date.now())
              return 30
            }
            return current + 1
          })
        }, 1000)
        timerRef.current = window.setTimeout(() => {
          setTimedElapsed(30)
          finishTest(typedTextRef.current, Date.now())
        }, 30000)
      }
    }

    if (running || value.length > 0) {
      updateStats(value, now)
    }

    if (mode === 'sentence' && normalizeText(value) === normalizeText(prompt)) {
      finishTest(value, now)
    }
  }

  useEffect(() => {
    userIdRef.current = getOrCreateUserId()
    resetTest(mode)
    loadScores()
    return () => stopTimers()
  }, [])

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return
    const ctx = canvas.getContext('2d')
    if (!ctx) return

    const width = canvas.width
    const padding = 26
    const lineHeight = 40
    const maxWidth = width - padding * 2
    ctx.font = '700 26px Consolas, Monaco, monospace'
    ctx.textBaseline = 'top'
    const spaceWidth = ctx.measureText(' ').width
    const visibleLines = Math.max(1, Math.floor((canvas.height - padding * 2) / lineHeight))

    ctx.fillStyle = '#0f172a'
    ctx.fillRect(0, 0, canvas.width, canvas.height)

    let x = padding
    let line = 0
    const typedWords = splitWords(typedText)
    const hasTrailingSpace = /\s$/.test(typedText)
    const currentWordIndex = typedWords.length === 0
      ? 0
      : hasTrailingSpace
        ? typedWords.length
        : typedWords.length - 1
    const wordLines = []

    for (let i = 0; i < promptWords.length; i += 1) {
      const word = promptWords[i]
      const wordWidth = ctx.measureText(word).width
      const neededWidth = i === promptWords.length - 1 ? wordWidth : wordWidth + spaceWidth

      if (x + neededWidth > padding + maxWidth) {
        x = padding
        line += 1
      }
      wordLines[i] = line
      x += neededWidth
    }

    const safeCurrentWordIndex = Math.min(currentWordIndex, Math.max(promptWords.length - 1, 0))
    const currentLine = wordLines[safeCurrentWordIndex] ?? 0
    const startLine = Math.max(0, currentLine - visibleLines + 2)

    x = padding
    line = 0
    for (let i = 0; i < promptWords.length; i += 1) {
      const word = promptWords[i]
      const wordWidth = ctx.measureText(word).width
      const neededWidth = i === promptWords.length - 1 ? wordWidth : wordWidth + spaceWidth

      if (x + neededWidth > padding + maxWidth) {
        x = padding
        line += 1
      }

      const y = padding + (line - startLine) * lineHeight
      if (y < padding - lineHeight || y > canvas.height - padding) {
        x += neededWidth
        continue
      }

      if (typedWords[i] === undefined) {
        ctx.fillStyle = i === currentWordIndex ? '#facc15' : '#64748b'
      } else if (!hasTrailingSpace && i === currentWordIndex) {
        ctx.fillStyle = '#facc15'
      } else if (typedWords[i] === word) {
        ctx.fillStyle = '#ffffff'
      } else {
        ctx.fillStyle = '#ef4444'
      }

      ctx.fillText(word, x, y)
      x += wordWidth

      if (i !== promptWords.length - 1) {
        ctx.fillStyle = '#64748b'
        ctx.fillText(' ', x, y)
        x += spaceWidth
      }
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
            <p className="text-sm text-slate-300">Word-wise rendering: white = correct, yellow = current word, red = wrong word</p>
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

        <div className="mb-4 flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => changeMode('sentence')}
            className={`rounded-full px-4 py-2 text-sm font-semibold transition ${mode === 'sentence' ? 'bg-amber-400 text-slate-950' : 'bg-slate-800 text-slate-200 hover:bg-slate-700'}`}
          >
            {testModes.sentence.label}
          </button>
          <button
            type="button"
            onClick={() => changeMode('timed')}
            className={`rounded-full px-4 py-2 text-sm font-semibold transition ${mode === 'timed' ? 'bg-emerald-400 text-slate-950' : 'bg-slate-800 text-slate-200 hover:bg-slate-700'}`}
          >
            {testModes.timed.label}
          </button>
          <p className="text-sm text-slate-400">{testModes[mode].helper}</p>
        </div>

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
          <div className="rounded-xl bg-slate-800 p-3 text-slate-100">Mode: {testModes[mode].label}</div>
          <div className="rounded-xl bg-slate-800 p-3 text-slate-100">Timer: {mode === 'timed' ? `${timedElapsed}s / 30s` : 'No timer'}</div>
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
              <li key={`${formatScoreTimestamp(row)}-${idx}`} className="rounded-lg bg-slate-800 px-3 py-2">
                {Number(row.speed ?? 0).toFixed(2)} WPM | Accuracy: {formatScoreAccuracy(row)} | {formatScoreTimestamp(row)}
              </li>
            ))}
          </ul>
        </section>
      </motion.section>
      <ToastContainer position="top-right" autoClose={2200} />
    </main>
  )
}

export default App
