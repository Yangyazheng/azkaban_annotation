package azkaban.depFlows;

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
public class jdbcDepFlowsLoader extends AbstractJdbcLoader implements DepFlowsLoader {
    private static Logger logger = Logger.getLogger(jdbcDepFlowsLoader.class);

    public jdbcDepFlowsLoader(Props props) {
        super(props);
    }

    private static String GET_DEPFLOWS =
            "SELECT flow_id,dep_flow_id,submit_date,project_id,dep_project_id FROM dep_flows WHERE flow_id=? and project_id=?";
    private static String GET_EXEFLOWS =
            "select count(*) from execution_flows where flow_id=? and `status` in (10,20,30,40,50)";


    public class DepFlowsResultHandler implements ResultSetHandler<List<DepFlows>> {

        @Override
        public List<DepFlows> handle(ResultSet rs) throws SQLException {
            if (!rs.next()) {
                return Collections.<DepFlows>emptyList();
            }

            ArrayList<DepFlows> flows = new ArrayList<DepFlows>();
            do {
                String flowId = rs.getString(1);
                String depFlowId = rs.getString(2);
                Date submitDate = rs.getDate(3);
                Integer projectId=rs.getInt(4);
                Integer depProjectId=rs.getInt(5);
                Object jsonObj = null;

                DepFlows flow = new DepFlows();
                flow.setDepFlowId(depFlowId);
                flow.setFlowId(flowId);
                flow.setSubmitDate(submitDate);
                flow.setProjectId(projectId);
                flow.setDepProjectId(depProjectId);

                flows.add(flow);
            } while (rs.next());

            return flows;
        }

    }
/*public class  ExeFlowsResultHandler implements  ResultSetHandler<Integer>
{
    @Override
    public Integer handle(ResultSet rs) throws SQLException {
        System.out.println(rs.getRow()+"-----");
        do {
            System.out.println(rs.getString("count")+"----===");
        } while (rs.next());
        return null;
    }

}*/
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
    public List<DepFlows> loadDepFlows(String flow_id,Integer projectId) {
        Connection connection = getConnection();
        QueryRunner runner = new QueryRunner();
        ResultSetHandler<List<DepFlows>> handler = new DepFlowsResultHandler();
        List<DepFlows> flows = null;
        try {
            flows = runner.query(connection, GET_DEPFLOWS, handler, flow_id,projectId);
        } catch (SQLException e) {
            logger.error(GET_DEPFLOWS + " failed.");
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return flows;
    }
   /* @Override
    public int checkMutFlows(String flow_id) {
        Connection connection = getConnection();
        QueryRunner runner = new QueryRunner();
        ResultSetHandler<Integer> handler = new ExeFlowsResultHandler();
        Integer count = 0;
        try {
             runner.query(connection, GET_EXEFLOWS, handler, flow_id);
           // System.out.println(count);
        } catch (SQLException e) {
            logger.error(GET_EXEFLOWS + " failed.");
        } finally {
            DbUtils.closeQuietly(connection);
        }
        return count;
    }*/
  /*  public static void main(String[] args) {
        Props props = new Props();
        props.put("mysql.port", 3306);
        props.put("database.type","mysql");
        props.put("mysql.host","172.20.0.28");
        props.put("mysql.database","azkaban");
        props.put("mysql.user","root");
        props.put("mysql.password","handhand");
        props.put("mysql.numconnections", 10);
        new jdbcDepFlowsLoader(props).loadDepFlows("test2",0);
    }*/
}
