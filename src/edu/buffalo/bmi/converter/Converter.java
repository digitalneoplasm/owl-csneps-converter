package edu.buffalo.bmi.converter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLRendererException;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;

import edu.buffalo.bmi.csneps.OWLCSNePSSyntaxRenderer;

public class Converter {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		if (args.length != 2) {
			System.out.println("Usage: Converter <infile> <outfile>");
			System.exit(1);
		}

		String infilename = args[0];
		String outfilename = args[1];

		File file = new File(infilename);
		OWLOntologyManager manager;
		OWLOntology localOntology = null;

		manager = OWLManager.createOWLOntologyManager();

		try {
			localOntology = manager.loadOntologyFromOntologyDocument(file);
		} catch (OWLOntologyCreationException e) {
			e.printStackTrace();
		}

		StringWriter sw = new StringWriter();
		OWLCSNePSSyntaxRenderer renderer = new OWLCSNePSSyntaxRenderer();

		try {
			renderer.render(localOntology, sw);
		} catch (OWLRendererException e) {
			e.printStackTrace();
		}

		String csnepsTerms = sw.toString();

		PrintWriter writer = null;

		try {
			writer = new PrintWriter(outfilename);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}

		writer.println("(in-ns 'csneps.core.snuser)");
		writer.println("(clearkb true)");
		writer.println("(krnovice true)");
		writer.println();
		
		FileReader cfreader = null;
		try {
			cfreader = new FileReader("res/caseframes.sneps");
			int c = cfreader.read();
			while (c != -1) {
				writer.write(c);
				c = cfreader.read();
			}
			cfreader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		writer.println();
		writer.println();

		Scanner scanner = new Scanner(csnepsTerms);
		while (scanner.hasNextLine()) {
			String line = scanner.nextLine();
			if (line.startsWith("(")) {
				writer.println("(assert '" + line + ")");
			} else
				writer.println(line);
		}
		scanner.close();

		writer.close();
	}
}
