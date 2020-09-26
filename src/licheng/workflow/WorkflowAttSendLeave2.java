package licheng.workflow;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.licheng.workflow.utile.WorkflowCommon;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.Property;
import weaver.soa.workflow.request.RequestInfo;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 *@描述 请假流程(考勤假期)
 *@参数
 *@返回值
 *@创建人  lzh
 *@创建时间  2020/5/9
 */
public class WorkflowAttSendLeave2 extends BaseBean implements Action{

    /**
     * @Author 李志辉
     * @Description 请假流程
     * @Date 2019/8/14
     * @Param [request]
     * @return java.lang.String
     **/
    @Override
    public String execute(RequestInfo request) {
        String workName="请假流程";
        writeLog("------------------------------------------------------------------");
        String urlEncrypt = getPropValue("workflowhr", "urlEncrypt");
        String getTokenUrl = getPropValue("workflowhr", "getTokenUrl");
        String urlPPostDat = getPropValue("workflowhr", "urlPPostDat");
        String urlPGetData = getPropValue("workflowhr", "urlPGetData");
        String userNameOrEmailAddress = getPropValue("workflowhr", "userNameOrEmailAddress");
        writeLog(urlEncrypt);
        writeLog(getTokenUrl);
        writeLog(urlPPostDat);
        writeLog(urlPGetData);
        writeLog(userNameOrEmailAddress);
        writeLog(workName+"对接HR请假接口开始执行！");
        //设置主表id参数
        int mainid=0;
        String name,value,sql,url,tableMain,tableDeatil,message;
        //表体
        String[] tableBody;
        //json对象
        JSONObject header;
        boolean panudan=false;
        //执行sql使用
        RecordSet rs=new RecordSet();
        //表单流程id
        String requestid=request.getRequestid();
        //审批人
        String lastoperator=request.getLastoperator();
        //判断提交/驳回
        String src=request.getRequestManager().getSrc();
        writeLog("提交的类型"+src);
        //用来存取表头信息
        Map<String,String> mainTableDataMap = new HashMap<String,String>();
        //获取表单信息
        Property[] properties = request.getMainTableInfo().getProperty();
        //获取表名
        String formTableName = request.getRequestManager().getBillTableName();
        //根据流程id获取相关主表id 执行主表sql
        sql="select id from "+formTableName+" where requestid="+requestid;
        rs.execute(sql);
        //获取
        if(rs.next()){
            mainid=Util.getIntValue(rs.getString("id"), 0);
        }
        for (Property property : properties) {
            name = property.getName();
            value = Util.null2String(property.getValue());
            mainTableDataMap.put(name, value);
        }

        writeLog(mainTableDataMap.toString()+"查看表单提交数据！");
        //提交
        if("submit".equals(src)){
            //提交执行方法
            writeLog(workName+"开始执行提交的方法！");
            //表头开始获取数据
            header=new JSONObject();
            //主表字段赋值
            header.put("ProcName","WSPROC_AttSendLeave");
            JSONObject tbody;
            List<String> list = new ArrayList<String>();
            rs.execute("select id from "+formTableName+" where requestid="+requestid);
            while(rs.next()){
                tbody=new JSONObject();
                Date date=new Date();
                DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                System.out.println(date);
                String sprq=format.format(date);
                System.out.println(sprq);
                tbody.put("OAID",Util.null2String(requestid));
                tbody.put("OASubID","kqsub201905150001");
                tbody.put("empcode","08000570");
                tbody.put("empname","");
                tbody.put("LeaveClientele","梁正伟");
                tbody.put("LeaveType","事假");
                tbody.put("LeaveBegin",sprq);
                tbody.put("BeginTime",sprq);
                tbody.put("LeaveEnd",sprq);
                tbody.put("EndTime",sprq);
                tbody.put("LEAVE_BegNo","上半段");
                tbody.put("LEAVE_EndNo","下半段");
                tbody.put("LeaveDay",2);
                tbody.put("LeaveHour",0);
                tbody.put("LeaveStatus",1);
                tbody.put("LeaveReason","公司原因");
                list.add(tbody.toString());
            }
            tableBody=list.toArray(new String[0]);
            header.put("ProcParams", JSON.toJSONString(list));
            //表体获取数据完成
            tableDeatil="";
            for (String val : tableBody) {
                tableDeatil=tableDeatil+val;
            }
            message=workName+"提交接口成功！";
            writeLog(workName+"提交到请假的表头数据："+header.toString());
            //开始调用Hr接口
            try {
                WorkflowCommon workflowCommon = new WorkflowCommon();
                String com = workflowCommon.common(userNameOrEmailAddress, urlEncrypt, getTokenUrl, urlPPostDat, header);
                JSONObject jsonObject = JSONObject.parseObject(com);
                writeLog(workName+"的数据："+jsonObject.toString());
                String success = jsonObject.get("success").toString();
                if(success.equals("true")){
                    writeLog("HR接口调用成功");
                    if((com.toUpperCase().indexOf("Error".toUpperCase()))<0){
                        panudan=true;
                        message=workName+"接口成功！";
                        writeLog(workName+"的数据："+jsonObject.toString());
                    }else{
                        writeLog("HR接口调用失败");
                        request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
                        request.getRequestManager().setMessagecontent(message);
                        return Action.FAILURE_AND_CONTINUE;
                    }
                }else{
                    message=workName+"补卡接口失败！返回错误";
                }
            }catch(Exception e){
                panudan=false;
                message=workName+"提交NC接口发生错误，错误码："+e.getMessage();
            }
            //调用NC接口完成
            if (panudan){
                writeLog(message);
                return Action.SUCCESS;
            }else{
                writeLog(message);
                request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
                request.getRequestManager().setMessagecontent(message);
                return Action.FAILURE_AND_CONTINUE;
            }
        }else if ("reject".equals(src)){                                                                                    //退回执行方法
            writeLog(workName+"开始执行退回到NC的方法！");
            //表头开始获取数据
            header=new JSONObject();																						//主表json
            //主表字段赋值
            header.put("pk",Util.null2String(mainTableDataMap.get("ncdjzj")));		                //NC单据主键
            header.put("isapprove","1");																				        //审批批注
            tableMain=header.toString();
            //表头获取数据完成
            //表体开始获取数据isapprove
            JSONObject tbody=new JSONObject();
            List<String> list = new ArrayList<String>();
            rs.execute("select hh from  "+formTableName+"_dt1 where mainid="+mainid);
            while(rs.next()){
                tbody.put("crowno",Util.null2String(rs.getString("hh")));				                //行号
                list.add(tbody.toString());
            }
            tableBody=list.toArray(new String[0]);
            //表体获取数据完成
            //开始调用NC接口
            tableDeatil="";
            for (String val : tableBody) {
                tableDeatil=tableDeatil+val;
            }
            message=workName+"提交NC接口成功！";
            try {

            }catch(Exception e){
                panudan=false;
                message=workName+"退回NC接口发生错误，错误码："+e.getMessage();
            }
            //调用NC接口完成
            if (panudan){
                //writeLog(message);
                return Action.SUCCESS;
            }else{
                //writeLog(message);
                request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
                request.getRequestManager().setMessagecontent(message);
                return Action.FAILURE_AND_CONTINUE;
            }
        }else{
            message="未能识别的操作类型，请联系系统管理员！"+src;
            writeLog(workName+"对接NC接口："+message);
            request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
            request.getRequestManager().setMessagecontent(message);
            return Action.FAILURE_AND_CONTINUE;
        }
    }

}
