package springcloud.hellospringcloudalibabanacosmonitor.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import groovy.json.StringEscapeUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import springcloud.hellospringcloudalibabanacosmonitor.executor.MyExecutor;
import springcloud.hellospringcloudalibabanacosmonitor.qps.QpsHelper;
import springcloud.hellospringcloudalibabanacosmonitor.qps.TimeUtil;
import springcloud.hellospringcloudalibabanacosmonitor.service.BlockchainService;
import springcloud.hellospringcloudalibabanacosmonitor.service.MonitorService;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

@CrossOrigin
@RestController
@RequestMapping("/grafana/blockchain/instance/{service}")
public class BlockchainInstanceMonitorController extends MonitorController{
    String ip;
    String port;

    @Autowired
    private LoadBalancerClient loadBalancerClient;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private BlockchainService blockchainService;

    @Value("${spring.application.name}")
    private String appName;

    public BlockchainInstanceMonitorController() throws UnknownHostException {
    }
    @GetMapping(value = "getMonitoringData")
    public String getMonitoringData(@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception {
        String ServiceMonitoringdata=blockchainService.Monitor_InstanceByAll(service,ipaddr);
        return ServiceMonitoringdata;
    }

    @PostMapping(value = "/resetGrafanaSearchList")
    public String deleteMonitorSearchList() throws Exception {
        List MonitorKeys=new LinkedList();
        String MonitorList=blockchainService.getMonitorList();
        JSONArray jsonArray=JSONArray.parseArray(MonitorList);
        String res="";
        if(jsonArray!=null){
            for(int i=0;i<jsonArray.size();i++){
                System.out.println();
                MonitorKeys.add(jsonArray.getJSONObject(i).getString("Key"));
            }
        }
        for(int i=0;i<MonitorKeys.size();i++){
            res=res+blockchainService.fabricDelete((String) MonitorKeys.get(i));
        }
        return res;
    }

    @PostMapping(value = "/resetMonitor")
    public String deleteMonitor(@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception {
        String res1=deleteMonitorSearchList();
        String ServiceMonitoringdata=blockchainService.Monitor_InstanceByAll(service,ipaddr);
        List MonitorKeys=new LinkedList();
        JSONArray jsonArray=JSONArray.parseArray(ServiceMonitoringdata);
        String res="";
        if(jsonArray!=null){
            for(int i=0;i<jsonArray.size();i++){
                System.out.println();
                MonitorKeys.add(jsonArray.getJSONObject(i).getString("Key"));
            }
        }
        for(int i=0;i<MonitorKeys.size();i++){
            res=res+blockchainService.fabricDelete((String) MonitorKeys.get(i));
        }
        return res1+res;
    }

    @PostMapping(value = "/resetAll")
    public String deleteAll() throws Exception {
        String ServiceMonitoringdata=blockchainService.Monitor_ServiceByAll();
        List MonitorKeys=new LinkedList();
        JSONArray jsonArray=JSONArray.parseArray(ServiceMonitoringdata);
        String res="";
        if(jsonArray!=null){
            for(int i=0;i<jsonArray.size();i++){
                MonitorKeys.add(jsonArray.getJSONObject(i).getString("Key"));
            }
        }
        for(int i=0;i<MonitorKeys.size();i++){
            res=res+blockchainService.fabricDelete((String) MonitorKeys.get(i));
        }
        return res;
    }

    @GetMapping(value="/queryTest")
    public String queryTest()throws Exception {

        long clickTime = TimeUtil.currentTimeMillis();
        if(blockchainService.token==null) {
            blockchainService.fabricGetToken();
        }
        QpsHelper qpsHelper= QPSHelpers.entrySet().iterator().next().getValue();
        MonitorService monitorService= monitorServices.entrySet().iterator().next().getValue();
        String address=monitorServices.entrySet().iterator().next().getKey();
        String add[]=address.split("-");
        String ipaddr=add[1];
        String service=add[0];
            blockchainService.LAST_TEST=clickTime;
            try{
                //添加最近数据
                JSONArray InstanceInfo = blockchainService.getQueryResult(monitorService.targets_all,service,ipaddr,clickTime);
                System.out.println("距更新时间为："+ (TimeUtil.currentTimeMillis()-blockchainService.LAST_UPDATE));
                return InstanceInfo.toString();}
            catch (Exception e){
                return e.getMessage();
            }
    }

    @Override
    public String query(@RequestBody String requestBody,@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception {
        //查找是否有，如果没有新建一个实例
        long clickTime = TimeUtil.currentTimeMillis();
        String address=service+"-"+ipaddr;
        QpsHelper qpsHelper=null;
        MonitorService monitorService=null;
        if(QPSHelpers.get(address)!=null&&monitorServices.get(address)!=null) {
            if (blockchainService.token == null) {
                blockchainService.fabricGetToken();
            }
            qpsHelper = QPSHelpers.get(address);
            monitorService = monitorServices.get(address);
        }
        JSONObject jsonObject1 = JSON.parseObject(requestBody);
        String json=jsonObject1.getString("targets");
        JSONArray array = JSONArray.parseArray(json);
        String[] targets = new String[array.size()];
        for (int i=0;i<array.size();i++){
            targets[i]= array.getJSONObject(i).getString("target");
        }
        System.out.println("array is :"+ Arrays.toString(targets));

        //添加最近数据
//        if(clickTime>qpsHelper.getTimestamp()){
//            collectDataNow(service,ipaddr,qpsHelper,monitorService);
//            collectDataBlockchain(service,ipaddr,qpsHelper,monitorService);
//        }

        JSONArray InstanceInfo = blockchainService.getQueryResult(targets,service,ipaddr,clickTime);
        System.out.println("距更新时间为："+ (TimeUtil.currentTimeMillis()-blockchainService.LAST_UPDATE));
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
        qpsHelper=QPSHelpers.get(address);
        monitorService=monitorServices.get(address);
        JSONArray metrics = blockchainService.getSearchResult();
        return metrics.toString();
    }
}

