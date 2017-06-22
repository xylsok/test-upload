package net.gddata.index.dao;

import lombok.Data;
import net.gddata.index.model.CnkwToEnKw;
import net.gddata.kw.tables.records.CnkwtoenkwRecord;
import org.springframework.stereotype.Component;

import static net.gddata.kw.Tables.CNKWTOENKW;

/**
 * Created by zhangzf on 17/6/22.
 */
@Component
public class CnkwToEnKwDao extends JooqDao<CnkwtoenkwRecord,CnkwToEnKw,Integer> {
    protected CnkwToEnKwDao() {
        super(CNKWTOENKW, CnkwToEnKw.class);
    }

    @Override
    protected Integer getId(CnkwToEnKw cnkwToEnKw) {
        return cnkwToEnKw.getId();
    }
}
