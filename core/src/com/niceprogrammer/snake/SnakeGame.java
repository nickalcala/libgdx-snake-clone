package com.niceprogrammer.snake;

import com.badlogic.gdx.Game;
import com.niceprogrammer.snake.screen.GameScreen;

public class SnakeGame extends Game {

	@Override
	public void create() {
		setScreen(new GameScreen());
	}
}
