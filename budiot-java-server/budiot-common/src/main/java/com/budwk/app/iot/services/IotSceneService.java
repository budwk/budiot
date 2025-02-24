package com.budwk.app.iot.services;

import com.budwk.app.iot.models.Iot_scene;
import com.budwk.starter.database.service.BaseService;

public interface IotSceneService extends BaseService<Iot_scene> {
    void save(Iot_scene iotScene);
    void update(Iot_scene iotScene);
    void delete(Iot_scene iotScene);
}