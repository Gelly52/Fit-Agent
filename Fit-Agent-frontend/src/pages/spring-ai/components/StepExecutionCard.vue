<template>
  <div
    class="step-execution-card"
    v-if="showCard()"
    :class="{
      'step-card-active': isThinking || (isSending && !isStreaming),
      'step-card-collapsed': !thinkingExpanded,
    }"
    role="region"
    aria-label="任务执行进度与思考过程"
  >
    <div class="step-card-header">
      <div class="step-card-heading">
        <h4 class="step-card-title">任务执行进度</h4>
        <p class="step-card-summary">{{ resolveCardSummary() }}</p>
      </div>
      <button
        type="button"
        class="step-card-toggle"
        :aria-expanded="thinkingExpanded ? 'true' : 'false'"
        :aria-label="thinkingExpanded ? '收起任务执行详情' : '展开任务执行详情'"
        @click="$emit('toggle-thinking')"
      >
        {{ thinkingExpanded ? "收起" : "展开" }}
      </button>
    </div>

    <div
      class="step-timeline"
      v-if="steps.length > 0"
      aria-label="任务执行步骤"
    >
      <div
        class="step-item"
        v-for="(step, index) in steps"
        :key="step.id || step.stepNo || index"
        :class="stepClass(step)"
      >
        <div class="step-indicator">
          <span class="step-dot"></span>
          <span class="step-line" v-if="index < steps.length - 1"></span>
        </div>
        <div class="step-content">
          <span class="step-label">{{ resolveStepLabel(step) }}</span>
          <span class="step-status-text">{{ stepStatusText(step) }}</span>
        </div>
      </div>
    </div>

    <div class="step-thinking">
      <div class="step-thinking-header">
        <div class="step-thinking-title">
          <span
            v-if="isThinking || (isSending && !isStreaming)"
            class="message-thinking-dot"
          ></span>
          {{ thinkingStatusTitle() }}
        </div>
        <span v-if="resolveThinkingMeta()" class="step-thinking-meta">
          {{ resolveThinkingMeta() }}
        </span>
      </div>

      <div v-if="thinkingExpanded" class="step-thinking-body">
        <template v-if="hasThinkingContent()">
          <div class="step-thinking-content">{{ thinkingContent }}</div>
        </template>
        <template v-else>
          <div class="step-thinking-placeholder">
            <span class="message-thinking-dot"></span>
            正在执行任务，请稍候...
          </div>
        </template>
      </div>

      <div v-else class="step-thinking-collapsed">
        {{ resolveCollapsedThinkingText() }}
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: "StepExecutionCard",
  emits: ["toggle-thinking"],
  props: {
    steps: {
      type: Array,
      default: function () {
        return [];
      },
    },
    thinkingContent: {
      type: String,
      default: "",
    },
    isThinking: {
      type: Boolean,
      default: false,
    },
    thinkingExpanded: {
      type: Boolean,
      default: true,
    },
    isSending: {
      type: Boolean,
      default: false,
    },
    isStreaming: {
      type: Boolean,
      default: false,
    },
  },
  methods: {
    showCard() {
      return (
        this.steps.length > 0 ||
        this.hasThinkingContent() ||
        (this.isSending && !this.isStreaming)
      );
    },
    hasThinkingContent() {
      return !!(this.thinkingContent && this.thinkingContent.length > 0);
    },
    normalizeStepStatus(step) {
      var rawStatus =
        step && (step.status != null ? step.status : step.stepStatus);
      var status =
        rawStatus == null ? "pending" : String(rawStatus).toLowerCase();
      if (status === "success" || status === "completed") {
        return "completed";
      }
      if (status === "running") {
        return "running";
      }
      if (status === "failed" || status === "error") {
        return "failed";
      }
      return "pending";
    },
    resolveStepLabel(step) {
      if (!step) {
        return "未命名步骤";
      }
      return step.label || step.stepName || "未命名步骤";
    },
    stepClass(step) {
      var status = this.normalizeStepStatus(step);
      return {
        "step-completed": status === "completed",
        "step-running": status === "running",
        "step-pending": status === "pending",
        "step-failed": status === "failed",
      };
    },
    stepStatusText(step) {
      var status = this.normalizeStepStatus(step);
      if (status === "completed") return "已完成";
      if (status === "running") return "执行中";
      if (status === "failed") return "失败";
      return "等待中";
    },
    completedStepCount() {
      var count = 0;
      for (var i = 0; i < this.steps.length; i++) {
        if (this.normalizeStepStatus(this.steps[i]) === "completed") {
          count += 1;
        }
      }
      return count;
    },
    findStepByStatus(status) {
      for (var i = 0; i < this.steps.length; i++) {
        if (this.normalizeStepStatus(this.steps[i]) === status) {
          return this.steps[i];
        }
      }
      return null;
    },
    resolveCardSummary() {
      var failedStep = this.findStepByStatus("failed");
      if (failedStep) {
        return this.resolveStepLabel(failedStep) + " · 失败";
      }

      var runningStep = this.findStepByStatus("running");
      if (runningStep) {
        return this.resolveStepLabel(runningStep) + " · 执行中";
      }

      if (this.steps.length > 0) {
        var completedCount = this.completedStepCount();
        if (completedCount >= this.steps.length) {
          return "全部步骤已完成";
        }
        return "已完成 " + completedCount + " / " + this.steps.length + " 步";
      }

      if (this.isThinking || (this.isSending && !this.isStreaming)) {
        return "正在生成思考过程";
      }

      if (this.hasThinkingContent()) {
        return "可查看本轮思考过程";
      }

      return "等待开始";
    },
    thinkingStatusTitle() {
      if (this.isThinking || (this.isSending && !this.isStreaming)) {
        return "思考中...";
      }
      if (this.hasThinkingContent()) {
        return "思考过程";
      }
      return "等待思考内容";
    },
    resolveThinkingMeta() {
      var failedStep = this.findStepByStatus("failed");
      if (failedStep) {
        return "失败于「" + this.resolveStepLabel(failedStep) + "」";
      }

      var runningStep = this.findStepByStatus("running");
      if (runningStep) {
        return "当前步骤：" + this.resolveStepLabel(runningStep);
      }

      if (this.steps.length > 0) {
        return "共 " + this.steps.length + " 步";
      }

      if (this.isThinking || (this.isSending && !this.isStreaming)) {
        return "任务进行中";
      }

      if (this.hasThinkingContent()) {
        return "可回看";
      }

      return "";
    },
    resolveCollapsedThinkingText() {
      if (this.hasThinkingContent()) {
        var text = String(this.thinkingContent).replace(/\s+/g, " ").trim();
        if (text.length > 48) {
          return text.slice(0, 48) + "...";
        }
        return text || "可展开查看完整思考过程";
      }

      if (this.isThinking || (this.isSending && !this.isStreaming)) {
        return "正在执行任务，请稍候...";
      }

      return this.resolveCardSummary();
    },
  },
};
</script>
