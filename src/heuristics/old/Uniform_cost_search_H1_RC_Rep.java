/* 
 * Copyright (C) 2010-2017 Enrico Scala. Contact: enricos83@gmail.com.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package heuristics.old;

import conditions.Comparison;
import conditions.Condition;
import conditions.Predicate;
import expressions.BinaryOp;
import expressions.ExtendedNormExpression;
import expressions.PDDLNumber;
import extraUtils.Pair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import problem.GroundAction;
import problem.State;

/**
 *
 * @author enrico
 */
public class Uniform_cost_search_H1_RC_Rep extends Uniform_cost_search_H1_Rep {

    protected HashMap<Condition, Boolean> redundant_constraints;

    public Uniform_cost_search_H1_RC_Rep(Condition G, Set<GroundAction> A) {
        super(G, A);
        try {
            this.add_redundant_constraints();
        } catch (Exception ex) {
            Logger.getLogger(Uniform_cost_search_H1_RC.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    protected void add_redundant_constraints() throws Exception {
        redundant_constraints = new HashMap();

        for (GroundAction a : A) {
            if (a.getPreconditions() != null) {
                compute_redundant_constraint((Set<Condition>) a.getPreconditions().sons);
            }
            //System.out.println(a.toPDDL());
        }

        compute_redundant_constraint((Set<Condition>) G.sons);
    }

    protected void compute_redundant_constraint(Set<Condition> set) throws Exception {
        LinkedHashSet temp = new LinkedHashSet();
        ArrayList<Condition> set_as_array = new ArrayList(set);
        int counter = 0;
        for (int i = 0; i < set_as_array.size(); i++) {
            for (int j = i + 1; j < set_as_array.size(); j++) {
                Condition c_1 = set_as_array.get(i);
                Condition c_2 = set_as_array.get(j);
                if ((c_1 instanceof Comparison) && (c_2 instanceof Comparison)) {
                    counter++;
                    Comparison a1 = (Comparison) c_1;
                    Comparison a2 = (Comparison) c_2;
                    ExtendedNormExpression lhs_a1 = (ExtendedNormExpression) a1.getLeft();
                    ExtendedNormExpression lhs_a2 = (ExtendedNormExpression) a2.getLeft();
                    ExtendedNormExpression expr = lhs_a1.sum(lhs_a2);
                    String new_comparator = ">=";
                    if (a1.getComparator().equals(">") && a2.getComparator().equals(">")) {
                        new_comparator = ">";
                    }
                    Comparison newC = new Comparison(new_comparator);
                    newC.setLeft(expr);
                    newC.setRight(new ExtendedNormExpression(new Float(0.0)));
                    newC.normalize();

                    ExtendedNormExpression tempLeft = (ExtendedNormExpression) newC.getLeft();

                    if (tempLeft.summations.size() < 2) {
                        continue;
                    }
                    redundant_constraints.put(newC, true);
                    temp.add(newC);
                }
            }
        }
//        System.out.println("New conditions now:"+counter);
//        System.out.println("Set before:"+set.size());
        set.addAll(temp);
//        System.out.println("Set after:"+set.size());
    }

}
