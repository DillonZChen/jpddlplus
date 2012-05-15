/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package domain;

import conditions.Conditions;

import conditions.Term;
import java.util.HashMap;
import java.util.Map;
import problem.InstatiatedAction;

/**
 *
 * @author enrico
 */
public class ActionSchema extends GenericActionType {
    private ActionParameters parameters;

    public ActionSchema() {
        parameters = new ActionParameters();
  
    }
    
//
//    public Action ground(ArrayList terms){
//        
//        
//        substitution = new HashMap();
//        
//        
//    }
    /**
     * @return the parameters
     */
    public ActionParameters getParameters() {
        return parameters;
    }


    /**
     * @param parameters the parameters to set
     */
    public void setParameters(ActionParameters parameters) {
        this.parameters = parameters;
    }
    public void addParameter(Variable o){
        
        parameters.add(o);
        
    }
    
    public InstatiatedAction ground(Map substitution){
        InstatiatedAction ret = new InstatiatedAction(this.name);
        ActionParametersAsTerms input  = new ActionParametersAsTerms();
        for (Object o: parameters){
            Variable el  = (Variable)o;
            Term t = (Term)substitution.get(el);
            input.add(t);
        }
        ret.setParameters(input);
        
        ret.setNumeric(this.numeric.ground(substitution));
        ret.setAddList(this.addList.ground(substitution));
        ret.setDelList(this.delList.ground(substitution));
        ret.setPreconditions(this.preconditions.ground(substitution));
        
        return ret;
   
    }

    public InstatiatedAction ground(ActionParametersAsTerms par) {
        InstatiatedAction ret = new InstatiatedAction(this.name);
        ActionParametersAsTerms input  = new ActionParametersAsTerms();
        int i=0;
        
        Map substitution = new HashMap();
        for (Object o: parameters){
            Variable el  = (Variable)o;
            substitution.put(el, par.get(i));
            Term t = (Term)substitution.get(el);
            i++;
        }
        ret.setParameters(par);
        
        ret.setNumeric(this.numeric.ground(substitution));
        ret.setAddList(this.addList.ground(substitution));
        if (delList != null)
            ret.setDelList(this.delList.ground(substitution));
        ret.setPreconditions(this.preconditions.ground(substitution));
        
        return ret;
    }

    @Override
    public String toString()  {
        String parametri = "";
        for (Object o : parameters) {
            parametri = parametri.concat(o.toString()).concat(" ");
        }
        return "\n\nAction Name:" + this.name + " Parameters: " + parametri + "\nPre: " + this.preconditions + "\nEffetti positivi: " + this.getAddList() + "\nEffetti negativi: " + this.getDelList() + "\nNumeric Effects:  " + this.getNumeric();
    }



}
