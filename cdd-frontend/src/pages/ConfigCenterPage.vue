<template>
  <WorkspaceLayout
    eyebrow="Config"
    title="配置中心"
    description="当前页面已接入真实功能开关、生效配置、发布记录、回滚和发布详情快照。"
  >
    <UiStatePanel
      v-if="configStatePanel"
      :tone="configStatePanel.tone"
      :title="configStatePanel.title"
      :description="configStatePanel.description"
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.grid">
      <UiCard elevated :class="$style.mainPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">功能开关</div>
            <h3 :class="$style.title">商家当前生效开关</h3>
          </div>
          <UiButton variant="secondary" @click="loadConfigCenter">刷新配置</UiButton>
        </div>
        <div :class="$style.configList">
          <article v-for="item in configGroups" :key="item.switchCode" :class="$style.configItem">
            <div>
              <div :class="$style.configName">{{ item.name }}</div>
              <div :class="$style.configDescription">{{ item.description }}</div>
            </div>
            <div :class="$style.configActions">
              <UiTag :tone="item.statusTone as 'default' | 'primary' | 'success'">
                {{ item.status }}
              </UiTag>
              <UiButton variant="secondary" @click="handleToggleSwitch(item)">
                {{ item.effectiveValue === 'on' ? '关闭' : '开启' }}
              </UiButton>
            </div>
          </article>
          <UiStatePanel
            v-if="!configGroups.length"
            tone="empty"
            title="当前没有真实开关数据"
            description="请确认 config-service 是否已完成商家开关初始化。"
          />
        </div>
      </UiCard>

      <UiCard elevated :class="$style.sidePanel">
        <div :class="$style.eyebrow">生效配置</div>
        <h3 :class="$style.title">本地联调基线</h3>
        <ul :class="$style.notes">
          <li>平台默认时区：{{ platformDefaultTimeZone }}</li>
          <li>默认时区：{{ effectiveConfigSummary.timeZone }}</li>
          <li>配置来源：{{ effectiveConfigSummary.configSource }}</li>
          <li>商家标识：{{ effectiveConfigSummary.merchantId }}</li>
          <li>报表状态：{{ reportHealthSummary }}</li>
        </ul>
        <div :class="$style.formStack">
          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">平台默认时区</span>
            <input
              v-model="platformTimeZoneInput"
              :class="$style.input"
              placeholder="例如：Asia/Shanghai"
            />
          </label>
          <div :class="$style.inlineActions">
            <UiButton variant="secondary" @click="handleUpdatePlatformTimeZone">更新平台默认值</UiButton>
          </div>
          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">商家时区覆盖</span>
            <input
              v-model="merchantOverrideTimeZoneInput"
              :class="$style.input"
              placeholder="例如：Asia/Shanghai"
            />
          </label>
          <div :class="$style.inlineActions">
            <UiButton variant="secondary" @click="handleUpdateMerchantTimeZone">保存商家覆盖</UiButton>
          </div>
        </div>
        <div :class="$style.sideState">
          <UiStatePanel
            :tone="configMode === 'remote' ? 'info' : 'error'"
            :title="configMode === 'remote' ? '已接入真实配置接口' : '真实配置接口异常'"
            :description="
              configMode === 'remote'
                ? '商家功能开关与生效配置已从 config-service 读取。'
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
            <div :class="$style.eyebrow">联调健康度</div>
            <h3 :class="$style.title">报表与看板数据就绪情况</h3>
          </div>
        </div>
        <UiStatePanel
          :tone="reportHealth?.ready ? 'info' : 'error'"
          :title="reportHealth?.ready ? '报表健康度已就绪' : '报表健康度未完成'"
          :description="reportHealth?.summary || '正在检查报表健康度。'"
        />
        <div v-if="reportHealth?.items?.length" :class="$style.recordTable">
          <div :class="$style.recordHead">
            <span>检查项</span>
            <span>状态</span>
            <span>最近数据</span>
            <span>说明</span>
          </div>
          <div v-for="item in reportHealth.items" :key="item.code" :class="$style.recordRow">
            <div :class="$style.strong">{{ item.name }}</div>
            <div>{{ item.status === 'ready' ? '已就绪' : '缺失' }}</div>
            <div>{{ item.latest_data_time || '-' }}</div>
            <div>{{ item.message }}</div>
          </div>
        </div>

        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">发布记录</div>
            <h3 :class="$style.title">配置发布与回滚</h3>
          </div>
          <UiButton @click="handleCreatePublish">发起发布</UiButton>
        </div>

        <div :class="$style.formStack">
          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">发布说明</span>
            <textarea v-model="publishNote" :class="$style.textarea" placeholder="请输入发布说明" />
          </label>
          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">回滚原因</span>
            <textarea v-model="rollbackReason" :class="$style.textarea" placeholder="请输入回滚原因" />
          </label>
          <UiButton variant="secondary" :disabled="!publishRecords.length" @click="handleRollbackLatest">
            回滚最新记录
          </UiButton>
        </div>

        <div v-if="publishRecords.length" :class="$style.recordTable">
          <div :class="$style.recordHead">
            <span>任务号</span>
            <span>状态</span>
            <span>创建时间</span>
            <span>说明</span>
          </div>
          <button
            v-for="record in publishRecords"
            :key="record.task_no"
            type="button"
            :class="[$style.recordRow, selectedPublishRecord?.task_no === record.task_no ? $style.recordRowActive : '']"
            @click="handleSelectPublishRecord(record.task_no)"
          >
            <div :class="$style.strong">{{ record.task_no }}</div>
            <div>{{ record.release_status }}</div>
            <div>{{ record.created_at }}</div>
            <div>{{ record.publish_note || '-' }}</div>
          </button>
        </div>

        <UiStatePanel
          v-if="publishDetailLoading"
          tone="loading"
          title="正在加载发布详情"
          description="正在读取发布快照、步骤和回滚信息。"
        />

        <div v-else-if="selectedPublishRecord" :class="$style.publishDetail">
          <div :class="$style.panelHead">
            <div>
              <div :class="$style.eyebrow">记录详情</div>
              <h3 :class="$style.title">{{ selectedPublishRecord.task_no }}</h3>
            </div>
          </div>
          <div :class="$style.detailGrid">
            <div>
              <div :class="$style.fieldLabel">发布状态</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.release_status }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">触发方式</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.trigger_source }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">配置数量</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.config_count ?? 0 }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">开关数量</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.feature_switch_count ?? 0 }}</div>
            </div>
          </div>
          <div :class="$style.detailGrid">
            <div>
              <div :class="$style.fieldLabel">发布说明</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.publish_note || '-' }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">回滚原因</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.rollback_reason || '-' }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">回滚任务</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.rollback_task_no || '-' }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">完成时间</div>
              <div :class="$style.detailValue">{{ selectedPublishRecord.finished_at || '-' }}</div>
            </div>
          </div>

          <div v-if="selectedPublishRecord.steps.length" :class="$style.recordTable">
            <div :class="$style.recordHead">
              <span>步骤</span>
              <span>状态</span>
              <span>重试</span>
              <span>结果</span>
            </div>
            <div v-for="step in selectedPublishRecord.steps" :key="step.step_code" :class="$style.recordRowStatic">
              <div :class="$style.strong">{{ step.step_name }}</div>
              <div>{{ step.step_status }}</div>
              <div>{{ step.retry_count ?? 0 }}</div>
              <div>{{ step.result_message || step.error_code || '-' }}</div>
            </div>
          </div>

          <div v-if="selectedPublishRecord.snapshot" :class="$style.snapshotGrid">
            <UiStatePanel
              tone="info"
              title="发布快照"
              :description="`平台配置 ${selectedPublishRecord.snapshot.platform_configs.length} 项，商家覆盖 ${selectedPublishRecord.snapshot.merchant_overrides.length} 项。`"
            />
            <UiStatePanel
              tone="info"
              title="功能开关快照"
              :description="`平台开关 ${selectedPublishRecord.snapshot.platform_feature_switches.length} 项，商家开关 ${selectedPublishRecord.snapshot.merchant_feature_switches.length} 项。`"
            />
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
import {
  changeMerchantFeatureSwitch,
  createPublishRecord,
  fetchEffectiveConfig,
  fetchPublishRecord,
  fetchMerchantFeatureSwitches,
  fetchPublishRecords,
  rollbackPublishRecord,
  upsertMerchantConfigOverride,
  upsertPlatformConfig,
} from '@/services/config';
import { fetchReportHealth } from '@/services/report';
import { useAuthStore } from '@/stores/auth';
import type {
  ConfigKvValueResponseRaw,
  ConfigPublishRecordResponseRaw,
  FeatureSwitchValueResponseRaw,
} from '@/types/config';
import type { ReportHealthResponseRaw } from '@/types/report';

type ConfigGroupItem = {
  switchCode: string;
  name: string;
  description: string;
  status: string;
  statusTone: 'default' | 'primary' | 'success';
  effectiveValue: string;
};

const authStore = useAuthStore();
const configMode = ref<'remote' | 'error'>('remote');
const configNotice = ref('正在加载真实配置接口。');
const configGroups = ref<ConfigGroupItem[]>([]);
const effectiveConfig = ref<ConfigKvValueResponseRaw | null>(null);
const platformConfig = ref<ConfigKvValueResponseRaw | null>(null);
const reportHealth = ref<ReportHealthResponseRaw | null>(null);
const publishRecords = ref<ConfigPublishRecordResponseRaw[]>([]);
const selectedPublishRecord = ref<ConfigPublishRecordResponseRaw | null>(null);
const publishDetailLoading = ref(false);
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const publishNote = ref('本地联调发布');
const rollbackReason = ref('联调验证后回滚');
const platformTimeZoneInput = ref('Asia/Shanghai');
const merchantOverrideTimeZoneInput = ref('Asia/Shanghai');

const reportHealthSummary = computed(() => {
  if (!reportHealth.value) {
    return '未检查';
  }
  return reportHealth.value.ready ? '已就绪' : '待补数';
});

const platformDefaultTimeZone = computed(() => platformConfig.value?.config_value || 'Asia/Shanghai');

const effectiveConfigSummary = computed(() => {
  if (!effectiveConfig.value) {
    return {
      timeZone: 'Asia/Shanghai',
      configSource: '未加载',
      merchantId: authStore.context?.merchantId || '-',
    };
  }
  return {
    timeZone: effectiveConfig.value.config_value,
    configSource: effectiveConfig.value.source,
    merchantId: effectiveConfig.value.merchant_id || authStore.context?.merchantId || 'merchant_1001',
  };
});

const configStatePanel = computed(() => {
  if (configMode.value === 'error') {
    return {
      tone: 'error' as const,
      title: '配置接口调用失败',
      description: configNotice.value,
    };
  }
  return null;
});

function resolveMerchantId(): string {
  return authStore.context?.merchantId || `merchant_${authStore.merchantIdForQuery ?? 1001}`;
}

function toConfigGroups(items: FeatureSwitchValueResponseRaw[]): ConfigGroupItem[] {
  return items.map((item) => ({
    switchCode: item.switch_code,
    name: item.switch_name,
    description: `${item.switch_code} · 范围 ${item.switch_scope} · 默认 ${item.default_value} · 来源 ${item.source}`,
    status: item.effective_value === 'on' ? '已开启' : '已关闭',
    statusTone: item.effective_value === 'on'
      ? 'success'
      : item.status === 'enabled'
        ? 'primary'
        : 'default',
    effectiveValue: item.effective_value,
  }));
}

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
}

async function loadConfigCenter() {
  await authStore.ensureCurrentContext();
  const merchantId = resolveMerchantId();
  const storeId = authStore.context?.storeId || `store_${authStore.storeIdForQuery ?? 1001}`;
  const merchantIdForQuery = authStore.merchantIdForQuery ?? 1001;
  const storeIdForQuery = authStore.storeIdForQuery ?? 1001;
  try {
    const [switches, timeZoneConfig, nextPlatformConfig, nextReportHealth, nextPublishRecords] = await Promise.all([
      fetchMerchantFeatureSwitches(merchantId),
      fetchEffectiveConfig({
        merchantId,
        configGroup: 'system',
        configKey: 'default_time_zone',
      }),
      fetchEffectiveConfig({
        configGroup: 'system',
        configKey: 'default_time_zone',
      }),
      fetchReportHealth({
        merchantId: merchantIdForQuery,
        storeId: storeIdForQuery,
      }),
      fetchPublishRecords({
        merchantId,
        storeId,
      }),
    ]);
    configMode.value = 'remote';
    configNotice.value = '配置中心已连接真实接口。';
    configGroups.value = toConfigGroups(switches);
    effectiveConfig.value = timeZoneConfig;
    platformConfig.value = nextPlatformConfig;
    reportHealth.value = nextReportHealth;
    publishRecords.value = nextPublishRecords;
    platformTimeZoneInput.value = nextPlatformConfig.config_value;
    merchantOverrideTimeZoneInput.value = timeZoneConfig.config_value;
    if (selectedPublishRecord.value) {
      const matched = nextPublishRecords.find((item) => item.task_no === selectedPublishRecord.value?.task_no);
      if (matched) {
        void handleSelectPublishRecord(matched.task_no);
      }
    }
  } catch (error) {
    configMode.value = 'error';
    configNotice.value = error instanceof Error ? error.message : '配置服务未就绪。';
    configGroups.value = [];
    effectiveConfig.value = null;
    platformConfig.value = null;
    reportHealth.value = null;
    publishRecords.value = [];
    selectedPublishRecord.value = null;
  }
}

async function handleToggleSwitch(item: ConfigGroupItem) {
  try {
    const merchantId = resolveMerchantId();
    const switchValue = item.effectiveValue === 'on' ? 'off' : 'on';
    await changeMerchantFeatureSwitch({
      merchantId,
      switchCode: item.switchCode,
      switchValue,
    });
    await loadConfigCenter();
    setActionMessage(`已将开关 ${item.name} 调整为 ${switchValue === 'on' ? '开启' : '关闭'}。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '开关调整失败。', 'error');
  }
}

async function handleCreatePublish() {
  try {
    const merchantId = resolveMerchantId();
    const storeId = authStore.context?.storeId || `store_${authStore.storeIdForQuery ?? 1001}`;
    const record = await createPublishRecord({
      merchantId,
      storeId,
      operatorName: authStore.user.operatorName || '商家管理员',
      publishNote: publishNote.value.trim() || '本地联调发布',
    });
    await loadConfigCenter();
    await handleSelectPublishRecord(record.task_no);
    setActionMessage('配置发布记录已创建。');
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '发起发布失败。', 'error');
  }
}

async function handleRollbackLatest() {
  try {
    const latest = publishRecords.value[0];
    if (!latest) {
      throw new Error('当前没有可回滚的发布记录。');
    }
    await rollbackPublishRecord({
      taskNo: latest.task_no,
      operatorName: authStore.user.operatorName || '商家管理员',
      rollbackReason: rollbackReason.value.trim() || '联调验证后回滚',
    });
    await loadConfigCenter();
    await handleSelectPublishRecord(latest.task_no);
    setActionMessage(`发布记录 ${latest.task_no} 已发起回滚。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '回滚发布记录失败。', 'error');
  }
}

async function handleSelectPublishRecord(taskNo: string) {
  try {
    publishDetailLoading.value = true;
    selectedPublishRecord.value = await fetchPublishRecord(taskNo);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '查询发布记录详情失败。', 'error');
  } finally {
    publishDetailLoading.value = false;
  }
}

async function handleUpdatePlatformTimeZone() {
  try {
    const nextValue = platformTimeZoneInput.value.trim();
    if (!nextValue) {
      throw new Error('平台默认时区不能为空。');
    }
    await upsertPlatformConfig({
      configGroup: 'system',
      configKey: 'default_time_zone',
      configValue: nextValue,
      configDesc: '前端联调更新的平台默认时区',
    });
    await loadConfigCenter();
    setActionMessage(`平台默认时区已更新为 ${nextValue}。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '更新平台默认时区失败。', 'error');
  }
}

async function handleUpdateMerchantTimeZone() {
  try {
    const nextValue = merchantOverrideTimeZoneInput.value.trim();
    if (!nextValue) {
      throw new Error('商家时区覆盖不能为空。');
    }
    await upsertMerchantConfigOverride({
      merchantId: resolveMerchantId(),
      configGroup: 'system',
      configKey: 'default_time_zone',
      configValue: nextValue,
    });
    await loadConfigCenter();
    setActionMessage(`商家时区覆盖已更新为 ${nextValue}。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '更新商家时区覆盖失败。', 'error');
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

.configActions {
  display: grid;
  justify-items: end;
  gap: 10px;
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

.formStack {
  display: grid;
  gap: 12px;
  margin-top: 18px;
}

.fieldBlock {
  display: grid;
  gap: 8px;
}

.fieldLabel {
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.textarea {
  min-height: 88px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  resize: vertical;
}

.input {
  min-height: 54px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  font: inherit;
}

.inlineActions {
  display: flex;
  gap: 12px;
}

.recordHead,
.recordRow,
.recordRowStatic {
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

.recordRow,
.recordRowStatic {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.04);
}

.recordRow {
  border: 0;
  text-align: left;
  cursor: pointer;
}

.recordRowActive {
  box-shadow: inset 0 0 0 1px rgba(255, 107, 0, 0.3);
  background: rgba(255, 247, 238, 0.96);
}

.strong {
  font-weight: 800;
}

.publishDetail {
  display: grid;
  gap: 18px;
  margin-top: 18px;
}

.detailGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.detailValue {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 700;
}

.snapshotGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

@media (max-width: 960px) {
  .grid {
    grid-template-columns: 1fr;
  }

  .detailGrid,
  .snapshotGrid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .panelHead {
    flex-direction: column;
    align-items: flex-start;
  }

  .inlineActions {
    width: 100%;
  }

  .recordHead {
    display: none;
  }

  .recordRow,
  .recordRowStatic {
    grid-template-columns: 1fr;
  }
}
</style>
