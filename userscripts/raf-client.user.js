// ==UserScript==
// @name         RAFAELIA Local Client
// @namespace    https://rafcode.local/
// @version      0.1.0
// @description  Sends only user-selected or manually entered text to the local Raf Bridge.
// @author       Rafael Melo Reis / RAFCODE-Φ
// @match        https://github.com/*
// @match        https://drive.google.com/*
// @match        https://docs.google.com/*
// @match        https://chatgpt.com/*
// @grant        GM_registerMenuCommand
// @grant        GM_getValue
// @grant        GM_setValue
// @grant        GM_xmlhttpRequest
// @connect      127.0.0.1
// @run-at       document-idle
// ==/UserScript==

(() => {
  "use strict";

  const BRIDGE_URL = "http://127.0.0.1:8765";
  const MAX_MESSAGE_CHARS = 32768;

  GM_registerMenuCommand("RAFAELIA: configurar token local", configureToken);
  GM_registerMenuCommand("RAFAELIA: testar ponte local", testBridge);
  GM_registerMenuCommand("RAFAELIA: enviar seleção", sendSelection);
  GM_registerMenuCommand("RAFAELIA: escrever mensagem", sendManualMessage);

  async function configureToken() {
    const current = await GM_getValue("rafBridgeToken", "");
    const token = window.prompt("Token local mostrado pelo APK RafGitTools:", current);
    if (token === null) return;

    const normalized = token.trim();
    if (!normalized) {
      window.alert("Token vazio. Nenhuma alteração foi salva.");
      return;
    }

    await GM_setValue("rafBridgeToken", normalized);
    window.alert("Token salvo no armazenamento do userscript.");
  }

  async function testBridge() {
    try {
      const response = await request({
        method: "GET",
        url: `${BRIDGE_URL}/health`
      });
      const body = parseJson(response.responseText);
      if (response.status !== 200 || !body.ok) {
        throw new Error(body.message || `HTTP ${response.status}`);
      }
      window.alert(`Raf Bridge ativo. Modelo: ${body.model || "local"}.`);
    } catch (error) {
      window.alert(`Ponte indisponível: ${error.message}`);
    }
  }

  async function sendSelection() {
    const selected = String(window.getSelection?.() || "").trim();
    if (!selected) {
      window.alert("Nenhum texto está selecionado. Nada foi enviado.");
      return;
    }
    await prepareAndSend(selected, true);
  }

  async function sendManualMessage() {
    const message = window.prompt("Mensagem para o modelo local:", "");
    if (message === null) return;
    await prepareAndSend(message.trim(), false);
  }

  async function prepareAndSend(message, selectionOnly) {
    if (!message) {
      window.alert("Mensagem vazia. Nada foi enviado.");
      return;
    }
    if (message.length > MAX_MESSAGE_CHARS) {
      window.alert(`Mensagem excede ${MAX_MESSAGE_CHARS} caracteres.`);
      return;
    }

    const token = String(await GM_getValue("rafBridgeToken", "")).trim();
    if (!token) {
      await configureToken();
      return;
    }

    const intent = window.prompt("Declare a intenção deste envio:", "análise local autorizada");
    if (intent === null || !intent.trim()) {
      window.alert("Intenção ausente. Nada foi enviado.");
      return;
    }

    const dataClassInput = window.prompt(
      "Classe do dado: public, private ou sensitive",
      "private"
    );
    if (dataClassInput === null) return;

    const dataClass = dataClassInput.trim().toLowerCase();
    if (!["public", "private", "sensitive"].includes(dataClass)) {
      window.alert("Classe inválida. Nada foi enviado.");
      return;
    }

    const consent = window.confirm(
      "Enviar somente este texto ao Raf Bridge local em 127.0.0.1?\n\n" +
      "A URL, o título da página, cookies e o DOM não serão enviados."
    );
    if (!consent) return;

    const payload = {
      schema: "raf.client.envelope.v1",
      request_id: newRequestId(),
      action: "chat",
      intent: intent.trim(),
      consent: true,
      consent_at: new Date().toISOString(),
      data_class: dataClass,
      source: "tampermonkey-userscript",
      transport: "loopback-http",
      message,
      selection_only: selectionOnly,
      page_metadata_included: false,
      retention_request: "none",
      capabilities_requested: ["LOCAL_CHAT"]
    };

    try {
      const response = await request({
        method: "POST",
        url: `${BRIDGE_URL}/v1/chat`,
        headers: {
          "Content-Type": "application/json",
          "X-Raf-Token": token
        },
        data: JSON.stringify(payload)
      });
      const body = parseJson(response.responseText);
      if (response.status !== 200 || !body.ok) {
        throw new Error(body.message || body.error || `HTTP ${response.status}`);
      }
      window.prompt("Resposta local — copie quando desejar:", body.reply || "");
    } catch (error) {
      window.alert(`Falha no envio local: ${error.message}`);
    }
  }

  function request(details) {
    return new Promise((resolve, reject) => {
      GM_xmlhttpRequest({
        timeout: 130000,
        ...details,
        onload: resolve,
        ontimeout: () => reject(new Error("tempo limite excedido")),
        onerror: () => reject(new Error("erro de transporte local")),
        onabort: () => reject(new Error("requisição cancelada"))
      });
    });
  }

  function parseJson(text) {
    try {
      return JSON.parse(text || "{}");
    } catch (_error) {
      return {};
    }
  }

  function newRequestId() {
    if (globalThis.crypto?.randomUUID) {
      return globalThis.crypto.randomUUID();
    }
    const random = new Uint32Array(4);
    globalThis.crypto.getRandomValues(random);
    return Array.from(random, value => value.toString(16).padStart(8, "0")).join("-");
  }
})();
