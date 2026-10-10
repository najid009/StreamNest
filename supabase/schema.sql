-- StreamNest: Supabase schema (re-runnable)
-- Supabase Dashboard -> SQL Editor-এ চালান। একাধিকবার চালালেও সমস্যা হবে না।

-- ===== Tables =====

create table if not exists public.channels (
  id          uuid primary key default gen_random_uuid(),
  name        text not null,
  logo_url    text,
  stream_url  text not null,                       -- HLS (.m3u8) লিংক
  category    text not null default 'general',     -- 'sports' | 'general' | 'news' | 'entertainment'
  is_active   boolean not null default true,
  sort_order  int not null default 0,
  created_at  timestamptz not null default now()
);

create table if not exists public.featured_items (
  id            uuid primary key default gen_random_uuid(),
  title         text not null,
  image_url     text not null,
  action_type   text not null check (action_type in ('url', 'channel', 'match')),
  action_value  text not null,                     -- url হলে লিংক; channel/match হলে id
  is_active     boolean not null default true,
  sort_order    int not null default 0,
  created_at    timestamptz not null default now()
);

create table if not exists public.matches (
  id           uuid primary key default gen_random_uuid(),
  sport        text not null check (sport in ('football', 'cricket')),
  external_id  text unique,                        -- API provider-এর id (custom event হলে null)
  title        text not null,
  league       text,
  team_a       text,
  team_b       text,
  start_time   timestamptz,
  status       text not null default 'upcoming' check (status in ('upcoming', 'live', 'finished')),
  score        text,
  channel_id   uuid references public.channels(id) on delete set null,
  updated_at   timestamptz not null default now()
);

create table if not exists public.api_settings (
  provider        text primary key check (provider in ('cricket', 'football')),
  enabled         boolean not null default false,
  api_key         text,                            -- শুধু admin পড়তে পারবে (RLS)
  last_synced_at  timestamptz,
  last_error      text,
  daily_usage     int not null default 0
);
alter table public.api_settings add column if not exists api_key text;
alter table public.api_settings add column if not exists last_error text;

insert into public.api_settings (provider) values ('cricket'), ('football')
on conflict (provider) do nothing;

-- ===== Admin role =====

create table if not exists public.admins (
  user_id uuid primary key references auth.users(id) on delete cascade
);

create or replace function public.is_admin()
returns boolean
language sql
stable
security definer
set search_path = public
as $$
  select exists (select 1 from public.admins a where a.user_id = auth.uid());
$$;

-- ===== Row Level Security =====

alter table public.channels        enable row level security;
alter table public.featured_items  enable row level security;
alter table public.matches         enable row level security;
alter table public.api_settings    enable row level security;
alter table public.admins          enable row level security;

-- Public (anon) read: শুধু active চ্যানেল/ফিচার এবং সব ম্যাচ
drop policy if exists "public read active channels" on public.channels;
create policy "public read active channels" on public.channels
  for select to anon, authenticated using (is_active);

drop policy if exists "public read active featured" on public.featured_items;
create policy "public read active featured" on public.featured_items
  for select to anon, authenticated using (is_active);

drop policy if exists "public read matches" on public.matches;
create policy "public read matches" on public.matches
  for select to anon, authenticated using (true);

-- Admin: সব টেবিলে পুরো নিয়ন্ত্রণ
drop policy if exists "admin manage channels" on public.channels;
create policy "admin manage channels" on public.channels
  for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists "admin manage featured" on public.featured_items;
create policy "admin manage featured" on public.featured_items
  for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists "admin manage matches" on public.matches;
create policy "admin manage matches" on public.matches
  for all to authenticated using (public.is_admin()) with check (public.is_admin());

drop policy if exists "admin manage api settings" on public.api_settings;
create policy "admin manage api settings" on public.api_settings
  for all to authenticated using (public.is_admin()) with check (public.is_admin());

-- admin নিজের row পড়তে পারবে (login-এর সময় যাচাই)
drop policy if exists "admin read own row" on public.admins;
create policy "admin read own row" on public.admins
  for select to authenticated using (user_id = auth.uid());

-- ===== Seed: প্রথম চ্যানেল (একবারই যোগ হবে) =====

insert into public.channels (name, stream_url, category, sort_order)
select 'Sports Channel 1', 'https://tvsen6.aynaott.com/6xyZ3N4oHv2KBJdB6W4p/index.m3u8', 'sports', 1
where not exists (
  select 1 from public.channels where stream_url = 'https://tvsen6.aynaott.com/6xyZ3N4oHv2KBJdB6W4p/index.m3u8'
);

-- ===== Admin তৈরি (একবার, নিচের ধাপগুলো অনুসরণ করুন) =====
-- 1. Supabase Dashboard -> Authentication -> Users -> "Add user" -> Email + Password
--    ("Auto Confirm User" টিক দিন)। Authentication -> Providers -> Email থেকে
--    "Allow new users to sign up" বন্ধ করে দিন।
-- 2. তারপর নিচের SQL চালান (email বদলে নিন):
--
--    insert into public.admins (user_id)
--    select id from auth.users where email = 'your-admin@email.com'
--    on conflict do nothing;
