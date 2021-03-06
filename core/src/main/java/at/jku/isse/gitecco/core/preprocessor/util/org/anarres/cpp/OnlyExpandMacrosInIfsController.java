package at.jku.isse.gitecco.core.preprocessor.util.org.anarres.cpp;

import at.jku.isse.gitecco.core.preprocessor.util.org.anarres.cpp.featureExpr.FeatureExpression;
import at.jku.isse.gitecco.core.preprocessor.util.org.anarres.cpp.featureExpr.FeatureExpressionParser;
import at.jku.isse.gitecco.core.preprocessor.util.org.anarres.cpp.featureExpr.MacroCall;
import at.jku.isse.gitecco.core.preprocessor.util.org.anarres.cpp.featureExpr.PostOrderTraversal;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.List;

public class OnlyExpandMacrosInIfsController extends PreprocessorControlListener {

    public boolean addMacro(Macro m, Source source) {
        return true;
    }

    public boolean removeMacro(Macro m, Source source) {
        return true;
    }

    public boolean expandMacro(Macro m, Source source, int line, int column, boolean isInIf) {
        return isInIf;
    }

    public boolean include(@Nonnull Source source, int line, @Nonnull String name, boolean quoted, boolean next) {
        return true;
    }

    public boolean processIf(List<Token> condition, Source source, IfType type) {
        if (source instanceof FileLexerSource) {
            if (((FileLexerSource) source).getFile().equals(getFileCurrentlyProcessed())) {
                return false;
            }
        }
        return true;
    }

    public String getPartiallyProcessedCondition(List<Token> condition, Source source, IfType type, final Preprocessor pp) {
        if (source instanceof FileLexerSource) {
            if (((FileLexerSource) source).getFile().equals(getFileCurrentlyProcessed())) {
                if (type == IfType.IF || type == IfType.ELSIF) {

                    FeatureExpressionParser parser = new FeatureExpressionParser(condition);

                    final FeatureExpression expr = parser.parse();

                    ExpandTraversal traversal = new ExpandTraversal(expr, pp);
                    expr.traverse(traversal);
                    return traversal.getRoot().toString();
                }
            }
        }
        return null;
    }

    private class ExpandTraversal extends  PostOrderTraversal{

        private FeatureExpression root;

        private final Preprocessor pp;

        public ExpandTraversal(FeatureExpression root, final Preprocessor pp){
            this.root = root;
            this.pp = pp;
        }

        public void postVisit(FeatureExpression visitedExpr) {
            if(visitedExpr instanceof MacroCall){
                try {
                    List<Token> expanded = pp.expand(visitedExpr.toString());
                    FeatureExpressionParser parser = new FeatureExpressionParser(expanded);
                    FeatureExpression expandedExpr = parser.parse();
                    //replace visitedExpr with expandedExpr in expr
                    if(visitedExpr == this.root){
                        root = expandedExpr;
                    } else {
                        visitedExpr.getParent().replace(visitedExpr, expandedExpr);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (LexerException e) {
                    e.printStackTrace();
                }
            }
        }

        public FeatureExpression getRoot() {
            return root;
        }
    }
}