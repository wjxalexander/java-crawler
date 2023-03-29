package com.github.hcsp;

import java.sql.SQLException;

public interface CrawlerDao extends Cloneable {
    String getNextLinkThenDelete() throws SQLException;
    void updateDatabase(String link, String sql) throws SQLException;
    void insertNewsIntoDatabase(String link, String title, String content) throws SQLException;
    boolean isLinkProcessed(String link) throws SQLException;
    Object clone() throws CloneNotSupportedException;
    void insertLinkIntoProcessed(String link);
    void insertLinkIntoToBeProcessed(String link);

}
