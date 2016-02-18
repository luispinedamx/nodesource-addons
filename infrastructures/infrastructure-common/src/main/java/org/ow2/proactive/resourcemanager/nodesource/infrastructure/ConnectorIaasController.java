package org.ow2.proactive.resourcemanager.nodesource.infrastructure;

import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.json.JSONObject;


public class ConnectorIaasController {

    private static final Logger logger = Logger.getLogger(ConnectorIaasController.class);

    protected final ConnectorIaasClient connectorIaasClient;
    private final String infrastructureType;

    public ConnectorIaasController(String connectorIaasURL, String infrastructureType) {
        this.connectorIaasClient = new ConnectorIaasClient(
            ConnectorIaasClient.generateRestClient(connectorIaasURL));
        this.infrastructureType = infrastructureType;

    }

    public ConnectorIaasController(ConnectorIaasClient connectorIaasClient, String infrastructureType) {
        this.connectorIaasClient = connectorIaasClient;
        this.infrastructureType = infrastructureType;

    }

    public void waitForConnectorIaasToBeUP() {
        connectorIaasClient.waitForConnectorIaasToBeUP();
    }

    public String createInfrastructure(String nodeSourceName, String username, String password,
            String endPoint, boolean destroyOnShutdown) {

        String infrastructureId = nodeSourceName.trim().replace(" ", "_").toLowerCase();

        String infrastructureJson = ConnectorIaasJSONTransformer.getInfrastructureJSON(infrastructureId,
                infrastructureType, username, password, destroyOnShutdown);

        logger.info("Creating infrastructure : " + infrastructureJson);

        connectorIaasClient.createInfrastructure(infrastructureId, infrastructureJson);

        logger.info("Infrastructure created");

        return infrastructureId;
    }

    public Set<String> createInstances(String infrastructureId, String instanceTag, String image,
            int numberOfInstances, int cores, int ram) {

        String instanceJson = ConnectorIaasJSONTransformer.getInstanceJSON(instanceTag, image,
                "" + numberOfInstances, "" + cores, "" + ram);

        return createInstance(infrastructureId, instanceJson);
    }

    public Set<String> createInstancesWithPublicKeyNameAndInitScript(String infrastructureId,
            String instanceTag, String image, int numberOfInstances, int hardwareType, String publicKeyName,
            List<String> scripts) {

        String instanceJson = ConnectorIaasJSONTransformer.getInstanceJSONWithPublicKeyAndScripts(instanceTag,
                image, "1", publicKeyName, String.valueOf(hardwareType), scripts);

        return createInstance(infrastructureId, instanceJson);
    }

    public void executeScript(String infrastructureId, String instanceId, List<String> scripts) {

        String instanceScriptJson = ConnectorIaasJSONTransformer.getScriptInstanceJSON(scripts);

        String scriptResult = null;
        try {
            scriptResult = connectorIaasClient.runScriptOnInstance(infrastructureId, instanceId,
                    instanceScriptJson);
            if (logger.isDebugEnabled()) {
                logger.debug("Executed successfully script for instance id :" + instanceId +
                    "\nScript contents : " + instanceScriptJson + " \nResult : " + scriptResult);
            } else {
                logger.info("Script result for instance id " + instanceId + " : " + scriptResult);
            }
        } catch (Exception e) {
            logger.error("Error while executing script :\n" + instanceScriptJson, e);
        }
    }

    public void terminateInstance(String infrastructureId, String instanceId) {
        connectorIaasClient.terminateInstance(infrastructureId, instanceId);
    }

    private Set<String> createInstance(String infrastructureId, String instanceJson) {
        Set<JSONObject> existingInstancesByInfrastructureId = connectorIaasClient
                .getAllJsonInstancesByInfrastructureId(infrastructureId);

        logger.info("Total existing Instances By Infrastructure Id : " +
            existingInstancesByInfrastructureId.size());

        logger.info("InstanceJson : " + instanceJson);

        Set<String> instancesIds = connectorIaasClient.createInstancesIfNotExisist(infrastructureId,
                infrastructureId, instanceJson, existingInstancesByInfrastructureId);

        logger.info("Instances ids created : " + instancesIds);

        return instancesIds;
    }

}