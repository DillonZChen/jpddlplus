/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package conditions;

import expressions.Expression;
import java.util.Map;
import problem.State;
import expressions.Number;


/**
 *
 * @author enrico
 */
public class Comparator extends Conditions{
    private String bin_comp;
    private Expression one;
    private Expression two;


    public Comparator(String bin_comp_){
        super();
        bin_comp = bin_comp_;
    }
    public String toString(){
    
        return "(" +getBin_comp() +" "+ getFirst() + " " + getTwo() +")";
    
    
    }

    /**
     * @return the bin_comp
     */
    public String getBin_comp() {
        return bin_comp;
    }

    /**
     * @param bin_comp the bin_comp to set
     */
    public void setBin_comp(String bin_comp) {
        this.bin_comp = bin_comp;
    }

    /**
     * @return the one
     */
    public Expression getFirst() {
        return one;
    }

    /**
     * @param one the one to set
     */
    public void setFirst(Expression one) {
        this.one = one;
    }

    /**
     * @return the two
     */
    public Expression getTwo() {
        return two;
    }

    /**
     * @param two the two to set
     */
    public void setTwo(Expression two) {
        this.two = two;
    }

    @Override
    public Conditions ground(Map substitution) {
        Comparator ret = new Comparator(bin_comp);
        
       ret.one = one.ground(substitution);
       ret.two = two.ground(substitution);
       ret.grounded=true;
       return ret;
    }

    @Override
    public boolean eval(State s) {
        Number first = one.eval(s);
        Number second = two.eval(s);

        if (this.getBin_comp().equals("<")){
            return first.getNumber() < second.getNumber();
        }else if (this.getBin_comp().equals("<=")){
            return first.getNumber() <= second.getNumber();
        }else if (this.getBin_comp().equals(">")){
            return first.getNumber() > second.getNumber();
        }else if (this.getBin_comp().equals(">=")){
            return first.getNumber() >= second.getNumber();
        }else if (this.getBin_comp().equals("=")){
            return first.getNumber() == second.getNumber();
        }else{
            System.out.println(this.getBin_comp() + "  does not supported");
        }

        return false;
    }
}
