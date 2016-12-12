package azkaban.depFlows;

import java.util.List;

/**
 * Created by 邓志龙 on 2016/10/19.
 */
public interface DepFlowsLoader {
    public List<DepFlows> loadDepFlows(String flow_id, Integer project_id);
    //public int checkMutFlows(String flow_id);
}
