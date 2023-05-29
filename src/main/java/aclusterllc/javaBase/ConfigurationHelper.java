package aclusterllc.javaBase;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

public class ConfigurationHelper {
    public static final Properties configIni = new Properties();
    static Logger logger = LoggerFactory.getLogger(ConfigurationHelper.class);
    private static HikariDataSource hikariDataSource;
    public static final JSONObject dbBasicInfo = new JSONObject();
    public static final JSONObject countersCurrentValue = new JSONObject();
    public static final JSONObject motorsCurrentSpeed = new JSONObject();
    public static final Map<Integer, Integer> apeClientConnectionStatus  = new HashMap<>();
    public static final JSONObject systemConstants = new JSONObject();





    public static void loadConfig(){
     //loading file config
        try {
            configIni.load(new FileInputStream("./resources/config.ini"));
        }
        catch (IOException e)
        {
            logger.info("File Config Read failed"+e);
            System.exit(0);
        }
        setSystemConstants();
        createDatabaseConnection();
        try {
            Connection connection=getConnection();
            Statement stmt = connection.createStatement();
            String query = "SELECT * FROM alarms";
            ResultSet rs = stmt.executeQuery(query);
            ResultSetMetaData rsMetaData = rs.getMetaData();
            int numColumns = rsMetaData.getColumnCount();
            JSONObject results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("alarm_id")+"_"+rs.getString("alarm_type"),item);
            }
            dbBasicInfo.put("alarms",results);

            query = "SELECT * FROM bins";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("bin_id"),item);
            }
            dbBasicInfo.put("bins",results);

            query = "SELECT * FROM boards";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("board_id"),item);
            }
            dbBasicInfo.put("boards",results);

            query = "SELECT * FROM board_ios";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("id"),item);
            }
            dbBasicInfo.put("board_ios",results);

            query = "SELECT * FROM conveyors";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("conveyor_id"),item);
            }
            dbBasicInfo.put("conveyors",results);

            query = "SELECT * FROM devices";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("device_id"),item);
            }
            dbBasicInfo.put("devices",results);

            query = "SELECT * FROM inputs";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("input_id"),item);
            }
            dbBasicInfo.put("inputs",results);

            query = "SELECT * FROM machines";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id"),item);
                for(int i=0;i<32;i++){
                    countersCurrentValue.put(rs.getString("machine_id")+"_"+(i+1),0);
                }
            }
            dbBasicInfo.put("machines",results);

            query = "SELECT * FROM motors";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("motor_id"),item);
                motorsCurrentSpeed.put(rs.getString("machine_id")+"_"+rs.getString("motor_id"),0);
            }
            dbBasicInfo.put("motors",results);

            query = "SELECT * FROM parameters";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("machine_id")+"_"+rs.getString("param_id"),item);
            }
            dbBasicInfo.put("parameters",results);

            query = "SELECT * FROM scs";
            rs = stmt.executeQuery(query);
            rsMetaData = rs.getMetaData();
            numColumns = rsMetaData.getColumnCount();
            results = new JSONObject();
            while (rs.next())
            {
                JSONObject item=new JSONObject();
                for (int i=1; i<=numColumns; i++) {
                    String column_name = rsMetaData.getColumnName(i);
                    item.put(column_name,rs.getString(column_name));
                }
                results.put(rs.getString("value"),item);
            }
            dbBasicInfo.put("scs",results);

        }
        catch (SQLException e) {
            logger.error("[Database] Failed To Connect with database.Closing Java Program."+CommonHelper.getStackTraceString(e));
            System.exit(0);
        }
        catch (Exception ex) {
            logger.error("[Database] Failed To get Data from database.Closing Java Program."+CommonHelper.getStackTraceString(ex));
            System.exit(0);
        }
    }
    public static void setSystemConstants(){
        JSONObject APE_MESSAGE_ID_NAME  = new JSONObject();

        APE_MESSAGE_ID_NAME.put("1", "System State Message");
        APE_MESSAGE_ID_NAME.put("2", "Inputs Message");
        APE_MESSAGE_ID_NAME.put("3", "Input Change Message");
        APE_MESSAGE_ID_NAME.put("4", "Errors Message");
        APE_MESSAGE_ID_NAME.put("5", "Jams Message");
        APE_MESSAGE_ID_NAME.put("6", "ConfirmationPEBlocked Message");
        APE_MESSAGE_ID_NAME.put("7", "ConfirmationPEBlocked change Message");
        APE_MESSAGE_ID_NAME.put("8", "BinPartiallyFull Message");
        APE_MESSAGE_ID_NAME.put("9", "BinPartiallyFull change Message");
        APE_MESSAGE_ID_NAME.put("10", "BinFull Message");
        APE_MESSAGE_ID_NAME.put("11", "BinFull change Message");
        APE_MESSAGE_ID_NAME.put("12", "BinDisabled Message");
        APE_MESSAGE_ID_NAME.put("13", "BinDisabled change Message");
        APE_MESSAGE_ID_NAME.put("14", "Devices Connected Message");
        APE_MESSAGE_ID_NAME.put("15", "Device Connected Change Message");
        APE_MESSAGE_ID_NAME.put("16", "Sync Response");
        APE_MESSAGE_ID_NAME.put("17", "TrayMissing Message");
        APE_MESSAGE_ID_NAME.put("18", "TrayMissing change Message");
        APE_MESSAGE_ID_NAME.put("20", "Dimension Message");
        APE_MESSAGE_ID_NAME.put("21", "Barcode Result");
        APE_MESSAGE_ID_NAME.put("22", "ConfirmDestination Message");
        APE_MESSAGE_ID_NAME.put("30", "Ping Response");
        APE_MESSAGE_ID_NAME.put("40", "BinMode Message");
        APE_MESSAGE_ID_NAME.put("41", "BinModechange Message");
        APE_MESSAGE_ID_NAME.put("42", "ConveyorState Message");
        APE_MESSAGE_ID_NAME.put("43", "ConveyorStateChange Message");
        APE_MESSAGE_ID_NAME.put("44", "SensorHit Message");
        APE_MESSAGE_ID_NAME.put("45", "Event Message");
        APE_MESSAGE_ID_NAME.put("46", "InductLineState Message");
        APE_MESSAGE_ID_NAME.put("47", "InductLineStateChange Message");
        APE_MESSAGE_ID_NAME.put("48", "PieceInducted Message");
        APE_MESSAGE_ID_NAME.put("49", "Motor Speed Message");
        APE_MESSAGE_ID_NAME.put("50", "EStop Message");
        APE_MESSAGE_ID_NAME.put("53", "Outputs Message");
        APE_MESSAGE_ID_NAME.put("54", "Param Value Message");
        APE_MESSAGE_ID_NAME.put("120", "SetMode Message");
        APE_MESSAGE_ID_NAME.put("101", "Request Inputs State Message");
        APE_MESSAGE_ID_NAME.put("102", "Request Errors Message");
        APE_MESSAGE_ID_NAME.put("103", "Request Jams Message");
        APE_MESSAGE_ID_NAME.put("105", "Request ConfirmationPEBlocked Message");
        APE_MESSAGE_ID_NAME.put("106", "Request BinPartiallyFull Message");
        APE_MESSAGE_ID_NAME.put("107", "Request BinFull Message");
        APE_MESSAGE_ID_NAME.put("108", "Request BinDisabled Message");
        APE_MESSAGE_ID_NAME.put("109", "Request DevicesConnected State Message");
        APE_MESSAGE_ID_NAME.put("110", "Request BinMode Message");
        APE_MESSAGE_ID_NAME.put("111", "SetBinMode Message");
        APE_MESSAGE_ID_NAME.put("112", "Request ConveyorState Message");
        APE_MESSAGE_ID_NAME.put("113", "Request TrayMissing Message");
        APE_MESSAGE_ID_NAME.put("114", "Request InductLineState Message");
        APE_MESSAGE_ID_NAME.put("116", "Sync Request");
        APE_MESSAGE_ID_NAME.put("130", "Ping Request");
        systemConstants.put("APE_MESSAGE_ID_NAME",APE_MESSAGE_ID_NAME);

        JSONObject SYSTEM_STATES  = new JSONObject();
        SYSTEM_STATES.put("0", "Not Ready");
        SYSTEM_STATES.put("1", "Ready");
        SYSTEM_STATES.put("2", "Starting");
        SYSTEM_STATES.put("3", "Running");
        SYSTEM_STATES.put("4", "Stopping");
        systemConstants.put("SYSTEM_STATES",SYSTEM_STATES);

        JSONObject SYSTEM_MODES  = new JSONObject();
        SYSTEM_MODES.put("0", "Auto");
        SYSTEM_MODES.put("1", "Manual");
        systemConstants.put("SYSTEM_MODES",SYSTEM_MODES);
    }
    public static void createDatabaseConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        }
        catch (ClassNotFoundException e) {
            logger.info("[Database] Mysql Driver Not Found.Closing Java Program.");
            System.exit(0);
        }
        HikariConfig config = new HikariConfig();
        String jdbcUrl = String.format("jdbc:mysql://%s:3306/%s" +
                        "?allowPublicKeyRetrieval=true" +
                        "&useSSL=false" +
                        "&useUnicode=true" +
                        "&characterEncoding=utf8" +
                        "&allowMultiQueries=true",
                configIni.getProperty("db_host"),
                configIni.getProperty("db_name"));

        config.setJdbcUrl(jdbcUrl);
        config.setUsername(configIni.getProperty("db_username"));
        config.setPassword(configIni.getProperty("db_password"));
        config.setMaximumPoolSize(30);
        config.setConnectionTimeout(300000);
        config.setLeakDetectionThreshold(300000);
        config.addDataSourceProperty( "cachePrepStmts" , "true" );
        config.addDataSourceProperty( "prepStmtCacheSize" , "250" );
        config.addDataSourceProperty( "prepStmtCacheSqlLimit" , "2048" );
        try{
            hikariDataSource = new HikariDataSource(config);
            logger.info("[Database] Connected.");
        }
        catch (Exception ex){
            logger.error("[Database] Connection Failed.Closing Java Program.");
            System.exit(0);
        }
    }
    public static Connection getConnection() throws SQLException {
        return hikariDataSource.getConnection();
    }
}
