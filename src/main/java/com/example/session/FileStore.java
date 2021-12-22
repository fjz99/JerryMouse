package com.example.session;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletContext;
import java.io.*;
import java.util.Collection;
import java.util.Objects;

/**
 * 线程不安全，因为不会被并发调用
 *
 * @date 2021/12/22 11:52
 */
@Slf4j
public final class FileStore extends AbstractStore {
    /**
     * 文件扩展名
     */
    private static final String FILE_EXT = ".session";
    private static final String FILE_EXT_WITHOUT_POINT;

    static {
        FILE_EXT_WITHOUT_POINT = FILE_EXT.substring (1);
    }

    /**
     * 文件夹位置
     */
    private String dir = ".";

    /**
     * file缓存
     */
    private File dirFile;

    public String getDir() {
        return dir;
    }

    public void setDir(String dir) {
        this.dir = dir;
    }

    @Override
    public int getSize() throws IOException {
        return files ().size ();
    }

    private Collection<File> files() throws IOException {
        return FileUtils.listFiles (directory (), new String[]{FILE_EXT_WITHOUT_POINT}, false);
    }

    private File directory() throws IOException {
        if (dir == null) {
            throw new IllegalStateException ();
        }

        if (dirFile == null) {
            File file;
            file = new File (dir);
            if (!file.isAbsolute ()) {
                Object attribute = getManager ().getContext ().getServletContext ().getAttribute (ServletContext.TEMPDIR);
                File work = (File) attribute;
                file = new File (work, dir);
            }

            if (!file.exists () || !file.isDirectory ()) {
                if (!file.delete () && file.exists ()) {
                    throw new IOException ();
                }

                if (!file.mkdirs ()) {
                    throw new IOException ();
                }
            }

            dirFile = file;
        }

        return dirFile;
    }

    @Override
    public String[] keys() throws IOException {
        return files ().stream ()
                .map (File::getName)
                .map (x -> x.substring (0, x.lastIndexOf (FILE_EXT)))
                .distinct ()
                .toArray (String[]::new);
    }

    @Override
    public Session load(String id) throws ClassNotFoundException, IOException {
        File file = new File (directory (), id + FILE_EXT);
        if (!file.exists () || !file.isFile ()) {
            return null;
        }

        //注意资源关闭和buffer
        FileInputStream is = new FileInputStream (file);
        BufferedInputStream stream = new BufferedInputStream (is);
        try (ObjectInputStream inputStream = getObjectInputStream (stream)) {
            Object o = inputStream.readObject ();
            if (o instanceof Session) {
                return (Session) o;
            } else {
                log.error ("{} 反序列化失败，类型错误", file.getPath ());
                return null;
            }
        }
    }

    @Override
    public void remove(String id) throws IOException {
        File file = file (id);
        if (file != null) {
            if (file.delete ()) {
                log.trace ("{} 删除成功", file.getPath ());
            } else {
                log.trace ("{} 删除失败", file.getPath ());
            }
        }
    }

    private File file(String id) throws IOException {
        String name = id + FILE_EXT;
        File file = new File (directory (), name);
        if (file.exists () && file.isFile ()) {
            return file;
        } else {
            log.trace ("{} session 不存在", file.getPath ());
            return null;
        }
    }

    @Override
    public void clear() throws IOException {
        for (String key : keys ()) {
            remove (key);
        }
    }

    @Override
    public void save(Session session) throws IOException {
        Objects.requireNonNull (session.getId ());

        File file = new File (directory (), session.getId () + FILE_EXT);
        FileOutputStream out = new FileOutputStream (file);
        BufferedOutputStream stream = new BufferedOutputStream (out);
        try (ObjectOutputStream objectOutputStream = new ObjectOutputStream (stream)) {
            objectOutputStream.writeObject (session);
        }
    }
}
