package net.gddata.index.model;

import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Created by zhangzf on 17/3/24.
 */
@Data
public class IndexInfo {
    private String dbName;
    private Boolean isUpdate;
    private List<IndexDirInfo> indexDirInfoList;
    private Date checkDate;
}
