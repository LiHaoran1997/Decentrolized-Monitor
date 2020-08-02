package springcloud.hellospringcloudalibabanacosmonitor.service;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.google.common.collect.Maps;
import groovy.util.logging.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import springcloud.hellospringcloudalibabanacosmonitor.controller.MonitorController;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Service
@Slf4j
public class MonitorService extends GrafanaService{
    private long timestamp=0;
    public static final String TPS_Request = "totalRequestInSec";
    public static final String TPS_Success = "totalSuccessInSec";
    public static final String TPS_Exception = "totalExceptionInSec";
    public static final String RT_Min = "minRtInSec";
    public static final String RT_Avg = "avgRtInSec";
    public static final String QPS_exception = "exceptionQps";
    public static final String QPS_success = "successQps";
    public static final int TIME_COLLECT_INTERVAL = 1000;
    public static int MAX_SIZE = 100;
    public List<Map<Long, Double>> TPS_RequestInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> TPS_SuccessInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> TPS_ExceptionInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> RT_MinInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> RT_AvgInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> QPS_exceptionInfo = new LinkedList<Map<Long, Double>>();
    public List<Map<Long, Double>> QPS_successInfo = new LinkedList<Map<Long, Double>>();
    public long LAST_UPDATE=0;

    public InetAddress ia=InetAddress.getLocalHost();
    public String[] targets_all={ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Request,ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Success,ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Exception,ia.getHostAddress()+":"+MonitorController.port+"-"+RT_Min,ia.getHostAddress()+":"+MonitorController.port+"-"+RT_Avg,ia.getHostAddress()+":"+MonitorController.port+"-"+QPS_exception,ia.getHostAddress()+":"+MonitorController.port+"-"+QPS_success};
    public String[] getTargets_all() {
        return targets_all;
    }

    public String getIp() {
        return ia.getHostAddress();
    }

    public MonitorService() throws UnknownHostException {
    }

    public void setTimestamp(long tm) {
       this.timestamp = tm;
    }



    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Override
    public JSONArray getSearchResult() throws Exception {
        JSONArray searchResult = new JSONArray();
        try{
            searchResult.add(ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Request);
            searchResult.add(ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Success);
            searchResult.add(ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Exception);
            searchResult.add(ia.getHostAddress()+":"+MonitorController.port+"-"+RT_Min);
            searchResult.add(ia.getHostAddress()+":"+MonitorController.port+"-"+RT_Avg);
            searchResult.add(ia.getHostAddress()+":"+MonitorController.port+"-"+QPS_exception);
            searchResult.add(ia.getHostAddress()+":"+MonitorController.port+"-"+QPS_success);
        }catch (Exception e){
            e.printStackTrace();
        }
        return searchResult;
    }

    @Override
    public JSONArray getQueryResult(String[] targets) {
        JSONArray Info = timeSeriesFormat(targets);
        System.out.println(targets);
        return Info;
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
        if(QPS_successInfo.size()>0){
            for(long key : QPS_successInfo.get(QPS_successInfo.size()-1).keySet()){
                if (key==tm){
                    QPS_successInfo.get(QPS_successInfo.size()-1).put(tm,value);
                }
                else{
                    Map<Long, Double> map = Maps.newHashMap();
                    map.put(tm,value);
                    QPS_successInfo.add(map);
                }
            } }
        else{
            Map<Long, Double> map = Maps.newHashMap();
            map.put(tm,value);
            QPS_successInfo.add(map);
        }
    }

    public JSONArray timeSeriesFormat(String[] targets) {
        JSONArray timeSeriesResult = new JSONArray();
        HashMap<String, List> map = new HashMap<String,List>();
        map.put(ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Request, TPS_RequestInfo);
        map.put(ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Success, TPS_SuccessInfo);
        map.put(ia.getHostAddress()+":"+MonitorController.port+"-"+TPS_Exception, TPS_ExceptionInfo);
        map.put(ia.getHostAddress()+":"+MonitorController.port+"-"+RT_Min, RT_MinInfo);
        map.put(ia.getHostAddress()+":"+MonitorController.port+"-"+RT_Avg, RT_AvgInfo);
        map.put(ia.getHostAddress()+":"+MonitorController.port+"-"+QPS_exception, QPS_exceptionInfo);
        map.put(ia.getHostAddress()+":"+MonitorController.port+"-"+QPS_success, QPS_successInfo);
        System.out.println(map.toString());
        System.out.println(Arrays.toString(targets));
        for (int i = 0; i < targets.length; i++) {
            JSONObject Info = new JSONObject();
            //添加target
            Info.put(TARGET, targets[i]);
            //添加datapoints
            Info.put(DATA_POINTS, getDatapoints(map.get(targets[i])));
            //添加到timeSeries
            timeSeriesResult.add(Info);
        }
        return timeSeriesResult;
    }
    public JSONArray getDatapoints(List<Map<Long, Double>> UtilInfo){
        JSONArray datapoints = new JSONArray();
        System.out.println(UtilInfo.size());
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


}

