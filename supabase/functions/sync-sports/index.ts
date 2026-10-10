// StreamNest: cricket ও football ইভেন্ট sync করে `matches` টেবিলে রাখে।
// Caller হতে পারে: (ক) লগইন করা admin (JWT), অথবা (খ) GitHub Actions cron যার কাছে x-sync-secret আছে।
// Free limit মেনে চলার জন্য এটি আলাদা cron থেকে নির্দিষ্ট সময় পর পর চালানো হয়।
import { createClient } from "npm:@supabase/supabase-js@2";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type, x-sync-secret",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

type Row = Record<string, unknown>;

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function footballStatus(s: string): "upcoming" | "live" | "finished" {
  if (["IN_PLAY", "PAUSED", "LIVE", "HALFTIME"].includes(s)) return "live";
  if (["FINISHED", "AWARDED"].includes(s)) return "finished";
  return "upcoming";
}

// CricketData.org (CricAPI) — currentMatches; free plan: দিনে ১০০ hit
async function fetchCricket(key: string): Promise<Row[]> {
  const res = await fetch(
    `https://api.cricapi.com/v1/currentMatches?apikey=${encodeURIComponent(key)}&offset=0`,
  );
  const body = await res.json();
  if (body.status !== "success") {
    throw new Error(`Cricket API: ${body.reason ?? body.status ?? res.status}`);
  }
  const now = new Date().toISOString();
  return (body.data ?? []).map((m: any) => {
    const status = m.matchEnded ? "finished" : m.matchStarted ? "live" : "upcoming";
    const score = Array.isArray(m.score) && m.score.length
      ? m.score.map((s: any) => `${s.r}/${s.w} (${s.o} ov)`).join(" | ")
      : null;
    const gmt = m.dateTimeGMT
      ? (String(m.dateTimeGMT).endsWith("Z") ? m.dateTimeGMT : `${m.dateTimeGMT}Z`)
      : null;
    return {
      sport: "cricket",
      external_id: `cricket:${m.id}`,
      title: m.name ?? "Cricket match",
      league: m.matchType ? String(m.matchType).toUpperCase() : null,
      team_a: m.teams?.[0] ?? null,
      team_b: m.teams?.[1] ?? null,
      start_time: gmt,
      status,
      score,
      updated_at: now,
    };
  });
}

// football-data.org v4 — আজ থেকে ২ দিনের ম্যাচ; free plan: মিনিটে ১০ call
async function fetchFootball(key: string): Promise<Row[]> {
  const fmt = (d: Date) => d.toISOString().slice(0, 10);
  const from = fmt(new Date());
  const to = fmt(new Date(Date.now() + 2 * 24 * 60 * 60 * 1000));
  const res = await fetch(
    `https://api.football-data.org/v4/matches?dateFrom=${from}&dateTo=${to}`,
    { headers: { "X-Auth-Token": key } },
  );
  const body = await res.json();
  if (!res.ok) throw new Error(`Football API: ${body.message ?? res.status}`);
  const now = new Date().toISOString();
  return (body.matches ?? []).map((m: any) => {
    const h = m.score?.fullTime?.home;
    const a = m.score?.fullTime?.away;
    const home = m.homeTeam?.shortName ?? m.homeTeam?.name ?? "Home";
    const away = m.awayTeam?.shortName ?? m.awayTeam?.name ?? "Away";
    return {
      sport: "football",
      external_id: `football:${m.id}`,
      title: `${home} vs ${away}`,
      league: m.competition?.name ?? null,
      team_a: home,
      team_b: away,
      start_time: m.utcDate ?? null,
      status: footballStatus(m.status ?? ""),
      score: h != null && a != null ? `${h} - ${a}` : null,
      updated_at: now,
    };
  });
}

Deno.serve(async (req) => {
  if (req.method === "OPTIONS") return new Response("ok", { headers: corsHeaders });
  if (req.method !== "POST") return json({ error: "method not allowed" }, 405);

  const supabase = createClient(
    Deno.env.get("SUPABASE_URL")!,
    Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")!,
    { auth: { persistSession: false, autoRefreshToken: false } },
  );

  // ---- Authorization ----
  let authorized = false;
  const syncSecret = Deno.env.get("SYNC_SECRET");
  if (syncSecret && req.headers.get("x-sync-secret") === syncSecret) {
    authorized = true;
  }
  if (!authorized) {
    const token = (req.headers.get("authorization") ?? "").replace(/^Bearer\s+/i, "");
    if (token) {
      const { data } = await supabase.auth.getUser(token);
      if (data?.user) {
        const { data: row } = await supabase
          .from("admins")
          .select("user_id")
          .eq("user_id", data.user.id)
          .maybeSingle();
        authorized = !!row;
      }
    }
  }
  if (!authorized) return json({ error: "unauthorized" }, 401);

  // ---- Sync ----
  const { data: settings, error: settingsErr } = await supabase
    .from("api_settings")
    .select("provider, enabled, api_key");
  if (settingsErr) return json({ error: settingsErr.message }, 500);

  const results: Record<string, string> = {};
  let synced = 0;

  for (const s of settings ?? []) {
    if (!s.enabled || !s.api_key) {
      results[s.provider] = "skipped (disabled or no key)";
      continue;
    }
    try {
      const fetched = s.provider === "cricket"
        ? await fetchCricket(s.api_key)
        : await fetchFootball(s.api_key);

      // একই batch-এ একই external_id দুবার থাকলে upsert ব্যর্থ হয়, তাই dedupe
      const unique = Array.from(new Map(fetched.map((r) => [r.external_id, r])).values());
      if (unique.length) {
        const { error: upErr } = await supabase
          .from("matches")
          .upsert(unique, { onConflict: "external_id" });
        if (upErr) throw new Error(upErr.message);
      }
      await supabase
        .from("api_settings")
        .update({ last_synced_at: new Date().toISOString(), last_error: null })
        .eq("provider", s.provider);

      synced += unique.length;
      results[s.provider] = `ok (${unique.length})`;
    } catch (e) {
      const msg = e instanceof Error ? e.message : String(e);
      await supabase.from("api_settings").update({ last_error: msg }).eq("provider", s.provider);
      results[s.provider] = `error: ${msg}`;
    }
  }

  return json({ synced, results });
});
