<template>
  <svg
    version="1.1"
    xmlns="http://www.w3.org/2000/svg"
    xmlns:xlink="http://www.w3.org/1999/xlink"
    viewBox="0 0 800 600"
    xml:space="preserve"
    class="mes-logo-svg"
  >
  <defs>
    <linearGradient id="blueGradient" x1="0%" y1="0%" x2="100%" y2="100%">
      <stop offset="0%" style="stop-color:#00c6fb;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#005bea;stop-opacity:1" />
    </linearGradient>

    <linearGradient id="darkBlueGradient" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" style="stop-color:#4587ef;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#00209e;stop-opacity:1" />
    </linearGradient>

    <linearGradient id="screenGradient" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" style="stop-color:#1b2a4e;stop-opacity:0.95" />
      <stop offset="100%" style="stop-color:#131d36;stop-opacity:0.95" />
    </linearGradient>

    <linearGradient id="highlight" x1="0%" y1="0%" x2="0%" y2="100%">
      <stop offset="0%" style="stop-color:#ffffff;stop-opacity:0.3" />
      <stop offset="100%" style="stop-color:#ffffff;stop-opacity:0" />
    </linearGradient>

    <filter id="dropShadow" x="-50%" y="-50%" width="200%" height="200%">
      <feGaussianBlur in="SourceAlpha" stdDeviation="8"/>
      <feOffset dx="0" dy="8" result="offsetblur"/>
      <feComponentTransfer>
        <feFuncA type="linear" slope="0.4"/>
      </feComponentTransfer>
      <feMerge>
        <feMergeNode/>
        <feMergeNode in="SourceGraphic"/>
      </feMerge>
    </filter>
  </defs>

    <g transform="translate(400, 300)" opacity="0.08">
      <circle r="350" fill="none" stroke="#00c6fb" stroke-width="1" stroke-dasharray="20 10">
        <animateTransform attributeName="transform" type="rotate" from="0 0 0" to="360 0 0" dur="60s" repeatCount="indefinite"/>
      </circle>
      <circle r="260" fill="none" stroke="#005bea" stroke-width="2" />
      <circle r="180" fill="none" stroke="#4587ef" stroke-width="30" stroke-dasharray="60 90" opacity="0.5">
         <animateTransform attributeName="transform" type="rotate" from="360 0 0" to="0 0 0" dur="40s" repeatCount="indefinite"/>
      </circle>
  </g>

    <g transform="translate(130, 400) scale(1.4)" filter="url(#dropShadow)">
      <path d="M0,20 L60,50 L120,20 L60,-10 Z" fill="url(#darkBlueGradient)" />
      <path d="M0,20 L60,50 V70 L0,40 Z" fill="#00156b" />
      <path d="M60,50 L120,20 V40 L60,70 Z" fill="#00209e" />

      <g transform="translate(60, 10)">
          <circle cx="0" cy="0" r="32" fill="url(#blueGradient)" stroke="#fff" stroke-width="2"/>
        <circle cx="0" cy="0" r="12" fill="#fff" />
        <g stroke="#fff" stroke-width="5" stroke-linecap="round">
            <path d="M-38,0 H-24 M38,0 H24 M0,-38 V-24 M0,38 V24" />
          <path d="M-27,-27 L-17,-17 M27,27 L17,17 M-27,27 L-17,17 M27,-27 L17,-17" />
          </g>
        <animateTransform attributeName="transform" type="rotate" from="0 0 0" to="360 0 0" dur="10s" repeatCount="indefinite"/>
      </g>
      <text x="60" y="100" text-anchor="middle" fill="#333" font-family="Microsoft YaHei, Arial" font-weight="bold" font-size="16" letter-spacing="1">生产执行</text>
  </g>

    <g transform="translate(670, 400) scale(1.4)" filter="url(#dropShadow)">
      <path d="M-60,-10 L0,20 L60,-10 L0,-40 Z" fill="url(#darkBlueGradient)" />
      <path d="M-60,-10 L0,20 V40 L-60,10 Z" fill="#00156b" />
      <path d="M0,20 L60,-10 V10 L0,40 Z" fill="#00209e" />

      <g transform="translate(0, -20)">
          <path d="M-25,0 L25,0 L25,-70 L-25,-70 Z" fill="url(#blueGradient)" />
        <ellipse cx="0" cy="0" rx="25" ry="10" fill="#005bea" />
        <ellipse cx="0" cy="-70" rx="25" ry="10" fill="#00c6fb" />
        <circle cx="-12" cy="-50" r="3" fill="#fff" opacity="0.8">
              <animate attributeName="opacity" values="0.2;1;0.2" dur="0.8s" repeatCount="indefinite"/>
          </circle>
        <circle cx="-12" cy="-30" r="3" fill="#fff" opacity="0.8">
              <animate attributeName="opacity" values="0.2;1;0.2" dur="1.2s" repeatCount="indefinite"/>
          </circle>
        <path d="M10,-60 V-10" stroke="#fff" stroke-width="2" stroke-dasharray="5 5" opacity="0.5" />
      </g>
      <text x="0" y="70" text-anchor="middle" fill="#333" font-family="Microsoft YaHei, Arial" font-weight="bold" font-size="16" letter-spacing="1">数据中心</text>
  </g>

    <g opacity="0.6">
      <path d="M220,380 L300,320 L400,320" fill="none" stroke="url(#blueGradient)" stroke-width="6" stroke-dasharray="15 8" stroke-linecap="round">
           <animate attributeName="stroke-dashoffset" from="23" to="0" dur="1s" repeatCount="indefinite" />
      </path>
      <path d="M580,380 L500,320 L400,320" fill="none" stroke="url(#blueGradient)" stroke-width="6" stroke-dasharray="15 8" stroke-linecap="round">
          <animate attributeName="stroke-dashoffset" from="0" to="23" dur="1s" repeatCount="indefinite" />
      </path>
  </g>

    <g transform="translate(400, 240) scale(1.7)" filter="url(#dropShadow)">
      <rect x="-110" y="-80" width="220" height="150" rx="12" ry="12" fill="url(#screenGradient)" stroke="#4587ef" stroke-width="3" />

      <path d="M-110,-68 A12,12 0 0 1 -98,-80 H98 A12,12 0 0 1 110,-68 V-55 H-110 Z" fill="#00209e" />
      <g transform="translate(-85, -67)">
        <circle cx="0" cy="0" r="4" fill="#ff5f56" />
        <circle cx="15" cy="0" r="4" fill="#ffbd2e" />
        <circle cx="30" cy="0" r="4" fill="#27c93f" />
      </g>

      <g transform="translate(-90, -35)">
          <g transform="translate(10, 0)">
            <rect x="0" y="40" width="18" height="50" fill="#00c6fb" opacity="0.9">
                <animate attributeName="height" values="50;20;50" dur="3s" repeatCount="indefinite"/>
              <animate attributeName="y" values="40;70;40" dur="3s" repeatCount="indefinite"/>
            </rect>
            <rect x="25" y="10" width="18" height="80" fill="#4587ef" opacity="0.9" />
            <rect x="50" y="30" width="18" height="60" fill="#005bea" opacity="0.9" />
          </g>

        <g transform="translate(90, 10)">
             <rect x="0" y="0" width="80" height="70" fill="#000" fill-opacity="0.2" rx="4" />
          <polyline points="10,50 30,20 50,40 70,10" fill="none" stroke="#27c93f" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
          <circle cx="10" cy="50" r="3" fill="#fff" />
          <circle cx="30" cy="20" r="3" fill="#fff" />
          <circle cx="50" cy="40" r="3" fill="#fff" />
          <circle cx="70" cy="10" r="3" fill="#fff" />
          </g>
      </g>

      <path d="M-110,-80 L-10,-80 L-90,100 L-110,70 Z" fill="url(#highlight)" />

      <text x="0" y="105" text-anchor="middle" fill="#0035bd" font-family="Arial, sans-serif" font-weight="900" font-size="28" letter-spacing="3" style="text-shadow: 0px 1px 1px rgba(255,255,255,0.3);">MES</text>
      <text x="0" y="128" text-anchor="middle" fill="#555" font-family="Microsoft YaHei" font-size="12" font-weight="bold">智能制造核心</text>
  </g>
</svg>

</template>
