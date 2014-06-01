import java.sql.Statement;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

 public class populate {
    public Connection mainConnect;
    
	public static void main(String[] args) throws SQLException, IOException{
	 if (args.length == 0) {
            System.out.print("No Command Line arguments");
        } 
        else {            System.out.print("You provided " + args.length
                    + " arguments");

            for (int i = 0; i < 3; i++) {
                System.out.println("args[" + i + "]: "
               + args[i]);
            
         	populate populateDB = new populate();
		populateDB.dropdb();   
               
         populateDB.populatedb1(args[0]);
        populateDB.populatedb2(args[1]);
       populateDB.populatedb3(args[2]);           
            }}                   
}
public populate(){
      connectdb();
        
    }
    
    private void connectdb() {
        try {
            //load oracle driver
            System.out.println("looking for JDBC-ODBC drive..");
            DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
            System.out.print("Driver found and Loaded.");

            String url = "jdbc:oracle:thin:@localhost:1521:orcl";
            String userId = "SYSTEM";
            String password = "1234";
            System.out.print("Connecting to DB...");
            mainConnect = DriverManager.getConnection(url, userId, password);
            System.out.println("connected !!");
        } catch (Exception e) {
            System.out.println("ERROR" + e);
        }
    }
 
    
    
    private void populatedb3(String firebuildings) throws SQLException, IOException {
        try {
            
            FileInputStream File1 = new FileInputStream(firebuildings);
            DataInputStream Data1 = new DataInputStream(File1);
            BufferedReader read1 = new BufferedReader(new InputStreamReader(Data1));
            String Query = "INSERT INTO firebuildings VALUES(?)";
            mainConnect.setAutoCommit(false);
            PreparedStatement prep1 = mainConnect.prepareStatement(Query);
            String Line3;
               int totalInserts = 0;
                while ((Line3 = read1.readLine()) != null) {
                // System.out.println(Line1); 
                StringTokenizer st = new StringTokenizer(Line3, " ");
                while (st.hasMoreTokens()) {
                    //int count = st.countTokens(); 
                    //System.out.println(count); 

                    String FBname = st.nextToken();
                    System.out.println(FBname);
                    prep1.setString(1, FBname);
                     prep1.addBatch();
                    System.out.println("batch added");
                    totalInserts++;

                }

            }
            System.out.println("Total inserts " + totalInserts);
            prep1.executeBatch();
            System.out.println("batch executed");
            mainConnect.commit();
        }
        
        
        catch (FileNotFoundException ex) {
            Logger.getLogger(populate.class.getName()).log(Level.SEVERE, null, ex);       
        }
   
    }
    

    private void populatedb2(String hydrant) {
        try {
            //String file = "C:\\Users\\sakshi\\Desktop\\HW2 585\\homework2_final\\hydrant.xy";
            FileInputStream File1 = new FileInputStream(hydrant);
            DataInputStream Data1 = new DataInputStream(File1);
            BufferedReader read1 = new BufferedReader(new InputStreamReader(Data1));
            String Line1;
            int totalInserts = 0;
            String Query = "INSERT INTO hydrants VALUES(?,SDO_GEOMETRY(2001,NULL,SDO_POINT_TYPE(?,?,NULL),NULL,NULL))";
            mainConnect.setAutoCommit(false);
            PreparedStatement prep = mainConnect.prepareStatement(Query);
            while ((Line1 = read1.readLine()) != null) {
                // System.out.println(Line1); 
                StringTokenizer st = new StringTokenizer(Line1, ",");
                while (st.hasMoreTokens()) {
                    //int count = st.countTokens(); 
                    //System.out.println(count); 

                    String HID = st.nextToken().trim();
                    String Hx = st.nextToken().trim();
                    String Hy = st.nextToken().trim();

                    prep.setString(1, HID);
                    prep.setInt(2, Integer.parseInt(Hx));
                    prep.setInt(3, Integer.parseInt(Hy));
                    System.out.println("Ready to insert into table  values " + HID + Hx + Hy);
                    prep.addBatch();
                    System.out.println("batch added");
                    totalInserts++;

                }

            }
            System.out.println("Total inserts " + totalInserts);
            prep.executeBatch();
            System.out.println("batch executed");
            mainConnect.commit();
        }
        
        catch (SQLException e) {
            e.printStackTrace();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(populate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(populate.class.getName()).log(Level.SEVERE, null, ex);
        } finally {
            //mainConnect.setAutoCommit(true);
        }
    }

    private void populatedb1(String building) {
        try {
           // String file = "C:\\Users\\sakshi\\Desktop\\HW2 585\\homework2_final\\building.xy";
            FileInputStream File2 = new FileInputStream(building);
            DataInputStream Data2 = new DataInputStream(File2);
            BufferedReader read1 = new BufferedReader(new InputStreamReader(Data2));
            String Line2;
            int totalInserts = 0;
            while ((Line2 = read1.readLine()) != null) {
                System.out.println(Line2);
                String[] str = Line2.split(",");
                //System.out.println(str.length) ;

                String BID = str[0].trim();
                String Bname = str[1].trim();
                String Bcoord = str[2].trim();
                int B = Integer.parseInt(Bcoord);
                int count = B * 2;

                String A = "?";
                for (int i = 1; i < count; i++) {

                    A += ", ?";
                }
                String Query = "INSERT INTO buildings VALUES(?,?,SDO_GEOMETRY(2003,NULL, NULL,SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY(" + A + ")))";
                System.out.println(Query);
                mainConnect.setAutoCommit(false);
                PreparedStatement prep = mainConnect.prepareStatement(Query);

                if (BID != null || Bname != null) {
                    prep.setString(1, BID);
                    prep.setString(2, Bname);

                    System.out.println(BID + ".." + Bname);

                } else {

                    throw new NullPointerException();
                }

                for (int i = 3; i < count + 3; i++) {
                    int no = Integer.parseInt(str[i].trim());
                    System.out.println(i + ".." + no);
                    prep.setInt(i, no);

                }

                //System.out.println("Ready to insert into table  values " + BID + Bname+Bcordinates);
                prep.addBatch();
                System.out.println("batch added");
                totalInserts++;


                System.out.println("Total inserts " + totalInserts);
                prep.executeBatch();
                System.out.println("batch executed");
                mainConnect.commit();
            }
        } catch (SQLException ex) {
            Logger.getLogger(populate.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(populate.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private void dropdb() {
        try {
            Statement mainStat1 = mainConnect.createStatement();
            String insert3, insert4,insert5;
            insert3 = "Delete from Hydrants";
            insert4 = "Delete from buildings";
           insert5 =  "Delete from firebuildings";
           mainStat1.executeUpdate(insert3);
            System.out.println("Deleted");
           mainStat1.executeUpdate(insert4);
            System.out.println("Deleted");
            mainStat1.executeUpdate(insert5);
            System.out.println("Deletedfirebuilding");
            this.mainConnect.commit();
            //this.mainConnect.close();
        } catch (Exception e) {
            System.out.println(e.toString());
        }
    }
}

