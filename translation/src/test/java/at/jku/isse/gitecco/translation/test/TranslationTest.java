package at.jku.isse.gitecco.translation.test;

import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.constraints.Constraint;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.text.StyledEditorKit;
import java.io.File;

import static org.chocosolver.solver.constraints.nary.cnf.LogOp.*;

public class TranslationTest {

    @Test
    public void testSolver() {
        String condition = "C>5";

        ExpressionSolver es = new ExpressionSolver(condition);

        es.solve().entrySet().forEach(x->System.out.println(x.getKey() + " = " + x.getValue()));
    }

    @Test
    public void failingIfThenChain() {
        Model model = new Model("test1");

        BoolVar y = model.boolVar("y");
        BoolVar x = model.boolVar("x");
        BoolVar b = model.boolVar("b");

        //model.post(LogOp.and(x,b));

        model.addClauses(ifThenElse(y,b,ifThenElse(x,b.not(),b.not())));
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);

        Assert.assertNotNull(s);
    }

    @Test
    public void successfullIfThenChain() {
        Model model = new Model("test2");

        BoolVar x = model.boolVar("x");
        BoolVar y = model.boolVar("y");
        BoolVar b = model.boolVar("b");

        //solution here is comprehensible
        model.post(y.ift(b.not(), x.ift(b,b.not())).intVar().asBoolVar().extension());
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);

        Assert.assertNotNull(s);
    }

    @Test
    public void chocoExperiment2() {
        Model model = new Model("test3");

        BoolVar x = model.boolVar("x");
        BoolVar y = model.boolVar("y");
        IntVar c = model.intVar("c",Short.MIN_VALUE,Short.MAX_VALUE);

        //why does this not have a solution?!?!?
        model.addClauses(ifThenElse(y,c.eq(4).boolVar(),ifThenElse(x,c.eq(9).boolVar(),c.eq(0).boolVar())));
        //model.post(y.ift(c.eq(4), x.ift(c.eq(9),model.boolVar(false))).intVar().asBoolVar().extension());
        model.post(c.gt(7).boolVar().extension());

        model.getSolver().findAllSolutions().forEach(System.out::println);
    }

    @Test
    public void chocoExperiment3() {
        Model model = new Model("test3");

        BoolVar x = model.boolVar("x");
        BoolVar y = model.boolVar("y");
        IntVar c = model.intVar("c",Short.MIN_VALUE,Short.MAX_VALUE);

        //why does this not have a solution?!?!?
        model.addClauses(ifThenElse(y,c.eq(4).boolVar(),implies(x,c.eq(9).boolVar())));
        //model.post(y.ift(c.eq(4), x.ift(c.eq(9),model.boolVar(false))).intVar().asBoolVar().extension());
        model.addClauses(and(c.gt(7).boolVar()));
        //model.addClauses(and(c.add(c).intVar().asBoolVar()));

        model.getSolver().findAllSolutions().forEach(System.out::println);
    }

    @Test
    public void chocoExperimentGabi() {
        Model model = new Model("test3");

        BoolVar a = model.boolVar("A");
        BoolVar b = model.boolVar("B");
        BoolVar c = model.boolVar("C");
        BoolVar y = model.boolVar("Y");
        model.addClauses(ifThenElse(a,b, implies(c.not(),b.not())));
        model.addClauses(implies(b,y));
        System.out.println(model.getSolver().findSolution());
    }


    @Test
    public void pptest() {
        String f2 = "C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2_sub\\clean";
        PreprocessorHelper pph = new PreprocessorHelper();
        String f1 = "C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2";
        File file = new File("C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2");
        File file2 = new File("C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2_sub\\clean");

        pph.generateCleanVersion(file,file2);
    }

    @Test
    public void chocoSolverExperiment() {
        Model model = new Model("test4");
        IntVar c = model.intVar("C", Short.MIN_VALUE, Short.MAX_VALUE);
        //IntVar c = checkVars(model, "C").asIntVar();
        model.post(c.gt(4).boolVar().and(model.boolVar("A")).extension());
        Variable var = checkVars(model, "C");
        model.ifOnlyIf(model.boolVar("X").extension(),model.arithm(var.asIntVar(),"=",6));
        Solution solution = model.getSolver().findSolution();
        System.out.println(solution);
    }

    //helper method also in ExpressionSolver class
    private Variable checkVars(Model model, String name) {
        for (Variable var : model.getVars()) {
            if (var.getName().equals(name)) return var;
        }
        return null;
    }
}
