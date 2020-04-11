package at.jku.isse.gitecco.translation.visitor;

import at.jku.isse.gitecco.core.git.Change;
import at.jku.isse.gitecco.core.tree.nodes.*;
import at.jku.isse.gitecco.core.tree.visitor.TreeVisitor;
import org.glassfish.grizzly.utils.ArraySet;

import java.util.*;

public class GetNodesForChangeVisitor implements TreeVisitor {
    private Change change;
    private final ArrayList<ConditionalNode> changedNodes;

    public GetNodesForChangeVisitor(Change change) {
        this.change = change;
        this.changedNodes = new ArrayList<>();
    }

    public GetNodesForChangeVisitor() {
        this.changedNodes = new ArrayList<>();
    }

    public void setChange(Change c) {
        this.change = c;
        this.changedNodes.clear();
    }

    public Collection<ConditionalNode> getchangedNodes() {
        return this.changedNodes;
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
            int lines=0;
            int i= change.getLines().get(0);

                while (i <= change.getLines().get(1) && i <= c.getLineTo()) {
                    if (i >= c.getLineFrom() && i <= c.getLineTo()) {
                        lines++;
                    }
                    i++;
                }
            if(change.getChangeType().equals("INSERT")) {
                c.addLinesInserted(change.getLines().get(0));
                c.addLinesInserted(change.getLines().get(1));
                c.addLinesInserted(lines);
            }else{
                c.addLinesDeleted(change.getLines().get(0));
                c.addLinesDeleted(change.getLines().get(1));
                c.addLinesDeleted(lines);
            }
            this.changedNodes.add(c);
            //this is necessary to mark newly added features as changed.
            if(!change.contains(c)) change = null;
            //change is set to null so that no further nodes will be interpreted as changed.
        }
    }

    @Override
    public void visit(IFDEFCondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            int lines=0;
            int i= change.getLines().get(0);
            while(i<=change.getLines().get(1) && i <= c.getLineTo()){
                if(i>=c.getLineFrom() && i<=c.getLineTo()){
                    lines++;
                }
                i++;
            }
            if(change.getChangeType().equals("INSERT")) {
                c.addLinesInserted(change.getLines().get(0));
                c.addLinesInserted(change.getLines().get(1));
                c.addLinesInserted(lines);
            }else{
                c.addLinesDeleted(change.getLines().get(0));
                c.addLinesDeleted(change.getLines().get(1));
                c.addLinesDeleted(lines);
            }
            this.changedNodes.add(c);
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(IFNDEFCondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            int lines=0;
            int i= change.getLines().get(0);
            while(i<=change.getLines().get(1) && i <= c.getLineTo()){
                if(i>=c.getLineFrom() && i<=c.getLineTo()){
                    lines++;
                }
                i++;
            }
            if(change.getChangeType().equals("INSERT")) {
                c.addLinesInserted(change.getLines().get(0));
                c.addLinesInserted(change.getLines().get(1));
                c.addLinesInserted(lines);
            }else{
                c.addLinesDeleted(change.getLines().get(0));
                c.addLinesDeleted(change.getLines().get(1));
                c.addLinesDeleted(lines);
            }
            this.changedNodes.add(c);
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(ELSECondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            int lines=0;
            int i= change.getLines().get(0);
            while(i<=change.getLines().get(1) && i <= c.getLineTo()){
                if(i>=c.getLineFrom() && i<=c.getLineTo()){
                    lines++;
                }
                i++;
            }
            if(change.getChangeType().equals("INSERT")) {
                c.addLinesInserted(change.getLines().get(0));
                c.addLinesInserted(change.getLines().get(1));
                c.addLinesInserted(lines);
            }else{
                c.addLinesDeleted(change.getLines().get(0));
                c.addLinesDeleted(change.getLines().get(1));
                c.addLinesDeleted(lines);
            }
            this.changedNodes.add(c);
            if(!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(ELIFCondition c) {
        if(change != null && (c.containsChange(change) || change.contains(c))) {
            int lines=0;
            int i= change.getLines().get(0);
            while(i<=change.getLines().get(1) && i <= c.getLineTo()){
                if(i>=c.getLineFrom() && i<=c.getLineTo()){
                    lines++;
                }
                i++;
            }
            if(change.getChangeType().equals("INSERT")) {
                c.addLinesInserted(change.getLines().get(0));
                c.addLinesInserted(change.getLines().get(1));
                c.addLinesInserted(lines);
            }else{
                c.addLinesDeleted(change.getLines().get(0));
                c.addLinesDeleted(change.getLines().get(1));
                c.addLinesDeleted(lines);
            }
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

    @Override
    public void visit(IncludeNode n) {

    }

    @Override
    public void visit(BaseNode n) {
        if(change != null && (n.containsChange(change) || change.contains(n))) {
            int lines=0;
            int i= change.getLines().get(0);
            while(i<=change.getLines().get(1) && i <= n.getLineTo()){
                if(i>=n.getLineFrom() && i<=n.getLineTo()){
                    lines++;
                }
                i++;
            }
            if(change.getChangeType().equals("INSERT")) {
                n.addLinesInserted(change.getLines().get(0));
                n.addLinesInserted(change.getLines().get(1));
                n.addLinesInserted(lines);
            }else{
                n.addLinesDeleted(change.getLines().get(0));
                n.addLinesDeleted(change.getLines().get(1));
                n.addLinesDeleted(lines);
            }
            this.changedNodes.add(n);
            //this is necessary to mark newly added features as changed.
            if(!change.contains(n)) change = null;
            //change is set to null so that no further nodes will be interpreted as changed.
        }
    }
}
