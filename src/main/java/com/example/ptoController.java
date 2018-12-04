
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
import com.example.request;

@Controller
public class ptoController {
    
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

	@RequestMapping(value = "/request", method = RequestMethod.GET)
	String requestPTO(Map<String, Object> model, HttpServletRequest request) {
		if (!request.getSession().isNew())
		{
			Enumeration attributes = request.getSession().getAttributeNames();
			while(attributes.hasMoreElements())
			{
				String ele = attributes.nextElement().toString();
				model.put(ele, request.getSession().getAttribute(ele));
			}

			// load requests
			try (Connection connection = database.getDataSource().getConnection()) {
				
				Statement stmt = connection.createStatement();
				ResultSet rrs = stmt.executeQuery("SELECT * FROM users WHERE USERNAME='" + getAttributeFromSession(request, "user") + "'");
				rrs.next();
				String UserID = rrs.getObject("ID").toString();
				String DeptId = rrs.getString("DEPARTMENT");
				ResultSet rs = stmt.executeQuery("SELECT * FROM pto_requests WHERE EMPLOYEE='" + UserID + "' ORDER BY ID desc");

				ArrayList<request> output = new ArrayList<request>();
				while (rs.next()) {
					// output.add( rs.getObject("REASON") + " - " + rs.getObject("REQUESTDATE") + " - " + rs.getObject("HOURS") + " - " + rs.getObject("STATUS") );
					request r = new request();
					r.Date = rs.getObject("REQUESTDATE").toString();
					r.Hours = rs.getObject("HOURS").toString();
					r.Reason = rs.getObject("REASON").toString();
					r.Status = rs.getObject("STATUS").toString();
					output.add(r);
				}
				model.put("requests", output.toArray());

				if (database.isManage(UserID))
				{
					System.out.println("Manager!");
					model.put("manager", "1");

					ArrayList<request> allPendingRequests = new ArrayList<request>();

					for (String employee : database.getEmployeesByDepartmentId(DeptId, UserID))
					{
						String empName = database.getFromUserById(employee, "FULLNAME");

						for (request r : database.getPendingRequestsByUserId(employee))
						{
							r.Name = empName;
							allPendingRequests.add(r);
						}
					}
					model.put("pendingRequests", allPendingRequests);
				}
				else
				{
					model.put("manager", "0");
				}

			} catch (Exception e) {
				model.put("message", e.getMessage());
				System.out.println("ERROR loading PTO! - " + e.getMessage());
			}
			
			return "request";
		}
		else
		{
			return "redirect:/";
		}
	}
    
	@RequestMapping(value = "/request", method = RequestMethod.POST)
	@ResponseBody
	String request(HttpServletRequest request) {
        // String Username, String Reason, String date, String hours

        if (database.addRequestForUser(getAttributeFromSession(request, "user"), request.getParameter("pto_reason"), request.getParameter("date"), request.getParameter("inputHours") ))
        {
            System.out.println("Request - OK");
            return "Request successfully submitted!";
        }
        else
        {
            System.out.println("Request - FAILED!");
            return "Request failed!";
        }
	}

	@RequestMapping(value = "/requestManager", method = RequestMethod.POST)
	@ResponseBody
	String requestManager(HttpServletRequest request) {

		switch (request.getParameter("status").toLowerCase())
		{
			case "approve":
				database.approveRequestById(request.getParameter("PTO_ID"));
				return "PTO Request Approved!";
			case "deny":
				database.denyRequestById(request.getParameter("PTO_ID"));
				return "PTO Request Approved!";
			default:
				return "";
		}
	}

}
