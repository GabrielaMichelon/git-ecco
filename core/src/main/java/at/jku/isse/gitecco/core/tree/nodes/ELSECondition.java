package at.jku.isse.gitecco.core.tree.nodes;


import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import at.jku.isse.gitecco.core.tree.visitor.Visitable;

/**
 * Class for representing an ELSECondition.
 * The name is slightly misleading since this has no condition on its own.
 * This feature/cond. depends on the corresponding IF/IFDEF/IFNDEF Condition.
 * It should represent an ELSE clause of an IF ELSE PPStatement.
 */
public final class ELSECondition extends ConditionalNode implements Visitable {

    public ELSECondition(ConditionBlockNode parent) {
        super(parent);
    }

    @Override
    public String getCondition() {
        StringBuilder ret = new StringBuilder();
        ret.append("!(" + getParent().getIfBlock().getCondition() +")");
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            ret.append(" && !(" + elseIfBlock.getLocalCondition() + ")");
        }
        return ret.toString();
       /* String aux = getParent().getIfBlock().getLocalCondition();
        if (aux.contains("defined (")) {
            aux = aux.replace("defined (", "");
            aux = aux.replace(")", "");
        } else if (aux.contains("defined(")) {
            aux = aux.replace("defined(", "");
            aux = aux.replace(")", "");
        } else if (aux.contains("defined")) {
            aux = aux.replace("defined", "");
        }
        aux=aux.replace("(","");
        aux=aux.replace(")","");

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
                features[i] = features[i].replace("(", "");
                features[i] = features[i].replace(")", "");
                if (!features[i].contains("!") && features[i].contains("==1")) {
                    features[i] = "!(" + features[i].replace("==1", ")");
                }else if(!features[i].contains("!")){
                    features[i] = "!(" + features[i]+")";
                } else {
                    features[i] =  "("+features[i].replace("!","") + "==1)";
                }

                if (i < features.length - 1) {
                    newAux += features[i] + " || ";
                } else {
                    newAux += features[i] +"";
                }
            }
            aux = "("+newAux+")";
        } else {

            if (aux.contains(">")) {
                aux = aux.substring(0, aux.indexOf(">"));
            } else if (aux.contains("<")) {
                aux = aux.substring(0, aux.indexOf("<"));
            } else if (aux.contains("==")) {
                aux = aux.substring(0, aux.indexOf("="));
            }

            if (aux.contains("!")) {
                aux = aux.replace("(","");
                aux = aux.replace(")","");
                aux = aux.replace("!", "(") + "==1)";
            } else {
                aux = aux.replace("(","");
                aux = aux.replace(")","");
                if (aux.contains("==1")) {
                    aux = "!(" + aux.replace("==1", ")");
                } else {
                    aux = "!(" + aux + ")";
                }
            }

        }
        ret.append(aux);

        //ret.append("!" + getParent().getIfBlock().getCondition() );
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            aux= elseIfBlock.getLocalCondition().replaceAll("[()]", "");
            if (elseIfBlock.getLocalCondition().contains("!")) {
                //ret.append(" && "+elseIfBlock.getLocalCondition().replace("==0","==1"));
                ret.append(" && (" + aux.replace("!", "") + "==1)");
            } else if (elseIfBlock.getLocalCondition().contains("==1")) {
                ret.append(" && !(" +aux.replace("==1", ""));
                //ret.append(" && "+elseIfBlock.getLocalCondition().replace("==1","==0"));
            } else{
                ret.append(" && !("+aux+")");
            }
            ret.append(")");
            //ret.append("!" + elseIfBlock.getCondition() + " && ");
        }
        return ret.toString();*/
    }

    @Override
    public String getLocalCondition() {
        //return getCondition();
        return "";
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
