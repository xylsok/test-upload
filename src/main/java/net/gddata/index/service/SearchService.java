package net.gddata.index.service;

import net.gddata.common.util.FormatDateTime.FormatDateTime;
import net.gddata.index.dao.KwordDao;
import net.gddata.index.dao.Master201601Dao;
import net.gddata.index.dao.ViewDao;
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
import java.time.Instant;
import java.util.*;

import static com.google.common.base.Preconditions.checkNotNull;

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
    ViewDao viewDao;

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
        String string = stringBuffer.toString();
        string = string.replace("\"\"", "");
        return string;
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
            //循环一个课题里的多个中文词 r 为每一个中文关键词
            List<Result> list = new ArrayList();
            //开始计时
            Instant now = Instant.now();

            List<String> titleList = new ArrayList();
            List<String> descriptionList = new ArrayList();
            List<String> subjectList = new ArrayList();

            List<Set<String>> jjTitleList = new ArrayList();
            List<Set<String>> jjDescriptionList = new ArrayList();
            List<Set<String>> jjSubjectList = new ArrayList();

            StringBuffer stringBuffer = new StringBuffer();
            for (String r : split) {
                if (null != r && !"".equals(r)) {
                    //拿单个中文关键词换多个英文关键词
                    String kewordByCnKw = kwordDao.getKewordByCnKw(r.trim());
                    if (null != kewordByCnKw && !"".equals(kewordByCnKw)) {
                        Set<String> resoultList = new HashSet();
                        //存储
                        Result result = new Result();

                        //得到英文词并处理好
                        now = Instant.now();//查询开始时间
                        String keyword = checkKeyword(kewordByCnKw.trim());
                        stringBuffer.append(keyword + " ");
                        Set<String> title = getSearch3(keyword, parser, searcher, "title");
                        Set<String> description = getSearch3(keyword, parser, searcher, "description");
                        Set<String> subject = getSearch3(keyword, parser, searcher, "subject");


                        /**
                         * 总： std算并集
                         */
                        title.forEach(titleList::add);
                        description.forEach(descriptionList::add);
                        subjectList.forEach(subjectList::add);


                        //s d t 算并集
                        resoultList.addAll(title);
                        resoultList.addAll(description);
                        resoultList.addAll(subject);

                        result.setIds(resoultList);
                        list.add(result);
                    }
                }
            }


            if (list.size() > 0) {
                /**
                 *   甲算法
                 */
                arithmetic1(keTeLog, list, now);
                /**
                 *   已算法
                 */
                arithmetic2(keTeLog, list, now);

                /**
                 *   丙算法
                 */
                arithmetic3(keTeLog, titleList, now, "丙1-title");
                arithmetic3(keTeLog, descriptionList, now, "丙2-desc");
                arithmetic3(keTeLog, subjectList, now, "丙3-subject");

                /**
                 *  丁算法
                 */
                arithmetic4(keTeLog, jjTitleList, jjSubjectList, jjDescriptionList, now);

            }
            if (stringBuffer.length() > 0) {
                Set<String> sumList = getSearch3(stringBuffer.toString(), parser, searcher, "complex2");
                if (sumList.size() > 0) {
                    /**
                     * 戊算法
                     */
                    //仅放前5个
                    List<String> list2 = new ArrayList();
                    sumList.forEach(list2::add);
                    if (list2.size() > 5) {
                        keTeLog.setGuis(list2.subList(0, 5));
                    } else {
                        keTeLog.setGuis(list2);
                    }
                    keTeLog.setDesc("戊法丁:所有英文词在三个字段中搜索后的值");
                    keTeLog.setSize(sumList.size());
                    keTeLog.setTime(FormatDateTime.betweenTime(now));
                    indexUtils.ObjectSerialization2(keTeLog, "/data/log/戊.txt");
                }
            }
            if (master.getId() % 100 == 0) {
                System.out.println("masterID" + master.getId());
            }
            return null;
        }
        return null;
    }

    public void arithmetic1(KeTeLog keTeLog, List<Result> list, Instant now) {
        List<String> ketilist = new ArrayList<>();
        for (int i = 0; i < list.size(); i++) {
            Result result = list.get(i);
            if (i == list.size() - 1) {
                ketilist.retainAll(result.getIds());
            } else {
                ketilist.addAll(result.getIds());
            }
        }
        //仅放前5个
        if (ketilist.size() > 5) {
            keTeLog.setGuis(ketilist.subList(0, 5));
        } else {
            keTeLog.setGuis(ketilist);
        }
        keTeLog.setDesc("甲算法: 结果交集最相关");
        keTeLog.setSize(ketilist.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        indexUtils.ObjectSerialization2(keTeLog, "/data/log/甲.txt");
    }

    public void arithmetic2(KeTeLog keTeLog, List<Result> rlist, Instant now) {
        List<String> list = new ArrayList();
        for (int i = 0; i < rlist.size(); i++) {
            Result result = rlist.get(i);
            list.addAll(result.getIds());
        }
        List l = core(list, keTeLog);
        keTeLog.setDesc("算法乙: 结果并集重复数据>1 次相关");
        keTeLog.setSize(l.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        indexUtils.ObjectSerialization2(keTeLog, "/data/log/乙.txt");

    }

    public void arithmetic3(KeTeLog keTeLog, List<String> list, Instant now, String name) {
        List l = core(list, keTeLog);
        keTeLog.setDesc("算法丙: 结果并集重复数据>1 次相关");
        keTeLog.setSize(l.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        indexUtils.ObjectSerialization2(keTeLog, "/data/log/" + name + ".txt");
    }

    public void arithmetic4(KeTeLog keTeLog, List<Set<String>> listT, List<Set<String>> listS, List<Set<String>> listD, Instant now) {
        Set<String> listTt = retainElementList(listT);
        Set<String> listSs = retainElementList(listS);
        Set<String> listDd = retainElementList(listD);
        List lsit = new ArrayList();
        listTt.forEach(lsit::add);
        listSs.forEach(lsit::add);
        listDd.forEach(lsit::add);
        //仅放前5个
        if (lsit.size() > 5) {
            keTeLog.setGuis(lsit.subList(0, 5));
        } else {
            keTeLog.setGuis(lsit);
        }
        keTeLog.setDesc("算法丁: (As^Bs^Cs) V (Ad^Bd^Cd) V (At^Bt^Ct)");
        keTeLog.setSize(lsit.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        indexUtils.ObjectSerialization2(keTeLog, "/data/log/丁.txt");

    }

    public List core(List<String> list, KeTeLog keTeLog) {
        HashMap<String, Integer> hs = new HashMap<String, Integer>();
        for (String string : list) {
            Integer count = 1;
            if (hs.get(string) != null) {
                count = hs.get(string) + 1;
            }
            hs.put(string, count);
        }

        List l = new ArrayList();
        for (String key : hs.keySet()) {
            if (hs.get(key) != null & hs.get(key) > 1) {
                l.add(key);
            }
        }
        //仅放前5个
        if (l.size() > 5) {
            keTeLog.setGuis(l.subList(0, 5));
        } else {
            keTeLog.setGuis(l);
        }
        return l;
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

    @Test
    public void run() {
        List<Set<String>> samlist = new ArrayList<>();
        Set<String> list1 = new HashSet<>();
        list1.add("1");
        list1.add("2");
        list1.add("3");
        Set<String> list2 = new HashSet<>();
        list2.add("2");
        list2.add("3");
        list2.add("4");
        Set<String> list3 = new HashSet<>();
        list3.add("3");
        list3.add("4");
        list3.add("5");
        samlist.add(list1);
        samlist.add(list2);
        samlist.add(list3);
        Set<String> strings = retainElementList(samlist);
        System.out.println(strings);
    }

    public List<String> intersection(List<List<String>> lists) {
        if (lists == null || lists.size() == 0) {
            return null;
        }
        ArrayList<List<String>> arrayList = new ArrayList<>(lists);
        for (int i = 0; i < arrayList.size(); i++) {
            List<String> list = arrayList.get(i);
            // 去除空集合
            if (list == null || list.size() == 0) {
                arrayList.remove(list);
                i--;
            }
        }
        if (arrayList.size() == 0) {
            return null;
        }
        List<String> intersection = arrayList.get(0);
        // 就只有一个非空集合，结果就是他咯
        if (arrayList.size() == 1) {
            return intersection;
        }
        // 有多个非空集合，直接挨个交集
        for (int i = 1; i < arrayList.size() - 1; i++) {
            intersection.retainAll(arrayList.get(i));
        }
        return intersection;
    }

    public Set<String> retainElementList(List<Set<String>> elementLists) {
        checkNotNull(elementLists, "elementLists should not be null!");
        Optional<Set<String>> result = elementLists.parallelStream().filter(elementList -> elementList != null && ((Set) elementList).size() != 0).reduce((a, b) -> {
            a.retainAll(b);
            return a;
        });
        return result.orElse(new HashSet<>());
    }
}
