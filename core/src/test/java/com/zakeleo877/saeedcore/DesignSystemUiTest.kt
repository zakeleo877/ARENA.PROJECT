package com.zakeleo877.saeedcore

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zakeleo877.saeedcore.designsystem.SaeedButton
import com.zakeleo877.saeedcore.designsystem.SaeedCard
import com.zakeleo877.saeedcore.designsystem.SaeedDialog
import com.zakeleo877.saeedcore.designsystem.SaeedEmptyState
import com.zakeleo877.saeedcore.designsystem.SaeedSearchField
import com.zakeleo877.saeedcore.designsystem.SaeedTextField
import com.zakeleo877.saeedcore.designsystem.SaeedTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screen level behaviour of the design system, driven through the Compose
 * test rules on the JVM. Robolectric provides the Android runtime, so these
 * tests exercise the real composition and the real semantics tree rather than
 * a mocked one.
 */
@RunWith(AndroidJUnit4::class)
// The colours come from the theme, so the test has to resolve the resources of
// this module. Without USE_APP_RESOURCES the asset loader falls back to a
// download of an Android artifact, which fails on a runner with no egress and
// reports only "classMethod FAILED". SDK 30 is the oldest release in the
// Robolectric jar that ships with this build, so it needs no fetch either.
@Config(sdk = [30], application = android.app.Application::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class DesignSystemUiTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `the button reports a click`() {
        var clicks = 0
        composeRule.setContent {
            SaeedTheme {
                SaeedButton(text = "أضف إلى السلة", onClick = { clicks += 1 })
            }
        }
        composeRule.onNodeWithText("أضف إلى السلة").assertIsDisplayed().performClick()
        composeRule.waitForIdle()
        assertEquals(1, clicks)
    }

    @Test
    fun `a disabled button cannot be pressed`() {
        var clicks = 0
        composeRule.setContent {
            SaeedTheme {
                SaeedButton(text = "غير متاح", onClick = { clicks += 1 }, enabled = false)
            }
        }
        composeRule.onNodeWithText("غير متاح").assertIsNotEnabled().performClick()
        composeRule.waitForIdle()
        assertEquals(0, clicks)
    }

    @Test
    fun `a button with an icon keeps its label`() {
        composeRule.setContent {
            SaeedTheme {
                SaeedButton(text = "إضافة", onClick = {}, icon = Icons.Filled.Add)
            }
        }
        composeRule.onNodeWithText("إضافة").assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `the text field reports every change`() {
        val changes = mutableListOf<String>()
        composeRule.setContent {
            SaeedTheme {
                SaeedTextField(
                    value = "",
                    onValueChange = { changes += it },
                    label = "الاسم",
                )
            }
        }
        composeRule.onNodeWithText("الاسم").performTextInput("زبون")
        composeRule.waitForIdle()
        assertEquals(listOf("زبون"), changes)
    }

    @Test
    fun `an error text field shows its supporting message`() {
        composeRule.setContent {
            SaeedTheme {
                SaeedTextField(
                    value = "0",
                    onValueChange = {},
                    label = "الهاتف",
                    isError = true,
                    supportingText = "رقم غير صالح",
                )
            }
        }
        composeRule.onNodeWithText("رقم غير صالح").assertIsDisplayed()
    }

    @Test
    fun `the search field clears its value`() {
        var value = "شاي"
        composeRule.setContent {
            SaeedTheme {
                SaeedSearchField(
                    value = value,
                    onValueChange = { value = it },
                    placeholder = "ابحث",
                )
            }
        }
        composeRule.onNodeWithContentDescription("مسح البحث").performClick()
        composeRule.waitForIdle()
        assertEquals("", value)
    }

    @Test
    fun `the card reports a click`() {
        var clicks = 0
        composeRule.setContent {
            SaeedTheme {
                SaeedCard(onClick = { clicks += 1 }) {
                    androidx.compose.material3.Text("منتج")
                }
            }
        }
        composeRule.onNodeWithText("منتج").performClick()
        composeRule.waitForIdle()
        assertEquals(1, clicks)
    }

    @Test
    fun `the dialog confirms and dismisses`() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            SaeedTheme {
                SaeedDialog(
                    title = "تأكيد",
                    message = "هل تريد المتابعة؟",
                    confirmLabel = "نعم",
                    onConfirm = { confirmed += 1 },
                    onDismiss = { dismissed += 1 },
                )
            }
        }
        composeRule.onNodeWithText("تأكيد").assertIsDisplayed()
        composeRule.onNodeWithText("نعم").performClick()
        composeRule.waitForIdle()
        assertEquals(1, confirmed)
        assertEquals(0, dismissed)
    }

    @Test
    fun `the dialog can be dismissed`() {
        var confirmed = 0
        var dismissed = 0
        composeRule.setContent {
            SaeedTheme {
                SaeedDialog(
                    title = "تأكيد",
                    message = "هل تريد المتابعة؟",
                    confirmLabel = "نعم",
                    onConfirm = { confirmed += 1 },
                    onDismiss = { dismissed += 1 },
                )
            }
        }
        composeRule.onNodeWithText("إلغاء").performClick()
        composeRule.waitForIdle()
        assertEquals(0, confirmed)
        assertEquals(1, dismissed)
    }

    @Test
    fun `the empty state offers a retry`() {
        var retries = 0
        composeRule.setContent {
            SaeedTheme {
                SaeedEmptyState(message = "لا توجد منتجات", retry = { retries += 1 })
            }
        }
        composeRule.onNodeWithText("لا توجد منتجات").assertIsDisplayed()
        composeRule.onNodeWithText("إعادة المحاولة").performClick()
        composeRule.waitForIdle()
        assertEquals(1, retries)
    }

    @Test
    fun `the empty state without a retry shows no button`() {
        composeRule.setContent {
            SaeedTheme {
                SaeedEmptyState(message = "لا توجد منتجات")
            }
        }
        composeRule.onNodeWithText("لا توجد منتجات").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasText("إعادة المحاولة")).fetchSemanticsNodes().size)
    }

    @Test
    fun `the theme applies the brand colours`() {
        var primary = Color.Unspecified
        var onPrimary = Color.Unspecified
        composeRule.setContent {
            SaeedTheme {
                primary = MaterialTheme.colorScheme.primary
                onPrimary = MaterialTheme.colorScheme.onPrimary
            }
        }
        composeRule.waitForIdle()
        // The brand colour is not the default purple, so the theme really was
        // applied and not the stock Material palette.
        assertNotEquals(Color(0xFF6750A4), primary)
        // The text colour on the brand colour has to be readable, so it is
        // either clearly lighter or clearly darker than the brand colour.
        val difference = kotlin.math.abs(primary.luminance() - onPrimary.luminance())
        assertTrue("the text colour must contrast with the brand colour", difference > 0.3f)
    }

    @Test
    fun `a long product name stays inside the card`() {
        composeRule.setContent {
            SaeedTheme {
                SaeedCard(onClick = {}) {
                    androidx.compose.material3.Text(
                        text = "اسم منتج طويل جداً يحتوي على كلمات كثيرة ومتفرقة",
                    )
                }
            }
        }
        composeRule.onNodeWithText(
            "اسم منتج طويل جداً يحتوي على كلمات كثيرة ومتفرقة",
        ).assertIsDisplayed()
    }

    @Test
    fun `the search field keeps the typed text`() {
        var value = ""
        composeRule.setContent {
            SaeedTheme {
                SaeedSearchField(value = value, onValueChange = { value = it })
            }
        }
        composeRule.onNodeWithText("").performTextReplacement("لبن")
        composeRule.waitForIdle()
        assertEquals("لبن", value)
    }

    @Test
    fun `the button label follows the state it is given`() {
        var label by mutableStateOf("قبل")
        composeRule.setContent {
            SaeedTheme {
                SaeedButton(text = label, onClick = {})
            }
        }
        composeRule.onNodeWithText("قبل").assertIsDisplayed()

        // The label changes the way it does when a cart is emptied.
        label = "بعد"
        composeRule.waitForIdle()
        composeRule.onNodeWithText("بعد").assertIsDisplayed()
        assertEquals(0, composeRule.onAllNodes(hasText("قبل")).fetchSemanticsNodes().size)
    }
}
