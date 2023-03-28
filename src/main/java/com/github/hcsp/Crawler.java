package com.github.hcsp;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class Crawler {

    private static final String USER_NAME = "root";
    private static final String USER_PASSWORD = "root";
    private final CrawlerDao dao;
    public Crawler(CrawlerDao dao) {
        this.dao = dao;
    }

    private static boolean isTargetLink(String link) {
        return link.startsWith("/news/article") || link.equals("https://m.163.com");
    }


    private static Document getAndParseHtml(String link) {
        if (link.startsWith("/")) {
            link = "https://m.163.com" + link;
        }
        try (CloseableHttpClient httpclient = HttpClients.createDefault()) {
            ClassicHttpRequest httpGet = ClassicRequestBuilder.get(link).build();
            httpGet.addHeader("user-agent:", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (HTML, like Gecko) Chrome/110.0.0.0 Safari/537.36");
            return httpclient.execute(httpGet, Crawler::handleResponse);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressFBWarnings(value = "DMI_CONSTANT_DB_PASSWORD", justification = "Document why this should be ignored here")
    public void run() throws SQLException {
        String jdbcUrl = "jdbc:h2:file:~/IdeaProjects/java-crawler/target/news";
        Connection connection = DriverManager.getConnection(jdbcUrl, USER_NAME, USER_PASSWORD);
        String link;
        while ((link = dao.getNextLinkThenDelete()) != null) {
            dao.updateDatabase(link, "DELETE FROM LINKS_TO_BE_PROCESSED where LINK = ?");
            if (!dao.isLinkProcessed(link)) {
                if (isTargetLink(link)) {
                    System.out.println(link);
                    Document doc = getAndParseHtml(link);
                    parseURLFromPageAndStoreIntoDB(doc);
                    storeNewsIntoDb(doc, link);
                    dao.updateDatabase(link, "INSERT into LINKS_ALREADY_PROCESSED (LINK) values (?)");
                }
            }

        }
    }

    public static void main(String[] args) throws SQLException {
        new Crawler(new DataObjectAccess()).run();
    }

    private void parseURLFromPageAndStoreIntoDB(Document doc) throws SQLException {
        for (Element linkTag : doc.select("a")) {
            String aTag = linkTag.attr("href");
            if (isTargetLink(aTag)) {
                dao.updateDatabase(aTag, "INSERT into LINKS_TO_BE_PROCESSED (LINK) values (?)");
            }
        }
    }


    private void storeNewsIntoDb(Document doc,String link) throws SQLException {
        Elements articleTags = doc.select("article");
        if (!articleTags.isEmpty()) {
            for (Element articleTag : articleTags) {
                Elements headers = articleTag.select("h1");
                if (!headers.isEmpty()) {
                    String title = headers.get(0).text();
                    String content = articleTag.select("p").stream().map(Element::text).collect(Collectors.joining("\n"));
                    dao.insertNewsIntoDatabase(link, title, content);
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
