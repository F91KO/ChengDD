<template>
  <WorkspaceLayout
    eyebrow="Release"
    title="发布治理"
    description="当前页面接入真实发布任务创建、任务查询和回滚骨架，便于联调模板发布链路。"
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

        <div :class="$style.formGrid">
          <UiInput v-model="createForm.miniProgramId" label="小程序 ID" placeholder="例如：1001" />
          <UiInput
            v-model="createForm.templateVersionId"
            label="模板版本 ID"
            placeholder="例如：9800001"
          />
          <UiInput v-model="createForm.releaseType" label="发布类型" placeholder="例如：full_release" />
          <UiInput
            v-model="createForm.triggerSource"
            label="触发来源"
            placeholder="例如：merchant_console"
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
          <UiButton variant="secondary" @click="resetCreateForm">重置表单</UiButton>
          <UiButton :disabled="submitting === 'create'" @click="handleCreateTask">
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

        <div :class="$style.formGrid">
          <UiInput v-model="lookupTaskNo" label="任务号" placeholder="例如：rls_..." />
          <UiInput
            v-model="rollbackTargetVersion"
            label="回滚目标版本"
            placeholder="例如：0.9.0"
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
          <UiButton variant="secondary" :disabled="submitting === 'query'" @click="handleFetchTask">
            {{ submitting === 'query' ? '正在查询...' : '查询任务详情' }}
          </UiButton>
          <UiButton
            variant="secondary"
            :disabled="!releaseTask || submitting === 'run'"
            @click="handleMarkRunning"
          >
            {{ submitting === 'run' ? '正在推进...' : '标记运行中' }}
          </UiButton>
          <UiButton
            variant="secondary"
            :disabled="!releaseTask || submitting === 'success'"
            @click="handleMarkSuccess"
          >
            {{ submitting === 'success' ? '正在推进...' : '标记执行成功' }}
          </UiButton>
          <UiButton
            variant="secondary"
            :disabled="!releaseTask || submitting === 'sync'"
            @click="handleSyncResult"
          >
            {{ submitting === 'sync' ? '正在回写...' : '回写 active' }}
          </UiButton>
          <UiButton
            variant="secondary"
            :disabled="!releaseTask || submitting === 'rollback'"
            @click="handleRollbackTask"
          >
            {{ submitting === 'rollback' ? '正在回滚...' : '发起回滚任务' }}
          </UiButton>
        </div>
      </UiCard>
    </section>

    <UiCard v-if="releaseTask" elevated :class="$style.detailPanel">
      <div :class="$style.panelHead">
        <div>
          <div :class="$style.eyebrow">任务详情</div>
          <h3 :class="$style.title">{{ releaseTask.task_no }}</h3>
        </div>
      </div>

      <div :class="$style.detailGrid">
        <div>
          <div :class="$style.fieldLabel">发布状态</div>
          <div :class="$style.detailValue">{{ releaseTask.release_status }}</div>
        </div>
        <div>
          <div :class="$style.fieldLabel">当前步骤</div>
          <div :class="$style.detailValue">{{ releaseTask.current_step_code || '-' }}</div>
        </div>
        <div>
          <div :class="$style.fieldLabel">结果回写</div>
          <div :class="$style.detailValue">{{ releaseTask.result_sync_status }}</div>
        </div>
        <div>
          <div :class="$style.fieldLabel">回滚任务</div>
          <div :class="$style.detailValue">{{ releaseTask.rollback_task_no || '-' }}</div>
        </div>
      </div>

      <div :class="$style.detailGrid">
        <div>
          <div :class="$style.fieldLabel">商家 / 店铺</div>
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
          <div :class="$style.detailValue">{{ releaseTask.trigger_source }}</div>
        </div>
        <div>
          <div :class="$style.fieldLabel">错误信息</div>
          <div :class="$style.detailValue">
            {{ releaseTask.last_error_message || releaseTask.last_error_code || '-' }}
          </div>
        </div>
      </div>

      <div v-if="releaseTask.steps.length" :class="$style.stepTable">
        <div :class="$style.stepHead">
          <span>步骤</span>
          <span>状态</span>
          <span>重试</span>
          <span>结果</span>
        </div>
        <div v-for="step in releaseTask.steps" :key="step.step_code" :class="$style.stepRow">
          <div :class="$style.strong">{{ step.step_name }}</div>
          <div>{{ step.step_status }}</div>
          <div>{{ step.retry_count }}</div>
          <div>{{ step.result_message || step.error_code || '-' }}</div>
        </div>
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
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import {
  createReleaseTask,
  fetchReleaseTask,
  rollbackReleaseTask,
  syncReleaseTaskResult,
  updateReleaseTaskStatus,
  updateReleaseTaskStep,
} from '@/services/release';
import { useAuthStore } from '@/stores/auth';
import type { ReleaseTaskResponseRaw } from '@/types/release';

const DEFAULT_TEMPLATE_VERSION_ID = '9800001';

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const loading = ref(false);
const submitting = ref<'create' | 'query' | 'run' | 'success' | 'sync' | 'rollback' | ''>('');
const actionMessage = ref('');
const actionTone = ref<'info' | 'error'>('info');
const releaseTask = ref<ReleaseTaskResponseRaw | null>(null);
const lookupTaskNo = ref('');
const rollbackTargetVersion = ref('0.9.0');
const rollbackReason = ref('联调验证后回滚');

const createForm = reactive({
  miniProgramId: '',
  templateVersionId: DEFAULT_TEMPLATE_VERSION_ID,
  releaseType: 'full_release',
  triggerSource: 'merchant_console',
  releaseSnapshotJson: '',
});

const pageStatePanel = computed(() => {
  if (loading.value) {
    return {
      tone: 'loading' as const,
      title: '正在准备发布上下文',
      description: '正在读取当前商家、小程序和模板发布基线。',
    };
  }
  if (!authStore.merchantIdForQuery || !authStore.storeIdForQuery) {
    return {
      tone: 'error' as const,
      title: '缺少发布上下文',
      description: '当前登录身份缺少商家或店铺范围，无法创建发布任务。',
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

function resetCreateForm() {
  const fallbackMiniProgramId =
    parseNumericTail(authStore.context?.miniProgramId) ?? authStore.storeIdForQuery ?? 1001;
  createForm.miniProgramId = String(fallbackMiniProgramId);
  createForm.templateVersionId = DEFAULT_TEMPLATE_VERSION_ID;
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
      throw new Error('当前缺少商家或店铺上下文，无法创建发布任务。');
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
    setActionMessage(`已为任务 ${releaseTask.value.task_no} 创建回滚骨架。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '发起回滚失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function reloadCurrentTask() {
  if (!releaseTask.value) {
    return;
  }
  releaseTask.value = await fetchReleaseTask(releaseTask.value.task_no);
}

async function handleMarkRunning() {
  try {
    if (!releaseTask.value) {
      throw new Error('请先查询或创建发布任务。');
    }
    submitting.value = 'run';
    await updateReleaseTaskStep({
      taskNo: releaseTask.value.task_no,
      stepCode: 'validate_env',
      stepName: '环境校验',
      stepOrder: 2,
      stepStatus: 'running',
      resultMessage: '前端联调将任务推进到运行中',
    });
    releaseTask.value = await updateReleaseTaskStatus({
      taskNo: releaseTask.value.task_no,
      targetStatus: 'running',
      currentStepCode: 'validate_env',
    });
    setActionMessage(`任务 ${releaseTask.value.task_no} 已推进到 running。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '推进任务运行失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function handleMarkSuccess() {
  try {
    if (!releaseTask.value) {
      throw new Error('请先查询或创建发布任务。');
    }
    submitting.value = 'success';
    await updateReleaseTaskStep({
      taskNo: releaseTask.value.task_no,
      stepCode: 'validate_env',
      stepName: '环境校验',
      stepOrder: 2,
      stepStatus: 'success',
      resultMessage: '前端联调标记环境校验通过',
    });
    await updateReleaseTaskStep({
      taskNo: releaseTask.value.task_no,
      stepCode: 'release_done',
      stepName: '发布完成',
      stepOrder: 3,
      stepStatus: 'success',
      resultMessage: '前端联调标记发布完成',
    });
    releaseTask.value = await updateReleaseTaskStatus({
      taskNo: releaseTask.value.task_no,
      targetStatus: 'success',
      currentStepCode: 'release_done',
    });
    setActionMessage(`任务 ${releaseTask.value.task_no} 已标记为 success。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '标记任务成功失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function handleSyncResult() {
  try {
    if (!releaseTask.value) {
      throw new Error('请先查询或创建发布任务。');
    }
    submitting.value = 'sync';
    releaseTask.value = await syncReleaseTaskResult({
      taskNo: releaseTask.value.task_no,
      mappingStatus: 'active',
    });
    await reloadCurrentTask();
    setActionMessage(`任务 ${releaseTask.value.task_no} 已完成 active 回写。`);
  } catch (error) {
    setActionMessage(error instanceof Error ? error.message : '回写发布结果失败。', 'error');
  } finally {
    submitting.value = '';
  }
}

async function initializePage() {
  loading.value = true;
  try {
    await authStore.ensureCurrentContext();
    resetCreateForm();
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

.formGrid {
  display: grid;
  gap: 14px;
  margin-top: 18px;
}

.fieldWide {
  display: grid;
  gap: 8px;
}

.fieldLabel {
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.textarea {
  min-height: 108px;
  width: 100%;
  padding: 14px 16px;
  border: 0;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.92);
  color: var(--cdd-text);
  resize: vertical;
}

.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 18px;
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

.detailValue {
  margin-top: 8px;
  font-size: 14px;
  font-weight: 700;
  line-height: 1.7;
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
  align-items: center;
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

.strong {
  font-weight: 800;
}

@media (max-width: 960px) {
  .grid,
  .detailGrid {
    grid-template-columns: 1fr;
  }

  .stepHead,
  .stepRow {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .panelHead,
  .actions {
    flex-direction: column;
    align-items: flex-start;
  }

  .actions :global(button) {
    width: 100%;
  }
}
</style>
