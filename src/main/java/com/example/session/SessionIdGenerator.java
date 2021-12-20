package com.example.session;

public interface SessionIdGenerator {

    /**
     * @return the number of bytes for a session ID
     */
    int getSessionIdLength();

    /**
     * Specify the number of bytes for a session ID
     *
     * @param sessionIdLength   Number of bytes
     */
    void setSessionIdLength(int sessionIdLength);

    /**
     * Generate and return a new session identifier.
     *
     * @return the newly generated session id
     */
    String generateSessionId();
}
