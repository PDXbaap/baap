//package biz.pdxtech.baap.chaincode.driver.fabric;
//
//import biz.pdxtech.baap.api.Constants;
//import biz.pdxtech.baap.api.fabric.Orderer;
//import biz.pdxtech.baap.api.fabric.Org;
//import biz.pdxtech.baap.api.fabric.Peer;
//import biz.pdxtech.baap.driver.BlockChainDriverFactory;
//import biz.pdxtech.baap.driver.fabric.FabricBlockchainDriver;
//import net.sf.json.JSONArray;
//import net.sf.json.JSONObject;
//import org.apache.commons.io.IOUtils;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//
//import java.io.FileInputStream;
//import java.io.InputStream;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.Properties;
//
//public class FabricClient {
//   private static Logger                 log               = LoggerFactory.getLogger(FabricClient.class);
//   private  FabricBlockchainDriver driver            = null;//initDriver();
//
//   private static class Holder {
//       private static FabricClient fabricClient = new FabricClient();
//   }
//   
//   public static FabricClient instance() {
//       return FabricClient.Holder.fabricClient;
//   }
//   
//   public FabricBlockchainDriver initDriver(String urlHost) {
//       String fabricConfig = System.getenv("fabric_config");
//       if (fabricConfig == null || fabricConfig.isEmpty()) {
//           fabricConfig = "/opt/fabric-config";
//       }
//       try (InputStream inputStream = new FileInputStream(fabricConfig)) {
//           String stackConfig = IOUtils.toString(inputStream, "UTF-8");
//           JSONObject jsonObject = JSONObject.fromObject(stackConfig);
//           
//           Properties properties = new Properties();
//           properties.setProperty(Constants.BAAP_ENGINE_TYPE_KEY, Constants.BAAP_ENGINE_TYPE_FABRIC);
//           properties.setProperty(Constants.BAAP_ENGINE_URL_HOST_KEY, "http://" + urlHost);
//           properties.setProperty(Constants.BAAP_SENDER_PRIVATE_KEY, "153e73d76d6fd80df14282005e4a3c49c9307ab549d5bdb7d529f30399320fbb");
//           
//           Org org = null;
//           JSONArray orgList = jsonObject.getJSONArray("org");
//           for (Object object : orgList) {
//               JSONObject orgJsonObject = (JSONObject) object;
//               Properties orgProp = new Properties();
//               orgProp.setProperty("privateKeyFile", (String) orgJsonObject.get("privateKeyFile"));
//               orgProp.setProperty("certificateFile", (String) orgJsonObject.get("certificateFile"));
//               org = Org.builder().name((String) orgJsonObject.get("name")).mspId((String) orgJsonObject.get("mspId")).properties(orgProp).build();
//           }
//           
//           List<Orderer> orderers = new ArrayList<>();
//           JSONArray ordererList = jsonObject.getJSONArray("orderer");
//           for (Object object : ordererList) {
//               JSONObject ordererJsonObject = (JSONObject) object;
//               Properties ordererProp = new Properties();
//               ordererProp.setProperty("pemFile", (String) ordererJsonObject.get("pemFile"));
//               ordererProp.setProperty("hostnameOverride", (String) ordererJsonObject.get("hostnameOverride"));
//               ordererProp.setProperty("sslProvider", (String) ordererJsonObject.get("sslProvider"));
//               ordererProp.setProperty("negotiationType", (String) ordererJsonObject.get("negotiationType"));
//               ordererProp.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", (Integer) ordererJsonObject.get("grpc.ManagedChannelBuilderOption.maxInboundMessageSize"));
//               ordererProp.setProperty("ordererWaitTimeMilliSecs", (String) ordererJsonObject.get("ordererWaitTimeMilliSecs"));
//               orderers.add(Orderer.builder().name((String) ordererJsonObject.get("name")).url((String) ordererJsonObject.get("url")).properties(ordererProp).build());
//           }
//           
//           List<Peer> peers = new ArrayList<>();
//           JSONArray peerList = jsonObject.getJSONArray("peer");
//           for (Object object : peerList) {
//               JSONObject peerJsonObject = (JSONObject) object;
//               Properties peerProp = new Properties();
//               peerProp.setProperty("pemFile", (String) peerJsonObject.get("pemFile"));
//               peerProp.setProperty("hostnameOverride", (String) peerJsonObject.get("hostnameOverride"));
//               peerProp.setProperty("sslProvider", (String) peerJsonObject.get("sslProvider"));
//               peerProp.setProperty("negotiationType", (String) peerJsonObject.get("negotiationType"));
//               peerProp.put("grpc.ManagedChannelBuilderOption.maxInboundMessageSize", (Integer) peerJsonObject.get("grpc.ManagedChannelBuilderOption.maxInboundMessageSize"));
//               peers.add(Peer.builder().name((String) peerJsonObject.get("name")).url((String) peerJsonObject.get("url")).properties(peerProp).build());
//           }
//           
//           return (FabricBlockchainDriver) BlockChainDriverFactory.get(properties, org, orderers, peers);
//       } catch (Exception e) {
//           e.printStackTrace();
//       }
//       return null;
//   }
//}
