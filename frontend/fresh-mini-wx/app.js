import updateManager from './common/updateManager';
import { config } from './config/index';
import { ensureLogin, ensureDefaultAddress } from './services/auth/login';
import { purgeLegacyImageStorage } from './utils/localImage';

App({
  onLaunch() {
    // 清掉历史上误写入永久目录的图片，避免存储配额爆掉
    purgeLegacyImageStorage().catch(() => {});

    if (config.useFreshPay) {
      ensureLogin()
        .then(() => ensureDefaultAddress())
        .catch((err) => {
          console.warn('[Fresh] 登录失败，下单前会重试', err && err.message);
        });
    }
  },
  onShow() {
    updateManager();
  },
});
