/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package domain;

import java.util.HashSet;
import java.util.Iterator;

/**
 *
 * @author enrico
 */
public class ActionParameters extends HashSet{

    public Variable containsVariable(Variable o){
        Integer ret_val=-1;
        Iterator it = this.iterator();
        while (it.hasNext()){
            Variable v = (Variable)it.next();
            if (v.getName() == null ? o.getName() == null : v.getName().equals(o.getName())){
                return v;
            }
        }
        return null;
    }
}
