package top.zff06.toolkit.support;

import lombok.Getter;
import lombok.Setter;

/**
 * @auther zff06
 * @data 2026/10/22
 * @description QrSession
 **/
@Getter
public class QrSession {

    public enum State {
        WAITING, SCANNED, CONFIRMED, CANCELED
    }

    private final String ticket;
    private final String content;
    private final long createdAt = System.currentTimeMillis();

    @Setter
    private volatile State state = State.WAITING;

    @Setter
    private volatile String phone;

    @Setter
    private volatile String token;

    public QrSession(String ticket, String content) {
        this.ticket = ticket;
        this.content = content;
    }
}
