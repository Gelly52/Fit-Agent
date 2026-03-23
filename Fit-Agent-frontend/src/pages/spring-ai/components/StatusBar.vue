<template>
    <footer
        class="console-status-bar"
        role="contentinfo"
        aria-label="系统状态栏"
    >
        <span class="status-bar-item">
            <span class="status-bar-dot" :class="sseClass"></span>
            {{ sseLabel }}
        </span>
        <span class="status-bar-item" v-if="docCount > 0">
            文档库：{{ docCount }} 篇
        </span>
        <span class="status-bar-item" v-if="lastTaskDuration">
            上次耗时：{{ lastTaskDuration }}
        </span>
        <span class="status-bar-item" v-if="uploadSynced"> 文档已同步 </span>
    </footer>
</template>

<script>
export default {
    name: "StatusBar",
    props: {
        currentMode: {
            type: String,
            default: "Agent",
        },
        sseState: {
            type: String,
            default: "idle",
        },
        docCount: {
            type: Number,
            default: 0,
        },
        lastTaskDuration: {
            type: String,
            default: "",
        },
        uploadSynced: {
            type: Boolean,
            default: false,
        },
    },
    computed: {
        sseClass() {
            if (this.sseState === "connected") return "dot-active";
            if (this.sseState === "connecting") return "dot-warn";
            if (this.sseState === "disconnected") return "dot-alert";
            return "dot-idle";
        },
        sseLabel() {
            if (this.sseState === "connected") return "SSE 已连接";
            if (this.sseState === "connecting") return "SSE 连接中";
            if (this.sseState === "disconnected") return "SSE 已断开";
            if (this.sseState === "unsupported") return "SSE 不可用";
            return "SSE 待初始化";
        },
    },
};
</script>
