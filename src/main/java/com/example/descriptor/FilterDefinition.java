package com.example.descriptor;

import lombok.*;

import javax.servlet.Filter;
import javax.servlet.annotation.WebInitParam;
import java.util.HashMap;
import java.util.Map;

/**
 * 描述filter配置的实体类
 * 对应的就是{@link javax.servlet.annotation.WebFilter}
 * 或者web.xml中的配置
 *
 * @date 2021/12/24 21:43
 */
@ToString
@Setter
@Getter
public final class FilterDefinition {
    /**
     * 对应 {@link WebInitParam}
     */
    @Setter(AccessLevel.NONE)
    private final Map<String, String> parameters = new HashMap<> ();

    private String description;
    private String displayName;
    private transient Filter filter;
    /**
     * The name of this filter, which must be unique among the filters
     * defined for a particular web application.
     */
    private String filterName;
    /**
     * The fully qualified name of the Java class that implements this filter.
     */
    private String filterClass;
    /**
     * ??
     */
    private String largeIcon;
    /**
     * ??
     */
    private String smallIcon;


    public void addInitParameter(String name, String value) {
        if (parameters.containsKey (name)) {
            // The spec does not define this but the TCK expects the first
            // definition to take precedence
            // 避免覆盖
            return;
        }
        parameters.put (name, value);
    }
}
