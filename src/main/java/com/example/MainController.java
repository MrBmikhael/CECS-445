
package com.example;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.sql.Connection;
import java.sql.ResultSet;
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
				model.put(ele, request.getSession().getAttribute(ele));
			}
			
			ArrayList<day> week = database.getWeekTimesheet(request.getSession().getAttribute("user").toString());
			
			double totalHours = 0.0;
			SimpleDateFormat format = new SimpleDateFormat("HH:mm");
			
			for (int i = 0; i < week.size(); i++)
			{
				try {
					Date hours = format.parse(week.get(i).Total_Hours);
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
				System.out.println(ele + " : " + request.getSession().getAttribute(ele));
				model.put(ele, request.getSession().getAttribute(ele));
			}

			if (request.getParameterMap().containsKey("id"))
			{
				if (request.getParameter("id").equals("") || request.getParameter("id").isEmpty())
				{
					model.put("ProfileData", database.getProfileById(database.getUserID(getAttributeFromSession(request, "user"))));
					return "profile";
				}

				model.put("ProfileData", database.getProfileById(request.getParameter("id").toString()));
				return "profile";
			}

			model.put("ProfileData", database.getProfileById(database.getUserID(getAttributeFromSession(request, "user"))));
			return "profile";
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
		System.out.println("Starting Setup ...");
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS departments (ID SERIAL PRIMARY KEY, NAME TEXT NOT NULL)");
			stmt.executeUpdate("INSERT INTO departments (NAME) VALUES ('HR') ON CONFLICT DO NOTHING");
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (ID SERIAL PRIMARY KEY, FULLNAME TEXT NOT NULL, USERNAME TEXT UNIQUE NOT NULL, PASSWORD TEXT NOT NULL, DEPARTMENT INTEGER REFERENCES departments(ID))");
			stmt.executeUpdate("INSERT INTO users (FULLNAME, USERNAME, PASSWORD, DEPARTMENT) VALUES ('admin', 'admin', 'admin', 1) ON CONFLICT DO NOTHING");
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS managers (ID SERIAL PRIMARY KEY, DEPARTMENT_ID INTEGER REFERENCES departments(ID), MANAGER_ID INTEGER REFERENCES users(ID))");
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pto_balances (ID SERIAL PRIMARY KEY, PTO_BALANCE INTEGER NOT NULL, EMPLOYEE_ID INTEGER REFERENCES users(ID))");
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pto_requests (ID SERIAL PRIMARY KEY, HOURS INTEGER NOT NULL, EMPLOYEE INTEGER REFERENCES users(ID) NOT NULL, REQUESTDATE DATE NOT NULL, STATUS TEXT, REASON TEXT)");
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS timesheet (ID SERIAL, USER_ID INTEGER NOT NULL REFERENCES users(ID), ACTION TEXT NOT NULL, TIMESTAMP timestamp NOT NULL, PRIMARY KEY (ID,USER_ID))");

			ResultSet rs = stmt.executeQuery("SELECT * FROM users");

			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getObject("FULLNAME") + " - " + rs.getObject("USERNAME") + " / " + rs.getObject("PASSWORD") + " - " + rs.getObject("DEPARTMENT"));
			}

			System.out.println("Setup Complete!");

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}
	
	@RequestMapping(value = "/demo", method = RequestMethod.GET)
	String demo(Map<String, Object> model) {
		System.out.println("Starting Demo ...");
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS departments (ID SERIAL PRIMARY KEY, NAME TEXT NOT NULL)");
			stmt.executeUpdate("INSERT INTO departments (NAME) VALUES ('HR') ON CONFLICT DO NOTHING");
			stmt.executeUpdate("INSERT INTO departments (NAME) VALUES ('Development') ON CONFLICT DO NOTHING");
			stmt.executeUpdate("INSERT INTO departments (NAME) VALUES ('Quality Assurance') ON CONFLICT DO NOTHING");
			stmt.executeUpdate("INSERT INTO departments (NAME) VALUES ('Support') ON CONFLICT DO NOTHING");

			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS users (ID SERIAL PRIMARY KEY, FULLNAME TEXT NOT NULL, USERNAME TEXT UNIQUE NOT NULL, PASSWORD TEXT NOT NULL, DEPARTMENT INTEGER REFERENCES departments(ID))");
			stmt.executeUpdate("INSERT INTO users (FULLNAME, USERNAME, PASSWORD, DEPARTMENT) VALUES ('Bryson Sherman', 'bryson.sherman', '123456', 1) ON CONFLICT DO NOTHING");
			stmt.executeUpdate("INSERT INTO users (FULLNAME, USERNAME, PASSWORD, DEPARTMENT) VALUES ('Bishoy Mikhael', 'bishoy.mikhael', '123456', 2) ON CONFLICT DO NOTHING");
			stmt.executeUpdate("INSERT INTO users (FULLNAME, USERNAME, PASSWORD, DEPARTMENT) VALUES ('Agaby Azer', 'agaby.azer', '123456', 2) ON CONFLICT DO NOTHING");

			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS managers (ID SERIAL PRIMARY KEY, DEPARTMENT_ID INTEGER REFERENCES departments(ID), MANAGER_ID INTEGER REFERENCES users(ID))");
			stmt.executeUpdate("INSERT INTO managers (DEPARTMENT_ID, MANAGER_ID) VALUES (2, 3) ON CONFLICT DO NOTHING");
			stmt.executeUpdate("INSERT INTO managers (DEPARTMENT_ID, MANAGER_ID) VALUES (1, 1) ON CONFLICT DO NOTHING");
			
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pto_balances (ID SERIAL PRIMARY KEY, PTO_BALANCE INTEGER NOT NULL, EMPLOYEE_ID INTEGER REFERENCES users(ID))");
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS pto_requests (ID SERIAL PRIMARY KEY, HOURS INTEGER NOT NULL, EMPLOYEE INTEGER REFERENCES users(ID) NOT NULL, REQUESTDATE DATE NOT NULL, STATUS TEXT, REASON TEXT)");

			ResultSet rs = stmt.executeQuery("SELECT * FROM users");
			
			stmt = connection.createStatement();
			stmt.executeUpdate("CREATE TABLE IF NOT EXISTS timesheet (ID SERIAL, USER_ID INTEGER NOT NULL REFERENCES users(ID), ACTION TEXT NOT NULL, TIMESTAMP timestamp NOT NULL, PRIMARY KEY (ID,USER_ID))");
			
			ResultSet rss = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='bishoy.mikhael'");
			rss.next();
			String UserID = rss.getObject("ID").toString();

			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Clock_In', '2018-12-02 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Lunch_Start', '2018-12-02 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Lunch_End', '2018-12-02 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Clock_Out', '2018-12-02 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Clock_In', '2018-12-01 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Lunch_Start', '2018-12-01 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Lunch_End', '2018-12-01 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('1', 'Clock_Out', '2018-12-01 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");

			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('2', 'Clock_In', '2018-12-02 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('2', 'Lunch_Start', '2018-12-02 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('2', 'Lunch_End', '2018-12-02 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('2', 'Clock_Out', '2018-12-02 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");

			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('3', 'Clock_In', '2018-12-02 07:00:38.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('3', 'Lunch_Start', '2018-12-02 11:15:28.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('3', 'Lunch_End', '2018-12-02 11:45:47.1234' AT TIME ZONE 'America/Los_Angeles')");
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('3', 'Clock_Out', '2018-12-02 15:30:42.1234' AT TIME ZONE 'America/Los_Angeles')");
			
			ArrayList<String> output = new ArrayList<String>();
			while (rs.next()) {
				output.add("Read from DB: " + rs.getObject("FULLNAME") + "<br>" + rs.getObject("USERNAME") + " / " + rs.getObject("PASSWORD"));
			}

			System.out.println("Demo Complete!");

			model.put("records", output);
			return "db";
		} catch (Exception e) {
			model.put("message", e.getMessage());
			return "error";
		}
	}
}
