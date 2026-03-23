<template>
  <div class="view-page training-log-view">
    <div class="view-header">
      <button type="button" class="view-back-btn" @click="$emit('back')">
        <span class="view-back-arrow">&larr;</span> 返回对话
      </button>
      <h2 class="view-title">记录今天训练</h2>
    </div>

    <div class="view-body">
      <!-- Week Summary Context Bar -->
      <div class="view-context-bar">
        <div class="context-stat">
          <span class="context-stat-value">{{ weekSummary.trainingDays || 0 }}</span>
          <span class="context-stat-label">本周训练天数</span>
        </div>
        <div class="context-stat">
          <span class="context-stat-value">{{ weekSummary.totalVolume || 0 }} <small>kg</small></span>
          <span class="context-stat-label">本周总训练量</span>
        </div>
        <div class="context-stat">
          <span class="context-stat-value" :class="trendClass">{{ weekSummary.trend || '暂无数据' }}</span>
          <span class="context-stat-label">趋势</span>
        </div>
      </div>

      <!-- Training Form -->
      <div class="view-form-card">
        <div
          v-for="(ex, idx) in exercises"
          :key="idx"
          class="exercise-entry"
        >
          <div class="exercise-entry-header">
            <span class="exercise-entry-index">#{{ idx + 1 }}</span>
            <button
              v-if="exercises.length > 1"
              type="button"
              class="exercise-remove-btn"
              @click="removeExercise(idx)"
            >
              删除
            </button>
          </div>
          <div class="exercise-field">
            <label class="exercise-label">动作名称</label>
            <el-input
              v-model="ex.name"
              placeholder="如：卧推、深蹲、硬拉"
              size="default"
            />
          </div>
          <div class="exercise-metrics-row">
            <div class="exercise-field">
              <label class="exercise-label">组数</label>
              <el-input-number
                v-model="ex.sets"
                :min="1"
                :max="99"
                size="default"
                controls-position="right"
              />
            </div>
            <div class="exercise-field">
              <label class="exercise-label">次数/组</label>
              <el-input-number
                v-model="ex.reps"
                :min="1"
                :max="999"
                size="default"
                controls-position="right"
              />
            </div>
            <div class="exercise-field">
              <label class="exercise-label">重量 (kg)</label>
              <el-input-number
                v-model="ex.weight"
                :min="0"
                :max="9999"
                :precision="1"
                :step="2.5"
                size="default"
                controls-position="right"
              />
            </div>
          </div>
        </div>

        <button type="button" class="exercise-add-btn" @click="addExercise">
          + 添加动作
        </button>
      </div>

      <div class="view-actions">
        <button
          type="button"
          class="view-submit-btn"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          提交训练记录
        </button>
      </div>

      <!-- Recent Training History -->
      <div class="view-history-section" v-if="recentTraining.length > 0">
        <h3 class="view-history-title">最近训练</h3>
        <div class="view-history-list">
          <div
            class="view-history-item"
            v-for="(record, idx) in recentTraining"
            :key="idx"
          >
            <span class="view-history-date">{{ record.date }}</span>
            <span class="view-history-detail">{{ record.summary }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'TrainingLogView',
  emits: ['submit', 'back'],
  props: {
    weekSummary: {
      type: Object,
      default: function () {
        return {};
      },
    },
    recentTraining: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  data() {
    return {
      exercises: [
        { name: '', sets: 4, reps: 10, weight: 0 }
      ],
    };
  },
  computed: {
    canSubmit() {
      for (var i = 0; i < this.exercises.length; i++) {
        if ((this.exercises[i].name || '').trim()) {
          return true;
        }
      }
      return false;
    },
    trendClass() {
      var trend = this.weekSummary.trend || '';
      if (trend.indexOf('上升') >= 0 || trend.indexOf('增长') >= 0) return 'trend-up';
      if (trend.indexOf('下降') >= 0 || trend.indexOf('减少') >= 0) return 'trend-down';
      return 'trend-stable';
    },
  },
  methods: {
    addExercise() {
      this.exercises.push({ name: '', sets: 4, reps: 10, weight: 0 });
    },
    removeExercise(idx) {
      this.exercises.splice(idx, 1);
    },
    handleSubmit() {
      var valid = [];
      for (var i = 0; i < this.exercises.length; i++) {
        var ex = this.exercises[i];
        if ((ex.name || '').trim()) {
          valid.push({
            name: ex.name.trim(),
            sets: ex.sets || 1,
            reps: ex.reps || 1,
            weight: ex.weight || 0,
          });
        }
      }
      if (valid.length === 0) {
        return;
      }
      this.$emit('submit', { exercises: valid, date: new Date().toISOString().slice(0, 10) });
    },
  },
};
</script>
