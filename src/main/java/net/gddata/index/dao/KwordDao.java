package net.gddata.index.dao;

import net.gddata.index.model.Keword;
import net.gddata.kw.tables.records.KwordRecord;
import org.jooq.*;
import org.junit.Test;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static net.gddata.kw.tables.Kword.KWORD;

/**
 * Created by zhangzf on 17/6/12.
 */
@Component
public class KwordDao extends JooqDao<KwordRecord, Keword, Integer> {
    protected KwordDao() {
        super(KWORD, Keword.class);
    }

    @Override
    protected Integer getId(Keword keword) {
        return keword.getId();
    }

    public List<Keword> getAll() {
        Result<Record2<Integer, String>> fetch = create().select(KWORD.ID, KWORD.SCH_KW).from(KWORD).fetch();
        if (null != fetch) {
            return fetch.into(Keword.class);
        } else {
            return new ArrayList<>();
        }
    }

    public void updateCount(Integer id, Integer search) {
        create().update(KWORD).set(KWORD.P, 9).set(KWORD.SCH_COUNT, search).where(KWORD.ID.eq(id)).execute();
    }

    public void updateInvalid(Integer id) {
        create().update(KWORD).set(KWORD.INVALID, 1).where(KWORD.ID.eq(id)).execute();
    }

    public void updateKeword() {
        create().update(KWORD).set(KWORD.INVALID, 0).set(KWORD.SCH_COUNT, 0).set(KWORD.P, 0).execute();
    }


    public List<Keword> get12(Integer num) {
        Result<KwordRecord> fetch = create().selectFrom(KWORD).limit(num).fetch();
        return null != fetch ? fetch.into(Keword.class) : new ArrayList<>();
    }

    public String getKewordByCnKw(String keword) {
        Result<Record1<String>> fetch = create().select(KWORD.SCH_KW).from(KWORD).where(KWORD.CN_KW.eq(keword)).fetch();
        if (null != fetch) {
            List<Keword> into = fetch.into(Keword.class);
            if (into.size() > 0) {
                String schKw = into.get(0).getSchKw();
                return schKw;
            } else {
                return null;
            }
        }
        return null;
    }

//    public void getKewordByCnKwLike(String s) {
//        boolean containChinese = isContainChinese("1212sdfsd中国人");
//        System.out.println(containChinese);
//        if(containChinese){
//
//        }
//        return keyword;
//    }


    public static boolean isContainChinese(String str) {
        Pattern p = Pattern.compile("[\u4e00-\u9fa5]");
        Matcher m = p.matcher(str);
        if (m.find()) {
            return true;
        }
        return false;
    }

    public List<Keword> getDateAll() {
        Result<Record2<String, String>> fetch = create().select(KWORD.CN_KW, KWORD.EN_KW).from(KWORD).fetch();
        if (null != fetch) {
            return fetch.into(Keword.class);
        } else {
            return new ArrayList<>();
        }
    }
}
