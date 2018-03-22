package stormfengji;

/**
 * Created by li on 2018/1/14.
 */

import com.mysql.jdbc.Connection;
import org.apache.storm.Config;
import org.apache.storm.LocalCluster;
import org.apache.storm.StormSubmitter;
import org.apache.storm.generated.AlreadyAliveException;
import org.apache.storm.generated.InvalidTopologyException;
import org.apache.storm.kafka.*;
import org.apache.storm.spout.SchemeAsMultiScheme;
import org.apache.storm.task.OutputCollector;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.TopologyBuilder;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.topology.base.BaseWindowedBolt;
import org.apache.storm.topology.base.BaseWindowedBolt.Duration;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;
import org.apache.storm.utils.Utils;
import org.apache.storm.windowing.TupleWindow;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.text.DateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class Toplogy{
    private static Integer min_windspeed;//最小风速
    private static Integer max_windspeed;//最大风速
    private static Integer min_power;//最小功率
    private static Integer max_power;//最大功率
    private static Integer windowsize;//窗口大小
    private static Integer Interval_Time;//滑动窗口间隔时间
    private static Integer threshold_temperature;//阈值发动机温度
    private static Integer threshold_count;//超过阈值温度的次数
    public static  class encapsulate extends BaseBasicBolt{
        @Override
        public void execute(Tuple tuple, BasicOutputCollector collector){
            String value=tuple.getString(0);
            String data[]=value.split(",");
            String key=data[1];
            collector.emit(new Values(key,value));
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer){
            declarer.declare(new Fields("key","value"));
        }
    }

    public static class filter1 extends BaseBasicBolt{
        @Override
        public void execute(Tuple tuple, BasicOutputCollector collector) {
            String key = tuple.getString(0);
            String value = tuple.getString(1);
            String data[] = value.split(",");
            DateFormat df = new SimpleDateFormat("yyyy/MM/dd");
            Date now=new Date();
            String time=data[2].split(" ")[0].toString();
            String TimeFanNum = time + "_" + key;
            String label = null;
            try {
                Date system_time=df.parse(now.toString());
                Date fan_time=df.parse(time);
                if(system_time.getTime()==fan_time.getTime()){
                    label="Normal";
                    collector.emit(new Values(TimeFanNum,value,label));
                }else {
                    label="Abnormal";
                    collector.emit(new Values(TimeFanNum,value,label));
                }
            }catch (Exception exception) {
                exception.printStackTrace();
            }
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer){
            declarer.declare(new Fields("TimeFanNum","value","label"));
        }
    }


    public static class filter2 extends BaseBasicBolt{
        @Override
        public void execute(Tuple tuple, BasicOutputCollector collector) {
            String key=tuple.getString(0);
            String value=tuple.getString(1);
            String data[]=value.split(",");
            String s_windspeed=data[4];
            String s_power=data[22];
            String time=data[2];
            String TimeFanNum=time+"_"+key;
            String label=null;
            if(s_windspeed!=null){
                if(s_power!=null){
                    if(Double.parseDouble(s_windspeed)>min_windspeed&&Double.parseDouble(s_windspeed)<max_windspeed
                            &&Double.parseDouble(s_power)>min_power&&Double.parseDouble(s_power)<max_power){
                        label="Normal";
                        collector.emit(new Values(TimeFanNum,value,label));
                    }
                    else{
                        label="Abnormal";
                        collector.emit(new Values(TimeFanNum,value,label));
                    }
                }
                else {
                    label="Abnormal";
                    collector.emit(new Values(TimeFanNum,value,label));
                }
            }else {
                label="Abnormal";
                collector.emit(new Values(TimeFanNum,value,label));
            }
        }
        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer){
            declarer.declare(new Fields("TimeFanNum","value","label"));
        }
    }

    public static class SlidingWindowSumBolt extends BaseWindowedBolt {
        private OutputCollector collector;
        Map<String ,Integer> counts=new HashMap<>();

        @Override
        public void prepare(Map stormConf, TopologyContext context, OutputCollector collector) {
            this.collector = collector;
        }

        @Override
        public void execute(TupleWindow inputWindow) {
            List<Tuple> tuplesInWindow = inputWindow.get();
            for (Tuple tuple : tuplesInWindow) {
                String FanId=tuple.getValue(0).toString().split("_")[1];
                String data[]=tuple.getValue(1).toString().split(",");
                Double temperature=Double.parseDouble(data[13].toString());
                String time=data[2];
                //对于每条数据如果温度大于标准设定的温度，则把该条数据放入map容器中
                if(temperature>threshold_temperature){
                    Integer warn_count=counts.get(FanId);
                    if (warn_count == null)
                        warn_count = 0;
                    warn_count++;
                    counts.put(FanId, warn_count);
                    //每次加入一条新数据判断发电机温度是否已经超过阈值温度5次以上了，若超过，则将该数据写入Mysql
                    if(warn_count>threshold_count){
                        String description=FanId+"风机"+"过去"+windowsize+"秒内发电机温度高于" + threshold_temperature+
                                "度以上出现:"+warn_count+"次";
                        collector.emit(new Values(FanId,time,description));
                    }
                }
            }
        }

        @Override
        public void declareOutputFields(OutputFieldsDeclarer declarer) {
            declarer.declare(new Fields("FanId","time","warn_message"));
        }
    }

    public static void getvalue(){
        Connection conn=null;
        String driver = "com.mysql.jdbc.Driver";
        String url = "jdbc:mysql://localhost:3306/mytrip";
        String username = "root";
        String password = "root";
        try {
            Class.forName(driver); //classLoader,加载对应驱动
            conn = (Connection) DriverManager.getConnection(url, username, password);
            Statement stmt = conn.createStatement();
            String sql = "select * from vartable where id=1"; //将数据库读取记录
            ResultSet rs = stmt.executeQuery(sql);
            while (rs.next()){
                min_windspeed=Integer.parseInt(rs.getString(2));
                max_windspeed=Integer.parseInt(rs.getString(3));
                min_power=Integer.parseInt(rs.getString(4));
                max_power=Integer.parseInt(rs.getString(5));
                windowsize=Integer.parseInt(rs.getString(6));
                Interval_Time=Integer.parseInt(rs.getString(7));
                threshold_temperature=Integer.parseInt(rs.getString(8));
                threshold_count=Integer.parseInt(rs.getString(9));
            }
            rs.close();
            stmt.close();
            conn.close();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws AlreadyAliveException, InvalidTopologyException, InterruptedException {
        //从数据库中读取清洗规则的各项阈值数据，赋值给全局变量
        getvalue();
        //System.out.println("新设置的窗口大小是:"+windowsize);
       // System.out.println("新设置的阈值温度是:"+threshold_temperature);
        //kafka-spout
        String topic = "lwl10";
        String zkRoot = "/usr/local/zookeeper-3.4.10";
        String id = "lala";
        BrokerHosts brokerHosts = new ZkHosts("172.17.11.160:2181,172.17.11.161:2181,172.17.11.162:2181");
        SpoutConfig spoutConfig = new SpoutConfig(brokerHosts, topic, zkRoot, id);
        spoutConfig.scheme = new SchemeAsMultiScheme(new StringScheme());


        //拓扑结构
        TopologyBuilder builder = new TopologyBuilder();
        builder.setSpout("KafkaSpout", new KafkaSpout(spoutConfig));
        builder.setBolt("encapsulate", new encapsulate(),1).shuffleGrouping("KafkaSpout");
        builder.setBolt("filter", new filter2(), 3).shuffleGrouping("encapsulate");
        builder.setBolt("Hbase", new HbaseBolt(), 3).shuffleGrouping("filter");
        builder.setBolt("slidingWarn", new SlidingWindowSumBolt().withWindow(new Duration(windowsize, TimeUnit.SECONDS), new Duration(Interval_Time,TimeUnit.SECONDS))).shuffleGrouping("filter");
        builder.setBolt("mysql",new WriteMysql(),3).shuffleGrouping("slidingWarn");
        Config conf = new Config();
        conf.setNumWorkers(4);
        conf.setMessageTimeoutSecs(50000);
        if (args != null && args.length > 0) {
            try {
               // StormSubmitter.submitTopology(Toplogy.class.getSimpleName(),
                 //       conf, builder.createTopology());
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
           LocalCluster cluster = new LocalCluster();
            cluster.submitTopology(Toplogy.class.getSimpleName(), conf,
                    builder.createTopology());
            Utils.sleep(60*1000);
        }
    }
}
