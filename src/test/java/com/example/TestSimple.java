package com.example;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @date 2021/12/23 19:15
 */
@Slf4j
public class TestSimple {
    @Test
    void test() {
//        List<L> list = new ArrayList<> ();
        List<L> list = new CopyOnWriteArrayList<> ();
        list.add (new L () {
            @Override
            public void f(List<L> list) {
                System.out.println ("1");
//                list.remove (this);
                list.add (new L () {
                    @Override
                    public void f(List<L> list) {

                    }
                });
            }
        });
        list.add (list1 -> System.out.println ("11: " + list1.size ()));

        for (L l : list) {
            l.f (list);
        }
    }

    @Test
    void testLog() {
        Throwable e = new IllegalStateException ("fffff");
        e.printStackTrace ();
        log.error ("线程异常", e);
    }

    interface L {
        void f(List<L> list);
    }
}
