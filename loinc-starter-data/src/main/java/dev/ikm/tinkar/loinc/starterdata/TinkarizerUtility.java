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

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.UUIDUtil;
import com.google.protobuf.ByteString;
import dev.ikm.tinkar.schema.PublicId;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

import static dev.ikm.tinkar.loinc.starterdata.Bindings.LOINC_NAMESPACE;
import static dev.ikm.tinkar.loinc.starterdata.Bindings.TIME_STAMP;
import static dev.ikm.tinkar.loinc.starterdata.LoincConstants.*;


public class TinkarizerUtility {


    private static Map<String, List<String>> dataMap;
    private static int totalRows;
    private static int totalColumns;

    public static Map<String, List<String>> getDataMap() {
        return dataMap;
    }

    public static int getTotalRows() {
        return totalRows;
    }

    public static int getTotalColumns() {
        return totalColumns;
    }

    public static synchronized void loadCSVFileData(Path path) throws IOException {
        try (Stream<String> headerLines = Files.lines(path)) {
            Optional<String> headerRow = headerLines.findFirst();
            headerRow.ifPresent((x) -> processHeaders(x));
        } catch (IOException e) {
            throw new IOException("Unable to process csv file.");
        }
        try (Stream<String> dataLines = Files.lines(path)) {
            dataLines.skip(1).forEach(linedata -> loadLineData(linedata));
        } catch (IOException e) {
            throw new IOException("Unable to process csv file.");
        }

    }

    /**
     * Utility function to parse the CSV file for headers.
     * This method loads the headers for the csv file in hashMap.
     * Each record in the map is the header value which acts as a key.
     * The ArrayList is the list of values actual data values.
     * {@code @Param} lineData
     * {@code @Return} dataMap (Map<String, List<String>)
     */
    public static synchronized void processHeaders(String lineData) {
        //There is BOM data in the CSV file which needs to be replaced. Hence the below statement.
        String formmatedLineData = lineData.replaceAll("\uFEFF", "");
        String[] columnHeaders = formmatedLineData.split(",");
        dataMap = new LinkedHashMap<>();
        for (String columnHeader : columnHeaders) {
            dataMap.put(columnHeader.trim(), new ArrayList<>());
        }
        totalColumns = columnHeaders.length;
        totalRows = 0;
    }

    public static synchronized void loadLineData(String lineData) {
        if (dataMap == null) {
            throw new ExceptionInInitializerError("The Header data and hence the Map is null and not initialized.");
        }
        String[] columnValues = lineData.split(REGEX_LINEDATA, -1);
        List<String> columnsHeaders = new ArrayList<>(dataMap.keySet());
        for (int i = 0; i < columnValues.length; i++) {
            List<String> values = dataMap.get(columnsHeaders.get(i));
            values.add(columnValues[i]);
        }
        totalRows++;
    }

    /**
     * This method returns the string representation of the row data.
     *
     * @Param int
     * @Returns String
     **/
    public static synchronized String toString(int rowNumber) {
        List<String> columns = new ArrayList<>(dataMap.keySet());
        StringBuilder sb = new StringBuilder();
        return TinkarizerUtility.toString(rowNumber, (String[]) columns.toArray());
    }

    /**
     * This method returns the string representation of the row data for the requested columns.
     *
     * @Param int
     * @Param String []
     * @Returns String
     **/
    public static synchronized String toString(int rowNumber, String... columns) {
        String string = "";
        for (String column : columns) {
            List<String> values = dataMap.get(column);
            string = String.join("", values.get(rowNumber));
        }
        return string;
    }

    public static synchronized String getLoincNum(int rowNumber) {
        return dataMap.get(LOINC_NUM).get(rowNumber);
    }

    public static synchronized String getStatusString(int rowNumber) {
        return dataMap.get(STATUS).get(rowNumber).equalsIgnoreCase(DEPRECATED) ? INACTIVE : ACTIVE;
    }

    public static synchronized UUID createStampUUID(int lineDataRow, String... strings) {
        String tempString = generateStampString(lineDataRow);
        if (strings != null && strings.length > 0 && strings[0] != null) {
            String string = Arrays.toString(strings);
            tempString = tempString + string.substring(1, string.length() - 1);
        }
        return getNamespacedUUIDForText(tempString);
    }

    public static synchronized String generateStampString(int lineDataRow) {
        return new StringBuilder()
                .append(TinkarizerUtility.getStatusString(lineDataRow))
                .append(TIME_STAMP)
                .append(LOINC_AUTHOR)
                .append(LOINC_MODULE)
                .append(DEVELOPMENT_PATH).toString();
    }

    public static synchronized UUID getNamespacedUUIDForText(String... strings) {
        String text = String.join("", strings);
        return getNamespacedUUIDForText(text);
    }

    public static synchronized UUID getNamespacedUUIDForText(String text) {
        return Generators.nameBasedGenerator(LOINC_NAMESPACE.uuid()).generate(text);
    }

//    public static synchronized PublicId getPBpublicId(UUID uuid) {
//        byte[] publicId = UUIDUtil.asByteArray(uuid);
//        return PublicId.newBuilder().addUuids(String.valueOf(ByteString.copyFrom(publicId))).build();
//    }
//
//    public static synchronized PublicId getPBpublicId(int lindDataRow, String... strings) {
//        byte[] publicId = UUIDUtil.asByteArray(createStampUUID(lindDataRow, strings));
//        return PublicId.newBuilder().addUuids(String.valueOf(ByteString.copyFrom(publicId))).build();
//    }

}