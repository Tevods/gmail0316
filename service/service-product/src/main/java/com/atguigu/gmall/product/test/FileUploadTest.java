package com.atguigu.gmall.product.test;

import org.csource.common.MyException;
import org.csource.fastdfs.ClientGlobal;
import org.csource.fastdfs.StorageClient;
import org.csource.fastdfs.TrackerClient;
import org.csource.fastdfs.TrackerServer;

import java.io.IOException;

public class FileUploadTest {
    public static void main(String[] args) throws IOException, MyException {
        // 获取fdfs的全局配置信息
        String path = FileUploadTest.class.getClassLoader().getResource("tracker.conf").getPath();
        ClientGlobal.init(path);
        // 获取tracker
        TrackerClient trackerClient = new TrackerClient();
        TrackerServer connection = trackerClient.getConnection();
        // 获取storage
        StorageClient storageClient = new StorageClient(connection,null);
        // 上传
        String[] strings = storageClient.upload_file("C:/Users/10750/Pictures/壁纸/wallhaven-k93oed.jpg","jpg",null);
        // 返回url
        String url = "";
        for (String string : strings) {
            url = url + "/" + string;
        }
        System.out.println(url);
    }

}
