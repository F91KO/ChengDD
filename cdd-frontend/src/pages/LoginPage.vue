<template>
  <div :class="$style.page">
    <div :class="$style.backdrop"></div>
    <section :class="$style.panel">
      <div :class="$style.intro">
        <div :class="$style.badge">ChengDD 商家后台</div>
        <h1 :class="$style.title">把商家运营动作整理成一张低噪音工作台。</h1>
        <p :class="$style.description">
          一期先覆盖登录、工作台、商品、订单、售后和配置中心。当前页面只走真实接口，
          认证失败时会直接提示错误，不再回退前端本地假数据。
        </p>
        <div :class="$style.statusCard">
          <div :class="$style.statusLabel">当前接入状态</div>
          <div :class="$style.statusValue">{{ authStore.authNotice }}</div>
        </div>
        <ul :class="$style.highlights">
          <li>Java 21 后端运行基线已确认，前端统一接入真实 API。</li>
          <li>统一 token 注入、401 处理、刷新令牌与路由守卫已接入。</li>
          <li>登录成功后才会加载商户、店铺与操作人上下文，不再使用前端假数据。</li>
        </ul>
      </div>

      <form :class="$style.form" @submit.prevent="handleLogin">
        <div :class="$style.formHeader">
          <div :class="$style.formEyebrow">商家后台登录</div>
          <div :class="$style.formTitle">欢迎回来</div>
        </div>
        <div :class="$style.accountNotice">
          <div :class="$style.accountTitle">默认本地账号</div>
          <div :class="$style.accountValue">merchant_admin</div>
          <div :class="$style.accountHint">请先启动本地认证服务和数据库，再使用该账号登录。</div>
        </div>
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
        <UiButton :disabled="submitting" type="submit" size="lg" block>
          {{ submitting ? '正在进入工作台...' : '进入工作台' }}
        </UiButton>
        <div :class="$style.helpText">登录成功后会优先加载真实身份上下文，再进入工作台。</div>
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

const account = ref('merchant_admin');
const password = ref('merchant123456');
const submitting = ref(false);
const feedback = ref('');

const feedbackTone = computed(() => (authStore.authMode === 'remote' ? 'info' : 'error'));
const feedbackTitle = computed(() => '接口状态');

async function handleLogin() {
  submitting.value = true;
  feedback.value = '';

  try {
    const result = await authStore.login({
      account: account.value.trim() || 'merchant_admin',
      password: password.value,
    });
    feedback.value = result.message;
    await router.push('/dashboard');
  } catch (error) {
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

.accountNotice {
  padding: 16px 18px;
  border-radius: 20px;
  background: rgba(237, 244, 255, 0.78);
}

.accountTitle {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.accountValue {
  margin-top: 8px;
  font-size: 18px;
  font-weight: 800;
  letter-spacing: -0.03em;
}

.accountHint {
  margin-top: 6px;
  color: var(--cdd-text-soft);
  font-size: 12px;
  line-height: 1.7;
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
