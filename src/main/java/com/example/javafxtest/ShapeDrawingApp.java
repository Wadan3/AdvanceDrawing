package com.example.javafxtest;

import javafx.application.Application;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.ArrayDeque;
import java.util.Deque;

public class ShapeDrawingApp extends Application {

    private Canvas drawingCanvas;
    private Canvas previewCanvas;
    private GraphicsContext gc;
    private GraphicsContext previewGc;

    private double startX;
    private double startY;

    private final Deque<WritableImage> undoStack = new ArrayDeque<>();

    private ColorPicker colorPicker;
    private Slider brushSlider;
    private ComboBox<String> modeBox;

    private boolean darkMode = false;

    @Override
    public void start(Stage stage) {
        drawingCanvas = new Canvas(1200, 700);
        previewCanvas = new Canvas(1200, 700);

        gc = drawingCanvas.getGraphicsContext2D();
        previewGc = previewCanvas.getGraphicsContext2D();

        initializeCanvas();

        StackPane canvasLayer = new StackPane(drawingCanvas, previewCanvas);
        canvasLayer.setPadding(new Insets(20));
        canvasLayer.setStyle("-fx-background-color: linear-gradient(to bottom right, #eef2ff, #f8fafc);");

        ScrollPane scrollPane = new ScrollPane(canvasLayer);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);

        BorderPane root = new BorderPane();
        root.setTop(createToolbar(stage));
        root.setCenter(scrollPane);
        root.setStyle("-fx-background-color: #f8fafc;");

        setupMouseActions();

        Scene scene = new Scene(root, 1250, 780);
        stage.setTitle("AdvanceDrawing - Professional JavaFX Drawing App");
        stage.setScene(scene);
        stage.show();
    }

    private ToolBar createToolbar(Stage stage) {
        colorPicker = new ColorPicker(Color.web("#2563eb"));

        brushSlider = new Slider(1, 50, 6);
        brushSlider.setShowTickLabels(true);
        brushSlider.setShowTickMarks(true);
        brushSlider.setMajorTickUnit(10);
        brushSlider.setPrefWidth(180);

        modeBox = new ComboBox<>();
        modeBox.getItems().addAll("Pen", "Line", "Rectangle", "Circle", "Eraser");
        modeBox.setValue("Pen");
        modeBox.setPrefWidth(130);

        Button undoButton = new Button("Undo");
        Button clearButton = new Button("Clear");
        Button saveButton = new Button("Save PNG");
        Button resizeButton = new Button("Resize Canvas");
        Button themeButton = new Button("Dark Mode");

        undoButton.setOnAction(e -> undo());
        clearButton.setOnAction(e -> clearCanvas());
        saveButton.setOnAction(e -> saveCanvas(stage));
        resizeButton.setOnAction(e -> resizeCanvas());
        themeButton.setOnAction(e -> toggleTheme(themeButton));

        ToolBar toolbar = new ToolBar(
                new Label("Color"),
                colorPicker,
                new Separator(),
                new Label("Brush"),
                brushSlider,
                new Separator(),
                new Label("Mode"),
                modeBox,
                new Separator(),
                undoButton,
                clearButton,
                saveButton,
                resizeButton,
                themeButton
        );

        toolbar.setPadding(new Insets(12));
        toolbar.setStyle(
                "-fx-background-color: linear-gradient(to right, #0f172a, #1e293b);" +
                "-fx-border-color: #334155;" +
                "-fx-border-width: 0 0 1 0;"
        );

        for (var node : toolbar.getItems()) {
            if (node instanceof Label label) {
                label.setTextFill(Color.WHITE);
                label.setStyle("-fx-font-weight: bold;");
            }
            if (node instanceof Button button) {
                button.setStyle(
                        "-fx-background-color: #2563eb;" +
                        "-fx-text-fill: white;" +
                        "-fx-font-weight: bold;" +
                        "-fx-background-radius: 8;" +
                        "-fx-padding: 7 14;"
                );
            }
        }

        return toolbar;
    }

    private void initializeCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        drawGrid();
    }

    private void drawGrid() {
        gc.setStroke(Color.web("#e5e7eb"));
        gc.setLineWidth(0.5);

        for (int x = 0; x < drawingCanvas.getWidth(); x += 25) {
            gc.strokeLine(x, 0, x, drawingCanvas.getHeight());
        }

        for (int y = 0; y < drawingCanvas.getHeight(); y += 25) {
            gc.strokeLine(0, y, drawingCanvas.getWidth(), y);
        }
    }

    private void setupMouseActions() {
        previewCanvas.addEventHandler(MouseEvent.MOUSE_PRESSED, event -> {
            saveState();

            startX = event.getX();
            startY = event.getY();

            if (getMode().equals("Pen") || getMode().equals("Eraser")) {
                prepareGraphics(gc);
                gc.beginPath();
                gc.moveTo(startX, startY);
                gc.stroke();
            }
        });

        previewCanvas.addEventHandler(MouseEvent.MOUSE_DRAGGED, event -> {
            String mode = getMode();

            if (mode.equals("Pen") || mode.equals("Eraser")) {
                prepareGraphics(gc);
                gc.lineTo(event.getX(), event.getY());
                gc.stroke();
            } else {
                drawPreview(event.getX(), event.getY());
            }
        });

        previewCanvas.addEventHandler(MouseEvent.MOUSE_RELEASED, event -> {
            String mode = getMode();

            if (!mode.equals("Pen") && !mode.equals("Eraser")) {
                clearPreview();
                drawShape(gc, mode, startX, startY, event.getX(), event.getY());
            }
        });
    }

    private void prepareGraphics(GraphicsContext graphics) {
        if (getMode().equals("Eraser")) {
            graphics.setStroke(Color.WHITE);
            graphics.setLineWidth(brushSlider.getValue() * 2);
        } else {
            graphics.setStroke(colorPicker.getValue());
            graphics.setLineWidth(brushSlider.getValue());
        }

        graphics.setLineCap(javafx.scene.shape.StrokeLineCap.ROUND);
        graphics.setLineJoin(javafx.scene.shape.StrokeLineJoin.ROUND);
    }

    private void drawPreview(double endX, double endY) {
        clearPreview();
        prepareGraphics(previewGc);
        drawShape(previewGc, getMode(), startX, startY, endX, endY);
    }

    private void drawShape(GraphicsContext graphics, String mode, double x1, double y1, double x2, double y2) {
        prepareGraphics(graphics);

        double x = Math.min(x1, x2);
        double y = Math.min(y1, y2);
        double width = Math.abs(x2 - x1);
        double height = Math.abs(y2 - y1);

        switch (mode) {
            case "Line":
                graphics.strokeLine(x1, y1, x2, y2);
                break;

            case "Rectangle":
                graphics.strokeRoundRect(x, y, width, height, 12, 12);
                break;

            case "Circle":
                double size = Math.max(width, height);
                graphics.strokeOval(x, y, size, size);
                break;

            default:
                break;
        }
    }

    private void clearPreview() {
        previewGc.clearRect(0, 0, previewCanvas.getWidth(), previewCanvas.getHeight());
    }

    private String getMode() {
        return modeBox.getValue();
    }

    private void saveState() {
        WritableImage snapshot = drawingCanvas.snapshot(new SnapshotParameters(), null);
        undoStack.push(snapshot);

        if (undoStack.size() > 20) {
            undoStack.removeLast();
        }
    }

    private void undo() {
        if (!undoStack.isEmpty()) {
            WritableImage previous = undoStack.pop();
            gc.drawImage(previous, 0, 0);
        }
    }

    private void clearCanvas() {
        saveState();
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, drawingCanvas.getWidth(), drawingCanvas.getHeight());
        drawGrid();
    }

    private void saveCanvas(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Drawing");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );
        fileChooser.setInitialFileName("advanced-drawing.png");

        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                WritableImage image = drawingCanvas.snapshot(new SnapshotParameters(), null);
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
                showAlert("Success", "Drawing saved successfully.");
            } catch (Exception e) {
                showAlert("Error", "Could not save image: " + e.getMessage());
            }
        }
    }

    private void resizeCanvas() {
        TextInputDialog dialog = new TextInputDialog("1200x700");
        dialog.setTitle("Resize Canvas");
        dialog.setHeaderText("Enter new canvas size");
        dialog.setContentText("Format: widthxheight");

        dialog.showAndWait().ifPresent(input -> {
            try {
                String[] parts = input.toLowerCase().split("x");
                double newWidth = Double.parseDouble(parts[0].trim());
                double newHeight = Double.parseDouble(parts[1].trim());

                if (newWidth < 300 || newHeight < 300) {
                    showAlert("Invalid size", "Width and height must be at least 300.");
                    return;
                }

                saveState();

                WritableImage oldImage = drawingCanvas.snapshot(new SnapshotParameters(), null);

                drawingCanvas.setWidth(newWidth);
                drawingCanvas.setHeight(newHeight);
                previewCanvas.setWidth(newWidth);
                previewCanvas.setHeight(newHeight);

                gc.setFill(Color.WHITE);
                gc.fillRect(0, 0, newWidth, newHeight);
                drawGrid();
                gc.drawImage(oldImage, 0, 0);

            } catch (Exception e) {
                showAlert("Invalid format", "Please use format like 1200x700.");
            }
        });
    }

    private void toggleTheme(Button themeButton) {
        darkMode = !darkMode;

        if (darkMode) {
            themeButton.setText("Light Mode");
            drawingCanvas.getParent().setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #020617, #111827);"
            );
        } else {
            themeButton.setText("Dark Mode");
            drawingCanvas.getParent().setStyle(
                    "-fx-background-color: linear-gradient(to bottom right, #eef2ff, #f8fafc);"
            );
        }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
