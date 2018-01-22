package com.crawl.core.parser;

import com.crawl.music.entity.Page;

import java.util.List;

/**
 * 列表解析器
 */
public interface ListPageParser extends Parser {
    /**
     * 解析页面歌曲
     * @param page
     * @return
     */
    List parseListPage(Page page);
}
