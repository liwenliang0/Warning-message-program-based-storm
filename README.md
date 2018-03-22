# Warning-message-program-based-storm

利用风场实时采集的机组运行数据，对数据进行实时清洗，保证数据的质量。同时处理后的数据划分为两类   
（正常数据、不合理数据），分别存储到HBase中，作为历史数据分析。通过时间窗口对正常数据进行指标监控，   
指标数值超出特定界限时，给出报警信息，并存储到MySQL，最后推送到Web端实时展示报警信息   
一、机组运行数据清洗规则    
1、运行数据日期不是当日数据   
2、运行数据风速 为空||=-902||风速在 3～12之外    
3、运行数据功率 为空||=-902||功率在 -0.5*1500~2*1500之外   
二、清洗数据后存储HBase    
1、正常数据 & 不合理数据 全部存入HBase中    
2、划分两个表（Normal/Abnormal）； Rowkey设计： 年月日时分秒_机组编号； 列： Value（把数据写入一个列中）    
 三、实时监控报警     
对于正常数据监控异常指标，并输出到MySQL中记录， Web显示报警信息。   
规则：每5S监控30S内发电机温度高于80度以上5次，报警（机组编号、报警时间、报警描述：过去30S内发电机温度高于80   
度以上出现： 6/10（次 ）    
由于实战项目无法接入机组实时运行数据，现提供程序，模拟机组运行数据，实时写入Kafka   
参见代码： GenDataKafka    
该模拟程序实时产生三台机组的实时运行数据   
高级功能:    
清洗规则配置在MySQL中，每次启动时，加载规则。而规则的配置可以开放前端页面来配置   
实现步骤:     
采用Topology spout将上游kafka数据读入，在filter2 bolt清洗数据，hbase bolt将正常以及非正常数据存入hbase中，   
然后采用滑动窗口机制，将fliterbolt过滤出来的正常数据计算发电机温度高于80度的风机，计数。然后将风机，描述存入mysql中，通过web页面调取，展示出来。    
数据字段含义：    
![Alt_text]https://github.com/liwenliang0/Warning-message-program-based-storm/raw/master/image/description.png
