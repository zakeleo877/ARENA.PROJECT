# نتائج التحقق — المرحلة الثانية

تاريخ الفحص: 2026-11-04. هذه النتائج للنسخة المحلية من الكود، وليست شهادة على تشغيل Supabase حقيقي أو نجاح GitHub Actions.

## ما جرى التحقق منه

| الفحص | النتيجة والنطاق |
|---|---|
| `:core:testDebugUnitTest` | نجح: 16 حالة دون فشل أو تخطٍ. تشمل Ktor MockEngine، عدم إرسال أسعار في الطلب، أنواع الأخطاء، الحفاظ على الكاش، تنسيق الأرقام والروابط، وتباين أزواج الألوان الأساسية. |
| Room عبر Robolectric | أربع حالات ضمن العدد أعلاه: اختبارا الاستبدال والطابور على API 24 و28. قاعدة SQLite ومحاكاة Android محلية، وليست جهازًا فعليًا. |
| `:core:lintDebug` | نجح؛ تقرير الوحدة: `No issues found`. ليس فحص lint لكامل التطبيقين. |
| `:customer:compileDebugKotlin :admin:compileDebugKotlin` | نجحت ترجمة Kotlin للشاشتين المؤقتتين مع الثيم المشترك؛ لم يُبْنَ APK. |
| `:customer:dependencies --configuration debugRuntimeClasspath` | نجح بلا `FAILED`، ولم تظهر `auth-kt` أو `postgrest-kt` من Supabase. ليس تحليلًا لـAPK موقّع أو نسخة R8 نهائية. |
| `supabase/tests`: `npm ci && npm test` | 30 فحصًا محليًا، تشمل قراءة المنتج غير المتاح ورفض طلبه؛ تطبيق المخطط مرتين وبدائل Auth وStorage. |
| خط Tajawal | أُضيف Regular وMedium وBold من Google Fonts مع نسخة OFL في `docs/licenses/Tajawal-OFL.txt`. |
| Room | خرج `core/schemas/.../1.json` بواسطة معالج Room 3.0.3؛ لا تُستخدم الهجرة المدمرة، ولم تُختبر هجرة من إصدار سابق. |

تم تنزيل Android SDK محليًا لفحوص الترجمة الجزئية فقط. لم تُنفّذ عمليتا `assembleRelease` أو `test lint` الشاملتان، ولم يُنشأ APK. بيئة الفحص تقارب 1.9 GiB من الذاكرة: محاولة اختبار Room الأولى انتهت باختفاء Gradle daemon بعد استنفاد الذاكرة. نجح تشغيل الاختبارات لاحقًا بعد خفض ذاكرة Gradle والعامل وتنفيذ مهام الترجمة تدريجيًا، ولم يُحذف الإخفاق من السجل.

لجهاز محدود الذاكرة، يمكن بعد تنزيل التبعيات استخدام نمط مشابه:

```bash
./gradlew :core:testDebugUnitTest --no-daemon --max-workers=1 \
  -Dorg.gradle.jvmargs='-Xmx384m -XX:MaxMetaspaceSize=384m -Dfile.encoding=UTF-8' \
  -Pkotlin.compiler.execution.strategy=in-process -Psaeed.testHeap=384m
```

قد تكون هناك حاجة إلى موارد أعلى في الأجهزة والبيئات المختلفة. يعتمد Robolectric على تنزيل صور Android الاختبارية من Maven Central في أول تشغيل.

## حدود مهمة

- MockEngine يختبر صياغة HTTP والبيانات لا مشروع Supabase حقيقيًا. `sb_publishable_` يُرسل في `apikey`، بينما يُستخدم `Bearer` فقط لرمز anon JWT قديم.
- اختبار PGlite لا يغطي Auth أو PostgREST أو Storage الحقيقي أو التصادم والتزامن على خادم فعلي؛ يلزم مشروع Supabase تجريبي منفصل وفق البرومبت.
- `pending_orders` مخطط تخزين فقط، بلا مدير طابور أو WorkManager أو إشعارات. لا تستخدمه لطلبات فعلية بعد.
- لم تُجرَّب Hilt على تطبيق متكامل، أو الانتقالات والشفافية على شاشة وجهاز، أو إتاحة قارئ الشاشة، أو أحجام APK أو الصور الفعلية أو جلب البيانات دون إنترنت فعليًا.
- سير `.github/workflows/core-check.yml` أُضيف دون ادعاء نجاح تشغيله. وهو لا يوقّع ولا يُصدر APK.
