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
import java.io.*;

/**
 *  A Trie is used to store a dictionary of words and their stems.
 *  Actually, what is stored are words with their respective patch
 *  commands. A trie can be termed forward (keys read from left to right)
 *  or backward (keys read from right to left). This property will vary
 *  depending on the language for which a Trie is constructed.
 *  
 *  This product includes software developed by the Egothor Project. http://www.egothor.org/
 *
 * @author    Leo Galambos
 */
public class Trie {
    Vector<Row> rows = new Vector<Row>();
    Vector<String> cmds = new Vector<String>();
    int root;

    boolean forward = false;

    /**
     * 
     * get root Row id
     * 
     * @return root Row id
     */
    public int getRoot() {
    	return root;
    }

    /**
     * get the value associated with a command ID
     * 
     * @param cmd the command id
     * @return the associated value in the Trie
     */
    public String getCommandVal(int cmd) {
    	return (cmd == -1) ? null : cmds.elementAt(cmd);
    }
    
    /**
     *  Constructor for the Trie object.
     *
     * @param  is               the input stream
     * @exception  IOException  if an I/O error occurs
     */
    public Trie(DataInput is) throws IOException {
        forward = is.readBoolean();
        root = is.readInt();
        for (int i = is.readInt(); i > 0; i--) {
            cmds.addElement(is.readUTF());
        }
        for (int i = is.readInt(); i > 0; i--) {
            rows.addElement(new Row(is));
        }
    }


    /**
     *  Constructor for the Trie object.
     *
     * @param  forward  set to <tt>true</tt>
     */
    public Trie(boolean forward) {
        rows.addElement(new Row());
        root = 0;
        this.forward = forward;
    }


    /**
     *  Constructor for the Trie object.
     *
     * @param  forward  <tt>true</tt> if read left to right, <tt>false</tt>
     *      if read right to left
     * @param  root     index of the row that is the root node
     * @param  cmds     the patch commands to store
     * @param  rows     a Vector of Vectors. Each inner Vector is a node of
     *      this Trie
     */
    public Trie(boolean forward, int root, Vector<String> cmds, Vector<Row> rows) {
        this.rows = rows;
        this.cmds = cmds;
        this.root = root;
        this.forward = forward;
    }


    /**
     *  Gets the all attribute of the Trie object
     *
     * @param  key  Description of the Parameter
     * @return      The all value
     */
    public String[] getAll(String key) {
        int res[] = new int[key.length()];
        int resc = 0;
        Row now = getRow(root);
        int w;
        StrEnum e = new StrEnum(key, forward);
        boolean br = false;

        for (int i = 0; i < key.length() - 1; i++) {
            Character ch = new Character(e.next());
            w = now.getCmd(ch);
            if (w >= 0) {
                int n = w;
                for (int j = 0; j < resc; j++) {
                    if (n == res[j]) {
                        n = -1;
                        break;
                    }
                }
                if (n >= 0) {
                    res[resc++] = n;
                }
            }
            w = now.getRef(ch);
            if (w >= 0) {
                now = getRow(w);
            } else {
                br = true;
                break;
            }
        }
        if (br == false) {
            w = now.getCmd(new Character(e.next()));
            if (w >= 0) {
                int n = w;
                for (int j = 0; j < resc; j++) {
                    if (n == res[j]) {
                        n = -1;
                        break;
                    }
                }
                if (n >= 0) {
                    res[resc++] = n;
                }
            }
        }

        if (resc < 1) {
            return null;
        }
        String R[] = new String[resc];
        for (int j = 0; j < resc; j++) {
            R[j] = cmds.elementAt(res[j]);
        }
        return R;
    }


    /**
     *  Return the number of cells in this Trie object.
     *
     * @return    the number of cells
     */
    public int getCells() {
        int size = 0;
        Enumeration<Row> e = rows.elements();
        while (e.hasMoreElements()) {
            size += (e.nextElement()).getCells();
        }
        return size;
    }


    /**
     *  Gets the cellsPnt attribute of the Trie object
     *
     * @return    The cellsPnt value
     */
    public int getCellsPnt() {
        int size = 0;
        Enumeration<Row> e = rows.elements();
        while (e.hasMoreElements()) {
            size += (e.nextElement()).getCellsPnt();
        }
        return size;
    }


    /**
     *  Gets the cellsVal attribute of the Trie object
     *
     * @return    The cellsVal value
     */
    public int getCellsVal() {
        int size = 0;
        Enumeration<Row> e = rows.elements();
        while (e.hasMoreElements()) {
            size += (e.nextElement()).getCellsVal();
        }
        return size;
    }


    /**
     *  Return the element that is stored in a cell associated with the
     *  given key.
     *
     * @param  key  the key
     * @return      the associated element
     */
    public String getFully(String key) {
        Row now = getRow(root);
        int w;
        Cell c;
        int cmd = -1;
        StrEnum e = new StrEnum(key, forward);
        Character ch = null;

        for (int i = 0; i < key.length(); ) {
            ch = new Character(e.next());
            i++;

            c = now.at(ch);
            if (c == null) {
                return null;
            }

            cmd = c.cmd;

            for (int skip = c.skip; skip > 0; skip--) {
                if (i >= key.length()) {
                    return null;
                }
                i++;
            }

            w = now.getRef(ch);
            if (w >= 0) {
                now = getRow(w);
            } else if (i < key.length()) {
                return null;
            }
        }
        return (cmd == -1) ? null : cmds.elementAt(cmd);
    }


    /**
     *  Return the element that is stored as last on a path associated with
     *  the given key.
     *
     * @param  key  the key associated with the desired element
     * @return      the last on path element
     */
    public String getLastOnPath(String key) {
        Row now = getRow(root);
        int w;
        String last = null;
        StrEnum e = new StrEnum(key, forward);

        for (int i = 0; i < key.length() - 1; i++) {
            Character ch = new Character(e.next());
            w = now.getCmd(ch);
            if (w >= 0) {
                last = cmds.elementAt(w);
            }
            w = now.getRef(ch);
            if (w >= 0) {
                now = getRow(w);
            } else {
                return last;
            }
        }
        w = now.getCmd(new Character(e.next()));
        return (w >= 0) ? cmds.elementAt(w) : last;
    }


    /**
     *  Return the Row at the given index.
     *
     * @param  index  the index containing the desired Row
     * @return        the Row
     */
    private Row getRow(int index) {
        if (index < 0 || index >= rows.size()) {
            return null;
        }
        return rows.elementAt(index);
    }


    /**
     *  Write this Trie to the given output stream.
     *
     * @param  os               the output stream
     * @exception  IOException  if an I/O error occurs
     */
    public void store(DataOutput os) throws IOException {
        os.writeBoolean(forward);
        os.writeInt(root);
        Enumeration<String> e = cmds.elements();
        os.writeInt(cmds.size());
        while (e.hasMoreElements()) {
            os.writeUTF(e.nextElement());
        }
        Enumeration<Row> erow = rows.elements();
        os.writeInt(rows.size());
        while (erow.hasMoreElements()) {
            Row r = erow.nextElement();
            r.store(os);
        }
    }


    /**
     *  Add the given key associated with the given patch command. If
     *  either parameter is null this method will return without executing.
     *
     * @param  key  the key
     * @param  cmd  the patch command
     */
    public void add(String key, String cmd) {
        if (key == null || cmd == null) {
            return;
        }
        if (cmd.length() == 0) {
            return;
        }
        int id_cmd = cmds.indexOf(cmd);
        if (id_cmd == -1) {
            id_cmd = cmds.size();
            cmds.addElement(cmd);
        }

        int node = root;
        Row r = getRow(node);

        StrEnum e = new StrEnum(key, forward);

        for (int i = 0; i < e.length() - 1; i++) {
            Character ch = new Character(e.next());
            node = r.getRef(ch);
            if (node >= 0) {
                r = getRow(node);
            } else {
                node = rows.size();
                Row n;
                rows.addElement(n = new Row());
                r.setRef(ch, node);
                r = n;
            }
        }
        r.setCmd(new Character(e.next()), id_cmd);
    }


    /**
     *  Remove empty rows from the given Trie and return the newly reduced
     *  Trie.
     *
     * @param  by  the Trie to reduce
     * @return     the newly reduced Trie
     */
    public Trie reduce(Reduce by) {
        return by.optimize(this);
    }


    /**
     *  Description of the Method
     *
     * @param  prefix  Description of the Parameter
     */
    public void printInfo(String prefix) {
        System.out.println(prefix + "nds " + rows.size() + " cmds " +
                cmds.size() + " cells " + getCells() +
                " valcells " + getCellsVal() +
                " pntcells " + getCellsPnt());
    }

    /**
     *  Description of the Method
     */
    public String toString() {
        return "nds " + rows.size() + " cmds " +
                cmds.size() + " cells " + getCells() +
                " valcells " + getCellsVal() +
                " pntcells " + getCellsPnt();
    }

    /**
     *  This class is part of the Egothor Project
     *
     * @author    Leo Galambos
     */
    class StrEnum {
        String s;
        int from;
        int by;


        /**
         *  Constructor for the StrEnum object
         *
         * @param  s   Description of the Parameter
         * @param  up  Description of the Parameter
         */
        StrEnum(String s, boolean up) {
            this.s = s;
            if (up) {
                from = 0;
                by = 1;
            } else {
                from = s.length() - 1;
                by = -1;
            }
        }


        /**
         *  Description of the Method
         *
         * @return    Description of the Return Value
         */
        int length() {
            return s.length();
        }


        /**
         *  Description of the Method
         *
         * @return    Description of the Return Value
         */
        char next() {
            char ch = s.charAt(from);
            from += by;
            return ch;
        }
    }
}
