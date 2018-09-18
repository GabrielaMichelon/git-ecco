package at.jku.isse.gitecco.cdt;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.gnu.cpp.GPPLanguage;
import org.eclipse.cdt.core.index.IIndex;
import org.eclipse.cdt.core.model.ILanguage;
import org.eclipse.cdt.core.parser.*;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to make working with the Eclipse CDT easier.
 * This class contains methods to prepare the c++ parser and work with the AST.
 */
public class CDTHelper {

    /**
     * Opens the File at the given path string and returns the file content as string.
     *
     * @param path
     * @param encoding
     * @return
     * @throws IOException
     */
    public static String readFile(String path, Charset encoding) throws IOException {
        byte[] encoded = Files.readAllBytes(Paths.get(path));
        return "#if BASE\n"+new String(encoded, encoding)+"\n #endif";
    }

    /**
     * Creates the IASTTranslationUnit, which gives access to the AST and all
     * preprocessor directives.
     *
     * @param code the Code of the c++ file as char[].
     * @return IASTTranslationUnit - ultimately gives access to the AST
     * @throws Exception
     */
    public static IASTTranslationUnit parse(char[] code) throws Exception {

        FileContent fc = FileContent.create("/Path/ToResolveIncludePaths.cpp", code);
        Map<String, String> macroDefinitions = new HashMap<String, String>();
        String[] includeSearchPaths = new String[0];
        IScannerInfo si = new ScannerInfo(macroDefinitions, includeSearchPaths);
        IncludeFileContentProvider ifcp = IncludeFileContentProvider.getEmptyFilesProvider();
        IIndex idx = null;
        int options = ILanguage.OPTION_PARSE_INACTIVE_CODE;
        IParserLogService log = new DefaultLogService();

        return GPPLanguage.getDefault().getASTTranslationUnit(fc, si, ifcp, idx, options, log);
    }

    /**
     * Extracts the condition string of a conditional preprocessor statement.
     * Only if given Statement is in fact conditional. Other statements are ignored.
     *
     * @param s a PreprocessorStatement of type IASTPreprocessorStatement.
     * @return String Condition
     */
    public static String getCondName(IASTPreprocessorStatement s) {
        StringBuilder result = new StringBuilder();
        if (s instanceof IASTPreprocessorIfStatement) {
            IASTPreprocessorIfStatement is = (IASTPreprocessorIfStatement) s;
            result.append(is.getCondition());
        } else if (s instanceof IASTPreprocessorIfndefStatement) {
            IASTPreprocessorIfndefStatement is = (IASTPreprocessorIfndefStatement) s;
            result.append(is.getCondition());
        } else if (s instanceof IASTPreprocessorIfdefStatement) {
            IASTPreprocessorIfdefStatement is = (IASTPreprocessorIfdefStatement) s;
            result.append(is.getCondition());
        } else if (s instanceof IASTPreprocessorElifStatement) {
            IASTPreprocessorElifStatement is = (IASTPreprocessorElifStatement) s;
            result.append(is.getCondition());
        }
        return result.toString();
    }

    /**
     * Checks if the given PreprocessorStatement is the start of a feature.
     * Features are usually indicated through #if, #ifdef, #ifndef, #elif, #else.
     *
     * @param s the IASTPreProcessorStatement to check
     * @return <code>true</code> if the PreProcessorStatement is the start of a feature, otherwise <code>false</code> if returned.
     */
    public static boolean isFeatureStart(IASTPreprocessorStatement s) {
        return s instanceof IASTPreprocessorIfStatement
                || s instanceof IASTPreprocessorIfndefStatement
                || s instanceof IASTPreprocessorIfdefStatement;
    }
}
