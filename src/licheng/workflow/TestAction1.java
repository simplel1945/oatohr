package licheng.workflow;

import weaver.general.Util;
import weaver.interfaces.workflow.action.Action;
import weaver.soa.workflow.request.Property;
import weaver.soa.workflow.request.RequestInfo;

import java.util.HashMap;
import java.util.Map;

import static org.yozopdf.core.util.LogWriter.writeLog;
import static weaver.file.Prop.getPropValue;

public class TestAction1 implements Action{
    private String interfaceName="入账流程参数对接HR接口";
    @Override
    public String execute(RequestInfo request) {
        String workName="请假流程测试";
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
        writeLog(mainTableDataMap.toString());
        writeLog(urlEncrypt);
        writeLog(getTokenUrl);
        writeLog(urlPPostDat);
        writeLog(userNameOrEmailAddress);
        writeLog(workName+"对接NC接口开始执行！");
        return Action.FAILURE_AND_CONTINUE;
    }
}
