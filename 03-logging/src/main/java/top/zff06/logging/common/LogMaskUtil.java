package top.zff06.logging.common;

/**
 * @auther zff06
 * @data 2026/9/23
 * @description LogMaskUtil
 **/
public class LogMaskUtil {
    private LogMaskUtil() {
    }

    public static String maskPhone(String phone) {
        if (phone == null || phone.length() != 11) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(7);
    }
}
