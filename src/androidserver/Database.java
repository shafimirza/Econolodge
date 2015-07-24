/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package androidserver;
import com.devdaily.opensource.database.DDConnectionBroker;
import java.util.regex.*;
import java.sql.*;
import java.net.*;
import java.util.*;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

//import java.awt.*;
//import javax.swing.*;
public class Database {

    public boolean DEBUG = false;
    public static final int MYSQL = 0;
    public static final int EMBEDDED_DERBY = 1;
    public static final int NETWORK_DERBY = 2;
    int mode = 0;//default to mysql
    DDConnectionBroker broker;
    Connection connection;
    Statement statement;
    // regex used to strip non numeric characters
    Matcher regexMatcher;
    Pattern numericRegex = Pattern.compile("[^0-9.-]", 0);
    String database = "androidhotel";
    String host = "127.0.0.1";
    String port = "3306";
    String user = "androidhotel";
    String password = "hotel";
    String mysqldriver = "com.mysql.jdbc.Driver";
    String embeddedderbydriver = "org.apache.derby.jdbc.EmbeddedDriver";
    String networkderbydriver = "org.apache.derby.jdbc.ClientDriver";
    String driver;
    String url;
    int minConnections = 1;
    int maxConnections = 2;
    long timeout = 100;
    long leaseTime = 60000;
    String logFile = "/usr/AndroidServer/AndroidServer/logs/DDConnectionBroker.log";
    String datalogFile = "/usr/AndroidServer/AndroidServer/dist/JPropertyDatalog.log";
    Logger logger;
    FileHandler fh;

    public Database(String host, String port, String database, String user, String password) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.user = user;
        this.password = password;
        logger = Logger.getLogger("AndroidHotelDataLog");
        this.writeLog();
        setDatabaseBroker();
    }
    public Database(String host, String port, String database, int mode) {
        this.host = host;
        this.port = port;
        this.database = database;
        this.mode = mode;
        setDatabaseBroker();
    }

    public Database() {
        setDatabaseBroker();
    }
    public void setDEBUG(boolean DEBUG) {
        this.DEBUG = DEBUG;
    }
    public void setDatabaseBroker() {
        if (mode == MYSQL) {
            driver = new String(mysqldriver);
            url = new String("jdbc:mysql://" + host + ":" + port + "/" + database + "?user=" + user + "&password=" + password);
        } else if (mode == EMBEDDED_DERBY) {
            driver = new String(embeddedderbydriver);
            url = new String("jdbc:derby:" + database + ";create=true");
        } else if (mode == NETWORK_DERBY) {
            driver = new String(networkderbydriver);
            url = new String("jdbc:derby://" + host + ":" + port + "/" + database + ";create=true");
        }
//        logger.info(url);
        System.out.println(url);

        try {
            if (mode == MYSQL) {
                broker = new DDConnectionBroker(driver, url, user, password, minConnections, maxConnections, timeout, leaseTime, logFile);
            } else {
                Class.forName(driver);
                connection = DriverManager.getConnection(url);
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            logger.info(e.getMessage());
        }
    }
    // This will load a set of data into a Vector
    public Vector<String> fetchColumn(String command) {
        if (DEBUG) {
            System.out.println("fetchColumn:" + command);
            logger.info("fetchColumn:" + command);

        }
        Vector<String> data = new Vector<String>();

        try {
            ResultSet rset = execute(command);
            while (rset.next()) {
                data.addElement(rset.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("SQL Error");
            logger.severe("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    // This will load a set of data into a Vector[Vector]
    public Vector<Vector> fetchVector(String command) {
        int i, numrows = 0;

        Vector<Vector> data = new Vector<Vector>();
        if (DEBUG) {
             System.out.println("fetchVector:" + command);
            logger.info("fetchVector:" + command);

        }
        try {
            ResultSet rset = execute(command);
            if (rset == null) {
                return null;
            }
            ResultSetMetaData rsmd = rset.getMetaData();
            int numCols = rsmd.getColumnCount();

            while (rset.next()) {
                String field = new String();
                Vector<String> indata = new Vector<String>();
                for (i = 1; i <= numCols; i++) {
                    field = rset.getString(i);
                    if (field != null) {
                        field.trim();
                    }
                    indata.addElement(field);
                }
                data.addElement(indata);
                numrows++;
            }
            if (numrows == 0) {
                return null;
            }

        } catch (SQLException e) {
            System.err.println("SQL Error");
            logger.severe("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    // This will load a set of data into a Vector[HashMap]
    public Vector<HashMap> fetchVectorHashMap(String command) {

        int i;

        Vector<HashMap> data = new Vector<HashMap>();
        if (DEBUG) {
            System.out.println("fetchVectorHashMap:" + command);
            logger.info("fetchVectorHashMap:" + command);
        }
        try {
            ResultSet rset = execute(command);
            if (rset == null) {
                return null;
            }
            ResultSetMetaData rsmd = rset.getMetaData();
            int numCols = rsmd.getColumnCount();
            while (rset.next()) {
                String field = new String();
                HashMap<String, String> indata = new HashMap<String, String>();
                for (i = 1; i <= numCols; i++) {
                    field = rset.getString(i);
                    if (field != null) {
                        field.trim();
                    }
                    indata.put(rsmd.getColumnName(i), field);  // columnName->Value
                }
                data.addElement(indata);
            }
        } catch (SQLException e) {
            System.err.println("SQL Error");
            logger.severe("SQL Error: " + e.getMessage());
            e.printStackTrace();
        }

        return data;
    }

    public boolean updateHashMap(String start, HashMap data, String end) {

        boolean firsttime = true;

        String query = start;

        Set keys = data.keySet();
        Iterator iter = keys.iterator();
        while (iter.hasNext()) {
            Object key = iter.next();
            Object value = data.get(key);

            if (!firsttime) {
                query += ",";
            } else {
                firsttime = false;
            }
            query += key + " = " + "'" + value + "'";
        }

        query += end;
        if (executeUpdate(query) != 0) {
            return true;
        } else {
            return false;
        }
    }

    public String selectString(String query) {
        if (DEBUG) {
            System.out.println("selectString:" + query);
            logger.info("selectString:" + query);

        }

        String itemValue = "";
        ResultSet rs = null;
        try {
            if (mode == MYSQL) {
                connection = broker.getConnection();
            }

            statement = connection.createStatement();
            rs = statement.executeQuery(query);
            if (rs.next()) {
                itemValue = trim(rs.getString(1));
            }
        } catch (SQLException e) {
            System.err.println("[database communication error #" + e.getErrorCode() + "]");
             System.err.println(query);
            logger.severe("[database communication error #" + e.getErrorCode() + "]"+"\n          "+query);


            return null;
        } finally {
            try {
                if (mode == MYSQL) {
                    broker.freeConnection(connection);
                }
            } catch (Exception e) {
                System.err.println("Threw an exception trying to free my Connection: "
                + e.getMessage());
                logger.severe("Threw an exception trying to free my Connection: "
                        + e.getMessage());

            }
        }
        return itemValue;
    }

    public String selectString(String table, String column, String where) {

        if (where.equals("")) {
            where = " 1=1";
        }

        String query = "SELECT " + column + " FROM " + table + " WHERE " + where;
        return selectString(query);
    }

    public ResultSet execute(String query) {

        if (DEBUG) {
             System.out.println("execute:" + query);
            logger.info("execute:" + query);

        }


        ResultSet rs = null;
        try {
            if (mode == MYSQL) {
                connection = broker.getConnection();
            }
            statement = connection.createStatement();
            rs = statement.executeQuery(query);
        } catch (SQLException e) {
             System.err.println("[database communication error #" + e.getErrorCode() + "]");
             System.err.println(query);
            logger.severe("[database communication error #" + e.getErrorCode() + "]"+"\n"+query);

            return null;
        } finally {
            try {
                if (mode == MYSQL) {
                    broker.freeConnection(connection);
                }
            } catch (Exception e) {
                 System.err.println("Threw an exception trying to free my Connection: "
                  + e.getMessage());
                logger.info("Threw an exception trying to free my Connection: "
                        + e.getMessage());

            }
        }
        return rs;
    }

    public int executeUpdate(String query) {

        if (DEBUG) {
             System.out.println("executeUpdate:" + query);
            logger.info("executeUpdate:" + query);

        }

        int result = 0;

        try {
            if (mode == MYSQL) {
                connection = broker.getConnection();
            }
            statement = connection.createStatement();
            result = statement.executeUpdate(query);
        } catch (SQLException e) {
            System.err.println("[database communication error]");
             System.err.println(query);
            logger.severe("[database communication error]"+"\n +e.getErrorCode()"+query);


        } finally {
            try {
                if (mode == MYSQL) {
                    broker.freeConnection(connection);
                }
            } catch (Exception e) {
                 System.err.println("Threw an exception trying to free my Connection: "
                       + e.getMessage());
                logger.severe("Threw an exception trying to free my Connection: "
                        + e.getMessage());


            }
        }
        return result;
    }

    public String getLastInsertID() {
        String idString = "0";
        try {
            ResultSet rset = execute("SELECT LAST_INSERT_ID()");
            if (rset.next()) {
                idString = rset.getString(1);
            }
        } catch (SQLException e) {
            System.err.println("[can not get insert id]");
            logger.severe("[can not get insert id]");

        }
        return idString;
    }

    public boolean logEntry(String user_id, int code, String title, String notes, String related_id, String date) {
        String query = new String("INSERT INTO log (user_id, code, title, notes, related_id, date) VALUES ('" + user_id + "','" + code + "','" + title + "','" + notes + "','" + related_id + "','" + date + "')");

        if (executeUpdate(query) != 0) {
            return true;
        } else {
            return false;
        }
    }

    public String trim(String text) {
        if (text == null) {
            return "";
        } else {
            return text.trim();
        }
    }

    public String formatString(char inchars[]) {
        return new String(inchars);
    }

    // make a database-safe string
    public String formatString(String instring) {
        instring = instring.replaceAll("\'", "\\\'\\\'");   // single quote
        instring = instring.replace('\b', ' ');             // backspace
        instring = instring.replace('\t', ' ');             // tab
        instring = instring.replace('\n', ' ');             // newline
        instring = instring.replace('\f', ' ');             // formfeed
        instring = instring.replace('\r', ' ');             // return
        instring = instring.replace('\\', '/');             // backslash
        instring = instring.replaceAll("%", "\\%");         // percent
        instring = instring.replaceAll("_", "\\_");         // underscore

        return instring;
    }

    // make a database-safe price
    public String formatPrice(String instring) {
        regexMatcher = numericRegex.matcher(instring);
        String tmp = regexMatcher.replaceAll("");
        if (tmp.equals("")) {
            tmp = "0";
        }
        return tmp;
    }

    // make a database-safe number
    public String formatNumber(String instring) {
        regexMatcher = numericRegex.matcher(instring);
        return regexMatcher.replaceAll("");
    }

    public void writeLog() {
        try {
            // This block configure the logger with handler and formatter
            fh = new FileHandler(datalogFile);
            logger.addHandler(fh);
            logger.setUseParentHandlers(false);
            //logger.setLevel(Level.ALL);
            SimpleFormatter formatter = new SimpleFormatter();
            fh.setFormatter(formatter);
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public int executeUpdate(String query, String uery) {
        if (DEBUG) {
             System.out.println("executeUpdate:" + query+ uery);
            logger.info("executeUpdate:" + query+ uery);

        }

        int result = 0;

        try {
            if (mode == MYSQL) {
                connection = broker.getConnection();
            }
            statement = connection.createStatement();
            result = statement.executeUpdate(query+ uery);
        } catch (SQLException e) {
            System.err.println("[database communication error]");
             System.err.println(query);
            logger.severe("[database communication error]"+"\n+e.getErrorCode() + \"]\""+query+ uery);


        } finally {
            try {
                if (mode == MYSQL) {
                    broker.freeConnection(connection);
                }
            } catch (Exception e) {
                 System.err.println("Threw an exception trying to free my Connection: "
                       + e.getMessage());
                logger.severe("Threw an exception trying to free my Connection: "
                        + e.getMessage());


            }
        }
        return result;
    }}
