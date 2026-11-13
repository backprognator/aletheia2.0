@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package aletheia.archive.ui

import aletheia.archive.R
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object AletheiaColor {
    val Ink = Color(0xFF100E0C)
    val InkRaised = Color(0xFF1C1915)
    val Rule = Color(0xFF3A342C)
    val Brass = Color(0xFFC6A37A)
    val BrassInk = Color(0xFF6E5436)
    val Paper = Color(0xFFEFE7DA)
    val PaperInk = Color(0xFF1C1814)
    val Parchment = Color(0xFFD7CDBD)
    val Faded = Color(0xFF8F867B)
    val Seal = Color(0xFFD46558)
}

val Newsreader = FontFamily(
    Font(
        R.font.newsreader,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(430),
            FontVariation.opticalSizing(18.sp),
        ),
    ),
    Font(
        R.font.newsreader,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(560),
            FontVariation.opticalSizing(28.sp),
        ),
    ),
    Font(
        R.font.newsreader,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(
            FontVariation.weight(640),
            FontVariation.opticalSizing(40.sp),
        ),
    ),
)

val Outfit = FontFamily(
    Font(
        R.font.outfit,
        FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(420)),
    ),
    Font(
        R.font.outfit,
        FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(560)),
    ),
    Font(
        R.font.outfit,
        FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(640)),
    ),
)

val Wordmark = TextStyle(
    fontFamily = Newsreader,
    fontWeight = FontWeight.Medium,
    fontSize = 40.sp,
    lineHeight = 44.sp,
    color = AletheiaColor.Paper,
    letterSpacing = (-0.6).sp,
)

val HerBody = TextStyle(
    fontFamily = Newsreader,
    fontWeight = FontWeight.Normal,
    fontSize = 18.sp,
    lineHeight = 28.sp,
    color = AletheiaColor.PaperInk,
)

val YourBody = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight.Normal,
    fontSize = 16.sp,
    lineHeight = 24.sp,
    color = AletheiaColor.Parchment,
)

val Label = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight.Medium,
    fontSize = 12.sp,
    lineHeight = 16.sp,
    letterSpacing = 1.2.sp,
    color = AletheiaColor.Brass,
)

val Meta = TextStyle(
    fontFamily = Outfit,
    fontWeight = FontWeight.Normal,
    fontSize = 13.sp,
    lineHeight = 18.sp,
    color = AletheiaColor.Faded,
)

private val Scheme = darkColorScheme(
    primary = AletheiaColor.Brass,
    onPrimary = AletheiaColor.Ink,
    background = AletheiaColor.Ink,
    onBackground = AletheiaColor.Paper,
    surface = AletheiaColor.InkRaised,
    onSurface = AletheiaColor.Paper,
    surfaceContainer = AletheiaColor.InkRaised,
    error = AletheiaColor.Seal,
    outline = AletheiaColor.Rule,
)

@Composable
fun AletheiaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Scheme, content = content)
}

@Composable
fun inkFieldColors() = TextFieldDefaults.colors(
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    focusedIndicatorColor = AletheiaColor.Brass,
    unfocusedIndicatorColor = AletheiaColor.Rule,
    disabledIndicatorColor = AletheiaColor.Rule,
    cursorColor = AletheiaColor.Brass,
    focusedTextColor = AletheiaColor.Paper,
    unfocusedTextColor = AletheiaColor.Paper,
    disabledTextColor = AletheiaColor.Faded,
    focusedPlaceholderColor = AletheiaColor.Faded,
    unfocusedPlaceholderColor = AletheiaColor.Faded,
    focusedLabelColor = AletheiaColor.Brass,
    unfocusedLabelColor = AletheiaColor.Faded,
)
