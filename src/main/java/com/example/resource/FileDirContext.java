package com.example.resource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 简化版本的jndi功能
 * 因为只能用到很少的jndi功能
 *
 * @date 2021/12/18 11:00
 */
public final class FileDirContext extends AbstractContext {

    /**
     * The document base directory.
     */
    private File base = null;

    private String absolutePath;

    private boolean caseSensitive = true;


    /**
     * 会检验合法性
     * 相对路径绝对路径都行，只要能定位到就行
     */
    public void setDocBase(String docBase) {
        super.setDocBase (docBase);

        if (docBase == null) {
            throw new IllegalArgumentException ("docBase null");
        }

        base = new File (docBase);
        if (base.isDirectory () && base.canRead ()) {
            this.absolutePath = base.getAbsolutePath ();
        } else {
            throw new IllegalArgumentException ("can not find path " + docBase);
        }
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    /**
     * Retrieves the named object.
     * 注意，返回的文件夹的doc base会变为对应文件夹的地址
     *
     * @param name the name of the object to look up
     * @return the object bound to name
     * @throws IllegalArgumentException 如果找不到
     */
    public Object lookup(String name) {
        Object result;
        File file = file (name);

//        if (file == null)
//            throw new IllegalArgumentException ("name " + name + " 不存在");
        if (file == null) {
            return null;
        }

        //如果查找的资源是文件夹的话，就还返回context，否则返回 FileResource
        if (file.isDirectory ()) {
            FileDirContext tempContext = new FileDirContext ();
            tempContext.setDocBase (file.getPath ());
            result = tempContext;
        } else {
            result = new FileResource (file);
        }

        return result;
    }

    public ResourceAttributes getAttributes(String name) {
        File file = file (name);

//        if (file == null)
//            throw new IllegalArgumentException (name);

        if (file == null) {
            return null;
        }
        return new FileResourceAttributes (file);
    }

    private String normalize(String path) {
        while (path.contains ("\\")) {
            path = path.replace ('\\', '/');
        }
        return path;
    }

    @Override
    public Collection<Object> list(String path) {
        path = normalize (path);
        File file = new File (base, path);
        String[] strings = file.list ();
        if (strings == null) {
            return new ArrayList<> ();
        }

        List<File> files = Arrays.stream (strings).map (x -> new File (file, x)).collect (Collectors.toList ());
        List<Object> list = new ArrayList<> ();
        for (File aFile : files) {
            if (aFile.canRead () && aFile.exists ()) {
                if (aFile.isDirectory ()) {
                    FileDirContext fileDirContext = new FileDirContext ();
                    fileDirContext.setDocBase (aFile.getAbsolutePath ());
                    list.add (fileDirContext);
                } else {
                    list.add (new FileResource (aFile));
                }
            }
        }
        return list;
    }

    public File getAbsoluteFile() {
        return new File (base.getAbsolutePath ());
    }

    /**
     * 删除了源码中的很多内容
     * Return a File object representing the specified normalized
     * context-relative path if it exists and is readable.  Otherwise,
     * return <code>null</code>.
     *
     * @param name Normalized context-relative path (with leading '/')
     */
    private File file(String name) {
        name = normalize (name);
        File file = new File (name);
        if (file.isAbsolute ()) {
            return file;
        }

        file = new File (base, name);
        if (file.exists () && file.canRead ()) {
            return file;
        } else {
            return null;
        }

    }

    @Override
    public String toString() {
        return "FileDirContext{" +
                "base=" + base.getPath () +
                '}';
    }

    public static class FileResource extends Resource {

        /**
         * 绝对路径的文件
         */
        protected File file;
        /**
         * File length.
         */
        protected long length = -1L;

        public FileResource(File file) {
            this.file = file;
        }

        public File getFile() {
            return file;
        }

        @Override
        public String toString() {
            return "FileResource{" +
                    "file=" + file +
                    '}';
        }

        /**
         * 创建一个新的stream
         * 如果有缓存的文件内容，那就直接返回
         *
         * @return InputStream
         */
        public InputStream streamContent()
                throws IOException {
            if (binaryContent == null) {
                inputStream = new FileInputStream (file);
            }
            return super.streamContent ();
        }

        public ResourceAttributes getAttribute() {
            return new FileResourceAttributes (file);
        }
    }

    public static class FileResourceAttributes extends ResourceAttributes {

        protected File file;

        protected boolean accessed = false;

        public FileResourceAttributes(File file) {
            this.file = file;
        }


        /**
         * Is collection.
         */
        public boolean isCollection() {
            if (!accessed) {
                collection = file.isDirectory ();
                accessed = true;
            }
            return super.isCollection ();
        }


        /**
         * Get content length.
         *
         * @return content length value
         */
        public long getContentLength() {
            if (contentLength != -1L)
                return contentLength;
            contentLength = file.length ();
            return contentLength;
        }


        /**
         * Get creation time.
         *
         * @return creation time value
         */
        public long getCreation() {
            if (creation != -1L)
                return creation;
            creation = file.lastModified ();
            return creation;
        }

        @Override
        public String toString() {
            return "FileResourceAttributes{" +
                    "file=" + file +
                    ", accessed=" + accessed +
                    ", collection=" + collection +
                    ", contentLength=" + contentLength +
                    ", creation=" + creation +
                    ", creationDate=" + creationDate +
                    ", lastModified=" + lastModified +
                    ", lastModifiedDate=" + lastModifiedDate +
                    ", name='" + name + '\'' +
                    '}';
        }

        /**
         * Get creation date.
         *
         * @return Creation date value
         */
        public Date getCreationDate() {
            if (creation == -1L) {
                creation = file.lastModified ();
            }
            return super.getCreationDate ();
        }


        /**
         * Get last modified time.
         *
         * @return lastModified time value
         */
        public long getLastModified() {
            if (lastModified != -1L)
                return lastModified;
            lastModified = file.lastModified ();
            return lastModified;
        }


        /**
         * Get lastModified date.
         *
         * @return LastModified date value
         */
        public Date getLastModifiedDate() {
            if (lastModified == -1L) {
                lastModified = file.lastModified ();
            }
            return super.getLastModifiedDate ();
        }


        /**
         * Get name.
         *
         * @return Name value
         */
        public String getName() {
            if (name == null)
                name = file.getName ();
            return name;
        }


        /**
         * Get resource type.
         *
         * @return String resource type
         */
        public String getResourceType() {
            if (!accessed) {
                collection = file.isDirectory ();
                accessed = true;
            }
            return super.getResourceType ();
        }

    }
}
