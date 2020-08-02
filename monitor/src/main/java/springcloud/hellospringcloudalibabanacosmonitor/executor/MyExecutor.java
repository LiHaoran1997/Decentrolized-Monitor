package springcloud.hellospringcloudalibabanacosmonitor.executor;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import groovy.json.StringEscapeUtils;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import springcloud.hellospringcloudalibabanacosmonitor.config.jmsConfig;
import org.springframework.beans.factory.annotation.Autowired;
import springcloud.hellospringcloudalibabanacosmonitor.controller.BlockchainInstanceMonitorController;
import springcloud.hellospringcloudalibabanacosmonitor.controller.MonitorController;
import springcloud.hellospringcloudalibabanacosmonitor.qps.QpsHelper;
import springcloud.hellospringcloudalibabanacosmonitor.qps.RunableInsert;
import springcloud.hellospringcloudalibabanacosmonitor.qps.TimeUtil;
import springcloud.hellospringcloudalibabanacosmonitor.service.BlockchainService;
import springcloud.hellospringcloudalibabanacosmonitor.service.MonitorService;
import springcloud.hellospringcloudalibabanacosmonitor.service.ProviderService;

import java.sql.Time;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyExecutor {
    private ExecutorService executor = Executors.newCachedThreadPool() ;
    private QpsHelper qpsHelper;
    private MonitorService monitorService;
    private String service;
    private String ipaddr;
    private BlockchainService blockchainService;
    private long clickTime;
    private String seriviceCompleted;
    private int nums;
    private ProviderService providerService;
    @Autowired
    private jmsConfig jmsConfig;
    public MyExecutor(String service, String ipaddr, QpsHelper qpsHelper, MonitorService monitorService, BlockchainService blockchainService, long clickTime, String serviceCompleted, int nums, ProviderService providerService){
        this.service=service;
        this.ipaddr=ipaddr;
        this.qpsHelper=qpsHelper;
        this.monitorService=monitorService;
        this.blockchainService=blockchainService;
        this.clickTime=clickTime;
        this.seriviceCompleted=serviceCompleted;
        this.nums=nums;
        this.providerService=providerService;
    }
    public void fun() throws Exception {
        executor.submit(new Runnable(){
            @Override
             public void run() {
                    try {
                        System.out.println("睡前"+TimeUtil.currentTimeMillis());
                        Timestamp serviceTime = new Timestamp(TimeUtil.currentTimeMillis());
                        System.out.println("睡后"+TimeUtil.currentTimeMillis());
                        Timestamp clicktime = new Timestamp(clickTime);
                        if (qpsHelper.getTimestamp()-monitorService.LAST_UPDATE>=1000){
                            String blockchainCompleted="unknown";
                            monitorService.LAST_UPDATE =qpsHelper.getTimestamp();
                            System.out.println("更新hou："+blockchainService.LAST_UPDATE);
                            System.out.println(Arrays.toString(monitorService.getTargets_all()));
                            JSONArray InstanceInfo = blockchainService.UpdateChainData(monitorService.getTargets_all());
                            System.out.println("上链数据："+InstanceInfo.toJSONString());
                            String Info= StringEscapeUtils.escapeJava(InstanceInfo.toJSONString());
                            String fabric_key=BlockchainInstanceMonitorController.blockchain_path+service+"."+ipaddr+"@@"+monitorService.getIp()+":"+ MonitorController.port;
                            JSONObject jsonObject=new JSONObject();
                            jsonObject.put("fabricKey",fabric_key);
                            jsonObject.put("fabricValue",Info);
                            jsonObject.put("clickTime",clickTime);
                            jsonObject.put("serviceTime",serviceTime.getTime());
                            jsonObject.put("service",service);
                            jsonObject.put("ipaddr",ipaddr);
                            jsonObject.put("MonitorIp",monitorService.getIp());
                            jsonObject.put("MonitorPort",MonitorController.port);
                            jsonObject.put("nums",nums);
                            jsonObject.put("serviceCompleted",seriviceCompleted);
                            Message message = new Message(jmsConfig.TOPIC, "blockchain", jsonObject.toJSONString().getBytes());
                            //发送
                            providerService.getProducer().sendOneway(message);

//                            String res=blockchainService.fabricPut(BlockchainInstanceMonitorController.blockchain_path+service+"."+ipaddr+"@@"+monitorService.getIp()+":"+ MonitorController.port,Info);
//                            System.out.println("上链数据："+res);
//                            System.out.println("更新："+blockchainService.LAST_UPDATE);
//                            Timestamp blockchainTime = new Timestamp(TimeUtil.currentTimeMillis());
//                            if(res.indexOf("success")!=-1){
//                                blockchainCompleted="success";
//                            }
//                            else blockchainCompleted="failed";
//                            RunableInsert runableInsert=new RunableInsert(clicktime,serviceTime,blockchainTime,clickTime,service,ipaddr,monitorService.getIp(),MonitorController.port,(int)(serviceTime.getTime()-clickTime),(int)(blockchainTime.getTime()-serviceTime.getTime()),(int)(blockchainTime.getTime()-clickTime),nums,seriviceCompleted,blockchainCompleted);
//                            Thread thread=new Thread(runableInsert);
//                            thread.start();
                        }
                    //要执行的业务代码，我们这里没有写方法，可以让线程休息几秒进行测试
                    }catch(Exception e) {
                             throw new RuntimeException("报错啦！！");
                        }
            }
        });
    }
}
