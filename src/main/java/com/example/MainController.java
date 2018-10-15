
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.boot.builder.SpringApplicationBuilder;

import java.util.concurrent.TimeUnit;
import java.text.SimpleDateFormat;
import java.util.Date;
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
import com.example.record;

@Controller
public class MainController {
	
	public String getAttributeFromSession(HttpServletRequest request, String key) {
		if (!request.getSession().isNew())
		{
			Enumeration attributes = request.getSession().getAttributeNames();
			while(attributes.hasMoreElements())
			{
				String ele = attributes.nextElement().toString();
				
				if (ele.equals(key)) {
					return request.getSession().getAttribute(ele).toString();
				}
			}
		}
		
		return "";
	}
	
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
			
			try (Connection connection = database.getDataSource().getConnection()) {
				Statement stmt = connection.createStatement();
				String Username = request.getSession().getAttribute("user").toString();
				String UserID = database.getUserID(Username).toString();
				ResultSet rs = stmt.executeQuery("SELECT * FROM timesheet WHERE USER_ID='" + UserID + "' AND TIMESTAMP >= current_date ORDER BY TIMESTAMP desc LIMIT 4");

				ArrayList<record> output = new ArrayList<record>();
				while (rs.next()) {
					record a = new record();
					a.action = rs.getObject("ACTION").toString().replace("_"," ");
					a.timestamp = rs.getObject("TIMESTAMP").toString();
					output.add(a);
				}

				model.put("records", output);
			} catch (Exception e) {
				model.put("message", e.getMessage());
			}
			
		}
		
		return "index";
	}
	
	@RequestMapping(value = "/record", method = RequestMethod.POST)
	@ResponseBody
	String record(HttpServletRequest request) {
		if (database.addRecordForUser(getAttributeFromSession(request, "user"), request.getParameter("action")))
			return "Record updated successfully!";
		else
			return "Error adding record to database!";
	}
	
	@RequestMapping(value = "/timesheet", method = RequestMethod.GET)
	String timesheet(Map<String, Object> model, HttpServletRequest request) {
		if (!request.getSession().isNew())
		{
			Enumeration attributes = request.getSession().getAttributeNames();
			while(attributes.hasMoreElements())
			{
				String ele = attributes.nextElement().toString();
				System.out.println(ele + ":" + request.getSession().getAttribute(ele));
				model.put(ele, request.getSession().getAttribute(ele));
			}
			
			day[] week = database.getWeekTimesheet(request.getSession().getAttribute("user").toString());
			
			double totalHours = 0.0;
			SimpleDateFormat format = new SimpleDateFormat("HH:mm");
			
			for (int i = 0; i < 7; i++)
			{
				try {
				Date hours = format.parse(week[i].Total_Hours);
				int h = hours.getHours();
				double m = (hours.getMinutes()/60.00);
				totalHours += (h + m);
				}
				catch (Exception e) {}
			}

			model.put("week", week);
			model.put("totalHours", totalHours);
			
			return "timesheet";
		}
		else
		{
			return "redirect:/";
		}
	}
	
	@RequestMapping(value = "/profile", method = RequestMethod.GET)
	String profile(Map<String, Object> model, HttpServletRequest request) {
		if (!request.getSession().isNew())
		{
			Enumeration attributes = request.getSession().getAttributeNames();
			while(attributes.hasMoreElements())
			{
				String ele = attributes.nextElement().toString();
				System.out.println(ele + ":" + request.getSession().getAttribute(ele));
				model.put(ele, request.getSession().getAttribute(ele));
			}
			
			return "profile";
		}
		else
		{
			return "redirect:/";
		}
	}
	
	@RequestMapping(value = "/request", method = RequestMethod.GET)
	String requestPTO(Map<String, Object> model, HttpServletRequest request) {
		if (!request.getSession().isNew())
		{
			Enumeration attributes = request.getSession().getAttributeNames();
			while(attributes.hasMoreElements())
			{
				String ele = attributes.nextElement().toString();
				System.out.println(ele + ":" + request.getSession().getAttribute(ele));
				model.put(ele, request.getSession().getAttribute(ele));
			}
			
			return "request";
		}
		else
		{
			return "redirect:/";
		}
	}
  
    @RequestMapping(value = "/view", method = RequestMethod.GET)
	String view(Map<String, Object> model) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM timesheet");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getObject("USER_ID") + " | " + rs.getObject("ACTION").toString().replace("_"," ") + " | " + rs.getObject("TIMESTAMP"));
			}

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}
  
	@RequestMapping(value = "/setup", method = RequestMethod.GET)
	String setup(Map<String, Object> model) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (ID SERIAL PRIMARY KEY, FULLNAME TEXT NOT NULL, USERNAME TEXT UNIQUE NOT NULL, PASSWORD TEXT NOT NULL)");
			stmt.executeUpdate("INSERT INTO users (FULLNAME, USERNAME, PASSWORD) VALUES ('admin', 'admin', 'admin') ON CONFLICT DO NOTHING");
			ResultSet rs = stmt.executeQuery("SELECT * FROM users");
			
			stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS timesheet (ID SERIAL, USER_ID INTEGER NOT NULL REFERENCES users(ID), ACTION TEXT NOT NULL, TIMESTAMP timestamp NOT NULL, PRIMARY KEY (ID,USER_ID))");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getObject("FULLNAME") + "<br>" + rs.getObject("USERNAME") + " / " + rs.getObject("PASSWORD"));
			}

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}
	
	@RequestMapping(value = "/demo", method = RequestMethod.GET)
	String demo(Map<String, Object> model) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (ID SERIAL PRIMARY KEY, FULLNAME TEXT NOT NULL, USERNAME TEXT UNIQUE NOT NULL, PASSWORD TEXT NOT NULL)");
			stmt.executeUpdate("INSERT INTO users (FULLNAME, USERNAME, PASSWORD) VALUES ('admin', 'admin', 'admin') ON CONFLICT DO NOTHING");
			stmt.executeUpdate("INSERT INTO users (FULLNAME, USERNAME, PASSWORD) VALUES ('Bishoy Mikhael', 'bishoy.mikhael', '123456') ON CONFLICT DO NOTHING");
			ResultSet rs = stmt.executeQuery("SELECT * FROM users");
			
			stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS timesheet (ID SERIAL, USER_ID INTEGER NOT NULL REFERENCES users(ID), ACTION TEXT NOT NULL, TIMESTAMP timestamp NOT NULL, PRIMARY KEY (ID,USER_ID))");
			
			ResultSet rss = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='bishoy.mikhael'");
			rss.next();
			String UserID = rss.getObject("ID").toString();
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-14 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-14 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-14 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-14 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-13 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-13 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-13 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-13 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-12 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-12 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-12 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-12 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-11 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-11 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-11 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-11 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-10 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-10 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-10 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-10 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-09 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-09 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-09 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-09 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-08 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-08 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-08 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-08 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-07 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-07 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-07 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-07 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-06 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-06 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-06 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-06 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_In', '2018-10-05 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_Start', '2018-10-05 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Lunch_End', '2018-10-05 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', 'Clock_Out', '2018-10-05 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getObject("FULLNAME") + "<br>" + rs.getObject("USERNAME") + " / " + rs.getObject("PASSWORD"));
			}

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}
}
