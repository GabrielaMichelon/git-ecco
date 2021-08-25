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
    public void visit(RootNode n, String feature) {

    }

    @Override
    public void visit(BinaryFileNode n, String feature) {

    }

    @Override
    public void visit(SourceFileNode n, String feature) {

    }

    @Override
    public void visit(ConditionBlockNode n, String feature) {

    }

    @Override
    public void visit(IFCondition c, String feature) {
        if (change != null && (c.containsChange(change) || change.contains(c))) {
            int lines = 0;
            int i = change.getLines().get(0);
            while (i <= change.getLines().get(1) && i <= c.getLineTo()) {
                if (i >= c.getLineFrom() && i <= c.getLineTo()) {
                    lines++;
                }
                i++;
            }
            int position = -1;
            if (change.getChangeType().equals("INSERT")) {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position, -1, -1, -1, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(lines);
            } else if (change.getChangeType().equals("CHANGE")) {
                int diff = change.getLines().get(1) - change.getLines().get(0);
                if (diff == 0)
                    diff = 1;
                int diff2 = change.getLines().get(3) - change.getLines().get(2);
                if (diff2 == 0)
                    diff2 = 1;
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, diff, -1, change.getLines().get(2)+1, change.getLines().get(3)+1, diff2, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(diff);
                c.addLineNumberDeleted(change.getLines().get(2));
                c.addLineNumberDeleted(change.getLines().get(3));
                c.addLineNumberDeleted(diff2);
            } else {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(-1, -1, -1, -1, change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position);
                c.addDeltas(deltas);
                c.addLineNumberDeleted(change.getLines().get(0));
                c.addLineNumberDeleted(change.getLines().get(1));
                c.addLineNumberDeleted(lines);
            }
            this.changedNodes.add(c);
            //this is necessary to mark newly added features as changed.
            if (!change.contains(c)) change = null;
            //change is set to null so that no further nodes will be interpreted as changed.
        }
    }

    @Override
    public void visit(IFDEFCondition c, String feature) {
        if (change != null && (c.containsChange(change) || change.contains(c))) {
            int lines = 0;
            int i = change.getLines().get(0);
            while (i <= change.getLines().get(1) && i <= c.getLineTo()) {
                if (i >= c.getLineFrom() && i <= c.getLineTo()) {
                    lines++;
                }
                i++;
            }
            int position = -1;
            if (change.getChangeType().equals("INSERT")) {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position, -1, -1, -1, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(lines);
            } else if (change.getChangeType().equals("CHANGE")) {
                int diff = change.getLines().get(1) - change.getLines().get(0);
                if (diff == 0)
                    diff = 1;
                int diff2 = change.getLines().get(3) - change.getLines().get(2);
                if (diff2 == 0)
                    diff2 = 1;
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, diff, -1, change.getLines().get(2)+1, change.getLines().get(3)+1, diff2, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(diff);
                c.addLineNumberDeleted(change.getLines().get(2));
                c.addLineNumberDeleted(change.getLines().get(3));
                c.addLineNumberDeleted(diff2);
            } else {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(-1, -1, -1, -1, change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position);
                c.addDeltas(deltas);
                c.addLineNumberDeleted(change.getLines().get(0));
                c.addLineNumberDeleted(change.getLines().get(1));
                c.addLineNumberDeleted(lines);
            }
            this.changedNodes.add(c);
            if (!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(IFNDEFCondition c, String feature) {
        if (change != null && (c.containsChange(change) || change.contains(c))) {
            int lines = 0;
            int i = change.getLines().get(0);
            while (i <= change.getLines().get(1) && i <= c.getLineTo()) {
                if (i >= c.getLineFrom() && i <= c.getLineTo()) {
                    lines++;
                }
                i++;
            }
            int position = -1;
            if (change.getChangeType().equals("INSERT")) {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position, -1, -1, -1, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(lines);
            } else if (change.getChangeType().equals("CHANGE")) {
                int diff = change.getLines().get(1) - change.getLines().get(0);
                if (diff == 0)
                    diff = 1;
                int diff2 = change.getLines().get(3) - change.getLines().get(2);
                if (diff2 == 0)
                    diff2 = 1;
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, diff, -1, change.getLines().get(2)+1, change.getLines().get(3)+1, diff2, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(diff);
                c.addLineNumberDeleted(change.getLines().get(2));
                c.addLineNumberDeleted(change.getLines().get(3));
                c.addLineNumberDeleted(diff2);
            } else {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(-1, -1, -1, -1, change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position);
                c.addDeltas(deltas);
                c.addLineNumberDeleted(change.getLines().get(0));
                c.addLineNumberDeleted(change.getLines().get(1));
                c.addLineNumberDeleted(lines);
            }
            this.changedNodes.add(c);
            if (!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(ELSECondition c, String feature) {
        if (change != null && (c.containsChange(change) || change.contains(c))) {
            int lines = 0;
            int i = change.getLines().get(0);
            while (i <= change.getLines().get(1) && i <= c.getLineTo()) {
                if (i >= c.getLineFrom() && i <= c.getLineTo()) {
                    lines++;
                }
                i++;
            }
            int position = -1;
            if (change.getChangeType().equals("INSERT")) {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position, -1, -1, -1, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(lines);
            } else if (change.getChangeType().equals("CHANGE")) {
                int diff = change.getLines().get(1) - change.getLines().get(0);
                if (diff == 0)
                    diff = 1;
                int diff2 = change.getLines().get(3) - change.getLines().get(2);
                if (diff2 == 0)
                    diff2 = 1;
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, diff, -1, change.getLines().get(2)+1, change.getLines().get(3)+1, diff2, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(diff);
                c.addLineNumberDeleted(change.getLines().get(2));
                c.addLineNumberDeleted(change.getLines().get(3));
                c.addLineNumberDeleted(diff2);
            } else {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(-1, -1, -1, -1, change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position);
                c.addDeltas(deltas);
                c.addLineNumberDeleted(change.getLines().get(0));
                c.addLineNumberDeleted(change.getLines().get(1));
                c.addLineNumberDeleted(lines);
            }
            this.changedNodes.add(c);
            if (!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(ELIFCondition c, String feature) {
        if (change != null && (c.containsChange(change) || change.contains(c))) {
            int lines = 0;
            int i = change.getLines().get(0);
            while (i <= change.getLines().get(1) && i <= c.getLineTo()) {
                if (i >= c.getLineFrom() && i <= c.getLineTo()) {
                    lines++;
                }
                i++;
            }
            int position = -1;
            if (change.getChangeType().equals("INSERT")) {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position, -1, -1, -1, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(lines);
            } else if (change.getChangeType().equals("CHANGE")) {
                int diff = change.getLines().get(1) - change.getLines().get(0);
                if (diff == 0)
                    diff = 1;
                int diff2 = change.getLines().get(3) - change.getLines().get(2);
                if (diff2 == 0)
                    diff2 = 1;
                Deltas deltas = new Deltas(change.getLines().get(0)+1, change.getLines().get(1)+1, diff, -1, change.getLines().get(2)+1, change.getLines().get(3)+1, diff2, -1);
                c.addDeltas(deltas);
                c.addLineNumberInserts(change.getLines().get(0));
                c.addLineNumberInserts(change.getLines().get(1));
                c.addLineNumberInserts(diff);
                c.addLineNumberDeleted(change.getLines().get(2));
                c.addLineNumberDeleted(change.getLines().get(3));
                c.addLineNumberDeleted(diff2);
            } else {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(-1, -1, -1, -1, change.getLines().get(0)+1, change.getLines().get(1)+1, lines, position);
                c.addDeltas(deltas);
                c.addLineNumberDeleted(change.getLines().get(0));
                c.addLineNumberDeleted(change.getLines().get(1));
                c.addLineNumberDeleted(lines);
            }
            this.changedNodes.add(c);
            if (!change.contains(c)) change = null;
        }
    }

    @Override
    public void visit(Define d, String feature) {

    }

    @Override
    public void visit(Undef d, String feature) {

    }

    @Override
    public void visit(IncludeNode n, String feature) {

    }

    @Override
    public void visit(BaseNode n, String feature) {
        if (change != null && (n.containsChange(change) || change.contains(n))) {
            int lines = 0;
            int i = change.getLines().get(0);
            while (i <= change.getLines().get(1) && i <= n.getLineTo()) {
                if (i >= n.getLineFrom() && i <= n.getLineTo()) {
                    lines++;
                }
                i++;
            }
            int position = -1;
            if (change.getChangeType().equals("INSERT")) {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(change.getLines().get(0), change.getLines().get(1), lines, position, -1, -1, -1, -1);
                n.addDeltas(deltas);
                n.addLineNumberInserts(change.getLines().get(0));
                n.addLineNumberInserts(change.getLines().get(1));
                n.addLineNumberInserts(lines);
            } else if (change.getChangeType().equals("CHANGE")) {
                int diff = change.getLines().get(1) - change.getLines().get(0);
                if (diff == 0)
                    diff = 1;
                int diff2 = change.getLines().get(3) - change.getLines().get(2);
                if (diff2 == 0)
                    diff2 = 1;
                Deltas deltas = new Deltas(change.getLines().get(0), change.getLines().get(1), diff, -1, change.getLines().get(2), change.getLines().get(3), diff2, -1);
                n.addDeltas(deltas);
                n.addLineNumberInserts(change.getLines().get(0));
                n.addLineNumberInserts(change.getLines().get(1));
                n.addLineNumberInserts(change.getLines().get(1) - change.getLines().get(0));
                n.addLineNumberDeleted(change.getLines().get(2));
                n.addLineNumberDeleted(change.getLines().get(3));
                n.addLineNumberDeleted(change.getLines().get(3) - change.getLines().get(2));
            } else {
                if (change.getPosition() != -1)
                    position = change.getPosition();
                Deltas deltas = new Deltas(-1, -1, -1, -1, change.getLines().get(0), change.getLines().get(1), lines, position);
                n.addDeltas(deltas);
                n.addLineNumberDeleted(change.getLines().get(0));
                n.addLineNumberDeleted(change.getLines().get(1));
                n.addLineNumberDeleted(lines);
            }
            this.changedNodes.add(n);
            //this is necessary to mark newly added features as changed.
            if (!change.contains(n)) change = null;
            //change is set to null so that no further nodes will be interpreted as changed.
        }
    }
}
