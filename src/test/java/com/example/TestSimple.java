package com.example;

import com.google.common.net.InetAddresses;
import com.sun.javaws.IconUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.util.InetAddressUtils;
import org.junit.jupiter.api.Test;
import sun.net.util.IPAddressUtil;

import java.net.*;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @date 2021/12/23 19:15
 */
@Slf4j
public class TestSimple {
    private final Object lock = new Object ();

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

    @Test
    void testSync() throws InterruptedException {
//        lock.notifyAll ();

    }

    @Test
    void testIp() throws UnknownHostException {
        String c = "0:0:0:0:0:0:0:1";
        InetAddress byName = Inet6Address.getByName (c);
        System.out.println (byName.getHostName ());

        InetAddress address = Inet4Address.getByName ("127.0.0.1");
        System.out.println (address.getHostName ());
    }

    interface L {
        void f(List<L> list);
    }
}
