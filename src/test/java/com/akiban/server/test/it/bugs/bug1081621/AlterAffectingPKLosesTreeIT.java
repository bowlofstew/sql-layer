/**
 * END USER LICENSE AGREEMENT (“EULA”)
 *
 * READ THIS AGREEMENT CAREFULLY (date: 9/13/2011):
 * http://www.akiban.com/licensing/20110913
 *
 * BY INSTALLING OR USING ALL OR ANY PORTION OF THE SOFTWARE, YOU ARE ACCEPTING
 * ALL OF THE TERMS AND CONDITIONS OF THIS AGREEMENT. YOU AGREE THAT THIS
 * AGREEMENT IS ENFORCEABLE LIKE ANY WRITTEN AGREEMENT SIGNED BY YOU.
 *
 * IF YOU HAVE PAID A LICENSE FEE FOR USE OF THE SOFTWARE AND DO NOT AGREE TO
 * THESE TERMS, YOU MAY RETURN THE SOFTWARE FOR A FULL REFUND PROVIDED YOU (A) DO
 * NOT USE THE SOFTWARE AND (B) RETURN THE SOFTWARE WITHIN THIRTY (30) DAYS OF
 * YOUR INITIAL PURCHASE.
 *
 * IF YOU WISH TO USE THE SOFTWARE AS AN EMPLOYEE, CONTRACTOR, OR AGENT OF A
 * CORPORATION, PARTNERSHIP OR SIMILAR ENTITY, THEN YOU MUST BE AUTHORIZED TO SIGN
 * FOR AND BIND THE ENTITY IN ORDER TO ACCEPT THE TERMS OF THIS AGREEMENT. THE
 * LICENSES GRANTED UNDER THIS AGREEMENT ARE EXPRESSLY CONDITIONED UPON ACCEPTANCE
 * BY SUCH AUTHORIZED PERSONNEL.
 *
 * IF YOU HAVE ENTERED INTO A SEPARATE WRITTEN LICENSE AGREEMENT WITH AKIBAN FOR
 * USE OF THE SOFTWARE, THE TERMS AND CONDITIONS OF SUCH OTHER AGREEMENT SHALL
 * PREVAIL OVER ANY CONFLICTING TERMS OR CONDITIONS IN THIS AGREEMENT.
 */

package com.akiban.server.test.it.bugs.bug1081621;

import com.akiban.ais.model.TableName;
import com.akiban.ais.util.TableChangeValidator;
import com.akiban.server.test.it.ITBase;
import org.junit.Test;

public class AlterAffectingPKLosesTreeIT extends ITBase {
    private final static String SCHEMA = "test";
    private final static TableName P_NAME = new TableName(SCHEMA, "p");
    private final static TableName C_NAME = new TableName(SCHEMA, "c");

    private void createTables() {
        createTable(P_NAME, "id int not null primary key, x int");
        createTable(C_NAME, "id int not null primary key, pid int, grouping foreign key(pid) references p(id)");
    }

    @Test
    public void test() throws Exception {
        createTables();

        runAlter(TableChangeValidator.ChangeLevel.GROUP, SCHEMA, "ALTER TABLE p DROP COLUMN id");
        runAlter(TableChangeValidator.ChangeLevel.GROUP, SCHEMA, "ALTER TABLE c DROP COLUMN id");

        ddl().dropTable(session(), P_NAME);
        ddl().dropTable(session(), C_NAME);

        safeRestartTestServices();

        createTables();
    }
}
