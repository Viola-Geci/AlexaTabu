package com.amazon.customskill;

import java.sql.*;

public class DBConnect {
	
	static String DBName = "Tabu.db";
	private static Connection con = null;
	
public static void main(String[] args) {
		Connection con = null;
	}
	
	public static Connection getConnection() {
		
		try {
			Class.forName("org.sqlite.JDBC");
			try {
				con = DriverManager.getConnection("jdbc:sqlite::resource:" + 
			               DBConnect.class.getClassLoader().getResource("Tabu.db"));
		        } catch (SQLException ex) {
			System.out.println("Open Database");
			ex.printStackTrace();
		}	
	} catch (ClassNotFoundException ex) {
		ex.printStackTrace();
	} return con;
	}
}
	


