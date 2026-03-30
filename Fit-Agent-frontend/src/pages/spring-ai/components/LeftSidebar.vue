<template>
  <aside
    class="console-left-sidebar"
    role="complementary"
    aria-label="功能导航"
  >
    <section class="sidebar-section quick-tasks-section">
      <h3 class="sidebar-section-title">功能导航</h3>
      <div class="quick-task-list">
        <div class="chat-entry-group" :class="{ 'is-expanded': chatExpanded }">
          <div class="chat-entry-row">
            <button
              type="button"
              class="quick-task-btn chat-entry-btn"
              :class="{ 'nav-active': activeView === 'chat' }"
              title="返回聊天"
              @click="$emit('switch-view', 'chat')"
            >
              <span class="quick-task-icon">💬</span>
              <span class="quick-task-label">聊一聊</span>
            </button>
            <button
              type="button"
              class="chat-toggle-caret"
              :class="{ 'is-expanded': chatExpanded }"
              :aria-expanded="chatExpanded ? 'true' : 'false'"
              :aria-label="chatExpanded ? '收起聊天记录' : '展开聊天记录'"
              :title="chatExpanded ? '收起聊天记录' : '展开聊天记录'"
              @click="$emit('toggle-chat-expand')"
            >
              <span class="chat-toggle-caret-icon" aria-hidden="true"></span>
            </button>
          </div>

          <div v-if="chatExpanded" class="chat-entry-panel">
            <button
              type="button"
              class="quick-task-btn chat-history-create-btn"
              :disabled="conversationBusy"
              @click="$emit('create-chat')"
            >
              <span class="quick-task-icon">＋</span>
              <span class="quick-task-label">新建聊天</span>
            </button>

            <div v-if="chatRecordsLoading" class="chat-history-empty">
              正在加载聊天记录...
            </div>
            <div
              v-else-if="chatSessionList.length === 0"
              class="chat-history-empty"
            >
              暂无聊天记录
            </div>
            <div v-else class="chat-history-list">
              <button
                v-for="session in chatSessionList"
                :key="session.sessionId"
                type="button"
                class="chat-history-item"
                :class="{
                  'is-active': session.sessionId === activeChatSessionId,
                }"
                :disabled="conversationBusy"
                @click="$emit('select-chat-session', session.sessionId)"
              >
                <span class="chat-history-item-title">{{ session.title }}</span>
                <span class="chat-history-item-meta">{{
                  session.updatedAtLabel || "最近更新"
                }}</span>
              </button>
            </div>
          </div>
        </div>

        <button
          v-for="task in navItems"
          :key="task.id"
          type="button"
          class="quick-task-btn"
          :class="{ 'nav-active': isActive(task) }"
          :title="task.label"
          @click="handleTaskClick(task)"
        >
          <span class="quick-task-icon">{{ task.icon }}</span>
          <span class="quick-task-label">{{ task.label }}</span>
        </button>
      </div>
    </section>

    <section class="sidebar-section theme-section">
      <h3 class="sidebar-section-title">主题</h3>
      <div class="theme-switcher">
        <button
          v-for="opt in themeOptions"
          :key="opt.value"
          type="button"
          class="theme-option-btn"
          :class="{ 'theme-active': currentTheme === opt.value }"
          @click="switchTheme(opt.value)"
        >
          <span class="theme-option-icon">{{ opt.icon }}</span>
          <span class="theme-option-label">{{ opt.label }}</span>
        </button>
      </div>
    </section>
  </aside>
</template>

<script>
export default {
  name: "LeftSidebar",
  emits: [
    "execute-task",
    "switch-view",
    "toggle-chat-expand",
    "select-chat-session",
    "create-chat",
  ],
  props: {
    activeView: {
      type: String,
      default: "chat",
    },
    chatExpanded: {
      type: Boolean,
      default: false,
    },
    chatSessionList: {
      type: Array,
      default: function () {
        return [];
      },
    },
    activeChatSessionId: {
      type: [String, Number],
      default: null,
    },
    chatRecordsLoading: {
      type: Boolean,
      default: false,
    },
    conversationBusy: {
      type: Boolean,
      default: false,
    },
  },
  data() {
    return {
      navItems: [
        {
          id: "dashboard",
          icon: "\u{1F4CB}",
          label: "数据总览",
          type: "view",
          view: "dashboard",
        },
        {
          id: "log-training",
          icon: "\u{1F3CB}",
          label: "记录今天训练",
          type: "view",
          view: "training-log",
        },
        {
          id: "log-body",
          icon: "\u{1F4CA}",
          label: "记录身体指标",
          type: "view",
          view: "body-metrics",
        },
        {
          id: "upload-doc",
          icon: "\u{1F4C1}",
          label: "上传健身知识文档",
          type: "view",
          view: "upload",
        },
      ],
      themeOptions: [
        { value: "system", icon: "\u{1F4BB}", label: "System" },
        { value: "light", icon: "\u{2600}", label: "Light" },
        { value: "dark", icon: "\u{1F319}", label: "Dark" },
      ],
      currentTheme: "dark",
    };
  },
  created() {
    var saved = localStorage.getItem("geogeo-theme");
    if (
      saved &&
      (saved === "system" || saved === "light" || saved === "dark")
    ) {
      this.currentTheme = saved;
    }
    this.applyTheme(this.currentTheme);
  },
  methods: {
    handleTaskClick(task) {
      if (task.type === "direct") {
        this.$emit("execute-task", task.prompt);
      } else if (task.type === "view") {
        this.$emit("switch-view", task.view);
      }
    },
    isActive(task) {
      if (task.type === "view") {
        return this.activeView === task.view;
      }
      return false;
    },
    switchTheme(theme) {
      this.currentTheme = theme;
      localStorage.setItem("geogeo-theme", theme);
      this.applyTheme(theme);
    },
    applyTheme(theme) {
      var root = document.documentElement;
      root.removeAttribute("data-theme");

      if (theme === "light") {
        root.setAttribute("data-theme", "light");
      } else if (theme === "dark") {
        root.setAttribute("data-theme", "dark");
      } else {
        // system: follow OS preference
        var prefersDark =
          window.matchMedia &&
          window.matchMedia("(prefers-color-scheme: dark)").matches;
        root.setAttribute("data-theme", prefersDark ? "dark" : "light");
      }
    },
  },
};
</script>
