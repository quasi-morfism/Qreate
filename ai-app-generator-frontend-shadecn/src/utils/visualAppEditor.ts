/**
 * Visual App Editor - Using shadcn UI style
 * Manages visual editing functionality within iframe
 */
export interface ElementInfo {
  tagName: string
  id: string
  className: string
  textContent: string
  selector: string
  pagePath: string
  rect: {
    top: number
    left: number
    width: number
    height: number
  }
}

export interface VisualEditorOptions {
  onElementSelected?: (elementInfo: ElementInfo) => void
  onElementHover?: (elementInfo: ElementInfo) => void
}

export type SelectedElement = ElementInfo

export class VisualEditor {
  private iframe: HTMLIFrameElement | null = null
  private isEditMode = false
  private options: VisualEditorOptions
  private injectedStyles: HTMLStyleElement | null = null

  constructor(options: VisualEditorOptions = {}) {
    this.options = options
    this.injectGlobalStyles()
  }

  // Inject global styles to page
  private injectGlobalStyles() {
    if (typeof document === 'undefined') return

    const styleId = 'visual-app-editor-global-styles'
    if (document.getElementById(styleId)) return

    const style = document.createElement('style')
    style.id = styleId
    style.textContent = this.getGlobalStyles()
    document.head.appendChild(style)
    this.injectedStyles = style
  }

  // Get ginger theme global styles - mimicking preview/download button style
  private getGlobalStyles(): string {
    return `
      /* Visual Editor Button Styles - Ginger theme, mimicking preview/download buttons */
      .visual-edit-button {
        display: inline-flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        padding: 6px 12px;
        border-radius: 6px;
        border: 1px solid transparent;
        background: #ffffff;
        color: #374151;
        font-size: 13px;
        font-weight: 500;
        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
        line-height: 1.4;
        text-decoration: none;
        transition: all 0.15s ease;
        cursor: pointer;
        user-select: none;
        white-space: nowrap;
        box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
      }

      .visual-edit-button:hover {
        opacity: 0.8;
        box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
      }

      .visual-edit-button:not(.active) {
        border-color: #fbbf24;
        color: #a16207;
      }

      .visual-edit-button:not(.active):hover {
        background-color: #fef3c7;
        border-color: #f59e0b;
      }

      .visual-edit-button.active {
        background: #d97706;
        border-color: #d97706;
        color: white;
      }

      .visual-edit-button.active:hover {
        background: #b45309;
        border-color: #b45309;
      }

      .visual-edit-button:disabled {
        opacity: 0.5;
        cursor: not-allowed;
        pointer-events: none;
      }

      .visual-edit-button svg {
        width: 14px;
        height: 14px;
        flex-shrink: 0;
      }

      /* Selected Element Info Styles - Ginger theme */
      .selected-element-info {
        background: #fef3c7;
        border: 1px solid #fde68a;
        border-radius: 8px;
        padding: 8px 12px;
        margin-bottom: 8px;
        display: flex;
        align-items: center;
        justify-content: space-between;
        animation: slideIn 0.3s ease;
        font-family: 'Fira Code', 'SF Mono', Consolas, monospace;
        font-size: 13px;
        line-height: 1.5;
        box-shadow: 0 1px 3px rgba(217, 119, 6, 0.1);
      }

      .selected-element-info .element-details {
        display: flex;
        align-items: center;
        gap: 8px;
        color: #92400e;
        min-width: 0;
        flex: 1;
      }

      .selected-element-info .tag-name {
        font-weight: 600;
        color: #b45309;
        flex-shrink: 0;
      }

      .selected-element-info .element-id {
        color: #059669;
        font-weight: 500;
      }

      .selected-element-info .element-class {
        color: #dc2626;
        font-weight: 500;
      }

      .selected-element-info .selector {
        color: #d97706;
        background: #fef9e7;
        padding: 2px 6px;
        border-radius: 4px;
        border: 1px solid #f3e8c2;
        font-size: 12px;
        max-width: 200px;
        overflow: hidden;
        text-overflow: ellipsis;
        white-space: nowrap;
      }

      .selected-element-info .close-button {
        background: none;
        border: none;
        color: #92400e;
        cursor: pointer;
        border-radius: 4px;
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        transition: all 0.15s ease;
        flex-shrink: 0;
      }

      .selected-element-info .close-button:hover {
        background: rgba(146, 64, 14, 0.1);
        color: #78350f;
      }

      .selected-element-info .close-button svg {
        width: 12px;
        height: 12px;
      }

      @keyframes slideIn {
        from {
          opacity: 0;
          transform: translateY(-8px);
        }
        to {
          opacity: 1;
          transform: translateY(0);
        }
      }

      /* Dark mode support */
      @media (prefers-color-scheme: dark) {
        .visual-edit-button {
          background: hsl(222.2 84% 4.9%);
          border-color: hsl(217.2 32.6% 17.5%);
          color: hsl(210 40% 98%);
        }

        .visual-edit-button:hover {
          background: hsl(217.2 32.6% 17.5%);
          border-color: hsl(215 27.9% 16.9%);
        }

        .visual-edit-button.active {
          background: hsl(221.2 83.2% 53.3%);
          border-color: hsl(221.2 83.2% 53.3%);
        }

        .selected-element-info {
          background: hsl(222.2 84% 4.9%);
          border-color: hsl(217.2 32.6% 17.5%);
        }

        .selected-element-info .element-details {
          color: hsl(210 40% 98%);
        }

        .selected-element-info .selector {
          background: hsl(217.2 32.6% 17.5%);
          border-color: hsl(215 27.9% 16.9%);
          color: hsl(213.3 31% 91.4%);
        }

        .selected-element-info .close-button {
          color: hsl(215.4 16.3% 56.9%);
        }

        .selected-element-info .close-button:hover {
          background: hsl(217.2 32.6% 17.5%);
          color: hsl(210 40% 98%);
        }
      }
    `
  }

  /**
   * 初始化编辑器
   */
  init(iframe: HTMLIFrameElement) {
    this.iframe = iframe
    // 设置消息监听器
    window.addEventListener('message', this.handleIframeMessage.bind(this))
  }

  /**
   * 开启编辑模式
   */
  enableEditMode() {
    if (!this.iframe) {
      return
    }
    this.isEditMode = true
    setTimeout(() => {
      this.injectEditScript()
    }, 300)
  }

  /**
   * 关闭编辑模式
   */
  disableEditMode() {
    this.isEditMode = false
    this.sendMessageToIframe({
      type: 'TOGGLE_EDIT_MODE',
      editMode: false,
    })
    // 清除所有编辑状态
    this.sendMessageToIframe({
      type: 'CLEAR_ALL_EFFECTS',
    })
  }

  /**
   * 切换编辑模式
   */
  toggleEditMode() {
    if (this.isEditMode) {
      this.disableEditMode()
    } else {
      this.enableEditMode()
    }
    return this.isEditMode
  }

  /**
   * 强制同步状态并清理
   */
  syncState() {
    if (!this.isEditMode) {
      this.sendMessageToIframe({
        type: 'CLEAR_ALL_EFFECTS',
      })
    }
  }

  /**
   * 清除选中的元素
   */
  clearSelection() {
    this.sendMessageToIframe({
      type: 'CLEAR_SELECTION',
    })
  }

  /**
   * iframe 加载完成时调用
   */
  onIframeLoad() {
    if (this.isEditMode) {
      setTimeout(() => {
        this.injectEditScript()
      }, 500)
    } else {
      // 确保非编辑模式时清理状态
      setTimeout(() => {
        this.syncState()
      }, 500)
    }
  }

  /**
   * 处理来自 iframe 的消息
   */
  handleIframeMessage(event: MessageEvent) {
    const { type, data } = event.data
    switch (type) {
      case 'ELEMENT_SELECTED':
        if (this.options.onElementSelected && data.elementInfo) {
          this.options.onElementSelected(data.elementInfo)
        }
        break
      case 'ELEMENT_HOVER':
        if (this.options.onElementHover && data.elementInfo) {
          this.options.onElementHover(data.elementInfo)
        }
        break
    }
  }

  /**
   * 销毁编辑器
   */
  destroy() {
    this.disableEditMode()
    // 移除消息监听器
    window.removeEventListener('message', this.handleIframeMessage.bind(this))
    if (this.injectedStyles) {
      this.injectedStyles.remove()
      this.injectedStyles = null
    }
    this.iframe = null
  }

  /**
   * 向 iframe 发送消息
   */
  private sendMessageToIframe(
    message:
      | { type: 'TOGGLE_EDIT_MODE'; editMode: boolean }
      | { type: 'CLEAR_ALL_EFFECTS' }
      | { type: 'CLEAR_SELECTION' },
  ) {
    if (this.iframe?.contentWindow) {
      this.iframe.contentWindow.postMessage(message, '*')
    }
  }

  /**
   * 注入编辑脚本到 iframe
   */
  private injectEditScript() {
    if (!this.iframe) return

    const waitForIframeLoad = () => {
      try {
        if (this.iframe!.contentWindow && this.iframe!.contentDocument) {
          // 检查是否已经注入过脚本
          if (this.iframe!.contentDocument.getElementById('visual-edit-script')) {
            this.sendMessageToIframe({
              type: 'TOGGLE_EDIT_MODE',
              editMode: true,
            })
            return
          }

          const script = this.generateEditScript()
          const scriptElement = this.iframe!.contentDocument.createElement('script')
          scriptElement.id = 'visual-edit-script'
          scriptElement.textContent = script
          this.iframe!.contentDocument.head.appendChild(scriptElement)
        } else {
          setTimeout(waitForIframeLoad, 100)
        }
      } catch {
        // 静默处理注入失败
      }
    }

    waitForIframeLoad()
  }

  /**
   * 生成编辑脚本内容 - 使用 shadcn 风格样式
   */
  private generateEditScript() {
    return `
      (function() {
        let isEditMode = true;
        let currentHoverElement = null;
        let currentSelectedElement = null;

        function injectStyles() {
          if (document.getElementById('edit-mode-styles')) return;
          const style = document.createElement('style');
          style.id = 'edit-mode-styles';
          style.textContent = \`
            /* Minimal hover effect - lighter ginger */
            .edit-hover {
              outline: 1px solid hsl(215 20% 65%) !important;
              outline-offset: 1px !important;
              cursor: crosshair !important;
              transition: outline-color 0.2s ease, background-color 0.2s ease !important;
              position: relative !important;
              background: hsla(215, 20%, 65%, 0.06) !important;
              border-radius: 4px !important;
            }

            .edit-hover::before {
              content: attr(data-element-name) !important;
              position: absolute !important;
              top: -20px !important;
              left: -1px !important;
              background: hsl(210 40% 96%) !important;
              color: hsl(222.2 47.4% 11.2%) !important;
              padding: 2px 6px !important;
              font-size: 10px !important;
              font-weight: 500 !important;
              border-radius: 4px !important;
              border: 1px solid hsl(214.3 31.8% 91.4%) !important;
              z-index: 99999 !important;
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
              text-transform: none !important;
              letter-spacing: 0.02em !important;
              white-space: nowrap !important;
              pointer-events: none !important;
            }

            /* Minimal selected effect - primary ginger */
            .edit-selected {
              outline: 2px solid hsl(38 92% 50%) !important;
              outline-offset: 1px !important;
              cursor: default !important;
              position: relative !important;
              background: hsla(38, 92%, 50%, 0.10) !important;
              border-radius: 6px !important;
            }

            .edit-selected::before {
              content: 'Selected' !important;
              position: absolute !important;
              top: -24px !important;
              left: -1px !important;
              background: hsl(38 92% 50%) !important;
              color: #ffffff !important;
              padding: 3px 10px !important;
              font-size: 11px !important;
              font-weight: 600 !important;
              border-radius: 6px !important;
              z-index: 99999 !important;
              font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif !important;
              text-transform: none !important;
              letter-spacing: 0.01em !important;
              white-space: nowrap !important;
              pointer-events: none !important;
              box-shadow: none !important;
            }



            /* Dark mode support */
            @media (prefers-color-scheme: dark) {
              .edit-hover {
                outline-color: hsl(215 20% 65%) !important;
                background: hsla(215, 20%, 65%, 0.12) !important;
              }

              .edit-hover::before {
                background: hsl(217.2 32.6% 17.5%) !important;
                color: hsl(210 40% 98%) !important;
                border-color: hsl(215 27.9% 16.9%) !important;
              }

              .edit-selected {
                outline-color: hsl(38 92% 50%) !important;
                background: hsla(38, 92%, 50%, 0.12) !important;
              }

              .edit-selected::before {
                background: hsl(38 92% 50%) !important;
                color: hsl(210 40% 98%) !important;
              }
            }
          \`;
          document.head.appendChild(style);
        }

        // 生成元素选择器
        function generateSelector(element) {
          const path = [];
          let current = element;
          while (current && current !== document.body) {
            let selector = current.tagName.toLowerCase();
            if (current.id) {
              selector += '#' + current.id;
              path.unshift(selector);
              break;
            }
            if (current.className) {
              const classes = current.className.split(' ').filter(c => c && !c.startsWith('edit-'));
              if (classes.length > 0) {
                selector += '.' + classes.join('.');
              }
            }
            const siblings = Array.from(current.parentElement?.children || []);
            const index = siblings.indexOf(current) + 1;
            selector += ':nth-child(' + index + ')';
            path.unshift(selector);
            current = current.parentElement;
          }
          return path.join(' > ');
        }

        // 获取元素信息
        function getElementInfo(element) {
          const rect = element.getBoundingClientRect();
          let pagePath = window.location.search + window.location.hash;
          if (!pagePath) {
            pagePath = '';
          }

          return {
            tagName: element.tagName,
            id: element.id,
            className: element.className,
            textContent: element.textContent?.trim().substring(0, 100) || '',
            selector: generateSelector(element),
            pagePath: pagePath,
            rect: {
              top: rect.top,
              left: rect.left,
              width: rect.width,
              height: rect.height
            }
          };
        }

        // 清除悬浮效果
        function clearHoverEffect() {
          if (currentHoverElement) {
            currentHoverElement.classList.remove('edit-hover');
            currentHoverElement.removeAttribute('data-element-name');
            currentHoverElement = null;
          }
        }

        // 清除选中效果
        function clearSelectedEffect() {
          const selected = document.querySelectorAll('.edit-selected');
          selected.forEach(el => el.classList.remove('edit-selected'));
          currentSelectedElement = null;
        }

        let eventListenersAdded = false;

        function addEventListeners() {
           if (eventListenersAdded) return;

           const mouseoverHandler = (event) => {
             if (!isEditMode) return;

             const target = event.target;
             if (target === currentHoverElement || target === currentSelectedElement) return;
             if (target === document.body || target === document.documentElement) return;
             if (target.tagName === 'SCRIPT' || target.tagName === 'STYLE') return;

             clearHoverEffect();
             target.classList.add('edit-hover');
             target.setAttribute('data-element-name', target.tagName.toLowerCase());
             currentHoverElement = target;
           };

           const mouseoutHandler = (event) => {
             if (!isEditMode) return;

             const target = event.target;
             if (!event.relatedTarget || !target.contains(event.relatedTarget)) {
               clearHoverEffect();
             }
           };

           const clickHandler = (event) => {
             if (!isEditMode) return;

             event.preventDefault();
             event.stopPropagation();

             const target = event.target;
             if (target === document.body || target === document.documentElement) return;
             if (target.tagName === 'SCRIPT' || target.tagName === 'STYLE') return;

             clearSelectedEffect();
             clearHoverEffect();

             target.classList.add('edit-selected');
             currentSelectedElement = target;

             const elementInfo = getElementInfo(target);
             try {
               window.parent.postMessage({
                 type: 'ELEMENT_SELECTED',
                 data: { elementInfo }
               }, '*');
             } catch {
               // 静默处理发送失败
             }
           };

           document.body.addEventListener('mouseover', mouseoverHandler, true);
           document.body.addEventListener('mouseout', mouseoutHandler, true);
           document.body.addEventListener('click', clickHandler, true);
           eventListenersAdded = true;
         }

         function setupEventListeners() {
           addEventListeners();
         }

        // 监听父窗口消息
        window.addEventListener('message', (event) => {
           const { type, editMode } = event.data;
           switch (type) {
             case 'TOGGLE_EDIT_MODE':
               isEditMode = editMode;
               if (isEditMode) {
                 injectStyles();
                 setupEventListeners();
               } else {
                 clearHoverEffect();
                 clearSelectedEffect();
               }
               break;
             case 'CLEAR_SELECTION':
               clearSelectedEffect();
               break;
             case 'CLEAR_ALL_EFFECTS':
               isEditMode = false;
               clearHoverEffect();
               clearSelectedEffect();
               const tip = document.getElementById('edit-tip');
               if (tip) tip.remove();
               break;
           }
         });



         injectStyles();
         setupEventListeners();
      })();
    `
  }
}

export function generateElementPrompt(element: ElementInfo): string {
  let prompt = `Selected element context:\n\n`
  prompt += `Element: ${element.tagName.toLowerCase()}`
  if (element.id) prompt += `#${element.id}`
  if (element.className) {
    const classes = element.className
      .split(' ')
      .filter((c) => c.trim())
      .slice(0, 2)
    if (classes.length > 0) prompt += `.${classes.join('.')}`
  }
  prompt += `\n`
  if (element.textContent) prompt += `Text content: ${element.textContent}\n`
  prompt += `CSS selector: ${element.selector}\n`
  if (element.pagePath) prompt += `Page: ${element.pagePath}\n`
  prompt += `\nPlease modify this element according to the user's request below:`
  return prompt
}

export interface VisualEditorPublicApi {
  init: (iframe: HTMLIFrameElement) => void
  enableEditMode: () => void
  disableEditMode: () => void
  toggleEditMode: () => boolean
  syncState: () => void
  clearSelection: () => void
  onIframeLoad: () => void
  destroy: () => void
}

// Optional helpers for parity with previous API
type AnyObject = Record<string, unknown>

export function isInEditMode(editor: unknown): boolean {
  const obj = editor as AnyObject | null
  return !!(obj && (obj as AnyObject)['isEditMode'] === true)
}

export function destroyEditor(editor: unknown): void {
  try {
    const obj = editor as AnyObject
    const destroy = obj && (obj['destroy'] as (() => void) | undefined)
    if (destroy) {
      destroy()
      return
    }

    // Fallback to disable method
    const disable = obj && (obj['disableEditMode'] as (() => void) | undefined)
    if (disable) disable()
    if (obj && 'iframe' in obj) {
      ;(obj as AnyObject)['iframe'] = null
    }
  } catch {}
}
