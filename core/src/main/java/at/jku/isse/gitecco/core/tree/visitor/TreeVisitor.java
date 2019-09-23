package at.jku.isse.gitecco.core.tree.visitor;

import at.jku.isse.gitecco.core.tree.nodes.*;

public interface TreeVisitor {
    void visit(RootNode n);

    void visit(BinaryFileNode n);

    void visit(SourceFileNode n);

    void visit(ConditionBlockNode n);

    void visit(IFCondition c);

    void visit(IFDEFCondition c);

    void visit(IFNDEFCondition c);

    void visit(ELSECondition c);

    void visit(ELIFCondition c);

    void visit(Define d);

    void visit(Undef d);

    void visit(IncludeNode n);

    void visit(BaseNode n);
}
