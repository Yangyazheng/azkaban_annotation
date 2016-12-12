package azkaban.mutFlows;

import java.util.Date;

/**
 * Created by 邓志龙 on 2016/10/19.
 */
public class MutFlows {
    public String getFlowId() {
        return flowId;
    }

    public void setFlowId(String flowId) {
        this.flowId = flowId;
    }

    public String getMutFlowId() {
        return mutFlowId;
    }

    public void setMutFlowId(String mutFlowId) {
        this.mutFlowId = mutFlowId;
    }

    public Date getSubmitDate() {
        return submitDate;
    }

    public void setSubmitDate(Date submitDate) {
        this.submitDate = submitDate;
    }

    private String flowId;
    private String mutFlowId;
    private Date submitDate;

    public Integer getProjectId() {
        return projectId;
    }

    public void setProjectId(Integer projectId) {
        this.projectId = projectId;
    }

    public Integer getMutProjectId() {
        return mutProjectId;
    }

    public void setMutProjectId(Integer mutProjectId) {
        this.mutProjectId = mutProjectId;
    }

    private Integer projectId;
    private Integer mutProjectId;

}
