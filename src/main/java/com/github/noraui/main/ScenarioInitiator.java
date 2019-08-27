/**
 * NoraUi is licensed under the license GNU AFFERO GENERAL PUBLIC LICENSE
 *
 * @author Nicolas HALLOUIN
 * @author Stéphane GRILLON
 */
package com.github.noraui.main;

import static com.github.noraui.utils.Constants.USER_DIR;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.github.noraui.data.DataUtils;
import com.github.noraui.exception.TechnicalException;
import com.github.noraui.gherkin.GherkinFactory;
import com.github.noraui.model.Model;
import com.github.noraui.model.ModelList;
import com.github.noraui.utils.Context;
import com.github.noraui.utils.Messages;

public class ScenarioInitiator {

    /**
     * Specific LOGGER
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(ScenarioInitiator.class);

    public static final String SCENARIO_INITIATOR_ERROR_EMPTY_FILE = "SCENARIO_INITIATOR_ERROR_EMPTY_FILE";
    
    private static final String SCENARIO_INITIATOR_ERROR_UNABLE_TO_GET_TAGS = "SCENARIO_INITIATOR_ERROR_UNABLE_TO_GET_TAGS";
    private static final String SCENARIO_INITIATOR_USAGE = "SCENARIO_INITIATOR_USAGE";
    private static final String SCENARIO_INITIATOR_INJECT_WITHOUT_MODEL = "SCENARIO_INITIATOR_INJECT_WITHOUT_MODEL";
    private static final String SCENARIO_INITIATOR_INJECT_WITH_MODEL = "SCENARIO_INITIATOR_INJECT_WITH_MODEL";
    private static final String SCENARIO_INITIATOR_ERROR_ON_INJECTING_MODEL = "SCENARIO_INITIATOR_ERROR_ON_INJECTING_MODEL";

    /**
     * This method inject data from dataProvider in Gherkin feature file. (With or Without Model).
     * 
     * @param args
     *            can be contains a specific a scenario name if you want run ONLY ONE feature by run(job in CICD).
     */
    public void start(String[] args) {
        LOGGER.info("Working Directory is '{}'", System.getProperty(USER_DIR));
        LOGGER.info("ScenarioInitiator > start()");
        if (args != null && args.length == 1 && !"@TOSPECIFY".equals(args[0])) {
            LOGGER.info("# {}", args[0]);
            final String scenarioName = args[0];
            processInjection(scenarioName);
        } else {
            LOGGER.warn(Messages.getMessage(SCENARIO_INITIATOR_USAGE));
            final String cucumberOptions = System.getProperty("cucumber.options");
            if (cucumberOptions != null && cucumberOptions.contains("--tags")) {
                final Matcher matcher = Pattern.compile(".*--tags '(.*)'.*").matcher(cucumberOptions);
                if (matcher.find() && matcher.groupCount() > 0) {
                    final String tags = matcher.group(1).replace("not ", "").replace(")", "").replace("(", "").replace(" and ", " ").replace(" or ", " ").replace("@", "");
                    for (final String s : tags.split(" ")) {
                        if (!s.startsWith("~")) {
                            processInjection(s);
                        }
                    }
                }
            } else {
                LOGGER.error(Messages.getMessage(SCENARIO_INITIATOR_ERROR_UNABLE_TO_GET_TAGS));
            }
        }
    }

    /**
     * This method inject data from dataProvider in Gherkin feature file. (With or Without Model).
     * 
     * @param scenarioName
     *            is the name of scenario.
     */
    private static void processInjection(String scenarioName) {
        try {
            Context.getDataInputProvider().prepare(scenarioName);
            final Class<Model> model = Context.getDataInputProvider().getModel(Context.getModelPackages());
            if (model == null) {
                LOGGER.info(Messages.getMessage(SCENARIO_INITIATOR_INJECT_WITHOUT_MODEL), scenarioName);
                injectWithoutModel(scenarioName);
            } else {
                LOGGER.info(Messages.getMessage(SCENARIO_INITIATOR_INJECT_WITH_MODEL), scenarioName, model.getSimpleName());
                injectWithModel(scenarioName, model);
            }
        } catch (final Exception e) {
            LOGGER.error("error ScenarioInitiator.processInjection()", e);
        }
    }

    /**
     * This method inject data from dataProvider in Gherkin feature file.
     * 
     * @param scenarioName
     *            is the name of scenario.
     * @throws TechnicalException
     *             is throws if you have a technical error (format, configuration, datas, ...) in NoraUi.
     */
    private static void injectWithoutModel(String scenarioName) throws TechnicalException {
        final String[] headers = Context.getDataInputProvider().readLine(0, false);
        if (headers != null) {
            List<String[]> examples = new ArrayList<>();
            final HashMap<Integer, List<String[]>> examplesTable = new HashMap<>();
            String[] example;
            int i = 1;
            int j = 0;
            do {
                example = Context.getDataInputProvider().readLine(i, false);
                if (example == null) {
                    examplesTable.put(Integer.valueOf(j++), examples);
                    examples = new ArrayList<>();
                } else {
                    examples.add(example);
                }
            } while (Context.getDataInputProvider().readLine(++i, false) != null || example != null);
            GherkinFactory.injectDataInGherkinExamples(scenarioName, examplesTable);
        } else {
            LOGGER.error(Messages.getMessage(SCENARIO_INITIATOR_ERROR_EMPTY_FILE));
        }
    }

    /**
     * This method inject data from dataProvider in Gherkin feature file.
     * The object corresponding to the model is in JSON format on the corresponding column.
     * 
     * @param scenarioName
     *            is the name of scenario.
     * @param model
     *            is the class of model find in 'model' package.
     * @throws TechnicalException
     *             is throws if you have a technical error (format, configuration, datas, ...) in NoraUi.
     */
    private static void injectWithModel(String scenarioName, Class<Model> model) throws TechnicalException {
        try {
            final String[] headers = Context.getDataInputProvider().readLine(0, false);
            if (headers != null) {
                List<String[]> examples = new ArrayList<>();
                final Constructor<Model> modelConstructor = DataUtils.getModelConstructor(model, headers);
                final Map<Integer, Map<String, ModelList>> fusionedData = DataUtils.fusionProcessor(model, modelConstructor);
                final HashMap<Integer, List<String[]>> examplesTable = new HashMap<>();

                for (final Entry<Integer, Map<String, ModelList>> e : fusionedData.entrySet()) {
                    for (final Entry<String, ModelList> e2 : e.getValue().entrySet()) {
                        examples.add(new String[] { e2.getKey(), e2.getValue().serialize() });
                    }
                    examplesTable.put(e.getKey(), examples);
                    examples = new ArrayList<>();
                }
                GherkinFactory.injectDataInGherkinExamples(scenarioName, examplesTable);
            } else {
                LOGGER.error(Messages.getMessage(SCENARIO_INITIATOR_ERROR_EMPTY_FILE));
            }
        } catch (final Exception te) {
            throw new TechnicalException(Messages.getMessage(SCENARIO_INITIATOR_ERROR_ON_INJECTING_MODEL) + te.getMessage(), te);
        }
    }

}
