package com.example.startup;


public final class Constants {

    /**
     * Name of the system property containing
     * the tomcat product installation path
     */
    public static final String CATALINA_HOME_PROP = "catalina.home";
    /**
     * Name of the system property containing
     * the tomcat instance installation path
     */
    public static final String CATALINA_BASE_PROP = "catalina.base";

    public static final String ApplicationContextXml = "META-INF/context.xml";
    public static final String DefaultContextXml = "conf/context.xml";
    public static final String HostContextXml = "context.xml.default";

    public static final String ParserLogName = "xmlParser";

    public static final String ApplicationWebXml = "/WEB-INF/web.xml";
    public static final String DefaultWebXml = "conf/web.xml";


    public static final String WebDtdPublicId_22 =
            "-//Sun Microsystems, Inc.//DTD Web Application 2.2//EN";
    public static final String WebDtdResourcePath_22 =
            //      "conf/web_22.dtd";
            "/web-app_2_2.dtd";

    public static final String WebDtdPublicId_23 =
            "-//Sun Microsystems, Inc.//DTD Web Application 2.3//EN";
    public static final String WebDtdResourcePath_23 =
            //      "conf/web_23.dtd";
            "/web-app_2_3.dtd";

    public static final String WarTracker = "/META-INF/war-tracker";

}
