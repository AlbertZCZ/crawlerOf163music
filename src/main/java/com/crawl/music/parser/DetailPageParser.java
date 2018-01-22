package com.crawl.music.parser;

import com.crawl.core.parser.ListPageParser;
import com.crawl.music.entity.Music;
import com.crawl.music.entity.Page;
import com.jayway.jsonpath.JsonPath;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by zhang on 2018/1/12
 * 歌曲详情解析页
 */
public class DetailPageParser implements ListPageParser {
    private static DetailPageParser detailPageParser;
    public static DetailPageParser getInstance() {
        if (detailPageParser == null) {
            synchronized (DetailPageParser.class) {
                detailPageParser = new DetailPageParser();
            }
        }
        return detailPageParser;
    }

    @Override
    public List<Music> parseListPage(Page page) {
        List<Music> musicList = new ArrayList<>();
        Document doc = Jsoup.parse(page.getHtml());
        Elements commentNums = doc.select("script[type=application/ld+json] ");
        String description = JsonPath.read(commentNums.get(0).html(),"$.description");
        String id = JsonPath.read(commentNums.get(0).html(),"$.@id");
        Music music = new Music();
        music.setId(id.substring(id.indexOf("=") + 1));
        music.setSinger(description.substring(description.indexOf("歌手：") + 3,description.indexOf("。")));
        music.setAlbum(description.substring(description.indexOf("所属专辑：") + 5,description.length() - 1));
        musicList.add(music);
        return musicList;
    }
}
