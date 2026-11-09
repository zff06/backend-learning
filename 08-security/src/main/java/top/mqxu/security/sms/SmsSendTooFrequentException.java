package top.mqxu.security.sms;

/**
 * 发码太频繁，Controller 接到它返回 400。
 */
public class SmsSendTooFrequentException extends RuntimeException {

    public SmsSendTooFrequentException(String message) {
        super(message);
    }
}
