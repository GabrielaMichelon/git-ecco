package at.jku.isse.gitecco.translation.commit.util;

import at.jku.isse.ecco.service.EccoService;
import org.testng.annotations.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class CommitOperation {


    public void createRepo(Path variantDir, Path scenarioOutputDir, String config) {
        // create new repository
        EccoService service = new EccoService();
        service.setRepositoryDir(scenarioOutputDir.resolve("repo"));
        service.init();
        System.out.println("Repository initialized.");
        System.out.println("Committing: " + variantDir);
        String configurationString = config;
        if (configurationString.isEmpty())
            configurationString = "BASE.1";

        System.out.println("CONFIG: " + configurationString);

        service.setBaseDir(variantDir.resolve("src"));
        service.commit(configurationString);

        System.out.println("Committed: " + variantDir);
        
        // close repository
        service.close();
        System.out.println("Repository closed.");

    }


}
