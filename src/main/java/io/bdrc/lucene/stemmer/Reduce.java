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
 *  The Reduce object is used to remove gaps in a Trie which stores a
 *  dictionary..
 *  
 *  This product includes software developed by the Egothor Project. http://www.egothor.org/
 *
 * @author    Leo Galambos
 */
public class Reduce {

    /**
     *  Constructor for the Reduce object.
     */
    public Reduce() { }


    /**
     *  Optimize (remove holes in the rows) the given Trie and return the
     *  restructured Trie.
     *
     * @param  orig  the Trie to optimize
     * @return       the restructured Trie
     */
    public Trie optimize(Trie orig) {
        final Vector<String> cmds = orig.cmds;
        Vector<Row> rows = new Vector<Row>();
        final Vector<Row> orows = orig.rows;
        final int remap[] = new int[orows.size()];

        Arrays.fill(remap, -1);
        rows = removeGaps(orig.root, orows, rows, remap);

        return new Trie(orig.forward, remap[orig.root], cmds, rows);
    }


    /**
     *  Description of the Method
     *
     * @param  ind    Description of the Parameter
     * @param  old    Description of the Parameter
     * @param  to     Description of the Parameter
     * @param  remap  Description of the Parameter
     * @return        Description of the Return Value
     */
    Vector<Row> removeGaps(int ind, Vector<Row> old, Vector<Row> to, int remap[]) {
        System.out.println("ind: "+ind);
        System.out.println("to.size: "+to.size());
        remap[ind] = to.size();
        
        final Row now = old.elementAt(ind);
        to.addElement(now);
        System.out.println("remove gaps in "+now.toString());
        final Iterator<Cell> i = now.cells.values().iterator();
        for (; i.hasNext(); ) {
            final Cell c = i.next();
            if (c.ref >= 0 && remap[c.ref] < 0) {
                removeGaps(c.ref, old, to, remap);
            }
        }
//        System.out.println(Arrays.toString(remap));
//        for (Row r : old)
//            System.out.println(r.toString());
        to.setElementAt(new Remap(now, remap), remap[ind]);
        return to;
    }


    /**
     *  This class is part of the Egothor Project
     *
     * @author    Leo Galambos
     */
    class Remap extends Row {
        /**
         *  Constructor for the Remap object
         *
         * @param  old    Description of the Parameter
         * @param  remap  Description of the Parameter
         */
        public Remap(Row old, int remap[]) {
            super();
            final Iterator<Character> i = old.cells.keySet().iterator();
            for (; i.hasNext(); ) {
                final Character ch = i.next();
                final Cell c = old.at(ch);
                final Cell nc;
                if (c.ref >= 0) {
                    nc = new Cell(c);
                    nc.ref = remap[nc.ref];
                } else {
                    nc = new Cell(c);
                }
                cells.put(ch, nc);
            }
        }
    }
}
