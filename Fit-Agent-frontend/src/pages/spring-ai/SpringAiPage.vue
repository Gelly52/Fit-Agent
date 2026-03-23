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
                    <span class="metric-item" v-if="lastTtft">
                        TTFT: <span class="metric-value">{{ lastTtft }}ms</span>
                    </span>
                    <span class="metric-item" v-if="lastExecTime">
                        耗时:
                        <span class="metric-value">{{ lastExecTime }}</span>
                    </span>
                </div>
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
            @execute-task="handleDirectTask"
            @switch-view="handleSwitchView"
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
                            <span class="context-stat-value">{{
                                docCount
                            }}</span>
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
                    <div
                        class="view-history-section"
                        v-if="uploadedDocs.length > 0"
                    >
                        <h3 class="view-history-title">已上传文档</h3>
                        <div class="view-history-list">
                            <div
                                class="view-history-item"
                                v-for="(doc, idx) in uploadedDocs"
                                :key="idx"
                            >
                                <span class="view-history-date">{{
                                    doc.date || "--"
                                }}</span>
                                <span class="view-history-detail">{{
                                    doc.name
                                }}</span>
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
import { marked } from "marked";
import ChatInputPanel from "./components/ChatInputPanel.vue";
import ChatMessageList from "./components/ChatMessageList.vue";
import UploadPanel from "./components/UploadPanel.vue";
import LeftSidebar from "./components/LeftSidebar.vue";
import RightPanel from "./components/RightPanel.vue";
import StatusBar from "./components/StatusBar.vue";
import TrainingLogView from "./components/TrainingLogView.vue";
import BodyMetricsView from "./components/BodyMetricsView.vue";
import DashboardView from "./components/DashboardView.vue";
import doctorApi from "../../services/doctorApi";
import { connectSse, closeSse } from "../../services/sseService";
import { extractSourcesFromResponse as extractSourcesFromResponseUtil } from "./utils/sourceNormalizer";

export default {
    name: "SpringAiPage",
    components: {
        ChatMessageList,
        ChatInputPanel,
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
            activeView: "chat",
            chatList: [],
            draftMessage: "",
            internetSearchSelected: false,
            knowledgeSearchSelected: false,
            agentModeSelected: true,
            imageReadSelected: false,
            isSending: false,
            isStreaming: false,
            showBackToBottom: false,
            selectedUploadName: "",
            sseState: "idle",
            guidanceMessage: "选择任务模式后，输入指令开始执行。",
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
            if (this.agentModeSelected) {
                return "Agent";
            }
            if (this.knowledgeSearchSelected) {
                return "知识库增强";
            }
            if (this.internetSearchSelected) {
                return "联网补充";
            }
            return "普通问答";
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
        var userId = Math.random().toString(36).substring(2, 12);
        this.initSSE(userId);
    },
    mounted() {
        this.scrollToBottom(true);
    },
    beforeUnmount() {
        this.teardownSSE();
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
                    me.draftMessage = me.buildTrainingPrompt(
                        formData.exercises,
                    );
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
                    if (formData.weight != null)
                        me.todayStatus.weight = formData.weight;
                    if (formData.bodyFat != null)
                        me.todayStatus.bodyFat = formData.bodyFat;
                    if (formData.sleep != null)
                        me.todayStatus.sleep = formData.sleep;
                    if (formData.fatigue)
                        me.todayStatus.fatigue = formData.fatigue;
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
                        "kg",
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
        showUiMessage(type, text) {
            if (this.$message && typeof this.$message[type] === "function") {
                this.$message[type](text);
            }
        },
        teardownSSE() {
            closeSse(this._sseConnection || this._sseSource);
            this._sseConnection = null;
            this._sseSource = null;
        },
        ensureSseConnection() {
            if (
                this._sseConnection &&
                (this._sseSource || this._sseConnection.isSupported === false)
            ) {
                return this._sseConnection;
            }

            return this.initSSE(
                this.currentUserName ||
                    Math.random().toString(36).substring(2, 12),
            );
        },
        initSSE(userId) {
            var me = this;
            var resolvedUserId =
                userId ||
                this.currentUserName ||
                Math.random().toString(36).substring(2, 12);
            var handleCustomEvent = function (event, context) {
                var eventName =
                    context && context.eventName
                        ? context.eventName
                        : "customEvent";
                console.log(eventName + "事件...");
                console.log(event && event.lastEventId);
                console.log(event && event.data);
            };

            this.currentUserName = resolvedUserId;
            this.teardownSSE();
            this.sseState = "connecting";
            this.guidanceMessage = "正在连接 SSE 实时通道，请稍候。";
            console.log("连接用户=" + resolvedUserId);

            try {
                var connection = connectSse({
                    userId: resolvedUserId,
                    onOpen: function () {
                        console.log("建立连接。。。");
                        me.sseState = "connected";
                        me.guidanceMessage = "SSE 已连接，可开始执行任务。";
                    },
                    onAdd: function (event) {
                        var receiveMsg =
                            event && event.data != null
                                ? String(event.data)
                                : "";
                        var botMsgId = me.botMsgId;
                        var targetChatItem = null;

                        // Track TTFT
                        if (me.taskStartTime && !me.lastTtft) {
                            me.lastTtft = Date.now() - me.taskStartTime;
                        }

                        console.log(receiveMsg);
                        me.isSending = false;
                        me.isStreaming = true;

                        // Update agent steps if in agent mode
                        if (me.agentModeSelected && me.agentSteps.length > 0) {
                            me.updateAgentStepsOnStream();
                        }

                        for (var i = 0; i < me.chatList.length; i++) {
                            var chatItem = me.chatList[i];
                            if (chatItem.botMsgId == botMsgId) {
                                targetChatItem = chatItem;
                                break;
                            }
                        }

                        if (!targetChatItem) {
                            me.chatList.push({
                                id: "temp-" + me.generateRandomId(8),
                                content: receiveMsg,
                                userName: "bot",
                                chatType: "bot",
                                botMsgId: botMsgId,
                                createdAt: new Date().toISOString(),
                                sources: [],
                            });
                        } else {
                            targetChatItem.content =
                                (targetChatItem.content || "") + receiveMsg;
                        }

                        me.guidanceMessage = "正在生成回答，请稍候。";
                        me.scrollToBottom();
                    },
                    onFinish: function (event) {
                        console.log("finish事件...");
                        console.log(event && event.data);

                        // Calculate exec time
                        if (me.taskStartTime) {
                            var elapsed = Date.now() - me.taskStartTime;
                            if (elapsed < 1000) {
                                me.lastExecTime = elapsed + "ms";
                            } else {
                                me.lastExecTime =
                                    (elapsed / 1000).toFixed(1) + "s";
                            }
                            me.taskStartTime = null;
                        }

                        try {
                            var chatResponse = JSON.parse(
                                (event && event.data) || "{}",
                            );
                            var message = chatResponse.message;
                            var botMsgId = chatResponse.botMsgId;
                            var normalizedSources =
                                me.extractSourcesFromResponse(chatResponse);
                            var matched = false;

                            for (var i = 0; i < me.chatList.length; i++) {
                                var chatItem = me.chatList[i];
                                if (chatItem.botMsgId == botMsgId) {
                                    chatItem.content = marked.parse(
                                        message || "",
                                    );
                                    chatItem.sources = normalizedSources;
                                    matched = true;
                                }
                            }

                            if (!matched && botMsgId) {
                                me.chatList.push({
                                    id: "temp-" + me.generateRandomId(8),
                                    content: marked.parse(message || ""),
                                    userName: "bot",
                                    chatType: "bot",
                                    botMsgId: botMsgId,
                                    createdAt: new Date().toISOString(),
                                    sources: normalizedSources,
                                });
                            }

                            // Update knowledge sources for right panel
                            if (
                                normalizedSources &&
                                normalizedSources.length > 0
                            ) {
                                me.knowledgeSources = normalizedSources;
                            }

                            // Complete agent steps
                            if (me.agentSteps.length > 0) {
                                me.completeAllAgentSteps();
                            }

                            me.guidanceMessage =
                                "本轮任务已完成，可继续发起新任务。";
                        } catch (error) {
                            console.error("解析finish事件失败:", error);
                            me.guidanceMessage =
                                "任务已结束，但结果解析失败，请稍后重试。";
                        }

                        me.botMsgId = null;
                        me.isSending = false;
                        me.isStreaming = false;
                        me.scrollToBottom();
                    },
                    onError: function (event, context) {
                        var source =
                            context && context.source
                                ? context.source
                                : me._sseSource;
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

                        // Mark agent steps as failed
                        if (me.agentSteps.length > 0) {
                            me.failCurrentAgentStep();
                        }

                        me.botMsgId = null;
                        me.isSending = false;
                        me.isStreaming = false;
                        me.sseState = "disconnected";
                        me.guidanceMessage =
                            "SSE 通道已断开，发送新任务时会自动重连。";
                        me.teardownSSE();
                    },
                    onCustomEvent: handleCustomEvent,
                    onCustomEventSnake: handleCustomEvent,
                });

                this._sseConnection = connection;
                this._sseSource =
                    connection && connection.source ? connection.source : null;

                if (!connection || connection.isSupported === false) {
                    console.log("浏览器不支持SSE");
                    this.sseState = "unsupported";
                    this.guidanceMessage =
                        "当前环境暂不支持 SSE 实时通道，回复可能无法实时展示。";
                }

                return connection;
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
                            this.showUiMessage(
                                "success",
                                "上传知识库文档成功！",
                            );
                            this.docCount = this.docCount + 1;
                            this.uploadSynced = true;
                            if (
                                params &&
                                typeof params.onSuccess === "function"
                            ) {
                                params.onSuccess(response, file);
                            }
                        } else {
                            var uploadError = new Error("上传知识库文档失败！");
                            this.guidanceMessage =
                                "文档已提交，但服务器返回异常，请稍后重试。";
                            this.showUiMessage("error", "上传知识库文档失败！");
                            if (
                                params &&
                                typeof params.onError === "function"
                            ) {
                                params.onError(uploadError);
                            }
                        }
                    }.bind(this),
                )
                .catch(
                    function (error) {
                        console.error("上传知识库文档请求失败:", error);
                        this.guidanceMessage =
                            "知识库文档上传失败，请稍后重试。";
                        this.showUiMessage(
                            "error",
                            "上传知识库文档失败，请稍后重试！",
                        );
                        if (params && typeof params.onError === "function") {
                            params.onError(error);
                        }
                    }.bind(this),
                );
        },
        extractSourcesFromResponse(chatResponse) {
            return extractSourcesFromResponseUtil(chatResponse);
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
                        }.bind(this),
                    )
                    .catch(
                        function () {
                            var copied = this.copyTextWithFallback(text);
                            if (copied) {
                                this.showUiMessage("success", "复制成功");
                            } else {
                                this.showUiMessage(
                                    "error",
                                    "复制失败，请手动复制",
                                );
                            }
                        }.bind(this),
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

            this.guidanceMessage =
                "已将所选消息引用到输入框，可继续编辑后发送。";
            this.focusInputPanel(true);
        },
        retryUserMessage(item) {
            var text = this.extractMessageText(item && item.content).trim();
            if (!text) {
                this.showUiMessage("error", "暂无可重试内容");
                return;
            }

            this.draftMessage = text;
            this.guidanceMessage =
                "已将历史任务填回输入框，可直接调整后再次发送。";
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
        doChat() {
            var currentUserName = this.currentUserName;

            if (this.isSending || this.isStreaming) {
                this.showUiMessage("error", "正在执行任务，请稍后再发送。");
                return;
            }

            var pendingMsg = (this.draftMessage || "").trim();
            if (pendingMsg === "") {
                return;
            }

            this.ensureSseConnection();
            currentUserName = this.currentUserName;

            var botMsgId = this.generateRandomId(12);
            this.botMsgId = botMsgId;
            this.isSending = true;
            this.isStreaming = false;
            this.taskStartTime = Date.now();
            this.lastTtft = null;

            // Build agent steps if in agent mode
            if (this.agentModeSelected) {
                this.agentSteps = this.buildAgentSteps(pendingMsg);
            } else {
                this.agentSteps = [];
            }

            var singleChat = {
                currentUserName: currentUserName,
                message: pendingMsg,
                botMsgId: botMsgId,
            };

            var agentModeSelected = this.agentModeSelected;
            var internetSearchSelected = this.internetSearchSelected;
            var knowledgeSearchSelected = this.knowledgeSearchSelected;
            var requestPromise = null;

            if (agentModeSelected) {
                console.log("agentExecute");
                requestPromise = doctorApi
                    .agentExecute(singleChat)
                    .catch(function () {
                        // Fallback to doChat if agent endpoint not available
                        console.log(
                            "agent endpoint not available, fallback to doChat",
                        );
                        return doctorApi.doChat(singleChat);
                    });
            } else if (knowledgeSearchSelected && !internetSearchSelected) {
                console.log("ragSearch");
                requestPromise = doctorApi.ragSearch(singleChat);
            } else if (!knowledgeSearchSelected && internetSearchSelected) {
                console.log("internetSearch");
                requestPromise = doctorApi.internetSearch(singleChat);
            } else {
                console.log("doChat");
                requestPromise = doctorApi.doChat(singleChat);
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
                    }.bind(this),
                );
            }

            this.chatList.push({
                id: "user-" + this.generateRandomId(8),
                content: pendingMsg,
                userName: currentUserName || "用户",
                chatType: "user",
                createdAt: new Date().toISOString(),
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
                    Math.floor(Math.random() * charactersLength),
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
                this.agentModeSelected = false;
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
                this.agentModeSelected = false;
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
                this.internetSearchSelected = false;
                this.knowledgeSearchSelected = false;
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
