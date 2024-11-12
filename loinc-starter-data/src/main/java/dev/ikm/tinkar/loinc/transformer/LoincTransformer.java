package dev.ikm.tinkar.loinc.transformer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class LoincTransformer {

    private static final Logger LOG = LoggerFactory.getLogger(LoincTransformer.class);

    public LoincTransformer(File starterDataFile, File partFile) {

    }

    public static void main(String[] args) {
        if (args.length < 2) {
            LOG.info("Usage: LoincTransformer <starter data csv> <loinc part csv>");
            System.exit(0);
        }
        LoincTransformer transformer = new LoincTransformer(new File(args[0]), new File(args[1]));

    }
}
