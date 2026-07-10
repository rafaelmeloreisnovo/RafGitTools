const BRIDGE = "http://127.0.0.1:8765";

const tokenInput = document.getElementById("token");
const intentInput = document.getElementById("intent");
const dataClassInput = document.getElementById("dataClass");
const messageInput = document.getElementById("message");
const consentInput = document.getElementById("consent");
const responseView = document.getElementById("response");
const healthStatus = document.getElementById("healthStatus");
const sendStatus = document.getElementById("sendStatus");
const sendButton = document.getElementById("send");

initialize();

async function initialize() {
  const saved = await chrome.storage.local.get(["rafBridgeToken", "rafBridgeIntent", "rafBridgeDataClass"]);
  tokenInput.value = saved.rafBridgeToken || "";
  intentInput.value = saved.rafBridgeIntent || "conversa natural";
  dataClassInput.value = saved.rafBridgeDataClass || "private";
}

document.getElementById("saveToken").addEventListener("click", async () => {
  await chrome.storage.local.set({ rafBridgeToken: tokenInput.value.trim() });
  healthStatus.textContent = "Token salvo somente no armazenamento local da extensão.";
});

document.getElementById("health").addEventListener("click", async () => {
  healthStatus.textContent = "Verificando…";
  try {
    const response = await fetch(`${BRIDGE}/health`, { cache: "no-store" });
    const body = await response.json();
    if (!response.ok || !body.ok) {
      throw new Error(body.message || `HTTP ${response.status}`);
    }
    healthStatus.textContent = `Ponte ativa. Modelo: ${body.model || "local"}.`;
  } catch (error) {
    healthStatus.textContent = `Ponte indisponível: ${error.message}`;
  }
});

document.getElementById("selection").addEventListener("click", async () => {
  sendStatus.textContent = "Lendo apenas o texto selecionado na aba atual…";
  try {
    const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
    if (!tab || !tab.id) {
      throw new Error("Aba ativa não encontrada");
    }
    const results = await chrome.scripting.executeScript({
      target: { tabId: tab.id },
      func: () => window.getSelection()?.toString() || ""
    });
    const selected = results?.[0]?.result?.trim() || "";
    if (!selected) {
      throw new Error("Nenhum texto está selecionado");
    }
    messageInput.value = selected.slice(0, 32768);
    sendStatus.textContent = "Seleção copiada para a mensagem. Nada foi enviado ainda.";
  } catch (error) {
    sendStatus.textContent = error.message;
  }
});

document.getElementById("clear").addEventListener("click", () => {
  messageInput.value = "";
  responseView.textContent = "Aguardando conversa.";
  sendStatus.textContent = "";
  consentInput.checked = false;
});

sendButton.addEventListener("click", async () => {
  const token = tokenInput.value.trim();
  const intent = intentInput.value.trim();
  const dataClass = dataClassInput.value;
  const message = messageInput.value.trim();

  if (!token) {
    sendStatus.textContent = "Cole o token mostrado pelo Raf Bridge no APK.";
    return;
  }
  if (!intent) {
    sendStatus.textContent = "Declare a intenção desta conversa.";
    return;
  }
  if (!message) {
    sendStatus.textContent = "Escreva uma mensagem.";
    return;
  }
  if (!consentInput.checked) {
    sendStatus.textContent = "O consentimento explícito é obrigatório para cada envio.";
    return;
  }

  await chrome.storage.local.set({
    rafBridgeToken: token,
    rafBridgeIntent: intent,
    rafBridgeDataClass: dataClass
  });

  sendButton.disabled = true;
  sendStatus.textContent = "Conversando com o modelo local…";
  responseView.textContent = "";

  try {
    const response = await fetch(`${BRIDGE}/v1/chat`, {
      method: "POST",
      cache: "no-store",
      headers: {
        "Content-Type": "application/json",
        "X-Raf-Token": token
      },
      body: JSON.stringify({
        request_id: newRequestId(),
        action: "chat",
        intent,
        consent: true,
        data_class: dataClass,
        source: "kiwi-extension",
        message
      })
    });

    const body = await response.json().catch(() => ({}));
    if (!response.ok || !body.ok) {
      throw new Error(body.message || body.error || `HTTP ${response.status}`);
    }

    responseView.textContent = body.reply;
    sendStatus.textContent = "Resposta local recebida. Nenhuma ação externa foi executada.";
    consentInput.checked = false;
  } catch (error) {
    responseView.textContent = "";
    sendStatus.textContent = `Falha: ${error.message}`;
  } finally {
    sendButton.disabled = false;
  }
});

function newRequestId() {
  if (crypto.randomUUID) {
    return crypto.randomUUID();
  }
  const random = new Uint32Array(4);
  crypto.getRandomValues(random);
  return Array.from(random, value => value.toString(16).padStart(8, "0")).join("-");
}
