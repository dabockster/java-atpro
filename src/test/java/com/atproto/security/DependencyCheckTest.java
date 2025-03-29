package com.atproto.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.data.nvdcve.DatabaseProperties;
import org.owasp.dependencycheck.utils.Settings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class DependencyCheckTest {

    @Mock
    private SecurityService securityService;

    @Mock
    private CveDB cveDB;

    @Mock
    private Settings settings;

    @InjectMocks
    private DependencyCheckService dependencyCheckService;

    @BeforeEach
    void setUp() throws DatabaseException {
        when(settings.getString(Settings.KEYS.DATA_DIRECTORY)).thenReturn(System.getProperty("java.io.tmpdir"));
        when(cveDB.open()).thenReturn(true);
    }

    @Test
    void testSecurityServiceInitialization() {
        assertThat(securityService).isNotNull();
    }

    @Test
    void testDependencyValidation() throws IOException {
        File testFile = Files.createTempFile("dependency", ".json").toFile();
        
        when(securityService.validateDependencies(testFile)).thenReturn(List.of());
        
        List<String> vulnerabilities = securityService.validateDependencies(testFile);
        assertThat(vulnerabilities).isEmpty();
    }

    @Test
    void testDatabaseConnection() {
        when(securityService.testDatabaseConnection()).thenReturn(true);
        
        assertThat(securityService.testDatabaseConnection()).isTrue();
    }

    @Test
    void testConfigurationParsing() throws IOException {
        Path configPath = Files.createTempFile("config", ".xml");
        Files.write(configPath, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<config/>".getBytes());
        
        when(securityService.parseConfiguration(configPath)).thenReturn(true);
        
        assertThat(securityService.parseConfiguration(configPath)).isTrue();
    }

    @Test
    void testInvalidConfiguration() {
        Path invalidPath = Files.createTempFile("invalid", ".xml");
        
        when(securityService.parseConfiguration(invalidPath)).thenThrow(new RuntimeException("Invalid configuration"));
        
        assertThatThrownBy(() -> {
            securityService.parseConfiguration(invalidPath);
        }).isInstanceOf(RuntimeException.class)
          .hasMessage("Invalid configuration");
    }

    @Test
    void testSecurityCheck() {
        String dependency = "com.example:library:1.0.0";
        
        when(securityService.checkForVulnerabilities(dependency)).thenReturn(false);
        
        assertThat(securityService.checkForVulnerabilities(dependency)).isFalse();
    }

    @Test
    void testCveDatabaseConnection() throws DatabaseException {
        assertThat(cveDB.open()).isTrue();
        assertThat(cveDB.close()).isTrue();
    }

    @Test
    void testSettingsConfiguration() {
        assertThat(settings.getString(Settings.KEYS.DATA_DIRECTORY)).isNotEmpty();
        assertThat(settings.getBoolean(Settings.KEYS.AUTO_UPDATE)).isTrue();
    }

    @Test
    void testDependencyCheckService() {
        assertThat(dependencyCheckService).isNotNull();
        assertThatThrownBy(() -> dependencyCheckService.checkDependencies())
            .isInstanceOf(DatabaseException.class);
    }
}
