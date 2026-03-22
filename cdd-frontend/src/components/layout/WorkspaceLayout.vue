<template>
  <div :class="$style.shell">
    <aside :class="$style.sidebar">
      <div :class="$style.brandBlock">
        <div :class="$style.brandMark">CD</div>
        <div>
          <div :class="$style.brandTitle">ChengDD</div>
          <div :class="$style.brandMeta">商家中台一期</div>
        </div>
      </div>

      <div :class="$style.mobileSummary">
        <div :class="$style.mobileSummaryLabel">当前商户</div>
        <div :class="$style.mobileSummaryValue">{{ authStore.user.merchantName }}</div>
      </div>

      <nav :class="$style.nav">
        <RouterLink
          v-for="item in appMenuItems"
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
        <div :class="$style.sidebarFootLabel">当前商户</div>
        <div :class="$style.sidebarFootValue">{{ authStore.user.merchantName }}</div>
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
            <div :class="$style.operatorName">{{ authStore.user.operatorName }}</div>
            <div :class="$style.operatorRole">{{ authStore.user.roleName }}</div>
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
import { RouterLink, useRoute, useRouter } from 'vue-router';
import UiCard from '@/components/base/UiCard.vue';
import { appMenuItems } from '@/app/menu';
import { useAuthStore } from '@/stores/auth';

defineProps<{
  title: string;
  eyebrow: string;
  description?: string;
}>();

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

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
  grid-template-rows: auto 1fr auto;
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
    color 0.18s ease;
}

.navItem:hover {
  transform: translateX(3px);
  background: rgba(255, 255, 255, 0.72);
}

.navItemActive {
  color: #fff;
  background: linear-gradient(135deg, rgba(160, 65, 0, 0.94), rgba(255, 107, 0, 0.94));
  box-shadow: 0 18px 24px rgba(255, 107, 0, 0.18);
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
  background: rgba(255, 255, 255, 0.16);
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

.sidebarFoot {
  padding: 18px;
}

.mobileSummary {
  display: none;
}

.sidebarFootLabel {
  color: var(--cdd-text-faint);
  font-size: 12px;
  font-weight: 700;
}

.sidebarFootValue {
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
}

.operatorCard {
  min-width: 180px;
  padding: 14px 16px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.78);
  box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.06);
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
    grid-template-rows: auto auto auto;
  }

  .nav {
    grid-template-columns: repeat(5, minmax(136px, 1fr));
    overflow-x: auto;
    padding-bottom: 4px;
  }

  .main {
    padding-top: 0;
  }

  .mobileSummary {
    display: block;
    padding: 14px 16px;
    border-radius: 18px;
    background: rgba(255, 255, 255, 0.72);
    box-shadow: inset 0 0 0 1px rgba(9, 29, 46, 0.06);
  }

  .mobileSummaryLabel {
    color: var(--cdd-text-faint);
    font-size: 12px;
    font-weight: 700;
  }

  .mobileSummaryValue {
    margin-top: 6px;
    font-size: 16px;
    font-weight: 800;
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
    justify-content: space-between;
  }

  .operatorCard {
    min-width: 0;
    flex: 1;
  }

  .title {
    font-size: 28px;
  }

  .nav {
    grid-template-columns: repeat(5, minmax(122px, 1fr));
  }
}
</style>
