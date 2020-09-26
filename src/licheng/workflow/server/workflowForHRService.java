package licheng.workflow.server;

import javax.jws.WebMethod;
import javax.jws.WebService;

@WebService
public abstract interface workflowForHRService {
    @WebMethod(operationName="createWorkflow_qgd", action="urn:webService.createWorkflow_qgd")
    public abstract String createWorkflow(String fields)
            throws Exception;
}
