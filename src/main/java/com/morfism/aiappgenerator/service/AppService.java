package com.morfism.aiappgenerator.service;

import com.morfism.aiappgenerator.model.dto.app.AppQueryRequest;
import com.morfism.aiappgenerator.model.entity.App;
import com.morfism.aiappgenerator.model.vo.AppVO;
import com.mybatisflex.core.query.QueryWrapper;
import com.mybatisflex.core.service.IService;

import java.util.List;

/**
 * Application 服务层。
 *
 * @author Morfism
 */
public interface AppService extends IService<App> {

    AppVO getAppVO(App app);

    List<AppVO> getAppVOList(List<App> appList);

    QueryWrapper getQueryWrapper(AppQueryRequest request);


}
