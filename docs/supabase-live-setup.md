# دليل ربط Supabase الإنتاجي

هذا الملف هو الإجراء التنفيذي الكامل: ما يُضبط، وكيف يُتحقق، وما الدليل
المطلوب على كل خطوة. لا يُعلن أي بند مكتملاً هنا؛ الأرقام تأتي من تشغيل
GitHub Actions الحقيقي في `docs/live-results.md`.

## ١. المفتاحان في GitHub Secrets

المكان الوحيد: **Settings ← Secrets and variables ← Actions ← تبويب Secrets**
ولا في تبويب Variables. الاسم يجب أن يكون حرفياً:

| الاسم | القيمة | ملاحظة |
|---|---|---|
| `SUPABASE_URL` | `https://<project-ref>.supabase.co` | بلا `/` في النهاية |
| `SUPABASE_KEY` | `sb_publishable_...` | المفتاح العام فقط |

**لا** يوضع `sb_secret_...` في أي إعداد. البناء يرفضه في أربع نقاط مختلفة:

| الموضع | ما يفعله |
|---|---|
| خطوة `Generate local.properties` | يُفشل البناء إذا بدأ المفتاح بـ `sb_secret_` |
| `customer/build.gradle.kts` و`admin/build.gradle.kts` | يرفض المفتاح السري وأي JWT بدور عالٍ (المفتاح القديم الذي يتجاوز RLS) |
| `scripts/verify-repo.sh` (12) | يرفض أي مفتاح سري أو عام يُلصق في Git |
| `scripts/verify-apk-keys.sh` (12b) | يقرأ داخل الـ APK ويؤكد عدم وجود مفتاح سري |

## ٢. تطبيق المخطط

`supabase/schema.sql` يحتوي كل شيء في معاملة واحدة (`begin ... commit`).
يُنفَّذ في **SQL Editor** في لوحة المشروع، مرة واحدة، ولا يحتاج صلاحيات خاصة.

ما يفعله: ينشئ الجداول الأربعة، يفعّل RLS، **يحذف كل السياسات القديمة** على
تلك الجداول ثم ينشئ سياساته، ينشئ `place_order` و`claim_first_admin`، ينشئ
دلو `product-images` عاماً، ويزرع الإعدادات الأربعة.

**آمن للتكرار**: تحققت من ثلاثة تطبيقات متتالية، والنتيجة متطابقة
(٠ منتج، ٤ إعدادات، ٥ سياسات، ٤ سياسات storage، والدلو عام، والطلب في Realtime).

ما **لا** يفعله: لا يحذف جدولاً ولا صفّاً ولا حساب مستخدم. `drop` الوحيدة
موجودة على السياسات وعلى التوقيع القديم لـ `place_order`.

## ٣. قبل التشغيل: إعدادان في لوحة Supabase

| الإعداد | لماذا | أين |
|---|---|---|
| تعطيل التسجيل العام | `claim_first_admin` يصبح بلا معنى إذا استطاع أي شخص إنشاء حساب | Authentication ← Sign In / Providers ← إيقاف «Allow new users to sign up» |
| إنشاء حساب المدير | لا يوجد مسار لإنشاء حساب إداري من التطبيق | Authentication ← Users ← Add user |

ثم يطالب حساب المدير بالدور الإداري من شاشة الدخول في تطبيق الإدارة
(`claim_first_admin` ينجح مرة واحدة فقط).

## ٤. الفحوص الحقيقية

### أ. الاتصال والترجمة والبناء — `Build and verify`

يعمل تلقائياً عند كل push. ما يجب أن يظهر بعد ضبط السرّين:

```
PASS: local.properties was written with the project address and a publishable key.
      address length: 40, key length: 47, key prefix: sb
PASS: the project address answers and the key is accepted.
PASS: the catalog is readable, so the schema is applied.
PASS: com.zakeleo877.saeedstore.apk carries a publishable key
PASS: com.zakeleo877.saeedstore.apk contains no secret key
```

إن لم يُضبط السرّان، يبقى البناء يعمل بعنوان مؤقت وتُفشل الخطوة الأخيرة
`Require the project configuration` العملية — وهذا هو السلوك المقصود.

### ب. ١٩ فحص وظيفي على المشروع — `Live Supabase checks`

يدويّ، ويحتاج أسراراً إضافية:

| الاسم | الاستخدام |
|---|---|
| `TEST_SUPABASE_URL` | عنوان المشروع |
| `TEST_SUPABASE_KEY` | المفتاح العام نفسه |
| `TEST_USER_EMAIL` / `TEST_USER_PASSWORD` | حساب زبون موجود |
| `TEST_ADMIN_EMAIL` / `TEST_ADMIN_PASSWORD` | حساب في جدول `admins` |
| المتغيّر `TEST_PROJECT_CONFIRMED=true` | إقرار صريح أن المشروع آمن للكتابة |

ويغطي: تسجيل الدخول والخروج، رفض كلمة المرور الخاطئة، الكتالوج
والتصنيفات والإعدادات، **تحميل الصورة فعلياً**، إنشاء الطلب مرتين بنفس
المعرّف (لا تكرار)، رفض كمية غير صالحة، وصول الطلب للوحة الإدارة، تغيير
الحالة وحفظها، وفصل صلاحيات الزبون عن المدير — ثم حذف الطلب.

### ج. ١٣ فحص أمني ١٤–٢٥ — `Live Supabase checks` (وضع test)

نفس السكربت المستخدم في CI المحلي لكن على مشروعك: قراءة الطلبات مجهولة،
الإدراج المباشر، الكتابة على المنتجات والإعدادات، `claim_first_admin`
مرتين، حد الطلبات، وإعادة الإرسال.

## ٥. بخصوص القيم التي مرّت في المحادثة

المفتاح العام `sb_publishable_...` **ليس سرّاً بحكم التصميم**: يُرسل في ترويسة
`apikey` من التطبيق ويُستخرج من أي APK. الحماية الحقيقية هي RLS، والفحص
`9a-9d` يثبتها.

المفتاح `sb_secret_...` **سرّ فعلي**: يتجاوز كل سياسات RLS. وقد ظهر في نص
محادثة، فأي مكان نسخته فيه يُعد مكشوفاً. لم يُستخدم في أي سطر من المشروع
ولن يُستخدم. **التوصية: دوّره من لوحة Supabase** (Settings ← API Keys ←
إبطال القديم وإنشاء جديد). المشروع لا يعتمد عليه في شيء، فالتدوير لا يكسر
شيئاً.

## ٦. قاعدة لا تُخترق

لا تُعطّل Legacy API Keys في لوحة Supabase قبل أن تُظهر الفحوص أعلاه
`0 failed` على المشروع. الترحيل يُثبت بالنتائج، لا بالثقة.
