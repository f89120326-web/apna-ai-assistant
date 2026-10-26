export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const cors = {
      "Access-Control-Allow-Origin": "*",
      "Access-Control-Allow-Methods": "POST, OPTIONS",
      "Access-Control-Allow-Headers": "Content-Type"
    };
    if (request.method === "OPTIONS") return new Response(null, { headers: cors });
    if (url.pathname !== "/api/chat") return new Response("Not found", { status: 404, headers: cors });
    if (request.method !== "POST") return Response.json({ error: "Use POST" }, { status: 405, headers: cors });
    if (!env.AI) return Response.json({ error: "Cloudflare Workers AI binding is not configured." }, { status: 503, headers: cors });
    try {
      const body = await request.json();
      const message = String(body.message || "").trim().slice(0, 6000);
      if (!message) return Response.json({ error: "Message is required" }, { status: 400, headers: cors });
      const language = ["hi-IN", "pa-IN", "en-IN"].includes(body.language) ? body.language : "auto";
      const history = Array.isArray(body.history) ? body.history.slice(-8).map(m => ({
        role: m.role === "assistant" ? "assistant" : "user",
        content: String(m.content || "").slice(0, 3000)
      })) : [];
      const system = "You are APNA AI, a smart multilingual assistant. Developer: Pindu-Tiwana. If asked who created, developed, or made you, answer exactly: Pindu-Tiwana. LANGUAGE RULE: if selected language is pa-IN, reply in Punjabi, preferably Gurmukhi; if the user writes Roman Punjabi, mirror Roman Punjabi. If selected language is hi-IN, reply in Hindi using Devanagari. If selected language is en-IN, reply in English. Treat the selected language as a strong instruction unless the user explicitly asks for another language. Be informative, thoughtful, structured, and practical. Explain steps and examples for complex questions. Use conversation history, correct misunderstandings, and do not invent facts, capabilities, or sources. Be transparent about uncertainty. Never claim device actions unless actually performed. User language preference: " + language + ".";
      const result = await env.AI.run("@cf/meta/llama-3.3-70b-instruct-fp8-fast", {
        messages: [{ role: "system", content: system }, ...history, { role: "user", content: message }],
        max_tokens: 900
      });
      const response = result.response || result.result?.response || "";
      if (!response) return Response.json({ error: "The model returned an empty response." }, { status: 502, headers: cors });
      return Response.json({ response }, { headers: { ...cors, "Cache-Control": "no-store" } });
    } catch (error) {
      return Response.json({ error: "Unable to answer right now. Please try again.", detail: String(error).slice(0, 180) }, { status: 500, headers: cors });
    }
  }
};