package net.gddata.index.service;

import net.gddata.index.dao.KwordDao;
import net.gddata.index.dao.Master201601Dao;
import net.gddata.index.model.*;
import net.gddata.index.utils.IndexUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;

/**
 * Created by zhangzf on 16/12/12.
 */
@Service("searchService")
public class SearchService {
    @Autowired
    IndexUtils indexUtils;

    @Autowired
    KwordDao kwordDao;

    @Autowired
    Master201601Dao master201601Dao;

    private boolean createIndexState = false;

    public String searchArticls() {
        System.out.println("开始搜索");
        if (createIndexState) {
            System.out.println("有一个正在执行的任务，请等待");
            return "有一个正在执行的任务，请等待";
        }
        createIndexState = true;
        //索引
        List<SubIndex> indexes = indexUtils.getIndexs();
        if (indexes.size() == 0) {
            System.out.println("没有可用的索引");
            return "没有可用的索引";
        }
        IndexSearcher searcher = getSearchers(indexes);
        if (null != searcher) {
            getResult(searcher);
        }
        return "OK";
    }

    private void getResult(IndexSearcher searcher) {

        Analyzer analyzer = new StandardAnalyzer();
        String defaultField = "title";
        QueryParser parser = new QueryParser(defaultField, analyzer);

        List<Keword> all = kwordDao.getAll();
        System.out.println("datas:" + all.size());

        for (Keword keword : all) {
            if (null != keword && null != keword.getSchKw() && !"".equals(keword.getSchKw())) {
                Boolean status = getSearchItem(keword.getSchKw(), parser, searcher);
                if (status) {
                    kwordDao.updateInvalid(keword.getId());
                } else {
                    String keyword = checkKeyword(keword.getSchKw());
                    keyword = keyword.replace("\"\"", "");
                    Integer count = getSearch(keyword, parser, searcher);
                    kwordDao.updateCount(keword.getId(), count);
                }
            }
            if (keword.getId() % 100 == 0) {
                System.out.println(keword.getId() + "===");
            }

        }
        System.out.println("success");
    }

    public String checkKeyword(String keyword) {
        StringBuffer stringBuffer = new StringBuffer();
        String[] split = keyword.split(";");
        for (String s : split) {
            stringBuffer.append("\"" + s.trim() + "\"" + " ");
        }
        return stringBuffer.toString();
    }

    public Boolean getSearchItem(String keyword, QueryParser parser, IndexSearcher searcher) {
        Boolean status = false;
        String[] split = keyword.split(";");
        for (String s : split) {
            Query complex = getDissClause("complex", s.trim(), parser);
            if (null != complex) {
                try {
                    //searching
                    TopDocs docs = searcher.search(complex, Integer.MAX_VALUE);
                    int totalHits = docs.totalHits;
                    if (totalHits >= 20000) {
                        status = true;
                        return status;
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return status;
    }

    public Integer getSearch(String keyword, QueryParser parser, IndexSearcher searcher) {
        Query complex = getDissClause("complex2", keyword.trim(), parser);
        if (null != complex) {
            //search
            try {
                //searching
                TopDocs docs = searcher.search(complex, Integer.MAX_VALUE);
                int totalHits = docs.totalHits;
                return totalHits;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public Set<String> getSearch3(String keyword, QueryParser parser, IndexSearcher searcher, String fieldName) {
        Query complex = getDissClause(fieldName, keyword.trim(), parser);
        if (null != complex) {
            //search
            try {
                //searching
                TopDocs docs = searcher.search(complex, Integer.MAX_VALUE);
                int totalHits = docs.totalHits;
                Set<String> ids = new HashSet<>();
                for (int i = 0; i < docs.scoreDocs.length; i++) {
                    Document doc = searcher.doc(docs.scoreDocs[i].doc);
                    String gui = doc.get("gui");
                    ids.add(gui);
                }
                return ids;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public IndexSearcher getSearchers(List<SubIndex> indexList) {
        List<IndexReader> list = new ArrayList<>();
        for (SubIndex subIndex : indexList) {
            list.add(subIndex.getReader());
        }
        if (list.size() > 0) {
            //转换成一个包含多个小索引的searcher
            try {
                int i = 0;
                IndexReader[] readers = new IndexReader[list.size()];
                for (IndexReader indexReader : list) {
                    readers[i] = indexReader;
                    i++;
                }
                MultiReader multiReader = new MultiReader(readers);
                IndexSearcher searcher = new IndexSearcher(multiReader);
                return searcher;
            } catch (IOException e) {
                System.out.println("获取用户可用索引时出错:\r\n" + e.getStackTrace());
                return null;
            }
        } else {
            System.out.println("没有找到用户有权的子索引");
            return null;
        }
    }

    private Query getDissClause(String fieldName, String keyword, QueryParser parser) {
        try {
            Query q = null;
            switch (fieldName) {
                case "title":
                    q = parser.parse("title:(" + keyword + ")");
                    break;
                case "description":
                    q = parser.parse("description:(" + keyword + ")");
                    break;
                case "subject":
                    Query subject = parser.parse("subject:(" + keyword + ")");
                    Query msubject = parser.parse("msubject:(" + keyword + ")");
                    q = new BooleanQuery.Builder()
                            .add(subject, BooleanClause.Occur.SHOULD)
                            .add(msubject, BooleanClause.Occur.SHOULD)
                            .build();
                    break;
                case "complex":
                    Query q1 = parser.parse("title:(\"" + keyword + "\")");
                    Query q2 = parser.parse("description:(\"" + keyword + "\")");
                    Query q4 = parser.parse("msubject:(\"" + keyword + "\")");
                    Query q3 = parser.parse("subject:(\"" + keyword + "\")");
                    BooleanQuery.Builder builder = new BooleanQuery.Builder()
                            .add(q1, BooleanClause.Occur.SHOULD)
                            .add(q2, BooleanClause.Occur.SHOULD)
                            .add(q3, BooleanClause.Occur.SHOULD)
                            .add(q4, BooleanClause.Occur.SHOULD);
                    q = builder.build();
                    break;
                case "complex2":
                    Query q11 = parser.parse("title:(" + keyword + ")");
                    Query q21 = parser.parse("description:(" + keyword + ")");
                    Query q41 = parser.parse("msubject:(" + keyword + ")");
                    Query q31 = parser.parse("subject:(" + keyword + ")");
                    BooleanQuery.Builder builder2 = new BooleanQuery.Builder()
                            .add(q11, BooleanClause.Occur.SHOULD)
                            .add(q21, BooleanClause.Occur.SHOULD)
                            .add(q31, BooleanClause.Occur.SHOULD)
                            .add(q41, BooleanClause.Occur.SHOULD);
                    q = builder2.build();
                    break;
            }
            if (null != q) ;
//            BooleanClause clause = new BooleanClause(q, occur);
            return q;
        } catch (Exception ex) {
            return null;
        }
    }

    private BooleanClause.Occur logic2ClauseOccur(String logic) {
        BooleanClause.Occur occur = BooleanClause.Occur.SHOULD;
        switch (logic.toLowerCase().trim()) {
            case "and":
                occur = BooleanClause.Occur.MUST;
                break;
            case "not":
                occur = BooleanClause.Occur.MUST_NOT;
                break;
        }
        return occur;
    }

    public Integer getRetrieve(String keyword) {
        //索引
        List<SubIndex> indexes = indexUtils.getIndexs();
        if (indexes.size() == 0) {
            System.out.println("没有可用的索引");
            return null;
        }
        IndexSearcher searcher = getSearchers(indexes);
        if (null != searcher) {
            Analyzer analyzer = new StandardAnalyzer();
            String defaultField = "title";
            QueryParser parser = new QueryParser(defaultField, analyzer);
            Integer count = getSearch(keyword, parser, searcher);
            return count;
        }
        return null;
    }

    public List<Keword> getRetrieve2(Integer num) {
        return kwordDao.get12(num);
    }

    private boolean search2 = false;

    //1 开始
    public void searchArticls2() {
        if (search2) {
            System.out.println("有一个任务正工作");
            return;
        }
        search2 = true;

        List<SubIndex> indexes = indexUtils.getIndexs();
        if (indexes.size() == 0) {
            System.out.println("没有可用的索引");
        }
        IndexSearcher searcher = getSearchers(indexes);
        Analyzer analyzer = new StandardAnalyzer();
        String defaultField = "title";
        QueryParser parser = new QueryParser(defaultField, analyzer);

        List<Master201601> list = master201601Dao.getDate();
        for (Master201601 master : list) {
            forKeywords(master, searcher, parser);
            System.out.println("masterID:" + master.getId());
        }

    }

    //2 查询英文词
    public Set<Integer> forKeywords(Master201601 master, IndexSearcher searcher, QueryParser parser) {
        if (null != master) {
            String keywords = master.getKeywords2();
            String[] split = {};
            if (null != keywords && !"".equals(keywords)) {
                split = keywords.split("；");
            }

            KeTeLog keTeLog = new KeTeLog();
            keTeLog.setId(master.getId());
            keTeLog.setKeywords2(master.getKeywords2());
            //循环一个词课题里的多个词 r 为每一个中文关键词
            List<SubInfo> list = new ArrayList();
            for (String r : split) {
                if (null != r && !"".equals(r)) {
                    //拿单个中文关键词换多个英文关键词
                    String kewordByCnKw = kwordDao.getKewordByCnKw(r.trim());
                    if (null != kewordByCnKw && !"".equals(kewordByCnKw)) {
                        Set<String> resoult = new HashSet();
                        SubInfo subInfo = new SubInfo();
                        //得到英文词并处理好
                        String keyword = checkKeyword(kewordByCnKw.trim());
                        keyword = keyword.replace("\"\"", "");
//                        System.out.println("处理好的英文词" + keyword);
                        Set<String> title = getSearch3(keyword, parser, searcher, "title");
                        Set<String> description = getSearch3(keyword, parser, searcher, "description");
                        Set<String> subject = getSearch3(keyword, parser, searcher, "subject");
                        resoult.addAll(title);
                        resoult.addAll(description);
                        resoult.addAll(subject); //第一次算交集
//                      System.out.println(resoult);

                        subInfo.setCnKw(r.trim());
                        subInfo.setEnKw(keyword);
                        subInfo.setIds(resoult);
                        list.add(subInfo);
                    }
                }
            }
            keTeLog.setList(list);
            indexUtils.ObjectSerialization2(keTeLog, "/data/log/sublog/sublog" + random() + ".txt");

            List<String> ketilist = new ArrayList<>();
            if (list.size() > 0) {
                for (int i = 0; i < list.size(); i++) {
                    SubInfo subInfo = list.get(i);
                    ketilist.addAll(subInfo.getIds());
                }
                HashMap<String, Integer> hs = new HashMap<String, Integer>();
                for (String string : ketilist) {
                    Integer count = 1;
                    if (hs.get(string) != null) {
                        count = hs.get(string) + 1;
                    }
                    hs.put(string, count);
                }

                Keti k = new Keti();



                List l = new ArrayList();
                StringBuffer sb = new StringBuffer();
                for (String key : hs.keySet()) {
                    if (hs.get(key) != null & hs.get(key) > 1) {
//                        System.out.print(key + " ");
                        Integer integer = hs.get(key);
                        sb.append(key+"="+integer);
                        l.add(key);
                    }
                }
                k.setDesc(sb.toString());
                k.setSize(l.size());
                if (l.size() > 10) {
                    k.setGuis(l.subList(0, 9));
                } else {
                    k.setGuis(l);
                }
                k.setId(master.getId());
                k.setKeywords2(master.getKeywords2());
                indexUtils.ObjectSerialization2(k, "/data/log/sublog" + random() + ".txt");
            }
            return null;
        }
        return null;
    }


    public int random() {
        java.util.Random random = new java.util.Random();// 定义随机类
        int result = random.nextInt(15);// 返回[0,10)集合中的整数，注意不包括10
        return result;
    }

    @Test
    public void ssss() {
        /*Set<String> result = new HashSet<String>();
        Set<String> set1 = new HashSet<String>(){{
            add("1");
            add("2");
            add("3");
        }};

        Set<String> set2 = new HashSet<String>(){{
            add("2");
            add("3");
            add("4");
        }};

        Set<String> set3 = new HashSet<String>(){{
            add("3");
            add("4");
            add("5");
        }};

        result.clear();
        result.addAll(set1);
        result.addAll(set2);
        result.retainAll(set3);
        System.out.println("交集："+result);

        result.clear();
        result.addAll(set1);
        result.addAll(set2);
        result.removeAll(set3);
        System.out.println("差集："+result);

        result.clear();
        result.addAll(set1);
        result.addAll(set2);
        result.addAll(set3);
        System.out.println("并集："+result);*/

        /*List<String> l =new ArrayList<String>();
        l.add("a") ;
        l.add("a") ;
        l.add("b") ;
        l.add("b") ;
        l.add("b") ;
        l.add("c") ;
        l.add("d") ;
        l.add("d") ;
        HashMap<String, Integer> hs = new HashMap<String, Integer>();
        for (String string : l) {
            Integer count = 1;
            if(hs.get(string) != null) {
                count = hs.get(string) + 1;
            }
            hs.put(string, count);
        }
        System.out.println(hs.toString());
        System.out.print("重复的有:");
        for (String key : hs.keySet()) {
            if (hs.get(key)!=null&hs.get(key)>1) {
                System.out.print(key+" ");
            }
        }*/
    }
}
