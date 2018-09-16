
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
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

public class database {

	private static DataSource single_instance = null; 

	@Value("${spring.datasource.url}")
	private static String dbUrl;

	private static DataSource dataSource;

	public static DataSource getDataSource()
	{
        if (single_instance == null)
		{
			try {
				single_instance = createDataSource();
			}
			catch (Exception e) {}
		}
  
        return single_instance;
	}
	
	public static boolean checkLogin(String username, String password)
	{
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + username + "'");
			rs.next();
			
			// System.out.println("Read from DB: " + rs.getObject("USERNAME") + " / " + rs.getObject("PASSWORD"));
			// System.out.println("Read from input: " + username + " / " + password);

			return (rs.getObject("PASSWORD").equals(password));
			
		} catch (Exception e) {
			return false;
		}
	}
	
	private static DataSource createDataSource() throws SQLException {
        if (dbUrl == null || dbUrl.isEmpty()) {
			String url = "jdbc:postgresql://ec2-54-83-50-145.compute-1.amazonaws.com:5432/dfaehfj8qacat4?sslmode=require";
			System.out.println("================================================================================");
			System.out.println("No dbUrl!");
			System.out.println(url);
			System.out.println("================================================================================");
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(url);
			config.setUsername("aynswouxmwktev");
			config.setPassword("a6e450bd67a99278ae791bc37b5755acb08c0e452476f1de8c97a1c4f28a372c");
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
