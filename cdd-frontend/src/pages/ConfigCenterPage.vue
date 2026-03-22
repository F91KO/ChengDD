<template>
  <WorkspaceLayout
    eyebrow="Config"
    title="配置中心"
    description="当前先承载配置文件状态、发布记录与环境说明，后续再接 Nacos 与发布流水。"
  >
    <UiStatePanel
      v-if="configStatePanel"
      :tone="configStatePanel.tone"
      :title="configStatePanel.title"
      :description="configStatePanel.description"
    />

    <section :class="$style.grid">
      <UiCard elevated :class="$style.mainPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">功能开关</div>
            <h3 :class="$style.title">商家当前生效开关</h3>
          </div>
          <UiButton variant="secondary">发起发布</UiButton>
        </div>
        <div :class="$style.configList">
          <article v-for="item in configGroups" :key="item.name" :class="$style.configItem">
            <div>
              <div :class="$style.configName">{{ item.name }}</div>
              <div :class="$style.configDescription">{{ item.description }}</div>
            </div>
            <UiTag :tone="item.statusTone as 'default' | 'primary' | 'success'">
              {{ item.status }}
            </UiTag>
          </article>
        </div>
      </UiCard>

      <UiCard elevated :class="$style.sidePanel">
        <div :class="$style.eyebrow">生效配置</div>
        <h3 :class="$style.title">本地联调基线</h3>
        <ul :class="$style.notes">
          <li>默认时区：{{ effectiveConfigSummary.timeZone }}</li>
          <li>配置来源：{{ effectiveConfigSummary.configSource }}</li>
          <li>商家标识：{{ effectiveConfigSummary.merchantId }}</li>
        </ul>
        <div :class="$style.sideState">
          <UiStatePanel
            :tone="configMode === 'remote' ? 'info' : 'loading'"
            :title="configMode === 'remote' ? '已接入真实配置接口' : '当前使用演示配置摘要'"
            :description="
              configMode === 'remote'
                ? '商家功能开关与生效配置已从 config-service 读取，发布记录仍保留演示数据。'
                : configNotice
            "
          />
        </div>
      </UiCard>
    </section>

    <section>
      <UiCard elevated :class="$style.recordPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">发布记录</div>
            <h3 :class="$style.title">最近三次操作</h3>
          </div>
        </div>
        <div :class="$style.recordTable">
          <div :class="$style.recordHead">
            <span>版本</span>
            <span>操作人</span>
            <span>目标</span>
            <span>结果</span>
          </div>
          <div v-for="record in publishRecords" :key="record.version" :class="$style.recordRow">
            <span :class="$style.strong">{{ record.version }}</span>
            <span>{{ record.operator }}</span>
            <span>{{ record.target }}</span>
            <UiTag :tone="record.tone as 'success' | 'danger' | 'info'">{{ record.result }}</UiTag>
          </div>
        </div>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import { fetchEffectiveConfig, fetchMerchantFeatureSwitches } from '@/services/config';
import { useAuthStore } from '@/stores/auth';
import type { ConfigKvValueResponseRaw, FeatureSwitchValueResponseRaw } from '@/types/config';
import { configGroups as mockConfigGroups, publishRecords } from '@/modules/config/mock';

const authStore = useAuthStore();
const configMode = ref<'remote' | 'mock'>('mock');
const configNotice = ref('正在尝试连接配置中心接口，未接通时会回退到演示数据。');
const configGroups = ref(mockConfigGroups.map((item) => ({ ...item })));
const effectiveConfig = ref<ConfigKvValueResponseRaw | null>(null);

const effectiveConfigSummary = computed(() => {
  if (!effectiveConfig.value) {
    return {
      timeZone: 'Asia/Shanghai',
      configSource: '演示数据',
      merchantId: authStore.context?.merchantId || 'merchant_1001',
    };
  }
  return {
    timeZone: effectiveConfig.value.config_value,
    configSource: effectiveConfig.value.source,
    merchantId: effectiveConfig.value.merchant_id || authStore.context?.merchantId || 'merchant_1001',
  };
});

const configStatePanel = computed(() => {
  if (authStore.authMode === 'mock') {
    return {
      tone: 'empty' as const,
      title: '当前展示演示配置',
      description: '认证服务未接通，配置中心页面先使用演示数据。',
    };
  }
  if (configMode.value !== 'remote') {
    return {
      tone: 'info' as const,
      title: '配置接口暂未接通',
      description: configNotice.value,
    };
  }
  return null;
});

function resolveMerchantId(): string {
  return authStore.context?.merchantId || `merchant_${authStore.merchantIdForQuery ?? 1001}`;
}

function toConfigGroups(items: FeatureSwitchValueResponseRaw[]) {
  return items.map((item) => ({
    name: item.switch_name,
    description: `${item.switch_code} · 范围 ${item.switch_scope} · 默认 ${item.default_value} · 来源 ${item.source}`,
    status: item.effective_value === 'on' ? '已开启' : '已关闭',
    statusTone: item.effective_value === 'on' ? 'success' : item.status === 'enabled' ? 'primary' : 'default',
  }));
}

function fallbackToMock(message: string) {
  configMode.value = 'mock';
  configNotice.value = message;
  configGroups.value = mockConfigGroups.map((item) => ({ ...item }));
  effectiveConfig.value = null;
}

async function loadConfigCenter() {
  await authStore.ensureCurrentContext();
  const merchantId = resolveMerchantId();
  try {
    const [switches, timeZoneConfig] = await Promise.all([
      fetchMerchantFeatureSwitches(merchantId),
      fetchEffectiveConfig({
        merchantId,
        configGroup: 'system',
        configKey: 'default_time_zone',
      }),
    ]);
    if (!switches.length) {
      fallbackToMock('配置接口已接通，但当前商家还没有可展示的功能开关数据。');
      return;
    }
    configMode.value = 'remote';
    configNotice.value = '配置中心已连接真实接口。';
    configGroups.value = toConfigGroups(switches);
    effectiveConfig.value = timeZoneConfig;
  } catch (error) {
    fallbackToMock(`配置服务未就绪，页面已回退到演示数据。${error instanceof Error ? error.message : ''}`.trim());
  }
}

onMounted(() => {
  void loadConfigCenter();
});
</script>

<style module>
.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.4fr) minmax(320px, 0.8fr);
  gap: 18px;
}

.mainPanel,
.sidePanel,
.recordPanel {
  padding: 24px;
}

.panelHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
}

.eyebrow {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title {
  margin: 8px 0 0;
  font-size: 22px;
  letter-spacing: -0.04em;
}

.configList {
  display: grid;
  gap: 14px;
  margin-top: 20px;
}

.configItem {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 18px;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.72);
}

.configName {
  font-size: 16px;
  font-weight: 800;
}

.configDescription {
  margin-top: 8px;
  color: var(--cdd-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.notes {
  margin: 18px 0 0;
  padding-left: 18px;
  color: var(--cdd-text-soft);
  line-height: 1.9;
}

.sideState {
  margin-top: 20px;
}

.recordTable {
  display: grid;
  gap: 10px;
  margin-top: 20px;
}

.recordHead,
.recordRow {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
  align-items: center;
  padding: 14px 16px;
}

.recordHead {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: rgba(237, 244, 255, 0.72);
  border-radius: 16px;
}

.recordRow {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.04);
}

.strong {
  font-weight: 800;
}

@media (max-width: 960px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .panelHead {
    flex-direction: column;
    align-items: flex-start;
  }

  .recordHead {
    display: none;
  }

  .recordRow {
    grid-template-columns: 1fr;
  }
}
</style>
