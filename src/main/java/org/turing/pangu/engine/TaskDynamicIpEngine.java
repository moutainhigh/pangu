package org.turing.pangu.engine;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.turing.pangu.controller.common.PhoneTask;
import org.turing.pangu.controller.pc.request.VpnLoginReq;
import org.turing.pangu.controller.pc.response.VpnOperUpdateRsp;
import org.turing.pangu.controller.phone.request.TaskFinishReq;
import org.turing.pangu.model.App;
import org.turing.pangu.model.Task;
import org.turing.pangu.phone.ChangeDeviceInfo;
import org.turing.pangu.task.DateUpdateListen;
import org.turing.pangu.task.IpQueryResult;
import org.turing.pangu.task.OptimalApp;
import org.turing.pangu.task.StockTask;
import org.turing.pangu.task.TaskIF;
import org.turing.pangu.task.TaskListSort;
import org.turing.pangu.task.VpnTask;
import org.turing.pangu.task.VpnTaskStatistics;
import org.turing.pangu.utils.RandomUtils;
import org.turing.pangu.utils.TraceUtils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
/*
 * 任务引擎，负责每日任务生成,配置任务,跟踪任务进展,
 * */
public class TaskDynamicIpEngine implements TaskIF{
	private static final Logger logger = Logger.getLogger(TaskDynamicIpEngine.class);
	private static TaskDynamicIpEngine mInstance = new TaskDynamicIpEngine();
	private List<VpnTask> vpnTaskList = new ArrayList<VpnTask>();
	private DateUpdateListen mListen = null;
	
	public static TaskDynamicIpEngine getInstance(){
		if(null == mInstance)
			mInstance = new TaskDynamicIpEngine();
		return mInstance;
	}
	public void setDataUpdateListen(DateUpdateListen listen){
		mListen = listen;
	}
	public boolean isRunVpn(String remoteIp){
		for(VpnTask task :vpnTaskList){
			if(task.getRemoteIp().equals(remoteIp)){
				return true;
			}
		}
		return false;
	}
	@Override
	public void init(DateUpdateListen listen) {
		// TODO Auto-generated method stub
		setDataUpdateListen(listen);
		clear();
	}
	@Override
	public boolean isHavaTaskByOperType(int type, Task dbTask) {
		// TODO Auto-generated method stub
		Task ext = (Task)dbTask;
		switch(type){
		case TaskEngine.INCREMENT_MONEY_TYPE:
			return (ext.getIncrementMoney() - ext.getExecuteIncrementMoney()) > 0 ? true:false;
		case TaskEngine.INCREMENT_WATERAMY_TYPE:
			return (ext.getIncrementWaterAmy() - ext.getExecuteIncrementWaterAmy()) > 0 ? true:false;
		case TaskEngine.STOCK_MONEY_TYPE:
			return (ext.getStockMoney() - ext.getExecuteStockMoney()) > 0 ? true:false;
		case TaskEngine.STOCK_WATERAMY_TYPE:
			return (ext.getStockWaterAmy() - ext.getExecuteIncrementWaterAmy()) > 0 ? true:false;
		}
		return false;
	}
	@Override
	public synchronized String vpnLogin(VpnLoginReq req,String remoteIp,String realIp){
		TraceUtils.getTraceInfo();
		// 此IP不能运行任务了
		if(false == IpLimitMngEngine.getInstance().isCanGetTask(remoteIp)){
			return null;
		}
		VpnTask task = new VpnTask();
		logger.info("vpnLogin---001--"+task.toString());
		if( vpnTaskList.size() > 0){
			for (int i = vpnTaskList.size()-1; i >=0; i--){
				VpnTask tmp = vpnTaskList.get(i);
				if(tmp.getRemoteIp().equals(remoteIp)){
					vpnTaskList.remove(tmp); // 删除原来的VPN task
					logger.info("vpnLogin---002--del old task");
				}
			}
		}
		IpQueryResult ipResult = IpTrunkEngine.getInstance().getStockInfoList(remoteIp);
		task.setStockDeviceList(ipResult.getStockList()); // 先从数据库中取好留存（同IP或同城IP）
		task.setLocation(ipResult.getLocation());
		task.setDeviceId(req.getDeviceId());
		task.setOperType(req.getOperType());
		task.setRemoteIp(remoteIp);
		task.setRealIp(realIp);
		task.setToken(RandomUtils.getRandomNumbersAndCapitalLetters(TaskEngine.VPN_TOKEN_LENGH));
		task.getStatistics().setTaskStockTotalCount(task.getStockDeviceList().size()>0?1:0);
		int activeUserCount = AppEngine.getInstance().getActiveUserCount();
		int random = RandomUtils.getRandom(0, 100);
		task.getStatistics().setTaskIncrementTotalCount((activeUserCount + ((random%3==0)?1:0))); // 尽量让脚本多跑一个
		task.setCreateTime(new Date());
		task.getStatistics().print();
		vpnTaskList.add(task);
		logger.info("vpnLogin---end");
		return task.getToken();
	}
	//正在运行的模拟器数量
	private int getRunningMoniterCount(VpnTask task){
		int count = 0;
		for(PhoneTask pt: task.getPhoneStockTaskList()){
			if(pt.getIsFinished() == TaskEngine.TASK_STATE_INIT ){
				count++;
			}
		}
		for(PhoneTask pt: task.getPhoneTaskList()){
			if(pt.getIsFinished() == TaskEngine.TASK_STATE_INIT ){
				count++;
			}
		}
		return count;
	}
	@Override
	public synchronized VpnOperUpdateRsp vpnIsNeedSwitch(String token,String remoteIp,String realIp){
		TraceUtils.getTraceInfo();
		VpnOperUpdateRsp dataRsp = new VpnOperUpdateRsp();
		dataRsp.setIsSwitchVpn(1);
		for(VpnTask task :vpnTaskList){
			if(task.getRemoteIp().equals(remoteIp)){
				dataRsp.setStatistics(task.getStatistics());
				if((task.getPhoneTaskList() == null || task.getPhoneTaskList().size() == 0 )
						&&(task.getPhoneStockTaskList() == null || task.getPhoneStockTaskList().size() == 0)){
					logger.info("not task run");
					dataRsp.setIsSwitchVpn(0);
					dataRsp.setTaskTotal(0);
					break;
				}
				if(false == task.getStatistics().isTaskFinished()){
					dataRsp.setIsSwitchVpn(0);
				}else{
					dataRsp.setIsSwitchVpn(1);
				}
				dataRsp.setRunningCount(getRunningMoniterCount(task));
			}
		}
		logger.info("vpnIsNeedSwitch----end");
		return dataRsp;
	}
	@Override
	public synchronized boolean switchVpnFinish(String token,String remoteIp,String realIp){
		// 此IP不能运行任务了
		TraceUtils.getTraceInfo();
		if(false == IpLimitMngEngine.getInstance().isCanGetTask(remoteIp)){
			logger.info("ip is Cannot GetTask ");
			return false;
		}
		for(VpnTask task :vpnTaskList){
			if(task.getRemoteIp().equals(remoteIp)){
				if(!remoteIp.equals(task.getRemoteIp())){
					logger.info("warn !!! newRemoteIp != oldRemoteIp");
				}
				if(!realIp.equals(task.getRealIp())){
					logger.info("warn !!!  newRealIp != oldRealIp");
				}
				logger.info("switchVpnFinish|remoteIp:"+remoteIp+"|realIp:"+realIp);
				task.setRemoteIp(remoteIp);
				task.getPhoneTaskList().clear();//清空所有任务
				task.setRealIp(realIp);
				task.setCreateTime(new Date());//重新设置超时起点时间
				break;
			}
		}
		logger.info("switchVpnFinish---end");
		return true;
	}
	
	private void updateVpnTaskInfo(VpnTaskStatistics statistics ,int state){
		switch(state){
			case TaskEngine.TASK_STATE_REPORT_FINISHED:
				statistics.setTaskReportFinishedCount(statistics.getTaskReportFinishedCount()+1);
				break;
			case TaskEngine.TASK_STATE_REPORT_UNFINISHED:
				statistics.setTaskReportNotFinishedCount(statistics.getTaskReportNotFinishedCount()+1);
				break;
			case TaskEngine.TASK_STATE_TIMEOUT:
				statistics.setTaskReportNotFinishedCount(statistics.getTaskReportNotFinishedCount()+1);
				break;
		}
	}
	// 手机端一个任务完成
	@Override
	public synchronized void taskFinish(TaskFinishReq req,String remoteIp,String realIp){
		TraceUtils.getTraceInfo();
		logger.info("taskId:"+req.getTaskId()+"--remoteIp:"+remoteIp+"--realIp:"+realIp);
		for(VpnTask task:vpnTaskList){
			if(task.getToken().equals(req.getVpnToken())){
				for(PhoneTask tmpTask:task.getPhoneTaskList()){
					// 任务ID相同,且状态未被设置过, timeout 也会设置状态 TaskEngine.TASK_STATE_TIMEOUT
					if(tmpTask.getTaskId().equals(req.getTaskId())&& tmpTask.getIsFinished() == TaskEngine.TASK_STATE_INIT){
						if(req.getIsFinished() == 1 ){
							tmpTask.setIsFinished(TaskEngine.TASK_STATE_REPORT_FINISHED);//找到任务并设为完成状态
							tmpTask.setIsReport(1);
							mListen.updateExecuteTask(tmpTask,true); // 更新执行任务
							updateVpnTaskInfo(task.getStatistics() ,TaskEngine.TASK_STATE_REPORT_FINISHED);
							logger.info(" set tast Finished appId:"+tmpTask.getApp().getId());
						}else{
							tmpTask.setIsFinished(TaskEngine.TASK_STATE_REPORT_UNFINISHED);//找到任务并设为未完成状态
							tmpTask.setIsReport(1);
							mListen.updateExecuteTask(tmpTask,false); // 更新执行任务
							updateVpnTaskInfo(task.getStatistics() ,TaskEngine.TASK_STATE_REPORT_UNFINISHED);
							logger.info(" set tast not Finished appId:"+tmpTask.getApp().getId());
						}
					}
				}
				
				for(PhoneTask tmpTask:task.getPhoneStockTaskList()){
					if(tmpTask.getTaskId().equals(req.getTaskId())){
						if(req.getIsFinished() == 1 && tmpTask.getIsFinished() == TaskEngine.TASK_STATE_INIT){
							tmpTask.setIsFinished(TaskEngine.TASK_STATE_REPORT_FINISHED);//找到任务并设为完成状态
							tmpTask.setIsReport(1);
							mListen.updateExecuteTask(tmpTask,true); // 更新执行任务
							updateVpnTaskInfo(task.getStatistics() ,TaskEngine.TASK_STATE_REPORT_FINISHED);
							logger.info(" set tast Finished appId:"+tmpTask.getApp().getId());
						}else{
							tmpTask.setIsFinished(TaskEngine.TASK_STATE_REPORT_UNFINISHED);//找到任务并设为未完成状态
							tmpTask.setIsReport(1);
							mListen.updateExecuteTask(tmpTask,false); // 更新执行任务
							updateVpnTaskInfo(task.getStatistics(),TaskEngine.TASK_STATE_REPORT_UNFINISHED);
							logger.info(" set tast not Finished appId:"+tmpTask.getApp().getId());
						}
					}
				}
			}
		}
		logger.info("taskFinish---end");
	}
	// 删除该模拟器原有的任务
	private void addTaskAndDelSame(List<PhoneTask> list,PhoneTask addTask){
		if( list.size() > 0){
			for (int i = list.size()-1; i >=0; i--){
				PhoneTask tmp = list.get(i);
				if(tmp.getDeviceId().equals(addTask.getDeviceId())){
					list.remove(tmp); // 删除原来的VPN task
					logger.info("addTaskAndDelSame--del old PhoneTask");
				}
			}
		}
		list.add(addTask);
	}
	//取一个任务
	@Override
	public synchronized PhoneTask getTask(String deviceId,String remoteIp,String realIp){
		// 1. 先查VPN用途
		PhoneTask pTask = null;
		TraceUtils.getTraceInfo();
		logger.info("deviceId:"+deviceId+"--remoteIp:"+remoteIp+"--realIp:"+realIp);
		for(VpnTask task:vpnTaskList){
			if(task.getRemoteIp().equals(remoteIp)){
				for(PhoneTask tmpTask:task.getPhoneTaskList()){
					if(tmpTask.getDeviceId().equals(deviceId)){ // 发现取过任务了
						if(tmpTask.getIsFinished() == TaskEngine.TASK_STATE_INIT){ // 任务没完成,返回原来的任务
							logger.info("repeat task not finished,return old task");
							tmpTask.setCreateTime(new Date());
							return tmpTask; 
						}
					}
				}
				for(PhoneTask tmpTask:task.getPhoneStockTaskList()){
					if(tmpTask.getDeviceId().equals(deviceId)){ // 发现取过任务了
						if(tmpTask.getIsFinished() == TaskEngine.TASK_STATE_INIT){ // 任务没完成,返回原来的任务
							logger.info("repeat task not finished,return old task");
							tmpTask.setCreateTime(new Date());
							return tmpTask; 
						}
					}
				}	
				pTask = getTask(task,deviceId,remoteIp,realIp);
				if(pTask != null){
					logger.info("Task:"+pTask.toString());
					if(pTask.getOperType() == TaskEngine.INCREMENT_MONEY_TYPE || pTask.getOperType() == TaskEngine.INCREMENT_WATERAMY_TYPE ){
						task.getStatistics().setTaskAllocIncrementCount(task.getStatistics().getTaskAllocIncrementCount()+ 1); // 分配任务++
						addTaskAndDelSame(task.getPhoneTaskList(),pTask);
					}else{
						task.getStatistics().setTaskAllocStockCount(task.getStatistics().getTaskAllocStockCount()+1);
						addTaskAndDelSame(task.getPhoneStockTaskList(),pTask);
					}
				}else{
					logger.info("this ip task all alloc ");
				}
			}
		}
		return pTask;
	}
	private boolean isHavaStockDevice(VpnTask task){
		for(StockTask device: task.getStockDeviceList()){
			if(device.isUsed() == false)
			{
				return true;
			}
		}
		return false;
	}

	private synchronized PhoneTask getTask(VpnTask task,String deviceId,String remoteIp,String realIp){
		PhoneTask pTask = new PhoneTask();
		int operType = task.getOperType();
		VpnTaskStatistics statistics = task.getStatistics();
		// 先存量都变成增量
		operType = TaskEngine.INCREMENT_MONEY_TYPE;
		if(!statistics.isCanAllocIncrement() &&!statistics.isCanAllocStock()){
			return null;
		}else if(statistics.isCanAllocStock()){ // 先跑存量
			operType = TaskEngine.STOCK_MONEY_TYPE;
		}
        
        OptimalApp opt = getOptimalApp(task,operType,remoteIp,realIp);
		if(opt == null || opt.getApp() == null){
			return null;
		}
		pTask.setDeviceId(deviceId);
		pTask.setVpnToken(task.getToken());
		pTask.setTaskId(task.getToken() + RandomUtils.getRandomNumbersAndCapitalLetters(TaskEngine.PHONE_TASKID_LENGH/2));//32位
		pTask.setOperType(opt.getOperType());
		pTask.setApp(opt.getApp());
		pTask.setTimes(1);// 暂定一次
		pTask.setSpanTime(5);//暂定5S 
		pTask.setChangeDeviceInfo(opt.getInfo());
		pTask.setCreateTime(new Date());
		pTask.setIsFinished(TaskEngine.TASK_STATE_INIT);
		return pTask;
	}
	private int getOptimalOperType(int operType, long appId){
		Task task = TaskEngine.getInstance().getTaskInfoByAppId(appId);
		switch(operType){
			case TaskEngine.INCREMENT_MONEY_TYPE:
			case TaskEngine.INCREMENT_WATERAMY_TYPE:
				if(task.getAllotIncrementWaterAmy()== 0 && task.getAllotIncrementMoney()== 0){
					return operType;
				}
				int increPercent = (int)(100 * ((float)task.getAllotIncrementMoney()/(float)(task.getAllotIncrementMoney() + task.getAllotIncrementWaterAmy())));
				if(increPercent > TaskEngine.MONEY_PERCENT){
					return TaskEngine.INCREMENT_WATERAMY_TYPE;
				}else{
					return TaskEngine.INCREMENT_MONEY_TYPE;
				}
			case TaskEngine.STOCK_MONEY_TYPE:
			case TaskEngine.STOCK_WATERAMY_TYPE:
				if(task.getAllotStockWaterAmy()== 0 && task.getAllotStockMoney()== 0){
					return operType;
				}
				int stockPercent = (int)(100 * (float)(task.getAllotStockMoney()/(float)(task.getAllotStockMoney() + task.getAllotStockWaterAmy())));
				if(stockPercent > TaskEngine.MONEY_PERCENT){
					return TaskEngine.STOCK_WATERAMY_TYPE;
				}else{
					return TaskEngine.STOCK_MONEY_TYPE;
				}
		}
		return operType;
	}
    private synchronized OptimalApp getOptimalApp(VpnTask task,int operType,String remoteIp,String realIp){
        // -- 排序
    	OptimalApp opt = new OptimalApp();
		TraceUtils.getTraceInfo();
        logger.info("getOptimalAppId--operType:"+operType+" remoteIp:"+remoteIp+" realIp:"+realIp);
        //TaskListSort.taskSort(TaskEngine.getInstance().todayTaskList, operType);//对任务列表排序
        List<StockTask> deviceList = task.getStockDeviceList();
        // 处理存量
        if(operType == TaskEngine.STOCK_MONEY_TYPE || operType == TaskEngine.STOCK_WATERAMY_TYPE ){
            logger.info("getOptimalAppId---002--STOCK");
            int flag = 0;
            /*算法核心:
             * 一个IP只能运行一个APP,即便找到了很多符合条件的device,但只运行一次
             * */
            for(StockTask dev:deviceList){
            	if(true == dev.isUsed()){
            		continue;
            	}
            	// 因为随机取，所以乘2 最大程度确保每个能跑到
                for(int index = 0;index < 2 * TaskEngine.getInstance().todayTaskList.size(); index ++){
                	int random = RandomUtils.getRandom(0, TaskEngine.getInstance().todayTaskList.size());
                	Task dbTask= TaskEngine.getInstance().todayTaskList.get(random);
                    flag = 0;
                	for(PhoneTask tmpTask:task.getPhoneStockTaskList()){
    					// 同一个用户,同一个应用不下发两次
    					App same = AppEngine.getInstance().getAppInfo(dbTask.getAppId());
    					if(tmpTask.getApp().getUserId() == same.getUserId() ){
    						flag = 1;
    						logger.info("find same user:"+same.getUserId()+"|AppId:"+same.getId());
    						break;
    					}
                    }
                    if(flag == 0 && task.getStatistics().isCanAllocStock() &&dev.getDevice().getAppId() == dbTask.getAppId() && isHavaTaskByOperType(operType,dbTask)){
	        			ChangeDeviceInfo info = JSON.parseObject(dev.getDevice().getConfigure(),
	        					new TypeReference<ChangeDeviceInfo>() {
	        					});
                    	opt.setInfo(info); // 保存好不容易找到的存量信息，函数外赋值。
                        dev.setUsed(true);
                        dev.setSendDate(new Date());
                        logger.info("getOptimalAppId---end--find STOCK mDevice:"+dev.getDevice().toString());
                        opt.setApp(AppEngine.getInstance().getAppInfo(dbTask.getAppId()));
                        int newOperType = getOptimalOperType(operType, dbTask.getAppId());
                        mListen.updateAllocTask(newOperType,dbTask); // 对应派发 ++ 
                        opt.setOperType(newOperType);
                        return opt;
                    }
                }
            }
            logger.info("getOptimalAppId---end--not match STOCK");
            return null;
        }else{// 处理增量
            logger.info("getOptimalAppId---004--INCREMENT");
            int flag = 0;
            //----------保证每个用户名下都能跑到-------------------------------
            for(int index = 0;index < 2 * TaskEngine.getInstance().todayTaskList.size(); index ++){
            	int random = RandomUtils.getRandom(0, TaskEngine.getInstance().todayTaskList.size());
            	Task dbTask= TaskEngine.getInstance().todayTaskList.get(random);
                flag = 0;
                for(PhoneTask tmpTask:task.getPhoneTaskList()){
					// 同一个用户,同一个应用不下发两次,只要用户名不一样就可以分配
					App same = AppEngine.getInstance().getAppInfo(dbTask.getAppId());
					if(tmpTask.getApp().getUserId() == same.getUserId()){
						flag = 1;
						logger.info("find same user:"+same.getUserId()+"|AppId:"+same.getId());
						break;
					}
                }
                if(flag == 0 && isHavaTaskByOperType(operType,dbTask)){
                    logger.info("getOptimalAppId---end--return INCREMENT ");
                    opt.setApp(AppEngine.getInstance().getAppInfo(dbTask.getAppId()));
                    opt.setInfo(PhoneBrandEngine.getInstance().getNewDeviceInfo(remoteIp,task.getLocation(), opt.getApp()));
                    int newOperType = getOptimalOperType(operType, dbTask.getAppId());
                    mListen.updateAllocTask(newOperType,dbTask); // 对应派发 ++ 
                    opt.setOperType(newOperType);
                    return opt;
                }
            }
            //-----------------------------------------------------------
            //--------一个 IP下跑尽量多跑些---------------------
            if(task.getPhoneTaskList().size() > 0 && task.getStatistics().isCanAllocIncrement()){
	            int random = RandomUtils.getRandom(0, task.getPhoneTaskList().size());
	            PhoneTask pt = task.getPhoneTaskList().get(random);
	            for(Task dbTask:TaskEngine.getInstance().todayTaskList){
	            	if(dbTask.getAppId() == pt.getApp().getId()){
			            logger.info("getOptimalAppId---may be more run--- " + pt.getApp().getId());
			            opt.setApp(AppEngine.getInstance().getAppInfo(dbTask.getAppId()));
			            opt.setInfo(PhoneBrandEngine.getInstance().getNewDeviceInfo(remoteIp,task.getLocation(), opt.getApp()));
	                    int newOperType = getOptimalOperType(operType, dbTask.getAppId());
	                    mListen.updateAllocTask(newOperType,dbTask); // 对应派发 ++ 
	                    opt.setOperType(newOperType);
                        return opt;
		            }
	            }
            }
        }
        logger.info("getOptimalAppId---end--not task send ");
        return null;
    }
    
	private void clear(){
		if(null != vpnTaskList)
			vpnTaskList.clear();
	}
	
	@Override
	public void CheckVpnTimeoutJob() {	// 一分钟查一次
		
		if(null == vpnTaskList || vpnTaskList.size() == 0){
			return;
		}
		for(VpnTask vpnTask : vpnTaskList){
			for(PhoneTask ptask:vpnTask.getPhoneStockTaskList()){
				if(ptask.getIsFinished() == TaskEngine.TASK_STATE_INIT){
					if(true == TaskEngine.isTimeOut(ptask)){
						ptask.setIsFinished(TaskEngine.TASK_STATE_TIMEOUT);
						ptask.setIsReport(0);
						logger.info("Timeout task"+ptask.getApp().getPackageName());
						mListen.updateExecuteTask(ptask,false); // 更新执行任务
						updateVpnTaskInfo(vpnTask.getStatistics() ,TaskEngine.TASK_STATE_TIMEOUT);
					}
				}
			}
			for(PhoneTask ptask:vpnTask.getPhoneTaskList()){
				if(ptask.getIsFinished() == TaskEngine.TASK_STATE_INIT){
					if(true == TaskEngine.isTimeOut(ptask)){
						ptask.setIsFinished(TaskEngine.TASK_STATE_TIMEOUT);
						ptask.setIsReport(0);
						logger.info("Timeout task"+ptask.getApp().getPackageName());
						mListen.updateExecuteTask(ptask,false); // 更新执行任务
						updateVpnTaskInfo(vpnTask.getStatistics() ,TaskEngine.TASK_STATE_TIMEOUT);
					}
				}
			}
		}
		/*
		for (int i = vpnTaskList.size()-1; i >=0; i--){
			VpnTask vpn = vpnTaskList.get(i);
			
			for()
			
			
			if(true == TaskEngine.isTimeOut(vpn)){
				vpnTaskList.remove(vpn);//删除这个已结束的VPN
				logger.info("remove isTimeOut vpn ");
			}
		}*/
	}
}
