package com.github.hcsp;

import java.sql.*;

public class DataObjectAccess implements CrawlerDao{
    private static final String USER_NAME = "root";
    private static final String USER_PASSWORD = "root";
    private static final String jdbcUrl = "jdbc:h2:file:~/IdeaProjects/java-crawler/target/news";
    private static Connection connection;

    public DataObjectAccess() {
        try {
            this.connection = DriverManager.getConnection(jdbcUrl, USER_NAME, USER_PASSWORD);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

    }

    public String getNextLink(String sql) {
        ResultSet resultSet;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            resultSet = statement.executeQuery();
            while (resultSet.next()) {
                return resultSet.getString(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    public String getNextLinkThenDelete() {
        String link = getNextLink("select link from LINKS_TO_BE_PROCESSED");
        if (link != null) {
            updateDatabase(link, "DELETE FROM LINKS_TO_BE_PROCESSED where LINK = ?");
        }
        return link;
    }

    public void updateDatabase(String link, String sql) {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, link);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void insertNewsIntoDatabase(String link, String title, String content) {
        try (PreparedStatement statement = connection.prepareStatement("INSERT INTO NEWS (TITLE, CONTENT, URL, CREATED_AT, MODIFIED_AT) VALUES ( ?,?,?,NOW(),NOW() )")) {
            statement.setString(1, title);
            statement.setString(2, content);
            statement.setString(3, link);
            statement.executeUpdate();
        } catch (
                SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        return false;
    }

    public boolean ç(String link) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT LINK FROM LINKS_ALREADY_PROCESSED where LINK=?")) {
            statement.setString(1, link);
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return true;
            }
        }
        return false;
    }

}
