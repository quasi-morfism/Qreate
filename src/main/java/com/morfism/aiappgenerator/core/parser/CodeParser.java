package com.morfism.aiappgenerator.core.parser;

public interface CodeParser<T> {

    /**
     * parse code content
     *
     * @param codeContent
     * @return
     */
    T parseCode(String codeContent);
}
