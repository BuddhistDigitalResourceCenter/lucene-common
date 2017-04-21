/*
                    Egothor Software License version 2.00
                    Copyright (C) 1997-2004 Leo Galambos.
                 Copyright (C) 2002-2004 "Egothor developers"
                      on behalf of the Egothor Project.
                             All rights reserved.

   This  software  is  copyrighted  by  the "Egothor developers". If this
   license applies to a single file or document, the "Egothor developers"
   are the people or entities mentioned as copyright holders in that file
   or  document.  If  this  license  applies  to the Egothor project as a
   whole,  the  copyright holders are the people or entities mentioned in
   the  file CREDITS. This file can be found in the same location as this
   license in the distribution.

   Redistribution  and  use  in  source and binary forms, with or without
   modification, are permitted provided that the following conditions are
   met:
    1. Redistributions  of  source  code  must retain the above copyright
       notice, the list of contributors, this list of conditions, and the
       following disclaimer.
    2. Redistributions  in binary form must reproduce the above copyright
       notice, the list of contributors, this list of conditions, and the
       disclaimer  that  follows  these  conditions  in the documentation
       and/or other materials provided with the distribution.
    3. The name "Egothor" must not be used to endorse or promote products
       derived  from  this software without prior written permission. For
       written permission, please contact leo.galambos@egothor.org
    4. Products  derived  from this software may not be called "Egothor",
       nor  may  "Egothor"  appear  in  their name, without prior written
       permission from leo.galambos@egothor.org.

   In addition, we request that you include in the end-user documentation
   provided  with  the  redistribution  and/or  in the software itself an
   acknowledgement equivalent to the following:
   "This product includes software developed by the Egothor Project.
    http://www.egothor.org/"

   THIS  SOFTWARE  IS  PROVIDED  ``AS  IS''  AND ANY EXPRESSED OR IMPLIED
   WARRANTIES,  INCLUDING,  BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
   MERCHANTABILITY  AND  FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
   IN  NO  EVENT  SHALL THE EGOTHOR PROJECT OR ITS CONTRIBUTORS BE LIABLE
   FOR   ANY   DIRECT,   INDIRECT,  INCIDENTAL,  SPECIAL,  EXEMPLARY,  OR
   CONSEQUENTIAL  DAMAGES  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
   SUBSTITUTE  GOODS  OR  SERVICES;  LOSS  OF  USE,  DATA, OR PROFITS; OR
   BUSINESS  INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
   WHETHER  IN  CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
   OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN
   IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

   This  software  consists  of  voluntary  contributions  made  by  many
   individuals  on  behalf  of  the  Egothor  Project  and was originally
   created by Leo Galambos (leo.galambos@egothor.org).

 */

package io.bdrc.lucene.stemmer;

import java.util.*;

/**
 *  The Optimizer class is a Trie that will be reduced (have empty rows
 *  removed). The reduction will be made by joining two rows where the
 *  first is a subset of the second.
 *  
 *  This product includes software developed by the Egothor Project. http://www.egothor.org/
 *
 * @author    Leo Galambos
 */
public class Optimizer extends Reduce {
    /**
     *  Constructor for the Optimizer object.
     */
    public Optimizer() { }


    /**
     *  Optimize (remove empty rows) from the given Trie and return the
     *  resulting Trie.
     *
     * @param  orig  the Trie to consolidate
     * @return       the newly consolidated Trie
     */
    public Trie optimize(Trie orig) {
        Vector<String> cmds = orig.cmds;
        Vector<Row> rows = new Vector<Row>();
        Vector<Row> orows = orig.rows;
        int remap[] = new int[orows.size()];

        for (int j = orows.size() - 1; j >= 0; j--) {
            Row now = new Remap((Row) orows.elementAt(j), remap);
            boolean merged = false;

            for (int i = 0; i < rows.size(); i++) {
                Row q = merge(now, (Row) rows.elementAt(i));
                if (q != null) {
                    rows.setElementAt(q, i);
                    merged = true;
                    remap[j] = i;
                    break;
                }
            }

            if (merged == false) {
                remap[j] = rows.size();
                rows.addElement(now);
            }
        }

        int root = remap[orig.root];
        Arrays.fill(remap, -1);
        rows = removeGaps(root, rows, new Vector<Row>(), remap);

        return new Trie(orig.forward, remap[root], cmds, rows);
    }


    /**
     *  Merge the given rows and return the resulting Row.
     *
     * @param  master    the master Row
     * @param  existing  the existing Row
     * @return           the resulting Row, or <tt>null</tt> if the
     *      operation cannot be realized
     */
    public Row merge(Row master, Row existing) {
        Iterator<Character> i = master.cells.keySet().iterator();
        Row n = new Row();
        for (; i.hasNext(); ) {
            Character ch = i.next();
            // XXX also must handle Cnt and Skip !!
            Cell a = (Cell) master.cells.get(ch);
            Cell b = (Cell) existing.cells.get(ch);

            Cell s = (b == null) ? new Cell(a) : merge(a, b);
            if (s == null) {
                return null;
            }
            n.cells.put(ch, s);
        }
        i = existing.cells.keySet().iterator();
        for (; i.hasNext(); ) {
            Character ch = (Character) i.next();
            if (master.at(ch) != null) {
                continue;
            }
            n.cells.put(ch, existing.at(ch));
        }
        return n;
    }


    /**
     *  Merge the given Cells and return the resulting Cell.
     *
     * @param  m  the master Cell
     * @param  e  the existing Cell
     * @return    the resulting Cell, or <tt>null</tt> if the operation
     *      cannot be realized
     */
    public Cell merge(Cell m, Cell e) {
        Cell n = new Cell();

        if (m.skip != e.skip) {
            return null;
        }

        if (m.cmd >= 0) {
            if (e.cmd >= 0) {
                if (m.cmd == e.cmd) {
                    n.cmd = m.cmd;
                } else {
                    return null;
                }
            } else {
                n.cmd = m.cmd;
            }
        } else {
            n.cmd = e.cmd;
        }
        if (m.ref >= 0) {
            if (e.ref >= 0) {
                if (m.ref == e.ref) {
                    if (m.skip == e.skip) {
                        n.ref = m.ref;
                    } else {
                        return null;
                    }
                } else {
                    return null;
                }
            } else {
                n.ref = m.ref;
            }
        } else {
            n.ref = e.ref;
        }
        n.cnt = m.cnt + e.cnt;
        n.skip = m.skip;
        return n;
    }
}
