package aclusterllc.javaBase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.String.format;

public class HmiServer implements Runnable {
    private Thread worker;
    Selector selector;
    ServerSocketChannel serverSocketChannel;
    ByteBuffer bufferForReadData = ByteBuffer.allocate(10240000);

    ConcurrentHashMap<SocketChannel, ConnectedHmiClient> connectedHmiClientList = new ConcurrentHashMap<>();
    Logger logger = LoggerFactory.getLogger(HmiServer.class);
    final List<HmiMessageObserver> hmiMessageObservers = new ArrayList<>();
    public HmiServer() {
    }
    public void start(){
        logger.info("HMI Server Started");
        worker = new Thread(this);
        try {
            selector = Selector.open();
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.bind(new InetSocketAddress(Integer.parseInt(ConfigurationHelper.configIni.getProperty("hmi_server_port"))));
            serverSocketChannel.configureBlocking(false);
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
            worker.start();
        } catch (IOException ex) {
            logger.error(CommonHelper.getStackTraceString(ex));
        }
    }
    public void run() {
        while (true) {
            try {
                selector.select();
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if(!key.isValid()){
                        continue;
                    }
                    if(key.isAcceptable()){
                        registerConnectedHmiClient(key);
                    }
                    else if (key.isReadable()) {
                        readReceivedDataFromConnectedHmiClient(key);
                    }
                }
            }
            catch (IOException ex) {
                logger.error(CommonHelper.getStackTraceString(ex));
                //e.printStackTrace();
            }
        }
    }
    public void registerConnectedHmiClient(SelectionKey key){
        try {

            SocketChannel socketChannel=serverSocketChannel.accept();
            socketChannel.configureBlocking(false);
            socketChannel.register(selector, SelectionKey.OP_READ);
            connectedHmiClientList.put(socketChannel,new ConnectedHmiClient(socketChannel));
        } catch (IOException ex) {
            logger.error(CommonHelper.getStackTraceString(ex));
        }
    }
    public void readReceivedDataFromConnectedHmiClient(SelectionKey key) {
        SocketChannel socketChannel = (SocketChannel) key.channel();
        bufferForReadData.clear();
        int numRead = 0;
        try {
            numRead = socketChannel.read(bufferForReadData);
        } catch (IOException e) {
            logger.error(e.toString());
            disconnectConnectedHmiClient(socketChannel);
            return;
        }
        if (numRead == -1) {
            // Remote entity shut the socket down cleanly. Do the same from our end and cancel the channel.
            disconnectConnectedHmiClient(socketChannel);
            return;
        }

        byte[] b = new byte[bufferForReadData.position()];
        bufferForReadData.flip();
        bufferForReadData.get(b);
        ConnectedHmiClient connectedHmiClient=connectedHmiClientList.get(socketChannel);
        if(connectedHmiClient==null){
            logger.error("[DATA_PROCESS] ConnectedHmiClient Not found.");
        }
        else{
            connectedHmiClient.processReceivedData(b);
        }
    }
    class ConnectedHmiClient implements Runnable{
        SocketChannel socketChannel;
        String buffer="";
        Thread thread=new Thread(this);

        public ConnectedHmiClient(SocketChannel socketChannel) {
            this.socketChannel=socketChannel;
            try {
                logger.info("Connected with HmiClient: " + socketChannel.getRemoteAddress());
            }
            catch (Exception ex) {
                logger.error(CommonHelper.getStackTraceString(ex));
            }
            this.start();
        }
        public void start(){
            thread.start();
        }
        public void stop(){
            try {
                thread.interrupt();
                logger.info("Closing Connection with HmiClient: " + socketChannel.getRemoteAddress());
                System.out.println("Closing Connection with HmiClient: " + socketChannel.getRemoteAddress());
                socketChannel.close();
            } catch (IOException ex) {
                logger.error(CommonHelper.getStackTraceString(ex));
            }

        }
        public synchronized void processReceivedData(byte[] b){
            buffer=buffer+new String( b, StandardCharsets.UTF_8 );
            this.notify();
        }
        public void run(){
            synchronized (this){
                try {
                    while (true){
                        wait();
                        processReceivedData();
                    }
                }
                catch (Exception ex) {
                    logger.error(CommonHelper.getStackTraceString(ex));
                }
            }
            System.out.println("Run Ended");
            //notifyToHmiMessageObservers(new JSONObject(),new JSONObject());

        }
        public void processReceivedData(){
            String startTag="<begin>";
            String endTag="</begin>";

            int startPos=buffer.indexOf(startTag);
            int endPos=buffer.indexOf(endTag);
            while (startPos>-1 && endPos>-1){
                if(startPos>0){
                    logger.warn("[DATA_PROCESS][START_POS_ERROR] Message did not started with begin. Data: "+buffer);
                }
                if(startPos>endPos){
                    logger.warn("[DATA_PROCESS][END_POS_ERROR] End tag found before start tag. Data: "+buffer);
                    buffer=buffer.substring(startPos);
                }
                else{
                    String messageString=buffer.substring(startPos+startTag.length(),endPos);
                    try {
                        JSONObject jsonObject = new JSONObject(messageString);
                            processReceivedMessage(jsonObject);
                    }
                    catch (JSONException ex) {
                        logger.error(CommonHelper.getStackTraceString(ex));
                    }
                    buffer=buffer.substring(endPos+endTag.length());
                    //parse
                }
                startPos=buffer.indexOf(startTag);
                endPos=buffer.indexOf(endTag);
            }
        }
        public void processReceivedMessage(JSONObject jsonObject){
            try {
                JSONObject response=new JSONObject();

                //System.out.println(jsonObject);
                String request = jsonObject.getString("request");
                JSONObject params = jsonObject.getJSONObject("params");
                JSONArray requestData = jsonObject.getJSONArray("requestData");

                response.put("request",request);
                response.put("params",params);

                int machine_id=0;
                if(params.has("machine_id")){machine_id=params.getInt("machine_id");}

                if(requestData.length()>0){
                    Connection connection=ConfigurationHelper.getConnection();
                    JSONObject responseData=new JSONObject();
                    for(int i=0;i<requestData.length();i++){
                        JSONObject requestFunction=requestData.getJSONObject(i);
                        String requestFunctionName=requestFunction.getString("name");
                        switch (requestFunctionName) {
                            case "active_alarms": {
                                responseData.put(requestFunctionName,DatabaseHelper.getActiveAlarms(connection,machine_id));
                                break;
                            }
                            case "alarms_history": {
                                responseData.put(requestFunctionName,DatabaseHelper.getAlarmsHistory(connection,machine_id,requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "alarms_hit_list": {
                                responseData.put(requestFunctionName,DatabaseHelper.getAlarmsHitList(connection,machine_id,requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "bin_states": {
                                responseData.put(requestFunctionName,DatabaseHelper.getBinStates(connection,machine_id));
                                break;
                            }
                            case "conveyor_states": {
                                responseData.put(requestFunctionName,DatabaseHelper.getConveyorStates(connection,machine_id));
                                break;
                            }
                            case "counters_current_value": {
                                responseData.put(requestFunctionName,ConfigurationHelper.countersCurrentValue);
                                break;
                            }
                            case "device_states": {
                                JSONObject deviceStates=DatabaseHelper.getDeviceStates(connection,machine_id);
                                //for main plc manually set status
                                if(deviceStates.has(machine_id+"_2")){
                                    JSONObject plc= (JSONObject) deviceStates.get(machine_id+"_2");
                                    plc.put("state",ConfigurationHelper.apeClientConnectionStatus.get(machine_id));
                                    deviceStates.put(machine_id+"_2",plc);
                                }
                                responseData.put(requestFunctionName,deviceStates);
                                break;
                            }
                            case "disconnected_device_counter": {
                                responseData.put(requestFunctionName,DatabaseHelper.getDisconnectedDeviceCounter(connection,machine_id));
                                break;
                            }
                            case "input_states": {
                                responseData.put(requestFunctionName,DatabaseHelper.getInputStates(connection,machine_id));
                                break;
                            }
                            case "machine_mode": {
                                responseData.put(requestFunctionName,DatabaseHelper.getMachineMode(connection,machine_id));
                                break;
                            }
                            case "motors_current_speed": {
                                responseData.put(requestFunctionName,ConfigurationHelper.motorsCurrentSpeed);
                                break;
                            }
                            case "output_states": {
                                responseData.put(requestFunctionName,DatabaseHelper.getOutputStates(connection,machine_id));
                                break;
                            }
                            case "parameters_value": {
                                responseData.put(requestFunctionName,DatabaseHelper.getParameterValues(connection,machine_id));
                                break;
                            }
                            case "products_history": {
                                responseData.put(requestFunctionName,DatabaseHelper.getProductsHistory(connection,machine_id,requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics",requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics_hourly": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics_hourly",requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics_minutely": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics_minutely",requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics_counter": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics_counter",requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics_bins": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics_bins",requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics_bins_counter": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics_bins_counter",requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics_bins_hourly": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics_bins_hourly",requestFunction.getJSONObject("params")));
                                break;
                            }
                            case "statistics_oee": {
                                responseData.put(requestFunctionName,DatabaseHelper.getStatisticsData(connection,machine_id,"statistics_oee",requestFunction.getJSONObject("params")));
                                break;
                            }
                        }
                    }
                    connection.close();
                    response.put("data",responseData);
                    sendMessage(response.toString());
                }
                else {
                    switch (request) {
                        case "basic_info": {
                            response.put("data",ConfigurationHelper.dbBasicInfo);
                            sendMessage(response.toString());
                            break;
                        }
                        case "forward_ape_message":{
                            JSONObject info=new JSONObject();
                            notifyToHmiMessageObservers(jsonObject,info);
                            break;
                        }
                        case "getLoginUser":{
                            JSONObject responseData=new JSONObject();
                            String username = params.getString("username");
                            String password = params.getString("password");
                            Connection connection=ConfigurationHelper.getConnection();
                            String query = String.format("SELECT id,name, role FROM users WHERE username='%s' AND password='%s' LIMIT 1", username, password);
                            JSONArray queryResult=DatabaseHelper.getSelectQueryResults(connection,query);

                            if(queryResult.length()>0){
                                responseData.put("status",true);
                                responseData.put("user",queryResult.getJSONObject(0));
                            }
                            else{
                                responseData.put("status",false);
                            }
                            connection.close();
                            response.put("data",responseData);
                            sendMessage(response.toString());
                            break;
                        }
                        case "changeUserPassword":{
                            JSONObject responseData=new JSONObject();
                            responseData.put("status",false);
                            int id = Integer.parseInt(params.get("id").toString());
                            String password = params.get("password").toString();
                            String password_new = params.get("password_new").toString();
                            Connection connection=ConfigurationHelper.getConnection();
                            String query = String.format("SELECT id,password FROM users WHERE id='%d';", id);
                            JSONArray queryResult=DatabaseHelper.getSelectQueryResults(connection,query);

                            if(queryResult.length()>0){
                                JSONObject user=queryResult.getJSONObject(0);
                                if(user.getString("password").equals(password)){
                                    String updateQuery = format("UPDATE %s SET password='%s' WHERE id=%d;","users",password_new,id);
                                    int num_row = DatabaseHelper.runUpdateQuery(connection,updateQuery);
                                    if(num_row>0){
                                        responseData.put("status",true);
                                        responseData.put("message","Password Changed Successfully.");
                                    }
                                    else{
                                        responseData.put("message","Failed to change password");
                                    }

                                }
                                else{
                                    responseData.put("messages","Old Password did not matched.");
                                }
                            }
                            else{
                                responseData.put("status",false);
                                responseData.put("messages","User not found.");
                            }
                            connection.close();
                            response.put("data",responseData);
                            sendMessage(response.toString());
                            break;
                        }
                    }
                }
                //notify

            }
            catch (Exception ex){
                ex.printStackTrace();
                //logger.error(CommonHelper.getStackTraceString(ex));
            }

        }
        public void sendMessage(String msg) {
            String startTag="<begin>";
            String endTag="</begin>";
            msg=startTag+msg+endTag;
            ByteBuffer buf = ByteBuffer.wrap(msg.getBytes());
            try {
                while (buf.hasRemaining()){
                    int n=socketChannel.write(buf);
                    if(buf.remaining()>0){
                        logger.info("[DATA_SEND_TO_HMI]] waiting 30 for next send. MSG Len "+msg.length()+" Written bytes: " + n + ", Remaining: " + buf.remaining()+" MSG "+msg.substring(0,30));
                        Thread.sleep(30);
                    }
                }
            }
            catch (Exception ex) {
                logger.error(CommonHelper.getStackTraceString(ex));
            }
        }
    }

    public void disconnectConnectedHmiClient(SocketChannel socketChannel) {
        try {
            ConnectedHmiClient connectedHmiClient=connectedHmiClientList.remove(socketChannel);
            if(connectedHmiClient==null){
                logger.error("[DATA_PROCESS] ConnectedHmiClient Not found.");
            }
            else{
               connectedHmiClient.stop();
            }
        }
        catch (Exception ex) {
            logger.error(CommonHelper.getStackTraceString(ex));
        }
    }
    public void disconnectAllConnectedHmiClient(){
        for (SocketChannel key : connectedHmiClientList.keySet()) {
            this.disconnectConnectedHmiClient(key);
        }
    }
    public void addHmiMessageObserver(HmiMessageObserver hmiMessageObserver){
        hmiMessageObservers.add(hmiMessageObserver);
    }
    public void notifyToHmiMessageObservers(JSONObject jsonMessage,JSONObject info){
        //int messageId=jsonMessage.getInt("messageId");
        for(HmiMessageObserver hmiMessageObserver:hmiMessageObservers){
            //System.out.println(apeMessageObserver.getClass().getSimpleName());
            //limit messageId for others class
            hmiMessageObserver.processHmiMessage(jsonMessage,info);
        }
    }
}
