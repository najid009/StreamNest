# StreamNest

লাইভ টিভি চ্যানেল ও স্পোর্টস ইভেন্ট (ফুটবল, ক্রিকেট) দেখানোর Android অ্যাপ। ইউজারদের লগইন লাগে না।
Package: `com.ngi.streamnest` · Language: Kotlin (Jetpack Compose) · Backend: Supabase (কোনো Firebase নেই)

## Structure

```
StreamNest/
├── user-app/      # ইউজার অ্যাপ (Android) — Home, Sports, Content, Settings
├── admin-app/     # এডমিন অ্যাপ (Android) — লগইন, চ্যানেল, ফিচার্ড, ইভেন্ট, API সেটআপ
├── supabase/
│   ├── schema.sql              # টেবিল, RLS, is_admin(), প্রথম চ্যানেল (re-runnable)
│   ├── config.toml             # Supabase CLI config
│   └── functions/sync-sports/  # Edge Function: ক্রিকেট/ফুটবল ইভেন্ট সিঙ্ক
└── .github/workflows/
    ├── user-app.yml            # User APK build (artifact: streamnest-user-debug)
    ├── admin-app.yml           # Admin APK build (artifact: streamnest-admin-debug)
    ├── supabase-functions.yml  # sync-sports deploy (supabase/** পরিবর্তনে)
    └── sync-sports.yml         # প্রতি ৩০ মিনিটে sync-sports চালায় (cron)
```

## 1. Supabase setup

1. Supabase Dashboard -> **SQL Editor**-এ `supabase/schema.sql` পুরোটা চালান। বারবার চালালেও সমস্যা হবে না।
2. **Authentication -> Providers -> Email**: Sign-ups বন্ধ করুন (public signup off)। Auto Confirm চালু রাখুন।
3. **Authentication -> Users -> Add user**: এডমিনের email ও password দিয়ে user তৈরি করুন।
4. SQL Editor-এ এডমিনকে যোগ করুন (user id টা Users তালিকা থেকে নিন):
   ```sql
   insert into public.admins (user_id) values ('<admin-user-uuid>');
   ```
5. `service_role` key কখনো GitHub Secret, অ্যাপ বা Git-এ দেবেন না। এটি শুধু Edge Function-এর ভেতরে Supabase নিজে ব্যবহার করে।

## 2. GitHub Actions secrets

GitHub repo -> **Settings -> Secrets and variables -> Actions** -> নিচের Secret গুলো যোগ করুন। মান কখনো কোডে বা চ্যাটে লিখবেন না।

| Secret | কোথায় লাগে | কোথা থেকে |
|---|---|---|
| `SUPABASE_URL` | user-app, admin-app build, sync cron | Project Settings -> API -> Project URL (`https://<ref>.supabase.co`) |
| `SUPABASE_ANON_KEY` | user-app, admin-app build, sync cron | Project Settings -> API -> `anon` `public` key |
| `SUPABASE_ACCESS_TOKEN` | Edge Function deploy | Supabase Account -> Access Tokens |
| `SUPABASE_PROJECT_REF` | Edge Function deploy | Project URL-এর `<ref>` অংশ |
| `SYNC_SECRET` | cron ও Edge Function | যেকোনো লম্বা random string (নিজে তৈরি করুন) |

Secret না থাকলে user app নমুনা ডেটা দেখাবে; deploy workflow স্পষ্ট বার্তা দিয়ে থেমে যাবে।

`SYNC_SECRET` Edge Function-এর Supabase Secret হিসেবে `supabase-functions.yml` deploy-এর সময় নিজেই সেট হয়। API key গুলো Edge Function-এ আলাদা Secret নয়; এডমিন অ্যাপ থেকে `api_settings` টেবিলে সেভ হয়।

## 3. Admin app

- APK: GitHub Actions -> **Admin App Build** -> artifact `streamnest-admin-debug`।
- লগইন: Supabase Auth-এর এডমিন email/password। অন্য কেউ লগইন করলে RLS অনুযায়ী কিছু লিখতে পারবে না।
- **ড্যাশবোর্ড:** চ্যানেল, ফিচার্ড ও ইভেন্টের সংখ্যা, শেষ সিঙ্কের সময়।
- **চ্যানেল:** যোগ, সম্পাদনা, চালু/বন্ধ, মুছে ফেলা (HLS `.m3u8` URL দিন)।
- **ফিচার্ড:** Home-এর ব্যানার। ধরন তিনটি: বাইরের বিজ্ঞাপন লিংক (`url`), চ্যানেল (`channel`), ইভেন্ট (`match`)।
- **ইভেন্ট:** নিজের তৈরি (custom) ইভেন্ট যোগ/সম্পাদনা/মুছা; সিঙ্ক করা ইভেন্টে চ্যানেল, অবস্থা ও স্কোর বদলানো যায়।
- **API ও অ্যাকাউন্ট:** ক্রিকেট ও ফুটবলের API key দিন, সিঙ্ক চালু করুন, **এখনই ইভেন্ট সিঙ্ক করুন** চাপুন।

### API key

- **Cricket:** CricketData.org (আগের নাম CricAPI) — Free plan দিনে ১০০ hit। ৩০ মিনিট পর পর সিঙ্ক ~৪৮ hit/দিন ব্যবহার করে, তাই বাকি hit ম্যানুয়াল সিঙ্কের জন্য থাকে।
- **Football:** football-data.org — Free plan মিনিটে ১০ call, সীমিত লিগ, স্কোর কিছুটা দেরিতে আসতে পারে।
- Key দুটি admin app-এর API সেটআপ স্ক্রিনে দেওয়া হয়; Git বা APK-তে থাকে না (Supabase `api_settings` টেবিলে থাকে, শুধু এডমিন পড়তে/লিখতে পারে)।

## 4. Local build

`user-app/local.properties` বা `admin-app/local.properties` ফাইলে (Git-এ যাবে না):

```
SUPABASE_URL=https://<project-id>.supabase.co
SUPABASE_ANON_KEY=<anon-key>
```

তারপর `cd user-app && gradle :app:assembleDebug` (অথবা `admin-app`)। Android Studio-তেও খোলা যাবে। JDK 17 লাগবে।

## Features (User App)

- **হোম:** ফিচার্ড ব্যানার (বিজ্ঞাপন লিংক Custom Tab-এ খোলে / চ্যানেল / ইভেন্ট), ইভেন্ট, লাইভ চ্যানেল
- **স্পোর্টস:** ফুটবল/ক্রিকেট ফিল্টার, ইভেন্ট ও স্কোর, স্পোর্টস চ্যানেল
- **প্লেয়ার:** HLS লাইভ স্ট্রিম, ফুলস্ক্রিন, Retry, হোম বাটনে Picture-in-Picture
- **কনটেন্ট:** শীঘ্রই আসছে
- **সেটিংস:** ভার্সন, ডেটা উৎস, রিফ্রেশ

## Status / Not yet

- Supabase project, GitHub secrets ও Edge Function deploy এখনো আপনার পক্ষে সেট করতে হবে।
- Push notification ও ইউজার ব্লক এখনো পরিকল্পনায় নেই।
- Fixtures, favorites, search, reminder ও theme option পরের ধাপে।
