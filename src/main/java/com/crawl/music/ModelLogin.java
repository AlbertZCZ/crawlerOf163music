package com.crawl.music;

import com.crawl.core.util.EncryptTools;
import org.apache.commons.codec.binary.Hex;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;


/**
 * 模拟登录
 */
public class ModelLogin {
    private static Logger logger = LoggerFactory.getLogger(ModelLogin.class);
    /**
     *
     * @param username 邮箱或手机号码
     * @param password 密码
     * @return
     */
    public boolean login(String username, String password) throws Exception {
        password = EncryptTools.md5(password);
        // 私钥，随机16位字符串（自己可改）
        String secKey = "cd859f54539b24b7";
        String text = "{\"username\": \"" + username + "\", \"rememberLogin\": \"true\", \"password\": \"" + password
                + "\"}";
        String modulus = "00e0b509f6259df8642dbc35662901477df22677ec152b5ff68ace615bb7b725152b3ab17a876aea8a5aa76d2e417629ec4ee341f56135fccf695280104e0312ecbda92557c93870114af6c9d05c4f7f0c3685b7a46bee255932575cce10b424d813cfe4875d3e82047b97ddef52741d546b8e289dc6935b3ece0462db0a22b8e7";
        String nonce = "0CoJUm6Qyw8W8jud";
        String pubKey = "010001";
        // 2次AES加密，得到params
        String params = EncryptTools.encrypt(EncryptTools.encrypt(text, nonce), secKey);
        StringBuffer stringBuffer = new StringBuffer(secKey);
        // 逆置私钥
        secKey = stringBuffer.reverse().toString();
        String hex = Hex.encodeHexString(secKey.getBytes());
        BigInteger bigInteger1 = new BigInteger(hex, 16);
        BigInteger bigInteger2 = new BigInteger(pubKey, 16);
        BigInteger bigInteger3 = new BigInteger(modulus, 16);
        // RSA加密计算
        BigInteger bigInteger4 = bigInteger1.pow(bigInteger2.intValue()).remainder(bigInteger3);
        String encSecKey = Hex.encodeHexString(bigInteger4.toByteArray());
        // 字符填充
        encSecKey = EncryptTools.zfill(encSecKey, 256);
        Map<String,String> cookie = new HashMap<>();
        cookie.put("JSESSIONID-WYYY","o9c%5Cwp3TCMFIKnyCl1Mt1S71TH%2B1TMn6VCAvlmouFzYSm0akoVYb50pgayDSHpXQzSfC4spTPtd090J%5CtsZmaUzvqgyUBv0A%5CPzvqKuksnnV2PJAb3N3ioXcxa7D6woOsi05IfSnv4QMcmoxtkOI5fBHm4cHQ0wwZct5EXJw%5CW7PWrWH%3A1515580999423");

        // 登录请求
        Document document = Jsoup.connect("https://music.163.com/weapi/login?csrf_token=").cookies(cookie)
                .header("Referer", "https://music.163.com/")
                .header("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/63.0.3239.132 Safari/537.36")
                .data("params", params).data("encSecKey", encSecKey)
                .ignoreContentType(true).post();
        logger.info("登录结果：" + document.text());
        return true;
    }
}
