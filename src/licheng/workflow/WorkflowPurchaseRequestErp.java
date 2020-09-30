package licheng.workflow;

import com.licheng.workflow.utile.GeneralToolForHR;
import licheng.workflow.utile.InvokeHelper;
import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.TimeUtil;
import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.Property;
import weaver.soa.workflow.request.RequestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *@描述 考勤出差外出申请流程
 *@参数
 *@返回值
 *@创建人  lzh
 *@创建时间  2020/5/9
 */
public class WorkflowPurchaseRequestErp extends BaseBean implements Action{
    private String interfaceName="OA生成ERP采购申请单";

    @Override
    public String execute(RequestInfo request){
        String message="";
        InvokeHelper.POST_K3CloudURL = "http://192.168.1.152/k3cloud/";
        String dbId = "5f49dd5adf3429";
        String uid = "Administrator";
        String pwd = "888888";
        int lang = 2052;
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
                if (InvokeHelper.Login(dbId, uid, pwd, lang)) {
                    writeLog("登录成功");
                    writeLog(interfaceName + ",获取凭证成功!");
                    parameter = getParam(mainTableDataMap, requestid,formTableName);
                    String s = InvokeHelper.Save("PUR_Requisition", parameter.toString());
                }else {
                    writeLog(interfaceName + ",登录失败:");
                    request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
                    request.getRequestManager().setMessagecontent("提示信息：" + interfaceName + ",登录失败：没有成功,请联系系统管理员！");
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
        JSONObject tbody;
        JSONObject json = new JSONObject();
        List<JSONObject> list=new ArrayList<JSONObject>();
        RecordSet rs=new RecordSet();
        json.put("ProcName", "WSPROC_AttGOOB");
        //考核月份
//        String khyf=Util.null2String(mainTableDataMap.get("kaohyf1"));
//        if (!khyf.equals("")){
//            khyf=khyf.substring(0,7).replace("-","");
//        }
        //请假单ID
        String sql="select * from "+formTableName+"_dt1 where requestid="+requestid;
        rs.execute(sql);
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("NeedUpDateFields",new Object[0]);
        jsonObject.put("NeedReturnFields",new Object[0]);
        jsonObject.put("IsDeleteEntry","true");
        jsonObject.put("SubSystemId","");
        jsonObject.put("IsVerifyBaseDataField","false");
        jsonObject.put("IsEntryBatchFill","true");
        jsonObject.put("ValidateFlag","true");
        jsonObject.put("NumberSearch","true");
        jsonObject.put("InterationFlags","");
        JSONObject Model = new JSONObject();
        Model.put("FID",0);
        JSONObject FBillTypeID = new JSONObject();
        FBillTypeID.put("FNUMBER","CGSQD01_SYS");
        Model.put("FBillTypeID",FBillTypeID);
        Model.put("FApplicationDate",mainTableDataMap.get("sqrq"));
        Model.put("FRequestType","Material");
        JSONObject FApplicationOrgId = new JSONObject();
        FApplicationOrgId.put("FNUMBER","10101");
        Model.put("FApplicationOrgId",FApplicationOrgId);
        JSONObject FCurrencyId = new JSONObject();
        FCurrencyId.put("FNUMBER","PRE001");
        Model.put("FCurrencyId",FCurrencyId);
        Model.put("FISPRICEEXCLUDETAX",true);
        JSONObject FExchangeTypeId = new JSONObject();
        FExchangeTypeId.put("FNUMBER","HLTX01_SYS");
        Model.put("FExchangeTypeId",FExchangeTypeId);
        Model.put("FIsConvert",false);
        JSONObject FMobBillHead = new JSONObject();
        FMobBillHead.put("FIsMobBill",false);
        FMobBillHead.put("FMobIsPending",false);
        Model.put("FMobBillHead",FMobBillHead);
        List<JSONObject> FEntity=new ArrayList<JSONObject>();
        while (rs.next()){
            JSONObject FEntity1 = new JSONObject();
            JSONObject FRequireOrgId = new JSONObject();
            FRequireOrgId.put("FNUMBER","10101");
            FEntity1.put("FRequireOrgId",FRequireOrgId);
            JSONObject FMaterialId = new JSONObject();
            FMaterialId.put("FNUMBER",rs.getString("k3cloudwl"));
            FEntity1.put("FMaterialId",FMaterialId);
            FEntity1.put("FMaterialDesc",rs.getString("wlmct"));
            JSONObject FUnitId = new JSONObject();
            FUnitId.put("FNUMBER",rs.getString("dwid"));
            FEntity1.put("FUnitId",FUnitId);
            FEntity1.put("FReqQty",rs.getFloat("sl"));
            FEntity1.put("FApproveQty",rs.getFloat("sl"));
            JSONObject FPurchaseOrgId = new JSONObject();
            FPurchaseOrgId.put("FNUMBER","10101");
            FEntity1.put("FPurchaseOrgId",FPurchaseOrgId);
            JSONObject FReceiveOrgId = new JSONObject();
            FReceiveOrgId.put("FNUMBER","10101");
            FEntity1.put("FReceiveOrgId",FReceiveOrgId);
            JSONObject FPriceUnitId = new JSONObject();
            FPriceUnitId.put("FNUMBER",rs.getString("dwid"));
            FEntity1.put("FPriceUnitId",FPriceUnitId);
            FEntity1.put("FPriceUnitQty",rs.getFloat("sl"));
            JSONObject FREQSTOCKUNITID = new JSONObject();
            FREQSTOCKUNITID.put("FNUMBER",rs.getString("dwid"));
            FEntity1.put("FREQSTOCKUNITID",FREQSTOCKUNITID);
            JSONObject FSalUnitID = new JSONObject();
            FSalUnitID.put("FNUMBER",rs.getString("dwid"));
            FEntity1.put("FSalUnitID",FSalUnitID);
            FEntity1.put("FREQSTOCKQTY",rs.getFloat("sl"));
            FEntity1.put("FBaseReqQty",rs.getFloat("sl"));
            FEntity1.put("FSalQty",rs.getFloat("sl"));
            FEntity1.put("FSalBaseQty",rs.getFloat("sl"));
            FEntity1.put("FIsVmiBusiness",false);
            FEntity1.put("FEntryNote",rs.getString("k3cloudckname"));
            FEntity.add(FEntity1);
        }
        Model.put("FEntity",FEntity);
        jsonObject.put("Model",Model);
        writeLog(interfaceName+",获取JSONObject对象："+jsonObject.toString());
        return jsonObject;
    }
}
