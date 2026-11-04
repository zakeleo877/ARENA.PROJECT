# نتائج معايير القبول الفعلية

تاريخ التشغيل: 2026-11-04. رقم التشغيل في GitHub Actions: **41879848618**.
الالتزام: `5832a54`. النتيجة النهائية: **كل الخطوات ناجحة**.

كل الأرقام أدناه مأخوذة من ناتج التشغيل الفعلي، وليست من توثيق سابق.

## ملخص التشغيل

| الخطوة | النتيجة |
|---|---|
| Set up JDK 17 | نجاح |
| Use the runner Android SDK | نجاح |
| Accept licenses and install the platform | نجاح |
| Verify the pinned versions | نجاح |
| Run unit tests and lint | **نجاح** |
| Build both release APKs | **نجاح** (`BUILD SUCCESSFUL in 6m 25s`) |
| Verify the packages (checks 1-10) | **نجاح** |
| Verify the repository (checks 11-13) | **نجاح** |
| Run the local database smoke tests | **نجاح** |

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
PASS: 10. APK size is 852 KiB (limit 20 MiB)

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

## معايير القبول ١٤-٢٥ على قاعدة بيانات حقيقية

**لم تُنفَّذ.** تتطلب مشروع Supabase تجريبيًا منفصلًا ومفاتيح معلومة. السكربت جاهز في
`scripts/db-security-tests.sh`، ويقرأ القيم من متغيرات البيئة:

```bash
TEST_SUPABASE_URL=... TEST_SUPABASE_KEY=... \
TEST_USER_EMAIL=... TEST_USER_PASSWORD=... \
bash scripts/db-security-tests.sh
```

السكربت **يتوقف فورًا** إذا تطابق `TEST_SUPABASE_URL` مع `SUPABASE_URL` الخاص بالإنتاج، أو إذا احتوى
عنوان الاختبار على الكلمة `prod`.

## فحوص الواجهة على جهاز

**لم تُنفَّذ.** لم يتوفّر محاكي أو جهاز في هذه البيئة، ولم تُلتقط لقطات شاشة.
الفحوص البصرية (RTL، تباين WCAG، بديل blur على API < 31، Predictive Back، Shared Element،
أهداف 48dp، خط 200%، وضع الطيران، Realtime، إشعارات 15 دقيقة) تبقى غير مُتحقَّق منها على جهاز.

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
