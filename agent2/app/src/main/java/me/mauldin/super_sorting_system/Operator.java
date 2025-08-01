package me.mauldin.super_sorting_system;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

public class Operator {
  private final String baseUrl;
  private final String apiKey;
  private final HttpClient httpClient;

  public Operator(String baseUrl, String apiKey) {
    this.baseUrl = baseUrl;
    this.apiKey = apiKey;
    this.httpClient = HttpClient.newHttpClient();
  }

  private String agentEndpoint(String path) {
    return baseUrl + "/agent/" + path;
  }

  private String automationEndpoint(String path) {
    return baseUrl + "/automation/" + path;
  }

  private HttpRequest.Builder requestBuilder() {
    return HttpRequest.newBuilder()
        .header("X-Api-Key", apiKey)
        .header("Content-Type", "application/json");
  }

  private HttpRequest.Builder agentRequestBuilder(Agent agent) {
    return requestBuilder().header("X-Agent-Id", agent.getId());
  }

  public Agent registerAgent() throws IOException, InterruptedException {
    HttpRequest request =
        requestBuilder()
            .uri(URI.create(agentEndpoint("register")))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject json = new JSONObject(response.body());
    JSONObject agentJson = json.getJSONObject("agent");

    return new Agent(agentJson.getString("id"), agentJson.getString("last_seen"));
  }

  public String heartbeat(Agent agent) throws IOException, InterruptedException {
    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("heartbeat")))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public String alert(String description, Agent agent) throws IOException, InterruptedException {
    JSONObject body = new JSONObject();
    body.put("description", description);

    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("alert")))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public static class PollOperationResponse {
    public static class OperationUnavailable extends PollOperationResponse {
      public String getType() {
        return "OperationUnavailable";
      }
    }

    public static class OperationAvailable extends PollOperationResponse {
      private final Operation operation;

      public OperationAvailable(Operation operation) {
        this.operation = operation;
      }

      public String getType() {
        return "OperationAvailable";
      }

      public Operation getOperation() {
        return operation;
      }
    }
  }

  public PollOperationResponse pollOperation(
      Agent agent, Location location, boolean hasClearInventory)
      throws IOException, InterruptedException {
    JSONObject body = new JSONObject();
    body.put("location", locationToJson(location));
    body.put("has_clear_inventory", hasClearInventory);

    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("poll_operation")))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject json = new JSONObject(response.body());

    String type = json.getString("type");
    if ("OperationUnavailable".equals(type)) {
      return new PollOperationResponse.OperationUnavailable();
    } else {
      Operation operation = operationFromJson(json.getJSONObject("operation"));
      return new PollOperationResponse.OperationAvailable(operation);
    }
  }

  public String operationComplete(Agent agent, Operation operation, String finalStatus)
      throws IOException, InterruptedException {
    JSONObject body = new JSONObject();
    body.put("operation_id", operation.getId());
    body.put("final_status", finalStatus);

    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("operation_complete")))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public String inventoryScanned(
      List<Item> slots, Location inventoryLocation, Vec3 openFrom, Agent agent)
      throws IOException, InterruptedException {
    JSONObject body = new JSONObject();
    body.put("location", locationToJson(inventoryLocation));
    body.put("slots", itemsToJsonArray(slots));
    body.put("open_from", vec3ToJson(openFrom));

    System.out.println(body.toString());

    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("inventory_scanned")))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public Hold getHold(String id, Agent agent) throws IOException, InterruptedException {
    HttpRequest request =
        agentRequestBuilder(agent).uri(URI.create(agentEndpoint("hold/" + id))).GET().build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject json = new JSONObject(response.body());
    return holdFromJson(json.getJSONObject("hold"));
  }

  public static class FreeHoldResponse {
    public static class HoldAcquired extends FreeHoldResponse {
      private final Hold hold;

      public HoldAcquired(Hold hold) {
        this.hold = hold;
      }

      public String getType() {
        return "HoldAcquired";
      }

      public Hold getHold() {
        return hold;
      }
    }

    public static class HoldUnavailable extends FreeHoldResponse {
      public String getType() {
        return "HoldUnavailable";
      }
    }
  }

  public FreeHoldResponse getFreeHold(Agent agent) throws IOException, InterruptedException {
    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("hold/free")))
            .POST(HttpRequest.BodyPublishers.noBody())
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject json = new JSONObject(response.body());

    String type = json.getString("type");
    if ("HoldUnavailable".equals(type)) {
      return new FreeHoldResponse.HoldUnavailable();
    } else {
      Hold hold = holdFromJson(json.getJSONObject("hold"));
      return new FreeHoldResponse.HoldAcquired(hold);
    }
  }

  public void releaseHold(String holdId) throws IOException, InterruptedException {
    HttpRequest request =
        requestBuilder().uri(URI.create(automationEndpoint("holds/" + holdId))).DELETE().build();

    httpClient.send(request, HttpResponse.BodyHandlers.ofString());
  }

  public static class PathfindingResponse {
    public static class PathFound extends PathfindingResponse {
      private final List<PfResultNode> path;

      public PathFound(List<PfResultNode> path) {
        this.path = path;
      }

      public String getType() {
        return "PathFound";
      }

      public List<PfResultNode> getPath() {
        return path;
      }
    }

    public static class Error extends PathfindingResponse {
      public String getType() {
        return "Error";
      }
    }
  }

  public PathfindingResponse findPath(Agent agent, Location startLoc, Location endLoc)
      throws IOException, InterruptedException {
    JSONObject body = new JSONObject();
    body.put("start_loc", locationToJson(startLoc));
    body.put("end_loc", locationToJson(endLoc));

    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("pathfinding")))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject json = new JSONObject(response.body());

    String type = json.getString("type");
    if ("Error".equals(type)) {
      return new PathfindingResponse.Error();
    } else {
      JSONArray pathArray = json.getJSONArray("path");
      List<PfResultNode> path = new java.util.ArrayList<>();
      for (int i = 0; i < pathArray.length(); i++) {
        path.add(pfResultNodeFromJson(pathArray.getJSONObject(i)));
      }
      return new PathfindingResponse.PathFound(path);
    }
  }

  public String sendSignScanData(Agent agent, List<ScanRegion> scanRegions)
      throws IOException, InterruptedException {
    JSONObject body = new JSONObject();
    JSONArray regionsArray = new JSONArray();
    for (ScanRegion region : scanRegions) {
      regionsArray.put(scanRegionToJson(region));
    }
    body.put("scan_regions", regionsArray);

    HttpRequest request =
        agentRequestBuilder(agent)
            .uri(URI.create(agentEndpoint("sign_scan_data")))
            .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
            .build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    return response.body();
  }

  public CompiledSignConfig getSignConfig() throws IOException, InterruptedException {
    HttpRequest request =
        requestBuilder().uri(URI.create(automationEndpoint("sign_config"))).GET().build();

    HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    JSONObject json = new JSONObject(response.body());
    return compiledSignConfigFromJson(json);
  }

  private JSONObject locationToJson(Location location) {
    JSONObject json = new JSONObject();
    json.put("vec3", vec3ToJson(location.getVec3()));
    json.put("dim", location.getDim());
    return json;
  }

  private JSONObject vec3ToJson(Vec3 vec3) {
    JSONObject json = new JSONObject();
    json.put("x", vec3.getX());
    json.put("y", vec3.getY());
    json.put("z", vec3.getZ());
    return json;
  }

  private JSONArray itemsToJsonArray(List<Item> items) {
    JSONArray array = new JSONArray();
    for (Item item : items) {
      if (item != null) {
        JSONObject itemJson = new JSONObject();
        itemJson.put("item_id", item.getItemId());
        itemJson.put("count", item.getCount());
        itemJson.put("data_components", item.getDataComponents());
        array.put(itemJson);
      } else {
        array.put(JSONObject.NULL);
      }
    }
    return array;
  }

  private Operation operationFromJson(JSONObject json) {
    String id = json.getString("id");
    String priority = json.getString("priority");
    String status = json.getString("status");
    OperationKind kind = operationKindFromJson(json.getJSONObject("kind"));
    return new Operation(id, priority, status, kind);
  }

  private OperationKind operationKindFromJson(JSONObject json) {
    String type = json.getString("type");
    switch (type) {
      case "ScanInventory":
        return new ScanInventoryOperationKind(
            locationFromJson(json.getJSONObject("location")),
            vec3FromJson(json.getJSONObject("open_from")));
      case "ScanSigns":
        Location location = locationFromJson(json.getJSONObject("location"));
        Vec3 takePortal =
            json.has("take_portal") && !json.isNull("take_portal")
                ? vec3FromJson(json.getJSONObject("take_portal"))
                : null;
        return new ScanSignsOperationKind(location, takePortal);
      case "MoveItems":
        JSONArray sourceHoldsArray = json.getJSONArray("source_holds");
        String[] sourceHolds = new String[sourceHoldsArray.length()];
        for (int i = 0; i < sourceHoldsArray.length(); i++) {
          sourceHolds[i] = sourceHoldsArray.getString(i);
        }
        JSONArray destinationHoldsArray = json.getJSONArray("destination_holds");
        String[] destinationHolds = new String[destinationHoldsArray.length()];
        for (int i = 0; i < destinationHoldsArray.length(); i++) {
          destinationHolds[i] = destinationHoldsArray.getString(i);
        }
        JSONArray countsArray = json.getJSONArray("counts");
        int[] counts = new int[countsArray.length()];
        for (int i = 0; i < countsArray.length(); i++) {
          counts[i] = countsArray.getInt(i);
        }
        return new MoveItemsOperationKind(sourceHolds, destinationHolds, counts);
      case "DropItems":
        Location dropFrom = locationFromJson(json.getJSONObject("drop_from"));
        Vec3 aimTowards = vec3FromJson(json.getJSONObject("aim_towards"));
        JSONArray dropSourceHoldsArray = json.getJSONArray("source_holds");
        String[] dropSourceHolds = new String[dropSourceHoldsArray.length()];
        for (int i = 0; i < dropSourceHoldsArray.length(); i++) {
          dropSourceHolds[i] = dropSourceHoldsArray.getString(i);
        }
        return new DropItemsOperationKind(dropFrom, aimTowards, dropSourceHolds);
      case "ImportInventory":
        Vec3 chestLocation = vec3FromJson(json.getJSONObject("chest_location"));
        Location nodeLocation = locationFromJson(json.getJSONObject("node_location"));
        JSONArray importDestinationHoldsArray = json.getJSONArray("destination_holds");
        String[] importDestinationHolds = new String[importDestinationHoldsArray.length()];
        for (int i = 0; i < importDestinationHoldsArray.length(); i++) {
          importDestinationHolds[i] = importDestinationHoldsArray.getString(i);
        }
        return new ImportInventoryOperationKind(
            chestLocation, nodeLocation, importDestinationHolds);
      case "LoadShulker":
        Location loadShulkerStationLocation =
            locationFromJson(json.getJSONObject("shulker_station_location"));
        String loadShulkerHold = json.getString("shulker_hold");
        JSONArray loadSourceHoldsArray = json.getJSONArray("source_holds");
        String[] loadSourceHolds = new String[loadSourceHoldsArray.length()];
        for (int i = 0; i < loadSourceHoldsArray.length(); i++) {
          if (loadSourceHoldsArray.isNull(i)) {
            loadSourceHolds[i] = null;
          } else {
            loadSourceHolds[i] = loadSourceHoldsArray.getString(i);
          }
        }
        return new LoadShulkerOperationKind(
            loadShulkerStationLocation, loadShulkerHold, loadSourceHolds);
      case "UnloadShulker":
        Location unloadShulkerStationLocation =
            locationFromJson(json.getJSONObject("shulker_station_location"));
        String unloadShulkerHold = json.getString("shulker_hold");
        JSONArray unloadDestinationHoldsArray = json.getJSONArray("destination_holds");
        String[] unloadDestinationHolds = new String[unloadDestinationHoldsArray.length()];
        for (int i = 0; i < unloadDestinationHoldsArray.length(); i++) {
          unloadDestinationHolds[i] = unloadDestinationHoldsArray.getString(i);
        }
        return new UnloadShulkerOperationKind(
            unloadShulkerStationLocation, unloadShulkerHold, unloadDestinationHolds);
      default:
        throw new IllegalArgumentException("Unknown operation type: " + type);
    }
  }

  private Hold holdFromJson(JSONObject json) {
    return new Hold(
        json.getString("id"),
        locationFromJson(json.getJSONObject("location")),
        json.getInt("slot"),
        json.getString("valid_until"),
        vec3FromJson(json.getJSONObject("open_from")));
  }

  private Location locationFromJson(JSONObject json) {
    return new Location(vec3FromJson(json.getJSONObject("vec3")), json.getString("dim"));
  }

  private Vec3 vec3FromJson(JSONObject json) {
    return new Vec3(json.getInt("x"), json.getInt("y"), json.getInt("z"));
  }

  private PfResultNode pfResultNodeFromJson(JSONObject json) {
    if (json.has("Vec")) {
      return new PfResultNode.Vec(vec3FromJson(json.getJSONObject("Vec")));
    } else if (json.has("Portal")) {
      JSONObject portal = json.getJSONObject("Portal");
      return new PfResultNode.Portal(
          vec3FromJson(portal.getJSONObject("vec")), portal.getString("destination_dim"));
    }
    throw new IllegalArgumentException("Unknown PfResultNode type");
  }

  private JSONObject scanRegionToJson(ScanRegion region) {
    JSONObject json = new JSONObject();

    JSONArray signsArray = new JSONArray();
    for (Sign sign : region.getSigns()) {
      JSONObject signJson = new JSONObject();
      signJson.put("lines", new JSONArray(sign.getLines()));
      signJson.put("location", locationToJson(sign.getLocation()));
      signsArray.put(signJson);
    }
    json.put("signs", signsArray);

    JSONArray boundsArray = new JSONArray();
    boundsArray.put(vec2ToJson(region.getBounds()[0]));
    boundsArray.put(vec2ToJson(region.getBounds()[1]));
    json.put("bounds", boundsArray);

    json.put("dimension", region.getDimension());
    return json;
  }

  private JSONObject vec2ToJson(Vec2 vec2) {
    JSONObject json = new JSONObject();
    json.put("x", vec2.getX());
    json.put("z", vec2.getZ());
    return json;
  }

  private CompiledSignConfig compiledSignConfigFromJson(JSONObject json) {
    return new CompiledSignConfig(json);
  }

  public static class Agent {
    private final String id;
    private final String lastSeen;

    public Agent(String id, String lastSeen) {
      this.id = id;
      this.lastSeen = lastSeen;
    }

    public String getId() {
      return id;
    }

    public String getLastSeen() {
      return lastSeen;
    }
  }

  public static class Vec3 {
    private final int x, y, z;

    public Vec3(int x, int y, int z) {
      this.x = x;
      this.y = y;
      this.z = z;
    }

    public int getX() {
      return x;
    }

    public int getY() {
      return y;
    }

    public int getZ() {
      return z;
    }
  }

  public static class Vec2 {
    private final double x, z;

    public Vec2(double x, double z) {
      this.x = x;
      this.z = z;
    }

    public double getX() {
      return x;
    }

    public double getZ() {
      return z;
    }
  }

  public static class Location {
    private final Vec3 vec3;
    private final String dim;

    public Location(Vec3 vec3, String dim) {
      this.vec3 = vec3;
      this.dim = dim;
    }

    public Vec3 getVec3() {
      return vec3;
    }

    public String getDim() {
      return dim;
    }
  }

  public static class Operation {
    private final String id;
    private final String priority;
    private final String status;
    private final OperationKind kind;

    public Operation(String id, String priority, String status, OperationKind kind) {
      this.id = id;
      this.priority = priority;
      this.status = status;
      this.kind = kind;
    }

    public String getId() {
      return id;
    }

    public String getPriority() {
      return priority;
    }

    public String getStatus() {
      return status;
    }

    public OperationKind getKind() {
      return kind;
    }
  }

  public abstract static class OperationKind {}

  public static class ScanInventoryOperationKind extends OperationKind {
    private final Location location;
    private final Vec3 openFrom;

    public ScanInventoryOperationKind(Location location, Vec3 openFrom) {
      this.location = location;
      this.openFrom = openFrom;
    }

    public Location getLocation() {
      return location;
    }

    public Vec3 getOpenFrom() {
      return openFrom;
    }
  }

  public static class ScanSignsOperationKind extends OperationKind {
    private final Location location;
    private final Vec3 takePortal;

    public ScanSignsOperationKind(Location location, Vec3 takePortal) {
      this.location = location;
      this.takePortal = takePortal;
    }

    public Location getLocation() {
      return location;
    }

    public Vec3 getTakePortal() {
      return takePortal;
    }
  }

  public static class MoveItemsOperationKind extends OperationKind {
    private final String[] sourceHolds;
    private final String[] destinationHolds;
    private final int[] counts;

    public MoveItemsOperationKind(String[] sourceHolds, String[] destinationHolds, int[] counts) {
      this.sourceHolds = sourceHolds;
      this.destinationHolds = destinationHolds;
      this.counts = counts;
    }

    public String[] getSourceHolds() {
      return sourceHolds;
    }

    public String[] getDestinationHolds() {
      return destinationHolds;
    }

    public int[] getCounts() {
      return counts;
    }
  }

  public static class DropItemsOperationKind extends OperationKind {
    private final Location dropFrom;
    private final Vec3 aimTowards;
    private final String[] sourceHolds;

    public DropItemsOperationKind(Location dropFrom, Vec3 aimTowards, String[] sourceHolds) {
      this.dropFrom = dropFrom;
      this.aimTowards = aimTowards;
      this.sourceHolds = sourceHolds;
    }

    public Location getDropFrom() {
      return dropFrom;
    }

    public Vec3 getAimTowards() {
      return aimTowards;
    }

    public String[] getSourceHolds() {
      return sourceHolds;
    }
  }

  public static class ImportInventoryOperationKind extends OperationKind {
    private final Vec3 chestLocation;
    private final Location nodeLocation;
    private final String[] destinationHolds;

    public ImportInventoryOperationKind(
        Vec3 chestLocation, Location nodeLocation, String[] destinationHolds) {
      this.chestLocation = chestLocation;
      this.nodeLocation = nodeLocation;
      this.destinationHolds = destinationHolds;
    }

    public Vec3 getChestLocation() {
      return chestLocation;
    }

    public Location getNodeLocation() {
      return nodeLocation;
    }

    public String[] getDestinationHolds() {
      return destinationHolds;
    }
  }

  public static class LoadShulkerOperationKind extends OperationKind {
    private final Location shulkerStationLocation;
    private final String shulkerHold;
    private final String[] sourceHolds;

    public LoadShulkerOperationKind(
        Location shulkerStationLocation, String shulkerHold, String[] sourceHolds) {
      this.shulkerStationLocation = shulkerStationLocation;
      this.shulkerHold = shulkerHold;
      this.sourceHolds = sourceHolds;
    }

    public Location getShulkerStationLocation() {
      return shulkerStationLocation;
    }

    public String getShulkerHold() {
      return shulkerHold;
    }

    public String[] getSourceHolds() {
      return sourceHolds;
    }
  }

  public static class UnloadShulkerOperationKind extends OperationKind {
    private final Location shulkerStationLocation;
    private final String shulkerHold;
    private final String[] destinationHolds;

    public UnloadShulkerOperationKind(
        Location shulkerStationLocation, String shulkerHold, String[] destinationHolds) {
      this.shulkerStationLocation = shulkerStationLocation;
      this.shulkerHold = shulkerHold;
      this.destinationHolds = destinationHolds;
    }

    public Location getShulkerStationLocation() {
      return shulkerStationLocation;
    }

    public String getShulkerHold() {
      return shulkerHold;
    }

    public String[] getDestinationHolds() {
      return destinationHolds;
    }
  }

  public static class Hold {
    private final String id;
    private final Location location;
    private final int slot;
    private final String validUntil;
    private final Vec3 openFrom;

    public Hold(String id, Location location, int slot, String validUntil, Vec3 openFrom) {
      this.id = id;
      this.location = location;
      this.slot = slot;
      this.validUntil = validUntil;
      this.openFrom = openFrom;
    }

    public String getId() {
      return id;
    }

    public Location getLocation() {
      return location;
    }

    public int getSlot() {
      return slot;
    }

    public String getValidUntil() {
      return validUntil;
    }

    public Vec3 getOpenFrom() {
      return openFrom;
    }
  }

  public static class Item {
    private final int itemId;
    private final int count;
    private final Object dataComponents;

    public Item(int itemId, int count, Object dataComponents) {
      this.itemId = itemId;
      this.count = count;
      this.dataComponents = dataComponents;
    }

    public int getItemId() {
      return itemId;
    }

    public int getCount() {
      return count;
    }

    public Object getDataComponents() {
      return dataComponents;
    }
  }

  public abstract static class PfResultNode {
    public static class Vec extends PfResultNode {
      private final Vec3 vec;

      public Vec(Vec3 vec) {
        this.vec = vec;
      }

      public Vec3 getVec() {
        return vec;
      }
    }

    public static class Portal extends PfResultNode {
      private final Vec3 vec;
      private final String destinationDim;

      public Portal(Vec3 vec, String destinationDim) {
        this.vec = vec;
        this.destinationDim = destinationDim;
      }

      public Vec3 getVec() {
        return vec;
      }

      public String getDestinationDim() {
        return destinationDim;
      }
    }
  }

  public static class Sign {
    private final List<String> lines;
    private final Location location;

    public Sign(List<String> lines, Location location) {
      this.lines = lines;
      this.location = location;
    }

    public List<String> getLines() {
      return lines;
    }

    public Location getLocation() {
      return location;
    }
  }

  public static class ScanRegion {
    private final List<Sign> signs;
    private final Vec2[] bounds;
    private final String dimension;

    public ScanRegion(List<Sign> signs, Vec2[] bounds, String dimension) {
      this.signs = signs;
      this.bounds = bounds;
      this.dimension = dimension;
    }

    public List<Sign> getSigns() {
      return signs;
    }

    public Vec2[] getBounds() {
      return bounds;
    }

    public String getDimension() {
      return dimension;
    }
  }

  public static class CompiledSignConfig {
    private final JSONObject data;

    public CompiledSignConfig(JSONObject data) {
      this.data = data;
    }

    public JSONObject getData() {
      return data;
    }
  }
}
