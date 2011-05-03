/**
 * Copyright (C) 2011 Akiban Technologies Inc.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License, version 3,
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package com.akiban.server.service.tree;

import com.akiban.server.service.config.Property;
import com.akiban.server.test.it.ITBase;
import com.persistit.Exchange;
import com.persistit.Tree;
import com.persistit.exception.PersistitException;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TreeServiceImplVolumeIT extends ITBase {

    private class TestLink implements TreeLink {
        final String schemaName;
        final String treeName;
        TreeCache cache;

        TestLink(String s, String t) {
            schemaName = s;
            treeName = t;
        }

        @Override
        public String getSchemaName() {
            return schemaName;
        }

        @Override
        public String getTreeName() {
            return treeName;
        }

        @Override
        public void setTreeCache(TreeCache cache) {
            this.cache = cache;
        }

        @Override
        public TreeCache getTreeCache() {
            return cache;
        }
    }

    @Override
    protected Collection<Property> startupConfigProperties() {
        final Collection<Property> properties = new ArrayList<Property>();
        properties.add(new Property("akserver.treespace.a",
                                    "drupal*:${datapath}/${schema}.v0,create,pageSize:${buffersize},"
                                    + "initialSize:10K,extensionSize:1K,maximumSize:10G"));
        properties.add(new Property("akserver.treespace.b",
                                    "liveops*:${datapath}/${schema}.v0,create,pageSize:${buffersize},"
                                    + "initialSize:10K,extensionSize:1K,maximumSize:10G"));
        return properties;
    }

    @Test
    public void testCreateVolume() throws Exception {
        final TreeService treeService = serviceManager().getTreeService();
        final TestLink link0 = new TestLink("not_drupal", "_schema_");
        final Exchange ex0 = treeService.getExchange(session(), link0);
        assertEquals("akiban_data", ex0.getVolume().getName());
        final TestLink link1 = new TestLink("drupal_large", "_schema_");
        final Exchange ex1 = treeService.getExchange(session(), link1);
        assertEquals("drupal_large", ex1.getVolume().getName());
        final TestLink link2 = new TestLink("drupal.org", "_schema_");
        final Exchange ex2 = treeService.getExchange(session(), link2);
        assertEquals("drupal.org", ex2.getVolume().getName());
        final Set<Tree> trees = new HashSet<Tree>();
        treeService.visitStorage(session(), new TreeVisitor() {
            @Override
            public void visit(Exchange exchange) throws Exception {
                trees.add(exchange.getTree());
            }
        }, "_schema_");
        assertEquals(3, trees.size());
        final int d0 = verifyTableId(treeService, 1, link0);
        final int d1 = verifyTableId(treeService, 100002, link1);
        final int d2 = verifyTableId(treeService, 200003, link2);
        assertEquals(0, d0);
        assertTrue(d1 > d0);
        assertTrue(d2 > d1);
    }

    private int verifyTableId(final TreeService treeService, final int aisId, TreeLink link)
            throws PersistitException {
        final int stored = treeService.aisToStore(link, aisId);
        final int recovered = treeService.storeToAis(link, stored);
        assertEquals(recovered, aisId);
        return aisId - stored;
    }
}