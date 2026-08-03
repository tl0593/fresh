import { config } from '../config/index';

const TOKEN_KEY = 'token';

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || '';
}

function setToken(token) {
  if (token) {
    wx.setStorageSync(TOKEN_KEY, token);
  } else {
    wx.removeStorageSync(TOKEN_KEY);
  }
}

/**
 * Fresh 网关请求封装
 * @param {object} options
 * @param {string} options.url 相对路径，如 /api/user/mini/login
 * @param {string} [options.method]
 * @param {object} [options.data]
 * @param {boolean} [options.auth=true]
 */
function request(options = {}) {
  const { url, method = 'GET', data, auth = true } = options;
  const header = {
    'Content-Type': 'application/json',
  };
  if (auth) {
    const token = getToken();
    if (token) {
      header.Authorization = token;
    }
  }

  return new Promise((resolve, reject) => {
    wx.request({
      url: `${config.baseURL}${url}`,
      method,
      data,
      header,
      success(res) {
        const body = res.data || {};
        if (res.statusCode === 401 || body.code === 401) {
          setToken('');
          reject(new Error(body.msg || '未登录或登录已过期'));
          return;
        }
        if (res.statusCode >= 200 && res.statusCode < 300 && (body.code === 200 || body.code === undefined)) {
          resolve(body.data !== undefined ? body.data : body);
          return;
        }
        reject(new Error(body.msg || `请求失败(${res.statusCode})`));
      },
      fail(err) {
        reject(new Error(err.errMsg || '网络异常'));
      },
    });
  });
}

request.get = (url, data, opts) => request({ url, method: 'GET', data, ...opts });
request.post = (url, data, opts) => request({ url, method: 'POST', data, ...opts });
request.put = (url, data, opts) => request({ url, method: 'PUT', data, ...opts });
request.getToken = getToken;
request.setToken = setToken;

export default request;
export { getToken, setToken };
