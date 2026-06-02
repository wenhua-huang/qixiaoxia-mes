<template>
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
    <defs>
      <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:#3b8fd4;stop-opacity:1" />
        <stop offset="50%" style="stop-color:#2f67a9;stop-opacity:1" />
        <stop offset="100%" style="stop-color:#1e4d8c;stop-opacity:1" />
      </linearGradient>
    </defs>
    <!-- rounded background -->
    <rect x="2" y="2" width="60" height="60" rx="14" ry="14" fill="url(#logo-grad)" />
    <!-- building shape -->
    <g transform="translate(14, 10)" fill="white" opacity="0.95">
      <!-- main building body -->
      <rect x="2" y="12" width="32" height="30" rx="2" />
      <!-- building roof / top section -->
      <polygon points="0,14 18,2 36,14" />
      <!-- left window row -->
      <rect x="8" y="20" width="6" height="6" rx="1" fill="#1e4d8c" />
      <rect x="22" y="20" width="6" height="6" rx="1" fill="#1e4d8c" />
      <!-- middle windows -->
      <rect x="8" y="30" width="6" height="6" rx="1" fill="#1e4d8c" />
      <rect x="22" y="30" width="6" height="6" rx="1" fill="#1e4d8c" />
      <!-- door -->
      <rect x="14" y="30" width="8" height="12" rx="2" fill="#1e4d8c" />
      <!-- door arch -->
      <path d="M16,30 L16,27 Q18,24 20,27 L20,30 Z" fill="white" />
    </g>
  </svg>
</template>

<script lang="ts">
export default {
  name: 'AppLogo'
}
</script>
