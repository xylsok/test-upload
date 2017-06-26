package net.gddata.index.model;

import lombok.Data;

import java.util.Date;

/**
 * Created by zhangzf on 17/6/26.
 */
@Data
public class FastDb {
    private Integer id;
    private String db;
    private String category;
    private String gui;
    private String user;
    private Date time;
}
