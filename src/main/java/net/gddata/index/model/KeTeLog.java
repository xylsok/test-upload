package net.gddata.index.model;

import lombok.Data;

import java.util.List;


/**
 * Created by zhangzf on 17/6/14.
 */
@Data
public class KeTeLog {
    private Integer id;//课题iD
    private String keywords2;
    private List<SubInfo> list;
}
