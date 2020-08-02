package springcloud.hellospringcloudalibabanacosmonitor.service;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Service
public abstract class GrafanaService {

	protected String STATUS = "status";
	protected String SUCCESS = "success";
	protected String ANNOTATIONS = "annotations";
	public String TARGET = "target";
	public String DATA_POINTS = "datapoints";
	
	public JSONObject testConnection() {
		JSONObject connection = new JSONObject();
		connection.put(STATUS, SUCCESS);
		return connection;
	}

	public abstract JSONArray getQueryResult(String[] a);

	public abstract JSONArray getSearchResult() throws Exception;

	public JSONObject getAnnotationResult() {
		JSONObject annotationResult = new JSONObject();
		annotationResult.put(ANNOTATIONS, "result");
		return annotationResult;
	}
	
}
