package net.gddata.index.model;

import lombok.Data;


/**
 * Created by knix on 16/9/8.
 */
@Data
public class IndexDirInfo {
    private String id; //索引的id
    private String path; //索引的绝对路径
    private String dbName; //索引的绝对路径
    private Integer docCount; //索引中的文档数量
    private String spanTime; //用时
    private Boolean isEffective;//是否有效
    private String indexSize;//索引大小
    private Boolean isCreateIng;//是否是正在建立中的索引
    private Boolean currentIndex;//是否是当前索引
    private Boolean isStop;//是否停用
    private Long stamp;
}
