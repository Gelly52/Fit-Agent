<template>
  <div
    class="console-workspace"
    role="main"
    aria-label="GeoGeo AI 智能健身教练控制台"
  >
    <!-- Header -->
    <header class="console-header">
      <div class="console-header-main">
        <h1 class="console-title">
          <span class="console-title-accent">Fit Agent</span>
          你的私人健身教练
        </h1>
        <p class="console-subtitle">
          训练分析、恢复建议、知识增强与健身知识库工作台
        </p>
      </div>
      <div class="console-header-side">
        <div class="mobile-sidebar-toggle">
          <button
            type="button"
            class="mobile-drawer-toggle-btn"
            :class="{ 'is-active': mobileLeftOpen }"
            @click="toggleMobileLeft"
          >
            任务
          </button>
          <button
            type="button"
            class="mobile-drawer-toggle-btn"
            :class="{ 'is-active': mobileRightOpen }"
            @click="toggleMobileRight"
          >
            结果
          </button>
        </div>
        <span class="console-mode-badge">{{ activeModeLabel }}</span>
        <div class="console-header-metrics">
          <span
            class="metric-item"
            v-if="currentUserInfo && currentUserInfo.phone"
          >
            手机号:
            <span class="metric-value">{{ currentUserInfo.phone }}</span>
          </span>
          <span class="metric-item" v-if="lastTtft">
            TTFT: <span class="metric-value">{{ lastTtft }}ms</span>
          </span>
          <span class="metric-item" v-if="lastExecTime">
            耗时:
            <span class="metric-value">{{ lastExecTime }}</span>
          </span>
        </div>
        <button
          type="button"
          class="console-logout-btn"
          :disabled="isLoggingOut"
          @click="handleLogout"
        >
          {{ isLoggingOut ? "退出中..." : "退出登录" }}
        </button>
      </div>
    </header>

    <!-- Mobile Overlay -->
    <div
      class="mobile-drawer-overlay"
      :class="{ 'is-visible': mobileLeftOpen || mobileRightOpen }"
      @click="closeMobileDrawers"
    ></div>

    <!-- Left Sidebar -->
    <left-sidebar
      :class="{ 'mobile-drawer-open': mobileLeftOpen }"
      :active-view="activeView"
      :chat-expanded="chatExpanded"
      :chat-session-list="chatSessionList"
      :active-chat-session-id="activeChatSessionId"
      :chat-records-loading="chatRecordsLoading"
      :conversation-busy="isSending || isStreaming"
      @execute-task="handleDirectTask"
      @switch-view="handleSwitchView"
      @toggle-chat-expand="handleToggleChatExpand"
      @select-chat-session="handleSelectChatSession"
      @create-chat="handleCreateChat"
    />

    <!-- Main Center -->
    <div class="console-main">
      <!-- Chat View -->
      <div
        class="chat-container"
        id="doctorPage"
        v-if="activeView === 'chat'"
        :aria-busy="isSending || isStreaming ? 'true' : 'false'"
      >
        <chat-message-list
          :chat-list="chatList"
          :is-sending="isSending"
          :is-streaming="isStreaming"
          :show-back-to-bottom="showBackToBottom"
          :is-mobile-viewport="false"
          :mobile-info-expanded="true"
          :agent-steps="agentSteps"
          @chat-scroll="handleChatScroll"
          @copy-message="copyMessageContent"
          @quote-message="quoteMessageToInput"
          @retry-message="retryUserMessage"
          @apply-quick-prompt="applyQuickPrompt"
          @execute-direct="handleDirectTask"
          @scroll-to-bottom="scrollToBottom(true)"
        />

        <div class="chat-input-container">
          <chat-input-panel
            ref="chatInputPanel"
            v-model="draftMessage"
            :internet-search-selected="internetSearchSelected"
            :knowledge-search-selected="knowledgeSearchSelected"
            :agent-mode-selected="agentModeSelected"
            :is-sending="isSending"
            :is-streaming="isStreaming"
            @send="doChat"
            @toggle-internet-search="doInternetSearch"
            @toggle-knowledge-search="doKnowledgeSearch"
            @toggle-agent-mode="doAgentMode"
          />
        </div>
      </div>

      <!-- Training Log View -->
      <training-log-view
        v-else-if="activeView === 'training-log'"
        :week-summary="weekSummary"
        :recent-training="recentTraining"
        @submit="handleTrainingSubmit"
        @back="activeView = 'chat'"
      />

      <!-- Body Metrics View -->
      <body-metrics-view
        v-else-if="activeView === 'body-metrics'"
        :today-status="todayStatus"
        :recent-metrics="recentMetrics"
        @submit="handleBodyMetricsSubmit"
        @back="activeView = 'chat'"
      />

      <!-- Upload View -->
      <div
        class="view-page upload-full-view"
        v-else-if="activeView === 'upload'"
      >
        <div class="view-header">
          <button
            type="button"
            class="view-back-btn"
            @click="activeView = 'chat'"
          >
            <span class="view-back-arrow">&larr;</span> 返回对话
          </button>
          <h2 class="view-title">知识库管理</h2>
        </div>
        <div class="view-body upload-view-body-enhanced">
          <!-- Upload Stats -->
          <div class="view-context-bar">
            <div class="context-stat">
              <span class="context-stat-value">{{ docCount }}</span>
              <span class="context-stat-label">文档总数</span>
            </div>
            <div class="context-stat">
              <span class="context-stat-value">{{
                uploadSynced ? "已同步" : "未同步"
              }}</span>
              <span class="context-stat-label">同步状态</span>
            </div>
          </div>
          <!-- Upload Panel -->
          <div class="upload-panel-wrapper">
            <upload-panel
              :is-mobile-viewport="false"
              :mobile-upload-expanded="true"
              @upload-doc="uploadDoc"
            />
          </div>
          <!-- Uploaded Docs List -->
          <div class="view-history-section" v-if="uploadedDocs.length > 0">
            <h3 class="view-history-title">已上传文档</h3>
            <div class="view-history-list">
              <div
                class="view-history-item"
                v-for="(doc, idx) in uploadedDocs"
                :key="idx"
              >
                <span class="view-history-date">{{ doc.date || "--" }}</span>
                <span class="view-history-detail">{{ doc.name }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- Dashboard View -->
      <dashboard-view
        v-else-if="activeView === 'dashboard'"
        :today-status="todayStatus"
        :week-summary="weekSummary"
        :result-summary="resultSummary"
        :report-content="reportContent"
        @back="activeView = 'chat'"
        @execute-task="handleDirectTask"
      />
    </div>

    <!-- Right Panel -->
    <right-panel
      :class="{ 'mobile-drawer-open': mobileRightOpen }"
      :active-view="activeView"
      :knowledge-sources="knowledgeSources"
      @open-upload="focusUpload"
      @open-rag-config="showUiMessage('info', 'RAG 配置功能即将上线')"
      @open-benchmark="showUiMessage('info', 'Benchmark 功能即将上线')"
      @execute-task="handleDirectTask"
    />

    <!-- Status Bar -->
    <status-bar
      :current-mode="activeModeLabel"
      :sse-state="sseState"
      :doc-count="docCount"
      :last-task-duration="lastExecTime"
      :upload-synced="uploadSynced"
    />
  </div>
</template>

<script>
import { defineAsyncComponent } from "vue";
import { marked } from "marked";
import ChatInputPanel from "./components/ChatInputPanel.vue";
import ChatMessageList from "./components/ChatMessageList.vue";
import LeftSidebar from "./components/LeftSidebar.vue";
import RightPanel from "./components/RightPanel.vue";
import StatusBar from "./components/StatusBar.vue";
import doctorApi from "../../services/doctorApi";
import { clearUserSession, getUserInfo } from "../../services/http";
import { connectSse, closeSse } from "../../services/sseService";
import { extractSourcesFromResponse as extractSourcesFromResponseUtil } from "./utils/sourceNormalizer";

const UploadPanel = defineAsyncComponent(() =>
  import("./components/UploadPanel.vue")
);
const TrainingLogView = defineAsyncComponent(() =>
  import("./components/TrainingLogView.vue")
);
const BodyMetricsView = defineAsyncComponent(() =>
  import("./components/BodyMetricsView.vue")
);
const DashboardView = defineAsyncComponent(() =>
  import("./components/DashboardView.vue")
);

export default {
  name: "SpringAiPage",
  emits: ["logout-success"],
  components: {
    ChatInputPanel,
    ChatMessageList,
    UploadPanel,
    LeftSidebar,
    RightPanel,
    StatusBar,
    TrainingLogView,
    BodyMetricsView,
    DashboardView,
  },
  data() {
    return {
      botMsgId: null,
      currentUserName: null,
      currentUserInfo: null,
      isLoggingOut: false,
      activeView: "chat",
      chatExpanded: false,
      chatSessionList: [],
      activeChatSessionId: null,
      currentSessionCode: null,
      currentSessionSceneType: null,
      chatRecordsLoading: false,
      chatRecordsLoaded: false,
      chatList: [],
      draftMessage: "",

      knowledgeSearchSelected: false,
      internetSearchSelected: false,
      agentModeSelected: true,
      imageReadSelected: false,
      isSending: false,
      isStreaming: false,
      showBackToBottom: false,
      selectedUploadName: "",
      sseState: "idle",
      guidanceMessage: "选择任务模式后，输入指令开始执行。",
      activeAgentRun: null,
      agentStepEventReceived: false,
      // Console-specific state
      mobileLeftOpen: false,
      mobileRightOpen: false,
      todayStatus: {
        weight: null,
        bodyFat: null,
        fatigue: null,
        sleep: null,
        lastMuscleGroup: null,
      },
      weekSummary: {
        trainingDays: 0,
        totalVolume: 0,
        trend: "暂无数据",
      },
      resultSummary: {},
      reportContent: "",
      knowledgeSources: [],
      agentSteps: [],
      docCount: 0,
      uploadSynced: false,
      uploadedDocs: [],
      recentTraining: [],
      recentMetrics: [],
      lastTtft: null,
      lastExecTime: "",
      taskStartTime: null,
    };
  },
  computed: {
    activeModeLabel() {
      var mainLabel = this.agentModeSelected ? "Agent" : "普通问答";
      if (this.knowledgeSearchSelected) {
        return mainLabel + " + 知识库增强";
      }
      if (this.internetSearchSelected) {
        return mainLabel + " + 联网补充";
      }
      return mainLabel;
    },
    connectionBadgeText() {
      if (this.isStreaming) {
        return "流式处理中";
      }
      if (this.isSending) {
        return "请求处理中";
      }
      if (this.sseState === "connected") {
        return "SSE 已连接";
      }
      if (this.sseState === "connecting") {
        return "SSE 连接中";
      }
      if (this.sseState === "unsupported") {
        return "SSE 不可用";
      }
      if (this.sseState === "disconnected") {
        return "SSE 已断开";
      }
      return "SSE 待初始化";
    },
    sseBadgeClass() {
      if (this.isStreaming || this.isSending) {
        return "sse-connected";
      }
      return "sse-" + this.sseState;
    },
    connectionStatusText() {
      var userText = this.currentUserName
        ? "当前会话用户：" + this.currentUserName + "。"
        : "当前会话正在初始化。";
      var uploadText = this.selectedUploadName
        ? "已选文档：" + this.selectedUploadName + "。"
        : "未选择知识库文档。";
      var sseText = "SSE 通道尚未建立。";

      if (this.sseState === "connected") {
        sseText = "SSE 通道已连接，可接收实时回复。";
      } else if (this.sseState === "connecting") {
        sseText = "SSE 通道连接中，稍后即可接收实时回复。";
      } else if (this.sseState === "unsupported") {
        sseText = "当前环境暂不支持 SSE，回复可能无法实时展示。";
      } else if (this.sseState === "disconnected") {
        sseText = "SSE 通道已断开，发送新问题时会自动重连。";
      }

      return userText + sseText + uploadText;
    },
  },
  created() {
    this._sseConnection = null;
    this._sseSource = null;
    this._sseConnectingPromise = null;
    this.loadUserSessionFromCookie();
    this.restoreActiveAgentRun();
    var stableUserKey = this.resolveStableUserKey();
    if (stableUserKey) {
      this.ensureSseConnection();
    } else {
      this.guidanceMessage = "请先完成手机号登录，再开始你的专属健身会话。";
    }
  },
  mounted() {
    this.scrollToBottom(true);
  },
  beforeUnmount() {
    this.teardownSSE({ clearPending: true });
  },
  methods: {
    toggleMobileLeft() {
      this.mobileLeftOpen = !this.mobileLeftOpen;
      if (this.mobileLeftOpen) {
        this.mobileRightOpen = false;
      }
    },
    toggleMobileRight() {
      this.mobileRightOpen = !this.mobileRightOpen;
      if (this.mobileRightOpen) {
        this.mobileLeftOpen = false;
      }
    },
    closeMobileDrawers() {
      this.mobileLeftOpen = false;
      this.mobileRightOpen = false;
    },
    handleDirectTask(prompt) {
      this.activeView = "chat";
      this.closeMobileDrawers();
      this.draftMessage = prompt;
      var me = this;
      this.$nextTick(function () {
        me.doChat();
      });
    },
    handleSwitchView(viewName) {
      this.activeView = viewName;
      this.closeMobileDrawers();
      // Fetch contextual data for the target view
      if (viewName === "training-log") {
        this.fetchRecentTraining();
      } else if (viewName === "body-metrics") {
        this.fetchRecentMetrics();
      } else if (viewName === "upload") {
        this.fetchUploadedDocs();
      }
    },
    async handleToggleChatExpand() {
      this.activeView = "chat";
      if (this.chatExpanded) {
        this.chatExpanded = false;
        return;
      }

      this.chatExpanded = true;
      if (!this.chatRecordsLoaded && !this.chatRecordsLoading) {
        await this.fetchChatRecords();
      }
    },
    handleSelectChatSession(sessionId) {
      if (this.isSending || this.isStreaming || this.hasPendingAgentRun()) {
        this.showUiMessage(
          "error",
          "当前有运行中的任务，请稍后再切换聊天记录。"
        );
        return;
      }

      var targetSession = null;
      for (var i = 0; i < this.chatSessionList.length; i++) {
        if (this.chatSessionList[i].sessionId == sessionId) {
          targetSession = this.chatSessionList[i];
          break;
        }
      }

      if (!targetSession) {
        return;
      }

      var mappedChatList = this.mapSessionMessagesToChatList(
        targetSession.messages
      );

      this.clearActiveAgentRun();
      this.activeView = "chat";
      this.activeChatSessionId = targetSession.sessionId;
      this.currentSessionCode = targetSession.sessionCode || null;
      this.currentSessionSceneType = targetSession.sceneType || null;
      this.applyChatMode(this.resolvePreferredModeFromSession(targetSession));
      this.chatList = mappedChatList;
      this.agentSteps = [];
      this.botMsgId = null;
      this.showBackToBottom = false;
      this.knowledgeSources = this.resolveChatHistorySources(mappedChatList);
      this.closeMobileDrawers();
      this.scrollToBottom(true);
    },
    handleCreateChat() {
      if (this.isSending || this.isStreaming || this.hasPendingAgentRun()) {
        this.showUiMessage("error", "当前有运行中的任务，请稍后再新建聊天。");
        return;
      }

      this.clearActiveAgentRun();
      this.activeView = "chat";
      this.activeChatSessionId = null;
      this.currentSessionCode = null;
      this.currentSessionSceneType = null;
      this.chatList = [];
      this.agentSteps = [];
      this.draftMessage = "";
      this.botMsgId = null;
      this.showBackToBottom = false;
      this.knowledgeSources = [];
      this.closeMobileDrawers();
      this.scrollToBottom(true);
    },
    handleTrainingSubmit(formData) {
      var me = this;
      doctorApi
        .logTraining(formData)
        .then(function (res) {
          me.showUiMessage("success", "训练记录已保存");
          me.activeView = "chat";
        })
        .catch(function () {
          // API unavailable, fallback to agent chat
          me.activeView = "chat";
          me.draftMessage = me.buildTrainingPrompt(formData.exercises);
          me.$nextTick(function () {
            me.doChat();
          });
        });
    },
    handleBodyMetricsSubmit(formData) {
      var me = this;
      doctorApi
        .logBodyMetrics(formData)
        .then(function (res) {
          me.showUiMessage("success", "身体指标已保存");
          me.activeView = "chat";
          // sync todayStatus
          if (formData.weight != null) me.todayStatus.weight = formData.weight;
          if (formData.bodyFat != null)
            me.todayStatus.bodyFat = formData.bodyFat;
          if (formData.sleep != null) me.todayStatus.sleep = formData.sleep;
          if (formData.fatigue) me.todayStatus.fatigue = formData.fatigue;
        })
        .catch(function () {
          me.activeView = "chat";
          me.draftMessage = me.buildBodyMetricsPrompt(formData);
          me.$nextTick(function () {
            me.doChat();
          });
        });
    },
    buildTrainingPrompt(exercises) {
      var lines = ["记录今天训练："];
      for (var i = 0; i < exercises.length; i++) {
        var ex = exercises[i];
        lines.push(
          "- " +
            ex.name +
            "：" +
            ex.sets +
            "组 x " +
            ex.reps +
            "次，" +
            ex.weight +
            "kg"
        );
      }
      return lines.join("\n");
    },
    buildBodyMetricsPrompt(data) {
      var parts = ["记录今天身体指标："];
      if (data.weight != null) parts.push("体重 " + data.weight + "kg");
      if (data.bodyFat != null) parts.push("体脂 " + data.bodyFat + "%");
      if (data.sleep != null) parts.push("睡眠 " + data.sleep + "小时");
      if (data.fatigue) parts.push("疲劳度 " + data.fatigue);
      if (data.note) parts.push("备注：" + data.note);
      return parts.join("，");
    },
    focusUpload() {
      this.activeView = "upload";
      this.fetchUploadedDocs();
    },
    fetchRecentTraining() {
      var me = this;
      doctorApi
        .getRecentTraining(5)
        .then(function (res) {
          var data = res && res.data;
          if (Array.isArray(data)) {
            me.recentTraining = data;
          }
        })
        .catch(function () {
          // API not available, keep empty
        });
    },
    fetchRecentMetrics() {
      var me = this;
      doctorApi
        .getRecentMetrics(5)
        .then(function (res) {
          var data = res && res.data;
          if (Array.isArray(data)) {
            me.recentMetrics = data;
          }
        })
        .catch(function () {
          // API not available, keep empty
        });
    },
    fetchUploadedDocs() {
      var me = this;
      doctorApi
        .getUploadedDocs()
        .then(function (res) {
          var data = res && res.data;
          if (Array.isArray(data)) {
            me.uploadedDocs = data;
          }
        })
        .catch(function () {
          // API not available, keep empty
        });
    },
    fetchChatRecords() {
      var stableUserKey = this.resolveStableUserKey();
      if (!stableUserKey) {
        this.chatSessionList = [];
        this.chatRecordsLoaded = false;
        return Promise.resolve([]);
      }

      var me = this;
      this.chatRecordsLoading = true;
      return doctorApi
        .getRecords(stableUserKey)
        .then(function (res) {
          var data = me.unwrapApiData(res, "加载聊天记录失败");
          me.chatSessionList = me.normalizeChatSessions(data);
          me.chatRecordsLoaded = true;
          return me.chatSessionList;
        })
        .catch(function (error) {
          console.error("加载聊天记录失败:", error);
          me.chatSessionList = [];
          me.chatRecordsLoaded = false;
          me.showUiMessage(
            "error",
            error && error.message
              ? error.message
              : "加载聊天记录失败，请稍后重试。"
          );
          return [];
        })
        .finally(function () {
          me.chatRecordsLoading = false;
        });
    },
    normalizeChatSessions(recordData) {
      var sessions = [];
      if (Array.isArray(recordData)) {
        sessions = recordData.slice();
      } else if (recordData && Array.isArray(recordData.sessions)) {
        sessions = recordData.sessions.slice();
      }

      var me = this;
      return sessions
        .map(function (session) {
          var messages =
            session && Array.isArray(session.messages)
              ? session.messages.slice()
              : [];
          var updatedAt =
            (session && (session.updatedAt || session.createdAt)) ||
            (messages.length > 0
              ? messages[messages.length - 1].createdAt || null
              : null);

          return {
            sessionId: session ? session.sessionId : null,
            sessionCode:
              session && session.sessionCode
                ? String(session.sessionCode)
                : null,
            sceneType:
              session && session.sceneType
                ? String(session.sceneType).toLowerCase()
                : null,
            lastBotMsgId:
              session && session.lastBotMsgId
                ? session.lastBotMsgId
                : me.resolveLastSessionBotMsgId(messages),
            title: me.buildChatSessionTitle(session, messages),
            updatedAt: updatedAt,
            updatedAtLabel: me.formatChatSessionTime(updatedAt),
            messages: messages,
          };
        })
        .filter(function (session) {
          return session.sessionId != null;
        })
        .sort(function (a, b) {
          var aDate = me.resolveChatSessionDate(a.updatedAt);
          var bDate = me.resolveChatSessionDate(b.updatedAt);
          var aTime = aDate ? aDate.getTime() : 0;
          var bTime = bDate ? bDate.getTime() : 0;
          return bTime - aTime;
        });
    },
    buildChatSessionTitle(session, messages) {
      var explicitTitle =
        session && session.title ? String(session.title).trim() : "";
      if (explicitTitle) {
        return explicitTitle;
      }

      var safeMessages = Array.isArray(messages) ? messages : [];
      for (var i = 0; i < safeMessages.length; i++) {
        var message = safeMessages[i];
        if (!message || message.role !== "user") {
          continue;
        }

        var content =
          message.content == null
            ? ""
            : String(message.content).replace(/\s+/g, " ").trim();
        if (!content) {
          continue;
        }

        return content.length > 18 ? content.slice(0, 18) + "..." : content;
      }

      return "未命名会话";
    },
    formatChatSessionTime(rawValue) {
      var date = this.resolveChatSessionDate(rawValue);
      if (!date) {
        return "";
      }

      var month = String(date.getMonth() + 1).padStart(2, "0");
      var day = String(date.getDate()).padStart(2, "0");
      var hours = String(date.getHours()).padStart(2, "0");
      var minutes = String(date.getMinutes()).padStart(2, "0");
      return month + "-" + day + " " + hours + ":" + minutes;
    },
    resolveChatSessionDate(rawValue) {
      if (!rawValue) {
        return null;
      }

      var date = rawValue instanceof Date ? rawValue : new Date(rawValue);
      if (isNaN(date.getTime())) {
        return null;
      }

      return date;
    },
    resolveLastSessionBotMsgId(messages) {
      if (!Array.isArray(messages)) {
        return null;
      }

      for (var i = messages.length - 1; i >= 0; i--) {
        if (messages[i] && messages[i].botMsgId) {
          return messages[i].botMsgId;
        }
      }

      return null;
    },
    resolvePreferredModeFromSession(session) {
      var sceneType =
        session && session.sceneType === "agent" ? "agent" : "chat";
      var sourceType = "chat";

      var messages =
        session && Array.isArray(session.messages) ? session.messages : [];
      for (var i = messages.length - 1; i >= 0; i--) {
        var message = messages[i];
        var currentSourceType =
          message && message.sourceType
            ? String(message.sourceType).toLowerCase()
            : "";

        if (
          currentSourceType === "rag" ||
          currentSourceType === "internet" ||
          currentSourceType === "chat"
        ) {
          sourceType = currentSourceType;
          break;
        }
      }

      return {
        sceneType: sceneType,
        sourceType: sourceType,
      };
    },
    applyChatMode(modeState) {
      var sceneType =
        modeState && modeState.sceneType
          ? String(modeState.sceneType).toLowerCase()
          : "chat";
      var sourceType =
        modeState && modeState.sourceType
          ? String(modeState.sourceType).toLowerCase()
          : "chat";

      this.agentModeSelected = sceneType === "agent";
      this.imageReadSelected = false;
      this.knowledgeSearchSelected = sourceType === "rag";
      this.internetSearchSelected = sourceType === "internet";
    },
    resolveExpectedSessionSceneType() {
      return this.agentModeSelected ? "agent" : "chat";
    },
    applyServerSessionMeta(payload, fallbackSceneType) {
      if (!payload || typeof payload !== "object") {
        return;
      }

      if (payload.chatSessionId != null) {
        this.activeChatSessionId = payload.chatSessionId;
      }

      if (payload.sessionCode) {
        this.currentSessionCode = String(payload.sessionCode);
      }

      var nextSceneType =
        payload.sceneType != null && payload.sceneType !== ""
          ? String(payload.sceneType).toLowerCase()
          : fallbackSceneType
          ? String(fallbackSceneType).toLowerCase()
          : null;
      if (nextSceneType) {
        this.currentSessionSceneType = nextSceneType;
      }
    },
    refreshChatRecordsIfNeeded() {
      if (
        (!this.chatExpanded && !this.chatRecordsLoaded) ||
        this.chatRecordsLoading
      ) {
        return Promise.resolve(this.chatSessionList);
      }

      return this.fetchChatRecords();
    },
    showUiMessage(type, text) {
      if (this.$message && typeof this.$message[type] === "function") {
        this.$message[type](text);
      }
    },
    unwrapApiData(res, fallbackMsg) {
      if (!res) {
        throw new Error(fallbackMsg || "请求失败");
      }
      if (typeof res.status !== "undefined" && res.status !== 200) {
        throw new Error(res.msg || fallbackMsg || "请求失败");
      }
      return typeof res.data === "undefined" ? res : res.data;
    },
    loadUserSessionFromCookie() {
      var userInfo = getUserInfo();
      if (!userInfo) {
        this.currentUserInfo = null;
        this.currentUserName = null;
        return;
      }
      this.currentUserInfo = userInfo;
      this.currentUserName = userInfo.userKey || userInfo.id || null;
    },
    resolveStableUserKey() {
      if (this.currentUserInfo) {
        return this.currentUserInfo.userKey || this.currentUserInfo.id || null;
      }
      return this.currentUserName || null;
    },
    getActiveAgentRunStorageKey() {
      var stableUserKey = this.resolveStableUserKey();
      if (!stableUserKey || typeof window === "undefined") {
        return null;
      }
      return "fit-agent:active-run:" + String(stableUserKey);
    },
    normalizeAgentRunStatus(status) {
      var normalized =
        status == null ? "pending" : String(status).toLowerCase();
      if (normalized === "completed") {
        return "success";
      }
      if (normalized === "error") {
        return "failed";
      }
      if (
        normalized === "pending" ||
        normalized === "running" ||
        normalized === "success" ||
        normalized === "failed"
      ) {
        return normalized;
      }
      return "pending";
    },
    normalizeAgentStepStatus(status) {
      var normalized =
        status == null ? "pending" : String(status).toLowerCase();
      if (normalized === "success" || normalized === "completed") {
        return "completed";
      }
      if (normalized === "failed" || normalized === "error") {
        return "failed";
      }
      if (normalized === "running") {
        return "running";
      }
      return "pending";
    },
    isTerminalAgentRunStatus(status) {
      var normalized = this.normalizeAgentRunStatus(status);
      return normalized === "success" || normalized === "failed";
    },
    hasPendingAgentRun() {
      return !!(
        this.activeAgentRun &&
        this.activeAgentRun.runId != null &&
        !this.isTerminalAgentRunStatus(this.activeAgentRun.status)
      );
    },
    normalizeAgentStepItem(step, index) {
      if (!step) {
        return null;
      }
      var rawStepNo = step.stepNo != null ? Number(step.stepNo) : index + 1;
      var stepNo = isNaN(rawStepNo) ? index + 1 : rawStepNo;
      var label = step.label || step.stepName || "步骤" + stepNo;
      return {
        id: step.id || "agent-step-" + stepNo,
        stepNo: stepNo,
        label: label,
        stepName: step.stepName || label,
        status: this.normalizeAgentStepStatus(
          step.status != null ? step.status : step.stepStatus
        ),
        message: step.message || null,
      };
    },
    buildActiveAgentRunSnapshot() {
      if (!this.activeAgentRun || this.activeAgentRun.runId == null) {
        return null;
      }
      var steps = [];
      if (Array.isArray(this.agentSteps)) {
        for (var i = 0; i < this.agentSteps.length; i++) {
          var normalizedStep = this.normalizeAgentStepItem(
            this.agentSteps[i],
            i
          );
          if (normalizedStep) {
            steps.push(normalizedStep);
          }
        }
      }
      return {
        runId: this.activeAgentRun.runId,
        chatSessionId: this.activeAgentRun.chatSessionId || null,
        sessionCode: this.activeAgentRun.sessionCode || null,
        botMsgId: this.activeAgentRun.botMsgId || this.botMsgId || null,
        status: this.normalizeAgentRunStatus(this.activeAgentRun.status),
        requestText: this.activeAgentRun.requestText || "",
        sceneType: this.activeAgentRun.sceneType || "agent",
        sourceType: this.activeAgentRun.sourceType || "chat",
        finishReceived: !!this.activeAgentRun.finishReceived,
        steps: steps,
        lastUpdatedAt: new Date().toISOString(),
      };
    },
    snapshotActiveAgentRun() {
      var key = this.getActiveAgentRunStorageKey();
      if (!key || typeof window === "undefined") {
        return;
      }
      var snapshot = this.buildActiveAgentRunSnapshot();
      if (!snapshot || this.isTerminalAgentRunStatus(snapshot.status)) {
        window.sessionStorage.removeItem(key);
        return;
      }
      window.sessionStorage.setItem(key, JSON.stringify(snapshot));
    },
    clearActiveAgentRun(options) {
      var key = this.getActiveAgentRunStorageKey();
      if (key && typeof window !== "undefined") {
        window.sessionStorage.removeItem(key);
      }
      this.activeAgentRun = null;
      this.agentStepEventReceived = false;
      if (!options || options.clearSteps !== false) {
        this.agentSteps = [];
      }
    },
    restoreActiveAgentRun() {
      var key = this.getActiveAgentRunStorageKey();
      if (!key || typeof window === "undefined") {
        return;
      }
      var rawValue = window.sessionStorage.getItem(key);
      if (!rawValue) {
        return;
      }
      var snapshot = this.safeParseJson(rawValue);
      if (!snapshot || typeof snapshot !== "object" || snapshot.runId == null) {
        window.sessionStorage.removeItem(key);
        return;
      }
      if (this.isTerminalAgentRunStatus(snapshot.status)) {
        window.sessionStorage.removeItem(key);
        return;
      }

      var steps = [];
      if (Array.isArray(snapshot.steps)) {
        for (var i = 0; i < snapshot.steps.length; i++) {
          var normalizedStep = this.normalizeAgentStepItem(
            snapshot.steps[i],
            i
          );
          if (normalizedStep) {
            steps.push(normalizedStep);
          }
        }
      }

      this.activeAgentRun = {
        runId: snapshot.runId,
        chatSessionId:
          snapshot.chatSessionId != null ? snapshot.chatSessionId : null,
        sessionCode: snapshot.sessionCode ? String(snapshot.sessionCode) : null,
        botMsgId: snapshot.botMsgId ? String(snapshot.botMsgId) : null,
        status: this.normalizeAgentRunStatus(snapshot.status),
        requestText: snapshot.requestText || "",
        sceneType: snapshot.sceneType || "agent",
        sourceType: snapshot.sourceType || "chat",
        steps: steps,
        finishReceived: !!snapshot.finishReceived,
      };
      this.agentStepEventReceived = steps.length > 0;
      this.activeView = "chat";
      this.agentModeSelected = true;
      if (this.activeAgentRun.chatSessionId != null) {
        this.activeChatSessionId = this.activeAgentRun.chatSessionId;
      }
      if (this.activeAgentRun.sessionCode) {
        this.currentSessionCode = this.activeAgentRun.sessionCode;
      }
      this.currentSessionSceneType = this.activeAgentRun.sceneType || "agent";
      if (this.activeAgentRun.botMsgId) {
        this.botMsgId = this.activeAgentRun.botMsgId;
      }
      if (steps.length > 0) {
        this.agentSteps = steps;
      } else if (this.activeAgentRun.requestText) {
        this.agentSteps = this.buildAgentSteps(this.activeAgentRun.requestText);
      }
      this.silentRestoreChatSession(this.activeAgentRun.chatSessionId);
      this.silentFetchAgentRunDetail(this.activeAgentRun.runId);
    },
    safeParseJson(rawValue) {
      if (rawValue == null) {
        return null;
      }
      if (typeof rawValue === "object") {
        return rawValue;
      }
      try {
        return JSON.parse(String(rawValue));
      } catch (error) {
        return null;
      }
    },
    isAgentRunQueryUnavailable(error) {
      var status =
        error && error.response && error.response.status != null
          ? error.response.status
          : null;
      return status === 404 || status === 405 || status === 501;
    },
    silentRestoreChatSession(sessionId) {
      if (sessionId == null) {
        return Promise.resolve(null);
      }
      var stableUserKey = this.resolveStableUserKey();
      if (!stableUserKey) {
        return Promise.resolve(null);
      }
      var me = this;
      return doctorApi
        .getRecords(stableUserKey, sessionId, 1)
        .then(function (res) {
          var data = me.unwrapApiData(res, "加载聊天记录失败");
          var sessions = me.normalizeChatSessions(data);
          if (!Array.isArray(sessions) || sessions.length === 0) {
            return null;
          }
          var targetSession = sessions[0];
          var mappedChatList = me.mapSessionMessagesToChatList(
            targetSession.messages
          );
          me.activeView = "chat";
          me.activeChatSessionId = targetSession.sessionId;
          me.currentSessionCode =
            targetSession.sessionCode || me.currentSessionCode;
          me.currentSessionSceneType =
            targetSession.sceneType || me.currentSessionSceneType;
          me.chatList = mappedChatList;
          me.knowledgeSources = me.resolveChatHistorySources(mappedChatList);
          me.scrollToBottom(true);
          return targetSession;
        })
        .catch(function (error) {
          console.warn("静默恢复聊天会话失败:", error);
          return null;
        });
    },
    applyAgentRunDetail(detail) {
      if (!detail || typeof detail !== "object") {
        return;
      }
      var runId = detail.runId != null ? detail.runId : null;
      var normalizedStatus = this.normalizeAgentRunStatus(detail.status);
      if (!this.activeAgentRun) {
        this.activeAgentRun = {
          runId: runId,
          chatSessionId: null,
          sessionCode: null,
          botMsgId: null,
          status: normalizedStatus,
          requestText: "",
          sceneType: "agent",
          sourceType: "chat",
          steps: [],
          finishReceived: false,
        };
      }
      if (runId != null) {
        this.activeAgentRun.runId = runId;
      }
      if (detail.chatSessionId != null) {
        this.activeAgentRun.chatSessionId = detail.chatSessionId;
      }
      if (detail.sessionCode) {
        this.activeAgentRun.sessionCode = String(detail.sessionCode);
      }
      if (detail.botMsgId) {
        this.activeAgentRun.botMsgId = String(detail.botMsgId);
      }
      if (detail.requestText) {
        this.activeAgentRun.requestText = detail.requestText;
      }
      this.activeAgentRun.status = normalizedStatus;
      this.activeAgentRun.sceneType = "agent";
      if (detail.sourceType) {
        this.activeAgentRun.sourceType = String(
          detail.sourceType
        ).toLowerCase();
      }

      var steps = [];
      if (Array.isArray(detail.steps)) {
        for (var i = 0; i < detail.steps.length; i++) {
          var normalizedStep = this.normalizeAgentStepItem(detail.steps[i], i);
          if (normalizedStep) {
            steps.push(normalizedStep);
          }
        }
      }
      if (steps.length > 0) {
        this.agentSteps = steps;
        this.agentStepEventReceived = true;
        this.activeAgentRun.steps = steps;
      }

      this.applyServerSessionMeta(detail, "agent");
      if (this.activeAgentRun.chatSessionId != null) {
        this.activeChatSessionId = this.activeAgentRun.chatSessionId;
      }
      if (this.activeAgentRun.sessionCode) {
        this.currentSessionCode = this.activeAgentRun.sessionCode;
      }
      if (this.activeAgentRun.botMsgId) {
        this.botMsgId = this.activeAgentRun.botMsgId;
      }
      this.currentSessionSceneType = "agent";
      this.snapshotActiveAgentRun();
      if (this.isTerminalAgentRunStatus(normalizedStatus)) {
        this.guidanceMessage =
          normalizedStatus === "success"
            ? "本轮任务已完成，可继续发起新任务。"
            : "任务执行失败，请调整后重试。";
      }
    },
    silentFetchAgentRunDetail(runId) {
      if (runId == null) {
        return Promise.resolve(null);
      }
      var me = this;
      return doctorApi
        .getAgentRunDetail(runId)
        .then(function (res) {
          var data = me.unwrapApiData(res, "加载运行详情失败");
          me.applyAgentRunDetail(data);
          return data;
        })
        .catch(function (error) {
          if (me.isAgentRunQueryUnavailable(error)) {
            return null;
          }
          console.warn("静默加载Agent运行详情失败:", error);
          return null;
        });
    },
    applyAgentExecuteAck(
      payload,
      requestPayload,
      expectedSceneType,
      sourceType
    ) {
      if (!payload || payload.runId == null) {
        return false;
      }
      var steps = [];
      if (Array.isArray(this.agentSteps)) {
        for (var i = 0; i < this.agentSteps.length; i++) {
          var normalizedStep = this.normalizeAgentStepItem(
            this.agentSteps[i],
            i
          );
          if (normalizedStep) {
            steps.push(normalizedStep);
          }
        }
      }
      this.activeAgentRun = {
        runId: payload.runId,
        chatSessionId:
          payload.chatSessionId != null ? payload.chatSessionId : null,
        sessionCode: payload.sessionCode ? String(payload.sessionCode) : null,
        botMsgId: payload.botMsgId
          ? String(payload.botMsgId)
          : requestPayload && requestPayload.botMsgId
          ? String(requestPayload.botMsgId)
          : null,
        status: this.normalizeAgentRunStatus(payload.status),
        requestText:
          requestPayload && requestPayload.message
            ? requestPayload.message
            : "",
        sceneType: expectedSceneType || "agent",
        sourceType: sourceType || "chat",
        steps: steps,
        finishReceived: false,
      };
      this.agentStepEventReceived = false;
      this.applyServerSessionMeta(payload, expectedSceneType || "agent");
      this.currentSessionSceneType = expectedSceneType || "agent";
      if (this.activeAgentRun.botMsgId) {
        this.botMsgId = this.activeAgentRun.botMsgId;
      }
      this.snapshotActiveAgentRun();
      this.silentFetchAgentRunDetail(payload.runId);
      return true;
    },
    normalizeAddPayload(rawValue) {
      var parsed = this.safeParseJson(rawValue);
      if (parsed && typeof parsed === "object" && !Array.isArray(parsed)) {
        var chunkText = parsed.contentChunk;
        if (chunkText == null) {
          chunkText = parsed.delta;
        }
        if (chunkText == null) {
          chunkText = parsed.content;
        }
        if (chunkText == null) {
          chunkText = parsed.message;
        }
        if (chunkText == null) {
          chunkText = parsed.text;
        }
        return {
          chunkText: chunkText == null ? "" : String(chunkText),
          botMsgId: parsed.botMsgId ? String(parsed.botMsgId) : null,
          runId: parsed.runId != null ? parsed.runId : null,
          chatSessionId:
            parsed.chatSessionId != null ? parsed.chatSessionId : null,
          sessionCode: parsed.sessionCode ? String(parsed.sessionCode) : null,
          sceneType: parsed.sceneType
            ? String(parsed.sceneType).toLowerCase()
            : null,
          sourceType: parsed.sourceType
            ? String(parsed.sourceType).toLowerCase()
            : null,
        };
      }
      return {
        chunkText: rawValue == null ? "" : String(rawValue),
        botMsgId: null,
        runId: null,
        chatSessionId: null,
        sessionCode: null,
        sceneType: null,
        sourceType: null,
      };
    },
    upsertStreamingBotMessage(payload) {
      if (!payload) {
        return;
      }
      if (
        payload.runId != null &&
        this.activeAgentRun &&
        this.activeAgentRun.runId != null &&
        String(payload.runId) !== String(this.activeAgentRun.runId)
      ) {
        return;
      }
      var receiveMsg =
        payload.chunkText == null ? "" : String(payload.chunkText);
      if (!receiveMsg) {
        return;
      }
      var botMsgId =
        payload.botMsgId ||
        (this.activeAgentRun && this.activeAgentRun.botMsgId) ||
        this.botMsgId;
      if (!botMsgId) {
        return;
      }
      if (this.taskStartTime && !this.lastTtft) {
        this.lastTtft = Date.now() - this.taskStartTime;
      }
      this.isSending = false;
      this.isStreaming = true;
      this.guidanceMessage = "正在生成回答，请稍候。";

      if (
        this.agentModeSelected &&
        this.agentSteps.length > 0 &&
        !this.agentStepEventReceived
      ) {
        this.updateAgentStepsOnStream();
      }

      var sessionMeta = {
        chatSessionId:
          payload.chatSessionId != null
            ? payload.chatSessionId
            : this.activeAgentRun && this.activeAgentRun.chatSessionId != null
            ? this.activeAgentRun.chatSessionId
            : null,
        sessionCode:
          payload.sessionCode ||
          (this.activeAgentRun && this.activeAgentRun.sessionCode) ||
          this.currentSessionCode ||
          null,
        sceneType:
          payload.sceneType ||
          this.currentSessionSceneType ||
          this.resolveExpectedSessionSceneType(),
      };
      this.applyServerSessionMeta(sessionMeta, sessionMeta.sceneType);

      if (this.activeAgentRun) {
        this.activeAgentRun.botMsgId = botMsgId;
        if (payload.chatSessionId != null) {
          this.activeAgentRun.chatSessionId = payload.chatSessionId;
        }
        if (payload.sessionCode) {
          this.activeAgentRun.sessionCode = payload.sessionCode;
        }
        if (payload.runId != null) {
          this.activeAgentRun.runId = payload.runId;
        }
        if (!this.isTerminalAgentRunStatus(this.activeAgentRun.status)) {
          this.activeAgentRun.status = "running";
        }
        this.snapshotActiveAgentRun();
      }

      var targetChatItem = null;
      for (var i = 0; i < this.chatList.length; i++) {
        var chatItem = this.chatList[i];
        if (chatItem.botMsgId == botMsgId) {
          targetChatItem = chatItem;
          break;
        }
      }

      if (!targetChatItem) {
        this.chatList.push({
          id: "temp-" + this.generateRandomId(8),
          content: receiveMsg,
          userName: "bot",
          chatType: "bot",
          botMsgId: botMsgId,
          createdAt: new Date().toISOString(),
          sources: [],
          sourceType: payload.sourceType || null,
          sessionCode: sessionMeta.sessionCode,
          sceneType: sessionMeta.sceneType,
        });
      } else {
        targetChatItem.content = (targetChatItem.content || "") + receiveMsg;
        targetChatItem.sessionCode =
          targetChatItem.sessionCode || sessionMeta.sessionCode || null;
        targetChatItem.sceneType =
          targetChatItem.sceneType || sessionMeta.sceneType || null;
        targetChatItem.sourceType =
          targetChatItem.sourceType || payload.sourceType || null;
      }
      this.scrollToBottom();
    },
    handleAgentCustomEvent(rawValue) {
      var payload = this.safeParseJson(rawValue);
      if (!payload || typeof payload !== "object" || Array.isArray(payload)) {
        return;
      }
      if (
        payload.runId != null &&
        this.activeAgentRun &&
        this.activeAgentRun.runId != null &&
        String(payload.runId) !== String(this.activeAgentRun.runId)
      ) {
        return;
      }
      var rawStepNo = payload.stepNo != null ? Number(payload.stepNo) : null;
      var normalizedStep = this.normalizeAgentStepItem(
        {
          stepNo: isNaN(rawStepNo) ? this.agentSteps.length + 1 : rawStepNo,
          stepName: payload.stepName || payload.label || payload.name,
          label: payload.label || payload.stepName || payload.name,
          stepStatus:
            payload.stepStatus != null ? payload.stepStatus : payload.status,
          message: payload.message || null,
        },
        isNaN(rawStepNo) ? this.agentSteps.length : rawStepNo - 1
      );
      if (!normalizedStep) {
        return;
      }
      if (!this.activeAgentRun && payload.runId != null) {
        this.activeAgentRun = {
          runId: payload.runId,
          chatSessionId: null,
          sessionCode: this.currentSessionCode || null,
          botMsgId: this.botMsgId || null,
          status: "running",
          requestText: this.draftMessage || "",
          sceneType: "agent",
          sourceType: "chat",
          steps: [],
          finishReceived: false,
        };
      }
      this.applyAgentStepEvent(normalizedStep, payload);
    },
    applyAgentStepEvent(stepEvent, eventPayload) {
      if (!stepEvent) {
        return;
      }
      this.agentStepEventReceived = true;
      var matchedIndex = -1;
      for (var i = 0; i < this.agentSteps.length; i++) {
        var currentStep = this.agentSteps[i];
        if (
          currentStep &&
          currentStep.stepNo != null &&
          stepEvent.stepNo != null &&
          Number(currentStep.stepNo) === Number(stepEvent.stepNo)
        ) {
          matchedIndex = i;
          break;
        }
      }
      if (matchedIndex >= 0) {
        this.agentSteps[matchedIndex] = Object.assign(
          {},
          this.agentSteps[matchedIndex],
          stepEvent
        );
      } else {
        this.agentSteps.push(stepEvent);
      }
      this.agentSteps = this.agentSteps
        .slice()
        .sort(function (a, b) {
          var aStepNo = a && a.stepNo != null ? Number(a.stepNo) : 999;
          var bStepNo = b && b.stepNo != null ? Number(b.stepNo) : 999;
          return aStepNo - bStepNo;
        })
        .map(
          function (item, index) {
            return this.normalizeAgentStepItem(item, index);
          }.bind(this)
        )
        .filter(function (item) {
          return !!item;
        });

      if (!this.activeAgentRun) {
        this.activeAgentRun = {
          runId:
            eventPayload && eventPayload.runId != null
              ? eventPayload.runId
              : null,
          chatSessionId: null,
          sessionCode: this.currentSessionCode || null,
          botMsgId: this.botMsgId || null,
          status: "running",
          requestText: "",
          sceneType: "agent",
          sourceType: "chat",
          steps: [],
          finishReceived: false,
        };
      }
      if (eventPayload) {
        if (eventPayload.runId != null) {
          this.activeAgentRun.runId = eventPayload.runId;
        }
        if (eventPayload.chatSessionId != null) {
          this.activeAgentRun.chatSessionId = eventPayload.chatSessionId;
        }
        if (eventPayload.sessionCode) {
          this.activeAgentRun.sessionCode = String(eventPayload.sessionCode);
        }
        if (eventPayload.botMsgId) {
          this.activeAgentRun.botMsgId = String(eventPayload.botMsgId);
        }
      }
      this.activeAgentRun.steps = this.agentSteps.slice();
      if (stepEvent.status === "failed") {
        this.activeAgentRun.status = "failed";
        this.guidanceMessage =
          stepEvent.message || "任务执行失败，请调整后重试。";
        this.isSending = false;
        this.isStreaming = false;
      } else if (stepEvent.stepNo != null && Number(stepEvent.stepNo) >= 5) {
        this.activeAgentRun.status = "success";
        this.guidanceMessage = "本轮任务已完成，可继续发起新任务。";
        this.isSending = false;
        this.isStreaming = false;
        this.botMsgId = null;
      } else if (!this.isTerminalAgentRunStatus(this.activeAgentRun.status)) {
        this.activeAgentRun.status = "running";
      }
      this.snapshotActiveAgentRun();
    },
    applyFinishPayload(chatResponse) {
      var payload =
        chatResponse && typeof chatResponse === "object"
          ? chatResponse
          : { message: chatResponse == null ? "" : String(chatResponse) };
      var message = payload.message == null ? "" : String(payload.message);
      var botMsgId =
        payload.botMsgId ||
        (this.activeAgentRun && this.activeAgentRun.botMsgId) ||
        this.botMsgId;
      var normalizedSources = this.extractSourcesFromResponse(payload);
      var matched = false;
      this.applyServerSessionMeta(
        payload,
        this.currentSessionSceneType || this.resolveExpectedSessionSceneType()
      );

      for (var i = 0; i < this.chatList.length; i++) {
        var chatItem = this.chatList[i];
        if (chatItem.botMsgId == botMsgId) {
          chatItem.content = marked.parse(message || "");
          chatItem.sources = normalizedSources;
          chatItem.sourceType =
            payload.sourceType || chatItem.sourceType || null;
          chatItem.sessionCode =
            payload.sessionCode || chatItem.sessionCode || null;
          chatItem.sceneType = payload.sceneType || chatItem.sceneType || null;
          matched = true;
        }
      }

      if (!matched && botMsgId) {
        this.chatList.push({
          id: "temp-" + this.generateRandomId(8),
          content: marked.parse(message || ""),
          userName: "bot",
          chatType: "bot",
          botMsgId: botMsgId,
          createdAt: new Date().toISOString(),
          sources: normalizedSources,
          sourceType: payload.sourceType || null,
          sessionCode: payload.sessionCode || this.currentSessionCode || null,
          sceneType: payload.sceneType || this.currentSessionSceneType || null,
        });
      }

      this.knowledgeSources =
        normalizedSources && normalizedSources.length > 0
          ? normalizedSources
          : [];

      if (this.activeAgentRun) {
        if (payload.chatSessionId != null) {
          this.activeAgentRun.chatSessionId = payload.chatSessionId;
        }
        if (payload.sessionCode) {
          this.activeAgentRun.sessionCode = String(payload.sessionCode);
        }
        if (botMsgId) {
          this.activeAgentRun.botMsgId = String(botMsgId);
        }
        this.activeAgentRun.finishReceived = true;
        if (this.normalizeAgentRunStatus(payload.status) === "failed") {
          this.activeAgentRun.status = "failed";
          if (this.agentSteps.length > 0) {
            this.failCurrentAgentStep();
          }
          this.guidanceMessage = "任务执行失败，请稍后重试。";
        } else if (!this.isTerminalAgentRunStatus(this.activeAgentRun.status)) {
          this.activeAgentRun.status = "running";
          this.guidanceMessage = "任务结果已返回，正在同步执行状态。";
        }
        this.snapshotActiveAgentRun();
        this.silentFetchAgentRunDetail(
          payload.runId != null ? payload.runId : this.activeAgentRun.runId
        );
      } else if (this.agentModeSelected && this.agentSteps.length > 0) {
        this.completeAllAgentSteps();
        this.guidanceMessage = "本轮任务已完成，可继续发起新任务。";
      } else {
        this.guidanceMessage = "本轮任务已完成，可继续发起新任务。";
      }

      this.refreshChatRecordsIfNeeded();
      this.botMsgId = null;
      this.isSending = false;
      this.isStreaming = false;
      this.scrollToBottom();
    },
    handleLogout() {
      if (this.isLoggingOut) {
        return;
      }

      this.isLoggingOut = true;
      doctorApi
        .userLogout()
        .then(
          function (res) {
            this.unwrapApiData(res, "退出登录失败");
            this.showUiMessage("success", "已退出登录");
          }.bind(this)
        )
        .catch(
          function (error) {
            this.showUiMessage(
              "error",
              error && error.message
                ? error.message
                : "退出登录失败，已清理本地登录态"
            );
          }.bind(this)
        )
        .finally(
          function () {
            this.teardownSSE({ clearPending: true });
            this.clearActiveAgentRun();
            clearUserSession();
            this.currentUserInfo = null;
            this.currentUserName = null;
            this.isLoggingOut = false;
            this.$emit("logout-success");
          }.bind(this)
        );
    },
    teardownSSE(options) {
      closeSse(this._sseConnection || this._sseSource);
      this._sseConnection = null;
      this._sseSource = null;
      if (options && options.clearPending === true) {
        this._sseConnectingPromise = null;
      }
    },
    async ensureSseConnection() {
      if (
        this._sseConnection &&
        (this._sseSource || this._sseConnection.isSupported === false)
      ) {
        return this._sseConnection;
      }

      var stableUserKey = this.resolveStableUserKey();
      if (!stableUserKey) {
        return null;
      }

      if (this._sseConnectingPromise) {
        return this._sseConnectingPromise;
      }

      this._sseConnectingPromise = this.initSSE();

      try {
        return await this._sseConnectingPromise;
      } finally {
        this._sseConnectingPromise = null;
      }
    },
    async initSSE() {
      var me = this;
      var resolvedUserKey = this.resolveStableUserKey();
      if (!resolvedUserKey) {
        this.sseState = "idle";
        this.guidanceMessage = "请先完成手机号登录，再建立实时会话通道。";
        return null;
      }
      var handleCustomEvent = function (event, context) {
        var eventName =
          context && context.eventName ? context.eventName : "customEvent";
        console.log(eventName + "事件...");
        console.log(event && event.lastEventId);
        console.log(event && event.data);
        me.handleAgentCustomEvent(event && event.data);
      };

      this.currentUserName = resolvedUserKey;
      this.teardownSSE();
      this.sseState = "connecting";
      this.guidanceMessage = "正在连接 SSE 实时通道，请稍候。";
      console.log("连接用户=" + resolvedUserKey);

      try {
        var ticketResponse = await doctorApi.createSseTicket();
        var ticketData = this.unwrapApiData(
          ticketResponse,
          "获取 SSE 连接票据失败"
        );
        var ticket =
          ticketData && ticketData.ticket != null
            ? String(ticketData.ticket)
            : "";

        if (!ticket) {
          throw new Error("SSE 连接票据为空");
        }

        return await new Promise(function (resolve) {
          var settled = false;
          var connection = null;
          var connectTimeout = null;
          var settle = function (value) {
            if (settled) {
              return;
            }
            settled = true;
            if (connectTimeout) {
              clearTimeout(connectTimeout);
            }
            resolve(value);
          };

          connection = connectSse({
            ticket: ticket,
            onOpen: function () {
              console.log("建立连接。。。");
              me.sseState = "connected";
              me.guidanceMessage = "SSE 已连接，可开始执行任务。";
              settle(connection);
            },
            onAdd: function (event) {
              var payload = me.normalizeAddPayload(
                event && event.data != null ? event.data : ""
              );
              me.upsertStreamingBotMessage(payload);
            },
            onFinish: function (event) {
              console.log("finish事件...");
              console.log(event && event.data);

              if (me.taskStartTime) {
                var elapsed = Date.now() - me.taskStartTime;
                if (elapsed < 1000) {
                  me.lastExecTime = elapsed + "ms";
                } else {
                  me.lastExecTime = (elapsed / 1000).toFixed(1) + "s";
                }
                me.taskStartTime = null;
              }

              try {
                var payload =
                  me.safeParseJson((event && event.data) || "") ||
                  (event && event.data) ||
                  "";
                me.applyFinishPayload(payload);
              } catch (error) {
                console.error("解析finish事件失败:", error);
                me.guidanceMessage = "任务已结束，但结果解析失败，请稍后重试。";
                me.botMsgId = null;
                me.isSending = false;
                me.isStreaming = false;
              }

              me.scrollToBottom();
            },
            onError: function (event, context) {
              var source =
                context && context.source ? context.source : me._sseSource;
              var readyState =
                source && typeof source.readyState !== "undefined"
                  ? source.readyState
                  : "unknown";

              console.log("error事件...");
              console.log("e.readyState: " + readyState);

              if (
                typeof EventSource !== "undefined" &&
                source &&
                source.readyState === EventSource.CLOSED
              ) {
                console.log("connection is closed");
              } else {
                console.log("Error occurred", event);
              }

              if (me.isSending || me.isStreaming) {
                me.showUiMessage("error", "响应中断，请稍后重试！");
              }

              if (me.agentSteps.length > 0) {
                me.failCurrentAgentStep();
              }
              if (
                me.activeAgentRun &&
                !me.isTerminalAgentRunStatus(me.activeAgentRun.status)
              ) {
                me.activeAgentRun.status = "failed";
                me.snapshotActiveAgentRun();
              }

              me.botMsgId = null;
              me.isSending = false;
              me.isStreaming = false;
              me.sseState = "disconnected";
              me.guidanceMessage = "SSE 通道已断开，发送新任务时会自动重连。";
              me.teardownSSE();
              settle(null);
            },
            onCustomEvent: handleCustomEvent,
            onCustomEventSnake: handleCustomEvent,
          });

          me._sseConnection = connection;
          me._sseSource =
            connection && connection.source ? connection.source : null;

          if (!connection || connection.isSupported === false) {
            console.log("浏览器不支持SSE");
            me.sseState = "unsupported";
            me.guidanceMessage =
              "当前环境暂不支持 SSE 实时通道，回复可能无法实时展示。";
            settle(connection);
            return;
          }

          connectTimeout = setTimeout(function () {
            settle(connection);
          }, 3000);
        });
      } catch (error) {
        console.error("建立SSE连接失败:", error);
        this.sseState = "disconnected";
        this.guidanceMessage =
          "SSE 通道初始化失败，发送新任务时会自动重试连接。";
        this._sseConnection = null;
        this._sseSource = null;
        return null;
      }
    },
    uploadDoc(params) {
      var file = params && params.file ? params.file : null;
      if (!file) {
        return;
      }

      this.selectedUploadName = file.name || "";
      this.guidanceMessage = "正在上传知识库文档，请稍候。";

      var formData = new FormData();
      formData.append("file", file);

      return doctorApi
        .uploadRagDoc(formData)
        .then(
          function (response) {
            console.log(response);
            if (response.status == 200) {
              this.guidanceMessage =
                "知识库文档上传成功，可切换到知识库增强模式继续提问。";
              this.showUiMessage("success", "上传知识库文档成功！");
              this.docCount = this.docCount + 1;
              this.uploadSynced = true;
              if (params && typeof params.onSuccess === "function") {
                params.onSuccess(response, file);
              }
            } else {
              var uploadError = new Error("上传知识库文档失败！");
              this.guidanceMessage =
                "文档已提交，但服务器返回异常，请稍后重试。";
              this.showUiMessage("error", "上传知识库文档失败！");
              if (params && typeof params.onError === "function") {
                params.onError(uploadError);
              }
            }
          }.bind(this)
        )
        .catch(
          function (error) {
            console.error("上传知识库文档请求失败:", error);
            this.guidanceMessage = "知识库文档上传失败，请稍后重试。";
            this.showUiMessage("error", "上传知识库文档失败，请稍后重试！");
            if (params && typeof params.onError === "function") {
              params.onError(error);
            }
          }.bind(this)
        );
    },
    extractSourcesFromResponse(chatResponse) {
      return extractSourcesFromResponseUtil(chatResponse);
    },
    parseRecordSources(sourcesJson) {
      if (!sourcesJson) {
        return [];
      }

      var parsedSources = sourcesJson;
      if (typeof sourcesJson === "string") {
        try {
          parsedSources = JSON.parse(sourcesJson);
        } catch (error) {
          return [];
        }
      }

      if (Array.isArray(parsedSources)) {
        return extractSourcesFromResponseUtil({ sources: parsedSources });
      }

      if (parsedSources && typeof parsedSources === "object") {
        var normalizedCollection =
          parsedSources.sources ||
          parsedSources.items ||
          parsedSources.list ||
          parsedSources.references ||
          parsedSources.citations ||
          parsedSources.docs ||
          parsedSources.sourceList ||
          parsedSources.sourceDocs;

        if (Array.isArray(normalizedCollection)) {
          return extractSourcesFromResponseUtil({
            sources: normalizedCollection,
          });
        }

        return extractSourcesFromResponseUtil({ sources: [parsedSources] });
      }

      if (typeof parsedSources === "string") {
        return extractSourcesFromResponseUtil({ sources: [parsedSources] });
      }

      return [];
    },
    mapSessionMessagesToChatList(messages) {
      if (!Array.isArray(messages)) {
        return [];
      }

      var me = this;
      return messages
        .slice()
        .sort(function (a, b) {
          var aSeq = a && a.seqNo != null ? Number(a.seqNo) : NaN;
          var bSeq = b && b.seqNo != null ? Number(b.seqNo) : NaN;
          if (!isNaN(aSeq) && !isNaN(bSeq) && aSeq !== bSeq) {
            return aSeq - bSeq;
          }

          var aDate = me.resolveChatSessionDate(a && a.createdAt);
          var bDate = me.resolveChatSessionDate(b && b.createdAt);
          var aTime = aDate ? aDate.getTime() : 0;
          var bTime = bDate ? bDate.getTime() : 0;
          return aTime - bTime;
        })
        .map(function (message, index) {
          return me.mapRecordToChatItem(message, index);
        })
        .filter(function (item) {
          return !!item;
        });
    },
    mapRecordToChatItem(message, index) {
      if (!message) {
        return null;
      }

      var role = message.role === "assistant" ? "assistant" : "user";
      var rawContent = message.content == null ? "" : String(message.content);
      return {
        id:
          message.messageId != null
            ? String(message.messageId)
            : "record-" + index + "-" + this.generateRandomId(6),
        content: role === "assistant" ? marked.parse(rawContent) : rawContent,
        userName: role === "assistant" ? "bot" : this.currentUserName || "用户",
        chatType: role === "assistant" ? "bot" : "user",
        botMsgId: message.botMsgId || null,
        createdAt: message.createdAt || new Date().toISOString(),
        sessionCode: message.sessionCode || null,
        sceneType: message.sceneType || null,
        sourceType: message.sourceType || null,
        sources:
          role === "assistant"
            ? this.parseRecordSources(message.sourcesJson)
            : [],
      };
    },
    resolveChatHistorySources(chatItems) {
      if (!Array.isArray(chatItems)) {
        return [];
      }

      for (var i = chatItems.length - 1; i >= 0; i--) {
        var item = chatItems[i];
        if (item && Array.isArray(item.sources) && item.sources.length > 0) {
          return item.sources;
        }
      }

      return [];
    },
    handleChatScroll() {
      var chatMessages = document.getElementById("chat-messages");
      if (!chatMessages) {
        return;
      }

      var distanceFromBottom =
        chatMessages.scrollHeight -
        (chatMessages.scrollTop + chatMessages.clientHeight);
      this.showBackToBottom = distanceFromBottom > 120;
    },
    extractMessageText(content) {
      if (content == null) {
        return "";
      }

      var contentString = String(content);
      if (contentString.indexOf("<") === -1) {
        return contentString;
      }

      var tempDiv = document.createElement("div");
      tempDiv.innerHTML = contentString;
      return (tempDiv.textContent || tempDiv.innerText || "").trim();
    },
    copyTextWithFallback(text) {
      var textarea = document.createElement("textarea");
      textarea.value = text;
      textarea.setAttribute("readonly", "readonly");
      textarea.style.position = "fixed";
      textarea.style.top = "-9999px";
      textarea.style.left = "-9999px";

      document.body.appendChild(textarea);
      textarea.focus();
      textarea.select();

      var copied = false;
      try {
        copied = document.execCommand("copy");
      } catch (error) {
        copied = false;
      }

      document.body.removeChild(textarea);
      return copied;
    },
    copyMessageContent(item) {
      var text = this.extractMessageText(item && item.content).trim();
      if (!text) {
        this.showUiMessage("error", "暂无可复制内容");
        return;
      }

      if (
        typeof navigator !== "undefined" &&
        navigator.clipboard &&
        typeof window !== "undefined" &&
        window.isSecureContext
      ) {
        navigator.clipboard
          .writeText(text)
          .then(
            function () {
              this.showUiMessage("success", "复制成功");
            }.bind(this)
          )
          .catch(
            function () {
              var copied = this.copyTextWithFallback(text);
              if (copied) {
                this.showUiMessage("success", "复制成功");
              } else {
                this.showUiMessage("error", "复制失败，请手动复制");
              }
            }.bind(this)
          );
        return;
      }

      var copied = this.copyTextWithFallback(text);
      if (copied) {
        this.showUiMessage("success", "复制成功");
      } else {
        this.showUiMessage("error", "复制失败，请手动复制");
      }
    },
    focusInputPanel(moveCursorToEnd) {
      var panel = this.$refs.chatInputPanel;
      if (!panel) {
        return;
      }

      if (moveCursorToEnd && typeof panel.setCursorToEnd === "function") {
        panel.setCursorToEnd();
        return;
      }

      if (typeof panel.focusInput === "function") {
        panel.focusInput();
      }
    },
    quoteMessageToInput(item) {
      var text = this.extractMessageText(item && item.content).trim();
      if (!text) {
        this.showUiMessage("error", "暂无可引用内容");
        return;
      }

      var quotedText = "引用：\n" + text;
      var currentValue = this.draftMessage || "";
      if (currentValue) {
        currentValue = currentValue.replace(/\s+$/, "");
        this.draftMessage = currentValue + "\n\n" + quotedText;
      } else {
        this.draftMessage = quotedText;
      }

      this.guidanceMessage = "已将所选消息引用到输入框，可继续编辑后发送。";
      this.focusInputPanel(true);
    },
    retryUserMessage(item) {
      var text = this.extractMessageText(item && item.content).trim();
      if (!text) {
        this.showUiMessage("error", "暂无可重试内容");
        return;
      }

      this.draftMessage = text;
      this.guidanceMessage = "已将历史任务填回输入框，可直接调整后再次发送。";
      this.focusInputPanel(true);
    },
    applyQuickPrompt(promptText) {
      this.draftMessage = promptText || "";
      this.guidanceMessage = "已写入快捷指令，可直接调整内容后发送。";
      this.focusInputPanel(true);
    },
    // Agent steps management
    buildAgentSteps(message) {
      var steps = [];
      var msg = (message || "").toLowerCase();

      if (
        msg.indexOf("分析") >= 0 ||
        msg.indexOf("周报") >= 0 ||
        msg.indexOf("总结") >= 0
      ) {
        steps.push({
          id: "s1",
          label: "解析任务意图",
          status: "running",
        });
        steps.push({
          id: "s2",
          label: "查询训练记录",
          status: "pending",
        });
        steps.push({
          id: "s3",
          label: "查询身体指标",
          status: "pending",
        });
        steps.push({
          id: "s4",
          label: "多维训练分析",
          status: "pending",
        });
        if (msg.indexOf("知识") >= 0 || this.knowledgeSearchSelected) {
          steps.push({
            id: "s5",
            label: "检索健身知识库",
            status: "pending",
          });
        }
        if (msg.indexOf("周报") >= 0 || msg.indexOf("报告") >= 0) {
          steps.push({
            id: "s6",
            label: "生成周报",
            status: "pending",
          });
        }
        if (msg.indexOf("邮") >= 0 || msg.indexOf("发送") >= 0) {
          steps.push({
            id: "s7",
            label: "发送邮件",
            status: "pending",
          });
        }
        steps.push({
          id: "s-final",
          label: "流式回传结果",
          status: "pending",
        });
      } else if (
        msg.indexOf("记录") >= 0 &&
        (msg.indexOf("训练") >= 0 ||
          msg.indexOf("身体") >= 0 ||
          msg.indexOf("体重") >= 0)
      ) {
        steps.push({
          id: "s1",
          label: "解析任务意图",
          status: "running",
        });
        steps.push({
          id: "s2",
          label: "提取数据参数",
          status: "pending",
        });
        steps.push({
          id: "s3",
          label: "写入训练记录",
          status: "pending",
        });
        steps.push({
          id: "s-final",
          label: "确认并回传结果",
          status: "pending",
        });
      } else if (
        msg.indexOf("恢复") >= 0 ||
        msg.indexOf("疲劳") >= 0 ||
        msg.indexOf("累") >= 0
      ) {
        steps.push({
          id: "s1",
          label: "解析任务意图",
          status: "running",
        });
        steps.push({
          id: "s2",
          label: "查询近期训练",
          status: "pending",
        });
        steps.push({
          id: "s3",
          label: "分析恢复状态",
          status: "pending",
        });
        steps.push({
          id: "s4",
          label: "检索恢复建议",
          status: "pending",
        });
        steps.push({
          id: "s-final",
          label: "生成建议报告",
          status: "pending",
        });
      } else {
        steps.push({
          id: "s1",
          label: "解析任务意图",
          status: "running",
        });
        steps.push({ id: "s2", label: "执行任务", status: "pending" });
        steps.push({
          id: "s-final",
          label: "回传结果",
          status: "pending",
        });
      }

      return steps;
    },
    updateAgentStepsOnStream() {
      var foundRunning = false;
      for (var i = 0; i < this.agentSteps.length; i++) {
        var step = this.agentSteps[i];
        if (step.status === "running") {
          step.status = "completed";
          if (i + 1 < this.agentSteps.length) {
            this.agentSteps[i + 1].status = "running";
          }
          foundRunning = true;
          break;
        }
      }
      if (!foundRunning) {
        // If last step, mark the final as running
        var lastStep = this.agentSteps[this.agentSteps.length - 1];
        if (lastStep && lastStep.status === "pending") {
          lastStep.status = "running";
        }
      }
    },
    completeAllAgentSteps() {
      for (var i = 0; i < this.agentSteps.length; i++) {
        this.agentSteps[i].status = "completed";
      }
    },
    failCurrentAgentStep() {
      for (var i = 0; i < this.agentSteps.length; i++) {
        if (this.agentSteps[i].status === "running") {
          this.agentSteps[i].status = "failed";
          break;
        }
      }
    },
    async doChat() {
      var currentUserName = this.currentUserName;

      if (this.isSending || this.isStreaming) {
        this.showUiMessage("error", "正在执行任务，请稍后再发送。");
        return;
      }

      var pendingMsg = (this.draftMessage || "").trim();
      if (pendingMsg === "") {
        return;
      }

      this.isSending = true;
      this.isStreaming = false;
      this.guidanceMessage = "正在建立 SSE 实时通道，请稍候。";

      var sseConnection = null;
      try {
        sseConnection = await this.ensureSseConnection();
      } catch (error) {
        console.error("准备 SSE 连接失败:", error);
      }
      currentUserName = this.currentUserName;

      if (!currentUserName) {
        this.showUiMessage(
          "error",
          "请先完成手机号登录，再开始你的专属健身会话。"
        );
        this.isSending = false;
        this.guidanceMessage = "请先完成手机号登录，再开始你的专属健身会话。";
        return;
      }

      if (!sseConnection) {
        this.showUiMessage("error", "SSE 通道初始化失败，请稍后重试。");
        this.isSending = false;
        this.guidanceMessage = "SSE 通道初始化失败，请稍后重试。";
        return;
      }

      var botMsgId = this.generateRandomId(12);
      this.botMsgId = botMsgId;
      this.taskStartTime = Date.now();
      this.lastTtft = null;

      if (this.agentModeSelected) {
        this.agentSteps = this.buildAgentSteps(pendingMsg);
      } else {
        this.agentSteps = [];
      }

      var agentModeSelected = this.agentModeSelected;
      var internetSearchSelected = this.internetSearchSelected;
      var knowledgeSearchSelected = this.knowledgeSearchSelected;
      var currentSourceType = knowledgeSearchSelected
        ? "rag"
        : internetSearchSelected
        ? "internet"
        : "chat";
      var expectedSceneType = this.resolveExpectedSessionSceneType();
      var singleChat = {
        currentUserName: currentUserName,
        message: pendingMsg,
        botMsgId: botMsgId,
        sessionCode: this.currentSessionCode || null,
        ragEnabled: knowledgeSearchSelected,
        internetEnabled: internetSearchSelected,
      };

      var requestPromise = null;
      var me = this;

      if (agentModeSelected) {
        console.log("agentExecute");
        requestPromise = doctorApi.agentExecute(singleChat).catch(function () {
          console.log("agent endpoint not available, fallback to doChat");
          return doctorApi.doChat(singleChat);
        });
      } else {
        console.log("doChat");
        requestPromise = doctorApi.doChat(singleChat);
      }

      if (requestPromise && typeof requestPromise.then === "function") {
        requestPromise = requestPromise.then(function (res) {
          var data = me.unwrapApiData(res, "任务请求失败，请稍后重试！");
          var ackApplied = false;
          me.applyServerSessionMeta(data, expectedSceneType);
          if (agentModeSelected) {
            ackApplied = me.applyAgentExecuteAck(
              data,
              singleChat,
              expectedSceneType,
              currentSourceType
            );
            if (!ackApplied) {
              me.clearActiveAgentRun();
            }
          }
          return res;
        });
      }

      if (requestPromise && typeof requestPromise.catch === "function") {
        requestPromise.catch(
          function (error) {
            console.error("任务请求失败:", error);
            this.showUiMessage("error", "请求失败，请稍后重试！");
            this.botMsgId = null;
            this.isSending = false;
            this.isStreaming = false;
            this.guidanceMessage = "任务发送失败，请稍后重试。";
            this.failCurrentAgentStep();
          }.bind(this)
        );
      }

      this.chatList.push({
        id: "user-" + this.generateRandomId(8),
        content: pendingMsg,
        userName: currentUserName || "用户",
        chatType: "user",
        createdAt: new Date().toISOString(),
        sessionCode: this.currentSessionCode || null,
        sceneType: expectedSceneType,
        sourceType: currentSourceType,
      });

      this.draftMessage = "";
      this.guidanceMessage = "任务已提交，正在执行中。";
      this.scrollToBottom();
    },
    generateRandomId(length) {
      var characters =
        "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
      var result = "";
      var charactersLength = characters.length;
      for (var i = 0; i < length; i++) {
        result += characters.charAt(
          Math.floor(Math.random() * charactersLength)
        );
      }
      return result;
    },
    scrollToBottom(force) {
      var me = this;
      this.$nextTick(function () {
        var chatMessages = document.getElementById("chat-messages");
        if (!chatMessages) {
          return;
        }

        var distanceFromBottom =
          chatMessages.scrollHeight -
          (chatMessages.scrollTop + chatMessages.clientHeight);
        var nearBottom = distanceFromBottom <= 120;

        if (force === true || nearBottom || !me.showBackToBottom) {
          chatMessages.scrollTop = chatMessages.scrollHeight;
        }

        me.handleChatScroll();
      });
    },
    doInternetSearch(internetSearchSelected) {
      this.internetSearchSelected = !internetSearchSelected;

      if (this.internetSearchSelected) {
        this.knowledgeSearchSelected = false;
        this.imageReadSelected = false;
      }

      this.guidanceMessage =
        "\u5DF2\u5207\u6362\u4E3A\u300C" +
        this.activeModeLabel +
        "\u300D\u6A21\u5F0F\u3002";
    },
    doKnowledgeSearch(knowledgeSearchSelected) {
      this.knowledgeSearchSelected = !knowledgeSearchSelected;

      if (this.knowledgeSearchSelected) {
        this.internetSearchSelected = false;
        this.imageReadSelected = false;
      }

      this.guidanceMessage =
        "\u5DF2\u5207\u6362\u4E3A\u300C" +
        this.activeModeLabel +
        "\u300D\u6A21\u5F0F\u3002";
    },
    doAgentMode(agentModeSelected) {
      this.agentModeSelected = !agentModeSelected;

      if (this.agentModeSelected) {
        this.imageReadSelected = false;
      }

      this.guidanceMessage =
        "\u5DF2\u5207\u6362\u4E3A\u300C" +
        this.activeModeLabel +
        "\u300D\u6A21\u5F0F\u3002";
    },
  },
};
</script>
