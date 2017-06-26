package net.gddata.index.ui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.gddata.index.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

/**
 * Created by zhangzf on 17/6/24.
 */
@Api(value = "计算机最相关与次相关", description = "计算机最相关与次相关")
@RestController
@RequestMapping(value = "/searchcount")
public class ArticleSearch2Web {

    @Autowired
    SearchService searchService;

    @ApiOperation(value = "计算最相关", notes = "计算最相关与次相关")
    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public void start() {
        searchService.start();
    }
}
