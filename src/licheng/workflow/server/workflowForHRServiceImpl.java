package licheng.workflow.server;

import net.sf.json.JSONObject;
import weaver.conn.RecordSet;
import weaver.general.BaseBean;
import weaver.general.Util;
import weaver.workflow.webservices.*;

import java.util.Iterator;
import java.util.List;

public class workflowForHRServiceImpl implements workflowForHRService {
    private BaseBean log=new BaseBean();
//    private String creatUserId="329";
    /**
     * @Author Zhoujz
     * @Description NC调用接口主文件，用于逻辑判断
     * @Date 2019/8/14 10:37
     * @Param String fields （流程的JSON字符串）
     * @return String   （JSONObject字符串）
     **/
    @Override
    public String createWorkflow(String fields) {
        String tablename,requestid;
        JSONObject result_json = new JSONObject();
        RecordSet rs=new RecordSet();
        log.writeLog("workflow接口，开始！");
        log.writeLog("workflow接口。获取异构系统传过来的json字符串:"+fields);
        JSONObject jsonObject = JSONObject.fromObject(fields);
        try{
            //主表字段
            JSONObject main_json = JSONObject.fromObject(jsonObject.get("mainData"));
            //工作流ID
            String workflowid=(String) jsonObject.get("workflowid");
            String ids= Util.null2String(main_json.get("ids"));
            String sql="select b.tablename from workflow_base a join workflow_bill b on a.formid=b.id where a.id='"+workflowid+"'";
            log.writeLog("workflowFor接口，获取工作流表单的SQL："+sql);
            log.writeLog("workflowFor接口，获取工作流表单的入职单据PK："+ids);
            rs.execute(sql);
            if (rs.next()){
                tablename=Util.null2String(rs.getString("tablename"));
                log.writeLog("workflowFor接口，获取工作流表单的表名："+tablename);
                sql="select * from "+tablename+" where id='"+ids+"'";
                log.writeLog("workflowFor接口，获取工作流表单的SQL："+sql);
                rs.execute(sql);
                if (rs.next()){
                    //存在重复数据，更新数据，调用提交流程接口
                    requestid=Util.null2String(rs.getString("requestid"));
                    result_json=submitOldWorkflow(fields,requestid);
                }else{
                    //不存在重复数据，调用创建流程接口
                    result_json=createNewWorkflow(fields);
                }
            }else{
                result_json.put("result","0");
                result_json.put("message","workflowFor接口，获取流程表名失败！");
            }
        }catch (Exception err){
            result_json.put("result","0");
            result_json.put("message","接口执行出错，错误代码："+err);
        }
        return result_json.toString();
    }
    /**
     * @Author Zhoujz
     * @Description 创建新流程的通用方法
     * @Date 2019/8/14 10:37
     * @Param String fields （流程的JSON字符串）
     * @return net.sf.json.JSONObject
     **/
    private JSONObject createNewWorkflow(String fields){
        RecordSet rs=new RecordSet();
        JSONObject result_json = new JSONObject();
        try{
            log.writeLog("workflowFor接口。不存在重复数据，调用创建流程接口！");
            JSONObject detail_json;
            JSONObject jsonObject = JSONObject.fromObject(fields);
            String workflowid,creatorId,requestName,requestLevel,messageType,key,value;
            //主表字段
            JSONObject main_json = JSONObject.fromObject(jsonObject.get("mainData"));
            //明细字段
            List detail_list = (List) jsonObject.get("detailData");
            //工作流ID
            workflowid=(String) jsonObject.get("workflowid");
            //创建人ID
            creatorId= (String) jsonObject.get("creatorId");
            //紧急程度
            requestLevel=(String) jsonObject.get("requestLevel");
            //流程标题
            requestName=(String) jsonObject.get("requestName");
            //短信提醒类型
            messageType="2";
            //主表字段赋值
            WorkflowRequestTableField[] wrti = new WorkflowRequestTableField[main_json.size()];
            log.writeLog("workflowFor创建流程接口。主表字段json:"+main_json.toString());
            Iterator iterator = main_json.keys();
            int sign=0;
            while(iterator.hasNext()){
                key = (String) iterator.next();
                value = main_json.getString(key);
                log.writeLog("workflowFor创建流程接口。主表赋值。字段:"+key+";值："+value);
                wrti[sign] = new WorkflowRequestTableField();
                wrti[sign].setFieldName(key);
                wrti[sign].setFieldValue(value);
                wrti[sign].setView(true);
                wrti[sign].setEdit(true);
                sign++;
            }
            WorkflowRequestTableRecord[] wrtri = new WorkflowRequestTableRecord[1];
            wrtri[0] = new WorkflowRequestTableRecord();
            wrtri[0].setWorkflowRequestTableFields(wrti);
            WorkflowMainTableInfo wmi = new WorkflowMainTableInfo();
            wmi.setRequestRecords(wrtri);
            //明细字段赋值
            // 明细字段
            log.writeLog("workflowFor创建流程接口。明细的list:"+detail_list);
            WorkflowRequestTableField[] detailField1;
            WorkflowDetailTableInfo wdti[] = new WorkflowDetailTableInfo[1];// 明细表1
            WorkflowRequestTableRecord[] detailRecord = new WorkflowRequestTableRecord[detail_list.size()];// 数据行数
            log.writeLog("workflowFor创建流程接口。明细行数:"+detail_list.size());
            for (int i = 0; i < detail_list.size(); i++) {
                log.writeLog("workflowFor创建流程接口。开始赋值第"+ i+1 +"行！");
                sign=0;
                detail_json= (JSONObject) detail_list.get(i);
                detailField1 = new WorkflowRequestTableField[detail_json.size()]; // 每行字段数量
                iterator = detail_json.keys();
                while(iterator.hasNext()){
                    key = (String) iterator.next();
                    value = detail_json.getString(key);
                    log.writeLog("workflowFor创建流程接口。明细赋值key:"+key);
                    log.writeLog("workflowFor创建流程接口。明细赋值value:"+value);
                    detailField1[sign] = new WorkflowRequestTableField();
                    detailField1[sign].setFieldName(key);
                    detailField1[sign].setFieldValue(value);
                    detailField1[sign].setView(true);
                    detailField1[sign].setEdit(true);
                    sign++;
                }
                detailRecord[i] = new WorkflowRequestTableRecord();
                detailRecord[i].setWorkflowRequestTableFields(detailField1);
            }
            wdti[0] = new WorkflowDetailTableInfo();
            wdti[0].setWorkflowRequestTableRecords(detailRecord);
            WorkflowBaseInfo wbi = new WorkflowBaseInfo();
            //工作流id
            wbi.setWorkflowId(workflowid);
            //流程基本信息
            WorkflowRequestInfo wri = new WorkflowRequestInfo();
            //创建人id
            wri.setCreatorId(creatorId);
            //0正常，1重要，2紧急
            wri.setRequestLevel(requestLevel);
            //流程标题
            wri.setRequestName(requestName);
            //短信提醒类型
            wri.setMessageType(messageType);
            //添加主字段数据
            wri.setWorkflowMainTableInfo(wmi);
            //添加明细字段数据
            wri.setWorkflowBaseInfo(wbi);
            wri.setWorkflowDetailTableInfos(wdti);
            wri.setRemark("HR系统提交流程请求");
            WorkflowService workflowService = new WorkflowServiceImpl();
            log.writeLog("workflowHR创建流程接口。不存在重复数据，调用创建流程接口！开始调用接口！");
            String returnStr=workflowService.doCreateWorkflowRequest(wri, Integer.parseInt(creatorId));
            log.writeLog("workflowHR创建流程接口。不存在重复数据，调用创建流程接口！调用接口成功，返回："+returnStr);
            int result= Util.getIntValue(returnStr);
            log.writeLog("-------------------"+result);
            if (result > 0){
                result_json.put("result","1");
                result_json.put("message","流程创建成功");//192.168.1.172:8777
                result_json.put("requestid",returnStr);
                result_json.put("url","192.168.1.106:8082/workflow/request/ViewRequest.jsp?requestid="+returnStr+"&ismonitor=1&ssoToken=");
            }else if (result == -1){
                result_json.put("result","0");
                result_json.put("message","创建流程失败");
            }else if (result == -2){
                result_json.put("result","0");
                result_json.put("message","用户没有流程创建权限");
            }else if (result == -3){
                result_json.put("result","0");
                result_json.put("message","创建流程基本信息失败");
            }else if (result == -4){
                result_json.put("result","0");
                result_json.put("message","保存表单主表信息失败");
            }else if (result == -5){
                result_json.put("result","0");
                result_json.put("message","更新紧急程度失败");
            }else if (result == -6){
                result_json.put("result","0");
                result_json.put("message","流程操作者失败");
            }else if (result == -7){
                result_json.put("result","0");
                result_json.put("message","流转至下一节点失败");
            }else if (result == -8){
                result_json.put("result","0");
                result_json.put("message","节点附加操作失败");
            }else{
                result_json.put("result","0");
                result_json.put("message","创建流程失败，未知错误代码！");
            }
        }catch (Exception err){
            result_json.put("result","0");
            result_json.put("message","创建流程出错，错误代码："+err);
        }
        return result_json;
    }
    /**
     * @Author Zhoujz
     * @Description 更新并提交原有流程的通用方法
     * @Date 2019/8/14 10:37
     * @Param String fields   （流程的JSON字符串）
     * @Param String requestid     （原流程的请求ID）
     * @return net.sf.json.JSONObject
     **/
    private JSONObject submitOldWorkflow(String fields,String requestid){
        JSONObject result_json = new JSONObject();
        try{
            log.writeLog("workflowForNC流程接口。存在重复数据，调用提交流程接口！");
            JSONObject detail_json;
            JSONObject jsonObject = JSONObject.fromObject(fields);
            String workflowid,creatorId,requestName,requestLevel,messageType,key,value;
            //主表字段
            JSONObject main_json = JSONObject.fromObject(jsonObject.get("mainData"));
            //明细字段
            List detail_list = (List) jsonObject.get("detailData");
            //工作流ID
            workflowid=(String) jsonObject.get("workflowid");
            //创建人ID
            creatorId= (String) jsonObject.get("creatorId");
            //紧急程度
            requestLevel=(String) jsonObject.get("requestLevel");
            //流程标题
            requestName=(String) jsonObject.get("requestName");
            //短信提醒类型
            messageType="2";
            //主表字段赋值
            log.writeLog("workflowForHR更新流程接口。主表字段json:"+main_json.toString());
            WorkflowRequestTableField[] wrti = new WorkflowRequestTableField[main_json.size()];
            Iterator iterator = main_json.keys();
            int sign=0;
            while(iterator.hasNext()){
                key = (String) iterator.next();
                value = main_json.getString(key);
                log.writeLog("workflowForHR更新流程接口。主表赋值。字段:"+key+";值："+value);
                wrti[sign] = new WorkflowRequestTableField();
                wrti[sign].setFieldName(key);
                wrti[sign].setFieldValue(value);
                wrti[sign].setView(true);
                wrti[sign].setEdit(true);
                sign++;
            }
            WorkflowRequestTableRecord[] wrtri = new WorkflowRequestTableRecord[1];
            wrtri[0] = new WorkflowRequestTableRecord();
            wrtri[0].setWorkflowRequestTableFields(wrti);
            WorkflowMainTableInfo wmi = new WorkflowMainTableInfo();
            wmi.setRequestRecords(wrtri);
            //明细字段赋值
            // 明细字段
            log.writeLog("workflowForHR更新流程接口。明细的list:"+detail_list);
            WorkflowRequestTableField[] detailField1;
            WorkflowDetailTableInfo wdti[] = new WorkflowDetailTableInfo[1];// 明细表1
            WorkflowRequestTableRecord[] detailRecord = new WorkflowRequestTableRecord[detail_list.size()];// 数据行数
            log.writeLog("workflowForHR更新流程接口。明细行数:"+detail_list.size());
            for (int i = 0; i < detail_list.size(); i++) {
                log.writeLog("workflowForHR更新流程接口。开始赋值第"+ i+1 +"行！");
                sign=0;
                detail_json= (JSONObject) detail_list.get(i);
                detailField1 = new WorkflowRequestTableField[detail_json.size()]; // 每行字段数量
                iterator = detail_json.keys();
                while(iterator.hasNext()){
                    key = (String) iterator.next();
                    value = detail_json.getString(key);
                    log.writeLog("workflowForHR更新流程接口。明细赋值key:"+key);
                    log.writeLog("workflowForHR更新流程接口。明细赋值value:"+value);
                    detailField1[sign] = new WorkflowRequestTableField();
                    detailField1[sign].setFieldName(key);
                    detailField1[sign].setFieldValue(value);
                    detailField1[sign].setView(true);
                    detailField1[sign].setEdit(true);
                    sign++;
                }
                detailRecord[i] = new WorkflowRequestTableRecord();
                detailRecord[i].setWorkflowRequestTableFields(detailField1);
            }
            wdti[0] = new WorkflowDetailTableInfo();
            wdti[0].setWorkflowRequestTableRecords(detailRecord);
            String type="submit";
            String remark="HR已完成表单数据修改，再次提交！";
            WorkflowBaseInfo wbi = new WorkflowBaseInfo();
            wbi.setWorkflowId(workflowid);                                              //工作流id
            WorkflowRequestInfo wri = new WorkflowRequestInfo();    //流程基本信息
            wri.setCreatorId(creatorId);                                                    //创建人id
            wri.setRequestLevel(requestLevel);                                       //0正常，1重要，2紧急
            wri.setRequestName(requestName);                                     //流程标题
            wri.setMessageType(messageType);                                     //短信提醒类型
            wri.setWorkflowMainTableInfo(wmi);                                     //添加主字段数据
            wri.setWorkflowBaseInfo(wbi);
            wri.setWorkflowDetailTableInfos(wdti);                                  //添加明细字段数据
            wri.setRemark("HR系统提交流程请求");
            WorkflowService workflowService = new WorkflowServiceImpl();
            log.writeLog("workflowForHR流程接口。存在重复数据，调用提交流程接口！开始调用接口！");
            String returnStr=workflowService.submitWorkflowRequest(wri,Util.getIntValue(requestid),Util.getIntValue(creatorId),type,remark);
            log.writeLog("workflowForHR流程接口。存在重复数据，调用提交流程接口！调用接口成功，返回："+returnStr);
            if (returnStr.equals("error")){
                result_json.put("result","0");
                result_json.put("message","再次提交流程请求出错："+returnStr);
            }else if (returnStr.equals("failed")){
                result_json.put("result","0");
                result_json.put("message","再次提交流程失败："+returnStr);
            }else if (returnStr.equals("success")){
                result_json.put("result","2");
                result_json.put("message","再次提交流程成功："+returnStr);
            }else{
                result_json.put("result","0");
                result_json.put("message","再次提交流程失败，未知错误代码！");
            }
        }catch (Exception err){
            result_json.put("result","0");
            result_json.put("message","再次提交流程出错，错误代码："+err);
        }
        return result_json;
    }
}
