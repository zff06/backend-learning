package top.mqxu.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import top.mqxu.security.entity.SysRole;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * ROLE_ 前缀从哪来。三个用例对应 6.3 节表格的三行。
 */
class SysRoleTest {

    private static SysRole role(String code, String grantType) {
        SysRole role = new SysRole();
        role.setRoleCode(code);
        role.setGrantType(grantType);
        return role;
    }

    @Test
    @DisplayName("grant_type=ROLE 补 ROLE_ 前缀，hasRole 才能通过")
    void roleGetsPrefix() {
        assertThat(role("ADMIN", SysRole.TYPE_ROLE).toAuthority())
                .isEqualTo("ROLE_ADMIN");
    }

    @Test
    @DisplayName("grant_type=PERM 原样返回，不补前缀")
    void permStaysRaw() {
        assertThat(role("ARTICLE_DELETE", SysRole.TYPE_PERM).toAuthority())
                .isEqualTo("ARTICLE_DELETE");
    }

    @Test
    @DisplayName("grant_type 为空时按角色处理")
    void nullGrantTypeTreatedAsRole() {
        // 库里有默认值 'ROLE'，但手工 insert 或老数据可能漏掉这一列。
        // 按角色处理比抛 NPE 更稳，也不至于让用户登不进来。
        assertThat(role("ADMIN", null).toAuthority())
                .isEqualTo("ROLE_ADMIN");
    }
}
