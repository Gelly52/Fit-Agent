<template>
  <div class="step-execution-card" v-if="steps.length > 0" role="region" aria-label="任务执行步骤">
    <h4 class="step-card-title">任务执行进度</h4>
    <div class="step-timeline">
      <div
        class="step-item"
        v-for="(step, index) in steps"
        :key="step.id || index"
        :class="stepClass(step)"
      >
        <div class="step-indicator">
          <span class="step-dot"></span>
          <span class="step-line" v-if="index < steps.length - 1"></span>
        </div>
        <div class="step-content">
          <span class="step-label">{{ step.label }}</span>
          <span class="step-status-text">{{ stepStatusText(step) }}</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'StepExecutionCard',
  props: {
    steps: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  methods: {
    stepClass(step) {
      var status = step.status || 'pending';
      return {
        'step-completed': status === 'completed',
        'step-running': status === 'running',
        'step-pending': status === 'pending',
        'step-failed': status === 'failed',
      };
    },
    stepStatusText(step) {
      var status = step.status || 'pending';
      if (status === 'completed') return '已完成';
      if (status === 'running') return '执行中';
      if (status === 'failed') return '失败';
      return '等待中';
    },
  },
};
</script>
