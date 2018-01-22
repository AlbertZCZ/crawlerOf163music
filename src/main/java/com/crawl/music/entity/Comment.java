package com.crawl.music.entity;

/**
 * Create by zhang on 2018/1/12
 * 评论bean
 */
public class Comment {
    private Integer commentId;
    private String songId;
    private Integer userId;
    private String nickname;
    private String content;
    private Long time;
    private Integer beReplied;
    private int likedCount;



    public String getSongId() {
        return songId;
    }

    public void setSongId(String songId) {
        this.songId = songId;
    }



    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Integer getCommentId() {
        return commentId;
    }

    public void setCommentId(Integer commentId) {
        this.commentId = commentId;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }

    public Integer getBeReplied() {
        return beReplied;
    }

    public void setBeReplied(Integer beReplied) {
        this.beReplied = beReplied;
    }

    public int getLikedCount() {
        return likedCount;
    }

    public void setLikedCount(int likedCount) {
        this.likedCount = likedCount;
    }

    @Override
    public String toString() {
        return "Comment{" +
                "commentId='" + commentId + '\'' +
                ", songId='" + songId + '\'' +
                ", userId='" + userId + '\'' +
                ", nickname='" + nickname + '\'' +
                ", content='" + content + '\'' +
                ", time='" + time + '\'' +
                ", beReplied='" + beReplied + '\'' +
                ", likedCount=" + likedCount +
                '}';
    }
}
