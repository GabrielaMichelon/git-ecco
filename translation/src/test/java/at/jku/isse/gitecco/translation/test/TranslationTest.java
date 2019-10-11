package at.jku.isse.gitecco.translation.test;


import at.jku.isse.gitecco.core.git.GitHelper;
import at.jku.isse.gitecco.core.preprocessor.PreprocessorHelper;
import at.jku.isse.gitecco.core.solver.ExpressionSolver;
import at.jku.isse.gitecco.core.type.Feature;
import at.jku.isse.gitecco.core.type.FeatureImplication;
import at.jku.isse.gitecco.translation.variantscomparison.util.CompareVariants;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import org.anarres.cpp.featureExpr.FeatureExpressionParser;
import org.chocosolver.solver.Model;
import org.chocosolver.solver.Solution;
import org.chocosolver.solver.variables.BoolVar;
import org.chocosolver.solver.variables.IntVar;
import org.chocosolver.solver.variables.Variable;
import org.junit.Assert;
import org.junit.Test;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import static org.chocosolver.solver.constraints.nary.cnf.LogOp.*;

public class TranslationTest {

    //git checkout $(git log --branches -1 --pretty=format:"%H")

    @Test
    public void resetGitRepo() {
        //does not work
        String path = "C:\\obermanndavid\\git-ecco-test\\test_featureid\\Marlin";
        Process p;
        try {
            p = Runtime.getRuntime().exec("git " + path + " checkout $(git "+ path + " log --branches -1 --pretty=format:\"%H\")");
            p.waitFor();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void getCSVInformation() throws IOException {
        File folder = new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite");
        File[] lista = folder.listFiles();
        Float meanRunEccoCommit = Float.valueOf(0), meanRunEccoCheckout = Float.valueOf(0), meanRunPPCheckoutCleanVersion = Float.valueOf(0), meanRunPPCheckoutGenerateVariant = Float.valueOf(0), meanRunGitCommit = Float.valueOf(0), meanRunGitCheckout = Float.valueOf(0);
        Float totalnumberFiles= Float.valueOf(0), matchesFiles= Float.valueOf(0),  eccototalLines= Float.valueOf(0), originaltotalLines= Float.valueOf(0), missingFiles= Float.valueOf(0), totalVariantsMatch= Float.valueOf(0), truepositiveLines = Float.valueOf(0), falsepositiveLines = Float.valueOf(0), falsenegativeLines = Float.valueOf(0);
        Boolean variantMatch = true;
        Float numberCSV= Float.valueOf(0);
        for (File file : lista) {
            if ((file.getName().indexOf(".csv") != -1) && !(file.getName().contains("features_report_each_project_commit"))) {
                System.out.println(file.getName());
                Reader reader = Files.newBufferedReader(Paths.get(file.getAbsolutePath()));
                CSVReader csvReader = new CSVReaderBuilder(reader).build();
                List<String[]> matchesVariants = csvReader.readAll();
                String[] runtimes = matchesVariants.get(1);
                meanRunEccoCommit += Float.valueOf(runtimes[0]);
                meanRunEccoCheckout += Float.valueOf(runtimes[1]);
                meanRunPPCheckoutCleanVersion += Float.valueOf(runtimes[2]);
                meanRunPPCheckoutGenerateVariant += Float.valueOf(runtimes[3]);
                meanRunGitCommit += Float.valueOf(runtimes[4]);
                meanRunGitCheckout += Float.valueOf(runtimes[5]);
                Float qtdLines = Float.valueOf(matchesVariants.size()-4);
                totalnumberFiles += qtdLines;
                for(int i = 4; i < matchesVariants.size(); i++){
                    String[] line = matchesVariants.get(i);
                    truepositiveLines += Integer.valueOf(line[2]);
                    falsepositiveLines +=  Integer.valueOf(line[3]);
                    falsenegativeLines += Integer.valueOf(line[4]);
                    originaltotalLines +=  Integer.valueOf(line[5]);
                    eccototalLines += Integer.valueOf(line[6]);
                    if(line[1].equals("true"))
                        matchesFiles++;
                    else if(line[1].equals("not")){
                        variantMatch = false;
                        missingFiles++;
                    }
                }
            }
            numberCSV++;
            if(variantMatch)
                totalVariantsMatch++;
            variantMatch=true;
        }
        meanRunEccoCommit = (meanRunEccoCommit/numberCSV)/1000;
        meanRunEccoCheckout = (meanRunEccoCheckout/numberCSV)/1000;
        meanRunPPCheckoutCleanVersion = (meanRunPPCheckoutCleanVersion/numberCSV)/1000;
        meanRunPPCheckoutGenerateVariant = (meanRunPPCheckoutGenerateVariant/numberCSV)/1000;
        meanRunGitCommit = (meanRunGitCommit/numberCSV)/1000;
        meanRunGitCheckout = (meanRunGitCheckout/numberCSV)/1000;
        Float totalVariantsNotMatch = (numberCSV-totalVariantsMatch);
        Float precisionVariants = totalVariantsMatch / (totalVariantsMatch-totalVariantsNotMatch);
        Float recallVariants = totalVariantsMatch / (numberCSV);
        Float f1scoreVariants = 2*((precisionVariants*recallVariants)/(precisionVariants+recallVariants));
        Float precisionLines = Float.valueOf(truepositiveLines/ (truepositiveLines+falsepositiveLines));
        Float recallLines = Float.valueOf(truepositiveLines/(truepositiveLines+falsenegativeLines));
        Float f1scorelines = 2*((precisionLines*recallVariants)/(precisionLines+recallLines));
        Float precisionFiles = Float.valueOf(matchesFiles/(matchesFiles-missingFiles));
        Float recallFiles = Float.valueOf(matchesFiles/totalnumberFiles);
        Float f1scoreFiles = 2*((precisionFiles*recallFiles)/(precisionFiles+recallFiles));
        System.out.println("mean runtime ecco checkout"+ String.valueOf(meanRunEccoCheckout)+" matches "+String.valueOf(matchesFiles)+" numberfiles "+String.valueOf(totalnumberFiles));
    }

    @Test
    public void testeCompareVariants() {
        CompareVariants cV = new CompareVariants();
        File variantsrc = new File(String.valueOf("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\ecco\\BASE.1"));
        File checkoutfile = new File(String.valueOf("C:\\Users\\gabil\\Desktop\\ECCO_Work\\variant_result\\checkout\\BASE.1"));
        try {
            cV.compareVariant(variantsrc, checkoutfile);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCheckoutEcco() throws IOException {
        CompareVariants cV = new CompareVariants();
        ArrayList<String> configsToCheckout = new ArrayList<>();
        Path OUTPUT_DIR = Paths.get("C:\\Users\\gabil\\Desktop\\ECCO_Work\\variant_result");
        File eccoFolder = new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\", "ecco");
        File checkoutFolder = new File("C:\\Users\\gabil\\Desktop\\ECCO_Work\\variant_result\\checkout\\");
        configsToCheckout.add("BASE.1");
        cV.eccoCheckout(configsToCheckout, OUTPUT_DIR, eccoFolder, checkoutFolder);
    }

    @Test
    public void JGitCommitAndCheckout() throws IOException {
        GitHelper gh = new GitHelper();
        try {
            gh.gitCommitAndCheckout("C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\sqlite", "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite\\testeCommitJGit", "afa90f0a0069f4fcb54f85d831071f1072e2eb3c");
            System.out.println("time git commit" + gh.getRuntimeGitCommit());
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    @Test
    public void readCSV() throws IOException {
        String outputCSV = "C:\\Users\\gabil\\Desktop\\ECCO_Work\\spls\\spls\\sqllite";
        String fileStr = outputCSV + File.separator + "BASE.1.csv";
        BufferedReader csvReader = null;
        try {
            csvReader = new BufferedReader(new FileReader(fileStr));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        String row = null;
        ArrayList<String> listHeader = new ArrayList<>();
        ArrayList<String> listRuntimeData = new ArrayList<>();
        Boolean header = true;
        while ((row = csvReader.readLine()) != null) {
            String[] data = row.split(",");
            if (header) {
                for (int i = 0; i < data.length; i++) {
                    listHeader.add(data[i]);
                }
                header = false;
            } else {
                for (int i = 0; i < data.length; i++) {
                    if (i == 1) {
                        listRuntimeData.add(String.valueOf(10));
                    } else {
                        listRuntimeData.add(data[i]);
                    }
                }
            }
        }
        csvReader.close();
        File fwriter = new File(fileStr);
        FileWriter csvWriter = new FileWriter(fwriter);
        listRuntimeData.get(1).valueOf(10);
        csvWriter.append(String.join(",", listHeader));
        csvWriter.append("\n");
        csvWriter.append(String.join(",", listRuntimeData));
        csvWriter.flush();
        csvWriter.close();
    }

    @Test
    public void testNumberFormat() {
        String s = "F_CPU >= 16000000L";
        ExpressionSolver es = new ExpressionSolver();
        es.traverse(new FeatureExpressionParser(s).parse());
    }

    @Test
    public void testCorrectedClauseAdding() {
        String condition = "TESTVAR";
        ExpressionSolver es = new ExpressionSolver(condition);

        LinkedList<FeatureImplication> impls = new LinkedList<>();
        impls.add(new FeatureImplication("A", "FILE_H == 0"));
        impls.add(new FeatureImplication("!(FILE_H)", "FILE_H == 1"));
        es.addClause(new Feature("FILE_H"), impls);

        es.solve().entrySet().forEach(x -> System.out.println(x.getKey().getName() + " = " + x.getValue()));
    }

    @Test
    public void asdf() {
        //(BASE) && ((MOTHERBOARD == 62) && (!(PINS_H)) && (BASE)) -> MOTHERBOARD == 1
        ExpressionSolver es = new ExpressionSolver();
        BoolVar ifc = es.getBoolVarFromExpr("(BASE) && ((MOTHERBOARD == 62) && (!(PINS_H)) && (BASE))");
        Model model = es.getModel();

        BoolVar y = model.boolVar("MOTHERBOARD").eq(1).boolVar();

        model.addClauses(ifThenElse(ifc, y, y.not()));
        BoolVar b = model.boolVar("b");
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);
    }

    @Test
    public void testunsatproblem() {
        Model model = new Model("test1");

        BoolVar y = model.boolVar("y");
        BoolVar x = model.boolVar("x");
        BoolVar b = model.boolVar("b");

        model.addClauses(ifThenElse(x.not(), x, x.not()));
        //model.addClauses(implies(x.not(),x));
        model.post(b.extension());

        Solution s = model.getSolver().findSolution();
        System.out.println(s);
    }

    @Test
    public void testSolver() {
        String condition = "C>5";

        ExpressionSolver es = new ExpressionSolver(condition);

        es.solve().entrySet().forEach(x -> System.out.println(x.getKey() + " = " + x.getValue()));
    }

    @Test
    public void testAddClause() {
        String condition = "C>5";
        ExpressionSolver es = new ExpressionSolver(condition);
        Queue<FeatureImplication> impls = new LinkedList<>();
        impls.add(new FeatureImplication("X", "C == 4"));
        impls.add(new FeatureImplication("Y", "C == 8"));
        es.addClause(new Feature("C"), impls);
        es.solve().entrySet().forEach(x -> System.out.println(x.getKey() + " = " + x.getValue()));
    }

    @Test
    public void testNegativeNumber() {
        String condition = "PIDTEMP > -1";
        ExpressionSolver es = new ExpressionSolver(condition);
        es.solve().entrySet().forEach(x -> System.out.println(x.getKey() + " = " + x.getValue()));
    }

    @Test
    public void failingIfThenChain() {
        Model model = new Model("test1");

        BoolVar y = model.boolVar("y");
        BoolVar x = model.boolVar("x");
        BoolVar b = model.boolVar("b");

        //model.post(LogOp.and(x,b));

        model.addClauses(ifThenElse(y, b, ifThenElse(x, b.not(), b.not())));
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
        model.post(y.ift(b.not(), x.ift(b, b.not())).intVar().asBoolVar().extension());
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
        IntVar c = model.intVar("c", Short.MIN_VALUE, Short.MAX_VALUE);

        //why does this not have a solution?!?!?
        model.addClauses(ifThenElse(y, c.eq(4).boolVar(), ifThenElse(x, c.eq(9).boolVar(), c.eq(0).boolVar())));
        //model.post(y.ift(c.eq(4), x.ift(c.eq(9),model.boolVar(false))).intVar().asBoolVar().extension());
        model.post(c.gt(7).boolVar().extension());

        model.getSolver().findAllSolutions().forEach(System.out::println);
    }

    @Test
    public void chocoExperiment3() {
        Model model = new Model("test3");

        BoolVar x = model.boolVar("x");
        BoolVar y = model.boolVar("y");
        IntVar c = model.intVar("c", Short.MIN_VALUE, Short.MAX_VALUE);

        //why does this not have a solution?!?!?
        model.addClauses(ifThenElse(y, c.eq(4).boolVar(), implies(x, c.eq(9).boolVar())));
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
        model.addClauses(ifThenElse(a, b, implies(c.not(), b.not())));
        model.addClauses(implies(b, y));
        System.out.println(model.getSolver().findSolution());
    }


    @Test
    public void pptest() {
        String f2 = "C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2_sub\\clean";
        PreprocessorHelper pph = new PreprocessorHelper();
        String f1 = "C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2";
        File file = new File("C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2");
        File file2 = new File("C:\\obermanndavid\\git-ecco\\ppfiles\\test\\git2_sub\\clean");
        pph.generateCleanVersion(file, file2, null);
    }

    @Test
    public void chocoSolverExperiment() {
        Model model = new Model("test4");
        IntVar c = model.intVar("C", Short.MIN_VALUE, Short.MAX_VALUE);
        //IntVar c = checkVars(model, "C").asIntVar();
        model.post(c.gt(4).boolVar().and(model.boolVar("A")).extension());
        Variable var = checkVars(model, "C");
        model.ifOnlyIf(model.boolVar("X").extension(), model.arithm(var.asIntVar(), "=", 6));
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
