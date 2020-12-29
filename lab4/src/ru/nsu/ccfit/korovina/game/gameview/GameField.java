package ru.nsu.ccfit.korovina.game.gameview;

import me.ippolitov.fit.snakes.SnakesProto.GameState.Coord;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Snake;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class GameField extends JPanel {
    private BufferedImage gameFieldImage;
    private int cellSize;
    private int cellsHorizontal, cellsVertical;
    private int gameFieldWidth, gameFieldHeight;
    private HashMap<Integer, Color> snakesColors;

    GameField(int cellsHorizontal, int cellsVertical, int contentPaneWidth, int contentPaneHeight) {
        int wCellSize = (int)(((double)contentPaneWidth / cellsHorizontal));
        int hCellSize = (int)(((double)contentPaneHeight / cellsVertical));

        this.cellSize = Math.min(wCellSize, hCellSize);

        this.cellsHorizontal = cellsHorizontal;
        this.cellsVertical = cellsVertical;

        this.gameFieldWidth = cellSize * cellsHorizontal;
        this.gameFieldHeight = cellSize * cellsVertical;
        this.gameFieldImage = new BufferedImage(gameFieldWidth, gameFieldHeight, BufferedImage.TYPE_INT_RGB);
        this.setPreferredSize(new Dimension(gameFieldWidth, gameFieldHeight));

        this.snakesColors = new HashMap<>();

    }

    private void drawSnake(Snake snake) {
        Graphics2D graphics = gameFieldImage.createGraphics();

        if (snakesColors.keySet().contains(snake.getPlayerId())) {
            graphics.setColor(snakesColors.get(snake.getPlayerId()));
        } else {
            Random rand = new Random();
            int r = rand.nextInt(255);
            int g = rand.nextInt(255);
            int b = rand.nextInt(255);
            Color color = new Color(r, g, b);
            graphics.setColor(color);
            snakesColors.put(snake.getPlayerId(), color);
        }

        List<Coord> coords = snake.getPointsList();
        int previousX = coords.get(0).getX();
        int previousY = coords.get(0).getY();

        Coord point;
        int dx, dy, startX, startY, nextX, nextY;
        for (int i = 1; i < coords.size(); i++) {
            point = coords.get(i);
            startX = previousX;
            startY = previousY;
            nextX = previousX + point.getX();
            nextY = previousY + point.getY();
            dx = startX > nextX ? -1 : 1;
            dy = startY > nextY ? -1 : 1;
            for (int x = startX; x != nextX + dx; x += dx) {
                for (int y = startY; y != nextY + dy; y += dy) {
                    graphics.fillRect(((x + cellsHorizontal) % cellsHorizontal) * cellSize,
                            ((y + cellsVertical) % cellsVertical) * cellSize, cellSize, cellSize);
                }
            }
            previousX = (previousX + point.getX() + cellsHorizontal) % cellsHorizontal;
            previousY = (previousY + point.getY() + cellsVertical) % cellsVertical;
        }
    }

    private void drawFood(Coord food) {
        Graphics2D graphics = gameFieldImage.createGraphics();
        graphics.setColor(Color.ORANGE);

        graphics.fillRect(food.getX() * cellSize, food.getY() * cellSize, cellSize, cellSize);
    }

    void updateGameField(List<Snake> snakes, List<Coord> foods) {
        Graphics2D graphics = gameFieldImage.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, gameFieldImage.getWidth(), gameFieldImage.getHeight());

        graphics.setColor(Color.DARK_GRAY);
        graphics.setStroke(new BasicStroke(1));
        int cellSize = gameFieldImage.getWidth() / cellsHorizontal;
        for (int i = 1; i < cellsHorizontal; i++) {
            graphics.drawLine(i * cellSize, 0, i * cellSize, gameFieldImage.getHeight());
        }
        for (int i = 1; i < cellsVertical; i++) {
            graphics.drawLine(0, i * cellSize, gameFieldImage.getWidth(), i * cellSize);
        }

        snakes.forEach(this::drawSnake);
        foods.forEach(this::drawFood);
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(gameFieldImage, 0, 0, this);
    }

    int getGameFieldHeight() {
        return gameFieldHeight;
    }

    int getGameFieldWidth() {
        return gameFieldWidth;
    }
}
