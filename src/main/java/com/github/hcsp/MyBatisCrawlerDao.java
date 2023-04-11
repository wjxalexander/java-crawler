package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

public class MyBatisCrawlerDao implements CrawlerDao {
    private final SqlSessionFactory sqlSessionFactory;

    public MyBatisCrawlerDao() throws IOException {
        String resource = "db/mybatis/mybatis-config.xml";
        InputStream inputStream = Resources.getResourceAsStream(resource);
        sqlSessionFactory =
                new SqlSessionFactoryBuilder().build(inputStream);
    }

    @Override
    public synchronized String getNextLinkThenDelete() throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            String url = session.selectOne("com.github.hcsp.selectNextAvailableLink");
            if (url != null) {
                session.delete("com.github.hcsp.deleteLink", url);
            }
            return url;
        }
    }

    @Override
    public void updateDatabase(String link, String sql) throws SQLException {

    }

    @Override
    public void insertNewsIntoDatabase(String link, String title, String content) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.insertNews", new News(link, content, title));
        }
    }

    @Override
    public boolean isLinkProcessed(String link) throws SQLException {
        try (SqlSession session = sqlSessionFactory.openSession()) {
            int count = session.selectOne("com.github.hcsp.countLink", link);
            return count > 0;
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    private void insertLinkIntoCertainTable(String tableName, String link) {
        Map<String, Object> param = new HashMap<>();
        System.out.println(tableName);
        param.put("_tableName", tableName);
        param.put("link", link);
        try (SqlSession session = sqlSessionFactory.openSession(true)) {
            session.insert("com.github.hcsp.insertLink", param);
        }
    }

    @Override
    public void insertLinkIntoProcessed(String link) {
        // INSERT into LINKS_ALREADY_PROCESSED (LINK) values (?) LINKS_TO_BE_PROCESSED
        insertLinkIntoCertainTable("LINKS_ALREADY_PROCESSED", link);
    }

    @Override
    public void insertLinkIntoToBeProcessed(String link) {
        insertLinkIntoCertainTable("LINKS_TO_BE_PROCESSED", link);
    }
}
