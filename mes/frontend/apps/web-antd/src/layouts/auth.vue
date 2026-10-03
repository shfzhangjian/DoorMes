<script lang="ts" setup>
import { computed, provide, ref } from 'vue';

import { AuthPageLayout } from '@vben/layouts';
import { preferences, updatePreferences } from '@vben/preferences';

updatePreferences({
  app: { authPageLayout: 'panel-right' },
  theme: { mode: 'light' },
});

const appName = computed(() => preferences.app.name);

const focusItems = ['门窗绘图与材料映射', '标准设计与定制需求', '图纸版本与生产协同'];
const showTenantSelect = ref(true);

provide('hechenShowTenantSelect', showTenantSelect);

</script>

<template>
  <AuthPageLayout
    :app-name="appName"
    :copyright="false"
    :toolbar="false"
    class="hechen-auth-page"
    page-description="从订单需求到门窗设计、图纸定稿和生产变更"
    page-title="门窗工厂设计与制造协同"
  >
    <template #logo>
      <div
        class="hechen-auth-corner-brand"
        aria-label="DoorMes 门窗工厂"
      >
        <strong>DoorMes · 门窗工厂</strong>
        <span>WINDOW &amp; DOOR FACTORY</span>
      </div>
    </template>

    <template #showcase>
      <section class="hechen-auth-showcase">
        <div class="hechen-auth-kicker">DOORMES / DESIGN &amp; MANUFACTURING</div>
        <h1>
          <span>门窗设计</span>
          <span>连接制造现场</span>
        </h1>
        <p>
          以图纸与组成件为核心，协同销售、设计研发、工艺、采购与生产。
          标准设计复用，定制需求绘制，保留每次图纸修订和制造变更依据。
        </p>
        <div class="hechen-auth-focus">
          <span v-for="item in focusItems" :key="item">{{ item }}</span>
        </div>
      </section>
    </template>
  </AuthPageLayout>
</template>

<style>
.hechen-auth-page {
  position: relative;
  min-height: 100vh;
  background:
    linear-gradient(
      90deg,
      rgb(4 19 32 / 74%) 0%,
      rgb(4 26 45 / 58%) 44%,
      rgb(248 251 253 / 36%) 68%,
      rgb(248 251 253 / 76%) 100%
    ),
    url('/doormes-login-background.svg') center / cover no-repeat;
  color: #fff;
}

.hechen-auth-page > .relative.hidden > .absolute.inset-0 {
  background: transparent !important;
}

.hechen-auth-page .login-background {
  background: transparent !important;
  filter: none !important;
}

.hechen-auth-page > [class*='w-2/5'] {
  flex: 0 0 min(460px, 38vw) !important;
  width: min(460px, 38vw) !important;
  min-width: 420px;
  border-left: 1px solid rgb(255 255 255 / 58%);
  background: rgb(255 255 255 / 90%) !important;
  box-shadow: -24px 0 60px rgb(7 26 42 / 18%);
  color: hsl(var(--foreground));
  backdrop-filter: blur(18px);
}

.hechen-auth-page .side-content {
  width: min(100%, 380px) !important;
  max-width: 380px !important;
  margin-top: 0 !important;
  color: hsl(var(--foreground));
}

.hechen-auth-corner-brand {
  position: absolute;
  top: 28px;
  left: 32px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 4px;
  align-items: flex-start;
  background: transparent;
  padding: 0;
  color: #fff;
  cursor: default;
  text-shadow: 0 2px 14px rgb(0 0 0 / 24%);
  user-select: none;
}

.hechen-auth-corner-brand strong {
  font-size: 25px;
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.1;
}

.hechen-auth-corner-brand span {
  color: rgb(230 247 255 / 86%);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
}

.hechen-auth-showcase {
  display: flex;
  width: min(720px, 74%);
  flex-direction: column;
  gap: 20px;
  margin-right: 6vw;
  color: #fff;
  text-shadow: 0 2px 18px rgb(0 0 0 / 22%);
}

.hechen-auth-kicker {
  width: fit-content;
  border-bottom: 2px solid rgb(80 190 255 / 92%);
  padding-bottom: 8px;
  color: rgb(220 245 255 / 94%);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0;
}

.hechen-auth-showcase h1 {
  max-width: 760px;
  margin: 0;
  color: #fff;
  font-size: clamp(36px, 4vw, 58px);
  font-weight: 900;
  letter-spacing: 0;
  line-height: 1.08;
}

.hechen-auth-showcase h1 span {
  display: block;
}

.hechen-auth-showcase p {
  max-width: 640px;
  margin: 0;
  color: rgb(240 248 255 / 88%);
  font-size: 16px;
  line-height: 1.9;
}

.hechen-auth-focus {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding-top: 8px;
}

.hechen-auth-focus span {
  border: 1px solid rgb(255 255 255 / 34%);
  border-radius: 8px;
  background: rgb(9 38 63 / 34%);
  padding: 9px 13px;
  color: rgb(255 255 255 / 92%);
  font-size: 13px;
  font-weight: 700;
}

@media (max-width: 1180px) {
  .hechen-auth-page > [class*='w-2/5'] {
    flex-basis: 400px !important;
    width: 400px !important;
    min-width: 400px;
  }

  .hechen-auth-showcase {
    width: min(620px, 72%);
    margin-right: 4vw;
  }
}

@media (max-width: 1023px) {
  .hechen-auth-page {
    background:
      linear-gradient(
        180deg,
        rgb(4 19 32 / 42%) 0%,
        rgb(248 251 253 / 76%) 46%,
        rgb(248 251 253 / 95%) 100%
      ),
      url('/doormes-login-background.svg') center top / cover no-repeat;
  }

  .hechen-auth-page > [class*='w-2/5'] {
    flex: 1 1 auto !important;
    width: 100% !important;
    min-width: 0;
    min-height: 100vh;
    border-left: 0;
    background: rgb(255 255 255 / 88%) !important;
    box-shadow: none;
  }

  .hechen-auth-corner-brand {
    top: 22px;
    left: 22px;
  }
}

@media (max-width: 640px) {
  .hechen-auth-corner-brand {
    right: 18px;
    left: 18px;
    width: fit-content;
    padding: 0;
  }

  .hechen-auth-corner-brand strong {
    font-size: 19px;
  }

  .hechen-auth-corner-brand span {
    font-size: 11px;
  }
}
</style>
