package net.gddata.index.dao;

import lombok.Data;
import net.gddata.index.model.CnkwToEnKw;
import net.gddata.index.model.Keword;
import net.gddata.kw.tables.records.CnkwtoenkwRecord;
import org.jooq.Record1;
import org.jooq.Result;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.gddata.kw.Tables.CNKWTOENKW;

/**
 * Created by zhangzf on 17/6/22.
 */
@Component
public class CnkwToEnKwDao extends JooqDao<CnkwtoenkwRecord, CnkwToEnKw, Integer> {
    protected CnkwToEnKwDao() {
        super(CNKWTOENKW, CnkwToEnKw.class);
    }

    @Override
    protected Integer getId(CnkwToEnKw cnkwToEnKw) {
        return cnkwToEnKw.getId();
    }

    public String getKewordByCnKw(String trim) {
        Result<Record1<String>> fetch = create().select(CNKWTOENKW.ENKW).from(CNKWTOENKW).where(CNKWTOENKW.CNKW.eq(trim)).fetch();
        if (null != fetch) {
            List<String> into = fetch.into(String.class);
            if (into.size() > 0) {
                String schKw = into.get(0);
                return schKw;
            }
        }
        return null;
    }

    public boolean checkCNKW(String s) {
        Result<CnkwtoenkwRecord> fetch = create().selectFrom(CNKWTOENKW).where(CNKWTOENKW.CNKW.eq(s)).fetch();
        if(null!=fetch&&fetch.size()>0){
            return true;
        }else {
            return false;
        }
    }
}
