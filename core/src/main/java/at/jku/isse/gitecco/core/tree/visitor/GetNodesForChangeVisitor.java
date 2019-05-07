package at.jku.isse.gitecco.core.tree.visitor;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.tree.nodes.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class GetNodesForChangeVisitor implements TreeVisitor{
    private Change change;
    private final Set<ConditionalNode> changedNodes;

    public GetNodesForChangeVisitor(Change change) {
        this.change = change;
        this.changedNodes = new HashSet<>();
    }

    public GetNodesForChangeVisitor() {
        this.changedNodes = new HashSet<>();
    }

    public void setChange(Change c) {
        this.change = c;
        this.changedNodes.clear();
    }

    public Collection<ConditionalNode> getchangedNodes() {
        return Collections.unmodifiableSet(this.changedNodes);
    }

    @Override
    public void visit(RootNode n) {

    }

    @Override
    public void visit(BinaryFileNode n) {

    }

    @Override
    public void visit(SourceFileNode n) {

    }

    @Override
    public void visit(ConditionBlockNode n) {

    }

    @Override
    public void visit(IFCondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.changedNodes.add(c);
            //this is necessary to mark newly added features as changed.
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(IFDEFCondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.changedNodes.add(c);
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(IFNDEFCondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.changedNodes.add(c);
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(ELSECondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            this.changedNodes.add(c);
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(Define d) {

    }

    @Override
    public void visit(Undef d) {

    }
}
