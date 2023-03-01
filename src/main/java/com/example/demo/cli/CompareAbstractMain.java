package com.example.demo.cli;

import java.io.BufferedReader;
import java.io.IOException;

public abstract class CompareAbstractMain {
	public static String COMPARATEUR_URL;
	public static final String QUIT = "0";
	
	protected void setCompareSearchUrl(BufferedReader inputReader) 
			throws IOException {
		
		System.out.println("Veuillez fournir l'URL de recherche de comparaison au service Web à consommer:");
		COMPARATEUR_URL = inputReader.readLine();
		
		while(!validCompareSearchUrl()) {
			System.err.println("Erreur: "+COMPARATEUR_URL+
					" n'existe-t-il pas une comparaison valide pour la recherche d'URL de services Web. "
					+ "Veuillez réessayer: ");
			COMPARATEUR_URL = inputReader.readLine();
		}
	}
	
	protected abstract boolean validCompareSearchUrl();
	
	
	protected abstract void menu();
}