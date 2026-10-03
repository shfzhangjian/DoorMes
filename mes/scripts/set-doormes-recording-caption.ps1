param(
  [Parameter(Mandatory = $true)][string]$Step,
  [Parameter(Mandatory = $true)][string]$Message
)
$ErrorActionPreference = 'Stop'
# A test-only, non-interactive overlay. It never changes business state or assertions.
$captionPayload = @{ step = $Step; message = $Message } | ConvertTo-Json -Compress
$captionScript = @'
const payload = __CAPTION_PAYLOAD__;
if (!window.__s1Caption) {
  window.__s1Timeline = [];
  window.__s1Caption = (step, message) => {
    let el = document.getElementById('s1-test-caption');
    if (!el) {
      el = document.createElement('aside');
      el.id = 's1-test-caption';
      el.setAttribute('aria-hidden', 'true');
      el.style.cssText = 'position:fixed;left:24px;bottom:126px;z-index:2147483000;max-width:620px;padding:14px 18px;background:rgba(15,23,42,.94);color:white;border-left:4px solid #60a5fa;border-radius:6px;box-shadow:0 3px 16px #0003;font-family:Microsoft YaHei,sans-serif;pointer-events:none;line-height:1.55';
      document.body.append(el);
    }
    el.replaceChildren();
    const title = document.createElement('div');
    title.style.cssText = 'font-size:12px;color:#93c5fd;margin-bottom:5px';
    title.textContent = `真实操作录屏 · S1 验收 · ${step}`;
    const detail = document.createElement('div');
    detail.style.cssText = 'font-size:16px';
    detail.textContent = message;
    el.append(title, detail);
    window.__s1Timeline.push({ step, message, time: Date.now() });
  };
}
window.__s1Caption(payload.step, payload.message);
JSON.stringify({ step: payload.step, time: Date.now() });
'@
$captionScript = $captionScript.Replace('__CAPTION_PAYLOAD__', $captionPayload)
& (Join-Path $PSScriptRoot 'run-doormes-browser.ps1') eval -b ([Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($captionScript)))
exit $LASTEXITCODE
