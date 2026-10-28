// سرور واسط RiseAI (Cloudflare Worker)
// کلید هوش مصنوعی فقط اینجا، به‌صورت Secret، نگه‌داری می‌شود و هرگز داخل اپ نیست.
//
// Secret ها / Variable ها در تنظیمات Worker:
//   ANTHROPIC_API_KEY  (لازم)     کلید API تو
//   APP_TOKEN          (پیشنهادی) یک رمز دلخواه؛ همان را در تنظیمات RiseAI اپ هم بنویس
//   MODEL              (اختیاری)  پیش‌فرض: claude-haiku-5-5
//
// اپ یک درخواست POST با این بدنه می‌فرستد:
//   { "app": "risefit", "messages": [ { "role": "user", "content": "..." }, ... ] }
// و جواب این است:  { "reply": "..." }

const SYSTEM_PROMPT =
  "تو RiseAI هستی، دستیار فارسی‌زبان اپ RiseFit. به نوجوان و جوانی کمک می‌کنی که " +
  "ورزش، درس و عادت‌های روزانه‌اش را مرتب کند. کوتاه، ساده، مهربان و به فارسی روان جواب بده. " +
  "هیچ‌وقت شرمنده‌کننده یا سرزنش‌کننده نباش. توصیه‌ی پزشکی تخصصی نده و اگر موضوع جدی بود، " +
  "بگو با یک بزرگ‌تر مطمئن یا پزشک صحبت کند. اطلاعات شخصی (آدرس، رمز و ...) را درخواست نکن.";

const MAX_MESSAGES = 10;
const MAX_CHARS = 1000;

function json(obj, status = 200) {
  return new Response(JSON.stringify(obj), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

// فقط نقش‌های مجاز، متن کوتاه، شروع با user و بدون دو پیام پشت‌سرهم از یک نقش
function cleanMessages(list) {
  if (!Array.isArray(list)) return [];
  const out = [];
  for (const m of list.slice(-MAX_MESSAGES)) {
    if (!m || (m.role !== "user" && m.role !== "assistant")) continue;
    const content = String(m.content ?? "").slice(0, MAX_CHARS).trim();
    if (!content) continue;
    if (out.length && out[out.length - 1].role === m.role) {
      out[out.length - 1].content += "\n" + content;
    } else {
      out.push({ role: m.role, content });
    }
  }
  while (out.length && out[0].role !== "user") out.shift();
  return out;
}

export default {
  async fetch(request, env) {
    if (request.method !== "POST") return json({ error: "method not allowed" }, 405);

    if (env.APP_TOKEN && request.headers.get("X-RiseFit-Token") !== env.APP_TOKEN) {
      return json({ error: "unauthorized" }, 401);
    }
    if (!env.ANTHROPIC_API_KEY) return json({ error: "server not configured" }, 500);

    let body;
    try {
      body = await request.json();
    } catch (e) {
      return json({ error: "bad json" }, 400);
    }
    const messages = cleanMessages(body && body.messages);
    if (messages.length === 0) return json({ error: "no messages" }, 400);

    let upstream;
    try {
      upstream = await fetch("https://api.anthropic.com/v1/messages", {
        method: "POST",
        headers: {
          "content-type": "application/json",
          "x-api-key": env.ANTHROPIC_API_KEY,
          "anthropic-version": "2023-06-01",
        },
        body: JSON.stringify({
          model: env.MODEL || "claude-haiku-5-5",
          max_tokens: 600,
          system: SYSTEM_PROMPT,
          messages,
        }),
      });
    } catch (e) {
      return json({ error: "upstream unreachable" }, 502);
    }
    if (!upstream.ok) return json({ error: "upstream " + upstream.status }, 502);

    const data = await upstream.json();
    const reply = (data.content || [])
      .filter((b) => b.type === "text")
      .map((b) => b.text)
      .join("\n")
      .trim();
    return json({ reply: reply || "الان جوابی ندارم، دوباره بپرس." });
  },
};
