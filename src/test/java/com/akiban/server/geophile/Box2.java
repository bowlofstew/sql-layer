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

package com.akiban.server.geophile;

import com.akiban.server.geophile.Region;
import com.akiban.server.geophile.RegionComparison;
import com.akiban.server.geophile.SpatialObject;

public class Box2 implements SpatialObject
{
    // Object interface

    @Override
    public String toString()
    {
        return String.format("(%s:%s, %s:%s)", xLo, xHi, yLo, yHi);
    }

    // SpatialObject interface

    public long[] arbitraryPoint()
    {
        return new long[]{xLo, yLo};
    }

    public boolean containedBy(Region region)
    {
        long[] rLo = region.lo();
        long[] rHi = region.hi();
        return rLo[0] <= xLo && xHi <= rHi[0] && rLo[1] <= yLo && yHi <= rHi[1];
    }

    public RegionComparison compare(Region region)
    {
        long[] rLo = region.lo();
        long[] rHi = region.hi();
        long rxLo = rLo[0];
        long ryLo = rLo[1];
        long rxHi = rHi[0];
        long ryHi = rHi[1];
        if (xLo <= rxLo && rxHi <= xHi && yLo <= ryLo && ryHi <= yHi) {
            return RegionComparison.INSIDE;
        } else if (rxHi < xLo || rxLo > xHi || ryHi < yLo || ryLo > yHi) {
            return RegionComparison.OUTSIDE;
        } else {
            return RegionComparison.OVERLAP;
        }
    }

    // Box2 interface

    public Box2(long xLo, long xHi, long yLo, long yHi)
    {
        this.xLo = xLo;
        this.xHi = xHi;
        this.yLo = yLo;
        this.yHi = yHi;
    }

    // Object state

    private final long xLo;
    private final long xHi;
    private final long yLo;
    private final long yHi;
}
