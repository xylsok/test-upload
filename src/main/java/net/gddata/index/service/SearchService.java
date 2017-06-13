package net.gddata.index.service;

import net.gddata.index.dao.KwordDao;
import net.gddata.index.model.Keword;
import net.gddata.index.model.SubIndex;
import net.gddata.index.utils.IndexUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.MultiReader;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zhangzf on 16/12/12.
 */
@Service("searchService")
public class SearchService {
    @Autowired
    IndexUtils indexUtils;

    @Autowired
    KwordDao kwordDao;

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
//                    kwordDao.updateInvalid(keword.getId());
                } else {
                    String keyword = checkKeyword(keword.getSchKw());
                    keyword = keyword.replace("\"\"","");
                    Integer count = getSearch(keyword, parser, searcher);
//                    kwordDao.updateCount(keword.getId(), count);
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
//        List<BooleanClause> clauseList = new ArrayList<>();
        if (null != complex) {
//            clauseList.clear();
//            clauseList.add(complex);
//            BooleanQuery.Builder builder = new BooleanQuery.Builder();
//            clauseList.forEach(builder::add);
//            BooleanQuery bq = builder.build();
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
}
