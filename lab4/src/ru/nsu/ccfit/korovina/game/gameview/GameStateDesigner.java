package ru.nsu.ccfit.korovina.game.gameview;

import me.ippolitov.fit.snakes.SnakesProto.GameState;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Coord;
import me.ippolitov.fit.snakes.SnakesProto.GameState.Snake;
import ru.nsu.ccfit.korovina.game.GameController;

import java.util.List;

public class GameStateDesigner {
    private GameController gameController;

    public GameStateDesigner(GameController gameController) {
        this.gameController = gameController;
    }

    public void run() {
        GameState gameState = gameController.getGameState();
        List<Snake> snakes = gameState.getSnakesList();
        List<Coord> food = gameState.getFoodsList();

        gameController.getGameView().getGameField().updateGameField(snakes, food);
        gameController.getGameView().getGameField().repaint();
    }
}
