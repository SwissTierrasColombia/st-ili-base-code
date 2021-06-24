package com.ai.st.microservice.ili.services;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ai.st.microservice.ili.business.ConceptBusiness;
import com.ai.st.microservice.ili.business.QueryTypeBusiness;
import com.ai.st.microservice.ili.drivers.PostgresDriver;
import com.ai.st.microservice.ili.dto.IntegrationStatDto;
import com.ai.st.microservice.ili.entities.QueryEntity;
import com.ai.st.microservice.ili.entities.VersionConceptEntity;
import com.ai.st.microservice.ili.entities.VersionEntity;

import ch.ehi.ili2db.base.Ili2db;
import ch.ehi.ili2db.gui.Config;

@Service
public class Ili2pgService {

    @Autowired
    private IVersionService versionService;

    private final Logger log = LoggerFactory.getLogger(this.getClass());

    private static final String MODELS_INTERLIS_CH = "http://models.interlis.ch";

    public Config getDefaultConfig() {

        Config config = new Config();
        new ch.ehi.ili2pg.PgMain().initConfig(config);

        config.setCreateFk(Config.CREATE_FK_YES); // --createFk
        config.setCatalogueRefTrafo(Config.CATALOGUE_REF_TRAFO_COALESCE); // --coalesceCatalogueRef
        config.setMultiSurfaceTrafo(Config.MULTISURFACE_TRAFO_COALESCE); // --coalesceMultiSurface
        config.setInheritanceTrafo(Config.INHERITANCE_TRAFO_SMART2); // --smart2Inheritance
        config.setSetupPgExt(true); // --setupPgExt
        config.setMultiLineTrafo(Config.MULTILINE_TRAFO_COALESCE); // --coalesceMultiLine
        config.setCreateUniqueConstraints(true); // --createUnique
        config.setBeautifyEnumDispName(Config.BEAUTIFY_ENUM_DISPNAME_UNDERSCORE); // --beautifyEnumDispName
        config.setCreateFkIdx(Config.CREATE_FKIDX_YES); // --createFkIdx
        config.setCreateEnumDefs(Config.CREATE_ENUM_DEFS_MULTI_WITH_ID); // --createEnumTabsWithId
        config.setCreateNumChecks(true); // --createNumChecks
        config.setValue(Config.CREATE_GEOM_INDEX, Config.TRUE); // --createGeomIdx
        config.setCreateMetaInfo(true); // --createMetaInfo
        Config.setStrokeArcs(config, Config.STROKE_ARCS_ENABLE); // --strokeArcs

        // config.setTidHandling(Config.TID_HANDLING_PROPERTY); // --createTidCol
        // config.setBasketHandling(Config.BASKET_HANDLING_READWRITE); //
        // --createBasketCol
        // config.setMultilingualTrafo(Config.MULTILINGUAL_TRAFO_EXPAND); //
        // --expandMultilingual

        return config;
    }

    public Boolean generateSchema(String logFileSchemaImport, String iliDirectory, String srsCode, String models,
                                  String databaseHost, String databasePort, String databaseName, String databaseSchema,
                                  String databaseUsername, String databasePassword) {

        boolean result;

        try {

            Config config = getDefaultConfig();

            config.setFunction(Config.FC_SCHEMAIMPORT); // --schemaimport
            config.setModeldir(MODELS_INTERLIS_CH + ";" + iliDirectory); // -- modeldir
            config.setDefaultSrsCode(srsCode); // --defaultSrsCode
            config.setModels(models); // --models
            config.setLogfile(logFileSchemaImport); // --log

            try {
                final Path path = Files.createTempFile("myTempFile", ".sql");

                String dataFile = "INSERT into spatial_ref_sys (\r\n" +
                        "  srid, auth_name, auth_srid, proj4text, srtext\r\n" +
                        ")\r\n" +
                        "values\r\n" +
                        "  (\r\n" +
                        "    9377,\r\n" +
                        "    'EPSG',\r\n" +
                        "    9377,\r\n" +
                        "    '+proj=tmerc +lat_0=4.0 +lon_0=-73.0 +k=0.9992 +x_0=5000000 +y_0=2000000 +ellps=GRS80 +towgs84=0,0,0,0,0,0,0 +units=m +no_defs ',\r\n" +
                        "    'PROJCRS[\"MAGNA-SIRGAS / Origen-Nacional\", BASEGEOGCRS[\"MAGNA-SIRGAS\", DATUM[\"Marco Geocentrico Nacional de Referencia\", ELLIPSOID[\"GRS 1980\",6378137,298.257222101, LENGTHUNIT[\"metre\",1]]], PRIMEM[\"Greenwich\",0, ANGLEUNIT[\"degree\",0.0174532925199433]], ID[\"EPSG\",4686]], CONVERSION[\"Colombia Transverse Mercator\", METHOD[\"Transverse Mercator\", ID[\"EPSG\",9807]], PARAMETER[\"Latitude of natural origin\",4, ANGLEUNIT[\"degree\",0.0174532925199433], ID[\"EPSG\",8801]], PARAMETER[\"Longitude of natural origin\",-73, ANGLEUNIT[\"degree\",0.0174532925199433], ID[\"EPSG\",8802]], PARAMETER[\"Scale factor at natural origin\",0.9992, SCALEUNIT[\"unity\",1], ID[\"EPSG\",8805]], PARAMETER[\"False easting\",5000000, LENGTHUNIT[\"metre\",1], ID[\"EPSG\",8806]], PARAMETER[\"False northing\",2000000, LENGTHUNIT[\"metre\",1], ID[\"EPSG\",8807]]], CS[Cartesian,2], AXIS[\"northing (N)\",north, ORDER[1], LENGTHUNIT[\"metre\",1]], AXIS[\"easting (E)\",east, ORDER[2], LENGTHUNIT[\"metre\",1]], USAGE[ SCOPE[\"unknown\"], AREA[\"Colombia\"], BBOX[-4.23,-84.77,15.51,-66.87]], ID[\"EPSG\",9377]]'\r\n" +
                        "  ) ON CONFLICT (srid) DO NOTHING;";

                // Writing data here
                byte[] buf = dataFile.getBytes();
                Files.write(path, buf);

                // Delete file on exit
                path.toFile().deleteOnExit();

                config.setPreScript(path.toFile().getAbsolutePath());

            } catch (IOException e) {
                log.error("Error executing pre script: " + e.getMessage());
            }


            config.setDburl("jdbc:postgresql://" + databaseHost + ":" + databasePort + "/" + databaseName);
            config.setDbschema(databaseSchema);
            config.setDbusr(databaseUsername);
            config.setDbpwd(databasePassword);
            config.setSkipGeometryErrors(true);

            Ili2db.readSettingsFromDb(config);
            Ili2db.run(config, null);
            result = true;
        } catch (Exception e) {
            log.error("ERROR generating schema: " + e.getMessage());
            result = false;
        }

        return result;
    }

    public Boolean import2pg(String fileXTF, String logFileSchemaImport, String logFileImport, String iliDirectory,
                             String srsCode, String models, String databaseHost, String databasePort, String databaseName,
                             String databaseSchema, String databaseUsername, String databasePassword) {

        boolean result = false;

        Boolean generateSchema = generateSchema(logFileSchemaImport, iliDirectory, srsCode, models, databaseHost,
                databasePort, databaseName, databaseSchema, databaseUsername, databasePassword);

        if (generateSchema) {

            try {

                Config config = getDefaultConfig();

                config.setFunction(Config.FC_IMPORT); // --schemaimport
                config.setModeldir(MODELS_INTERLIS_CH + ";" + iliDirectory); // -- modeldir
                config.setDefaultSrsCode(srsCode); // --defaultSrsCode
                config.setModels(models); // --models
                config.setLogfile(logFileImport); // --log
                config.setValidation(false);
                config.setSkipGeometryErrors(true);
                config.setXtffile(fileXTF);
                if (fileXTF != null && Ili2db.isItfFilename(fileXTF)) {
                    config.setItfTransferfile(true);
                }

                config.setDburl("jdbc:postgresql://" + databaseHost + ":" + databasePort + "/" + databaseName);
                config.setDbschema(databaseSchema);
                config.setDbusr(databaseUsername);
                config.setDbpwd(databasePassword);

                Ili2db.readSettingsFromDb(config);
                Ili2db.run(config, null);
                result = true;
            } catch (Exception e) {
                log.error(e.getMessage());
                result = false;
            }
        }

        return result;
    }

    public IntegrationStatDto integration(String cadastreFileXTF, String cadastreLogFileSchemaImport,
                                          String cadastreLogFileImport, String registrationFileXTF, String registrationLogFileSchemaImport,
                                          String registrationLogFileImport, String iliDirectory, String srsCode, String models, String databaseHost,
                                          String databasePort, String databaseName, String databaseSchema, String databaseUsername,
                                          String databasePassword, String modelVersion) {

        IntegrationStatDto integrationStat = new IntegrationStatDto();

        // load cadastral information
        Boolean loadCadastral = this.import2pg(cadastreFileXTF, cadastreLogFileSchemaImport, cadastreLogFileImport,
                iliDirectory, srsCode, models, databaseHost, databasePort, databaseName, databaseSchema,
                databaseUsername, databasePassword);

        log.info("Se han importado los datos de Catastro ...");

        // load registration information
        Boolean loadRegistration = this.import2pg(registrationFileXTF, registrationLogFileSchemaImport,
                registrationLogFileImport, iliDirectory, srsCode, models, databaseHost, databasePort, databaseName,
                databaseSchema, databaseUsername, databasePassword);

        log.info("Se han importado los datos de Registro ...");

        VersionEntity versionEntity = versionService.getVersionByName(modelVersion);

        log.info("Se han importado los datos de Catastro y Registro ...");

        if (loadCadastral && loadRegistration && versionEntity != null) {

            VersionConceptEntity versionConcept = versionEntity.getVersionsConcepts().stream()
                    .filter(vC -> vC.getConcept().getId().equals(ConceptBusiness.CONCEPT_INTEGRATION)).findAny()
                    .orElse(null);

            QueryEntity queryMatchIntegrationEntity = versionConcept.getQuerys().stream()
                    .filter(q -> q.getQueryType().getId().equals(QueryTypeBusiness.QUERY_TYPE_MATCH_INTEGRATION))
                    .findAny().orElse(null);

            PostgresDriver connection = new PostgresDriver();
            String urlConnection = "jdbc:postgresql://" + databaseHost + ":" + databasePort + "/" + databaseName;
            connection.connect(urlConnection, databaseUsername, databasePassword, "org.postgresql.Driver");

            log.info("Conexión establecida para iniciar integración catastro-registro ...");

            String pairingTypeId = null;
            try {

                QueryEntity queryGetPairingTypeIntegrationEntity = versionConcept.getQuerys().stream().filter(
                        q -> q.getQueryType().getId().equals(QueryTypeBusiness.QUERY_TYPE_GET_PAIRING_TYPE_INTEGRATION))
                        .findAny().orElse(null);
                String sqlObjects = queryGetPairingTypeIntegrationEntity.getQuery()
                        .replace("{pairingTypeCode}", String.valueOf(4)).replace("{dbschema}", databaseSchema);

                ResultSet resultsetObjects = connection.getResultSetFromSql(sqlObjects);

                while (resultsetObjects.next()) {
                    pairingTypeId = resultsetObjects.getString("t_id");
                }

            } catch (SQLException e) {
                log.error("Error getting pairing type I: " + e.getMessage());
                pairingTypeId = null;
            } catch (Exception e) {
                log.error("Error getting pairing type II: " + e.getMessage());
                pairingTypeId = null;
            }

            log.info("EMPAREJAMIENTO: " + pairingTypeId);

            String sqlObjects = queryMatchIntegrationEntity.getQuery().replace("{dbschema}", databaseSchema);
            ResultSet resultsetObjects = connection.getResultSetFromSql(sqlObjects);
            try {

                while (resultsetObjects.next()) {

                    String snr = resultsetObjects.getString("snr_predio_juridico");
                    String gc = resultsetObjects.getString("gc_predio_catastro");

                    QueryEntity queryInsertEntity = versionConcept.getQuerys().stream().filter(
                            q -> q.getQueryType().getId().equals(QueryTypeBusiness.QUERY_TYPE_INSERT_INTEGRATION_))
                            .findAny().orElse(null);

                    String sqlInsert = queryInsertEntity.getQuery().replace("{dbschema}", databaseSchema)
                            .replace("{cadastre}", gc).replace("{snr}", snr).replace("{pairingType}", pairingTypeId);

                    connection.insert(sqlInsert);

                }
                connection.disconnect();

                integrationStat = this.getIntegrationStats(databaseHost, databasePort, databaseName, databaseUsername,
                        databasePassword, databaseSchema, modelVersion);
                integrationStat.setStatus(true);

            } catch (SQLException e) {
                integrationStat.setStatus(false);
                connection.disconnect();
            }

        } else {
            integrationStat.setStatus(false);
        }

        return integrationStat;
    }

    public IntegrationStatDto getIntegrationStats(String databaseHost, String databasePort, String databaseName,
                                                  String databaseUsername, String databasePassword, String databaseSchema, String modelVersion) {

        IntegrationStatDto integrationStat = new IntegrationStatDto();

        VersionEntity versionEntity = versionService.getVersionByName(modelVersion);
        if (versionEntity instanceof VersionEntity) {

            VersionConceptEntity versionConcept = versionEntity.getVersionsConcepts().stream()
                    .filter(vC -> vC.getConcept().getId().equals(ConceptBusiness.CONCEPT_INTEGRATION)).findAny()
                    .orElse(null);

            PostgresDriver connection = new PostgresDriver();

            String urlConnection = "jdbc:postgresql://" + databaseHost + ":" + databasePort + "/" + databaseName;
            connection.connect(urlConnection, databaseUsername, databasePassword, "org.postgresql.Driver");

            QueryEntity queryCountSnrEntity = versionConcept.getQuerys().stream()
                    .filter(q -> q.getQueryType().getId().equals(QueryTypeBusiness.QUERY_TYPE_COUNT_SNR_INTEGRATION))
                    .findAny().orElse(null);
            String sqlCountSNR = queryCountSnrEntity.getQuery().replace("{dbschema}", databaseSchema);
            long countSNR = connection.count(sqlCountSNR);

            QueryEntity queryCountCadastreEntity = versionConcept.getQuerys().stream().filter(
                    q -> q.getQueryType().getId().equals(QueryTypeBusiness.QUERY_TYPE_COUNT_CADASTRE_INTEGRATION))
                    .findAny().orElse(null);
            String sqlCountGC = queryCountCadastreEntity.getQuery().replace("{dbschema}", databaseSchema);
            long countGC = connection.count(sqlCountGC);

            QueryEntity queryCountMatchEntity = versionConcept.getQuerys().stream()
                    .filter(q -> q.getQueryType().getId().equals(QueryTypeBusiness.QUERY_TYPE_COUNT_MATCH_INTEGRATION))
                    .findAny().orElse(null);
            String sqlCountMatch = queryCountMatchEntity.getQuery().replace("{dbschema}", databaseSchema);
            long countMatch = connection.count(sqlCountMatch);

            double percentage = 0.0;

            if (countSNR >= countGC) {
                percentage = (double) (countMatch * 100) / countSNR;
            } else {
                percentage = (double) (countMatch * 100) / countGC;
            }

            connection.disconnect();

            integrationStat.setCountGC(countGC);
            integrationStat.setCountSNR(countSNR);
            integrationStat.setCountMatch(countMatch);
            integrationStat.setPercentage(percentage);
        }

        return integrationStat;
    }

    public Boolean exportToXtf(String filePath, String logFileExport, String iliDirectory, String srsCode,
                               String models, String databaseHost, String databasePort, String databaseName, String databaseSchema,
                               String databaseUsername, String databasePassword) {

        boolean result;

        try {

            Config config = new Config();
            new ch.ehi.ili2pg.PgMain().initConfig(config);

            config.setFunction(Config.FC_EXPORT); // --schemaimport
            config.setModeldir(iliDirectory); // -- modeldir
            config.setModels(models); // --models
            config.setLogfile(logFileExport); // --log
            // config.setDefaultSrsCode(srsCode); // --defaultSrsCode
            config.setValidation(false);
            config.setSkipGeometryErrors(true);

            config.setXtffile(filePath);

            config.setDburl("jdbc:postgresql://" + databaseHost + ":" + databasePort + "/" + databaseName);
            config.setDbschema(databaseSchema);
            config.setDbusr(databaseUsername);
            config.setDbpwd(databasePassword);

            Ili2db.readSettingsFromDb(config);
            Ili2db.run(config, null);

            result = true;
        } catch (Exception e) {
            log.error("Error export to xtf: " + e.getMessage());
            result = false;
        }

        return result;
    }

}
