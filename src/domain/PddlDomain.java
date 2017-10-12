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
package domain;

import conditions.ForAll;
import conditions.AndCond;
import conditions.Comparison;
import conditions.ConditionalEffect;
import conditions.Condition;
import conditions.FactoryConditions;
import conditions.NotCond;
import conditions.NumFluentValue;
import conditions.PDDLObject;
import conditions.PostCondition;
import conditions.Predicate;
import expressions.ComplexFunction;
import expressions.BinaryOp;
import expressions.Expression;
import expressions.MinusUnary;
import expressions.MultiOp;
import expressions.NumEffect;
import expressions.NumFluent;
import expressions.PDDLNumber;
import expressions.TrigonometricFunction;
import extraUtils.Utils;
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
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.antlr.runtime.ANTLRInputStream;
import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.CommonTree;
import org.antlr.runtime.tree.Tree;
import parser.PddlLexer;
import parser.PddlParser;
import problem.PDDLObjects;
import problem.PddlProblem;

/**
 *
 * @author enrico
 */
public final class PddlDomain extends Object {

    private String name;
    protected Set<ActionSchema> ActionsSchema;
    private Set<ProcessSchema> ProcessesSchema;
    private Set<EventSchema> eventsSchema;

    private PredicateSet predicates;
    private Set<Type> types;
    private PDDLObjects constants;
    private List functions;
    private List<NumFluent> derived_variables;
    private List DurativeActions;
    private List<String> Requirements;
    private String pddlReferenceFile;
    private HashMap abstractInvariantFluents;
    private LinkedHashSet<SchemaGlobalConstraint> SchemaGlobalConstraints;
    private FactoryConditions fc;

    private PddlDomain(Set<ActionSchema> ActionsSchema, PredicateSet Predicates, Set<Type> types, List Functions, List DurativeActions, List<String> Requirements) {
        this.ActionsSchema = ActionsSchema;
        this.predicates = Predicates;
        this.types = types;
        this.functions = Functions;
        this.DurativeActions = DurativeActions;
        this.Requirements = Requirements;
        SchemaGlobalConstraints = new LinkedHashSet();
    }

    /**
     *
     */
    public PddlDomain() {
        super();
        types = new LinkedHashSet<>();
        ActionsSchema = new TreeSet<>(new ActionComparator());
        functions = new ArrayList();
        derived_variables = new ArrayList();
        Requirements = new ArrayList<>();
        constants = new PDDLObjects();
        SchemaGlobalConstraints = new LinkedHashSet();
        ProcessesSchema = new LinkedHashSet();
        eventsSchema = new LinkedHashSet();

    }

    public PddlDomain(String domainFile) {
        super();
        try {
            SchemaGlobalConstraints = new LinkedHashSet();

            types = new LinkedHashSet<>();
            ActionsSchema = new TreeSet<>(new ActionComparator());
            functions = new ArrayList();
            derived_variables = new ArrayList();
            Requirements = new ArrayList<>();
            constants = new PDDLObjects();
            ProcessesSchema = new LinkedHashSet();
            eventsSchema = new LinkedHashSet();

            this.parseDomain(domainFile);
        } catch (IOException | RecognitionException ex) {
            Logger.getLogger(PddlDomain.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param p - The PddlProblem to validate the consistency for. BETA
     * @return true whether the problem is consistent wrt to the domain.
     * Otherwise false
     */
    public boolean validate(PddlProblem p) {

        for (Object o : p.getProblemObjects()) {
            PDDLObject t = (PDDLObject) o;
            Iterator<Type> it = types.iterator();
            boolean founded = false;
            while (it.hasNext()) {
                Type ele = it.next();
                if (ele.equals(t.getType())) {
                    t.setType(ele);
                    founded = true;
                    break;
                }
            }
            if (!founded) {
                System.out.println("The following object is not valid:" + t);
                System.exit(-1);
            }
        }

        for (Object o : p.getInit().getPropositions()) {
            if (o instanceof NumFluentValue) {
                NumFluentValue nf = (NumFluentValue) o;
                for (Object o1 : nf.getNFluent().getTerms()) {
                    PDDLObject t = (PDDLObject) o1;
                    Iterator<Type> it = types.iterator();
                    boolean founded = false;
                    while (it.hasNext()) {
                        Type ele = it.next();
                        if (ele.equals(t.getType())) {
                            t.setType(ele);
                            founded = true;
                            break;
                        }
                    }
                    if (!founded) {
                        System.out.println("The following object is not valid:" + t);
                        System.exit(-1);
                    }
                }
            } else {
                Predicate t1 = (Predicate) o;
                for (Object o1 : t1.getTerms()) {
                    PDDLObject t = (PDDLObject) o1;
                    Iterator<Type> it = types.iterator();
                    boolean found = false;
                    while (it.hasNext()) {
                        Type ele = it.next();
                        if (ele.equals(t.getType())) {
                            t.setType(ele);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("The following object is not valid:" + t);
                        System.exit(-1);
                    }
                }
            }
        }

        for (Object o : p.getInit().getNumericFluents()) {

            if (o instanceof NumFluent) {

                NumFluent nf = (NumFluent) o;
//                System.out.println(nf.getName());
                for (Object o1 : nf.getTerms()) {
                    PDDLObject t = (PDDLObject) o1;
                    Iterator<Type> it = types.iterator();
                    boolean found = false;
                    while (it.hasNext()) {
                        Type ele = it.next();
                        if (t == null) {
                            System.out.println("Type error; Probably you are using an object in a numeric fluent which is not specified..");
                            System.out.println("    It happened when dealing with: " + nf);
                            return false;
                        }
                        if (ele.equals(t.getType())) {
                            t.setType(ele);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("The following object is not valid:" + t);
                        System.exit(-1);
                    }
                }
            } else {
                Predicate t1 = (Predicate) o;
                for (Object o1 : t1.getTerms()) {
                    PDDLObject t = (PDDLObject) o1;
                    Iterator<Type> it = types.iterator();
                    boolean found = false;
                    while (it.hasNext()) {
                        Type ele = it.next();
                        if (ele.equals(t.getType())) {
                            t.setType(ele);
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("The following object is not valid:" + t);
                        System.exit(-1);
                    }
                }
            }
        }
        if (p.getGoals().sons != null) {
            for (Object o : p.getGoals().sons) {

                if (o instanceof Predicate) {

                    Predicate t = (Predicate) o;
                    //                if (!predicates.validateInst(t)) {
                    //                    System.out.println("Predicato: " + t + " non valido");
                    //                    System.exit(-1);
                    //
                    //                }
                }
            }
        }
        for (final PDDLObject o : this.getConstants()) {
            p.getProblemObjects().add(o);
        }
        //System.out.println(p.getProblemObjects());

        p.setDomain(this);
        p.setValidatedAgainstDomain(true);

//        p.generate_universe_of_variables(this.getPredicates(),this.getFunctions(),this.derived_variables);
//        System.out.println(p.num_fluent_universe);
//        System.out.println(p.predicates_universe);
        return true;
    }

    /**
     *
     * @param file - the path of the pddl file representing the domain. As
     * return the object will be fullfilled with the information in the pddl
     * domain file
     * @throws IOException
     * @throws RecognitionException
     */
    public void parseDomain(String file) throws IOException, RecognitionException {
        this.setPddlFilRef(file);
        ANTLRInputStream in;
        in = new ANTLRInputStream(new FileInputStream(file));
        PddlLexer lexer = new PddlLexer(in);
        CommonTokenStream tokens = new CommonTokenStream(lexer);

        PddlParser parser = new PddlParser(tokens);
        PddlParser.pddlDoc_return root = parser.pddlDoc();

        if (parser.invalidGrammar()) {
            System.out.println("Some Syntax Error");
            System.exit(-1);
        }

//        fc = new FactoryConditions(this.predicates, (LinkedHashSet<Type>) this.types,this.constants);
        CommonTree t = (CommonTree) root.getTree();
        int i;
        for (i = 0; i < t.getChildCount(); i++) {
            Tree c = (Tree) t.getChild(i);
            int type = c.getType();
//            System.out.println("Tipo: " + c.getText());
            switch (type) {
                case PddlParser.DOMAIN_NAME:
                    setName(c.getChild(0).getText());
                    break;
                case PddlParser.TYPES:
                    addTypes(c);
                    break;
                case PddlParser.PREDICATES:
                    this.predicates = (PredicateSet) addPredicates(c);
                    fc = new FactoryConditions(this.predicates, (LinkedHashSet<Type>) this.types,this.constants);
                    break;
                case PddlParser.ACTION:
                    addActions(c);
                    break;
                case PddlParser.EVENT:
                    addEvent(c);
                    break;
                case PddlParser.REQUIREMENTS:
                    addRequirements(c);
                    break;
                case PddlParser.FUNCTIONS:
                    addFunctions(c);
                    break;
                case PddlParser.CONSTANTS:
                    addConstants(c);
                    break;
                case PddlParser.FREE_FUNCTIONS:
                    addFree_Functions(c);
                    break;
                case PddlParser.GLOBAL_CONSTRAINT:
                    addGlobal_constraint(c);
                    break;
                case PddlParser.PROCESS:
                    addProcess(c);
                    break;
//                case PddlParser.DOM_CONSTRAINTS:
//                    addGlobalConstraints(c);
//                    break; 
            }
        }
        push_not_at_the_terminals();

    }

    /**
     * A pretty representation of the domain
     */
    public void prettyPrint() {
        System.out.println("Requirements: " + this.Requirements);
        System.out.println("Actions Domain: " + this.ActionsSchema);
        if (this.ProcessesSchema != null) {
            System.out.println("Process Domain: " + this.ProcessesSchema);
        }
        //if (!this.eventsSchema.isEmpty())
        System.out.println("Events Domain: " + this.eventsSchema);
        System.out.println("Predicates: " + this.predicates);
        System.out.println("Functions: " + this.functions);
        System.out.println("Global Constraints: " + this.getSchemaGlobalConstraints());

    }

    /**
     * @return the ActionsSchema- a Set which contains all the action schema of
     * the domain
     */
    public Set<ActionSchema> getActionsSchema() {
        return ActionsSchema;
    }

    /**
     * @return the types declared in the domain
     */
    public Set<Type> getTypes() {
        return types;
    }

    /**
     * @param types the types to set
     */
    private void setTypes(Set<Type> Types) {
        this.types = Types;
    }

    /**
     * @return the functions declared in the domain
     */
    public List getFunctions() {
        return functions;
    }

    /**
     * @param functions the functions to set
     */
    private void setFunctions(List Functions) {
        this.functions = Functions;
    }

    /**
     * @return the DurativeActions
     */
    private List getDurativeActions() {
        return DurativeActions;
    }

    /**
     * @param DurativeActions the DurativeActions to set
     */
    private void setDurativeActions(List DurativeActions) {
        this.DurativeActions = DurativeActions;
    }

    /**
     * @return the Requirements declared in the domain
     */
    public List<String> getRequirements() {
        return Requirements;
    }

    /**
     * @param Requirements the Requirements to set
     */
    private void setRequirements(List<String> Requirements) {
        this.Requirements = Requirements;
    }

    /**
     * @return the predicates definitions
     */
    public PredicateSet getPredicates() {
        return predicates;
    }

    /**
     * @param predicates the predicates to set
     */
    private void setPredicates(PredicateSet Predicates) {
        this.predicates = Predicates;
    }

    //da migliorare perchè dovrebbe rappresentare una potenziale gerarchica di oggetti!!!
    private void addTypes(Tree c) {
        for (int i = 0; i < c.getChildCount(); i++) {
            Type tip = new Type(c.getChild(i).getText());

            Tree tipo = (Tree) c.getChild(i);
            if (tipo.getChildCount() > 0) {
                boolean subTypeOfExist = false;
                Type father = new Type(tipo.getChild(0).getText());
                if (father.isObject()) {
                    if (!(types.contains(father))) {
                        types.add(father);
                    }
                    tip.setSubTypeOf(father);
                    subTypeOfExist = true;
                } else {
                    Iterator<Type> it = types.iterator();
                    while (it.hasNext()) {
                        Type ele = it.next();
                        if (ele.equals(father)) {
                            tip.setSubTypeOf(ele);
                            subTypeOfExist = true;
                            break;
                        }
                    }
                }
                if (subTypeOfExist) {
                    this.types.add(tip);
                } else {
                    System.out.println("Error: " + tip + " has declared father: " + father + " which is not a type neither an object; inferring object");
                    tip.setSubTypeOf(new Type("object"));
                    this.types.add(tip);
                }
            } else {
                this.types.add(tip);
            }
//                
//            
//            this.types.add(tip);

        }
    }

    private void addActions(Tree c) {
        this.addGenericActionSchemas(c, new ActionSchema());
    }

   

    private Object addPredicates(Tree t) {
        PredicateSet col = new PredicateSet();
        if (t == null) {
            return null;
        } //Assumo che ogni variabile sia tipata: da aggiungere nella grammatica l'impossibilità di avere variabili non tipate
        //in realtà anche per pddl non tipati funziona. Il risultato ritornato è un null che è comunque un risultato accettabile come tipo.
        if (t.getChildCount() == 0) {
            Type unTipo = new Type(t.getText());
            if (types.contains(unTipo)) {
                return unTipo;
            } else {
                return null;
            }
        }
        if (t.getType() == PddlParser.PREDICATES) {//Sono uno dei predicati
            for (int i = 0; i < t.getChildCount(); i++) {
                Tree child = t.getChild(i);
                Predicate p = new Predicate();
                p.setPredicateName(child.getText());
                Variable v;
                for (int j = 0; j < child.getChildCount(); j++) {
                    v = (Variable) addPredicates(child.getChild(j));
                    p.addVariable(v);
                }
                col.add(p);
            }
            return col;
        } else {
            Variable v = new Variable(t.getText());

            v.setType((Type) addPredicates(t.getChild(0)));
            return v;
        }
    }

    private PostCondition createPostCondition(SchemaParameters parTable, Tree infoAction) {

        if (infoAction == null) {
            return new AndCond();
        }
        switch (infoAction.getType()) {
            case PddlParser.PRED_HEAD:
                //estrapola tutti i predicati e ritornali come set di predicati
                return fc.buildPredicate(infoAction, parTable);

            case PddlParser.AND_EFFECT:
                AndCond and = new AndCond();
                for (int i = 0; i < infoAction.getChildCount(); i++) {
                    Object ret_val = createPostCondition(parTable, infoAction.getChild(i));
                    if (ret_val != null) {
                        and.sons.add(ret_val);
                    }
                }
                return and;
            case PddlParser.NOT_EFFECT:
                Condition ret_val = (Condition) createPostCondition(parTable, infoAction.getChild(0));
                NotCond not = new NotCond(ret_val);
                return not;
            case PddlParser.ASSIGN_EFFECT:
                NumEffect a = new NumEffect(infoAction.getChild(0).getText());
//                System.out.println("DEBUG: Working out this effect:"+a);
                a.setFluentAffected((NumFluent) createExpression(infoAction.getChild(1), parTable));
                a.setRight((Expression) createExpression(infoAction.getChild(2), parTable));
                return a;
            case PddlParser.FORALL_EFFECT:
                ForAll forall = this.createForAllEffect(infoAction, parTable);
                return forall;
            case PddlParser.WHEN_EFFECT:
                Condition lhs = fc.createCondition(infoAction.getChild(0), parTable);
                PostCondition rhs = (PostCondition) this.createPostCondition(parTable, infoAction.getChild(1));
                return new ConditionalEffect(lhs, rhs);
            default:
                System.out.println("Serious error in parsing:" + infoAction);
                System.err.println("ADL not fully supported");
                System.exit(-1);
                break;
        }
        return null;
    }

    private void addRequirements(Tree c) {
        if (c != null) {
            //System.out.println(c.getText());
            for (int i = 0; i < c.getChildCount(); i++) {
                final String req = c.getChild(i).getText();
                this.Requirements.add(req);
            }
        }
    }

    private void addFunctions(Tree c) {
        if (c != null) {
            for (int i = 0; i < c.getChildCount(); i++) {

                //System.out.println(c.getChild(i).getText());
                NumFluent ret = new NumFluent(c.getChild(i).getText());
                Tree t = c.getChild(i);
                for (int j = 0; j < t.getChildCount(); j++) {
                    Variable v = new Variable(t.getChild(j).getText());
                    if (t.getChild(j).getChild(0) != null);
                    //System.out.println(t.getChild(j));

                    //System.out.println(t.getChild(j).getChild(0));
                    v.setType(new Type(t.getChild(j).getChild(0).getText()));

                    ret.addVariable(v);
                }
                this.functions.add(ret);
            }
        }
    }

    //
    /**
     * Returns the action with specified name. Notice that this method is rather
     * inefficient if there are many actions, and that a table that maps names
     * to action schemas could be more effective.
     *
     * @param name the name of the action
     * @return an ActionSchema object (if any) with the name in input this
     * assumes that there is a 1:1 relation between action and name, i.e. we
     * cannot have different actions with the same name
     */
    public ActionSchema getActionByName(String name) {
        for (final ActionSchema el : ActionsSchema) {
            final String elname = el.getName();
            if (elname.equalsIgnoreCase(name)) {
                return el;
            }
        }

        return null;
    }

    private Expression createExpression(Tree t, SchemaParameters parTable) {

        switch (t.getType()) {
            case PddlParser.BINARY_OP: {
                BinaryOp ret = new BinaryOp();
                ret.setOperator(t.getChild(0).getText());
                ret.setOne(createExpression(t.getChild(1), parTable));
                ret.setRight(createExpression(t.getChild(2), parTable));

                return ret;
            }
            case PddlParser.SIN: {
                TrigonometricFunction ret = new TrigonometricFunction();
//                System.out.println(t.getChild(1));
                ret.setOperator("sin");
                ret.setArg(createExpression(t.getChild(0), parTable));
                return ret;
            }
            case PddlParser.ABS: {
                ComplexFunction ret = new ComplexFunction();
//                System.out.println(t.getChild(1));
                ret.setOperator("abs");
                ret.setArg(createExpression(t.getChild(0), parTable));
                return ret;
            }
            case PddlParser.COS: {
                TrigonometricFunction ret = new TrigonometricFunction();
//                System.out.println(t.getChild(1));
                ret.setOperator("cos");
                ret.setArg(createExpression(t.getChild(0), parTable));
                return ret;
            }
            case PddlParser.NUMBER: {
                PDDLNumber ret = new PDDLNumber(new Float(t.getText()));
                return ret;
            }
            case PddlParser.FUNC_HEAD: {
                NumFluent ret = new NumFluent(t.getChild(0).getText());
                for (int i = 1; i < t.getChildCount(); i++) {
//                System.out.println("Constant Type:" + PddlParser.CONSTANTS);
//                System.out.println("Name Type:" + PddlParser.NAME);
//                System.out.println("Current Type:" + t.getChild(i).getType());
                    if (t.getChild(i).getType() == PddlParser.NAME) {
                        PDDLObject o = new PDDLObject(t.getChild(i).getText());
                        PDDLObject o1 = this.getConstants().containsTerm(o);
                        if (o1 != null) {
                            ret.addTerms(o1);
                        } else {

                            System.out.println("NumFluent:Variable " + o + " is not a constant object");
                            System.exit(-1);
                        }
                    } else {
                        Variable v = new Variable(t.getChild(i).getText());
                        //System.out.println(parTable);
                        Variable v1 = parTable.containsVariable(v);

                        if (v1 != null) {
                            ret.addVariable(v1);
                        } else {
//                        System.out.println("t.getType: " + t.getChild(i).getText());
                            System.out.println("NumFluent: Variable " + v + " not involved in the action model");
                            System.exit(-1);
                        }
                    }
                }
                return ret;
            }
            case PddlParser.UNARY_MINUS:
                return new MinusUnary(createExpression(t.getChild(0), parTable));
            case PddlParser.MULTI_OP: {
                MultiOp ret = new MultiOp(t.getChild(0).getText());
                for (int i = 1; i < t.getChildCount(); i++) {
                    //System.out.println("Figlio di + o * " + createExpression(t.getChild(i)));
                    ret.addExpression(createExpression(t.getChild(i), parTable));
                }
                return ret;
            }
            default:
                break;
        }

        return null;

    }

    /**
     * @return the pddlFilRef
     */
    public String getPddlFilRef() {
        return pddlReferenceFile;
    }

    /**
     * @param pddlFilRef the pddlFilRef to set
     */
    public void setPddlFilRef(String pddlFilRef) {
        this.pddlReferenceFile = pddlFilRef;
    }

    private void addConstants(Tree c) {

        for (int i = 0; i < c.getChildCount(); i++) {
            this.getConstants().add(new PDDLObject(c.getChild(i).getText(), new Type(c.getChild(i).getChild(0).getText())));
//            System.out.println("Aggiungo l'oggetto:" + c.getChild(i).getText());
//            System.out.println("che è di tipo:" + new Type(c.getChild(i).getChild(0).getText()));
        }

    }

    /**
     * @return the constants
     */
    public PDDLObjects getConstants() {
        return constants;
    }

    /**
     * @param constants the constants to set
     */
    public void setConstants(PDDLObjects constants) {
        this.constants = constants;
    }

    public void saveDomain(String file) throws IOException {
        PddlDomain domain = this;
        Writer f;

        f = new BufferedWriter(new FileWriter(file));
        ParametersAsTerms constants = new ParametersAsTerms();
        String actions = "\n";

        f.write("(define (domain " + domain.getName() + ")\n");
        if (domain.getRequirements() != null) {
            f.write("(:requirements " + Utils.toPDDLSet(domain.getRequirements()) + ")\n");
        }
        if (domain.getTypes() != null) {
            f.write("(:types " + Utils.toPDDLTypesSet(domain.getTypes()) + ")\n");
        }
        if (!domain.getPredicates().isEmpty()) // f.write("(:constants "+constants.pddlPrint()+")\n");
        {
            f.write("(:predicates " + domain.getPredicates().pddlPrint(true) + "\n");
        }
        if (!domain.getFunctions().isEmpty()) {
            f.write("(:functions " + Utils.toPDDLSet(domain.getFunctions()) + ")\n");
        }

        if (!domain.getActionsSchema().isEmpty()) {
            f.write(Utils.toPDDLSetWithBreak(domain.getActionsSchema()));
        }

        f.write(actions);
        f.write("\n)");
        f.close();
        f.close();
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public HashMap generateAbstractInvariantFluents() {
        if (getAbstractInvariantFluents() != null) {
            return getAbstractInvariantFluents();
        }
        abstractInvariantFluents = new HashMap();
        for (final ActionSchema as : this.ActionsSchema) {
            Set s = as.getAbstractNumericFluentAffected();
            for (NumFluent nf : (Set<NumFluent>) s) {
                abstractInvariantFluents.put(nf.getName(), false);
            }
        }
        for (NumFluent nf : (Collection<NumFluent>) this.get_derived_variables()) {
            abstractInvariantFluents.put(nf.getName(), false);
        }

        return abstractInvariantFluents;
    }

    public Type getTypeByName(String text) {
        for (Type t : (List<Type>) this.getTypes()) {
            if (t.getName().equals(text)) {
                return t;
            }
        }
        return null;
    }

    public HashMap<Object, Boolean> generateInvariant() {
        HashMap<Object, Boolean> ret = new HashMap();
        for (ActionSchema as : this.getActionsSchema()) {
            Condition prop_effects = as.getAddList();
            if (prop_effects instanceof AndCond) {
                AndCond ac = (AndCond) prop_effects;
                for (Object o : ac.sons) {
                    if (o instanceof Predicate) {
                        Predicate p = (Predicate) o;
                        Predicate pDef = this.getPredicates().findAssociated(p);
                        ret.put(pDef, Boolean.FALSE);
                    }
                }
            } else {
                System.out.println("Support only and cond as prop effects. In case of singleton, please put it under AND");
            }
            prop_effects = as.getDelList();
            if (prop_effects != null) {
                if (prop_effects instanceof AndCond) {
                    AndCond ac = (AndCond) prop_effects;
                    for (Object o : ac.sons) {
                        if (o instanceof NotCond) {
                            NotCond nc = (NotCond) o;
                            //System.out.println(nc);
//                            for (Object o1 : nc.son) {
                            Predicate p = (Predicate) nc.getSon();
                            Predicate pDef = this.getPredicates().findAssociated(p);
                            ret.put(pDef, Boolean.FALSE);
//                            }

                        }
                    }
                } else {
                    System.out.println("Support only AND as prop effects. In case of singleton, please put it under AND also if it is just one proposition");
                }
            }
            Set<NumFluent> anfa = (Set<NumFluent>) as.getAbstractNumericFluentAffected();
            for (NumFluent nf : anfa) {
                ret.put(nf, Boolean.FALSE);
            }
        }
        return ret;
    }

    private void addFree_Functions(Tree c) {
        if (c != null) {
            for (int i = 0; i < c.getChildCount(); i++) {
                //System.out.println(c.getChild(i).getText());
                NumFluent ret = new NumFluent(c.getChild(i).getText());
                Tree t = c.getChild(i);
                for (int j = 0; j < t.getChildCount(); j++) {
                    Variable v = new Variable(t.getChild(j).getText());
                    if (t.getChild(j).getChild(0) != null);
                    //System.out.println(t.getChild(j));

                    //System.out.println(t.getChild(j).getChild(0));
                    v.setType(new Type(t.getChild(j).getChild(0).getText()));

                    ret.addVariable(v);
                }
                this.get_derived_variables().add(ret);
            }
        }
    }

    /**
     * @return the free_functions
     */
    public List<NumFluent> get_derived_variables() {
        return derived_variables;
    }

    /**
     * @param free_functions the free_functions to set
     */
    public void setFree_functions(List free_functions) {
        this.derived_variables = free_functions;
    }

    private void addGlobalConstraints(Tree c) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    /**
     * @return the abstractInvariantFluents
     */
    public HashMap getAbstractInvariantFluents() {
        return abstractInvariantFluents;
    }

    /**
     * @param abstractInvariantFluents the abstractInvariantFluents to set
     */
    public void setAbstractInvariantFluents(HashMap abstractInvariantFluents) {
        this.abstractInvariantFluents = abstractInvariantFluents;
    }

    private void addGlobal_constraint(Tree c) {
        SchemaGlobalConstraint con = new SchemaGlobalConstraint(c.getChild(0).getText());
        //System.out.println("Adding:"+a.getName());
        this.getSchemaGlobalConstraints().add(con);

        for (int i = 1; i < c.getChildCount(); i++) {
            Tree infoConstraint = (Tree) c.getChild(i);
            int type = infoConstraint.getType();

            switch (type) {
                case (PddlParser.PRECONDITION):
                    
                    Condition condition = fc.createCondition(infoConstraint.getChild(0), con.parameters);
                    if ((condition instanceof Comparison) || (condition instanceof Predicate)) {
                        AndCond and = new AndCond();
                        and.addConditions(condition);
                        con.condition = condition;
                    } else {
                        con.condition = condition;;
                    }
                    break;
                case (PddlParser.VARIABLE):
                    if (infoConstraint.getChild(0) == null) {
                        break;
                    }
                    Type t = new Type(infoConstraint.getChild(0).getText());
                    boolean found = false;
                    for (Object o : this.getTypes()) {
                        if (t.equals(o)) {
                            t = (Type) o;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Type: " + t + " is not specified. Please revise the model");
                        System.exit(-1);
                    } else {
                        con.parameters.add(new Variable(infoConstraint.getText(), t));
                    }
                    break;

            }

        }
    }

    /**
     * @return the SchemaGlobalConstraints
     */
    public LinkedHashSet<SchemaGlobalConstraint> getSchemaGlobalConstraints() {
        return SchemaGlobalConstraints;
    }

    /**
     * @param SchemaGlobalConstraints the SchemaGlobalConstraints to set
     */
    public void setSchemaGlobalConstraints(LinkedHashSet SchemaGlobalConstraints) {
        this.SchemaGlobalConstraints = SchemaGlobalConstraints;
    }

    private void addProcess(Tree c) {
        ProcessSchema a = new ProcessSchema();
        Tree process = (Tree) c.getChild(0);
        a.setName(process.getText());
//        System.out.println("DEBUG: Adding:"+a.getName());
        this.ProcessesSchema.add(a);

        for (int i = 1; i < c.getChildCount(); i++) {
            Tree infoAction = (Tree) c.getChild(i);
            int type = infoAction.getType();

            switch (type) {
                case (PddlParser.PRECONDITION):
                    Condition con = fc.createCondition(infoAction.getChild(0), a.getParameters());
                    if ((con instanceof Comparison) || (con instanceof Predicate)) {
                        AndCond and = new AndCond();
                        and.addConditions(con);
                        a.setPreconditions(and);
                    } else if (con != null) {
                        a.setPreconditions(con);
                    }
                    break;
                case (PddlParser.VARIABLE):
                    if (infoAction.getChild(0) == null) {
                        break;
                    }
                    Type t = new Type(infoAction.getChild(0).getText());
                    boolean found = false;
                    for (Object o : this.getTypes()) {
                        if (t.equals(o)) {
                            t = (Type) o;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Type: " + t + " is not specified. Please Fix the Model");
                        System.exit(-1);
                    } else {
                        a.addParameter(new Variable(infoAction.getText(), t));
                    }
                    break;
                case (PddlParser.EFFECT):
                    add_effects(a, infoAction);
//                    System.out.println(a);
                    break;
            }

        }
    }

    /**
     * @return the ProcessesSchema
     */
    public Set<ProcessSchema> getProcessesSchema() {
        return ProcessesSchema;
    }

    /**
     * @param ProcessesSchema the ProcessesSchema to set
     */
    public void setProcessesSchema(Set ProcessesSchema) {
        this.ProcessesSchema = ProcessesSchema;
    }

//    private Object createConditionalEffect(SchemaParameters parameters, Tree t) {
//        
//        if (t.getType() == PddlParser.AND_EFFECT){
//            for (int j = 0; j < t.getChildCount(); j++) {
//                    if (t.getChild(j).getType() == PddlParser.WHEN_EFFECT){
//                        Tree child = t.getChild(j);
//                        System.out.println(t.getChild(j).toStringTree());
//                    }
//                }
//        }
//        return null;
//    }
    private void add_effects(GenericActionType a, Tree infoAction) {
        PostCondition res = createPostCondition(a.parameters, infoAction.getChild(0));
        a.create_effects_by_cases(res);
    }

    

    public void saveDomainWithInterpretationObjects(String file) throws IOException {
        PddlDomain domain = this;
        Writer f;

        f = new BufferedWriter(new FileWriter(file));
        ParametersAsTerms constants = new ParametersAsTerms();

        f.write("(define (domain " + domain.getName() + ")\n");
        if (domain.getRequirements() != null && !domain.getRequirements().isEmpty()) {
            f.write("(:requirements " + Utils.toPDDLSet(domain.getRequirements()) + ")\n");
        }
        if (domain.getTypes() != null && !domain.getTypes().isEmpty()) {
            f.write("(:types interpretation " + Utils.toPDDLTypesSet(domain.getTypes()) + ")\n");
        }
        if (!domain.getPredicates().isEmpty()) // f.write("(:constants "+constants.pddlPrint()+")\n");
        {
            f.write("(:predicates (true) " + domain.getPredicates().pddlPrintWithExtraObject(true) + " \n");
        }

        if (!domain.getActionsSchema().isEmpty()) {
            f.write(Utils.toPDDLWithExtraObject(domain.getActionsSchema()));
        }

        f.write("\n)");
        f.close();
        f.close();
    }

    private void push_not_at_the_terminals() {
        for (ActionSchema a : this.ActionsSchema) {
            a.push_not_to_terminals();
        }

        for (ProcessSchema a : this.ProcessesSchema) {
            a.push_not_to_terminals();
        }
        for (EventSchema a : this.eventsSchema) {
            a.push_not_to_terminals();
        }

    }

    private void addEvent(Tree c) {
        this.addGenericActionSchemas(c, new EventSchema());
    }

    private void addGenericActionSchemas(Tree c, ActionSchema a) {

        Tree action = (Tree) c.getChild(0);
        a.setName(action.getText());
        //System.out.println("Adding:"+a.getName());
        if (a instanceof EventSchema) {
            this.getEventSchema().add((EventSchema) a);

        } else if (a instanceof ActionSchema) {
            this.ActionsSchema.add(a);
        }
        for (int i = 1; i < c.getChildCount(); i++) {
            Tree infoAction = (Tree) c.getChild(i);
            int type = infoAction.getType();

            switch (type) {
                case (PddlParser.PRECONDITION):
                    
                    Condition con = fc.createCondition(infoAction.getChild(0), a.parameters);
                    if ((con instanceof Comparison) || (con instanceof Predicate) || (con instanceof ForAll)) {
                        AndCond and = new AndCond();
                        and.addConditions(con);
                        a.setPreconditions(and);
                    } else if (con != null) {
                        a.setPreconditions(con);
                    }
                    break;
                case (PddlParser.VARIABLE):
                    if (infoAction.getChild(0) == null) {
                        break;
                    }
                    Type t = new Type(infoAction.getChild(0).getText());
                    boolean found = false;
                    for (Object o : this.getTypes()) {
                        if (t.equals(o)) {
                            t = (Type) o;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Type: " + t + " is not specified. Please Fix the Model");
                        System.exit(-1);
                    } else {
                        a.addParameter(new Variable(infoAction.getText(), t));
                    }
                    break;
                case (PddlParser.EFFECT):
                    add_effects(a, infoAction);
//                    System.out.println(a);
                    break;
            }

        }
    }

    /**
     * @return the EventSchema
     */
    public Set<EventSchema> getEventSchema() {
        return eventsSchema;
    }

    /**
     * @param EventSchema the EventSchema to set
     */
    public void setEventSchema(Set<EventSchema> EventSchema) {
        this.eventsSchema = EventSchema;
    }

    public boolean can_be_abstract_dominant_constraints() {
        Set<Condition> set = new HashSet();
        for (ActionSchema as : this.ActionsSchema) {
            set.addAll(as.getPreconditions().getTerminalConditions());
        }

        for (int i = 0; i < set.toArray().length; i++) {
            for (int j = i + 1; j < set.toArray().length; j++) {
                Condition c1 = (Condition) set.toArray()[i];
                Condition c2 = (Condition) set.toArray()[j];
                if ((c1 instanceof Comparison) && (c2 instanceof Comparison)) {
                    Comparison comp_c1 = (Comparison) c1;
                    Comparison comp_c2 = (Comparison) c2;
                    if (comp_c1.getInvolvedFluents().equals(comp_c2.getInvolvedFluents())) {
                        //System.out.println(comp_c1+" "+comp_c2);
                        return true;
                    }
                }
            }

        }
        return false;
    }

    public ForAll createForAll(Tree infoAction, SchemaParameters parTable) {
        ForAll forall = new ForAll();
        for (int i = 0; i < infoAction.getChildCount(); i++) {
            Tree child = infoAction.getChild(i);
            switch (child.getType()) {
                case PddlParser.VARIABLE:
                    if (child.getChild(0) == null) {
                        break;
                    }
                    Type t = new Type(child.getChild(0).getText());
                    boolean found = false;
                    for (Object o : this.getTypes()) {
                        if (t.equals(o)) {
                            t = (Type) o;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Type: " + t + " is not specified. Please Fix the Model");
                        System.exit(-1);
                    } else {
                        forall.addParameter(new Variable(child.getText(), t));
                    }
                    break;
                default:
                    //at this point I should have collected all the parameters for grounding
                    //the variable into constants
                    SchemaParameters aug_par_table = new SchemaParameters();
                    aug_par_table.addAll(parTable);
                    aug_par_table.addAll(forall.getParameters());
                    Condition ret_val = fc.createCondition(child, aug_par_table);
                    //System.out.println("ret_val for forall condition is:"+ret_val);
                    if (ret_val != null) {
                        forall.addConditions(ret_val);
                    } else {
                        System.out.println("Something fishy here.." + child);
                        System.exit(-1);
                    }
                    break;

            }

        }
        return forall;

    }

    private ForAll createForAllEffect(Tree infoAction, SchemaParameters parTable) {
        ForAll forall = new ForAll();
        for (int i = 0; i < infoAction.getChildCount(); i++) {
            Tree child = infoAction.getChild(i);
            switch (child.getType()) {
                case PddlParser.VARIABLE:
                    if (child.getChild(0) == null) {
                        break;
                    }
                    Type t = new Type(child.getChild(0).getText());
                    boolean found = false;
                    for (Object o : this.getTypes()) {
                        if (t.equals(o)) {
                            t = (Type) o;
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        System.out.println("Type: " + t + " is not specified. Please Fix the Model");
                        System.exit(-1);
                    } else {
                        forall.addParameter(new Variable(child.getText(), t));
                    }
                    break;
                default:
                    //at this point I should have collected all the parameters for grounding
                    //the variable into constants
                    SchemaParameters aug_par_table = new SchemaParameters();
                    aug_par_table.addAll(parTable);
                    aug_par_table.addAll(forall.getParameters());
                    PostCondition ret_val = createPostCondition(aug_par_table,child);
                    //System.out.println("ret_val for forall condition is:"+ret_val);
                    if (ret_val != null) {
                        forall.sons.add(ret_val);
                    } else {
                        System.out.println("Something fishy here.." + child);
                        System.exit(-1);
                    }
                    break;

            }

        }
        return forall;
    }

}
