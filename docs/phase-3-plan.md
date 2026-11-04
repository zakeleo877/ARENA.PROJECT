# خطة المرحلة الثالثة — تنفيذ شاشات العميل والإدارة

تاريخ: 2026-11-04. هذه الخطة تصف ما سُينفَّذ فعليًا في هذه المرحلة، وأي بند تعذّر تنفيذه بسبب البيئة.

## قيد تنفيذي في هذه البيئة

- لا يوجد JDK ولا Android SDK (`java: command not found`)، و`ANDROID_HOME` غير مضبوط.
- `dl.google.com` و`services.gradle.org` و`repo1.maven.org` محجوبة (HTTP 000)، فلا يمكن تنزيل التبعيات ولا بناء APK محليًا.
- `api.github.com` متاح، لذا يُنفَّذ البناء والاختبار عبر **GitHub Actions** على runner يملك JDK 17 وAndroid SDK.
- النتيجة: كل ما سيُكتب هنا **يُرفع إلى GitHub ليُختبر هناك**، ولا يُ claiming نجاحًا محليًا غير محقق.

## المرحلة الثالثة — النطاق

### أ. حزم和数据 customer

1. `customer/ui/catalog` — المنتجات، البحث، شرائح الفئات، `GridCells.Adaptive`، Shared Element للتفاصيل، تكبير الصورة.
2. `customer/ui/cart` — سلة محفوظة محليًا، تعديل الكمية، حذف بالسحب مع تراجع، شارة العدّاد.
3. `customer/ui/checkout` — `ModalBottomSheet` قابل للسحب، تحقق الهاتف الجزائري، موقع اختياري، طلب بدون اتصال.
4. `customer/ui/pending` — قائمة الطلبات المعلّقة والفاشلة، إعادة المحاولة، تعديل، حذف.
5. `customer/data` — `CartStore` (DataStore)، `PendingOrderQueue` (Room + WorkManager)، `OrderSubmitter`.
6. `customer/di` — `CustomerModule` يوفّر `SupabasePublicConfig` من `BuildConfig`.

### ب. admin

1. `admin/ui/login` — تسجيل دخول Supabase Auth، تجديد الجلسة، `is_admin()` ثم `claim_first_admin()`.
2. `admin/ui/dashboard` — بطاقات إحصائية، تحديث لحظي.
3. `admin/ui/orders` — قائمة، فلترة، بحث، تفاصيل بـ`ModalBottomSheet`، تغيير الحالة.
4. `admin/ui/products` — قائمة، إضافة/تعديل، صورة Photo Picker → WebP، رفع Storage، ترتيب.
5. `admin/ui/settings` — تعديل الإعدادات، مفتاح الإشعارات، استثناء البطارية.
6. `admin/ui/invoice` — فاتورة PDF عبر `PdfDocument` + `FileProvider`.
7. `admin/data` — `AdminApi` (Ktor + Bearer)، Realtime، `OrdersRefreshWorker` (15 دقيقة)، إشعار محلي.

### ج. مشترك

1. `core`: `NetworkMonitor`، `OrderRepository` للعميل، `AdminRepository` للإدارة، `AuthManager`، `SaeedBottomSheet` بلون `surfaceContainerHigh`، `Formatters.dateTime` بصيغة `dd/MM/yyyy HH:mm`.
2. **إصلاح سياسة RLS**: `products read` تعرض `available = true` فقط للزائر (معيار القبول 21).

### د. التسليم

1. `.github/workflows/build.yml` — JDK 17 + Android SDK، بناء `test lint assembleRelease`، فحص حزمة ١–١٠، فحص مستودع ١١–١٣، رفع artifacts.
2. `scripts/db-security-tests.sh` — فحوص ١٤–٢٥ على مشروع Supabase تجريبي فقط، تتوقف إن تطابق URL الإنتاج.
3. `docs/acceptance-results.md` — ناتج كل معيار فعلي.

## معايير القبول التي ستُغطّى

| # | كيف ستُنفَّذ |
|---|---|
| 1–10 | `scripts/verify-apk.sh` داخل workflow على APKين موقّعين |
| 11–13 | في نفس workflow + `npm test` |
| 14–25 | `scripts/db-security-tests.sh` بمفاتيح anon، على مشروع تجريبي |
| الواجهة | لقطات شاشة من emulator داخل workflow إن أمكن، وإلا توثيق صريح لعدم توفرها |

## ما لن يدّعيه هذا التسليم

- لن يُذكر نجاح أي فحص لم يُشغَّل فعلًا.
- لا FCM: الإشعار محلي عبر `PeriodicWorkRequest` فقط.
- لا تتبّع: «موقعي» للتواصل فقط، وليس عنوان توصيل.
