package licheng.workflow;

import com.licheng.workflow.utile.GeneralToolForHR;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
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
 *@描述 考勤加班申请流程/校驗
 *@参数
 *@返回值
 *@创建人  lzh
 *@创建时间  2020/5/9
 */
public class WorkflowAttSendOver extends BaseBean implements Action{

    private String interfaceName="考勤加班对接HR接口";
    /**
     * @Author 李志辉
     * @Description 考勤加班流程
     * @Date 2019/8/14
     * @Param [request]
     * @return java.lang.String
     **/
    @Override
    public String execute(RequestInfo request){
        String message="";
        String urlEncrypt = getPropValue("workflowhr", "urlEncrypt");
        String getTokenUrl = getPropValue("workflowhr", "getTokenUrl");
        String urlPPostDat = getPropValue("workflowhr", "urlPPostDat");
        String urlPGetData = getPropValue("workflowhr", "urlPGetData");
        String userNameOrEmailAddress = getPropValue("workflowhr", "userNameOrEmailAddress");
        //表单流程id
        int requestid = request.getRequestManager().getRequestid();
        //获取表名
        String formTableName = request.getRequestManager().getBillTableName();
        //判断提交/驳回
        String src=request.getRequestManager().getSrc();
        writeLog("提交的类型"+src);
        //审批人
        String lastoperator=request.getLastoperator();
        writeLog("审批人"+lastoperator);
        writeLog(interfaceName+",开始！");
        boolean judge=false;
        Map<String,String> mainTableDataMap = new HashMap<String,String>();
        Property[] properties = request.getMainTableInfo().getProperty();
        for (Property property : properties) {
            String name = property.getName();
            String value = Util.null2String(property.getValue());
            mainTableDataMap.put(name, value);
        }
        writeLog(interfaceName+",获取主表数据成功!");
        if("submit".equals(src)) {
            try {
                String accessToken = "";
                JSONObject parameter = new JSONObject();
                GeneralToolForHR gtfr = new GeneralToolForHR();
                JSONObject Encrypt = gtfr.doPost(interfaceName, urlEncrypt, parameter, accessToken);
                writeLog(Encrypt);
                if (Encrypt.getBoolean("success")) {
                    writeLog(interfaceName + ",获取凭证成功!");
                    JSONObject jsonObject2 = new JSONObject();
                    jsonObject2 = new JSONObject();
                    jsonObject2.put("userNameOrEmailAddress",userNameOrEmailAddress);
                    jsonObject2.put("password", Encrypt.get("result").toString());
                    jsonObject2.put("language", "string");
                    jsonObject2.put("platform", 0);
                    jsonObject2.put("clientVersion", "string");
                    jsonObject2.put("curNum", 0);
                    jsonObject2.put("rememberClient", true);
                    JSONObject login_json = gtfr.doPost(interfaceName, getTokenUrl, jsonObject2, accessToken);
                    writeLog(login_json);
                    if (login_json.getBoolean("success")) {
                        accessToken = login_json.getJSONObject("result").get("accessToken").toString();
                        parameter = getParam(mainTableDataMap, requestid,formTableName);
                        JSONObject result = gtfr.doPost(interfaceName, urlPPostDat, parameter, accessToken);
                        if (result.getBoolean("success")) {
                            if((result.get("result").toString().indexOf("Error"))<0&&result.getString("error")=="null"){
                                judge = true;
                                JSONArray data = result.getJSONObject("result").getJSONArray("data");
                                writeLog(interfaceName + ",接口返回成功：" + data.toString());
                            }else{
                                writeLog("数据返回失败"+result.toString());
                                request.getRequestManager().setMessageid(requestid+"-"+ TimeUtil.getCurrentTimeString());
                                request.getRequestManager().setMessagecontent("提示信息：" + interfaceName + ",失败：存在错误,请联系系统管理员！"+result.toString());
                                return Action.FAILURE_AND_CONTINUE;
                            }
                        } else {
                            writeLog(interfaceName + ",接口返回失败：" + result.get("error").toString());
                            request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
                            request.getRequestManager().setMessagecontent("提示信息：" + interfaceName + ",失败：没有成功,请联系系统管理员！");
                        }
                    }else {
                        writeLog(interfaceName + ",没有返回token：" + login_json.get("error").toString());
                        request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
                        request.getRequestManager().setMessagecontent("提示信息：" + interfaceName + ",失败：没有返回token,请联系系统管理员！");
                    }
                } else {
                    writeLog(interfaceName + ",获取密钥失败!");
                    request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
                    request.getRequestManager().setMessagecontent("提示信息：" + interfaceName + ",获取密钥失败,请联系系统管理员！");
                }
            } catch (Exception e) {
                writeLog(interfaceName + ",出错" + e);
                request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
                request.getRequestManager().setMessagecontent("提示信息：" + interfaceName + ",出错：" + e + ",请联系系统管理员！");
            }
            if (judge) {
                writeLog(interfaceName + ",结束！pass");
                return SUCCESS;
            } else {
                writeLog(interfaceName + ",结束！do not pass");
                return FAILURE_AND_CONTINUE;
            }
        }else if ("reject".equals(src)){
            message="撤回";
            request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
            request.getRequestManager().setMessagecontent(message);
            return Action.FAILURE_AND_CONTINUE;
        }else{
            message="未能识别的操作类型，请联系系统管理员！"+src;
            writeLog(interfaceName+"对接Hr接口："+message);
            request.getRequestManager().setMessageid(requestid+"-"+TimeUtil.getCurrentTimeString());
            request.getRequestManager().setMessagecontent(message);
            return Action.FAILURE_AND_CONTINUE;
        }
    }

    /**
     * create by: Json.Zhou
     * description:	获取接口所需的JSONObject对象
     * create time: 2018/10/27 10:32
     * @param mainTableDataMap	（流程表单主表字段的Map<String,String>）
     * @param requestid	（流程的requestid）
     * @return net.sf.json.JSONObject
     */
    private net.sf.json.JSONObject getParam(Map<String,String> mainTableDataMap, int requestid, String formTableName){
        writeLog(interfaceName+",获取流程的JSONObject对象开始！");
        writeLog("表单对应参数"+mainTableDataMap.toString());
        int mainid=0;
        JSONObject tbody;
        JSONObject json = new JSONObject();
        List<JSONObject> list=new ArrayList<JSONObject>();
        RecordSet rs=new RecordSet();
        json.put("ProcName", "WSPROC_AttSendOver");
        //考核月份
//        String khyf=Util.null2String(mainTableDataMap.get("kaohyf1"));
//        if (!khyf.equals("")){
//            khyf=khyf.substring(0,7).replace("-","");
//        }
        //请假单ID
        String  sql="select id from "+formTableName+" where requestid="+requestid;
        rs.execute(sql);
        //获取
        if(rs.next()){
            mainid= Util.getIntValue(rs.getString("id"), 0);
        }
         sql="select sqrhr,jbsy,jbksrq,jbjssj,jbkssj,jbjssj1,bz,zjbsj from "+formTableName+"_dt1 where mainid="+mainid;
        writeLog("----------"+sql);
        rs.execute(sql);
        while (rs.next()){

            tbody = new net.sf.json.JSONObject();
            Date date=new Date();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println(date);
            String sprq=format.format(date);
            System.out.println(sprq);
            tbody.put("OAID",Util.null2String(requestid));
            tbody.put("OASubID","");
            tbody.put("empcode",Util.null2String(rs.getString("sqrhr")));
            tbody.put("empname",Util.null2String(rs.getString("sqrhr")));
            tbody.put("OverClientele",Util.null2String(mainTableDataMap.get("jblb")));
            tbody.put("OverType",Util.null2String(mainTableDataMap.get("jblb")));
            tbody.put("OverDate",Util.null2String(mainTableDataMap.get("xuqrq"))+" "+Util.null2String(mainTableDataMap.get("xqsj")));
            tbody.put("OverBegin",rs.getString("jbksrq")+" "+rs.getString("jbkssj"));
            tbody.put("OverEnd",rs.getString("jbjssj")+" "+rs.getString("jbjssj1"));
            tbody.put("Dinnertime",0);//吃饭时间
            tbody.put("OverHour",rs.getFloat("zjbsj"));
            tbody.put("OverStatus",1);//1审核通过，2审核中，3驳回
            tbody.put("OverReason",rs.getString("jbsy"));
            list.add(tbody);
        }
        json.put("ProcParams", list);
        writeLog(interfaceName+",获取JSONObject对象："+json.toString());
        return json;
    }
}
