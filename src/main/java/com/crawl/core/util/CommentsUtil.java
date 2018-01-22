package com.crawl.core.util;

import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.message.BasicNameValuePair;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by zhang on 2018/1/18
 */
public class CommentsUtil {
    public static HttpPost getPost(String url,int index) {
        String encText;
        String encSecKey;
        try {
            String secKey = new BigInteger(100, new SecureRandom()).toString(32).substring(0, 16);
            if (index == 0) {
                encText = EncryptTools.aesEncrypt(EncryptTools.aesEncrypt(Constants.DEFALT_TEXT, "0CoJUm6Qyw8W8jud"), secKey);
            }else {
                encText = EncryptTools.aesEncrypt(EncryptTools.aesEncrypt(Constants.TEXT.replace("{offset}",String.valueOf(index)), "0CoJUm6Qyw8W8jud"), secKey);
            }
            encSecKey = EncryptTools.rsaEncrypt(secKey);
            HttpPost post = new HttpPost(url);
            List<NameValuePair> params= new ArrayList<>();
            //建立一个NameValuePair数组，用于存储欲传送的参数
            params.add(new BasicNameValuePair("params",encText));
            params.add(new BasicNameValuePair("encSecKey",encSecKey));
            post.setEntity(new UrlEncodedFormEntity(params));
            return post;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
