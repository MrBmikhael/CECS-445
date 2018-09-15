/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

@Controller
@SpringBootApplication
public class Main {

	@Value("${spring.datasource.url}")
	private String dbUrl;

	@Autowired
	private DataSource dataSource;

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Main.class, args);
	}

	@RequestMapping("/")
	String index() {
		return "index";
	}

	@RequestMapping("/dashboard")
	String dashboard() {
		return "dashboard";
	}
  
	@RequestMapping("/setup")
	String db(Map<String, Object> model) {
		try (Connection connection = dataSource.getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (ID SERIAL PRIMARY KEY, USERNAME TEXT UNIQUE NOT NULL, PASSWORD TEXT NOT NULL)");
			stmt.executeUpdate("INSERT INTO users (USERNAME, PASSWORD) VALUES ('admin', 'admin')");
			ResultSet rs = stmt.executeQuery("SELECT * FROM users");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getObject("USERNAME") + " / " + rs.getObject("PASSWORD"));
			}

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}

    @Bean
    public DataSource dataSource() throws SQLException {
        if (dbUrl == null || dbUrl.isEmpty()) {
			String url = "postgres://aynswouxmwktev:a6e450bd67a99278ae791bc37b5755acb08c0e452476f1de8c97a1c4f28a372c@ec2-54-83-50-145.compute-1.amazonaws.com:5432/dfaehfj8qacat4";
			System.out.println("================================================================================");
			System.out.println("No dbUrl!");
			System.out.println(u1rl);
			System.out.println("================================================================================");
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(url);
			return new HikariDataSource(config);
		} else {
			System.out.println("================================================================================");
			System.out.println("Found dbUrl!");
			System.out.println(dbUrl);
			System.out.println("================================================================================");
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(dbUrl);
			return new HikariDataSource(config);
		}
	}

}
