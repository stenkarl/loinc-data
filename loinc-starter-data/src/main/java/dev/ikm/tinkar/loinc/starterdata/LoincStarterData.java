/*
 * Copyright © 2015 Integrated Knowledge Management (support@ikm.dev)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.ikm.tinkar.loinc.starterdata;

import dev.ikm.tinkar.common.id.IntIds;
import dev.ikm.tinkar.common.id.PublicId;
import dev.ikm.tinkar.common.id.PublicIds;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.util.uuid.UuidT5Generator;
import dev.ikm.tinkar.composer.ComposerSession;
import dev.ikm.tinkar.composer.ComposerSessionManager;
import dev.ikm.tinkar.composer.template.Definition;
import dev.ikm.tinkar.composer.template.FullyQualifiedName;
import dev.ikm.tinkar.composer.template.Synonym;
import dev.ikm.tinkar.composer.template.USEnglishDialect;
import dev.ikm.tinkar.coordinate.stamp.*;
import dev.ikm.tinkar.coordinate.stamp.calculator.Latest;
import dev.ikm.tinkar.entity.*;
import dev.ikm.tinkar.entity.export.ExportEntitiesController;
import dev.ikm.tinkar.entity.graph.DiTreeEntity;
import dev.ikm.tinkar.entity.graph.adaptor.axiom.LogicalExpression;
import dev.ikm.tinkar.entity.graph.isomorphic.IsomorphicResults;
import dev.ikm.tinkar.entity.graph.isomorphic.IsomorphicResultsLeafHash;
import dev.ikm.tinkar.entity.transaction.Transaction;
import dev.ikm.tinkar.ext.lang.owl.SctOwlUtilities;
import dev.ikm.tinkar.starterdata.StarterData;
import dev.ikm.tinkar.starterdata.UUIDUtility;
import dev.ikm.tinkar.terms.EntityProxy;
import dev.ikm.tinkar.terms.State;
import dev.ikm.tinkar.terms.TinkarTerm;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.stream.Stream;

import static dev.ikm.tinkar.loinc.starterdata.LoincConstants.REGEX_LINEDATA;
import static dev.ikm.tinkar.loinc.starterdata.TinkarizerUtility.processHeaders;


/**
 * Builds the starter data for a LOINC data import using the Tinkar Composer
 */
public class LoincStarterData {

    public static final String ATTRIBUTE = "Attribute";
    public static final String IDENTIFIER_SOURCE = "IDENTIFIER_SOURCE";
    public static final String ORDER_VS_OBSERVATION = "Order Vs Observation";
    public static final String STATUS_VALUE = "STATUS_VALUE";
    public static final String AUTHOR = "AUTHOR";
    public static final String LOINC_AUTHOR = "Regenstrief Institute, Inc. Author";
    public static final String MODULE = "MODULE";
    public static final String PHENOMENON = "PHENOMENON";
    private static final int PART_FQN_INDEX = 2;
    private static final int PART_SYNOMYM_INDEX = 3;
    private static final int PART_TYPE_INDEX = 1;
    private static final int LOINC_LONG_COMMON_NAME_INDEX = 25;
    public static final String LOINC_NUMBER = "LOINC Number";
    public static final String OBSERVABLE_ENTITY = "Observable Entity";
    public static final String CLASS = "CLASS";
    private static final int STATUS_INDEX = 11;
    public static final int CLASSNAME_INDEX = 7;
    public static final int CLASS_TYPE_INDEX = 13;
    public static final String EXAMPLE_UCUM_UNITS_PATTERN = "Example UCUM Units Pattern";
    public static final int ORDER_OBS_INDEX = 21;
    public static final int UUCM_PATTERN_INDEX = 26;
    public static final String LOINC_TRIAL_STATUS_PATTERN = "LOINC Trial Status Pattern";
    public static final String LOINC_DISCOURAGED_STATUS_PATTERN = "LOINC Discouraged Status Pattern";
    public static final String LOINC_CLASS_PATTERN = "LOINC Class Pattern";
    public static final String TEST_REPORTABLE_MEMBERSHIP_PATTERN = "Test Reportable Membership Pattern";
    public static final String TEST_SUBSET_MEMBERSHIP_PATTERN = "Test Subset Membership Pattern";
    public static final String TEST_ORDERABLE_PATTERN = "Test Orderable Pattern";
    public static final int PART_STATUS_INDEX = 4;
    private final int CONCEPT_INDEX = 0;
    private final int FQN_INDEX = 1;
    private final int SYNOMYM_INDEX = 2;
    private final int DEFINITION_INDEX = 3;
    private final int ORIGINS_INDEX = 4;

    private final int DESTINATIONS_INDEX = 6;

    private static final int CLASS_INDEX = 7;
    private final int destinationPatternNid = TinkarTerm.EL_PLUS_PLUS_STATED_AXIOMS_PATTERN.nid();

    private final State status = State.ACTIVE;
    //private final long time = PrimitiveData.PREMUNDANE_TIME;
    private final long time = System.currentTimeMillis();
    private EntityProxy.Concept author = TinkarTerm.USER;
    private EntityProxy.Concept module = TinkarTerm.PRIMORDIAL_MODULE;
    private final EntityProxy.Concept path = TinkarTerm.PRIMORDIAL_PATH;

    private final HashMap<String, EntityProxy.Concept> fqnToConceptHashMap = new HashMap<>();

    private static final Logger LOG = LoggerFactory.getLogger(LoincStarterData.class.getSimpleName());

    public static final Function<String, File> createFilePathInTarget = (pathName) -> new File("%s/target/%s".formatted(System.getProperty("user.dir"), pathName));
    //public static final File PB_STARTER_DATA = createFilePathInTarget.apply("data/tinkar-starter-data-1.0.0-pb.zip");

    public static final File PB_EXPORT_DATA = createFilePathInTarget.apply("data/tinkar-export-data-1.0.0-pb.zip");
    public static final ComposerSessionManager COMPOSER_SESSION_MANAGER = new ComposerSessionManager();

    UUIDUtility uuidUtility = new UUIDUtility();
    public static List<String> partTypeNames = List.of("COMPONENT", "PROPERTY", "TIME", "SYSTEM", "SCALE", "METHOD", "CLASS", "CLASSTYPE");
    private final File loincStarterDataCSVfile;

    private final File loincPartCSVFile;

    private File loincConceptFile = null;

    private ComposerSession session;
    private int conceptCount = 0;
    private int semanticsCreated = 0;

    public void setDataStore(File dataStore) {
        this.dataStore = dataStore;
    }

    private File dataStore;

    public enum LOINC_AXES {
        COMPONENT(1),
        PROPERTY(2),
        TIME_ASPECT(3),
        SYSTEM(4),
        SCALE_TYPE(5),
        METHOD(6);

        private final int value;

        LOINC_AXES(int i) {
            this.value = i;
        }
    }

    private final String[] AXIS_FQNS = {"Component", "Property", "Time Aspect", "System", "Scale", "Method"};

    public LoincStarterData(File loincStarterDataCSVfile, File loincPartsCSVfile) {
        this.loincStarterDataCSVfile = loincStarterDataCSVfile;
        this.loincPartCSVFile = loincPartsCSVfile;
        if (!loincStarterDataCSVfile.getName().toLowerCase().endsWith(".csv"))
            throw new IllegalArgumentException("Please pass at least 1 argument. This argument should be the starterdata.csv file");

        if (!loincPartCSVFile.getName().contains("csv")) {
            throw new IllegalArgumentException("This argument should be the Part.csv file");
        }

        author = EntityProxy.Concept.make(LOINC_AUTHOR, UUID.nameUUIDFromBytes(LOINC_AUTHOR.getBytes()));

        session = COMPOSER_SESSION_MANAGER.sessionWithStamp(status, time, TinkarTerm.USER, module, path);

        session.composeConcept(author)
                .with(new FullyQualifiedName(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, LOINC_AUTHOR, TinkarTerm.DESCRIPTION_CASE_SENSITIVE)
                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                .with(new Synonym(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, "LOINC Author", TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                .with(new Definition(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, "Regenstrief Institute, Inc. Author - The entity responsible for publishing LOINC", TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)));
        stopIngest();
        PrimitiveData.start();
        session = COMPOSER_SESSION_MANAGER.sessionWithStamp(status, time, author, module, path);

        fqnToConceptHashMap.put(LOINC_AUTHOR, author);

    }

    public static void main(String[] args) throws IOException {

        if (args.length != 1 && !args[0].toLowerCase().endsWith(".csv"))
            throw new IllegalArgumentException("Please pass at least 1 argument. This argument should be the starterdata.csv file");

        if (args.length == 2 && args[1].toLowerCase().contains("Part.csv")) {
            throw new IllegalArgumentException("This argument should be the Part.csv file");
        }

        LoincStarterData loincStarterData = new LoincStarterData((new File(args[0])), (new File(args[1])));

        if (loincStarterData.loincStarterDataCSVfile.exists() && loincStarterData.loincPartCSVFile.exists()) {
            LOG.info("Processing LOINC starter data");
            loincStarterData.processLoincStarterDataCSVFile();
            //loincStarterData.processLoincPartCSVfile();
        } else {
            throw new RuntimeException("Not all input files found");
        }


    }

    public void setLoincConceptFile(File loincConceptFile) {
        this.loincConceptFile = loincConceptFile;
    }

    public void processLoincConceptDataFile() throws IOException {

        if (loincConceptFile == null)
            return;

        try (Stream<String> lines = Files.lines(loincConceptFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .filter(data -> !(String.valueOf(data[STATUS_INDEX]).toLowerCase().contains("deprecated")))
                    .forEach(data -> {
                        conceptCount++;
                        // LOG.info(String.format("%s | %s | %s | %s | %s",data[0], data[1],data[2],data[3],data[4]));
                        String loincLongCommonName = fixString(data[LOINC_LONG_COMMON_NAME_INDEX]);
                        String conceptID = fixString(data[CONCEPT_INDEX]);

                        //if (fqnToConceptHashMap.get(loincLongCommonName) != null) {
                        EntityProxy.Concept newConcept = EntityProxy.Concept.make(conceptID, UUID.nameUUIDFromBytes(conceptID.getBytes()));
                        session.composeConcept(newConcept)
                                .with(new FullyQualifiedName(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, loincLongCommonName, TinkarTerm.DESCRIPTION_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Synonym(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, loincLongCommonName, TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Definition(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, loincLongCommonName, TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))

                        ;

                        fqnToConceptHashMap.put(conceptID, newConcept);
                    });


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        stopIngest();
        PrimitiveData.start();
        session = COMPOSER_SESSION_MANAGER.sessionWithStamp(State.INACTIVE, time, author, module, path);

        try (Stream<String> lines = Files.lines(loincConceptFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .filter(data -> (String.valueOf(data[STATUS_INDEX]).toLowerCase().contains("deprecated")))
                    .forEach(data -> {
                        conceptCount++;
                        // LOG.info(String.format("%s | %s | %s | %s | %s",data[0], data[1],data[2],data[3],data[4]));
                        String loincLongCommonName = fixString(data[LOINC_LONG_COMMON_NAME_INDEX]);
                        String conceptID = fixString(data[CONCEPT_INDEX]);

                        EntityProxy.Concept newConcept = EntityProxy.Concept.make(conceptID, UUID.nameUUIDFromBytes(conceptID.getBytes()));
                        session.composeConcept(newConcept)
                                .with(new FullyQualifiedName(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, loincLongCommonName, TinkarTerm.DESCRIPTION_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Synonym(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, loincLongCommonName, TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Definition(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, loincLongCommonName, TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))

                        ;

                        fqnToConceptHashMap.put(conceptID, newConcept);

                    });


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        stopIngest();
        PrimitiveData.start();
        session = COMPOSER_SESSION_MANAGER.sessionWithStamp(State.ACTIVE, time, author, module, path);

    }

    public void processLoincPartCsvFile() throws IOException {

        processHeaderLine(loincPartCSVFile);

        try (Stream<String> lines = Files.lines(loincPartCSVFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .filter(data -> {
                                String cleanString = fixString(data[1]);
                                return partTypeNames.contains(fixString(cleanString));
                            }
                    )
                    .forEach(data -> {
                        // LOG.info(String.format("%s | %s | %s | %s | %s",data[0], data[1],data[2],data[3],data[4]));
                        EntityProxy.Concept newConcept = EntityProxy.Concept.make(fixString(data[CONCEPT_INDEX]), UUID.nameUUIDFromBytes(fixString(data[CONCEPT_INDEX]).getBytes()));
                        session.composeConcept(newConcept)
                                .with(new FullyQualifiedName(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, fixString(data[PART_FQN_INDEX]), TinkarTerm.DESCRIPTION_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Synonym(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, fixString(data[PART_SYNOMYM_INDEX]), TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Definition(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, fixString(data[PART_SYNOMYM_INDEX]), TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)));

                        fqnToConceptHashMap.put(fixString(data[PART_FQN_INDEX]), newConcept);
                        fqnToConceptHashMap.put(fixString(data[CONCEPT_INDEX]), newConcept);

                    });


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    private static String fixString(String inputString) {
        String cleanString;
        cleanString = inputString.replaceAll("\"", "");
        return cleanString;
    }

    public void processLoincStarterDataCSVFile() throws IOException {

        try (Stream<String> lines = Files.lines(loincStarterDataCSVfile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .forEach(data -> {

                        String synonym = getSynonym(data);

                        EntityProxy.Concept newConcept = EntityProxy.Concept.make(data[CONCEPT_INDEX], UUID.nameUUIDFromBytes(data[CONCEPT_INDEX].getBytes()));
                        session.composeConcept(newConcept)
                                .with(new FullyQualifiedName(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, data[FQN_INDEX], TinkarTerm.DESCRIPTION_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Synonym(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, synonym, TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Definition(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, data[DEFINITION_INDEX], TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)));

                        fqnToConceptHashMap.put(data[CONCEPT_INDEX], newConcept);

                    });

            processLoincPartCsvFile();

            processLoincConceptDataFile();


        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        //Stop the composer and release the lock on the database so StartData can use it.
        stopIngest();

        addStatedDefinitionAndNavigation();

        LOG.info(conceptCount + " LOINC Concepts were created");
    }

    private void addStatedDefinitionAndNavigation() {

        StarterData starterData = new StarterData(dataStore, uuidUtility)
                .init()
                .authoringSTAMP(
                        TinkarTerm.ACTIVE_STATE,
                        time,
                        author,
                        module,
                        path);


        addNavigationAndDefintionToStarterConcepts(starterData, uuidUtility);

        addNavigationAndDefinitionToPartsConcepts(starterData);

        addNavigationAndDefinitionToMainLoincConcepts(starterData);

        starterData.build(); //Natively writing data to spined array
        //exportStarterData(); //exports starter data to pb.zip
        starterData.shutdown();

        starterData = new StarterData(dataStore, uuidUtility)
                .init()
                .authoringSTAMP(
                        TinkarTerm.INACTIVE_STATE,
                        time,
                        author,
                        module,
                        path);

        addNavigationAndDefinitionToDeprecatedMainLoincConcepts(starterData);

        starterData.build(); //Natively writing data to spined array
        //exportStarterData(); //exports starter data to pb.zip
        starterData.shutdown();


    }

    private void addNavigationAndDefinitionToMainLoincConcepts(StarterData starterData) {

        try (Stream<String> lines = Files.lines(loincConceptFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .filter(data -> !(String.valueOf(data[STATUS_INDEX]).toLowerCase().contains("deprecated")))
                    .forEach(data -> {
                        // LOG.info(String.format("%s | %s | %s | %s | %s",data[0], data[1],data[2],data[3],data[4]));
                        StringBuilder owlString = new StringBuilder();

                        EntityProxy.Concept newConcept = fqnToConceptHashMap.get(fixString(data[CONCEPT_INDEX]));

                        starterData.concept(newConcept)
                                .identifier(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER, data[CONCEPT_INDEX])
                                //TODO Doing the below will cause the reasoner to break
                                //.statedDefinition(Arrays.asList(fqnToConceptHashMap.get(fixString(data[CLASS_INDEX]))))
                                .build();

                        owlString.append("EquivalentClasses( \n");
                        owlString.append(":[" + newConcept.publicId().asUuidArray()[0] + "] \n");
                        owlString.append("\tObjectIntersectionOf( ");
                        owlString.append(":[" + fqnToConceptHashMap.get(OBSERVABLE_ENTITY).publicId().asUuidArray()[0] + "] \n");


                        for (LOINC_AXES axis : LOINC_AXES.values()) {
                            EntityProxy.Concept loincAxisConcept = fqnToConceptHashMap.get(fixString(data[axis.value]));

                            if (loincAxisConcept != null) {
                                owlString.append("\tObjectSomeValuesFrom( \n");
                                owlString.append("\t\t:[" + TinkarTerm.ROLE_GROUP.publicId().asUuidArray()[0] + "] \n");
                                owlString.append("\t\t\tObjectSomeValuesFrom( \n");
                                owlString.append("\t\t\t:[" + fqnToConceptHashMap.get(AXIS_FQNS[axis.value - 1]).publicId().asUuidArray()[0] + "] \n");
                                owlString.append("\t\t\t:[" + loincAxisConcept.publicId().asUuidArray()[0] + "] \n");
                                owlString.append("\t\t )\n");
                                owlString.append("\t )\n");

                            } else {
                                LOG.info("There was no axis value for " + axis);
                            }
                        }

                        owlString.append("\t\t)");  // Close ObjectIntersectionOf
                        owlString.append("\t"); // Close EquivalentClasses

                        //LOG.info(owlString.toString());

                        try {
                            int nid = newConcept.nid();
                            reasonOWLexpression(owlString.toString(), nid);
                            addLoincClassSemanticPattern(newConcept, data[CLASSNAME_INDEX], data[CLASS_TYPE_INDEX], starterData);
                            addUUCMSemanticPattern(newConcept, data[UUCM_PATTERN_INDEX], starterData);
                            addLoincTestOrdObservationSemanticPattern(newConcept, data[ORDER_OBS_INDEX], starterData);
                        } catch (NullPointerException nullPointerException) {
                            LOG.error(nullPointerException.getMessage());
                        }

                    });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addNavigationAndDefinitionToDeprecatedMainLoincConcepts(StarterData starterData) {

        try (Stream<String> lines = Files.lines(loincConceptFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .filter(data -> (String.valueOf(data[STATUS_INDEX]).toLowerCase().contains("deprecated")))
                    .forEach(data -> {
                        // LOG.info(String.format("%s | %s | %s | %s | %s",data[0], data[1],data[2],data[3],data[4]));
                        StringBuilder owlString = new StringBuilder();

                        EntityProxy.Concept newConcept = fqnToConceptHashMap.get(fixString(data[CONCEPT_INDEX]));

                        starterData.concept(newConcept)
                                .identifier(TinkarTerm.UNIVERSALLY_UNIQUE_IDENTIFIER, data[CONCEPT_INDEX])
                                //TODO Doing the below will cause the reasoner to break
                                //.statedDefinition(Arrays.asList(fqnToConceptHashMap.get(fixString(data[CLASS_INDEX]))))
                                .build();

                        owlString.append("EquivalentClasses( \n");
                        owlString.append(":[" + newConcept.publicId().asUuidArray()[0] + "] \n");
                        owlString.append("\tObjectIntersectionOf( ");
                        owlString.append(":[" + fqnToConceptHashMap.get(OBSERVABLE_ENTITY).publicId().asUuidArray()[0] + "] \n");


                        for (LOINC_AXES axis : LOINC_AXES.values()) {
                            EntityProxy.Concept loincAxisConcept = fqnToConceptHashMap.get(fixString(data[axis.value]));

                            if (loincAxisConcept != null) {
                                owlString.append("\tObjectSomeValuesFrom( \n");
                                owlString.append("\t\t:[" + TinkarTerm.ROLE_GROUP.publicId().asUuidArray()[0] + "] \n");
                                owlString.append("\t\t\tObjectSomeValuesFrom( \n");
                                owlString.append("\t\t\t:[" + fqnToConceptHashMap.get(AXIS_FQNS[axis.value - 1]).publicId().asUuidArray()[0] + "] \n");
                                owlString.append("\t\t\t:[" + loincAxisConcept.publicId().asUuidArray()[0] + "] \n");
                                owlString.append("\t\t )\n");
                                owlString.append("\t )\n");

                            } else {
                                LOG.info("There was no axis value for " + axis);
                            }
                        }

                        owlString.append("\t\t)");  // Close ObjectIntersectionOf
                        owlString.append("\t"); // Close EquivalentClasses

                        //LOG.info(owlString.toString());

                        try {
                            int nid = newConcept.nid();
                            reasonOWLexpression(owlString.toString(), nid);
                            addLoincClassSemanticPattern(newConcept, data[CLASSNAME_INDEX], data[CLASS_TYPE_INDEX], starterData);
                            addLoincTestOrdObservationSemanticPattern(newConcept, data[ORDER_OBS_INDEX], starterData);
                            addUUCMSemanticPattern(newConcept, data[UUCM_PATTERN_INDEX], starterData);
                        } catch (NullPointerException nullPointerException) {
                            LOG.error(nullPointerException.getMessage());
                            //severeErrors++;
                        }

                    });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void reasonOWLexpression(String owlString, int nid) {
        try {
            LOG.info("Attempting to reason this Ontology: ");
            // LOG.info(owlString);
            LogicalExpression expression = SctOwlUtilities.sctToLogicalExpression(owlString, "");
            // LOG.info(expression.toString());
            Transaction transaction = new Transaction("LOINC concept " + nid);
            StampPositionRecord stampPositionRecord = StampPositionRecordBuilder.builder().time(Long.MAX_VALUE).pathForPositionNid(path.nid()).build();
            StampCoordinateRecord stampCoordinateRecord = StampCoordinateRecordBuilder.builder()
                    .allowedStates(StateSet.ACTIVE)
                    .stampPosition(stampPositionRecord)
                    .moduleNids(IntIds.set.of(module.nid()))
                    .build().withStampPositionTime(Long.MAX_VALUE);

            LOG.info("Processing Concept : " + EntityService.get().getEntityFast(nid).description());
            addLogicalExpression(transaction, nid,
                    expression,
                    System.currentTimeMillis(),
                    stampCoordinateRecord);


        } catch (IOException e) {
            LOG.error(e.getMessage());
            //severeErrors++;

        } catch (Exception e) {
            LOG.error(e.getMessage());
            //severeErrors++;
        }
    }

    /**
     * Adds the relationship graph.
     *
     * @param conceptNid        the conceptNid
     * @param logicalExpression the logical expression
     * @param time              the time
     * @param stampCoordinate   for determining current version if a graph already
     */
    private void addLogicalExpression(Transaction transaction, int conceptNid,
                                      LogicalExpression logicalExpression,
                                      long time, StampCoordinateRecord stampCoordinate) throws Exception {

        // See if a semantic already exists in this pattern referencing this concept...

        int[] semanticNidsForComponentOfPattern = EntityService.get().semanticNidsForComponentOfPattern(conceptNid, destinationPatternNid);
        if (semanticNidsForComponentOfPattern.length > 0) {
            if (semanticNidsForComponentOfPattern.length != 1) {
                throw new IllegalStateException("To many graphs for component: " + PrimitiveData.text(conceptNid));
            }
            SemanticRecord existingSemantic = EntityService.get().getEntityFast(semanticNidsForComponentOfPattern[0]);
            Latest<SemanticVersionRecord> latest = stampCoordinate.stampCalculator().latest(existingSemantic);

            if (latest.isPresent()) {
                SemanticEntityVersion logicalExpressionSemanticVersion = latest.get();
                DiTreeEntity latestExpression = (DiTreeEntity) logicalExpressionSemanticVersion.fieldValues().get(0);
                DiTreeEntity newExpression = (DiTreeEntity) logicalExpression.sourceGraph();

                IsomorphicResultsLeafHash isomorphicResultsComputer = new IsomorphicResultsLeafHash(latestExpression, newExpression, conceptNid);
                IsomorphicResults isomorphicResults = isomorphicResultsComputer.call();

                if (!isomorphicResults.equivalent()) {
                    addNewVersion(transaction, logicalExpression, time, SemanticRecordBuilder.builder(existingSemantic));
                }
            } else {
                // Latest is inactive or non-existent, need to add new.
                addNewVersion(transaction, logicalExpression, time, SemanticRecordBuilder.builder(existingSemantic));
            }
        } else {
// Create UUID from seed and assign SemanticBuilder the value
            UUID generartedSemanticUuid = UuidT5Generator.singleSemanticUuid(EntityService.get().getEntityFast(destinationPatternNid),
                    EntityService.get().getEntityFast(conceptNid));

            SemanticRecordBuilder newSemanticBuilder = SemanticRecordBuilder.builder();
            newSemanticBuilder.mostSignificantBits(generartedSemanticUuid.getMostSignificantBits());
            newSemanticBuilder.leastSignificantBits(generartedSemanticUuid.getLeastSignificantBits());
            newSemanticBuilder.patternNid(destinationPatternNid);
            newSemanticBuilder.referencedComponentNid(conceptNid);
            newSemanticBuilder.nid(PrimitiveData.nid(generartedSemanticUuid));

            addNewVersion(transaction, logicalExpression, time, newSemanticBuilder);
        }
    }

    /**
     * Helper function used in building Semantics.
     *
     * @param transaction
     * @param logicalExpression
     * @param time
     * @param newSemanticBuilder
     */

    private void addNewVersion(Transaction transaction, LogicalExpression logicalExpression,
                               long time, SemanticRecordBuilder newSemanticBuilder) {

        ImmutableList<SemanticVersionRecord> oldSemanticVersions = newSemanticBuilder.versions();
        RecordListBuilder<SemanticVersionRecord> versionListBuilder = new RecordListBuilder<>();
        newSemanticBuilder.versions(versionListBuilder);
        SemanticRecord newSemantic = newSemanticBuilder.build();

        if (oldSemanticVersions != null) {
            oldSemanticVersions.forEach(version -> {
                versionListBuilder.add(SemanticVersionRecordBuilder.builder(version).chronology(newSemantic).build());
            });
        }

        SemanticVersionRecordBuilder semanticVersionBuilder = SemanticVersionRecordBuilder.builder();
        semanticVersionBuilder.fieldValues(Lists.immutable.of(logicalExpression.sourceGraph()));
        StampEntity transactionStamp = transaction.getStamp(State.ACTIVE, time, author.nid(), module.nid(), path.nid());
        semanticVersionBuilder.stampNid(transactionStamp.nid());
        semanticVersionBuilder.chronology(newSemantic);
        versionListBuilder.add(semanticVersionBuilder.build());
        versionListBuilder.build();

        EntityService.get().putEntity(newSemantic);
    }

    private void addNavigationAndDefinitionToPartsConcepts(StarterData starterData) {
        try (Stream<String> lines = Files.lines(loincPartCSVFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1)).filter(data -> {
                                String cleanString = data[1];
                                return partTypeNames.contains(fixString(cleanString));
                            }
                    )

                    .forEach(data -> {

                        String conceptIdentifier = fixString(data[CONCEPT_INDEX]);
                        EntityProxy.Concept newConcept = fqnToConceptHashMap.get(conceptIdentifier);

                        if (String.valueOf(data[PART_TYPE_INDEX]).toLowerCase().contains("component")) {
                            starterData.concept(newConcept)
                                    .identifier(fqnToConceptHashMap.get(LOINC_NUMBER), conceptIdentifier)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get("Component")))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get("Component")))
                                    .build();
                        } else if (String.valueOf(data[PART_TYPE_INDEX]).toLowerCase().contains("method")) {
                            starterData.concept(newConcept)
                                    .identifier(fqnToConceptHashMap.get(LOINC_NUMBER), conceptIdentifier)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get("Method")))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get("Method")))
                                    .build();
                        } else if (String.valueOf(data[PART_TYPE_INDEX]).toLowerCase().contains("property")) {
                            starterData.concept(newConcept)
                                    .identifier(fqnToConceptHashMap.get(LOINC_NUMBER), conceptIdentifier)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get("Property")))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get("Property")))
                                    .build();
                        } else if (String.valueOf(data[PART_TYPE_INDEX]).toLowerCase().contains("scale")) {
                            starterData.concept(newConcept)
                                    .identifier(fqnToConceptHashMap.get(LOINC_NUMBER), conceptIdentifier)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get("Scale")))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get("Scale")))
                                    .build();
                        } else if (String.valueOf(data[PART_TYPE_INDEX]).toLowerCase().contains("time")) {
                            starterData.concept(newConcept)
                                    .identifier(fqnToConceptHashMap.get(LOINC_NUMBER), conceptIdentifier)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get("Time Aspect")))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get("Time Aspect")))
                                    .build();
                        } else if (String.valueOf(data[PART_TYPE_INDEX]).toLowerCase().contains("system")) {
                            starterData.concept(newConcept)
                                    .identifier(fqnToConceptHashMap.get(LOINC_NUMBER), conceptIdentifier)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get("System")))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get("System")))
                                    .build();
                        }

                        addStatusSemanticPatterns(newConcept, data[PART_STATUS_INDEX], starterData);

                    });

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addNavigationAndDefintionToStarterConcepts(StarterData starterData, UUIDUtility uuidUtility) {
        try (Stream<String> lines = Files.lines(loincStarterDataCSVfile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .forEach(data -> {

                        EntityProxy.Concept newConcept = fqnToConceptHashMap.get(data[CONCEPT_INDEX]);

                        if (String.valueOf(data[ORIGINS_INDEX]).trim().equals(PHENOMENON)) {
                            starterData.concept(newConcept)
                                    .statedDefinition(List.of(TinkarTerm.PHENOMENON))
                                    .statedNavigation(List.of(newConcept), List.of(TinkarTerm.PHENOMENON))
                                    .build();
                        } else if (String.valueOf(data[ORIGINS_INDEX]).trim().equals(ATTRIBUTE)) {
                            starterData.concept(newConcept)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get(ATTRIBUTE)))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get(ATTRIBUTE)))
                                    .build();
                        } else if (String.valueOf(data[ORIGINS_INDEX]).trim().equals(IDENTIFIER_SOURCE)) {
                            starterData.concept(newConcept)
                                    .statedDefinition(List.of(TinkarTerm.IDENTIFIER_SOURCE))
                                    .statedNavigation(List.of(newConcept), List.of(TinkarTerm.IDENTIFIER_SOURCE))
                                    .build();
                        } else if (String.valueOf(data[ORIGINS_INDEX]).trim().equals(ORDER_VS_OBSERVATION)) {
                            starterData.concept(newConcept)
                                    .statedDefinition(Collections.singletonList(fqnToConceptHashMap.get(ORDER_VS_OBSERVATION)))
                                    .statedNavigation(List.of(newConcept), Collections.singletonList(fqnToConceptHashMap.get(ORDER_VS_OBSERVATION)))
                                    .build();
                        } else if (String.valueOf(data[ORIGINS_INDEX]).trim().equals(STATUS_VALUE)) {
                            starterData.concept(newConcept)
                                    .statedDefinition(List.of(TinkarTerm.STATUS_VALUE))
                                    .statedNavigation(List.of(newConcept), List.of(TinkarTerm.STATUS_VALUE))
                                    .build();
                        } else if (String.valueOf(data[ORIGINS_INDEX]).trim().equals(AUTHOR)) {
                            EntityProxy.Concept baseAuthor = EntityProxy.Concept.make("Author", UUID.fromString("f7495b58-6630-3499-a44e-2052b5fcf06c")); // Author GUID from Tinkar root concept: "f7495b58-6630-3499-a44e-2052b5fcf06c"
                            starterData.concept(newConcept)
                                    .statedDefinition(List.of(baseAuthor))
                                    .statedNavigation(List.of(newConcept), List.of(baseAuthor))
                                    .build();
                        } else if (String.valueOf(data[ORIGINS_INDEX]).trim().equals(MODULE)) {
                            starterData.concept(newConcept)
                                    .statedDefinition(List.of(TinkarTerm.MODULE))
                                    .statedNavigation(List.of(newConcept), List.of(TinkarTerm.MODULE))
                                    .build();
                            module = newConcept;
                        }

                    });


            buildLoincPatterns(starterData, uuidUtility);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addLoincClassSemanticPattern(EntityProxy.Concept loincConcept, String loincClass, String loincClassType, StarterData starterData) {

        if (loincConcept == null) {
            return;
        }

        MutableList<Object> classPatternFields = Lists.mutable.empty();

        EntityProxy.Concept loincClassConcept = fqnToConceptHashMap.get(fixString(loincClass));

        if (loincClassConcept == null) {
            return;
        }

        //classPatternFields.add(loincClassConcept.nid());
        //classPatternFields.add(fixString(loincClassType));

        UUIDUtility uuidUtility = new UUIDUtility();
        PublicId patternPublicId = PublicIds.of(uuidUtility.createUUID(LOINC_CLASS_PATTERN));
        int patternNid = EntityService.get().nidForPublicId(patternPublicId);
        PublicId referencedComponentPublicID = loincConcept.publicId();
        int referencedComponentNid = EntityService.get().nidForPublicId(referencedComponentPublicID);
        PublicId semantic = PublicIds.singleSemanticId(patternPublicId, referencedComponentPublicID);
        int semanticNid = EntityService.get().nidForPublicId(semantic);
        UUID primordialUUID = semantic.asUuidArray()[0];
        int stampNid = EntityService.get().nidForPublicId(starterData.getAuthoringSTAMP());

        writeSemantic(semanticNid, primordialUUID, patternNid, referencedComponentNid, stampNid, classPatternFields);


    }

    private void addUUCMSemanticPattern(EntityProxy.Concept loincConcept, String uucmPattern, StarterData starterData) {

        if (loincConcept == null) {
            return;
        }

        MutableList<Object> classPatternFields = Lists.mutable.empty();

        if (fixString(uucmPattern).isEmpty() || fixString(uucmPattern).isBlank()) {
            return;
        }

        classPatternFields.add(fixString(uucmPattern));

        UUIDUtility uuidUtility = new UUIDUtility();
        PublicId patternPublicId = PublicIds.of(uuidUtility.createUUID(EXAMPLE_UCUM_UNITS_PATTERN));
        int patternNid = EntityService.get().nidForPublicId(patternPublicId);
        PublicId referencedComponentPublicID = loincConcept.publicId();
        int referencedComponentNid = EntityService.get().nidForPublicId(referencedComponentPublicID);
        PublicId semantic = PublicIds.singleSemanticId(patternPublicId, referencedComponentPublicID);
        int semanticNid = EntityService.get().nidForPublicId(semantic);
        UUID primordialUUID = semantic.asUuidArray()[0];
        int stampNid = EntityService.get().nidForPublicId(starterData.getAuthoringSTAMP());

        writeSemantic(semanticNid, primordialUUID, patternNid, referencedComponentNid, stampNid, classPatternFields);

    }

    private void addStatusSemanticPatterns(EntityProxy.Concept loincPartConcept, String status, StarterData starterData) {

        if (loincPartConcept == null) {
            return;
        }

        MutableList<Object> classPatternFields = Lists.mutable.empty();
        String semanticPattern;

        if (fixString(status).isEmpty() || fixString(status).isBlank()) {
            return;
        }

        if (status.toLowerCase().contains("active")) {
            semanticPattern = LOINC_TRIAL_STATUS_PATTERN;
            //classPatternFields.add(fqnToConceptHashMap.get("Trial Status").nid());
        } else if (status.toLowerCase().contains("deprecated")) {
            semanticPattern = LOINC_DISCOURAGED_STATUS_PATTERN;
            //classPatternFields.add(fqnToConceptHashMap.get("Discouraged Status").nid());
        } else return;

        UUIDUtility uuidUtility = new UUIDUtility();
        PublicId patternPublicId = PublicIds.of(uuidUtility.createUUID(semanticPattern));
        int patternNid = EntityService.get().nidForPublicId(patternPublicId);
        PublicId referencedComponentPublicID = loincPartConcept.publicId();
        int referencedComponentNid = EntityService.get().nidForPublicId(referencedComponentPublicID);
        PublicId semantic = PublicIds.singleSemanticId(patternPublicId, referencedComponentPublicID);
        int semanticNid = EntityService.get().nidForPublicId(semantic);
        UUID primordialUUID = semantic.asUuidArray()[0];
        int stampNid = EntityService.get().nidForPublicId(starterData.getAuthoringSTAMP());

        writeSemantic(semanticNid, primordialUUID, patternNid, referencedComponentNid, stampNid, classPatternFields);

    }


    private void addLoincTestOrdObservationSemanticPattern(EntityProxy.Concept loincConcept, String orderObsValue, StarterData starterData) {


        ArrayList<String> requiredPatterns = new ArrayList<>();

        if (loincConcept == null || orderObsValue == null || orderObsValue.isBlank() || orderObsValue.isEmpty()) {
            return;
        }

        if (orderObsValue.trim().toLowerCase().contains("observation")) {
            requiredPatterns.add(TEST_REPORTABLE_MEMBERSHIP_PATTERN);
        } else if (orderObsValue.trim().toLowerCase().contains("order")) {
            requiredPatterns.add(TEST_ORDERABLE_PATTERN);
        } else if (orderObsValue.trim().toLowerCase().contains("both")) {
            requiredPatterns.add(TEST_ORDERABLE_PATTERN);
            requiredPatterns.add(TEST_REPORTABLE_MEMBERSHIP_PATTERN);
        } else if (orderObsValue.trim().toLowerCase().contains("subset")) {
            requiredPatterns.add(TEST_SUBSET_MEMBERSHIP_PATTERN);
        } else {
            return;
        }

        MutableList<Object> classPatternFields = Lists.mutable.empty();


        //classPatternFields.add(loincClassConcept);
        //classPatternFields.add(loincClassType);
        for (String pattern : requiredPatterns) {
            UUIDUtility uuidUtility = new UUIDUtility();
            PublicId patternPublicId = PublicIds.of(uuidUtility.createUUID(pattern));
            int patternNid = EntityService.get().nidForPublicId(patternPublicId);
            PublicId referencedComponentPublicID = loincConcept.publicId();
            int referencedComponentNid = EntityService.get().nidForPublicId(referencedComponentPublicID);
            PublicId semantic = PublicIds.singleSemanticId(patternPublicId, referencedComponentPublicID);
            int semanticNid = EntityService.get().nidForPublicId(semantic);
            UUID primordialUUID = semantic.asUuidArray()[0];
            int stampNid = EntityService.get().nidForPublicId(starterData.getAuthoringSTAMP());

            writeSemantic(semanticNid, primordialUUID, patternNid, referencedComponentNid, stampNid, classPatternFields);
        }

    }

    private void writeSemantic(int semanticNid, UUID primordialUUID, int patternNid, int referencedComponentNid, int stampNid, MutableList<Object> lidrRecordFields) {
        /************
         * Below: Creates the semantic with one version and write it to the database
         */
        //Create empty version list
        RecordListBuilder<SemanticVersionRecord> versions = RecordListBuilder.make();

        //Create Semantic Chronology
        SemanticRecord semanticRecord = SemanticRecordBuilder.builder()
                .nid(semanticNid)
                .leastSignificantBits(primordialUUID.getLeastSignificantBits())
                .mostSignificantBits(primordialUUID.getMostSignificantBits())
                .additionalUuidLongs(null)
                .patternNid(patternNid)
                .referencedComponentNid(referencedComponentNid)
                .versions(versions.toImmutable())
                .build();

        //Create Semantic Version
        SemanticVersionRecord semanticVersionRecord = SemanticVersionRecordBuilder.builder()
                .chronology(semanticRecord)
                .stampNid(stampNid)
                .fieldValues(lidrRecordFields.toImmutable())
                .build();

        versions.add(semanticVersionRecord);

        //Rebuild the Semantic with the now populated version data
        SemanticEntity<? extends SemanticEntityVersion> semanticEntity = SemanticRecordBuilder
                .builder(semanticRecord)
                .versions(versions.toImmutable()).build();
        EntityService.get().putEntity(semanticEntity);
        this.semanticsCreated++;
    }

    private String getSynonym(String[] data) {
        String synonym;
        if (String.valueOf(data[SYNOMYM_INDEX]).isEmpty() || String.valueOf(data[SYNOMYM_INDEX]).isBlank()) {
            synonym = data[FQN_INDEX];
        } else {
            synonym = data[SYNOMYM_INDEX];
        }
        return synonym;
    }

    private void buildLoincPatterns(StarterData starterData, UUIDUtility uuidUtility) {
        starterData.pattern(EntityProxy.Pattern.make(LOINC_TRIAL_STATUS_PATTERN, uuidUtility.createUUID(LOINC_TRIAL_STATUS_PATTERN)))
                .meaning(fqnToConceptHashMap.get("Trial Status"))
                .purpose(TinkarTerm.STATUS_VALUE)
                .build();

        starterData.pattern(EntityProxy.Pattern.make(LOINC_DISCOURAGED_STATUS_PATTERN, uuidUtility.createUUID(LOINC_DISCOURAGED_STATUS_PATTERN)))
                .meaning(fqnToConceptHashMap.get("Discouraged Status"))
                .purpose(TinkarTerm.STATUS_VALUE)
                .build();

        starterData.pattern(EntityProxy.Pattern.make(LOINC_CLASS_PATTERN, uuidUtility.createUUID(LOINC_CLASS_PATTERN)))
                .meaning(fqnToConceptHashMap.get("LOINC Class"))
                .purpose(fqnToConceptHashMap.get("LOINC Class"))
                .fieldDefinition(
                        fqnToConceptHashMap.get("LOINC Class"),
                        fqnToConceptHashMap.get("LOINC Class"),
                        TinkarTerm.CONCEPT_TYPE)
                .fieldDefinition(
                        fqnToConceptHashMap.get("LOINC ClassType"),
                        fqnToConceptHashMap.get("LOINC ClassType"),
                        TinkarTerm.STRING)
                .build();

        starterData.pattern(EntityProxy.Pattern.make(EXAMPLE_UCUM_UNITS_PATTERN, uuidUtility.createUUID(EXAMPLE_UCUM_UNITS_PATTERN)))
                .meaning(fqnToConceptHashMap.get("Example Units (UCUM)"))
                .purpose(fqnToConceptHashMap.get("Example Units (UCUM)"))
                .fieldDefinition(
                        fqnToConceptHashMap.get("Example Units (UCUM)"),
                        fqnToConceptHashMap.get("Example Units (UCUM)"),
                        TinkarTerm.STRING)
                .build();

        starterData.pattern(EntityProxy.Pattern.make(TEST_REPORTABLE_MEMBERSHIP_PATTERN, uuidUtility.createUUID(TEST_REPORTABLE_MEMBERSHIP_PATTERN)))
                .meaning(fqnToConceptHashMap.get("Test Reportable"))
                .purpose(TinkarTerm.MEMBERSHIP_SEMANTIC)
                .build();

        starterData.pattern(EntityProxy.Pattern.make(TEST_SUBSET_MEMBERSHIP_PATTERN, uuidUtility.createUUID(TEST_SUBSET_MEMBERSHIP_PATTERN)))
                .meaning(fqnToConceptHashMap.get("Test Subset"))
                .purpose(TinkarTerm.MEMBERSHIP_SEMANTIC)
                .build();

        starterData.pattern(EntityProxy.Pattern.make(TEST_ORDERABLE_PATTERN, uuidUtility.createUUID(TEST_ORDERABLE_PATTERN)))
                .meaning(fqnToConceptHashMap.get("Test Orderable"))
                .purpose(TinkarTerm.MEMBERSHIP_SEMANTIC)
                .build();
    }

    private void exportStarterData() {
        ExportEntitiesController exportEntitiesController = new ExportEntitiesController();
        try {
            exportEntitiesController.export(PB_EXPORT_DATA).get();
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }


    private void processHeaderLine(File datafile) throws IOException {
        try (Stream<String> headerLines = Files.lines(datafile.toPath())) {
            Optional<String> headerRow = headerLines.findFirst();
            headerRow.ifPresent((x) -> processHeaders(x));
        } catch (IOException e) {
            throw new IOException("Unable to process csv file.");
        }
    }

    public void processLoincPartCSVfile() {
        try {
            processHeaderLine(loincPartCSVFile);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try (Stream<String> lines = Files.lines(loincPartCSVFile.toPath())) {
            lines.skip(1) //skip first line, i.e. header line
                    .map(row -> row.split(REGEX_LINEDATA, -1))
                    .forEach(data -> {

                        EntityProxy.Concept newConcept = EntityProxy.Concept.make(data[CONCEPT_INDEX], UUID.nameUUIDFromBytes(data[CONCEPT_INDEX].getBytes()));
                        session.composeConcept(newConcept)
                                .with(new FullyQualifiedName(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, data[FQN_INDEX], TinkarTerm.DESCRIPTION_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Synonym(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, data[SYNOMYM_INDEX], TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)))
                                .with(new Definition(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.ENGLISH_LANGUAGE, data[DEFINITION_INDEX], TinkarTerm.DESCRIPTION_NOT_CASE_SENSITIVE)
                                        .with(new USEnglishDialect(EntityProxy.Semantic.make(PublicIds.newRandom()), TinkarTerm.PREFERRED)));

                    });


        } catch (IOException e) {
            throw new RuntimeException(e);
        }


    }


    public void stopIngest() {
        COMPOSER_SESSION_MANAGER.closeSession(session);
        PrimitiveData.stop();
    }


    public static long timestampToEpochSeconds(String effectiveTime) {
        long epochTime;
        try {
            epochTime = new SimpleDateFormat("yyyyMMdd").parse(effectiveTime).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
        return epochTime;
    }


}
