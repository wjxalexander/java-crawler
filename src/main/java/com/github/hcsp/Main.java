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

public class Main {
    private static final String USER_NAME = "root";
    private static final String USER_PASSWORD = "root";

    private static boolean isTargetLink(String link) {
        return link.startsWith("/news/article") || link.equals("https://m.163.com");
    }

    private static Document getAndParseHtml(String link) {
        if (link.startsWith("/")) {
            link = "https://m.163.com" + link;
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

    @SuppressFBWarnings(value = "DMI_CONSTANT_DB_PASSWORD", justification = "Document why this should be ignored here")
    public static void main(String[] args) throws SQLException {
        String jdbcUrl = "jdbc:h2:file:~/IdeaProjects/java-crawler/target/news";
        Connection connection = DriverManager.getConnection(jdbcUrl, USER_NAME, USER_PASSWORD);
        while (true) {
            List<String> linkPool = loadURLFromDatabase(connection, "select link from LINKS_TO_BE_PROCESSED");
            if (linkPool.isEmpty()) {
                break;
            }

            String link = linkPool.remove(linkPool.size() - 1);
            insertLinkToDB(connection, link, "DELETE FROM LINKS_TO_BE_PROCESSED where LINK = ?");
            if (!isProcessed(connection, link)) {
                if (isTargetLink(link)) {
                    System.out.println(link);
                    Document doc = getAndParseHtml(link);
                    parseURLFromPageAndStoreIntoDB(connection, doc);
                    storeNewsIntoDb(doc);
                    insertLinkToDB(connection, link, "INSERT into LINKS_ALREADY_PROCESSED (LINK) values (?)");
                }
            }

        }
    }

    private static void parseURLFromPageAndStoreIntoDB(Connection connection, Document doc) throws SQLException {
        for (Element linkTag : doc.select("a")) {
            String aTag = linkTag.attr("href");
            if (!isTargetLink(aTag)) {
                continue;
            }
            insertLinkToDB(connection, aTag, "INSERT into LINKS_TO_BE_PROCESSED (LINK) values (?)");
        }
    }

    private static boolean isProcessed(Connection connection, String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS_ALREADY_PROCESSED where LINK=?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }

        }
        return false;
    }

    private static void insertLinkToDB(Connection connection, String link, String sql) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        }
    }

    private static List<String> loadURLFromDatabase(Connection connection, String sql) {
        List<String> result = new ArrayList<>();
        try (PreparedStatement linksToBeProcessed = connection.prepareStatement(sql)) {
            ResultSet resultSet = linksToBeProcessed.executeQuery();
            while (resultSet.next()) {
                result.add(resultSet.getString(1));
            }
            return result;
        } catch (SQLException e) {
            throw new RuntimeException(e);
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
