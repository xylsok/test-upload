package net.gddata.index.ui;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import net.gddata.index.model.Keword;
import net.gddata.index.model.SearchResult;
import net.gddata.index.model.View;
import net.gddata.index.service.SearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import springfox.documentation.annotations.ApiIgnore;

import java.util.List;
import java.util.Map;

/**
 * Created by zhangzf on 16/12/10.
 */
@Api(value = "索引搜索相关接口", description = "索引搜索相关接口")
@RestController
@RequestMapping(value = "/articleindexsearch")
public class ArticleSearchWeb {

    @Autowired
    SearchService searchService;


    @ApiIgnore
    @ApiOperation(value = "搜索文章", notes = "高级搜索与快速搜索")
    @RequestMapping(value = "/search", method = RequestMethod.GET)
    public void retrieve() {
        searchService.searchArticls();
    }

    @ApiIgnore
    @ApiOperation(value = "搜索文章数量", notes = "搜索文章数量")
    @RequestMapping(value = "/count", method = RequestMethod.GET)
    public Integer getRetrieve(@RequestParam String keyword) {
        return searchService.getRetrieve(keyword);
    }

    @ApiIgnore
    @ApiOperation(value = "查询列表", notes = "查询列表")
    @RequestMapping(value = "/list", method = RequestMethod.GET)
    public List<Keword> getRetrieve2(@ApiParam("条数") @RequestParam(value = "num", defaultValue = "20", required = false) Integer num) {
        return searchService.getRetrieve2(num);
    }

    @ApiOperation(value = "查询列表", notes = "查询列表")
    @RequestMapping(value = "/list2", method = RequestMethod.GET)
    public List<View> getRetrieve3(@ApiParam("条数") @RequestParam(value = "num", defaultValue = "20", required = false) Integer num) {
        return searchService.getRetrieve3(num);
    }

    @ApiOperation(value = "查询列表2", notes = "查询列表2(keywordds 5个关键词)")
    @RequestMapping(value = "/list3", method = RequestMethod.GET)
    public List<View> getRetrieve4(@ApiParam("条数") @RequestParam(value = "num", defaultValue = "20", required = false) Integer num) {
        return searchService.getRetrieve4(num);
    }


    @ApiOperation(value = "根据关键词测试各算法得数", notes = "根据关键词测试各算法得数")
    @RequestMapping(value = "/getcountbykeword", method = RequestMethod.GET)
    private SearchResult searc(@RequestParam("keyword") String keyword) {
        SearchResult searchresult = searchService.search(keyword);
        return searchresult;
    }

    @ApiOperation(value = "查看各算法占比1", notes = "查看各算法占比")
    @RequestMapping(value = "/getcount", method = RequestMethod.GET)
    private Map getCount() {
        return searchService.getCount();
    }

    @ApiOperation(value = "查看各算法占比2", notes = "查看各算法占比使用keywords")
    @RequestMapping(value = "/getcount2", method = RequestMethod.GET)
    private Map getCount2() {
        return searchService.getCount2();
    }


    @ApiOperation(value = "多算法测试搜索文章量(重新计算并写表)", notes = "多算法测试搜索文章量(重新计算并写表)")
    @RequestMapping(value = "/search2", method = RequestMethod.GET)
    public void retrieve2() {
        searchService.searchArticls2("keywords2");
    }

    @ApiOperation(value = "多算法测试搜索文章量(使用keywords 重新计算并写表)", notes = "多算法测试搜索文章量(使用keywords 重新计算并写表)")
    @RequestMapping(value = "/keywords", method = RequestMethod.GET)
    public void retrieve5() {
        searchService.searchArticls2("keywords5");
    }

    @ApiOperation(value = "整理数据", notes = "整理数据")
    @RequestMapping(value = "/sortingdata", method = RequestMethod.GET)
    public void sortingData() {
        searchService.sortingData();
    }




}
