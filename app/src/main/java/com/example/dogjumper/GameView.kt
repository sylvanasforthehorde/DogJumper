package com.example.dogjumper

import android.content.Context
import android.graphics.*
import android.media.MediaPlayer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.MotionEvent
import kotlin.random.Random

class GameView(context: Context) : SurfaceView(context), Runnable {

    private var thread: Thread? = null
    private var isPlaying = false
    private val surfaceHolder = holder
    private val paint = Paint()

    private val background: Bitmap = BitmapFactory.decodeResource(resources, R.drawable.background)
    private lateinit var scaledBackground: Bitmap
    private var backgroundX = 0f
    private val backgroundSpeed = 5f

    private var dog: Bitmap = Bitmap.createScaledBitmap(
        BitmapFactory.decodeResource(resources, R.drawable.dog),
        200, 200, true
    )

    private var dogX = 0f
    private var dogY = 0f
    private var dogVelocity = 0f
    private val gravity = 2.5f
    private val jumpPower = -45f
    private var jumpCount = 0

    private var mediaPlayer: MediaPlayer? = null

    private val meatballBitmap: Bitmap = Bitmap.createScaledBitmap(
        BitmapFactory.decodeResource(resources, R.drawable.meatball),
        100, 100, true
    )

    private val meatballs = mutableListOf<RectF>()
    private var score = 0
    private var meatballSpawnCounter = 0
    private val spawnInterval = 60

    override fun run() {
        while (isPlaying) {
            update()
            draw()
            sleep()
        }
    }

    private fun update() {
        dogVelocity += gravity
        dogY += dogVelocity
        dogX = (width / 2 - dog.width / 2).toFloat()

        val groundY = (height - dog.height - 370)
        if (dogY > groundY) {
            dogY = groundY.toFloat()
            dogVelocity = 0f
            jumpCount = 0
        }

        backgroundX -= backgroundSpeed
        if (backgroundX <= -width) {
            backgroundX = 0f
        }

        meatballSpawnCounter++
        if (meatballSpawnCounter >= spawnInterval) {
            spawnMeatball()
            meatballSpawnCounter = 0
        }

        val iterator = meatballs.iterator()
        while (iterator.hasNext()) {
            val meatball = iterator.next()
            meatball.left -= backgroundSpeed
            meatball.right -= backgroundSpeed

            if (RectF(dogX, dogY, dogX + dog.width, dogY + dog.height).intersect(meatball)) {
                iterator.remove()
                score++
            } else if (meatball.right < 0) {
                iterator.remove()
            }
        }
    }

    private fun draw() {
        if (!surfaceHolder.surface.isValid || width == 0 || height == 0) return

        val canvas = surfaceHolder.lockCanvas() ?: return

        if (!::scaledBackground.isInitialized && width > 0 && height > 0) {
            scaledBackground = Bitmap.createScaledBitmap(background, width, height, false)
        }

        canvas.drawBitmap(scaledBackground, backgroundX, 0f, paint)
        canvas.drawBitmap(scaledBackground, backgroundX + width, 0f, paint)

        canvas.drawBitmap(dog, dogX, dogY, paint)

        for (meatball in meatballs) {
            canvas.drawBitmap(meatballBitmap, null, meatball, paint)
        }

        paint.color = Color.WHITE
        paint.textSize = 60f
        canvas.drawText("Score: $score", 50f, 100f, paint)

        surfaceHolder.unlockCanvasAndPost(canvas)
    }

    private fun spawnMeatball() {
        val groundY = (height - dog.height - 370)
        val jumpHeight = 250f
        val levels = listOf(
            groundY.toFloat(),
            groundY - jumpHeight,
            groundY - jumpHeight * 2
        )
        val y = levels.random()
        val rect = RectF(
            width.toFloat(),
            y,
            width + 100f,
            y + 100f
        )
        meatballs.add(rect)
    }

    private fun sleep() {
        try {
            Thread.sleep(17)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }

    fun resume() {
        isPlaying = true
        thread = Thread(this)
        thread?.start()

        mediaPlayer = MediaPlayer.create(context, R.raw.game_music)
        mediaPlayer?.isLooping = true
        mediaPlayer?.start()
    }

    fun pause() {
        try {
            isPlaying = false
            thread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }

        mediaPlayer?.pause()
        mediaPlayer?.release()
        mediaPlayer = null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN && jumpCount < 2) {
            dogVelocity = jumpPower
            jumpCount++
        }
        return true
    }
}
