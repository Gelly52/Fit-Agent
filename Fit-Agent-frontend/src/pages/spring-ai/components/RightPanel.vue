<template>
    <aside
        class="console-right-panel"
        :class="{ 'right-panel-hidden': shouldHide }"
        role="complementary"
        aria-label="结果与工具面板"
    >
        <section class="right-section quick-tasks-section">
            <h3 class="right-section-title">快捷任务</h3>
            <div class="quick-task-list">
                <button
                    v-for="task in quickTasks"
                    :key="task.id"
                    type="button"
                    class="quick-task-btn"
                    :title="task.label"
                    @click="$emit('execute-task', task.prompt)"
                >
                    <span class="quick-task-icon">{{ task.icon }}</span>
                    <span class="quick-task-label">{{ task.label }}</span>
                </button>
            </div>
        </section>

        <section class="right-section knowledge-sources-section">
            <h3 class="right-section-title">知识来源</h3>
            <div
                class="knowledge-source-list"
                v-if="knowledgeSources.length > 0"
            >
                <div
                    class="knowledge-source-item"
                    v-for="(source, index) in knowledgeSources"
                    :key="source.id || index"
                >
                    <span class="knowledge-source-index">{{ index + 1 }}</span>
                    <div class="knowledge-source-info">
                        <span class="knowledge-source-title">{{
                            source.title || "未命名来源"
                        }}</span>
                        <span
                            class="knowledge-source-snippet"
                            v-if="source.snippet"
                            >{{ source.snippet }}</span
                        >
                    </div>
                </div>
            </div>
            <div class="right-panel-empty" v-else>
                <p>RAG 检索后，知识来源将在此展示</p>
            </div>
        </section>

        <section class="right-section tool-panel-section">
            <h3 class="right-section-title">工具面板</h3>
            <div class="tool-panel-grid">
                <div class="tool-panel-item" @click="$emit('open-upload')">
                    <span class="tool-panel-icon">DOC</span>
                    <span class="tool-panel-label">文档上传</span>
                </div>
                <div class="tool-panel-item" @click="$emit('open-rag-config')">
                    <span class="tool-panel-icon">RAG</span>
                    <span class="tool-panel-label">RAG 配置</span>
                </div>
                <div class="tool-panel-item" @click="$emit('open-benchmark')">
                    <span class="tool-panel-icon">BM</span>
                    <span class="tool-panel-label">Benchmark</span>
                </div>
                <div class="tool-panel-item">
                    <span class="tool-panel-icon">SYS</span>
                    <span class="tool-panel-label">指标信息</span>
                </div>
            </div>
        </section>
    </aside>
</template>

<script>
export default {
    name: "RightPanel",
    emits: ["open-upload", "open-rag-config", "open-benchmark", "execute-task"],
    props: {
        activeView: {
            type: String,
            default: "chat",
        },
        knowledgeSources: {
            type: Array,
            default: function () {
                return [];
            },
        },
    },
    data() {
        return {
            quickTasks: [
                {
                    id: "analyze-week",
                    icon: "\u{1F50D}",
                    label: "分析本周训练",
                    prompt: "分析我这周的训练情况",
                },
                {
                    id: "recovery",
                    icon: "\u{1F49A}",
                    label: "恢复状态评估",
                    prompt: "帮我看下最近的恢复状态",
                },
                {
                    id: "weekly-report",
                    icon: "\u{1F4DD}",
                    label: "生成本周周报",
                    prompt: "分析我这周的训练情况，生成周报发到我的邮箱",
                },
            ],
        };
    },
    computed: {
        shouldHide: function () {
            return this.activeView !== "chat";
        },
    },
};
</script>
