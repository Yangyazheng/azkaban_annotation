package azkaban.mutFlows;

import java.util.List;

/**
 * Created by yyz on 2016/10/19.
 */
public interface MutFlowsLoader {
    public List<MutFlows> loadMutFlows(String flow_id, Integer project_id);
}
