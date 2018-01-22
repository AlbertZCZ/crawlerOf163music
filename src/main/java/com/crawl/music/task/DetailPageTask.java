package com.crawl.music.task;

import com.crawl.core.dao.ConnectionManager;
import com.crawl.core.parser.ListPageParser;
import com.crawl.core.util.Config;
import com.crawl.core.util.SimpleInvocationHandler;
import com.crawl.music.entity.Music;
import com.crawl.music.entity.Page;
import com.crawl.music.parser.DetailPageParser;
import org.apache.http.client.methods.HttpRequestBase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Create by zhang on 2018/1/12
 * 歌曲详情页下载
 */
public class DetailPageTask extends AbstractPageTask {

    private static Logger logger = LoggerFactory.getLogger(DetailPageTask.class);
    private static ListPageParser proxyDetailListPageParser;
    /**
     * Thread-数据库连接
     */
    private static Map<Thread, Connection> connectionMap = new ConcurrentHashMap<>();
    static {
        proxyDetailListPageParser = getProxyDetailListPageParser();
    }

    /**
     * 代理类
     * @return
     */
    private static ListPageParser getProxyDetailListPageParser() {
        ListPageParser detailPageParser = DetailPageParser.getInstance();
        InvocationHandler invocationHandler = new SimpleInvocationHandler(detailPageParser);
        ListPageParser proxyCommentListPageParser = (ListPageParser) Proxy.newProxyInstance(detailPageParser.getClass().getClassLoader(),
                detailPageParser.getClass().getInterfaces(), invocationHandler);
        return proxyCommentListPageParser;
    }

    public DetailPageTask(HttpRequestBase request, boolean proxyFlag) {
        super(request, proxyFlag);
    }
    @Override
    void retry() {
        musicHttpClient.getDetailListPageThreadPool().execute(new DetailPageTask(request, Config.isProxy));
    }

    @Override
    void handle(Page page) {
        if(!page.getHtml().contains("网易云音乐")){
            //代理异常，未能正确返回目标请求数据，丢弃
            currentProxy = null;
            return;
        }
        List<Music> list = proxyDetailListPageParser.parseListPage(page);
        if(Config.dbEnable && list != null) {
            Connection cn = getConnection();
            musicDao1.updateMusic(cn, list.get(0));
        }else if(!Config.dbEnable || musicHttpClient.getDetailListPageThreadPool().getActiveCount() == 1) {
//          parseMusicCount.incrementAndGet();
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
