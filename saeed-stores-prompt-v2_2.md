# 🛍️ برومبت بناء تطبيقَي «محلات السعيد» و«إدارة محلات السعيد» (من الصفر)

> **طريقة الاستخدام:** انسخ هذا الملف كاملاً وأعطه لمساعد برمجة (يفضَّل أن يملك بيئة تنفيذ حقيقية: Gradle + Android SDK).
> **الإصدار:** 2.2 · **الحالة:** مشروع جديد كلياً بـ Kotlin + Jetpack Compose + Supabase (يحلّ محل نسخة WebView القديمة).
> **جديد في 2.2:** هوية لونية جديدة «Neon Void» (داكنة ثابتة، بلا Dynamic Color) · اختبارات أمان القاعدة على مشروع Supabase تجريبي منفصل · توضيح غرض «الموقع» · تخفيف فحص الصلاحيات المدمجة من AndroidX.
> **من 2.1:** رقم واتساب جديد · طابور طلبات بلا إنترنت (مع منع التكرار) · إشعارات خلفية محلية للمدير عبر WorkManager · ادّعاء المدير الأول من التطبيق بدل SQL يدوي · بناء وفحص عبر GitHub Actions.

---

## ١. دورك ومهمتك

أنت مهندس تطبيقات أندرويد متمرّس في Kotlin وJetpack Compose وMaterial 3. **مهمتك في هذه الجلسة:** بناء تطبيقَين أصليَّين (Native) من الصفر، مع كامل سكربت قاعدة البيانات، وتسليمهما بعد اجتياز معايير القبول في القسم ١١.

**قواعد قبل أن تبدأ:**
1. **قبل كتابة أي كود:** لخّص خطتك في 5–8 أسطر (الوحدات، المكتبات، ترتيب المراحل) وانتظر موافقتي.
2. **لا تضف ميزات وظيفية لم تُطلب.** البنية التقنية اللازمة لتشغيل الميزات المطلوبة (صلاحيات، إعدادات، ProGuard، اختبارات…) مسموحة وليست «ميزات جديدة».
3. **خارج النطاق تماماً:** التوصيل/الشحن، الدفع الإلكتروني، حسابات للزبائن، المخزون/الكميات المتاحة، أكواد الخصم، إشعارات FCM/Firebase (الإشعارات **المحلية** عبر WorkManager داخلة في النطاق، القسم ٥)، أي ذكاء اصطناعي.

---

## ٢. نظرة عامة

| العنصر | القيمة |
|---|---|
| **النوع** | تطبيقان أندرويد أصليان (APK) من مشروع Gradle واحد |
| **الجمهور** | زبائن ومدير محلات أدوات حلاقة وتجميل في الجزائر (ولاية الجلفة) |
| **اللغة** | العربية فقط، RTL كامل |
| **العملة** | دينار جزائري — تُقرأ من الإعدادات (الافتراضي: `دج`) |
| **الخلفية (Backend)** | Supabase: Postgres + Auth + Storage + Realtime |
| **الفكرة** | الزبون يتصفح المنتجات ويرسل طلباً. المدير يستقبل الطلبات ويدير المنتجات. قاعدة البيانات واحدة مشتركة. |

### التطبيقان (مطلب أساسي)

| | **تطبيق الزبائن** | **تطبيق الإدارة** |
|---|---|---|
| **الاسم الظاهر** | محلات السعيد | إدارة محلات السعيد |
| **applicationId** | `com.zakeleo877.saeedstore` | `com.zakeleo877.saeedadmin` |
| **الصلاحية** | تصفح وإرسال طلب | إدارة كاملة بعد تسجيل الدخول |
| **اسم ملف APK** | `saeed-store-1.0.apk` | `saeed-admin-1.0.apk` |

> ❌ **ممنوع** تطبيق واحد يبدّل الأدوار. تطبيقان منفصلان **كوداً وحزمةً**: تطبيق الزبائن **لا يحتوي أي كود إدارة** ولا مكتبات Auth/Storage/Realtime.

---

## ٣. المعمارية والتقنيات

**الهيكل (3 وحدات Gradle):**

```
saeed-stores/
├─ settings.gradle.kts · build.gradle.kts · gradle/libs.versions.toml
├─ core/        مكتبة مشتركة: designsystem (Color/Type/Shape/Spacing/Theme/مكونات)،
│               model، تنسيق الأرقام والتواريخ، Haptics، قراءة المنتجات والإعدادات + كاش Room
├─ customer/    application — com.zakeleo877.saeedstore
├─ admin/       application — com.zakeleo877.saeedadmin
├─ supabase/schema.sql
├─ scripts/db-security-tests.sh   (فحوص قاعدة البيانات ١٤–٢٥ في القسم ١١ — على مشروع Supabase تجريبي فقط)
├─ .github/workflows/build.yml   (بناء وفحص APK تلقائياً — القسم ١٠)
├─ local.properties.example   (SUPABASE_URL, SUPABASE_KEY — قيم فارغة)
└─ README.md                  خطوات الإعداد والبناء
```

| الجانب | القرار |
|---|---|
| اللغة والواجهة | Kotlin + Jetpack Compose + Material 3 (`material3`) |
| البنية | MVVM + Repository، `StateFlow`، تدفّق بيانات أحادي الاتجاه |
| الحقن | Hilt (KSP) |
| التنقل | Navigation Compose بمسارات Type-safe (`@Serializable`) |
| Supabase | مكتبة `supabase-kt`. **الزبائن:** `postgrest` فقط. **الإدارة:** `postgrest` + `auth` + `storage` + `realtime` |
| الصور | Coil مع كاش على القرص |
| الكاش المحلي | Room: المنتجات والإعدادات (للتصفح بلا إنترنت) + جدول `pending_orders` (الزبائن). السلة: Room أو DataStore |
| المهام الخلفية | WorkManager: طابور الطلبات المعلّقة (الزبائن) + الفحص الدوري للطلبات الجديدة (الإدارة). **بدون FCM وبدون Firebase** |
| SDK | `minSdk 24` · `compileSdk/targetSdk` = أحدث مستقر (35 على الأقل) |
| الإصدارات | Version Catalog. **تحقق من أحدث الإصدارات المستقرة بنفسك** (AGP, Kotlin, Compose BOM, Navigation ≥ 2.8, supabase-kt, Coil, Room, Hilt) ولا تعتمد على الذاكرة. |
| الإعدادات الحساسة | `SUPABASE_URL` و`SUPABASE_KEY` من `local.properties` عبر `BuildConfig`، والملف في `.gitignore` |
| الأسلوب | معرّفات الكود بالإنجليزية، كل نصوص الواجهة في `strings.xml` بالعربية (لا نصوص صلبة في الكود) |

**سلوك الاتصال:**
- **التصفح** يعمل بلا إنترنت من الكاش مع شريط «أنت غير متصل».
- **إرسال الطلب (الزبائن) يعمل بلا إنترنت عبر طابور محلي:** عند الضغط على «إرسال» يُنشئ التطبيق `client_id` (UUID) ويحفظ الطلب في `pending_orders` (الاسم، الهاتف، الملاحظات، الموقع، العناصر `product_id`+`qty`، `client_id`)، ثم يُجدوِل `WorkManager` مهمة بقيد `NetworkType.CONNECTED` وإعادة محاولة أُسّية (exponential backoff). عند نجاح `place_order` يُحذف الطلب من الطابور ويصل إشعار محلي «تم إرسال طلبك رقم #N» مع زر «إرسال عبر واتساب».
- **منع التكرار:** يُرسَل نفس `client_id` في كل محاولة، والخادم يُرجع نفس رقم الفاتورة ولا ينشئ طلباً ثانياً (القسم ٤).
- **أخطاء مؤقتة** (انقطاع، مهلة، 5xx) ← إعادة محاولة تلقائية. **أخطاء نهائية** (`invalid_customer` · `invalid_phone` · `invalid_items` · `rate_limited`) ← يتوقف الطلب ويُعلَّم «فشل» مع إشعار للزبون وأزرار «تعديل وإعادة الإرسال» / «حذف».
- الطلب المعلّق يبقى على الجهاز فقط (`allowBackup=false`) ويُحذف فور نجاحه. إذن `POST_NOTIFICATIONS` (أندرويد 13+) يُطلب **عند أول طلب معلّق فقط** مع شرح السبب، والتطبيق يعمل بدونه.

---

## ٤. قاعدة البيانات (Supabase)

> نفّذ `supabase/schema.sql` كاملاً من SQL Editor. السكربت **قابل لإعادة التشغيل** (idempotent).
> ⚠️ إن كان مشروع Supabase قديماً ويحوي طلبات حقيقية: خذ نسخة CSV منها أولاً، والأسهل استخدام **مشروع جديد**.

### الجداول والقيود

```sql
create table if not exists public.products (
  id          uuid primary key default gen_random_uuid(),
  name        text not null check (char_length(name) between 1 and 120),
  price       numeric(12,2) not null default 0 check (price >= 0),
  description text not null default '' check (char_length(description) <= 1000),
  image_url   text not null default '',
  category    text not null default 'عام' check (char_length(category) <= 60),
  available   boolean not null default true,
  sort_order  integer not null default 0,
  created_at  timestamptz not null default now()
);

create table if not exists public.orders (
  id         uuid primary key default gen_random_uuid(),
  number     bigint generated always as identity unique,   -- رقم فاتورة متسلسل
  customer   text not null default '' check (char_length(customer) <= 100),
  phone      text not null default '' check (char_length(phone) <= 20),
  notes      text not null default '' check (char_length(notes) <= 500),
  location   text not null default '' check (char_length(location) <= 200),  -- رابط خرائط اختياري لتسهيل التواصل مع الزبون: https://maps.google.com/?q=lat,lng
  channel    text not null default 'التطبيق' check (channel in ('التطبيق','يدوي')),
  status     text not null default 'جديد'
             check (status in ('جديد','قيد التحضير','جاهز','مُسلَّم','ملغي')),
  details    jsonb not null default '[]'::jsonb,           -- [{product_id,name,price,qty}] لقطة وقت الطلب
  total      numeric(12,2) not null default 0 check (total >= 0),
  client_id  uuid,                                         -- معرّف الطلب من الجهاز لمنع التكرار عند إعادة الإرسال
  created_at timestamptz not null default now()
);

create table if not exists public.settings (
  key       text primary key,
  value     text not null default '',
  is_public boolean not null default true
);

create table if not exists public.admins (
  user_id uuid primary key references auth.users(id) on delete cascade
);

alter table public.orders add column if not exists client_id uuid;   -- لقواعد بيانات قائمة
create unique index if not exists orders_client_id_key on public.orders (client_id);
create index if not exists orders_created_at_idx on public.orders (created_at desc);
create index if not exists orders_phone_idx      on public.orders (phone, created_at desc);
create index if not exists products_sort_idx     on public.products (sort_order, created_at);
```

### دالة المدير (بدل «أي مسجَّل = مدير»)

```sql
create or replace function public.is_admin() returns boolean
language sql stable security definer set search_path = ''
as $$ select exists (select 1 from public.admins where user_id = (select auth.uid())) $$;
```

### ادّعاء المدير الأول (بدل إدراج SQL يدوي)

يجعل أول حساب يسجّل دخوله في تطبيق الإدارة مديراً، **فقط إذا كان جدول `admins` فارغاً**؛ بعدها تُرجع الدالة `false` دائماً.

```sql
create or replace function public.claim_first_admin() returns boolean
language plpgsql security definer set search_path = ''
as $$
declare v_uid uuid := (select auth.uid());
begin
  if v_uid is null then return false; end if;
  lock table public.admins in share row exclusive mode;      -- يمنع سباقاً بين طلبين
  if exists (select 1 from public.admins) then return false; end if;
  insert into public.admins (user_id) values (v_uid);
  return true;
end $$;

revoke all on function public.claim_first_admin() from public, anon;
grant execute on function public.claim_first_admin() to authenticated;
```

### سياسات RLS (إلزامية)

```sql
alter table public.products enable row level security;
alter table public.orders   enable row level security;
alter table public.settings enable row level security;
alter table public.admins   enable row level security;   -- بلا سياسات = لا وصول عبر الـ API

-- تنظيف أي سياسات قديمة
drop policy if exists "public read products"   on public.products;
drop policy if exists "admin write products"   on public.products;
drop policy if exists "anon insert orders"     on public.orders;
drop policy if exists "admin manage orders"    on public.orders;
drop policy if exists "public read settings"   on public.settings;
drop policy if exists "admin manage settings"  on public.settings;

-- products: الزائر يرى المتاح فقط، والمدير يرى ويعدّل الكل
create policy "products read" on public.products for select to anon, authenticated
  using (available or public.is_admin());
create policy "products admin write" on public.products for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- orders: لا وصول مباشر للزائر إطلاقاً (الإدراج عبر الدالة place_order فقط)
create policy "orders admin all" on public.orders for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- settings
create policy "settings read" on public.settings for select to anon, authenticated
  using (is_public or public.is_admin());
create policy "settings admin write" on public.settings for all to authenticated
  using (public.is_admin()) with check (public.is_admin());

-- دفاع إضافي على مستوى الصلاحيات
revoke all on public.orders, public.admins from anon;
revoke all on public.admins from authenticated;
revoke insert, update, delete on public.products, public.settings from anon;
```

### إرسال الطلب: دالة آمنة تحسب السعر من الخادم

الزبون لا يرسل سعراً ولا مجموعاً؛ يرسل `product_id` و`qty` فقط (+ `p_client_id` لمنع التكرار)، والخادم يتحقق ويحسب.

```sql
drop function if exists public.place_order(text,text,text,text,jsonb);   -- التوقيع القديم (v2.0)

create or replace function public.place_order(
  p_customer text, p_phone text, p_notes text, p_location text, p_items jsonb,
  p_client_id uuid default null
) returns bigint
language plpgsql security definer set search_path = ''
as $$
declare
  v_details jsonb;
  v_total   numeric(12,2);
  v_number  bigint;
begin
  -- إعادة الإرسال بنفس المعرّف تُرجع نفس الرقم دون إنشاء طلب مكرَّر
  if p_client_id is not null then
    select number into v_number from public.orders where client_id = p_client_id;
    if found then return v_number; end if;
  end if;

  if char_length(trim(coalesce(p_customer,''))) < 2 then raise exception 'invalid_customer'; end if;
  if coalesce(p_phone,'') !~ '^[0-9+ ]{8,15}$' then raise exception 'invalid_phone'; end if;
  if jsonb_typeof(p_items) <> 'array'
     or jsonb_array_length(p_items) not between 1 and 50 then raise exception 'invalid_items'; end if;

  if (select count(*) from public.orders
       where phone = p_phone and created_at > now() - interval '1 hour') >= 5
  then raise exception 'rate_limited'; end if;

  select jsonb_agg(jsonb_build_object('product_id', p.id, 'name', p.name,
                                      'price', p.price, 'qty', i.qty)),
         sum(p.price * i.qty)
    into v_details, v_total
    from (select (e->>'product_id')::uuid as product_id, (e->>'qty')::int as qty
            from jsonb_array_elements(p_items) e) i
    join public.products p on p.id = i.product_id and p.available
   where i.qty between 1 and 99;

  -- أي منتج غير موجود/غير متاح/كمية خاطئة = رفض الطلب كله
  if v_details is null or jsonb_array_length(v_details) <> jsonb_array_length(p_items)
  then raise exception 'invalid_items'; end if;

  begin
    insert into public.orders (customer, phone, notes, location, channel, status, details, total, client_id)
    values (trim(p_customer), p_phone, coalesce(p_notes,''), coalesce(p_location,''),
            'التطبيق', 'جديد', v_details, v_total, p_client_id)
    returning number into v_number;
  exception when unique_violation then      -- محاولتان متزامنتان بنفس client_id
    select number into v_number from public.orders where client_id = p_client_id;
  end;

  return v_number;
end $$;

revoke all on function public.place_order(text,text,text,text,jsonb,uuid) from public;
grant execute on function public.place_order(text,text,text,text,jsonb,uuid) to anon, authenticated;
```

**رسائل الخطأ في التطبيق** (ترجمها للعربية): `invalid_customer` · `invalid_phone` · `invalid_items` (← حدّث المنتجات من الخادم وأخبر الزبون أن بعض المنتجات تغيّرت) · `rate_limited` (طلبات كثيرة، حاول لاحقاً).

### Realtime والتخزين

```sql
-- الإدارة تستقبل الطلبات الجديدة لحظياً (Realtime يحترم RLS: المدير فقط يستلم)
do $$ begin
  alter publication supabase_realtime add table public.orders;
exception when duplicate_object then null; end $$;

-- صور المنتجات: قراءة عامة، كتابة للمدير فقط
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('product-images','product-images', true, 1048576, array['image/webp','image/jpeg','image/png'])
on conflict (id) do nothing;

drop policy if exists "product images admin insert" on storage.objects;
drop policy if exists "product images admin update" on storage.objects;
drop policy if exists "product images admin delete" on storage.objects;
create policy "product images admin insert" on storage.objects for insert to authenticated
  with check (bucket_id = 'product-images' and public.is_admin());
create policy "product images admin update" on storage.objects for update to authenticated
  using (bucket_id = 'product-images' and public.is_admin());
create policy "product images admin delete" on storage.objects for delete to authenticated
  using (bucket_id = 'product-images' and public.is_admin());
```

### البيانات الأولية

```sql
insert into public.settings (key, value, is_public) values
  ('storeName',  'محلات السعيد',                 true),
  ('whatsapp',   '213776151686',                 true),
  ('currency',   'دج',                           true),
  ('footerNote', 'شكراً لثقتكم بمحلات السعيد 🛍️', true)
on conflict (key) do nothing;
```

> جدول `products` يبدأ **فارغاً** ويملؤه المدير من التطبيق. لا تُدخل منتجات وهمية.

### إنشاء حساب المدير (خطوات يدوية — وثّقها في README)

1. Supabase ← **Authentication → Sign In / Providers → Email**: عطّل **Allow new users to sign up**. وتأكد أن Anonymous sign-ins معطّل.
2. **Authentication → Users → Add user**: أنشئ حساب المدير ببريد وكلمة مرور قوية.
3. افتح **تطبيق الإدارة** وسجّل الدخول بهذا الحساب. بعد الدخول يستدعي التطبيق `rpc/is_admin`؛ إن كانت `false` يستدعي `rpc/claim_first_admin` فيصبح الحساب مديراً **فقط إذا كان جدول `admins` فارغاً** (أول مرة). بعدها لا يستطيع أي حساب آخر ادّعاء الإدارة. ⚠️ لهذا يجب تنفيذ الخطوة 1 (تعطيل التسجيل العام) **قبل** الخطوة 2.
4. *(اختياري لاحقاً — إضافة مدير آخر)* من SQL Editor:
   ```sql
   insert into public.admins (user_id)
   select id from auth.users where email = 'ANOTHER_ADMIN_EMAIL'
   on conflict do nothing;
   ```

---

## ٥. الشاشات والميزات

### 🛒 تطبيق «محلات السعيد» (الزبائن)

| الشاشة / الميزة | الوصف |
|---|---|
| **البداية** | شاشة Splash (SplashScreen API) بشعار الاسم |
| **الرئيسية** | شبكة منتجات تكيّفية (`GridCells.Adaptive`)، بطاقة: صورة، اسم، سعر، فئة. بحث نصي + شرائح فئات (`FilterChip`) |
| **تفاصيل المنتج** | انتقال Shared Element من البطاقة. صورة كبيرة، وصف، اختيار الكمية، زر «أضف إلى السلة». الضغط على الصورة يفتح عرضاً مكبّراً (تكبير بالقرص) في نافذة بخلفية ضبابية |
| **السلة** | عناصر قابلة للإغلاق بالسحب مع «تراجع» (Snackbar)، تعديل الكمية، المجموع، شارة عدّاد على أيقونة السلة. محفوظة محلياً |
| **إتمام الطلب** | `ModalBottomSheet` قابل للسحب: الاسم، الهاتف (تحقق من صيغة هاتف جزائري)، ملاحظات. زر اختياري «إضافة موقعي» مع نص توضيحي: «لمساعدة المحل على التواصل معك — اختياري» (إذن الموقع يُطلب **عند الضغط فقط** مع نص يشرح السبب)، والطلب يعمل بدونه. الموقع يُحفظ كرابط خرائط في `orders.location` |
| **نجاح الطلب** | نافذة بـ **رقم الفاتورة** (القادم من `place_order`) + أزرار: «إرسال عبر واتساب» (رسالة تحوي الرقم والعناصر والمجموع إلى رقم `whatsapp` من الإعدادات)، و«مشاركة موقعي مع المحل» إن وُجد. ثم تفريغ السلة |
| **عدم الاتصال** | تصفح من الكاش + شريط حالة. زر «إرسال الطلب» **يبقى مفعّلاً**: يُحفظ الطلب في الطابور وتظهر نافذة «تم حفظ طلبك وسيُرسل تلقائياً عند عودة الاتصال» (بلا رقم فاتورة بعد، وبلا زر واتساب حتى يُرسَل فعلاً) |
| **طلبات قيد الإرسال** | أيقونة/شارة في الشريط العلوي تفتح قائمة الطلبات المعلّقة والفاشلة: حالة كل طلب (بانتظار الاتصال · جارٍ الإرسال · فشل + السبب بالعربية)، مع «إعادة المحاولة الآن» و«تعديل» و«حذف». لا تظهر إن كان الطابور فارغاً |
| **حالات الفراغ/الخطأ** | رسالة عربية + أيقونة + زر «إعادة المحاولة»؛ هيكل تحميل (Skeleton/Shimmer) |

### 🔧 تطبيق «إدارة محلات السعيد»

| الشاشة / الميزة | الوصف |
|---|---|
| **تسجيل الدخول** | بريد + كلمة مرور (Supabase Auth)، تجديد الجلسة تلقائياً، تسجيل خروج. بعد الدخول: `is_admin()` ← إن كانت `false` جرّب `claim_first_admin()` (ينجح مرة واحدة فقط عند خلو `admins`)؛ إن بقيت `false` أخرج الحساب برسالة «هذا الحساب غير مخوَّل» |
| **التنقل** | `NavigationBar` سفلي: اللوحة · الطلبات · المنتجات · الإعدادات |
| **اللوحة** | بطاقات: طلبات جديدة، طلبات اليوم، إجمالي الطلبات، عدد المنتجات، **مبيعات المسلَّمة** (مجموع `total` للحالة «مُسلَّم»). تُحدَّث لحظياً |
| **الطلبات** | قائمة (الأحدث أولاً)، فلترة بالحالة، بحث (رقم/اسم/هاتف). تفاصيل الطلب في `ModalBottomSheet`: العناصر، المجموع، **تغيير الحالة** (جديد ← قيد التحضير ← جاهز ← مُسلَّم؛ و«ملغي» من أي حالة غير نهائية)، اتصال بالزبون، فتح واتساب، فتح موقع الزبون في الخرائط (إن وُجد) |
| **الفاتورة** | عرض فاتورة كاملة (اسم المتجر، رقم، تاريخ، زبون، جدول العناصر، المجموع، `footerNote`) مع **طباعة/مشاركة PDF** (`PdfDocument` + `FileProvider`) |
| **طلب يدوي** | اختيار منتجات وكميات + بيانات الزبون ← إدراج مباشر بـ `channel = 'يدوي'` |
| **الطلب الجديد لحظياً** | **التطبيق مفتوح:** اشتراك Realtime على `orders` ← شارة + Snackbar فوراً. |
| **إشعارات الطلبات في الخلفية** | **التطبيق مغلق:** `PeriodicWorkRequest` كل 15 دقيقة (الحد الأدنى في أندرويد) بقيد اتصال، يستعلم عن الطلبات `status='جديد'` الأحدث من آخر طلب شوهد (محفوظ في DataStore)، ويرسل **إشعاراً محلياً** (قناة «طلبات جديدة»، يفتح تفاصيل الطلب عند الضغط). لا FCM ولا Firebase. إذن `POST_NOTIFICATIONS` يُطلب بعد أول تسجيل دخول. **تنبيه صريح للمدير في الواجهة:** الإشعار قد يتأخر ~15 دقيقة أو أكثر (وضع Doze وتحسين البطارية)، وإن لم يصل فليُعفَ التطبيق من تحسين البطارية. |
| **المنتجات** | قائمة بالفئة والحالة. إضافة/تعديل في `ModalBottomSheet`: الاسم، السعر، الوصف، الفئة (اقتراح من الفئات الموجودة)، **صورة** (Photo Picker ← ضغط إلى WebP بعرض أقصى 1024px وجودة 80 ← رفع إلى `product-images/<uuid>.webp`، وحذف الصورة القديمة عند الاستبدال/الحذف)، مفتاح «متاح/مخفي»، الترتيب (أزرار أعلى/أسفل). الحذف بالسحب مع تأكيد وتراجع |
| **الإعدادات** | تعديل: اسم المتجر، رقم الواتساب، العملة، ملاحظة الفاتورة. + مفتاح «إشعارات الطلبات الجديدة» (يوقف/يشغّل مهمة WorkManager) + زر يفتح إعدادات استثناء البطارية للتطبيق |
| **التصدير** | CSV للمنتجات والطلبات عبر Storage Access Framework (UTF-8 مع BOM ليفتح بالعربية في Excel) |

**حالات الطلب:** جديد ← قيد التحضير ← جاهز ← مُسلَّم ← ملغي (القيم العربية هي نفسها المخزّنة في قاعدة البيانات؛ اربطها بـ `enum` في الكود). «مُسلَّم» تعني **تسليم الطلب للزبون من المحل**، وتُسمّى في الكود `HANDED_OVER` (لا `DELIVERED`).

**توضيح «الموقع» (لا علاقة له بالتوصيل):** الموقع اختياري ويُستخدم **للتواصل فقط**. ممنوع في الكود والنصوص تسميته «عنوان التوصيل» أو `deliveryAddress` أو ما شابه؛ الاسم المعتمد في الكود `customerLocationUrl` وفي الواجهة «موقعي».

---

## ٦. متطلبات التصميم والواجهة (UI/UX)

التركيز الأساسي على **جمال النوافذ، وتناسق الألوان، وسلاسة التحكم**. التزم بفلسفة **Material Design 3** مع **Jetpack Compose** وبهوية «Neon Void» الداكنة المعرَّفة أدناه، وطبّق التالي على **التطبيقين معاً** (الكود في `core:designsystem`):

### 1. نظام الألوان — هوية «Neon Void»

**القرارات:**
- الهوية **داكنة بالكامل وثابتة** على كل إصدارات أندرويد: لا Dynamic Color/Monet (لأنه يستبدل الهوية بألوان الخلفية)، ولا وضع فاتح في هذا الإصدار. فعّل `enableEdgeToEdge` بأيقونات شريط حالة فاتحة، وخلفية Splash بلون `Shadow Black`.
- المصدر الوحيد للألوان هو الجدول التالي، وتُعرَّف في `Color.kt` كـ `darkColorScheme` كاملة. لا ألوان hex صلبة داخل الشاشات.

| اسم اللون | Hex | الاستخدام المثالي في الواجهات |
|---|---|---|
| **Electric Cyan** (الأنوار الجليدية الساطعة) | `#00F0FF` | لون التوهج الأساسي، النصوص المضيئة، والمؤشرات البارزة |
| **Neon Blue** (الأزرق النيون العميق) | `#0077FF` | خلفيات العناصر النشطة، الحدود المضيئة (Borders)، والتدرجات العميقة |
| **Royal Purple** (البنفسجي الملكي للظل) | `#7000FF` | تدرجات الهالة المحيطة، خلفيات النوافذ العائمة (Dialogs)، وأزرار الإجراءات (FAB) |
| **Deep Void Violet** (الأرجواني الداكن) | `#240046` | لون خلفية الطبقات لإعطاء طابع الظلال والعمق السينمائي |
| **Shadow Black** (الأسود الفحمائي الحالك) | `#0A0A0F` | الخلفية الأساسية للشاشة (Dark Canvas) لزيادة تباين التوهج |
| **Spectral White** (الأبيض الطيفي الساطع) | `#E0FFFF` | ألوان النصوص الرئيسية، ومضات الشرارات، وبؤرة التركيز (Focus/Highlight) |

**ربطها بأدوار Material 3:**

| الدور | القيمة |
|---|---|
| `background` · `surface` | Shadow Black |
| `surfaceContainerLowest … Highest` | مزج Deep Void Violet فوق Shadow Black بنسب تصاعدية (≈ 6 · 10 · 14 · 18 · 24%) |
| `onBackground` · `onSurface` | Spectral White — و`onSurfaceVariant` بشفافية ≈ 70% |
| `primary` / `onPrimary` | Electric Cyan / Shadow Black (الأزرار الرئيسية والمؤشرات) |
| `primaryContainer` / `onPrimaryContainer` | Neon Blue بشفافية ≈ 35% فوق Shadow Black / Spectral White (العناصر النشطة مثل Chip المحدد ومؤشر شريط التنقل) |
| `secondary` / `onSecondary` | Royal Purple / Spectral White |
| `secondaryContainer` / `onSecondaryContainer` | Deep Void Violet / Spectral White |
| `tertiary` / `onTertiary` | Neon Blue / Shadow Black |
| `outline` · `outlineVariant` | Neon Blue · Neon Blue بشفافية ≈ 30% (الحدود المضيئة) |
| `error` وحالات النجاح/التحذير | لوني دلالة (أحمر/أخضر/كهرماني) مُعرَّفان في `Color.kt` بتباين كافٍ على الخلفية الداكنة، ولا يُستخدمان لغير الدلالة |

**قواعد الاستخدام:**
- **FAB:** Royal Purple بنص/أيقونة Spectral White. **النوافذ (Dialogs):** Royal Purple شبه شفاف (القسم ٢) بنص Spectral White. **الأوراق السفلية:** Deep Void Violet (`surfaceContainerHigh`).
- **التوهج (Glow/Halo):** تدرّج شعاعي أو خطي Royal Purple ← Neon Blue ← Electric Cyan يُرسم بـ `Brush` أو `drawBehind`. لا تستخدم `blur` لصناعة التوهج، وأبقِه على عناصر قليلة (FAB، العنصر المحدد، الزر الرئيسي) حفاظاً على 60fps.
- **التباين (WCAG AA):** نص Spectral White على Neon Blue الصلب ≈ 3.9:1 فقط، لذا Neon Blue **لا يكون خلفية لنص صغير**: استخدمه للحدود والمؤشرات والتدرجات، أو انزل بشفافيته كما في `primaryContainer`. تحقّق من التباين لكل زوج نص/خلفية في الثيم وأرفق النتائج.
- استخدم **أدوار الألوان** لا القيم المباشرة (`MaterialTheme.colorScheme.primary` …).
- لا تعتمد على اللون وحده لإيصال المعنى (حالة الطلب: لون + أيقونة + نص).

### 2. النوافذ والطبقات
- النوافذ (Dialogs) بخلفية **Royal Purple** والبطاقات (Cards) بلون `surfaceContainer*`، وكلاهما **شبه شفاف** (alpha ≈ 0.85–0.92) مع **تأثير ضبابي**: عند فتح نافذة أو ورقة سفلية طبّق `Modifier.blur` (أو `RenderEffect`) على **محتوى الشاشة الذي خلفها** على API 31+، مع **بديل لوني صلب** (بلا blur وبلا شفافية) للأجهزة غير الداعمة.
- زوايا منحنية ومتناسقة بـ `RoundedCornerShape`، وتُعرَّف أحجامها **مركزياً** داخل `Shapes` الخاصة بالثيم (extraSmall … extraLarge) ولا تُكتب أرقام الزوايا في الشاشات.
- استخدم **Tonal Elevation** (طبقات `surfaceContainer*`) بدل الظلال الحادة للتفريق بين الطبقات.
- نظام **spacing ثابت** (مثلاً 4/8/12/16/24/32dp) في كائن `Spacing` واحد، يُستخدم في كل الشاشات.
- **الخط:** Tajawal (أو Cairo) **مضمَّن** في `res/font` بالأوزان Regular/Medium/Bold (لا Downloadable Fonts لأن التطبيق يعمل بلا إنترنت). معرّف في `Typography` الثيم بمقياس Material 3 كامل.

### 3. الحركة والتفاعل
- استبدل الانتقالات الخطية بـ **حركات فيزيائية (spring)** عند الضغط والسحب وفتح العناصر (`spring(dampingRatio, stiffness)`، مع قيم موحّدة في ملف `Motion.kt`).
- **Shared Element Transitions** (`SharedTransitionLayout`) من عنصر القائمة (بطاقة منتج / صف طلب) إلى شاشة أو نافذة التفاصيل.
- **استجابة لمسية خفيفة (Haptic Feedback)** عند فتح النوافذ والضغط على الأزرار وأدوات التحكم، عبر غلاف واحد `Haptics` يستخدم أنسب `HapticFeedbackType` المتاح.
- حركات دخول وخروج ناعمة للعناصر: `AnimatedVisibility`، `animateContentSize`، `animateItem()` في القوائم.
- حافظ على **60fps+** وتجنّب إعادة التركيب غير الضرورية: مفاتيح ثابتة (`key`) في القوائم، نماذج `@Immutable/@Stable`، `derivedStateOf`، `remember` للقيم المكلفة، وتجنّب قراءة الحالة في مراحل مبكرة. افحص بـ Layout Inspector / Compose compiler reports.

### 4. الإيماءات
- **ModalBottomSheet** قابل للسحب ويستجيب لسرعة إصبع المستخدم.
- بطاقات وعناصر **قابلة للإغلاق بالسحب** (`SwipeToDismissBox`) عند الحاجة (عناصر السلة، حذف منتج) مع تأكيد/تراجع.
- فعّل **Predictive Back**: `android:enableOnBackInvokedCallback="true"` في الـ Manifest، وأنيميشن تقلّص الشاشة أو النافذة قبل إتمام الرجوع (Navigation Compose ≥ 2.8، و`PredictiveBackHandler` للنوافذ المخصّصة).

### 5. معايير الجودة
- **واجهة متجاوبة:** هاتف، قابل للطي، تابلت (`WindowSizeClass`، شبكات تكيّفية، `NavigationRail` على الشاشات العريضة بدل الشريط السفلي).
- **RTL كامل**: `supportsRtl="true"` وفرض `LayoutDirection.Rtl` على مستوى الثيم، أيقونات `AutoMirrored`، واللغة العربية فقط (`localeFilters`).
- **إتاحة الوصول (Accessibility):** أهداف لمس **لا تقل عن 48dp**، `contentDescription` لكل أيقونة/صورة تفاعلية، `semantics` للحالات، تباين كافٍ (WCAG AA)، ودعم تكبير الخط حتى 200% دون كسر التخطيط.
- **أرقام وتواريخ:** أرقام لاتينية بفاصل آلاف، `19,500 دج` (العملة من الإعدادات)، وتاريخ `dd/MM/yyyy HH:mm`.
- **كود منظم:** ثيم مركزي (`Color`, `Type`, `Shape`, `Spacing`, `Motion`, `Theme`) ومكوّنات قابلة لإعادة الاستخدام (`SaeedCard`, `SaeedDialog`, `SaeedBottomSheet`, `EmptyState`, `PriceText`, `StatusChip`…) مع `@Preview` لكل منها (الوضع الداكن).
- شاشة الإدارة بنفس نظام التصميم، وتتميّز بشارة «إدارة» في الشريط العلوي وأيقونة تطبيق مختلفة.

---

## ٧. الأمان (غير قابل للتفاوض)

| القاعدة | السبب |
|---|---|
| ❌ لا `service_role` في أي مكان من المشروع | يتجاوز RLS كلياً |
| ✅ مفتاح `publishable/anon` فقط في التطبيقين، من `local.properties` | الحماية الحقيقية في RLS |
| ✅ التسجيل العام (Sign-ups) **معطّل**، والمدير يُحدَّد بجدول `admins` + `is_admin()` | «أي مسجَّل = مدير» ثغرة |
| ✅ الزبون لا يملك أي وصول مباشر لجدول `orders`؛ الإرسال عبر `place_order` فقط | منع التزوير والتخريب |
| ✅ الأسعار والمجموع تُحسب في الخادم | منع تلاعب العميل |
| ✅ قيود `check` في قاعدة البيانات، والتحقق في الواجهة للتجربة فقط | التحقق في الواجهة ليس حماية |
| ✅ تطبيق الزبائن بلا كود/مكتبات Auth وStorage وRealtime | تقليل سطح الهجوم |
| ✅ `allowBackup="false"`، و`usesCleartextTraffic="false"`، وR8 مفعّل في release | حماية البيانات والحزمة |
| ❌ لا مفاتيح ذكاء اصطناعي في الكود | سبق أن تسرّب مفتاح من مشروع سابق |
| ❌ لا كلمات مرور/توكنات/ملف keystore في Git أو في المحادثة | تسريب = فقدان السيطرة |

---

## ٨. 🚫 قيود صارمة

| # | القيد |
|---|---|
| 1 | **لا ميزة توصيل أو شحن**، ولا أثر لها في الكود أو النصوص |
| 2 | **لا مفتاح ذكاء اصطناعي** من أي نوع |
| 3 | **تطبيقان منفصلان فقط**، لا تطبيق موحّد بتبديل أدوار |
| 4 | **لا ترفع إلى Git:** `build/`، `.gradle/`، `local.properties`، `*.jks`، `keystore.properties`، `*.apk` |
| 5 | **لا بيانات عملاء حقيقية** في الاختبارات أو العيّنات |
| 6 | **التوزيع عبر واتساب فقط** (لا Google Play)، يُرسل الملف كـ **مستند** لا كصورة، وأسماء ملفات APK لاتينية فقط |
| 7 | **كل نصوص الواجهة بالعربية** في `strings.xml`، لا نصوص صلبة |
| 8 | **الألوان من جدول «Neon Void» فقط** (القسم ٦)، والاختبارات الأمنية على قاعدة تجريبية فقط (القسم ١٠) |

---

## ٩. ⚠️ دروس تقنية (من تجارب سابقة — خذها بجدية)

| الدرس | التفصيل |
|---|---|
| **`resources.arsc` غير مضغوط** | أندرويد 11+ يرفض تثبيت APK يستهدف API 30+ إن كان `resources.arsc` مضغوطاً. AGP يتولى ذلك عادةً، لكن **تحقق منه دائماً** (القسم ١١) ولا تعيد حزم APK يدوياً. التوقيع يأتي **آخراً** دائماً. |
| **ذاكرة Gradle** | فشل Gradle سابقاً بنفاد الذاكرة على جهاز < 4GB. Compose يحتاج ≥ 4GB (يُفضَّل 8GB). اضبط `org.gradle.jvmargs=-Xmx3g` و`kotlin.daemon.jvmargs`. إن كانت البيئة أضعف فابنِ على جهاز آخر أو GitHub Actions، **ولا تكرر محاولات البناء الفاشلة بعشوائية**. |
| **`android:exported`** | إلزامي لكل نشاط/خدمة/مستقبِل فيه `intent-filter` (targetSdk ≥ 31) |
| **أسماء ملفات لاتينية** | الأسماء العربية لملفات الإخراج تكسر الأدوات. اسم التطبيق العربي في `strings.xml` فقط |
| **قراءة المفتاح** | مفتاح `sb_publishable_…` ليس JWT: اتركه لمكتبة `supabase-kt` ولا تبنِ ترويسات `Authorization` يدوياً |
| **R8** | فعّل `minifyEnabled` و`shrinkResources` وأضف قواعد keep اللازمة لـ `kotlinx.serialization` وsupabase-kt، واختبر نسخة release فعلاً (لا debug فقط) |
| **WorkManager** | أقل فترة للمهام الدورية 15 دقيقة، ولا يضمن التوقيت الدقيق. لا تحاول التحايل بخدمة أمامية دائمة (تستنزف البطارية). تأكد من أن مهمة الزبائن تستخدم `Constraints(NetworkType.CONNECTED)` وأن `client_id` يُولَّد **مرة واحدة** عند إنشاء الطلب لا عند كل محاولة. |
| **ملفات توقيع** | `saeed-release.jks` يُولَّد محلياً ويُنسخ خارج مساحة العمل. فقدانه = عدم إمكانية تحديث التطبيق |

---

## ١٠. خطوات البناء والتسليم

```bash
# المتطلبات: JDK 17 + Android SDK (platform + build-tools) + ذاكرة كافية (القسم ٩)

# ١ الإعداد المحلي
cp local.properties.example local.properties     # املأ SUPABASE_URL و SUPABASE_KEY (Project Settings → API)

# ٢ قاعدة البيانات: نفّذ supabase/schema.sql ثم خطوات حساب المدير (القسم ٤)

# ٣ اختبارات وفحص
./gradlew test lint

# ٤ بناء النسختين (release موقَّع من keystore.properties الخارج عن Git)
./gradlew :customer:assembleRelease :admin:assembleRelease

# ٥ إعادة تسمية المخرجات لاتينياً
#   saeed-store-1.0.apk   saeed-admin-1.0.apk
```

### بناء عبر GitHub Actions (إن لم تتوفر لدى المساعد بيئة Gradle + Android SDK)

إن كان المساعد أداة دردشة متصلة بمستودعاتي في GitHub بلا بيئة بناء، فيُنشئ `.github/workflows/build.yml` الذي:
1. يعمل عند `push` وعند التشغيل اليدوي (`workflow_dispatch`) على `ubuntu-latest` مع JDK 17 وAndroid SDK، وبإعدادات ذاكرة Gradle مناسبة للـ runner.
2. يقرأ `SUPABASE_URL` و`SUPABASE_KEY` وبيانات التوقيع (`KEYSTORE_BASE64`، `KEYSTORE_PASSWORD`، `KEY_ALIAS`، `KEY_PASSWORD`) من **GitHub Secrets فقط**؛ أضيفها أنا من Settings → Secrets، ولا تُكتب في المستودع ولا في المحادثة. ويولّد `local.properties` و`keystore.properties` أثناء التشغيل فقط.
3. ينفّذ `./gradlew test lint :customer:assembleRelease :admin:assembleRelease`.
4. ينفّذ فحوص الحزمة ١–١٠ وفحوص المستودع ١١–١٣ (القسم ١١) ويطبع ناتج كل أمر في السجل، ويفشل إن فشل أي فحص.
5. يرفع ملفي APK (بالأسماء اللاتينية) وسجل الفحوص كـ `artifacts`.

يضع المساعد أيضاً `scripts/db-security-tests.sh` لفحوص قاعدة البيانات ١٤–٢٥، وهو يعمل **على مشروع Supabase تجريبي منفصل فقط وليس على الإنتاج**:
- أنشئ مشروعاً تجريبياً ثانياً، ونفّذ عليه `schema.sql` نفسه، وعطّل التسجيل العام فيه كما في الإنتاج، وأنشئ فيه حساباً عادياً (غير مدير) للفحوص ٢٢–٢٣.
- يقرأ السكربت `TEST_SUPABASE_URL` و`TEST_SUPABASE_KEY` و`TEST_USER_EMAIL` و`TEST_USER_PASSWORD` من متغيرات البيئة (أو GitHub Secrets) ولا يقرأ مفاتيح الإنتاج إطلاقاً، و**يتوقف فوراً** إن تطابق `TEST_SUPABASE_URL` مع `SUPABASE_URL` الخاص بالإنتاج.
- يستخدم **أرقام هاتف وبيانات وهمية فقط**، ويحذف طلباته التجريبية في نهاية التشغيل.
- لا يغني عن التحقق اليدوي على الإنتاج: يتأكد المالك من لوحة Supabase (Authentication ← Providers ← Email) أن Allow new users to sign up معطّل وأن Anonymous sign-ins معطّل، ويوثّق ذلك في README (لا يُنفَّذ `POST /auth/v1/signup` على الإنتاج لأنه قد ينشئ مستخدماً فعلياً).

**ما يُسلَّم:** كود المشروع كاملاً، `supabase/schema.sql`، `README.md` (إعداد Supabase، حساب المدير، البناء، التوقيع، الإصدار)، وملفا APK، **وسجل نتائج معايير القبول الفعلية**.

---

## ١١. ✅ معايير القبول (لا تُسلِّم قبل استيفائها كلها، واعرض الناتج الفعلي)

### فحوص الحزمة (لكل APK)

| # | الفحص | الأمر / المتوقَّع |
|---|---|---|
| 1 | `resources.arsc` غير مضغوط | `unzip -v app.apk \| grep resources.arsc` ← العمود الثاني = `Stored` |
| 2 | المحاذاة | `zipalign -c -p 4 app.apk` |
| 3 | التوقيع | `apksigner verify --verbose --print-certs app.apk` (v2 على الأقل) |
| 4 | الهوية | `aapt2 dump badging app.apk` ← الاسم الصحيح لكل تطبيق، `minSdk 24`، `versionName 1.0` |
| 5 | غير قابل للتصحيح | نفس الأمر ← لا يحتوي `application-debuggable` |
| 6 | `exported` | `aapt2 dump xmltree app.apk --file AndroidManifest.xml` ← موجود حيث يلزم |
| 7 | سلامة الحزمة | `unzip -t app.apk` |
| 8 | الصلاحيات | `aapt2 dump permissions app.apk`. **المطلوبة** — الزبائن: `INTERNET` + `ACCESS_NETWORK_STATE` + `POST_NOTIFICATIONS` + الموقع الاختياري (`ACCESS_COARSE_LOCATION` و`ACCESS_FINE_LOCATION` معاً لأن أندرويد 12+ يطلبهما معاً). الإدارة: `INTERNET` + `ACCESS_NETWORK_STATE` + `POST_NOTIFICATIONS` فقط، بلا موقع. **المقبولة تلقائياً** إن ظهرت في المانيفست المدمج بسبب مكتبات AndroidX (مثل WorkManager وcore): `WAKE_LOCK` و`RECEIVE_BOOT_COMPLETED` و`FOREGROUND_SERVICE` و`${applicationId}.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`. **أي صلاحية أخرى** تُعرَض مع مصدرها (`./gradlew :app:processReleaseMainManifest` وملف `manifest-merger-release-report.txt`)؛ ما كان من مكتبة وسيطة مقبول مع تبريره، وما أضافه كودنا خارج القائمة أعلاه مرفوض |
| 9 | **عزل الأكواد** | `apkanalyzer dex packages --defined-only saeed-store-1.0.apk \| grep -i saeedadmin` ← **فارغ** |
| 10 | الحجم منطقي | كل APK ≤ 20 ميغابايت |

### فحوص المستودع

| # | الفحص | الأمر / المتوقَّع |
|---|---|---|
| 11 | لا أثر للتوصيل | `grep -rniwE "shipping\|delivery\|توصيل\|شحن" --exclude-dir={.git,build,.gradle} .` ← فارغ (خارج README الذي قد يذكر أنها خارج النطاق) |
| 12 | لا أسرار | `grep -rnE "AIza[0-9A-Za-z_-]{35}\|sk-[A-Za-z0-9]{20,}\|service_role\|eyJ[A-Za-z0-9_-]{20,}\.[A-Za-z0-9_-]{20,}" --exclude-dir={.git,build,.gradle} .` ← فارغ |
| 13 | الاختبارات | `./gradlew test lint` بلا أخطاء (اختبارات وحدة لحساب السلة، تنسيق الأرقام، تحويل الحالات، ومعالجة أخطاء `place_order`) |

### اختبار الأمان على قاعدة البيانات (بمفتاح anon — اعرض النتائج)

| # | الاختبار | المتوقَّع |
|---|---|---|
| 14 | `GET /rest/v1/orders` | رفض أو قائمة فارغة، **لا بيانات** |
| 15 | `POST /rest/v1/orders` مباشرة | رفض (42501) |
| 16 | `POST/PATCH/DELETE` على `products` و`settings` | رفض |
| 17 | `POST /auth/v1/signup` | رفض (التسجيل معطّل) |
| 18 | `rpc/place_order` بعناصر صحيحة | يُرجع رقماً؛ والمجموع يساوي مجموع أسعار الخادم |
| 19 | `rpc/place_order` بمنتج مخفي أو كمية 0 أو 100 | `invalid_items` |
| 20 | `rpc/place_order` 6 مرات بنفس الهاتف خلال ساعة (بمعرّفات `p_client_id` مختلفة) | السادس `rate_limited` |
| 21 | قراءة `products` | المنتجات `available = true` فقط |
| 22 | حساب مسجَّل **غير** موجود في `admins` | لا يرى `orders` ولا يكتب |
| 23 | `rpc/claim_first_admin` من حساب مسجَّل غير مدير بينما `admins` غير فارغ | يُرجع `false` ولا يُضاف أحد |
| 24 | `rpc/claim_first_admin` بمفتاح anon (بلا تسجيل دخول) | رفض (42501) |
| 25 | `rpc/place_order` مرتين بنفس `p_client_id` | **نفس الرقم** في المرتين، وصف واحد فقط في `orders` |

### فحوص الواجهة (على محاكي/جهاز إن توفّر، مع لقطات شاشة)

RTL سليم · هوية Neon Void الداكنة متطابقة على API 24 وعلى API 31+ (بلا Dynamic Color) · تباين النصوص WCAG AA · بديل blur على API < 31 · Predictive Back · Shared Element من القائمة للتفاصيل · الأهداف ≥ 48dp · خط 200% بلا كسر · تصفح بلا إنترنت · لوحة الإدارة تُحدَّث عند وصول طلب جديد · **إرسال طلب في وضع الطيران ثم إعادة الاتصال ← يصل مرة واحدة فقط بنفس الرقم، ويصل إشعار للزبون** · **إشعار طلب جديد على جهاز الإدارة والتطبيق مغلق** (ضمن ~15–30 دقيقة) · طلب بخطأ نهائي يظهر «فشل» في قائمة الطلبات المعلّقة.

---

## ١٢. أسلوب العمل المطلوب منك

1. **المراحل:** (أ) الخطة ← موافقتي · (ب) هيكل المشروع + `schema.sql` · (ج) وحدة `core` (الثيم، الأنظمة، الاتصال) · (د) تطبيق الزبائن · (هـ) تطبيق الإدارة · (و) الاختبارات والفحوص والتسليم. **توقّف بعد كل مرحلة** وأرني ما أُنجز.
2. **شخّص قبل أن تعيد البناء** — البناء العشوائي المتكرر يعيد إنتاج نفس الخطأ.
3. **اقرأ ناتج الأدوات بدقة** قبل أن تستنتج.
4. **لا تدّعي النجاح** — اعرض ناتج الأوامر الفعلي لكل معيار في القسم ١١، وإن لم تستطع تشغيل فحص فقل ذلك صراحةً (أو أرفق رابط تشغيل GitHub Actions الذي نفّذه).
5. **بلّغ عن العوائق بصراحة** — «لا أستطيع» أفضل من وهم التقدّم. وإن تعارض أي بند هنا مع الواقع التقني فاذكر التعارض واقترح بديلاً بدل الالتفاف عليه.
6. **كل ردودك بالعربية.**
