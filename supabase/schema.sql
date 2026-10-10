-- StreamNest: Supabase schema
-- Supabase Dashboard -> SQL Editor-এ একবার চালান।

-- ===== Tables =====

create table if not exists public.channels (
  id          uuid primary key default gen_random_uuid(),
  name        text not null,
  logo_url    text,
  stream_url  text not null,                       -- HLS (.m3u8) লিংক
  category    text not null default 'general',     -- 'sports' | 'general' | ...
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
  external_id  text unique,                        -- API provider-এর ম্যাচ id
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

-- API key এখানে থাকবে না; পরের ধাপে Supabase Vault ব্যবহার করা হবে।
create table if not exists public.api_settings (
  provider        text primary key check (provider in ('cricket', 'football')),
  enabled         boolean not null default false,
  last_synced_at  timestamptz,
  daily_usage     int not null default 0
);

-- ===== Row Level Security =====

alter table public.channels        enable row level security;
alter table public.featured_items  enable row level security;
alter table public.matches         enable row level security;
alter table public.api_settings    enable row level security;

-- ইউজার অ্যাপ (anon key) শুধু পড়তে পারবে
create policy "public read active channels"
  on public.channels for select
  to anon, authenticated
  using (is_active);

create policy "public read active featured"
  on public.featured_items for select
  to anon, authenticated
  using (is_active);

create policy "public read matches"
  on public.matches for select
  to anon, authenticated
  using (true);

-- api_settings-এ কোনো public policy নেই: anon পড়তে/লিখতে পারবে না।
-- Admin write policy পরের ধাপে (admin app) যোগ হবে।

-- ===== Seed: প্রথম চ্যানেল (একবারই চালাবেন) =====

insert into public.channels (name, stream_url, category, sort_order)
values ('Sports Channel 1', 'https://tvsen6.aynaott.com/6xyZ3N4oHv2KBJdB6W4p/index.m3u8', 'sports', 1);
