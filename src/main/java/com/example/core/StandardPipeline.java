package com.example.core;

import com.example.Contained;
import com.example.Container;
import com.example.Pipeline;
import com.example.Valve;
import com.example.connector.Request;
import com.example.connector.Response;
import com.example.life.Lifecycle;
import com.example.life.LifecycleBase;
import com.example.life.LifecycleException;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 就是一个链表.<p>
 * 调用方法，getFirst.invoke,然后后面就会invoke了.<p>
 * 职责链模式.<p>
 * basic必须有一个，否则报错.<p>
 * 只能在stop的时候改动valve，其他情况会报错{@link LifecycleException}!
 *
 * @date 2021/12/23 13:51
 */
@Slf4j
public class StandardPipeline extends LifecycleBase implements Pipeline {

    private static final Valve[] EMPTY_VALVE_ARRAY = new Valve[0];

    /**
     * 尾部节点
     */
    protected Valve basic;
    /**
     * 第一个节点，没有头结点
     */
    protected Valve first;

    protected Container container;

    /**
     * 决定抛出异常还是返回null
     */
    protected boolean strict = false;

    public StandardPipeline() {
        this (null);
    }

    public StandardPipeline(Container container) {
        super ();
        setContainer (container);
    }

    public void setStrict(boolean strict) {
        this.strict = strict;
    }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start ();

        Valve n = first;
        while (n != null) {
            startValve (n);
            n = n.getNext ();
        }
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        super.stop ();

        Valve n = first;
        while (n != null) {
            stopValve (n);
            n = n.getNext ();
        }
    }

    @Override
    public Container getContainer() {
        return container;
    }

    @Override
    public void setContainer(Container container) {
        this.container = container;
    }

    @Override
    public Valve getBasic() {
        return basic;
    }


    /**
     * 因为basic是链表末尾，所以必须先设置basic
     */
    @Override
    public void setBasic(Valve valve) {
        if (basic == valve) {
            return;
        }

        valve.setNext (null);//保证安全
        this.basic = valve;
        if (first == null || first.getNext () == null) {
            first = basic;
        } else {
            //basic在末尾
            if (strict)
                throw new IllegalStateException ();
        }
//        startValve (valve);
//        stopValve (valve);
    }

    /**
     * add 不会start valve，因为
     */
    @Override
    public void addValve(Valve valve) {
        if (basic == null || first == null) {
            throw new IllegalStateException ("应该先添加basic valve");
        }

        //找倒数
        if (first == basic) {
            first = valve;
            valve.setNext (basic);
        } else {
            findBehindOf (basic).setNext (valve);
            valve.setNext (basic);
        }
//        startValve (valve);
    }

    @Override
    public Valve[] getValves() {
        Valve n = first;
        List<Valve> list = new ArrayList<> ();
        while (n != null) {
            list.add (n);
            n = n.getNext ();
        }
        return list.toArray (EMPTY_VALVE_ARRAY);
    }

    @Override
    public void removeValve(Valve valve) {
        if (basic == null || first == null) {
            throw new IllegalStateException ("应该先添加basic valve");
        }
        if (valve == basic) {
            throw new IllegalStateException ("basic valve 无法移除");
        }

        if (valve == first) {
            first = first.getNext ();
        } else {
            Valve prev = findBehindOf (valve);
            if (prev != null) {
                prev.setNext (prev.getNext ().getNext ());
            } else {
                log.warn ("remove一个不存在的valve");
                return;
            }
        }
//        stopValve (valve);
    }

    protected void stopValve(Valve valve) {
        if (valve instanceof Lifecycle) {
            try {
                ((Lifecycle) valve).stop ();
            } catch (LifecycleException e) {
                e.printStackTrace ();
            }
        }

        if (valve instanceof Contained) {
            ((Contained) valve).setContainer (null);
        }
    }

    protected void startValve(Valve valve) {
        if (valve instanceof Contained) {
            ((Contained) valve).setContainer (container);
        }

        if (valve instanceof Lifecycle) {
            try {
                ((Lifecycle) valve).start ();
            } catch (LifecycleException e) {
                e.printStackTrace ();
            }
        }
    }

    /**
     * 查找链表前驱节点,如果没有节点那就返回null
     * 如果为第一个节点，那需要自行判断
     */
    private Valve findBehindOf(Valve valve) {
        if (valve == first) {
            throw new IllegalArgumentException ();
        }

        Valve n = first;
        while (n.getNext () != null && n.getNext () != valve) {
            n = n.getNext ();
        }

        if (n.getNext () == null) {
            return null;
        } else {
            return n;
        }
    }


    public Valve getFirst() {
        if (basic == null && strict) {
            throw new IllegalStateException ("未设置basic valve");
        }

        return first;
    }

    @Override
    public String toString() {
        return "StandardPipeline{" +
                "basic=" + basic +
                ", first=" + first +
                ", container=" + container +
                '}';
    }

    @Override
    public void invoke(Request request, Response response) throws IOException, ServletException {
        getFirst ().invoke (request, response);
    }
}
