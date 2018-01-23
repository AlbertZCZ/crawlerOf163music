package com.crawl.music.parser;

import com.crawl.core.parser.ListPageParser;
import com.crawl.music.MusicHttpClient;
import com.crawl.music.entity.Comment;
import com.crawl.music.entity.Page;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Create by zhang on 2018/1/19
 * 评论解析器
 */
public class CommentsParser implements ListPageParser {
    private static CommentsParser instance;
    public static CommentsParser getInstance() {
        if (instance == null) {
            synchronized (MusicHttpClient.class) {
                if (instance == null) {
                    instance = new CommentsParser();
                }
            }
        }
        return instance;
    }

    @Override
    public List<Comment> parseListPage(Page page) {
        List<Comment> list = new ArrayList<>();
        //https://music.163.com/weapi/v1/resource/comments/R_SO_4_{id}?csrf_token=
        String url = page.getUrl();
        String songId = url.substring(url.indexOf("R_SO_4_") + "R_SO_4_".length(),url.indexOf("?"));

        Integer commentsLength = JsonPath.read(page.getHtml(),"$.comments.length()");
        try {
            Integer hotLength =  JsonPath.read(page.getHtml(),"$.hotComments.length()");
            for (int hot = 0;hot < hotLength;hot ++) {
                Comment comment = new Comment();
                String commetJsonPath = "$.hotComments[" + hot + "]";
                setCommentInfoByElement(comment,"content",page.getHtml(),commetJsonPath+".content");
                setCommentInfoByElement(comment,"time",page.getHtml(),commetJsonPath+".time");
                setCommentInfoByElement(comment,"likedCount",page.getHtml(),commetJsonPath+".likedCount");
                setCommentInfoByElement(comment,"commentId",page.getHtml(),commetJsonPath+".commentId");
                setCommentInfoByElement(comment,"userId",page.getHtml(),commetJsonPath+".user.userId");
                setCommentInfoByElement(comment,"nickname",page.getHtml(),commetJsonPath+".user.nickname");
                if ((int)JsonPath.read(page.getHtml(),"$.hotComments["+hot+"].beReplied.length()") > 0) {
                    setCommentInfoByElement(comment,"beReplied",page.getHtml(),commetJsonPath+".beReplied.userId");
                }else {
                    comment.setBeReplied(0);
                }
                comment.setSongId(songId);
                list.add(comment);
            }
        }catch (Exception e) {

        }
        for (int i = 0;i < commentsLength;i ++) {
            Comment comment = new Comment();
            String commetJsonPath = "$.comments[" + i + "]";
            setCommentInfoByElement(comment,"content",page.getHtml(),commetJsonPath+".content");
            setCommentInfoByElement(comment,"time",page.getHtml(),commetJsonPath+".time");
            setCommentInfoByElement(comment,"likedCount",page.getHtml(),commetJsonPath+".likedCount");
            setCommentInfoByElement(comment,"commentId",page.getHtml(),commetJsonPath+".commentId");
            setCommentInfoByElement(comment,"userId",page.getHtml(),commetJsonPath+".user.userId");
            setCommentInfoByElement(comment,"nickname",page.getHtml(),commetJsonPath+".user.nickname");
            if ((int)JsonPath.read(page.getHtml(),"$.comments["+i+"].beReplied.length()") > 0) {
                setCommentInfoByElement(comment,"beReplied",page.getHtml(),commetJsonPath+".beReplied.userId");
            }else {
                comment.setBeReplied(0);
            }
            comment.setSongId(songId);
            list.add(comment);
        }
        return list;
    }

    /**
     * jsonPath获取值，并通过反射直接注入到comment中
     * @param comment bean
     * @param fieldName 字段名
     * @param jsonPath jsonPath
     */
    private void setCommentInfoByElement(Comment comment, String fieldName ,String json, String jsonPath){
        try {
            Object o = JsonPath.read(json,jsonPath);
            Field field = comment.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(comment, o);
        } catch (PathNotFoundException e1) {
            //no results
        } catch (Exception e){
            e.printStackTrace();
        }
    }
}
