package com.example.core;

import com.example.Context;
import com.example.descriptor.FilterDefinition;
import com.example.filter.FilterConfigImpl;
import org.junit.jupiter.api.Test;

import javax.servlet.FilterConfig;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;

class FilterChainImplTest {
    Context context = mock (Context.class);
    Servlet servlet = mock (Servlet.class);

    {
        try {
            doAnswer (invocation -> {
//                System.out.println ("service");
                return null;
            }).when (servlet).service (any (), any ());
        } catch (ServletException | IOException e) {
            e.printStackTrace ();
        }
    }

    @Test
    void test() throws ServletException, IOException {
        FilterChainImpl filterChain = new FilterChainImpl ();
        IntStream.range (0, 10).forEach (x -> filterChain.addFilter (gf (String.valueOf (x))));
        filterChain.setServlet (servlet);
        filterChain.doFilter (null, null);
//        System.out.println ("done");
        assertThrows (IllegalStateException.class, () ->
                filterChain.doFilter (null, null));
        filterChain.release ();
        assertThrows (NullPointerException.class, () ->
                filterChain.doFilter (null, null));
    }

    private FilterConfig gf(String s) {
        FilterDefinition definition = new FilterDefinition ();
        definition.setFilter ((request, response, chain) -> {
//            System.out.println (s);
            chain.doFilter (request, response);
        });
        return new FilterConfigImpl (context, definition);
    }
}
