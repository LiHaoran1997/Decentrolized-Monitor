package springcloud.hellospringcloudalibabanacosmonitor.controller;

import org.springframework.web.bind.annotation.*;

import java.net.UnknownHostException;

public interface GrafanaController{

    @RequestMapping(value = "/", method = {RequestMethod.GET})
    @ResponseBody String test(@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception;

    @RequestMapping(value = "/query", method = {RequestMethod.POST})
    @ResponseBody String query(@RequestBody String requestBody,@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr)throws Exception;

    @RequestMapping(value = "/search", method = {RequestMethod.POST})
    @ResponseBody String search(@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws Exception;

    @RequestMapping(value = "/annotations", method = {RequestMethod.POST})
    @ResponseBody String annotations(@RequestBody String requestBody,@PathVariable("service") String service,@RequestHeader("ipaddr") String ipaddr) throws UnknownHostException;

}
