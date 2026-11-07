-- محلات السعيد: schema for a dedicated Supabase project (PostgreSQL 15+).
-- Run in the Supabase SQL editor, not as an application user.
-- Existing policies on the four managed public tables are replaced on every run.
-- Back up an existing database first; prefer a new dedicated project.
-- Disable public and anonymous sign-up before any authenticated user can claim admin.

begin;

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
    number     bigint generated always as identity unique,
    customer   text not null default '' check (char_length(customer) <= 100),
    phone      text not null default '' check (char_length(phone) <= 20),
    notes      text not null default '' check (char_length(notes) <= 500),
    location   text not null default '' check (char_length(location) <= 200),
    channel    text not null default 'التطبيق' check (channel in ('التطبيق', 'يدوي')),
    status     text not null default 'جديد'
               check (status in ('جديد', 'قيد التحضير', 'جاهز', 'مُسلَّم', 'ملغي')),
    details    jsonb not null default '[]'::jsonb,
    total      numeric(12,2) not null default 0 check (total >= 0),
    client_id  uuid,
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

-- Allows upgrading the specific older version that did not have this column.
alter table public.orders add column if not exists client_id uuid;
create unique index if not exists orders_client_id_key on public.orders (client_id);
create index if not exists orders_created_at_idx on public.orders (created_at desc);
create index if not exists orders_phone_idx on public.orders (phone, created_at desc);
create index if not exists orders_phone_digits_created_at_idx
    on public.orders ((regexp_replace(phone, '[^0-9]', '', 'g')), created_at desc);
create index if not exists products_sort_idx on public.products (sort_order, created_at);

alter table public.products enable row level security;
alter table public.orders enable row level security;
alter table public.settings enable row level security;
alter table public.admins enable row level security;

-- SECURE DEFINER functions must keep an empty search path and qualified references.
create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
    select exists (
        select 1 from public.admins where user_id = (select auth.uid())
    );
$$;
revoke all on function public.is_admin() from public, anon, authenticated;
grant execute on function public.is_admin() to anon, authenticated;

create or replace function public.claim_first_admin()
returns boolean
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_uid uuid := (select auth.uid());
begin
    if v_uid is null then
        return false;
    end if;
    -- Serialize claim attempts so only one first manager can be inserted.
    lock table public.admins in share row exclusive mode;
    if exists (select 1 from public.admins) then
        return false;
    end if;
    insert into public.admins (user_id) values (v_uid);
    return true;
end;
$$;
revoke all on function public.claim_first_admin() from public, anon, authenticated;
grant execute on function public.claim_first_admin() to authenticated;

-- Remove every policy on the managed tables; permissive policies are OR-ed by
-- PostgreSQL, so leaving an unknown legacy policy in place can defeat RLS.
do $drop_managed_policies$
declare
    p record;
begin
    for p in
        select schemaname, tablename, policyname
        from pg_catalog.pg_policies
        where schemaname = 'public'
          and tablename in ('products', 'orders', 'settings', 'admins')
    loop
        execute format('drop policy if exists %I on %I.%I',
                       p.policyname, p.schemaname, p.tablename);
    end loop;
end;
$drop_managed_policies$;

-- All catalog rows, including unavailable products, are visible for disabled UI.
-- Ordering remains restricted to available products inside place_order.
create policy "products read" on public.products
    for select to anon, authenticated
    using (true);
create policy "products admin write" on public.products
    for all to authenticated
    using ((select public.is_admin()))
    with check ((select public.is_admin()));

-- The anonymous role receives no direct order-table permission or policy.
create policy "orders admin all" on public.orders
    for all to authenticated
    using ((select public.is_admin()))
    with check ((select public.is_admin()));

create policy "settings read" on public.settings
    for select to anon, authenticated
    using (is_public or (select public.is_admin()));
create policy "settings admin write" on public.settings
    for all to authenticated
    using ((select public.is_admin()))
    with check ((select public.is_admin()));
-- No direct policy or privilege is granted for admins.

revoke all on public.products, public.settings, public.orders, public.admins
    from public, anon, authenticated;
grant usage on schema public to anon, authenticated;
grant select on public.products, public.settings to anon, authenticated;
grant insert, update, delete on public.products, public.settings to authenticated;
grant select, insert, update, delete on public.orders to authenticated;

-- Remove the earlier signature so PostgREST sees one unambiguous RPC.
drop function if exists public.place_order(text, text, text, text, jsonb);
create or replace function public.place_order(
    p_customer text,
    p_phone text,
    p_notes text,
    p_location text,
    p_items jsonb,
    p_client_id uuid default null
)
returns bigint
language plpgsql
security definer
set search_path = ''
as $$
declare
    v_details jsonb;
    v_total numeric(12,2);
    v_number bigint;
    v_phone_digits text;
begin
    -- An order ID is generated once on the device. Serialize retries with the
    -- same ID, then return the original number without re-applying rate limits.
    if p_client_id is not null then
        perform pg_catalog.pg_advisory_xact_lock(
            pg_catalog.hashtextextended('saeed:order:' || p_client_id::text, 0)
        );
        select number into v_number from public.orders where client_id = p_client_id;
        if found then
            return v_number;
        end if;
    end if;

    if char_length(trim(coalesce(p_customer, ''))) not between 2 and 100 then
        raise exception 'invalid_customer';
    end if;
    v_phone_digits := regexp_replace(coalesce(p_phone, ''), '[^0-9]', '', 'g');
    if coalesce(p_phone, '') !~ '^[0-9+ ]{8,15}$'
       or char_length(v_phone_digits) not between 8 and 15 then
        raise exception 'invalid_phone';
    end if;
    if char_length(coalesce(p_notes, '')) > 500 then
        raise exception 'invalid_notes';
    end if;
    if char_length(coalesce(p_location, '')) > 200 then
        raise exception 'invalid_location';
    end if;
    if p_items is null or jsonb_typeof(p_items) <> 'array' then
        raise exception 'invalid_items';
    end if;
    if jsonb_array_length(p_items) not between 1 and 50 then
        raise exception 'invalid_items';
    end if;

    -- Phone formatting cannot bypass the limit, and concurrent requests for
    -- the same number cannot race past it. This is not an IP-based abuse limit.
    perform pg_catalog.pg_advisory_xact_lock(
        pg_catalog.hashtextextended('saeed:phone:' || v_phone_digits, 0)
    );
    if (
        select count(*) from public.orders
        where regexp_replace(phone, '[^0-9]', '', 'g') = v_phone_digits
          and created_at > now() - interval '1 hour'
    ) >= 5 then
        raise exception 'rate_limited';
    end if;

    begin
        select jsonb_agg(
                   jsonb_build_object('product_id', p.id, 'name', p.name,
                                      'price', p.price, 'qty', i.qty)
                   order by e.ordinality
               ),
               sum(p.price * i.qty)
          into v_details, v_total
          from jsonb_array_elements(p_items) with ordinality as e(item, ordinality)
          cross join lateral (
              select (e.item ->> 'product_id')::uuid as product_id,
                     (e.item ->> 'qty')::integer as qty
          ) as i
          join public.products as p on p.id = i.product_id and p.available
         where i.qty between 1 and 99;
    exception
        when invalid_text_representation or numeric_value_out_of_range then
            raise exception 'invalid_items';
    end;

    if v_details is null or jsonb_array_length(v_details) <> jsonb_array_length(p_items) then
        raise exception 'invalid_items';
    end if;

    begin
        insert into public.orders
            (customer, phone, notes, location, channel, status, details, total, client_id)
        values
            (trim(p_customer), p_phone, coalesce(p_notes, ''), coalesce(p_location, ''),
             'التطبيق', 'جديد', v_details, v_total, p_client_id)
        returning number into v_number;
    exception when unique_violation then
        -- Do not hide unrelated unique-constraint errors.
        select number into v_number from public.orders where client_id = p_client_id;
        if not found then
            raise;
        end if;
    end;
    return v_number;
end;
$$;
revoke all on function public.place_order(text, text, text, text, jsonb, uuid)
    from public, anon, authenticated;
grant execute on function public.place_order(text, text, text, text, jsonb, uuid)
    to anon, authenticated;

-- Supabase creates this publication in its managed projects.
do $realtime$
begin
    if not exists (
        select 1 from pg_catalog.pg_publication_tables
        where pubname = 'supabase_realtime' and schemaname = 'public' and tablename = 'orders'
    ) then
        alter publication supabase_realtime add table public.orders;
    end if;
end;
$realtime$;

-- Public image URLs; only managers can enumerate, insert, replace, or delete.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('product-images', 'product-images', true, 1048576,
        array['image/webp', 'image/jpeg', 'image/png'])
on conflict (id) do update
set public = excluded.public,
    file_size_limit = excluded.file_size_limit,
    allowed_mime_types = excluded.allowed_mime_types;

drop policy if exists "product images admin select" on storage.objects;
drop policy if exists "product images admin insert" on storage.objects;
drop policy if exists "product images admin update" on storage.objects;
drop policy if exists "product images admin delete" on storage.objects;
create policy "product images admin select" on storage.objects
    for select to authenticated
    using (bucket_id = 'product-images' and (select public.is_admin()));
create policy "product images admin insert" on storage.objects
    for insert to authenticated
    with check (bucket_id = 'product-images' and (select public.is_admin()));
create policy "product images admin update" on storage.objects
    for update to authenticated
    using (bucket_id = 'product-images' and (select public.is_admin()))
    with check (bucket_id = 'product-images' and (select public.is_admin()));
create policy "product images admin delete" on storage.objects
    for delete to authenticated
    using (bucket_id = 'product-images' and (select public.is_admin()));

insert into public.settings (key, value, is_public) values
    ('storeName',  'محلات السعيد',                 true),
    ('whatsapp',   '213776151686',                 true),
    ('currency',   'دج',                           true),
    ('footerNote', 'شكراً لثقتكم بمحلات السعيد 🛍️', true)
on conflict (key) do nothing;
-- products intentionally starts empty.

commit;
