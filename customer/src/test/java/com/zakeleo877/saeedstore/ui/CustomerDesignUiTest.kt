package com.zakeleo877.saeedstore.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zakeleo877.saeedcore.designsystem.SaeedTheme
import com.zakeleo877.saeedcore.data.PendingOrderRow
import com.zakeleo877.saeedcore.data.PendingState
import com.zakeleo877.saeedcore.model.PendingOrderItem
import com.zakeleo877.saeedcore.model.Product
import java.math.BigDecimal
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** Real Compose semantics with isolated, fictional data; no backend writes. */
@RunWith(AndroidJUnit4::class)
@Config(sdk = [30], application = android.app.Application::class, qualifiers = "w360dp-h800dp")
@OptIn(ExperimentalSharedTransitionApi::class)
class CustomerDesignUiTest {
    @get:Rule val composeRule = createComposeRule()

    private val first = Product(
        id = "test-1", name = "أداة تصفيف", price = BigDecimal("100"), description = "للشعر",
        imageUrl = "", category = "العناية", available = true, sortOrder = 0, createdAt = Instant.EPOCH,
    )
    private val second = first.copy(id = "test-2", name = "كريم", category = "التجميل", available = false)

    private fun setCatalog(initialHome: Boolean = true, onAdd: (Product) -> Unit = {}) {
        composeRule.setContent {
            var home by remember { mutableStateOf(initialHome) }
            var query by remember { mutableStateOf("") }
            var category by remember { mutableStateOf<String?>(null) }
            var offers by remember { mutableStateOf(false) }
            SaeedTheme {
                SharedTransitionLayout {
                    AnimatedContent(targetState = home, label = "test-catalog") { homeView ->
                        CatalogScreen(
                            sharedScope = this@SharedTransitionLayout,
                            animatedScope = this,
                            state = CustomerUiState(products = listOf(first, second), everLoaded = true),
                            isHome = homeView,
                            cartCount = 0,
                            query = query,
                            onQueryChange = { query = it },
                            selectedCategory = category,
                            onCategorySelected = { category = it },
                            offersOnly = offers,
                            onOffersOnlyChange = { offers = it },
                            onProductClick = {},
                            onAddToCart = onAdd,
                            onCartClick = {},
                            onShowAll = { home = false },
                            onRetry = {},
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `home discovery opens the distinct products view`() {
        setCatalog()
        composeRule.onNodeWithText("حلاقة وتجميل").assertIsDisplayed()
        composeRule.onNodeWithText("عنايتك تبدأ هنا").assertIsDisplayed()
        composeRule.onNodeWithText("من اختيارات السعيد").assertDoesNotExist()
        composeRule.onNodeWithText("تصفّح المنتجات").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("كل المنتجات").assertIsDisplayed()
        composeRule.onNodeWithText("تصفّح المنتجات").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `home feature stays visible and usable on a narrow screen`() {
        setCatalog()
        composeRule.onNodeWithText("عنايتك تبدأ هنا").assertIsDisplayed()
        composeRule.onNodeWithText("تصفّح المنتجات").assertIsDisplayed().performClick()
        composeRule.onNodeWithText("كل المنتجات").assertIsDisplayed()
    }

    @Test
    fun `products search uses Arabic folding and adds the matching product`() {
        var added = ""
        setCatalog(initialHome = false, onAdd = { added = it.id })
        composeRule.onNodeWithText("ابحث عن منتج…").performTextInput("اداة")
        composeRule.onNodeWithTag("catalog-grid").performScrollToNode(hasText("أداة تصفيف"))
        composeRule.onNodeWithText("أداة تصفيف").assertIsDisplayed()
        composeRule.onNodeWithText("كريم").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("أضف أداة تصفيف إلى السلة").performClick()
        assertEquals("test-1", added)
    }

    @Test
    fun `an unavailable product cannot be added from its detail`() {
        var added = false
        composeRule.setContent {
            SaeedTheme {
                SharedTransitionLayout {
                    AnimatedContent(targetState = second, label = "test-detail") { product ->
                        with(this@SharedTransitionLayout) {
                            ProductDetailScreen(
                                animatedScope = this@AnimatedContent,
                                product = product,
                                currency = "د.ج",
                                onBack = {},
                                onAddToCart = { added = true },
                            )
                        }
                    }
                }
            }
        }
        composeRule.onNodeWithText("غير متاح").assertIsNotEnabled()
        assertEquals(false, added)
    }

    @Test
    fun `the pending tab is a real destination with an honest label`() {
        var selected by mutableStateOf(SaeedTab.HOME)
        composeRule.setContent {
            SaeedTheme {
                SaeedBottomBar(
                    cartCount = 0, pendingCount = 0, current = selected,
                    onHomeClick = { selected = SaeedTab.HOME },
                    onProductsClick = { selected = SaeedTab.PRODUCTS },
                    onCartClick = { selected = SaeedTab.CART },
                    onPendingClick = { selected = SaeedTab.PENDING },
                )
            }
        }
        composeRule.onNodeWithContentDescription("المنتجات").performClick()
        assertEquals(SaeedTab.PRODUCTS, selected)
        composeRule.onNodeWithContentDescription("الطلبات المعلّقة").performClick()
        assertEquals(SaeedTab.PENDING, selected)
    }

    @Test
    @Config(qualifiers = "w320dp-h640dp")
    fun `checkout validates fields and returns the form on a narrow screen`() {
        var submitted: CheckoutForm? = null
        composeRule.setContent { SaeedTheme { CheckoutSheet(submitting = false, onSubmit = { submitted = it }) } }
        composeRule.onNodeWithText("إرسال الطلب").performScrollTo().performClick()
        composeRule.onNodeWithText("أدخل اسمك الكامل.").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithText("الاسم الكامل").performScrollTo().performTextInput("زبون تجريبي")
        composeRule.onNodeWithText("رقم الهاتف").performScrollTo().performTextInput("0555123456")
        composeRule.onNodeWithText("إرسال الطلب").performScrollTo().performClick()
        assertEquals("زبون تجريبي", submitted?.customer)
        assertEquals("0555123456", submitted?.phone)
    }

    @Test
    fun `pending orders show the local queue state and retry the same client id`() {
        var retried = ""
        val pending = PendingOrderRow(
            clientId = "example-local-id", customer = "زبون تجريبي", phone = "0555123456",
            notes = "", customerLocationUrl = "", items = listOf(PendingOrderItem("example-product", 2)),
            state = PendingState.WAITING, attemptCount = 1, lastError = null,
        )
        composeRule.setContent {
            SaeedTheme { PendingOrdersScreen(orders = listOf(pending), onRetry = { retried = it }, onDelete = {}) }
        }
        composeRule.onNodeWithText("بانتظار الإرسال").assertIsDisplayed()
        composeRule.onNodeWithText("قيد التحضير").assertDoesNotExist()
        composeRule.onNodeWithText("إعادة المحاولة الآن").performClick()
        assertEquals("example-local-id", retried)
    }

}
