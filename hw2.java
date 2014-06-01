
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.Polygon;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class hw2 extends javax.swing.JFrame {

    private HashMap<Parameter, Boolean> optionsParameter = new HashMap<>();

    enum Parameter {

        BUILDINGS("buildings", "checkbox", true),
        HYDRANTS("hydrants", "checkbox", true),
        FIRE_BUILDING("fire_building", "checkbox", true),
        //CHECKBOX
        WHOLE_RANGE("whole_range", "radio", false),
        RANGE_QUERY("range_query", "radio", false),
        NEIGHBOUR_BUILDINGS("neighbour_building", "radio", false),
        CLOSEST_HYDRANTS("closest_hydrants", "radio", false);
        private final String boxName;
        private final String boxType;
        private final boolean isTable;

        private Parameter(String boxName, String boxType, boolean isTable) {
            this.boxName = boxName;
            this.boxType = boxType;
            this.isTable = isTable;
        }

        public boolean isTable() {
            return this.isTable;
        }

        public String getBoxType() {
            return this.boxType;
        }

        public String getBoxName() {
            return this.boxName;
        }
    }

    enum ShapeType {

        BUILDINGS("BID", "Bshape", "b"),
        HYDRANTS("HID", "Hshape", "p"),
        FIRE_BUILDING("NAME", null, "f");
        private String columnid;
        private String columnshape;
        private String columnName;

        public String getColumnName() {
            return this.columnName;
        }

        private ShapeType(String columnid, String columnshape, String columName) {
            this.columnid = columnid;
            this.columnshape = columnshape;
            this.columnName = columName;
        }

        public String getColumnid() {
            return this.columnid;
        }

        public String getColumnshape() {
            return this.columnshape;
        }
        
        
    }

    public class DatabaseHandler {

        private Connection mainCon;

        private void connectDB() {
            try {
                // loading Oracle Driver
                System.out.print("Looking for Oracle's jdbc-odbc driver ... ");
                DriverManager.registerDriver(new oracle.jdbc.OracleDriver());
                System.out.print("Loaded.");

                //url = "jdbc:oracle:thin:@localhost:1521:SWAPNIL";
                String url = "jdbc:oracle:thin:@localhost:1521:orcl";
                String userId = "SYSTEM";
                String password = "1234";

                System.out.print("Connecting to DB...");
                mainCon = DriverManager.getConnection(url, userId, password);
                System.out.println("connected !!");

            } catch (Exception e) {
                System.out.println("Error while connecting to DB: " + e.toString());
                System.exit(-1);
            }
        }

        public ResultSet runQuery(String query) throws SQLException {
            Connection connection = this.getConnection();
            Statement statement = connection.createStatement (ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY );
            ResultSet resultSet = statement.executeQuery(query);
            return resultSet;
        }

        public Connection getConnection() {
            if (mainCon == null) {
                connectDB();
            }
            return mainCon;

        }
    }

    public interface Shape {

        public ArrayList<Point> getPoints();

        public void addPoint(Point point);

        public Color getColor();
        
        public void setColor(Color color);
    }

    public abstract class ShapeAbstract implements Shape {

        protected ArrayList<Point> pointList = new ArrayList<>();
        protected String name;

        @Override
        public ArrayList<Point> getPoints() {
            return pointList;
        }

        /**
         * adds point to pointList
         *
         * @param point
         */
        @Override
        public void addPoint(Point point) {
            this.pointList.add(point);
        }
    }

    public class Building extends ShapeAbstract{
        private Color color = Color.YELLOW;

        @Override
        public Color getColor() {
            return this.color;
        }

        @Override
        public void setColor(Color color) {
            this.color = color;
        }
    }

    public class Hydrant extends ShapeAbstract {
        private Color color = Color.GREEN;

        @Override
        public Color getColor() {
            return this.color;
        }

        @Override
        public void setColor(Color color) {
            this.color = color;
        }
    }

    public class FireBuilding extends ShapeAbstract {
        private Color color = Color.RED;

        @Override
        public Color getColor() {
            return this.color;
        }

        @Override
        public void setColor(Color color) {
            this.color = color;
        }
    }

    public class ShapeBuilder {

        public Shape createShape(ShapeType shapeType) {
            switch (shapeType) {
                case BUILDINGS:
                    return new Building();
                case FIRE_BUILDING:
                    return new FireBuilding();
                case HYDRANTS:
                    return new Hydrant();
                default:
                //nothing

            }
            return null;
        }
    }

    public class ResultSetHandler {

        private ShapeBuilder shapeBuilder = new ShapeBuilder();
        private ArrayList<Shape> shapeList = new ArrayList<>();

        public ArrayList<Shape> parse(ResultSet resultSet)  {
            try {
            shapeList.clear();
            String previousId = "";
            String currentId = "";
            boolean isFireBuilding = false;
            Shape shape = null;
            while (resultSet.next()) {
               
                currentId = resultSet.getString(1);
                try {
                    if(resultSet.getString(4) != null ){
                        isFireBuilding = true;
                    }
                } catch (SQLException e) {
                    //intentionally ignoring exception
                }
                
                if (previousId.equals("")) {
                    previousId = currentId;
                    shape = shapeBuilder.createShape(this.getShapeTypeFromId(previousId, isFireBuilding));
                    shape = this.changeShapeColor(shape, optionsParameter);                    
                    Point point = new Point(resultSet.getInt(2), resultSet.getInt(3));
                    shape.addPoint(point);
                    continue;
                }
                if (currentId.equals(previousId)) {
                    Point point = new Point(resultSet.getInt(2), resultSet.getInt(3));
                    shape.addPoint(point);
                    if(resultSet.isLast()){
                         shapeList.add(shape);

                    }
                } else {
                    //add the previous shape here before creating a new one 
                    shapeList.add(shape);
                    previousId = resultSet.getString(1);
                    shape = shapeBuilder.createShape(this.getShapeTypeFromId(previousId, isFireBuilding));
                    shape = this.changeShapeColor(shape, optionsParameter);   
                    Point point = new Point(resultSet.getInt(2), resultSet.getInt(3));
                    shape.addPoint(point);
                }
            }
            } catch (SQLException e) {
                  Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, "Failed to Parse query");
                 Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, e);
            }
           

            return shapeList;
        }

        private ShapeType getShapeTypeFromId(String rowId, boolean isFireBuilding) {
            if(isFireBuilding){
                return ShapeType.FIRE_BUILDING;
            }
            String prefix = rowId.substring(0, 1);
            for (ShapeType shapeType : ShapeType.values()) {
                if (shapeType.columnName.startsWith(prefix, 0)) {
                    return shapeType;
                }
            }
            return null;
        }

        private Shape changeShapeColor(Shape shape, HashMap<Parameter, Boolean> optionsParameter) {
            if(optionsParameter.get(Parameter.CLOSEST_HYDRANTS) != null && shape instanceof Building){
                        shape.setColor(Color.red);
                    }
              if(optionsParameter.get(Parameter.FIRE_BUILDING) != null && shape instanceof FireBuilding){
                        shape.setColor(Color.red);
                    }
            
            return shape;
        }
    }

    public class PolygonDrawingHandler {
        private final static int OFFSET_X = 8;
        private final static int OFFSET_Y = 32;
        
        private final static int HYDRANT_OFFSET_X = 6;
        private final static int HYDRANT_OFFSET_Y = 6;
        
        /**
         * Draws polygon
         *
         * @param shapeList
         */
        public void draw(ArrayList<Shape> shapeList) {
            for (Shape shape : shapeList) {
                if (shape instanceof Hydrant) {
                    drawHydrant(shape);
                } else {
                    drawOthers(shape);
                }

            }
        }

        private void drawHydrant(Shape shape) {
            ArrayList<Point> points = shape.getPoints();
            points = makeHydrantSquare(points);
            Graphics graphics = getGraphics();
            int[] xpoints = new int[points.size()];
            int[] ypoints = new int[points.size()];
            int i =0;
            for (Point point : points) {
                xpoints[i] = point.x + OFFSET_X;
                ypoints[i]  = point.y + OFFSET_Y;
                i++;
            }
            Polygon polygon = new Polygon(xpoints, ypoints, points.size());
            graphics.setColor(shape.getColor());
            graphics.fillPolygon(polygon);
        }

        private void drawOthers(Shape shape) {
            ArrayList<Point> points = shape.getPoints();
            Graphics graphics = getGraphics();
            int[] xpoints = new int[points.size()];
            int[] ypoints = new int[points.size()];
            int i =0;
            for (Point point : points) {
                xpoints[i] = point.x + OFFSET_X;
                ypoints[i]  = point.y + OFFSET_Y;
                i++;
            }
            Polygon polygon = new Polygon(xpoints, ypoints, points.size());
            graphics.setColor(shape.getColor());
            graphics.drawPolygon(polygon);
        }

        private ArrayList<Point> makeHydrantSquare(ArrayList<Point> points) {
            Point point1 = new Point(points.get(0).x - HYDRANT_OFFSET_X , points.get(0).y + HYDRANT_OFFSET_Y);
            Point point2 = new Point(points.get(0).x + HYDRANT_OFFSET_X,  points.get(0).y + HYDRANT_OFFSET_Y);
            Point point3 = new Point(points.get(0).x + HYDRANT_OFFSET_X,  points.get(0).y - HYDRANT_OFFSET_Y );
            Point point4 = new Point(points.get(0).x - HYDRANT_OFFSET_X,  points.get(0).y  - HYDRANT_OFFSET_Y);
            points.add(point1);
            points.add(point2);
            points.add(point3);  
            points.add(point4);
            points.remove(0);
            return points;
        }
    }

    public class QueryBuilder {
private int OFFSET_X =10;
private int OFFSET_Y =10;
        public ArrayList<String> buildQuery(HashMap<Parameter, Boolean> queryParamter) throws SQLException {

             ArrayList<String> query =  new ArrayList<>();
            if (queryParamter.get(Parameter.WHOLE_RANGE) != null) {
                query = this.wholeArea(queryParamter);
                return query;
            }
            
            if(queryParamter.get(Parameter.NEIGHBOUR_BUILDINGS) != null){
                query = this.neighbourArea(queryParamter);
                return query;
            }
            
            return null;
        }
        
            public ArrayList<String> buildQuery(HashMap<Parameter, Boolean> queryParamter, ArrayList<Point> points) throws SQLException {

             ArrayList<String> query =  new ArrayList<>();
            
            if(queryParamter.get(Parameter.RANGE_QUERY) != null){
                query = this.rangeQuery(queryParamter, points);
                return query;
            }
            if(queryParamter.get(Parameter.CLOSEST_HYDRANTS)!= null){
                query = this.closestHydrantArea(queryParamter, points);
                return query;
            }
            return query;
        }
        

        private  ArrayList<String> wholeArea(HashMap<Parameter, Boolean> queryParamter) {
            String buildingRange = "SELECT b.bid, t.X, t.Y FROM buildings b ,TABLE(SDO_UTIL.GETVERTICES(b.bshape)) t";
            String hydrantRange = "SELECT h.hid, t.X, t.Y FROM hydrants h ,TABLE(SDO_UTIL.GETVERTICES(h.hshape)) t";
            String fireBuildings = "SELECT b.bid, t.X, t.Y , fb.fbname FROM buildings b,firebuildings fb ,TABLE(SDO_UTIL.GETVERTICES(b.bshape)) t where fb.fbname = bname";

            ArrayList<String> queryString = new ArrayList<>();
            if(queryParamter.containsKey(Parameter.BUILDINGS)){
                queryString.add(buildingRange);
            }
            if(queryParamter.containsKey(Parameter.HYDRANTS)){
                queryString.add(hydrantRange);
            }
            if(queryParamter.containsKey(Parameter.FIRE_BUILDING)){
                 queryString.add(fireBuildings);
            }
            
            
            
           


            return queryString;
        }

        private ArrayList<String> rangeQuery(HashMap<Parameter, Boolean> queryParamter, ArrayList<Point> points) {
           String buildingRange = "SELECT bid, t.X, t.Y FROM buildings, TABLE(SDO_UTIL.GETVERTICES(bshape)) t WHERE SDO_RELATE(" +
                                    "bshape,SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1)," +
                                    "SDO_ORDINATE_ARRAY(POINTS_ARRAY_PLACEHOLDER)),'mask=anyinteract query = WINDOW') = 'TRUE'";
                   
                           
                     
           
           String hydrantRange = "SELECT hid, t.X, t.Y FROM hydrants, TABLE(SDO_UTIL.GETVERTICES(hshape))   t   WHERE SDO_RELATE(" +
                                "hshape,SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1)," +
                                    "SDO_ORDINATE_ARRAY(POINTS_ARRAY_PLACEHOLDER)),'mask=anyinteract query = WINDOW') = 'TRUE'";
           
           String fireBuildings = "SELECT b.bid, t.X, t.Y , fb.fbname FROM buildings b,firebuildings fb ,\n" +
                                    "TABLE(SDO_UTIL.GETVERTICES(b.bshape)) t where fb.fbname = b.bname ";
           String pointsString = "" ;

            for (Point point : points) {
                if(pointsString.equals("")){
                    
                    pointsString += point.x+ "," + point.y;
                    continue;
                }
                
                pointsString += ", " + point.x + "," + point.y;
            }
           
           
            ArrayList<String> queryString = new ArrayList<>();
            if(queryParamter.containsKey(Parameter.BUILDINGS)){
                buildingRange = buildingRange.replaceFirst("POINTS_ARRAY_PLACEHOLDER", pointsString);
                
                queryString.add(buildingRange);
            }
            if(queryParamter.containsKey(Parameter.HYDRANTS)){
                hydrantRange = hydrantRange.replaceFirst("POINTS_ARRAY_PLACEHOLDER", pointsString);
                
                queryString.add(hydrantRange);
            }
            if(queryParamter.containsKey(Parameter.FIRE_BUILDING)){
                 queryString.add(fireBuildings);
            }
          


            return queryString;
        
        }

        private ArrayList<String> fireBuilding(HashMap<Parameter, Boolean> queryParamter) {
          
           String buildingRange = "SELECT b.bid, t.X, t.Y FROM buildings b ,TABLE(SDO_UTIL.GETVERTICES(b.bshape)) t";
           String hydrantRange = "SELECT h.hid, t.X, t.Y FROM hydrants h ,TABLE(SDO_UTIL.GETVERTICES(h.hshape)) t";
           String fireBuildings = "SELECT b.bid, t.X, t.Y , fb.fbname FROM buildings b,firebuildings fb ,TABLE(SDO_UTIL.GETVERTICES(b.bshape)) t where fb.fbname = bname";

           
            ArrayList<String> queryString = new ArrayList<>();
            if(queryParamter.containsKey(Parameter.BUILDINGS)){
                queryString.add(buildingRange);
            }
            if(queryParamter.containsKey(Parameter.HYDRANTS)){
                queryString.add(hydrantRange);
            }
            if(queryParamter.containsKey(Parameter.FIRE_BUILDING)){
                 queryString.add(fireBuildings);
            }


            return queryString;
        }

        private ArrayList<String> neighbourArea(HashMap<Parameter, Boolean> queryParamter) {
             
           String buildingRange = "SELECT bid, t.X, t.Y FROM buildings,TABLE(SDO_UTIL.GETVERTICES(bshape)) t\n" +
"                                    WHERE SDO_WITHIN_DISTANCE(bshape,\n" +
"                                    SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1),\n" +
"                                    SDO_ORDINATE_ARRAY(226,150,254,164,240,191,212,176)),\n" +
"                                    'distance=100') = 'TRUE'                               \n" +
"union all\n" +
"SELECT bid, t.X, t.Y FROM buildings,TABLE(SDO_UTIL.GETVERTICES(bshape)) t\n" +
"                                    WHERE SDO_WITHIN_DISTANCE(bshape,\n" +
"                                    SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1),\n" +
"                                    SDO_ORDINATE_ARRAY(564,425,585,436,573,458,552,447,677,320,708,337,690,368,661,351)),\n" +
"                                    'distance=100') = 'TRUE'";
          
            String fireBuildings = "SELECT b.bid, t.X, t.Y , fb.fbname FROM buildings b,firebuildings fb ,TABLE(SDO_UTIL.GETVERTICES(b.bshape)) t where fb.fbname = bname";

       ArrayList<String> queryString = new ArrayList<>();
            if(queryParamter.containsKey(Parameter.BUILDINGS)){
                queryString.add(buildingRange);
            }

            if(queryParamter.containsKey(Parameter.FIRE_BUILDING)){
                 queryString.add(fireBuildings);
            }


            return queryString;
        }

        
        private ArrayList<String> closestHydrantArea(HashMap<Parameter, Boolean> queryParamter, ArrayList<Point> points) {
           String buildingRange = "select bid, t.X, t.Y FROM buildings,TABLE(SDO_UTIL.GETVERTICES(bshape)) t \n" +
	"WHERE sdo_NN (bshape,SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1),SDO_ORDINATE_ARRAY(POINTS_ARRAY_PLACEHOLDER)),'sdo_num_res= 1',1) = 'TRUE' ";
                    
       
            
            
           String hydrantRange =    "Select distinct hid,t.X, t.Y FROM hydrants,buildings , TABLE(SDO_UTIL.GETVERTICES(hshape)) t WHERE SDO_NN(" +
                                    "hshape,SDO_GEOMETRY(2003, NULL, NULL, SDO_ELEM_INFO_ARRAY(1,1003,1)," +
                                       "SDO_ORDINATE_ARRAY(POINTS_ARRAY_PLACEHOLDER)),'sdo_num_res= 2', 1) = 'TRUE'";
                 
          
           String pointsString="";
             for (Point point : points) {
                if(pointsString.equals("")){
                    pointsString += point.x + "," + point.y;
                    continue;
                }
                pointsString += ", " + point.x + "," + point.y;
            }
             ArrayList<String> queryString = new ArrayList<>();
            if(queryParamter.containsKey(Parameter.BUILDINGS)){
                buildingRange = buildingRange.replaceFirst("POINTS_ARRAY_PLACEHOLDER", pointsString);

                queryString.add(buildingRange);
                        
            }
            if(queryParamter.containsKey(Parameter.HYDRANTS)){
                hydrantRange = hydrantRange.replaceFirst("POINTS_ARRAY_PLACEHOLDER", pointsString);
                queryString.add(hydrantRange);
            }
          

            return queryString;
        }
    }


    public class DrawFromClick {

        /**
         *
         */
        private ArrayList<Point> pointlist = new ArrayList<Point>();

        public void setPoint(Point point) {
            pointlist.add(point);
            System.out.println(pointlist.toString());
        }

        public void drawPolygon(JLabel jLabel) {
            try {
                Graphics g;
                g = getGraphics();

                int sizeOfList = pointlist.size();
                int[] x = new int[sizeOfList];
                int[] y = new int[sizeOfList];
                int i = 0;
                for (Point point : pointlist) {
                    x[i] = point.x;
                    y[i] = point.y;
                    i++;
                }

                Polygon polygon = new Polygon(x, y, pointlist.size());
                g.setColor(Color.red);
                g.drawPolygon(polygon);


            } catch (Exception e) {
                throw e;
            }

        }

        public ArrayList<Point> getPointList() {
            return pointlist;
        }

        public void clear() {
            pointlist.clear();
        }
    }
  
    public class  SubmitQueryValidator{
        public boolean isValid(){
            boolean checkboxSelected = false;
            boolean radioButtonSelected = false;
            // check if we have atleast one chaeckbox selected with one radio button
            for (Map.Entry<Parameter, Boolean> entry : optionsParameter.entrySet()) {
                Parameter parameter = entry.getKey();
                Boolean isSelected = entry.getValue();
                if(isSelected && parameter.getBoxType().equals("checkbox")){
                    checkboxSelected = true;
                }
                if(isSelected && parameter.getBoxType().equals("radio")){
                    radioButtonSelected = true;
                }
            }
            
            if(checkboxSelected && radioButtonSelected){
                return true;
            }
            
            return false;
        }
    }
    
    private ButtonGroup group = new ButtonGroup();
    private DrawFromClick drawFromClick = new DrawFromClick();
    private DatabaseHandler databaseHandler = new DatabaseHandler();

    public hw2() {

        optionsParameter.clear();
        initComponents();
        group.add(Whole_Range);
        group.add(Range_Query);
        group.add(Neighbour_buildings);
        group.add(Closest_Hydrants);


    }

    public static void checkAndRemove(Parameter parameter, HashMap<Parameter, Boolean> optionsParameter) {
        switch (parameter) {
            case WHOLE_RANGE:
                optionsParameter.put(Parameter.WHOLE_RANGE, Boolean.TRUE);
                optionsParameter.remove(Parameter.RANGE_QUERY);
                optionsParameter.remove(Parameter.NEIGHBOUR_BUILDINGS);
                optionsParameter.remove(Parameter.CLOSEST_HYDRANTS);
                break;
            case RANGE_QUERY:
                optionsParameter.put(Parameter.RANGE_QUERY, Boolean.TRUE);
                optionsParameter.remove(Parameter.WHOLE_RANGE);
                optionsParameter.remove(Parameter.NEIGHBOUR_BUILDINGS);
                optionsParameter.remove(Parameter.CLOSEST_HYDRANTS);
                break;
            case NEIGHBOUR_BUILDINGS:
                optionsParameter.put(Parameter.NEIGHBOUR_BUILDINGS, Boolean.TRUE);
                optionsParameter.remove(Parameter.RANGE_QUERY);
                optionsParameter.remove(Parameter.WHOLE_RANGE);
                optionsParameter.remove(Parameter.CLOSEST_HYDRANTS);
                break;
            case CLOSEST_HYDRANTS:
                optionsParameter.put(Parameter.CLOSEST_HYDRANTS, Boolean.TRUE);
                optionsParameter.remove(Parameter.RANGE_QUERY);
                optionsParameter.remove(Parameter.WHOLE_RANGE);
                optionsParameter.remove(Parameter.NEIGHBOUR_BUILDINGS);
            default:

        }

    }

    // <editor-fold defaultstate="collapsed" desc="Generated Code">                          
    private void initComponents() {

        jMenuItem1 = new javax.swing.JMenuItem();
        jCheckBoxMenuItem1 = new javax.swing.JCheckBoxMenuItem();
        jTextField1 = new javax.swing.JTextField();
        buttonGroup1 = new javax.swing.ButtonGroup();
        buttonGroup2 = new javax.swing.ButtonGroup();
        buttonGroup3 = new javax.swing.ButtonGroup();
        buttonGroup4 = new javax.swing.ButtonGroup();
        buttonGroup5 = new javax.swing.ButtonGroup();
        imageLabel = new javax.swing.JLabel();
        jCheckBox3 = new javax.swing.JCheckBox();
        Submit_Query = new javax.swing.JButton();
        Mouse_current = new javax.swing.JTextField();
        jPanel1 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        Buildings = new javax.swing.JCheckBox();
        Building_On_Fire = new javax.swing.JCheckBox();
        Hydrants = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jPanel2 = new javax.swing.JPanel();
        Whole_Range = new javax.swing.JRadioButton();
        Range_Query = new javax.swing.JRadioButton();
        Neighbour_buildings = new javax.swing.JRadioButton();
        Closest_Hydrants = new javax.swing.JRadioButton();
        Query = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        currentQuery = new javax.swing.JTextArea();

        jMenuItem1.setText("jMenuItem1");

        jCheckBoxMenuItem1.setSelected(true);
        jCheckBoxMenuItem1.setText("jCheckBoxMenuItem1");

        jTextField1.setText("jTextField1");

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Sakshi Arora-3238043687");
        setAlwaysOnTop(true);
        setBackground(new java.awt.Color(105, 105, 105));
        setBounds(new java.awt.Rectangle(0, 0, 1000, 1000));
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        imageLabel.setIcon(new javax.swing.ImageIcon(getClass().getResource("map.jpg"))); // NOI18N
        imageLabel.setVerticalAlignment(javax.swing.SwingConstants.TOP);
        imageLabel.setAlignmentY(0.0F);
        imageLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                imageLabelMouseClicked(evt);
            }
        });
        imageLabel.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                imageLabelComponentShown(evt);
            }
        });
        imageLabel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                imageLabelMouseMoved(evt);
            }
        });
        imageLabel.addContainerListener(new java.awt.event.ContainerAdapter() {
            public void componentAdded(java.awt.event.ContainerEvent evt) {
                imageLabelComponentAdded(evt);
            }
        });

        jCheckBox3.setText("jCheckBox1");

        Submit_Query.setText("Submit Query");
        Submit_Query.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Submit_QueryActionPerformed(evt);
            }
        });

        Mouse_current.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Mouse_currentActionPerformed(evt);
            }
        });

        jPanel3.setBorder(javax.swing.BorderFactory.createEtchedBorder());

        Buildings.setText("Buildings");
        Buildings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                BuildingsActionPerformed(evt);
            }
        });

        Building_On_Fire.setText("Buildings on ifre");
        Building_On_Fire.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Building_On_FireActionPerformed(evt);
            }
        });

        Hydrants.setText("Hydrants");
        Hydrants.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                HydrantsActionPerformed(evt);
            }
        });

        jLabel2.setText("ActiveFeaturetypes");

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(Building_On_Fire)
                    .addComponent(Hydrants)
                    .addComponent(Buildings)
                    .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 109, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addComponent(jLabel2)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 11, Short.MAX_VALUE)
                .addComponent(Buildings)
                .addGap(6, 6, 6)
                .addComponent(Building_On_Fire)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Hydrants))
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(203, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGap(31, 31, 31)
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(45, Short.MAX_VALUE))
        );

        jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder(new java.awt.Color(0, 0, 0), null));
        jPanel2.setForeground(new java.awt.Color(240, 240, 240));
        jPanel2.setToolTipText("");
        jPanel2.addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentShown(java.awt.event.ComponentEvent evt) {
                jPanel2ComponentShown(evt);
            }
        });

        Whole_Range.setText("Whole Range");
        Whole_Range.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Whole_RangeActionPerformed(evt);
            }
        });

        Range_Query.setText("Range Query");
        Range_Query.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Range_QueryActionPerformed(evt);
            }
        });

        Neighbour_buildings.setText("Find Neighbour Buildings");
        Neighbour_buildings.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Neighbour_buildingsActionPerformed(evt);
            }
        });

        Closest_Hydrants.setText("Find Closest Fire hydrants");
        Closest_Hydrants.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                Closest_HydrantsActionPerformed(evt);
            }
        });

        Query.setText("Query");

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addContainerGap()
                        .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(Range_Query)
                            .addComponent(Neighbour_buildings)
                            .addComponent(Closest_Hydrants)
                            .addComponent(Whole_Range)))
                    .addGroup(jPanel2Layout.createSequentialGroup()
                        .addGap(21, 21, 21)
                        .addComponent(Query, javax.swing.GroupLayout.PREFERRED_SIZE, 49, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(28, Short.MAX_VALUE))
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap(24, Short.MAX_VALUE)
                .addComponent(Query, javax.swing.GroupLayout.PREFERRED_SIZE, 14, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(Whole_Range)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Range_Query)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(Neighbour_buildings)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(Closest_Hydrants)
                .addContainerGap())
        );

        currentQuery.setColumns(20);
        currentQuery.setRows(5);
        jScrollPane1.setViewportView(currentQuery);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(imageLabel)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addGap(102, 102, 102)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                            .addGroup(layout.createSequentialGroup()
                                .addGap(112, 112, 112)
                                .addComponent(Submit_Query, javax.swing.GroupLayout.PREFERRED_SIZE, 187, javax.swing.GroupLayout.PREFERRED_SIZE))))
                    .addComponent(Mouse_current, javax.swing.GroupLayout.PREFERRED_SIZE, 237, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 549, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(473, Short.MAX_VALUE))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                    .addContainerGap(1465, Short.MAX_VALUE)
                    .addComponent(jCheckBox3)
                    .addGap(195, 195, 195)))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addGap(19, 19, 19)
                        .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(70, 70, 70)
                        .addComponent(Submit_Query, javax.swing.GroupLayout.PREFERRED_SIZE, 34, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addComponent(imageLabel))
                .addGap(37, 37, 37)
                .addComponent(Mouse_current, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 51, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(24, 24, 24))
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(layout.createSequentialGroup()
                    .addGap(71, 71, 71)
                    .addComponent(jCheckBox3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addContainerGap(647, Short.MAX_VALUE)))
        );

        pack();
    }// </editor-fold>                        

    private void Submit_QueryActionPerformed(java.awt.event.ActionEvent evt) {                                             
        try {
            QueryBuilder queryBuilder = new QueryBuilder();
            ResultSetHandler resultSetHandler = new ResultSetHandler();
            PolygonDrawingHandler polygonDrawingHandler = new PolygonDrawingHandler();            
            SubmitQueryValidator submitQueryValidator = new SubmitQueryValidator();
            
            if(submitQueryValidator.isValid() == false){
                Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, "Sumbit event invalid");
                throw new Exception("Invalid query Submitted, please check one checkbox and one radio box");
            }
            
            
                   ArrayList<String> queryList;
            
            if(drawFromClick.getPointList().isEmpty() == false){
               
                queryList = queryBuilder.buildQuery(optionsParameter, drawFromClick.getPointList());
            }else{
                queryList = queryBuilder.buildQuery(optionsParameter);
            }
            printQueryToLabel(queryList, currentQuery);
                        
            
            for(String query: queryList){
                
                ResultSet resultSet = databaseHandler.runQuery(query);
                ArrayList<Shape> shapes = resultSetHandler.parse(resultSet);
                polygonDrawingHandler.draw(shapes);
            }
            
        } catch (SQLException ex) {
            Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, ex);
            imageLabel.repaint();
           drawFromClick.clear();
        }catch (Exception e){
             Logger.getLogger(hw2.class.getName()).log(Level.SEVERE, null, e);
        }
    }                                            

    private void BuildingsActionPerformed(java.awt.event.ActionEvent evt) {                                          
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        drawFromClick.clear();
            imageLabel.repaint();// clear range polygon
        if (selected) {
            optionsParameter.put(Parameter.BUILDINGS, Boolean.TRUE);
        } else {
            optionsParameter.remove(Parameter.BUILDINGS);
        }


        // TODO add your handling code here:
    }                                         
private void printQueryToLabel(ArrayList<String> queryList, JTextArea currentQuery){

String queryString ="";

  for (int i = 0; i < queryList.size(); i++) {
currentQuery.append("query " + (i+1) + ":" + queryList.get(i));
      queryString = queryString + "  query " + (i+1) + ":" + queryList.get(i) ;
      
}

currentQuery.setText(queryString);
}

    private void Mouse_currentActionPerformed(java.awt.event.ActionEvent evt) {                                              
        }                                             

    private void imageLabelMouseMoved(java.awt.event.MouseEvent evt) {                                      
        Mouse_current.setText("current Mouse Position=(" + evt.getX() + ","+ evt.getY() + ")");

    }                                     

    private void imageLabelComponentShown(java.awt.event.ComponentEvent evt) {                                          
    }                                         

    private void imageLabelComponentAdded(java.awt.event.ContainerEvent evt) {                                          
        // TODO add your handling code here:
    }                                         

    private void jPanel2ComponentShown(java.awt.event.ComponentEvent evt) {                                       
        /* listen to clicks on 'unselectRadio' button */
        // evt.
    }                                      

    private void Whole_RangeActionPerformed(java.awt.event.ActionEvent evt) {                                            
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        drawFromClick.clear();
            imageLabel.repaint();
        if (selected) {
            checkAndRemove(Parameter.WHOLE_RANGE, optionsParameter);
            
        }

    }                                           

    private void Range_QueryActionPerformed(java.awt.event.ActionEvent evt) {                                            

        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        drawFromClick.clear();
         imageLabel.repaint();
        if (selected) {
            checkAndRemove(Parameter.RANGE_QUERY, optionsParameter);
            
        }


    }                                           
//checkbox
    private void Building_On_FireActionPerformed(java.awt.event.ActionEvent evt) {                                                 
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
         drawFromClick.clear();
        imageLabel.repaint();
        if (selected) {
            optionsParameter.put(Parameter.FIRE_BUILDING, Boolean.TRUE);
        } else {
            optionsParameter.remove(Parameter.FIRE_BUILDING);
           
        }

        // TODO add your handling code here:
    }                                                

    private void HydrantsActionPerformed(java.awt.event.ActionEvent evt) {                                         
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        drawFromClick.clear();
        imageLabel.repaint();
        if (selected) {
            optionsParameter.put(Parameter.HYDRANTS, Boolean.TRUE);
        } else {
            optionsParameter.remove(Parameter.HYDRANTS);
             
            
        }



        // TODO add your handling code here:
    }                                        

    private void Neighbour_buildingsActionPerformed(java.awt.event.ActionEvent evt) {                                                    
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
         drawFromClick.clear();
            imageLabel.repaint();// clear range polygon
        if (selected) {
            checkAndRemove(Parameter.NEIGHBOUR_BUILDINGS, optionsParameter);
           
        }

    }                                                   

    private void Closest_HydrantsActionPerformed(java.awt.event.ActionEvent evt) {                                                 
        AbstractButton abstractButton = (AbstractButton) evt.getSource();
        boolean selected = abstractButton.getModel().isSelected();
        drawFromClick.clear();
        imageLabel.repaint();// clear range polygon
        if (selected) {
            checkAndRemove(Parameter.CLOSEST_HYDRANTS, optionsParameter);
            
        }

    }                                                

    private void imageLabelMouseClicked(java.awt.event.MouseEvent evt) {                                        
        // if range query is selected
        if (optionsParameter.containsKey(Parameter.RANGE_QUERY)) {
            if (SwingUtilities.isLeftMouseButton(evt)) {

                drawFromClick.setPoint(evt.getPoint());

            }
            if (SwingUtilities.isRightMouseButton(evt)) {
                drawFromClick.drawPolygon(imageLabel);
            }
            return;
        }
        
       if (optionsParameter.containsKey(Parameter.CLOSEST_HYDRANTS)) {
            if (SwingUtilities.isLeftMouseButton(evt)) {

                drawFromClick.setPoint(evt.getPoint());

            }
           
        }


    }                                       
    public static void main(String args[]) {
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new hw2().setVisible(true);
                
            }
        });
    }
    // Variables declaration - do not modify                     
    private javax.swing.JCheckBox Building_On_Fire;
    private javax.swing.JCheckBox Buildings;
    private javax.swing.JRadioButton Closest_Hydrants;
    private javax.swing.JCheckBox Hydrants;
    private javax.swing.JTextField Mouse_current;
    private javax.swing.JRadioButton Neighbour_buildings;
    private javax.swing.JLabel Query;
    private javax.swing.JRadioButton Range_Query;
    private javax.swing.JButton Submit_Query;
    private javax.swing.JRadioButton Whole_Range;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.ButtonGroup buttonGroup2;
    private javax.swing.ButtonGroup buttonGroup3;
    private javax.swing.ButtonGroup buttonGroup4;
    private javax.swing.ButtonGroup buttonGroup5;
    private javax.swing.JTextArea currentQuery;
    private javax.swing.JLabel imageLabel;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JCheckBoxMenuItem jCheckBoxMenuItem1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTextField jTextField1;
    // End of variables declaration                   
}
