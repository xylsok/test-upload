package net.gddata.index.ui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.gddata.index.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Created by zhangzf on 17/6/24.
 */
@Api(value = "计算机最相关与次相关", description = "计算机最相关与次相关")
@RestController
@RequestMapping(value = "/searchcount")
public class ArticleSearch2Web {

    @Autowired
    SearchService searchService;

    @ApiOperation(value = "计算最相关", notes = "计算最相关")
    @RequestMapping(value = "/start", method = RequestMethod.GET)
    public void start() {
        searchService.start();
    }

    @ApiOperation(value = "计算次相关", notes = "计算次相关")
    @RequestMapping(value = "/start2", method = RequestMethod.GET)
    public void start2() {
        searchService.start2();
    }

    @ApiOperation(value = "计算最相关占比", notes = "计算最相关占比")
    @RequestMapping(value = "/info", method = RequestMethod.GET)
    public Map info() {
        return searchService.info("z");
    }

    @ApiOperation(value = "计算最次关占比", notes = "计算最次关占比")
    @RequestMapping(value = "/info2", method = RequestMethod.GET)
    public Map info2() {
        return searchService.info("c");
    }

}
