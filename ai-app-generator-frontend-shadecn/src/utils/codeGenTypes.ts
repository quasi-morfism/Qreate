/**
 * 代码生成类型枚举
 */
export enum CodeGenTypeEnum {
  HTML = 'HTML',
  VUE_PROJECT = 'VUE_PROJECT',
  REACT_PROJECT = 'REACT_PROJECT',
}

/**
 * 格式化代码生成类型显示名称
 * @param codeGenType 代码生成类型
 * @returns 格式化后的显示名称
 */
export const formatCodeGenType = (codeGenType: string): string => {
  switch (codeGenType) {
    case CodeGenTypeEnum.HTML:
      return 'HTML网页'
    case CodeGenTypeEnum.VUE_PROJECT:
      return 'Vue项目'
    case CodeGenTypeEnum.REACT_PROJECT:
      return 'React项目'
    default:
      return codeGenType
  }
}
