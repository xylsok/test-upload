package net.gddata.index.model;

import lombok.Data;

/**
 * Created by zhangzf on 17/6/12.
 */
@Data
public class Keword {
    private Integer id;
    private String cnKw;
    private String enKw;
    private String schKw;
    private Integer p;
    private Integer invalid;
    private Integer schCount;
}
