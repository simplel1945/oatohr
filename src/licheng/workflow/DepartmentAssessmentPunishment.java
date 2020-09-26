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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DepartmentAssessmentPunishment extends BaseBean implements Action{
	private String interfaceName="对接HR接口";

	public String execute(RequestInfo request){
        String urlEncrypt = getPropValue("workflowhr", "urlEncrypt");
        String getTokenUrl = getPropValue("workflowhr", "getTokenUrl");
        String urlPPostDat = getPropValue("workflowhr", "urlPPostDat");
        String urlPGetData = getPropValue("workflowhr", "urlPGetData");
        String userNameOrEmailAddress = getPropValue("workflowhr", "userNameOrEmailAddress");

		writeLog(interfaceName+",开始！");
		boolean judge=false;
		int requestid = request.getRequestManager().getRequestid();
		Map<String,String> mainTableDataMap = new HashMap<String,String>();
		Property[] properties = request.getMainTableInfo().getProperty();
		for (Property property : properties) {
			String name = property.getName();
			String value = Util.null2String(property.getValue());
			mainTableDataMap.put(name, value);
		}
		writeLog(interfaceName+",获取主表数据成功!");
		try {
			String accessToken="";
			JSONObject parameter = new JSONObject();
			GeneralToolForHR gtfr=new GeneralToolForHR();
			JSONObject Encrypt = gtfr.doPost(interfaceName,urlEncrypt,parameter,accessToken);
			if (Encrypt.getBoolean("success")){
				writeLog(interfaceName+",获取凭证成功!");

                JSONObject jsonObject2 = new JSONObject();
                jsonObject2 = new JSONObject();
                jsonObject2.put("userNameOrEmailAddress", userNameOrEmailAddress);
                jsonObject2.put("password", Encrypt.get("result").toString());
                jsonObject2.put("language", "string");
                jsonObject2.put("platform", 0);
                jsonObject2.put("clientVersion", "string");
                jsonObject2.put("curNum", 0);
                jsonObject2.put("rememberClient", true);

                JSONObject login_json = gtfr.doPost(interfaceName,getTokenUrl,jsonObject2,accessToken);
                if(login_json.getBoolean("success")){
                    accessToken=login_json.getJSONObject("result").get("accessToken").toString();
                    accessToken="Bearer "+accessToken;
                    parameter=getParam(mainTableDataMap,requestid);
                    JSONObject result = gtfr.doPost(interfaceName,urlPPostDat,parameter,accessToken);
                    if (result.getBoolean("success")){
                        judge=true;
                        JSONArray data=result.getJSONObject("result").getJSONArray("data");
                        writeLog(interfaceName+",接口返回成功："+data.toString());
                    }else{
                        writeLog(interfaceName+",接口返回失败："+result.get("error").toString());
                        request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
                        request.getRequestManager().setMessagecontent("提示信息：" + interfaceName+",失败：校验凭证不通过,请联系系统管理员！"+result.toString());
                    }
                }
			}else{
				writeLog(interfaceName+",获取凭证失败!");
				request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
				request.getRequestManager().setMessagecontent("提示信息：" + interfaceName+",获取凭证失败,请联系系统管理员！");
			}
		} catch (Exception e) {
			writeLog(interfaceName+",出错"+e);
			request.getRequestManager().setMessageid(requestid + "-" + TimeUtil.getCurrentTimeString());
			request.getRequestManager().setMessagecontent("提示信息：" + interfaceName+",出错："+e+",请联系系统管理员！");
		}
		if (judge){
			writeLog(interfaceName+",结束！pass");
			return SUCCESS;
		}else{
			writeLog(interfaceName+",结束！do not pass");
			return FAILURE_AND_CONTINUE;
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
	private JSONObject getParam(Map<String,String> mainTableDataMap,int requestid){
		writeLog(interfaceName+",获取流程的JSONObject对象开始！");
		JSONObject json_;
		JSONObject json = new JSONObject();
		List<JSONObject> list=new ArrayList<JSONObject>();
		RecordSet rs=new RecordSet();

		json.put("ProcName", "WsProc_into_KH");

		//考核月份
		String khyf=Util.null2String(mainTableDataMap.get("kaohyf1"));
		if (!khyf.equals("")){
			khyf=khyf.substring(0,7).replace("-","");
		}
		//请假单ID
		String sql="select b.id,b.gongh,b.jiangj from formtable_main_122 a join formtable_main_122_dt1 b on a.id=b.mainid where a.requestid='"+requestid+"'";
		String gongh,jiangj;
		rs.execute(sql);
		while (rs.next()){
			json_ = new JSONObject();
			gongh=Util.null2String(rs.getString("gongh"));				//工号
			jiangj=Util.null2String(rs.getString("jiangj"));			//奖金
			json_.put("Gongh", gongh);
			json_.put("kaohyf1", khyf);
			json_.put("jiangj", jiangj);
			list.add(json_);
		}
		json.put("ProcParams", list);
		writeLog(interfaceName+",获取JSONObject对象："+json.toString());
		return json;
	}
}