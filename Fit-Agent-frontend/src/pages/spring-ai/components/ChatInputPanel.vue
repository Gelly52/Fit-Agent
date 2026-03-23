<template>
  <div
    class="chat-input-group"
    role="region"
    aria-labelledby="chat-input-panel-title"
    aria-describedby="chat-panel-mode-hint chat-input-guidance chat-runtime-status"
  >
    <div class="chat-panel-heading">
      <p class="chat-panel-title" id="chat-input-panel-title">任务输入区</p>
      <p
        class="chat-panel-status"
        id="chat-runtime-status"
        :class="{ 'is-active': isSending || isStreaming }"
        role="status"
        aria-live="polite"
        aria-atomic="true"
      >
        <span class="chat-panel-status-dot"></span>
        {{ chatRuntimeStatusText }}
      </p>
    </div>

    <div class="search-group">
      <div
        class="checkbox-group"
        id="searchOptionRadioGroup"
        role="group"
        aria-label="任务模式切换"
      >
        <label>
          <input
            type="button"
            class="agent-mode-btn"
            :class="{ 'agent-mode-active': agentModeSelected }"
            value="Agent"
            role="button"
            :aria-pressed="agentModeSelected ? 'true' : 'false'"
            aria-label="切换 Agent 模式"
            @click="handleAgentModeToggle"
          />
        </label>
        <label>
          <input
            type="button"
            :class="{
              'search-style': !knowledgeSearchSelected,
              'search-style-selected': knowledgeSearchSelected,
            }"
            id="knowledgeSearch"
            value="知识库增强"
            role="button"
            :aria-pressed="knowledgeSearchSelected ? 'true' : 'false'"
            aria-label="切换知识库增强模式"
            @click="handleKnowledgeSearchToggle"
          />
        </label>
        <label>
          <input
            type="button"
            :class="{
              'search-style': !internetSearchSelected,
              'search-style-selected': internetSearchSelected,
            }"
            id="internetSearch"
            value="联网补充"
            role="button"
            :aria-pressed="internetSearchSelected ? 'true' : 'false'"
            aria-label="切换联网补充模式"
            @click="handleInternetSearchToggle"
          />
        </label>
      </div>
    </div>

    <div class="chat-input">
      <textarea
        ref="userInput"
        id="user-input"
        class="user-input"
        :value="modelValue"
        :placeholder="inputPlaceholderText"
        aria-label="任务输入框"
        aria-describedby="chat-input-guidance chat-runtime-status"
        autocomplete="off"
        spellcheck="false"
        :aria-busy="isSending || isStreaming ? 'true' : 'false'"
        rows="3"
        @input="updateValue"
        @keydown="handleKeyDown"
      ></textarea>
      <button
        type="button"
        id="btn-chat"
        class="btn-chat"
        :class="{ 'is-loading': isSending || isStreaming }"
        :disabled="isSending || isStreaming"
        :aria-label="
          isSending || isStreaming ? '任务执行中，请稍候' : '发送任务'
        "
        :aria-disabled="isSending || isStreaming ? 'true' : 'false'"
        @click="$emit('send')"
      >
        {{ isSending || isStreaming ? "执行中..." : "发送" }}
      </button>
    </div>
    <p
      class="chat-input-guidance"
      id="chat-input-guidance"
      :class="{ 'is-busy': isSending || isStreaming }"
      role="note"
      aria-live="polite"
    >
      {{ inputGuidanceText }}
    </p>
  </div>
</template>

<script>
export default {
  name: "ChatInputPanel",
  emits: [
    "update:modelValue",
    "send",
    "toggle-internet-search",
    "toggle-knowledge-search",
    "toggle-agent-mode",
  ],
  props: {
    modelValue: {
      type: String,
      default: "",
    },
    internetSearchSelected: {
      type: Boolean,
      default: false,
    },
    knowledgeSearchSelected: {
      type: Boolean,
      default: false,
    },
    agentModeSelected: {
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
  computed: {
    chatRuntimeStatusText() {
      if (this.isStreaming) {
        return "任务执行中，正在流式回传结果...";
      }
      if (this.isSending) {
        return "任务已提交，正在等待执行...";
      }
      return "系统就绪，可输入任务指令。";
    },
    inputPlaceholderText() {
      if (this.isStreaming) {
        return "任务执行中，可先输入下一条指令...";
      }
      if (this.isSending) {
        return "任务已提交，等待执行中...";
      }
      if (this.agentModeSelected) {
        return "请输入任务，例如：分析我这周训练并生成周报";
      }
      if (this.knowledgeSearchSelected) {
        return "输入问题，将从知识库中检索相关信息回答...";
      }
      if (this.internetSearchSelected) {
        return "输入问题，将联网搜索获取最新信息...";
      }
      return "直接描述训练目标，我会拆解并执行步骤...";
    },
    inputGuidanceText() {
      if (this.isStreaming) {
        return "Enter 发送 | Shift+Enter 换行 | 任务执行中，可先准备下一条指令。";
      }
      if (this.isSending) {
        return "Enter 发送 | Shift+Enter 换行 | 等待当前任务完成。";
      }
      return "Enter 发送 | Shift+Enter 换行 | 可输入记录训练、分析恢复、生成周报等任务。";
    },
  },
  methods: {
    updateValue(event) {
      this.$emit("update:modelValue", event.target.value);
    },
    handleKeyDown(event) {
      if (!event) {
        return;
      }

      var isEnter = event.key === "Enter" || event.keyCode === 13;
      if (!isEnter) {
        return;
      }

      var isComposing = event.isComposing || event.keyCode === 229;
      if (isComposing) {
        return;
      }

      if (event.ctrlKey || event.altKey || event.metaKey) {
        return;
      }

      if (event.shiftKey) {
        return;
      }

      event.preventDefault();
      this.$emit("send");
    },
    handleInternetSearchToggle() {
      this.$emit("toggle-internet-search", this.internetSearchSelected);
    },
    handleKnowledgeSearchToggle() {
      this.$emit("toggle-knowledge-search", this.knowledgeSearchSelected);
    },
    handleAgentModeToggle() {
      this.$emit("toggle-agent-mode", this.agentModeSelected);
    },
    focusInput() {
      var userInput = this.$refs.userInput;
      if (!userInput) {
        return;
      }

      userInput.focus();
    },
    setCursorToEnd() {
      var me = this;
      this.$nextTick(function () {
        var userInput = me.$refs.userInput;
        if (!userInput) {
          return;
        }

        userInput.focus();
        var valueLength = userInput.value.length;
        if (typeof userInput.setSelectionRange === "function") {
          userInput.setSelectionRange(valueLength, valueLength);
        }
      });
    },
  },
};
</script>
