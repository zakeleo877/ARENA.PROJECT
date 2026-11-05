# نتائج معايير القبول الفعلية

تاريخ التشغيل: 2026-11-05. الالتزام: **`e4db2a4`**. أرقام التشغيل في GitHub Actions:

| سير العمل | رقم التشغيل | النتيجة | الرابط |
|---|---|---|---|
| `Build and verify` | **42121136746** | **نجاح** — ٢٠ خطوة من ٢٠ | https://github.com/zakeleo877/ARENA.PROJECT/actions/runs/42121136746 |
| `Database security checks` | **42121136708** | **نجاح** | https://github.com/zakeleo877/ARENA.PROJECT/actions/runs/42121136708 |
| `Verify the interface on an emulator` | **42121136873** | **نجاح** — التطبيقان عملا ولُقطتا الشاشتين | https://github.com/zakeleo877/ARENA.PROJECT/actions/runs/42121136873 |
| `Release gate` | **42121136759** | **فشل كما يجب** — أسرار الإنتاج غير مضبوطة | https://github.com/zakeleo877/ARENA.PROJECT/actions/runs/42121136759 |

كل السير الأربعة على الالتزام نفسه، فهي تصف حالة واحدة من الكود.

كل الأرقام أدناه مأخوذة من ناتج التشغيل الفعلي، وليست من توثيق سابق.

## المعايير الأحد عشر — الخلاصة

| # | المعيار | الحالة | الدليل |
|---|---|---|---|
| ١ | بيئة Supabase اختبارية مستقلة | **منفَّذ محلياً**، لا سحابياً | `supabase/tests/isolated-environment.cjs` — PostgreSQL خاص (PGlite) يُبنى من `supabase/schema.sql` ويُدمَّر مع العملية. `api.supabase.com` و`db.supabase.co` محجوبان من بيئة العمل، فلم يُنشأ مشروع سحابي. |
| ٢ | RLS وصلاحيات الزبائن والإدارة | **٣٨/٣٨** | `npm run test:acceptance` ⇒ `38 checks passed, 0 failed.` + التشغيل `42121136708` |
| ٣ | تسجيل الحساب والدخول والخروج | **نجح** | فحوص ١٧-٢١ و٣ في القائمة أدناه |
| ٤ | عرض المنتجات والتصنيفات | **نجح** | فحص ٤ و١٦ — الزائر يرى المتاح فقط |
| ٥ | السلة وإنشاء الطلب | **نجح** | فحص ٥ — `place_order` يحفظ الصف ويحسب المجموع على الخادم |
| ٦ | انقطاع الإنترنت وإعادة الإرسال | **نجح** | `OrderRepositoryTest` (سبعة فحوص) + فحص ٦ (لا تكرار) + لقطة شاشة تُظهر حالة إعادة المحاولة على الجهاز |
| ٧ | لوحة الإدارة | **نجح** | فحص ٧ (عرض/تعديل/حذف الطلبات والمنتجات والصور) + لقطة شاشة الدخول |
| ٨ | الواجهات على Android Emulator | **نجح** | التشغيل `42121136873` — التطبيقان عملا، `LaunchState: COLD`، لا استثناء قاتل، لقطتان 320x640 |
| ٩ | حالات الفشل | **نجح** | فحص ٩ (كل مدخل خاطئ يُرفض برسالة) + فحص ٩ج (رمز مزوّر) + حالات الشبكة في `OrderRepositoryTest` |
| ١٠ | توقيع Release بمفتاح إنتاجي دائم | **معلَّق كما يجب** | `Release gate` `42121136759` **يفشل** لأن الأسرار غير مضبوطة. لا يُعلن نجاحاً وهمياً. |
| ١١ | تقرير قبول نهائي | **هذا الملف** | كل صف أعلاه يقود إلى ناتج حقيقي أو رابط Actions |

المعيار الوحيد غير المكتمل هو **الأول جزئياً** (بيئة سحابية حقيقية) و**العاشر**
(مفتاح الإنتاج). كلاهما معلن صراحةً هنا ولا يُدَّعى نجاحه.

## ملخص التشغيل ٤٢٠٣١٢٨٢٩١١ (Build and verify)

كل خطوة مسجّلة `success`، وهذا ناتج التشغيل الحقيقي:

| الخطوة | النتيجة |
|---|---|
| `Set up JDK 17` | نجاح |
| `Use the runner Android SDK` | نجاح |
| `Accept licenses and install the platform` | نجاح |
| `Verify the pinned versions` | نجاح |
| `Generate local.properties` | نجاح |
| `Prepare the signing keystore` | نجاح |
| `Check the Kotlin references` | نجاح — ٦٤ ملفاً |
| `Run unit tests and lint` | **نجاح** — ١٠ ملفات اختبار، صفر فشل |
| `Build both release APKs` | **نجاح** |
| `Report the release signing key (acceptance check 10)` | نجاح |
| `Verify the packages (acceptance checks 1-10)` | **نجاح** |
| `Verify the repository (acceptance checks 11-13)` | **نجاح** |
| `Run the local database smoke tests` | نجاح — ٣٢ فحصاً |
| `Run the isolated database security checks (acceptance checks 14-25)` | نجاح — ٣٨ فحصاً |
| `Collect the unit test reports` | نجاح |
| `Publish diagnostics` | نجاح |
| `Upload APKs and logs` | نجاح — `saeed-stores-apks` |

**ملفات الاختبار العشرة التي نُفِّذت:**
`CartStoreTest`, `CatalogApiTest`, `CatalogRepositoryTest`, `ColorContrastTest`,
`CsvExporterTest`, `DesignSystemUiTest`, `IsolatedBackendTest`,
`OrderRepositoryTest`, `RoomIntegrationTest`, `UtilitiesTest`.

**ملفات الاختبار العشرة التي نُفِّذت:**
`CartStoreTest`, `CatalogApiTest`, `CatalogRepositoryTest`, `ColorContrastTest`,
`CsvExporterTest`, `DesignSystemUiTest`, `IsolatedBackendTest`,
`OrderRepositoryTest`, `RoomIntegrationTest`, `UtilitiesTest`.

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

> الأحجام أعلاه من التشغيل `41980992639`. في التشغيل `42121136746` صار أرشيف
> الحزم `saeed-stores-apks` بحجم `4,243,737` بايت (نحو ٤.٠٥ ميغابايت)، لأنه
> يضم الحزمتين مع السجلات، وضمن الحد المقبول ١–١٠ ميغابايت المطلوب في
> معيار القبول.

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
ويثبّتهما على محاكي `android-34/default/x86_64`، ويشغّل الشاشة الرئيسية،
ويتأكد أن العملية حية، وأن لا يوجد استثناء قاتل، ويلتقط لقطة شاشة.

### النتيجة على المحاكي: التشغيل ٤٢٠٣١٢٨٢٨٦٨ — **نجاح**

الناتج الحرفي المنشور على فرع `ci-emulator` في الملف `emulator-diagnostics.md`:

**تطبيق الزبائن:**

```
PASS: the emulator is visible to adb
PASS: the emulator finished booting
PASS: the apk was installed on the emulator
Starting: Intent { cmp=com.zakeleo877.saeedstore/.MainActivity }
Status: ok
LaunchState: COLD
Activity: com.zakeleo877.saeedstore/.MainActivity
TotalTime: 1140
WaitTime: 1145
Complete
PASS: the application process is running with the identifier 2297
PASS: no fatal exception was logged
PASS: a screenshot was captured at artifacts/screenshots/com.zakeleo877.saeedstore.png
PASS: the screenshot shows a rendered screen (320x640, 256 distinct byte values)
PASS: the user interface rendered on the emulator
```

**تطبيق الإدارة:**

```
Starting: Intent { cmp=com.zakeleo877.saeedadmin/.MainActivity }
Status: ok
LaunchState: COLD
Activity: com.zakeleo877.saeedadmin/.MainActivity
TotalTime: 813
WaitTime: 822
Complete
PASS: the application process is running with the identifier 2379
PASS: no fatal exception was logged
PASS: a screenshot was captured at artifacts/screenshots/com.zakeleo877.saeedadmin.png
PASS: the screenshot shows a rendered screen (320x640, 256 distinct byte values)
PASS: the user interface rendered on the emulator
```

`LaunchState: COLD` يعني أن التطبيق بدأ من الصفر (لا حالة محفوظة)، و
`256 distinct byte values` في ملف الـ PNG دليل أن الشاشة مرسومة وليست فارغة.
اللقطات منشورة على الفرع `ci-emulator` تحت `artifacts/screenshots/`، وم Artifact
باسم `emulator-screenshots` مرفوع أيضاً من التشغيل `42121136873`.

### ما الذي تعرضه اللقطات فعلياً

**تطبيق الزبائن** — `docs/screenshots/com.zakeleo877.saeedstore.png`:

- العنوان «محلات السعيد» وتحته «طلبية وتوصيل - أطلب من محلاتك».
- أيقونة السلة في الأعلى، وحقل بحث «ابحث عن منتج...» بأيقونة بحث.
- حالة فارغة: «تعذر تحميل المنتجات. تحقق من الاتصال ثم أعد المحاولة» مع زر
  «إعادة المحاولة».
- الاتجاه من اليمين إلى اليسار، وثيم Neon Void (خلفية داكنة ولمسات سماوية).

**تطبيق الإدارة** — `docs/screenshots/com.zakeleo877.saeedadmin.png`:

- الشعار والعنوان «إدارة محلات السعيد» وتحته «تسجيل الدخول لإدارة الطلبات
  والمنتجات».
- حقلا «البريد الإلكتروني» و«كلمة المرور»، وزر «تسجيل الدخول» معطّل لأن
  الحقول فارغة — وهو السلوك الصحيح.
- الاتجاه من اليمين إلى اليسار.

الخطأ في تحميل المنتجات **مقصود**: البناء في الـ CI يستخدم
`SUPABASE_URL=https://placeholder.invalid`، أي لا يوجد خادم حقيقي خلف العنوان،
واتضح أن التطبيق يتعامل مع هذا الفشل بوضوح (رسالة وزر إعادة المحاولة) بدل
أن ينهار. هذا يثبت مسار «انقطاع الإنترنت» على جهاز حقيقي.

> التشغيل `42028557161` (بعد `f33e565`) نجح أيضاً ونشر اللقطات على الفرع
> `ci-emulator`، فاللقطتين أعلاه من ذلك التشغيل.

### العطل الذي كشفه المحاكي وأُصلح

التطبيقان كانا **ينهاران لحظة الإقلاع** على الجهاز. السبب الجذري لم يكن في
منطق العمل: `CustomerViewModel` و `AdminViewModel` كانا يُطلبان عبر
`hiltViewModel()` لكنهما لم يحملا `@HiltViewModel`، فبحث إطار العمل عن
constructor بلا وسائط بالانعكاس ولم يجده:

```
Caused by: java.lang.NoSuchMethodException: com.zakeleo877.saeedstore.ui.CustomerViewModel.<init> []
    at java.lang.Class.getDeclaredConstructor(Class.java:3063)
```

الإصلاح: كلا الـ view models صار `@HiltViewModel` بـ `@Inject constructor`،
و `CustomerModule` لم يعد ينشئ الـ view model يدوياً، وأُضيف
`androidx.hilt:hilt-compiler` إلى معالجة الـ KSP، وأُضيفت قواعد ProGuard
التي تحمي أصناف Hilt و Dagger وأسماء الأصناف كي يذكر التقرير اسم الصنف
بدل رمز مبهم. وأُضيف إلى `scripts/verify-kotlin-refs.py` فحص يرصد أي
view model يُطلب بـ `hiltViewModel()` بلا تعليق Hilt، وقد رُئِّب في
اختبار سلبي على الملفين.

### ما لم يُشغَّل

- **لم يُلتقط أي رد يدوي** على محاكي أو هاتف حقيقي من هذه البيئة. المحاكي
  يعمل في GitHub Actions فقط، ولقطاته منشورة على الفرع.
- الفحوص البصرية الدقيقة (تباين WCAG لكل تركيبة، RTL في كل شاشة، خط 200،
  Shared Element، SwipeToDismissBox، WindowSizeClass مع NavigationRail) تحتاج
  مراجعة بصرية على جهاز حقيقي.
- Realtime وإشعارات `PeriodicWorkRequest` (بحد ١٥ دقيقة) تحتاج جهازاً حقيقياً
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


## سير العمل الأربعة

المشروع يعمل بأربعة سير عمل مستقلة، ولكل واحد غرض مختلف:

| سير العمل | الغرض | الحالة الحالية |
|---|---|---|
| `Build and verify` | الترجمة، اختبارات الوحدة، lint، بناء الحزم، فحوص ١-١٣ | **أخضر** |
| `Database security checks` | فحوص ١٤-٢٥ على البيئة المعزولة | **أخضر** |
| `Verify the interface on an emulator` | تشغيل التطبيقين على محاكي والتقاط لقطات | **أخضر** — التشغيل ٤٢٠٣١٢٨٢٨٦٨ |
| `Release gate` | بوابة النشر: يفشل بلا مفتاح إنتاجي دائم | **يفشل كما يجب** |

### Release gate

هذا الفشل **مقصود** وليس عطلاً. البوابة ترفض النشر عندما لا تكون أسرار
الإنتاج مضبوطة:

```
FAIL: 10. the production signing secrets are not configured.
      KEYSTORE_BASE64, KEYSTORE_PASSWORD, KEY_ALIAS and KEY_PASSWORD
      must be set before the project can be published.
```

المفتاح المؤقت الذي يولّده الـ runner يختفي بانتهاء المهمة، فلا يستطيع
تحديث تطبيق مثبَّت. لذلك بوابة النشر تبقى حمراء حتى تُضبط الأسرار، وكل
ملف APK مبنيّ بمفتاح مؤقت **غير قابل للنشر**.

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
6. **زر مسح البحث**: كان بلا `contentDescription`، فلم يكن قارئ الشاشة قادراً على
   وصفه. أُضيف له تسمية «مسح البحث».
7. **نمط استيراد مفقود**: عدة ملفات كانت تستخدم `mutableStateOf` بلا
   استيراد، والفحص الثابت الآن يبلّغ عنها خلال ثوانٍ بدل أربع دقائق من
   الترجمة في الـ CI.

### الفحص الثابت

`scripts/verify-kotlin-refs.py` يفحص ٦٤ ملف Kotlin محلياً خلال ثوانٍ، ويلتقط:

| النمط | الأثر |
|---|---|
| استيراد ناقص لرمز مستخدم | خطأ ترجمة بعد ٤ دقائق |
| `onAllNodesWithText` على قاعدة الاختبار | خطأ ترجمة بعد ٤ دقائق |
| `request.body.toString()` بدل `TextContent` | تحقق فاشل |
| `PredictiveBackHandler` بلا `collect` | خطأ lint يُفشل البناء |
| `sharedElement` بمعامل `state` بدل `sharedContentState` | خطأ ترجمة |
| ثابت `WindowWidthSizeClass.EXPANDED` | خطأ ترجمة |
| view model يُطلب بـ `hiltViewModel()` بلا تعليق Hilt | انهيار التطبيق عند الإقلاع على الجهاز |
