
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

import com.example.day;

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
	
	public static day[] getWeekTimesheet(String Username) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + Username + "'");
			rs.next();
			String UserID = rs.getObject("ID").toString();
			
			day[] weekData = new day[7];
			
			System.out.println("Getting last week timesheet ...");
			
			// AND TIMESTAMP BETWEEN (CURRENT_DATE - interval '7 day') AND CURRENT_DATE
			ResultSet rss = stmt.executeQuery("SELECT * FROM timesheet WHERE USER_ID='" + UserID + "' AND TIMESTAMP BETWEEN (CURRENT_DATE - interval '7 day') AND CURRENT_DATE ORDER BY TIMESTAMP desc;");
			
			SimpleDateFormat format = new SimpleDateFormat("HH:mm");
			
			for (int i = 0; i < 7; i++) {
				weekData[i] = new day();
				
				rss.next();
				weekData[i].Clock_Out = rss.getString("TIMESTAMP").split(" ")[1];
				
				rss.next();
				weekData[i].Lunch_End = rss.getString("TIMESTAMP").split(" ")[1];
				
				rss.next();
				weekData[i].Lunch_Start = rss.getString("TIMESTAMP").split(" ")[1];
				
				rss.next();
				weekData[i].Clock_In = rss.getString("TIMESTAMP").split(" ")[1];
				
				weekData[i].Date = rss.getString("TIMESTAMP").split(" ")[0];

				Date cin = format.parse(weekData[i].Clock_In);
				Date cout = format.parse(weekData[i].Clock_Out);
				long difference = cout.getTime() - cin.getTime();
				
				Date lunchStart = format.parse(weekData[i].Lunch_Start);
				Date lunchEnd = format.parse(weekData[i].Lunch_End);
				difference = difference - (lunchEnd.getTime() - lunchStart.getTime());
				
				weekData[i].Total_Hours = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toHours(difference),
				  TimeUnit.MILLISECONDS.toMinutes(difference) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference)));

				// System.out.println("DAY DATA: Clock IN: " + weekData[i].Clock_In + " - Lunch Start: " + weekData[i].Lunch_Start + " - Lunch End: " 
				//  + weekData[i].Lunch_End + " - Clock Out: " + weekData[i].Clock_Out + " - Hours Worked: " + weekData[i].Total_Hours);
			}
			
			return weekData;
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			return null;
		}		
	}
	
	public static boolean addRecordForUser(String Username, String Action) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + Username + "'");
			rs.next();
			String UserID = rs.getObject("ID").toString();
			stmt.executeUpdate("INSERT INTO timesheet (USER_ID, ACTION, TIMESTAMP) VALUES ('" + UserID + "', '" + Action + "', now() AT TIME ZONE 'America/Los_Angeles')");
			
			return true;
		} catch (Exception e) {
			System.out.println("ERROR: " + e);
			return false;
		}
	}
	
	public static String getUserID(String username) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();		
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + username + "'");
			rs.next();

			return rs.getString("ID");
		} catch (Exception e) {
			return "";
		}
	}
	
	public static String getUserFullName(String username) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();		
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + username + "'");
			rs.next();

			return rs.getString("FULLNAME");
		} catch (Exception e) {
			return "";
		}
	}
	
	public static boolean checkLogin(String username, String password)
	{
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + username + "'");
			rs.next();

			return (rs.getString("PASSWORD").equals(password));
			
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
			DataSource ds = new HikariDataSource(config);
			try (Connection connection = ds.getConnection()) {
				Statement stmt = connection.createStatement();
				stmt.executeQuery("SET TIME ZONE 'America/Los_Angeles';");
			}
			catch (Exception e) {}
			return ds;
		} else {
			System.out.println("================================================================================");
			System.out.println("Found dbUrl!");
			System.out.println(dbUrl);
			System.out.println("================================================================================");
			HikariConfig config = new HikariConfig();
			config.setJdbcUrl(dbUrl);
			DataSource ds = new HikariDataSource(config);
			try (Connection connection = ds.getConnection()) {
				Statement stmt = connection.createStatement();
				stmt.executeQuery("SET TIME ZONE 'America/Los_Angeles';");
			}
			catch (Exception e) {}
			return ds;
		}
	}
}
