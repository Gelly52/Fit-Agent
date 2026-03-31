<template>
  <div
    class="step-execution-card"
    v-if="steps.length > 0"
    role="region"
    aria-label="任务执行步骤"
  >
    <h4 class="step-card-title">任务执行进度</h4>
    <div class="step-timeline">
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
  </div>
</template>

<script>
export default {
  name: "StepExecutionCard",
  props: {
    steps: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  methods: {
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
  },
};
</script>
