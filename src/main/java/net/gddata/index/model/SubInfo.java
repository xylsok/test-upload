package net.gddata.index.model;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * Created by zhangzf on 17/6/14.
 */
@Data
public class SubInfo {
    private String cnKw;
    private String enKw;
    private Set<String> ids;
}
