package dev.ikm.tinkar.loinc.transformer;

import dev.ikm.tinkar.common.service.CachingService;
import dev.ikm.tinkar.common.service.PrimitiveData;
import dev.ikm.tinkar.common.service.ServiceKeys;
import dev.ikm.tinkar.common.service.ServiceProperties;
import dev.ikm.tinkar.composer.Composer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LoincTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(LoincTransformer.class);

    private final File datastore;
    private final File partFile;

    private final Composer composer = new Composer("LOINC");

    public LoincTransformer(File datastore, File partFile) {
        this.datastore = datastore;
        this.partFile = partFile;
    }

    private void init() {
        LOG.info("Starting database");
        LOG.info("Loading data from {}", datastore.getAbsolutePath());
        CachingService.clearAll();
        ServiceProperties.set(ServiceKeys.DATA_STORE_ROOT, datastore);
        PrimitiveData.selectControllerByName("Open SpinedArrayStore");
        PrimitiveData.start();
    }

    private void cleanup() {
        PrimitiveData.stop();
    }

    private void transform() {
        new LoincStarterData(composer).transform();
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            LOG.info("Usage: LoincTransformer <spined array datastore> <loinc part csv>");
            System.exit(0);
        }
        LoincTransformer transformer = new LoincTransformer(new File(args[0]), new File(args[1]));
        transformer.init();
        transformer.transform();
        transformer.cleanup();
    }
}
