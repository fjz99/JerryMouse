package com.example.mapper;

import com.example.Container;
import com.example.Engine;
import com.example.Host;
import com.example.connector.Request;
import lombok.extern.slf4j.Slf4j;

/**
 * 注意server的名字必须和engine要map的相等，才能map到host
 *
 * @date 2021/12/30 20:54
 */
@Slf4j
public class StandardEngineMapper extends AbstractMapper {

    @Override
    public Container map(Request request, boolean update) {

        Engine engine = (Engine) getContainer ();

        //基于server name，即域名
        String server = request.getRequest ().getServerName ();
        if (server == null) {
            server = engine.getDefaultHost ();
            if (update)
                request.setServerName (server);
        }
        if (server == null)
            return null;
        server = server.toLowerCase ();
        log.trace ("Mapping server name '" + server + "'");

        // Find the matching child Host directly
        log.trace (" Trying a direct match");
        Host host = (Host) engine.findChild (server);

        // Find a matching Host by alias.
        if (host == null) {
            log.trace (" Trying an alias match");
            Container[] children = engine.findChildren ();
            for (Container child : children) {
                String[] aliases = ((Host) child).findAliases ();
                for (String alias : aliases) {
                    if (server.equals (alias)) {
                        host = (Host) child;
                        break;
                    }
                }
                if (host != null)
                    break;
            }
        }

        // Trying the "default" host if any
        if (host == null) {
            log.trace (" Trying the default host");
            host = (Host) engine.findChild (engine.getDefaultHost ());
        }

        // Update the Request if requested, and return the selected Host
        // No update to the Request is required
        if (host == null) {
            log.warn ("mapping host {} 失败，没有找到对应的host组件", server);
        } else {
            log.debug ("mapping host {} 成功，映射到的host组件为 {}", server, host.getName ());
        }


        return host;

    }
}
