package stormfengji;
import com.mysql.jdbc.Connection;
import com.mysql.jdbc.PreparedStatement;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Tuple;

import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Map;

/**
 * Created by li on 2018/1/22.
 */
public class WriteMysql extends BaseBasicBolt {
    @Override
    public void prepare(Map stormConf, TopologyContext context) {

    }

    public static Connection getConn() {
        Connection conn=null;
        String driver = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/mytrip";
        String username = "root";
        String password = "root";
        try {
            Class.forName(driver); //classLoader,加载对应驱动
            conn = (Connection) DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return conn;
    }

    private static void insert (String Primary_ID,String FanId,String time,String description) {
        String sql="insert into warnmessage (ID,FanId,time,description) values(?,?,?,?)";
        PreparedStatement pstmt;
        Connection conn=getConn();
        try {
            pstmt = (PreparedStatement) conn.prepareStatement(sql);
            pstmt.setString(1,Primary_ID);
            pstmt.setString(2, FanId);
            pstmt.setString(3,time);
            pstmt.setString(4, description);
            pstmt.executeUpdate();
            pstmt.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
        String FanId=tuple.getString(0);
        String time=tuple.getString(1);
        String description=tuple.getString(2);
        String Primary_ID=FanId+time;
        insert(Primary_ID,FanId, time,description);
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        //outputFieldsDeclarer.declare();
    }


}
