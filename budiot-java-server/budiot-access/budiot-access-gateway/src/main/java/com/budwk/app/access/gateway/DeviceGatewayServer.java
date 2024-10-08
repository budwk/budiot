package com.budwk.app.access.gateway;

import com.budwk.app.access.network.DeviceGateway;
import com.budwk.app.access.network.DeviceGatewayManager;
import org.nutz.boot.starter.ServerFace;
import org.nutz.ioc.loader.annotation.Inject;
import org.nutz.ioc.loader.annotation.IocBean;

import java.util.List;

@IocBean
public class DeviceGatewayServer implements ServerFace {
    @Inject
    private DeviceGatewayManager deviceGatewayManager;

    @Override
    public void start() throws Exception {
        List<DeviceGateway> gatewayList = deviceGatewayManager.loadGateway();
        gatewayList.forEach(this::startGateway);
    }

    private void startGateway(DeviceGateway deviceGateway) {
        deviceGateway.start();
    }
}
