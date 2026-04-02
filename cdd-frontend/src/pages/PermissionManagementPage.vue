<template>
  <WorkspaceLayout
    eyebrow="Permission"
    title="权限配置"
    description="管理商家子账号、模块权限、动作权限和数据范围。创建后会自动重置一次登录信息，方便直接把临时密码交给对应运营人员。"
  >
    <UiStatePanel
      v-if="pageState"
      :tone="pageState.tone"
      :title="pageState.title"
      :description="pageState.description"
    />

    <UiStatePanel
      v-if="actionMessage"
      :tone="actionTone"
      title="操作结果"
      :description="actionMessage"
    />

    <section :class="$style.hero">
      <div>
        <div :class="$style.eyebrow">账号与权限中心</div>
        <h3 :class="$style.heroTitle">子账号、权限范围和登录信息在一个页面收口</h3>
        <div :class="$style.heroMeta">
          <span>子账号 {{ accountPage.total }} 个</span>
          <span>当前页 {{ accountPage.page }}</span>
          <span>每页 {{ accountPage.page_size }}</span>
        </div>
      </div>
      <div :class="$style.heroActions">
        <UiButton variant="secondary" size="sm" @click="loadAccounts">刷新列表</UiButton>
        <UiButton variant="secondary" size="sm" @click="resetForm">新建账号</UiButton>
      </div>
    </section>

    <section :class="$style.grid">
      <UiCard elevated :class="$style.listPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">子账号列表</div>
            <h3 :class="$style.title">当前商家子账号</h3>
          </div>
        </div>

        <div v-if="accountPage.list.length" :class="$style.accountList">
          <article
            v-for="account in accountPage.list"
            :key="account.account_id"
            :class="[$style.accountItem, selectedAccountId === account.account_id ? $style.accountItemActive : '']"
          >
            <div :class="$style.accountMain">
              <div :class="$style.accountTitleRow">
                <div>
                  <div :class="$style.accountName">{{ account.display_name }}</div>
                  <div :class="$style.accountMeta">{{ account.account_name }} · {{ account.mobile }}</div>
                </div>
                <UiTag :tone="account.status === 'enabled' ? 'success' : 'default'">
                  {{ account.status === 'enabled' ? '启用中' : '已停用' }}
                </UiTag>
              </div>
              <div :class="$style.accountDesc">{{ account.remark || '暂无备注' }}</div>
              <div :class="$style.accountFacts">
                <span>模块：{{ account.permission_modules.join(' / ') || '-' }}</span>
                <span>动作：{{ account.action_permissions.join(' / ') || '-' }}</span>
                <span>范围：{{ formatScope(account) }}</span>
              </div>
            </div>
            <div :class="$style.accountActions">
              <UiButton variant="secondary" size="sm" @click="selectAccount(account)">编辑</UiButton>
              <UiButton
                variant="ghost"
                size="sm"
                :disabled="account.status !== 'enabled'"
                @click="handleDisable(account.account_id)"
              >
                停用
              </UiButton>
              <UiButton variant="ghost" size="sm" @click="handleResetLogin(account.account_id)">
                重置登录
              </UiButton>
            </div>
          </article>
        </div>

        <UiStatePanel
          v-else
          tone="empty"
          title="当前还没有子账号"
          description="先在右侧填写账号资料并创建第一个子账号。"
        />
      </UiCard>

      <UiCard elevated :class="$style.formPanel">
        <div :class="$style.panelHead">
          <div>
            <div :class="$style.eyebrow">权限表单</div>
            <h3 :class="$style.title">{{ selectedAccountId ? '编辑子账号' : '创建子账号' }}</h3>
          </div>
        </div>

        <div :class="$style.formGrid">
          <UiInput v-model="form.accountName" label="登录账号" placeholder="例如 ops_beijing" />
          <UiInput v-model="form.displayName" label="显示名称" placeholder="例如 北京运营" />
          <UiInput v-model="form.mobile" label="手机号" placeholder="例如 13900000000" />
          <label :class="$style.fieldBlock">
            <span :class="$style.fieldLabel">备注</span>
            <textarea v-model="form.remark" :class="$style.textarea" placeholder="说明负责门店或业务范围" />
          </label>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">模块权限</div>
          <div :class="$style.optionGrid">
            <label v-for="option in moduleOptions" :key="option.value" :class="$style.optionItem">
              <input
                type="checkbox"
                :checked="form.permissionModules.includes(option.value)"
                @change="toggleSelection(form.permissionModules, option.value)"
              />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">动作权限</div>
          <div :class="$style.optionGrid">
            <label v-for="option in actionOptions" :key="option.value" :class="$style.optionItem">
              <input
                type="checkbox"
                :checked="form.actionPermissions.includes(option.value)"
                @change="toggleSelection(form.actionPermissions, option.value)"
              />
              <span>{{ option.label }}</span>
            </label>
          </div>
        </div>

        <div :class="$style.formSection">
          <div :class="$style.sectionTitle">数据范围</div>
          <div :class="$style.formGrid">
            <label :class="$style.fieldBlock">
              <span :class="$style.fieldLabel">范围类型</span>
              <select v-model="form.dataScopeType" :class="$style.select">
                <option value="merchant">商家级</option>
                <option value="store">店铺级</option>
                <option value="mini_program">小程序级</option>
              </select>
            </label>

            <label :class="$style.fieldBlock">
              <span :class="$style.fieldLabel">范围对象</span>
              <textarea
                v-model="form.scopeIdText"
                :class="$style.textarea"
                :placeholder="scopePlaceholder"
                :disabled="form.dataScopeType === 'merchant'"
              />
            </label>
          </div>
        </div>

        <div :class="$style.actions">
          <UiButton variant="secondary" size="sm" @click="resetForm">清空表单</UiButton>
          <UiButton :disabled="submitting || !canSubmit" @click="handleSubmit">
            {{ submitting ? '提交中...' : selectedAccountId ? '保存修改' : '创建子账号' }}
          </UiButton>
        </div>
      </UiCard>
    </section>
  </WorkspaceLayout>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue';
import UiButton from '@/components/base/UiButton.vue';
import UiCard from '@/components/base/UiCard.vue';
import UiInput from '@/components/base/UiInput.vue';
import UiStatePanel from '@/components/base/UiStatePanel.vue';
import UiTag from '@/components/base/UiTag.vue';
import WorkspaceLayout from '@/components/layout/WorkspaceLayout.vue';
import {
  createSubAccount,
  disableSubAccount,
  fetchSubAccounts,
  resetSubAccountLogin,
  updateSubAccount,
} from '@/services/permission';
import type {
  MerchantSubAccountPageResponseRaw,
  MerchantSubAccountResponseRaw,
  MerchantSubAccountUpsertPayload,
} from '@/types/permission';

const accountPage = ref<MerchantSubAccountPageResponseRaw>({
  list: [],
  page: 1,
  page_size: 20,
  total: 0,
});
const loading = ref(false);
const submitting = ref(false);
const selectedAccountId = ref('');
const actionMessage = ref('');
const actionTone = ref<'success' | 'error'>('success');

const form = reactive({
  accountName: '',
  displayName: '',
  mobile: '',
  remark: '',
  permissionModules: [] as string[],
  actionPermissions: [] as string[],
  dataScopeType: 'merchant' as 'merchant' | 'store' | 'mini_program',
  scopeIdText: '',
});

const moduleOptions = [
  { value: 'store', label: '店铺' },
  { value: 'product', label: '商品' },
  { value: 'order', label: '订单' },
  { value: 'release', label: '发布' },
  { value: 'config', label: '配置' },
];

const actionOptions = [
  { value: 'view', label: '查看' },
  { value: 'edit', label: '编辑' },
  { value: 'publish', label: '发布' },
  { value: 'export', label: '导出' },
];

const canSubmit = computed(() => {
  if (!form.accountName.trim() || !form.displayName.trim() || !form.mobile.trim()) {
    return false;
  }
  if (!form.permissionModules.length || !form.actionPermissions.length) {
    return false;
  }
  if (form.dataScopeType === 'merchant') {
    return true;
  }
  return parseScopeIds(form.scopeIdText).length > 0;
});

const pageState = computed(() => {
  if (loading.value) {
    return {
      tone: 'info' as const,
      title: '正在同步子账号数据',
      description: '页面会从真实接口拉取商家子账号和权限配置。',
    };
  }
  return null;
});

const scopePlaceholder = computed(() => {
  if (form.dataScopeType === 'store') {
    return '输入 store_1001 或 1001，支持逗号/换行分隔';
  }
  if (form.dataScopeType === 'mini_program') {
    return '输入 mini_program_2001 或 2001，支持逗号/换行分隔';
  }
  return '商家级范围不需要填写具体 ID';
});

onMounted(() => {
  void loadAccounts();
});

async function loadAccounts() {
  loading.value = true;
  try {
    accountPage.value = await fetchSubAccounts({
      page: accountPage.value.page,
      pageSize: accountPage.value.page_size,
    });
  } catch (error) {
    pushAction(error instanceof Error ? error.message : '加载子账号失败', 'error');
  } finally {
    loading.value = false;
  }
}

function resetForm() {
  selectedAccountId.value = '';
  form.accountName = '';
  form.displayName = '';
  form.mobile = '';
  form.remark = '';
  form.permissionModules = [];
  form.actionPermissions = [];
  form.dataScopeType = 'merchant';
  form.scopeIdText = '';
}

function selectAccount(account: MerchantSubAccountResponseRaw) {
  selectedAccountId.value = account.account_id;
  form.accountName = account.account_name;
  form.displayName = account.display_name;
  form.mobile = account.mobile;
  form.remark = account.remark || '';
  form.permissionModules = [...account.permission_modules];
  form.actionPermissions = [...account.action_permissions];
  form.dataScopeType = account.data_scope_type;
  form.scopeIdText = account.data_scope_ids.join('\n');
}

async function handleSubmit() {
  submitting.value = true;
  try {
    const payload = buildPayload();
    let account: MerchantSubAccountResponseRaw;
    if (selectedAccountId.value) {
      account = await updateSubAccount(selectedAccountId.value, payload);
      pushAction(`已更新 ${account.display_name} 的权限配置。`, 'success');
    } else {
      account = await createSubAccount(payload);
      const resetResult = await resetSubAccountLogin(account.account_id);
      pushAction(
        `已创建 ${account.display_name}，临时密码：${resetResult.temporary_password}`,
        'success',
      );
    }
    resetForm();
    await loadAccounts();
  } catch (error) {
    pushAction(error instanceof Error ? error.message : '提交失败', 'error');
  } finally {
    submitting.value = false;
  }
}

async function handleDisable(accountId: string) {
  try {
    const result = await disableSubAccount(accountId);
    pushAction(`已停用 ${result.display_name}。`, 'success');
    await loadAccounts();
    if (selectedAccountId.value === accountId) {
      resetForm();
    }
  } catch (error) {
    pushAction(error instanceof Error ? error.message : '停用失败', 'error');
  }
}

async function handleResetLogin(accountId: string) {
  try {
    const result = await resetSubAccountLogin(accountId);
    pushAction(`重置成功，临时密码：${result.temporary_password}`, 'success');
  } catch (error) {
    pushAction(error instanceof Error ? error.message : '重置登录失败', 'error');
  }
}

function buildPayload(): MerchantSubAccountUpsertPayload {
  return {
    accountName: form.accountName.trim(),
    displayName: form.displayName.trim(),
    mobile: form.mobile.trim(),
    remark: form.remark.trim(),
    permissionModules: [...form.permissionModules],
    actionPermissions: [...form.actionPermissions],
    dataScopeType: form.dataScopeType,
    dataScopeIds: parseScopeIds(form.scopeIdText),
  };
}

function toggleSelection(target: string[], value: string) {
  const index = target.indexOf(value);
  if (index >= 0) {
    target.splice(index, 1);
    return;
  }
  target.push(value);
}

function parseScopeIds(raw: string): string[] {
  return raw
    .split(/[\n,]/)
    .map((item) => item.trim())
    .filter(Boolean);
}

function formatScope(account: MerchantSubAccountResponseRaw): string {
  if (account.data_scope_type === 'merchant') {
    return '商家级';
  }
  return `${account.data_scope_type} · ${account.data_scope_ids.join(' / ') || '-'}`;
}

function pushAction(message: string, tone: 'success' | 'error') {
  actionMessage.value = message;
  actionTone.value = tone;
}
</script>

<style module>
.hero {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  padding: 24px 28px;
  border-radius: 28px;
  background: linear-gradient(135deg, rgba(255, 243, 230, 0.98), rgba(237, 244, 255, 0.96));
  box-shadow: 0 18px 36px rgba(9, 29, 46, 0.06);
}

.eyebrow {
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
  color: var(--cdd-text-faint);
}

.heroTitle,
.title {
  margin: 8px 0 0;
  font-size: 24px;
  color: var(--cdd-text);
}

.heroMeta {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 12px;
  color: var(--cdd-text-soft);
  font-size: 13px;
  font-weight: 700;
}

.heroActions {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.grid {
  display: grid;
  grid-template-columns: 1.15fr 0.95fr;
  gap: 24px;
  margin-top: 24px;
}

.listPanel,
.formPanel {
  display: grid;
  gap: 20px;
  align-content: start;
}

.panelHead {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.accountList {
  display: grid;
  gap: 14px;
}

.accountItem {
  display: grid;
  gap: 14px;
  padding: 18px 20px;
  border-radius: 22px;
  background: rgba(248, 250, 252, 0.88);
  border: 1px solid rgba(9, 29, 46, 0.06);
}

.accountItemActive {
  background: linear-gradient(135deg, rgba(255, 243, 230, 0.96), rgba(255, 255, 255, 0.98));
  border-color: rgba(255, 107, 0, 0.18);
}

.accountTitleRow {
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: start;
}

.accountName {
  font-size: 18px;
  font-weight: 800;
  color: var(--cdd-text);
}

.accountMeta,
.accountDesc,
.accountFacts {
  color: var(--cdd-text-soft);
  font-size: 13px;
}

.accountDesc {
  margin-top: 8px;
}

.accountFacts {
  display: flex;
  flex-wrap: wrap;
  gap: 10px 16px;
  margin-top: 10px;
}

.accountActions,
.actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.formGrid {
  display: grid;
  gap: 16px;
}

.formSection {
  display: grid;
  gap: 14px;
}

.sectionTitle,
.fieldLabel {
  font-size: 13px;
  font-weight: 800;
  color: var(--cdd-text-soft);
}

.fieldBlock {
  display: grid;
  gap: 8px;
}

.textarea,
.select {
  width: 100%;
  min-height: 52px;
  padding: 14px 16px;
  border: 0;
  outline: 0;
  border-radius: 18px;
  background: rgba(237, 244, 255, 0.95);
  color: var(--cdd-text);
  font: inherit;
}

.textarea {
  min-height: 110px;
  resize: vertical;
}

.optionGrid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 10px;
}

.optionItem {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 48px;
  padding: 0 14px;
  border-radius: 16px;
  background: rgba(244, 247, 252, 0.9);
  color: var(--cdd-text);
  font-size: 14px;
  font-weight: 700;
}

@media (max-width: 1100px) {
  .grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 720px) {
  .hero,
  .heroActions {
    display: grid;
  }

  .optionGrid {
    grid-template-columns: 1fr;
  }
}
</style>
