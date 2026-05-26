package com.example.javafxtest;

import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;

import javax.imageio.ImageIO;
import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Stack;

public class ShapeDrawingAppController {
    @FXML private Canvas canvas;
    @FXML private ColorPicker colorPicker;
    @FXML private Slider lineWidthSlider;
    @FXML private ComboBox<String> shapeSelector;
    @FXML private ToggleButton eraserButton;
    @FXML private Button clearButton;
    @FXML private Button saveButton;
    @FXML private Button undoButton;
    @FXML private Button redoButton;
    @FXML private Button resizeButton;
    @FXML private ToggleButton themeToggle;
    @FXML private Label statusBar;

    private GraphicsContext gc;
    private Color currentColor = Color.BLACK;
    private double currentLineWidth = 3.0;
    private String drawingMode = "Freehand"; // Modes: Freehand, Rectangle, Circle, Line
    private boolean isEraser = false;
    private double startX, startY, endX, endY;
    private Stack<Image> undoStack = new Stack<>();
    private Stack<Image> redoStack = new Stack<>();

    @FXML
    private void initialize() {
        gc = canvas.getGraphicsContext2D();
        initializeCanvas();

        // Bind controls to handlers
        colorPicker.setOnAction(e -> {
            currentColor = colorPicker.getValue();
            gc.setStroke(currentColor);
        });

        lineWidthSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
            currentLineWidth = newVal.doubleValue();
            gc.setLineWidth(currentLineWidth);
        });

        shapeSelector.valueProperty().addListener((obs, oldVal, newVal) -> {
            drawingMode = newVal;
            isEraser = false;
            eraserButton.setSelected(false);
        });

        eraserButton.setOnAction(e -> isEraser = eraserButton.isSelected());
        clearButton.setOnAction(e -> clearCanvas());
        saveButton.setOnAction(e -> saveCanvas());
        undoButton.setOnAction(e -> undo());
        redoButton.setOnAction(e -> redo());
        resizeButton.setOnAction(e -> resizeCanvas());
        themeToggle.setOnAction(e -> toggleTheme());

        canvas.setOnMousePressed(this::onMousePressed);
        canvas.setOnMouseDragged(this::onMouseDragged);
        canvas.setOnMouseReleased(this::onMouseReleased);
    }

    private void onMousePressed(MouseEvent e) {
        saveState();
        startX = e.getX();
        startY = e.getY();
        if (drawingMode.equals("Freehand")) {
            gc.beginPath();
            gc.moveTo(startX, startY);
            gc.stroke();
        }
    }

    private void onMouseDragged(MouseEvent e) {
        if (drawingMode.equals("Freehand") && !isEraser) {
            gc.lineTo(e.getX(), e.getY());
            gc.stroke();
        } else if (isEraser) {
            gc.clearRect(e.getX() - currentLineWidth / 2, e.getY() - currentLineWidth / 2, currentLineWidth, currentLineWidth);
        }
    }

    private void onMouseReleased(MouseEvent e) {
        endX = e.getX();
        endY = e.getY();
        drawShape();
    }

    private void drawShape() {
        gc.setStroke(currentColor);
        gc.setLineWidth(currentLineWidth);

        switch (drawingMode) {
            case "Rectangle":
                double rectWidth = Math.abs(endX - startX);
                double rectHeight = Math.abs(endY - startY);
                gc.strokeRect(Math.min(startX, endX), Math.min(startY, endY), rectWidth, rectHeight);
                break;

            case "Circle":
                double radius = Math.hypot(endX - startX, endY - startY);
                gc.strokeOval(startX - radius, startY - radius, radius * 2, radius * 2);
                break;

            case "Line":
                gc.strokeLine(startX, startY, endX, endY);
                break;

            default:
                break;
        }
    }

    private void initializeCanvas() {
        gc.setFill(Color.WHITE);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());
        gc.setStroke(currentColor);
        gc.setLineWidth(currentLineWidth);
    }

    private void saveState() {
        Image snapshot = canvas.snapshot(null, null);
        undoStack.push(snapshot);
        redoStack.clear();
    }

    void undo() {
        if (!undoStack.isEmpty()) {
            Image previousState = undoStack.pop();
            redoStack.push(canvas.snapshot(null, null));
            gc.drawImage(previousState, 0, 0);
        }
    }

    void redo() {
        if (!redoStack.isEmpty()) {
            Image nextState = redoStack.pop();
            undoStack.push(canvas.snapshot(null, null));
            gc.drawImage(nextState, 0, 0);
        }
    }

    void saveCanvas() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PNG Files", "*.png"),
                new FileChooser.ExtensionFilter("JPEG Files", "*.jpg")
        );
        File file = fileChooser.showSaveDialog(canvas.getScene().getWindow());
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(canvas.snapshot(null, null), null), "png", file);
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

    private void clearCanvas() {
        saveState();
        initializeCanvas();
    }
    
    private void resizeCanvas() {
        TextInputDialog dialog = new TextInputDialog(String.valueOf(canvas.getWidth()));
        dialog.setTitle("Resize Canvas");
        dialog.setHeaderText("Enter new canvas dimensions");
        dialog.setContentText("Width:");
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(width -> {
            double newWidth = Double.parseDouble(width);
            canvas.setWidth(newWidth);
            initializeCanvas();
        });
    }

    private void toggleTheme() {
        if (themeToggle.isSelected()) {
            canvas.getScene().getRoot().setStyle("-fx-background-color: #333; -fx-text-fill: white;");
        } else {
            canvas.getScene().getRoot().setStyle("-fx-background-color: white; -fx-text-fill: black;");
        }
    }
}
