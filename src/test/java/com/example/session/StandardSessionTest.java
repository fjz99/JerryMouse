package com.example.session;

import com.example.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.servlet.http.*;
import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StandardSessionTest {
    Manager manager;
    Context context;

    @BeforeEach
    void b() {
        manager = mock (Manager.class);
        context = mock (Context.class);
        when (manager.getContext ()).thenReturn (context);
    }

    @Test
    void testSimple() {
        StandardSession session = new StandardSession (manager);
        session.setValid (true);
        String name = "1";
        Object value = new Object ();
        session.setAttribute (name, value);
        assertEquals (session.getValue (name), value);
        session.removeAttribute (name);
        assertNull (session.getValue (name));
    }

    @Test
    void testCallback1() {
        StandardSession session = new StandardSession (manager);
        session.setValid (true);
        String name = "1";
        Object value = new HttpSessionBindingListener () {
            @Override
            public void valueBound(HttpSessionBindingEvent event) {
                System.out.println ("valueBound");
            }

            @Override
            public void valueUnbound(HttpSessionBindingEvent event) {
                System.out.println ("valueUnbound");
            }
        };
        Object value2 = new HttpSessionBindingListener () {
            @Override
            public void valueBound(HttpSessionBindingEvent event) {
                System.out.println ("valueBound222");
            }
        };
        session.setAttribute (name, value);
        session.removeAttribute (name);
        session.setAttribute (name, value);
        session.setAttribute (name, value2);

        Object o = new HttpSessionActivationListener () {
            @Override
            public void sessionWillPassivate(HttpSessionEvent se) {
                System.out.println ("sessionWillPassivate");
            }

            @Override
            public void sessionDidActivate(HttpSessionEvent se) {
                System.out.println ("sessionDidActivate");
            }
        };
        session.setAttribute (name, o);
        session.passivate ();
        session.activate ();
    }

    @Test
    void testCallback2() {
        StandardSession session = new StandardSession (manager);
        session.setValid (true);
        String name = "1";
        List<Object> list = new ArrayList<> ();
        list.add (new Object ());
        list.add (new HttpSessionAttributeListener () {
            @Override
            public void attributeAdded(HttpSessionBindingEvent event) {
                Container.msg = "1";
            }

            @Override
            public void attributeRemoved(HttpSessionBindingEvent event) {
                Container.msg = "2";
            }

            @Override
            public void attributeReplaced(HttpSessionBindingEvent event) {
                Container.msg = "3";
            }
        });
        when (context.getApplicationEventListeners ()).thenReturn (list.toArray ());

        session.setAttribute (name, new Object ());
        assertEquals (Container.msg, "1");
        session.setAttribute (name, new Object ());
        assertEquals (Container.msg, "3");
        session.removeAttribute (name);
        assertEquals (Container.msg, "2");
    }

    @Test
    void serializeTest() throws IOException, ClassNotFoundException {
        StandardSession session = new StandardSession (manager);
        session.setValid (true);
        int n = 10;
        Random random = new Random ();
        random.setSeed (System.currentTimeMillis ());

        IntStream.range (0, n).forEach (x -> {
            session.setAttribute (String.valueOf (x), x + random.nextInt (1000));
        });
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        ObjectOutputStream objectOutputStream = new ObjectOutputStream (out);
        objectOutputStream.writeObject (session);

        ObjectInputStream objectInputStream = new ObjectInputStream (new ByteArrayInputStream (out.toByteArray ()));
        Object o = objectInputStream.readObject ();
        assertTrue (o instanceof StandardSession);
        System.out.println (o);
        for (String valueName : ((StandardSession) o).getValueNames ()) {
            System.out.println (((StandardSession) o).getAttribute (valueName));
        }
    }

    @Test
    void otherTest() throws InterruptedException {
        StandardSession session = new StandardSession (manager);
        session.setValid (true);

        int n = 10;
        Random random = new Random ();
        random.setSeed (System.currentTimeMillis ());

        IntStream.range (0, n).forEach (x -> {
            session.setAttribute (String.valueOf (x), x + random.nextInt (1000));
        });

        session.invalidate ();
        assertThrows (IllegalStateException.class, () -> session.getValue ("1"));

        session.setValid (true);
        session.setManager (manager);
        session.setMaxInactiveInterval (1);
        session.access ();
        Thread.sleep (1010);
        assertTrue (session.isValidInternal ());
        assertFalse (session.isValid ());
        assertFalse (session.isValidInternal ());
    }

    static class Container {
        static String msg;

        static void clear() {
            msg = null;
        }
    }

}
