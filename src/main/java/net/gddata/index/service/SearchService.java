package net.gddata.index.service;

import net.gddata.common.util.FormatDateTime.FormatDateTime;
import net.gddata.index.dao.*;
import net.gddata.index.model.*;
import net.gddata.index.utils.IndexUtils;
import net.gddata.index.utils.SearchKeywordFilter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

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
    CnkwToEnKwDao cnkwToEnKwDao;

    @Autowired
    ViewDao viewDao;

    @Autowired
    View5Dao view5Dao;

    @Autowired
    Master201601Dao master201601Dao;

    @Autowired
    Result2Dao result2Dao;

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

    public List<String> getSearch3(String keyword, QueryParser parser, IndexSearcher searcher, String fieldName) {
        Query complex = getDissClause(fieldName, keyword.trim(), parser);
        if (null != complex) {
            //search
            try {
                //searching
                TopDocs docs = searcher.search(complex, Integer.MAX_VALUE);
                List<String> ids = new ArrayList<>();
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
//        parser.setDefaultOperator(QueryParser.Operator.OR);

        keyword = SearchKeywordFilter.escape(keyword);
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
//            System.out.println(q);
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
    public void searchArticls2(String keywords) {
//        if (search2) {
//            System.out.println("有一个任务正工作");
//            return;
//        }
//        search2 = true;

        List<SubIndex> indexes = indexUtils.getIndexs();
        if (indexes.size() == 0) {
            System.out.println("没有可用的索引");
        }
        IndexSearcher searcher = getSearchers(indexes);
        Analyzer analyzer = new StandardAnalyzer();
        String defaultField = "title";
        QueryParser parser = new QueryParser(defaultField, analyzer);


        List<Master201601> list = null;
        if ("keywords2".equals(keywords)) {
            list = master201601Dao.getDate();
        } else {
            list = master201601Dao.getDate5();
        }
        System.out.println("查询到"+list.size());
        int i = 0;
        for (Master201601 master : list) {
            i++;
            forKeywords(master, searcher, parser, keywords);
            System.out.println(i + "--" + master.getId());

        }
    }

    //2 查询英文词
    public void forKeywords(Master201601 master, IndexSearcher searcher, QueryParser parser, String key) {
        if (null != master) {
            View view = new View();
            view.setKid(master.getId());
            view.setCnKw(master.getKeywords2());
            String keywords = master.getKeywords2();
            String[] split = {};
            if (null != keywords && !"".equals(keywords)) {
                split = keywords.split("；");
            }
//            if (split.length == 1) {
//                System.out.println(master);
//            }
            KeTeLog keTeLog = new KeTeLog();
            keTeLog.setId(master.getId());
            //循环一个课题里的多个中文词 r 为每一个中文关键词
            List<Result> list = new ArrayList();
            //开始计时
            Instant now = Instant.now();

            List<String> titleList = new ArrayList();
            List<String> descriptionList = new ArrayList();
            List<String> subjectList = new ArrayList();

            List<List<String>> jjTitleList = new ArrayList();
            List<List<String>> jjDescriptionList = new ArrayList();
            List<List<String>> jjSubjectList = new ArrayList();

            StringBuffer stringBuffer = new StringBuffer();
            for (int i = 0; i < split.length; i++) {
                String r = split[i];
                if (null != r && !"".equals(r)) {
                    //拿单个中文关键词换多个英文关键词
                    String kewordByCnKw = kwordDao.getKewordByCnKw(r.trim());
                    if (null != kewordByCnKw && !"".equals(kewordByCnKw)) {
                        List<String> resoultList = new ArrayList<>();
                        //存储
                        Result result = new Result();

                        //得到英文词并处理好
                        now = Instant.now();//查询开始时间
                        String keyword = checkKeyword(kewordByCnKw.trim());
                        stringBuffer.append(keyword + " ");
                        System.out.println("搜索开始");
                        List<String> title = getSearch3(keyword, parser, searcher, "title");
                        List<String> description = getSearch3(keyword, parser, searcher, "description");
                        List<String> subject = getSearch3(keyword, parser, searcher, "subject");
                        System.out.println("搜索结束");
                        System.out.println("||||||||||||||||||||||||||||");
                        System.out.println("");
                        jjTitleList.add(title);
                        jjSubjectList.add(subject);
                        jjDescriptionList.add(description);
                        /**
                         * 总： std算并集
                         */
                        title.forEach(titleList::add);
                        description.forEach(descriptionList::add);
                        subject.forEach(subjectList::add);


                        //s d t 算并集
                        resoultList.addAll(title);
                        resoultList.addAll(description);
                        resoultList.addAll(subject);

                        result.setIds(resoultList);
                        list.add(result);
                    } else {
                        List<String> resoultList = new ArrayList<>();
                        //存储
                        Result result = new Result();

                        List<String> title = new ArrayList<>();
                        List<String> description = new ArrayList<>();
                        List<String> subject = new ArrayList<>();

                        title.forEach(titleList::add);
                        description.forEach(descriptionList::add);
                        subject.forEach(subjectList::add);

                        jjTitleList.add(title);
                        jjSubjectList.add(subject);
                        jjDescriptionList.add(description);

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
                NewResult newResult = arithmetic1(keTeLog, list, now, false);
                view.setN1(newResult.getSize());

                /**
                 *   已算法
                 */
                NewResult newResult1 = arithmetic2(keTeLog, list, now, false);
                view.setN2(newResult1.getSize());

                /**
                 *   丙算法
                 */
                NewResult newResult2 = arithmetic3(keTeLog, titleList, now, "丙1-title" + random(), false);
                NewResult newResult3 = arithmetic3(keTeLog, descriptionList, now, "丙2-desc" + random(), false);
                NewResult newResult4 = arithmetic3(keTeLog, subjectList, now, "丙3-subject" + random(), false);
                view.setN3(newResult2.getSize());
                view.setN4(newResult3.getSize());
                view.setN5(newResult4.getSize());
                /**
                 *  丁算法
                 */
                NewResult newResult5 = arithmetic4(keTeLog, jjTitleList, jjSubjectList, jjDescriptionList, now, false);
                view.setN6(newResult5.getSize());


            }
            if (stringBuffer.length() > 0) {
                view.setEnKw(stringBuffer.toString());
                List<String> sumList = getSearch3(stringBuffer.toString(), parser, searcher, "complex2");
                if (sumList.size() > 0) {
                    /**
                     * 戊算法
                     */
                    //仅放前5个
                    List<String> list2 = new ArrayList();
                    sumList.forEach(list2::add);
//                    NewResult newResult = new NewResult();
                    if (list2.size() > 5) {
                        keTeLog.setGuis(list2.subList(0, 5));
//                        newResult.setGuis(list2.subList(0, 5));
                    } else {
                        keTeLog.setGuis(list2);
//                        newResult.setGuis(list2);
                    }
                    keTeLog.setDesc("戊法丁:所有英文词在三个字段中搜索后的值");
                    keTeLog.setSize(sumList.size());
//                    newResult.setSize(sumList.size());
                    keTeLog.setTime(FormatDateTime.betweenTime(now));
                    indexUtils.ObjectSerialization2(keTeLog, "/data/log/戊" + random() + ".txt");
                    view.setN7(sumList.size());
                }
            }
            if (master.getId() % 10 == 0) {
                System.out.println("masterID" + master.getId());
            }
            if (key.equals("keywords2")) {
                viewDao.save(view);
            } else if (key.equals("keywords5")) {
                view5Dao.save(view);
            }
        }
    }

    public NewResult arithmetic1(KeTeLog keTeLog, List<Result> list, Instant now, boolean isFlag) {
//        List<String> ketilist = new ArrayList<>();
//        Result result1 = list.stream().filter(r -> r.getIds().size() == 0).findFirst().orElse(null);
//        if (null != result1) {
//            list.add(result1);
//        }
//        for (int i = 0; i < list.size(); i++) {
//            Result result = list.get(i);
//            if (list.size() > 1 && i == list.size() - 1) {
//                ketilist.retainAll(result.getIds());
//            } else {
//                ketilist.addAll(result.getIds());
//            }
//        }
        List<List<String>> newList = new ArrayList();
        list.stream().forEach(r -> {
            List<String> ids = r.getIds();
            newList.add(ids);
        });
        List<String> ketilist = null;
        Result result1 = list.stream().filter(r -> r.getIds().size() == 0).findFirst().orElse(null);
        NewResult newResult = new NewResult();
        if (null != result1) {
            ketilist = new ArrayList<>();
        } else {
            List<String> qjj = retainElementList(newList);
            ketilist = qjj.parallelStream().collect(Collectors.toList());
        }

        //仅放前5个
        if (ketilist.size() > 5) {
            keTeLog.setGuis(ketilist.subList(0, 5));
            newResult.setGuis(ketilist.subList(0, 5));
        } else {
            keTeLog.setGuis(ketilist);
            newResult.setGuis(ketilist);
        }
        keTeLog.setDesc("甲算法: 结果交集最相关");
        keTeLog.setSize(ketilist.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        if (isFlag) {
            indexUtils.ObjectSerialization2(keTeLog, "/data/log/甲" + random() + ".txt");
        }
        newResult.setSize(ketilist.size());
        return newResult;
    }

    public NewResult arithmetic2(KeTeLog keTeLog, List<Result> rlist, Instant now, boolean isFlag) {
        List<String> list = new ArrayList();
        for (int i = 0; i < rlist.size(); i++) {
            Result result = rlist.get(i);
            list.addAll(result.getIds());
        }
        NewResult newResult = new NewResult();
        //仅放前5个
        List l = core(list, keTeLog);
        if (l.size() > 5) {
            keTeLog.setGuis(l.subList(0, 5));
            newResult.setGuis(l.subList(0, 5));
        } else {
            keTeLog.setGuis(l);
            newResult.setGuis(l);
        }
        keTeLog.setDesc("算法乙: 结果并集重复数据>1 次相关");
        keTeLog.setSize(l.size());
        newResult.setSize(l.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        if (isFlag) {
            indexUtils.ObjectSerialization2(keTeLog, "/data/log/乙" + random() + ".txt");
        }
        return newResult;
    }

    public NewResult arithmetic3(KeTeLog keTeLog, List<String> list, Instant now, String name, boolean isFlag) {
        List l = core(list, keTeLog);
        NewResult newResult = new NewResult();
        if (l.size() > 5) {
            keTeLog.setGuis(l.subList(0, 5));
            newResult.setGuis(l.subList(0, 5));
        } else {
            keTeLog.setGuis(l);
            newResult.setGuis(l);
        }
        keTeLog.setDesc("算法丙: 结果并集重复数据>1 次相关");
        keTeLog.setSize(l.size());
        newResult.setSize(l.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        if (isFlag) {
            indexUtils.ObjectSerialization2(keTeLog, "/data/log/" + name + ".txt");
        }
        return newResult;
    }

    public NewResult arithmetic4(KeTeLog keTeLog, List<List<String>> listT, List<List<String>> listS, List<List<String>> listD, Instant now, boolean isFlag) {
        List<String> strings = listT.stream().filter(r -> r.size() == 0).findFirst().orElse(null);
        List<String> listTt = null;
        if (null != strings) {
            listTt = new ArrayList<>();
        } else {
            listTt = retainElementList(listT);
        }
        List<String> strings2 = listS.stream().filter(r -> r.size() == 0).findFirst().orElse(null);
        List<String> listSs = null;
        if (null != strings2) {
            listSs = new ArrayList<>();
        } else {
            listSs = retainElementList(listS);
        }

        List<String> strings3 = listD.stream().filter(r -> r.size() == 0).findFirst().orElse(null);
        List<String> listDd = null;
        if (null != strings3) {
            listDd = new ArrayList<>();
        } else {
            listDd = retainElementList(listD);
        }
        List lsit = new ArrayList();
        listTt.forEach(lsit::add);
        listSs.forEach(lsit::add);
        listDd.forEach(lsit::add);
        NewResult newResult = new NewResult();
        //仅放前5个
        if (lsit.size() > 5) {
            keTeLog.setGuis(lsit.subList(0, 5));
            newResult.setGuis(lsit.subList(0, 5));
        } else {
            keTeLog.setGuis(lsit);
            newResult.setGuis(lsit);
        }
        keTeLog.setDesc("算法丁: (As^Bs^Cs) V (Ad^Bd^Cd) V (At^Bt^Ct)");
        keTeLog.setSize(lsit.size());
        newResult.setSize(lsit.size());
        keTeLog.setTime(FormatDateTime.betweenTime(now));
        if (isFlag) {
            indexUtils.ObjectSerialization2(keTeLog, "/data/log/丁" + random() + ".txt");
        }
        return newResult;
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
        // 就只有一个非空集合，结果就是他
        if (arrayList.size() == 1) {
            return intersection;
        }
        // 有多个非空集合，直接挨个交集
        for (int i = 1; i < arrayList.size() - 1; i++) {
            intersection.retainAll(arrayList.get(i));
        }
        return intersection;
    }

    public List<String> retainElementList(List<List<String>> elementLists) {
        checkNotNull(elementLists, "elementLists should not be null!");
        Optional<List<String>> result = elementLists.parallelStream().filter(elementList -> elementList != null && ((List) elementList).size() != 0).reduce((a, b) -> {
            a.retainAll(b);
            return a;
        });
        return result.orElse(new ArrayList<>());
    }

    boolean status2 = false;

    public SearchResult search(String keyword) {
        if (status2) {
            System.out.println("正在搜索");
            return null;
        }
        status2 = true;
        if (null == keyword || "".equals(keyword)) {
            return new SearchResult();
        }
        SearchResult searchResult = new SearchResult();
        searchResult.setCnKw(keyword);
        List<SubIndex> indexes = indexUtils.getIndexs();
        if (indexes.size() == 0) {
            System.out.println("没有可用的索引");
        }
        IndexSearcher searcher = getSearchers(indexes);
        Analyzer analyzer = new StandardAnalyzer();
        String defaultField = "title";
        QueryParser parser = new QueryParser(defaultField, analyzer);
        String[] split = keyword.split(";");

        List<String> titleList = new ArrayList();
        List<String> descriptionList = new ArrayList();
        List<String> subjectList = new ArrayList();

        List<List<String>> jjTitleList = new ArrayList();
        List<List<String>> jjDescriptionList = new ArrayList();
        List<List<String>> jjSubjectList = new ArrayList();

        StringBuffer stringBuffer = new StringBuffer();
        Instant now = Instant.now();
        if (split.length > 0) {
            List<Result> list = new ArrayList();
            for (String r : split) {
                String kewordByCnKw = kwordDao.getKewordByCnKw(r.trim());

                if (null != kewordByCnKw && !"".equals(kewordByCnKw)) {
                    List<String> resoultList = new ArrayList<>();
                    //存储
                    Result result = new Result();

                    //得到英文词并处理好
                    String key = checkKeyword(kewordByCnKw.trim());
                    stringBuffer.append(key + " ");
                    List<String> title = getSearch3(key, parser, searcher, "title");
                    List<String> description = getSearch3(key, parser, searcher, "description");
                    List<String> subject = getSearch3(key, parser, searcher, "subject");

                    /**
                     * 总： std算并集
                     */
                    title.forEach(titleList::add);
                    description.forEach(descriptionList::add);
                    subject.forEach(subjectList::add);

                    jjTitleList.add(title);
                    jjSubjectList.add(subject);
                    jjDescriptionList.add(description);

                    //s d t 算并集
                    resoultList.addAll(title);
                    resoultList.addAll(description);
                    resoultList.addAll(subject);

                    result.setIds(resoultList);
                    list.add(result);
                } else {
                    List<String> resoultList = new ArrayList<>();
                    //存储
                    Result result = new Result();

                    List<String> title = new ArrayList<>();
                    List<String> description = new ArrayList<>();
                    List<String> subject = new ArrayList<>();

                    title.forEach(titleList::add);
                    description.forEach(descriptionList::add);
                    subject.forEach(subjectList::add);

                    jjTitleList.add(title);
                    jjSubjectList.add(subject);
                    jjDescriptionList.add(description);

                    //s d t 算并集
                    resoultList.addAll(title);
                    resoultList.addAll(description);
                    resoultList.addAll(subject);

                    result.setIds(resoultList);
                    list.add(result);
                }
            }

            KeTeLog keTeLog = new KeTeLog();
            if (list.size() > 0) {
                /**
                 *   甲算法
                 */
                NewResult newResult = arithmetic1(keTeLog, list, now, false);
                searchResult.setN1(newResult.getSize());
                searchResult.setN1Guis(newResult.getGuis());

                /**
                 *   已算法
                 */
                NewResult newResult1 = arithmetic2(keTeLog, list, now, false);
                searchResult.setN2(newResult1.getSize());
                searchResult.setN2Guis(newResult1.getGuis());

                /**
                 *   丙算法
                 */
                NewResult newResult2 = arithmetic3(keTeLog, titleList, now, "丙1-title" + random(), false);
                searchResult.setN3(newResult2.getSize());
                searchResult.setN3Guis(newResult2.getGuis());
                NewResult newResult3 = arithmetic3(keTeLog, descriptionList, now, "丙2-desc" + random(), false);
                searchResult.setN4(newResult3.getSize());
                searchResult.setN4Guis(newResult3.getGuis());
                NewResult newResult4 = arithmetic3(keTeLog, subjectList, now, "丙3-subject" + random(), false);
                searchResult.setN5(newResult4.getSize());
                searchResult.setN5Guis(newResult4.getGuis());
                /**
                 *  丁算法
                 */
                NewResult newResult5 = arithmetic4(keTeLog, jjTitleList, jjSubjectList, jjDescriptionList, now, false);
                searchResult.setN6(newResult5.getSize());
                searchResult.setN6Guis(newResult5.getGuis());

            }
            if (stringBuffer.length() > 0) {
                searchResult.setEnKw(stringBuffer.toString());
                List<String> sumList = getSearch3(stringBuffer.toString(), parser, searcher, "complex2");
                if (sumList.size() > 0) {
                    /**
                     * 戊算法
                     */
                    //仅放前5个
                    List<String> list2 = new ArrayList();
                    sumList.forEach(list2::add);
                    if (list2.size() > 5) {
                        keTeLog.setGuis(list2.subList(0, 5));
                        searchResult.setN7Guis(list2.subList(0, 5));
                    } else {
                        keTeLog.setGuis(list2);
                        searchResult.setN7Guis(list2);
                    }
                    searchResult.setN7(list2.size());
                }
            }
        }
        searchResult.setTime(FormatDateTime.betweenTime(now));
        status2 = false;
        return searchResult;
    }

    public Map getCount() {
        Map map = new HashMap();

        //总数
        Integer total = viewDao.getTotal();
        Integer n1 = getItemCount("N1");//甲
        Integer n2 = getItemCount("N2");//乙
        Integer n3 = getItemCount("N3");//丙1
        Integer n4 = getItemCount("N4");//丙2
        Integer n5 = getItemCount("N5");//丙3
        Integer n6 = getItemCount("N6");//丁
        Integer n7 = getItemCount("N7");//戊

        map.put("甲:", get(total, n1));
        map.put("乙:", get(total, n2));
        map.put("丙1-title:", get(total, n3));
        map.put("丙2-desc:", get(total, n4));
        map.put("丙3-subject:", get(total, n5));
        map.put("丁:", get(total, n6));
        map.put("戊:", get(total, n7));

        map.put("甲+乙", get(total, (n1 + n2)));
        map.put("甲+乙+丙", get(total, (n1 + n2 + n4 + n4 + n5)));

        map.put("测试数量:", total);

        return map;
    }

    public Map getCount2() {
        Map map = new HashMap();

        //总数
        Integer total = view5Dao.getTotal();
        Integer n1 = getItemCount2("N1");//甲
        Integer n2 = getItemCount2("N2");//乙
        Integer n3 = getItemCount2("N3");//丙1
        Integer n4 = getItemCount2("N4");//丙2
        Integer n5 = getItemCount2("N5");//丙3
        Integer n6 = getItemCount2("N6");//丁
        Integer n7 = getItemCount2("N7");//戊

        map.put("甲:", get(total, n1));
        map.put("乙:", get(total, n2));
        map.put("丙1-title:", get(total, n3));
        map.put("丙2-desc:", get(total, n4));
        map.put("丙3-subject:", get(total, n5));
        map.put("丁:", get(total, n6));
        map.put("戊:", get(total, n7));

        map.put("甲+乙", get(total, (n1 + n2)));
        map.put("甲+乙+丙", get(total, (n1 + n2 + n4 + n4 + n5)));

        map.put("测试数量:", total);

        return map;
    }

    public String get(Integer total, Integer count) {
        return String.valueOf(((float) count / total) * 100);
    }

    public Integer getItemCount(String t) {
        return viewDao.getItemCount(t);
    }

    public Integer getItemCount2(String t) {
        return view5Dao.getItemCount(t);
    }

    public List<View> getRetrieve3(Integer num) {
        return viewDao.getRetrieve3(num);
    }

    public List<View> getRetrieve4(Integer num) {
        return view5Dao.getRetrieve3(num);
    }



    public void sortingData() {
        Set<String> set = new HashSet();
        CopyOnWriteArrayList<Keword> dateAll = new CopyOnWriteArrayList();
        CopyOnWriteArrayList<String> master201601DaoDateAll = new CopyOnWriteArrayList();
        List<Keword> d = kwordDao.getDateAll();
        dateAll.addAll(d);
        List<String> m = master201601Dao.getDateAll();
        master201601DaoDateAll.addAll(m);

        master201601DaoDateAll.stream().forEach(r -> {
            if (null != r && !"".equals(r)) {
                String[] cnKw = r.split("；");
                if (null != cnKw && cnKw.length > 0) {
                    for (String s : cnKw) {
                        if (null != s && !"".equals(s)) {
                            set.add(s);
                        }
                    }
                }
            }
        });

        for (Iterator<Keword> it = dateAll.iterator(); it.hasNext(); ) {
            Keword next = it.next();
            if (null != next && null != next.getCnKw() && !"".equals(next.getCnKw())) {
                String s = set.stream().filter(y -> y.equals(next.getCnKw())).findFirst().orElse(null);
                if (null != s && !"".equals(s)) {
                    CnkwToEnKw cnkwToEnKw = new CnkwToEnKw();
                    cnkwToEnKw.setCnKw(next.getCnKw());
                    cnkwToEnKw.setEnKw(next.getSchKw());
                    cnkwToEnKw.setQm(1);
                    cnkwToEnKwDao.save(cnkwToEnKw);
                    dateAll.remove(next);
                }
            }
        }
        System.out.println("dateAll======:" + dateAll.size());
        for (Iterator<Keword> it = dateAll.iterator(); it.hasNext(); ) {
            Keword next = it.next();
            if (null != next && null != next.getCnKw() && !"".equals(next.getCnKw()) && (next.getCnKw().length() >= 2)) {
                List<Master201601> like = master201601Dao.getLike(next.getCnKw());
                if (null != like && like.size() > 0) {
                    for (Master201601 master201601 : like) {
                        if (null != master201601 && null != master201601.getKeywords()) {
                            String[] cnKw = master201601.getKeywords().split("；");
                            if (null != cnKw && cnKw.length > 0) {
                                for (String y : cnKw) {
                                    if (null != y && !"".equals(y) && y.contains(next.getCnKw())) {
                                        boolean status = cnkwToEnKwDao.checkCNKW(next.getCnKw());
                                        if (!status) {
                                            CnkwToEnKw cnkwToEnKw = new CnkwToEnKw();
                                            cnkwToEnKw.setCnKw(y);
                                            cnkwToEnKw.setEnKw(next.getSchKw());
                                            cnkwToEnKw.setQm(2);
                                            cnkwToEnKw.setOldCnkw(next.getCnKw());
                                            cnkwToEnKwDao.save(cnkwToEnKw);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
            dateAll.remove(next);
            System.out.println("dateAll减去后大小:" + dateAll.size());
        }
        for (Iterator<Keword> it = dateAll.iterator(); it.hasNext(); ) {
            Keword next = it.next();
            if (null != next && null != next.getCnKw() && !"".equals(next.getCnKw())) {
                CnkwToEnKw cnkwToEnKw = new CnkwToEnKw();
                cnkwToEnKw.setCnKw(next.getCnKw());
                cnkwToEnKw.setEnKw(next.getSchKw());
                cnkwToEnKw.setQm(3);
                cnkwToEnKwDao.save(cnkwToEnKw);
            }
        }
    }


    public Map sortingData2(String keyword) throws ParseException {
        Map map = new HashMap();
        if (null == keyword || "".equals(keyword)) {
            map.put("masage", "请输入");
            return map;
        }
        SearchResult searchResult = new SearchResult();
        searchResult.setCnKw(keyword);
        List<SubIndex> indexes = indexUtils.getIndexs();
        if (indexes.size() == 0) {
            System.out.println("没有可用的索引");
        }
        IndexSearcher searcher = getSearchers(indexes);
        Analyzer analyzer = new StandardAnalyzer();
        String defaultField = "title";
        QueryParser parser = new QueryParser(defaultField, analyzer);
        List<String> title = getSearch3(keyword, parser, searcher, "title");
        List<String> description = getSearch3(keyword, parser, searcher, "description");
        List<String> subject = getSearch3(keyword, parser, searcher, "subject");
        map.put("title:" ,title );
        map.put("desc:",description);
        map.put("subject:",subject);


        /*try {
            Query parse = parser.parse("title:(" + keyword + ")");
            TopDocs search = searcher.search(parse, 1000);
            Set<String> ids = new HashSet<>();
            for (int i = 0; i < search.scoreDocs.length; i++) {
                Document doc = searcher.doc(search.scoreDocs[i].doc);
                String gui = doc.get("gui");
                ids.add(gui);
            }
            map.put("guis", "title:" + ids);
            return map;

        } catch (IOException e) {
            e.printStackTrace();
        }*/


        return map;
    }

    Boolean searchFlog =false;
    public void start() {
        if(searchFlog){
            return;
        }
        searchFlog = true;
        List<SubIndex> indexes = indexUtils.getIndexs();
        if (indexes.size() == 0) {
            System.out.println("没有可用的索引");
        }
        IndexSearcher searcher = getSearchers(indexes);
        Analyzer analyzer = new StandardAnalyzer();
        String defaultField = "title";
        QueryParser parser = new QueryParser(defaultField, analyzer);


        List<Master201601> list = master201601Dao.getDate25();

        System.out.println("查询到"+list.size());
        for (Master201601 master : list) {
            jinque(master, searcher, parser);

        }
    }
    public void jinque(Master201601 master, IndexSearcher searcher, QueryParser parser){
        if (null != master) {
            String keywords2 = master.getKeywords2();
            String[] strings = checkItem(keywords2);

            String keywords5 = master.getKeywords();
            String[] strings5 = checkItem(keywords5);

            List<String> search3 = search(strings, searcher, parser);
            List<String> search5 = search(strings5, searcher, parser);
            Result2 result2 = new Result2();
            result2.setQm(1);
            result2.setKid(master.getId());
            if(search3.size()<=0||search5.size()<=0){
                result2.setSize(0);

                System.out.println("一个为空交集即为空");
            }else {
                List<List<String>> list  = new ArrayList<>();
                list.add(search3);
                list.add(search5);
                List<String> list1 = retainElementList(list);
                result2.setSize(list1.size());
                if (list1.size() > 2000) {
                    List<String> list2 = list1.subList(0, 2000);
                    result2.setGui(list2.toString());
                }
            }
            result2Dao.save(result2);
        }
    }

    public List<String> search(String[] strings,IndexSearcher searcher, QueryParser parser){
        List<String> sumList = new ArrayList();
        for (int i = 0; i < strings.length; i++) {
            String r = strings[i];
            if (null != r && !"".equals(r)) {
                //拿单个中文关键词换多个英文关键词
                String kewordByCnKw = kwordDao.getKewordByCnKw(r.trim());
                if (null != kewordByCnKw && !"".equals(kewordByCnKw)) {
                    List<String> resoultList = new ArrayList<>();
                    //存储
                    Result result = new Result();
                    //得到英文词并处理好
                    String keyword = checkKeyword(kewordByCnKw.trim());
                    List<String> title = getSearch3(keyword, parser, searcher, "title");
                    List<String> subject = getSearch3(keyword, parser, searcher, "subject");
                    title.forEach(sumList::add);
                    subject.forEach(sumList::add);
                } else {
                    //存储
                    List<String> title = new ArrayList<>();
                    List<String> subject = new ArrayList<>();
                    title.forEach(sumList::add);
                    subject.forEach(sumList::add);
                }
            }
        }
        return core(sumList, new KeTeLog());
    }

    public String[] checkItem(String keywords){
        String[] split = {};
        if (null != keywords && !"".equals(keywords)) {
            split = keywords.split("；");
        }
        return split;
    }

    public void start2() {

    }
}
