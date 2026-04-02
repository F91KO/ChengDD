<template>
  <div :class="$style.shell">
    <aside :class="$style.sidebar">
      <div :class="$style.brandBlock">
        <div :class="$style.brandMark">CD</div>
        <div>
          <div :class="$style.brandTitle">ChengDD</div>
          <div :class="$style.brandMeta">{{ brandMeta }}</div>
        </div>
      </div>

      <div :class="$style.mobileSummary">
        <div :class="$style.mobileSummaryLabel">{{ subjectLabel }}</div>
        <div :class="$style.mobileSummaryValue">{{ subjectValue }}</div>
      </div>

      <nav :class="$style.nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.path"
          :to="item.path"
          :class="[$style.navItem, route.path === item.path ? $style.navItemActive : '']"
        >
          <span :class="$style.navIcon">{{ item.shortLabel }}</span>
          <span :class="$style.navLabel">{{ item.label }}</span>
          <span v-if="item.badge" :class="$style.navBadge">{{ item.badge }}</span>
        </RouterLink>
      </nav>

      <UiCard :class="$style.sidebarFoot">
        <div :class="$style.sidebarFootLabel">{{ subjectLabel }}</div>
        <div :class="$style.sidebarFootValue">{{ subjectValue }}</div>
        <div :class="$style.sidebarFootMeta">{{ authStore.user.roleName }}</div>
      </UiCard>
    </aside>

    <div :class="$style.main">
      <header :class="$style.topbar">
        <div>
          <div :class="$style.eyebrow">{{ eyebrow }}</div>
          <h1 :class="$style.title">{{ title }}</h1>
          <p v-if="description" :class="$style.description">{{ description }}</p>
        </div>
        <div :class="$style.topbarAside">
          <div :class="$style.operatorCard">
            <div :class="$style.operatorMeta">
              <div :class="$style.operatorName">{{ authStore.user.operatorName }}</div>
              <div :class="$style.operatorRole">{{ authStore.user.roleName }}</div>
            </div>
            <div :class="$style.operatorMerchant">{{ authStore.user.merchantName }}</div>
          </div>
          <button :class="$style.logout" @click="logout">退出</button>
        </div>
      </header>

      <main :class="$style.content">
        <slot />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue';
import { RouterLink, useRoute, useRouter } from 'vue-router';
import UiCard from '@/components/base/UiCard.vue';
import { appMenuItems, type AppMenuItem } from '@/app/menu';
import { useAuthStore } from '@/stores/auth';

const props = withDefaults(defineProps<{
  title: string;
  eyebrow: string;
  description?: string;
  brandMeta?: string;
  subjectLabel?: string;
  subjectValue?: string;
  navItems?: AppMenuItem[];
}>(), {
  description: undefined,
  brandMeta: '商家中台一期',
  subjectLabel: '当前商户',
  subjectValue: undefined,
  navItems: () => appMenuItems,
});

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const subjectValue = computed(() => props.subjectValue || authStore.user.merchantName);
const navItems = computed(() => props.navItems.filter((item) => authStore.canAccess(item)));
const brandMeta = computed(() => props.brandMeta);
const subjectLabel = computed(() => props.subjectLabel);

function logout() {
  authStore.logout();
  void router.push('/login');
}
</script>

<style module>
.shell {
  display: grid;
  grid-template-columns: 280px minmax(0, 1fr);
  min-height: 100vh;
}

.sidebar {
  position: sticky;
  top: 0;
  display: grid;
  grid-template-rows: auto auto 1fr auto;
  gap: 24px;
  height: 100vh;
  padding: 28px 22px;
  background:
    linear-gradient(180deg, rgba(255, 255, 255, 0.8), rgba(237, 244, 255, 0.96)),
    var(--cdd-surface-low);
  backdrop-filter: blur(18px);
  box-shadow: inset -1px 0 0 rgba(9, 29, 46, 0.05);
}

.brandBlock {
  display: flex;
  align-items: center;
  gap: 14px;
}

.brandMark {
  width: 54px;
  height: 54px;
  display: grid;
  place-items: center;
  border-radius: 18px;
  background: linear-gradient(135deg, var(--cdd-primary-deep), var(--cdd-primary));
  color: #fff;
  font-size: 18px;
  font-weight: 800;
  box-shadow: 0 20px 30px rgba(255, 107, 0, 0.22);
}

.brandTitle {
  font-size: 20px;
  font-weight: 800;
  color: var(--cdd-text);
}

.brandMeta {
  margin-top: 4px;
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.nav {
  display: grid;
  gap: 10px;
  align-content: start;
  min-height: 0;
}

.navItem {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 56px;
  padding: 0 16px;
  border-radius: 18px;
  color: var(--cdd-text-soft);
  transition:
    transform 0.18s ease,
    background 0.18s ease,
    color 0.18s ease,
    box-shadow 0.18s ease;
}

.navItem:hover {
  transform: translateX(3px);
  background: rgba(255, 255, 255, 0.72);
}

.navItemActive {
  color: var(--cdd-text);
  background: linear-gradient(135deg, rgba(255, 231, 214, 0.96), rgba(255, 245, 237, 0.98));
  box-shadow:
    inset 0 0 0 1px rgba(255, 107, 0, 0.16),
    0 12px 20px rgba(255, 107, 0, 0.08);
}

.navIcon {
  width: 30px;
  height: 30px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  background: rgba(9, 29, 46, 0.08);
  font-size: 13px;
  font-weight: 800;
}

.navItemActive .navIcon {
  background: linear-gradient(135deg, var(--cdd-primary-deep), var(--cdd-primary));
  color: #fff;
}

.navItemActive .navLabel,
.navItemActive .navBadge {
  color: var(--cdd-text);
}

.navLabel {
  flex: 1;
  font-size: 14px;
  font-weight: 700;
}

.navBadge {
  min-width: 28px;
  height: 28px;
  display: grid;
  place-items: center;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 800;
  background: rgba(255, 255, 255, 0.18);
}

.navItemActive .navBadge {
  background: rgba(255, 107, 0, 0.12);
}

.sidebarFoot {
  padding: 18px;
}

.mobileSummary {
  display: none;
}

.sidebarFootLabel,
.mobileSummaryLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.sidebarFootValue,
.mobileSummaryValue {
  margin-top: 10px;
  color: var(--cdd-text);
  font-size: 18px;
  font-weight: 800;
}

.sidebarFootMeta {
  margin-top: 4px;
  color: var(--cdd-text-soft);
  font-size: 13px;
}

.main {
  min-width: 0;
  padding: 28px 28px 40px;
}

.topbar {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20px;
  padding: 12px 4px 28px;
}

.eyebrow {
  color: var(--cdd-primary-deep);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.title {
  margin: 10px 0 0;
  font-size: 34px;
  line-height: 1.05;
  letter-spacing: -0.04em;
}

.description {
  max-width: 720px;
  margin: 12px 0 0;
  color: var(--cdd-text-soft);
  font-size: 15px;
  line-height: 1.7;
}

.topbarAside {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  justify-content: flex-end;
}

.operatorCard {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  min-width: 0;
  padding: 12px 14px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.06);
}

.operatorMeta {
  min-width: 0;
}

.operatorName {
  font-size: 14px;
  font-weight: 800;
}

.operatorRole {
  margin-top: 4px;
  color: var(--cdd-text-faint);
  font-size: 12px;
}

.operatorMerchant {
  min-width: 0;
  padding-left: 16px;
  border-left: 1px solid rgba(9, 29, 46, 0.08);
  color: var(--cdd-text-soft);
  font-size: 12px;
  font-weight: 700;
  white-space: normal;
  overflow-wrap: anywhere;
}

.logout {
  min-height: 46px;
  padding: 0 16px;
  border: 0;
  border-radius: 16px;
  color: var(--cdd-text-soft);
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.08);
}

.content {
  display: grid;
  gap: 24px;
}

@media (max-width: 1024px) {
  .shell {
    grid-template-columns: 1fr;
  }

  .sidebar {
    position: static;
    height: auto;
    grid-template-rows: auto auto auto auto;
  }

  .nav {
    grid-template-columns: repeat(auto-fit, minmax(132px, 1fr));
  }

  .mobileSummary {
    display: block;
    padding: 14px 16px;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.72);
    box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.06);
  }

  .main {
    padding-top: 0;
  }
}

@media (max-width: 720px) {
  .main {
    padding: 18px 16px 28px;
  }

  .sidebar {
    padding: 18px 16px;
  }

  .topbar {
    flex-direction: column;
    align-items: stretch;
    padding-bottom: 18px;
  }

  .topbarAside {
    align-items: stretch;
    justify-content: space-between;
  }

  .operatorCard {
    min-width: 0;
    flex: 1;
    align-items: flex-start;
    flex-direction: column;
    gap: 8px;
  }

  .operatorMerchant {
    padding-left: 0;
    border-left: 0;
  }

  .logout {
    width: 100%;
  }

  .title {
    font-size: 28px;
  }

  .nav {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
