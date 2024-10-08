package com.budwk.app.access.objects.query;

import lombok.Data;

import java.io.Serializable;

/**
 * @author wizzer.cn
 */
@Data
public class PageQuery implements Serializable {

    private Integer pageNo = 1;

    private Integer pageSize = 10;
}
