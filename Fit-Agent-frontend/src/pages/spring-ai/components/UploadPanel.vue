<template>
  <div
    id="chat-upload-panel"
    class="upload-group"
    :class="{
      'mobile-panel-collapsed': isMobileViewport && !mobileUploadExpanded,
    }"
    v-show="!isMobileViewport || mobileUploadExpanded"
    role="region"
    aria-labelledby="upload-panel-title"
    aria-describedby="upload-panel-hint upload-file-tip"
  >
    <div class="chat-panel-heading upload-panel-heading">
      <p class="chat-panel-title" id="upload-panel-title">知识库上传区</p>
      <p class="chat-panel-hint" id="upload-panel-hint">
        点击下方上传成功后，切换“知识库搜索”即可使用文档问答。
      </p>
    </div>
    <div class="button-group">
      <el-upload
        drag
        class="course-cover-uploader"
        action=""
        accept=".txt"
        aria-label="上传知识库文档（仅支持 .txt）"
        aria-describedby="upload-panel-hint upload-file-tip"
        :http-request="handleUploadRequest"
        :show-file-list="false"
      >
        <el-icon><UploadFilled /></el-icon>
        <div class="el-upload__text">
          拖拽 .txt 文档到此 或<em>点击上传</em>
        </div>
        <template #tip>
          <div class="el-upload__tip" id="upload-file-tip">
            仅支持 .txt 文本文件，且大小不超过 10MB。
          </div>
        </template>
      </el-upload>
    </div>
  </div>
</template>

<script>
import { UploadFilled } from "@element-plus/icons-vue";

export default {
  name: "UploadPanel",
  components: {
    UploadFilled,
  },
  emits: ["upload-doc"],
  props: {
    isMobileViewport: {
      type: Boolean,
      default: false,
    },
    mobileUploadExpanded: {
      type: Boolean,
      default: true,
    },
  },
  methods: {
    handleUploadRequest(payload) {
      this.$emit("upload-doc", payload);
    },
  },
};
</script>
