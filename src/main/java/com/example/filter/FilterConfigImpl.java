package com.example.filter;

import com.example.Context;
import com.example.descriptor.FilterDefinition;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.Objects;

/**
 * 用于在filter初始化的时候把配置传进去，
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
    private Filter filter;
    private final FilterDefinition filterDefinition;

    public FilterConfigImpl(Context context, FilterDefinition filterDefinition) {
        Objects.requireNonNull (context);
        Objects.requireNonNull (filterDefinition);

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
    private Filter getFilter() {
        if (filter != null) {
            return filter;
        }

        ClassLoader classLoader = context.getLoader ().getClassLoader ();
        String filterName = filterDefinition.getFilterClass ();
        Class<Filter> clazz = null;
        try {
            clazz = (Class<Filter>) classLoader.loadClass (filterName);
        } catch (ClassNotFoundException e) {
            e.printStackTrace ();
            log.error ("filter class {} not found", filterName);
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
}
