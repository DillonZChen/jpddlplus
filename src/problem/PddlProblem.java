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
package problem;

import antlr.RecognitionException;
import conditions.AndCond;
import conditions.ComplexCondition;

import conditions.NumFluentValue;
import conditions.Condition;
import conditions.FactoryConditions;
import conditions.OneOf;
import conditions.OrCond;
import conditions.Predicate;
import conditions.PDDLObject;
import domain.ParametersAsTerms;
import domain.ActionSchema;
import domain.GenericActionType;
import domain.PddlDomain;
import domain.SchemaParameters;

import domain.Type;

import expressions.BinaryOp;
import expressions.Expression;
import expressions.NumFluent;
import expressions.MinusUnary;
import expressions.MultiOp;
import expressions.PDDLNumber;

import java.io.BufferedWriter;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;

import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;

import parser.PddlLexer;
import parser.PddlParser;
import propositionalFactory.Grounder;

/**
 *
 * @author enrico
 */
public class PddlProblem {

    public PDDLObjects objects;
    public State init;
    public ComplexCondition goals;
    protected String name;
    protected Integer indexObject;
    protected Integer indexInit;
    protected Integer indexGoals;
    protected Metric metric;
    protected String pddlFilRef;
    protected String domainName;
    PddlDomain linkedDomain;
    protected boolean validatedAgainstDomain;
    public Set<GroundAction> actions;
    protected long propositionalTime;
    protected boolean grounded_representation;
    protected RelState possStates;
    public int counterNumericFluents = 0;
    protected boolean simplifyActions;
    protected HashMap staticFluents;
    public Condition belief;
    public Collection<Predicate> unknonw_predicates;
    public Collection<OneOf> one_of_s;
    public Collection<OrCond> or_s;
    public Collection<NumFluent> num_fluent_universe;
    public Collection<Predicate> predicates_universe;
    private FactoryConditions fc;
    public Set<Type> types;

    //This maps the string representation of a predicate (which uniquely defines it, into an integer)
    public HashMap<String, Predicate> predicateReference;
    public HashMap<String, NumFluent> numFluentReference;

    /**
     * Get the value of groundedActions
     *
     * @return the value of groundedActions
     */
    public boolean isGroundedActions() {
        return grounded_representation;
    }

    /**
     * Set the value of groundedActions
     *
     * @param groundedActions new value of groundedActions
     */
    public void setGroundedRepresentation(boolean groundedActions) {
        this.grounded_representation = groundedActions;
    }

    public PddlProblem(String problemFile, PDDLObjects po, Set<Type> types) {
        super();
        try {
            init = new State();
            indexObject = 0;
            indexInit = 0;
            indexGoals = 0;
            objects = new PDDLObjects();
            objects.addAll(po);
            metric = new Metric("NO");
            linkedDomain = null;
            actions = new LinkedHashSet();
            grounded_representation = false;
            validatedAgainstDomain = false;
            possStates = null;
            simplifyActions = true;
            this.types = types;
            predicateReference = new HashMap();
            numFluentReference = new HashMap();
            this.parseProblem(problemFile);

        } catch (IOException ex) {
            Logger.getLogger(PddlProblem.class.getName()).log(Level.SEVERE, null, ex);
        } catch (RecognitionException ex) {
            Logger.getLogger(PddlProblem.class.getName()).log(Level.SEVERE, null, ex);
        } catch (org.antlr.runtime.RecognitionException ex) {
            Logger.getLogger(PddlProblem.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * Get the value of domainName
     *
     * @return the value of domainName
     */
    public String getDomainName() {
        return domainName;
    }

    /**
     * Set the value of domainName
     *
     * @param domainName new value of domainName
     */
    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    /**
     * Get the value of pddlFilRef
     *
     * @return the value of pddlFilRef
     */
    public String getPddlFileReference() {
        return pddlFilRef;
    }

    /**
     * Set the value of pddlFilRef
     *
     * @param pddlFilRef new value of pddlFilRef
     */
    public void setPddlFilRef(String pddlFilRef) {
        this.pddlFilRef = pddlFilRef;
    }

    public void saveProblem(String pddlNewFile) throws IOException {

        pddlFilRef = pddlNewFile;

        String toWrite = "(define (problem " + this.getName() + ") "
                + "(:domain " + this.getDomainName() + ") "
                + this.getObjects().pddlPrint() + "\n"
                + this.init.pddlPrint() + "\n"
                + "(:goal " + this.getGoals().pddlPrint(false) + ")\n"
                + this.metric.pddlPrint() + "\n"
                + ")";
        Writer file = new BufferedWriter(new FileWriter(pddlNewFile));
        file.write(toWrite);
        file.close();
    }

    public void saveProblemWithObjectInterpretation(String pddlNewFile) throws IOException {

        pddlFilRef = pddlNewFile;

//        final StringBuilder toWrite = new StringBuilder().append(this.metric.pddlPrint()).append("\n"
//                + ")");
//        
        Writer file = new BufferedWriter(new FileWriter(pddlNewFile));
        file.write("(define (problem " + this.getName() + ") ");
        file.write("(:domain ");
        file.write(this.getDomainName() + ")");
        file.write(this.getObjects().pddlPrint());
        file.write(this.init.stringBuilderPddlPrintWithDummyTrue().toString());
        file.write("(:goal (forall (?interpr - interpretation)");
        file.write(this.getGoals().pddlPrintWithExtraObject() + ")))");
        file.close();
    }

    /**
     *
     */
    public PddlProblem() {

        init = new State();

        indexObject = 0;
        indexInit = 0;
        indexGoals = 0;
        objects = new PDDLObjects();
        metric = new Metric("NO");
        linkedDomain = null;
        actions = new HashSet();
        grounded_representation = false;

    }

    /**
     *
     * @param file - the pathfile representing the pddl problem
     * @throws IOException
     * @throws RecognitionException
     * @throws org.antlr.runtime.RecognitionException
     */
    public void parseProblem(String file) throws IOException, RecognitionException, org.antlr.runtime.RecognitionException {

        pddlFilRef = file;
        ANTLRInputStream in;
        in = new ANTLRInputStream(new FileInputStream(file));
        PddlLexer lexer = new PddlLexer(in);
        CommonTokenStream tokens = new CommonTokenStream(lexer);
        PddlParser parser = new PddlParser(tokens);
        PddlParser.pddlDoc_return root = parser.pddlDoc();

        if (parser.invalidGrammar()) {
            System.out.println("Grammar is violated");
        }
        //System.out.println("Problem Parsed, building data structure now");
        CommonTree t = (CommonTree) root.getTree();
//        System.out.println("tree:" + t.toStringTree());
//        exploreTree(t);
        this.one_of_s = new LinkedHashSet();
        this.unknonw_predicates = new LinkedHashSet();
        this.or_s = new LinkedHashSet();
        fc = new FactoryConditions(null, (LinkedHashSet<Type>) types, this.objects);
        for (int i = 0; i < t.getChildCount(); i++) {
            Tree child = t.getChild(i);
            //System.out.println(child.getChild(0).getText());

            switch (child.getType()) {
                case PddlParser.PROBLEM_DOMAIN:
                    this.setDomainName(child.getChild(0).getText());
                    break;
                case PddlParser.PROBLEM_NAME:
                    setName(child.getChild(0).getText());
                    break;
                case PddlParser.OBJECTS:
                    addObjects(child);
                    break;
                case PddlParser.INIT:
                    addInitFacts(child);
                    break;
                case PddlParser.FORMULAINIT:
                    this.belief = fc.createCondition(child.getChild(0), null);
                    break;
                case PddlParser.GOAL:
                    this.goals = null;
                    Condition con = fc.createCondition(child.getChild(0), null);
                    if (!(con instanceof ComplexCondition)){
                        this.goals = new AndCond();
                        this.goals.addConditions(con);
                    }else{
                        this.goals = (ComplexCondition) con;
                    }
                    
                    break;
                case PddlParser.PROBLEM_METRIC:
                    addMetric(child);
                    break;

            }
        }
        this.goals = (ComplexCondition) this.goals.push_not_to_terminals();
        this.goals = (ComplexCondition) this.goals.ground(new HashMap(),this.getObjects());
        this.keepCopyOfVariables(goals);
        this.keepCopyOfVariables(belief);
        this.keepUniqueVariable(init);
        //System.out.println("Total number of Numeric Fluents:"+this.counterNumericFluents);
    }

    protected void addObjects(Tree c) {
        for (int i = 0; i < c.getChildCount(); i++) {
            if (this.linkedDomain != null) {
                Type t = linkedDomain.getTypeByName(c.getChild(i).getChild(0).getText());
                if (t == null) {
                    System.out.println(c.getChild(i).getChild(0).getText() + " not found");
                    System.exit(-1);
                }
                this.getObjects().add(new PDDLObject(c.getChild(i).getText(), t));

            } else {
                this.getObjects().add(new PDDLObject(c.getChild(i).getText(), new Type(c.getChild(i).getChild(0).getText())));
//            System.out.println("Aggiungo l'oggetto:" + c.getChild(i).getText());
            }
//            System.out.println("che è di tipo:" + new Type(c.getChild(i).getChild(0).getText()));
        }
    }

    //Aggiungere controllo su dominio...in qualche modo!
    protected Predicate buildInstPredicate(Tree t, SchemaParameters aug_par_table) {

        //if (t.getType() == PddlParser.PRED_INST) {
        Predicate a = new Predicate(true);
        a.setPredicateName(t.getChild(0).getText());
        a.setGrounded(true);
        //System.out.println(a);
        for (int i = 1; i < t.getChildCount(); i++) {

            PDDLObject t1 = (PDDLObject) this.getObjectByName(t.getChild(i).getText());
            if (t1 != null) {
                a.addObject(t1);
            } else {

                System.out.println("Object " + t.getChild(i).getText() + " does not exist. Issue in building predicate " + a.getPredicateName());
                System.exit(-1);
            }
        }
        return a;
        //}

        //return null;
    }

    protected Expression createExpression(Tree t) {

        int test = t.getType();
        switch (t.getType()) {
            case PddlParser.BINARY_OP: {
                BinaryOp ret = new BinaryOp();
                ret.setOperator(t.getChild(0).getText());
                ret.setOne(createExpression(t.getChild(1)));
                ret.setRight(createExpression(t.getChild(2)));
                ret.grounded = true;
                return ret;
            }
            case PddlParser.NUMBER: {
                //Float.
                PDDLNumber ret = new PDDLNumber(Float.valueOf(t.getText()));
                return ret;
            }
            case PddlParser.FUNC_HEAD: {
                NumFluent ret = new NumFluent(t.getChild(0).getText());

                for (int i = 1; i < t.getChildCount(); i++) {
                    ret.addTerms(this.getObjectByName(t.getChild(i).getText()));
                }

                ret.grounded = true;
                return ret;
            }
            case PddlParser.UNARY_MINUS:
                return new MinusUnary(createExpression(t.getChild(0)));
            case PddlParser.MULTI_OP: {
                MultiOp ret = new MultiOp(t.getChild(0).getText());
                for (int i = 1; i < t.getChildCount(); i++) {
                    //System.out.println("Figlio di + o * " + createExpression(t.getChild(i)));
                    ret.addExpression(createExpression(t.getChild(i)));
                }
                ret.grounded = true;
                return ret;
            }
            default:
                break;
        }

        return null;

    }

    protected void addInitFacts(Tree child) {
        for (int i = 0; i < child.getChildCount(); i++) {
            Tree c = child.getChild(i);
            switch (c.getType()) {
                case PddlParser.PRED_INST:
                    init.initPred.put(buildInstPredicate(c, null), true);
//                    init.setPredTrue(buildInstPredicate(c, null));
                    break;
                case PddlParser.INIT_EQ:
                    counterNumericFluents++;
                    NumFluentValue a = new NumFluentValue("=");
                    
                    a.setNFluent((NumFluent) createExpression(c.getChild(0)));
                    a.setValue((PDDLNumber) createExpression(c.getChild(1)));
                    //System.out.println(a);
                    init.addNumericFluent(a);
                    break;
                case PddlParser.INIT_AT:
                    init.addTimedLiteral(buildInstPredicate(c, null));
                    break;
                case PddlParser.UNKNOWN:
//                    System.out.println("DEBUG: unknonw");
                    this.unknonw_predicates.add((Predicate) addUnknown(c));
                    break;
                case PddlParser.ONEOF:
//                    System.out.println("DEBUG: oneof");
//                    fc.createCondition(c, null);
                    this.one_of_s.add((OneOf) fc.createCondition(c, null));
                    break;
                case PddlParser.OR_GD:
//                    System.out.println("DEBUG: or Conditition");
                    this.or_s.add((OrCond) fc.createCondition(c, null));
                    break;
                default:
                    break;
            }

        }
    }

    /**
     * A pretty representation for the pddl problem
     */
    public void prettyPrint() {

        System.out.println("\ninit:" + getInit() + "\nObject" + getProblemObjects() + "\nGoals:" + getGoals() + "\n" + this.metric.toString());

        if (metric.getMetExpr() instanceof MultiOp) {
            MultiOp temp = (MultiOp) metric.getMetExpr();
            System.out.println("\n metrica ha :" + temp.getExpr().size());

        }

    }

    protected void exploreTree(Tree t) {
        if (t == null) {
            return;
        }
        if (t.getChildCount() == 0) {
            System.out.println("Foglia:" + t.getText() + "Tipo:" + t.getType());
            return;
        }
        System.out.println("Nodo intermedio: " + t.getText() + "Tipo:" + t.getType());
        for (int i = 0; i < t.getChildCount(); i++) {
            exploreTree(t.getChild(i));
        }
        return;
    }

    /**
     * @return the objects - the objects of the pddl problem
     */
    public PDDLObjects getProblemObjects() {
        return getObjects();
    }

    /**
     * @return the init - the initial status of the problem
     */
    public State getInit() {
        return init;
    }

    /**
     * @return the goals - the goal set
     */
    public ComplexCondition getGoals() {
        return goals;
    }

    /**
     * @return the name - the name of the problem
     */
    public String getName() {
        return name;
    }

    protected void addMetric(Tree t) {

        //System.out.println(t.toStringTree());
        metric = new Metric(t.getChild(0).getText());
        metric.setMetExpr(createExpression(t.getChild(1)));

    }

    public void setMetric(Metric m) {
        this.metric = m;
    }


    public void setInit(State init) {
        this.init = init;
    }

    public void setGoals(ComplexCondition goals) {
        this.goals = goals;
    }

    /**
     * @param name the name to set
     */
    protected void setName(String name) {
        this.name = name;
    }


    /**
     * @return the metric
     */
    public Metric getMetric() {
        return metric;
    }

    /**
     *
     * @param string - the name of the object we want
     * @return the term representing the object
     */
    public PDDLObject getObjectByName(String string) {
        for (Object o : this.getObjects()) {
            PDDLObject el = (PDDLObject) o;
            if (el.getName().equalsIgnoreCase(string)) {
                return el;
            }
        }
        return null;
    }

    public Float getInitFunctionValue(NumFluent f) {
        return init.functionValue(f).getNumber();
    }

    public NumFluent getFunction(String string, ArrayList terms) {
        for (Object o : init.getNumericFluents()) {

            if (o instanceof NumFluentValue) {
                NumFluentValue ele = (NumFluentValue) o;
                NumFluent fAssign = ele.getNFluent();

                if (fAssign.getName().equals(string)) {
                    if (fAssign.getTerms().equals(terms)) {
                        return fAssign;
                    }
                }
            }

        }
        return null;
    }

    public ArrayList getFunctions() {
        ArrayList res = new ArrayList();

        for (Object o : init.getNumericFluents()) {

            if (o instanceof NumFluentValue) {
                NumFluentValue ele = (NumFluentValue) o;
                NumFluent fAssign = ele.getNFluent();
                res.add(fAssign);

            }

        }
        return res;
    }

    public void setDomain(PddlDomain aThis) {
        linkedDomain = aThis;

    }

    public void generateActions() throws Exception {

        long start = System.currentTimeMillis();
        if (this.isValidatedAgainstDomain()) {
            Grounder af = new Grounder();
            for (ActionSchema act : (Set<ActionSchema>) linkedDomain.getActionsSchema()) {
                if (act.getPar().size() != 0) {
                    getActions().addAll(af.Propositionalize(act, getObjects()));
                } else {
                    GroundAction gr = act.ground();
                    getActions().add(gr);
                }
            }
            //pruneActions();
        } else {
            System.err.println("Please connect the domain of the problem via validation");
            System.exit(-1);
        }
        Iterator it = getActions().iterator();
        //System.out.println("prova");
        System.out.println("|A| just after grounding:" + getActions().size());
        while (it.hasNext()) {//iteration of the action for pruning the trivial unreacheable ones (because of the grounding and weak evaluation)
            GroundAction act = (GroundAction) it.next();
            boolean keep = true;
            if (isSimplifyActions()) {
//                System.out.println(act.toPDDL());
                keep = act.simplifyModel(linkedDomain, this);
//                System.out.println(act.toPDDL());

            }
            if (!keep) {
                //System.out.println("Action removed:" + act.toEcoString());
                it.remove();
            }
        }
        System.out.println("|A| just after simplification:" + getActions().size());

        setPropositionalTime(System.currentTimeMillis() - start);
        this.setGroundedRepresentation(true);

    }

    public int distance(State sIn, Condition c) {

        Set level;
        RelState s = sIn.relaxState();
        int distance = 0;
        while (true) {
            if (s.satisfy(c)) {
                return distance;
            } else {
                distance++;
                level = new HashSet();
                for (Iterator it = getActions().iterator(); it.hasNext();) {
                    GroundAction gr = (GroundAction) it.next();
                    if (gr.getPreconditions().can_be_true(s)) {
                        level.add(gr);
                        it.remove();
                    }
                }
                if (level.isEmpty()) {
                    return Integer.MAX_VALUE;
                }
                for (Object o : level) {
                    GroundAction gr = (GroundAction) o;
                    gr.apply(s);
                }
            }
        }
    }

    public Map distance(State sIn, List c_s) {

        Set level;
        RelState s = sIn.relaxState();

        Map order = new HashMap();
        ArrayList toVisit = new ArrayList();
        toVisit.addAll(c_s);
        int distance = 0;
        while (true) {
            for (Iterator it = toVisit.iterator(); it.hasNext();) {
                Condition c = (Condition) it.next();
                if (s.satisfy(c)) {
                    order.put(c, distance);
                    it.remove();
                }

            }
            if (toVisit.isEmpty()) {
                return order;
            } else {
                distance++;
                level = new HashSet();
                for (Iterator it = getActions().iterator(); it.hasNext();) {
                    GroundAction gr = (GroundAction) it.next();
                    if (gr.getPreconditions().can_be_true(s)) {
                        level.add(gr);
                        it.remove();
                    }
                }
                if (level.isEmpty()) {
                    return order;
                }
                for (Object o : level) {
                    GroundAction gr = (GroundAction) o;
                    gr.apply(s);
                }
            }
        }
    }

    protected void pruneActions() {
        boolean finished = false;
        boolean goalReached = false;
        Set level;
        RelState s = this.init.relaxState();
        int prec = 0;
        int distance = 0;
        Set totalActions = new HashSet();
        while (!finished && !goalReached) {
            distance++;
            level = new HashSet();
            for (Iterator it = getActions().iterator(); it.hasNext();) {
                GroundAction gr = (GroundAction) it.next();
                //System.out.println(gr.toEcoString());
                if (gr.getPreconditions().can_be_true(s)) {
                    totalActions.add(gr);
                    level.add(gr);
                    it.remove();
                }
            }

            for (Object o : level) {
                GroundAction gr = (GroundAction) o;
                gr.apply(s);
            }
            //if (s.satisfy(getGoals()))
            //  goalReached=true;
            System.out.println("Distance: " + distance);
            System.out.println("ApplicableActions: " + level.size());
            if (prec == totalActions.size()) {
                finished = true;
                getActions().clear();
                setActions(totalActions);
            }
            prec = totalActions.size();
        }

    }


    /**
     * @return the propositionalTime
     */
    public long getPropositionalTime() {
        return propositionalTime;
    }

    /**
     * @param propositionalTime the propositionalTime to set
     */
    public void setPropositionalTime(long propositionalTime) {
        this.propositionalTime = propositionalTime;
    }

    /**
     * @return the actions
     */
    public Set getActions() {
        return actions;
    }

    /**
     * @param actions the actions to set
     */
    public void setActions(Set actions) {
        this.actions = actions;
    }

    public Map computeKernelDistance(ArrayList k) {
        boolean finished = false;
        boolean kernelVisited = false;
        Set level;
        RelState s = this.init.relaxState();
        int prec = 0;
        ArrayList toVisit = new ArrayList();
        toVisit.addAll(k);
        Map order = new HashMap();
        Set totalActions = new HashSet();
        int distance = -1;
        while (!finished && !kernelVisited) {
            distance++;
            level = new HashSet();
            for (Iterator it = getActions().iterator(); it.hasNext();) {
                GroundAction gr = (GroundAction) it.next();
                //System.out.println(gr.toEcoString());
                if (gr.getPreconditions().can_be_true(s)) {
                    totalActions.add(gr);
                    level.add(gr);
                    it.remove();
                }
            }

            for (Object o : level) {
                GroundAction gr = (GroundAction) o;
                gr.apply(s);
            }
            for (Iterator it = toVisit.iterator(); it.hasNext();) {
                Condition con = (Condition) it.next();

                if (s.satisfy(con)) {
                    //System.out.println("Kernel " + con + " raggiunto a livello:" + distance);
                    order.put((k.size() - k.indexOf(con)), distance);
                    it.remove();
                }
            }
            if (toVisit.isEmpty()) {
                kernelVisited = true;

            }
            //  goalReached=true;
            System.out.println("Distance: " + distance);
            System.out.println("ApplicableActions: " + level.size());
            if (prec == totalActions.size()) {
                finished = true;
            } else {
                prec = totalActions.size();
            }
        }
        getActions().clear();
        setActions(totalActions);

        return order;
    }

    public void parseProblem(String string, PDDLObjects constants) throws IOException, RecognitionException, org.antlr.runtime.RecognitionException {
        this.getObjects().addAll(constants);
        parseProblem(string);
    }

    /**
     * @return the validatedAgainstDomain
     */
    public boolean isValidatedAgainstDomain() {
        return validatedAgainstDomain;
    }

    /**
     * @param validatedAgainstDomain the validatedAgainstDomain to set
     */
    public void setValidatedAgainstDomain(boolean validatedAgainstDomain) {
        this.validatedAgainstDomain = validatedAgainstDomain;
    }

    /**
     * @return the possStates
     */
    public RelState getPossStates() {
        return possStates;
    }

    /**
     * @param possStates the possStates to set
     */
    public void setPossStates(RelState possStates) {
        this.possStates = possStates;
    }

    public void removeObjects(ParametersAsTerms constantsFound) {
        for (Object c : constantsFound) {
            this.getObjects().remove(c);
        }
    }

    /**
     * @return the objects
     */
    public PDDLObjects getObjects() {
        return objects;
    }

    /**
     * @param objects the objects to set
     */
    public void setObjects(PDDLObjects objects) {
        this.objects = objects;
    }

    /**
     * @return the simplifyActions
     */
    public boolean isSimplifyActions() {
        return simplifyActions;
    }

    /**
     * @param simplifyActions the simplifyActions to set
     */
    public void setSimplifyActions(boolean simplifyActions) {
        this.simplifyActions = simplifyActions;
    }

    public HashMap getActualFluents() throws Exception {
        if (staticFluents == null) {
            staticFluents = new HashMap();
            if (this.getActions() == null || this.getActions().isEmpty()) {
                this.generateActions();
            }
            for (GroundAction gr : (Collection<GroundAction>) this.getActions()) {
                for (NumFluent nf : gr.getNumericFluentAffected().keySet()) {
                    staticFluents.put(nf, Boolean.FALSE);
                }
            }
        }
        return staticFluents;
    }

    public void transformNumericConditionsInActions() throws Exception {

        for (GroundAction gr : (Collection<GroundAction>) this.actions) {
            if (gr.getPreconditions() != null) {
                gr.setPreconditions((ComplexCondition) generate_inequalities(gr.getPreconditions()));
            }
        }
        this.goals = (ComplexCondition) generate_inequalities(goals);
    }

    protected ComplexCondition generate_inequalities(Condition con) {
        return (ComplexCondition)con.transform_equality();
    }

    public boolean print_actions() {
        for (GroundAction gr : (Collection<GroundAction>) this.actions) {
            System.out.println(gr.toFileCompliant());
        }

        return true;
    }

    private Condition addUnknown(Tree infoAction) {
        if (infoAction == null) {
            return null;
        }
        if (infoAction.getType() == PddlParser.PRED_INST) {
            //estrapola tutti i predicati e ritornali come set di predicati
//            AndCond and = new AndCond();
//            and.addConditions();
            return buildInstPredicate(infoAction, null);
        } else if (infoAction.getType() == PddlParser.UNKNOWN) {

            return addUnknown(infoAction.getChild(0));

        } else {
            System.out.println("Some serious error:" + infoAction);
            return null;
        }
    }





    
    
    public void keepCopyOfVariables(Condition cond) {
        if (cond != null && cond.getInvolvedPredicates() != null){
            for (Predicate p : cond.getInvolvedPredicates()) {
                PddlProblem.this.keepUniqueVariable(p);
            }
            for (NumFluent x : cond.getInvolvedFluents()) {
                PddlProblem.this.keepUniqueVariable(x);
            }
        }
    }

    public void keepUniqueVariable(GenericActionType act) {
        
        
        
        
        for (Predicate p : act.getInvolvedPredicates()) {
            PddlProblem.this.keepUniqueVariable(p);
        }
        for (NumFluent x : act.getInvolvedNumFluents()) {
            PddlProblem.this.keepUniqueVariable(x);
        }
    }

    public void keepUniqueVariable(State s) {
        for (Predicate p : s.getPropositions()) {
            PddlProblem.this.keepUniqueVariable(p);
        }
        for (NumFluent x : s.getNumericFluents()) {
            PddlProblem.this.keepUniqueVariable(x);
        }
    }

    private void keepUniqueVariable(Predicate p) {
        Predicate p1 = this.predicateReference.get(p.toString());
        if (p1 == null) {
            this.predicateReference.put(p.toString(), p);
        }
    }

    private void keepUniqueVariable(NumFluent x) {
        NumFluent x1 = this.numFluentReference.get(x.toString());
        if (x1 == null) {
            this.numFluentReference.put(x.toString(), x);
        }
    }
    
    protected void syncVariables(Metric cond) {
        if (cond != null && cond.getMetExpr()!= null){
            for (NumFluent x : cond.getMetExpr().rhsFluents()) {
                PddlProblem.this.keepUniqueVariable(x);
            }
        }    
    }
}
