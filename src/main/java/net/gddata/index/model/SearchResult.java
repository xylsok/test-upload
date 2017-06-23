package net.gddata.index.model;

import lombok.Data;

import java.util.List;

/**
 * Created by zhangzf on 17/6/16.
 */
@Data
public class SearchResult {
    private String cnKw;
    private String enKw;
    private Integer n1;//甲
    private Integer n2;//甲
    private Integer n3;//丙1
    private Integer n4;//丙2
    private Integer n5;//丙3
    private Integer n6;//丁
    private Integer n7;//戊
    private String time;
    private List<String> n1Guis;//甲
    private List<String> n2Guis;//甲
    private List<String> n3Guis;//甲
    private List<String> n4Guis;//甲
    private List<String> n5Guis;//甲
    private List<String> n6Guis;//甲
    private List<String> n7Guis;//甲
}
