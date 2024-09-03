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
package dev.ikm.tinkar.loinc.test;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.common.util.io.FileUtil;
import dev.ikm.tinkar.composer.Composer;


import java.io.File;
import java.io.IOException;
import java.util.function.Function;

import dev.ikm.tinkar.entity.EntityCountSummary;
import dev.ikm.tinkar.entity.load.LoadEntitiesFromProtobufFile;
import dev.ikm.tinkar.loinc.starterdata.LoincStarterData;
import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TestLoincStarterData  {

    private static final Logger LOG = LoggerFactory.getLogger(TestLoincStarterData.class.getSimpleName());

    public static final Function<String, File> createFilePathInTarget = (pathName) -> new File("%s/target/%s".formatted(System.getProperty("user.dir"), pathName));
    public static final File PB_STARTER_DATA = createFilePathInTarget.apply("data/tinkar-starter-data-1.0.0-pb.zip");
    public static final File DATASTORE = createFilePathInTarget.apply("generated-data/" + TestLoincStarterData.class.getSimpleName());
    public final Composer COMPOSER_SESSION_MANAGER = new Composer("LOINC Composer");

    public static LoincStarterData loincStarterData;
    @BeforeAll
    public void initialize()  {

        File loincStarterDataCSVfile  = new File(System.getProperty("user.dir") +"/src/main/resources/LOINC_Starter_Data.csv");
        File loincPartCSVfile  = new File(System.getProperty("user.dir") +"/src/main/resources/Part.csv");
        File loincConceptFile = new File(System.getProperty("user.dir") + "/src/main/resources/Loinc.csv");
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, DATASTORE);
        FileUtil.recursiveDelete(DATASTORE);
        PrimitiveData.selectControllerByName("Open SpinedArrayStore");
        PrimitiveData.start();
        LoadEntitiesFromProtobufFile loadProto = new LoadEntitiesFromProtobufFile(PB_STARTER_DATA);
        EntityCountSummary count = loadProto.compute();
        LOG.info(count + " entitles loaded from file: " + loadProto.summarize() + "\n\n");

        loincStarterData = new LoincStarterData(loincStarterDataCSVfile,loincPartCSVfile);
        loincStarterData.setDataStore(DATASTORE);
        loincStarterData.setLoincConceptFile(loincConceptFile);


    }

    @Test
    public void parseStarterDataCSVFile() throws IOException {
        System.out.println(" TESTING");
        loincStarterData.processLoincStarterDataCSVFile();
    }

    @AfterAll
    public void writeDataFromStarterDataObject()
    {
        System.out.println("End of tests");
        //COMPOSER_SESSION_MANAGER.closeAllSessions();
    }

    @Test
    @Disabled
    public void loadLoincData(){
        loincStarterData.processLoincPartCSVfile();
    }
}
