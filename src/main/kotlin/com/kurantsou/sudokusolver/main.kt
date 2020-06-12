package com.kurantsou.sudokusolver

import java.awt.Color
import java.awt.image.BufferedImage
import java.io.File
import java.lang.Integer.min
import javax.imageio.ImageIO


private const val FRAME_FILE_NAME = "currentFrame.png"

private const val FIELD_START_X = 14
private const val FIELD_START_Y = 306
private const val FIELD_END_X = 1065
private const val FIELD_END_Y = 1357

private val FIELD_POSITIONS =
    arrayOf(5, 117, 121, 232, 236, 348, 354, 466, 470, 581, 585, 697, 703, 815, 819, 930, 934, 1045)

private const val DIGITS_Y = 1980
private val DIGITS_POSITIONS = arrayOf(0, 87, 198, 310, 425, 542, 655, 765, 882, 994)

fun main(args: Array<String>) {
    val sudoku = extractSudoku()
    val solvedSudoku = sudoku.copyOf()
    solveSudoku(solvedSudoku)

    inputSolution(sudoku, solvedSudoku)
}

fun inputSolution(sudoku: IntArray, solvedSudoku: IntArray) {
    for (i in 0 until 9)
        for (j in 0 until 9) {
            if (sudoku[i, j] != 0) continue

            val cellStartX = FIELD_START_X + FIELD_POSITIONS[j * 2]
            val cellStartY = FIELD_START_Y + FIELD_POSITIONS[i * 2]
            val cellEndX = FIELD_START_X + FIELD_POSITIONS[j * 2 + 1]
            val cellEndY = FIELD_START_Y + FIELD_POSITIONS[i * 2 + 1]

            val cellCenterX = (cellStartX + cellEndX) / 2
            val cellCenterY = (cellStartY + cellEndY) / 2

            Runtime.getRuntime().exec("adb shell input tap $cellCenterX $cellCenterY").waitFor()
            Runtime.getRuntime().exec("adb shell input tap ${DIGITS_POSITIONS[solvedSudoku[i, j]]} $DIGITS_Y").waitFor()
        }
}

private fun BufferedImage.countDiffPixels(other: BufferedImage): Int {
    var diffCount = 0
    val minWidth = min(this.width, other.width)
    val minHeight = min(this.height, other.height)
    for (i in 0 until minHeight)
        for (j in 0 until minWidth)
            if (this.getRGB(j, i) != other.getRGB(j, i))
                diffCount++
    return diffCount
}

private fun extractSudoku(): IntArray {
    val fullImage = Runtime.getRuntime().exec("adb exec-out screencap -p").inputStream.readBytes()
    val capturedFile = File(FRAME_FILE_NAME)
    capturedFile.writeBytes(fullImage)

    val binaryImage = ImageIO.read(capturedFile)
        .cropImage()
        .grayoutImage()
        .transformToBinaryImage()

    val cells = arrayOf(
        ImageIO.read(File("cells", "cell_0.png")),
        ImageIO.read(File("cells", "cell_1.png")),
        ImageIO.read(File("cells", "cell_2.png")),
        ImageIO.read(File("cells", "cell_3.png")),
        ImageIO.read(File("cells", "cell_4.png")),
        ImageIO.read(File("cells", "cell_5.png")),
        ImageIO.read(File("cells", "cell_6.png")),
        ImageIO.read(File("cells", "cell_7.png")),
        ImageIO.read(File("cells", "cell_8.png")),
        ImageIO.read(File("cells", "cell_9.png"))
    )

    val matrix = IntArray(81)
    for (i in 0 until 9)
        for (j in 0 until 9) {
            val startX = FIELD_POSITIONS[j * 2]
            val startY = FIELD_POSITIONS[i * 2]
            val endX = FIELD_POSITIONS[j * 2 + 1]
            val endY = FIELD_POSITIONS[i * 2 + 1]
            val cell: BufferedImage =
                binaryImage.getSubimage(startX + (endX - startX - 42) / 2, startY + (endY - startY - 60) / 2, 42, 62)
            val diffs = cells.map { curCell -> cell.countDiffPixels(curCell) }
            matrix[i * 9 + j] = diffs.indexOf(diffs.min())
        }

    return matrix
}

private fun BufferedImage.cropImage(): BufferedImage =
    this.getSubimage(FIELD_START_X, FIELD_START_Y, FIELD_END_X - FIELD_START_X, FIELD_END_Y - FIELD_START_Y)

private fun BufferedImage.grayoutImage(): BufferedImage {
    val grayImage = BufferedImage(this.width, this.height, BufferedImage.TYPE_BYTE_GRAY)
    val graphic = grayImage.createGraphics()
    graphic.drawImage(this, 0, 0, Color.WHITE, null)
    graphic.dispose()
    return grayImage
}

private fun BufferedImage.transformToBinaryImage(): BufferedImage {
    val binaryImage = BufferedImage(this.width, this.height, BufferedImage.TYPE_BYTE_BINARY)

    for (i in 0 until this.width)
        for (j in 0 until this.height)
            binaryImage.setRGB(i, j, (if (Color(this.getRGB(i, j)).red > 160) Color.WHITE else Color.BLACK).rgb)
    return binaryImage
}

private operator fun IntArray.get(i: Int, j: Int): Int = this[i * 9 + j]
private operator fun IntArray.set(i: Int, j: Int, value: Int) {
    this[i * 9 + j] = value
}

private fun solveSudoku(inputMatrix: IntArray): Boolean {
    fun canBePlaced(i: Int, j: Int, value: Int): Boolean {
        for (k in 0 until 9)
            if (inputMatrix[k, j] == value || inputMatrix[i, k] == value)
                return false
        val ti = 3 * (i / 3)
        val tj = 3 * (j / 3)
        for (ii in ti until ti + 3)
            for (jj in tj until tj + 3)
                if (inputMatrix[ii, jj] == value)
                    return false
        return true
    }
    for (i in 0 until 9)
        for (j in 0 until 9) {
            if (inputMatrix[i, j] != 0)
                continue
            for (v in 1..9) {
                if (canBePlaced(i, j, v)) {
                    inputMatrix[i, j] = v
                    if (solveSudoku(inputMatrix)) {
                        return true
                    }
                    inputMatrix[i, j] = 0
                }
            }
            return false
        }
    return true
}

