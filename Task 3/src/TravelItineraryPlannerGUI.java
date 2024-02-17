import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.CompletableFuture;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class TravelItineraryPlannerGUI extends Application {

    private static final String WEATHER_API_KEY = "357a16abde26e1bbbad4e7868d2074cb";
    private static final String MAPS_API_KEY = "AIzaSyBbZTRVyGIPad-z0sUQ-y2CLPx83dVU_ps";

    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Your Travel Itinerary Planner");

        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        root.setStyle("-fx-background-color: #f0f0f0;");

        Label titleLabel = new Label("Travel Itinerary Planner");
        titleLabel.setStyle("-fx-font-size: 24pt; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        root.getChildren().add(titleLabel);

        GridPane grid = new GridPane();
        grid.setPadding(new Insets(20));
        grid.setVgap(10);
        grid.setHgap(10);
        grid.setStyle("-fx-background-color: #ffffff;");

        VBox plansContainer = new VBox(10);
        plansContainer.setPadding(new Insets(20));
        plansContainer.setStyle("-fx-background-color: #ffffff; -fx-border-width: 1; -fx-border-color: #cccccc;");
        GridPane.setConstraints(plansContainer, 0, 1);

        ScrollPane scrollPane = new ScrollPane(plansContainer);
        scrollPane.setFitToWidth(true);
        GridPane.setConstraints(scrollPane, 0, 1);

        Button addPlanButton = new Button("Add Plan");
        addPlanButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        addPlanButton.setOnMouseEntered(e -> addPlanButton.setEffect(new DropShadow()));
        addPlanButton.setOnMouseExited(e -> addPlanButton.setEffect(null));
        addPlanButton.setOnAction(event -> addPlan(plansContainer));

        root.getChildren().addAll(grid, scrollPane, addPlanButton);

        Scene scene = new Scene(root, 800, 600);
        scene.getStylesheets().add("style.css"); 
        primaryStage.setScene(scene);
        primaryStage.show();

        addPlan(plansContainer);
    }

    private void addPlan(VBox plansContainer) {
        GridPane planGrid = new GridPane();
        planGrid.setPadding(new Insets(20));
        planGrid.setVgap(10);
        planGrid.setHgap(10);


        Label destinationLabel = new Label("Destination:");
        GridPane.setConstraints(destinationLabel, 0, 1);
        TextField destinationField = new TextField();
        GridPane.setConstraints(destinationField, 1, 1);

        Label startDateLabel = new Label("Start Date:");
        GridPane.setConstraints(startDateLabel, 2, 1);
        DatePicker startDatePicker = new DatePicker();
        GridPane.setConstraints(startDatePicker, 3, 1);

        Label endDateLabel = new Label("End Date:");
        GridPane.setConstraints(endDateLabel, 4, 1);
        DatePicker endDatePicker = new DatePicker();
        GridPane.setConstraints(endDatePicker, 5, 1);

        Label preferencesLabel = new Label("Preferences:");
        GridPane.setConstraints(preferencesLabel, 0, 2);
        TextArea preferencesArea = new TextArea();
        preferencesArea.setPrefRowCount(5);
        preferencesArea.setWrapText(true);
        GridPane.setConstraints(preferencesArea, 1, 2);

        Button generateButton = new Button("Generate Plan");
        GridPane.setConstraints(generateButton, 3, 3);
        generateButton.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        generateButton.setOnMouseEntered(e -> generateButton.setEffect(new DropShadow()));
        generateButton.setOnMouseExited(e -> generateButton.setEffect(null));
        generateButton.setOnAction(event -> {
            String destination = destinationField.getText();
            LocalDate startDate = startDatePicker.getValue();
            LocalDate endDate = endDatePicker.getValue();
            String preferences = preferencesArea.getText();

            CompletableFuture<String> weatherFuture = CompletableFuture.supplyAsync(() -> getWeatherInformation(destination, startDate.toString()));
            CompletableFuture<String> mapViewUrlFuture = CompletableFuture.supplyAsync(() -> getMapImageUrl(destination));

            CompletableFuture.allOf(weatherFuture, mapViewUrlFuture).thenRun(() -> {
                String weatherInfo = weatherFuture.join();
                String mapViewUrl = mapViewUrlFuture.join();

                if (weatherInfo == null || weatherInfo.isEmpty()) {
                    System.out.println("Failed to fetch weather information.");
                    weatherInfo = "Weather information is not available.";
                }
                if (mapViewUrl == null || mapViewUrl.isEmpty()) {
                    System.out.println("Failed to fetch map image URL.");
                    mapViewUrl = "Map image is not available.";
                }

                String htmlContent = generateHTML(destination, startDate, endDate, weatherInfo, mapViewUrl, preferences);
                if (htmlContent != null && !htmlContent.isEmpty()) {
                    displayInWebView(htmlContent, planGrid);
                } else {
                    System.out.println("Failed to generate HTML content.");
                }
            }).exceptionally(ex -> {
                ex.printStackTrace();
                System.out.println("Failed to fetch weather information or map image URL.");
                return null;
            });
        });

        Image generateIcon = new Image(getClass().getResourceAsStream("download.png"));
        ImageView generateIconView = new ImageView(generateIcon);
        generateIconView.setFitHeight(16);
        generateIconView.setFitWidth(16);
        generateButton.setGraphic(generateIconView);

        planGrid.getChildren().addAll(destinationLabel, destinationField, startDateLabel, startDatePicker, endDateLabel, endDatePicker,
                preferencesLabel, preferencesArea, generateButton);

        plansContainer.getChildren().add(planGrid);
    }

    private String getWeatherInformation(String destination, String date) {
        try {
            URL url = new URL("http://api.openweathermap.org/data/2.5/weather?q=" + destination + "&appid=" + WEATHER_API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder response = new StringBuilder();
            while (scanner.hasNextLine()) {
                response.append(scanner.nextLine());
            }
            scanner.close();

            return response.toString();
        } catch (IOException e) {
            e.printStackTrace();
            return "Failed to fetch weather information.";
        }
    }

    private String getMapImageUrl(String destination) {
        return "https://maps.googleapis.com/maps/api/staticmap?center=" + destination + "&zoom=10&size=600x400&key=" + MAPS_API_KEY;
    }

    private String generateHTML(String destination, LocalDate startDate, LocalDate endDate, String weatherInfo, String mapViewUrl, String preferences) {
        String description = extractDescription(weatherInfo);
        String iconCode = extractIconCode(weatherInfo);
        double minTemperatureKelvin = extractMinTemperature(weatherInfo);
        double minTemperatureFahrenheit = (minTemperatureKelvin - 273.15) * 9 / 5 + 32;
        double maxTemperatureKelvin = extractMaxTemperature(weatherInfo);
        double maxTemperatureFahrenheit = (maxTemperatureKelvin - 273.15) * 9 / 5 + 32;

        // Calculate number of nights
        long numberOfNights = ChronoUnit.DAYS.between(startDate, endDate);

        // Construct HTML content with weather information, icons, and budget
        StringBuilder htmlContent = new StringBuilder();
        htmlContent.append("<html><body>");
        htmlContent.append("<h1>Travel Itinerary Plan</h1>");
        htmlContent.append("<p><strong>Destination: </strong>").append(destination).append("</p>");
        htmlContent.append("<p><strong>Stay Duration: </strong>").append(numberOfNights).append(" nights</p>");
        htmlContent.append("<p><strong>Date Range: </strong>").append(startDate).append(" to ").append(endDate).append("</p>");
        htmlContent.append("<p><strong>Weather Information:</strong> ");
        htmlContent.append(description).append(" <img src='http://openweathermap.org/img/wn/").append(iconCode).append(".png'/>").append("</p>");
        htmlContent.append("<p><strong>Min Temperature:</strong> ").append(minTemperatureFahrenheit).append("°F</p>");
        htmlContent.append("<p><strong>Max Temperature:</strong> ").append(maxTemperatureFahrenheit).append("°F</p>");
        htmlContent.append("<img src='").append(mapViewUrl).append("'/>");
        htmlContent.append("<p><strong>Preferences:</strong></p>");
        htmlContent.append("<ul>");
        String[] preferenceList = preferences.split("\n");
        for (String preference : preferenceList) {
            htmlContent.append("<li>").append(preference).append("</li>");
        }
        htmlContent.append("</ul>");

        htmlContent.append("<p><strong>Estimated Budget: $</strong> ").append(calculateBudget(startDate, endDate)).append("</p>");
        htmlContent.append("</body></html>");
        return htmlContent.toString();
    }

  
    private double extractMinTemperature(String weatherInfo) {
        int startIndex = weatherInfo.indexOf("\"temp_min\":") + "\"temp_min\":".length();
        int endIndex = weatherInfo.indexOf(",", startIndex);
        return Double.parseDouble(weatherInfo.substring(startIndex, endIndex));
    }


    private double extractMaxTemperature(String weatherInfo) {
        int startIndex = weatherInfo.indexOf("\"temp_max\":") + "\"temp_max\":".length();
        int endIndex = weatherInfo.indexOf(",", startIndex);
        return Double.parseDouble(weatherInfo.substring(startIndex, endIndex));
    }

 
    private String extractDescription(String weatherInfo) {
        int startIndex = weatherInfo.indexOf("\"description\":\"") + "\"description\":\"".length();
        int endIndex = weatherInfo.indexOf("\"", startIndex);
        return weatherInfo.substring(startIndex, endIndex);
    }


    private String extractIconCode(String weatherInfo) {
        int startIndex = weatherInfo.indexOf("\"icon\":\"") + "\"icon\":\"".length();
        int endIndex = weatherInfo.indexOf("\"", startIndex);
        return weatherInfo.substring(startIndex, endIndex);
    }

    private void displayInWebView(String htmlContent, GridPane planGrid) {
        Platform.runLater(() -> {
            WebView webView = new WebView();
            webView.getEngine().loadContent(htmlContent);
            planGrid.add(webView, 0, 6, 2, 1);
        });
    }


    private double calculateBudget(LocalDate startDate, LocalDate endDate) {

        double flightTicketCost = 500; 
        double hotelCostPerNight = 100; 
        long numberOfNights = ChronoUnit.DAYS.between(startDate, endDate);
        double totalHotelCost = hotelCostPerNight * numberOfNights;
        double otherExpenses = 50 * numberOfNights;
        return flightTicketCost + totalHotelCost + otherExpenses;
    }
}
