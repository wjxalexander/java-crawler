package com.github.hcsp;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.ParseException;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.io.support.ClassicRequestBuilder;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Main {
    private static boolean isTargetLink(String link) {
        return link.startsWith("https://news.sina.cn");
    }

    private static Document getAndParseHtml(String link) {
        if (link.startsWith("//")) {
            link = "https://" + link;
        }
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder.get(link)
                    .build();
            httpGet.addHeader("user-agent:", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (HTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
            return httpclient.execute(httpGet, Main::handleResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void main(String[] args) {
        List<String> linkPool = new ArrayList<>();
        linkPool.add("https://news.sina.cn/");
        Set<String> visitedLinkPool = new HashSet<>();

        while (true) {
            if (linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.remove(linkPool.size() - 1);
            if (visitedLinkPool.contains(link)) {
                continue;
            }

            if (isTargetLink(link)) {
                System.out.println(link);
                Document doc = getAndParseHtml(link);
                doc.select("a").stream().map(linkTag -> linkTag.attr("a")).forEach(linkPool::add);
                storeNewsIntoDb(doc);
                visitedLinkPool.add(link);
            }
        }
    }

    private static void storeNewsIntoDb(Document doc) {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                Elements headers = articleTag.select("h1");
                if (!headers.isEmpty()) {
                    String title = headers.get(0).text();
                    System.out.println(title);
                }
            }
        }
    }

    private static Document handleResponse(ClassicHttpResponse response) {
        final HttpEntity entity1 = response.getEntity();
        String htmlContent;
        try {
            htmlContent = EntityUtils.toString(entity1);
        } catch (IOException | ParseException e) {
            throw new RuntimeException(e);
        }
        return Jsoup.parse(htmlContent);
    }
}
