package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_classify;
import com.budwk.app.iot.models.Iot_scene_space;
import com.budwk.starter.database.service.BaseService;

public interface IotSceneSpaceService extends BaseService<Iot_scene_space> {
    void save(Iot_scene_space sceneSpace, String pid);

    void deleteAndChild(Iot_scene_space sceneSpace);

}