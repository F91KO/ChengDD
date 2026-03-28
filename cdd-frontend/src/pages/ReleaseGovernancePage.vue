<template>
  <WorkspaceLayout
    eyebrow="Release"
    title="发布治理"
    description="在一个页面内完成发布任务创建、任务查询和回滚处理。"
  >
    <UiStatePanel
      v-if="pageStatePanel"
      :tone="pageStatePanel.tone"
      :title="pageStatePanel.title"
      :description="pageStatePanel.description"
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.grid">
      <UiCard elevated :class="$style.formPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">创建任务</div>
            <h3 :class="$style.title">发布任务骨架</h3>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">基础参数</div>
          <div :class="$style.formGrid">
            <UiInput v-model="createForm.miniProgramId" label="小程序 ID" placeholder="例如 1001" />
            <UiInput
              v-model="createForm.templateVersionId"
              label="模板版本 ID"
              placeholder="请输入实际模板版本 ID"
            />

            <label :class="$style.selectField">
              <span :class="$style.fieldLabel">发布类型</span>
              <div :class="$style.selectWrap">
                <select v-model="createForm.releaseType" :class="$style.select">
                  <option
                    v-for="option in releaseTypeOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </label>

            <label :class="$style.selectField">
              <span :class="$style.fieldLabel">触发来源</span>
              <div :class="$style.selectWrap">
                <select v-model="createForm.triggerSource" :class="$style.select">
                  <option
                    v-for="option in triggerSourceOptions"
                    :key="option.value"
                    :value="option.value"
                  >
                    {{ option.label }}
                  </option>
                </select>
              </div>
            </label>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">发布快照</div>
          <UiStatePanel
            tone="info"
            title="创建前确认模板版本"
            description="请确认模板版本和发布快照内容后再创建任务。"
          />
          <label :class="$style.fieldWide">
            <span :class="$style.fieldLabel">发布快照</span>
            <textarea
              v-model="createForm.releaseSnapshotJson"
              :class="$style.textarea"
              placeholder="请输入发布快照 JSON"
            />
          </label>
        </div>

        <div :class="$style.actions">
          <UiButton variant="secondary" size="sm" @click="resetCreateForm">重置表单</UiButton>
          <UiButton :disabled="submitting === 'create' || !canCreateTask" @click="handleCreateTask">
            {{ submitting === 'create' ? '正在创建...' : '创建发布任务' }}
          </UiButton>
        </div>
      </UiCard>

      <UiCard elevated :class="$style.lookupPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">任务查询</div>
            <h3 :class="$style.title">按任务号查看详情</h3>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">任务定位</div>
          <div :class="$style.formGrid">
            <UiInput v-model="lookupTaskNo" label="任务号" placeholder="例如 rls_20260325_xxx" />
          </div>
        </div>

        <div :class="$style.actionBlock">
          <div :class="$style.actionGroupTitle">任务查询</div>
          <div :class="$style.actions">
            <UiButton variant="secondary" size="sm" :disabled="submitting === 'query' || !canQueryTask" @click="handleFetchTask">
              {{ submitting === 'query' ? '正在查询...' : '查询任务详情' }}
            </UiButton>
          </div>
          <UiStatePanel
            tone="info"
            title="查询后可查看任务进度"
            description="任务状态更新后，可在下方查看摘要和执行步骤。"
          />
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">回滚参数</div>
          <div :class="$style.formGrid">
            <UiInput
              v-model="rollbackTargetVersion"
              label="回滚目标版本"
              placeholder="例如 0.9.0"
            />
            <label :class="$style.fieldWide">
              <span :class="$style.fieldLabel">回滚原因</span>
              <textarea
                v-model="rollbackReason"
                :class="$style.textarea"
                placeholder="请输入回滚原因"
              />
            </label>
          </div>
          <div :class="$style.actions">
            <UiButton
              variant="secondary"
              size="sm"
              :disabled="submitting === 'rollback' || !canRollbackTask"
              @click="handleRollbackTask"
            >
              {{ submitting === 'rollback' ? '正在回滚...' : '发起回滚任务' }}
            </UiButton>
          </div>
        </div>

        <div :class="$style.actions">
          <UiButton
            variant="secondary"
            size="sm"
            :disabled="!releaseTask"
            @click="releaseTask && (lookupTaskNo = releaseTask.task_no)"
          >
            使用当前任务号
          </UiButton>
        </div>
      </UiCard>
    </section>

    <UiStatePanel
      v-if="!releaseTask && !loading"
      tone="empty"
      title="当前还没有选中发布任务"
      description="可以先创建任务，或者输入任务号查询历史发布记录。"
    >
      <UiButton variant="secondary" size="sm" @click="resetCreateForm">恢复表单内容</UiButton>
    </UiStatePanel>

    <UiCard v-if="releaseTask" elevated :class="$style.detailPanel">
      <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">任务详情</div>
            <h3 :class="$style.title">{{ releaseTask.task_no }}</h3>
          </div>
          <div :class="$style.statusGroup">
            <UiTag :tone="releaseStatusTone(releaseTask.release_status)">
              {{ formatReleaseStatus(releaseTask.release_status) }}
            </UiTag>
            <UiTag :tone="resultSyncTone(releaseTask.result_sync_status)">
              {{ formatResultSyncStatus(releaseTask.result_sync_status) }}
            </UiTag>
          </div>
        </div>

      <div :class="$style.detailTabs">
        <button
          v-for="tab in detailTabs"
          :key="tab.value"
          type="button"
          :class="[$style.detailTab, detailTab === tab.value ? $style.detailTabActive : '']"
          @click="detailTab = tab.value"
        >
          {{ tab.label }}
        </button>
      </div>

      <template v-if="detailTab === 'overview'">
        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">任务摘要</div>
          <div :class="$style.detailGrid">
            <div>
              <div :class="$style.fieldLabel">发布状态</div>
              <div :class="$style.detailValue">{{ formatReleaseStatus(releaseTask.release_status) }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">当前步骤</div>
              <div :class="$style.detailValue">{{ formatStepCode(releaseTask.current_step_code) }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">结果回写</div>
              <div :class="$style.detailValue">{{ formatResultSyncStatus(releaseTask.result_sync_status) }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">回滚任务</div>
              <div :class="$style.detailValue">{{ releaseTask.rollback_task_no || '-' }}</div>
            </div>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">上下文信息</div>
          <div :class="$style.detailGrid">
            <div>
              <div :class="$style.fieldLabel">商家 / 门店</div>
              <div :class="$style.detailValue">
                {{ releaseTask.merchant_id }} / {{ releaseTask.store_id }}
              </div>
            </div>
            <div>
              <div :class="$style.fieldLabel">小程序 / 模板</div>
              <div :class="$style.detailValue">
                {{ releaseTask.mini_program_id }} / {{ releaseTask.template_version_id }}
              </div>
            </div>
            <div>
              <div :class="$style.fieldLabel">触发来源</div>
              <div :class="$style.detailValue">{{ formatTriggerSource(releaseTask.trigger_source) }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">发布类型</div>
              <div :class="$style.detailValue">{{ formatReleaseType(releaseTask.release_type) }}</div>
            </div>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">执行时间与异常</div>
          <div :class="$style.detailGrid">
            <div>
              <div :class="$style.fieldLabel">开始时间</div>
              <div :class="$style.detailValue">{{ releaseTask.started_at || '-' }}</div>
            </div>
            <div>
              <div :class="$style.fieldLabel">完成时间</div>
              <div :class="$style.detailValue">{{ releaseTask.finished_at || '-' }}</div>
            </div>
            <div :class="$style.detailSpanTwo">
              <div :class="$style.fieldLabel">错误信息</div>
              <div :class="$style.detailValue">
                {{ releaseTask.last_error_message || releaseTask.last_error_code || '-' }}
              </div>
            </div>
          </div>
        </div>
      </template>

      <div v-else>
        <div v-if="releaseTask.steps.length" :class="$style.stepTable">
          <div :class="$style.stepHead">
            <span>步骤</span>
            <span>状态</span>
            <span>重试</span>
            <span>结果</span>
          </div>
          <div v-for="step in releaseTask.steps" :key="step.step_code" :class="$style.stepRow">
            <div :class="$style.stepCell">
              <div :class="$style.stepCellLabel">步骤</div>
              <div :class="$style.strong">{{ step.step_name || formatStepCode(step.step_code) }}</div>
            </div>
            <div :class="$style.stepCell">
              <div :class="$style.stepCellLabel">状态</div>
              <UiTag :tone="stepStatusTone(step.step_status)">
                {{ formatStepStatus(step.step_status) }}
              </UiTag>
            </div>
            <div :class="$style.stepCell">
              <div :class="$style.stepCellLabel">重试</div>
              <div>{{ step.retry_count }}</div>
            </div>
            <div :class="$style.stepCell">
              <div :class="$style.stepCellLabel">结果</div>
              <div>{{ step.result_message || step.error_code || '-' }}</div>
            </div>
          </div>
        </div>
        <UiStatePanel
          v-else
          tone="empty"
          title="暂无步骤记录"
          description="创建任务后推进执行，步骤记录会在这里展示。"
        />
      </div>
    </UiCard>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import {
  createReleaseTask,
  fetchReleaseTask,
  rollbackReleaseTask,
} from '@/services/release';
import { useAuthStore } from '@/stores/auth';
import type { ReleaseTaskResponseRaw } from '@/types/release';

const releaseTypeOptions = [
  { value: 'full_release', label: '全量发布' },
  { value: 'gray_release', label: '灰度发布' },
  { value: 'rollback_release', label: '回滚发布' },
];

const triggerSourceOptions = [
  { value: 'merchant_console', label: '商家后台' },
  { value: 'dashboard_manual', label: '工作台触发' },
  { value: 'system', label: '系统触发' },
  { value: 'api', label: '接口触发' },
];

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const submitting = ref<'create' | 'query' | 'rollback' | ''>('');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const releaseTask = ref<ReleaseTaskResponseRaw | null>(null);
const detailTab = ref<'overview' | 'steps'>('overview');
const lookupTaskNo = ref('');
const rollbackTargetVersion = ref('0.9.0');
const rollbackReason = ref('版本验证后回滚');
const detailTabs = [
  { value: 'overview' as const, label: '任务概览' },
  { value: 'steps' as const, label: '执行步骤' },
];

const createForm = reactive({
  miniProgramId: '',
  templateVersionId: '',
  releaseType: 'full_release',
  triggerSource: 'merchant_console',
  releaseSnapshotJson: '',
});
const canCreateTask = computed(() => (
  Boolean(authStore.merchantIdForQuery && authStore.storeIdForQuery)
  && createForm.miniProgramId.trim().length > 0
  && createForm.templateVersionId.trim().length > 0
  && createForm.releaseSnapshotJson.trim().length > 0
));
const canQueryTask = computed(() => lookupTaskNo.value.trim().length > 0);
const canRollbackTask = computed(() => (
  Boolean(releaseTask.value)
  && rollbackTargetVersion.value.trim().length > 0
  && rollbackReason.value.trim().length > 0
));

const pageStatePanel = computed(() => {
  if (loading.value) {
    return {
      tone: 'loading' as const,
      title: '正在准备发布上下文',
      description: '正在读取当前商家、小程序和模板发布基础数据。',
    };
  }

  if (!authStore.merchantIdForQuery || !authStore.storeIdForQuery) {
    return {
      tone: 'error' as const,
      title: '缺少发布上下文',
      description: '当前登录身份缺少商家或门店范围，无法创建发布任务。',
    };
  }

  return null;
});

function setActionMessage(message: string, tone: 'info' | 'error' = 'info') {
  actionMessage.value = message;
  actionTone.value = tone;
}

function parseNumericTail(raw: string | null | undefined): number | null {
  if (!raw) {
    return null;
  }
  const matched = raw.match(/(\d+)$/);
  if (!matched) {
    return null;
  }
  return Number(matched[1]);
}

function formatReleaseType(value: string | null | undefined): string {
  return releaseTypeOptions.find((item) => item.value === value)?.label || value || '-';
}

function formatTriggerSource(value: string | null | undefined): string {
  return triggerSourceOptions.find((item) => item.value === value)?.label || value || '-';
}

function formatReleaseStatus(value: string | null | undefined): string {
  const normalized = (value || '').toLowerCase();
  if (!normalized) {
    return '-';
  }
  if (normalized.includes('pending') || normalized.includes('created')) {
    return '待执行';
  }
  if (normalized.includes('running')) {
    return '执行中';
  }
  if (normalized.includes('success') || normalized.includes('done') || normalized.includes('finished')) {
    return '执行成功';
  }
  if (normalized.includes('fail') || normalized.includes('error')) {
    return '执行失败';
  }
  if (normalized.includes('rollback')) {
    return '已回滚';
  }
  return value || '-';
}

function releaseStatusTone(value: string | null | undefined): 'default' | 'primary' | 'info' | 'success' | 'danger' {
  const normalized = (value || '').toLowerCase();
  if (normalized.includes('success') || normalized.includes('done') || normalized.includes('finished')) {
    return 'success';
  }
  if (normalized.includes('running')) {
    return 'primary';
  }
  if (normalized.includes('fail') || normalized.includes('error')) {
    return 'danger';
  }
  return 'default';
}

function formatResultSyncStatus(value: string | null | undefined): string {
  const normalized = (value || '').toLowerCase();
  if (!normalized) {
    return '-';
  }
  if (normalized === 'active') {
    return '已生效';
  }
  if (normalized.includes('pending') || normalized.includes('wait')) {
    return '待回写';
  }
  if (normalized.includes('success') || normalized.includes('done')) {
    return '回写成功';
  }
  if (normalized.includes('fail') || normalized.includes('error')) {
    return '回写失败';
  }
  return value || '-';
}

function resultSyncTone(value: string | null | undefined): 'default' | 'primary' | 'info' | 'success' | 'danger' {
  const normalized = (value || '').toLowerCase();
  if (normalized === 'active' || normalized.includes('success') || normalized.includes('done')) {
    return 'success';
  }
  if (normalized.includes('pending') || normalized.includes('wait')) {
    return 'primary';
  }
  if (normalized.includes('fail') || normalized.includes('error')) {
    return 'danger';
  }
  return 'default';
}

function formatStepStatus(value: string | null | undefined): string {
  const normalized = (value || '').toLowerCase();
  if (!normalized) {
    return '-';
  }
  if (normalized.includes('pending') || normalized.includes('wait')) {
    return '待执行';
  }
  if (normalized.includes('running')) {
    return '执行中';
  }
  if (normalized.includes('success') || normalized.includes('done')) {
    return '成功';
  }
  if (normalized.includes('fail') || normalized.includes('error')) {
    return '失败';
  }
  return value || '-';
}

function stepStatusTone(value: string | null | undefined): 'default' | 'primary' | 'info' | 'success' | 'danger' {
  const normalized = (value || '').toLowerCase();
  if (normalized.includes('success') || normalized.includes('done')) {
    return 'success';
  }
  if (normalized.includes('running')) {
    return 'primary';
  }
  if (normalized.includes('fail') || normalized.includes('error')) {
    return 'danger';
  }
  return 'default';
}

function formatStepCode(value: string | null | undefined): string {
  const map: Record<string, string> = {
    validate_env: '环境校验',
    release_done: '发布完成',
    sync_result: '结果回写',
    create_snapshot: '生成快照',
    publish_template: '发布模板',
  };
  return (value && map[value]) || value || '-';
}

function resetCreateForm() {
  const fallbackMiniProgramId =
    parseNumericTail(authStore.context?.miniProgramId) ?? authStore.storeIdForQuery ?? 1001;
  createForm.miniProgramId = String(fallbackMiniProgramId);
  createForm.templateVersionId = '';
  createForm.releaseType = 'full_release';
  createForm.triggerSource = 'merchant_console';
  createForm.releaseSnapshotJson = JSON.stringify(
    {
      source: 'release_governance_page',
      merchant_id: authStore.merchantIdForQuery ?? 1001,
      store_id: authStore.storeIdForQuery ?? 1001,
      operator_name: authStore.user.operatorName,
    },
    null,
    2,
  );
}

async function handleFetchTask() {
  try {
    const taskNo = lookupTaskNo.value.trim();
    if (!taskNo) {
      throw new Error('请输入要查询的任务号。');
    }
    submitting.value = 'query';
    releaseTask.value = await fetchReleaseTask(taskNo);
    detailTab.value = 'overview';
    await router.replace({
      path: '/releases',
      query: {
        task_no: taskNo,
      },
    });
    setActionMessage(`已加载任务 ${taskNo} 的详情。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '查询发布任务失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function handleCreateTask() {
  try {
    const merchantId = authStore.merchantIdForQuery;
    const storeId = authStore.storeIdForQuery;
    const miniProgramId = Number(createForm.miniProgramId);
    const templateVersionId = Number(createForm.templateVersionId);

    if (!merchantId || !storeId) {
      throw new Error('当前缺少商家或门店上下文，无法创建发布任务。');
    }
    if (!Number.isFinite(miniProgramId) || miniProgramId <= 0) {
      throw new Error('请输入有效的小程序 ID。');
    }
    if (!Number.isFinite(templateVersionId) || templateVersionId <= 0) {
      throw new Error('请输入有效的模板版本 ID。');
    }

    submitting.value = 'create';
    const task = await createReleaseTask({
      merchantId,
      storeId,
      miniProgramId,
      templateVersionId,
      releaseType: createForm.releaseType.trim() || 'full_release',
      triggerSource: createForm.triggerSource.trim() || 'merchant_console',
      releaseSnapshotJson: createForm.releaseSnapshotJson.trim(),
    });
    releaseTask.value = task;
    detailTab.value = 'overview';
    lookupTaskNo.value = task.task_no;
    await router.replace({
      path: '/releases',
      query: {
        task_no: task.task_no,
      },
    });
    setActionMessage(`已创建发布任务 ${task.task_no}。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '创建发布任务失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function handleRollbackTask() {
  try {
    if (!releaseTask.value) {
      throw new Error('请先加载要回滚的发布任务。');
    }
    if (!rollbackTargetVersion.value.trim()) {
      throw new Error('请输入回滚目标版本。');
    }
    if (!rollbackReason.value.trim()) {
      throw new Error('请输入回滚原因。');
    }

    submitting.value = 'rollback';
    releaseTask.value = await rollbackReleaseTask({
      taskNo: releaseTask.value.task_no,
      rollbackTargetVersion: rollbackTargetVersion.value.trim(),
      rollbackReason: rollbackReason.value.trim(),
    });
    lookupTaskNo.value = releaseTask.value.task_no;
    detailTab.value = 'overview';
    setActionMessage(`已为任务 ${releaseTask.value.task_no} 创建回滚任务。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '发起回滚失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function initializePage() {
  loading.value = true;
  try {
    await authStore.ensureCurrentContext();
    resetCreateForm();
    const miniProgramId = typeof route.query.mini_program_id === 'string' ? route.query.mini_program_id : '';
    const triggerSource = typeof route.query.trigger_source === 'string' ? route.query.trigger_source : '';
    if (miniProgramId) {
      createForm.miniProgramId = miniProgramId;
    }
    if (triggerSourceOptions.some((item) => item.value === triggerSource)) {
      createForm.triggerSource = triggerSource;
    }
    const taskNo = typeof route.query.task_no === 'string' ? route.query.task_no : '';
    if (taskNo) {
      lookupTaskNo.value = taskNo;
      releaseTask.value = await fetchReleaseTask(taskNo);
    }
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '初始化发布治理页面失败。', 'error');
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  void initializePage();
});
</script>

<style module>
.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18px;
}

.formPanel,
.lookupPanel,
.detailPanel {
  padding: 24px;
}

.formPanel,
.lookupPanel,
.detailPanel {
  display: grid;
  gap: 16px;
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

.formSection {
  display: grid;
  gap: 12px;
  padding: 12px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.52);
}

.sectionTitle,
.actionGroupTitle {
  font-size: 15px;
  font-weight: 800;
  color: var(--cdd-text);
}

.formGrid {
  display: grid;
  gap: 12px;
}

.fieldWide,
.selectField {
  display: grid;
  gap: 8px;
}

.fieldLabel {
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.selectWrap {
  display: flex;
  align-items: center;
  min-height: 50px;
  padding: 0 14px;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.95);
  box-shadow: inset 0 0 0 1px transparent;
  transition:
    box-shadow 0.18s ease,
    background 0.18s ease;
}

.selectWrap:focus-within {
  background: rgba(255, 255, 255, 0.98);
  box-shadow: inset 0 0 0 1px rgba(160, 65, 0, 0.24);
}

.select {
  width: 100%;
  border: 0;
  outline: 0;
  background: transparent;
  color: var(--cdd-text);
  font: inherit;
}

.textarea {
  min-height: 96px;
  width: 100%;
  padding: 12px 14px;
  border: 0;
  border-radius: 16px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  resize: vertical;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.actionBlock {
  display: grid;
  gap: 12px;
}

.statusGroup {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  justify-content: flex-end;
}

.detailTabs {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.detailTab {
  min-height: 38px;
  padding: 0 14px;
  border: 1px solid rgba(9, 29, 46, 0.08);
  border-radius: 14px;
  background: rgba(248, 250, 253, 0.96);
  color: var(--cdd-text-soft);
  font: inherit;
  font-size: 13px;
  font-weight: 700;
  cursor: pointer;
}

.detailTabActive {
  border-color: rgba(255, 107, 0, 0.22);
  background: linear-gradient(135deg, rgba(255, 107, 0, 0.1), rgba(255, 249, 244, 0.98));
  color: #9c4304;
  box-shadow: inset 0 0 0 1px rgba(255, 107, 0, 0.08);
}

.detailPanel {
  margin-top: 18px;
  display: grid;
  gap: 18px;
}

.detailGrid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 16px;
}

.detailSpanTwo {
  grid-column: span 2;
}

.detailValue {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.7;
  word-break: break-word;
}

.stepTable {
  display: grid;
  gap: 10px;
}

.stepHead,
.stepRow {
  display: grid;
  grid-template-columns: 1fr 0.7fr 0.5fr 1.2fr;
  gap: 16px;
  align-items: start;
  padding: 14px 16px;
}

.stepHead {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  background: rgba(237, 244, 255, 0.72);
  border-radius: 16px;
}

.stepRow {
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.86);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.04);
}

.stepCell {
  min-width: 0;
}

.stepCellLabel {
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

@media (max-width: 960px) {
  .grid,
  .detailGrid {
    grid-template-columns: 1fr;
  }

  .detailSpanTwo {
    grid-column: auto;
  }

  .stepHead,
  .stepRow {
    grid-template-columns: 1fr;
  }

  .stepCellLabel {
    display: block;
  }

  .stepHead {
    display: none;
  }
}

@media (max-width: 640px) {
  .panelHead,
  .actions,
  .statusGroup {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions,
  .statusGroup,
  .actions :global(button) {
    width: 100%;
  }

  .detailTabs {
    flex-direction: column;
  }

  .actions :global(button) {
    width: 100%;
  }
}
</style>
