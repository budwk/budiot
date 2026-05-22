import { defaultConfig } from 'antd/lib/theme/internal';

defaultConfig.hashed = false;

const localStorageMock = {
  getItem: jest.fn(),
  setItem: jest.fn(),
  removeItem: jest.fn(),
  clear: jest.fn(),
};

global.localStorage = localStorageMock;

Object.defineProperty(URL, 'createObjectURL', {
  writable: true,
  value: jest.fn(),
});

class Worker {
  constructor(stringUrl) {
    this.url = stringUrl;
    this.onmessage = () => {};
  }

  postMessage(msg) {
    this.onmessage(msg);
  }
}
window.Worker = Worker;
// Polyfill MessageChannel for environments (like jest/jsdom) that don't provide it
if (typeof global.MessageChannel === 'undefined') {
  class PolyMessageChannel {
    constructor() {
      const channel = this;
      this.port1 = {
        postMessage(msg) {
          setTimeout(() => {
            if (
              channel.port2 &&
              typeof channel.port2.onmessage === 'function'
            ) {
              channel.port2.onmessage({ data: msg });
            }
          }, 0);
        },
      };
      this.port2 = {
        postMessage(msg) {
          setTimeout(() => {
            if (
              channel.port1 &&
              typeof channel.port1.onmessage === 'function'
            ) {
              channel.port1.onmessage({ data: msg });
            }
          }, 0);
        },
      };
    }
  }

  global.MessageChannel = PolyMessageChannel;
  if (typeof window !== 'undefined') {
    window.MessageChannel = PolyMessageChannel;
  }
}

if (typeof window !== 'undefined') {
  Object.defineProperty(global.window, 'matchMedia', {
    writable: true,
    configurable: true,
    value: jest.fn((query) => ({
      matches: query.includes('max-width'),
      media: query,
      onchange: null,
      addListener: jest.fn(),
      removeListener: jest.fn(),
      addEventListener: jest.fn(),
      removeEventListener: jest.fn(),
      dispatchEvent: jest.fn(),
    })),
  });
}
const errorLog = console.error;
Object.defineProperty(global.window.console, 'error', {
  writable: true,
  configurable: true,
  value: (...rest) => {
    const logStr = rest.join('');
    if (
      logStr.includes(
        'Warning: An update to %s inside a test was not wrapped in act(...)',
      )
    ) {
      return;
    }
    errorLog(...rest);
  },
});

// Mock ResizeObserver
global.ResizeObserver = class ResizeObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
};
