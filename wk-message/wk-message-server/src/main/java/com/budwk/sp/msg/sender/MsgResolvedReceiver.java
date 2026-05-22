package com.budwk.sp.msg.sender;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MsgResolvedReceiver {
    private String userId;
    private String receiver;
    private String receiverName;
}
