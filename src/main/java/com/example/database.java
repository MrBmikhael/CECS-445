
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
import java.util.Dictionary;
import java.util.HashMap;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Map;

import com.example.day;
import com.example.request;

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
	
	public static ArrayList<day> getWeekTimesheet(String Username) {
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + Username + "'");
			rs.next();
			String UserID = rs.getObject("ID").toString();
			
			ArrayList<day> weekData = new ArrayList<day>();
			
			System.out.println("Getting last week timesheet ...");
			
			// AND TIMESTAMP BETWEEN (CURRENT_DATE - interval '7 day') AND CURRENT_DATE
			ResultSet rss = stmt.executeQuery("SELECT * FROM timesheet WHERE USER_ID='" + UserID + "' AND TIMESTAMP BETWEEN (CURRENT_DATE - interval '15 day') AND CURRENT_DATE ORDER BY TIMESTAMP desc;");
			
			SimpleDateFormat format = new SimpleDateFormat("HH:mm");
			
			while (!rss.isLast()) {
				day d = new day();
				
				rss.next();
				d.Clock_Out = rss.getString("TIMESTAMP").split(" ")[1];
				
				rss.next();
				d.Lunch_End = rss.getString("TIMESTAMP").split(" ")[1];
				
				rss.next();
				d.Lunch_Start = rss.getString("TIMESTAMP").split(" ")[1];
				
				rss.next();
				d.Clock_In = rss.getString("TIMESTAMP").split(" ")[1];
				
				d.Date = rss.getString("TIMESTAMP").split(" ")[0];

				Date cin = format.parse(d.Clock_In);
				Date cout = format.parse(d.Clock_Out);
				long difference = cout.getTime() - cin.getTime();
				
				Date lunchStart = format.parse(d.Lunch_Start);
				Date lunchEnd = format.parse(d.Lunch_End);
				difference = difference - (lunchEnd.getTime() - lunchStart.getTime());
				
				d.Total_Hours = String.format("%02d:%02d", TimeUnit.MILLISECONDS.toHours(difference),
				  TimeUnit.MILLISECONDS.toMinutes(difference) - TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(difference)));

				weekData.add(d);

				System.out.println("DAY DATA: Clock IN: " + d.Clock_In + " - Lunch Start: " + d.Lunch_Start + " - Lunch End: " 
				  + d.Lunch_End + " - Clock Out: " + d.Clock_Out + " - Hours Worked: " + d.Total_Hours);
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
	
	public static boolean addRequestForUser(String Username, String Reason, String date, String hours) {
		// ID SERIAL PRIMARY KEY, HOURS INTEGER NOT NULL, EMPLOYEE INTEGER REFERENCES users(ID) NOT NULL, REQUESTDATE DATE NOT NULL, STATUS TEXT
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + Username + "'");
			rs.next();
			String UserID = rs.getObject("ID").toString();
			System.out.println("INSERT INTO pto_requests (HOURS, EMPLOYEE, REQUESTDATE, STATUS, REASON) VALUES (" + hours + ", '" + UserID + "', '" + date + "', 'Pending', '"+Reason+"')");
			stmt.executeUpdate("INSERT INTO pto_requests (HOURS, EMPLOYEE, REQUESTDATE, STATUS, REASON) VALUES (" + hours + ", '" + UserID + "', '" + date + "', 'Pending', '"+Reason+"')");
			
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
				stmt.executeQuery("SET timezone='America/Los_Angeles';");
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
				stmt.executeQuery("SET timezone='America/Los_Angeles';");
			}
			catch (Exception e) {}
			return ds;
		}
	}

	public static boolean approveRequestById(String requestId)
	{
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("UPDATE pto_requests SET STATUS='Approved' WHERE ID='" + requestId + "'");
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public static boolean denyRequestById(String requestId)
	{
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			stmt.executeUpdate("UPDATE pto_requests SET STATUS='Denied' WHERE ID='" + requestId + "'");
		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public static boolean isManage(String userId)
	{
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM MANAGERS WHERE MANAGER_ID='" + userId + "'");
			
			while (rs.next())
				return true;

		} catch (Exception e) {
			return false;
		}
		return false;
	}

	public static ArrayList<String> getEmployeesByDepartmentId(String deptId, String ManagerId)
	{
		ArrayList<String> Employees = new ArrayList<String>();

		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE DEPARTMENT='" + deptId + "'");
			
			while (rs.next())
			{
				if (!rs.getString("ID").equals(ManagerId))
				{
					Employees.add(rs.getString("ID"));
				}
			}
				

		} catch (Exception e) {
			System.out.println("Error - " + e.getMessage());
		}
		
		return Employees;
	}

	public static ArrayList<request> getPendingRequestsByUserId(String UserId)
	{
		ArrayList<request> req = new ArrayList<request>();

		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM pto_requests WHERE EMPLOYEE='" + UserId + "' AND STATUS='Pending'");
			
			while (rs.next())
			{
				request r = new request();
				r.Date = rs.getObject("REQUESTDATE").toString();
				r.Hours = rs.getObject("HOURS").toString();
				r.Reason = rs.getObject("REASON").toString();
				r.Status = rs.getObject("STATUS").toString();
				r.ID = rs.getObject("ID").toString();
				req.add(r);
			}
				

		} catch (Exception e) {
			System.out.println("Error - " + e.getMessage());
		}

		return req;
	}

	public static String getFromUserById(String UserId, String keyword)
	{
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE ID='" + UserId + "'");
			rs.next();

			return rs.getString(keyword);
			
		} catch (Exception e) {
			return "";
		}
	}

	public static String getDepartmentNameById(String Id)
	{
		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM departments WHERE ID='" + Id + "'");
			rs.next();
			return rs.getString("NAME");

		} catch (Exception e) {
			System.out.println("Error - " + e.getMessage());
		}
		return "";
	}

	public static Map<String, String> getProfileById(String UserId)
	{
		Map<String, String> ProfileData = new HashMap<String, String>();

		try (Connection connection = database.getDataSource().getConnection()) {
			Statement stmt = connection.createStatement();
			ResultSet rs = stmt.executeQuery("SELECT * FROM users WHERE ID='" + UserId + "'");
			rs.next();

			ProfileData.put("FULLNAME", rs.getString("FULLNAME"));
			ProfileData.put("USERNAME", rs.getString("USERNAME"));
			ProfileData.put("DEPARTMENT", getDepartmentNameById(rs.getString("DEPARTMENT")));
			ProfileData.put("PTO", "40");
			
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}

		return ProfileData;
	}
}
