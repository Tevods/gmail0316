package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.SpuService;
import com.atguigu.gmall.product.test.FileUploadTest;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@CrossOrigin
@RequestMapping("admin/product")
public class SpuApiController {

    @Autowired
    private SpuService spuService;

    @GetMapping("{page}/{limit}")
    public Result getSpuPage(@PathVariable("page") Long page
            ,@PathVariable("limit") Long limit
            ,@RequestParam("category3Id") String category3Id){
        Page<SpuInfo> pageParam = new Page<>(page,limit);
        IPage<SpuInfo> iPage = spuService.getSpuPage(pageParam,category3Id);
        return Result.ok(iPage);
    }

    @GetMapping("baseSaleAttrList")
    public Result baseSaleAttrList(){
        List<BaseSaleAttr> baseSaleAttrs = spuService.baseSaleAttrList();
        return Result.ok(baseSaleAttrs);
    }

    @GetMapping("baseSaleAttrList/getTrademarkList")
    public Result getTrademarkList(){
        List<BaseSaleAttr> baseSaleAttrs = spuService.baseSaleAttrList();
        return Result.ok(baseSaleAttrs);
    }

    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        spuService.saveSpuInfo(spuInfo);
        return Result.ok();
    }

    @PostMapping("fileUpload")
    public Result<String> fileUpload(MultipartFile file) throws IOException, MyException {
        // 获取fdfs的全局配置信息
        String path = FileUploadTest.class.getClassLoader().getResource("tracker.conf").getPath();
        ClientGlobal.init(path);
        // 获取tracker
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = trackerClient.getConnection();
        // 获取storage
        StorageClient storageClient = new StorageClient(connection,null);
        // 上传
        // 获取文件的后缀
        String originalFilename = file.getOriginalFilename();
        int i = originalFilename.lastIndexOf(".");
        String substring = originalFilename.substring(i + 1);

        String[] strings = storageClient.upload_file(file.getBytes(),substring,null); //meta_list元数据，描述上传对象的数据
        // 返回url
        String url = "http://192.168.200.128:8080";
        for (String string : strings) {
            url = url + "/" + string;
        }
        return Result.ok(url);
    }

    @GetMapping("spuSaleAttrList/{spuId}")
    public Result spuSaleAttrList(@PathVariable("spuId") String spuId){
        List<SpuSaleAttr> spuSaleAttrList = spuService.spuSaleAttrList(spuId);
        return Result.ok(spuSaleAttrList);
    }

    @GetMapping("spuImageList/{spuId}")
    public Result spuImageList(@PathVariable("spuId") String spuId){
        List<SpuImage> spuImageList = spuService.spuImageList(spuId);
        return Result.ok(spuImageList);
    }
}
