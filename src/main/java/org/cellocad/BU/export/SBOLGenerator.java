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
package org.cellocad.BU.export;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.cellocad.MIT.dnacompiler.Gate;
import org.cellocad.MIT.dnacompiler.LogicCircuit;
import org.cellocad.MIT.dnacompiler.Part;
import org.cellocad.adaptors.synbiohubadaptor.SynBioHubAdaptor;
import org.sbolstandard.core2.AccessType;
import org.sbolstandard.core2.Component;
import org.sbolstandard.core2.ComponentDefinition;
import org.sbolstandard.core2.RestrictionType;
import org.sbolstandard.core2.SBOLConversionException;
import org.sbolstandard.core2.SBOLDocument;
import org.sbolstandard.core2.SBOLValidationException;
import org.sbolstandard.core2.Sequence;
import org.sbolstandard.core2.SequenceAnnotation;
import org.sbolstandard.core2.SequenceOntology;
import org.synbiohub.frontend.SynBioHubException;
import org.virtualparts.VPRException;
import org.virtualparts.VPRTripleStoreException;
import org.virtualparts.data.SBOLInteractionAdder_GeneCentric;

/**
 * Generate an SBOL document based on transcriptional units.
 *
 * @author: Timothy Jones
 * @date: Mar 2, 2018
 *
 */
public class SBOLGenerator {

	/**
	 * Perform VPR model generation. 
	 * @param selectedRepo - The specified synbiohub repository the user wants VPR model generator to connect to. 
	 * @param generatedModel - The file to generate the model from.
	 * @return The generated model.
	 * @throws SBOLValidationException
	 * @throws IOException - Unable to read or write the given SBOLDocument
	 * @throws SBOLConversionException - Unable to perform conversion for the given SBOLDocument.
	 * @throws VPRException - Unable to perform VPR Model Generation on the given SBOLDocument.
	 * @throws VPRTripleStoreException - Unable to perform VPR Model Generation on the given SBOLDocument.
	 */
	public static SBOLDocument generateModel(URL selectedRepo, SBOLDocument generatedModel) throws SBOLValidationException, IOException, SBOLConversionException, VPRException, VPRTripleStoreException, URISyntaxException
	{
		URI endpoint = new URL(selectedRepo,"/sparql").toURI();
		SBOLInteractionAdder_GeneCentric interactionAdder = new SBOLInteractionAdder_GeneCentric(endpoint);
		interactionAdder.addInteractions(generatedModel);
		return generatedModel;
	}

	public static SBOLDocument generateSBOLDocument(LogicCircuit lc, SynBioHubAdaptor sbhAdaptor)
		throws SynBioHubException, SBOLValidationException {
		Validate.notNull(lc,"LogicCircuit cannot be null.");
		Validate.notNull(sbhAdaptor,"SynBioHub adaptor cannot be null.");
        
		SBOLDocument sbolDocument = new SBOLDocument();
		sbolDocument.setDefaultURIprefix("http://cellocad.org/");
		
		Map<String,List<Part>> txnUnitsMap = new HashMap<>();

		List<Gate> gates = new ArrayList<>(lc.get_logic_gates());
		gates.addAll(lc.get_output_gates());
		Set<Part> parts = new HashSet<>();
		
		for (Gate g : gates) {
			List<List<Part>> txnUnit = g.get_txn_units();
			if (txnUnit.size() > 1) {
				System.err.println("Multiple transcriptional units defined for gate " + g.getName() +
								   ", using only the first one to generate SBOLDocument.");
			}
			List<Part> unit = txnUnit.get(0);
			txnUnitsMap.put(g.getName(),unit);
			for (Part p : unit) {
				parts.add(p);
			}
		}

		for (Part p : parts) {
			URI uri = p.getSynBioHubURI();
			ComponentDefinition cd = null;
			if ((uri != null) && (sbhAdaptor != null)) {
				SBOLDocument partSbol = sbhAdaptor.getSBOL(uri);
				cd = partSbol.getComponentDefinition(uri);
				sbolDocument.createCopy(cd);
				Set<Sequence> sequences = cd.getSequences();
				if (sequences != null) {
					for (Sequence s : sequences) {
						sbolDocument.createCopy(s);
					}
				} else {
					sbolDocument.createSequence(p.get_name() + "_sequence",p.get_seq(),Sequence.IUPAC_DNA);
				}
			} else {
				cd = sbolDocument.createComponentDefinition(p.get_name(),ComponentDefinition.DNA);
				sbolDocument.createSequence(p.get_name() + "_sequence",p.get_seq(),Sequence.IUPAC_DNA);
			}
		}

        // make transcriptional units for input sen
        Part pConst = sbhAdaptor.getPartLibrary().get_ALL_PARTS().get("pCONST");
        sbolDocument.createCopy(pConst.getComponentDefinition());
        Map<URI,URI> promoterMap = sbhAdaptor.getInputPromoterRepressorCDSMap();
        for (Gate g : lc.get_input_gates()) {
            ComponentDefinition promoterCd = g.get_regulable_promoter().getComponentDefinition();
            URI u = promoterMap.get(promoterCd.getIdentity());
            ComponentDefinition cd = sbhAdaptor.getCelloSBOL().getComponentDefinition(u);
            sbolDocument.createCopy(cd);
            List<Part> unit = new ArrayList<>();
            Part p = sbhAdaptor.getPartLibrary().get_ALL_PARTS().get(cd.getName());
            unit.add(pConst);
            unit.add(p);
            txnUnitsMap.put(cd.getName(),unit);
        }

        // create component definitions for transcriptional units
        // add sequences, sequence annotations, sequence constraints, and components
		for (String gate : txnUnitsMap.keySet()) {
			String promoterPrefix = "";
			for (Part p : txnUnitsMap.get(gate)) {
				if (p.get_type().equals("promoter")) {
					promoterPrefix += p.get_name() + "_";
				}
			}
			String txnUnitName = promoterPrefix + gate;
			URI uri = URI.create("http://cellocad.org/" + txnUnitName);
			ComponentDefinition cd = null;
			cd = sbolDocument.createComponentDefinition(txnUnitName,ComponentDefinition.DNA);
			cd.addRole(SequenceOntology.ENGINEERED_REGION);

			int seqCounter = 1;
			String sequence = "";
			for (int j = 0; j < txnUnitsMap.get(gate).size(); j++) {
				Part p = txnUnitsMap.get(gate).get(j);
				Component c = cd.createComponent(p.get_name(),AccessType.PUBLIC,p.getComponentDefinition().getIdentity());
				SequenceAnnotation sa =
					cd.createSequenceAnnotation("SequenceAnnotation" + String.valueOf(j),
												"SequenceAnnotation" + String.valueOf(j) + "_Range",
												seqCounter,
												seqCounter + p.get_seq().length());
				sa.setComponent(c.getIdentity());
				seqCounter += p.get_seq().length() + 1;
				sequence += p.get_seq();
				if (j != 0) {
					cd.createSequenceConstraint(cd.getDisplayId() + "Constraint" + String.valueOf(j),
												RestrictionType.PRECEDES,
												cd.getComponent(txnUnitsMap.get(gate).get(j-1).get_name()).getIdentity(),
												cd.getComponent(p.get_name()).getIdentity());
				}
			}
			Sequence s = sbolDocument.createSequence(cd.getDisplayId() + "_sequence",sequence,Sequence.IUPAC_DNA);
			cd.addSequence(s);
		}

		return sbolDocument;
	}

}
