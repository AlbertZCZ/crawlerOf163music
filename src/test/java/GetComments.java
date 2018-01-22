import com.crawl.core.util.Constants;
import com.crawl.core.util.EncryptTools;
import com.crawl.core.util.HttpClientUtil;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by zhang on 2018/1/17
 */
public class GetComments {


    public static void main(String[] args) {
        String url = "https://music.163.com/weapi/v1/resource/comments/R_SO_4_167727?csrf_token=";
        String secKey = new BigInteger(100, new SecureRandom()).toString(32).substring(0, 16);
        String encText;
        String encSecKey;
        try {
            encText = EncryptTools.aesEncrypt(EncryptTools.aesEncrypt(Constants.DEFALT_TEXT.replace("{offset}","2"), "0CoJUm6Qyw8W8jud"), secKey);
            encSecKey = EncryptTools.rsaEncrypt(secKey);
            HttpPost post = new HttpPost(url);
            post.setHeader("Referer","https://music.163.com/song?id=167727");
            List<NameValuePair> params= new ArrayList<>();
            //建立一个NameValuePair数组，用于存储欲传送的参数
            params.add(new BasicNameValuePair("params",encText));
            params.add(new BasicNameValuePair("encSecKey",encSecKey));
            post.setEntity(new UrlEncodedFormEntity(params));
            HttpResponse response = HttpClientUtil.getResponse(post);
            if(response.getStatusLine().getStatusCode()==200)
                System.out.println(EntityUtils.toString(response.getEntity()));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
