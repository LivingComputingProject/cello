/**
 * Copyright (C) 2018 Boston University (BU)
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:

 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.

 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.cellocad.adaptors.synbiohubadaptor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.cellocad.MIT.dnacompiler.Gate;
import org.cellocad.MIT.dnacompiler.GateLibrary;
import org.cellocad.MIT.dnacompiler.HistogramUtil;
import org.cellocad.MIT.dnacompiler.Pair;
import org.cellocad.MIT.dnacompiler.Part;
import org.cellocad.MIT.dnacompiler.PartLibrary;
import org.sbolstandard.core2.Annotation;
import org.sbolstandard.core2.Attachment;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SequenceOntology;
import org.sbolstandard.core2.SystemsBiologyOntology;
import org.synbiohub.frontend.SynBioHubException;
import org.synbiohub.frontend.SynBioHubFrontend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * An adaptor to build the parts library from SynBioHub.
 *
 * @author: Timothy Jones
 * @date: Mar 2, 2018
 *
 */
public class SynBioHubAdaptor {
    private SBOLDocument celloSBOL;
    private PartLibrary partLibrary;
    private GateLibrary gateLibrary;

    private SynBioHubFrontend synBioHubFrontend;

    public SynBioHubAdaptor(URL url) throws SynBioHubException, IOException {
        this.setPartLibrary(new PartLibrary());
        this.setGateLibrary(new GateLibrary(2,1));

        this.setSynBioHubFrontend(new SynBioHubFrontend(url.toString()));

		try {
			URI u = new URL(url,"public/Cello_Parts/Cello_Parts_collection/1").toURI();
            SBOLDocument sbol = this.getSynBioHubFrontend().getSBOL(u);
            sbol.setDefaultURIprefix(url.toString());
			this.setCelloSBOL(sbol);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

        createLibraries();
        getInputPromoterRepressorCDSMap();
    }

    private void createLibraries() throws SynBioHubException, IOException {
        HashMap<String,Part> allParts = new HashMap<>();
        Map<String,URI> cytometryAttachments = new HashMap<>();
        Map<String,URI> toxicityAttachments = new HashMap<>();

        Set<ComponentDefinition> celloCD = this.getCelloSBOL().getComponentDefinitions();

        for (ComponentDefinition cd : celloCD) {
            // i dont know why there would be more than one type per
            // part in the cello parts there is one type per part, so
            // just grab the first type below
            URI type = cd.getTypes().iterator().next();

            if (type.equals(ComponentDefinition.DNA)) {
                URI role = cd.getRoles().iterator().next();

                if (role.equals(SequenceOntology.ENGINEERED_REGION)) { // if the CD is a gate

                    Gate g = new Gate();
                    g.setSynBioHubURI(cd.getIdentity());
                    g.setName(cd.getName());
					g.setComponentDefinition(cd);

                    for (Annotation a : cd.getAnnotations()) {
                        String annotationType = a.getQName().getLocalPart();
                        if (annotationType == "gate_type") {
                            g.setType(Gate.GateType.valueOf(a.getStringValue()));
                        }
                        if (annotationType == "group_name") {
                            g.setGroup(a.getStringValue());
                        }
                        if (annotationType == "family") {
                            g.setSystem(a.getStringValue());
                        }
                        if (annotationType == "color_hexcode") { // color
                            g.setColorHex(a.getStringValue());
                        }
                    }
                    Set<Attachment> attachments = cd.getAttachments();
                    if (attachments == null) {
                        throw new RuntimeException("ComponentDefinition " + cd.getName() + " has no attachments!");
                    }
                    for (Attachment a : attachments) {
                        if (a.getName().contains("toxicity")) {
                            setGateToxicityTable(a, g);
                        }
                        if (a.getName().contains("cytometry")) {
                            setGateCytometry(a, g);
                        }
                    }
					// setGateToxicityTable(toxicityAttachments.get(g.getName()),g);
					// setGateCytometry(cytometryAttachments.get(g.getName()),g);

                    this.getGateLibrary().get_GATES_BY_NAME().put(g.getName(), g);
                    this.getGateLibrary().setHashMapsForGates();

                } else { // otherwise it's a part
                    String name = cd.getName();
                    String roleString = "NOT_SET";
                    if (role.equals(SequenceOntology.PROMOTER))
                        roleString = "promoter";
                    if (role.equals(SequenceOntology.CDS))
                        roleString = "cds";
                    if (role.equals(SequenceOntology.RIBOSOME_ENTRY_SITE))
                        roleString = "rbs";
                    if (role.equals(SequenceOntology.TERMINATOR))
                        roleString = "terminator";
                    if (role.equals(URI.create("http://identifiers.org/so/SO:0000374")))
                        roleString = "ribozyme";
                    if (role.equals(URI.create("http://identifiers.org/so/SO:0001953")))
                        roleString = "scar";

                    if (cd.getSequences().size() > 0) {
                        String seq = cd.getSequences().iterator().next().getElements();
					
                        Part p = new Part(name, roleString, seq, cd.getIdentity());
                        p.setComponentDefinition(cd);
                        allParts.put(name, p);
                    }
                }
            }
        }
        this.getPartLibrary().set_ALL_PARTS(allParts);
    }

    private void setGateCytometry(Attachment a, Gate gate) throws MalformedURLException, IOException {
		URL url = a.getSource().toURL();
        String cytometryJson = getURLContentsAsString(url);

        Integer nbins = 0;
        Double logmin = 0.0;
        Double logmax = 0.0;

        ArrayList<double[]> xfer_binned = new ArrayList<double[]>();
        ArrayList<Double> xfer_titration_inputRPUs = new ArrayList<Double>();

        JsonParser parser = new JsonParser();
        JsonArray jsonArray = parser.parse(cytometryJson).getAsJsonObject().get("cytometry_data").getAsJsonArray();

        Gson gson = new Gson();
        for (int i = 0; i < jsonArray.size(); ++i) {
            JsonObject jsonObject = jsonArray.get(i).getAsJsonObject();
            Double input = jsonObject.get("input").getAsDouble();
            List<Double> outputBins = gson.fromJson(jsonObject.get("output_bins").getAsJsonArray(), ArrayList.class);
            List<Double> outputCounts = gson.fromJson(jsonObject.get("output_counts").getAsJsonArray(), ArrayList.class);

            if (i==0) {
                nbins = outputBins.size();
                logmin = Math.log10(outputBins.get(0));
                logmax = Math.log10(outputBins.get(outputBins.size() - 1));
            }
            else {
                double current_logmin = Math.log10(outputBins.get(0));
                double current_logmax = Math.log10(outputBins.get(outputBins.size() - 1));
            }
            xfer_titration_inputRPUs.add(input);

            double[] xfer_titration_counts = new double[outputCounts.size()];
            for (int b = 0; b < outputCounts.size(); ++b) {
                xfer_titration_counts[b] = outputCounts.get(b);
            }

            double[] xfer_normalized = HistogramUtil.normalize(xfer_titration_counts);
            xfer_binned.add(xfer_normalized);

        }
        gate.get_histogram_bins().init();
        gate.get_histogram_bins().set_NBINS( nbins );
        gate.get_histogram_bins().set_LOGMAX( logmax );
        gate.get_histogram_bins().set_LOGMIN( logmin );

        gate.get_xfer_hist().set_xfer_titration(xfer_titration_inputRPUs);

        gate.get_xfer_hist().set_xfer_binned(xfer_binned);
    }

    /**
     * @return the toxicity table
     */
    private void setGateToxicityTable(Attachment a, Gate g) throws MalformedURLException, IOException {
        URL url = a.getSource().toURL();
        String toxicityJson = getURLContentsAsString(url);

        JsonParser parser = new JsonParser();
        JsonObject jsonObject = parser.parse(toxicityJson).getAsJsonObject();

        Gson gson = new Gson();
        List<Double> input = gson.fromJson(jsonObject.get("input"), ArrayList.class);
        List<Double> growth = gson.fromJson(jsonObject.get("growth"), ArrayList.class);

        ArrayList<Pair> toxtable = new ArrayList<Pair>();
        for (int i = 0; i < input.size(); ++i) {
            toxtable.add(new Pair(input.get(i), growth.get(i)));
        }

        g.set_toxtable(toxtable);
    }

    /**
     * @return the URL contents (uncompressed) as a String
     */
    private String getURLContentsAsString(URL url) throws IOException {
        HttpClientBuilder builder = HttpClientBuilder.create();
        CloseableHttpClient httpClient = builder.build();
        HttpGet httpGet = new HttpGet(url.toString());
        HttpResponse httpResponse = httpClient.execute(httpGet);
        return EntityUtils.toString(httpResponse.getEntity());
    }

	public void setGateParts(GateLibrary gateLibrary, PartLibrary partLibrary) {
		Map<String,Gate> gatesMap = this.getGateLibrary().get_GATES_BY_NAME();
		for (String gateName : gatesMap.keySet()) {
			Gate g = gatesMap.get(gateName);
			ComponentDefinition cd = g.getComponentDefinition();
			Set<Component> components = cd.getComponents();

			// adapted from UCFAdaptor
			if(gateLibrary.get_GATES_BY_NAME().containsKey(gateName)) {

				HashMap<String, ArrayList<Part>> downstream_gate_parts = new HashMap<>();

				//regulable promoter
				String promoter_name = "p" + g.getGroup();

				if(!partLibrary.get_ALL_PARTS().containsKey(promoter_name)) {
					throw new IllegalStateException("reading part not found " + promoter_name);
				}

				Part regulable_promoter = new Part(partLibrary.get_ALL_PARTS().get(promoter_name));


				//downstream parts (can be >1 for multi-input logic gates)

				ArrayList<Part> parts = new ArrayList<Part>();

				for (Component c : components) {
					String partName = c.getDisplayId();
					if(!partLibrary.get_ALL_PARTS().containsKey(partName)) {
						throw new IllegalStateException("reading part not found " + partName);
					}

					Part p = partLibrary.get_ALL_PARTS().get(partName);

					parts.add(p);

				}

				downstream_gate_parts.put("x", parts);

				gateLibrary.get_GATES_BY_NAME().get(gateName).set_downstream_parts(downstream_gate_parts);
				gateLibrary.get_GATES_BY_NAME().get(gateName).set_regulable_promoter(regulable_promoter);
			}
		}
	}

	public void setResponseFunctions(GateLibrary gateLibrary) {
		Map<String,Gate> gatesMap = this.gateLibrary.get_GATES_BY_NAME();
		for (String gateName : gatesMap.keySet()) {
			Gate g = gatesMap.get(gateName);
			ComponentDefinition cd = g.getComponentDefinition();
			String responseFunction = cd.getAnnotation(new QName("http://cellocad.org/Terms/cello#","response_function")).getStringValue();

			if (gateLibrary.get_GATES_BY_NAME().containsKey(gateName)) {
                HashMap<String, Double> gate_params = new HashMap<String, Double>();
                HashMap<String, Double[]> gate_variables = new HashMap<String, Double[]>();

                ArrayList<String> gate_variable_names = new ArrayList<String>();

				for (String name : Arrays.asList("ymax","ymin","K","n")) {
					QName qName = new QName("http://cellocad.org/Terms/cello#",name);
					Annotation a = cd.getAnnotation(qName);
					gate_params.put(name,Double.valueOf(cd.getAnnotation(qName).getStringValue()));
				}

				gate_variable_names.add("x");
				Double off_threshold = gate_params.get("K")*Math.pow((gate_params.get("ymax")/2.0)/(gate_params.get("ymax")/2.0-gate_params.get("ymin")),
																	 1.0/gate_params.get("n"));
				Double on_threshold = gate_params.get("K")*Math.pow((gate_params.get("ymax")-gate_params.get("ymin")*2.0)/gate_params.get("ymin"),
																	1.0/gate_params.get("n"));

				Double[] thresholds = {off_threshold, on_threshold};

				gate_variables.put("x", thresholds);

                Gate gate = gateLibrary.get_GATES_BY_NAME().get(gateName);

                if (g != null) {

                    for (String v : gate_variables.keySet()) {
                        g.get_variable_wires().put(v, null);
                    }

                    g.set_params(gate_params);
                    g.set_variable_names(gate_variable_names);
                    g.set_variable_thresholds(gate_variables);
                    g.set_equation(responseFunction);
                }
            }
		}
	}

    public Map<URI,URI> getInputPromoterRepressorCDSMap() {
        Map<URI,URI> map = new HashMap<>();

		// query for input sensor to cds, e.g. pTet -> TetR
		String query = "PREFIX dcterms: <http://purl.org/dc/terms/>\n"
			+ "PREFIX dc: <http://purl.org/dc/elements/1.1/>\n"
			+ "PREFIX sbh: <http://wiki.synbiohub.org/wiki/Terms/synbiohub#>\n"
			+ "PREFIX prov: <http://www.w3.org/ns/prov#>\n"
			+ "PREFIX sbol: <http://sbols.org/v2#>\n"
			+ "\n"
			+ "select ?promoter ?cds where\n"
			+ "{\n"
			+ "  {\n"
			+ "    ?promoter a sbol:ComponentDefinition .\n"
			+ "    ?promoter sbol:type <http://www.biopax.org/release/biopax-level3.owl#DnaRegion> .\n"
			+ "\n"
			+ "    ?cds a sbol:ComponentDefinition .\n"
			+ "    ?cds sbol:type <http://www.biopax.org/release/biopax-level3.owl#DnaRegion> .\n"
			+ "\n"
			+ "    ?repressor a sbol:ComponentDefinition .  \n"
			+ "    ?repressor sbol:type <http://www.biopax.org/release/biopax-level3.owl#Protein> .\n"
			+ "\n"
			+ "    ?repression a sbol:ModuleDefinition .\n"
			+ "    ?repression sbol:interaction ?repressionInt .\n"
			+ "    ?repressionInt sbol:type <http://identifiers.org/biomodels.sbo/SBO:0000169> .\n"
			+ "    ?repression sbol:functionalComponent ?promoterComp .\n"
			+ "    ?promoterComp sbol:definition ?promoter .\n"
			+ "    ?repression sbol:functionalComponent ?repressorComp .\n"
			+ "    ?repressorComp sbol:definition ?repressor .\n"
			+ "\n"
			+ "    ?production a sbol:ModuleDefinition .\n"
			+ "    ?production sbol:interaction ?productionInt .\n"
			+ "    ?productionInt sbol:type <http://identifiers.org/biomodels.sbo/SBO:0000589> .\n"
			+ "    ?production sbol:functionalComponent ?cdsComp .\n"
			+ "    ?cdsComp sbol:definition ?cds .\n"
			+ "    ?production sbol:functionalComponent ?repressorComp2 .\n"
			+ "    ?repressorComp2 sbol:definition ?repressor .\n"
			+ "  } union {\n"
			+ "    ?promoter a sbol:ComponentDefinition .\n"
			+ "    ?promoter sbol:type <http://www.biopax.org/release/biopax-level3.owl#DnaRegion> .\n"
			+ "\n"
			+ "    ?cds a sbol:ComponentDefinition .\n"
			+ "    ?cds sbol:type <http://www.biopax.org/release/biopax-level3.owl#DnaRegion> .\n"
			+ "\n"
			+ "    ?activator a sbol:ComponentDefinition .  \n"
			+ "    ?activator sbol:type <http://www.biopax.org/release/biopax-level3.owl#Complex> .\n"
			+ "\n"
			+ "    ?reactant a sbol:ComponentDefinition .  \n"
			+ "    ?reactant sbol:type <http://www.biopax.org/release/biopax-level3.owl#Protein> .\n"
			+ "\n"
			+ "    ?activation a sbol:ModuleDefinition .\n"
			+ "    ?activation sbol:interaction ?activationInt .\n"
			+ "    ?activationInt sbol:type <http://identifiers.org/biomodels.sbo/SBO:0000170> .\n"
			+ "    ?activation sbol:functionalComponent ?promoterComp .\n"
			+ "    ?promoterComp sbol:definition ?promoter .\n"
			+ "    ?activation sbol:functionalComponent ?activatorComp .\n"
			+ "    ?activatorComp sbol:definition ?activator .\n"
			+ "\n"
			+ "    ?formation a sbol:ModuleDefinition .\n"
			+ "    ?formation sbol:interaction ?formationInt .\n"
			+ "    ?formationInt sbol:type <http://identifiers.org/biomodels.sbo/SBO:0000177> .\n"
			+ "    ?formation sbol:functionalComponent ?activatorComp2 .\n"
			+ "    ?activatorComp2 sbol:definition ?activator .\n"
			+ "    ?formation sbol:functionalComponent ?reactantComp .\n"
			+ "    ?reactantComp sbol:definition ?reactant .\n"
			+ "\n"
			+ "    ?production a sbol:ModuleDefinition .\n"
			+ "    ?production sbol:interaction ?productionInt .\n"
			+ "    ?productionInt sbol:type <http://identifiers.org/biomodels.sbo/SBO:0000589> .\n"
			+ "    ?production sbol:functionalComponent ?cdsComp .\n"
			+ "    ?cdsComp sbol:definition ?cds .\n"
			+ "    ?production sbol:functionalComponent ?reactantComp2 .\n"
			+ "    ?reactantComp2 sbol:definition ?reactant .\n"
			+ "  }\n"
			+ "}\n";

        String response = null;
        try {
            response = this.getSynBioHubFrontend().sparqlQuery(query);
        } catch (SynBioHubException e) {
            e.printStackTrace();
        }
        JsonParser parser = new JsonParser();
        JsonArray jsonArray = parser.parse(response).getAsJsonObject()
            .get("results").getAsJsonObject()
            .get("bindings").getAsJsonArray();
        for (JsonElement el : jsonArray) {
            URI promoter = URI.create(el.getAsJsonObject().get("promoter").getAsJsonObject().get("value").getAsString());
            URI cds = URI.create(el.getAsJsonObject().get("cds").getAsJsonObject().get("value").getAsString());
            map.put(promoter,cds);
        }
        return map;
    }

    /**
     * @return the SBOLDocument containing the SBOL of an arbitrary URI
     */
    public SBOLDocument getSBOL(URI uri) throws SynBioHubException {
        return synBioHubFrontend.getSBOL(uri);
    }

    /**
     * @return the SBOLDocument containing the parts library
     */
    public SBOLDocument getCelloSBOL() {
        return celloSBOL;
    }

    /**
     * @param celloSBOL the SBOLDocument to set
     */
    private void setCelloSBOL(SBOLDocument celloSBOL) {
        this.celloSBOL = celloSBOL;
    }

    /**
     * @return the PartLibrary object that was build from the SBOL
     */
    public PartLibrary getPartLibrary() {
        return partLibrary;
    }

    /**
     * @param partLibrary the PartLibrary to set
     */
    private void setPartLibrary(PartLibrary partLibrary) {
        this.partLibrary = partLibrary;
    }

    /**
     * @return the gateLibrary
     */
    public GateLibrary getGateLibrary() {
        return gateLibrary;
    }

    /**
     * @param gateLibrary the GateLibrary to set
     */
    private void setGateLibrary(GateLibrary gateLibrary) {
        this.gateLibrary = gateLibrary;
    }

    /**
     * @return the synBioHubFrontend
     */
    public SynBioHubFrontend getSynBioHubFrontend() {
        return synBioHubFrontend;
    }

    /**
     * @param synBioHubFrontend the SynBioHubFrontend to set
     */
    private void setSynBioHubFrontend(SynBioHubFrontend synBioHubFrontend) {
        this.synBioHubFrontend = synBioHubFrontend;
    }

}
