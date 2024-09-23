
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;



public class SlayTheSpireDeck {
    private static final int MAX_CARDS = 1000;
    private static final int MAX_INVALID_CARDS = 10;

    //generate random deck ID number
    private static final Random RANDOM = new Random();

    public static void main(String[] args) {
        //take in user input
        Scanner scanner = new Scanner(System.in);
        // prompt user to enter the deck name
        // note: if deck file is not in project directory, entire file path must be entered as input
        System.out.print("Enter the deck file name: ");
        String fileName = scanner.nextLine();

        try {

            DeckReport report = processDeck(fileName);
            generateReport(report);
        } catch (IOException e) {
            System.err.println("Error reading the file: " + e.getMessage());
        } finally {
            scanner.close();
        }
    }

    private static DeckReport processDeck(String fileName) throws IOException {

        // stores invalid cards
        List<String> invalidCards = new ArrayList<>();

        // histogram, track #of cards for each energy cost
        Map<Integer, Integer> costHistogram = new HashMap<>();

        //sum of valid card costs and number of valid cards processed
        int totalCost = 0, cardCount = 0;

        // read file
        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;

            //Parse file with delimiter :
            while ((line = br.readLine()) != null && cardCount < MAX_CARDS) {
                String[] parts = line.split(":");

                //check line is formatted as expected
                if (parts.length == 2) {
                    //split each line into the card name and its cost
                    String cardName = parts[0].trim();
                    String costStr = parts[1].trim();

                    // if card is valid adds cost to total, updates costHistogram, and card count
                    if (isValidCard(cardName, costStr)) {
                        int cost = Integer.parseInt(costStr);
                        totalCost += cost;
                        costHistogram.put(cost, costHistogram.getOrDefault(cost, 0) + 1);
                        cardCount++;
                    } else {
                        //not valid add to invalid cards list
                        invalidCards.add(line);
                    }
                } else {
                    invalidCards.add(line);
                }
            }
        }

        // method call to create and return a DeckReport with all the data
        return new DeckReport(generateDeckId(), totalCost, costHistogram, invalidCards);
    }

    // method for determining card name validity
    private static boolean isValidCard(String name, String costStr) {
        return !name.trim().isEmpty() && isValidCost(costStr);
    }

    // method for determining card cost validity
    private static boolean isValidCost(String costStr) {
        try {
            int cost = Integer.parseInt(costStr);
            return cost >= 0 && cost <= 6;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    // generate random deck ID
    private static String generateDeckId() {
        return String.format("%09d", RANDOM.nextInt(1_000_000_000));
    }


    private static void generateReport(DeckReport report) {
        String fileName = report.invalidCards.size() > MAX_INVALID_CARDS ?
               //if size is invalid void indicates a bad report
                "SpireDeck " + report.deckId + "(VOID).pdf" :
                // otherwise standard name given
                "SpireDeck " + report.deckId + ".pdf";


        // write to pdf file
        try (PdfWriter writer = new PdfWriter(fileName);
             PdfDocument pdf = new PdfDocument(writer);
             Document document = new Document(pdf)) {


            //  add deck ID and total energy to the PDF
            document.add(new Paragraph("Deck ID: " + report.deckId));
            document.add(new Paragraph("Total Energy Cost: " + report.totalCost + " energy"));

            // if max invalid cards reached adds void
            if (report.invalidCards.size() > MAX_INVALID_CARDS) {
                document.add(new Paragraph("VOID"));
            } else {
                // add cost histogram and any invalid cards
                document.add(new Paragraph("Cost Histogram:"));
                report.costHistogram.forEach((cost, count) ->
                        document.add(new Paragraph(cost + " energy: " + count + " card(s)"))
                );
                if (!report.invalidCards.isEmpty()) {
                    document.add(new Paragraph("Invalid Cards:"));
                    report.invalidCards.forEach(card ->
                            document.add(new Paragraph(card))
                    );
                }
            }
        } catch (IOException e) {
            // ensure any error when generating report does not crash program
            System.err.println("Error generating report: " + e.getMessage());
        }
    }
}


// class to hold report data
class DeckReport {

    String deckId;
    int totalCost;
    Map<Integer, Integer> costHistogram;
    List<String> invalidCards;

//constructor initialize data parameters
    DeckReport(String deckId, int totalCost, Map<Integer, Integer> costHistogram, List<String> invalidCards) {
        this.deckId = deckId;
        this.totalCost = totalCost;
        this.costHistogram = costHistogram;
        this.invalidCards = invalidCards;
    }
}
