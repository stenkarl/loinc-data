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


import com.google.protobuf.Timestamp;

import java.util.UUID;

//import static dev.ikm.loinc.transformer.util.TinkarizerUtility.getNamespacedUUIDForText;
import static dev.ikm.tinkar.loinc.starterdata.TinkarizerUtility.getNamespacedUUIDForText;

public class Bindings {

    public static final ConceptFacade LOINC_NAMESPACE = new ConceptFacade(UUID.fromString("48b004d4-6457-4648-8d58-e3287126d96b"), "LOINC_NAMESAPCE");
    public static final ConceptFacade DESCRIPTION_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Description Pattern"), "Description Pattern");
    public static final ConceptFacade RANK_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Rank Pattern"), "Rank Pattern");
    public static final ConceptFacade MEMBERSHIP_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Membership Pattern"), "Membership Pattern");
    public static final ConceptFacade PROVENANCE_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Provenance Pattern"), "Provenance Pattern");
    public static final ConceptFacade IDENTIFIER_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Identifier Pattern"), "Identifier Pattern");
    public static final ConceptFacade LOINC_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Loinc Pattern"), "Loinc Pattern");
    public static final ConceptFacade LOINC_GROUP_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Loinc Group Pattern"), "Loinc Group Pattern");
    public static final ConceptFacade FORMULA_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Formula Pattern"), "Formula Pattern");
    public static final ConceptFacade OPEN_SOURCE_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Open Source"), "Open Source");

    public static final ConceptFacade RANK_COMMON_ORDER_PATTERN = new ConceptFacade(getNamespacedUUIDForText("Rank Common Order Pattern"), "Rank Common Order Pattern");



    // Concepts
    public static final ConceptFacade ENGLISH_LANGUAGE_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("English Language"), "English Language");
    public static final ConceptFacade NOT_CASE_SIGNIFICANT_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("Not Case Significant"), "Not Case Significant");
    public static final ConceptFacade FULLY_QUALIFIED_NAME_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("Fully Qualified Name"), "Fully Qualified Name");
    public static final ConceptFacade LOINC_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("LOINC"), "LOINC");
    public static final ConceptFacade DEFINITION_DESCRIPTION_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("DefinitionDescription"), "DefinitionDescription");
    public static final ConceptFacade SYNONYM_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("Synonym"), "Synonym");
    public static final ConceptFacade DISPLAY_NAME_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("DisplayName"), "DisplayName");
    public static final ConceptFacade LOINC_AUTHOR_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("Deloitte User"), "Deloitte User");
    public static final ConceptFacade LOINC_MODULE_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("LOINC Starter Data Module"), "LOINC Starter Data Module");
    public static final ConceptFacade DEVELOPMENT_PATH_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("Development Path"), "Development Path");
    public static final ConceptFacade LONG_COMMON_NAME_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("LONG_COMMON_NAME"), "LONG_COMMON_NAME");
    public static final ConceptFacade CONSUMER_NAME_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("CONSUMER_NAME"), "CONSUMER_NAME");
    public static final ConceptFacade FORMAL_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("Formal"), "Formal");
    public static final ConceptFacade SHORTNAME_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("SHORTNAME"), "SHORTNAME");
    public static final ConceptFacade STATUS_TEXT_CONCEPT = new ConceptFacade(getNamespacedUUIDForText("STATUS_TEXT"), "STATUS_TEXT");
    public static final Timestamp TIME_STAMP = Timestamp.newBuilder().setSeconds(Long.parseLong("1678570411808")).build();

}

