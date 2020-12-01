package eclipselink.bug;


import org.eclipse.persistence.platform.database.H2Platform;
import org.eclipse.persistence.queries.ValueReadQuery;

/**
 * Since H2 1.4.198, "CURRENT_TIMESTAMP" returns a timestamp with time zone, which is not what EclipseLink expects.
 * Therefore, use the new function "LOCALTIMESTAMP" (alias "NOW()") which returns a timestamp without time zone.
 * 
 * @author alr
 * @since 02.11.2019
 */
public class H2Platform_1_4_198 extends H2Platform {
    private static final long serialVersionUID = 1;
    
    @Override
    public ValueReadQuery getTimestampQuery() {
        if (timestampQuery == null) {
            timestampQuery = new ValueReadQuery();
            timestampQuery.setSQLString("SELECT NOW()");
            timestampQuery.setAllowNativeSQLQuery(Boolean.TRUE);
        }
        return super.getTimestampQuery();
    }
}
