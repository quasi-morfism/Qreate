package com.morfism.aiappgenerator.mapper;

import com.mybatisflex.core.BaseMapper;
import com.morfism.aiappgenerator.model.entity.ChatHistory;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Chat History mapper interface
 *
 * @author Morfism
 */
public interface ChatHistoryMapper extends BaseMapper<ChatHistory> {

    /**
     * Get latest chat history by application ID with pagination
     * 
     * @param appId application ID
     * @param limit number of records to fetch
     * @param offset offset for pagination
     * @return list of chat history records
     */
    List<ChatHistory> selectLatestByAppId(@Param("appId") Long appId, 
                                         @Param("limit") Integer limit, 
                                         @Param("offset") Integer offset);

    /**
     * Get chat history count by application ID
     * 
     * @param appId application ID
     * @return total count of chat history records
     */
    Long countByAppId(@Param("appId") Long appId);

    /**
     * Delete all chat history by application ID
     * 
     * @param appId application ID
     * @return number of deleted records
     */
    int deleteByAppId(@Param("appId") Long appId);

}
