package gov.nasa.podaac.forge;

import com.amazonaws.AmazonClientException;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.google.gson.JsonParser;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.Gson;
import cumulus_message_adapter.message_parser.AdapterLogger;
import cumulus_message_adapter.message_parser.ITask;
import cumulus_message_adapter.message_parser.MessageAdapterException;
import cumulus_message_adapter.message_parser.MessageParser;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.Level;
import ucar.ma2.InvalidRangeException;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Cumulus task for performing footprint operation on a granule.
 */
public class FootprintHandler implements ITask, RequestHandler<String, String> {
    private final String className = this.getClass().getName();
    private final String region = System.getenv("REGION");
    
    /**
     * This function is called when the lambda is invoked
     *
     * @return The result of the footprint task
     */
    public String handleRequest(String input, Context context) {
        MessageParser parser = new MessageParser();
        try {
            AdapterLogger.LogInfo(className + " handleRequest is called with message: " + input);
            return parser.RunCumulusTask(input, context, this);
        } catch (MessageAdapterException mae) {
            AdapterLogger.LogError(className + " handleRequest calling parser exception" + mae);
            throw new FootprintHandlerException("Error running task " + className, mae);
        }
    }
    
    public Level getLogLevel(){

        String logLevel = System.getenv("LOGGING_LEVEL");

        switch (logLevel) {
            case "Off": return Level.OFF;
            case "Fatal": return Level.FATAL;
            case "Error":  return Level.ERROR;
            case "Warn":  return Level.WARN;
            case "Info":  return Level.INFO;
            case "Debug":  return Level.DEBUG;
            case "Trace":  return Level.TRACE;
            case "All":  return Level.ALL;
            default: return Level.INFO;
        }
    }

    public void handleRequestStreams(InputStream inputStream, OutputStream outputStream, Context context) throws IOException, MessageAdapterException {
  
        Configurator.setRootLevel(getLogLevel());

        MessageParser parser = new MessageParser();
        String input = IOUtils.toString(inputStream, StandardCharsets.UTF_8);
        AdapterLogger.LogInfo(className + " handleRequestStreams is called with message: " + input);
        String output = null;
        try{
            output = parser.RunCumulusTask(input, context, this);
        }
        finally{
            clean();
        }
        AdapterLogger.LogInfo(className + " handleRequestStreams output: " + output);
        outputStream.write(output.getBytes(StandardCharsets.UTF_8));
    }
    
    /**
     * Footprint task executed by the Cumulus message adapter. This function wil:
     *
     * @return The input message augmented with the newly calculated footprint, and serialized as a String.
     */
    public String PerformFunction(String input, Context context) {

        String granuleFileAbsolutePath = null;
        String datasetConfigFileAbsolutePath;
        
        AdapterLogger.LogInfo(className + " entered PerformFunction with input: " + input);
        JsonElement jsonElement = new JsonParser().parse(input);
        JsonObject inputKey = jsonElement.getAsJsonObject();
        
        // Parse config values
        JsonObject config = inputKey.getAsJsonObject("config");
        String collectionName = config.getAsJsonObject("collection").get("name").getAsString();
        String executionName = config.get("execution_name").getAsString();
        
        JsonArray granules = inputKey.getAsJsonObject("input").getAsJsonArray("granules");
        JsonObject granule = granules.get(0).getAsJsonObject();
        String granuleId = granule.get("granuleId").getAsString();
        JsonArray files = granule.get("files").getAsJsonArray();
        
        String workingDir;
        try {
            workingDir = createWorkDir();
        } catch (IOException exception) {
            throw new FootprintHandlerException("Error creating temporary working directory", exception);
        }
        
        /*
        Looping through the files array to find
        - data file
        - dataset-config file
         */
        
        for (int i = 0; i < files.size(); i++) {
            String type = files.get(i).getAsJsonObject().get("type").getAsString();
            String granuleFileName = files.get(i).getAsJsonObject().get("fileName").getAsString();
            if (StringUtils.equalsAnyIgnoreCase(type, "data")) {
                String sourceBucket = files.get(i).getAsJsonObject().get("bucket").getAsString();
                String key = files.get(i).getAsJsonObject().get("key").getAsString();

                AdapterLogger.LogInfo(this.className + " trying to get granule file from bucket: " + sourceBucket +
                        " key: " + key + "to workingDir: " + workingDir + " as filename: " + granuleFileName);
                granuleFileAbsolutePath = getGranuleFile(sourceBucket, key, workingDir, granuleFileName);
                break;
            }
        }
        
        /*
         * Get dataset config file bucket name set as environment variable in LAMBDA terraform definition
         * ex:
         * CONFIG_BUCKET=my-internal
         * CONFIG_DIR = dataset-configs
         * real values maybe set in environment.tfvars
         */
        
        String datasetConfigBucketName = this.getDatasetConfigBucketName();
        String datasetConfigDirectory = this.getDatasetConfigDirectory();
        String datasetConfigURL = this.getDatasetConfigURL();

        if(datasetConfigURL != null){
            try{
                datasetConfigFileAbsolutePath = downloadFromURL(datasetConfigURL, workingDir, collectionName);
            }
            catch(IOException ioe){
                throw new FootprintHandlerException("Error when downloading configuration file", ioe);
            }
        }
        else if(datasetConfigBucketName != null && datasetConfigDirectory != null){
            datasetConfigFileAbsolutePath = getDatasetConfigFile(datasetConfigBucketName, datasetConfigDirectory,
                                                                 workingDir, collectionName);
        }
        else{
            Exception r = new NullPointerException("Configuration env is null");
            throw new FootprintHandlerException("Environment variable to get configuration files were not set", r);
        }

        /*
        Perform footprint operation
         */
        Map<String, String> footprintExtend;
        try {
            Footprinter footprinter = new Footprinter(granuleFileAbsolutePath, datasetConfigFileAbsolutePath);
            footprintExtend = footprinter.footprint();
        } catch (FootprintException | IOException | InvalidRangeException e) {
            throw new FootprintHandlerException("Error processing granule", e);
        }
        
        /*
        Store footprint operation results in output Json
         */
        JsonObject outputFPJsonObj = new JsonObject();
        AdapterLogger.LogInfo(this.className + " FOOTPRINT: " + footprintExtend.get("FOOTPRINT"));
        AdapterLogger.LogInfo(this.className + " EXTENT: " + footprintExtend.get("EXTENT"));
        outputFPJsonObj.addProperty("FOOTPRINT", footprintExtend.get("FOOTPRINT"));
        outputFPJsonObj.addProperty("EXTENT", footprintExtend.get("EXTENT"));
        String outputFPStr = new Gson().toJson(outputFPJsonObj);
        AdapterLogger.LogInfo(this.className + " footprint file content: " + outputFPStr);

        try {
            // outputFootprint file and upload to S3.
            long fileSize = outputFootprint(workingDir, collectionName, granuleId, outputFPStr, executionName);
            // clean up working directory:
            FileUtils.forceDelete(new File(workingDir));
            // build new file json object and add to files array
            JsonObject extraFileObj = createFootprintFileJsonObj(fileSize, collectionName, granuleId, executionName);
            inputKey.get("input").getAsJsonObject().get("granules").getAsJsonArray().get(0).getAsJsonObject()
            .getAsJsonArray("files").add(extraFileObj);
        } catch (IOException ioe) {
            throw new FootprintHandlerException("Error output footprint file and upload: ", ioe);
        }

        String outputStr =  new Gson().toJson(inputKey);
        AdapterLogger.LogInfo(this.className + " output string:" + outputStr);
        return outputStr;
    }

    public void clean(){
        // clean out temp directory build up from cache lambdas
        try {
            AdapterLogger.LogInfo("tmp directory before clean up");
            displayTemp();
            FileUtils.cleanDirectory(new File("/tmp/")); 
            AdapterLogger.LogInfo("tmp directory after clean up");
            displayTemp();
            AdapterLogger.LogInfo(this.className + " delete everything in /tmp dir successfully: ");
        } catch (IOException ioe) {
            AdapterLogger.LogError(this.className + " error deleting everything in tmp dir: " + ioe.getMessage());
        }
    }

    private void displayTemp(){
        File f = new File("/tmp/"); // current directory
        String[] files = f.list();
        AdapterLogger.LogInfo("tmp directory files length: " + files.length);
    }

    public String getDatasetConfigBucketName(){
        return System.getenv("CONFIG_BUCKET");
    }

    public String getDatasetConfigDirectory(){
        return System.getenv("CONFIG_DIR");
    }

    public String getDatasetConfigURL(){
        return System.getenv("CONFIG_URL");
    }

    private JsonObject createFootprintFileJsonObj(long fileSize, String collectionName, String granuleId, String executionName) {
        JsonObject file = new JsonObject();
        String bucket = System.getenv().getOrDefault("FOOTPRINT_OUTPUT_BUCKET", "");
        String out_dir = System.getenv().getOrDefault("FOOTPRINT_OUTPUT_DIR", "");
        String filepath = Paths.get(out_dir, collectionName, granuleId + "_" + executionName + ".fp").toString();
        file.addProperty("bucket", bucket);

        // filename is s3 absolute path of the file
        file.addProperty("size", fileSize);
        file.addProperty("type", "metadata");
        file.addProperty("key", filepath);
        file.addProperty("fileName", granuleId + "_" + executionName + ".fp");
        return file;
    }
    
    /**
     * Create a directory where we dump granule file, nc file and generated footprint file
     * based on UUID.randomUUID()
     *
     * @return the path to new temporary directory
     */
    private String createWorkDir() throws IOException {
        try {
            Path path = Files.createTempDirectory(Paths.get("/tmp"), "workDir");
            return path.toString();
        } catch (IOException ioe) {
            AdapterLogger.LogError(this.className + " creating working dir failed: " + ioe.getMessage());
            throw ioe;
        }
    }

    /**
     * Create footprint file *.fp in working directory and upload to a environment setup
     * env_bucket_name and env_directory.   the fp file will be finally placed under
     * s3://env_bucket_name/env_directory/collection_name/granuleId.fp.
     * The fp file will then decoded and used in MetadataAggregator lambda to modify cmr.json and
     * post the new UMM-G to CMR.
     *
     * @param workingDir : the lambda/ECS working directory
     * @param collectionName : collection short name
     * @param granuleId : granule id
     * @param outJsonString: Json string including FOOTPRINT and EXTENT
     * @return the absolute path of the created fp file - if no error.
     */

    private long outputFootprint(String workingDir, String collectionName, String granuleId,
                                       String outJsonString, String executionName) throws IOException{
        try {
            String footprintBucketName = System.getenv("FOOTPRINT_OUTPUT_BUCKET");
            String footprintDirectory = System.getenv("FOOTPRINT_OUTPUT_DIR");
            // wrote a local working directory
            File f = new File(Paths.get(workingDir, granuleId + ".fp").toString());
            FileUtils.writeStringToFile(f, outJsonString, StandardCharsets.UTF_8.name());
            long fileSize = f.length();

            upload(footprintBucketName,
                    Paths.get(footprintDirectory, collectionName, granuleId + "_" + executionName +".fp").toString(),
                    f.getAbsoluteFile());
            return fileSize;
        } catch (IOException ioe) {
            AdapterLogger.LogError(this.className + " Error output footprint file: " + ioe);
            throw ioe;
        }
    }

    /**
     * Create the output footprint file in the given working directory.
     *
     * @param workingDir absolute path to the directory the calculated footprint should be written to
     * @return The absolute path to the output footprint file
     */
    public String createOutputFootprintFile(String workingDir) {
        File outputFootPrintFile = new File(Paths.get(workingDir, "footprint.txt").toString());
        return outputFootPrintFile.getAbsolutePath();
    }
    
    /**
     * Download granule file from S3.
     *
     * @param sourceBucket the bucket to retrieve the granule from
     * @param key          the key to the granule file
     * @param workDir      the local directory to store the granule file
     * @param granuleName  the name of the granule file to download
     * @return The absolute path of the downloaded granule file
     */
    public String getGranuleFile(String sourceBucket, String key, String workDir, String granuleName) {
        String fileNameWithAbsolutePath = download(sourceBucket, key, Paths.get(workDir, granuleName).toString());
        AdapterLogger.LogInfo("Successfully downloaded granule file : " + fileNameWithAbsolutePath);
        return fileNameWithAbsolutePath;
    }
    
    /**
     * Download dataset config file from S3.
     *
     * @param datasetConfigBucketName the bucket to retrieve the dataset config file from
     * @param datasetConfigKey        the key to the dataset config file
     * @param workDir                 The local directory to store the dataset config file
     * @param collectionName          The name of this collection, which is the name of the dataset config file.
     * @return The absolute path of the downloaded dataset config file.
     */
    public String getDatasetConfigFile(String datasetConfigBucketName, String datasetConfigKey, String workDir,
                                       String collectionName) {
        String fileNameWithAbsolutePath = this.download(datasetConfigBucketName,
                Paths.get(datasetConfigKey, collectionName + ".cfg").toString(),
                Paths.get(workDir, collectionName + ".cfg").toString());
        AdapterLogger.LogInfo("Successfully downloaded dataset config file : " + fileNameWithAbsolutePath);
        return fileNameWithAbsolutePath;
    }
    
    /**
     * Parses S3 bucket and key based on regex.
     *
     * @param s3Path path to archived location of file
     * @return Matcher object where group(1) is the bucket and group(2) is the key
     */
    public Matcher getSourceBucketAndKey(String s3Path) {
        Pattern pattern = Pattern.compile("s3://([^/]*)/(.*)");
        return pattern.matcher(s3Path);
    }

    /**
     * Download a file from a url
     *
     * @param url                 the base url of where to get the file
     * @param workDir             the local directory to store the dataset config file
     * @param collectionName      the name of this collection, which is the name of the dataset config file.
     * @return the absolute path of the downloaded file
     */
    public String downloadFromURL(String url, String workDir, String collectionName) throws IOException {
        AdapterLogger.LogInfo(this.className + " Downloading from url: " + url + " collectionName: " + collectionName+ " workingDir: " + workDir);

        String filePath = Paths.get(workDir, collectionName + ".cfg").toString();
        String fileUrl = url + "/" + collectionName + ".cfg";
        try{
            Files.copy(
                new URL(fileUrl).openStream(),
                Paths.get(workDir, collectionName + ".cfg")
            );
        }
        catch(IOException ioe){
            AdapterLogger.LogError(this.className + " Error downloading configuration file: " + ioe);
            throw ioe;
        }
        return filePath;
    }
    
    /**
     * Download a file from S3
     *
     * @param bucket                 the bucket the file is located in
     * @param key                    the key of the file
     * @param outputFileAbsolutePath the absolute path of where to download this S3 file to
     * @return the absolute path of the downloaded file
     */
    public String download(String bucket, String key, String outputFileAbsolutePath) {
        AdapterLogger.LogInfo(this.className + " Downloading from bucket: " + bucket + " key: " + key
                + " outputFileAbsolutePath: " + outputFileAbsolutePath);
        //TODO:  Of course we need to integration test this.
        // Also, a discussion about passing region value in our just hardcode here.
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
        
        File file = new File(outputFileAbsolutePath);
        if (!StringUtils.isBlank(bucket) && !StringUtils.isBlank(key)) {
            s3Client.getObject(new GetObjectRequest(
                    bucket, key), file);
            return file.getAbsolutePath();
        } else {
            return "";
        }
    }
    
    /**
     * Upload a file to S3
     *
     * @param bucket the bucket to upload the file to
     * @param key    the key to upload to file into
     * @param file   the file to upload
     * @return The S3 URI of the uploaded file
     */
    public String upload(String bucket, String key, File file) {
        AdapterLogger.LogInfo("Uploading to bucket: " + bucket + " key: " + key + " file: " + file);
        AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                .withRegion(region)
                .build();
        String path = bucket + "/" + key;
        try {
            AdapterLogger.LogInfo(this.className + " Uploading an object: " + path);
            s3Client.putObject(new PutObjectRequest(bucket, key, file));
            AdapterLogger.LogInfo(this.className + " Finished uploading an object: " + path);
        } catch (AmazonServiceException ase) {
            AdapterLogger.LogError(this.className + " Caught an AmazonServiceException, which " +
                    "means your request made it " +
                    "to Amazon S3, but was rejected with an error response" +
                    " for some reason.");
            AdapterLogger.LogError(this.className + " Error Message:    " + ase.getMessage());
            AdapterLogger.LogError(this.className + " HTTP Status Code: " + ase.getStatusCode());
            AdapterLogger.LogError(this.className + " AWS Error Code:   " + ase.getErrorCode());
            AdapterLogger.LogError(this.className + " Error Type:       " + ase.getErrorType());
            AdapterLogger.LogError(this.className + " Request ID:       " + ase.getRequestId());
            throw ase;
        } catch (AmazonClientException ace) {
            AdapterLogger.LogError(this.className + " Caught an AmazonClientException, which " +
                    "means the client encountered " +
                    "an internal error while trying to " +
                    "communicate with S3, " +
                    "such as not being able to access the network.");
            AdapterLogger.LogError(this.className + " Error Message: " + ace.getMessage());
            throw ace;
        }
        return "s3://" + path;
    }
}
