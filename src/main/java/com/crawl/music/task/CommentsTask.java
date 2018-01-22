package com.crawl.music.task;

import com.crawl.core.dao.ConnectionManager;
import com.crawl.core.parser.ListPageParser;
import com.crawl.core.util.*;
import com.crawl.music.entity.Comment;
import com.crawl.music.entity.Page;
import com.crawl.music.parser.CommentsParser;
import com.crawl.proxy.ProxyPool;
import com.crawl.proxy.entity.Direct;
import com.crawl.proxy.util.ProxyUtil;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpPost;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.crawl.music.MusicHttpClient.parseCommentCount;

/**
 * Create by zhang on 2018/1/18
 * 抓取评论任务
 */
public class CommentsTask extends AbstractPageTask {
    private static Logger logger = LoggerFactory.getLogger(CommentsTask.class);
    private static ListPageParser proxyCommentParser;
    private static Map<Thread, Connection> connectionMap = new ConcurrentHashMap<>();
    private String musicId;
    private boolean proxyFlag;
    private int commentsNum = 0;

    static {
        proxyCommentParser = getProxyCommentParser();
    }

    /**
     * 代理类
     * @return
     */
    private static ListPageParser getProxyCommentParser() {
        ListPageParser commentParser = CommentsParser.getInstance();
        InvocationHandler invocationHandler = new SimpleInvocationHandler(commentParser);
        ListPageParser proxyCommentParser = (ListPageParser) Proxy.newProxyInstance(commentParser.getClass().getClassLoader(),
                commentParser.getClass().getInterfaces(),invocationHandler);
        return proxyCommentParser;
    }

    public CommentsTask(String musicId, boolean proxyFlag) {
        this.musicId = musicId;
        this.proxyFlag = proxyFlag;
    }

    public CommentsTask(String musicId, boolean proxyFlag,int commentsNum) {
        this.musicId = musicId;
        this.proxyFlag = proxyFlag;
        this.commentsNum = commentsNum;
    }

    @Override
    void retry() {
        musicHttpClient.getCommentsPageThreadPool().execute(new CommentsTask(musicId, Config.isProxy));
    }

    /**
     * 特殊需求，重写run方法
     */
    @Override
    public void run() {
        long requestStartTime = 0l;
        HttpPost tempRequest = null;
        HttpPost post = null;
        try {
            Page page = null;
            if(musicId != null) {
                String commentUrl = Constants.COMMENTURL.replace("{id}",musicId);
                post = CommentsUtil.getPost(commentUrl,commentsNum);
                post.setHeader("Referer","https://music.163.com/song?id="+musicId);
                if (proxyFlag){
                    currentProxy = ProxyPool.proxyQueue.take();
                    if(!(currentProxy instanceof Direct)) {
                        HttpHost proxy = new HttpHost(currentProxy.getIp(), currentProxy.getPort());
                        post.setConfig(HttpClientUtil.getRequestConfigBuilder().setProxy(proxy).build());
                    }
                    requestStartTime = System.currentTimeMillis();
                    page = musicHttpClient.getWebPage(post);
                }else {
                    requestStartTime = System.currentTimeMillis();
                    page = musicHttpClient.getWebPage(post);
                }
            }
            long requestEndTime = System.currentTimeMillis();
            page.setProxy(currentProxy);
            int status = page.getStatusCode();
            String logStr = Thread.currentThread().getName() + " " + currentProxy + "  executing request " + page.getUrl()  +
                    " response statusCode:" + status + "  request cost time:" + (requestEndTime - requestStartTime) + "ms";
            if(status == HttpStatus.SC_OK) {
                if (page.getHtml().contains("music")) {
                    logger.debug(logStr);
                    if (currentProxy != null) {
                        currentProxy.setSuccessfulTimes(currentProxy.getSuccessfulTimes() + 1);
                        currentProxy.setSuccessfulTotalTime(currentProxy.getSuccessfulTotalTime() + (requestEndTime - requestStartTime));
                        double aTime = (currentProxy.getSuccessfulTotalTime() + 0.0) / currentProxy.getSuccessfulTimes();
                        currentProxy.setSuccessfulAverageTime(aTime);
                        currentProxy.setLastSuccessfulTime(System.currentTimeMillis());
                    }
                    handle(page);
                }else {
                     //代理异常，没有正确返回目标url
                    logger.warn("proxy exception:" + currentProxy.toString());
                }
            }
            /**
             * 401--不能通过验证
             */
            else if(status == 404 || status == 401 || status == 410) {
                logger.warn(logStr);
            }else {
                logger.error(logStr);
                Thread.sleep(100);
                retry();
            }
        } catch (InterruptedException e) {
            logger.error("InterruptedException", e);
        } catch (IOException e) {
            if(currentProxy != null) {
                // 该代理可用，将该代理继续添加到proxyQueue
                currentProxy.setFailureTimes(currentProxy.getFailureTimes() + 1);
            }
            if(!musicHttpClient.getDetailListPageThreadPool().isShutdown()) {
                retry();
            }
        } finally {
            if (post != null) {
                post.releaseConnection();
            }
            if (tempRequest != null) {
                tempRequest.releaseConnection();
            }
            if (currentProxy != null && !ProxyUtil.isDiscardProxy(currentProxy)) {
                currentProxy.setTimeInterval(Constants.TIME_INTERVAL);
                ProxyPool.proxyQueue.add(currentProxy);
            }
        }
    }

    @Override
    void handle(Page page) {
        if(!page.getHtml().contains("{")) {
            //代理异常，未能正确返回目标请求数据，丢弃
            currentProxy = null;
            return;
        }
        boolean more = JsonPath.read(page.getHtml(),"$.more");
        List<Comment> commentList = proxyCommentParser.parseListPage(page);
        for (Comment comment:commentList) {
            logger.info("解析评论成功:" + comment.toString());
            if(Config.dbEnable) {
                Connection cn = getConnection();
                if (musicDao1.insertComment(cn, comment)) {
                    parseCommentCount.incrementAndGet();
                }
            }else if(!Config.dbEnable || musicHttpClient.getCommentsPageThreadPool().getActiveCount() == 1) {
                parseCommentCount.incrementAndGet();
            }
        }
        //继续抓取下一页评论
        if (more) {
            musicHttpClient.getCommentsPageThreadPool().execute(new CommentsTask(musicId, Config.isProxy,commentsNum + 10));
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
}
