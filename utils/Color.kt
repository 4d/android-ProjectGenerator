
import java.lang.IllegalArgumentException
import java.util.HashMap

import java.util.Locale
import kotlin.math.min
import kotlin.math.roundToInt


/**
 * The Color class defines methods for creating and converting color ints.
 * Colors are represented as packed ints, made up of 4 bytes: alpha, red,
 * green, blue. The values are unpremultiplied, meaning any transparency is
 * stored solely in the alpha component, and not in the color components. The
 * components are stored as follows (alpha << 24) | (red << 16) |
 * (green << 8) | blue. Each component ranges between 0..255 with 0
 * meaning no contribution for that component, and 255 meaning 100%
 * contribution. Thus opaque-black would be 0xFF000000 (100% opaque but
 * no contributions from red, green, or blue), and opaque-white would be
 * 0xFFFFFFFF
 */
object Color {
    const val BLACK = -0x1000000
    const val DKGRAY = -0xbbbbbc
    const val GRAY = -0x777778
    const val LTGRAY = -0x333334
    const val WHITE = -0x1
    const val RED = -0x10000
    const val GREEN = -0xff0100
    const val BLUE = -0xffff01
    const val YELLOW = -0x100
    const val CYAN = -0xff0001
    const val MAGENTA = -0xff01
    const val TRANSPARENT = 0

    /**
     * Return the alpha component of a color int. This is the same as saying
     * color >>> 24
     */
    fun alpha(color: Int): Int {
        return color ushr 24
    }

    /**
     * Return the red component of a color int. This is the same as saying
     * (color >> 16) & 0xFF
     */
    fun red(color: Int): Int {
        return color shr 16 and 0xFF
    }

    /**
     * Return the green component of a color int. This is the same as saying
     * (color >> 8) & 0xFF
     */
    fun green(color: Int): Int {
        return color shr 8 and 0xFF
    }

    /**
     * Return the blue component of a color int. This is the same as saying
     * color & 0xFF
     */
    fun blue(color: Int): Int {
        return color and 0xFF
    }

    /**
     * Return a color-int from red, green, blue components.
     * The alpha component is implicity 255 (fully opaque).
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     * @param red  Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    fun rgb(red: Int, green: Int, blue: Int): Int {
        return 0xFF shl 24 or (red shl 16) or (green shl 8) or blue
    }

    /**
     * Return a color-int from alpha, red, green, blue components.
     * These component values should be [0..255], but there is no
     * range check performed, so if they are out of range, the
     * returned color is undefined.
     * @param alpha Alpha component [0..255] of the color
     * @param red   Red component [0..255] of the color
     * @param green Green component [0..255] of the color
     * @param blue  Blue component [0..255] of the color
     */
    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return alpha shl 24 or (red shl 16) or (green shl 8) or blue
    }

    /**
     * Returns the hue component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    fun hue(color: Int): Float {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        val V = Math.max(b, Math.max(r, g))
        val temp = Math.min(b, Math.min(r, g))
        var H: Float
        if (V == temp) {
            H = 0f
        } else {
            val vtemp = (V - temp).toFloat()
            val cr = (V - r) / vtemp
            val cg = (V - g) / vtemp
            val cb = (V - b) / vtemp
            H = if (r == V) {
                cb - cg
            } else if (g == V) {
                2 + cr - cb
            } else {
                4 + cg - cr
            }
            H /= 6f
            if (H < 0) {
                H++
            }
        }
        return H
    }

    /**
     * Returns the saturation component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    fun saturation(color: Int): Float {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        val V = Math.max(b, Math.max(r, g))
        val temp = Math.min(b, Math.min(r, g))
        val S: Float
        S = if (V == temp) {
            0f
        } else {
            (V - temp) / V.toFloat()
        }
        return S
    }

    /**
     * Returns the brightness component of a color int.
     *
     * @return A value between 0.0f and 1.0f
     *
     * @hide Pending API council
     */
    fun brightness(color: Int): Float {
        val r = color shr 16 and 0xFF
        val g = color shr 8 and 0xFF
        val b = color and 0xFF
        val V = Math.max(b, Math.max(r, g))
        return V / 255f
    }

    /**
     * Parse the color string, and return the corresponding color-int.
     * If the string cannot be parsed, throws an IllegalArgumentException
     * exception. Supported formats are:
     * #RRGGBB
     * #AARRGGBB
     * 'red', 'blue', 'green', 'black', 'white', 'gray', 'cyan', 'magenta',
     * 'yellow', 'lightgray', 'darkgray', 'grey', 'lightgrey', 'darkgrey',
     * 'aqua', 'fuschia', 'lime', 'maroon', 'navy', 'olive', 'purple',
     * 'silver', 'teal'
     */
    fun parseColor(colorString: String): Int {
        if (colorString[0] == '#') {
            // Use a long to avoid rollovers on #ffXXXXXX
            var color = colorString.substring(1).toLong(16)
            if (colorString.length == 7) {
                // Set the alpha value
                color = color or -0x1000000
            } else require(colorString.length == 9) { "Unknown color" }
            return color.toInt()
        } else {
            val color = sColorNameMap[colorString.toLowerCase(Locale.ROOT)]
            if (color != null) {
                return color
            }
        }
        throw IllegalArgumentException("Unknown color")
    }

    /**
     * Convert HSB components to an ARGB color. Alpha set to 0xFF.
     * hsv[0] is Hue [0 .. 1)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param hsb  3 element array which holds the input HSB components.
     * @return the resulting argb color
     *
     * @hide Pending API council
     */
    fun HSBtoColor(hsb: FloatArray): Int {
        return HSBtoColor(hsb[0], hsb[1], hsb[2])
    }

    /**
     * Convert HSB components to an ARGB color. Alpha set to 0xFF.
     * hsv[0] is Hue [0 .. 1)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param h Hue component
     * @param s Saturation component
     * @param b Brightness component
     * @return the resulting argb color
     *
     * @hide Pending API council
     */
    fun HSBtoColor(h: Float, s: Float, b: Float): Int {
        var h = h
        var s = s
        var b = b
        h = h.coerceIn(0.0f, 1.0f)
        s = s.coerceIn(0.0f, 1.0f)
        b = b.coerceIn(0.0f, 1.0f)
        var red = 0.0f
        var green = 0.0f
        var blue = 0.0f
        val hf = (h - h.toInt()) * 6.0f
        val ihf = hf.toInt()
        val f = hf - ihf
        val pv = b * (1.0f - s)
        val qv = b * (1.0f - s * f)
        val tv = b * (1.0f - s * (1.0f - f))
        when (ihf) {
            0 -> {
                red = b
                green = tv
                blue = pv
            }
            1 -> {
                red = qv
                green = b
                blue = pv
            }
            2 -> {
                red = pv
                green = b
                blue = tv
            }
            3 -> {
                red = pv
                green = qv
                blue = b
            }
            4 -> {
                red = tv
                green = pv
                blue = b
            }
            5 -> {
                red = b
                green = pv
                blue = qv
            }
        }
        return -0x1000000 or ((red * 255.0f).toInt() shl 16) or
                ((green * 255.0f).toInt() shl 8) or (blue * 255.0f).toInt()
    }

    /**
     * Convert RGB components to HSV.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * @param red  red component value [0..255]
     * @param green  green component value [0..255]
     * @param blue  blue component value [0..255]
     * @param hsv  3 element array which holds the resulting HSV components.
     */
    fun RGBToHSV(red: Int, green: Int, blue: Int, hsv: FloatArray) {
        if (hsv.size < 3) {
            throw RuntimeException("3 components required for hsv")
        }
        nativeRGBToHSV(red, green, blue, hsv)
    }

    /**
     * Convert the argb color to its HSV components.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * @param color the argb color to convert. The alpha component is ignored.
     * @param hsv  3 element array which holds the resulting HSV components.
     */
    fun colorToHSV(color: Int, hsv: FloatArray) {
        RGBToHSV(color shr 16 and 0xFF, color shr 8 and 0xFF, color and 0xFF, hsv)
    }

    /**
     * Convert HSV components to an ARGB color. Alpha set to 0xFF.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param hsv  3 element array which holds the input HSV components.
     * @return the resulting argb color
     */
    fun HSVToColor(hsv: FloatArray): Int {
        return HSVToColor(0xFF, hsv)
    }

    /**
     * Convert HSV components to an ARGB color. The alpha component is passed
     * through unchanged.
     * hsv[0] is Hue [0 .. 360)
     * hsv[1] is Saturation [0...1]
     * hsv[2] is Value [0...1]
     * If hsv values are out of range, they are pinned.
     * @param alpha the alpha component of the returned argb color.
     * @param hsv  3 element array which holds the input HSV components.
     * @return the resulting argb color
     */
    fun HSVToColor(alpha: Int, hsv: FloatArray): Int {
        if (hsv.size < 3) {
            throw RuntimeException("3 components required for hsv")
        }
        return nativeHSVToColor(alpha, hsv)
    }

    private external fun nativeRGBToHSV(red: Int, greed: Int, blue: Int, hsv: FloatArray)
    private external fun nativeHSVToColor(alpha: Int, hsv: FloatArray): Int

    /**
     * Converts an HTML color (named or numeric) to an integer RGB value.
     *
     * @param color Non-null color string.
     *
     * @return A color value, or `-1` if the color string could not be interpreted.
     *
     * @hide
     */
   /* fun getHtmlColor(color: String): Int {
        val i = sColorNameMap!![color.toLowerCase(Locale.ROOT)]
        return i
            ?: try {
                XmlUtils.convertValueToInt(color, -1)
            } catch (nfe: NumberFormatException) {
                -1
            }
    }*/

    private var sColorNameMap: HashMap<String, Int> = HashMap()

    init {
        sColorNameMap["black"] = BLACK
        sColorNameMap["darkgray"] = DKGRAY
        sColorNameMap["gray"] = GRAY
        sColorNameMap["lightgray"] = LTGRAY
        sColorNameMap["white"] = WHITE
        sColorNameMap["red"] = RED
        sColorNameMap["green"] = GREEN
        sColorNameMap["blue"] = BLUE
        sColorNameMap["yellow"] = YELLOW
        sColorNameMap["cyan"] = CYAN
        sColorNameMap["magenta"] = MAGENTA
        sColorNameMap["aqua"] = 0x00FFFF
        sColorNameMap["fuchsia"] = 0xFF00FF
        sColorNameMap["darkgrey"] = DKGRAY
        sColorNameMap["grey"] = GRAY
        sColorNameMap["lightgrey"] = LTGRAY
        sColorNameMap["lime"] = 0x00FF00
        sColorNameMap["maroon"] = 0x800000
        sColorNameMap["navy"] = 0x000080
        sColorNameMap["olive"] = 0x808000
        sColorNameMap["purple"] = 0x800080
        sColorNameMap["silver"] = 0xC0C0C0
        sColorNameMap["teal"] = 0x008080
    }
}

fun manipulateColor(color: Int, factor: Float): Int {
    val a: Int = Color.alpha(color)
    val r = (Color.red(color) * factor).roundToInt()
    val g = (Color.green(color) * factor).roundToInt()
    val b = (Color.blue(color) * factor).roundToInt()
    return Color.argb(
        a,
        min(r, 255),
        min(g, 255),
        min(b, 255)
    )
}