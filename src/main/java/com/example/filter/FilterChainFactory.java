package com.example.filter;

import com.example.Context;
import com.example.Wrapper;
import com.example.core.FilterChainImpl;
import com.example.descriptor.FilterMapping;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.Servlet;

import static com.example.util.ArgumentChecker.requireNonNull;

/**
 * todo filter池化
 * 根据{@link com.example.descriptor.FilterMapping}创建对应的{@link javax.servlet.FilterChain}
 * <p>注意，这个类创建filterChain只是负责组装，而不会创建新的实例，
 * 具体的实例cache map在context组件（显然）中</p>
 *
 * @date 2021/12/25 20:24
 */
public final class FilterChainFactory {
    private FilterChainFactory() {

    }

    /**
     * 简化版，只会根据url进行创建
     */
    public static FilterChain createFilterChain(String path, Wrapper wrapper, Servlet servlet) {
        requireNonNull (path, wrapper, servlet);

        Context context = (Context) wrapper.getParent ();
        FilterMapping[] filterMaps = context.findFilterMaps ();
        FilterChainImpl filterChain = new FilterChainImpl ();//todo 参见tomcat8，池化?

        filterChain.setServlet (servlet);

        if (filterMaps == null) {
            return filterChain;
        }

        for (FilterMapping filterMap : filterMaps) {
            if (matchFilterMapping (filterMap, path)) {
                //context维护FilterConfig实例，本方法只负责组装成chain
                FilterConfig filterConfig = context.findFilterConfig (filterMap.getFilterName ());
                if (filterConfig != null) {
                    filterChain.addFilter (filterConfig);
                }
            }
        }
        return filterChain;
    }

    private static boolean matchFilterMapping(FilterMapping filterMap, String url) {
        if (filterMap.isMatchAllUrlPatterns ()) {
            return true;
        }

        for (String urlPattern : filterMap.getUrlPatterns ()) {
            if (matchFiltersURL (urlPattern, url)) {
                return true;
            }
        }
        return false;
    }


    /**
     * 抄的
     * Return <code>true</code> if the context-relative request path
     * matches the requirements of the specified filter mapping;
     * otherwise, return <code>false</code>.
     *
     * @param testPath    URL mapping being checked
     * @param requestPath Context-relative request path of this request
     */
    private static boolean matchFiltersURL(String testPath, String requestPath) {

        if (testPath == null) {
            return false;
        }

        // Case 1 - Exact Match
        if (testPath.equals (requestPath)) {
            return true;
        }

        // Case 2 - Path Match ("/.../*")
        // /*是通配
        if (testPath.equals ("/*")) {
            return true;
        }

        //如果pattern是/*结尾的，那么就直接比较/*之前的长度为n-2的字符串是否匹配即可
        if (testPath.endsWith ("/*")) {
            if (testPath.regionMatches (0, requestPath, 0,
                    testPath.length () - 2)) {
                //如果前缀/xxx/yyy/zzz匹配成功
                if (requestPath.length () == (testPath.length () - 2)) {
                    //如果恰好是前缀
                    return true;
                } else if ('/' == requestPath.charAt (testPath.length () - 2)) {
                    //即通配,虽然长，但是n-2 pos下的char为/,所以通配了
                    return true;
                }
            }
            return false;
        }

        // Case 3 - Extension Match
        if (testPath.startsWith ("*.")) {
            int slash = requestPath.lastIndexOf ('/');
            int period = requestPath.lastIndexOf ('.');
            if ((slash >= 0) && (period > slash)
                    && (period != requestPath.length () - 1)
                    && ((requestPath.length () - period)
                    == (testPath.length () - 1))) {
                return testPath.regionMatches (2, requestPath, period + 1,
                        testPath.length () - 2);
            }
        }

        // Case 4 - "Default" Match
        return false; // NOTE - Not relevant for selecting filters

    }
}
