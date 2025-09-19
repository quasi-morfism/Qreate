# 前端集成指南 - AI代码生成器

## 📋 概述

本指南描述了如何在前端应用中集成AI代码生成器的流式API，支持HTML、多文件项目和Vue项目的实时代码生成。

## 🚀 核心API

### 1. 创建应用

**接口**: `POST /app/add`

**请求体**:
```json
{
  "appName": "我的Vue应用",
  "initPrompt": "创建一个待办事项管理应用",
  "codeGenType": "VUE_PROJECT"
}
```

**代码生成类型**:
- `"HTML"`: 单页面HTML应用，适合简单的静态页面
- `"MULTI_FILE"`: 多文件项目，支持复杂的项目结构  
- `"VUE_PROJECT"`: Vue.js项目，支持实时文件写入和组件化开发

### 2. 流式代码生成

**接口**: `GET /app/chat/gen/code`

**参数**:
- `appId`: 应用ID
- `message`: 用户消息

## 📡 SSE流式数据处理

### 数据格式

所有SSE消息都包装在JSON格式中：
```json
{"q": "消息内容"}
```

### 消息类型

#### 1. 普通AI响应
```json
{"q": "正在创建Vue组件..."}
```

#### 2. 工具执行标识 (仅Vue项目)
```json
{"q": "\n[TOOL_EXECUTED:writeFile:tool-001]"}
```

#### 3. 文件写入成功 (仅Vue项目)
```json
{"q": "\n[FILE_WRITE_SUCCESS:App.vue]"}
```

#### 4. 文件写入失败 (仅Vue项目)
```json
{"q": "\n[FILE_WRITE_FAILED:components/Header.vue]"}
```

#### 5. 生成完成
```json
{"q": "\n[GENERATION_COMPLETE]"}
```

#### 6. 流结束事件
```
event: done
data: ""
```

## 💻 前端实现示例

### JavaScript (原生)

```javascript
class CodeGeneratorClient {
  constructor() {
    this.eventSource = null;
    this.isConnected = false;
  }

  /**
   * 开始代码生成
   * @param {number} appId - 应用ID
   * @param {string} message - 用户消息
   * @param {Object} callbacks - 回调函数
   */
  startGeneration(appId, message, callbacks = {}) {
    const url = `/app/chat/gen/code?appId=${appId}&message=${encodeURIComponent(message)}`;
    
    this.eventSource = new EventSource(url);
    this.isConnected = true;

    // 处理消息
    this.eventSource.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data);
        const content = data.q;
        
        this.handleMessage(content, callbacks);
      } catch (error) {
        console.error('解析SSE消息失败:', error);
        callbacks.onError?.(error);
      }
    };

    // 处理完成事件
    this.eventSource.addEventListener('done', (event) => {
      console.log('流传输完成');
      this.disconnect();
      callbacks.onComplete?.();
    });

    // 处理错误
    this.eventSource.onerror = (error) => {
      console.error('SSE连接错误:', error);
      callbacks.onError?.(error);
      this.disconnect();
    };
  }

  /**
   * 处理不同类型的消息
   */
  handleMessage(content, callbacks) {
    if (content.includes('[FILE_WRITE_SUCCESS:')) {
      // 文件写入成功
      const fileName = content.match(/\[FILE_WRITE_SUCCESS:(.*?)\]/)?.[1];
      callbacks.onFileWriteSuccess?.(fileName);
      
    } else if (content.includes('[FILE_WRITE_FAILED:')) {
      // 文件写入失败
      const fileName = content.match(/\[FILE_WRITE_FAILED:(.*?)\]/)?.[1];
      callbacks.onFileWriteError?.(fileName);
      
    } else if (content.includes('[TOOL_EXECUTED:')) {
      // 工具执行
      const toolInfo = content.match(/\[TOOL_EXECUTED:(.*?):(.*?)\]/);
      const toolName = toolInfo?.[1];
      const toolId = toolInfo?.[2];
      callbacks.onToolExecuted?.(toolName, toolId);
      
    } else if (content.includes('[GENERATION_COMPLETE]')) {
      // 生成完成
      callbacks.onGenerationComplete?.();
      
    } else if (content.trim()) {
      // 普通AI响应内容
      callbacks.onContent?.(content);
    }
  }

  /**
   * 断开连接
   */
  disconnect() {
    if (this.eventSource) {
      this.eventSource.close();
      this.eventSource = null;
      this.isConnected = false;
    }
  }

  /**
   * 检查连接状态
   */
  isConnected() {
    return this.isConnected;
  }
}

// 使用示例
const client = new CodeGeneratorClient();

client.startGeneration(123, '添加一个登录页面', {
  onContent: (content) => {
    // 显示AI响应内容
    appendToChat(content);
  },
  
  onFileWriteSuccess: (fileName) => {
    // 显示文件写入成功
    showNotification(`✅ 文件 ${fileName} 写入成功`);
    updateFileList(fileName);
  },
  
  onFileWriteError: (fileName) => {
    // 显示文件写入失败
    showNotification(`❌ 文件 ${fileName} 写入失败`, 'error');
  },
  
  onToolExecuted: (toolName, toolId) => {
    // 显示工具执行
    console.log(`🛠️ 工具执行: ${toolName} (${toolId})`);
  },
  
  onGenerationComplete: () => {
    // 生成完成
    showNotification('🎉 代码生成完成！');
    enableDownloadButton();
  },
  
  onComplete: () => {
    // 流传输完成
    console.log('SSE连接已关闭');
  },
  
  onError: (error) => {
    // 错误处理
    console.error('生成过程出错:', error);
    showNotification('❌ 生成过程出错', 'error');
  }
});
```

### React Hook示例

```javascript
import { useState, useEffect, useRef } from 'react';

export const useCodeGenerator = () => {
  const [isGenerating, setIsGenerating] = useState(false);
  const [messages, setMessages] = useState([]);
  const [fileList, setFileList] = useState([]);
  const [error, setError] = useState(null);
  
  const clientRef = useRef(null);

  const startGeneration = async (appId, message) => {
    setIsGenerating(true);
    setMessages([]);
    setFileList([]);
    setError(null);

    const client = new CodeGeneratorClient();
    clientRef.current = client;

    client.startGeneration(appId, message, {
      onContent: (content) => {
        setMessages(prev => [...prev, { type: 'content', content }]);
      },
      
      onFileWriteSuccess: (fileName) => {
        setFileList(prev => [...prev, { name: fileName, status: 'success' }]);
        setMessages(prev => [...prev, { 
          type: 'file_success', 
          content: `✅ 文件 ${fileName} 写入成功` 
        }]);
      },
      
      onFileWriteError: (fileName) => {
        setFileList(prev => [...prev, { name: fileName, status: 'error' }]);
        setMessages(prev => [...prev, { 
          type: 'file_error', 
          content: `❌ 文件 ${fileName} 写入失败` 
        }]);
      },
      
      onGenerationComplete: () => {
        setMessages(prev => [...prev, { 
          type: 'complete', 
          content: '🎉 代码生成完成！' 
        }]);
      },
      
      onComplete: () => {
        setIsGenerating(false);
      },
      
      onError: (error) => {
        setError(error);
        setIsGenerating(false);
      }
    });
  };

  const stopGeneration = () => {
    if (clientRef.current) {
      clientRef.current.disconnect();
      clientRef.current = null;
    }
    setIsGenerating(false);
  };

  useEffect(() => {
    return () => {
      stopGeneration();
    };
  }, []);

  return {
    isGenerating,
    messages,
    fileList,
    error,
    startGeneration,
    stopGeneration
  };
};
```

### Vue 3 Composition API示例

```javascript
import { ref, onUnmounted } from 'vue';

export const useCodeGenerator = () => {
  const isGenerating = ref(false);
  const messages = ref([]);
  const fileList = ref([]);
  const error = ref(null);
  
  let client = null;

  const startGeneration = async (appId, message) => {
    isGenerating.value = true;
    messages.value = [];
    fileList.value = [];
    error.value = null;

    client = new CodeGeneratorClient();

    client.startGeneration(appId, message, {
      onContent: (content) => {
        messages.value.push({ type: 'content', content });
      },
      
      onFileWriteSuccess: (fileName) => {
        fileList.value.push({ name: fileName, status: 'success' });
        messages.value.push({ 
          type: 'file_success', 
          content: `✅ 文件 ${fileName} 写入成功` 
        });
      },
      
      onFileWriteError: (fileName) => {
        fileList.value.push({ name: fileName, status: 'error' });
        messages.value.push({ 
          type: 'file_error', 
          content: `❌ 文件 ${fileName} 写入失败` 
        });
      },
      
      onGenerationComplete: () => {
        messages.value.push({ 
          type: 'complete', 
          content: '🎉 代码生成完成！' 
        });
      },
      
      onComplete: () => {
        isGenerating.value = false;
      },
      
      onError: (err) => {
        error.value = err;
        isGenerating.value = false;
      }
    });
  };

  const stopGeneration = () => {
    if (client) {
      client.disconnect();
      client = null;
    }
    isGenerating.value = false;
  };

  onUnmounted(() => {
    stopGeneration();
  });

  return {
    isGenerating,
    messages,
    fileList,
    error,
    startGeneration,
    stopGeneration
  };
};
```

## 🔧 关键特性说明

### 1. 前端断开恢复能力
- **后台独立执行**: 前端断开连接不会影响后台文件写入和聊天历史保存
- **多客户端支持**: 支持多个前端同时订阅同一个生成流
- **状态恢复**: 可通过聊天历史API恢复之前的对话内容

### 2. Vue项目特殊功能
- **实时文件写入**: 文件通过工具调用实时写入服务器
- **文件状态追踪**: 实时反馈每个文件的写入状态
- **工具执行监控**: 显示AI工具的执行过程

### 3. 错误处理建议
```javascript
// 网络重连逻辑
const retryConnection = (appId, message, retryCount = 0) => {
  if (retryCount >= 3) {
    showError('连接失败，请稍后重试');
    return;
  }
  
  setTimeout(() => {
    startGeneration(appId, message, {
      onError: () => retryConnection(appId, message, retryCount + 1)
    });
  }, 1000 * Math.pow(2, retryCount)); // 指数退避
};
```

## 📚 API参考

### 完整的API端点列表

- `POST /app/add` - 创建应用
- `GET /app/get/vo?id={id}` - 获取应用详情
- `POST /app/my/list/page/vo` - 获取用户应用列表
- `GET /app/chat/gen/code` - 流式代码生成
- `POST /app/deploy` - 部署应用

### 错误码说明

- `40000` - 参数错误
- `40001` - 未登录
- `40003` - 权限不足  
- `40004` - 资源不存在
- `50000` - 系统内部错误

## 🎯 最佳实践

1. **连接管理**: 始终在组件卸载时关闭SSE连接
2. **错误处理**: 实现重连机制和用户友好的错误提示
3. **状态管理**: 合理管理生成状态，避免重复请求
4. **用户体验**: 提供清晰的进度指示和文件状态反馈
5. **性能优化**: 对消息进行适当的节流和防抖处理

---

🎉 现在您可以开始集成AI代码生成器的强大功能了！