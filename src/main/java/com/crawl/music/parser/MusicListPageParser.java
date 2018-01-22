package com.crawl.music.parser;


import com.crawl.core.parser.ListPageParser;
import com.crawl.core.util.Constants;
import com.crawl.music.MusicHttpClient;
import com.crawl.music.entity.Page;
import com.crawl.music.entity.Music;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * 歌曲列表页
 */
public class MusicListPageParser implements ListPageParser {
    private static MusicListPageParser instance;
    public static MusicListPageParser getInstance() {
        if (instance == null) {
            synchronized (MusicHttpClient.class) {
                if (instance == null){
                    instance = new MusicListPageParser();
                }
            }
        }
        return instance;
    }
    @Override
    public List<Music> parseListPage(Page page) {
        List<Music> musicList = new ArrayList<>();
        Document doc = Jsoup.parse(page.getHtml());
        Elements commentNums = doc.select("ul.f-hide > li ");
        for (int i = 0; i < commentNums.size(); i++) {
            Music music = new Music();
            Element element = commentNums.get(i);
            music.setName(element.text());

            String html = element.html();
            String path = html.substring(html.indexOf("/"),html.indexOf(">") - 1);
            music.setUrl(Constants.INDEX_URL + path);
            music.setId(path.substring(path.indexOf("=") + 1));
            musicList.add(music);
        }
        return musicList;
    }


}
