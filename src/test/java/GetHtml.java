import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * Create by zhang on 2018/1/11
 */
public class GetHtml {
    public static void main(String[] args) {
        String startUrl = "https://music.163.com/playlist?id=159866743";
        CloseableHttpClient httpclient = HttpClients.createDefault();
        HttpGet httpget = new HttpGet(startUrl);
        httpget.setHeader("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36 Edge/16.17025");
        System.out.println("executing request " + httpget.getURI());
        try {
            CloseableHttpResponse response = httpclient.execute(httpget);
            String entity = EntityUtils.toString(response.getEntity());
            System.out.println(entity);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
