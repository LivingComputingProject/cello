/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.cellocad.localoptimizer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.TreeMap;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.MaxIter;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.univariate.BrentOptimizer;
import org.apache.commons.math3.optim.univariate.SearchInterval;
import org.apache.commons.math3.optim.univariate.UnivariateObjectiveFunction;
import org.apache.commons.math3.optim.univariate.UnivariatePointValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 *
 * @author arashkh
 */
public class CelloAPI {

    private static String username, password;

    private static String getCredentials() {
        String authString = username + ":" + password;
        return "Basic " + new String(Base64.encodeBase64(authString.getBytes()));
    }

    /**
     * Setter for the private field username
     *
     * @param username username value to be set
     */
    public static void setUsername(String username) {
        CelloAPI.username = username;
    }

    /**
     * Setter for the private field username
     *
     * @param password password value to be set
     */
    public static void setPassword(String password) {
        CelloAPI.password = password;
    }

    /**
     * replaces one Gate in JSON Array that is supplied by the User Constraints File.
     *
     * @param jsonArray Parsed JSON Array from the UCF file
     * @param gate Gate to be replaced
     * @throws Exception
     */
    public static void jsonReplaceGate(JSONArray jsonArray, Gate gate) throws Exception {

        for (Object object : jsonArray) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.get("collection").equals("response_functions")
                    && jsonObject.get("gate_name").toString().equals(gate.getName())) {

                // Four natural parameters
                for (Object objectParameters : (JSONArray) jsonObject.get("parameters")) {
                    JSONObject jsonParameters = (JSONObject) objectParameters;
                    if (jsonParameters.get("name").equals("ymax")) {
                        jsonParameters.put("value", gate.getYmax());
                    } else if (jsonParameters.get("name").equals("ymin")) {
                        jsonParameters.put("value", gate.getYmin());
                    } else if (jsonParameters.get("name").equals("K")) {
                        jsonParameters.put("value", gate.getK());
                    } else if (jsonParameters.get("name").equals("n")) {
                        jsonParameters.put("value", gate.getN());
                    }
                }

                // On and Off threshold
                for (Object objectVariables : (JSONArray) jsonObject.get("variables")) {
                    JSONObject jsonVariables = (JSONObject) objectVariables;
                    if (jsonVariables.get("name").equals("x")) {
                        jsonVariables.put("off_threshold", gate.getOff_threshold());
                        jsonVariables.put("on_threshold", gate.getOn_threshold());
                    }
                }
                return;
            }
        }
        throw new NoSuchElementException();
    }

    /**
     * Find the gates found in the JSON Array of the UCF file
     *
     * @param jsonArray Parsed JSON Array from UCF file
     * @return ArrayList of the gates found in the JSON Array of the UCF file
     */
    public ArrayList<Gate> jsonReadAllGates(JSONArray jsonArray) {
        ArrayList<Gate> gates = new ArrayList();
        for (Object object : jsonArray) {
            JSONObject jsonObject = (JSONObject) object;
            if (jsonObject.get("collection").equals("response_functions")) {

                String name = jsonObject.get("gate_name").toString();

                double ymax = 0, ymin = 0, K = 0, n = 0;
                for (Object objectParameters : (JSONArray) jsonObject.get("parameters")) {
                    JSONObject jsonParameters = (JSONObject) objectParameters;
                    if (jsonParameters.get("name").equals("ymax")) {
                        ymax = Double.parseDouble(jsonParameters.get("value").toString());
                    } else if (jsonParameters.get("name").equals("ymin")) {
                        ymin = Double.parseDouble(jsonParameters.get("value").toString());
                    } else if (jsonParameters.get("name").equals("K")) {
                        K = Double.parseDouble(jsonParameters.get("value").toString());
                    } else if (jsonParameters.get("name").equals("n")) {
                        n = Double.parseDouble(jsonParameters.get("value").toString());
                    }
                }

                // Just for checking purposes
                double off_threshold = 0, on_threshold = 0;
                for (Object objectVariables : (JSONArray) jsonObject.get("variables")) {
                    JSONObject jsonVariables = (JSONObject) objectVariables;
                    if (jsonVariables.get("name").equals("x")) {
                        off_threshold = Double.parseDouble(jsonVariables.get("off_threshold").toString());
                        on_threshold = Double.parseDouble(jsonVariables.get("on_threshold").toString());
                    }
                }
                //************************************

                Gate tmpGate = new Gate(name, ymax, ymin, K, n);
                gates.add(tmpGate);
            }
        }
        return gates;
    }

    /**
     * Removes a certain collection from the JSON Array
     *
     * @param jsonArray Parsed JSON Array from the UCF file
     * @param collection NAme of the collection to be removed
     */
    public static void jsonRemoveCollection(JSONArray jsonArray, String collection) {
        Iterator<JSONObject> iter = jsonArray.iterator();
        while (iter.hasNext()) {
            JSONObject jsonObject = (JSONObject) iter.next();
            if (jsonObject.get("collection").equals(collection)) {
                iter.remove();
            }
        }
    }

    /**
     * Provides parsed JSON Array from a UCF file
     *
     * @param filePath String pointing to the path of the JSON file
     * @return JSON Array of the UCF file
     * @throws Exception
     */
    public static JSONArray jsonRead(String filePath) throws Exception {
        FileReader reader = new FileReader(filePath);
        JSONParser jsonParser = new JSONParser();
        return (JSONArray) jsonParser.parse(reader);
    }

    /**
     * Writes JSON Array to a file
     *
     * @param jsonArray JSON Array to be written
     * @param filePath String pointing to the path of the JSON file
     * @throws Exception
     */
    public static void jsonWrite(JSONArray jsonArray, String filePath) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonArray.toString());
        String prettyJsonString = gson.toJson(je);
        FileWriter writer = new FileWriter(filePath);
        writer.write(prettyJsonString);
        writer.close();
    }

    /**
     * Writes JSON Object to a file
     *
     * @param jsonObject JSON Object to be written
     * @param filePath String pointing to the path of the JSON file
     * @throws Exception
     */
    public static void jsonWrite(JSONObject jsonObject, String filePath) throws Exception {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(jsonObject.toString());
        String prettyJsonString = gson.toJson(je);
        FileWriter writer = new FileWriter(filePath);
        writer.write(prettyJsonString);
        writer.close();
    }

    /**
     * GET request to a specified URL with authorization header
     * 
     * @param url URL to which GET is being sent
     * @return Response code and String in a list respectively
     * @throws Exception 
     */
    private static ArrayList<Object> sendGet(String url) throws Exception {

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("GET");

        con.setRequestProperty("Authorization", getCredentials());

        Integer responseCode = con.getResponseCode();

        StringBuffer response = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (Exception e) {
        }
        ArrayList<Object> result = new ArrayList();
        result.add(responseCode);
        result.add(response.toString());
        return result;
    }

    /**
     * POST request to specified URL with authorization header
     * 
     * @param url URL to which POST is being sent
     * @param parameters Map from header names to values that need to be included
     * @return Response code and String in a list respectively
     * @throws Exception 
     */
    private static ArrayList<Object> sendPost(String url, Map<String, String> parameters) throws Exception {

        String data = "";
        for (String key : parameters.keySet()) {
            //curl does not encode the parameter name
            data += key + "=" + URLEncoder.encode(parameters.get(key), StandardCharsets.UTF_8.name()) + "&";
        }
        data = data.substring(0, data.length() - 1);

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("POST");
        con.setRequestProperty("Authorization", getCredentials());
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(data);
        wr.flush();
        wr.close();

        Integer responseCode = con.getResponseCode();

        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();

        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();

        ArrayList<Object> result = new ArrayList();
        result.add(responseCode);
        result.add(response.toString());
        return result;
    }

    /**
     * DELETE request to a specified URL with authorization header
     * 
     * @param url URL to which DELETE is being sent
     * @return Response code and String in a list respectively
     * @throws Exception 
     */
    private static ArrayList<Object> sendDelete(String url) throws Exception {
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();

        con.setRequestMethod("DELETE");
        con.setRequestProperty("Authorization", getCredentials());

        Integer responseCode = con.getResponseCode();

        StringBuffer response = new StringBuffer();
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        try {
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
        } catch (Exception e) {
        }

        ArrayList<Object> result = new ArrayList();
        result.add(responseCode);
        result.add(response.toString());
        return result;
    }

    /**
     * This method clones a circuit to get rid of modifying original gates.
     *
     * @param circuitOriginal the circuit that needs to be cloned
     * @return a cloned circuit from the original
     */
    public static TreeMap<Integer, Gate> clone(TreeMap<Integer, Gate> circuitOriginal) {
        TreeMap<Integer, Gate> circuit = new TreeMap();
        for (Integer i : circuitOriginal.keySet()) {
            Gate g = new Gate(circuitOriginal.get(i));
            circuit.put(i, g);
        }
        return circuit;
    }

    /**
     * Gets all the jobs of the certain user on the remote server
     *
     * @return ArrayList containing all the jobs of the user
     * @throws Exception
     */
    public static ArrayList<String> getJobs() throws Exception {
        String url = "http://cellocad.org:8080/results";
        ArrayList<Object> response = sendGet(url);
        String responseString = response.get(1).toString();

        ArrayList<String> result = new ArrayList();

        int startIndex = responseString.indexOf("[", responseString.indexOf("folders")) + 1;
        int endIndex = responseString.indexOf("]");
        String focusString = responseString.substring(startIndex, endIndex);
        String[] jobsStrArray = focusString.split(",");
        for (String str : jobsStrArray) {
            str = str.replaceAll("\"", "");
            if (!str.isEmpty()) {
                result.add(str);
            }
        }
        return result;
    }

    /**
     * Finds the circuit that has been used by a certain job on the remote server
     *
     * @param jobId String representing the requested job
     * @return Map representing the circuit used by that job
     * @throws Exception
     */
    public static TreeMap<Integer, Gate> getCircuit(String jobId) throws Exception {
        String url = "http://cellocad.org:8080/results/" + jobId + "/" + jobId + "_dnacompiler_output.txt";
        ArrayList<Object> response = sendGet(url);
        String responseString = response.get(1).toString();

        return getCircuitInternal(responseString);
    }

    /**
     * Finds the circuit from a GET response
     *
     * @param responseString String demonstrating the response to the GET request
     * @return Map representing the circuit used by that job
     */
    public static TreeMap<Integer, Gate> getCircuitInternal(String responseString) {
        TreeMap<Integer, Gate> result = new TreeMap(Collections.reverseOrder());

        int startIndex = 0;
        int endIndex = 0;
        startIndex = responseString.indexOf("----- Logic Circuit #0 -----", responseString.indexOf("=========== Circuit assignment details =======")) + 28;
        endIndex = responseString.indexOf("Circuit_score");
        String focusString = responseString.substring(startIndex, endIndex).replaceAll("\\r?\\n", "");
        //System.out.println("First: " + focusString);
        String[] netsStrArray = focusString.split("\\s+");

        // First Pass to make the map
        for (int i = 0; i < netsStrArray.length;) {
            Gate g = new Gate(netsStrArray[i + 2]);
            if (netsStrArray[i].contains("INPUT")) {
                g.type = Gate.TYPE.INPUT;
            } else if (netsStrArray[i].contains("OUTPUT_OR")) {
                g.type = Gate.TYPE.OUTPUT_OR;
            } else if (netsStrArray[i].contains("OUTPUT")) {
                g.type = Gate.TYPE.OUTPUT;
            } else if (netsStrArray[i].contains("NOT")) {
                g.type = Gate.TYPE.NOT;
            } else if (netsStrArray[i].contains("NOR")) {
                g.type = Gate.TYPE.NOR;
            }
            result.put(Integer.parseInt(netsStrArray[i + 3]), g);
            if (netsStrArray[i].contains("INPUT")) {
                i += 4;
                if (!netsStrArray[i].contains("INPUT")) {
                    break;
                }
            } else {
                i += 6;
            }
        }

        // Second pass to make the graph
        for (int i = 0; i < netsStrArray.length; i += 6) {
            if (netsStrArray[i].contains("INPUT")) {
                break;
            }
            String[] gateInputs = netsStrArray[i + 4].split(",");
            for (int j = 0; j < gateInputs.length; j++) {
                result.get(Integer.parseInt(netsStrArray[i + 3])).getFrom()
                        .add(result.get(Integer.parseInt(gateInputs[j].replaceAll("[^\\d.]", ""))));
                result.get(Integer.parseInt(gateInputs[j].replaceAll("[^\\d.]", "")))
                        .setTo(result.get(Integer.parseInt(netsStrArray[i + 3])));
            }
        }

        // Gate parameters used in the design
        //Parse the gates used in the design
        int notIndex = responseString.indexOf("NOT", responseString.indexOf("=========== Circuit assignment details ======="));
        if (notIndex==-1) notIndex = Integer.MAX_VALUE;
        int norIndex = responseString.indexOf("NOR", responseString.indexOf("=========== Circuit assignment details ======="));
        if (norIndex==-1) norIndex = Integer.MAX_VALUE;
        startIndex = Math.min(notIndex, norIndex);
        endIndex = responseString.indexOf("INPUT", responseString.indexOf("=========== Circuit assignment details ======="));
        focusString = responseString.substring(startIndex, endIndex).replaceAll("\\r?\\n", "");
        String[] namesStrArray = focusString.split("\\s+");
        //Parse gate parameters used in the design
        startIndex = responseString.indexOf("Loading Response Functions");
        endIndex = responseString.indexOf("Loading Toxicity Data");
        focusString = responseString.substring(startIndex, endIndex).replaceAll("\\r?\\n", "");
        //System.out.println("Second: " + focusString);
        String[] paramsStrArray = focusString.split("}");
        for (int i = 2; i < namesStrArray.length; i += 6) {
            String name = namesStrArray[i];
            double ymax = 0, ymin = 0, n = 1, K = 1;
            for (int j = 0; j < paramsStrArray.length; j++) {
                if (paramsStrArray[j].contains(namesStrArray[i])) {
                    String[] paramsArray = paramsStrArray[j].substring(paramsStrArray[j].indexOf("{") + 1).split("\\s+");
                    for (int k = 0; k < paramsArray.length; k++) {
                        if (paramsArray[k].contains("ymax=")) {
                            ymax = Double.parseDouble(paramsArray[k].replaceAll("[^\\d.]", ""));
                        } else if (paramsArray[k].contains("ymin=")) {
                            ymin = Double.parseDouble(paramsArray[k].replaceAll("[^\\d.]", ""));
                        } else if (paramsArray[k].contains("n=")) {
                            n = Double.parseDouble(paramsArray[k].replaceAll("[^\\d.]", ""));
                        } else if (paramsArray[k].contains("K=")) {
                            K = Double.parseDouble(paramsArray[k].replaceAll("[^\\d.]", ""));
                        }
                    }
                }
            }
            for (Integer key : result.keySet()) {
                Gate g = result.get(key);
                if (g.getName().equals(name)) {
                    g.setYmax(ymax);
                    g.setYmin(ymin);
                    g.setK(K);
                    g.setN(n);
                }
            }
        }

        //Parse circuitInputs used in the design
        startIndex = responseString.indexOf("input", responseString.indexOf("Loading Cytometry Data"));
        endIndex = responseString.indexOf("output", responseString.indexOf("Loading Cytometry Data"));
        focusString = responseString.substring(startIndex, endIndex).replaceAll("\\r?\\n", "");
        //System.out.println("Third: " + focusString);
        String[] inputStrArray = focusString.split("\\s+");
        for (int i = 1; i < inputStrArray.length; i += 3) {
            String name = inputStrArray[i];
            double ymax = 0, ymin = 0, n = 1, K = 1;
            for (int j = i + 1; j < i + 3; j++) {
                if (inputStrArray[j].contains("off_reu=")) {
                    ymin = Double.parseDouble(inputStrArray[j].replaceAll("[^\\d.]", ""));
                } else if (inputStrArray[j].contains("on_reu=")) {
                    ymax = Double.parseDouble(inputStrArray[j].replaceAll("[^\\d.]", ""));
                }
            }
            for (Integer key : result.keySet()) {
                Gate g = result.get(key);
                if (g.getName().equals(name)) {
                    g.setHigh(ymax);
                    g.setLow(ymin);
                }
            }
        }

        return result;
    }

    /**
     * Computes the score of a circuit
     * 
     * @param circuit circuit under study
     * @return double value as score of the circuit
     */
    public static double getScore(TreeMap<Integer, Gate> circuit) {
        // This has to be in the descending order --> topological sort
        ArrayList<Integer> list = new ArrayList(circuit.keySet());
        Collections.sort(list, Collections.reverseOrder());
        for (Integer i : list) {
            circuit.get(i).updateOutputs();
        }
        return circuit.lastEntry().getValue().getHigh() / circuit.lastEntry().getValue().getLow();
    }

    /**
     * This method optimizes a circuit starting from outer gates inward using a recursive algorithm, the outermost gate and the number of gates inward can be specified
     *
     * @param circuit circuit under study
     * @param numberOfGates number of gates that can be modified
     * @param start the outermost (closest to output) gate to be modified
     * @param ProteinEngineeringIsAllowed whether Protein engineering is allowed
     */
    public static void circuitOptimizer(TreeMap<Integer, Gate> circuit, int numberOfGates, int start, boolean ProteinEngineeringIsAllowed) {
        if (numberOfGates < 1) {
            return;
        }
        Gate gate = circuit.get(start);
        // Protein Engineering
        if (ProteinEngineeringIsAllowed) {
            gate.setN(1.05 * gate.getN());
            gate.setYmax(1.5 * gate.getYmax());
            gate.setYmin(gate.getYmin() / 1.5);
        }
        circuitOptimizer(circuit, numberOfGates - 1, start + 1, ProteinEngineeringIsAllowed);
        // Optimizing K
        optSolverK(gate);
        // Optimizing Offset
        //optSolverOffset(gate);
    }
    
    /**
     * A newer version of circuitOptimize, which uses an ordered list of the gates to be modified; this is used to get the minimum number of modifications
     *
     * @param circuit circuit under study
     * @param order A list representing the order by which the gates need to be modified
     * @param ProteinEngineeringIsAllowed whether Protein engineering is allowed
     */
    public static void circuitOptimizer(TreeMap<Integer, Gate> circuit, ArrayList<Integer> order, boolean ProteinEngineeringIsAllowed) {
        int numberOfGates = order.size();
        if (numberOfGates < 1) {
            return;
        }
        int start = order.get(0);
        Gate gate = circuit.get(start);
        // Protein Engineering
        if (ProteinEngineeringIsAllowed) {
            gate.setN(1.05 * gate.getN());
            gate.setYmax(1.5 * gate.getYmax());
            gate.setYmin(gate.getYmin() / 1.5);
        }
        order.remove(0);
        circuitOptimizer(circuit, order, ProteinEngineeringIsAllowed);
        // Optimizing K
        optSolverK(gate);
        // Optimizing Offset
        //optSolverOffset(gate);
    }

    /**
     * Mathematical K parameter Optimizer using Brent method
     * 
     * @param g the gate to be optimized
     */
    private static void optSolverK(final Gate g) {
        UnivariateFunction score = new UnivariateFunction() {
            @Override
            public double value(double d) {
                double result = Double.MIN_VALUE;
                ArrayList<Double> range = g.getInputs();
                double hVal = (g.getYmin() + (g.getYmax() - g.getYmin()) / (1 + Math.pow(range.get(0) / d, g.getN())));
                double lVal = (g.getYmin() + (g.getYmax() - g.getYmin()) / (1 + Math.pow(range.get(1) / d, g.getN())));
                result = lVal / hVal;
                return result;
            }
        };

        BrentOptimizer optimizer = new BrentOptimizer(1e-5, 1e-10);
        UnivariatePointValuePair pair = new UnivariatePointValuePair(1, 0);

        try {
            pair = optimizer.optimize(new MaxEval(1000), new MaxIter(1000), new UnivariateObjectiveFunction(score), GoalType.MAXIMIZE, new SearchInterval(0, 100, g.getK())
            );
        } catch (Exception e) {
            System.out.println(e.getMessage());
        }

        g.setK(pair.getPoint());
    }

    /**
     * Mathematical Offset parameter Optimizer which is not effective yet
     * 
     * @param g gate under study
     */
    private static void optSolverOffset(Gate g) {
        double coeff = 1;
        if (g.getTo().getFrom().size() > 1) {// This gate has to be optimized for
            for (Gate gate : g.getTo().getFrom()) {
                if (!gate.equals(g)) {
                    coeff = gate.getYmax() / g.getYmax();
                }
            }
            g.setYmax(coeff * g.getYmax());
            g.setYmin(coeff * g.getYmin());
        }
    }
}
