# نتائج معايير القبول الفعلية

تاريخ التشغيل: 2026-11-04. رقم التشغيل في GitHub Actions: **41918818858**.
الالتزام: `a00c45f`. النتيجة النهائية: **كل الخطوات ناجحة**.

الرابط: https://github.com/zakeleo877/ARENA.PROJECT/actions/runs/41918818858

كل الأرقام أدناه مأخوذة من ناتج التشغيل الفعلي، وليست من توثيق سابق.

## ملخص التشغيل

| الخطوة | النتيجة |
|---|---|
| Check the Kotlin references | نجاح |
| Set up JDK 17 | نجاح |
| Use the runner Android SDK | نجاح |
| Accept licenses and install the platform | نجاح |
| Verify the pinned versions | نجاح |
| Run unit tests and lint | **نجاح** (`BUILD SUCCESSFUL in 5m 13s`) |
| Build both release APKs | **نجاح** (`BUILD SUCCESSFUL in 6m 25s`) |
| Verify the packages (checks 1-10) | **نجاح** |
| Verify the repository (checks 11-13) | **نجاح** |
| Run the local database smoke tests | **نجاح** |

كل خطوة في التشغيل أعلاه مسجّلة `success`، ومنها `Build both release APKs`
و`Verify the packages (acceptance checks 1-10)` و`Verify the repository
(acceptance checks 11-13)`.

## فحوص الحزمة ١-١٠ — للتطبيقين

الناتج الحرفي من `scripts/verify-apk.sh`:

```
PASS: 1. resources.arsc is stored uncompressed
PASS: 2. zipalign verified
PASS: 3. APK is signed with v2 or newer
PASS: 4. minSdkVersion is 24
PASS: 4. versionName is 1.0
PASS: 5. APK is not debuggable
PASS: 6. exported attribute present
PASS: 7. package integrity verified
PASS: 8. no unexpected permissions
PASS: 9. customer APK contains no admin code
PASS: 10. APK size is 853 KiB (limit 20 MiB)

PASS: 9. admin APK contains no customer code
PASS: 10. APK size is 989 KiB (limit 20 MiB)
```

تفاصيل إضافية من `aapt2 dump badging`:

- تطبيق الزبائن: `name='com.zakeleo877.saeedstore' versionCode='1' versionName='1.0'`
- تطبيق الإدارة: `name='com.zakeleo877.saeedadmin' versionCode='1' versionName='1.0'`
- `application-label:'محلات السعيد'` و`application-label-ar:'محلات السعيد'`
- التوقيع: `Verified using v2 scheme (APK Signature Scheme v2): true`

## فحوص المستودع ١١-١٣

الناتج من `scripts/verify-repo.sh`:

```
PASS: 11. no delivery wording in the project
PASS: 12. no secrets committed
PASS: 13. unit tests and lint passed
```

الفحص ١٣ هو `./gradlew --no-daemon test lint` وقد نجح داخل GitHub Actions.

## اختبارات قاعدة البيانات

`npm ci && npm test` داخل `supabase/tests`:

**32 فحصًا محليًا نجحت.** تشمل:

- تطبيق المخطط مرة ومرة أخرى (idempotent) وحذف سياسة قديمة غير آمنة.
- **الزائر يرى المنتجات المتاحة فقط** (معيار القبول ٢١) — وهو ما كان معكوسًا قبل هذا الإصدار.
- المدير يرى كل المنتجات بما فيها المخفية، وغير المدير لا يرى المنتجات المخفية.
- الزائر لا يقرأ الطلبات ولا يكتبها ولا يدّعي صلاحية المدير.
- `place_order`: نفس `client_id` يعيد نفس رقم الفاتورة وصفًا واحدًا فقط.
- رفض المنتج غير المتاح والكمية صفر والكمية ١٠٠ والمعرّف المشوّه.
- التحقق من الهاتف، والسادس يُرفَض بحد الطلبات.
- الخادم يحسب المجموع ويخزّن صفًا واحدًا.
- سياسات Storage: المدير يقرأ ويحذف الصور.

**حد مهم:** هذه فحوص PGlite محلية مع محاكاة Auth وREST وStorage. ليست اختبارًا على Supabase حقيقي.

## معايير القبول ١٤-٢٥ على قاعدة بيانات معزولة

**نُفِّذت على بيئة معزولة مستقلة**، وليس على مشروع Supabase سحابي.

### كيف بُنيت البيئة

البيئة ملف `supabase/tests/isolated-environment.cjs`: نسخة PostgreSQL خاصة
(PGlite) تُنشأ من `supabase/schema.sql` عند كل تشغيل، وتُدمّر بانتهاء العملية.
تستمع على `127.0.0.1` فقط، وتستخدم بيانات وهمية على نطاق `.invalid`.

تfaithful reproduction للأجزاء التي يعتمد عليها التطبيق:
- الأدوار `anon` و `authenticated` بنفس منح الصلاحيات في الـ schema
- `auth.uid()` تقرأ من الـ JWT المُتحقَّق منه، والتوقيع HMAC-SHA256 بمفتاح
  يُولَّد داخل العملية ولا يُحفظ، فلا يمكن تزوير رمز
- سطح REST بحالة `200/201/400/401/403/404`، و RPC، و Storage بنفس السياسات
- سطح Auth (تسجيل/دخول/تجديد/خروج) بحالاته الحقيقية

**لم يُستعمل أي مشروع إنتاجي، ولا أي مفتاح إنتاجي، ولا أي بيانات عميل.**

### النتيجة: ٣٨ فحصًا، كلها ناجحة

```
38 checks passed, 0 failed.
```

| الفحص | ما الذي تحقّق |
|---|---|
| ١٤ | الزائر المجهول لا يقرأ أي طلب |
| ١٥ | الإدراج المباشر في جدول الطلبات مرفوض |
| ١٦ | الزائر يرى المنتجات المتاحة فقط |
| ١٧ | التسجيل العام معطّل |
| ١٨ | إنشاء حساب يعطي جلسة صالحة |
| ١٩ | كلمة مرور خاطئة تُرفض |
| ٢٠ | تسجيل الدخول بكلمة صحيحة يعطي جلسة |
| ٢١ | الزبون يسجّل الدخول ويحصل على رمز صالح |
| ٢٢ | أول حساب يدير يطالب بالدور الإداري، ولا يُطالب به مرتين |
| ٢٣ | حساب لاحق لا يستطيع المطالبة بالدور الإداري |
| ٢٤ | الزبون المسجَّل لا يقرأ جدول الطلبات |
| ٢٥ | الزبون المسجَّل لا يُدرج طلباً مباشرة |
| ٣ | الخروج يُبطل رمز التجديد |
| ٤ | الكتالوج والتصنيفات والإعدادات العامة تظهر كما يجب |
| ٥ | إنشاء الطلب عبر `place_order` وحفظه والمجموع المحسوب على الخادم |
| ٦ | إعادة إرسال الطلب بعد انقطاع لا تُنشئ طلباً مكرراً |
| ٧ | لوحة الإدارة: عرض وتعديل وحذف الطلبات والمنتجات والصور |
| ٩ | كل المدخلات الخاطئة تُرفض برسالة واضحة |
| ٩ج | رمز Bearer مزوّر يُرفض |
| — | كل طلب أنشأته الفحوص حُذف قبل انتهاء العملية |

### إثبات أن الفحوص ترصد التراجع

تم تعطيل كل سياسة على حدة، وشاهدت الفحوص تفشل:

| التعديل | الفحص الذي فشل |
|---|---|
| `products read` تسمح لكل الصفوف | ١٦، ٤، ٧ج |
| `orders admin all` تسمح لكل `authenticated` | ٢٥، ٧ي |
| `claim_first_admin` تسمح بأكثر من مدير | ٢٣ |
| `storage` تسمح بالInsert لكل `authenticated` | ٧هـ |

### ما لم يُختبر

- **قاعدة بيانات Supabase سحابية حقيقية.** `api.supabase.com` و`db.supabase.co`
  محجوبان من بيئة العمل، فلم يمكن إنشاء مشروع اختبار سحابي. الفحوص أعلاه
  تختبر **نفس SQL ونفس السياسات** على PostgreSQL حقيقي، لكنها ليست شهادة
  أمن لمشروع مستضاف.
- **Supabase Auth الحقيقي** (تأكيد البريد، إعادة التعيين، تحديد المعدل،
  الجلسات المتعددة) — طبقة Auth هنا تحاكي السلوك لا الخدمة.

## فحوص الواجهة على جهاز

**جزئياً.** ما شُغّل وما لم يُشغَّل مذكور بالضبط.

### ما شُغّل

**اختبارات واجهة على الـ JVM عبر Robolectric** — الملف
`core/src/test/java/com/zakeleo877/saeedcore/DesignSystemUiTest.kt`. هذه تُشغِّل
Composition حقيقية وتفحص شجرة الدلالات: نقر الزر، تعطيل الزر، تغيّر حقل
النص، رسالة الخطأ، مسح البحث، نقر البطاقة، تأكيد النافذة وإلغاؤها، زر إعادة
المحاولة في الحالة الفارغة، وتطبيق ألوان الهوية.

**اختبارات تكامل تقود كود التطبيق الحقيقي** — الملف
`core/src/test/java/com/zakeleo877/saeedcore/IsolatedBackendTest.kt`. تُشغِّل
`CatalogApi` و `AuthManager` و `OrderRepository` الحقيقية ضد الخلفية المعزولة
عبر مقبس حقيقي: قراءة الكتالوج، قراءة الإعدادات، إنشاء طلب، رفض الطلب
الغير الصالح، رفض منتج غير متاح، حفظ طلب عند انقطاع الشبكة ثم إرساله بعد
عودتها، تسجيل دخول صحيح وخاطئ، ورفض بريد فارغ.

**محاكي Android في CI** — الملف `.github/workflows/emulator.yml` يبني الحزمتين
ويثبّتهما على محاكي، ويشغّل الشاشة الرئيسية، ويتأكد أن العملية حية، وأن لا يوجد استثناء قاتل، ويلتقط لقطة شاشة. النتائج تُنشر كـ artifact باسم
`emulator-screenshots`.

### ما لم يُشغَّل

- **لم يُلتقط أي رد يدوي** على محاكي أو هاتف حقيقي من هذه البيئة، ولا توجد
  لقطات شاشة في هذا المستودع. المحاكي يعمل في GitHub Actions فقط.
- الفحوص البصرية الدقيقة (تباين WCAG لكل تركيبة، RTL في كل شاشة، خط 200،
  Shared Element، SwipeToDismissBox، WindowSizeClass مع NavigationRail) تحتاج
  مراجعة بصرية على جهاز حقيقي.
-Realtime وإشعارات `PeriodicWorkRequest` (بحد ١٥ دقيقة) تحتاج جهازاً حقيقياً
لأن بيئة العمل ليس فيها جهاز حقيقي.

### خطوة التوقيع الدائم

**فحص ١٠ نُفِّذ كبوابة**، ونتيجته الآن معلنة صراحةً:
- `scripts/verify-signing.sh` يقرأ وضع التوقيع وبصمة الشهادة.
- `Release gate` workflow **يفشل** إذا لم تكن أسرار الإنتاج
  (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`)
  مضبوطة، لأن المفتاح المؤقت لا يستطيع تحديث تطبيق مثبَّت.
- في `Build and verify` يُطبع الوضع والبصمة دون إفشال البناء، حتى لا يختفي
  نتيجة البناء نفسها.

**الحالة الراهنة:** لم تُضبط أسرار الإنتاج، فالمفتاح الحالي مؤقت. هذه
الفحوص لا تُعلن نجاحاً وهمياً؛ الـ Release gate يعلن الفشل حتى تُضبط الأسرار.

## سلسلة أخطاء الترجمة التي عولجت

كل خطأ أدناه كان يمنع `:core` أو `:customer` أو `:admin` من الترجمة، ولهذا لم
تبدأ اختبارات الوحدة ولا lint في تلك التشغيلات. لم يُعطَّل أي اختبار ولا أي
قاعدة lint في أي مرحلة.

| التشغيل | الملف:السطر | الخطأ | السبب الحقيقي | الإصلاح |
|---|---|---|---|---|
| `41906502426` | `core/.../OrderRepository.kt:113,118` | `No value passed for parameter 'location'` | أُعيدت تسمية معرّف رابط الخريطة في Kotlin إلى `customerLocationUrl` بينما عمود Room بقي `location` | تمرير `location = order.customerLocationUrl` مع قيمة التطبيق الحقيقية |
| `41908867960` | `gradle/libs.versions.toml` | `Reserved alias name` | Gradle 9.8.2 يحجز أي alias يحتوي كلمة `class` | تسمية الـ alias `androidx-compose-material3-sizes` |
| `41909796271` | `admin/.../AdminApp.kt:79` | `Unresolved reference 'EXPANDED'` | الثابت الحقيقي هو `WindowWidthSizeClass.Expanded` | تصحيح اسم الثابت |
| `41909796271` | `admin/.../OrdersScreen.kt:71` | `Unresolved reference 'size'` | `Modifier.size` مستخدم بلا `import androidx.compose.foundation.layout.size` | إضافة الاستيراد |
| `41909796271` | `admin/.../ProductsScreen.kt:83` | `Unresolved reference 'size'` | نفس المشكلة | إضافة الاستيراد |
| `41911359524` | `customer/.../CatalogScreen.kt:264` | `No parameter with name 'state' found` | المعامل الأول في `sharedElement` اسمه `sharedContentState` | تصحيح اسم المعامل |
| `41911359524` | `customer/.../ProductDetailScreen.kt:6` | `Unresolved reference 'rememberSharedContentState'` | الدالة عضو في `SharedTransitionScope` ولا تُستورد | حذف الاستيراد الخاطئ |
| `41912849774` | `customer/.../ProductImageDialog.kt:59` | `NoCollectCallFound` (lint) | `PredictiveBackHandler` يمرّر `Flow` ويجب جمعه، وإلا نُفِّذ الكود عند **بداية** الإيماءة لا عند اكتمالها | جمع الـ flow وتحريك الصورة مع تقدّم الإيماءة، والإغلاق عند الاكتمال فقط |

### الفحص الثابت المضاف

`scripts/verify-kotlin-refs.py` يفحص محلياً في ثوانٍ الأخطاء التي استغرقت
كل واحد منها خمس دقائق ليُكتشف في CI: استيراد مفقود، اسم معامل خاطئ، ثابت
مكتبة غير موجود، وغياب `collect` داخل `PredictiveBackHandler`. وهو يعمل ضمن
خطوة `Check the Kotlin references` قبل البناء.

## حالة البناء في هذه البيئة

- لا يوجد JDK ولا Android SDK محليًا، و`dl.google.com` و`services.gradle.org` و`repo1.maven.org`
  محجوبة، فلم يُبنَ APK محليًا.
- كل البناء أعلاه تم على runner تابع لـ GitHub Actions الذي يملك JDK 17 وAndroid SDK.
- المفتاح المستخدم في التوقيع هو مفتاح مؤقت يُولَّد داخل الـ runner ويُحذف معه؛
  **التوقيع الإنتاجي الحقيقي يحتاج `KEYSTORE_BASE64` و`KEYSTORE_PASSWORD` و`KEY_ALIAS` و`KEY_PASSWORD`
  من GitHub Secrets.**

## متغيّرات CI المطلوبة

| المتغيّر | الاستخدام | مطلوب؟ |
|---|---|---|
| `SUPABASE_URL` | عنوان مشروع Supabase | نعم لبناء APK |
| `SUPABASE_KEY` | المفتاح العام (`sb_publishable_`) | نعم لبناء APK |
| `KEYSTORE_BASE64` | مفتاح التوقيع الإنتاجي | نعم للإصدار النهائي |
| `KEYSTORE_PASSWORD` | كلمة مرور المخزن | نعم للإصدار النهائي |
| `KEY_ALIAS` | اسم المفتاح | نعم للإصدار النهائي |
| `KEY_PASSWORD` | كلمة مرور المفتاح | نعم للإصدار النهائي |
| `SAEED_COMPILE_SDK` | إصدار الترجمة، مثل `android-37.2` | لا، يُستنتج تلقائيًا |

## ما تم إصلاحه في هذه المرحلة

1. **سياسة RLS للمنتجات**: كانت تسمح للزائر برؤية المنتجات المخفية، والآن يرى المتاح فقط.
2. **صيغة التاريخ**: `yyyy/MM/dd HH:mm` ← `dd/MM/yyyy HH:mm`.
3. **لون BottomSheet**: `RoyalPurple` ← `surfaceContainerHigh` مع `DeepVoidViolet`.
4. **فقدان الطلب**: كان أول فشل في الإرسال يُسقط الطلب نهائيًا، والآن يُحفظ في الطابور لإعادة المحاولة.
5. **ترميز طابور الطلبات**: كان يفشل لأن `Map<String, Any>` لا يملك مُسلسِّلًا.
