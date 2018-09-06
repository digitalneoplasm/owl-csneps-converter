/**
 * Class to render OWL objects as a CSNePS KB.
 * 
 * Modified from OWL API's FunctionalSyntaxOntologyFormat class.
 * 
 * Author: Daniel R. Schlegel
 * Created: 4/12/2015
 */

package edu.buffalo.bmi.csneps;

import static org.semanticweb.owlapi.model.parameters.Imports.*;
import static org.semanticweb.owlapi.util.CollectionFactory.sortOptionally;
import static org.semanticweb.owlapi.vocab.OWLXMLVocabulary.*;

import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nonnull;

import org.semanticweb.owlapi.formats.PrefixDocumentFormat;
import org.semanticweb.owlapi.model.*;
import org.semanticweb.owlapi.model.parameters.Imports;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.EscapeUtils;
import org.semanticweb.owlapi.vocab.OWLXMLVocabulary;

import com.google.common.base.Optional;

public class CSNePSSyntaxRenderer implements OWLObjectVisitor {

    private PrefixManager prefixManager;
    protected final OWLOntology ont;
    private final Writer writer;
    private boolean writeEntitiesAsURIs = true;
    private OWLObject focusedObject;
    private boolean addMissingDeclarations = true;
    private int uniqueId;

    /**
     * @param ontology
     *        the ontology
     * @param writer
     *        the writer
     */
    public CSNePSSyntaxRenderer(@Nonnull OWLOntology ontology,
            Writer writer) {
        ont = ontology;
        this.writer = writer;
        prefixManager = new DefaultPrefixManager();
        OWLDocumentFormat ontologyFormat = ontology.getOWLOntologyManager()
                .getOntologyFormat(ontology);
        // reuse the setting on the existing format, if there is one
        if (ontologyFormat != null) {
            addMissingDeclarations = ontologyFormat.isAddMissingTypes();
        }
        if (ontologyFormat instanceof PrefixDocumentFormat) {
            prefixManager
                    .copyPrefixesFrom((PrefixDocumentFormat) ontologyFormat);
            prefixManager
                    .setPrefixComparator(((PrefixDocumentFormat) ontologyFormat)
                            .getPrefixComparator());
        }
        if (!ontology.isAnonymous()) {
            String existingDefault = prefixManager.getDefaultPrefix();
            String ontologyIRIString = ontology.getOntologyID()
                    .getOntologyIRI().get().toString();
            if (existingDefault == null
                    || !existingDefault.startsWith(ontologyIRIString)) {
                String defaultPrefix = ontologyIRIString;
                if (!ontologyIRIString.endsWith("/")) {
                    defaultPrefix = ontologyIRIString + '#';
                }
                prefixManager.setDefaultPrefix(defaultPrefix);
            }
        }
        focusedObject = ontology.getOWLOntologyManager().getOWLDataFactory()
                .getOWLThing();
    }

    /**
     * Set the add missing declaration flag.
     * 
     * @param flag
     *        new value
     */
    public void setAddMissingDeclarations(boolean flag) {
        addMissingDeclarations = flag;
    }

    /**
     * @param prefixManager
     *        the new prefix manager
     */
    public void setPrefixManager(PrefixManager prefixManager) {
        this.prefixManager = prefixManager;
    }

    /**
     * @param focusedObject
     *        the new focused object
     */
    protected void setFocusedObject(OWLObject focusedObject) {
        this.focusedObject = focusedObject;
    }

    protected void
            writePrefix(@Nonnull String prefix, @Nonnull String namespace) {
    	writeOpenBracket();
        write("Prefix");
        writeSpace();
        write("\"" + prefix);
        write("=");
        write("<");
        write(namespace);
        write(">\"");
        writeCloseBracket();
        writeReturn();
    }

    protected void writeIsa() {
	write("Isa");
	writeSpace();
    }

    protected void writeArb() {
	write("every");
	writeSpace();
    }

    protected void writeArbIsa(OWLClassExpression subclass) {
	writeOpenBracket();
	writeArb();
	write("x");
	write(""+ ++uniqueId);
	writeSpace();
	writeOpenBracket();
	writeIsa();
	write("x" + uniqueId);
	writeSpace();
	subclass.accept(this);
	writeCloseBracket();
	writeCloseBracket();
    }

    protected void writeInd() {
	write("exists");
	writeSpace();
    }

    @SuppressWarnings("null")
    protected void writePrefixes() {
        for (Map.Entry<String, String> e : prefixManager
                .getPrefixName2PrefixMap().entrySet()) {
            writePrefix(e.getKey(), e.getValue());
        }
    }

    private void write(@Nonnull OWLXMLVocabulary v) {
        write(v.getShortForm());
    }

    private void write(@Nonnull String s) {
        try {
            writer.write(s);
        } catch (IOException e) {
            throw new OWLRuntimeException(e);
        }
    }

    private void flush() {
        try {
            writer.flush();
        } catch (IOException e) {
            throw new OWLRuntimeException(e);
        }
    }

    private void write(@Nonnull IRI iri) {
        String qname = prefixManager.getPrefixIRI(iri);
        if (qname != null) {
            boolean lastCharIsColon = qname.charAt(qname.length() - 1) == ':';
            if (!lastCharIsColon) {
                write(qname);
                return;
            }
        }
        writeFullIRI(iri);
    }

    private void writeFullIRI(@Nonnull IRI iri) {
        write("<");
        write(iri.toString());
        write(">");
    }

    @SuppressWarnings("null")
    @Override
    public void visit(@Nonnull OWLOntology ontology) {
        writePrefixes();
        writeReturn();
        writeReturn();
        writeOpenBracket();
        write(ONTOLOGY);
        writeSpace();
        if (!ontology.isAnonymous()) {
            writeFullIRI(ontology.getOntologyID().getOntologyIRI().get());
            writeCloseBracket();
            Optional<IRI> versionIRI = ontology.getOntologyID().getVersionIRI();
            if (versionIRI.isPresent()) {
                writeReturn();
                writeFullIRI(versionIRI.get());
            }
            writeReturn();
        }
        for (OWLImportsDeclaration decl : ontology.getImportsDeclarations()) {
        	writeOpenBracket();
        	write(IMPORT);
            writeSpace();
            writeFullIRI(decl.getIRI());
            writeCloseBracket();
            writeReturn();
        }
        for (OWLAnnotation ontologyAnnotation : ontology.getAnnotations()) {
            ontologyAnnotation.accept(this);
            writeReturn();
        }
        writeReturn();
        Set<OWLAxiom> writtenAxioms = new HashSet<>();
        List<OWLEntity> signature = sortOptionally(ontology.getSignature());
        Collection<IRI> illegals = OWLDocumentFormatImpl
                .determineIllegalPunnings(addMissingDeclarations, signature,
                        ont.getPunnedIRIs(INCLUDED));
        for (OWLEntity ent : signature) {
            writeDeclarations(ent, writtenAxioms, illegals);
        }
        for (OWLEntity ent : signature) {
            writeAxioms(ent, writtenAxioms);
        }
        for (OWLAxiom ax : ontology.getAxioms()) {
            if (!writtenAxioms.contains(ax)) {
                ax.accept(this);
                writeReturn();
            }
        }
        //writeCloseBracket();
        flush();
    }

    /**
     * Writes out the axioms that define the specified entity.
     * 
     * @param entity
     *        The entity
     * @return The set of axioms that was written out
     */
    @Nonnull
    protected Set<OWLAxiom> writeAxioms(@Nonnull OWLEntity entity) {
        Set<OWLAxiom> writtenAxioms = new HashSet<>();
        writeAxioms(entity, writtenAxioms);
        return writtenAxioms;
    }

    private void writeAxioms(@Nonnull OWLEntity entity,
            @Nonnull Set<OWLAxiom> alreadyWrittenAxioms) {
        setFocusedObject(entity);
        writeAnnotations(entity, alreadyWrittenAxioms);
        List<? extends OWLAxiom> axs = entity
                .accept(new OWLEntityVisitorEx<List<? extends OWLAxiom>>() {

                    @Override
                    public List<? extends OWLAxiom> visit(OWLClass cls) {
                        return sortOptionally(ont.getAxioms(cls, EXCLUDED));
                    }

                    @Override
                    public List<? extends OWLAxiom> visit(
                            OWLObjectProperty property) {
                        return sortOptionally(ont.getAxioms(property, EXCLUDED));
                    }

                    @Override
                    public List<? extends OWLAxiom> visit(
                            OWLDataProperty property) {
                        return sortOptionally(ont.getAxioms(property, EXCLUDED));
                    }

                    @Override
                    public List<? extends OWLAxiom> visit(
                            OWLNamedIndividual individual) {
                        return sortOptionally(ont.getAxioms(individual,
                                EXCLUDED));
                    }

                    @Override
                    public List<? extends OWLAxiom> visit(OWLDatatype datatype) {
                        return sortOptionally(ont.getAxioms(datatype, EXCLUDED));
                    }

                    @Override
                    public List<? extends OWLAxiom> visit(
                            OWLAnnotationProperty property) {
                        return sortOptionally(ont.getAxioms(property, EXCLUDED));
                    }
                });
        Set<OWLAxiom> writtenAxioms = new HashSet<>();
        for (OWLAxiom ax : axs) {
            if (alreadyWrittenAxioms.contains(ax)) {
                continue;
            }
            if (ax.getAxiomType().equals(AxiomType.DIFFERENT_INDIVIDUALS)) {
                continue;
            }
            if (ax.getAxiomType().equals(AxiomType.DISJOINT_CLASSES)
                    && ((OWLDisjointClassesAxiom) ax).getClassExpressions()
                            .size() > 2) {
                continue;
            }
            ax.accept(this);
            writtenAxioms.add(ax);
            writeReturn();
        }
        alreadyWrittenAxioms.addAll(writtenAxioms);
    }

    /**
     * Writes out the declaration axioms for the specified entity.
     * 
     * @param entity
     *        The entity
     * @return The axioms that were written out
     */
    @Nonnull
    protected Set<OWLAxiom> writeDeclarations(@Nonnull OWLEntity entity) {
        Set<OWLAxiom> axioms = new HashSet<>();
        for (OWLAxiom ax : ont.getDeclarationAxioms(entity)) {
            ax.accept(this);
            axioms.add(ax);
            writeReturn();
        }
        return axioms;
    }

    private void writeDeclarations(@Nonnull OWLEntity entity,
            @Nonnull Set<OWLAxiom> alreadyWrittenAxioms,
            Collection<IRI> illegals) {
        Collection<OWLDeclarationAxiom> axioms = ont
                .getDeclarationAxioms(entity);
        for (OWLDeclarationAxiom ax : axioms) {
            if (!alreadyWrittenAxioms.contains(ax)) {
                ax.accept(this);
                writeReturn();
            }
        }
        // if multiple illegal declarations already exist, they have already
        // been outputted
        // the renderer cannot take responsibility for removing them
        // It should not add declarations for illegally punned entities here,
        // though
        if (addMissingDeclarations && axioms.isEmpty()) {
            // if declarations should be added, check if the IRI is illegally
            // punned
            if (!entity.isBuiltIn() && !illegals.contains(entity.getIRI())
                    && !ont.isDeclared(entity, Imports.INCLUDED)) {
                OWLDeclarationAxiom declaration = ont.getOWLOntologyManager()
                        .getOWLDataFactory().getOWLDeclarationAxiom(entity);
                declaration.accept(this);
                writeReturn();
            }
        }
        alreadyWrittenAxioms.addAll(axioms);
    }

    /**
     * Writes of the annotation for the specified entity.
     * 
     * @param entity
     *        The entity
     * @param alreadyWrittenAxioms
     *        already written axioms, to be updated with the newly written
     *        axioms
     */
    protected void writeAnnotations(@Nonnull OWLEntity entity,
            @Nonnull Set<OWLAxiom> alreadyWrittenAxioms) {
        Set<OWLAnnotationAssertionAxiom> annotationAssertionAxioms = ont
                .getAnnotationAssertionAxioms(entity.getIRI());
        for (OWLAnnotationAxiom ax : annotationAssertionAxioms) {
            if (!alreadyWrittenAxioms.contains(ax)) {
                ax.accept(this);
                writeReturn();
            }
        }
        alreadyWrittenAxioms.addAll(annotationAssertionAxioms);
    }

    /**
     * Write.
     * 
     * @param v
     *        the v
     * @param o
     *        the o
     */
    protected void write(@Nonnull OWLXMLVocabulary v, @Nonnull OWLObject o) {
    	writeOpenBracket();
    	write(v);
        writeSpace();
        o.accept(this);
        writeCloseBracket();
    }

    private void write(@Nonnull Collection<? extends OWLObject> objects) {
        if (objects.size() > 2) {
            for (Iterator<? extends OWLObject> it = objects.iterator(); it
                    .hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    writeSpace();
                }
            }
        } else if (objects.size() == 2) {
            Iterator<? extends OWLObject> it = objects.iterator();
            OWLObject objA = it.next();
            OWLObject objB = it.next();
            OWLObject lhs;
            OWLObject rhs;
            if (objA.equals(focusedObject)) {
                lhs = objA;
                rhs = objB;
            } else {
                lhs = objB;
                rhs = objA;
            }
            lhs.accept(this);
            writeSpace();
            rhs.accept(this);
        } else if (objects.size() == 1) {
            objects.iterator().next().accept(this);
        }
    }

    private void write(@Nonnull List<? extends OWLObject> objects) {
        if (objects.size() > 1) {
            for (Iterator<? extends OWLObject> it = objects.iterator(); it
                    .hasNext();) {
                it.next().accept(this);
                if (it.hasNext()) {
                    writeSpace();
                }
            }
        } else if (objects.size() == 1) {
            objects.iterator().next().accept(this);
        }
    }

    protected void writeOpenBracket() {
        write("(");
    }

    protected void writeCloseBracket() {
        write(")");
    }

    protected void writeSpace() {
        write(" ");
    }

    protected void writeReturn() {
        write("\n");
    }

    protected void writeAnnotations(@Nonnull OWLAxiom ax) {
        for (OWLAnnotation anno : ax.getAnnotations()) {
            anno.accept(this);
            writeSpace();
        }
    }

    protected void writeAxiomStart(@Nonnull OWLXMLVocabulary v,
            @Nonnull OWLAxiom axiom) {
        writeOpenBracket();
        write(v);
        writeSpace();
        writeAnnotations(axiom);
    }

    protected void writeAxiomEnd() {
        writeCloseBracket();
    }

    protected void writePropertyCharacteristic(@Nonnull OWLXMLVocabulary v,
            @Nonnull OWLAxiom ax, @Nonnull OWLPropertyExpression prop) {
        writeAxiomStart(v, ax);
        prop.accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLAsymmetricObjectPropertyAxiom axiom) {
        writePropertyCharacteristic(ASYMMETRIC_OBJECT_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLClassAssertionAxiom axiom) {
        writeAxiomStart(CLASS_ASSERTION, axiom);
        axiom.getClassExpression().accept(this);
        writeSpace();
        axiom.getIndividual().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDataPropertyAssertionAxiom axiom) {
        writeAxiomStart(DATA_PROPERTY_ASSERTION, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getSubject().accept(this);
        writeSpace();
        axiom.getObject().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDataPropertyDomainAxiom axiom) {
        writeAxiomStart(DATA_PROPERTY_DOMAIN, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getDomain().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDataPropertyRangeAxiom axiom) {
        writeAxiomStart(DATA_PROPERTY_RANGE, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getRange().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLSubDataPropertyOfAxiom axiom) {
        writeAxiomStart(SUB_DATA_PROPERTY_OF, axiom);
        axiom.getSubProperty().accept(this);
        writeSpace();
        axiom.getSuperProperty().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDeclarationAxiom axiom) {
        writeAxiomStart(DECLARATION, axiom);
        writeEntitiesAsURIs = false;
        axiom.getEntity().accept(this);
        writeEntitiesAsURIs = true;
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDifferentIndividualsAxiom axiom) {
        Set<OWLIndividual> individuals = axiom.getIndividuals();
        if (individuals.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(DIFFERENT_INDIVIDUALS, axiom);
        write(individuals);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDisjointClassesAxiom axiom) {
        Set<OWLClassExpression> classExpressions = axiom.getClassExpressions();
        if (classExpressions.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(DISJOINT_CLASSES, axiom);
        write(classExpressions);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDisjointDataPropertiesAxiom axiom) {
        Set<OWLDataPropertyExpression> properties = axiom.getProperties();
        if (properties.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(DISJOINT_DATA_PROPERTIES, axiom);
        write(properties);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDisjointObjectPropertiesAxiom axiom) {
        Set<OWLObjectPropertyExpression> properties = axiom.getProperties();
        if (properties.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(DISJOINT_OBJECT_PROPERTIES, axiom);
        write(properties);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDisjointUnionAxiom axiom) {
        writeAxiomStart(DISJOINT_UNION, axiom);
        axiom.getOWLClass().accept(this);
        writeSpace();
        write(axiom.getClassExpressions());
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLAnnotationAssertionAxiom axiom) {
        writeAxiomStart(ANNOTATION_ASSERTION, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getSubject().accept(this);
        writeSpace();
        axiom.getValue().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLEquivalentClassesAxiom axiom) {
        Set<OWLClassExpression> classExpressions = axiom.getClassExpressions();
        if (classExpressions.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(EQUIVALENT_CLASSES, axiom);
        write(classExpressions);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLEquivalentDataPropertiesAxiom axiom) {
        Set<OWLDataPropertyExpression> properties = axiom.getProperties();
        if (properties.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(EQUIVALENT_DATA_PROPERTIES, axiom);
        write(properties);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLEquivalentObjectPropertiesAxiom axiom) {
        Set<OWLObjectPropertyExpression> properties = axiom.getProperties();
        if (properties.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(EQUIVALENT_OBJECT_PROPERTIES, axiom);
        write(properties);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLFunctionalDataPropertyAxiom axiom) {
        writePropertyCharacteristic(FUNCTIONAL_DATA_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLFunctionalObjectPropertyAxiom axiom) {
        writePropertyCharacteristic(FUNCTIONAL_OBJECT_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLInverseFunctionalObjectPropertyAxiom axiom) {
        writePropertyCharacteristic(INVERSE_FUNCTIONAL_OBJECT_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLInverseObjectPropertiesAxiom axiom) {
        writeAxiomStart(INVERSE_OBJECT_PROPERTIES, axiom);
        axiom.getFirstProperty().accept(this);
        writeSpace();
        axiom.getSecondProperty().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLIrreflexiveObjectPropertyAxiom axiom) {
        writePropertyCharacteristic(IRREFLEXIVE_OBJECT_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLNegativeDataPropertyAssertionAxiom axiom) {
        writeAxiomStart(NEGATIVE_DATA_PROPERTY_ASSERTION, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getSubject().accept(this);
        writeSpace();
        axiom.getObject().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLNegativeObjectPropertyAssertionAxiom axiom) {
        writeAxiomStart(NEGATIVE_OBJECT_PROPERTY_ASSERTION, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getSubject().accept(this);
        writeSpace();
        axiom.getObject().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLObjectPropertyAssertionAxiom axiom) {
        writeAxiomStart(OBJECT_PROPERTY_ASSERTION, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getSubject().accept(this);
        writeSpace();
        axiom.getObject().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLSubPropertyChainOfAxiom axiom) {
        writeAxiomStart(SUB_OBJECT_PROPERTY_OF, axiom);
        writeOpenBracket();
        write(OBJECT_PROPERTY_CHAIN);
        writeSpace();
        for (Iterator<OWLObjectPropertyExpression> it = axiom
                .getPropertyChain().iterator(); it.hasNext();) {
            it.next().accept(this);
            if (it.hasNext()) {
                writeSpace();
            }
        }
        writeCloseBracket();
        writeSpace();
        axiom.getSuperProperty().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLObjectPropertyDomainAxiom axiom) {
        writeAxiomStart(OBJECT_PROPERTY_DOMAIN, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getDomain().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLObjectPropertyRangeAxiom axiom) {
        writeAxiomStart(OBJECT_PROPERTY_RANGE, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getRange().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLSubObjectPropertyOfAxiom axiom) {
        writeAxiomStart(SUB_OBJECT_PROPERTY_OF, axiom);
        axiom.getSubProperty().accept(this);
        writeSpace();
        axiom.getSuperProperty().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLReflexiveObjectPropertyAxiom axiom) {
        writePropertyCharacteristic(REFLEXIVE_OBJECT_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLSameIndividualAxiom axiom) {
        Set<OWLIndividual> individuals = axiom.getIndividuals();
        if (individuals.size() < 2) {
            // TODO log
            return;
        }
        writeAxiomStart(SAME_INDIVIDUAL, axiom);
        write(individuals);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLSubClassOfAxiom axiom) {
	writeOpenBracket();
	writeIsa(); 
	writeArbIsa(axiom.getSubClass());
	writeSpace();
        axiom.getSuperClass().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLSymmetricObjectPropertyAxiom axiom) {
        writePropertyCharacteristic(SYMMETRIC_OBJECT_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLTransitiveObjectPropertyAxiom axiom) {
        writePropertyCharacteristic(TRANSITIVE_OBJECT_PROPERTY, axiom,
                axiom.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLClass ce) {
        if (!writeEntitiesAsURIs) {
        	writeOpenBracket();
        	write(CLASS);
            writeSpace();
        }
        ce.getIRI().accept(this);
        if (!writeEntitiesAsURIs) {
            writeCloseBracket();
        }
    }

    @SuppressWarnings("null")
    private <F extends OWLPropertyRange> void writeRestriction(
            @Nonnull OWLXMLVocabulary v,
            @Nonnull OWLCardinalityRestriction<F> restriction,
            @Nonnull OWLPropertyExpression p) {
    	writeOpenBracket();
    	writeSpace();
        write(v);
        write(Integer.toString(restriction.getCardinality()));
        writeSpace();
        p.accept(this);
        if (restriction.isQualified()) {
            writeSpace();
            restriction.getFiller().accept(this);
        }
        writeCloseBracket();
    }

    private void writeRestriction(@Nonnull OWLXMLVocabulary v,
            @Nonnull OWLQuantifiedDataRestriction restriction) {
        writeRestriction(v, restriction.getProperty(), restriction.getFiller());
    }

    private void writeRestriction(@Nonnull OWLXMLVocabulary v,
            @Nonnull OWLQuantifiedObjectRestriction restriction) {
        writeRestriction(v, restriction.getProperty(), restriction.getFiller());
    }

    private void writeRestriction(@Nonnull OWLXMLVocabulary v,
            @Nonnull OWLPropertyExpression prop, @Nonnull OWLObject filler) {
    	writeOpenBracket();
        write(v);
        writeSpace();
        prop.accept(this);
        writeSpace();
        filler.accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(OWLDataAllValuesFrom ce) {
        writeRestriction(DATA_ALL_VALUES_FROM, ce);
    }

    @Override
    public void visit(@Nonnull OWLDataExactCardinality ce) {
        writeRestriction(DATA_EXACT_CARDINALITY, ce, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLDataMaxCardinality ce) {
        writeRestriction(DATA_MAX_CARDINALITY, ce, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLDataMinCardinality ce) {
        writeRestriction(DATA_MIN_CARDINALITY, ce, ce.getProperty());
    }

    @Override
    public void visit(OWLDataSomeValuesFrom ce) {
        writeRestriction(DATA_SOME_VALUES_FROM, ce);
    }

    @Override
    public void visit(@Nonnull OWLDataHasValue ce) {
        writeRestriction(DATA_HAS_VALUE, ce.getProperty(), ce.getFiller());
    }

    @Override
    public void visit(OWLObjectAllValuesFrom ce) {
        writeRestriction(OBJECT_ALL_VALUES_FROM, ce);
    }

    @Override
    public void visit(@Nonnull OWLObjectComplementOf ce) {
        write(OBJECT_COMPLEMENT_OF, ce.getOperand());
    }

    @Override
    public void visit(@Nonnull OWLObjectExactCardinality ce) {
        writeRestriction(OBJECT_EXACT_CARDINALITY, ce, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLObjectIntersectionOf ce) {
    	writeOpenBracket();
    	write(OBJECT_INTERSECTION_OF);
        writeSpace();
        write(ce.getOperands());
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLObjectMaxCardinality ce) {
        writeRestriction(OBJECT_MAX_CARDINALITY, ce, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLObjectMinCardinality ce) {
        writeRestriction(OBJECT_MIN_CARDINALITY, ce, ce.getProperty());
    }

    @Override
    public void visit(@Nonnull OWLObjectOneOf ce) {
    	writeOpenBracket();
    	write(OBJECT_ONE_OF);
        writeSpace();
        write(ce.getIndividuals());
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLObjectHasSelf ce) {
        write(OBJECT_HAS_SELF, ce.getProperty());
    }

    @Override
    public void visit(OWLObjectSomeValuesFrom ce) {
        writeRestriction(OBJECT_SOME_VALUES_FROM, ce);
    }

    @Override
    public void visit(@Nonnull OWLObjectUnionOf ce) {
    	writeOpenBracket();
    	write(OBJECT_UNION_OF);
        writeSpace();
        write(ce.getOperands());
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLObjectHasValue ce) {
        writeRestriction(OBJECT_HAS_VALUE, ce.getProperty(), ce.getFiller());
    }

    @Override
    public void visit(@Nonnull OWLDataComplementOf node) {
        write(DATA_COMPLEMENT_OF, node.getDataRange());
    }

    @Override
    public void visit(@Nonnull OWLDataOneOf node) {
    	writeOpenBracket();
    	write(DATA_ONE_OF);
        writeSpace();
        write(node.getValues());
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLDatatype node) {
        if (!writeEntitiesAsURIs) {
        	writeOpenBracket();
        	write(DATATYPE);
            writeSpace();
        }
        node.getIRI().accept(this);
        if (!writeEntitiesAsURIs) {
            writeCloseBracket();
        }
    }

    @Override
    public void visit(@Nonnull OWLDatatypeRestriction node) {
    	writeOpenBracket();
    	write(DATATYPE_RESTRICTION);
        writeSpace();
        node.getDatatype().accept(this);
        for (OWLFacetRestriction restriction : node.getFacetRestrictions()) {
            writeSpace();
            restriction.accept(this);
        }
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLFacetRestriction node) {
        write(node.getFacet().getIRI());
        writeSpace();
        node.getFacetValue().accept(this);
    }

    @Override
    public void visit(@Nonnull OWLLiteral node) {
        write("\"");
        write(EscapeUtils.escapeString(node.getLiteral()));
        write("\"");
        // TODO: Do something about this [DRS].
//        if (node.hasLang()) {
//            write("@");
//            write(node.getLang());
//        } else if (!node.isRDFPlainLiteral()) {
//            write("^^");
//            write(node.getDatatype().getIRI());
//        }
    }

    @Override
    public void visit(@Nonnull OWLDataProperty property) {
        if (!writeEntitiesAsURIs) {
        	writeOpenBracket();
        	write(DATA_PROPERTY);
            writeSpace();
        }
        property.getIRI().accept(this);
        if (!writeEntitiesAsURIs) {
            writeCloseBracket();
        }
    }

    @Override
    public void visit(@Nonnull OWLObjectProperty property) {
        if (!writeEntitiesAsURIs) {
        	writeOpenBracket();
        	write(OBJECT_PROPERTY);
            writeSpace();
        }
        property.getIRI().accept(this);
        if (!writeEntitiesAsURIs) {
            writeCloseBracket();
        }
    }

    @Override
    public void visit(@Nonnull OWLObjectInverseOf property) {
    	writeOpenBracket();
    	write(OBJECT_INVERSE_OF);
        writeSpace();
        property.getInverse().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLNamedIndividual individual) {
        if (!writeEntitiesAsURIs) {
        	writeOpenBracket();
        	write(NAMED_INDIVIDUAL);
            writeSpace();
        }
        individual.getIRI().accept(this);
        if (!writeEntitiesAsURIs) {
            writeCloseBracket();
        }
    }

    @Override
    public void visit(@Nonnull OWLHasKeyAxiom axiom) {
        writeAxiomStart(HAS_KEY, axiom);
        axiom.getClassExpression().accept(this);
        writeSpace();
        writeOpenBracket();
        for (Iterator<? extends OWLPropertyExpression> it = axiom
                .getObjectPropertyExpressions().iterator(); it.hasNext();) {
            OWLPropertyExpression prop = it.next();
            prop.accept(this);
            if (it.hasNext()) {
                writeSpace();
            }
        }
        writeCloseBracket();
        writeSpace();
        writeOpenBracket();
        for (Iterator<? extends OWLPropertyExpression> it = axiom
                .getDataPropertyExpressions().iterator(); it.hasNext();) {
            OWLPropertyExpression prop = it.next();
            prop.accept(this);
            if (it.hasNext()) {
                writeSpace();
            }
        }
        writeCloseBracket();
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLAnnotationPropertyDomainAxiom axiom) {
        writeAxiomStart(ANNOTATION_PROPERTY_DOMAIN, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getDomain().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLAnnotationPropertyRangeAxiom axiom) {
        writeAxiomStart(ANNOTATION_PROPERTY_RANGE, axiom);
        axiom.getProperty().accept(this);
        writeSpace();
        axiom.getRange().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLSubAnnotationPropertyOfAxiom axiom) {
        writeAxiomStart(SUB_ANNOTATION_PROPERTY_OF, axiom);
        axiom.getSubProperty().accept(this);
        writeSpace();
        axiom.getSuperProperty().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull OWLDataIntersectionOf node) {
    	writeOpenBracket();
    	write(DATA_INTERSECTION_OF);
        writeSpace();
        write(node.getOperands());
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLDataUnionOf node) {
    	writeOpenBracket();
    	write(DATA_UNION_OF);
        writeSpace();
        write(node.getOperands());
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLAnnotationProperty property) {
        if (!writeEntitiesAsURIs) {
        	writeOpenBracket();
        	write(ANNOTATION_PROPERTY);
            writeSpace();
        }
        property.getIRI().accept(this);
        if (!writeEntitiesAsURIs) {
            writeCloseBracket();
        }
    }

    @Override
    public void visit(@Nonnull OWLAnonymousIndividual individual) {
        write(individual.getID().toString());
    }

    @Override
    public void visit(@Nonnull IRI iri) {
        write(iri);
    }

    @Override
    public void visit(@Nonnull OWLAnnotation node) {
    	writeOpenBracket();
    	write(ANNOTATION);
        writeSpace();
        for (OWLAnnotation anno : node.getAnnotations()) {
            anno.accept(this);
            writeSpace();
        }
        node.getProperty().accept(this);
        writeSpace();
        node.getValue().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull OWLDatatypeDefinitionAxiom axiom) {
        writeAxiomStart(DATATYPE_DEFINITION, axiom);
        axiom.getDatatype().accept(this);
        writeSpace();
        axiom.getDataRange().accept(this);
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull SWRLRule rule) {
        writeAxiomStart(DL_SAFE_RULE, rule);
        writeOpenBracket();
        write(BODY);
        writeSpace();
        write(rule.getBody());
        writeCloseBracket();
        writeOpenBracket();
        write(HEAD);
        writeSpace();
        write(rule.getHead());
        writeCloseBracket();
        writeAxiomEnd();
    }

    @Override
    public void visit(@Nonnull SWRLIndividualArgument node) {
        node.getIndividual().accept(this);
    }

    @Override
    public void visit(@Nonnull SWRLClassAtom node) {
    	writeOpenBracket();
    	write(CLASS_ATOM);
        writeSpace();
        node.getPredicate().accept(this);
        writeSpace();
        node.getArgument().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull SWRLDataRangeAtom node) {
    	writeOpenBracket();
    	write(DATA_RANGE_ATOM);
        writeSpace();
        node.getPredicate().accept(this);
        writeSpace();
        node.getArgument().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull SWRLObjectPropertyAtom node) {
    	writeOpenBracket();
    	write(OBJECT_PROPERTY_ATOM);
        writeSpace();
        node.getPredicate().accept(this);
        writeSpace();
        node.getFirstArgument().accept(this);
        writeSpace();
        node.getSecondArgument().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull SWRLDataPropertyAtom node) {
    	writeOpenBracket();
    	write(DATA_PROPERTY_ATOM);
        writeSpace();
        node.getPredicate().accept(this);
        writeSpace();
        node.getFirstArgument().accept(this);
        writeSpace();
        node.getSecondArgument().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull SWRLBuiltInAtom node) {
    	writeOpenBracket();
    	write(BUILT_IN_ATOM);
        writeSpace();
        node.getPredicate().accept(this);
        writeSpace();
        write(node.getArguments());
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull SWRLVariable node) {
    	writeOpenBracket();
    	write(VARIABLE);
        writeSpace();
        node.getIRI().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull SWRLLiteralArgument node) {
        node.getLiteral().accept(this);
    }

    @Override
    public void visit(@Nonnull SWRLDifferentIndividualsAtom node) {
    	writeOpenBracket();
    	write(DIFFERENT_INDIVIDUALS_ATOM);
        writeSpace();
        node.getFirstArgument().accept(this);
        writeSpace();
        node.getSecondArgument().accept(this);
        writeCloseBracket();
    }

    @Override
    public void visit(@Nonnull SWRLSameIndividualAtom node) {
    	writeOpenBracket();
    	write(SAME_INDIVIDUAL_ATOM);
        writeSpace();
        node.getFirstArgument().accept(this);
        writeSpace();
        node.getSecondArgument().accept(this);
        writeCloseBracket();
    }
}
