package net.gddata.index.ui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import net.gddata.index.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Created by zhangzf on 16/12/10.
 */
@Api(value = "索引搜索相关接口", description = "索引搜索相关接口")
@RestController
@RequestMapping(value = "/articleindexsearch")
public class ArticleSearchWeb {

    @Autowired
    SearchService searchService;

    @ApiOperation(value = "搜索文章", notes = "高级搜索与快速搜索")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public void retrieve() {
        searchService.searchArticls();
    }

    @ApiOperation(value = "搜索文章", notes = "高级搜索与快速搜索")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    public Integer getRetrieve(@RequestParam String keyword) {
        return searchService.getRetrieve(keyword);
    }

}
