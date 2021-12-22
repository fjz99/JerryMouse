package com.example.session;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

/**
 * @date 2021/12/22 11:25
 */
@Slf4j
public abstract class AbstractPersistentManager
        extends AbstractManager
        implements StoreManager {

    /**
     * 查看是否加载到内存
     */
    public boolean isLoaded(String id) {
        try {
            if (super.findSession (id) != null) {
                return true;
            }
        } catch (IOException e) {
            e.printStackTrace ();
            log.error ("isLoaded ERR");
        }
        return false;
    }
}
