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

    @ApiOperation(value = "查询三个中文关键词搜索的结果列表", notes = "查询三个中文关键词搜索的结果列表")
    @RequestMapping(value = "/list3", method = RequestMethod.GET)
    public List<View> getRetrieve3(@ApiParam("条数") @RequestParam(value = "num", defaultValue = "20", required = false) Integer num) {
        return searchService.getRetrieve3(num);
    }

    @ApiOperation(value = "查询五个中文关键词搜索的结果列表", notes = "查询五个中文关键词搜索的结果列表")
    @RequestMapping(value = "/list5", method = RequestMethod.GET)
    public List<View> getRetrieve4(@ApiParam("条数") @RequestParam(value = "num", defaultValue = "20", required = false) Integer num) {
        return searchService.getRetrieve4(num);
    }


    @ApiOperation(value = "根据关键词测试各算法得数", notes = "根据关键词测试各算法得数")
    @RequestMapping(value = "/getcountbykeword", method = RequestMethod.GET)
    private SearchResult searc(@RequestParam("keyword") String keyword) {
        SearchResult searchresult = searchService.search(keyword);
        return searchresult;
    }

    @ApiOperation(value = "查看三个中文关键词生成的各算法占比情况", notes = "查看三个中文关键词生成的各算法占比情况")
    @RequestMapping(value = "/getcount3", method = RequestMethod.GET)
    private Map getCount() {
        return searchService.getCount();
    }

    @ApiOperation(value = "查看五个中文关键词生成的各算法占比情况", notes = "查看五个中文关键词生成的各算法占比情况")
    @RequestMapping(value = "/getcount5", method = RequestMethod.GET)
    private Map getCount2() {
        return searchService.getCount2();
    }


    @ApiOperation(value = "测试三个中文关键词的搜索数量(重新计算并写表)", notes = "测试三个中文关键词的搜索数量(重新计算并写表)")
    @RequestMapping(value = "/search3", method = RequestMethod.GET)
    public void retrieve2() {
        searchService.searchArticls2("keywords2");
    }

    @ApiOperation(value = "测试五个中文关键词的搜索数量(重新计算并写表)", notes = "测试五个中文关键词的搜索数量(重新计算并写表)")
    @RequestMapping(value = "/search5", method = RequestMethod.GET)
    public void retrieve5() {
        searchService.searchArticls2("keywords5");
    }

    @ApiIgnore
    @ApiOperation(value = "整理数据", notes = "整理数据")
    @RequestMapping(value = "/sortingdata", method = RequestMethod.GET)
    public void sortingData() {
        searchService.sortingData();
    }

    @ApiOperation(value = "输入英文搜索", notes = "输入英文搜索")
    @RequestMapping(value = "/searchen", method = RequestMethod.GET)
    public Map test(@RequestParam("keyword") String keyword){
        return searchService.sortingData2(keyword);
    }
}
