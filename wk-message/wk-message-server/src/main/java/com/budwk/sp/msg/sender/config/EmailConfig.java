package com.budwk.sp.msg.sender.config;

import lombok.Data;

@Data
public class EmailConfig {
    private String host;
    private Integer port;
    private String username;
    private String password;
    private String from;
    private String nickname;
    private Boolean ssl;
    private Boolean starttls;
}
