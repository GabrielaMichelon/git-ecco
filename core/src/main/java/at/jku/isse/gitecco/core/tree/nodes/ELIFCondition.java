package at.jku.isse.gitecco.core.tree.nodes;

import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;

public final class ELIFCondition extends ConditionalNode {

    private final String condition;

    public ELIFCondition(ConditionBlockNode parent, String condition) {
        super(parent);
        this.condition = condition;
    }

    @Override
    public String getCondition() {
        StringBuilder ret = new StringBuilder();
        /*ret.append("!(" + getParent().getIfBlock().getCondition() + ")");
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            if(this.equals(elseIfBlock)) {
                break;
            }
            ret.append("!" + elseIfBlock.getCondition() + " && ");
        }
        return ret.toString();
        if(getLocalCondition().contains("!")){
            ret.append(getLocalCondition().replace("!","==0"));
        }else{
            ret.append(getLocalCondition() + "==1");
        }*/

        ret.append(getLocalCondition());


        //appending the ifparent
        String aux=getParent().getIfBlock().getCondition().replaceAll("[()]","");

        String[] features;
        if(aux.contains("||")) {

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
                    features[i] =  "!("+features[i] + ")";
                }else{
                    features[i] = features[i].replace("!","");
                    features[i] = "("+features[i]+"==1";
                }

                if (i < features.length - 1) {
                    newAux += features[i] + " || ";
                } else {
                    newAux += features[i] +"";
                }
            }
            aux = "("+newAux+")";
        }else {

            if (aux.contains(">")) {
                aux = aux.substring(0, aux.indexOf(">"));
            } else if (aux.contains("<")) {
                aux = aux.substring(0, aux.indexOf("<"));
            } else if (aux.contains("==")) {
                aux = aux.substring(0, aux.indexOf("="));
            }

            if (aux.contains("!")) {
                aux = "("+ aux + "==1)";
            }else{
                aux = "!("+aux+")";
            }

        }
        ret.append(" && "+aux);


        /*if(getParent().getIfBlock().getCondition().contains("!")){
            ret.append(" && ("+getParent().getIfBlock().getCondition().replace("!", ""));
            ret.append("==1");
        }else{
            ret.append(" && !("+getParent().getIfBlock().getCondition().replace("==1", ""));
        }
        ret.append(")");
        /*
         */
        //appending elseifblock
        for (ELIFCondition elseIfBlock : getParent().getElseIfBlocks()) {
            if(this.equals(elseIfBlock)) {
                break;
            }
            //String aux =  elseIfBlock.getLocalCondition().replaceAll("[()]","");
            if(elseIfBlock.getLocalCondition().contains("!")){
                ret.append(" && "+elseIfBlock.getLocalCondition().replace("!", ""));
                //ret.append(" && "+elseIfBlock.getLocalCondition().replace("!","==0") );
            }else{
                ret.append(" && !("+elseIfBlock.getLocalCondition().replace("==1", ""));
                ret.append(")");
                //ret.append(" && "+elseIfBlock.getLocalCondition() + "==1");
            }
        }

        return ret.toString();
    }

    @Override
    public String getLocalCondition() {
        String aux = this.condition;
        if(aux.contains("defined (")) {
            aux = aux.replace("defined (", "");
        }else if(aux.contains("defined(")){
            aux = aux.replace("defined(", "");
        } else if(aux.contains("defined")){
            aux = aux.replace("defined", "");
        }
        aux=aux.replaceAll("[()]","");

        String[] features;
        if(aux.contains("||")) {

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
                    /*if (features[i].contains("("))
                        features[i] = features[i].replace("!", "") + "==0)";
                    else
                        features[i] = features[i].replace("!", "") + "==0";
                } else {*/
                    features[i] = features[i].replace("(","");
                    features[i] =  features[i].replace(")","") + "==1";
                }else{
                    features[i] = features[i].replace("(","");
                    features[i] =  features[i].replace(")","");
                }

                if (i < features.length - 1) {
                    newAux += features[i] + " || ";
                } else {
                    newAux += features[i] +"";
                }
            }
            aux = "("+newAux+")";
        }else {

            if (aux.contains(">")) {
                aux = aux.substring(0, aux.indexOf(">"));
            } else if (aux.contains("<")) {
                aux = aux.substring(0, aux.indexOf("<"));
            } else if (aux.contains("==")) {
                aux = aux.substring(0, aux.indexOf("="));
            }

            if (!aux.contains("!")) {/*
                if (aux.contains("("))
                    aux = aux.replace("!", "") + "==0)";
                else
                    aux = aux.replace("!", "") + "==0";
            } else {*/
                aux = aux + "==1";
            }

        }
        return aux;
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
