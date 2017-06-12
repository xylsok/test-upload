package net.gddata.index.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.gddata.index.model.FileOrIndexDir;
import net.gddata.index.model.IndexDirInfo;
import net.gddata.index.model.IndexInfo;
import net.gddata.index.model.SubIndex;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;

/**
 * Created by knix on 16/9/18.
 */
@Component
public class IndexUtils {

    @Value("${HDBSMIndex.rootPath}")
    private String rootPath;


    private static List<SubIndex> articleIndexs = new ArrayList<>();

    //读取标记
    public IndexDirInfo readFlag(String path) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuffer sb = new StringBuffer();
        try {
            File f = new File(path);
            if (f.isDirectory()) {
                String[] list = f.list();
                for (String s : list) {
                    if (s == "indexDirInfo.txt" || s.equals("indexDirInfo.txt")) {
                        BufferedReader br = new BufferedReader(new FileReader(new File(path + "/indexDirInfo.txt")));//构造一个BufferedReader类来读取文件
                        String y = null;
                        while ((y = br.readLine()) != null) {
                            sb.append(y);
                        }
                        br.close();
                        try {
                            return mapper.readValue(sb.toString(), IndexDirInfo.class);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
            } else {
                System.out.println("无效目录");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public IndexInfo readCarateIndexStateFlag(String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        StringBuffer sb = new StringBuffer();
        try {
            File f = new File(filePath);
            if (f.exists() && f.isFile()) {
                BufferedReader br = new BufferedReader(new FileReader(f));//构造一个BufferedReader类来读取文件
                String y = null;
                while ((y = br.readLine()) != null) {
                    sb.append(y);
                }
                br.close();
                try {
                    return mapper.readValue(sb.toString(), IndexInfo.class);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return null;
    }

    //从磁盘初始化IndexReader方法（initIndex()）调用该方法操作
    public IndexReader getIndexReaderByFSD(String path) {
        FSDirectory fsDirectory = null;
        IndexReader reader = null;
        try {
            fsDirectory = FSDirectory.open(Paths.get(path));
            reader = DirectoryReader.open(fsDirectory);
        } catch (Exception e) {
            System.out.println("初始化失败:" + path);
            e.printStackTrace();
        }
        return reader;
    }

    public List<SubIndex> getIndexs() {
        return articleIndexs;
    }


    //序列化IndexDirInfo
    public void setIndexDirInfo(String indexid, String path, Integer sumIndex, String spanTime, Boolean isCreateIng, Boolean isEffective, Boolean isCurrentIndex, String indexSize, String dbname) {
        IndexDirInfo indexDirInfo = new IndexDirInfo();
        indexDirInfo.setId(indexid);
        indexDirInfo.setPath(path);
        indexDirInfo.setDocCount(sumIndex);
        indexDirInfo.setSpanTime(spanTime);
        indexDirInfo.setIsCreateIng(isCreateIng);
        indexDirInfo.setIsEffective(isEffective);
        indexDirInfo.setCurrentIndex(isCurrentIndex);
        indexDirInfo.setIndexSize(indexSize);
        indexDirInfo.setStamp(new Date().getTime());
        indexDirInfo.setDbName(dbname);
        //对象序列化
        ObjectSerialization(indexDirInfo, path + "/indexDirInfo.txt");
    }

    public void ObjectSerialization(Object o, String filePath) {
        ObjectMapper mapper = new ObjectMapper();
        String json = null;
        try {
            json = mapper.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        OutputStreamWriter pw = null;
        try {
            pw = new OutputStreamWriter(new FileOutputStream(filePath), "UTF-8");
            pw.write(json.toString());
            pw.close();//关闭流
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (null != pw) {
                    pw.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


    public String checkOrInitIndex(String dbName, String indexid) {
        File rootDir = new File(rootPath);
        if (!rootDir.exists() || !rootDir.isDirectory()) {
            System.out.println("indexUtils:索引目录不存在");
            return "indexUtils:索引目录不存在";
        }

        //初始化指定索引
        if (!"".equals(dbName) && null != dbName && !"".equals(indexid) && null != indexid) {
            File subDir = new File(rootPath + dbName.toUpperCase() + "/" + indexid);
            if (!subDir.exists() || !subDir.isDirectory()) {
                System.out.println("要初始化的目录不存在。");
                return "要初始化的目录不存在";
            }
            IndexDirInfo indexDirInfo = readFlag(subDir.getPath());
            if (null != indexDirInfo && null != indexDirInfo.getDocCount() && indexDirInfo.getDocCount() > 0 && indexDirInfo.getIsEffective()) {
                IndexReader reader = getIndexReaderByFSD(subDir.getPath());
                if (null != reader) {
                    //查到当前初始化的索引并删除
                    SubIndex subIndex = articleIndexs.stream().filter(r -> r.getDbName().equals(dbName)).findFirst().orElse(null);
                    if (null != subIndex) {
                        articleIndexs.remove(subIndex);
                    }
                    setSubIndex(reader, subDir.getPath(), indexDirInfo, dbName);
                    System.out.println("初始化 " + dbName + " 成功！");
                }
            }
        } else {
            List<String> collect = asList(rootDir.list()).parallelStream().filter(r -> r.length() == 4 && StringUtils.isNotEmpty(r)).collect(Collectors.toList());
            List<FileOrIndexDir> waitIndexDir = new ArrayList<>();
            collect.stream().forEach((String r) -> {
                File childDir = new File(rootPath + r);
                List<String> collect1 = asList(childDir.list()).stream().filter(x -> x.length() == 14 && StringUtils.isNumeric(x)).collect(Collectors.toList());
                collect1.sort((i1, i2) -> i2.compareTo(i1));
                System.out.println(r + " 索引包:" + collect1);
                if (collect1.size() > 0) {
                    for (String x : collect1) {
                        File subDir = new File(rootPath + r + "/" + x);
                        if (subDir.isDirectory()) {
                            IndexDirInfo indexDirInfo = readFlag(subDir.getPath());
                            if (null == indexDirInfo) {
                                break;
                            }
                            if (null != indexDirInfo.getDocCount() && indexDirInfo.getDocCount() > 0 && indexDirInfo.getIsEffective()) {
                                FileOrIndexDir fileOrIndexDir = new FileOrIndexDir();
                                fileOrIndexDir.setIndexFullPath(subDir.getPath());
                                fileOrIndexDir.setIndexDirInfo(indexDirInfo);
                                fileOrIndexDir.setDbName(r);
                                waitIndexDir.add(fileOrIndexDir);
                                break;
                            }
                        }
                    }
                }
            });
            articleIndexs.clear();
            waitIndexDir.stream().forEach(fileOrIndexDir -> {
                IndexReader reader = getIndexReaderByFSD(fileOrIndexDir.getIndexFullPath());
                if (null != reader) {
                    setSubIndex(reader, fileOrIndexDir.getIndexFullPath(), fileOrIndexDir.getIndexDirInfo(), fileOrIndexDir.getDbName());
                    System.out.println("初始化 " + fileOrIndexDir.getDbName() + " 成功！");
                }
            });
            System.out.println("共初始化了" + articleIndexs.size() + "个索引包");
        }
        return null;
    }


    public void setSubIndex(IndexReader reader, String d, IndexDirInfo indexDirInfo, String dbName) {
        SubIndex si = new SubIndex();
        si.setReader(reader);
        si.setPath(d);
        si.setDbName(dbName);
        indexDirInfo.setCurrentIndex(true);
        si.setDirInfo(indexDirInfo);
        articleIndexs.add(si);
    }

}
