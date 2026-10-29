package io.github.igornunesfontoura.yourfinances.ui.format

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import io.github.igornunesfontoura.yourfinances.domain.money.centsFromAmountDigits

/** Shows cash-register digits as currency; the cursor always stays at the end. */
object AmountVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val formatted = formatCents(centsFromAmountDigits(text.text))
        val mapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int) = formatted.length
            override fun transformedToOriginal(offset: Int) = text.length
        }
        return TransformedText(AnnotatedString(formatted), mapping)
    }
}
