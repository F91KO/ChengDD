<template>
  <WorkspaceLayout
    eyebrow="Config"
    title="配置中心"
    description="统一查看商家功能开关、生效配置、报表健康度和发布记录，方便快速确认配置链路是否正常。"
  >
    <section :class="$style.toolbar">
      <div :class="$style.toolbarMain">
        <div :class="$style.eyebrow">配置工作台</div>
        <h3 :class="$style.toolbarTitle">生效开关、配置结果与发布记录统一查看</h3>
        <div :class="$style.toolbarMeta">
          <span>商家：{{ effectiveConfigSummary.merchantId }}</span>
          <span>配置来源：{{ effectiveConfigSummary.configSource }}</span>
          <span>商家时区：{{ effectiveConfigSummary.timeZone }}</span>
          <span>发布记录：{{ publishRecords.length }} 条</span>
        </div>
      </div>
      <div :class="$style.toolbarActions">
        <UiButton variant="secondary" size="sm" @click="loadConfigCenter">刷新配置</UiButton>
        <UiButton @click="handleCreatePublish">发起发布</UiButton>
      </div>
    </section>

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

    <section :class="$style.summaryGrid">
      <UiCard
        v-for="card in summaryCards"
        :key="card.label"
        elevated
        :class="$style.summaryCard"
      >
        <div :class="$style.summaryLabel">{{ card.label }}</div>
        <div :class="$style.summaryValue">{{ card.value }}</div>
        <div :class="[$style.summaryMetaText, $style[card.tone]]">{{ card.meta }}</div>
      </UiCard>
    </section>

    <section :class="$style.grid">
      <UiCard elevated :class="$style.mainPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">功能开关</div>
            <h3 :class="$style.title">当前商家生效中的能力</h3>
            <div :class="$style.panelDescription">直接查看开关状态、来源和作用域，避免进入详情页后再判断是否生效。</div>
          </div>
        </div>

        <div :class="$style.configList">
          <article v-for="item in configGroups" :key="item.switchCode" :class="$style.configItem">
            <div :class="$style.configMain">
              <div :class="$style.configName">{{ item.name }}</div>
              <div :class="$style.configDescription">{{ item.description }}</div>
            </div>
            <div :class="$style.configActions">
              <UiTag :tone="item.statusTone as 'default' | 'primary' | 'success'">
                {{ item.status }}
              </UiTag>
              <UiButton variant="secondary" size="sm" @click="handleToggleSwitch(item)">
                {{ item.effectiveValue === 'on' ? '关闭' : '开启' }}
              </UiButton>
            </div>
          </article>

          <UiStatePanel
            v-if="!configGroups.length"
            tone="empty"
            title="暂无开关数据"
            description="请确认 config-service 已完成商家开关初始化，或稍后刷新后重试。"
          />
        </div>
      </UiCard>

      <UiCard elevated :class="$style.sidePanel">
        <div :class="$style.eyebrow">生效配置</div>
        <h3 :class="$style.title">当前生效配置</h3>
        <div :class="$style.panelDescription">平台默认值、商家覆盖和报表健康度集中放在侧边，便于快速确认当前配置上下文。</div>

        <div :class="$style.sideFacts">
          <article :class="$style.factCard">
            <div :class="$style.fieldLabel">平台默认时区</div>
            <div :class="$style.factValue">{{ platformDefaultTimeZone }}</div>
          </article>
          <article :class="$style.factCard">
            <div :class="$style.fieldLabel">商家当前时区</div>
            <div :class="$style.factValue">{{ effectiveConfigSummary.timeZone }}</div>
          </article>
          <article :class="$style.factCard">
            <div :class="$style.fieldLabel">配置来源</div>
            <div :class="$style.factValue">{{ effectiveConfigSummary.configSource }}</div>
          </article>
          <article :class="$style.factCard">
            <div :class="$style.fieldLabel">报表状态</div>
            <div :class="$style.factValue">{{ reportHealthSummary }}</div>
          </article>
        </div>

        <div :class="$style.formStack">
          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">平台默认时区</span>
            <input
              v-model="platformTimeZoneInput"
              :class="$style.input"
              placeholder="例如 Asia/Shanghai"
            />
          </label>
          <div :class="$style.inlineActions">
            <UiButton variant="secondary" size="sm" @click="handleUpdatePlatformTimeZone">更新平台默认值</UiButton>
          </div>

          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">商家时区覆盖</span>
            <input
              v-model="merchantOverrideTimeZoneInput"
              :class="$style.input"
              placeholder="例如 Asia/Shanghai"
            />
          </label>
          <div :class="$style.inlineActions">
            <UiButton variant="secondary" size="sm" @click="handleUpdateMerchantTimeZone">保存商家覆盖</UiButton>
          </div>
        </div>

      </UiCard>
    </section>

    <section>
      <UiCard elevated :class="$style.recordPanel">
        <div :class="$style.sectionBlock">
          <div :class="$style.panelHead">
            <div>
              <div :class="$style.eyebrow">数据健康度</div>
              <h3 :class="$style.title">报表与看板数据状态</h3>
            </div>
          </div>

          <UiStatePanel
            :tone="reportHealth?.ready ? 'info' : 'error'"
            :title="reportHealth?.ready ? '报表健康度已就绪' : '报表健康度待补数'"
            :description="reportHealth?.summary || '正在检查报表侧数据准备情况。'"
          />

          <div v-if="reportHealth?.items?.length" :class="$style.recordTable">
            <div :class="$style.recordHead">
              <span>检查项</span>
              <span>状态</span>
              <span>最近数据时间</span>
              <span>说明</span>
            </div>
            <div v-for="item in reportHealth.items" :key="item.code" :class="$style.recordRowStatic">
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">检查项</div>
                <div :class="$style.strong">{{ item.name }}</div>
              </div>
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">状态</div>
                <div>{{ item.status === 'ready' ? '已就绪' : '缺失' }}</div>
              </div>
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">最近数据时间</div>
                <div>{{ item.latest_data_time || '-' }}</div>
              </div>
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">说明</div>
                <div>{{ item.message }}</div>
              </div>
            </div>
          </div>

          <UiStatePanel
            v-else
            tone="empty"
            title="暂无健康度明细"
            description="当前还没有报表健康检查项返回，可以稍后刷新后重试。"
          />
        </div>

        <div :class="$style.sectionBlock">
          <div :class="$style.panelHead">
            <div>
              <div :class="$style.eyebrow">发布记录</div>
              <h3 :class="$style.title">配置发布与回滚</h3>
            </div>
            <UiButton @click="handleCreatePublish">发起发布</UiButton>
          </div>

          <div :class="$style.publishFormGrid">
            <label :class="$style.fieldBlock">
              <span :class="$style.fieldLabel">发布说明</span>
              <textarea
                v-model="publishNote"
                :class="$style.textarea"
                placeholder="请输入本次发布说明"
              />
            </label>
            <label :class="$style.fieldBlock">
              <span :class="$style.fieldLabel">回滚原因</span>
              <textarea
                v-model="rollbackReason"
                :class="$style.textarea"
                placeholder="请输入需要回滚时的原因"
              />
            </label>
          </div>

          <div :class="$style.inlineActions">
            <UiButton variant="secondary" size="sm" :disabled="!publishRecords.length" @click="handleRollbackLatest">
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
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">任务号</div>
                <div :class="$style.strong">{{ record.task_no }}</div>
              </div>
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">状态</div>
                <div>{{ record.release_status }}</div>
              </div>
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">创建时间</div>
                <div>{{ record.created_at }}</div>
              </div>
              <div :class="$style.recordCell">
                <div :class="$style.recordCellLabel">说明</div>
                <div>{{ record.publish_note || '-' }}</div>
              </div>
            </button>
          </div>

          <UiStatePanel
            v-else
            tone="empty"
            title="暂无发布记录"
            description="当前商家还没有配置发布记录，可以先发起一次发布后再查看快照与步骤详情。"
          />

          <div ref="publishDetailAnchor">
            <UiStatePanel
              v-if="publishDetailLoading"
              tone="loading"
              title="正在加载发布详情"
              description="正在读取快照、执行步骤和回滚信息。"
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
                  <span>重试次数</span>
                  <span>结果</span>
                </div>
                <div
                  v-for="step in selectedPublishRecord.steps"
                  :key="step.step_code"
                  :class="$style.recordRowStatic"
                >
                  <div :class="$style.recordCell">
                    <div :class="$style.recordCellLabel">步骤</div>
                    <div :class="$style.strong">{{ step.step_name }}</div>
                  </div>
                  <div :class="$style.recordCell">
                    <div :class="$style.recordCellLabel">状态</div>
                    <div>{{ step.step_status }}</div>
                  </div>
                  <div :class="$style.recordCell">
                    <div :class="$style.recordCellLabel">重试次数</div>
                    <div>{{ step.retry_count ?? 0 }}</div>
                  </div>
                  <div :class="$style.recordCell">
                    <div :class="$style.recordCellLabel">结果</div>
                    <div>{{ step.result_message || step.error_code || '-' }}</div>
                  </div>
                </div>
              </div>

              <div v-if="selectedPublishRecord.snapshot" :class="$style.snapshotGrid">
                <UiStatePanel
                  tone="info"
                  title="配置快照"
                  :description="`平台配置 ${selectedPublishRecord.snapshot.platform_configs.length} 项，商家覆盖 ${selectedPublishRecord.snapshot.merchant_overrides.length} 项。`"
                />
                <UiStatePanel
                  tone="info"
                  title="开关快照"
                  :description="`平台开关 ${selectedPublishRecord.snapshot.platform_feature_switches.length} 项，商家开关 ${selectedPublishRecord.snapshot.merchant_feature_switches.length} 项。`"
                />
              </div>
            </div>
          </div>
        </div>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import {
  changeMerchantFeatureSwitch,
  createPublishRecord,
  fetchEffectiveConfig,
  fetchMerchantFeatureSwitches,
  fetchPublishRecord,
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
const configNotice = ref('正在加载配置数据。');
const configGroups = ref<ConfigGroupItem[]>([]);
const effectiveConfig = ref<ConfigKvValueResponseRaw | null>(null);
const platformConfig = ref<ConfigKvValueResponseRaw | null>(null);
const reportHealth = ref<ReportHealthResponseRaw | null>(null);
const publishRecords = ref<ConfigPublishRecordResponseRaw[]>([]);
const selectedPublishRecord = ref<ConfigPublishRecordResponseRaw | null>(null);
const publishDetailLoading = ref(false);
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const publishNote = ref('商家配置发布');
const rollbackReason = ref('配置回滚');
const platformTimeZoneInput = ref('Asia/Shanghai');
const merchantOverrideTimeZoneInput = ref('Asia/Shanghai');
const publishDetailAnchor = ref<HTMLElement | null>(null);

const reportHealthSummary = computed(() => {
  if (!reportHealth.value) {
    return '未检查';
  }
  return reportHealth.value.ready ? '已就绪' : '待补数';
});

const platformDefaultTimeZone = computed(() => platformConfig.value?.config_value || 'Asia/Shanghai');
const enabledSwitchCount = computed(() => configGroups.value.filter((item) => item.effectiveValue === 'on').length);
const reportHealthIssueCount = computed(() => {
  if (!reportHealth.value?.items?.length) {
    return 0;
  }
  return reportHealth.value.items.filter((item) => item.status !== 'ready').length;
});

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
    merchantId: effectiveConfig.value.merchant_id || authStore.context?.merchantId || '-',
  };
});

const summaryCards = computed(() => [
  {
    label: '已开启开关',
    value: String(enabledSwitchCount.value),
    meta: `共 ${configGroups.value.length} 项能力开关`,
    tone: 'success',
  },
  {
    label: '商家当前时区',
    value: effectiveConfigSummary.value.timeZone,
    meta: `来源 ${effectiveConfigSummary.value.configSource}`,
    tone: 'primary',
  },
  {
    label: '报表健康项',
    value: reportHealth.value?.ready ? '已就绪' : '待补数',
    meta: reportHealthIssueCount.value > 0 ? `异常 ${reportHealthIssueCount.value} 项` : '未发现异常',
    tone: reportHealthIssueCount.value > 0 ? 'danger' : 'success',
  },
  {
    label: '发布记录',
    value: String(publishRecords.value.length),
    meta: selectedPublishRecord.value ? `当前选中 ${selectedPublishRecord.value.task_no}` : '未选中记录',
    tone: 'default',
  },
]);

const configStatePanel = computed(() => {
  if (configMode.value !== 'error') {
    return null;
  }

  return {
    tone: 'error' as const,
    title: '配置数据加载失败',
    description: configNotice.value,
  };
});

function resolveMerchantId(): string {
  return authStore.context?.merchantId || `merchant_${authStore.merchantIdForQuery ?? 1001}`;
}

function toConfigGroups(items: FeatureSwitchValueResponseRaw[]): ConfigGroupItem[] {
  return items.map((item) => ({
    switchCode: item.switch_code,
    name: item.switch_name,
    description: `${item.switch_code} · 作用域 ${item.switch_scope} · 默认值 ${item.default_value} · 来源 ${item.source}`,
    status: item.effective_value === 'on' ? '已开启' : '已关闭',
    statusTone:
      item.effective_value === 'on'
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

function scrollPublishDetailIntoView() {
  void nextTick(() => {
    publishDetailAnchor.value?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  });
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
    configNotice.value = '配置数据已更新。';
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
        void handleSelectPublishRecord(matched.task_no, false);
      } else {
        selectedPublishRecord.value = null;
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
    setActionMessage(`已将开关“${item.name}”调整为${switchValue === 'on' ? '开启' : '关闭'}。`);
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
      publishNote: publishNote.value.trim() || '商家配置发布',
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
      rollbackReason: rollbackReason.value.trim() || '配置验证后回滚',
    });
    await loadConfigCenter();
    await handleSelectPublishRecord(latest.task_no);
    setActionMessage(`发布记录 ${latest.task_no} 已发起回滚。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '回滚发布记录失败。', 'error');
  }
}

async function handleSelectPublishRecord(taskNo: string, shouldScroll = true) {
  try {
    publishDetailLoading.value = true;
    selectedPublishRecord.value = await fetchPublishRecord(taskNo);
    if (shouldScroll) {
      scrollPublishDetailIntoView();
    }
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
      configDesc: '页面更新的平台默认时区',
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
.toolbar {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: center;
  padding: 18px;
  border-radius: 20px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  background: linear-gradient(180deg, #ffffff 0%, #f9fbff 100%);
}

.toolbarMain {
  display: grid;
  gap: 10px;
}

.toolbarTitle {
  margin: 0;
  font-size: 24px;
  font-weight: 800;
  letter-spacing: -0.04em;
}

.toolbarMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 14px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.toolbarActions {
  display: flex;
  gap: 10px;
}

.summaryGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summaryCard {
  display: grid;
  gap: 10px;
  padding: 18px;
}

.summaryLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.summaryValue {
  font-size: 24px;
  font-weight: 800;
  line-height: 1.3;
  word-break: break-word;
}

.summaryMetaText {
  color: var(--cdd-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.grid {
  display: grid;
  grid-template-columns: minmax(0, 1.45fr) minmax(320px, 0.9fr);
  gap: 18px;
}

.mainPanel,
.sidePanel,
.recordPanel {
  padding: 18px;
}

.sidePanel {
  position: sticky;
  top: 24px;
  align-self: start;
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

.panelDescription {
  margin-top: 10px;
  color: var(--cdd-text-soft);
  font-size: 13px;
  line-height: 1.7;
}

.configList,
.recordTable,
.formStack,
.publishDetail {
  display: grid;
  gap: 12px;
}

.configList,
.formStack {
  margin-top: 16px;
}

.configItem {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  padding: 14px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  border: 1px solid rgba(9, 29, 46, 0.05);
}

.configMain {
  min-width: 0;
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
  flex-shrink: 0;
}

.sideFacts {
  display: grid;
  gap: 12px;
  margin-top: 16px;
}

.factCard {
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.72);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.05);
}

.factValue {
  margin-top: 8px;
  font-size: 15px;
  font-weight: 800;
  line-height: 1.6;
  word-break: break-word;
}

.sideState {
  margin-top: 20px;
}

.sectionBlock + .sectionBlock {
  margin-top: 24px;
}

.publishFormGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
  margin-top: 14px;
}

.fieldBlock {
  display: grid;
  gap: 8px;
}

.fieldLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.textarea {
  min-height: 88px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  resize: vertical;
}

.input {
  min-height: 50px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  font: inherit;
}

.inlineActions {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.recordHead,
.recordRow,
.recordRowStatic {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
  align-items: start;
  padding: 11px 12px;
}

.recordHead {
  margin-top: 20px;
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

.recordCell {
  min-width: 0;
}

.recordCellLabel {
  display: none;
  margin-bottom: 6px;
  color: var(--cdd-text-faint);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.06em;
  text-transform: uppercase;
}

.strong {
  font-weight: 800;
}

.detailGrid,
.snapshotGrid {
  display: grid;
  gap: 16px;
}

.detailGrid {
  grid-template-columns: repeat(4, minmax(0, 1fr));
}

.snapshotGrid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.detailValue {
  margin-top: 8px;
  font-size: 14px;
  line-height: 1.7;
  font-weight: 700;
  word-break: break-word;
}

@media (max-width: 1080px) {
  .toolbar {
    flex-direction: column;
    align-items: stretch;
  }

  .summaryGrid,
  .grid,
  .publishFormGrid,
  .detailGrid,
  .snapshotGrid {
    grid-template-columns: 1fr;
  }

  .sidePanel {
    position: static;
  }
}

@media (max-width: 720px) {
  .toolbarMeta,
  .toolbarActions,
  .panelHead,
  .configItem,
  .configActions {
    flex-direction: column;
    align-items: flex-start;
  }

  .toolbarActions,
  .toolbarActions :global(button),
  .configActions,
  .inlineActions,
  .inlineActions :global(button) {
    width: 100%;
  }

  .toolbarActions :global(button),
  .configActions :global(button),
  .inlineActions :global(button) {
    width: 100%;
  }

  .recordHead {
    display: none;
  }

  .recordRow,
  .recordRowStatic {
    grid-template-columns: 1fr;
  }

  .recordCellLabel {
    display: block;
  }
}
</style>
