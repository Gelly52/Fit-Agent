<template>
    <aside
        class="console-left-sidebar"
        role="complementary"
        aria-label="功能导航"
    >
        <section class="sidebar-section quick-tasks-section">
            <h3 class="sidebar-section-title">功能导航</h3>
            <div class="quick-task-list">
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
    emits: ["execute-task", "switch-view"],
    props: {
        activeView: {
            type: String,
            default: "chat",
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
