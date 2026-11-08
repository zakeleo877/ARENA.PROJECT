package com.zakeleo877.saeedcore.util

/**
 * Folds Arabic text to the shape a person searches with.
 *
 * A product is entered by hand, so it carries whatever form the writer chose:
 * «أرز» with a hamza, «ارز» without one. A customer typing «ارز» on a phone
 * keyboard, whose default key is the bare alef, found nothing and concluded the
 * shop did not sell rice. The same happens with the final yaa («مصطفى» against
 * «مصطفي»), with the taa marbuta («فاكهة» against «فاكهه») and with the harakat
 * that a careful writer leaves in.
 *
 * Folding removes the difference without changing what is stored: the original
 * spelling is what the shop shows, and only the comparison is folded.
 */
object ArabicText {

    /** Harakat, tanween, shadda, sukun and the superscript alef. */
    private val DIACRITICS = Regex("[\\u064B-\\u0652\\u0670\\u0653-\\u0655]")

    /** Tatweel, the decorative letter-stretching character. */
    private val TATWEEL = Regex("\\u0640")

    /**
     * The form used for comparing. Two strings that fold to the same text are
     * treated as the same word.
     */
    fun fold(input: String): String {
        val lowered = input.lowercase()
        val builder = StringBuilder(lowered.length)
        for (c in lowered) {
            builder.append(
                when (c) {
                    // Every alef shape becomes the bare alef.
                    '\u0623', '\u0625', '\u0622', '\u0671' -> '\u0627'
                    // A final yaa written as alef maqsura becomes the yaa.
                    '\u0649' -> '\u064A'
                    // The taa marbuta is often typed as a haa.
                    '\u0629' -> '\u0647'
                    // Persian and Urdu forms that appear when a keyboard is
                    // switched by mistake.
                    '\u06A9' -> '\u0643'
                    '\u06CC' -> '\u064A'
                    else -> c
                },
            )
        }
        return TATWEEL.replace(DIACRITICS.replace(builder.toString(), ""), "")
    }

    /** True when [haystack] contains [needle] after both are folded. */
    fun contains(haystack: String, needle: String): Boolean {
        if (needle.isBlank()) return true
        return fold(haystack).contains(fold(needle))
    }
}
