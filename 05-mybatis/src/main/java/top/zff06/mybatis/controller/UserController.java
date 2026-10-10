package top.zff06.mybatis.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import top.zff06.mybatis.dto.UserQuery;
import top.zff06.mybatis.entity.User;
import top.zff06.mybatis.service.UserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * @auther zff06
 * @data 2026/10/10
 * @description UserController
 **/
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /* 增 */
    @PostMapping
    public User create(@RequestBody User user) {
        userService.createUser(user);
        return user;
    }

    @PostMapping("/batch")
    public int createBatch(@RequestBody List<User> users) {
        userService.createUserBatch(users);
        return users.size();
    }

    /* 查 */
    @GetMapping("/{id}")
    public User getById(@PathVariable Long id) {
        return userService.getUser(id);
    }

    @GetMapping
    public List<User> list(UserQuery query) {
        return userService.listByCondition(query);
    }

    @GetMapping("/page")
    public Page<User> page(UserQuery query,
                           @RequestParam(defaultValue = "1") long current,
                           @RequestParam(defaultValue = "10") long size) {
        return userService.pageByCondition(query, current, size);
    }

    @GetMapping("/keyword")
    public List<User> keyword(@RequestParam String keyword) {
        return userService.searchByKeyword(keyword);
    }

    @GetMapping("/age-range")
    public List<User> ageRange(@RequestParam(required = false) Integer minAge,
                               @RequestParam(required = false) Integer maxAge) {
        return userService.listByAgeRange(minAge, maxAge);
    }

    @GetMapping("/top")
    public List<User> top(@RequestParam(defaultValue = "3") Integer limit) {
        return userService.listTop(limit);
    }

    @GetMapping("/usernames")
    public List<String> usernames() {
        return userService.listUsernames();
    }

    @GetMapping("/tag")
    public List<User> byTag(@RequestParam String tag) {
        return userService.listByTag(tag);
    }

    @GetMapping("/exists")
    public boolean exists(@RequestParam String username) {
        return userService.existsUsername(username);
    }

    @GetMapping("/count")
    public long count(@RequestParam(required = false) Integer gender) {
        return userService.countByGender(gender);
    }

    @GetMapping("/statistics")
    public Map<String, Object> statistics() {
        return userService.statistics();
    }

    /* 改 */
    @PutMapping("/{id}")
    public User update(@PathVariable Long id, @RequestBody User user) {
        user.setId(id);
        userService.modifyUser(user);
        return userService.getUser(id);
    }

    @PutMapping("/{id}/status")
    public boolean updateStatus(@PathVariable Long id, @RequestParam Integer status) {
        return userService.modifyUserStatus(id, status);
    }

    @PostMapping("/{id}/recharge")
    public User recharge(@PathVariable Long id, @RequestParam BigDecimal amount) {
        userService.recharge(id, amount);
        return userService.getUser(id);
    }

    /* 删 */
    @DeleteMapping("/{id}")
    public boolean delete(@PathVariable Long id) {
        return userService.removeUser(id);
    }

    @DeleteMapping("/batch")
    public boolean deleteBatch(@RequestParam List<Long> ids) {
        return userService.removeUserBatch(ids);
    }

    @DeleteMapping("/status/{status}")
    public boolean deleteByStatus(@PathVariable Integer status) {
        return userService.removeUserByStatus(status);
    }
}
