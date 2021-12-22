package com.example.session;

import java.io.IOException;

/**
 * @date 2021/12/22 11:25
 */
public abstract class AbstractManager implements Manager {

    @Override
    public Session findSession(String id) throws IOException {
        return null;
    }
}
