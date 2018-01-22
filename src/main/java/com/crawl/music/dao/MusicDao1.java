package com.crawl.music.dao;


import com.crawl.music.entity.Comment;
import com.crawl.music.entity.Music;

import java.sql.Connection;
import java.sql.SQLException;

public interface MusicDao1 {

    boolean isExistRecord(String sql) throws SQLException;

    boolean isExistRecord(Connection cn, String sql) throws SQLException;

    boolean isExistMusic(String userToken);

    boolean isExistMusic(Connection cn, String id);

    boolean insertMusic(Music music);

    boolean insertMusic(Connection cn, Music music);

    boolean updateMusic(Connection cn, Music music);

    boolean insertComment(Connection cn, Comment comment);
    boolean isExistComment(Connection cn, Integer id);
    /**
     * 插入url,插入成功返回true，若已存在该url则返回false
     * @param cn
     * @param md5Url
     * @return
     */
    boolean insertUrl(Connection cn, String md5Url);
}
