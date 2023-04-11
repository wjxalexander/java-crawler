package com.github.hcsp;

import org.apache.ibatis.io.Resources;
import org.apache.ibatis.session.ExecutorType;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.List;
import java.util.Random;

public class MockDataGenerator {
    private static final int TARGET_ROW_COUNT = 500;

    public static void main(String[] args) {
        SqlSessionFactory sqlSessionFactory = null;
        InputStream inputStream = null;
        try {
            String resource = "db/mybatis/mybatis-config.xml";
            inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory =
                    new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        try (SqlSession session = sqlSessionFactory.openSession(ExecutorType.BATCH)) {
            List<News> currentNews = session.selectList("com.github.hcsp.MockMapper.selectNews");
            int count = TARGET_ROW_COUNT - currentNews.size();
            Random random = new Random();
            try {

                while (count-- > 0) {
                    News newsToBeInserted = currentNews.get(random.nextInt(currentNews.size() - 1));
                    Instant currentTime = newsToBeInserted.getCreatedAt();
                    currentTime = currentTime.minusSeconds(random.nextInt(3600 * 24 * 365));
                    newsToBeInserted.setModifiedAt(currentTime);
                    newsToBeInserted.setCreatedAt(currentTime);
                    session.insert("com.github.hcsp.MockMapper.insertNews", newsToBeInserted);
                }
                session.commit(); //原子性操作
            } catch (Exception e) {
                e.printStackTrace();
                session.rollback();
            }
        }
    }
}
