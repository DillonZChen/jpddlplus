/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package heuristics.advanced;

import conditions.Comparison;
import conditions.Conditions;
import conditions.Predicate;
import ilog.concert.IloException;
import ilog.concert.IloLinearNumExpr;
import ilog.concert.IloNumVar;
import ilog.concert.IloNumVarType;
import ilog.cplex.IloCplex;
import static java.lang.Float.MAX_VALUE;
import static java.lang.System.out;
import java.util.ArrayList;
import java.util.Collection;
import static java.util.Collections.nCopies;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import problem.GroundAction;
import problem.State;

/**
 *
 * @author enrico
 */
public class landmarks_factory_refactored extends Uniform_cost_search_H1 {

    public ArrayList<Integer> dplus;//this is the minimum number of actions needed to achieve a given condition

    private ArrayList<Set<GroundAction>> condition_to_action;

    public ArrayList<Set<repetition_landmark>> reachable_poss_achievers;
    public boolean compute_lp;
    private ArrayList<Float> dist_float;
    public boolean red_constraints = false;
    public boolean smart_intersection = false;

    public landmarks_factory_refactored(Conditions goal, Set<GroundAction> A) {
        super(goal, A);

    }

    @Override
    public Float setup(State s) {
        if (red_constraints) {
            try {
                this.add_redundant_constraints();
            } catch (Exception ex) {
                Logger.getLogger(Uniform_cost_search_H1_RC.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        build_integer_representation();
        identify_complex_conditions(all_conditions, A);
        this.generate_link_precondition_action();
        try {
            generate_achievers();
        } catch (Exception ex) {
            Logger.getLogger(Uniform_cost_search_H1.class.getName()).log(Level.SEVERE, null, ex);
        }
        reacheability_setting = true;
        this.dbg_print("Reachability Analysis Started");
        Float ret = compute_estimate(s);
        this.dbg_print("Reachability Analysis Terminated");
        reacheability_setting = false;
        sat_test_within_cost = false; //don't need to recheck precondition sat for each state. It is done in the beginning for every possible condition
        out.println("Hard Conditions: " + this.complex_conditions);
        out.println("Simple Conditions: " + (this.all_conditions.size() - this.complex_conditions));

        //redo construction of integer to avoid spurious actions
        build_integer_representation();
        identify_complex_conditions(all_conditions, A);
        this.generate_link_precondition_action();
        try {
            generate_achievers();
        } catch (Exception ex) {
            Logger.getLogger(Uniform_cost_search_H1.class.getName()).log(Level.SEVERE, null, ex);
        }
        return ret;
    }

    @Override
    public Float compute_estimate(State s_0) {
        LinkedList<GroundAction> a_plus = new LinkedList();//actions executable. Progressively updated
        ArrayList<Set<Conditions>> lm = new ArrayList<>(nCopies(all_conditions.size() + 1, null));//mapping between condition and landmarks
        ArrayList<Boolean> never_active = new ArrayList<>(nCopies(A.size() + 1, true));//mapping between action and boolean. True if action has not been activated yet
        HashMap<Integer, IloNumVar> action_to_variable = new HashMap();//mapping between action representation and integer variable in cplex
        reachable_poss_achievers = new ArrayList<>(nCopies(all_conditions.size() + 1, null));
        for (GroundAction gr : this.A) {//see which actions are executable at the current state
            if (gr.isApplicable(s_0)) {
                a_plus.add(gr);//add such an action
                never_active.set(gr.counter, false);
                if (this.reacheability_setting) {
                    this.reachable.add(gr);
                }
            }
        }
        dist_float = new ArrayList<>(nCopies(all_conditions.size() + 1, MAX_VALUE));//keep track of conditions that have been reachead yet
        for (Conditions c : all_conditions) {//update with a value of 0 to say that condition is sat in init state
            if (c.isSatisfied(s_0)) {
                dist_float.set(c.getCounter(), 0f);
            }
            lm.set(c.getCounter(), null);//this condition has no landmark yet. This structure is updated on the way
            reachable_poss_achievers.set(c.getCounter(), new LinkedHashSet());//this is a mapping between condition and its possible (reachable) achievers
        }

        while (!a_plus.isEmpty()) {//keep going till no action is in the list. Look that here actions can be re-added
            GroundAction gr = a_plus.poll();
            update_actions_conditions(s_0, gr, a_plus, never_active, lm);//this procedure updates
            //all the conditions that can be reached by using action gr. 
            //This also changes the set a_plus whenever some new action becomes active becasue of gr
        }

        if (this.reacheability_setting) {
            A = reachable;
        }
        

        Set<Conditions> goal_landmark = new LinkedHashSet();
        for (Conditions c : (Collection<Conditions>) this.G.sons) {
            Float distance = dist_float.get(c.getCounter());
            if (distance == Float.MAX_VALUE) {
                return Float.MAX_VALUE;
            }
            if (distance != 0f) {
                goal_landmark.addAll(lm.get(c.getCounter()));
            }
        }
        if (this.reacheability_setting) {
            System.out.println("Landmarks:" + goal_landmark.size());
            this.dbg_print("Landmarks:"+goal_landmark+"\n");

        }


        float estimate = 0;
        if (compute_lp) {
            try {
                IloCplex lp = new IloCplex();
                lp.setOut(null);

                IloLinearNumExpr objective = lp.linearNumExpr();
                for (Conditions c : goal_landmark) {
                    if (!c.isSatisfied(s_0)) {
                        IloLinearNumExpr expr = lp.linearNumExpr();

                        for (repetition_landmark dlm : this.reachable_poss_achievers.get(c.getCounter())) {
                            IloNumVar action;
                            dlm.gr.setAction_cost(s_0);
                            Float action_cost = dlm.gr.getAction_cost();
                            if (action_cost.isNaN()) {
                                continue;
                            }
                            if (action_to_variable.get(dlm.gr.counter) != null) {
                                action = action_to_variable.get(dlm.gr.counter);
                            } else {
                                action = (IloNumVar) lp.numVar(0.0, Float.MAX_VALUE, IloNumVarType.Float);
                                action_to_variable.put(dlm.gr.counter, action);
                                objective.addTerm(action, action_cost);
                            }
                            expr.addTerm(action, 1.0 / dlm.repetition);
                        }
                        lp.addGe(expr, 1);
                    }
                }
                lp.addMinimize(objective);

                if (lp.solve()) {
                    if (lp.getStatus() == IloCplex.Status.Optimal) {
                        estimate = (float) lp.getObjValue();
                    } else {
                        estimate = Float.MAX_VALUE;
                    }
                } else {
                    estimate = Float.MAX_VALUE;

                }
                lp.end();

            } catch (IloException ex) {
                Logger.getLogger(landmarks_factory_refactored.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else {
            for (Conditions c : goal_landmark) {//these are the landmarks for the planning task
//                System.out.println("Debug: Poss_achiever(" + c + ":)" + this.possible_achievers.get(c.getCounter()));

                estimate += dplus.get(c.getCounter());
            }
        }

        return estimate;
    }

    private boolean update_lm(Conditions p, GroundAction gr, ArrayList<Set<Conditions>> lm) {

        Set<Conditions> previous = lm.get(p.getCounter());

        if (previous == null) {
            previous = new LinkedHashSet();
            for (Conditions c : (Set<Conditions>) gr.getPreconditions().sons) {
                if (this.dist_float.get(c.getCounter()) != 0f) {
                    previous.addAll(lm.get(c.getCounter()));
                }
            }
            previous.add(p);//adding itself
            lm.set(p.getCounter(), previous);
            return true;
        } else {
            int previous_size = previous.size();
            Set<Conditions> temp = new LinkedHashSet();
            for (Conditions c : (Set<Conditions>) gr.getPreconditions().sons) {
                if (this.dist_float.get(c.getCounter()) != 0f) {
                    temp.addAll(lm.get(c.getCounter()));
                }
            }
            if (smart_intersection)
                metric_sensitive_intersection(previous,temp);
            else
                previous.retainAll(temp);
            lm.set(p.getCounter(), previous);
            previous.add(p);//adding itself again (the intersection may have removed this...
            if (previous_size != previous.size()) {
                return true;
            }
        }
        return false;

    }

    private void update_actions_conditions(State s_0, GroundAction gr, LinkedList<GroundAction> a_plus, ArrayList<Boolean> never_active, ArrayList<Set<Conditions>> lm) {
        for (Conditions comp : this.achieve.get(gr.counter)) {//This is the set of all predicates reachable because of gr
            Float rep_needed = 1f;
            if (dist_float.get(comp.getCounter()) != 0f) {//if this isn't in the init state yet
                dist_float.set(comp.getCounter(), rep_needed);//update distance. Meant only to keep track of whether condition reachead or not, and "how"
                update_action_condition(gr, comp, lm, rep_needed, never_active, a_plus);
                //for this specific condition check implications of having it reached.
            }
        }
        for (Conditions comp : this.possible_achievers.get(gr.counter)) {
            Float rep_needed = gr.getNumberOfExecutionInt(s_0, (Comparison) comp);
            if (rep_needed != Float.MAX_VALUE) {
                dist_float.set(comp.getCounter(), rep_needed);
                update_action_condition(gr, comp, lm, rep_needed, never_active, a_plus);
            }
        }
    }

    private void update_action_condition(GroundAction gr, Conditions comp, ArrayList<Set<Conditions>> lm, Float rep_needed, ArrayList<Boolean> never_active, LinkedList<GroundAction> a_plus) {
        boolean changed = update_lm(comp, gr, lm);//update set of landmarks for this condition.
        //this procedure shrink landmarks for condition comp using action gr
//        System.out.println(changed);
        Set<GroundAction> set = condition_to_action.get(comp.getCounter());
        //this mapping contains action that need to be triggered becasue of condition comp
        for (GroundAction gr2 : set) {
            if (gr2.counter == gr.counter) {//avoids self-loop. Thanks god I have integer mapping here.
                continue;
            }

//            System.out.println(never_active);
//            if (!A.contains(gr2))
//                continue;
            if (never_active.get(gr2.counter)) {//if this action has never been used before..
                if (check_conditions(gr2)) {//are conditions all reached?
                    a_plus.push(gr2);//push in the set of actions to consider. 
                    //Need to understand whether is worth to do check on the list to see if action already is there.
                    never_active.set(gr2.counter, false);//now is not never active anymore (just pushed in the a_plus)_
                    if (this.reacheability_setting) {
                        this.reachable.add(gr2);
                    }
                }
            } else if (changed) {//if the lm of the condition has changed,
                //we need to reconsider all the possible paths using this condition. Meaning all the possible actions
//                if (!a_plus.contains(gr2)) {
                a_plus.push(gr2);//see above for the eventual checking
//                }
            }
        }
        //update set of possible achiever for the condition with new action.
        Set<repetition_landmark> set2 = reachable_poss_achievers.get(comp.getCounter());
        repetition_landmark dlm = new repetition_landmark(gr, rep_needed);
        set2.add(dlm);
        reachable_poss_achievers.set(comp.getCounter(), set2);
    }

    private boolean check_conditions(GroundAction gr2) {

        for (Conditions c : (Collection<Conditions>) gr2.getPreconditions().sons) {
            if (dist_float.get(c.getCounter()) == Float.MAX_VALUE) {
                return false;
            }
        }
        return true;
    }

    private void generate_link_precondition_action() {
        condition_to_action = new ArrayList<>(nCopies(all_conditions.size() + 1, null));
        for (Conditions c : all_conditions) {
            LinkedHashSet<GroundAction> set = new LinkedHashSet();
            for (GroundAction gr : A) {
                if (gr.getPreconditions().sons.contains(c)) {
                    set.add(gr);
                }
            }
            condition_to_action.set(c.getCounter(), set);

        }
    }
    //TO-DO do the sensitive intersection to the metric
    private void metric_sensitive_intersection(Set<Conditions> previous, Set<Conditions> temp) {
        Set<Conditions> newset = new LinkedHashSet();
        for (Conditions c: temp){
            if (c instanceof Predicate){
                
            }else if (c instanceof Comparison){
                
            }
        }
    }

    public class repetition_landmark extends Object {

        public GroundAction gr;
        public float repetition;

        public repetition_landmark(GroundAction gr_input, float repetition_input) {
            super();
            this.gr = gr_input;
            this.repetition = repetition_input;
        }

        @Override
        public String toString() {
            return "(" + gr.toEcoString() + " " + repetition + ")";
        }

        @Override
        public boolean equals(Object b) {
            if (b instanceof repetition_landmark) {
                repetition_landmark b_rep = (repetition_landmark) b;
                return b_rep.gr.counter == gr.counter;
            } else {
                return false;
            }
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 89 * hash + Objects.hashCode(this.gr.counter);
            return hash;
        }
    }
}
