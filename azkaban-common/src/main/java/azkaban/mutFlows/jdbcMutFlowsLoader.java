package azkaban.mutFlows;

import azkaban.database.AbstractJdbcLoader;
import azkaban.utils.Props;
import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.ResultSetHandler;
import org.apache.log4j.Logger;

import java.sql.Connection;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Created by 邓志龙 on 2016/10/19.
 */
public class jdbcMutFlowsLoader extends AbstractJdbcLoader implements MutFlowsLoader {
    private static Logger logger = Logger.getLogger(jdbcMutFlowsLoader.class);
    public jdbcMutFlowsLoader(Props props) {
        super(props);
    }
    private EncodingType defaultEncodingType = EncodingType.PLAIN;
    private static String GET_MUTFLOWS =
            "SELECT flow_id,mut_flow_id,submit_date,project_id,mut_project_id FROM mut_flows WHERE flow_id=? and project_id=?";



    public class MutFlowsResultHandler implements ResultSetHandler<List<MutFlows>> {

        @Override
        public List<MutFlows> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return Collections.<MutFlows> emptyList();
            }

            ArrayList<MutFlows> flows = new ArrayList<MutFlows>();
            do {
                String flowId = rs.getString(1);
                String  mutFlowId = rs.getString(2);
                Date submitDate = rs.getDate(3);
                Integer projectId=rs.getInt(4);
                Integer mutProjectId=rs.getInt(5);
                Object jsonObj = null;

                MutFlows flow = new MutFlows();
                flow.setMutFlowId(mutFlowId);
                flow.setFlowId(flowId);
                flow.setSubmitDate(submitDate);
                flow.setProjectId(projectId);
                flow.setMutProjectId(mutProjectId);
                    flows.add(flow);
            } while (rs.next());

            return flows;
        }

    }
    private Connection getConnection() {
        Connection connection = null;
        try {
            connection = super.getDBConnection(false);
        } catch (Exception e) {
            DbUtils.closeQuietly(connection);
        }

        return connection;
    }
    @Override
    public List<MutFlows> loadMutFlows(String flow_id,Integer project_id){
        Connection connection = getConnection();
        QueryRunner runner = new QueryRunner();
        ResultSetHandler<List<MutFlows>> handler = new MutFlowsResultHandler();
        List<MutFlows> flows=null;
        try {
            flows = runner.query(connection, GET_MUTFLOWS, handler, flow_id,project_id);
        } catch (SQLException e) {
            logger.error(GET_MUTFLOWS + " failed.");
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return flows;
    }
}
