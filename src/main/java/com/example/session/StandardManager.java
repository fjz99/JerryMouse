package com.example.session;

import com.example.Container;
import com.example.life.LifecycleException;
import com.example.loader.Loader;
import com.example.util.EnhancedObjectInputStream;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 增加了在start stop时的持久化功能
 * 注意持久化的时候不会存储map，而是使用中间逻辑表示
 * 没有任何同步措施
 *
 * @date 2021/12/22 15:01
 */
@Slf4j
public class StandardManager extends AbstractManager {
    protected String fileName = "cache.session";

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        String oldPathname = this.fileName;
        this.fileName = fileName;
        support.firePropertyChange ("fileName", oldPathname, this.fileName);
    }

    @Override
    public void load() throws ClassNotFoundException, IOException {
        sessions.clear ();

        File file = file ();
        FileInputStream fileInputStream;
        try {
            fileInputStream = new FileInputStream (file);
        } catch (FileNotFoundException e) {
            e.printStackTrace ();
            log.debug ("load时文件{}不存在,放弃load", file.getPath ());
            return;
        }

        BufferedInputStream bufferedInputStream = new BufferedInputStream (fileInputStream);
        ClassLoader classLoader = Optional
                .ofNullable (getContext ())
                .map (Container::getLoader)
                .map (Loader::getClassLoader)
                .orElse (null);
        if (classLoader == null) {
            classLoader = getClass ().getClassLoader ();
        }

        try (ObjectInputStream stream = new EnhancedObjectInputStream (classLoader, bufferedInputStream)) {
            int n = stream.readInt ();
            for (int i = 0; i < n; i++) {
                StandardSession session = (StandardSession) stream.readObject ();
                sessions.put (session.getIdInternal (), session);

                session.setManager (this);
                session.activate ();
            }
        } finally {
            if (file.exists () && !file.delete ()) {
                log.warn ("load之后无法删除文件 {}", file.getPath ());
            }
        }

        log.debug ("load 完成");
    }

    /**
     * unload是stop的时候才会调用的，所以在unload的时候会expire所有的session
     * 但是不会触发destroy监听器。。会触发attr unbound监听器。。
     */
    @Override
    public void unload() throws IOException {
        File file = file ();
        FileOutputStream fileOutputStream = new FileOutputStream (file);
        BufferedOutputStream bufferedOutputStream = new BufferedOutputStream (fileOutputStream);

        try (ObjectOutputStream stream = new ObjectOutputStream (bufferedOutputStream)) {
            stream.writeInt (sessions.size ());

            for (Map.Entry<String, Session> e : sessions.entrySet ()) {
                Session session = e.getValue ();
                ((StandardSession) session).passivate ();
                stream.writeObject (session);
            }
        }

        super.unload ();
        log.debug ("unload 完成");
    }

    protected File file() {
        Objects.requireNonNull (fileName);

        Object attribute = getContext ().getServletContext ().getAttribute (ServletContext.TEMPDIR);
        if (attribute == null) {
            return new File (fileName);
        }

        File work = (File) attribute;
        return new File (work, fileName);
    }

    @Override
    public synchronized void start() throws LifecycleException {
        super.start ();

        try {
            load ();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace ();
            log.error ("start时load 失败");
        }
    }

    @Override
    public synchronized void stop() throws LifecycleException {
        super.stop ();

        try {
            unload ();
        } catch (IOException e) {
            e.printStackTrace ();
            log.error ("stop时unload 失败");
        }
    }
}
