package at.jku.isse.gitecco.tree.nodes;

import java.util.Objects;

/**
 * Class to represent a #undef preprocessor statement
 */
public class Undef extends DefineNodes {

    public Undef(String name, int lineInfo) {
        super(name, lineInfo);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Undef undef = (Undef) o;
        return Objects.equals(getMacroName(), undef.getMacroName());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getMacroName());
    }
}