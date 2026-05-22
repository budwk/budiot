package com.budwk.sp.device.network;

import com.budwk.sp.device.enums.DeviceNetworkProtocol;

public interface DeviceNetworkProvider {
    DeviceNetworkProtocol protocol();
    default boolean supports(DeviceGatewayBinding binding) {
        return binding != null && binding.getProtocol() == protocol();
    }
    DeviceNetworkServer create(DeviceGatewayBinding binding, DeviceNetworkHandler handler);
}
