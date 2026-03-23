<template>
  <div class="view-page dashboard-view">
    <div class="view-header">
      <button type="button" class="view-back-btn" @click="$emit('back')">
        <span class="view-back-arrow">&larr;</span> 返回对话
      </button>
      <h2 class="view-title">数据总览</h2>
    </div>

    <div class="view-body">
      <div class="dashboard-content">
        <!-- Today Status -->
        <section class="dashboard-section">
          <h3 class="dashboard-section-title">今日状态</h3>
          <div class="dashboard-status-grid">
            <div class="dashboard-stat-card">
              <span class="dashboard-stat-label">体重</span>
              <span class="dashboard-stat-value">{{ todayStatus.weight || '--' }} <small>kg</small></span>
            </div>
            <div class="dashboard-stat-card">
              <span class="dashboard-stat-label">体脂</span>
              <span class="dashboard-stat-value">{{ todayStatus.bodyFat || '--' }} <small>%</small></span>
            </div>
            <div class="dashboard-stat-card">
              <span class="dashboard-stat-label">疲劳度</span>
              <span class="dashboard-stat-value" :class="fatigueClass">{{ todayStatus.fatigue || '--' }}</span>
            </div>
            <div class="dashboard-stat-card">
              <span class="dashboard-stat-label">睡眠</span>
              <span class="dashboard-stat-value">{{ todayStatus.sleep || '--' }} <small>h</small></span>
            </div>
            <div class="dashboard-stat-card dashboard-stat-card-wide">
              <span class="dashboard-stat-label">最近训练肌群</span>
              <span class="dashboard-stat-value dashboard-stat-value-text">{{ todayStatus.lastMuscleGroup || '暂无记录' }}</span>
            </div>
          </div>
        </section>

        <!-- Week Overview -->
        <section class="dashboard-section">
          <h3 class="dashboard-section-title">本周概览</h3>
          <div class="dashboard-week-grid">
            <div class="dashboard-week-card">
              <span class="dashboard-week-value">{{ weekSummary.trainingDays || 0 }}</span>
              <span class="dashboard-week-label">训练天数</span>
            </div>
            <div class="dashboard-week-card">
              <span class="dashboard-week-value">{{ weekSummary.totalVolume || 0 }} <small>kg</small></span>
              <span class="dashboard-week-label">总训练量</span>
            </div>
            <div class="dashboard-week-card">
              <span class="dashboard-week-value" :class="trendClass">{{ weekSummary.trend || '暂无数据' }}</span>
              <span class="dashboard-week-label">趋势</span>
            </div>
          </div>
        </section>

        <!-- Execution Result Summary -->
        <section class="dashboard-section" v-if="hasResultData">
          <h3 class="dashboard-section-title">最近执行结果</h3>
          <div class="dashboard-result-grid">
            <div class="dashboard-result-item" v-if="resultSummary.trainingDays != null">
              <span class="dashboard-result-label">训练天数</span>
              <span class="dashboard-result-value">{{ resultSummary.trainingDays }}</span>
            </div>
            <div class="dashboard-result-item" v-if="resultSummary.totalVolume != null">
              <span class="dashboard-result-label">训练量</span>
              <span class="dashboard-result-value">{{ resultSummary.totalVolume }} <small>kg</small></span>
            </div>
            <div class="dashboard-result-item" v-if="resultSummary.calories != null">
              <span class="dashboard-result-label">热量消耗</span>
              <span class="dashboard-result-value">{{ resultSummary.calories }} <small>kcal</small></span>
            </div>
            <div class="dashboard-result-item" v-if="resultSummary.fatigue != null">
              <span class="dashboard-result-label">疲劳水平</span>
              <span class="dashboard-result-value">{{ resultSummary.fatigue }}</span>
            </div>
            <div class="dashboard-result-item" v-if="resultSummary.weightTrend != null">
              <span class="dashboard-result-label">体重趋势</span>
              <span class="dashboard-result-value">{{ resultSummary.weightTrend }}</span>
            </div>
            <div class="dashboard-result-item" v-if="resultSummary.recovery != null">
              <span class="dashboard-result-label">恢复状态</span>
              <span class="dashboard-result-value">{{ resultSummary.recovery }}</span>
            </div>
          </div>
        </section>

        <!-- Report Preview -->
        <section class="dashboard-section" v-if="reportContent">
          <h3 class="dashboard-section-title">周报预览</h3>
          <div class="dashboard-report-content" v-html="reportContent"></div>
        </section>

        <!-- Quick Actions -->
        <section class="dashboard-section">
          <h3 class="dashboard-section-title">快捷操作</h3>
          <div class="dashboard-actions-grid">
            <button
              type="button"
              class="dashboard-action-card"
              @click="$emit('execute-task', '分析我这周的训练情况')"
            >
              <span class="dashboard-action-icon">&#x1F50D;</span>
              <span class="dashboard-action-label">分析本周训练</span>
              <span class="dashboard-action-desc">查看训练数据与趋势分析</span>
            </button>
            <button
              type="button"
              class="dashboard-action-card"
              @click="$emit('execute-task', '帮我看下最近的恢复状态')"
            >
              <span class="dashboard-action-icon">&#x1F49A;</span>
              <span class="dashboard-action-label">恢复状态评估</span>
              <span class="dashboard-action-desc">评估疲劳与恢复建议</span>
            </button>
            <button
              type="button"
              class="dashboard-action-card"
              @click="$emit('execute-task', '分析我这周的训练情况，生成周报发到我的邮箱')"
            >
              <span class="dashboard-action-icon">&#x1F4DD;</span>
              <span class="dashboard-action-label">生成本周周报</span>
              <span class="dashboard-action-desc">分析并发送训练周报</span>
            </button>
          </div>
        </section>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'DashboardView',
  emits: ['back', 'execute-task'],
  props: {
    todayStatus: {
      type: Object,
      default: function () {
        return {};
      },
    },
    weekSummary: {
      type: Object,
      default: function () {
        return {};
      },
    },
    resultSummary: {
      type: Object,
      default: function () {
        return {};
      },
    },
    reportContent: {
      type: String,
      default: '',
    },
  },
  computed: {
    fatigueClass: function () {
      var fatigue = this.todayStatus.fatigue;
      if (!fatigue) return '';
      if (fatigue === '低' || fatigue === 'low') return 'status-good';
      if (fatigue === '中' || fatigue === 'medium') return 'status-warn';
      return 'status-alert';
    },
    trendClass: function () {
      var trend = this.weekSummary.trend || '';
      if (trend.indexOf('上升') >= 0 || trend.indexOf('增长') >= 0) return 'trend-up';
      if (trend.indexOf('下降') >= 0 || trend.indexOf('减少') >= 0) return 'trend-down';
      return 'trend-stable';
    },
    hasResultData: function () {
      var s = this.resultSummary;
      return s && (
        s.trainingDays != null ||
        s.totalVolume != null ||
        s.calories != null ||
        s.fatigue != null ||
        s.weightTrend != null ||
        s.recovery != null
      );
    },
  },
};
</script>
