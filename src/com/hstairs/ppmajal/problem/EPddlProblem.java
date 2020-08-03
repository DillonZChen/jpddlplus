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
package com.hstairs.ppmajal.problem;

import com.hstairs.ppmajal.propositionalFactory.MetricFFGrounder;
import com.google.common.collect.Sets;
import com.hstairs.ppmajal.conditions.*;
import com.hstairs.ppmajal.domain.PddlDomain;
import com.hstairs.ppmajal.domain.SchemaGlobalConstraint;
import com.hstairs.ppmajal.domain.Type;
import com.hstairs.ppmajal.expressions.NumEffect;
import com.hstairs.ppmajal.expressions.NumFluent;
import com.hstairs.ppmajal.expressions.PDDLNumber;
import com.hstairs.ppmajal.pddl.heuristics.advanced.Aibr;
import com.hstairs.ppmajal.pddl.heuristics.advanced.H1;
import com.hstairs.ppmajal.propositionalFactory.ExternalGrounder;
import com.hstairs.ppmajal.propositionalFactory.FDGrounder;
import com.hstairs.ppmajal.propositionalFactory.FDGrounderInstantiate;
import com.hstairs.ppmajal.propositionalFactory.Grounder;
import com.hstairs.ppmajal.search.SearchProblem;
import com.hstairs.ppmajal.transition.ConditionalEffects;
import com.hstairs.ppmajal.transition.Transition;
import com.hstairs.ppmajal.transition.TransitionGround;
import com.hstairs.ppmajal.transition.TransitionSchema;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.io.PrintStream;

import java.util.*;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.jgrapht.alg.util.Pair;

/**
 * @author enrico
 */
public class EPddlProblem extends PddlProblem implements SearchProblem {

    public HashSet<GlobalConstraint> globalConstraintSet;
    public AndCond globalConstraints;
    private Collection<TransitionGround> processesSet;
    private Collection<TransitionGround> eventsSet;
    private PDDLState pureInit;
    private HashMap<String, Terminal> terminalReference;
    private HashMap<String, NumFluent> numFluentReference;
    private boolean smallExpensive = false;
    private boolean debug;
    private boolean cacheComparison = false;
    static public HashSet<Predicate> booleanFluents;
    private int totNumberOfBoolVariables;
    private int totNumberOfNumVariables;
    final public PrintStream out;
    final private String groundingMethod;
    private long groundingTime;
    public EPddlProblem (String problemFile, PDDLObjects po, Set<Type> types, PddlDomain linked) {
        this(problemFile,po,types,linked,System.out,"internal");
    }



    public EPddlProblem(String problemFile, PDDLObjects constants, Set<Type> types, PddlDomain domain, PrintStream out, String groundingMethod) {
        super(problemFile, constants, types, domain);
        globalConstraintSet = new LinkedHashSet();
        eventsSet = new LinkedHashSet();
        globalConstraints = new AndCond();   
        this.out = out;
        this.groundingMethod = groundingMethod;
    }

    public long getGroundingTime() {
        return groundingTime;
    }



    @Override
    public Object clone ( ) throws CloneNotSupportedException {

        EPddlProblem cloned = new EPddlProblem(this.pddlFilRef, this.objects, this.types, linkedDomain);
        cloned.processesSet = new LinkedHashSet();
        for (TransitionGround gr : this.actions) {
            throw new UnsupportedOperationException();
        }
        for (TransitionGround pr : this.getProcessesSet()) {
            throw new UnsupportedOperationException();
        }
        for (GlobalConstraint constr : this.globalConstraintSet) {
            cloned.globalConstraintSet.add((GlobalConstraint) constr.clone());
        }
        return this;

    }

    public HashMap<String, NumFluent> getNumericFluentReference ( ) {
        if (this.numFluentReference == null) {
            numFluentReference = new HashMap<>();
        }
        return this.numFluentReference;
    }



    public void generateTransitions() {
        long start = System.currentTimeMillis();
        if (!"internal".equals(groundingMethod) && !"naive".equals(groundingMethod)) {
            System.out.println("Generate Transitions using " + groundingMethod);
            ExternalGrounder mff = null;
            switch (groundingMethod) {
                case "metricff":
                    mff = new MetricFFGrounder(this, this.linkedDomain.getPddlFilRef(), this.pddlFilRef);
                    break;
                case "fd":
                    mff = new FDGrounder(this, this.linkedDomain.getPddlFilRef(), this.pddlFilRef);
                    break;
                case "fdi":
                    mff = new FDGrounderInstantiate(this, this.linkedDomain.getPddlFilRef(), this.pddlFilRef);
                    break;
            }
            groundingTime = System.currentTimeMillis();
            Collection<TransitionGround> doGrounding = mff.doGrounding();
            groundingTime = System.currentTimeMillis()-groundingTime;
            for (var act : doGrounding) {
                switch (act.getSemantics()) {
                    case ACTION:
                        getActions().add(act);
                        break;
                    case EVENT:
                        getEventsSet().add(act);
                        break;
                    case PROCESS:
                        getProcessesSet().add(act);
                        break;
                }
            }
        } else {
            Grounder af = new Grounder(belief == null && !"naive".equals(groundingMethod));
//        Grounder af = new Grounder(false);

            ArrayList<TransitionSchema> transitions = new ArrayList<>();
            transitions.addAll(linkedDomain.getProcessesSchema());
            transitions.addAll(linkedDomain.getActionsSchema());
            transitions.addAll(linkedDomain.eventsSchema);
            groundingTime = System.currentTimeMillis();

            for (var act : transitions) {
                Collection<TransitionGround> propositionalize = af.Propositionalize(act, getObjects(), this, initBoolFluentsValues, linkedDomain);
                switch (act.getSemantics()) {
                    case ACTION:
                        getActions().addAll(propositionalize);
                        break;
                    case EVENT:
                        getEventsSet().addAll(propositionalize);
                        break;
                    case PROCESS:
                        getProcessesSet().addAll(propositionalize);
                        break;
                }
            }
            groundingTime = System.currentTimeMillis() - groundingTime;
        }

    }

    public void groundingSimplication(boolean aibrPreprocessing) throws Exception {

        this.groundingSimplication(aibrPreprocessing, false);

    }
    public void groundingSimplication(boolean aibrPreprocessing,boolean stopAfterGrounding) throws Exception {

        //simplification decoupled from the grounding
        this.groundingActionProcessesConstraints();
        System.out.println("Grounding Time: " + this.getGroundingTime());
        if (stopAfterGrounding)
            return;
        this.simplifyAndSetupInit(aibrPreprocessing);

        this.transformGoal();

    }

    public Set getActualFluents ( ) {
        if (actualFluents == null) {
            actualFluents = new LinkedHashSet();
            Sets.SetView<TransitionGround> transitions = Sets.union(Sets.union(new HashSet(this.actions), new HashSet(this.getEventsSet())),new HashSet(this.getProcessesSet()));

            for (TransitionGround gr : transitions) {
                gr.updateInvariantFluents(actualFluents);

            }
        }
//        System.out.println(actualFluents);
        return actualFluents;
    }




    public void generateConstraints ( ) throws Exception {
            Grounder af = new Grounder();
            for (SchemaGlobalConstraint constr : linkedDomain.getSchemaGlobalConstraints()) {
//                af.Propositionalize(act, objects);

                if (!constr.parameters.isEmpty()) {
                    globalConstraintSet.addAll(af.Propositionalize(constr, getObjects()));
                } else {
                    GlobalConstraint gr = constr.ground();
                    globalConstraintSet.add(gr);
                }
            }
    }
    

    public void groundingActionProcessesConstraints ( ) throws Exception {
        long start = System.currentTimeMillis();

        this.groundGoals();
        this.generateTransitions();
        this.generateConstraints();
        this.setGroundedRepresentation(true);
        this.getActualFluents();
        if (this.metric != null && this.metric.getMetExpr() != null) {
            this.metric.setMetExpr(this.metric.getMetExpr().normalize());
        } else {
            this.metric = null;
        }

        setPropositionalTime(this.getPropositionalTime() + (System.currentTimeMillis() - start));
        syncAllVariablesAndUpdateCollections(this);

    }



    public Iterable cleanEasyUnreachableTransitions (Iterable toWorkOut) {
        ArrayList arrayList = new ArrayList((Collection) toWorkOut);
        Collection<TransitionGround> res = new LinkedHashSet<>();
        ListIterator it = arrayList.listIterator();
        while (it.hasNext()) {
            TransitionGround act = (TransitionGround) it.next();
            boolean keep = true;
            for (final NumEffect effect : act.getAllNumericEffects()) {
                if (true) {
                    if (effect.weakEval(this, this.getActualFluents()) != null) {
                        effect.normalize();
                    } else {
                        keep = false;
                    }
                } else {
                    effect.normalize();

                }
            }
            if (isSimplifyActions() && keep) {
                try {
                    Set invariantFluents = this.getActualFluents();
                    Condition preconditions = act.getPreconditions();
                    final Condition condition = preconditions.weakEval(this, invariantFluents).normalize();
                    if (condition != null && !condition.isUnsatisfiable()){
                        ConditionalEffects conditionalNumericEffects = act.getConditionalNumericEffects();
                        ConditionalEffects conditionalPropositionalEffects = act.getConditionalPropositionalEffects();
                        res.add(new TransitionGround(act.getParameters(),act.getName(),
                                conditionalPropositionalEffects.weakEval(this,invariantFluents),
                                conditionalNumericEffects.weakEval(this,invariantFluents),
                                condition,
                                act.getSemantics()));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }


        }
        return new ArrayList(res);
    }

    protected void easyCleanUp(){
        this.easyCleanUp(false);
    }
    protected void easyCleanUp(boolean aibrPreprocessing) {
        //out.println("prova");
        this.saveInitInit();
        sweepStructuresForUnreachableStatements();

        debug = false;
        if (debug) {
            out.print("This is the universe of numeric fluent:");
            for (NumFluent nf : NumFluent.numFluentsBank.values()) {
                out.println("ID:" + nf.getId() + "->" + nf);
            }
            out.print("This is the universe of propositional fluent:");
            for (Predicate pred : Predicate.getPredicatesDB().values()) {
                out.println("ID:" + pred.getId() + "->" + pred);
            }
        }
        this.makeInit();
//        System.out.println(this.getActualFluents());
//        h1.computeEstimate(this.init);
//        final Collection<TransitionGround> transitions = h1.getTransitions(false);
        if (aibrPreprocessing){
                final Aibr heuristic = new Aibr(this, true);
                final float v = heuristic.computeEstimate(this.init);
                if (v == Float.MAX_VALUE){
                    out.println("Problem Detected as Unsolvable");
                    System.exit(-1);
                }
                final Collection<TransitionGround> transitions = heuristic.getAllTransitions();
                actions = new ArrayList<>();
                processesSet = new ArrayList<>();
                eventsSet = new ArrayList<>();
                for (final TransitionGround t : transitions){
                    switch (t.getSemantics()){
                        case ACTION:
                            actions.add(t);
                            break;
                        case PROCESS:
                            processesSet.add(t);
                            break;
                        case EVENT:
                            eventsSet.add(t);
                            break;
                    }
                }
                sweepStructuresForUnreachableStatements();
        }

//        this.makePddlState(); //remake init so as to account for only reachable actions
    }

    protected void sweepStructuresForUnreachableStatements ( ) {
        this.actualFluents = null;
        //the following just remove actions/processes/events over false and static predicates
        actions = (Collection<TransitionGround>) cleanEasyUnreachableTransitions(actions);
//        this.staticFluents = null;
        processesSet = (Collection<TransitionGround>) cleanEasyUnreachableTransitions(processesSet);
//        this.staticFluents = null;
        eventsSet = (Collection<TransitionGround>) cleanEasyUnreachableTransitions(eventsSet);
//        this.staticFluents = null;
        cleanIrrelevantConstraints(globalConstraintSet);
        this.setGroundedRepresentation(true);

        goals = (ComplexCondition) goals.weakEval(this, this.getActualFluents());
        goals = (ComplexCondition) goals.normalize();
        if (goals.isUnsatisfiable()){
            throw new RuntimeException("Goal is not reachable");
        }
        globalConstraints = (AndCond) globalConstraints.weakEval(this, this.getActualFluents());
        globalConstraints = (AndCond) globalConstraints.normalize();
        if (globalConstraints.isUnsatisfiable()){
            throw new RuntimeException("Goal is not reachable");
        }

        if (this.metric != null && this.metric.getMetExpr() != null) {
            this.metric.setMetExpr(this.metric.getMetExpr().weakEval(this, this.getActualFluents()));
            if (this.metric.getMetExpr() == null) {
                this.metric = null;
            } else {
                this.metric.setMetExpr(this.metric.getMetExpr().normalize());
            }
        } else {
            this.metric = null;
        }

    }

    public void simplifyAndSetupInit() throws Exception {
        simplifyAndSetupInit(true);
    }

    public void simplifyAndSetupInit(boolean aibrPreprocessing) throws Exception {

        long start = System.currentTimeMillis();
        out.println("(Pre Simplification) - |A|+|P|+|E|: " + (getActions().size() + getProcessesSet().size() + getEventsSet().size()));
        easyCleanUp(aibrPreprocessing);
        out.println("(After Easy Simplification) - |A|+|P|+|E|: " + (getActions().size() + getProcessesSet().size() + getEventsSet().size()));
        // normalize global constraints, once and forall
        globalConstraints = (AndCond) globalConstraints.normalize();
        makeInit();
        out.println("|F|:" + totNumberOfBoolVariables);
        out.println("|X|:" + totNumberOfNumVariables);

    }

//    private void idifyConditionsAndTransitions (Collection<GroundAction> reachableActions, ComplexCondition goals, AndCond globalConstraints) {
//        HashMap<Integer, GroundAction> actionIds = new HashMap<>();
//        HashMap<Integer, Condition> conditionsIds = new HashMap<>();
//        int actionID = 0;
//        int conditionID = 0;
//        for (GroundAction gr: reachableActions){
//            if (!actionIds.values().contains(gr)){
//                actionIds.put(actionID,gr);
//                Set<Condition> terminalConditions = gr.getPreconditions().getTerminalConditions();
//                Set<Condition> addListTerminalConditions = gr.getAddList().getTerminalConditions();
//                Set<Condition> delListTerminalConditions = gr.getDelList().getTerminalConditions();
//            }
//            //preconditions
//        }
//    }

    private void cleanIrrelevantConstraints (HashSet<GlobalConstraint> globalConstraintSet) {
        Iterator<GlobalConstraint> it = globalConstraintSet.iterator();
        globalConstraints = new AndCond();

        while (it.hasNext()) {
            GlobalConstraint constr = it.next();
            boolean keep = constr.simplifyModelWithControllableVariablesSem(linkedDomain, this);

            if (!keep) {
                it.remove();
            } else {
                globalConstraints.addConditions(constr.condition);
            }

        }
    }




    public void setDeltaTimeVariable (String delta_t) {
        this.initNumFluentsValues.put(NumFluent.createNumFluent("#t", new ArrayList()), new PDDLNumber(Double.parseDouble(delta_t)));
    }


    private void removeStaticPart ( ) {
        //invariant fluents
        LinkedHashSet<Predicate> predicateToRemove = new LinkedHashSet();
        for (Predicate p : this.initBoolFluentsValues.keySet()) {
            if (!this.getActualFluents().contains(p)) {
                predicateToRemove.add(p);
            }
        }
        LinkedHashSet<NumFluent> numFluentsToRemove = new LinkedHashSet();
        for (NumFluent p : this.initNumFluentsValues.keySet()) {
            if (!this.getActualFluents().contains(p)) {
                numFluentsToRemove.add(p);
            }
        }

        this.initBoolFluentsValues.keySet().removeAll(predicateToRemove);
        this.initNumFluentsValues.keySet().removeAll(numFluentsToRemove);

    }

    public Sets.SetView<TransitionGround> getTransitions() {
        return Sets.union(Sets.union(new HashSet(actions), new HashSet<>(getEventsSet())), new HashSet(getProcessesSet()));

    }
    
    private void fixNecessaryFluents ( ) {

        Set<NumFluent> involved_fluents = new LinkedHashSet();

       
        for (Transition a : getTransitions()) {
            involved_fluents.addAll(a.getPreconditions().getInvolvedFluents());
            involved_fluents.addAll(a.getNumFluentsNecessaryForExecution());
        }

//        for (Transition )
        for (SchemaGlobalConstraint a : this.linkedDomain.getSchemaGlobalConstraints()) {
            involved_fluents.addAll(a.condition.getInvolvedFluents());
        }
        involved_fluents.addAll(goals.getInvolvedFluents());

        
        if (NumFluent.numFluentsBank != null){
            Iterator<NumFluent> it = NumFluent.numFluentsBank.values().iterator();
            while (it.hasNext()) {
                NumFluent nf2 = it.next();
                boolean keep_it = false;
                for (NumFluent nf : involved_fluents) {
                    if (nf.getName().equals(nf2.getName())) {
                        keep_it = true;
                        break;
                    }
                }
                if (!keep_it) {
                    nf2.needsTrackingInState(false);
//                    it.remove();
                }
                else{
                    nf2.needsTrackingInState(true);
                }
            }
        }
    }

    
    public Set getAllFluents(){
        Set res = new HashSet();
        if (NumFluent.numFluentsBank != null){
            for (NumFluent nf : NumFluent.numFluentsBank.values()) {
                if (this.getActualFluents().contains(nf) && nf.has_to_be_tracked()) {
                    res.add(nf);
                }
            }
        }
        booleanFluents = new HashSet();
        if (Predicate.getPredicatesDB() != null) {
            for (Predicate p : Predicate.getPredicatesDB().values()) {
                if (this.getActualFluents().contains(p)) {
                    res.add(p);
                }

            }
        }
        return res;
    }
    
    private PDDLState makePddlState ( ) {
        //ensure compactness
        removeStaticPart();
        fixNecessaryFluents();
        HashMap<Integer,Double> numFluents = new HashMap();
        totNumberOfNumVariables = 0;
        totNumberOfBoolVariables = 0;
        if (NumFluent.numFluentsBank != null){
            for (NumFluent nf : NumFluent.numFluentsBank.values()) {
                if (this.getActualFluents().contains(nf) && nf.has_to_be_tracked()) {
                    PDDLNumber number = this.initNumFluentsValues.get(nf);
                    if (number == null) {
                        numFluents.put(nf.getId(), Double.NaN);
                    } else {
                        numFluents.put(nf.getId(), number.getNumber().doubleValue());
                    }
                    totNumberOfNumVariables++;
                }
            }
        }
        booleanFluents = new HashSet();
        BitSet boolFluents = new BitSet();
        if (Predicate.getPredicatesDB() != null) {
            for (Predicate p : Predicate.getPredicatesDB().values()) {
                if (this.getActualFluents().contains(p)) {
                    Boolean r = this.initBoolFluentsValues.get(p);
                    if (r == null || !r) {
                        //boolFluents.set(p.getId(), false);
                    } else {
                        boolFluents.set(p.getId(), true);
                    }
                    booleanFluents.add(p);
                    totNumberOfBoolVariables++;
                }

            }
        }
        PDDLState pddlState = null;
        if (cacheComparison){
            pddlState = new PDDLStateWithCache(numFluents,boolFluents);
        }else{
            if (smallExpensive){
                pddlState = new PDDLStateWithInt2Double(numFluents, boolFluents);
            }else{
                pddlState = new PDDLState(numFluents,boolFluents);
            }
        }

//        out.println(Printer.stringBuilderPddlPrintWithDummyTrue(this, pddlState));
        return pddlState;
        
    }

    protected void makeInit ( ) {
        this.init = makePddlState();
        addTimeFluentToInit();
    }


    public Boolean goalSatisfied (State s) {
        return s.satisfy(this.getGoals());
    }

    private void groundGoals ( ) {
        this.goals = (ComplexCondition) this.goals.ground(new HashMap(), objects);
    }


    protected HashMap<String, Terminal> getTerminalReference ( ) {
        if (terminalReference == null) {
            terminalReference = new HashMap<>();
        }
        return terminalReference;
    }


    public void syncAllVariablesAndUpdateCollections (EPddlProblem inputProblem) {

        if (inputProblem == null) {
            inputProblem = this;
        }
        HashMap<Predicate, Boolean> tempInitBool = new HashMap();
        for (Predicate p : this.initBoolFluentsValues.keySet()) {
            Boolean value = this.initBoolFluentsValues.get(p);
            Predicate newP = (Predicate) p.unifyVariablesReferences(inputProblem);
            tempInitBool.put(newP, value);

        }
        initBoolFluentsValues = tempInitBool;
        HashMap<NumFluent, PDDLNumber> tempInitFluent = new HashMap();
        for (NumFluent nf : this.initNumFluentsValues.keySet()) {
            PDDLNumber pddlNumber = initNumFluentsValues.get(nf);
            NumFluent numFluent = (NumFluent) nf.unifyVariablesReferences(inputProblem);
            tempInitFluent.put(numFluent, pddlNumber);
            this.getNumericFluentReference().put(nf.toString(), nf);
        }
        this.initNumFluentsValues = tempInitFluent;
        
        goals = (ComplexCondition) goals.unifyVariablesReferences(inputProblem);

        Iterator<GlobalConstraint> it = this.globalConstraintSet.iterator();
        while (it.hasNext()) {
            GlobalConstraint gc = it.next();
            gc.condition = gc.condition.unifyVariablesReferences(inputProblem);
        }

        globalConstraints = (AndCond) globalConstraints.unifyVariablesReferences(inputProblem);
        if (metric != null) {
            metric = metric.unifyVariablesReferences(inputProblem);
        }
        if (belief != null) {
            belief = belief.unifyVariablesReferences(inputProblem);
        }
        if (this.unknonw_predicates != null){
            for (Predicate p: this.unknonw_predicates){
                p = (Predicate) p.unifyVariablesReferences(inputProblem);
            }
        }
    }


    public void addTimeFluentToInit ( ) {
        ((PDDLState) this.init).time = 0d;
    }

    public NumFluent getNumfluentReference (String stringRepresentation) {
        return getNumericFluentReference().get(stringRepresentation);
    }

    @Override
    public ObjectIterator<Pair<State, Object>> getSuccessors (State s, Collection<Object> acts) {
        return new stateContainer(s, (Collection) acts);
    }

    public boolean milestoneReached (Float d, Float current_value, State temp) {
        return d < current_value && this.isSafeState(temp);
    }

    private ArrayList<TransitionGround> eventsApplication (State s, float delta1, Collection<TransitionGround> events) throws CloneNotSupportedException {
        ArrayList<TransitionGround> ret = new ArrayList<>();
        while (true) {
            boolean at_least_one = false;
            for (TransitionGround ev : events) {
                if (ev.isApplicable(s)) {
                    at_least_one = true;
                    s.apply(ev,s.clone());
                    ret.add(ev);
                }
            }
            if (!at_least_one) {
                return ret;
            }
        }

    }

    public Collection<TransitionGround> getEventsSet ( ) {
        if (eventsSet == null){
            eventsSet = new LinkedHashSet<>();
        }
        return eventsSet;
    }

    public Collection<TransitionGround> getProcessesSet ( ) {
        if (processesSet == null){
            processesSet = new LinkedHashSet<>();
        }
        return processesSet;
    }

    public State saveInitInit ( ) {
        if (this.pureInit == null) {
            this.pureInit = new MAPPDDLState(this.initNumFluentsValues, initBoolFluentsValues);
        }
        return pureInit;
    }


    public void putNumFluentReference (NumFluent t) {
        getNumericFluentReference().put(t.toString(), t);
    }

    @Override
    public boolean satisfyGlobalConstraints(State temp) {
        return temp.satisfy(globalConstraints);
    }

    private void groundViaMetricFF() {

        MetricFFGrounder mff = new MetricFFGrounder(this,this.linkedDomain.getPddlFilRef(),this.pddlFilRef);
        mff.doGrounding();

    }

    protected class stateContainer implements ObjectIterator<Pair<State, Object>> {
        protected final State source;
        protected final Iterator<Object> it;
        final private Iterable<Object> actionsSet;
        protected Object current;
        protected State newState;

        public stateContainer (State source, Iterable<Object> actionsSet) {
            this.source = source;
            this.actionsSet = actionsSet;
            it = actionsSet.iterator();
        }

        @Override
        public boolean hasNext ( ) {
            while (it.hasNext()) {
                current = it.next();
                if (current instanceof TransitionGround) {
                    if (((TransitionGround) current).isApplicable(source)) {
                        newState = source.clone();
                        newState.apply(((TransitionGround) current), source);
                        if (newState.satisfy(globalConstraints)) {
                            return true;
                        }
                    }
                }else if (current instanceof Pair){
                    
                    final Pair<TransitionGround,Integer> tempVar= (Pair<TransitionGround,Integer>)this.current;
                    final int b = applyActionMTimes(tempVar.getFirst(), tempVar.getSecond());
                    if (b > 1) {
                        current = new ImmutablePair(((Pair<TransitionGround, Integer>) this.current).getFirst(),b);
                        return true;
                    }
                }
            }
            return false;
        }

        public int applyActionMTimes(final TransitionGround act, int counter){
            final State prev = source.clone();
            int i=0;
            while (i<counter){
                prev.apply((act), source);
                if (!act.isApplicable(newState) || !newState.satisfy(globalConstraints)){
                    return i;
                }
                newState = prev;
                i++;
            }
            return i;
        }

        @Override
        public Pair<State, Object> next ( ) {
            return new Pair(newState, current);
        }
    }


}
