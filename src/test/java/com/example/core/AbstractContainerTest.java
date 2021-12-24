package com.example.core;

import com.example.life.LifecycleException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbstractContainerTest {
    C a = new C (), b = new C (), c = new C ();

    @BeforeEach
    void b() {
        a = new C ();
        b = new C ();
        c = new C ();
        a.setName ("a");
        b.setName ("b");
        c.setName ("c");
        a.setStartChildren (false);
        b.setStartChildren (false);
        c.setStartChildren (false);
    }

    @Test
    void test() throws LifecycleException {
        a.addChild (b);
        a.addChild (c);
        a.start ();
        assertEquals (a.findChild ("b"), b);
        assertArrayEquals (a.findChildren (), new C[]{b, c});
        a.stop ();

        a.removeChild (c);
        assertNull (a.findChild ("c"));

        b.addChild (c);
        a.start ();
        assertArrayEquals (a.findChildren (), new C[]{b});
        assertArrayEquals (b.findChildren (), new C[]{c});
        a.stop ();
    }

    @Test
    void background() throws LifecycleException, InterruptedException {
        a.addChild (b);
        b.addChild (c);
        a.setBackgroundProcessorDelay (1);
        a.start ();
        Thread.sleep (1500);
        a.stop ();
        Thread.sleep (100);
    }

    @Test
    void asyncStart() throws LifecycleException, InterruptedException {
        a.addChild (b);
        a.addChild (c);
        a.setStartStopThreads (4);
        a.start ();
        Thread.sleep (500);
        a.stop ();
    }

    private static class C extends AbstractContainer {
        @Override
        public void backgroundProcess() {
            super.backgroundProcess ();
            System.out.println (getName ()+" fuck!");
        }
    }
}
