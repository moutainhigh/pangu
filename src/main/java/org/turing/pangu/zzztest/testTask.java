package org.turing.pangu.zzztest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.Test;
import org.turing.pangu.bean.VpnConnectInfo;
import org.turing.pangu.controller.pc.request.VpnConnectInfoReq;
import org.turing.pangu.controller.pc.request.VpnLoginReq;
import org.turing.pangu.controller.phone.request.GetTaskReq;
import org.turing.pangu.controller.phone.request.TaskFinishReq;
import org.turing.pangu.controller.phone.response.GetTaskRsp;
import org.turing.pangu.model.Device;
import org.turing.pangu.model.Task;
import org.turing.pangu.task.TaskExtend;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
/*
 * 
 * {
    "isHaveTask": 1,
    "loopTime": 10,
    "task": {
        "appId": 3,
        "deviceId": "eeeeewww",
        "isFinished": 0,
        "operType": 2,
        "spanTime": 5,
        "stockInfo": {
            "androidId": "0g0d4ss1n72c6fwn",
            "androidSerial": "zq6c8k051se2judn",
            "androidVersion": "4.4",
            "appId": 3,
            "blueTooth": "cf:e4:7b:c8:37:b8",
            "board": "ZL9",
            "bootloader": "Android/armeabi/armeabi:4.4/金立GN708T/eng.work.47120588.060365:eng/test-keys",
            "brand": "",
            "bssid": "7e:c0:8b:0d:53:b2",
            "carrier": "China Mobile",
            "carrierCode": "46002",
            "countryCode": "CN",
            "cpuAbi": "armeabi",
            "createDate": 1485261881000,
            "device": "ZL9",
            "deviceType": 0,
            "display": "4.4.283.P2..IT",
            "height": 960,
            "id": 5011,
            "imei": "863955035740300",
            "imsi": "460074174449092",
            "ip": "127.0.0.1",
            "isActived": 0,
            "isRemain": 0,
            "isWhiteIp": 0,
            "mac": "7e:c0:8b:0d:53:b2",
            "manufacture": "",
            "model": "金立GN708T",
            "phone": "+8617865018446",
            "phoneStatus": 1,
            "product": "ZL9",
            "sdk": 19,
            "simSerial": "89860045293689388947",
            "simStatus": 5,
            "ssid": "ChinaNet-01wcd",
            "ua": "Dalvik/1.4.0(Linux; U; 4.4;  Build/7T68C",
            "updateDate": 1485261881000,
            "width": 540
        },
        "taskId": "7G6Q6qHGQB30J8x57fP8Sh4337Nb9O4x",
        "times": 1,
        "vpnToken": "7G6Q6qHGQB30J8x5"
    },
    "taskIp": "127.0.0.1"
}*/
public class testTask {
	@Test
	public void test(){
		//for(int i = 0;i<1000;i++)
		{
			//getConnectInfo();
		}
		List<VpnConnectInfo> lista = new ArrayList<VpnConnectInfo>();

		VpnConnectInfo a = new VpnConnectInfo();
		VpnConnectInfo b = new VpnConnectInfo();
		VpnConnectInfo c = new VpnConnectInfo();
		VpnConnectInfo d = new VpnConnectInfo();
		
		a.setVpnName("Turing");
		a.setTunnelType("Automatic");
		a.setIp("139.196.39.239");
		a.setAuthenticationMethod("{Chap, MsChapv2}");
		a.setEncryptionLevel("Optional");
		a.setUserName("radiusyun04\\airobot");
		a.setPassword("qwerfdsa");
		lista.add(a);
		
		b.setVpnName("Turing");
		b.setTunnelType("Automatic");
		b.setIp("139.196.39.239");
		b.setAuthenticationMethod("{Chap, MsChapv2}");
		b.setEncryptionLevel("Optional");
		b.setUserName("airobot");
		b.setPassword("qwerfdsa");
		lista.add(b);

		c.setVpnName("Turing");
		c.setTunnelType("Automatic");
		c.setIp("139.196.39.239");
		c.setAuthenticationMethod("{Chap, MsChapv2}");
		c.setEncryptionLevel("Optional");
		c.setUserName("chuanqi520");
		c.setPassword("27933460ac9a");
		lista.add(c);

		d.setVpnName("Turing");
		d.setTunnelType("Automatic");
		d.setIp("139.196.39.239");
		d.setAuthenticationMethod("{Chap, MsChapv2}");
		d.setEncryptionLevel("Optional");
		d.setUserName("moshouzx");
		d.setPassword("b7d6a5358a6b");
		lista.add(d);
		
		for(VpnConnectInfo tmp:lista){
			// 判断是否连接成功
			String result = "";
			try {
				VpnUtil.getInstance().editVpn(tmp);
				result = VpnUtil.getInstance().connectVpn(tmp);
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (result.indexOf("已连接") > 0) {
				result = VpnUtil.getInstance().getAllUserConnection();
				System.out.print(result);
				break;
			} else {
				System.out.print("失败");
			}
		}
		/*
		TaskExtend task = new TaskExtend();
		task.setAllotIncrementMoney(100);
		task.setAllotIncrementWaterAmy(19989);
		task.setAppId(3L);
		Task ts = new Task();
		ts = task;
		ts.setAllotIncrementMoney(1222);
		
		try {
			Calendar cal=Calendar.getInstance(TimeZone.getTimeZone( "GMT+8")); 
			int hour =cal.get(Calendar.HOUR_OF_DAY);
			System.out.print("\n" + hour );
			Thread.sleep(1000);
			
			List<Task> allTaskList = new ArrayList<Task>();
			Task a = new Task();
			Task b = new Task();
			
			a.setId(1L);
			a.setAllotIncrementMoney(1000);
			b.setId(2L);
			b.setAllotIncrementMoney(900);
			
			allTaskList.add(a);
			allTaskList.add(b);
			
			
			Task c = new Task();
			c.setId(6L);
			c.setAllotIncrementMoney(500);
			int count = 0;
			for(Task tmp :allTaskList){
				tmp.setAllotIncrementMoney(c.getAllotIncrementMoney());
				//tmp.setAppId(5L);
				//if(tmp.getId() == c.getId()){
					//allTaskList.set(count, c);
				//}
				count ++;
			}
			Thread.sleep(1000);
			
			loginPangu();
			GetTaskRsp aTask = getTask();
			Thread.sleep(1000);
			GetTaskRsp bTask = getTask();
			Thread.sleep(1000);
			//GetTaskRsp cTask = getTask();
			
			Thread.sleep(3*1000);
			//1487425049683
			taskFinish(aTask,1);
			taskFinish(bTask,1);
			Thread.sleep(2*1000);
			
			aTask = getTask();
			Thread.sleep(1000);
			bTask = getTask();
			

		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}*/
	}
	public void getConnectInfo(){
		String loginUrl = "http://localhost:8080/pc/getConnectInfo.pangu";
		VpnConnectInfoReq req = new VpnConnectInfoReq();
		String json = JSON.toJSONString(req);
		String contentStr = HttpUtils.doPost(loginUrl, json, HttpUtils.UTF8);
		System.out.print("\n"+contentStr);
	}
	public void loginPangu(){
		String loginUrl = "http://localhost:8080/pc/vpnLogin.pangu";
		VpnLoginReq req = new VpnLoginReq();
		req.setOperType(2);
		req.setDeviceId("232dsssddd");// 取电脑mac地址
		String json = JSON.toJSONString(req);
		String contentStr = HttpUtils.doPost(loginUrl, json, HttpUtils.UTF8);
		System.out.print("\n"+contentStr);
	}
	public GetTaskRsp getTask(){
		String getTaskUrl = "http://localhost:8080/mobile/getTask.pangu";
		//String getTaskUrl="http://pangu.u-app.cn/mobile/getTask.pangu";
		//String taskFinishUrl = "http://localhost:8080/mobile/taskFinish.pangu";
		
		GetTaskReq req = new GetTaskReq();
		Date data = new Date();
		Long time = data.getTime();
		req.setDeviceId(time.toString());
		req.setDeviceId("18899882888");
		req.setAccessToken("");
		String json = JSON.toJSONString(req);
		String contentStr = HttpUtils.doPost(getTaskUrl, json, HttpUtils.UTF8);
		GetTaskRsp rsp = JSON.parseObject(contentStr,
				new TypeReference<GetTaskRsp>() {
				});
		Device device = new Device();
		contentStr = JSON.toJSONString(rsp);
		System.out.print("\n");
		System.out.print(contentStr);
		return rsp;
	}
	
	public void taskFinish(GetTaskRsp rsp,int isFinished){
		String taskFinishUrl = "http://localhost:8080/mobile/taskFinish.pangu";
		TaskFinishReq req = new TaskFinishReq();
		req.setTaskId(rsp.getTask().getTaskId());
		req.setVpnToken(rsp.getTask().getVpnToken());
		req.setIsFinished(isFinished);
		String json = JSON.toJSONString(req);
		String contentStr = HttpUtils.doPost(taskFinishUrl, json, HttpUtils.UTF8);
		System.out.print("\n");
		System.out.print(contentStr);
	}

}
