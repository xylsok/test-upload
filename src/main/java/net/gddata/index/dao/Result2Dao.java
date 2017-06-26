package net.gddata.index.dao;

import net.gddata.index.model.Result2;
import net.gddata.kw.tables.records.ResultRecord;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import static net.gddata.kw.tables.Result.RESULT;

/**
 * Created by zhangzf on 17/6/26.
 */
@Component
public class Result2Dao extends JooqDao<ResultRecord,Result2,Integer>{
    protected Result2Dao() {
        super(RESULT, Result2.class);
    }

    @Override
    protected Integer getId(Result2 result2) {
        return result2.getId();
    }

    public Integer count(String z) {
        if(z.equals("z")){
           return create().select(DSL.count()).from(RESULT).where(RESULT.QM.eq(1)).fetchOne().value1();
        }else {
            return create().select(DSL.count()).from(RESULT).where(RESULT.QM.eq(2)).fetchOne().value1();
        }
    }

    public Integer getItem(String z) {
        if(z.equals("z")){
            return create().select(DSL.count()).from(RESULT).where(RESULT.QM.eq(1)).and(RESULT.SIZE.gt(0)).fetchOne().value1();
        }else {
            return create().select(DSL.count()).from(RESULT).where(RESULT.QM.eq(2)).and(RESULT.SIZE.gt(0)).fetchOne().value1();
        }
    }
}
