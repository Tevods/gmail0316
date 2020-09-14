package com.atguigu.gmall.all.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.list.SearchAttr;
import com.atguigu.gmall.model.list.SearchParam;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class ListController {
    @Autowired
    private ListFeignClient listFeignClient;

    @RequestMapping({"list.html","search.html"})
    public String list(SearchParam searchParam, Model model){
        // 使用搜索服务，查询结果
        Result<Map> result = listFeignClient.list(searchParam);
        model.addAllAttributes(result.getData());
        model.addAttribute("searchParam",searchParam);
        model.addAttribute("urlParam",getUrlParam(searchParam));
        model.addAttribute("orderMap",getOrderMap(searchParam));
        //面包屑设置
        String trademark = searchParam.getTrademark();
        if (StringUtils.isNotBlank(trademark)){
           model.addAttribute("trademarkParam",trademark.split(":")[1]);
        }

        String[] props = searchParam.getProps();
        if (null != props && props.length>0){
            List<SearchAttr> searchAttrs = new ArrayList<>();
            for (String prop : props) {
                String[] split = prop.split(":");

                String attrId = split[0];
                String attrValue = split[1];
                String attrName = split[2];

                SearchAttr searchAttr = new SearchAttr();
                searchAttr.setAttrId(Long.parseLong(attrId));
                searchAttr.setAttrValue(attrValue);
                searchAttr.setAttrName(attrName);

                searchAttrs.add(searchAttr);
            }
            model.addAttribute("propsParamList",searchAttrs);
        }


        return "list/index";
    }

    private Map<String, String> getOrderMap(SearchParam searchParam) {
        String order = searchParam.getOrder();
        Map<String,String> orderMap = new HashMap<>();

        if (StringUtils.isNotBlank(order)){
            String[] split = order.split(":");
            String type = split[0];
            String sort = split[1];
            orderMap.put("type",type);
            orderMap.put("sort",sort);
        }else {
            orderMap.put("type","1");
            orderMap.put("sort","desc");
        }
        return orderMap;
    }

    private String getUrlParam(SearchParam searchParam) {
        String keyword = searchParam.getKeyword();
        Long category3Id = searchParam.getCategory3Id();
        String[] props = searchParam.getProps();
        String trademark = searchParam.getTrademark();

        String urlParam = "list.html?";
        // 设置只查询关键字/只查询三级分类id/优先查询关键字
        boolean keywordFlag = StringUtils.isNotBlank(keyword);
        boolean categoryFlag = category3Id != null && category3Id > 0;
        if (keywordFlag){
            urlParam = "search.html?";
            urlParam = urlParam + "keyword=" + keyword;
        }else if (categoryFlag){
            urlParam = urlParam + "category3Id=" + category3Id;
        }
        boolean trademarkFlag = StringUtils.isNotBlank(trademark);
        if (trademarkFlag){
            urlParam = urlParam + "&trademark=" + trademark;
        }

        if (null!=props && props.length>0){
            for (String prop : props) {
                urlParam = urlParam + "&props=" + prop;
            }
        }
        return urlParam;
    }
}
