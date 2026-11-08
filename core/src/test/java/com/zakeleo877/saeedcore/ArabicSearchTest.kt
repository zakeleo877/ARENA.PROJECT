package com.zakeleo877.saeedcore

import com.zakeleo877.saeedcore.util.ArabicText
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * A customer finds a product by typing a word, not by typing the exact spelling
 * the shop used.
 *
 * Phone keyboards produce the bare alef by default, so a search for «ارز» has to
 * find a product written «أرز». The same holds for the final yaa, the taa
 * marbuta and the harakat. Every case here is a customer who would otherwise
 * believe the shop does not sell the thing.
 */
class ArabicSearchTest {

    @Test
    fun `an alef with a hamza is found by the bare alef`() {
        assertTrue(ArabicText.contains("أرز أبيض", "ارز"))
        assertTrue(ArabicText.contains("إبريق", "ابريق"))
        assertTrue(ArabicText.contains("آلة قهوة", "الة"))
    }

    @Test
    fun `the bare alef is found by an alef with a hamza`() {
        // The reverse happens too: the shop wrote it plainly, the customer typed
        // the form their keyboard offered.
        assertTrue(ArabicText.contains("ارز", "أرز"))
    }

    @Test
    fun `a final yaa is found written either way`() {
        assertTrue(ArabicText.contains("مصطفى", "مصطفي"))
        assertTrue(ArabicText.contains("مصطفي", "مصطفى"))
    }

    @Test
    fun `a taa marbuta is found written as a haa`() {
        assertTrue(ArabicText.contains("فاكهة طازجة", "فاكهه"))
        assertTrue(ArabicText.contains("فاكهه", "فاكهة"))
    }

    @Test
    fun `harakat do not have to be typed`() {
        assertTrue(ArabicText.contains("سُكّر", "سكر"))
        assertTrue(ArabicText.contains("مَاء", "ماء"))
    }

    @Test
    fun `tatweel is ignored`() {
        assertTrue(ArabicText.contains("جميـــل", "جميل"))
    }

    @Test
    fun `a search that should not match still does not`() {
        assertFalse(ArabicText.contains("أرز", "سكر"))
        assertFalse(ArabicText.contains("ماء", "ماء ورد"))
        // A prefix does match: someone typing «تفا» is on the way to «تفاح» and
        // expecting to see it. The rule is containment, not whole words.
        assertTrue(ArabicText.contains("تفاح أحمر", "تفا"))
    }

    @Test
    fun `an empty search matches everything, and a blank one is not a filter`() {
        assertTrue(ArabicText.contains("أي شيء", ""))
        assertTrue(ArabicText.contains("أي شيء", "   "))
    }

    @Test
    fun `latin text keeps its case-insensitive behaviour`() {
        assertTrue(ArabicText.contains("Rice 1kg", "rice"))
        assertTrue(ArabicText.contains("rice", "RICE"))
    }
}
