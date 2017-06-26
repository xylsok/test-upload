package net.gddata.index.dao;

import net.gddata.index.model.Result2;
import net.gddata.kw.tables.records.ResultRecord;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.impl.DSL;
import org.springframework.stereotype.Component;

import java.util.List;

import static net.gddata.kw.tables.Master_201601.MASTER_201601;
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

    public List<Result2> getJinque() {
        Result<Record> fetch = create().select(RESULT.fields()).select(MASTER_201601.APPROVAL_NO).from(RESULT).leftJoin(MASTER_201601).on(MASTER_201601.ID.eq(RESULT.KID)).where(RESULT.QM.eq(1)).fetch();
        return fetch.into(Result2.class);
    }

    public List<Result2> getJinque2() {
        Result<Record> fetch = create().select(RESULT.fields()).select(MASTER_201601.APPROVAL_NO).from(RESULT).leftJoin(MASTER_201601).on(MASTER_201601.ID.eq(RESULT.KID)).where(RESULT.QM.eq(2)).fetch();
        return fetch.into(Result2.class);
    }
}
