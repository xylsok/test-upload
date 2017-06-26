package net.gddata.index.dao;

import net.gddata.index.model.Result2;
import net.gddata.kw.tables.records.ResultRecord;
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
}
