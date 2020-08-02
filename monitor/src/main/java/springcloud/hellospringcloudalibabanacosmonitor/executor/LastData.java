package springcloud.hellospringcloudalibabanacosmonitor.executor;

import com.alibaba.fastjson.JSONArray;
import groovy.json.StringEscapeUtils;
import springcloud.hellospringcloudalibabanacosmonitor.controller.BlockchainInstanceMonitorController;
import springcloud.hellospringcloudalibabanacosmonitor.controller.MonitorController;
import springcloud.hellospringcloudalibabanacosmonitor.qps.QpsHelper;
import springcloud.hellospringcloudalibabanacosmonitor.qps.RunableInsert;
import springcloud.hellospringcloudalibabanacosmonitor.qps.TimeUtil;
import springcloud.hellospringcloudalibabanacosmonitor.service.BlockchainService;
import springcloud.hellospringcloudalibabanacosmonitor.service.MonitorService;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LastData {
    private ExecutorService executor = Executors.newCachedThreadPool() ;
    private QpsHelper qpsHelper;
    private MonitorService monitorService;
    private String service;
    private String ipaddr;
    private BlockchainService blockchainService;
    private long clickTime;
    private String seriviceCompleted;
    public LastData(String service, String ipaddr, QpsHelper qpsHelper, MonitorService monitorService, BlockchainService blockchainService, long clickTime, String serviceCompleted){
        this.service=service;
        this.ipaddr=ipaddr;
        this.qpsHelper=qpsHelper;
        this.monitorService=monitorService;
        this.blockchainService=blockchainService;
        this.clickTime=clickTime;
        this.seriviceCompleted=serviceCompleted;
    }
    public void fun() throws Exception {
        executor.submit(new Runnable(){
            @Override
             public void run() {
                    try {
                        lastUpdate();
                    //要执行的业务代码，我们这里没有写方法，可以让线程休息几秒进行测试
                    }catch(Exception e) {
                             throw new RuntimeException("报错啦！！");
                        }
            }
            public synchronized  void lastUpdate() throws InterruptedException {
                Timestamp serviceTime = new Timestamp(TimeUtil.currentTimeMillis());
                Thread.sleep(1000L);
                if(TimeUtil.currentTimeMillis()-monitorService.LAST_UPDATE>=1000) {
                    System.out.println("最后一次"+TimeUtil.currentTimeMillis());
                    qpsHelper.setTimestamp(qpsHelper.getTimeInMills());
                    boolean lastUpdated=false;
                    blockchainService.collectTPS_RequestInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalRequest());
                    blockchainService.collectTPS_ExceptionInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalException());
                    blockchainService.collectTPS_SuccessInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalSuccess());
                    blockchainService.collectRT_MinInfo(qpsHelper.getTimestamp(), qpsHelper.getMinRt());
                    blockchainService.collectRT_AvgInfo(qpsHelper.getTimestamp(), qpsHelper.getAvgRt());
                    blockchainService.collectQPS_successInfo(qpsHelper.getTimestamp(), qpsHelper.getSuccessQps());
                    blockchainService.collectQPS_exceptionInfo(qpsHelper.getTimestamp(), qpsHelper.getExceptionQps());
                    System.out.println("此次数据上链");
                    //                }else{
                    //                    System.out.println("此次数据不上链");
                    //                }
                    monitorService.collectTPS_RequestInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalRequest());
                    monitorService.collectTPS_ExceptionInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalException());
                    monitorService.collectTPS_SuccessInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalSuccess());
                    monitorService.collectRT_MinInfo(qpsHelper.getTimestamp(), qpsHelper.getMinRt());
                    monitorService.collectRT_AvgInfo(qpsHelper.getTimestamp(), qpsHelper.getAvgRt());
                    monitorService.collectQPS_successInfo(qpsHelper.getTimestamp(), qpsHelper.getSuccessQps());
                    monitorService.collectQPS_exceptionInfo(qpsHelper.getTimestamp(), qpsHelper.getExceptionQps());
                    Timestamp clicktime = new Timestamp(clickTime);
                    String blockchainCompleted="unknown";
                    monitorService.LAST_UPDATE =qpsHelper.getTimestamp();
                    System.out.println("更新hou："+monitorService.LAST_UPDATE);
                    System.out.println(Arrays.toString(monitorService.getTargets_all()));
                    JSONArray InstanceInfo = blockchainService.UpdateChainData(monitorService.getTargets_all());
                    System.out.println("上链数据："+InstanceInfo.toJSONString());
                    String Info= StringEscapeUtils.escapeJava(InstanceInfo.toJSONString());
                    String res=blockchainService.fabricPut(BlockchainInstanceMonitorController.blockchain_path+service+"."+ipaddr+"@@"+monitorService.getIp()+":"+ MonitorController.port,Info);
                    System.out.println("上链数据："+res);
                    System.out.println("更新："+blockchainService.LAST_UPDATE);
                    Timestamp blockchainTime = new Timestamp(TimeUtil.currentTimeMillis());
                    if(res.indexOf("success")!=-1){
                        blockchainCompleted="lastsuccess";
                    }
                    else blockchainCompleted="failed";
                    RunableInsert runableInsert=new RunableInsert(clicktime,serviceTime,blockchainTime,clickTime,service,ipaddr,monitorService.getIp(),MonitorController.port,(int)(serviceTime.getTime()-clickTime),(int)(blockchainTime.getTime()-serviceTime.getTime()),(int)(blockchainTime.getTime()-clickTime),(int)qpsHelper.getTotalRequest(),seriviceCompleted,blockchainCompleted);
                    Thread thread=new Thread(runableInsert);
                    thread.start();
                }

            };
        });
    }
}
