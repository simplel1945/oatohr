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
import wscheck.CheckScheduler;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

import static weaver.file.Prop.getPropValue;

public class TestAction  implements Action{

    public String execute(RequestInfo request) {
        String workName="补卡流程";
        BaseBean baseBean = new BaseBean();
        baseBean.writeLog("------------------------------------------------------------------");
        String urlEncrypt = getPropValue("workflowhr", "urlEncrypt");
        String getTokenUrl = getPropValue("workflowhr", "getTokenUrl");
        String urlPPostDat = getPropValue("workflowhr", "urlPPostDat");
        String urlPGetData = getPropValue("workflowhr", "urlPGetData");
        String userNameOrEmailAddress = getPropValue("workflowhr", "userNameOrEmailAddress");
        CheckScheduler checkScheduler = new CheckScheduler();

        baseBean.writeLog(workName+"对接HR补卡接口开始执行！");
        //设置主表id参数
        int mainid=0;
        String name,value,sql,url,tableMain,tableDeatil,message;
        //表体
        String[] tableBody;
        //json对象
        System.out.println(1);
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
        baseBean.writeLog("提交的类型"+src);
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
            mainid= Util.getIntValue(rs.getString("id"), 0);
        }
        for (Property property : properties) {
            name = property.getName();
            value = Util.null2String(property.getValue());
            mainTableDataMap.put(name, value);
        }
        //提交
        if("submit".equals(src)){                                                                                               //提交执行方法
            baseBean.writeLog(workName+"开始执行提交到补卡的方法！");
            //表头开始获取数据
            header=new JSONObject();
            //主表字段赋值
            header.put("ProcName",Util.null2String("WSPROC_KQCard"));
            //备注
//            HrmResource hrm=new HrmResource();
//            hrm.getWorkcode();
//            sql="select workcode from hrmresource where id="+lastoperator;
//            writeLog(workName+"采购订单提交NC接口,当前审批人："+lastoperator);
//            rs.execute(sql);
//            if (rs.next()){
//                header.put("cauditpsn",Util.null2String(rs.getString("workcode")));				//人员编码																	//人员编码
//            }else{
//                header.put("cauditpsn","");																					//人员编码
//                writeLog(workName+"提交补卡接口,找不到审批人！");
//            }
            //表头获取数据完成
            //表体开始获取数据

            JSONObject tbody;
            List<String> list = new ArrayList<String>();
            rs.execute("select bkrq,bksj,bkyy,bklx from "+formTableName+"_dt1 where mainid="+mainid);
            Date date=new Date();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            String sprq=format.format(date);
            while(rs.next()){
                tbody=new JSONObject();
                //OA流程编号
                tbody.put("OAID",Util.null2String(requestid));
                //OA子表编号?
                tbody.put("OASubID",Util.null2String(""));
                //员工编号
                tbody.put("empcode",Util.null2String(mainTableDataMap.get("sqr")));
                //员工名称
                tbody.put("empname",Util.null2String(mainTableDataMap.get("sqr")));
                //刷卡日期
                tbody.put("CARD_DATE",Util.null2String(rs.getString("bkrq")));
                //刷卡时间
                tbody.put("card_time",Util.null2String(rs.getString("bksj")));
                //签卡原因
                tbody.put("FillCardReason",rs.getInt("bkyy"));
                //签卡类型
                tbody.put("FillCardType",Util.null2String(rs.getString("bklx")));
                //修改时间
                tbody.put("lastModifyTime",Util.null2String(sprq));
                list.add(tbody.toString());
            }
            tableBody=list.toArray(new String[0]);
            header.put("ProcParams", JSON.toJSONString(list));
            //表体获取数据完成
            tableDeatil="";
            for (String val : tableBody) {
                tableDeatil=tableDeatil+val;
            }
            message=workName+"提交补卡接口成功！";

            baseBean.writeLog(workName+"提交到补卡的表头数据："+header.toString());
            //开始调用NC接口
            try {
                WorkflowCommon workflowCommon = new WorkflowCommon();
                String com = workflowCommon.common(userNameOrEmailAddress, urlEncrypt, getTokenUrl, urlPPostDat, header);
                JSONObject jsonObject = JSONObject.parseObject(com);
                baseBean.writeLog(workName+"提交到补卡的数据："+com.toString());
                String success = jsonObject.get("success").toString();
                if(success.equals("true")){
                    baseBean.writeLog("HR接口调用成功");
                    if(!com.contains("Error")){
                        panudan=true;
                        message=workName+"补卡接口成功！";
                        baseBean.writeLog(workName+"提交到补卡的数据："+com);
                    }else{
                        baseBean.writeLog("HR接口调用失败"+com.toString());
                        request.getRequestManager().setMessageid(requestid+"-"+ TimeUtil.getCurrentTimeString());
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
                baseBean.writeLog(message);
                return Action.SUCCESS;
            }else{
                baseBean.writeLog(message);
                request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
                request.getRequestManager().setMessagecontent(message);
                return Action.FAILURE_AND_CONTINUE;
            }
        }else if ("reject".equals(src)){                                                                                    //退回执行方法
            baseBean.writeLog(workName+"开始执行退回到NC的方法！");
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
            baseBean.writeLog(workName+"对接NC接口："+message);
            request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
            request.getRequestManager().setMessagecontent(message);
            return Action.FAILURE_AND_CONTINUE;
        }

    }
}
