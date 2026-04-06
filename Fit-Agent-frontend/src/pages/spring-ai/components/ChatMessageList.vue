<template>
  <div
    class="chat-messages"
    id="chat-messages"
    @scroll="emitChatScroll"
    role="log"
    aria-live="polite"
    aria-relevant="additions text"
    aria-atomic="false"
    aria-label="任务执行与对话消息"
    tabindex="0"
  >
    <div
      id="chat-welcome-panel"
      class="chat-welcome"
      v-if="chatList.length === 0"
    >
      <div class="chat-welcome-hero">
        <div class="chat-welcome-icon">&#x1F3CB;</div>
        <h2 class="chat-welcome-title">我是Fit Agent~ 有什么可以帮你？</h2>
        <p class="chat-welcome-desc">直接描述你的目标，我来帮你拆解并执行。</p>
      </div>
      <div class="chat-welcome-prompts" aria-label="一键执行任务" role="list">
        <button
          type="button"
          class="chat-welcome-prompt"
          title="分析本周训练"
          aria-label="分析本周训练"
          @click="emitExecuteDirect('分析我这周的训练情况')"
        >
          <span class="chat-welcome-prompt-icon">&#x1F50D;</span>
          分析本周训练
        </button>
        <button
          type="button"
          class="chat-welcome-prompt"
          title="恢复状态评估"
          aria-label="恢复状态评估"
          @click="emitExecuteDirect('帮我看下最近的恢复状态')"
        >
          <span class="chat-welcome-prompt-icon">&#x1F49A;</span>
          恢复状态评估
        </button>
        <button
          type="button"
          class="chat-welcome-prompt"
          title="生成本周周报"
          aria-label="生成本周周报"
          @click="
            emitExecuteDirect('分析我这周的训练情况，生成周报发到我的邮箱')
          "
        >
          <span class="chat-welcome-prompt-icon">&#x1F4DD;</span>
          生成本周周报
        </button>
      </div>
    </div>

    <step-execution-card
      v-if="shouldShowTaskCard()"
      :steps="agentSteps"
      :thinking-content="thinkingContent"
      :is-thinking="isThinking"
      :thinking-expanded="thinkingExpanded"
      :is-sending="isSending"
      :is-streaming="isStreaming"
      @toggle-thinking="emitToggleThinking"
    />

    <template v-for="(item, index) in chatList">
      <div
        class="chat-date-separator"
        :key="'date-' + (item.id || index)"
        v-if="shouldShowDateSeparator(index)"
      >
        <span>{{ formatDateLabel(item.createdAt) }}</span>
      </div>

      <div
        class="message user"
        :key="
          'message-' + (item.chatType || 'unknown') + '-' + (item.id || index)
        "
        v-if="item.chatType === 'user'"
      >
        <div class="message-body">
          <div class="text">{{ item.content }}</div>
          <div class="message-meta message-meta-user">
            <span class="message-time">{{ formatMessageTime(item) }}</span>
            <div class="message-actions" role="group" aria-label="用户消息操作">
              <button
                type="button"
                class="message-action-btn"
                title="复制"
                aria-label="复制这条消息"
                @click="emitCopy(item)"
              >
                复制
              </button>
              <button
                type="button"
                class="message-action-btn"
                title="引用"
                aria-label="引用到输入框"
                @click="emitQuote(item)"
              >
                引用
              </button>
              <button
                type="button"
                class="message-action-btn"
                title="重试"
                aria-label="重新发送此任务"
                @click="emitRetry(item)"
              >
                重试
              </button>
            </div>
          </div>
        </div>
        <div class="user-avatar">U</div>
      </div>

      <div
        class="message bot"
        :key="
          'message-' + (item.chatType || 'unknown') + '-' + (item.id || index)
        "
        v-else
      >
        <div class="avatar">AI</div>
        <div class="message-body">
          <div class="text" v-html="item.content"></div>
          <source-card-list
            :sources="getMessageSources(item)"
          ></source-card-list>
          <div class="message-meta">
            <span class="message-time">{{ formatMessageTime(item) }}</span>
            <div class="message-actions" role="group" aria-label="AI消息操作">
              <button
                type="button"
                class="message-action-btn"
                title="复制"
                aria-label="复制这条消息"
                @click="emitCopy(item)"
              >
                复制
              </button>
              <button
                type="button"
                class="message-action-btn"
                title="引用"
                aria-label="引用到输入框"
                @click="emitQuote(item)"
              >
                引用
              </button>
            </div>
          </div>
        </div>
      </div>
    </template>

    <button
      type="button"
      class="chat-back-to-bottom"
      v-show="showBackToBottom"
      aria-label="回到底部"
      @click="emitScrollToBottom"
    >
      回到底部
    </button>
  </div>
</template>

<script>
import SourceCardList from "./SourceCardList.vue";
import StepExecutionCard from "./StepExecutionCard.vue";

export default {
  name: "ChatMessageList",
  components: {
    SourceCardList,
    StepExecutionCard,
  },
  props: {
    chatList: {
      type: Array,
      default: function () {
        return [];
      },
    },
    isSending: {
      type: Boolean,
      default: false,
    },
    isStreaming: {
      type: Boolean,
      default: false,
    },
    showBackToBottom: {
      type: Boolean,
      default: false,
    },
    isMobileViewport: {
      type: Boolean,
      default: false,
    },
    mobileInfoExpanded: {
      type: Boolean,
      default: true,
    },
    agentSteps: {
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
  },
  methods: {
    shouldShowTaskCard() {
      return (
        (this.agentSteps && this.agentSteps.length > 0) ||
        (this.thinkingContent && this.thinkingContent.length > 0) ||
        (this.isSending && !this.isStreaming)
      );
    },
    resolveMessageDate(rawValue) {
      var input = rawValue;
      if (rawValue && typeof rawValue === "object" && rawValue.createdAt) {
        input = rawValue.createdAt;
      }

      if (!input) {
        return null;
      }

      var date = input instanceof Date ? input : new Date(input);
      if (isNaN(date.getTime())) {
        return null;
      }

      return date;
    },
    getDateKey(rawValue) {
      var date = this.resolveMessageDate(rawValue);
      if (!date) {
        return "unknown";
      }

      var year = date.getFullYear();
      var month = (date.getMonth() + 1).toString().padStart(2, "0");
      var day = date.getDate().toString().padStart(2, "0");
      return year + "-" + month + "-" + day;
    },
    shouldShowDateSeparator(index) {
      if (index === 0) {
        return true;
      }

      var current = this.chatList[index];
      var previous = this.chatList[index - 1];
      var currentDateKey = this.getDateKey(current && current.createdAt);
      var previousDateKey = this.getDateKey(previous && previous.createdAt);

      return currentDateKey !== previousDateKey;
    },
    formatDateLabel(createdAt) {
      var date = this.resolveMessageDate(createdAt);
      if (!date) {
        return "今天";
      }

      var now = new Date();
      var todayDate = new Date(
        now.getFullYear(),
        now.getMonth(),
        now.getDate()
      );
      var targetDate = new Date(
        date.getFullYear(),
        date.getMonth(),
        date.getDate()
      );
      var diffDays = Math.round((todayDate - targetDate) / 86400000);

      if (diffDays === 0) {
        return "今天";
      }
      if (diffDays === 1) {
        return "昨天";
      }

      return this.getDateKey(date);
    },
    formatMessageTime(item) {
      var date = this.resolveMessageDate(item && item.createdAt);
      if (!date) {
        return "--:--";
      }

      var hours = date.getHours().toString().padStart(2, "0");
      var minutes = date.getMinutes().toString().padStart(2, "0");
      return hours + ":" + minutes;
    },
    getMessageSources(item) {
      return item && item.sources != null ? item.sources : [];
    },
    emitChatScroll() {
      this.$emit("chat-scroll");
    },
    emitCopy(item) {
      this.$emit("copy-message", item);
    },
    emitQuote(item) {
      this.$emit("quote-message", item);
    },
    emitRetry(item) {
      this.$emit("retry-message", item);
    },
    emitQuickPrompt(promptText) {
      this.$emit("apply-quick-prompt", promptText);
    },
    emitExecuteDirect(promptText) {
      this.$emit("execute-direct", promptText);
    },
    emitScrollToBottom() {
      this.$emit("scroll-to-bottom");
    },
    emitToggleThinking() {
      this.$emit("toggle-thinking");
    },
  },
};
</script>
