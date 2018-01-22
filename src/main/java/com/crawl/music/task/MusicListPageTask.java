package com.crawl.music.task;


import com.crawl.core.dao.ConnectionManager;
import com.crawl.core.parser.ListPageParser;
import com.crawl.core.util.Config;
import com.crawl.core.util.SimpleInvocationHandler;
import com.crawl.music.entity.Music;
import com.crawl.music.entity.Page;
import com.crawl.music.parser.MusicListPageParser;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.crawl.music.MusicHttpClient.parseMusicCount;

/**
 * 歌曲列表任务
 */
public class MusicListPageTask extends AbstractPageTask {
    private static Logger logger = LoggerFactory.getLogger(MusicListPageTask.class);
    private static ListPageParser proxyMusicListPageParser;
    /**
     * Thread-数据库连接
     */
    private static Map<Thread, Connection> connectionMap = new ConcurrentHashMap<>();
    static {
        proxyMusicListPageParser = getProxyMusicListPageParser();
    }


    public MusicListPageTask(HttpRequestBase request, boolean proxyFlag) {
        super(request, proxyFlag);
    }

    /**
     * 代理类
     * @return
     */
    private static ListPageParser getProxyMusicListPageParser() {
        ListPageParser musicListPageParser = MusicListPageParser.getInstance();
        InvocationHandler invocationHandler = new SimpleInvocationHandler(musicListPageParser);
        ListPageParser proxyMusicListPageParser = (ListPageParser) Proxy.newProxyInstance(musicListPageParser.getClass().getClassLoader(),
                musicListPageParser.getClass().getInterfaces(), invocationHandler);
        return proxyMusicListPageParser;
    }

    @Override
    void retry() {
        musicHttpClient.getDetailListPageThreadPool().execute(new MusicListPageTask(request, Config.isProxy));
    }

    @Override
    void handle(Page page) {
        if(!page.getHtml().contains("网易云音乐")) {
            //代理异常，未能正确返回目标请求数据，丢弃
            currentProxy = null;
            return;
        }
        List<Music> list = proxyMusicListPageParser.parseListPage(page);
        for(Music music : list) {
            logger.info("解析歌曲成功:" + music.toString());
            if(Config.dbEnable) {
                Connection cn = getConnection();
                if (musicDao1.insertMusic(cn, music)) {
                    parseMusicCount.incrementAndGet();
                }
            }else if(!Config.dbEnable || musicHttpClient.getDetailListPageThreadPool().getActiveCount() == 1) {
                parseMusicCount.incrementAndGet();
            }
            String detialUrl = music.getUrl();

            //防止死锁
            logger.info(musicHttpClient.getDetailPageThreadPool().getActiveCount()+"");
            //if (musicHttpClient.getDetailPageThreadPool().getActiveCount() == 0) {
            HttpGet request = new HttpGet(detialUrl);
            if (musicHttpClient.getDetailPageThreadPool().getQueue().size() > 1000) {
                continue;
            }
            //抓取歌曲详情页面
            musicHttpClient.getDetailPageThreadPool().execute(new DetailPageTask(request, Config.isProxy));
            if (musicHttpClient.getCommentsPageThreadPool().getQueue().size() > 1000)
                continue;
            //抓取评论页面
            musicHttpClient.getCommentsPageThreadPool().execute(new CommentsTask(music.getId(),Config.isProxy));
            //}
        }
    }

    /**
     * 每个thread维护一个Connection
     * @return
     */
    private Connection getConnection() {
        Thread currentThread = Thread.currentThread();
        Connection cn;
        if (!connectionMap.containsKey(currentThread)) {
            cn = ConnectionManager.createConnection();
            connectionMap.put(currentThread, cn);
        }else {
            cn = connectionMap.get(currentThread);
        }
        return cn;
    }

    public static Map<Thread, Connection> getConnectionMap() {
        return connectionMap;
    }

}
