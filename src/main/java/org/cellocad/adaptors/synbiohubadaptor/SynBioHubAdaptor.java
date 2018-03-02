package org.cellocad.adaptors.synbiohubadaptor;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.net.URISyntaxException;

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
import org.sbolstandard.core2.GenericTopLevel;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SequenceOntology;
import org.synbiohub.frontend.SynBioHubException;
import org.synbiohub.frontend.SynBioHubFrontend;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * An adaptor to build the parts library from SynBioHub.
 *
 * @author: Tim Jones
 */
public class SynBioHubAdaptor {
    private SBOLDocument celloSBOL;
    private PartLibrary partLibrary;
    private GateLibrary gateLibrary;

    private SynBioHubFrontend sbh;

    public SynBioHubAdaptor(URL url) throws SynBioHubException, IOException {
        partLibrary = new PartLibrary();
        gateLibrary = new GateLibrary(2,1);

        sbh = new SynBioHubFrontend(url.toString());

		try {
			URI u = new URL(url,"public/Cello_Parts/Cello_Parts_collection/1").toURI();
			celloSBOL = sbh.getSBOL(u);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

        createLibraries();
    }

    private void createLibraries() throws SynBioHubException, IOException {
        HashMap<String,Part> allParts = new HashMap<>();
        Map<String,URI> cytometryAttachments = new HashMap<>();
        Map<String,URI> toxicityAttachments = new HashMap<>();

        Set<ComponentDefinition> celloCD = celloSBOL.getComponentDefinitions();

        // Set<ModuleDefinition> celloMD = celloSBOL.getModuleDefinitions();
        // System.out.println(celloMD.size());
        // System.out.println(celloMD.iterator().next().getFunctionalComponents());
		for (GenericTopLevel tl : celloSBOL.getGenericTopLevels()) {
			String name = tl.getName();
			if (name.contains("cytometry")) {
				cytometryAttachments.put(name.substring(0,name.length()-15),tl.getIdentity());
			}
			if (name.contains("toxicity")) {
				toxicityAttachments.put(name.substring(0,name.length()-14),tl.getIdentity());
			}
			// URI attachmentUri = a.getIdentity();
			// String fileName = sbh.getSBOL(attachmentUri).getGenericTopLevel(attachmentUri).getName();
			// if (fileName.contains("toxicity")) {
			// 	setGateToxicityTable(attachmentUri, g);
			// }
			// if (fileName.contains("cytometry")) {
			// 	setGateCytometry(attachmentUri, g);
			// }
		}
        for (ComponentDefinition cd : celloCD) {
            // i dont know why there would be more than one type per
            // part in the cello parts there is one type per part, so
            // just grab the first type below
            URI type = cd.getTypes().iterator().next();

            if (type.equals(URI.create("http://www.biopax.org/release/biopax-level3.owl#DnaRegion"))) {
                URI role = cd.getRoles().iterator().next();

                if (role.equals(SequenceOntology.ENGINEERED_REGION)) { // if the CD is a gate

                    Gate g = new Gate();
                    g.setSynBioHubURI(cd.getIdentity());
                    g.setName(cd.getName());
					g.setComponentDefinition(cd);

                    // if a gate on synbiohub ever had more than one
                    // toxicity attachment, the last one would be what
                    // the gate gets here.
					// for (Attachment a : cd.getAttachments()) {
					// 	URI attachmentUri = a.getIdentity();
					// 	String fileName = sbh.getSBOL(attachmentUri).getGenericTopLevel(attachmentUri).getName();
					// 	if (fileName.contains("toxicity")) {
					// 		setGateToxicityTable(attachmentUri, g);
					// 	}
					// 	if (fileName.contains("cytometry")) {
					// 		setGateCytometry(attachmentUri, g);
					// 	}
					// }

                    for (Annotation a : cd.getAnnotations()) {
                        String annotationType = a.getQName().getLocalPart();
                        if (annotationType == "gate_type") {
                            g.setType(Gate.GateType.valueOf(a.getStringValue()));
                        }
                        if (annotationType == "group-name") {
                            g.setGroup(a.getStringValue());
                        }
                        if (annotationType == "family") {
                            g.setSystem(a.getStringValue());
                        }
                        if (annotationType == "gate-color-hexcode") { // color
                            g.setColorHex(a.getStringValue());
                        }
						// if (annotationType == "attachment") { // toxicity or cytometry
                        //     URI attachmentUri = a.getURIValue();
                        //     String fileName = sbh.getSBOL(attachmentUri).getGenericTopLevel(attachmentUri).getName();
                        //     if (fileName.contains("toxicity")) {
                        //         setGateToxicityTable(attachmentUri, g);
                        //     }
                        //     if (fileName.contains("cytometry")) {
                        //         setGateCytometry(attachmentUri, g);
                        //     }
                        // }
                    }
					setGateToxicityTable(toxicityAttachments.get(g.getName()),g);
					setGateCytometry(cytometryAttachments.get(g.getName()),g);

                    gateLibrary.get_GATES_BY_NAME().put(g.getName(), g);
                    gateLibrary.setHashMapsForGates();

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

                    String seq = cd.getSequences().iterator().next().getElements();
					
					Part p = new Part(name, roleString, seq, cd.getIdentity());
					p.setComponentDefinition(cd);
                    allParts.put(name, p);
                }
            }
        }
        partLibrary.set_ALL_PARTS(allParts);
    }

    private void setGateCytometry(URI uri, Gate gate) throws MalformedURLException, IOException {
        URL url = new URL(uri.toString() + "/download");
        String cytometryJson = getURLContentsAsString(url);

        Integer nbins = 0;
        Double logmin = 0.0;
        Double logmax = 0.0;

        ArrayList<double[]> xfer_binned = new ArrayList<double[]>();
        ArrayList<Double> xfer_titration_inputRPUs = new ArrayList<Double>();

        JsonParser parser = new JsonParser();
        JsonArray jsonArray = parser.parse(cytometryJson).getAsJsonArray();

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
    private void setGateToxicityTable(URI uri, Gate g) throws MalformedURLException, IOException {
        URL url = new URL(uri.toString() + "/download");
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
		Map<String,Gate> gatesMap = this.gateLibrary.get_GATES_BY_NAME();
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
					String partName = c.getName();

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
			String responseFunction = cd.getAnnotation(new QName("http://wiki.synbiohub.org/wiki/Terms/cello#","response-function")).getStringValue();

			if (gateLibrary.get_GATES_BY_NAME().containsKey(gateName)) {
                HashMap<String, Double> gate_params = new HashMap<String, Double>();
                HashMap<String, Double[]> gate_variables = new HashMap<String, Double[]>();

                ArrayList<String> gate_variable_names = new ArrayList<String>();

				for (String name : Arrays.asList("ymax","ymin","K","n")) {
					QName qName = new QName("http://wiki.synbiohub.org/wiki/Terms/cello#",name);
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

    /**
     * @return the SBOLDocument containing the SBOL of an arbitrary URI
     */
    public SBOLDocument getSBOL(URI uri) throws SynBioHubException {
        return sbh.getSBOL(uri);
    }

    /**
     * @return the SBOLDocument containing the parts library
     */
    public SBOLDocument getCelloSBOL() {
        return celloSBOL;
    }

    /**
     * @return the PartLibrary object that was build from the SBOL
     */
    public PartLibrary getPartLibrary() {
        return partLibrary;
    }

    /**
     * @return the gateLibrary
     */
    public GateLibrary getGateLibrary() {
        return gateLibrary;
    }
}
