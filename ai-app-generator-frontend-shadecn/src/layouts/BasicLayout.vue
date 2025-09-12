<template>
  <div style="min-height: 100vh; display: flex; flex-direction: column">
    <GlobalHeader />
    <!-- Decorative background (green curves) -->
    <svg class="bg-curves" viewBox="0 0 1440 800" preserveAspectRatio="none" aria-hidden="true">
      <defs>
        <linearGradient id="curveGrad1" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="rgba(16,185,129,0.22)" />
          <stop offset="100%" stop-color="rgba(16,185,129,0.06)" />
        </linearGradient>
        <linearGradient id="curveGrad2" x1="0%" y1="0%" x2="100%" y2="0%">
          <stop offset="0%" stop-color="rgba(5,150,105,0.18)" />
          <stop offset="100%" stop-color="rgba(5,150,105,0.04)" />
        </linearGradient>
      </defs>

      <path
        d="M0,120 C 240,80 480,160 720,120 C 960,80 1200,160 1440,120"
        class="line line1"
        stroke="url(#curveGrad1)"
        stroke-width="2.2"
        stroke-linecap="round"
        fill="none"
      />
      <path
        d="M0,220 C 260,180 520,260 780,220 C 1040,180 1300,260 1440,220"
        class="line line2"
        stroke="url(#curveGrad2)"
        stroke-width="1.8"
        stroke-linecap="round"
        fill="none"
      />
      <path
        d="M0,360 C 300,300 600,420 900,360 C 1200,300 1350,420 1440,380"
        class="line line3"
        stroke="url(#curveGrad1)"
        stroke-width="1.8"
        stroke-linecap="round"
        fill="none"
      />
      <path
        d="M0,560 C 240,520 520,620 820,560 C 1120,500 1280,640 1440,600"
        class="line line4"
        stroke="url(#curveGrad2)"
        stroke-width="1.5"
        stroke-linecap="round"
        fill="none"
      />
    </svg>

    <main class="content-layer" style="flex: 1; padding: 24px">
      <slot />
    </main>
    <GlobalFooter />
  </div>
</template>

<script setup lang="ts">
import GlobalHeader from '@/components/layouts/GlobalHeader.vue'
import GlobalFooter from '@/components/layouts/GlobalFooter.vue'
</script>

<style scoped>
.bg-curves {
  position: fixed;
  inset: 0;
  z-index: 0;
  pointer-events: none;
  opacity: 0.75;
}

.content-layer {
  position: relative;
  z-index: 1;
}

/* Flow animations to enhance "fluid" feel */
.line {
  transform-origin: center;
  animation: flow 14s ease-in-out infinite;
}
.line2 {
  animation-duration: 16s;
  animation-delay: 0.2s;
}
.line3 {
  animation-duration: 18s;
  animation-delay: 0.4s;
}
.line4 {
  animation-duration: 20s;
  animation-delay: 0.6s;
}

@keyframes flow {
  0% {
    transform: translateY(0px);
    opacity: 0.9;
  }
  50% {
    transform: translateY(6px);
    opacity: 1;
  }
  100% {
    transform: translateY(0px);
    opacity: 0.9;
  }
}

:deep(body) {
  margin: 0;
  padding: 0;
}

:deep(#app) {
  margin: 0;
  padding: 0;
}
</style>
