# StreamNest

লাইভ টিভি চ্যানেল ও স্পোর্টস ইভেন্ট (ফুটবল, ক্রিকেট) দেখানোর Android অ্যাপ।
Package: `com.ngi.streamnest` · Language: Kotlin (Jetpack Compose) · Backend: Supabase

## Structure

```
StreamNest/
├── user-app/      # ইউজার অ্যাপ (Android, Kotlin, Compose)  — Home, Sports, Content, Settings
├── admin-app/     # এডমিন অ্যাপ (পরের ধাপে)
├── supabase/
│   └── schema.sql # টেবিল, RLS policy, প্রথম চ্যানেল
└── .github/workflows/user-app.yml   # GitHub Actions: debug APK build
```

## Supabase setup

1. Supabase Dashboard -> SQL Editor-এ `supabase/schema.sql` চালান।
2. GitHub repo -> **Settings -> Secrets and variables -> Actions** -> নিচের দুটি Secret যোগ করুন:
   - `SUPABASE_URL` = `https://<project-id>.supabase.co`
   - `SUPABASE_ANON_KEY` = Supabase-এর anon (public) key
   - `service_role` key কখনো Secret বা অ্যাপে দেবেন না।
3. Secret না থাকলেও অ্যাপ build ও চলবে, তখন নমুনা ডেটা দেখাবে।

## Local build

`user-app/local.properties` ফাইলে (Git-এ যাবে না):

```
SUPABASE_URL=https://<project-id>.supabase.co
SUPABASE_ANON_KEY=<anon-key>
```

তারপর `cd user-app && gradle :app:assembleDebug` (Android Studio-তেও খোলা যাবে)।

## Features (User App)

- **হোম:** ফিচার্ড ব্যানার (বিজ্ঞাপন লিংক / চ্যানেল / ইভেন্ট), ইভেন্ট, লাইভ চ্যানেল, কনটেন্ট (শীঘ্রই)
- **স্পোর্টস:** ফুটবল/ক্রিকেট ফিল্টার, ইভেন্ট আপডেট, স্পোর্টস চ্যানেল
- **কনটেন্ট:** শীঘ্রই আসছে
- **সেটিংস:** ভার্সন, ডেটা উৎস, রিফ্রেশ
