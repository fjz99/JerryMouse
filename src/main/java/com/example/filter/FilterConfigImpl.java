package com.example.filter;

import com.example.Context;
import com.example.descriptor.FilterDefinition;
import com.example.util.ArgumentChecker;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.Enumeration;

import static com.example.filter.Constants.SYSTEM_FILTER_PREFIX;

/**
 * 用于在filter初始化的时候把配置传进去，还有维护filter的生命周期
 * 类似地，{@link javax.servlet.ServletConfig}用于设置servlet初始化的配置，
 * 可以类比为配置实体类
 * 和{@link FilterDefinition}的区别在于，{@link FilterDefinition}是内部的
 * 而本类是包装类。。
 * 热加载，构造器中直接初始化
 *
 * @date 2021/12/24 21:58
 */
@Slf4j
public final class FilterConfigImpl implements FilterConfig {

    private final Context context;
    private final FilterDefinition filterDefinition;
    private Filter filter;

    public FilterConfigImpl(Context context, FilterDefinition filterDefinition) {
        ArgumentChecker.requireNonNull (context, filterDefinition);

        this.context = context;
        this.filterDefinition = filterDefinition;

        if (filterDefinition.getFilter () != null) {
            filter = filterDefinition.getFilter ();
        } else {
            filter = getFilter ();
        }

        initFilter ();
    }

    public FilterDefinition getFilterDefinition() {
        return filterDefinition;
    }

    @SuppressWarnings("unchecked")
    public Filter getFilter() {
        if (filter != null) {
            return filter;
        }

        //这个要区分filter的类型，内置filter不要用context的loader进行加载
        ClassLoader classLoader;
        String filterClass = filterDefinition.getFilterClass ();
        if (filterClass.startsWith (SYSTEM_FILTER_PREFIX)) {
            classLoader = getClass ().getClassLoader ();
        } else {
            classLoader = context.getLoader ().getClassLoader ();
        }

        Class<Filter> clazz = null;
        try {
            clazz = (Class<Filter>) classLoader.loadClass (filterClass);
        } catch (ClassNotFoundException e) {
            e.printStackTrace ();
            log.error ("filter class {} not found", filterClass);
        }

        if (clazz != null) {
            try {
                filter = clazz.newInstance ();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace ();
                log.error ("实例化filter失败 {}", e.toString ());
            }
        }

        return filter;
    }

    private void initFilter() {
        try {
            getFilter ().init (this);
        } catch (ServletException e) {
            e.printStackTrace ();
            log.error ("filter init failed: {}", e.toString ());
        }
    }

    @Override
    public String getFilterName() {
        return filterDefinition.getFilterName ();
    }

    @Override
    public ServletContext getServletContext() {
        return context.getServletContext ();
    }

    @Override
    public String getInitParameter(String name) {
        return filterDefinition.getParameters ().get (name);
    }

    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration (filterDefinition.getParameters ().keySet ());
    }


    public String getFilterClass() {
        return filterDefinition.getFilterClass ();
    }

    /**
     * 释放资源
     */
    public void release() {
        if (filter != null) {
            filter.destroy ();
            filter = null;
        }
    }

}
