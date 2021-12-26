package com.example.descriptor;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import static com.example.util.RequestUtil.URLDecode;

/**
 * 描述<code>&lt;filter-mapping&gt;</code>配置的实体类
 *
 * <pre>{@code
 *
 * <filter>
 * 	<display-name></display-name>		<!-- 0或1个 -->
 * 	<description></description>			<!-- 0或1个 -->
 * 	<icon>								<!-- 0或1个 -->
 * 		<small-icon></small-icon>		<!-- 0或1个 -->
 * 		<large-icon></large-icon>		<!-- 0或1个 -->
 * 	</icon>
 * 	<filter-name></filter-name>			<!-- 1个 -->
 * 	<filter-class></filter-class>		<!-- 1个 -->
 * 	<init-param>						<!-- 0或多个 -->
 * 		<description></description>		<!-- 0或1个 -->
 * 		<param-name></param-name>		<!-- 1个 -->
 * 		<param-value></param-value>		<!-- 1个 -->
 * 	</init-param>
 * </filter>
 * <filter-mapping>						<!-- 1或多个 -->
 * 	<filter-name></filter-name>			<!-- 1个 -->
 * 	<url-pattern></url-pattern>			<!-- <url-pattern>和<servlet-name>任选一个 -->
 * 	<servlet-name></servlet-name>
 * 	<dispatcher></dispatcher>			<!-- 0或多个 -->
 * </filter-mapping>
 *
 * }</pre>
 *
 * <p>这是一个简化版,只实现了filterName和urlPattern，更详细的见tomcat8 source code</p>
 * todo 支持dispatcher、servletName等标签
 *
 * @date 2021/12/25 20:10
 */
@ToString
@Getter
@Setter
public final class FilterMapping {
    private String filterName;

//    private String servletName;

    //减少内存消耗
    @Setter(AccessLevel.NONE)
    private String[] urlPatterns = new String[0];

    @Setter(AccessLevel.NONE)
    private boolean matchAllUrlPatterns = false;

    public void addURLPattern(String urlPattern) {
        addURLPatternDecoded (URLDecode (urlPattern));
    }

    /**
     * charset只支持utf-8
     */
    public void addURLPatternDecoded(String urlPattern) {
        if ("*".equals (urlPattern)) {
            this.matchAllUrlPatterns = true;
        } else {
            String[] results = new String[urlPatterns.length + 1];
            System.arraycopy (urlPatterns, 0, results, 0, urlPatterns.length);
            results[urlPatterns.length] = URLDecode (urlPattern);
            urlPatterns = results;
        }
    }

}
