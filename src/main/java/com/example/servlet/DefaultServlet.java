package com.example.servlet;

import com.example.Globals;
import com.example.resource.FileDirContext;
import com.example.resource.ResourceAttributes;
import com.example.util.MD5Encoder;
import com.example.util.URLEncoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;


import static com.example.util.RequestUtil.normalize;
import static com.google.common.net.HttpHeaders.*;

/**
 * 处理静态资源的默认servlet
 * 包含的功能etag、last-modified、range（包括if-range等）、welcome file
 * 静态资源、文件夹展示渲染（类似于nginx的目录界面）
 * <p>
 * 这个类可以被继承，实现自己的静态资源管理类
 * <p>
 * 时区是gmt时区
 * <p>
 * TODO etag、很多其他的if头、是否支持range的标志位、put请求方法
 *
 * @date 2022/1/4 19:39
 */
@Slf4j
public class DefaultServlet extends HttpServlet {
    protected static final MD5Encoder md5Encoder = new MD5Encoder ();
    /**
     * 可能的日期时间格式
     */
    protected static final SimpleDateFormat[] formats = {
            new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat ("EEEEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),
            new SimpleDateFormat ("EEE MMMM d HH:mm:ss yyyy", Locale.US)
    };
    protected final static TimeZone gmtZone = TimeZone.getTimeZone ("GMT");
    /**
     * MIME multipart separation string，multipart的boundary
     */
    protected static final String mimeSeparation = "JERRY_MOUSE_BOUNDARY";
    protected static final SimpleDateFormat format =
            new SimpleDateFormat ("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
    /**
     * MD5 message digest provider.
     */
    protected static MessageDigest md5Helper;
    /**
     * Array containing the safe characters set.
     */
    protected static URLEncoder urlEncoder;

    static {
        formats[0].setTimeZone (gmtZone);
        formats[1].setTimeZone (gmtZone);
        formats[2].setTimeZone (gmtZone);

        urlEncoder = new URLEncoder ();
        urlEncoder.addSafeCharacter ('-');
        urlEncoder.addSafeCharacter ('_');
        urlEncoder.addSafeCharacter ('.');
        urlEncoder.addSafeCharacter ('*');
        urlEncoder.addSafeCharacter ('/');
    }

    protected boolean acceptRanges = true;
    protected boolean readOnly = true;
    /**
     * The set of welcome files for this web application
     */
    protected List<String> welcomes = new CopyOnWriteArrayList<> ();
    /**
     * 是否允许展示目录
     */
    protected boolean listFiles = true;
    /**
     * 静态资源的读取buffer大小
     */
    protected int input = 2048;
    /**
     * The output buffer size to use when serving resources.
     * ？？？
     */
    protected int output = 2048;
    protected boolean setCacheControl = true;
    protected long maxAge = 60 * 30;//30分钟
    protected String cacheControlString = "max-age=" + maxAge;

    /**
     * 解析web.xml中可以配置的servlet init参数
     */
    @SuppressWarnings("unchecked")
    public void init() throws ServletException {

        String value;
        try {
            value = getServletConfig ().getInitParameter ("input");
            input = Integer.parseInt (value);
        } catch (Throwable ignored) {
        }
        try {
            value = getServletConfig ().getInitParameter ("listings");
            listFiles = Boolean.parseBoolean (value);
        } catch (Throwable ignored) {

        }
        try {
            value = getServletConfig ().getInitParameter ("readonly");
            if (value != null)
                readOnly = Boolean.parseBoolean (value);
        } catch (Throwable ignored) {

        }
        try {
            value = getServletConfig ().getInitParameter ("output");
            output = Integer.parseInt (value);
        } catch (Throwable ignored) {

        }

        if (input < 256)
            input = 256;
        if (output < 256)
            output = 256;

        //因为servlet无法访问context，所以这些都是通过attr获得

        welcomes = (List<String>) getServletContext ().getAttribute (Globals.WELCOME_FILES_ATTR);

        log.debug ("DefaultServlet.init:  input buffer size=" + input +
                ", output buffer size=" + output);
        log.debug ("DefaultServlet.init:  welcome file=" + welcomes);

        try {
            md5Helper = MessageDigest.getInstance ("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace ();
            throw new IllegalStateException ();
        }

    }

    /**
     * 简单实现，resource必须由attribute获得（因为servlet无法访问Context类）
     *
     * @return 获得的是context总体的attr, 而defaultServlet服务的url不一定是/所以要变换
     */
    protected FileDirContext getResources() throws ServletException {
        FileDirContext result;

        try {
            result = (FileDirContext) getServletContext ().getAttribute (Globals.RESOURCES_ATTR);
            return result;
        } catch (ClassCastException e) {
            log.error ("ResourceContext 异常", e);
            throw new ServletException ("ResourceContext 异常", e);
        }

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        serveResource (request, response, true);
    }

    protected void doHead(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {

        //head没有body，不返回响应体
        serveResource (request, response, false);
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        doGet (request, response);
    }

    //TODO
    protected void doPut(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        throw new ServletException ();
    }

    /**
     * 删除资源
     */
    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (readOnly) {
            resp.sendError (HttpServletResponse.SC_METHOD_NOT_ALLOWED, "DELETE not allowed.");
            return;
        }

        //删除
        String relativePath = getRelativePath (req);
        FileDirContext resources = getResources ();
        Object lookup = resources.lookup (relativePath);
        if (lookup == null) {
            resp.sendError (HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        File file;
        if (lookup instanceof FileDirContext.FileResource) {
            file = ((FileDirContext.FileResource) lookup).getFile ();
        } else {
            file = ((FileDirContext) lookup).getAbsoluteFile ();
        }

        if (FileUtils.deleteQuietly (file)) {
            resp.setStatus (HttpServletResponse.SC_NO_CONTENT);
        } else {
            resp.sendError (HttpServletResponse.SC_METHOD_NOT_ALLOWED);
        }
    }

    private Date decodeDate(String s) {
        Date date = null;
        for (SimpleDateFormat dateFormat : formats) {
            try {
                date = dateFormat.parse (s);
            } catch (ParseException ignored) {

            }
        }
        return date;
    }

    /**
     * 从头部解析出range列表
     * range头部问题不会发送400，而是416或200
     * 如果发生改变了就返回200，否则就会按照range返回206或416
     *
     * @return 如果根本没有range，或者range不合法，就返回null并且设置响应码
     */
    protected List<Range> parseRange(HttpServletRequest request, HttpServletResponse response,
                                     ResourceInfo resourceInfo) {

        String header = request.getHeader (IF_RANGE);
        if (header != null) {
            //处理if-range
            Date date = decodeDate (header);
            //header格式错误,或者修改时间错误,后续会返回200
            //时间间隔最少一秒
            if (date == null ||
                    (resourceInfo.lasModifiedDate >= date.getTime () + 1000)) {
                response.setStatus (HttpServletResponse.SC_OK);
                return null;
            }

        }

        //开始解析range
        header = request.getHeader (RANGE);

        if (header == null) {
            response.setStatus (HttpServletResponse.SC_OK);
            return null;
        }

        log.trace ("开始解析RANGE header {}", header);
        if (!header.startsWith ("bytes=")) {
            send416 (response, resourceInfo);
            return null;
        }

        List<Range> ranges = new ArrayList<> ();
        header = header.substring (6);
        for (String token : header.split (",")) {
            token = token.trim ();//逗号后有空格
            if (StringUtils.countMatches (token, '-') != 1) {
                send416 (response, resourceInfo);
                return null;
            }

            Range range = new Range ();
            //支持0-空，表示到最后
            int index = token.indexOf ('-');
            if (index == token.length () - 1) {
                token = token + (resourceInfo.length - 1);
            }

            String[] split = token.split ("-");
            try {
                range.start = Long.parseLong (split[0]);
            } catch (NumberFormatException e) {
                send416 (response, resourceInfo);
                return null;
            }
            try {
                range.end = Long.parseLong (split[1]);
            } catch (NumberFormatException e) {
                send416 (response, resourceInfo);
                return null;
            }
            range.length = resourceInfo.length;

            if (!range.validate ()) {
                send416 (response, resourceInfo);
                return null;
            }
            ranges.add (range);
        }

        return ranges;
    }

    /**
     * 暂未实现,解析请求头的CONTENT_RANGE header。。
     * TODO
     */
    protected Range parseContentRange(HttpServletRequest request, HttpServletResponse response) {
        if (request.getHeader (CONTENT_RANGE) != null) {
            throw new AssertionError ();
        }
        return null;
    }

    private void send416(HttpServletResponse response, ResourceInfo resourceInfo) {
        response.setStatus (HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
        response.setHeader (CONTENT_RANGE, "bytes */" + resourceInfo.length);
    }

    /**
     * Return the relative path associated with this servlet.
     * 获得path，从而得到自己服务的静态资源路径
     *
     * @param request The servlet request we are processing
     */
    protected String getRelativePath(HttpServletRequest request) {
        String result = request.getPathInfo ();
        if (result == null) {
            result = request.getServletPath ();
        }
        if ((result == null) || (result.equals (""))) {
            result = "/";
        }
        return normalize (result);
    }

    /**
     * @param content 是否有发送响应体
     */
    protected void serveResource(HttpServletRequest request, HttpServletResponse response,
                                 boolean content) throws ServletException, IOException {
        //当前defaultServlet相对于当前context的path，即去除了context前缀的path
        //resources的docbase是context的基础路径
        String relativePath = getRelativePath (request);
        FileDirContext resources = getResources ();
        ResourceInfo resourceInfo = new ResourceInfo (relativePath, resources);

        //处理响应头:包括404、welcome file、list dir、range、last modified
        if (!resourceInfo.exists) {
            response.sendError (HttpServletResponse.SC_NOT_FOUND, request.getRequestURI ());
            return;
        }

        if (resourceInfo.collection) {
            //welcome files
            ResourceInfo welcomeFile = checkWelcomeFiles (relativePath, resources);
            if (welcomeFile != null) {
                String contextPath = request.getContextPath ();
                String redirectPath = welcomeFile.path;
                //加上context path才是路径
                if ((contextPath != null) && (!contextPath.equals ("/"))) {
                    redirectPath = contextPath + redirectPath;
                }
                response.sendRedirect (redirectPath);
                return;
            }

            //list dir,具体list只有有响应体的时候才render
            if (!listFiles) {
                //不能list dir就返回404
                response.sendError (HttpServletResponse.SC_NOT_FOUND, request.getRequestURI ());
                return;
            }
        } else {
            //todo etag
            //contentType,dir的content type=null
            String contentType;
            contentType = getServletContext ().getMimeType (resourceInfo.path);
            if (contentType != null) {
                response.setContentType (contentType);
            }

            if (acceptRanges) {
                response.setHeader (ACCEPT_RANGES, "bytes");
            } else {
                response.setHeader (ACCEPT_RANGES, "none");
            }

            //lastmodified
            response.setHeader (LAST_MODIFIED, resourceInfo.httpDate);
            response.setHeader (ETAG, getETag (resourceInfo));
            if (setCacheControl) {
                response.setHeader (CACHE_CONTROL, cacheControlString);
            }

            if (!checkIfHeaders (request, response, resourceInfo)) {
                return;
            }
        }


        ServletOutputStream stream = null;
        //处理响应体
        if (content) {
            stream = response.getOutputStream ();
            if (stream == null) {
                throw new IOException ();
            }
        }

        List<Range> ranges = parseRange (request, response, resourceInfo);
        if (!resourceInfo.collection && ranges != null && ranges.size () > 0) {
            //range,HEAD请求方法也会返回content-range header;head请求不会受到content-length干扰
            response.setStatus (HttpServletResponse.SC_PARTIAL_CONTENT);
            if (ranges.size () == 1) {
                Range range = ranges.get (0);
                response.setHeader (CONTENT_RANGE,
                        String.format ("bytes %d-%d/%d", range.start, range.end, resourceInfo.length));
                response.setContentLengthLong (range.end - range.start + 1);

                if (content) {
                    try {
                        response.setBufferSize (output);
                    } catch (IllegalStateException ignored) {

                    }
                    copy (resourceInfo, stream, range);
                }
            } else {
                response.setHeader (CONTENT_TYPE, "multipart/byteranges; boundary=" + mimeSeparation);
                if (content) {
                    try {
                        response.setBufferSize (output);
                    } catch (IllegalStateException ignored) {

                    }
                    String contentType = getServletContext ().getMimeType (resourceInfo.path);
                    copy (resourceInfo, stream, ranges, contentType);
                }

            }
            return;
        }

        //剩下的要么是collection，要么是没有range(或者range不合法)的普通文件
        response.setStatus (HttpServletResponse.SC_OK);
        if (content) {
            if (resourceInfo.collection) {
                response.setContentType ("text/html;charset=UTF-8");
                resourceInfo.setStream
                        (render (request.getContextPath (), resourceInfo));
            }

            if (!resourceInfo.collection) {
                response.setContentLengthLong (resourceInfo.length);
            }

            try {
                response.setBufferSize (output);
            } catch (IllegalStateException ignored) {

            }
            copy (resourceInfo, stream);
        }

    }

    /**
     * 输出多range的body,内部保证close
     * 需要多次打开文件
     */
    private void copy(ResourceInfo resourceInfo, ServletOutputStream outputStream,
                      List<Range> ranges, String contentType) throws IOException {
        try {
            for (Range range : ranges) {
                outputStream.println ("--" + mimeSeparation);
                //注意header冒号:后加一个空格
                outputStream.println (CONTENT_RANGE + ": " +
                        String.format ("bytes %d-%d/%d", range.start, range.end, resourceInfo.length));
                if (contentType != null) {
                    outputStream.println (CONTENT_TYPE + ": " + contentType);
                }
                outputStream.println ();//再加一个\r\n就和http header一样

                InputStream inputStream = resourceInfo.createStream ();
                inputStream.skip (range.start);
                try {
                    for (long i = range.start; i <= range.end; i++) {
                        outputStream.write (inputStream.read ());
                    }
                } finally {
                    inputStream.close ();
                }
            }
            outputStream.println ("--" + mimeSeparation + "--");
        } finally {
            try {
                outputStream.close ();
            } catch (Throwable ignored) {

            }
        }
    }

    protected String getETag(ResourceInfo resourceInfo) {
        if (!StringUtils.isEmpty (resourceInfo.strongETag)) {
            return resourceInfo.strongETag;
        } else if (!StringUtils.isEmpty (resourceInfo.weakETag)) {
            return resourceInfo.weakETag;
        } else {
            return "W/\"" + resourceInfo.length + "-"
                    + resourceInfo.lasModifiedDate + "\"";
        }
    }

    protected boolean checkIfHeaders(HttpServletRequest request, HttpServletResponse response,
                                     ResourceInfo resourceInfo) throws IOException {

        return checkIfMatch (request, response, resourceInfo)
                && checkIfModifiedSince (request, response, resourceInfo)
                && checkIfNoneMatch (request, response, resourceInfo)
                && checkIfUnmodifiedSince (request, response, resourceInfo);

    }

    private boolean checkIfMatch(HttpServletRequest request, HttpServletResponse response,
                                 ResourceInfo resourceInfo) throws IOException {
        String header = request.getHeader (IF_MATCH);
        String etag = getETag (resourceInfo);
        boolean match = false;

        if (header != null) {
            if (header.equals ("*")) {
                match = resourceInfo.exists;//存在就可以匹配
            } else {
                //可能有逗号分隔的多个
                for (String s : header.split (",")) {
                    if (s.trim ().equals (etag)) {
                        match = true;
                        break;
                    }
                }
            }
            if (!match) {
                response.sendError (HttpServletResponse.SC_PRECONDITION_FAILED);
            }
            return match;
        } else return true;
    }

    private boolean checkIfModifiedSince(HttpServletRequest request, HttpServletResponse response,
                                         ResourceInfo resourceInfo) throws IOException {
        long header = request.getDateHeader (IF_MODIFIED_SINCE);
        long modifiedTime = resourceInfo.lasModifiedDate;

        if (header != -1) {
            //if-none-match优先
            if ((request.getHeader (IF_NONE_MATCH) == null) &&
                    Math.abs (modifiedTime - header) <= 1000) {
                response.sendError (HttpServletResponse.SC_NOT_MODIFIED);
                return false;
            }
        }
        return true;
    }

    private boolean checkIfUnmodifiedSince(HttpServletRequest request, HttpServletResponse response,
                                           ResourceInfo resourceInfo) throws IOException {
        long header = request.getDateHeader (IF_UNMODIFIED_SINCE);
        long modifiedTime = resourceInfo.lasModifiedDate;

        if (header != -1) {
            //if-none-match优先
            if ((request.getHeader (IF_MATCH) == null) &&
                    Math.abs (modifiedTime - header) > 1000) {
                response.sendError (HttpServletResponse.SC_PRECONDITION_FAILED);
                return false;
            }
        }
        return true;
    }

    /**
     * 注意如果是GET和HEAD的话，条件不满足返回304（用于cache），否则条件不满足返回412（用于创建文件）
     */
    private boolean checkIfNoneMatch(HttpServletRequest request, HttpServletResponse response,
                                     ResourceInfo resourceInfo) throws IOException {
        String header = request.getHeader (IF_NONE_MATCH);
        String etag = getETag (resourceInfo);
        boolean nonMatch = true;

        if (header != null) {
            if (header.equals ("*")) {
                nonMatch = !resourceInfo.exists;//如果不存在这个资源，那就能保证无法匹配*
            } else {
                for (String s : header.split (",")) {
                    if (s.trim ().equals (etag)) {
                        nonMatch = false;
                        break;
                    }
                }
            }

            if (!nonMatch) {
                String method = request.getMethod ().toLowerCase ();
                if (method.equals ("get") || method.equals ("head")) {
                    response.sendError (HttpServletResponse.SC_NOT_MODIFIED);
                } else {
                    response.sendError (HttpServletResponse.SC_PRECONDITION_FAILED);
                }
            }
            return nonMatch;
        } else return true;
    }

    /**
     * 查看welcomefile是否存在
     * welcome file是一个文件名，不含路径，例如index.html
     *
     * @param pathname 必须是相对context的相对路径
     */
    private ResourceInfo checkWelcomeFiles(String pathname,
                                           FileDirContext resources) {
        if (!pathname.endsWith ("/")) {
            pathname += "/";
        }

        for (String welcome : welcomes) {
            ResourceInfo resourceInfo = new ResourceInfo (pathname + welcome, resources);
            if (resourceInfo.exists) {
                return resourceInfo;
            }
        }
        return null;
    }

    /**
     * 基于stream的方式和基于writer的方式不同，writer适合处理字符，而二进制数据必须使用stream
     */
    private void copy(ResourceInfo resourceInfo, ServletOutputStream outputStream)
            throws IOException {
        try (InputStream inputStream = resourceInfo.getStream ()) {
            IOUtils.copy (inputStream, outputStream);
        } finally {
            outputStream.close ();
        }
    }

    private void copy(ResourceInfo resourceInfo, ServletOutputStream ostream,
                      Range range)
            throws IOException {

        InputStream resourceInputStream = resourceInfo.getStream ();
        copyRange (resourceInputStream, ostream, range.start, range.end);

    }

    /**
     * 二进制资源
     */
    private void copyRange(InputStream inputStream,
                           ServletOutputStream outputStream,
                           long start, long end) throws IOException {

        try {
            inputStream.skip (start);
            try (BufferedOutputStream stream = new BufferedOutputStream (outputStream)) {

                for (long i = start; i <= end; i++) {
                    stream.write (inputStream.read ());
                }
            }
        } finally {
            try {
                inputStream.close ();
            } catch (IOException ignored) {

            }
        }

    }

    /**
     * 把\换成/
     */
    private String replace(String s) {
        while (s.contains ("\\")) {
            s = s.replace ('\\', '/');
        }
        return s;
    }

    private String getRelativePath(String s) {
        s = replace (s);
        if (s.contains ("/")) {
            return s.substring (s.lastIndexOf ('/') + 1);
        } else return s;
    }

    /**
     * Return an InputStream to an HTML representation of the contents
     * of this directory.
     * 渲染出类似于nginx的目录页面
     *
     * @param contextPath Context path to which our internal paths are
     *                    relative
     */
    protected InputStream render(String contextPath, ResourceInfo resourceInfo) {
        String name = resourceInfo.path;

        // Prepare a writer to a buffered area
        ByteArrayOutputStream stream = new ByteArrayOutputStream ();
        OutputStreamWriter osWriter = null;
        try {
            osWriter = new OutputStreamWriter (stream, StandardCharsets.UTF_8);
        } catch (Exception e) {
            // Should never happen
            osWriter = new OutputStreamWriter (stream);
        }
        PrintWriter writer = new PrintWriter (osWriter);

        StringBuilder sb = new StringBuilder ();

        // Render the page header
        sb.append ("<html>\r\n");
        sb.append ("<head>\r\n");
        sb.append ("<title>");
        sb.append ("目录:").append (name);
        sb.append ("</title>\r\n");
        sb.append ("<STYLE><!--");
        sb.append ("H1{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append ("H3{font-family : sans-serif,Arial,Tahoma;color : white;background-color : #0086b2;} ");
        sb.append ("BODY{font-family : sans-serif,Arial,Tahoma;color : black;background-color : white;} ");
        sb.append ("B{color : white;background-color : #0086b2;} ");
        sb.append ("A{color : black;} ");
        sb.append ("HR{color : #0086b2;} ");
        sb.append ("--></STYLE> ");
        sb.append ("</head>\r\n");
        sb.append ("<body>");
        sb.append ("<h1>");
        sb.append ("目录:").append (name);

        // Render the link to our parent (if required)
        String parentDirectory = name;
        if (parentDirectory.endsWith ("/")) {
            parentDirectory =
                    parentDirectory.substring (0, parentDirectory.length () - 1);
        }
        int slash = parentDirectory.lastIndexOf ('/');
        if (slash >= 0) {
            String parent = name.substring (0, slash);
            sb.append (" - <a href=\"");
            sb.append (urlEncoder.encode (contextPath));
            if (parent.equals (""))
                parent = "/";
            sb.append (urlEncoder.encode (parent));
            if (!parent.endsWith ("/"))
                sb.append ("/");
            sb.append ("\">");
            sb.append ("<b>");
            sb.append ("父目录:").append (parent);
            sb.append ("</b>");
            sb.append ("</a>");
        }

        sb.append ("</h1>");
        sb.append ("<HR size=\"1\" noshade>");

        sb.append ("<table width=\"100%\" cellspacing=\"0\"" +
                " cellpadding=\"5\" align=\"center\">\r\n");

        // Render the column headings
        sb.append ("<tr>\r\n");
        sb.append ("<td align=\"left\"><font size=\"+1\"><strong>");
        sb.append ("文件名");
        sb.append ("</strong></font></td>\r\n");
        sb.append ("<td align=\"center\"><font size=\"+1\"><strong>");
        sb.append ("文件大小");
        sb.append ("</strong></font></td>\r\n");
        sb.append ("<td align=\"right\"><font size=\"+1\"><strong>");
        sb.append ("上次修改时间");
        sb.append ("</strong></font></td>\r\n");
        sb.append ("</tr>");


        // Render the directory entries within this directory
        FileDirContext directory = resourceInfo.directory;
        Collection<Object> list = resourceInfo.resources.list (resourceInfo.path);
        boolean shade = false;
        for (Object o : list) {
            String resourceName;
            //不能absolute
            if (o instanceof FileDirContext) {
                resourceName = ((FileDirContext) o).getAbsoluteFile ().getAbsolutePath ();
            } else {
                resourceName = ((FileDirContext.FileResource) o).getFile ().getAbsolutePath ();
            }
            resourceName = getRelativePath (resourceName);

            ResourceInfo childResourceInfo =
                    new ResourceInfo (resourceName, directory);

            String trimmed = resourceName/*.substring(trim)*/;
            if (trimmed.equalsIgnoreCase ("WEB-INF") ||
                    trimmed.equalsIgnoreCase ("META-INF"))
                continue;

            sb.append ("<tr");
            if (shade)
                sb.append (" bgcolor=\"eeeeee\"");
            sb.append (">\r\n");
            shade = !shade;

            sb.append ("<td align=\"left\">&nbsp;&nbsp;\r\n");
            sb.append ("<a href=\"");
            sb.append (urlEncoder.encode (contextPath));
            resourceName = urlEncoder.encode (name + resourceName);
            sb.append (resourceName);
            if (childResourceInfo.collection)
                sb.append ("/");
            sb.append ("\"><tt>");
            sb.append (trimmed);
            if (childResourceInfo.collection)
                sb.append ("/");
            sb.append ("</tt></a></td>\r\n");

            sb.append ("<td align=\"right\"><tt>");
            if (childResourceInfo.collection)
                sb.append ("&nbsp;");
            else
                sb.append (renderSize (childResourceInfo.length));
            sb.append ("</tt></td>\r\n");

            sb.append ("<td align=\"right\"><tt>");
            sb.append (childResourceInfo.httpDate);
            sb.append ("</tt></td>\r\n");

            sb.append ("</tr>\r\n");

        }

        // Render the page footer
        sb.append ("</table>\r\n");

        sb.append ("<HR size=\"1\" noshade>");
        sb.append ("<h3>").append (Globals.SERVER_INFO).append ("</h3>");
        sb.append ("</body>\r\n");
        sb.append ("</html>\r\n");

        writer.write (sb.toString ());
        writer.flush ();
        return new ByteArrayInputStream (stream.toByteArray ());
    }

    protected String renderSize(long size) {
        long leftSide = size / 1024;
        long rightSide = (size % 1024) / 103;   // Makes 1 digit
        if ((leftSide == 0) && (rightSide == 0) && (size > 0))
            rightSide = 1;
        return ("" + leftSide + "." + rightSide + " kb");
    }

    /**
     * 区间类
     */
    protected static class Range {
        public long start;
        public long end;
        /**
         * length指的是总长度
         */
        public long length;

        public boolean validate() {
            if (end >= length)
                end = length - 1;
            return ((start >= 0) && (end >= 0) && (start <= end)
                    && (length > 0));
        }

    }

    /**
     * 资源类，会获得某个文件夹下的所有资源
     */
    @Slf4j
    protected static class ResourceInfo {

        public Object object;
        public FileDirContext directory;
        public FileDirContext.FileResource file;
        public ResourceAttributes attributes;
        public String path;
        public long creationDate;
        public String httpDate;
        public long lasModifiedDate;
        public long length;
        public boolean collection;
        public String weakETag;
        public String strongETag;
        public boolean exists;
        public FileDirContext resources;
        protected InputStream is;

        public ResourceInfo(String path, FileDirContext resources) {
            set (path, resources);
        }

        protected boolean isAbsolute(String path) {
            return new File (path).isAbsolute ();
        }

        public void set(String path, FileDirContext resources) {
            if (isAbsolute (path)) {
                exists = false;
                log.warn ("DefaultServlet 被请求绝对路径静态资源 {}", path);
                return;
            }

            this.path = path;
            this.resources = resources;
            exists = true;

            try {
                object = resources.lookup (path);
                if (object instanceof FileDirContext.FileResource) {
                    //是文件
                    file = (FileDirContext.FileResource) object;
                    collection = false;
                } else if (object instanceof FileDirContext) {
                    //是文件夹
                    directory = (FileDirContext) object;
                    collection = true;
                } else {
                    exists = false;
                }
            } catch (Throwable e) {
                exists = false;
            }

            if (exists) {
                try {
                    attributes = resources.getAttributes (path);
                    if (attributes != null) {
                        Date tempDate = attributes.getCreationDate ();
                        if (tempDate != null)
                            creationDate = tempDate.getTime ();

                        tempDate = attributes.getLastModifiedDate ();
                        if (tempDate != null) {
                            synchronized (format) {
                                httpDate = format.format (tempDate);
                            }
                            lasModifiedDate = tempDate.getTime ();
                        } else {
                            synchronized (format) {
                                httpDate = format.format (new Date (System.currentTimeMillis ()));
                            }
                        }

                        weakETag = attributes.getETag ();//FIXME
                        strongETag = attributes.getETag (true);
                        length = attributes.getContentLength ();
                    }
                } catch (Throwable e) {
                    exists = false;
                }
            }

        }

        public String toString() {
            return path;
        }

        public InputStream getStream() throws IOException {

            if (is != null)
                return is;
            if (file != null)
                return file.streamContent ();
            else
                return null;
        }

        public void setStream(InputStream is) {
            this.is = is;
        }

        public InputStream createStream() throws IOException {
            return file.streamContent ();
        }

    }

}
