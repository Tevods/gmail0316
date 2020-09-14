package com.atguigu.gmall.list.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.list.repository.GoodsElasticsearchRepository;
import com.atguigu.gmall.list.service.ListService;
import com.atguigu.gmall.model.list.*;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.search.join.ScoreMode;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.NestedQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.aggregations.Aggregation;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.nested.NestedAggregationBuilder;
import org.elasticsearch.search.aggregations.bucket.nested.ParsedNested;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedLongTerms;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightField;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ListServiceImpl implements ListService {

    @Autowired
    private GoodsElasticsearchRepository goodsElasticsearchRepository;
    @Autowired
    private ProductFeignClient productFeignClient;
    @Autowired
    private RedisTemplate redisTemplate;
    @Autowired
    private RestHighLevelClient restHighLevelClient;

    @Override
    public void onSale(String skuId) {
        Goods goods = new Goods();
        goods.setId(Long.parseLong(skuId));
        goodsElasticsearchRepository.delete(goods);
    }

    @Override
    public void cancelSale(String skuId) {
        Goods goods = new Goods();
        SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
        BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
        List<BaseAttrInfo> baseAttrInfoList = productFeignClient.getBaseAttr(skuId);
        BaseTrademark baseTrademark = productFeignClient.getTrademark(skuInfo.getTmId());

        if (baseAttrInfoList != null){
            List<SearchAttr> searchAttrs = baseAttrInfoList.stream().map(baseAttrInfo -> {
                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(baseAttrInfo.getId());
                searchAttr.setAttrName(baseAttrInfo.getAttrName());
                List<BaseAttrValue> attrValueList = baseAttrInfo.getAttrValueList();
                searchAttr.setAttrValue(attrValueList.get(0).getValueName());
                return searchAttr;
            }).collect(Collectors.toList());
            goods.setAttrs(searchAttrs);
        }
        //baseCategory123
        if (baseCategoryView != null){
            goods.setCategory1Id(baseCategoryView.getCategory1Id());
            goods.setCategory1Name(baseCategoryView.getCategory1Name());
            goods.setCategory2Id(baseCategoryView.getCategory2Id());
            goods.setCategory2Name(baseCategoryView.getCategory2Name());
            goods.setCategory3Id(baseCategoryView.getCategory3Id());
            goods.setCategory3Name(baseCategoryView.getCategory3Name());
        }
        //image
        goods.setId(Long.parseLong(skuId));
        goods.setDefaultImg(skuInfo.getSkuDefaultImg());
        goods.setTitle(skuInfo.getSkuName());
        goods.setPrice(skuInfo.getPrice().doubleValue());
        goods.setTmId(skuInfo.getTmId());
        goods.setTmLogoUrl(baseTrademark.getLogoUrl());
        goods.setTmName(baseTrademark.getTmName());
        goods.setCreateTime(new Date());
        //调用es的api插入数据
        goodsElasticsearchRepository.save(goods);
    }

    @Override
    public void hotScore(String skuId) {
        //Redis zset计数
        Long hotScore = 0L;
        //缓存原来的热度值
        hotScore = redisTemplate.opsForZSet().incrementScore("hotScore","sku:" + skuId,1).longValue();
        if (hotScore%10 == 0){
            //修改es
            Optional<Goods> goodsOptional = goodsElasticsearchRepository.findById(Long.parseLong(skuId));
            Goods goods = goodsOptional.get();
            goods.setHotScore(hotScore);
            goodsElasticsearchRepository.save(goods);
        }
    }

    @Override
    public SearchResponseVo list(SearchParam searchParam) {
        // 生成dsl搜索请求
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        SearchRequest searchRequest = buildQueryDsl(searchParam);
        // 执行查询操作，restHighLevelClient.search
        SearchResponse search = null;
        try {
            search = restHighLevelClient.search(searchRequest, RequestOptions.DEFAULT);
        } catch (IOException e) {
            e.printStackTrace();
        }
        // 解析返回结果
        searchResponseVo = parseSearchResult(search);
        return searchResponseVo;
    }

    private SearchResponseVo parseSearchResult(SearchResponse search){
        SearchResponseVo searchResponseVo = new SearchResponseVo();
        List<Goods> goodsList = new ArrayList<>();
        // 解析返回结果
        SearchHits hits = search.getHits();
        for (SearchHit hit : hits) {
            // 解析商品数据
            String sourceAsString = hit.getSourceAsString();
            Goods goods = JSON.parseObject(sourceAsString,Goods.class);
            // 解析设置高亮
            Map<String, HighlightField> highlightFields = hit.getHighlightFields();
            if (highlightFields != null && highlightFields.size()>0){
                HighlightField title = highlightFields.get("title");
                String highlightTitle = title.fragments()[0].string();
                goods.setTitle(highlightTitle);
            }

            goodsList.add(goods);
        }

        // 解析商品标签聚合
        Map<String, Aggregation> stringAggregationMap = search.getAggregations().asMap();

        ParsedLongTerms tmIdAgg = (ParsedLongTerms)stringAggregationMap.get("tmIdAgg");
        List<SearchResponseTmVo> searchResponseTmVoList = tmIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseTmVo searchResponseTmVo = new SearchResponseTmVo();

            long tmId = bucket.getKeyAsNumber().longValue();
            searchResponseTmVo.setTmId(tmId);

            Map<String, Aggregation> stringAggregationMapTmNameAndIdAgg = bucket.getAggregations().asMap();
            ParsedStringTerms tmNameAgg = (ParsedStringTerms) stringAggregationMapTmNameAndIdAgg.get("tmNameAgg");
            // tmNameAgg.getBuckets() 得到该聚合下所有的buckets,然后get(0)
            String tmName = tmNameAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmName(tmName);

            ParsedStringTerms tmLogoUrlAgg = (ParsedStringTerms) stringAggregationMapTmNameAndIdAgg.get("tmLogoUrlAgg");
            String tmLogoUrl = tmLogoUrlAgg.getBuckets().get(0).getKeyAsString();
            searchResponseTmVo.setTmLogoUrl(tmLogoUrl);

            return searchResponseTmVo;
        }).collect(Collectors.toList());

        // 解析商品平台属性聚合信息
        ParsedNested attrsAgg = (ParsedNested) stringAggregationMap.get("attrsAgg");
        ParsedLongTerms attrIdAgg = attrsAgg.getAggregations().get("attrIdAgg");
        List<SearchResponseAttrVo> searchResponseAttrVos = attrIdAgg.getBuckets().stream().map(bucket -> {
            SearchResponseAttrVo searchResponseAttrVo = new SearchResponseAttrVo();

            Long attrId = bucket.getKeyAsNumber().longValue();

            Map<String, Aggregation> stringAggregationMap1 = bucket.getAggregations().asMap();
            ParsedStringTerms attrValueAgg = (ParsedStringTerms) stringAggregationMap1.get("attrValueAgg");
            List<String> attrValueAggList = attrValueAgg.getBuckets().stream().map(bucket1 -> {
                String attrValueAggKey = bucket1.getKeyAsString();
                return attrValueAggKey;
            }).collect(Collectors.toList());

            ParsedStringTerms attrNameAgg = (ParsedStringTerms) stringAggregationMap1.get("attrNameAgg");
            String attrName = attrNameAgg.getBuckets().get(0).getKeyAsString();

            searchResponseAttrVo.setAttrId(attrId);
            searchResponseAttrVo.setAttrValueList(attrValueAggList);
            searchResponseAttrVo.setAttrName(attrName);

            return searchResponseAttrVo;
        }).collect(Collectors.toList());


        searchResponseVo.setGoodsList(goodsList); //设置搜索解析结果
        searchResponseVo.setTrademarkList(searchResponseTmVoList); //设置标签聚合解析结果
        searchResponseVo.setAttrsList(searchResponseAttrVos); //设置平台属性解析结果
        return searchResponseVo;
    }

    private SearchRequest buildQueryDsl(SearchParam searchParam){
        /*String keyword = searchParam.getKeyword();
        Long category3Id = searchParam.getCategory3Id();
        String[] props = searchParam.getProps();
        String order = searchParam.getOrder();
        String trademark = searchParam.getTrademark();

        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (StringUtils.isNotBlank(keyword)){
            boolQueryBuilder.must(new MatchQueryBuilder("title",keyword));
            //高亮显示
            //TODO 第二遍自己写
        }*/

        return getSearchRequest(searchParam);
    }

    private SearchRequest getSearchRequest(SearchParam searchParam) {
        // 获取搜索参数，三级分类id和keyword有且只有一个
        String keyword = searchParam.getKeyword(); // 特殊可选
        Long category3Id = searchParam.getCategory3Id(); // 特殊可选

        String[] props = searchParam.getProps();
        String trademark = searchParam.getTrademark();
        String order = searchParam.getOrder();
//        Integer pageNo = searchParam.getPageNo();
//        Integer pageSize = searchParam.getPageSize();

        // 定义dsl语句
        SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
        BoolQueryBuilder boolQueryBuilder = new BoolQueryBuilder();
        if (StringUtils.isNotBlank(keyword)){
            boolQueryBuilder.must(new MatchQueryBuilder("title",keyword));
            //设置高亮显示
            HighlightBuilder highlightBuilder = new HighlightBuilder();
            highlightBuilder.preTags("<span style='color:red;font-weight:bolder;'");
            highlightBuilder.field("title");
            highlightBuilder.postTags("</span>");
            // 设置高亮
            searchSourceBuilder.highlighter(highlightBuilder);
        }else if (null != category3Id && category3Id>0){
            boolQueryBuilder.filter(new TermQueryBuilder("category3Id",category3Id));
        }

        //设置属性搜索条件
        //设置props搜索条件
        if (props!=null && props.length>0){
            for (String prop : props) {
                String[] split = prop.split(":");
                String attrInfoId = split[0];
                String attrValue = split[1];
                String attrInfoName = split[2];

                BoolQueryBuilder boolQueryBuilderAttr = new BoolQueryBuilder();
                boolQueryBuilderAttr.filter(new TermQueryBuilder("attrs.attrId",attrInfoId));
                boolQueryBuilderAttr.must(new MatchQueryBuilder("attrs.attrValue",attrValue));
                boolQueryBuilderAttr.must(new MatchQueryBuilder("attrs.attrName",attrInfoName));
                //把搜索放到nested中去
                NestedQueryBuilder nestedQueryBuilder = new NestedQueryBuilder("attrs", boolQueryBuilderAttr, ScoreMode.None);
                boolQueryBuilder.filter(nestedQueryBuilder);
            }
        }

        //设置商标搜索条件
        if (trademark!=null && trademark.length()>0) {
            String[] split = trademark.split(":");
            String trademarkId = split[0];
            BoolQueryBuilder boolQueryBuilderTrademark = new BoolQueryBuilder();
            boolQueryBuilderTrademark.filter(new TermQueryBuilder("tmId",trademarkId));
            boolQueryBuilder.filter(boolQueryBuilderTrademark);
        }

        //设置排序
        if (StringUtils.isNotBlank(order)){
            String[] split = order.split(":");
            String type = split[0];
            String sort = split[1];
            if ("1".equals(type)){
                type = "hotScore";
            }else if ("2".equals(type)){
                type = "price";
            }

            searchSourceBuilder.sort(type,"asc".equals(sort)?SortOrder.ASC:SortOrder.DESC);
        }


        searchSourceBuilder.query(boolQueryBuilder);
        //品牌聚合
        TermsAggregationBuilder termsAggregationBuilderTmAgg = AggregationBuilders.terms("tmIdAgg").field("tmId")
                .subAggregation(AggregationBuilders.terms("tmNameAgg").field("tmName"))
                .subAggregation(AggregationBuilders.terms("tmLogoUrlAgg").field("tmLogoUrl"));
        //平台属性聚合
        NestedAggregationBuilder nestedAggregationBuilderAttrsAgg = AggregationBuilders.nested("attrsAgg", "attrs")
                .subAggregation(AggregationBuilders.terms("attrIdAgg").field("attrs.attrId")
                        .subAggregation(AggregationBuilders.terms("attrValueAgg").field("attrs.attrValue"))
                        .subAggregation(AggregationBuilders.terms("attrNameAgg").field("attrs.attrName"))
                );


        searchSourceBuilder.aggregation(termsAggregationBuilderTmAgg);
        searchSourceBuilder.aggregation(nestedAggregationBuilderAttrsAgg);

        System.out.println(searchSourceBuilder);

        // 封装SearchRequest
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.indices("goods"); // 设置查询index
        searchRequest.types("info"); // 设置查询type
        searchRequest.source(searchSourceBuilder);
        return searchRequest;
    }

}
