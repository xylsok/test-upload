package net.gddata.index.ui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.gddata.index.dao.FastDbDao;
import net.gddata.index.dao.Result2Dao;
import net.gddata.index.model.FastDb;
import net.gddata.index.model.Result;
import net.gddata.index.model.Result2;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Created by zhangzf on 17/6/26.
 */
@Api(value = "写入快建库", description = "写入快建库")
@RestController
@RequestMapping(value = "/fastdb")
public class FastDbWeb {

    @Autowired
    Result2Dao result2Dao;

    boolean temp = false;

    @ApiOperation(value = "写入快建库", notes = "写入快建库")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public void retrieve() {
        if (temp) {
            System.out.println("tasking");
            return;
        }
        temp = true;
        List<Result2> result2List = result2Dao.getJinque();
        insertInto(result2List, true);
        List<Result2> result2List2 = result2Dao.getJinque2();
        insertInto(result2List2, false);


    }

    @Autowired
    FastDbDao fastDbDao;

    public void insertInto(List<Result2> result2List, boolean state) {
        for (Result2 result2 : result2List) {
            String approvalNo = result2.getApprovalNo();
            String gui = result2.getGui();
            if (gui.length() > 2) {
                String replace = gui.replace("[", "");
                String replace1 = replace.replace("]", "");
                String[] split = replace1.split(",");
                for (String s : split) {
                    FastDb fastDb = new FastDb();
                    if (state) {
                        fastDb.setCategory("精确");
                    } else {
                        fastDb.setCategory("模糊");
                    }
                    fastDb.setDb(approvalNo);
                    fastDb.setGui(s);
                    try {
                        fastDbDao.save(fastDb);
                    } catch (Exception e) {
                        System.out.println("重复的gui" + s);
                        e.printStackTrace();
                    }
                }
            }
            System.out.println("kid: " + result2.getKid());
        }
    }

    @Test
    public void ss() {
        String gui = "[]";
        if (gui.length() > 2) {
            String replace = gui.replace("[", "");
            String replace1 = replace.replace("]", "");
            String[] split = replace1.split(",");
            for (String s : split) {
//            FastDb fastDb = new FastDb();
//            fastDb.setCategory("精确");
//            fastDb.setDb(approvalNo);
//            fastDb.setGui(s);
//            fastDbDao.save(fastDb);
            }
        }

    }
}
