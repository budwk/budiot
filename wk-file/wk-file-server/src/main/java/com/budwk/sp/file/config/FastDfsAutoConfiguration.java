package com.budwk.sp.file.config;

import com.github.tobato.fastdfs.FdfsClientConfig;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnProperty(prefix = "wk.file", name = "default-storage", havingValue = "FDFS")
@Import(FdfsClientConfig.class)
public class FastDfsAutoConfiguration {
}
