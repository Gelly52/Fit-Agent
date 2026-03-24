<template>
  <div class="login-page">
    <div class="login-card">
      <p class="login-brand">Fit Agent</p>
      <h1 class="login-title">手机号登录</h1>
      <p class="login-desc">
        获取验证码后登录，登录成功会自动进入聊天页并建立专属 SSE 会话。
      </p>

      <div class="login-form-row">
        <input
          v-model.trim="loginForm.phone"
          class="login-input"
          type="text"
          maxlength="11"
          placeholder="请输入手机号"
        />
      </div>

      <div class="login-form-row login-form-row-code">
        <input
          v-model.trim="loginForm.code"
          class="login-input"
          type="text"
          maxlength="6"
          placeholder="请输入验证码"
          @keyup.enter="submitLogin"
        />
        <button
          type="button"
          class="login-action-btn secondary"
          :disabled="isCodeSending || codeCountdown > 0"
          @click="sendLoginCode"
        >
          {{ codeCountdown > 0 ? `${codeCountdown}s 后重试` : "获取验证码" }}
        </button>
      </div>

      <div class="login-form-row">
        <button
          type="button"
          class="login-action-btn primary"
          :disabled="isLoginSubmitting"
          @click="submitLogin"
        >
          {{ isLoginSubmitting ? "登录中..." : "登录并进入 Fit Agent" }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
import { ElMessage } from "element-plus";
import doctorApi from "../../services/doctorApi";
import { setToken, setUserInfo } from "../../services/http";

export default {
  name: "LoginPage",
  emits: ["login-success"],
  data() {
    return {
      loginForm: {
        phone: "",
        code: "",
      },
      isCodeSending: false,
      isLoginSubmitting: false,
      codeCountdown: 0,
      codeCountdownTimer: null,
    };
  },
  beforeUnmount() {
    this.clearCodeCountdown();
  },
  methods: {
    showUiMessage(type, text) {
      if (ElMessage && typeof ElMessage[type] === "function") {
        ElMessage[type](text);
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
    persistLoginSession(loginData) {
      if (!loginData) {
        throw new Error("登录返回数据为空");
      }
      var token = loginData.token;
      var userInfo = loginData.userInfo || {};
      var stableUserKey = userInfo.userKey || userInfo.id;
      if (!token || !stableUserKey) {
        throw new Error("登录返回缺少 token 或 userKey");
      }
      userInfo.id = stableUserKey;
      userInfo.userKey = stableUserKey;
      setToken(token);
      setUserInfo(userInfo);
      return userInfo;
    },
    startCodeCountdown(seconds) {
      this.clearCodeCountdown();
      this.codeCountdown = seconds;
      this.codeCountdownTimer = window.setInterval(
        function () {
          if (this.codeCountdown <= 1) {
            this.clearCodeCountdown();
            return;
          }
          this.codeCountdown -= 1;
        }.bind(this),
        1000
      );
    },
    clearCodeCountdown() {
      if (this.codeCountdownTimer) {
        window.clearInterval(this.codeCountdownTimer);
        this.codeCountdownTimer = null;
      }
      this.codeCountdown = 0;
    },
    sendLoginCode() {
      var phone = (this.loginForm.phone || "").trim();
      if (!/^1\d{10}$/.test(phone)) {
        this.showUiMessage("error", "请输入正确的手机号");
        return;
      }
      this.isCodeSending = true;
      doctorApi
        .sendUserCode({ phone: phone })
        .then(
          function (res) {
            this.unwrapApiData(res, "验证码发送失败");
            this.showUiMessage("success", "验证码已发送");
            this.startCodeCountdown(60);
          }.bind(this)
        )
        .catch(
          function (error) {
            this.showUiMessage(
              "error",
              error && error.message
                ? error.message
                : "验证码发送失败，请稍后重试"
            );
          }.bind(this)
        )
        .finally(
          function () {
            this.isCodeSending = false;
          }.bind(this)
        );
    },
    submitLogin() {
      var phone = (this.loginForm.phone || "").trim();
      var code = (this.loginForm.code || "").trim();
      if (!/^1\d{10}$/.test(phone)) {
        this.showUiMessage("error", "请输入正确的手机号");
        return;
      }
      if (!/^\d{4,6}$/.test(code)) {
        this.showUiMessage("error", "请输入正确的验证码");
        return;
      }
      this.isLoginSubmitting = true;
      doctorApi
        .userLogin({ phone: phone, code: code })
        .then(
          function (res) {
            var data = this.unwrapApiData(res, "登录失败");
            var userInfo = this.persistLoginSession(data);
            this.showUiMessage(
              "success",
              data && data.newUser ? "注册并登录成功" : "登录成功"
            );
            this.$emit("login-success", userInfo);
          }.bind(this)
        )
        .catch(
          function (error) {
            this.showUiMessage(
              "error",
              error && error.message
                ? error.message
                : "登录失败，请检查手机号和验证码"
            );
          }.bind(this)
        )
        .finally(
          function () {
            this.isLoginSubmitting = false;
          }.bind(this)
        );
    },
  },
};
</script>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 20px;
  background: radial-gradient(
      circle at 18% 16%,
      rgba(var(--console-accent-rgb), 0.22),
      transparent 30%
    ),
    radial-gradient(
      circle at 84% 10%,
      rgba(var(--console-accent-secondary-rgb), 0.16),
      transparent 26%
    ),
    linear-gradient(
      180deg,
      var(--console-bg) 0%,
      var(--console-surface-solid) 100%
    );
}

.login-card {
  width: 100%;
  max-width: 460px;
  padding: 36px 32px;
  border-radius: 24px;
  border: 1px solid var(--console-border);
  background: linear-gradient(
    180deg,
    var(--console-surface-solid) 0%,
    var(--console-surface) 100%
  );
  box-shadow: var(--shadow-md),
    0 24px 60px rgba(var(--console-accent-rgb), 0.08);
  backdrop-filter: var(--glass-blur);
  -webkit-backdrop-filter: var(--glass-blur);
}

.login-brand {
  margin: 0 0 12px;
  color: var(--console-accent);
  font-size: 14px;
  font-weight: 700;
  letter-spacing: 0.12em;
  text-transform: uppercase;
}

.login-title {
  margin: 0;
  color: var(--console-text);
  font-size: 32px;
  font-weight: 700;
}

.login-desc {
  margin: 12px 0 28px;
  color: var(--console-text-secondary);
  line-height: 1.7;
}

.login-form-row {
  margin-bottom: 16px;
}

.login-form-row-code {
  display: flex;
  gap: 12px;
}

.login-input {
  width: 100%;
  height: 48px;
  padding: 0 16px;
  border: 1px solid var(--console-border);
  border-radius: 14px;
  background: var(--console-surface);
  color: var(--console-text);
  outline: none;
  transition: border-color 0.2s ease, box-shadow 0.2s ease,
    background-color 0.2s ease;
}

.login-input::placeholder {
  color: var(--console-text-muted);
}

.login-input:focus {
  border-color: rgba(var(--console-accent-rgb), 0.9);
  box-shadow: 0 0 0 4px rgba(var(--console-accent-rgb), 0.14);
}

.login-action-btn {
  height: 48px;
  border: 1px solid transparent;
  border-radius: 14px;
  cursor: pointer;
  font-weight: 600;
  font-family: inherit;
  transition: transform 0.2s ease, opacity 0.2s ease, background 0.2s ease,
    border-color 0.2s ease, box-shadow 0.2s ease;
}

.login-action-btn:not(:disabled):hover {
  transform: translateY(-1px);
}

.login-action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.login-action-btn.secondary {
  min-width: 132px;
  padding: 0 16px;
  background: rgba(var(--console-accent-rgb), 0.12);
  border-color: rgba(var(--console-accent-rgb), 0.18);
  color: var(--console-accent);
}

.login-action-btn.primary {
  width: 100%;
  background: var(--console-accent-gradient);
  color: #fff;
  box-shadow: 0 16px 32px rgba(var(--console-accent-rgb), 0.2);
}

.login-action-btn.primary:not(:disabled):hover {
  box-shadow: 0 20px 40px rgba(var(--console-accent-rgb), 0.28);
}

:global([data-theme="light"]) .login-page {
  background: radial-gradient(
      circle at 18% 16%,
      rgba(var(--console-accent-rgb), 0.14),
      transparent 30%
    ),
    radial-gradient(
      circle at 84% 10%,
      rgba(var(--console-accent-secondary-rgb), 0.1),
      transparent 26%
    ),
    linear-gradient(180deg, #eef6fb 0%, var(--console-bg) 100%);
}

:global([data-theme="light"]) .login-card {
  background: linear-gradient(
    180deg,
    rgba(255, 255, 255, 0.96) 0%,
    rgba(255, 255, 255, 0.88) 100%
  );
  box-shadow: 0 24px 60px rgba(15, 23, 42, 0.12),
    0 18px 36px rgba(var(--console-accent-rgb), 0.08);
}

@media (max-width: 640px) {
  .login-card {
    padding: 28px 20px;
    border-radius: 20px;
  }

  .login-title {
    font-size: 26px;
  }

  .login-form-row-code {
    flex-direction: column;
  }

  .login-action-btn.secondary {
    width: 100%;
  }
}
</style>
