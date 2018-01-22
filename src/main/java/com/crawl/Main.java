package com.crawl;

import com.crawl.music.MusicHttpClient;
import com.crawl.proxy.ProxyHttpClient;

/**
 * 爬虫入口
 */
public class Main {
    //private static Logger logger = LoggerFactory.getLogger(Main.class);
    public static void main(String args []){
        ProxyHttpClient.getInstance().startCrawl();
        MusicHttpClient.getInstance().startCrawl();
    }
}
