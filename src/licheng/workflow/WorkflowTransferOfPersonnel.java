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
 *@描述 人事异动申请流程
 *@参数
 *@返回值
 *@创建人  lzh
 *@创建时间  2020/5/9
 */
public class WorkflowTransferOfPersonnel extends BaseBean implements Action{

    private String interfaceName="人事异动申请流程对接HR接口";
    /**
     * @Author 李志辉
     * @Description 转岗申请流程
     * * @Date 2019/8/14
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
                    net.sf.json.JSONObject jsonObject2 = new net.sf.json.JSONObject();
                    jsonObject2 = new net.sf.json.JSONObject();
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
    private JSONObject getParam(Map<String,String> mainTableDataMap, int requestid, String formTableName){
        writeLog(interfaceName+",获取流程的JSONObject对象开始！");
        writeLog("表单对应参数"+mainTableDataMap.toString());
        JSONObject tbody;
        JSONObject json = new JSONObject();
        List<JSONObject> list=new ArrayList<JSONObject>();
        RecordSet rs=new RecordSet();
        json.put("ProcName", "WSPROC_TransferOfPersonnel");
        //考核月份
//        String khyf=Util.null2String(mainTableDataMap.get("kaohyf1"));
//        if (!khyf.equals("")){
//            khyf=khyf.substring(0,7).replace("-","");
//        }
        //请假单ID
        //"select bkrq,bksj,bkyy,bklx from "+formTableName+"_dt1 where mainid="+mainid
        String sql="select 1 from "+formTableName+" where requestid="+requestid;
        rs.execute(sql);
        while (rs.next()){
            tbody = new JSONObject();
            Date date=new Date();
            DateFormat format=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            System.out.println(date);
            String sprq=format.format(date);
            System.out.println(sprq);
            //OA流程编号
            tbody.put("OAID",Util.null2String(requestid));
            tbody.put("ApplyDate",Util.null2String(mainTableDataMap.get("tbrq")).length()==0?sprq:mainTableDataMap.get("tbrq"));
            tbody.put("EffectiveDate",Util.null2String(mainTableDataMap.get("sj2")));
            tbody.put("empcode",Util.null2String(mainTableDataMap.get("sqrhr")));
            tbody.put("empname","");
            tbody.put("AdjustType",Util.null2String(mainTableDataMap.get("tzlx")));//01调岗02调薪
            tbody.put("DeptBefore",Util.null2String(mainTableDataMap.get("ybm")));
            tbody.put("DeptAfter",Util.null2String(mainTableDataMap.get("drbm")));
            tbody.put("GWBefore",2521);//mainTableDataMap.get("dcgw")
            tbody.put("GWAfter",Util.null2String(mainTableDataMap.get("drgw")));//
            tbody.put("ZJBefore",Util.null2String(mainTableDataMap.get("yzd")));
            tbody.put("ZJAfter",Util.null2String(mainTableDataMap.get("jyzd")));
            tbody.put("GWLXBefore","");//Util.null2String(mainTableDataMap.get("ygw"))
            tbody.put("GWLXAfter","");//Util.null2String(mainTableDataMap.get("drgw"))
            tbody.put("A84141","");//--原薪等
            tbody.put("A84143","");//--原薪级
            tbody.put("A84145",0);
            tbody.put("A84149",0);
            tbody.put("A84151",0);
            tbody.put("A84154",0);
            tbody.put("A84142",mainTableDataMap.get("tzhzd")==null?"":mainTableDataMap.get("tzhzd"));//--拟调整薪等
            tbody.put("A84144",mainTableDataMap.get("tzhzj")==null?"":mainTableDataMap.get("tzhzj"));//--拟调整薪级
            tbody.put("A84148",(mainTableDataMap.get("dxje")==null&&mainTableDataMap.get("je")==null)?0:mainTableDataMap.get("dxje")==null?mainTableDataMap.get("je"):mainTableDataMap.get("dxje"));//--拟调整薪资总额
            tbody.put("A84150",mainTableDataMap.get("tzhjbgz")==null?0:mainTableDataMap.get("tzhjbgz"));//--拟调整基本
            tbody.put("A84153",mainTableDataMap.get("tzhjxgz")==null?0:mainTableDataMap.get("tzhjxgz"));//--拟调整绩效工资
            tbody.put("A84155",0);//--拟调整岗位津贴
            tbody.put("A84156",0);//--原加班工资
            tbody.put("A84157",mainTableDataMap.get("tzgz2")==null?0:mainTableDataMap.get("tzgz2"));//--拟调整加班工资
            tbody.put("A8419",mainTableDataMap.get("txly")==null?"":mainTableDataMap.get("txly"));//--拟调整加班工资
            list.add(tbody);
        }
        json.put("ProcParams", list);
        writeLog(interfaceName+",获取JSONObject对象："+json.toString());
        return json;
    }
}
