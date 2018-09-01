/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Classes;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Bishoy M
 */
public class DbManager {
    private Connection getConnection()
    {
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection conn = DriverManager.getConnection("jdbc:mysql://10.10.10.4:3306/cecs", "cecs", "cecs");
            return conn;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }
    
    public String getPasswordHash(String Username)
    {
        Connection conn = getConnection();
        
        if (conn == null)
            return null;
        
        String q = "SELECT `password`FROM `users` WHERE `username` = \"" + Username + "\";";
        
        try {
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(q);
            
            if (rs.first())
                return rs.getString("password");
            
        } catch (SQLException ex) {
            Logger.getLogger(DbManager.class.getName()).log(Level.SEVERE, null, ex);
        }

        return null;
    }
}
