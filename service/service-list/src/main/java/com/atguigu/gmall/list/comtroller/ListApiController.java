package com.atguigu.gmall.list.comtroller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/list")
public class ListApiController {

    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;
    @Autowired
    ListService listService;

    @RequestMapping("onSale/{skuId}")
    void onSale(@PathVariable("skuId") String skuId){
        listService.onSale(skuId);
    }

    @RequestMapping("cancelSale/{skuId}")
    void cancelSale(@PathVariable("skuId") String skuId){
        listService.cancelSale(skuId);
    }

    @RequestMapping("createIndex")
    public Result createIndex(){
        //创建索引
        elasticsearchRestTemplate.createIndex(Goods.class);
        elasticsearchRestTemplate.putMapping(Goods.class);
        return Result.ok();
    }

    @RequestMapping("hotScore/{skuId}")
    void hotScore(@PathVariable("skuId") String skuId){
        listService.hotScore(skuId);
    }

    @RequestMapping("list")
    Result list(@RequestBody SearchParam searchParam){
        SearchResponseVo searchResponseVo = listService.list(searchParam);
        return Result.ok(searchResponseVo);
    }
}
