package gov.nasa.podaac.forge;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.google.gson.*;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.regex.Matcher;

import org.everit.json.schema.Schema;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.json.JSONArray;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.any;

public class FootprintHandlerTest {
    static String granuleFilePath;
    static String cfgFilePath;
    static String outputFootprintFilePath;
    static String inputMessageStr;

    @Test
    public void testGetSourceBucketAndKey() {
        FootprintHandler footprintHandler = new FootprintHandler();
        Matcher m = footprintHandler.getSourceBucketAndKey("s3://public-bucket/collection-name/granule_id.nc");
        if (m.find()) {
            assertEquals(m.group(1), "public-bucket");
            assertEquals(m.group(2), "collection-name/granule_id.nc");
        }
    }

    @Test
    public void testPerformFunction() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File inputJsonFile = new File(classLoader.getResource("input.json").getFile());
        inputMessageStr = new String(Files.readAllBytes(inputJsonFile.toPath()));
        File granuleFile = new File(classLoader.getResource("20200101152000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc").getFile());
        File configFile = new File(classLoader.getResource("MODIS_A-JPL-L2P-v2019.0.cfg").getFile());
        File outputFile = new File(classLoader.getResource("footprint.txt").getFile());
        granuleFilePath = granuleFile.getAbsolutePath();
        cfgFilePath = configFile.getAbsolutePath();
        outputFootprintFilePath = outputFile.getAbsolutePath();

        FootprintHandler footprintHandler = new FootprintHandler();
        FootprintHandler spyFootprintHandler = Mockito.spy(footprintHandler);
        final AmazonS3 amazonS3 = Mockito.spy(AmazonS3.class);
        Mockito.doReturn(null).when(amazonS3).getObject(any(GetObjectRequest.class), any(File.class));
        Mockito.mock(GetObjectRequest.class);  // mock the GetObjectRequest's constructor

        Mockito.doReturn("TEST")
                .when(spyFootprintHandler)
                .getDatasetConfigBucketName();
        Mockito.doReturn("TEST")
                .when(spyFootprintHandler)
                .getDatasetConfigDirectory();
        Mockito.doReturn(null)
                .when(spyFootprintHandler)
                .getDatasetConfigURL();
        Mockito.doReturn("/tmp/UUID-222333/granule_file.nc")
                .when(spyFootprintHandler)
                .download(anyString(), anyString(), anyString());
        Mockito.doReturn("s3://public-bucket/collection_name/granule_id_footprint.txt")
                .when(spyFootprintHandler)
                .upload(any(), any(), any(File.class));
        Mockito.doReturn(granuleFilePath).when(spyFootprintHandler)
                .getGranuleFile(anyString(), anyString(), anyString(), anyString());
        Mockito.doReturn(cfgFilePath)
                .when(spyFootprintHandler)
                .getDatasetConfigFile(any(), any(), anyString(), anyString());
        Mockito.doReturn(outputFootprintFilePath).when(spyFootprintHandler)
                .createOutputFootprintFile(anyString());
        Mockito.doNothing()
                .when(spyFootprintHandler)
                .clean();

        String outputString = spyFootprintHandler.PerformFunction(inputMessageStr, null);
        JsonElement jsonElement = new JsonParser().parse(outputString);
        JsonObject outputKey = jsonElement.getAsJsonObject();
        JsonArray granules = outputKey.getAsJsonObject("input").getAsJsonArray("granules");
        JsonObject granule = granules.get(0).getAsJsonObject();
        String granuleId = granule.get("granuleId").getAsString();

        JsonArray files = granule.get("files").getAsJsonArray();
        boolean foundFPItem = false;
        for(int i =0; i< files.size(); i++) {
            if(ObjectUtils.allNotNull(files.get(i).getAsJsonObject(), files.get(i).getAsJsonObject().get("fileName")) &&
                    StringUtils.endsWith(files.get(i).getAsJsonObject().get("fileName").getAsString(), ".fp")
            ) {
                foundFPItem = true;
            }
        }
        assertEquals("L2_HR_LAKE_SP_product_0001-of-0050", granuleId);
        assert(foundFPItem);
    }

    @Test
    public void testPerformFunctionURL() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File inputJsonFile = new File(classLoader.getResource("input.json").getFile());
        inputMessageStr = new String(Files.readAllBytes(inputJsonFile.toPath()));
        File granuleFile = new File(classLoader.getResource("20200101152000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc").getFile());
        File configFile = new File(classLoader.getResource("MODIS_A-JPL-L2P-v2019.0.cfg").getFile());
        File outputFile = new File(classLoader.getResource("footprint.txt").getFile());
        granuleFilePath = granuleFile.getAbsolutePath();
        cfgFilePath = configFile.getAbsolutePath();
        outputFootprintFilePath = outputFile.getAbsolutePath();

        FootprintHandler footprintHandler = new FootprintHandler();
        FootprintHandler spyFootprintHandler = Mockito.spy(footprintHandler);
        final AmazonS3 amazonS3 = Mockito.spy(AmazonS3.class);
        Mockito.doReturn(null).when(amazonS3).getObject(any(GetObjectRequest.class), any(File.class));
        Mockito.mock(GetObjectRequest.class);  // mock the GetObjectRequest's constructor

        Mockito.doReturn(null)
                .when(spyFootprintHandler)
                .getDatasetConfigBucketName();
        Mockito.doReturn(null)
                .when(spyFootprintHandler)
                .getDatasetConfigDirectory();
        Mockito.doReturn("TEST")
                .when(spyFootprintHandler)
                .getDatasetConfigURL();
        Mockito.doReturn(cfgFilePath)
                .when(spyFootprintHandler)
                .downloadFromURL(anyString(), anyString(), anyString());
        Mockito.doReturn("/tmp/UUID-222333/granule_file.nc")
                .when(spyFootprintHandler)
                .download(anyString(), anyString(), anyString());
        Mockito.doReturn("s3://public-bucket/collection_name/granule_id_footprint.txt")
                .when(spyFootprintHandler)
                .upload(any(), any(), any(File.class));
        Mockito.doReturn(granuleFilePath).when(spyFootprintHandler)
                .getGranuleFile(anyString(), anyString(), anyString(), anyString());
        Mockito.doReturn(cfgFilePath)
                .when(spyFootprintHandler)
                .getDatasetConfigFile(any(), any(), anyString(), anyString());
        Mockito.doReturn(outputFootprintFilePath).when(spyFootprintHandler)
                .createOutputFootprintFile(anyString());
        Mockito.doNothing()
                .when(spyFootprintHandler)
                .clean();

        String outputString = spyFootprintHandler.PerformFunction(inputMessageStr, null);
        JsonElement jsonElement = new JsonParser().parse(outputString);
        JsonObject outputKey = jsonElement.getAsJsonObject();
        JsonArray granules = outputKey.getAsJsonObject("input").getAsJsonArray("granules");
        JsonObject granule = granules.get(0).getAsJsonObject();
        String granuleId = granule.get("granuleId").getAsString();

        JsonArray files = granule.get("files").getAsJsonArray();
        boolean foundFPItem = false;
        for(int i =0; i< files.size(); i++) {
            if(ObjectUtils.allNotNull(files.get(i).getAsJsonObject(), files.get(i).getAsJsonObject().get("fileName")) &&
                    StringUtils.endsWith(files.get(i).getAsJsonObject().get("fileName").getAsString(), ".fp")
            ) {
                foundFPItem = true;
            }
        }

        JSONObject jsonSchema = new JSONObject(new JSONTokener(classLoader.getResourceAsStream("schema.json")));
        Schema schema = SchemaLoader.load(jsonSchema);
        schema.validate(new JSONArray(granule.get("files").toString()));
        
        assertEquals("L2_HR_LAKE_SP_product_0001-of-0050", granuleId);
        assert(foundFPItem);
    }


    @Test
    public void testPerformFunctionNoEnv() throws Exception {
        ClassLoader classLoader = getClass().getClassLoader();
        File inputJsonFile = new File(classLoader.getResource("input.json").getFile());
        inputMessageStr = new String(Files.readAllBytes(inputJsonFile.toPath()));
        File granuleFile = new File(classLoader.getResource("20200101152000-JPL-L2P_GHRSST-SSTskin-MODIS_A-D-v02.0-fv01.0.nc").getFile());
        File configFile = new File(classLoader.getResource("MODIS_A-JPL-L2P-v2019.0.cfg").getFile());
        File outputFile = new File(classLoader.getResource("footprint.txt").getFile());
        granuleFilePath = granuleFile.getAbsolutePath();
        cfgFilePath = configFile.getAbsolutePath();
        outputFootprintFilePath = outputFile.getAbsolutePath();

        FootprintHandler footprintHandler = new FootprintHandler();
        FootprintHandler spyFootprintHandler = Mockito.spy(footprintHandler);
        final AmazonS3 amazonS3 = Mockito.spy(AmazonS3.class);
        Mockito.doReturn(null).when(amazonS3).getObject(any(GetObjectRequest.class), any(File.class));
        Mockito.mock(GetObjectRequest.class);  // mock the GetObjectRequest's constructor

        Mockito.doReturn(null)
                .when(spyFootprintHandler)
                .getDatasetConfigBucketName();
        Mockito.doReturn(null)
                .when(spyFootprintHandler)
                .getDatasetConfigDirectory();
        Mockito.doReturn(null)
                .when(spyFootprintHandler)
                .getDatasetConfigURL();
        Mockito.doReturn(cfgFilePath)
                .when(spyFootprintHandler)
                .downloadFromURL(anyString(), anyString(), anyString());
        Mockito.doReturn("/tmp/UUID-222333/granule_file.nc")
                .when(spyFootprintHandler)
                .download(anyString(), anyString(), anyString());
        Mockito.doReturn("s3://public-bucket/collection_name/granule_id_footprint.txt")
                .when(spyFootprintHandler)
                .upload(any(), any(), any(File.class));
        Mockito.doReturn(granuleFilePath).when(spyFootprintHandler)
                .getGranuleFile(anyString(), anyString(), anyString(), anyString());
        Mockito.doReturn(cfgFilePath)
                .when(spyFootprintHandler)
                .getDatasetConfigFile(any(), any(), anyString(), anyString());
        Mockito.doReturn(outputFootprintFilePath).when(spyFootprintHandler)
                .createOutputFootprintFile(anyString());
        Mockito.doNothing()
                .when(spyFootprintHandler)
                .clean();

        boolean thrown = false;

        try{
            String outputString = spyFootprintHandler.PerformFunction(inputMessageStr, null);
        }
        catch(FootprintHandlerException ex){
            // assert that a footprint handler exception will occur
            thrown = true;
        }
        assertTrue(thrown);
    }

    /**
     * Tested java.nio.path is "/" safe when there are duplicated "/" to concatenate
     */
    @Test
    public void testJavaNIOPathConcatenation() {
        String concatedString = null;
        concatedString = Paths.get("/tmp", "collection_name/asdfasdf123").toString();
        assertEquals(concatedString, "/tmp/collection_name/asdfasdf123");
        concatedString = Paths.get("/tmp", "/collection_name/asdfasdf123").toString();
        assertEquals(concatedString, "/tmp/collection_name/asdfasdf123");
        concatedString = Paths.get("/tmp", "collection_name/asdfasdf123/collection_name.cfg").toString();
        assertEquals(concatedString, "/tmp/collection_name/asdfasdf123/collection_name.cfg");
        concatedString = Paths.get("/tmp", "/collection_name/asdfasdf123/collection_name.cfg").toString();
        assertEquals(concatedString, "/tmp/collection_name/asdfasdf123/collection_name.cfg");
    }
}
