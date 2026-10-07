package com.example.tetris

import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import kotlin.math.abs
import kotlin.random.Random

class TetrisView(context: Context) : SurfaceView(context), SurfaceHolder.Callback, Runnable {

    private val holder: SurfaceHolder = getHolder()
    private var thread: Thread? = null
    private var running = false

    // Board size
    private val COLS = 10
    private val ROWS = 20
    private val board = Array(ROWS) { IntArray(COLS) }

    // Current piece
    private var currentPiece: Piece? = null
    private var nextPiece: Piece? = null
    private var pieceX = 0
    private var pieceY = 0

    // Game state
    private var score = 0
    private var level = 1
    private var linesCleared = 0
    private var gameOver = false
    private var paused = false

    // Timing
    private var lastDropTime = 0L
    private var dropInterval = 800L // ms

    // Drawing
    private var cellSize = 0f
    private var offsetX = 0f
    private var offsetY = 0f
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 48f
        typeface = Typeface.DEFAULT_BOLD
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY
        textSize = 36f
    }

    // Colors for pieces (classic-ish)
    private val colors = intArrayOf(
        Color.TRANSPARENT,          // 0 empty
        Color.parseColor("#00F0F0"), // I cyan
        Color.parseColor("#F0F000"), // O yellow
        Color.parseColor("#A000F0"), // T purple
        Color.parseColor("#00F000"), // S green
        Color.parseColor("#F00000"), // Z red
        Color.parseColor("#0000F0"), // J blue
        Color.parseColor("#F0A000")  // L orange
    )

    // Touch handling
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchStartTime = 0L

    init {
        holder.addCallback(this)
        isFocusable = true
    }

    // ==================== PIECE DEFINITIONS ====================

    data class Piece(val type: Int, val rotations: Array<Array<IntArray>>) {
        fun getShape(rotation: Int) = rotations[rotation % rotations.size]
    }

    private val pieces = listOf(
        // I
        Piece(1, arrayOf(
            arrayOf(intArrayOf(0,0,0,0), intArrayOf(1,1,1,1), intArrayOf(0,0,0,0), intArrayOf(0,0,0,0)),
            arrayOf(intArrayOf(0,0,1,0), intArrayOf(0,0,1,0), intArrayOf(0,0,1,0), intArrayOf(0,0,1,0))
        )),
        // O
        Piece(2, arrayOf(
            arrayOf(intArrayOf(2,2), intArrayOf(2,2))
        )),
        // T
        Piece(3, arrayOf(
            arrayOf(intArrayOf(0,3,0), intArrayOf(3,3,3), intArrayOf(0,0,0)),
            arrayOf(intArrayOf(0,3,0), intArrayOf(0,3,3), intArrayOf(0,3,0)),
            arrayOf(intArrayOf(0,0,0), intArrayOf(3,3,3), intArrayOf(0,3,0)),
            arrayOf(intArrayOf(0,3,0), intArrayOf(3,3,0), intArrayOf(0,3,0))
        )),
        // S
        Piece(4, arrayOf(
            arrayOf(intArrayOf(0,4,4), intArrayOf(4,4,0), intArrayOf(0,0,0)),
            arrayOf(intArrayOf(0,4,0), intArrayOf(0,4,4), intArrayOf(0,0,4))
        )),
        // Z
        Piece(5, arrayOf(
            arrayOf(intArrayOf(5,5,0), intArrayOf(0,5,5), intArrayOf(0,0,0)),
            arrayOf(intArrayOf(0,0,5), intArrayOf(0,5,5), intArrayOf(0,5,0))
        )),
        // J
        Piece(6, arrayOf(
            arrayOf(intArrayOf(6,0,0), intArrayOf(6,6,6), intArrayOf(0,0,0)),
            arrayOf(intArrayOf(0,6,6), intArrayOf(0,6,0), intArrayOf(0,6,0)),
            arrayOf(intArrayOf(0,0,0), intArrayOf(6,6,6), intArrayOf(0,0,6)),
            arrayOf(intArrayOf(0,6,0), intArrayOf(0,6,0), intArrayOf(6,6,0))
        )),
        // L
        Piece(7, arrayOf(
            arrayOf(intArrayOf(0,0,7), intArrayOf(7,7,7), intArrayOf(0,0,0)),
            arrayOf(intArrayOf(0,7,0), intArrayOf(0,7,0), intArrayOf(0,7,7)),
            arrayOf(intArrayOf(0,0,0), intArrayOf(7,7,7), intArrayOf(7,0,0)),
            arrayOf(intArrayOf(7,7,0), intArrayOf(0,7,0), intArrayOf(0,7,0))
        ))
    )

    private var currentRotation = 0

    // ==================== GAME LOGIC ====================

    private fun newPiece(): Piece {
        return pieces[Random.nextInt(pieces.size)].copy()
    }

    private fun spawnPiece() {
        if (nextPiece == null) nextPiece = newPiece()
        currentPiece = nextPiece
        nextPiece = newPiece()
        currentRotation = 0
        pieceX = COLS / 2 - 2
        pieceY = 0

        if (!isValidPosition(currentPiece!!, pieceX, pieceY, currentRotation)) {
            gameOver = true
        }
    }

    private fun isValidPosition(piece: Piece, x: Int, y: Int, rotation: Int): Boolean {
        val shape = piece.getShape(rotation)
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] != 0) {
                    val boardX = x + col
                    val boardY = y + row
                    if (boardX < 0 || boardX >= COLS || boardY >= ROWS) return false
                    if (boardY >= 0 && board[boardY][boardX] != 0) return false
                }
            }
        }
        return true
    }

    private fun lockPiece() {
        val piece = currentPiece ?: return
        val shape = piece.getShape(currentRotation)
        for (row in shape.indices) {
            for (col in shape[row].indices) {
                if (shape[row][col] != 0) {
                    val boardY = pieceY + row
                    val boardX = pieceX + col
                    if (boardY in 0 until ROWS && boardX in 0 until COLS) {
                        board[boardY][boardX] = shape[row][col]
                    }
                }
            }
        }
        clearLines()
        spawnPiece()
    }

    private fun clearLines() {
        var lines = 0
        var y = ROWS - 1
        while (y >= 0) {
            if (board[y].all { it != 0 }) {
                // Shift everything down
                for (row in y downTo 1) {
                    board[row] = board[row - 1].clone()
                }
                board[0] = IntArray(COLS)
                lines++
            } else {
                y--
            }
        }
        if (lines > 0) {
            linesCleared += lines
            score += when (lines) {
                1 -> 100 * level
                2 -> 300 * level
                3 -> 500 * level
                4 -> 800 * level
                else -> 0
            }
            level = (linesCleared / 10) + 1
            dropInterval = (800 - (level - 1) * 50).coerceAtLeast(100).toLong()
        }
    }

    private fun move(dx: Int, dy: Int): Boolean {
        if (gameOver || paused) return false
        val piece = currentPiece ?: return false
        if (isValidPosition(piece, pieceX + dx, pieceY + dy, currentRotation)) {
            pieceX += dx
            pieceY += dy
            return true
        }
        return false
    }

    private fun rotate() {
        if (gameOver || paused) return
        val piece = currentPiece ?: return
        val newRotation = (currentRotation + 1) % piece.rotations.size
        // Simple wall kick attempts
        val kicks = listOf(0, -1, 1, -2, 2)
        for (kick in kicks) {
            if (isValidPosition(piece, pieceX + kick, pieceY, newRotation)) {
                currentRotation = newRotation
                pieceX += kick
                return
            }
        }
    }

    private fun hardDrop() {
        if (gameOver || paused) return
        while (move(0, 1)) {
            score += 2
        }
        lockPiece()
        lastDropTime = System.currentTimeMillis()
    }

    private fun softDrop() {
        if (move(0, 1)) {
            score += 1
            lastDropTime = System.currentTimeMillis()
        } else {
            lockPiece()
            lastDropTime = System.currentTimeMillis()
        }
    }

    private fun resetGame() {
        for (row in board) row.fill(0)
        score = 0
        level = 1
        linesCleared = 0
        dropInterval = 800L
        gameOver = false
        paused = false
        nextPiece = null
        spawnPiece()
        lastDropTime = System.currentTimeMillis()
    }

    // ==================== RENDERING ====================

    private fun drawBoard(canvas: Canvas) {
        // Background
        canvas.drawColor(Color.parseColor("#111122"))

        // Calculate cell size to fit screen with side panel
        val boardWidth = width * 0.65f
        cellSize = boardWidth / COLS
        offsetX = (width - boardWidth) / 2 - cellSize * 0.5f
        offsetY = (height - ROWS * cellSize) / 2

        // Draw board background
        paint.color = Color.parseColor("#1a1a2e")
        canvas.drawRect(offsetX, offsetY, offsetX + COLS * cellSize, offsetY + ROWS * cellSize, paint)

        // Grid lines
        paint.color = Color.parseColor("#2a2a4e")
        paint.strokeWidth = 1f
        for (i in 0..COLS) {
            val x = offsetX + i * cellSize
            canvas.drawLine(x, offsetY, x, offsetY + ROWS * cellSize, paint)
        }
        for (i in 0..ROWS) {
            val y = offsetY + i * cellSize
            canvas.drawLine(offsetX, y, offsetX + COLS * cellSize, y, paint)
        }

        // Draw locked blocks
        for (y in 0 until ROWS) {
            for (x in 0 until COLS) {
                if (board[y][x] != 0) {
                    drawCell(canvas, x, y, board[y][x])
                }
            }
        }

        // Draw current piece
        currentPiece?.let { piece ->
            val shape = piece.getShape(currentRotation)
            for (row in shape.indices) {
                for (col in shape[row].indices) {
                    if (shape[row][col] != 0) {
                        drawCell(canvas, pieceX + col, pieceY + row, shape[row][col])
                    }
                }
            }
        }

        // Side panel - Score, Level, Next
        val panelX = offsetX + COLS * cellSize + 30f
        textPaint.textSize = cellSize * 0.9f
        smallTextPaint.textSize = cellSize * 0.7f

        canvas.drawText("SCORE", panelX, offsetY + cellSize, textPaint)
        canvas.drawText("$score", panelX, offsetY + cellSize * 2, smallTextPaint)

        canvas.drawText("LEVEL", panelX, offsetY + cellSize * 4, textPaint)
        canvas.drawText("$level", panelX, offsetY + cellSize * 5, smallTextPaint)

        canvas.drawText("LINES", panelX, offsetY + cellSize * 7, textPaint)
        canvas.drawText("$linesCleared", panelX, offsetY + cellSize * 8, smallTextPaint)

        canvas.drawText("NEXT", panelX, offsetY + cellSize * 10, textPaint)

        // Draw next piece
        nextPiece?.let { piece ->
            val shape = piece.getShape(0)
            val nextOffsetX = panelX
            val nextOffsetY = offsetY + cellSize * 11
            val nextCell = cellSize * 0.7f
            for (row in shape.indices) {
                for (col in shape[row].indices) {
                    if (shape[row][col] != 0) {
                        paint.color = colors[shape[row][col]]
                        val left = nextOffsetX + col * nextCell
                        val top = nextOffsetY + row * nextCell
                        canvas.drawRoundRect(left, top, left + nextCell - 2, top + nextCell - 2, 8f, 8f, paint)
                    }
                }
            }
        }

        // Game Over overlay
        if (gameOver) {
            paint.color = Color.parseColor("#AA000000")
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
            textPaint.textSize = 72f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("GAME OVER", width / 2f, height / 2f - 40, textPaint)
            smallTextPaint.textSize = 40f
            smallTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Tap to restart", width / 2f, height / 2f + 40, smallTextPaint)
            textPaint.textAlign = Paint.Align.LEFT
            smallTextPaint.textAlign = Paint.Align.LEFT
        }
    }

    private fun drawCell(canvas: Canvas, x: Int, y: Int, colorIndex: Int) {
        if (y < 0) return
        paint.color = colors[colorIndex]
        val left = offsetX + x * cellSize + 1
        val top = offsetY + y * cellSize + 1
        val right = left + cellSize - 2
        val bottom = top + cellSize - 2
        canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, paint)

        // Highlight
        paint.color = Color.argb(80, 255, 255, 255)
        canvas.drawRoundRect(left, top, right, top + cellSize * 0.25f, 6f, 6f, paint)
    }

    // ==================== THREAD & SURFACE ====================

    override fun surfaceCreated(holder: SurfaceHolder) {
        resetGame()
        running = true
        thread = Thread(this)
        thread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        running = false
        try {
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    override fun run() {
        while (running) {
            if (!holder.surface.isValid) continue

            val now = System.currentTimeMillis()
            if (!gameOver && !paused && now - lastDropTime >= dropInterval) {
                if (!move(0, 1)) {
                    lockPiece()
                }
                lastDropTime = now
            }

            val canvas = holder.lockCanvas()
            if (canvas != null) {
                try {
                    drawBoard(canvas)
                } finally {
                    holder.unlockCanvasAndPost(canvas)
                }
            }

            try {
                Thread.sleep(16) // ~60 FPS
            } catch (e: InterruptedException) {
                break
            }
        }
    }

    // ==================== TOUCH CONTROLS ====================

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                touchStartX = event.x
                touchStartY = event.y
                touchStartTime = System.currentTimeMillis()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (gameOver) {
                    resetGame()
                    return true
                }

                val dx = event.x - touchStartX
                val dy = event.y - touchStartY
                val duration = System.currentTimeMillis() - touchStartTime

                // Hard drop on quick double-tap area or long press simulation via distance
                if (abs(dx) < 30 && abs(dy) < 30 && duration < 200) {
                    // Simple tap - soft drop one step or ignore
                    return true
                }

                if (abs(dx) > abs(dy)) {
                    // Horizontal swipe
                    if (dx > 50) move(1, 0)
                    else if (dx < -50) move(-1, 0)
                } else {
                    // Vertical swipe
                    if (dy < -50) rotate()
                    else if (dy > 80) softDrop()
                }

                // Hard drop if swipe is very long downward
                if (dy > 250) hardDrop()

                return true
            }
        }
        return super.onTouchEvent(event)
    }
}
