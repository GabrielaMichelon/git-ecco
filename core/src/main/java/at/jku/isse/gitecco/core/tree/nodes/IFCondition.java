package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;
import org.chocosolver.solver.variables.BoolVar;

/**
 * Class for representing an IFCondition.
 * Name speaks for itself.
 */
public final class IFCondition extends ConditionalNode implements Visitable {
    private final String condition;

    public IFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public String getCondition() {
        String aux = this.condition;
        if (aux.contains("defined (")) {
            aux = aux.replace("defined (", "");
            aux = aux.replace(")", "");
        } else if (aux.contains("defined(")) {
            aux = aux.replace("defined(", "");
            aux = aux.replace(")", "");
        } else if (aux.contains("defined")) {
            aux = aux.replace("defined", "");
        }


        String[] features;
        if (aux.contains("||")) {
            features = aux.split("\\|\\|");

            String newAux = "";

            for (int i = 0; i < features.length; i++) {
                if (features[i].contains(">")) {
                    features[i] = features[i].substring(0, features[i].indexOf(">"));
                } else if (features[i].contains("<")) {
                    aux = features[i].substring(0, features[i].indexOf("<"));
                } else if (features[i].contains("==")) {
                    features[i] = features[i].substring(0, features[i].indexOf("="));
                }
                if (!features[i].contains("!")) {
                   /* if (features[i].contains("("))
                        features[i] = features[i].replace("!", "") + "==0)";
                    else
                        features[i] = features[i].replace("!", "") + "==0";
                } else {*/
                    if (features[i].contains("("))
                        features[i] = features[i] + "==1)";
                    else
                        features[i] = features[i] + "==1";
                }
                if (i < features.length - 1) {
                    newAux += features[i] + " || ";
                } else {
                    newAux += features[i];
                }
            }
            aux = newAux;
        } else {

            if (aux.contains(">")) {
                aux = aux.substring(0, aux.indexOf(">"));
            } else if (aux.contains("<")) {
                aux = aux.substring(0, aux.indexOf("<"));
            } else if (aux.contains("==")) {
                aux = aux.substring(0, aux.indexOf("="));
            }

            if (!aux.contains("!")) {
                /*if (aux.contains("("))
                    aux = aux.replace("!", "") + "==0)";
                else
                    aux = aux.replace("!", "") + "==0";
            } else {*/
                if (aux.contains("("))
                    aux = aux + "==1)";
                else
                    aux = aux + "==1";
            }


        }
        return aux;
    }

    @Override
    public String getLocalCondition() {
        return this.condition;
    }

    @Override
    public void accept(TreeVisitor v) {
        for (ConditionBlockNode child : getChildren()) {
            child.accept(v);
        }
        for (DefineNode defineNode : getDefineNodes()) {
            defineNode.accept(v);
        }
        for (IncludeNode includeNode : getIncludeNodes()) {
            includeNode.accept(v);
        }
        v.visit(this);
    }

}
