<template>
  <div class="view-page body-metrics-view">
    <div class="view-header">
      <button type="button" class="view-back-btn" @click="$emit('back')">
        <span class="view-back-arrow">&larr;</span> 返回对话
      </button>
      <h2 class="view-title">记录身体指标</h2>
    </div>

    <div class="view-body">
      <div class="view-context-bar">
        <div class="context-stat">
          <span class="context-stat-value"
            >{{ todayStatus.weight || "--" }} <small>kg</small></span
          >
          <span class="context-stat-label">当前体重</span>
        </div>
        <div class="context-stat">
          <span class="context-stat-value"
            >{{ todayStatus.bodyFat || "--" }} <small>%</small></span
          >
          <span class="context-stat-label">当前体脂</span>
        </div>
        <div class="context-stat">
          <span class="context-stat-value"
            >{{ todayStatus.sleep || "--" }} <small>h</small></span
          >
          <span class="context-stat-label">睡眠时长</span>
        </div>
        <div class="context-stat">
          <span class="context-stat-value" :class="fatigueClass">{{
            todayStatus.fatigue || "--"
          }}</span>
          <span class="context-stat-label">疲劳度</span>
        </div>
      </div>

      <div class="view-form-card">
        <div class="metrics-grid">
          <div class="metrics-field">
            <label class="exercise-label">体重 (kg)</label>
            <el-input-number
              v-model="form.weight"
              :min="20"
              :max="300"
              :precision="1"
              :step="0.1"
              size="default"
              controls-position="right"
              placeholder="kg"
            />
          </div>
          <div class="metrics-field">
            <label class="exercise-label">体脂 (%)</label>
            <el-input-number
              v-model="form.bodyFat"
              :min="1"
              :max="60"
              :precision="1"
              :step="0.1"
              size="default"
              controls-position="right"
              placeholder="%"
            />
          </div>
          <div class="metrics-field">
            <label class="exercise-label">睡眠时长 (h)</label>
            <el-input-number
              v-model="form.sleep"
              :min="0"
              :max="24"
              :precision="1"
              :step="0.5"
              size="default"
              controls-position="right"
              placeholder="h"
            />
          </div>
          <div class="metrics-field">
            <label class="exercise-label">疲劳度</label>
            <el-select
              v-model="form.fatigue"
              placeholder="请选择"
              size="default"
            >
              <el-option label="低" value="低" />
              <el-option label="中" value="中" />
              <el-option label="高" value="高" />
            </el-select>
          </div>
        </div>
        <div class="metrics-field metrics-field-wide">
          <label class="exercise-label">备注</label>
          <el-input
            v-model="form.note"
            type="textarea"
            :rows="3"
            placeholder="补充说明（可选）"
          />
        </div>
      </div>

      <div class="view-actions">
        <button
          type="button"
          class="view-submit-btn"
          :disabled="!canSubmit"
          @click="handleSubmit"
        >
          提交身体指标
        </button>
      </div>

      <div class="view-history-section" v-if="recentMetrics.length > 0">
        <h3 class="view-history-title">近期变化</h3>
        <div class="view-history-list">
          <div
            class="view-history-item"
            v-for="(record, idx) in recentMetrics"
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
import { ElInput, ElInputNumber, ElOption, ElSelect } from "element-plus";
import "element-plus/es/components/input/style/css";
import "element-plus/es/components/input-number/style/css";
import "element-plus/es/components/option/style/css";
import "element-plus/es/components/select/style/css";

export default {
  name: "BodyMetricsView",
  components: {
    ElInput,
    ElInputNumber,
    ElOption,
    ElSelect,
  },
  emits: ["submit", "back"],
  props: {
    todayStatus: {
      type: Object,
      default: function () {
        return {};
      },
    },
    recentMetrics: {
      type: Array,
      default: function () {
        return [];
      },
    },
  },
  data() {
    return {
      form: {
        weight: null,
        bodyFat: null,
        sleep: null,
        fatigue: "",
        note: "",
      },
    };
  },
  computed: {
    canSubmit() {
      return this.form.weight != null || this.form.bodyFat != null;
    },
    fatigueClass() {
      var fatigue = this.todayStatus.fatigue;
      if (!fatigue) return "";
      if (fatigue === "低" || fatigue === "low") return "status-good";
      if (fatigue === "中" || fatigue === "medium") return "status-warn";
      return "status-alert";
    },
  },
  methods: {
    handleSubmit() {
      if (!this.canSubmit) {
        return;
      }
      this.$emit("submit", {
        weight: this.form.weight,
        bodyFat: this.form.bodyFat,
        sleep: this.form.sleep,
        fatigue: this.form.fatigue,
        note: this.form.note,
        date: new Date().toISOString().slice(0, 10),
      });
    },
  },
};
</script>
