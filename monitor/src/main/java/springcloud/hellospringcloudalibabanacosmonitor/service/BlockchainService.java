package springcloud.hellospringcloudalibabanacosmonitor.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import groovy.json.StringEscapeUtils;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.PropertySource;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import springcloud.hellospringcloudalibabanacosmonitor.controller.MonitorController;
import springcloud.hellospringcloudalibabanacosmonitor.executor.MyExecutor;
import springcloud.hellospringcloudalibabanacosmonitor.qps.QpsHelper;
import springcloud.hellospringcloudalibabanacosmonitor.qps.RunableInsert;
import springcloud.hellospringcloudalibabanacosmonitor.qps.TimeUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author lhr
 */
@Service
@Repository
public class BlockchainService extends MonitorService{
    @Value(value = "${fabric.port}")
    private String port;
    //    private String port="4000";
//
    @Value(value = "${fabric.ipaddr}")
    private String host;
    //    private String host="127.0.0.1";
    public static long LAST_TEST=0;
    public static long TIME_INTERVAL=1000; //每隔多久上链时间
    public static String token;
    public List<Map<Long, Double>> QuertTimeInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> TPS_RequestInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> TPS_SuccessInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> TPS_ExceptionInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> RT_MinInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> RT_AvgInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> QPS_exceptionInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> QPS_successInfo = new LinkedList<Map<Long, Double>>();

    public BlockchainService() throws UnknownHostException {
    }


    public static void main(String[] args) throws Exception {
        BlockchainService blk=new BlockchainService();
        blk.fabricGetToken();
        System.out.println(blk.fabricQueryByKey("springcloud.monitor.provider.http://127.0.0.1:8088/hi@@127.0.1.1:8086"));
    }
    public void fabricGetToken() throws Exception {
        Map<String, String> headers = new HashMap<>(3);
        headers.put("content-type","application/x-www-form-urlencoded");
        HttpClient.HttpResult res = null;
        System.out.println(host+port);
        try{
            final String url = "http://"+host+":"+port+"/admins";
            String data="username=admin_cc_gfe&orgname=Gfe";
            res=HttpClient.fabricPostLarge(url,headers,data);
            JSONObject jsonObject = JSON.parseObject(res.content);
            token=jsonObject.getString("token");
            System.out.println(token);
        } catch (Exception e) {
            Loggers.RAFT.warn("Failed to request Enroll");
            throw e;
        }
//        token=res;
        Loggers.RAFT.info("token:"+token);
    }

    public String fabricPut(String key, String value){
        Map<String, String> headers = new HashMap<>(3);
        headers.put("authorization","Bearer "+token);
        headers.put("content-type","application/json");
        HttpClient.HttpResult res = null;
        try{

            final String url = "http://"+host+":"+port+"/channels/softwarechannel/chaincodes/monitor";
            String data="{" +
                    "\"peers\": [\"peer0.fabric.gfe.com\"],\n" +
                    "\t\"chaincodeName\":\"monitor\",\n" +
                    "\t\"chaincodeVersion\":\"v0\",\n" +
                    "\t\"chaincodeType\": \"go\",\n" +
                    "\t\"fcn\":\"Put\",\n" +
                    "\t\"args\":[\""+key+"\",\""+value+"\"]\n" +
                    "}";
            res=HttpClient.fabricPostLarge(url,headers,data);
        } catch (Exception e) {
            Loggers.RAFT.warn("waning: failed to put to fabric : {}",key+value);
            throw e;
        }
        return res.content;

    }

    public String fabricQueryByKey(String key) throws Exception {
        key=StringEscapeUtils.escapeJava(key);
        Map<String, String> headers = new HashMap<>(3);
        headers.put("authorization","Bearer "+token);
        headers.put("content-type","application/json");
        HttpClient.HttpResult res = null;
        try{

            final String url = "http://"+host+":"+port+"/channels/softwarechannel/chaincodes/monitor";
            String data="{" +
                    "\"peers\": [\"peer0.fabric.gfe.com\"],\n" +
                    "\t\"chaincodeName\":\"monitor\",\n" +
                    "\t\"chaincodeVersion\":\"v0\",\n" +
                    "\t\"chaincodeType\": \"go\",\n" +
                    "\t\"fcn\":\"QueryByKey\",\n" +
                    "\t\"args\":[\""+key+"\"]\n" +
                    "}";
            res=HttpClient.fabricPostLarge(url,headers,data);
        } catch (Exception e) {
            Loggers.RAFT.warn("waning: failed to queryByKey in fabric : {}",key);
            throw e;
        }
        return res.content;
    }

    public String fabricDelete(String key) throws Exception {
        Map<String, String> headers = new HashMap<>(3);
        headers.put("authorization","Bearer "+token);
        headers.put("content-type","application/json");
        HttpClient.HttpResult res = null;
        try{

            final String url = "http://"+host+":"+port+"/channels/softwarechannel/chaincodes/monitor";
            String data="{" +
                    "\"peers\": [\"peer0.fabric.gfe.com\"],\n" +
                    "\t\"chaincodeName\":\"monitor\",\n" +
                    "\t\"chaincodeVersion\":\"v0\",\n" +
                    "\t\"chaincodeType\": \"go\",\n" +
                    "\t\"fcn\":\"Delete\",\n" +
                    "\t\"args\":[\""+key+"\"]\n" +
                    "}";
            res=HttpClient.fabricPostLarge(url,headers,data);
        } catch (Exception e) {
            Loggers.RAFT.warn("waning: failed to delete data in  fabric : {}",key);
            throw e;
        }
        return res.content;
    }

    public String Monitor_InstanceByAll(String service,String ipaddr) throws Exception {
        Map<String, String> headers = new HashMap<>(3);
        headers.put("authorization","Bearer "+token);
        headers.put("content-type","application/json");
        HttpClient.HttpResult res = null;
        try{

            final String url = "http://"+host+":"+port+"/channels/softwarechannel/chaincodes/monitor";
            String data="{" +
                    "\"peers\": [\"peer0.fabric.gfe.com\"],\n" +
                    "\t\"chaincodeName\":\"monitor\",\n" +
                    "\t\"chaincodeVersion\":\"v0\",\n" +
                    "\t\"chaincodeType\": \"go\",\n" +
                    "\t\"fcn\":\"RangeQuery\",\n" +
                    "\t\"args\":[\"springcloud.monitor."+service+"."+ipaddr+"@@"+"\",\"springcloud.monitor."+service+"."+ipaddr+"@A"+"\"]\n" +
                    "}";
            res=HttpClient.fabricPostLarge(url,headers,data);
        } catch (Exception e) {
            Loggers.RAFT.warn("waning: failed to queryAlldata in  fabric : {}");
            throw e;
        }
        return res.content;
    }
    public String Monitor_ServiceByAll() throws Exception {
        Map<String, String> headers = new HashMap<>(3);
        headers.put("authorization","Bearer "+token);
        headers.put("content-type","application/json");
        HttpClient.HttpResult res = null;
        try{

            final String url = "http://"+host+":"+port+"/channels/softwarechannel/chaincodes/monitor";
            String data="{" +
                    "\"peers\": [\"peer0.fabric.gfe.com\"],\n" +
                    "\t\"chaincodeName\":\"monitor\",\n" +
                    "\t\"chaincodeVersion\":\"v0\",\n" +
                    "\t\"chaincodeType\": \"go\",\n" +
                    "\t\"fcn\":\"RangeQuery\",\n" +
                    "\t\"args\":[\"springcloud.monitor.\",\"springcloud.monitor0\"]\n" +
                    "}";
            res=HttpClient.fabricPostLarge(url,headers,data);
        } catch (Exception e) {
            Loggers.RAFT.warn("waning: failed to queryAlldata in  fabric : {}");
            throw e;
        }
        return res.content;
    }

    public String getMonitorList() throws Exception {
        Map<String, String> headers = new HashMap<>(3);
        headers.put("authorization","Bearer "+token);
        headers.put("content-type","application/json");
        HttpClient.HttpResult res = null;
        try{

            final String url = "http://"+host+":"+port+"/channels/softwarechannel/chaincodes/monitor";
            String data="{" +
                    "\"peers\": [\"peer0.fabric.gfe.com\"],\n" +
                    "\t\"chaincodeName\":\"monitor\",\n" +
                    "\t\"chaincodeVersion\":\"v0\",\n" +
                    "\t\"chaincodeType\": \"go\",\n" +
                    "\t\"fcn\":\"RangeQuery\",\n" +
                    "\t\"args\":[\"springcloud.monitorlist.\",\"springcloud.monitorlist0\"]\n" +
                    "}";
            res=HttpClient.fabricPostLarge(url,headers,data);
        } catch (Exception e) {
            Loggers.RAFT.warn("waning: failed to queryAlldata in  fabric : {}");
            throw e;
        }
        return res.content;
    }
    public static String execCurl(String[] cmds) {
        ProcessBuilder process = new ProcessBuilder(cmds);
        Process p;
        int runningStatus = 0;
        try {
            p = process.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()));
            BufferedReader stdError = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            StringBuilder builder = new StringBuilder();
            String line = null;
            String s=null;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
                builder.append(System.getProperty("line.separator"));

            }
            while ((s= stdError.readLine()) != null) {
                Loggers.RAFT.error(s);
            }
            try {
                runningStatus = p.waitFor();
            } catch (InterruptedException e) {
            }

            return builder.toString();

        } catch (IOException e) {
            System.out.print("error");
            e.printStackTrace();
        }
        return null;

    }
    public String Monitor_InstanceByLocal(String serviceName, String instanceHost) throws Exception {
        return fabricQueryByKey(StringEscapeUtils.escapeJava(MonitorController.blockchain_path+serviceName+"."+instanceHost+"@@"+ia.getHostAddress()+":"+getIp()));

    }

    public void collectQueryTimeInfo(long tm,double value) {
        if (QuertTimeInfo.size() > MAX_SIZE) {
            QuertTimeInfo.remove(0);
        }
        if(QuertTimeInfo.size()>0){
            for(long key : QuertTimeInfo.get(QuertTimeInfo.size()-1).keySet()){
                if (key==tm){
                    QuertTimeInfo.get(QuertTimeInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    QuertTimeInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            QuertTimeInfo.add(map);
        }
    }
    public JSONArray getQueryResult(String[] targets,String service,String ipaddr,long clickTime) throws Exception {
        String ServiceMonitoringdata=Monitor_InstanceByAll(service,ipaddr);
        JSONArray jsonArray=JSONArray.parseArray(ServiceMonitoringdata);
        System.out.println("MOnitoring data"+jsonArray.toJSONString());
        HashMap<String, JSONObject> map = new HashMap<String,JSONObject>();
        for(int i=0;i<jsonArray.size();i++){
            JSONObject jsondata=jsonArray.getJSONObject(i);
            String[] a=jsondata.get("Key").toString().split("@@");
            JSONArray instanceDataArray= JSONArray.parseArray(jsondata.get("Record").toString());
            for(int r=0;r<instanceDataArray.size();r++){
                String key=instanceDataArray.getJSONObject(r).getString("target");
                map.put(key,instanceDataArray.getJSONObject(r));
            }
        }
        //测试时间
        JSONObject timeInfo = new JSONObject();
        //添加target
        timeInfo.put(TARGET,"QueryTime");
        //添加datapoints
        collectQueryTimeInfo(clickTime,TimeUtil.currentTimeMillis()-LAST_UPDATE);


        //插入mysql以便于统计
//        Timestamp scurrtest = new Timestamp(clickTime);
//        RunableInsert runableInsert=new RunableInsert(scurrtest,getIp(),MonitorController.port,(int)(TimeUtil.currentTimeMillis()-LAST_UPDATE));
//        Thread thread=new Thread(runableInsert);
//        thread.start();
        //


        timeInfo.put(DATA_POINTS, getDatapoints(QuertTimeInfo));
        map.put("QueryTime",timeInfo);
        JSONArray Info = new JSONArray();
        for(int i=0;i<targets.length;i++){
            Info.add(map.get(targets[i]));
        }
        System.out.println(targets.toString());
        return Info;
    }
    private static String encodeFileName(String fileName) {
        return fileName.replace(':', '#');
    }

    public JSONArray getSearchResult() throws Exception {
        List ipList=new LinkedList();
        JSONArray searchResult = new JSONArray();
        String MonitorList=getMonitorList();
        searchResult.add("QueryTime");
        try{
            System.out.println(MonitorList);
            JSONArray jsonArray=JSONArray.parseArray(MonitorList);
            if(jsonArray!=null){
                for(int i=0;i<jsonArray.size();i++){
                    System.out.println();
                    ipList.add(jsonArray.getJSONObject(i).getJSONObject("Record").getString("monitorHost"));
                }
            }
            for(int i=0;i<ipList.size();i++){
                String ipaddr=ipList.get(i).toString();
                searchResult.add(ipaddr+"-"+TPS_Request);
                searchResult.add(ipaddr+"-"+TPS_Success);
                searchResult.add(ipaddr+"-"+TPS_Exception);
                searchResult.add(ipaddr+"-"+RT_Min);
                searchResult.add(ipaddr+"-"+RT_Avg);
                searchResult.add(ipaddr+"-"+QPS_exception);
                searchResult.add(ipaddr+"-"+QPS_success);
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return searchResult;
    }
    public JSONArray UpdateChainData(String[] targets) {
        System.out.println("targets："+Arrays.toString(targets));
        JSONArray Info = BlockchaintimeSeriesFormat(targets);
        System.out.println("info:"+Info);
        return Info;
    }
    public JSONArray BlockchaintimeSeriesFormat(String[] targets) {
        JSONArray timeSeriesResult = new JSONArray();
        HashMap<String, List> map = new HashMap<String,List>();

        //数据抽样
//        map.put(getIp()+":"+MonitorController.port+"-"+TPS_Request, getRandomData(TPS_RequestInfo,TPS_RequestInfo.size(),TPS_RequestInfo.size()/3+1));
//        map.put(getIp()+":"+MonitorController.port+"-"+TPS_Success, getRandomData(TPS_SuccessInfo,TPS_SuccessInfo.size(),TPS_SuccessInfo.size()/3+1));
//        map.put(getIp()+":"+MonitorController.port+"-"+TPS_Exception, getRandomData(TPS_ExceptionInfo,TPS_ExceptionInfo.size(),TPS_ExceptionInfo.size()/3+1));
//        map.put(getIp()+":"+MonitorController.port+"-"+RT_Min, getRandomData(RT_MinInfo,RT_MinInfo.size(),RT_MinInfo.size()/3+1));
//        map.put(getIp()+":"+MonitorController.port+"-"+RT_Avg, getRandomData(RT_AvgInfo,RT_AvgInfo.size(),RT_AvgInfo.size()/3+1));
//        map.put(getIp()+":"+MonitorController.port+"-"+QPS_exception, getRandomData(QPS_exceptionInfo,QPS_exceptionInfo.size(),QPS_exceptionInfo.size()/3+1));
//        map.put(getIp()+":"+MonitorController.port+"-"+QPS_success, getRandomData(QPS_successInfo,QPS_successInfo.size(),QPS_successInfo.size()/3+1));


        //全部上链
        map.put(getIp()+":"+MonitorController.port+"-"+TPS_Request, TPS_RequestInfo);
        map.put(getIp()+":"+MonitorController.port+"-"+TPS_Success, TPS_SuccessInfo);
        map.put(getIp()+":"+MonitorController.port+"-"+TPS_Exception, TPS_ExceptionInfo);
        map.put(getIp()+":"+MonitorController.port+"-"+RT_Min, RT_MinInfo);
        map.put(getIp()+":"+MonitorController.port+"-"+RT_Avg, RT_AvgInfo);
        map.put(getIp()+":"+MonitorController.port+"-"+QPS_exception, QPS_exceptionInfo);
        map.put(getIp()+":"+MonitorController.port+"-"+QPS_success, QPS_successInfo);
        System.out.println("上链map数据为："+map.toString());
        System.out.println("接受到targets："+Arrays.toString(targets));
        for (int i = 0; i < targets.length; i++) {
            JSONObject Info = new JSONObject();
            //添加target
            Info.put(TARGET, targets[i]);
            //添加datapoints
            Info.put(DATA_POINTS, getBlockchainDatapoints(map.get(targets[i])));
            //添加到timeSeries
            timeSeriesResult.add(Info);
        }
        return timeSeriesResult;
    }
    public JSONArray getBlockchainDatapoints(List<Map<Long, Double>> UtilInfo){
        JSONArray datapoints = new JSONArray();
        if(UtilInfo.size()!=0){
            for (Map<Long, Double> tempInfo : UtilInfo) {
                for (Map.Entry<Long, Double> entry : tempInfo.entrySet()) {
                    JSONArray tempArray = new JSONArray();
                    tempArray.add(entry.getValue());
                    tempArray.add(entry.getKey());
                    datapoints.add(tempArray);
                }
            }
        }
        return datapoints;
    }

    public void collectTPS_RequestInfo(long tm,double value) {
        if (TPS_RequestInfo.size() > MAX_SIZE) {
            TPS_RequestInfo.remove(0);
//            if(TURN_TIMES==0)coll
//            TURN_TIMES=(TURN_TIMES+1)%MAX_SIZE;

        }
        if(TPS_RequestInfo.size()>0){
            for(long key : TPS_RequestInfo.get(TPS_RequestInfo.size()-1).keySet()){
                if (key==tm){
                    TPS_RequestInfo.get(TPS_RequestInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    TPS_RequestInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            TPS_RequestInfo.add(map);
        }
    }

    public void collectTPS_SuccessInfo(long tm,double value) {
        if (TPS_SuccessInfo.size() > MAX_SIZE) {
            TPS_SuccessInfo.remove(0);
        }
        if(TPS_SuccessInfo.size()>0){
            for(long key : TPS_SuccessInfo.get(TPS_SuccessInfo.size()-1).keySet()){
                if (key==tm){
                    TPS_SuccessInfo.get(TPS_SuccessInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    TPS_SuccessInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            TPS_SuccessInfo.add(map);
        }
    }

    public void collectTPS_ExceptionInfo(long tm,double value) {
        if (TPS_ExceptionInfo.size() > MAX_SIZE) {
            TPS_ExceptionInfo.remove(0);
        }
        if(TPS_ExceptionInfo.size()>0){
            for(long key : TPS_ExceptionInfo.get(TPS_ExceptionInfo.size()-1).keySet()){
                if (key==tm){
                    TPS_ExceptionInfo.get(TPS_ExceptionInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    TPS_ExceptionInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            TPS_ExceptionInfo.add(map);
        }
    }

    public void collectRT_MinInfo(long tm,double value) {
        if (RT_MinInfo.size() > MAX_SIZE) {
            RT_MinInfo.remove(0);
        }
        if(RT_MinInfo.size()>0){
            for(long key : RT_MinInfo.get(RT_MinInfo.size()-1).keySet()){
                if (key==tm){
                    RT_MinInfo.get(RT_MinInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    RT_MinInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            RT_MinInfo.add(map);
        }
    }

    public void collectRT_AvgInfo(long tm,double value) {
        if (RT_AvgInfo.size() > MAX_SIZE) {
            RT_AvgInfo.remove(0);
        }
        if(RT_AvgInfo.size()>0){
            for(long key : RT_AvgInfo.get(RT_AvgInfo.size()-1).keySet()){
                if (key==tm){
                    RT_AvgInfo.get(RT_AvgInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    RT_AvgInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            RT_AvgInfo.add(map);
        }
    }

    public void collectQPS_exceptionInfo(long tm,double value) {
        if (QPS_exceptionInfo.size() > MAX_SIZE) {
            QPS_exceptionInfo.remove(0);
        }
        if(QPS_exceptionInfo.size()>0){
            for(long key : QPS_exceptionInfo.get(QPS_exceptionInfo.size()-1).keySet()){
                if (key==tm){
                    QPS_exceptionInfo.get(QPS_exceptionInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    QPS_exceptionInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            QPS_exceptionInfo.add(map);
        }
    }

    public void collectQPS_successInfo(long tm,double value) {
        if (QPS_successInfo.size() > MAX_SIZE) {
            QPS_successInfo.remove(0);
        }
        if (QPS_successInfo.size() > 0) {
            for (long key : QPS_successInfo.get(QPS_successInfo.size() - 1).keySet()) {
                if (key == tm) {
                    QPS_successInfo.get(QPS_successInfo.size() - 1).put(tm, value);
                } else {
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm, value);
                    QPS_successInfo.add(map);
                }
            }
        } else {
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm, value);
            QPS_successInfo.add(map);
        }

    }
    public static class ListSplit<T> {

        public List<List<T>> split(List<T> sList, int num) {
            List<List<T>> eList = new ArrayList<List<T>>();
            List<T> gList;
            int size = (sList.size()) / num;
            int size2 = (sList.size()) % num;
            int j = 0;
            int xx = 0;
            for (int i = 0; i < num; i++) {
                gList = new ArrayList<T>();

                for (j = xx; j < (size + xx); j++) {
                    gList.add(sList.get(j));
                }
                xx = j;
                eList.add(gList);
            }
            if (size2 != 0) {
                gList = new ArrayList<T>();
                for (int y = 1; y < size2 + 1; y++) {
                    gList.add(sList.get(sList.size() - y));
                }
                eList.add(gList);
            }
            return eList;
        }
    }
    private static List<Integer> getRandomId(int min,int items,int max){
        List<Integer> ids = Lists.newArrayList();
        while(ids.size()<items){
            int randomId = ThreadLocalRandom.current().nextInt(max)+min;
            if(!ids.contains(randomId))ids.add(randomId);
        }
        ids.sort((x,y)->x-y);
        return ids;
    }

    public  List<Map<Long, Double>> getRandomData(List<Map<Long, Double>> srcDatas,int sampleTotal,int splitCopies){
        if(splitCopies<=0)splitCopies = 1;
        int items = sampleTotal/splitCopies; //从每份中抽取的数据,数据总数将等于sampleTotal
        List<Map<Long, Double>> filterRes = Lists.newArrayList();//用于保存最终的抽样结果
        ListSplit<Map<Long, Double>> listSplit = new ListSplit<>();//对list做拆分算法,源码:https://my.oschina.net/u/2391658/blog/703032

        List<List<Map<Long, Double>>>splitRes = listSplit.split(srcDatas,splitCopies);

        int preListSize = 0;//初始化第一份List的最小下标
        for(int i=0;i<splitCopies;i++){
            List<Map<Long, Double>> listBlock = splitRes.get(i);//取出拆分后的list单元
            System.out.println(preListSize+"-->"+(preListSize+listBlock.size()-1));
            List<Integer> ids = getRandomId(preListSize,items,listBlock.size());//取到排序后的抽样数据id

            System.out.println(Arrays.toString(ids.toArray()));
            ids.forEach(id->filterRes.add(srcDatas.get(id)));//取出list 下标id对应的值
            preListSize = preListSize+listBlock.size(); //重新初始化list的最小下标
        }
        System.out.println("抽样结果:");
        System.out.println(Arrays.toString(filterRes.toArray()));
        return filterRes;
    }
}
