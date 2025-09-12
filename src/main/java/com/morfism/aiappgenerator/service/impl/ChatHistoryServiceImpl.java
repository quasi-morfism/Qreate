package com.morfism.aiappgenerator.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.morfism.aiappgenerator.model.entity.ChatHistory;
import com.morfism.aiappgenerator.mapper.ChatHistoryMapper;
import com.morfism.aiappgenerator.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * chat history 服务层实现。
 *
 * @author Morfism
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
