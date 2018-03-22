package stormfengji;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.hbase.HBaseConfiguration;
import org.apache.hadoop.hbase.TableName;
import org.apache.hadoop.hbase.client.Connection;
import org.apache.hadoop.hbase.client.ConnectionFactory;
import org.apache.hadoop.hbase.client.Put;
import org.apache.hadoop.hbase.client.Table;
import org.apache.hadoop.hbase.util.Bytes;
import org.apache.storm.task.TopologyContext;
import org.apache.storm.topology.BasicOutputCollector;
import org.apache.storm.topology.OutputFieldsDeclarer;
import org.apache.storm.topology.base.BaseBasicBolt;
import org.apache.storm.tuple.Fields;
import org.apache.storm.tuple.Tuple;
import org.apache.storm.tuple.Values;

import java.io.IOException;
import java.util.Map;

/**
 * Created by li on 2018/1/14 0014.
 */
public class HbaseBolt extends BaseBasicBolt {
    private Connection connection;
    private Table table_Normal;
    private Table table_Abnormal;

    @Override
    public void prepare(Map stormConf, TopologyContext context) {
        Configuration config = HBaseConfiguration.create();
        config.set("hbase.zookeeper.property.clientPort", "2181");
        config.set("hbase.zookeeper.quorum", "172.17.11.160,172.17.11.161,172.17.11.162");
        try {
            connection = ConnectionFactory.createConnection(config);
            table_Normal = connection.getTable(TableName.valueOf("LWL_Normal"));
            table_Abnormal=connection.getTable(TableName.valueOf("LWL_Abnormal"));
        } catch (IOException e) {
           e.printStackTrace();
        }
    }

    @Override
    public void execute(Tuple tuple, BasicOutputCollector basicOutputCollector) {
        String TimeFanNum = tuple.getString(0);
        String value = tuple.getString(1);
        String label=tuple.getString(2);
        String FanNum=TimeFanNum.split("_")[1];
        try {
            //以年月日时分秒_机组编号作为row key
            Put put = new Put(Bytes.toBytes(TimeFanNum));
            put.addColumn(Bytes.toBytes("result"), Bytes.toBytes("value"), Bytes.toBytes(value));
            if(label.equals("Normal")){
                table_Normal.put(put);
                //将每条正常数据发送至下游用于预警判断
                basicOutputCollector.emit(new Values(FanNum,value));
            }else{
                table_Abnormal.put(put);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void declareOutputFields(OutputFieldsDeclarer outputFieldsDeclarer) {
        outputFieldsDeclarer.declare(new Fields("FanNum","value"));
    }
}
