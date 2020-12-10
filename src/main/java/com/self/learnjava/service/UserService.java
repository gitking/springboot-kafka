package com.self.learnjava.service;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.BeanPropertyRowMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.self.learnjava.entity.User;

/*
 * 老大这一章的UserService没有加@Transactional注解，HikariCP的auto-commit是关上的，register方法里也没有事务提交，导致无法完成注册。
 * 自己加上，这就是debug的过程
 */
@Component
@Transactional
public class UserService {
	final Logger logger = LoggerFactory.getLogger(getClass());
	
	@Autowired
	JdbcTemplate jdbcTemplate;
	
	RowMapper<User> userRowMapper = new BeanPropertyRowMapper<>(User.class);
	
	public User getUserById(long id) {
		return jdbcTemplate.queryForObject("SELECT * FROM users WHERE id = ?", new Object[]{id}, userRowMapper);
	}
	
	public User getUserByEmail(String email) {
		return jdbcTemplate.queryForObject("SELECT * FROM users WHERE email = ?", new Object[]{email}, userRowMapper);
	}
	
	public User signin(String email, String password) {
		logger.info("try register by {}...", email);
		User user = getUserByEmail(email);
		if (user.getPassword().equals(password)) {
			return user;
		} else {
			throw new RuntimeException("login failed.");
		}
	}
	
	public User register(String email, String password, String name) {
		logger.info("try register by {}...", email);
		User user = new User();
		user.setEmail(email);
		user.setPassword(password);
		user.setName(name);
		user.setCreatedAt(System.currentTimeMillis());
		KeyHolder holder = new GeneratedKeyHolder();
		if (1 != jdbcTemplate.update((conn)->{
			PreparedStatement ps = conn.prepareStatement("INSERT INTO users (email, password, name, createdAt) VALUES (?,?,?,?)", Statement.RETURN_GENERATED_KEYS);
			ps.setObject(1, user.getEmail());
			ps.setObject(2, user.getPassword());
			ps.setObject(3, user.getName());
			ps.setObject(4, user.getCreatedAt());
			return ps;
		}, holder)) {
			throw new RuntimeException("Insert failed.");
		}
		user.setId(holder.getKey().longValue());
		return user;
	}
	
	public void updateUser(User user) {
		if (1 != jdbcTemplate.update("UPDATE users SET name = ? WHERE id = ?", user.getName(), user.getId())) {
			throw new RuntimeException("User not found by id");
		}
	}
	
	public List<User> getUsers() {
		return jdbcTemplate.query("SELECT * FROM users", userRowMapper);
	}
}