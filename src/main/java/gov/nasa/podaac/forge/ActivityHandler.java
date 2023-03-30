import java.io.*;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.stepfunctions.AWSStepFunctions;
import com.amazonaws.services.stepfunctions.AWSStepFunctionsClientBuilder;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskRequest;
import com.amazonaws.services.stepfunctions.model.GetActivityTaskResult;
import com.amazonaws.services.stepfunctions.model.SendTaskFailureRequest;
import com.amazonaws.services.stepfunctions.model.SendTaskSuccessRequest;
import com.amazonaws.util.json.Jackson;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.concurrent.TimeUnit;

import gov.nasa.podaac.forge.FootprintHandler;

import cumulus_message_adapter.message_parser.MessageAdapterException;
import cumulus_message_adapter.message_parser.MessageParser;
import cumulus_message_adapter.message_parser.AdapterLogger;

import org.apache.commons.lang3.StringUtils;


class ActivityHandler {

    public static void main(String[] args) {

        String stringActivityArn = args[0];
        ActivityHandler handler = new ActivityHandler();

        System.out.println(stringActivityArn);

        ClientConfiguration clientConfiguration = new ClientConfiguration();
        String stringtSocketTimeout = System.getenv("SOCKETTIMEOUT") != null ? System.getenv("SOCKETTIMEOUT") : "600";
        int socketTimeout=Integer.parseInt(stringtSocketTimeout); 

        clientConfiguration.setSocketTimeout((int)TimeUnit.SECONDS.toMillis(socketTimeout));
        while(true){
            try {
                handler.run(stringActivityArn, clientConfiguration);
            } catch (Exception e) {
                AdapterLogger.LogWarning("Activity level Exception:" + e);
            }
        }

    }

    public ActivityHandler(){

        
    }
    
    public void run(String stringActivityArn, ClientConfiguration clientConfiguration){

        String region = System.getenv("REGION");

        AWSStepFunctions client = AWSStepFunctionsClientBuilder.standard()
                .withClientConfiguration(clientConfiguration).withRegion(region)
                .build();

        GetActivityTaskResult getActivityTaskResult =
                client.getActivityTask(new GetActivityTaskRequest().withActivityArn(stringActivityArn));
        
        String taskToken = getActivityTaskResult.getTaskToken();
        String taskInput = getActivityTaskResult.getInput();

        FootprintHandler activityHandler = new FootprintHandler();

        //if a taskInput is null so getActivityTask timeout
        if(taskInput == null){
            AdapterLogger.LogDebug("ActivityHandler handleActivityRequest taskInput is null");
            return;
        }

        try
        {
            MessageParser parser = new MessageParser();
            AdapterLogger.LogDebug("ActivityHandler handleActivityRequest is called with message: " + taskInput);
            String output = parser.RunCumulusTask(taskInput, null , activityHandler);
            AdapterLogger.LogDebug("ActivityHandler handleActivityRequest output: " + output);
            SendTaskSuccessRequest successRequest = new SendTaskSuccessRequest();
            successRequest.setTaskToken(taskToken);
            successRequest.setOutput(output);
            client.sendTaskSuccess(successRequest);
        }
        catch(Exception e)
        {
            AdapterLogger.LogError("WaitForTaskToken pattern processing failed: " + e);
            try {
                SendTaskFailureRequest sendTaskFailureRequest = new SendTaskFailureRequest();
                sendTaskFailureRequest.setTaskToken(taskToken);
                sendTaskFailureRequest.setError(StringUtils.substring(e.getMessage(),0, 255));
                sendTaskFailureRequest.setCause(StringUtils.substring(e.toString(), 0, 32767));
                client.sendTaskFailure(sendTaskFailureRequest);
                AdapterLogger.LogError("sent back failure with token ");
            } catch (Exception ex) {
                /**
                 * If sent failure failed the first time.  there are a few guards
                 * 1. the following code retry sending error back to SNF without reason.
                 * 2. the finally block will stop this ecs/fargate docker container (no more charge) and
                 *    the ecs/fargate sfn step has a timeout configured so SFN won't hang forever.
                 *  Note. The SFN fargate step timeout has been verified by making this task not sending back
                 *        either SUCCESS or FAILUE.
                 *
                 *        In the case of SFN failue.  the way to debug is to research the fargate task's log group
                 */
                AdapterLogger.LogError("Can not sent back failure with token: " + ex);
                SendTaskFailureRequest sendTaskFailureRequest = new SendTaskFailureRequest();
                sendTaskFailureRequest.setTaskToken(taskToken);
                sendTaskFailureRequest.setError("Unknown error");
                client.sendTaskFailure(sendTaskFailureRequest);
                AdapterLogger.LogError("sent SFN failure with UNKNOWN reason.");
            }
        }

    }    
}
