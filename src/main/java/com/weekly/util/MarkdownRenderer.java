package com.weekly.util;

import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MarkdownRenderer {

    private final Parser parser = Parser.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();
    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .extensions(List.of(TablesExtension.create()))
            .build();

    public String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "<p>(응답 없음)</p>";
        }
        return renderer.render(parser.parse(markdown));
    }

    public String toPlainText(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        String html = renderer.render(parser.parse(markdown));
        return html.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }
}
