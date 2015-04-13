/**
 * Modified from OWL API's OWLFunctionalSyntaxOntologyFormat class.
 */

package edu.buffalo.bmi.csneps;

import java.io.IOException;
import java.io.Writer;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.io.AbstractOWLRenderer;
import org.semanticweb.owlapi.io.OWLRendererException;
import org.semanticweb.owlapi.io.OWLRendererIOException;
import org.semanticweb.owlapi.model.OWLOntology;

public class OWLCSNePSSyntaxRenderer extends AbstractOWLRenderer {

    @Override
    public void render(@Nonnull OWLOntology ontology, @Nonnull Writer writer)
            throws OWLRendererException {
        try {
        	CSNePSSyntaxRenderer ren = new CSNePSSyntaxRenderer(
                    ontology, writer);
            ontology.accept(ren);
            writer.flush();
        } catch (IOException e) {
            throw new OWLRendererIOException(e);
        }
    }
	
}
