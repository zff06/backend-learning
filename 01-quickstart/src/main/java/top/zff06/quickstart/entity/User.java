package top.zff06.quickstart.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * @auther zff06
 * @data 2026/9/9
 * @description User
 **/
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private Long id;
    private String name;
    private LocalDate birthday;
}
