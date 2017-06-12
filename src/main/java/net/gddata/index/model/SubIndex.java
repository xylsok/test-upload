package net.gddata.index.model;

import lombok.Data;
import org.apache.lucene.index.IndexReader;

/**
 * Created by knix on 16/9/10.
 */
@Data
public class SubIndex {
    private String path;
    private IndexReader reader;
    private IndexDirInfo dirInfo;
    private String dbName;
}
