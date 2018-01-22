package com.crawl.core.parser;

import com.crawl.music.entity.Page;
import com.crawl.music.entity.Music;

public interface DetailPageParser extends Parser {
    Music parseDetailPage(Page page);
}
