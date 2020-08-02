package springcloud.hellospringcloudalibabanacosmonitor.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import groovy.json.StringEscapeUtils;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.client.RestTemplate;
import springcloud.hellospringcloudalibabanacosmonitor.executor.LastData;
import springcloud.hellospringcloudalibabanacosmonitor.executor.MyExecutor;
import springcloud.hellospringcloudalibabanacosmonitor.qps.QpsHelper;
import springcloud.hellospringcloudalibabanacosmonitor.qps.RunableInsert;
import springcloud.hellospringcloudalibabanacosmonitor.qps.TimeUtil;
import springcloud.hellospringcloudalibabanacosmonitor.service.BlockchainService;
import springcloud.hellospringcloudalibabanacosmonitor.service.MonitorService;
import springcloud.hellospringcloudalibabanacosmonitor.service.ProviderService;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.TimeUnit;

@CrossOrigin
@EnableAsync          //开启异步执行
@RestController
@RequestMapping("/grafana/{service}")
public class MonitorController implements GrafanaController{
    String ip;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ProviderService providerService;

    @Autowired
    private BlockchainService blockchainService;
    public static long updateTime;
    public static String port;
    public static boolean hasDatatoBlockchain=false;
    private static final OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .connectionPool(new ConnectionPool(500, 5L, TimeUnit.MINUTES))
            .connectTimeout(10, TimeUnit.SECONDS).readTimeout(10, TimeUnit.SECONDS).writeTimeout(10, TimeUnit.SECONDS).build();

    @Value("${server.port}")
    public void setPort(String p){
        port=p;
    }
    @Value("${spring.application.name}")
    private String appName;
    private boolean updateMonitorList=false;

    public void setUpdateMonitorList(boolean updateMonitorList) {
        this.updateMonitorList = updateMonitorList;
    }
    public boolean getupdateMonitorList() {
        return updateMonitorList;
    }
    public static final String blockchain_path = "springcloud.monitor.";
    public static final String blockchain_monitor_path = "springcloud.monitorlist.";
    public static HashMap<String, QpsHelper> QPSHelpers = new HashMap<String, QpsHelper>();
    public static HashMap<String, MonitorService> monitorServices = new HashMap<String, MonitorService>();

    public MonitorController() {
    }

    @Override
    public String test(@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception {
        String address=service+"-"+ipaddr;
        QpsHelper qpsHelper = null;
        MonitorService monitorService;
        if(QPSHelpers.get(address)==null||monitorServices.get(address)==null){
            qpsHelper = new QpsHelper();
            monitorService=new MonitorService();
            QPSHelpers.put(address,qpsHelper);
            monitorServices.put(address,monitorService);
        }
        if(blockchainService.token==null) {
            blockchainService.fabricGetToken();
        }
        qpsHelper=QPSHelpers.get(address);
        monitorService=monitorServices.get(address);
        JSONObject connection = monitorService.testConnection();
        return connection.toString();
    }

    @Override
    public String query(@RequestBody String requestBody,@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception {
        //查找是否有，如果没有新建一个实例
        String address=service+"-"+ipaddr;
        QpsHelper qpsHelper ;
        MonitorService monitorService;
        if(QPSHelpers.get(address)==null||monitorServices.get(address)==null){
            qpsHelper = new QpsHelper();
            monitorService=new MonitorService();
            QPSHelpers.put(address,qpsHelper);
            monitorServices.put(address,monitorService);
        }
        if(blockchainService.token==null) {
            blockchainService.fabricGetToken();
        }
        qpsHelper=QPSHelpers.get(address);
        monitorService=monitorServices.get(address);

        JSONObject jsonObject1 = JSON.parseObject(requestBody);
        String json=jsonObject1.getString("targets");
        JSONArray array = JSONArray.parseArray(json);
        String[] targets = new String[array.size()];
        for (int i=0;i<array.size();i++){
            targets[i]= array.getJSONObject(i).getString("target");
        }
        System.out.println("array is :"+ Arrays.toString(targets));

        //添加最近数据
        if(TimeUtil.currentTimeMillis()>qpsHelper.getTimestamp()){
            collectDataNow(service,ipaddr,qpsHelper,monitorService);
        }
        JSONArray InstanceInfo = monitorService.getQueryResult(targets);
        return InstanceInfo.toString();
    }

    @Override
    public String search(@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception {
        System.out.println("service:"+service);
        System.out.println("ipaddr:"+ipaddr);
        String address=service+"-"+ipaddr;
        QpsHelper qpsHelper = null;
        MonitorService monitorService;
        if(QPSHelpers.get(address)==null||monitorServices.get(address)==null){
            qpsHelper = new QpsHelper();
            monitorService=new MonitorService();
            QPSHelpers.put(address,qpsHelper);
            monitorServices.put(address,monitorService);
        }
        if(blockchainService.token==null) {
            blockchainService.fabricGetToken();
        }
        System.out.println(getupdateMonitorList());
        if(getupdateMonitorList()==false) {
            JSONObject monitor = new JSONObject();
            monitor.put("monitorHost", blockchainService.getIp() + ":" + MonitorController.port);
            String monitor_list = blockchainService.fabricPut(BlockchainInstanceMonitorController.blockchain_monitor_path + blockchainService.getIp() + ":" + MonitorController.port, monitor.toJSONString());
            System.out.println(monitor_list);
            setUpdateMonitorList(true);
        }
        qpsHelper=QPSHelpers.get(address);
        monitorService=monitorServices.get(address);
        JSONArray metrics = monitorService.getSearchResult();
        return metrics.toString();
    }

    @Override
    public String annotations(String requestBody,@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws UnknownHostException {
        String address=service+"-"+ipaddr;
        QpsHelper qpsHelper = null;
        MonitorService monitorService;
        if(QPSHelpers.get(address)==null||monitorServices.get(address)==null){
            qpsHelper = new QpsHelper();
            monitorService=new MonitorService();
            QPSHelpers.put(address,qpsHelper);
            monitorServices.put(address,monitorService);
        }
        qpsHelper=QPSHelpers.get(address);
        monitorService=monitorServices.get(address);
        JSONObject annotations = monitorService.getAnnotationResult();
        return annotations.toString();
    }

    @RequestMapping(value = "/tag-keys", method = {RequestMethod.POST})
    @ResponseBody String tagKeys(@RequestBody String requestBody){

        String keys = "[\n" +
                "    {\"type\":\"string\",\"text\":\"City\"},\n" +
                "    {\"type\":\"string\",\"text\":\"Country\"}\n" +
                "]";
        return keys;
    }

    public void  collectDataHighPerformance(String service,String ipaddr,QpsHelper qpsHelper,MonitorService monitorService,long clickTime,String serviceCompleted) throws Exception {
        //误差1s左右，高性能监控
        //json发送至监控节点
        if(qpsHelper.getTimeInMills()-qpsHelper.getTimestamp()>= MonitorService.TIME_COLLECT_INTERVAL){
            //保存上一秒
            if(qpsHelper.getTimestamp()!=0) {
//                Random rand =new Random();
//                int i=rand.nextInt(3);
//                System.out.println("随机数字为："+i);
//                if(i!=0){

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
                MyExecutor myExecutor = new MyExecutor(service,ipaddr,qpsHelper,monitorService,blockchainService,clickTime,serviceCompleted, (int) qpsHelper.getTotalRequest(),providerService);
                myExecutor.fun();
            }
            //重置
//            updateTime=qpsHelper.getTimeInMills();
            qpsHelper.setTimestamp(qpsHelper.getTimeInMills());
            qpsHelper.setTotalRequest((double)qpsHelper.totalRequestInSec());
            qpsHelper.setTotalSuccess((double)qpsHelper.totalSuccessInSec());
            qpsHelper.setTotalException((double)qpsHelper.totalExceptionInSec());
            qpsHelper.setMinRt(qpsHelper.minRtInSec());
            qpsHelper.setAvgRt(qpsHelper.avgRtInSec());
            qpsHelper.setExceptionQps(qpsHelper.exceptionQps());
            qpsHelper.setSuccessQps(qpsHelper.successQps());
//            LastData lastData=new LastData(service,ipaddr,qpsHelper,monitorService,blockchainService,clickTime,serviceCompleted);
//            lastData.fun();
        }
        else {
//            updateTime=qpsHelper.getTimeInMills();
            qpsHelper.setTotalRequest((double)qpsHelper.totalRequestInSec());
            qpsHelper.setTotalSuccess((double)qpsHelper.totalSuccessInSec());
            qpsHelper.setTotalException((double)qpsHelper.totalExceptionInSec());
            qpsHelper.setMinRt(qpsHelper.minRtInSec());
            qpsHelper.setAvgRt(qpsHelper.avgRtInSec());
            qpsHelper.setExceptionQps(qpsHelper.exceptionQps());
            qpsHelper.setSuccessQps(qpsHelper.successQps());
        }
    }

    public void collectDataBlockchain(String service,String ipaddr,QpsHelper qpsHelper,MonitorService monitorService) throws Exception {
        //秒级监控&即时监控
        try{
            blockchainService.LAST_UPDATE=qpsHelper.getTimeInMills();
            System.out.println("更新hou："+blockchainService.LAST_UPDATE);
            System.out.println(Arrays.toString(monitorService.getTargets_all()));
            JSONArray InstanceInfo = monitorService.getQueryResult(monitorService.getTargets_all());
            System.out.println("上链数据："+InstanceInfo.toJSONString());
            String Info=StringEscapeUtils.escapeJava(InstanceInfo.toJSONString());
            String res=blockchainService.fabricPut(blockchain_path+service+"."+ipaddr+"@@"+monitorService.getIp()+":"+port,Info);
            System.out.println("上链数据："+res);
            System.out.println("更新："+blockchainService.LAST_UPDATE);

        }catch (Exception e){
            e.printStackTrace();
        }

    }

    public void collectDataNow(String service,String ipaddr,QpsHelper qpsHelper,MonitorService monitorService) throws Exception {
        //秒级监控&即时监控
//        updateTime=qpsHelper.getTimestamp();
        monitorService.collectTPS_RequestInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalRequest());
        monitorService.collectTPS_ExceptionInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalException());
        monitorService.collectTPS_SuccessInfo(qpsHelper.getTimestamp(), qpsHelper.getTotalSuccess());
        monitorService.collectRT_MinInfo(qpsHelper.getTimestamp(), qpsHelper.getMinRt());
        monitorService.collectRT_AvgInfo(qpsHelper.getTimestamp(), qpsHelper.getAvgRt());
        monitorService.collectQPS_successInfo(qpsHelper.getTimestamp(), qpsHelper.getSuccessQps());
        monitorService.collectQPS_exceptionInfo(qpsHelper.getTimestamp(), qpsHelper.getExceptionQps());
    }




    @GetMapping(value = "/run")
    public String Monitor(@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception {
        //使用 LoadBalanceClient 和 RestTemplate 结合的方式来访问
        long clickTime = TimeUtil.currentTimeMillis();
        String address=service+"-"+ipaddr;
        QpsHelper qpsHelper = null;
        MonitorService monitorService;
        if(QPSHelpers.get(address)==null||monitorServices.get(address)==null){
            qpsHelper = new QpsHelper();
            monitorService=new MonitorService();
            QPSHelpers.put(address,qpsHelper);
            monitorServices.put(address,monitorService);
        }
        qpsHelper=QPSHelpers.get(address);
        monitorService=monitorServices.get(address);
        if(blockchainService.token==null) {
            blockchainService.fabricGetToken();
        }
        System.out.println(getupdateMonitorList());
        String res;
        if(getupdateMonitorList()==false) {
            JSONObject monitor = new JSONObject();
            monitor.put("monitorHost", blockchainService.getIp() + ":" + MonitorController.port);
            String monitor_list = blockchainService.fabricPut(BlockchainInstanceMonitorController.blockchain_monitor_path + blockchainService.getIp() + ":" + MonitorController.port,  StringEscapeUtils.escapeJava(monitor.toJSONString()));
            System.out.println(monitor_list);
            setUpdateMonitorList(true);
        }
        try{
            long startTime = TimeUtil.currentTimeMillis();
            // 业务逻辑
//            ServiceInstance serviceInstance = loadBalancerClient.choose("provider");
//            String url = String.format("http://%s:%s/echo/%s", serviceInstance.getHost(), serviceInstance.getPort(), appName);
//            client.dispatcher().setMaxRequestsPerHost(20000);
//            client.dispatcher().setMaxRequests(200000);
            Request request = new Request.Builder().url(ipaddr).build();
            Response response = okHttpClient.newCall(request).execute();
            if (response.isSuccessful()) {
                res= response.body().string();
            } else {
                throw new IOException("Unexpected code " + response);
            }            // 计算耗时
            long rt = TimeUtil.currentTimeMillis() - startTime;
            qpsHelper.incrSuccess(rt);
            qpsHelper.printData();
            collectDataHighPerformance(service,ipaddr,qpsHelper,monitorService,clickTime,"success");
//            collectDataBlockchain(service,ipaddr,qpsHelper,monitorService);
//            持久化存储
//            qpsHelper.setResource(serviceInstance.getServiceId());
//            qpsHelper.WriteData(appName);

            return res;
        }catch (Exception e){
            qpsHelper.incrException();
            qpsHelper.printData();
            collectDataHighPerformance(service,ipaddr,qpsHelper,monitorService,clickTime,"failed");
            System.out.println(e);
//            持久化存储
//            qpsHelper.WriteData(appName);
            return "failed";
        }
    }


}

