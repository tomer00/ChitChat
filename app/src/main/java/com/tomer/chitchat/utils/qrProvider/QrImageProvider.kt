package com.tomer.chitchat.utils.qrProvider

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.VectorDrawable
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.toRect
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import com.tomer.chitchat.R
import kotlin.math.cos
import kotlin.math.sin


object QrImageProvider {


    private fun bmpFromSVG(dr: VectorDrawable, dimen: Int, rot: Int): Bitmap {
        val bmp = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.rotate(rot.toFloat(), dimen / 2f, dimen / 2f)
        dr.setBounds(0, 0, dimen, dimen)
        dr.draw(c)
        return bmp
    }

    private fun bmpFromBgAsset(bgAsset: Int, con: Context): Bitmap {
        val bmp = Bitmap.createBitmap(1125, 2436, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val dr = ContextCompat.getDrawable(con, bgAsset)!!
        dr.setBounds(0, 0, 1125, 2436)
        dr.draw(c)
        return bmp
    }

    private fun centerSquareBmp(dimen: Int): Bitmap {
        val bmp = Bitmap.createBitmap(dimen, dimen, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        c.drawRect(Rect(0, 0, dimen, dimen), Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
        })
        return bmp
    }

    private fun getBodyBmp(qrMatrtix: Array<BooleanArray>, unit: Int, x: Int, trail: Int, alone: Int, corner: Int, con: Context): Bitmap {
        val markUpBmp = Bitmap.createBitmap(unit * x, unit * x, Bitmap.Config.ARGB_8888)
        val c = Canvas(markUpBmp)

        val mainRect = RectF()

        val trailVec = ContextCompat.getDrawable(con, AssetsProvider.trailAssets.getOrDefault(trail, R.drawable.q_trail1)) as VectorDrawable
        val cornerVec = ContextCompat.getDrawable(con, AssetsProvider.cornerAssets.getOrDefault(corner, R.drawable.q_corner1)) as VectorDrawable
        val aloneVec = ContextCompat.getDrawable(con, AssetsProvider.aloneAssets.getOrDefault(alone, R.drawable.q_alone1)) as VectorDrawable

        val trailTop: Bitmap = bmpFromSVG(trailVec, unit, 0)
        val trailRight: Bitmap = bmpFromSVG(trailVec, unit, 90)
        val trailBottom: Bitmap = bmpFromSVG(trailVec, unit, 180)
        val trailleft: Bitmap = bmpFromSVG(trailVec, unit, 270)


        val cornerTopLeft: Bitmap = bmpFromSVG(cornerVec, unit, 90)
        val cornerTopRight: Bitmap = bmpFromSVG(cornerVec, unit, 180)
        val cornerBottomLeft: Bitmap = bmpFromSVG(cornerVec, unit, 0)
        val cornerBottomRight: Bitmap = bmpFromSVG(cornerVec, unit, 270)

        val centre: Bitmap = centerSquareBmp(unit)


        //draw simple plane complex
        for (i in 0 until x) for (j in 0 until x) if (qrMatrtix[i][j]) {
            val left = if (j == 0) false
            else qrMatrtix[i][j - 1]

            val top = if (i == 0) false
            else qrMatrtix[i - 1][j]

            val right = if (j == x - 1) false
            else qrMatrtix[i][j + 1]

            val bottom = if (i == x - 1) false
            else qrMatrtix[i + 1][j]


            mainRect[(j * unit).toFloat(), (i * unit).toFloat(), (j * unit).toFloat() + unit] = (i * unit + unit).toFloat()


            if (!top && left && !right) {
                if (bottom) {
                    c.drawBitmap(cornerTopRight, mainRect.left, mainRect.top, null)
                } else {
                    c.drawBitmap(trailRight, mainRect.left, mainRect.top, null)
                }
                continue
            }

            if (!top && !left) {
                if (bottom) {
                    if (right) {
                        c.drawBitmap(cornerTopLeft, mainRect.left, mainRect.top, null)
                    } else {
                        c.drawBitmap(trailTop, mainRect.left, mainRect.top, null)
                    }
                } else {
                    if (right) {
                        c.drawBitmap(trailleft, mainRect.left, mainRect.top, null)
                    } else {
                        aloneVec.bounds = mainRect.toRect()
                        aloneVec.draw(c)
                    }
                }
                continue
            }

            if (top && !left && right && !bottom) {
                c.drawBitmap(cornerBottomLeft, mainRect.left, mainRect.top, null)
                continue
            }
            if (top && left && !right && !bottom) {
                c.drawBitmap(cornerBottomRight, mainRect.left, mainRect.top, null)
                continue
            }
            if (top && !left && !right && !bottom) {
                c.drawBitmap(trailBottom, mainRect.left, mainRect.top, null)
                continue
            }
            c.drawBitmap(centre, mainRect.left, mainRect.top, null)
        }
        return markUpBmp
    }

    private fun drawEyes(bmp: Bitmap, eyeType: Int, unit: Int, x: Int, con: Context) {

        val c = Canvas(bmp)
        val wu = (7 * unit)

        val b = Bitmap.createBitmap(wu, wu, Bitmap.Config.ARGB_8888)
        val ce = Canvas(b)
        val eyeInner = ContextCompat.getDrawable(con, AssetsProvider.eyesAssets.getOrDefault(eyeType, R.drawable.q_eye_1)) as VectorDrawable
        eyeInner.setBounds(0, 0, wu, wu)
        eyeInner.draw(ce)

        var eye: Rect

        // eye TOPLeft
        c.drawBitmap(b, 0f, 0f, null)
        val halfWith = (b.width shr 1).toFloat()

        val m = Matrix()
        m.postScale(-1f, 1f, halfWith, halfWith)
        val unitDiff = x - 14


        // eye TOPRight
        eye = Rect(unit * (7 + unitDiff), 0, unit * (14 + unitDiff), unit * 7)
        c.drawBitmap(Bitmap.createBitmap(b, 0, 0, b.width, b.width, m, true), null, eye, null)


        // eye BottomLeft
        m.postScale(-1f, -1f, halfWith, halfWith)
        eye = Rect(0, unit * (7 + unitDiff), unit * 7, unit * (14 + unitDiff))
        c.drawBitmap(Bitmap.createBitmap(b, 0, 0, b.width, b.width, m, true), null, eye, null)
    }

    private fun shadeQR(grad: List<Int>, bmp: Bitmap, dimen: Float) {
        val shader: Shader = createLinearGradient(bmp.width, bmp.height, grad[0], grad[1], grad[2])
        val paint = Paint()
        paint.isDither = true
        paint.isAntiAlias = true
        paint.blendMode = BlendMode.SRC_ATOP
        paint.setShader(shader)
        val c = Canvas(bmp)
        c.drawRect(RectF(0f, 0f, dimen, dimen), paint)


    }

    //    private fun drawLogo() {
//        val d: Int = (dimen - (unit shl 2)) shr 1
//
//        val destR = Rect(d, d, d + (unit shl 2), d + (unit shl 2))
//        val b: Bitmap = makeLogo()
//        val sorc = Rect(0, 0, b.height, b.width)
//        c.drawBitmap(b, sorc, destR, Paint())
//
//    }

    private fun createLinearGradient(
        width: Int, height: Int,
        angleDegrees: Int,
        startColor: Int, endColor: Int
    ): LinearGradient {

        // Convert angle from degrees to radians
        val angleRadians = Math.toRadians((angleDegrees.mod(360)).toDouble())

        // Calculate the start and end points based on the angle
        val x0 = (width / 2 + width / 2 * cos(angleRadians)).toFloat()
        val y0 = (height / 2 - height / 2 * sin(angleRadians)).toFloat()
        val x1 = (width / 2 - width / 2 * cos(angleRadians)).toFloat()
        val y1 = (height / 2 + height / 2 * sin(angleRadians)).toFloat()

        // Create and return the LinearGradient
        return LinearGradient(
            x0, y0, x1, y1,
            startColor, endColor,
            Shader.TileMode.CLAMP
        )
    }

    private fun genTintedPattern(bgAsset: Int, shader: Shader, dimen: Point, con: Context): Bitmap {
        val tBmp = Bitmap.createBitmap(dimen.x, dimen.y, Bitmap.Config.ARGB_8888)
        val bgmp = bmpFromBgAsset(bgAsset, con)

        val paint = Paint().apply {
            isAntiAlias = true
            setShader(shader)
            blendMode = BlendMode.SRC_ATOP
        }

        val adjustedWidth = 900f.times(con.resources.displayMetrics.densityDpi / 440)
        val adjustedHeight = 1948.8f.times(con.resources.displayMetrics.densityDpi / 440)

        val cM = Canvas(tBmp)
        val w = dimen.x / adjustedWidth.toInt()
        val h = dimen.y / adjustedHeight.toInt()

        val widthExt = adjustedWidth * (w + 1)
        val heightExt = adjustedHeight * (h + 1)

        val xM = (widthExt - dimen.x).toInt() shr 1
        val yM = (heightExt - dimen.y).toInt() shr 1


        val pT = Paint(Paint.ANTI_ALIAS_FLAG)
        val destR = RectF(0f, 0f, adjustedWidth, adjustedHeight)
        for (i in 0..w)
            for (j in 0..h) {
                destR.set((adjustedWidth * i) - xM, (adjustedHeight * j) - yM, (adjustedWidth * (i + 1)) - xM, (adjustedHeight * (j + 1) - yM))
                cM.drawBitmap(bgmp, null, destR, pT)
            }
        cM.drawRect(0f, 0f, dimen.x.toFloat(), dimen.y.toFloat(), paint)
        return tBmp
    }

    private fun drawBgDark(bgAsset: Int, grad: List<Int>, bmp: Bitmap, con: Context) {
        val shader: Shader = createLinearGradient(bmp.width, bmp.height, grad[0] + 180, grad[1], grad[2])

        val c = Canvas(bmp)
        c.drawColor(Color.BLACK)
        c.drawBitmap(genTintedPattern(bgAsset, shader, Point(bmp.width, bmp.height), con), 0f, 0f, null)
    }

    private fun drawBgLight(bgAsset: Int, grad: List<Int>, bmp: Bitmap, con: Context) {
        val rectF = RectF(0f, 0f, bmp.width.toFloat(), bmp.height.toFloat())
        val shader: Shader = createLinearGradient(bmp.width, bmp.height, grad[0] + 180, grad[1], grad[2])

        val c = Canvas(bmp)
        c.drawColor(Color.WHITE)
        c.drawRect(rectF, Paint().apply {
            isAntiAlias = true
            setShader(shader)
            alpha = 110
        })
        c.drawBitmap(genTintedPattern(bgAsset, shader, Point(bmp.width, bmp.height), con), 0f, 0f, null)

    }

    fun getQRBMP(
        data: String, bodyType: Int, gradType: Int, con: Context,
        width: Int, height: Int, name: String, isDark: Boolean, bgAsset: Int
    ): Bitmap {
        Log.d("TAG--", "onCreate: $gradType")
        val grad = AssetsProvider.gradType.getOrDefault(gradType, listOf(45, Color.parseColor("#cb356b"), Color.parseColor("#bd3f32")))
        val mainBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        if (isDark)
            drawBgDark(bgAsset, grad, mainBmp, con)
        else
            drawBgLight(bgAsset, grad, mainBmp, con)

        val c = Canvas(mainBmp)
        val unit = 20
        val mat = getMATRIX(data)


        val size = mat.size * unit
        val p = Paint()
        p.isAntiAlias = true
        p.color = Color.WHITE
        p.style = Paint.Style.FILL

        val h80 = height.times(.8f)
        val h10 = height.times(.1f)

        val cen = width shr 1
        c.drawRoundRect(cen - size.div(2f) - 60f, h10, cen + size.div(2f) + 60f, h80 + h10, 32f, 32f, p)


        val bodyData = AssetsProvider.bodyType.getOrDefault(bodyType, listOf(1, 1, 5, 4))
        val body = getBodyBmp(mat, unit, mat.size, bodyData[2], bodyData[0], bodyData[1], con)
        drawEyes(body, bodyData[3], unit, mat.size, con)
        shadeQR(grad, body, size.toFloat())

        c.drawBitmap(body, cen - size.div(2f), h10 + 60f, null)
        val shader: Shader = createLinearGradient(width, height, grad[0], grad[1], grad[2])
        val textPaint = Paint().apply {
            isAntiAlias = true
            setShader(shader)
            textSize = 68f
            typeface = ResourcesCompat.getFont(con, R.font.nunito_bold)
        }

        val textWidth = textPaint.measureText(name)
        val xt = (width - textWidth).div(2)

        c.drawText(name, xt, h80 + 16, textPaint)

        return mainBmp
    }

    private fun genQr(data: String): BitMatrix {
        val r = QRCodeWriter()
        val b: BitMatrix
        val err: MutableMap<EncodeHintType, ErrorCorrectionLevel?> = HashMap(1)
        err[EncodeHintType.ERROR_CORRECTION] = ErrorCorrectionLevel.M
        b = try {
            r.encode(data, BarcodeFormat.QR_CODE, 120, 120, err)
        } catch (e: WriterException) {
            BitMatrix(4)
        }
        return b
    }

    private fun getMATRIX(data: String): Array<BooleanArray> {
        val bmp = genQr(data)
        val bmpDimen = bmp.width
        val unit: Int
        val topPoint: Int


        // sideways finding the first eye pixel
        var i = 0
        while (i < bmpDimen) {
            if (bmp[i, i]) break
            i++
        }
        topPoint = i

        while (i < bmpDimen) {
            if (!bmp[i, i]) break
            i++
        }
        unit = i - topPoint

        var x = bmpDimen - (topPoint shl 1)
        x /= unit

        val ret = Array(x) { BooleanArray(x) }

        var r = 0
        var c = 0

        val ru = bmpDimen - topPoint - unit + 1

        i = topPoint
        while (i < ru) {
            var j = topPoint
            while (j < ru) {
                if (bmp[i, j]) ret[c][r] = true
                c++
                j += unit
            }
            r++
            c = 0
            i += unit
        }

        //remove eyeTopRight
        for (k in 0..6) for (l in x - 7 until x) ret[l][k] = false


        //remove  eyeTopLeft
        for (k in 0..6) for (l in 0..6) ret[l][k] = false


        //remove eyeBottomLeft
        for (k in x - 7 until x) for (l in 0..6) ret[l][k] = false

        return ret
    }

}