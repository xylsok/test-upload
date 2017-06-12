package net.gddata.index.model;

import lombok.Data;


/**
 * Created by zhangzf on 17/3/24.
 */
@Data
public class FileOrIndexDir {
    private String indexFullPath;
    private String dbName;
    private IndexDirInfo indexDirInfo;
}
