import { ElMessage } from "element-plus";
import "element-plus/es/components/message/style/css";

export default function installElementPlus(app) {
  app.config.globalProperties.$message = ElMessage;
}

export { ElMessage };
