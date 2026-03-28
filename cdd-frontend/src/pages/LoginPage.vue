<template>
  <div :class="$style.page">
    <div :class="$style.backdrop"></div>
    <section :class="$style.panel">
      <div :class="$style.intro">
        <div :class="$style.badge">ChengDD 商家后台</div>
        <h1 :class="$style.title">把商家运营动作整理成一张低噪音工作台。</h1>
        <p :class="$style.description">
          一期先覆盖登录、工作台、商品、订单、售后和配置中心，帮助商家把日常经营操作收口到统一后台。
        </p>
        <div :class="$style.statusCard">
          <div :class="$style.statusLabel">当前状态</div>
          <div :class="$style.statusValue">{{ authStore.authNotice }}</div>
        </div>
        <ul :class="$style.highlights">
          <li>支持登录后进入工作台，统一查看商品、订单、售后和配置。</li>
          <li>登录成功后会自动加载商户、店铺与操作人上下文。</li>
          <li>异常状态会直接在页面中提示，便于快速定位问题。</li>
        </ul>
      </div>

      <form :class="$style.form" @submit.prevent="handleLogin">
        <div :class="$style.formHeader">
          <div :class="$style.formEyebrow">商家后台登录</div>
          <div :class="$style.formTitle">欢迎回来</div>
        </div>
        <UiStatePanel
          :tone="authStore.authMode === 'remote' ? 'info' : 'error'"
          :title="authStore.authMode === 'remote' ? '认证状态' : '认证服务暂未就绪'"
          :description="authStore.authNotice"
        />
        <UiInput v-model="account" label="账号" placeholder="请输入运营账号" prefix="ID" />
        <UiInput
          v-model="password"
          label="密码"
          type="password"
          placeholder="请输入登录密码"
          prefix="PW"
        />
        <UiStatePanel
          v-if="feedback"
          :tone="feedbackTone"
          :title="feedbackTitle"
          :description="feedback"
        />
        <UiButton :disabled="submitting || !canSubmitLogin" type="submit" size="lg" block>
          {{ submitting ? '正在进入工作台...' : '进入工作台' }}
        </UiButton>
        <div :class="$style.helpText">登录成功后会自动加载当前账号上下文，再进入工作台。</div>
      </form>
    </section>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue';
import { useRouter } from 'vue-router';
import UiButton from '@/components/base/UiButton.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import { useAuthStore } from '@/stores/auth';
import { ApiClientError } from '@/types/api';

const router = useRouter();
const authStore = useAuthStore();

const account = ref('');
const password = ref('');
const submitting = ref(false);
const feedback = ref('');
const feedbackTone = ref<'info' | 'error'>('info');
const feedbackTitle = ref('登录状态');
const canSubmitLogin = computed(() => account.value.trim().length > 0 && password.value.length > 0);

async function handleLogin() {
  submitting.value = true;
  feedback.value = '';

  try {
    if (!canSubmitLogin.value) {
      throw new Error('请输入账号和密码后再登录。');
    }
    const result = await authStore.login({
      account: account.value.trim(),
      password: password.value,
    });
    feedbackTone.value = 'info';
    feedbackTitle.value = '登录结果';
    feedback.value = result.message;
    await router.push('/dashboard');
  } catch (error) {
    feedbackTone.value = 'error';
    feedbackTitle.value = '登录失败';
    feedback.value =
      error instanceof ApiClientError ? error.message : '登录失败，请检查服务启动状态或账号密码。';
  } finally {
    submitting.value = false;
  }
}
</script>

<style module>
.page {
  position: relative;
  min-height: 100vh;
  display: grid;
  place-items: center;
  overflow: hidden;
  padding: 32px;
}

.backdrop {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(circle at 15% 18%, rgba(255, 107, 0, 0.22), transparent 0, transparent 30%),
    radial-gradient(circle at 84% 20%, rgba(9, 29, 46, 0.1), transparent 0, transparent 22%),
    linear-gradient(145deg, rgba(255, 255, 255, 0.86), rgba(237, 244, 255, 0.94));
}

.panel {
  position: relative;
  z-index: 1;
  display: grid;
  grid-template-columns: minmax(0, 1.05fr) minmax(380px, 460px);
  gap: 28px;
  width: min(1180px, 100%);
  padding: 28px;
  border-radius: 32px;
  background: rgba(255, 255, 255, 0.76);
  box-shadow: var(--cdd-shadow);
  backdrop-filter: blur(20px);
}

.intro {
  padding: 18px;
}

.badge {
  display: inline-flex;
  align-items: center;
  min-height: 34px;
  padding: 0 14px;
  border-radius: 999px;
  color: var(--cdd-primary-deep);
  background: rgba(255, 107, 0, 0.1);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title {
  max-width: 680px;
  margin: 24px 0 0;
  font-size: clamp(34px, 4vw, 58px);
  line-height: 0.98;
  letter-spacing: -0.06em;
}

.description {
  max-width: 560px;
  margin: 20px 0 0;
  color: var(--cdd-text-soft);
  font-size: 16px;
  line-height: 1.8;
}

.highlights {
  margin: 28px 0 0;
  padding: 0;
  list-style: none;
  display: grid;
  gap: 12px;
  color: var(--cdd-text);
}

.highlights li {
  padding: 16px 18px;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.9);
  font-size: 14px;
  line-height: 1.7;
}

.statusCard {
  max-width: 560px;
  margin-top: 24px;
  padding: 18px 20px;
  border-radius: 20px;
  background:
    linear-gradient(135deg, rgba(255, 241, 223, 0.92), rgba(255, 255, 255, 0.96)),
    var(--cdd-warning-soft);
}

.statusLabel {
  color: var(--cdd-primary-deep);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.statusValue {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.8;
  color: var(--cdd-text);
}

.form {
  display: grid;
  align-content: start;
  gap: 18px;
  padding: 26px;
  border-radius: 28px;
  background: rgba(255, 255, 255, 0.88);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.05);
}

.formHeader {
  margin-bottom: 8px;
}

.formEyebrow {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.formTitle {
  margin-top: 10px;
  font-size: 28px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.helpText {
  color: var(--cdd-text-faint);
  font-size: 12px;
  line-height: 1.7;
}

@media (max-width: 960px) {
  .panel {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .page {
    padding: 16px;
  }

  .panel {
    padding: 16px;
  }

  .form,
  .intro {
    padding: 10px;
  }
}
</style>
