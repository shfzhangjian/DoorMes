<script lang="ts" setup>
import type { VbenFormSchema } from '@vben/common-ui';

import type { AuthApi } from '#/api/core/auth';
import type { Ref } from 'vue';

import { computed, inject, nextTick, onMounted, ref, watch } from 'vue';
import { useRoute } from 'vue-router';

import { AuthenticationLogin, Verification, z } from '@vben/common-ui';
import { isCaptchaEnable, isTenantEnable } from '@vben/hooks';
import { $t } from '@vben/locales';
import { useAccessStore } from '@vben/stores';


import {
  checkCaptcha,
  getCaptcha,
  getTenantByWebsite,
  getTenantSimpleList,
  socialAuthRedirect,
} from '#/api/core/auth';
import { useAuthStore } from '#/store';

defineOptions({ name: 'Login' });

const { query } = useRoute();
const authStore = useAuthStore();
const accessStore = useAccessStore();
const tenantEnable = isTenantEnable();
const captchaEnable = isCaptchaEnable();
const showTenantSelect = inject<Ref<boolean>>(
  'hechenShowTenantSelect',
  ref(true),
);

const loginRef = ref();
const verifyRef = ref();

const captchaType = 'blockPuzzle'; // 验证码类型：'blockPuzzle' | 'clickWord'

function withTenantPayload(values: any) {
  if (!tenantEnable) {
    return values;
  }
  const tenantId = values?.tenantId ?? accessStore.tenantId;
  return {
    ...values,
    tenantId:
      tenantId === null || tenantId === undefined ? tenantId : `${tenantId}`,
  };
}

async function syncTenantFieldValue() {
  if (!tenantEnable || !showTenantSelect.value || !accessStore.tenantId) {
    return;
  }
  await nextTick();
  loginRef.value
    ?.getFormApi()
    ?.setFieldValue('tenantId', accessStore.tenantId.toString());
}

/** 获取租户列表，并默认选中 */
const tenantList = ref<AuthApi.TenantResult[]>([]); // 租户列表
async function fetchTenantList() {
  if (!tenantEnable) {
    return;
  }
  try {
    // 获取租户列表、域名对应租户
    const websiteTenantPromise = getTenantByWebsite(window.location.hostname);
    tenantList.value = await getTenantSimpleList();

    // 选中租户：域名 > store 中的租户 > 首个租户
    let tenantId: null | number = null;
    const websiteTenant = await websiteTenantPromise;
    if (websiteTenant?.id && tenantList.value.some((item) => item.id === websiteTenant.id)) {
      tenantId = websiteTenant.id;
    }
    // 如果没有从域名获取到租户，尝试从 store 中获取
    if (!tenantId && tenantList.value.some((item) => item.id === accessStore.tenantId)) {
      tenantId = accessStore.tenantId;
    }
    // 如果还是没有租户，使用列表中的第一个
    if (!tenantId && tenantList.value?.[0]?.id) {
      tenantId = tenantList.value[0].id;
    }

    // 设置选中的租户编号
    accessStore.setTenantId(tenantId);
    await syncTenantFieldValue();
  } catch (error) {
    console.error('获取租户列表失败:', error);
  }
}

/** 处理登录 */
async function handleLogin(values: any) {
  // 如果开启验证码，则先验证验证码
  if (captchaEnable) {
    verifyRef.value.show();
    return;
  }
  // 无验证码，直接登录
  await authStore.authLogin('username', withTenantPayload(values));
}

/** 验证码通过，执行登录 */
async function handleVerifySuccess({ captchaVerification }: any) {
  try {
    await authStore.authLogin('username', {
      ...withTenantPayload(await loginRef.value.getFormApi().getValues()),
      captchaVerification,
    });
  } catch (error) {
    console.error('Error in handleLogin:', error);
  }
}

/** 处理第三方登录 */
const redirect = query?.redirect;
async function handleThirdLogin(type: number) {
  if (type <= 0) {
    return;
  }
  try {
    // 计算 redirectUri
    // tricky: type、redirect 需要先 encode 一次，否则钉钉回调会丢失。配合 social-login.vue#getUrlValue() 使用
    const redirectUri = `${
      location.origin
    }/auth/social-login?${encodeURIComponent(
      `type=${type}&redirect=${redirect || '/'}`,
    )}`;

    // 进行跳转
    window.location.href = await socialAuthRedirect(type, redirectUri);
  } catch (error) {
    console.error('第三方登录处理失败:', error);
  }
}

/** 组件挂载时获取租户信息 */
onMounted(() => {
  fetchTenantList();
});

watch(showTenantSelect, () => {
  syncTenantFieldValue();
});

const formSchema = computed((): VbenFormSchema[] => {
  return [
    {
      component: 'VbenSelect',
      componentProps: {
        options: tenantList.value.map((item) => ({
          label: item.name,
          value: item.id.toString(),
        })),
        placeholder: $t('authentication.tenantTip'),
      },
      fieldName: 'tenantId',
      label: $t('authentication.tenant'),
      rules: z.string().min(1, { message: $t('authentication.tenantTip') }),
      dependencies: {
        triggerFields: ['tenantId'],
        if: tenantEnable && showTenantSelect.value,
        trigger(values) {
          if (values.tenantId) {
            accessStore.setTenantId(Number(values.tenantId));
          }
        },
      },
    },
    {
      component: 'VbenInput',
      componentProps: {
        placeholder: $t('authentication.usernameTip'),
      },
      fieldName: 'username',
      label: $t('authentication.username'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.usernameTip') })
        .default(import.meta.env.VITE_APP_DEFAULT_USERNAME),
    },
    {
      component: 'VbenInputPassword',
      componentProps: {
        placeholder: $t('authentication.passwordTip'),
      },
      fieldName: 'password',
      label: $t('authentication.password'),
      rules: z
        .string()
        .min(1, { message: $t('authentication.passwordTip') })
        .default(import.meta.env.VITE_APP_DEFAULT_PASSWORD),
    },
  ];
});
</script>

<template>
  <div>
    <AuthenticationLogin
      ref="loginRef"
      :form-schema="formSchema"
      :loading="authStore.loginLoading"
      submit-button-text="进入门窗工厂"
      @submit="handleLogin"
      @third-login="handleThirdLogin"
    >
      <template #title>
        <div class="hechen-login-brand">
          <img
            alt="DoorMes 门窗工厂"
            class="hechen-login-mark"
            src="/doormes-logo.svg"
          />
          <div class="hechen-login-copy">
            <div class="hechen-login-title">欢迎登录</div>
            <div class="hechen-login-subtitle">
              门窗设计与制造协同
            </div>
          </div>
        </div>
      </template>
    </AuthenticationLogin>
    <p class="doormes-login-note">请选择所属工厂，使用管理员分配的账号登录。</p>
    <Verification
      ref="verifyRef"
      v-if="captchaEnable"
      :captcha-type="captchaType"
      :check-captcha-api="checkCaptcha"
      :get-captcha-api="getCaptcha"
      :img-size="{ width: '400px', height: '200px' }"
      mode="pop"
      @on-success="handleVerifySuccess"
    />
  </div>
</template>

<style scoped>
.doormes-login-note {
  margin-top: 20px;
  color: #64748b;
  font-size: 13px;
  text-align: center;
}
.hechen-login-brand {
  display: flex;
  align-items: flex-end;
  gap: 14px;
  margin-bottom: 26px;
}

.hechen-login-mark {
  flex: 0 0 auto;
  display: block;
  width: 70px;
  height: 70px;
  object-fit: contain;
}

.hechen-login-copy {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.hechen-login-title {
  color: hsl(var(--foreground));
  font-size: 26px;
  font-weight: 800;
  letter-spacing: 0;
  line-height: 1.2;
}

.hechen-login-subtitle {
  color: hsl(var(--muted-foreground));
  font-size: 14px;
  line-height: 1.6;
}

.hechen-login-downloads {
  display: flex;
  justify-content: center;
  margin-top: 14px;
  font-size: 13px;
  line-height: 1.6;
}

.hechen-login-download-entry {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: hsl(var(--primary));
}

.hechen-download-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.hechen-download-card {
  display: grid;
  place-items: center;
  min-height: 128px;
  padding: 18px 12px;
  border: 1px solid hsl(var(--border));
  border-radius: 8px;
  color: hsl(var(--foreground));
  text-align: center;
  text-decoration: none;
  transition:
    border-color 0.2s ease,
    box-shadow 0.2s ease,
    transform 0.2s ease;
}

.hechen-download-card:hover {
  border-color: hsl(var(--primary));
  box-shadow: 0 8px 22px rgb(15 23 42 / 12%);
  transform: translateY(-1px);
}

.hechen-download-card svg {
  width: 42px;
  height: 42px;
  margin-bottom: 10px;
  color: hsl(var(--primary));
}

.hechen-download-card span {
  color: hsl(var(--foreground));
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 640px) {
  .hechen-login-brand {
    justify-content: center;
  }

  .hechen-login-mark {
    width: 62px;
    height: 62px;
  }

  .hechen-login-title {
    font-size: 24px;
  }

  .hechen-login-downloads {
    align-items: center;
  }

  .hechen-download-grid {
    grid-template-columns: 1fr;
  }
}
</style>
