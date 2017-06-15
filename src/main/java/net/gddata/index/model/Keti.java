package net.gddata.index.model;

import lombok.Data;

import java.util.List;
import java.util.Set;

/**
 * Created by zhangzf on 17-6-14.
 */
@Data
public class Keti {
    private Integer id;//课题iD
    private String keywords2;
    private List<String> guis;
    private int size;
    private String desc;
}
