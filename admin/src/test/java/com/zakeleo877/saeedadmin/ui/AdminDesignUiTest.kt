package com.zakeleo877.saeedadmin.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zakeleo877.saeedcore.designsystem.SaeedTheme
import com.zakeleo877.saeedcore.model.Order
import com.zakeleo877.saeedcore.model.OrderChannel
import com.zakeleo877.saeedcore.model.OrderStatus
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** UI tests use only in-memory examples, never a store account or production orders. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], application = android.app.Application::class, qualifiers = "w360dp-h800dp")
class AdminDesignUiTest {
    @get:Rule val composeRule = createComposeRule()

    private val example = Order(
        id = "test-order", number = 42, customer = "زبون تجريبي", phone = "0000000000",
        notes = "", customerLocationUrl = "", channel = OrderChannel.APP, status = OrderStatus.NEW,
        items = emptyList(), total = BigDecimal("125"), clientId = null, createdAt = Instant.EPOCH,
    )

    @Test
    fun `login masks the password and submits the entered credentials`() {
        var submitted = ""
        composeRule.setContent {
            SaeedTheme {
                LoginScreen(loading = false, errorMessage = null, onSignIn = { email, password ->
                    submitted = "$email|$password"
                })
            }
        }
        composeRule.onNodeWithText("البريد الإلكتروني").performTextInput("manager@example.invalid")
        composeRule.onNodeWithText("كلمة المرور").performTextInput("test-password")
        composeRule.onNode(SemanticsMatcher.keyIsDefined(SemanticsProperties.Password)).assertExists()
        composeRule.onNode(hasText("تسجيل الدخول") and hasClickAction()).performClick()
        assertEquals("manager@example.invalid|test-password", submitted)
    }

    @Test
    fun `dashboard shows unfiltered recent orders and reports a failed status update`() {
        var state by mutableStateOf(
            AdminUiState(
                orders = emptyList(), // A previous filter hid it from the order tab.
                allOrders = listOf(example),
                dashboardStats = calculateDashboardStats(listOf(example), emptyList()),
                ordersLoaded = true,
                productsLoaded = true,
            ),
        )
        var attempts = 0
        composeRule.setContent {
            SaeedTheme {
                DashboardScreen(
                    state = state, storeName = "متجر تجريبي", currency = "د.ج",
                    onRefresh = {}, onOrdersClick = {}, onProductsClick = {},
                    onStatusUpdate = { id, next, done ->
                        assertEquals(example.id, id)
                        attempts++
                        if (attempts == 1) {
                            done(false)
                        } else {
                            state = state.copy(allOrders = listOf(example.copy(status = next)))
                            done(true)
                        }
                    },
                )
            }
        }
        composeRule.onNodeWithText("الإحصاءات تخص أحدث 100 طلب محمّل، وليست إجمالي المتجر.").assertIsDisplayed()
        composeRule.onNodeWithTag("admin-dashboard").performScrollToNode(hasText("زبون تجريبي"))
        composeRule.onNodeWithText("زبون تجريبي").performClick()
        composeRule.onNodeWithText("قيد التحضير").performClick()
        composeRule.onNodeWithText("تعذّر تغيير حالة الطلب. تحقّق من الاتصال وأعد المحاولة.").assertIsDisplayed()
        composeRule.onNodeWithText("قيد التحضير").performClick()
        composeRule.onNodeWithText("جاهز").performScrollTo().assertIsDisplayed()
        assertEquals(2, attempts)
    }

    @Test
    fun `dashboard links lead to real sections`() {
        var destination = ""
        composeRule.setContent {
            SaeedTheme {
                DashboardScreen(
                    state = AdminUiState(ordersLoaded = true, productsLoaded = true),
                    storeName = "متجر تجريبي", currency = "د.ج",
                    onRefresh = {}, onOrdersClick = { destination = "orders" },
                    onProductsClick = { destination = "products" }, onStatusUpdate = { _, _, _ -> },
                )
            }
        }
        composeRule.onNodeWithTag("admin-dashboard").performScrollToNode(hasText("عرض الكل"))
        composeRule.onNodeWithText("عرض الكل").performClick()
        assertEquals("orders", destination)
        composeRule.onNodeWithTag("admin-dashboard").performScrollToNode(hasText("منتجات المتجر"))
        composeRule.onNodeWithText("منتجات المتجر").performClick()
        assertEquals("products", destination)
    }
}
