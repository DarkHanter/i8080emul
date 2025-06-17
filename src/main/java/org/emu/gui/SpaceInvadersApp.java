package org.emu.gui;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.animation.AnimationTimer;
import org.emu.machine.SpaceInvadersMachine;

public class SpaceInvadersApp extends Application {
    private static final int WIDTH = 224;
    private static final int HEIGHT = 256;
    private static final int SCALE = 2;

    private SpaceInvadersMachine machine;
    private Canvas canvas;
    private GraphicsContext gc;

    @Override
    public void start(Stage primaryStage) {
        machine = new SpaceInvadersMachine();
        try {
            machine.loadRoms();
        } catch (Exception e) {
            System.err.println("Ошибка загрузки ROM: " + e.getMessage());
            return;
        }

        canvas = new Canvas(WIDTH * SCALE, HEIGHT * SCALE);
        gc = canvas.getGraphicsContext2D();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, canvas.getWidth(), canvas.getHeight());

        StackPane root = new StackPane(canvas);
        Scene scene = new Scene(root);

        scene.setOnKeyPressed(event -> {
            KeyCode code = event.getCode();
            switch (code) {
                case LEFT:
                    machine.setLeftPressed(true);
                    break;
                case RIGHT:
                    machine.setRightPressed(true);
                    break;
                case SPACE:
                    machine.setFirePressed(true);
                    break;
                case C:
                    machine.setCoinInserted(true);
                    break;
                case DIGIT1:
                    machine.setStart1Pressed(true);
                    break;
                case DIGIT2:
                    machine.setStart2Pressed(true);
                    break;
                default:
                    break;
            }
        });
        scene.setOnKeyReleased(event -> {
            KeyCode code = event.getCode();
            switch (code) {
                case LEFT:
                    machine.setLeftPressed(false);
                    break;
                case RIGHT:
                    machine.setRightPressed(false);
                    break;
                case SPACE:
                    machine.setFirePressed(false);
                    break;
                case C:
                    machine.setCoinInserted(false);
                    break;
                case DIGIT1:
                    machine.setStart1Pressed(false);
                    break;
                case DIGIT2:
                    machine.setStart2Pressed(false);
                    break;
                default:
                    break;
            }
        });

        primaryStage.setTitle("Space Invaders 8080 Emulator");
        primaryStage.setScene(scene);
        primaryStage.setResizable(false);
        primaryStage.show();

        AnimationTimer timer = new AnimationTimer() {
            @Override
            public void handle(long now) {
                machine.executeFrame();
                renderFrame();
            }
        };
        timer.start();
    }

    private void renderFrame() {
        var writer = gc.getPixelWriter();
        gc.setFill(Color.BLACK);
        gc.fillRect(0, 0, WIDTH * SCALE, HEIGHT * SCALE);

        byte[] memory = machine.getMemory().getRaw();

        for (int i = 0; i < 0x1C00; i++) {
            int addr = 0x2400 + i;
            int byteValue = memory[addr] & 0xFF;

            int x = (i * 8) / HEIGHT;
            int yBase = (i * 8) % HEIGHT;

            for (int bit = 0; bit < 8; bit++) {
                boolean pixelOn = (byteValue & (1 << bit)) != 0;
                if (pixelOn) {
                    int y = yBase + bit;
                    int drawX = x * SCALE;
                    int drawY = (HEIGHT - 1 - y) * SCALE;
                    for (int dx = 0; dx < SCALE; dx++) {
                        for (int dy = 0; dy < SCALE; dy++) {
                            writer.setColor(drawX + dx, drawY + dy, Color.WHITE);
                        }
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
