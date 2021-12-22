package com.example.session;

import com.example.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

class AbstractManagerTest {
    Context context;

    @BeforeEach
    void b() {
        context = mock (Context.class);
    }

    @Test
    void test() throws IOException {
        AbstractManager manager = new AbstractManager () {
            @Override
            public void load() throws ClassNotFoundException, IOException {

            }

            @Override
            public void unload() throws IOException {

            }
        };
        manager.setContext (context);

        Session session = manager.createSession (null);
        System.out.println (session.getId ());
        assertEquals (manager.getSessionCount (), 1);
        manager.changeSessionId (session, "1");
        assertEquals (manager.findSession ("1").getId (), "1");
        assertEquals (manager.findSessions ().length, 1);
        assertEquals (manager.findSessions ()[0].getId (), "1");
    }

}
