package com.crawl.music.dao;


import com.crawl.core.dao.ConnectionManager;
import com.crawl.core.util.EncryptTools;
import com.crawl.music.entity.Comment;
import com.crawl.music.entity.Music;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.*;
import java.util.Properties;

public class MusicDao1Imp implements MusicDao1 {
    private static Logger logger = LoggerFactory.getLogger(MusicDao1.class);
    public static void DBTablesInit() {
        ResultSet rs;
        Properties p = new Properties();
        Connection cn = ConnectionManager.getConnection();
        try {
            //加载properties文件
            p.load(MusicDao1Imp.class.getResourceAsStream("/config.properties"));
            rs = cn.getMetaData().getTables(null, null, "url", null);
            Statement st = cn.createStatement();
            //不存在url表
            if(!rs.next()){
                //创建url表
                st.execute(p.getProperty("createUrlTable"));
                logger.info("url表创建成功");
            }
            else{
                logger.info("url表已存在");
            }
            rs = cn.getMetaData().getTables(null, null, "music", null);
            //不存在user表
            if(!rs.next()){
                //创建user表
                st.execute(p.getProperty("createMusicTable"));
                logger.info("music表创建成功");
            }
            else{
                logger.info("music表已存在");
            }
            rs.close();
            st.close();
            cn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean isExistRecord(String sql) throws SQLException{
        logger.info(sql);
        return isExistRecord(ConnectionManager.getConnection(), sql);
    }

    @Override
    public boolean isExistRecord(Connection cn, String sql) throws SQLException {
        int num = 0;
        PreparedStatement pstmt;
        pstmt = cn.prepareStatement(sql);
        ResultSet rs = pstmt.executeQuery();
        while(rs.next()){
            num = rs.getInt("count(0)");
        }
        rs.close();
        pstmt.close();
        if(num == 0){
            return false;
        }else{
            return true;
        }
    }

    @Override
    public boolean isExistMusic(String userToken) {
        return isExistMusic(ConnectionManager.getConnection(), userToken);
    }

    @Override
    public boolean isExistMusic(Connection cn, String id) {
        String isContainSql = "select count(0) from music WHERE id='" + id + "'";
        try {
            if(isExistRecord(isContainSql)){
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean insertMusic(Music u) {
        return insertMusic(ConnectionManager.getConnection(), u);
    }

    @Override
    public boolean insertMusic(Connection cn, Music u) {
        try {
            if (isExistMusic(cn, u.getId())){
                return false;
            }
            String column = "id,singer,album,name,url";
            String values = "?,?,?,?,?";
            String sql = "insert into music (" + column + ") values(" +values+")";
            PreparedStatement pstmt;
            pstmt = cn.prepareStatement(sql);
            pstmt.setString(1,u.getId());
            pstmt.setString(2,u.getSinger());
            pstmt.setString(3,u.getAlbum());
            pstmt.setString(4,u.getName());
            pstmt.setString(5,u.getUrl());
            pstmt.executeUpdate();
            pstmt.close();
            logger.info("插入数据库成功---" + u.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
//            ConnectionManager.close();
        }
        return true;
    }

    @Override
    public boolean updateMusic(Connection cn, Music music) {
        PreparedStatement pstmt = null;
        try {
            if (!isExistMusic(cn, music.getId())){
                logger.info("该歌曲不存在---id=" + music.getId());
                return false;
            }
            StringBuffer sql = new StringBuffer("UPDATE music SET id="+music.getId());//+music.getId();
            if (music.getAlbum() != null) {
                sql.append(",album='").append(music.getAlbum()).append("'");
            }
            if (music.getSinger() != null) {
                sql.append(",singer='").append(music.getSinger()).append("'");
            }
            sql.append(" where id='").append(music.getId()).append("'");

            pstmt = cn.prepareStatement(sql.toString());
            pstmt.executeUpdate();

            logger.info("更新歌曲成功---" + music.getName());
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
            try {
                if (pstmt != null)
                    pstmt.close();
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    @Override
    public boolean insertComment(Connection cn, Comment comment) {
        try {
            if (isExistComment(cn, comment.getCommentId())){
                return false;
            }
            String column = "commentId,songId,userId,nickname,content,time,beReplied,likedCount";
            String values = "?,?,?,?,?,?,?,?";
            String sql = "insert into comment (" + column + ") values(" +values+")";
            PreparedStatement pstmt;
            pstmt = cn.prepareStatement(sql);
            pstmt.setInt(1,comment.getCommentId());
            pstmt.setString(2,comment.getSongId());
            pstmt.setInt(3,comment.getUserId());
            pstmt.setString(4,comment.getNickname());
            pstmt.setString(5,comment.getContent());
            pstmt.setString(6, EncryptTools.stampToDate(comment.getTime()));
            pstmt.setInt(7,comment.getBeReplied() == null?0:comment.getBeReplied());
            pstmt.setInt(8,comment.getLikedCount());
            pstmt.executeUpdate();
            pstmt.close();
            logger.info("插入数据库成功---" + comment.getCommentId());
        }catch (Exception e) {
            e.printStackTrace();
            logger.error("错误content==="+comment.getContent());
        }finally {

        }
        return false;
    }

    @Override
    public boolean isExistComment(Connection cn, Integer id) {
        String isContainSql = "select count(0) from comment WHERE commentId='" + id + "'";
        try {
            if(isExistRecord(isContainSql)){
                return true;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    @Override
    public boolean insertUrl(Connection cn, String md5Url) {
        String isContainSql = "select count(*) from url WHERE md5_url ='" + md5Url + "'";
        try {
            if(isExistRecord(cn, isContainSql)){
                logger.debug("数据库已经存在该url---" + md5Url);
                return false;
            }
            String sql = "insert into url (md5_url) values( ?)";
            PreparedStatement pstmt;
            pstmt = cn.prepareStatement(sql);
            pstmt.setString(1,md5Url);
            pstmt.executeUpdate();
            pstmt.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        logger.debug("url插入成功---");
        return true;
    }
}
