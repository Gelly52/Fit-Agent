import axios from 'axios';
import { API_BASE } from '../config/runtime';

const TOKEN_COOKIE_KEY = 'user_token';
const USER_INFO_COOKIE_KEY = 'user_info';

export function getCookieValue(name, cookieString) {
  const source = typeof cookieString === 'string'
    ? cookieString
    : (typeof document !== 'undefined' ? document.cookie : '');

  if (!source) {
    return undefined;
  }

  const cookieEntries = source.split(';');
  for (let index = 0; index < cookieEntries.length; index += 1) {
    const cookieEntry = cookieEntries[index].trim();
    if (!cookieEntry) {
      continue;
    }

    const separatorIndex = cookieEntry.indexOf('=');
    const rawName = separatorIndex >= 0 ? cookieEntry.slice(0, separatorIndex) : cookieEntry;
    if (rawName !== name) {
      continue;
    }

    const rawValue = separatorIndex >= 0 ? cookieEntry.slice(separatorIndex + 1) : '';
    try {
      return decodeURIComponent(rawValue);
    } catch (error) {
      return rawValue;
    }
  }

  return undefined;
}

export function getToken() {
  return getCookieValue(TOKEN_COOKIE_KEY);
}

export function getUserInfo() {
  const userJson = getCookieValue(USER_INFO_COOKIE_KEY);
  if (userJson === undefined || userJson === '') {
    return undefined;
  }

  try {
    return JSON.parse(userJson);
  } catch (error) {
    return undefined;
  }
}

export function createHttpInstance() {
  const httpInstance = axios.create({
    baseURL: API_BASE,
    withCredentials: true,
    timeout: 60000
  });

  httpInstance.interceptors.request.use(
    (config) => {
      const nextConfig = config || {};
      nextConfig.headers = nextConfig.headers || {};

      const userInfo = getUserInfo();
      if (userInfo) {
        nextConfig.headers.headerUserId = userInfo.id;
      }

      const userToken = getToken();
      if (userToken) {
        nextConfig.headers.headerUserToken = userToken;
      }

      return nextConfig;
    },
    (error) => {
      console.log(error);
      return Promise.reject(error);
    }
  );

  httpInstance.interceptors.response.use(
    (response) => response.data,
    (error) => {
      console.log('err: ' + error);
      console.log('err: ' + (error && error.data));
      return Promise.reject(error);
    }
  );

  return httpInstance;
}

export const instance = createHttpInstance();
export const http = instance;

if (typeof window !== 'undefined') {
  window.instance = instance;
}

export default instance;
