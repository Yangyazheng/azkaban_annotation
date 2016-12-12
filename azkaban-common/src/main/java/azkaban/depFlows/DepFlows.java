package azkaban.depFlows;

import java.util.Date;

/**
 * Created by 邓志龙 on 2016/10/19.
 */
public class DepFlows {
    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getDepFlowId() {
        return depFlowId;
    }

    public void setDepFlowId(String depFlowId) {
        this.depFlowId = depFlowId;
    }

    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    private String flowId;
    private String depFlowId;
    private Date submitDate;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getDepProjectId() {
        return depProjectId;
    }

    public void setDepProjectId(Integer depProjectId) {
        this.depProjectId = depProjectId;
    }

    private Integer projectId;
    private Integer depProjectId;

    @Override
    public String toString() {
        return "DepFlows{" +
                "flowId='" + flowId + '\'' +
                ", depFlowId='" + depFlowId + '\'' +
                ", submitDate=" + submitDate +
                ", projectId=" + projectId +
                ", depProjectId=" + depProjectId +
                '}';
    }
}
