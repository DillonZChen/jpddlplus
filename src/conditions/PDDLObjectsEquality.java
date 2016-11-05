/**
 * *******************************************************************
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 ********************************************************************
 */
/**
 * *******************************************************************
 * Description: Part of the PPMaJaL library
 *
 * Author: Enrico Scala 2014 Contact: enricos83@gmail.com
 *
 ********************************************************************
 */
package conditions;

import domain.Variable;
import expressions.NumFluent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import problem.GroundAction;
import problem.RelState;
import problem.State;

/**
 *
 * @author enrico
 */
public class PDDLObjectsEquality extends Conditions {

    private PDDLObject left;
    private PDDLObject right;
    private Variable leftV;
    private Variable rightV;

    public PDDLObjectsEquality() {

        grounded = false;
    }

    public PDDLObjectsEquality(Variable leftV, Variable rightV) {
        this.leftV = leftV;
        this.rightV = rightV;
        grounded = false;
    }

    private PDDLObjectsEquality(PDDLObject t1, PDDLObject t2) {
        left = t1;
        right = t2;
        grounded = true;
    }

    /**
     * @return a grounded copy of the condition
     */
    @Override
    public Conditions ground(Map substitution) {
        PDDLObjectsEquality ret = null;

        PDDLObject t1 = (PDDLObject) substitution.get(getLeftV());
        PDDLObject t2 = (PDDLObject) substitution.get(getRightV());
        if ((t1 == null) || (t2 == null)) {
            System.out.println("Error in substitution  for " + getLeftV() + " or " + getRightV());
            System.exit(-1);
        } else {
            ret = new PDDLObjectsEquality(t1, t2);
        }

        return ret;

    }

    @Override
    public Conditions ground(Map substitution, int c) {
        Conditions ret = this.ground(substitution);
        ret.setCounter(c);
        return ret;
    }

    @Override
    public boolean eval(State s) {
        //it is actually independent from the state s. A state does not change the objects at hand.
        if (!grounded) {
            System.out.println(this + "  condition should be grounded..exit!");
            System.exit(-1);
        }

        return getLeft().equals(getRight());
    }

    @Override
    public boolean isSatisfied(State s) {
        //it is actually independent from the state s. A state does not change the objects at hand.
        if (!grounded) {
            System.out.println(this + "  condition should be grounded..exit!");
            System.exit(-1);
        }

        return getLeft().equals(getRight());
    }

    @Override
    public void changeVar(Map substitution) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

    }

    @Override
    public String pddlPrint(boolean typeInformation) {
        String ret = "";
        if (grounded) {
            ret += "( = " + this.getLeft().pddlPrint(typeInformation) + " " + this.getRight().pddlPrint(typeInformation) + ")";
        } else {
            ret += "( = " + this.getLeftV().pddlPrint(typeInformation) + " " + this.getRightV().pddlPrint(typeInformation) + ")";

        }
        return ret;
    }

    @Override
    public Conditions clone() {
        PDDLObjectsEquality ret = new PDDLObjectsEquality();
        ret.grounded = this.grounded;
        if (grounded) {
            ret.setLeft(this.getLeft());
            ret.setRight(this.getRight());
        } else {
            ret.setLeftV(this.getLeftV());
            ret.setRightV(this.getRightV());
        }
        return ret;
    }

    @Override
    public boolean can_be_true(RelState aThis) {
        //it is actually independent from the state s. A state does not change the objects at hand.
        //actions are not allowed to change objects..
        if (!grounded) {
            System.out.println(this + "  condition should be grounded..exit!");
            System.exit(-1);
        }

        return getLeft().equals(getRight());

    }

    @Override
    public void normalize() {
        return;
    }

    /**
     * @return the left
     */
    public PDDLObject getLeft() {
        return left;
    }

    /**
     * @param left the left to set
     */
    public void setLeft(PDDLObject left) {
        this.left = left;
    }

    /**
     * @return the right
     */
    public PDDLObject getRight() {
        return right;
    }

    /**
     * @param right the right to set
     */
    public void setRight(PDDLObject right) {
        this.right = right;
    }

    /**
     * @return the leftV
     */
    public Variable getLeftV() {
        return leftV;
    }

    /**
     * @param leftV the leftV to set
     */
    public void setLeftV(Variable leftV) {
        this.leftV = leftV;
    }

    /**
     * @return the rightV
     */
    public Variable getRightV() {
        return rightV;
    }

    /**
     * @param rightV the rightV to set
     */
    public void setRightV(Variable rightV) {
        this.rightV = rightV;
    }

    @Override
    public Conditions unGround(Map asbstractionOf) {
        PDDLObjectsEquality ret = null;

        Variable t1 = (Variable) asbstractionOf.get(getLeft().getName());
        Variable t2 = (Variable) asbstractionOf.get(getRight().getName());
        if ((t1 == null) || (t2 == null)) {
            System.out.println("Error in substitution  for " + getLeftV() + " or " + getRightV());
            System.exit(-1);
        } else {
            ret = new PDDLObjectsEquality(t1, t2);
        }
        ret.grounded = false;

        return ret;

    }

    @Override
    public boolean isUngroundVersionOf(Conditions conditions) {
        if (conditions instanceof PDDLObjectsEquality) {
            PDDLObjectsEquality c = (PDDLObjectsEquality) conditions;
            Variable v1 = (Variable) this.getLeftV();
            Variable v2 = (Variable) this.getRightV();
            PDDLObject obj1 = (PDDLObject) c.getLeft();
            PDDLObject obj2 = (PDDLObject) c.getRight();
            //System.out.print("Matching Types between: "+ v.getType() + obj.getType());
            if (!v1.getType().equals(obj1.getType())) {
                if (!v1.getType().isAncestorOf(obj1.getType())) {
                    return false;
                }
            }
            if (!v2.getType().equals(obj2.getType())) {
                if (!v2.getType().isAncestorOf(obj2.getType())) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    @Override
    public String toSmtVariableString(int i) {
        if (this.left.equals(right)) {
            return "true";
        } else {
            return "false";
        }
    }

    @Override
    public Set<NumFluent> getInvolvedFluents() {
        return new HashSet();
    }

    @Override
    public Conditions weakEval(State s, HashMap invF) {
        if (this.left.equals(this.right)) {
            this.setValid(true);
            this.setUnsatisfiable(false);
        } else {
            this.setUnsatisfiable(true);
            this.setValid(false);
        }
        return this;
    }

    @Override
    public String toSmtVariableString(int k, GroundAction gr, String var) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Conditions transform_equality() {
        return this;
    }

    @Override
    public boolean is_affected_by(GroundAction gr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public Conditions regress(GroundAction gr) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String pddlPrintWithExtraObject() {
        String ret = "";
//        if (grounded) {
//            ret += "( = " + this.getLeft().pddlPrintWithExtraObject() + " " + this.getRight().pddlPrintWithExtraObject() + ")";
//        } else {
            ret += "( = " + this.getLeftV().pddlPrint(false) + " " + this.getRightV().pddlPrint(false) + ")";

//        }
        return ret;
    }

    @Override
    public ArrayList<Variable> getInvolvedVariables() {

        ArrayList<Variable> ret = new ArrayList();
        ret.add(this.leftV);
        ret.add(this.rightV);
        return ret;

    }

    @Override
    public boolean can_be_false(RelState aThis) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

}
