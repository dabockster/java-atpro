package com.atproto.security;

import org.junit.jupiter.api.Test;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.data.nvdcve.DatabaseProperties;
import org.owasp.dependencycheck.data.nvdcve.DefaultDatabase;
import org.owasp.dependencycheck.data.nvdcve.NvdCve12Parser;
import org.owasp.dependencycheck.data.nvdcve.NvdCve20Parser;
import org.owasp.dependencycheck.data.nvdcve.NvdCveParser;
import org.owasp.dependencycheck.data.nvdcve.parsers.CveParser;
import org.owasp.dependencycheck.data.nvdcve.parsers.ParseException;
import org.owasp.dependencycheck.utils.Settings;
import org.owasp.dependencycheck.utils.XmlUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class DependencyCheckTest {

    private static final String TEST_PROJECT_NAME = "atproto-java";
    private static final String TEST_PROJECT_VERSION = "0.1.0-SNAPSHOT";

    @Test
    void testDependencyCheckInitialization() throws Exception {
        Settings settings = new Settings();
        settings.setString(Settings.KEYS.PROJECT_NAME, TEST_PROJECT_NAME);
        settings.setString(Settings.KEYS.PROJECT_VERSION, TEST_PROJECT_VERSION);

        Engine engine = new Engine(settings);
        assertNotNull(engine);
    }

    @Test
    void testCveDatabase() throws Exception {
        CveDB cveDB = new DefaultDatabase();
        cveDB.initialize();
        
        assertTrue(cveDB.hasData());
        assertTrue(cveDB.getDatabaseProperties().contains(DatabaseProperties.DB_VERSION));
    }

    @Test
    void testCveParser() throws Exception {
        File testFile = Files.createTempFile("cve-test", ".json").toFile();
        
        try (NvdCveParser parser = new NvdCve20Parser()) {
            List<String> vulnerabilities = parser.parse(testFile);
            assertTrue(vulnerabilities.isEmpty()); // Empty file should have no vulnerabilities
        }
    }

    @Test
    void testXmlUtils() throws Exception {
        String xmlContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
                "<dependencies>\n" +
                "    <dependency>\n" +
                "        <groupId>com.example</groupId>\n" +
                "        <artifactId>test</artifactId>\n" +
                "        <version>1.0.0</version>\n" +
                "    </dependency>\n" +
                "</dependencies>";

        try (var writer = Files.newBufferedWriter(Paths.get("test.xml"))) {
            writer.write(xmlContent);
        }

        assertTrue(XmlUtils.validateXmlFile(Paths.get("test.xml")));
    }

    @Test
    void testDatabaseException() {
        CveDB cveDB = new DefaultDatabase();
        
        assertThrows(DatabaseException.class, () -> {
            cveDB.initialize();
            cveDB.close();
            cveDB.query("SELECT * FROM vulnerabilities");
        });
    }

    @Test
    void testParseException() {
        File invalidFile = Files.createTempFile("invalid", ".json").toFile();
        
        try (NvdCveParser parser = new NvdCve20Parser()) {
            assertThrows(ParseException.class, () -> {
                parser.parse(invalidFile);
            });
        }
    }
}
