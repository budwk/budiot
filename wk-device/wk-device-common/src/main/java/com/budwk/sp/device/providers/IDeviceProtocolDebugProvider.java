package com.budwk.sp.device.providers;

import com.budwk.sp.device.dto.DeviceProtocolDebugRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolEncodeRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveRequestDTO;
import com.budwk.sp.device.dto.DeviceProtocolIdentityResolveResultDTO;
import com.budwk.sp.device.dto.DeviceProtocolParseResultDTO;

public interface IDeviceProtocolDebugProvider {
    DeviceProtocolParseResultDTO debug(DeviceProtocolDebugRequestDTO request);
    String encode(DeviceProtocolEncodeRequestDTO request);
    DeviceProtocolIdentityResolveResultDTO resolveIdentity(DeviceProtocolIdentityResolveRequestDTO request);
}
