/**
 * 微信基础库 3.x：普通 http:// 不能直接给 <image>。
 * 策略：只用 wx.downloadFile → 临时路径（系统会回收），绝不写 USER_DATA_PATH，
 * 避免「maximum size of the file storage limit is exceeded」。
 */
import { config } from '../config/index';

const memCache = Object.create(null);
const pending = Object.create(null);
const PLACEHOLDER =
  'https://tdesign.gtimg.com/miniprogram/template/retail/goods/nz-09a.png';

let purgedOnce = false;

/** 是否需要桥接 */
export function isLocalHttpUrl(url) {
  const src = (url || '').trim();
  if (!src) return false;
  if (src.indexOf('data:') === 0) return false;
  if (src.indexOf('wxfile://') === 0) return false;
  if (src.indexOf('http://tmp') === 0) return false;
  if (/\/api\/goods\/upload\//i.test(src) && !/^https:\/\//i.test(src)) return true;
  if (/^https:\/\//i.test(src)) return false;
  return /^http:\/\//i.test(src);
}

function normalizeToGatewayHost(url) {
  const src = (url || '').trim();
  if (!/^http:\/\/(127\.0\.0\.1|localhost)(:\d+)?\//i.test(src)) {
    return src;
  }
  const base = (config.baseURL || 'http://localhost:8080').replace(/\/$/, '');
  const m = base.match(/^(https?:\/\/[^/]+)/i);
  const origin = m ? m[1] : 'http://localhost:8080';
  return src.replace(/^http:\/\/(127\.0\.0\.1|localhost)(:\d+)?/i, origin);
}

function resolveAbsoluteUrl(raw) {
  let absolute = (raw || '').trim();
  if (absolute.indexOf('/api/goods/upload/') === 0 || absolute.indexOf('api/goods/upload/') === 0) {
    const base = (config.baseURL || 'http://localhost:8080').replace(/\/$/, '');
    absolute = `${base}/${absolute.replace(/^\//, '')}`;
  }
  return normalizeToGatewayHost(absolute);
}

/** 清理历史误写入 USER_DATA_PATH 的永久图片（只跑一次） */
export function purgeLegacyImageStorage() {
  if (purgedOnce) return Promise.resolve();
  purgedOnce = true;
  return new Promise((resolve) => {
    try {
      const fs = wx.getFileSystemManager();
      const dir = wx.env.USER_DATA_PATH;
      fs.readdir({
        dirPath: dir,
        success(res) {
          const files = (res.files || []).filter(
            (f) => f.indexOf('fresh_img_') === 0 || f.indexOf('fresh_') === 0,
          );
          if (!files.length) {
            resolve();
            return;
          }
          let left = files.length;
          files.forEach((name) => {
            fs.unlink({
              filePath: `${dir}/${name}`,
              complete() {
                left -= 1;
                if (left <= 0) resolve();
              },
            });
          });
        },
        fail() {
          resolve();
        },
      });
    } catch (e) {
      resolve();
    }
  });
}

function downloadToTemp(src) {
  return new Promise((resolve) => {
    wx.downloadFile({
      url: src,
      success(res) {
        if (res.statusCode >= 200 && res.statusCode < 300 && res.tempFilePath) {
          // 粗查：JSON 错误体不会很长且不是图片；用文件头更稳但 downloadFile 已落盘
          memCache[src] = res.tempFilePath;
          resolve(res.tempFilePath);
          return;
        }
        console.warn('[Fresh] 图片下载状态异常', src, res.statusCode);
        resolve('');
      },
      fail(err) {
        console.warn('[Fresh] 图片 downloadFile 失败', src, err);
        resolve('');
      },
    });
  });
}

/**
 * 把本地 http 上传图转成可展示的临时路径；https 原样返回。
 * 失败返回空串，由组件回退占位图。
 */
export function toDisplayableImage(url) {
  const raw = (url || '').trim();
  if (!raw) return Promise.resolve('');

  const src = resolveAbsoluteUrl(raw);
  if (!isLocalHttpUrl(src)) {
    return Promise.resolve(src);
  }

  if (memCache[src]) return Promise.resolve(memCache[src]);
  if (pending[src]) return pending[src];

  pending[src] = purgeLegacyImageStorage()
    .then(() => downloadToTemp(src))
    .then((path) => {
      delete pending[src];
      return path || '';
    });

  return pending[src];
}

export function getImagePlaceholder() {
  return PLACEHOLDER;
}

export async function mapDisplayableImages(urls = []) {
  await purgeLegacyImageStorage();
  return Promise.all((urls || []).map((u) => toDisplayableImage(u)));
}

/** @deprecated 兼容旧调用：现改为清理遗留永久文件 + 清空内存缓存 */
export function clearLocalImageCache() {
  Object.keys(memCache).forEach((k) => delete memCache[k]);
  purgedOnce = false;
  return purgeLegacyImageStorage();
}
