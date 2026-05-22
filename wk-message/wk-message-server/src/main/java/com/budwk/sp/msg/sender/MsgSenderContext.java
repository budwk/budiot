package com.budwk.sp.msg.sender;

import com.budwk.sp.msg.entity.Msg_channel;
import com.budwk.sp.msg.entity.Msg_template;
import lombok.Builder;
import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class MsgSenderContext {
    private final Msg_channel channel;
    private final Msg_template template;
    private final String title;
    private final String content;
    private final Map<String, Object> params;
    private final List<String> mentions;

    public Map<String, Object> getParams() {
        return params == null ? Collections.emptyMap() : params;
    }

    public List<String> getMentions() {
        return mentions == null ? Collections.emptyList() : mentions;
    }
}
