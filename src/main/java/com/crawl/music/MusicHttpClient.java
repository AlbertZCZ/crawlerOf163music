package com.crawl.music;

import com.crawl.Main;
import com.crawl.core.httpclient.AbstractHttpClient;
import com.crawl.core.httpclient.IHttpClient;
import com.crawl.core.util.*;
import com.crawl.music.task.MusicListPageTask;
import com.crawl.proxy.ProxyHttpClient;
import com.crawl.music.dao.MusicDao1Imp;
import org.apache.http.client.methods.HttpGet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;


public class MusicHttpClient extends AbstractHttpClient implements IHttpClient{
    private static Logger logger = LoggerFactory.getLogger(Main.class);
    private volatile static MusicHttpClient instance;
    /**
     * 统计歌曲数量
     */
    public static AtomicInteger parseMusicCount = new AtomicInteger(0);
    /**
     * 统计评论数量
     */
    public static AtomicInteger parseCommentCount = new AtomicInteger(0);

    private static long startTime = System.currentTimeMillis();
    public static volatile boolean isStop = false;

    public static MusicHttpClient getInstance(){
        if (instance == null){
            synchronized (MusicHttpClient.class){
                if (instance == null){
                    instance = new MusicHttpClient();
                }
            }
        }
        return instance;
    }
    /**
     * 歌曲详情页下载线程池(评论页面)
     */
    private ThreadPoolExecutor detailPageThreadPool;
    /**
     * 评论下载线程池
     */
    private ThreadPoolExecutor commentsPageThreadPool;
    /**
     * 歌曲列表页下载线程池
     */
    private ThreadPoolExecutor detailListPageThreadPool;
    /**
     * request　header
     * 获取列表页时，必须带上
     */
    private static String authorization;
    private MusicHttpClient() {
        initHttpClient();
        intiThreadPool();
    }
    /**
     * 初始化HttpClient
     */
    @Override
    public void initHttpClient() {
        if(Config.dbEnable){
            MusicDao1Imp.DBTablesInit();
        }
    }

    /**
     * 初始化线程池
     */
    private void intiThreadPool(){
        detailPageThreadPool = new SimpleThreadPoolExecutor(Config.downloadThreadSize,
                Config.downloadThreadSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(),
                "detailPageThreadPool");
        commentsPageThreadPool = new SimpleThreadPoolExecutor(50, 80,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(5000),
                new ThreadPoolExecutor.DiscardPolicy(), "listPageThreadPool");
        new Thread(new ThreadPoolMonitor(detailPageThreadPool, "DetailPageThreadPool")).start();
        new Thread(new ThreadPoolMonitor(commentsPageThreadPool, "CommentsPageThreadPool")).start();
        detailListPageThreadPool = new SimpleThreadPoolExecutor(Config.downloadThreadSize,
                Config.downloadThreadSize,
                0L, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(2000),
                new ThreadPoolExecutor.DiscardPolicy(),
                "detailListPageThreadPool");
        new Thread(new ThreadPoolMonitor(detailListPageThreadPool, "DetailListPageThreadPool")).start();

    }
    public void startCrawl(String url){
//        detailPageThreadPool.execute(new DetailPageTask(url, Config.isProxy));
//        manageHttpClient();
    }

    @Override
    public void startCrawl() {
        //authorization = initAuthorization();

        String startToken = Config.startUserToken;
        String startUrl = String.format(Constants.USER_FOLLOWEES_URL, startToken, 0);
        HttpGet request = new HttpGet(startUrl);
        //request.setHeader("authorization", "oauth " + MusicHttpClient.getAuthorization());
        detailListPageThreadPool.execute(new MusicListPageTask(request, Config.isProxy));
        manageHttpClient();
    }


    public static String getAuthorization(){
        return authorization;
    }
    /**
     * 管理客户端
     * 关闭整个爬虫
     */
    public void manageHttpClient(){
        while (true) {
            /**
             * 下载网页数
             */
            long downloadPageCount = detailListPageThreadPool.getTaskCount();

            if (downloadPageCount >= Config.downloadPageCount &&
                    !detailListPageThreadPool.isShutdown()) {
                isStop = true;
                ThreadPoolMonitor.isStopMonitor = true;
                detailListPageThreadPool.shutdown();
            }
            if(detailListPageThreadPool.isTerminated()){
                //关闭数据库连接
                Map<Thread, Connection> map = MusicListPageTask.getConnectionMap();
                for(Connection cn : map.values()){
                    try {
                        if (cn != null && !cn.isClosed()){
                            cn.close();
                        }
                    } catch (SQLException e) {
                        e.printStackTrace();
                    }
                }
                //关闭代理检测线程池
                ProxyHttpClient.getInstance().getProxyTestThreadExecutor().shutdownNow();
                //关闭代理下载页线程池
                ProxyHttpClient.getInstance().getProxyDownloadThreadExecutor().shutdownNow();
                break;
            }
            double costTime = (System.currentTimeMillis() - startTime) / 1000.0;//单位s
            logger.debug("抓取速率：" + parseMusicCount.get() / costTime + "个/s");
//            logger.info("downloadFailureProxyPageSet size:" + ProxyHttpClient.downloadFailureProxyPageSet.size());
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
    public ThreadPoolExecutor getDetailPageThreadPool() {
        return detailPageThreadPool;
    }

    public ThreadPoolExecutor getCommentsPageThreadPool() {
        return commentsPageThreadPool;
    }

    public ThreadPoolExecutor getDetailListPageThreadPool() {
        return detailListPageThreadPool;
    }

}
