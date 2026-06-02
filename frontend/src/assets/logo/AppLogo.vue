<template>
  <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" width="64" height="64">
    <defs>
      <linearGradient id="logo-grad" x1="0%" y1="0%" x2="100%" y2="100%">
        <stop offset="0%" style="stop-color:#7B5CFA;stop-opacity:1" />
        <stop offset="50%" style="stop-color:#A855F7;stop-opacity:1" />
        <stop offset="100%" style="stop-color:#6366F1;stop-opacity:1" />
      </linearGradient>
    </defs>
    <!-- rounded background -->
    <rect x="2" y="2" width="60" height="60" rx="14" ry="14" fill="url(#logo-grad)" />
    <!-- building shape -->
    <g transform="translate(14, 12)" fill="white" opacity="0.95">
      <!-- main building body -->
      <rect x="4" y="10" width="28" height="28" rx="1" />
      <!-- building roof -->
      <polygon points="0,10 18,0 36,10" />
      <!-- left window -->
      <rect x="9" y="16" width="6" height="6" rx="1" fill="#7B5CFA" />
      <!-- right window -->
      <rect x="21" y="16" width="6" height="6" rx="1" fill="#7B5CFA" />
      <!-- door -->
      <rect x="14" y="26" width="8" height="12" rx="1" fill="#7B5CFA" />
      <!-- door top arch -->
      <path d="M16,26 L16,24 Q18,22 20,24 L20,26 Z" fill="white" />
    </g>
  </svg>
</template>

<script lang="ts">
export default {
  name: 'AppLogo'
}
</script>
