package com.example.demo.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.example.demo.models.Agence;
import com.example.demo.models.AgencePropose;
import com.example.demo.models.Chambre;
import com.example.demo.models.Hotel;
import com.example.demo.models.Lit;
import com.example.demo.models.Propose;

@Component
public class ComparateurRestClientCLI extends CompareAbstractMain implements CommandLineRunner {

	@Autowired
	private RestTemplate proxy;
	public static StringToCalendar inputStringToCalendar;
	public static StringToDouble inputStringToDouble;
	public static StringToInt inputStringToInt;
	
	@Override
	public void run(String... args) throws Exception {
		BufferedReader inputReader;
		String userInput = "";
		try {
			inputReader = new BufferedReader(
					new InputStreamReader(System.in));
			setCompareSearchUrl(inputReader);
			do {
				menu();
				userInput = inputReader.readLine();
				processUserInput(inputReader, userInput);
				Thread.sleep(1000);
				
			} while(!userInput.equals(QUIT));
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}

	@Override
	protected boolean validCompareSearchUrl() {
		return COMPARATEUR_URL.equals(
				"http://localhost:8080/comparateur/api");
	}

	@Override
	protected void menu() {
		StringBuilder builder = new StringBuilder();
		builder.append(QUIT+". Quit.");
		builder.append("\n1. Afficher disponibilit√© de tous les hotels pour comparer.");
		System.out.println(builder);
	}
	
	private void processUserInput(BufferedReader reader, String userInput) {
		try {
			switch(userInput) {
				default:
					System.err.println("D√©sol√©, mauvaise saisie. Veuillez r√©essayer.");
					return;
				case QUIT:
					System.out.println("Au revoir ...");
					System.exit(0);
				case "1":
					System.out.println("Ville: ");
					String ville = reader.readLine();
					System.out.println();
					
					System.out.println("Date arriv√©e (dd/MM/yyyy) aujourd'hui ou apr√®s aujourd'hui : ");
					inputStringToCalendar = new StringToCalendar(reader);
					String dateArrivee = inputStringToCalendar.process();
					Calendar dateArriveeCal = (Calendar) inputStringToCalendar.processToCalendar(dateArrivee);
					System.out.println();
					
					System.out.println("Date d√©part (dd/MM/yyyy) apr√®s date arriv√©e : ");
					inputStringToCalendar = new StringToCalendar(reader);
					String dateDepart = inputStringToCalendar.process();
					Calendar dateDepartCal = (Calendar) inputStringToCalendar.processToCalendar(dateDepart);
					while (!dateDepartCal.after(dateArriveeCal)) {
						System.err.println("Date d√©part doit √™tre apr√®s date arriv√©e !");
						System.out.println();
						System.out.println("Date d√©part (dd/MM/yyyy): ");
						inputStringToCalendar = new StringToCalendar(reader);
						dateDepart = inputStringToCalendar.process();
						dateDepartCal = (Calendar) inputStringToCalendar.processToCalendar(dateDepart);
					}
					System.out.println();
					
					System.out.println("Cat√©gorie d'hotel (1,2,3,4,5): ");
					inputStringToInt = new StringToInt(reader);
					int etoile = (int) inputStringToInt.process();
					System.out.println();
					
					System.out.println("Nombre de personnes √† h√©berger: ");
					inputStringToInt = new StringToInt(reader);
					int nombrePerson = (int) inputStringToInt.process();
					System.out.println();
					
					String uri = COMPARATEUR_URL+"/propose?"
							+ "ville="+ville+
							"&dateArrivee="+dateArrivee+
							"&dateDepart="+dateDepart+
							"&etoile="+etoile+
							"&nombrePerson="+nombrePerson;
					System.out.println(uri);
					AgencePropose[] allCombinations = proxy.getForObject(uri, AgencePropose[].class);
					if (allCombinations.length == 0) {
						System.err.println("D√©sol√©, pas d'h√ītel correspond. Veuillez r√©essayer.");
						break;
					} else {
						System.out.println("Voici tous les propositions : ");
						int days = this.daysBetween(dateArriveeCal, dateDepartCal);
						this.displayAllCombinations(allCombinations, dateArriveeCal, dateDepartCal, days);
					}
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void displayAllCombinations(AgencePropose[] allCombinations, 
			Calendar dateArrivee, Calendar dateDepart, int days) {
		for (AgencePropose agencepropose : allCombinations) {
			Agence agence = agencepropose.getAgence();
			Propose propose = agencepropose.getPropose();
			Hotel hotel = propose.getHotelPartenaireTarif().getHotel();
			System.out.println(
					"Agence : "+ agence.getIdentifiant() + "\n" +
					"Nom de l'h√ītel : " + hotel.getNom() + "\n" +
					"Adresse de l'h√ītel : " + hotel.getAdresse() + "\n" +
					"Nombre d'√©toiles de l'h√ītel : " + hotel.getCategorie() + "\n" +
					"Date de disponibilit√© : de " + this.calendarToString(dateArrivee) + " √† " + this.calendarToString(dateDepart)
			);
			int nombreLits = 0;
			for (Chambre c : propose.getListChambre()) {
				String descLit = "";
				System.out.println(
						"#Chambre Id : " + c.getChambreId()
				);
				for (Lit lit : c.getLitCollection()) {
					descLit = descLit + lit.toString() + "\n";
					nombreLits++;
				}
				System.out.println(
					descLit
				);
			}
			System.out.println(
					"Nombre de lits totaux propos√©s : " + nombreLits + "\n" +
					"Prix total √† payer : " + this.doubleToString(agence.prixChoisi(propose)*days) + " (avec pourcentage de commission)" + " (Pour " + days + " nuits)" + "\n" +
					"--------------------"
			);
		}
	}
	
	private String calendarToString(Calendar date) {
        SimpleDateFormat format1 = new SimpleDateFormat("dd/MM/yyyy");
        String dateString = format1.format(date.getTime());
        return dateString;
	}
	
	private String doubleToString(double prix) {
		DecimalFormat df = new DecimalFormat("0.00");
		return df.format(prix);
	}
		
	private int daysBetween(Calendar dateArrivee, Calendar dateDepart) {
		Date d1 = dateArrivee.getTime();
		Date d2 = dateDepart.getTime();
		return (int) ((d2.getTime() - d1.getTime()) / (1000 * 60 * 60 * 24));
	}
	
}