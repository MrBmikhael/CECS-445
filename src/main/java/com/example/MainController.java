
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
import java.util.Enumeration;
import javax.servlet.http.*;

import com.example.database;

@Controller
public class MainController {
	
	@RequestMapping(value = "/", method = RequestMethod.GET)
	String root(Map<String, Object> model, HttpServletRequest request) {
		if (!request.getSession().isNew())
		{
			Enumeration attributes = request.getSession().getAttributeNames();
			while(attributes.hasMoreElements())
			{
				String ele = attributes.nextElement().toString();
				System.out.println(ele + ":" + request.getSession().getAttribute(ele));
				model.put(ele, request.getSession().getAttribute(ele));
			}
		}
		
		return "index";
	}
  
	@RequestMapping(value = "/setup", method = RequestMethod.GET)
	String setup(Map<String, Object> model) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (ID SERIAL PRIMARY KEY, USERNAME TEXT UNIQUE NOT NULL, PASSWORD TEXT NOT NULL)");
			stmt.executeUpdate("INSERT INTO users (USERNAME, PASSWORD) VALUES ('admin', 'admin')");
			ResultSet rs = stmt.executeQuery("SELECT * FROM users");
			
			stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS timesheet (ID SERIAL, USER_ID INTEGER NOT NULL REFERENCES users(ID), ACTION TEXT NOT NULL, TIMESTAMP timestamp NOT NULL, PRIMARY KEY (ID,USER_ID))");

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
}
